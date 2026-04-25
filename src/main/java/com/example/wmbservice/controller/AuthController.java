package com.example.wmbservice.controller;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.web.bind.annotation.*;

/**
 * Password-based login that returns a backend-issued JWT.
 *
 * React flow:
 * 1) POST /auth/login {"password":"..."}
 * 2) Receive {"accessToken":"...","tokenType":"Bearer","expiresIn":86400}
 * 3) Use Authorization: Bearer <accessToken> on /api/v2/**
 */
@RestController
@RequestMapping("/auth")
public class AuthController {

    private final JwtEncoder jwtEncoder;
    private final PasswordEncoder passwordEncoder = new BCryptPasswordEncoder();

    private final String passwordHash;
    private final String passwordPlain;
    private final long tokenTtlSeconds;

    private final int maxLoginAttempts;
    private final Duration lockDuration;

    private static final class AttemptState {
        int failedAttempts;
        Instant lockedUntil;
        Instant lastSeen;

        AttemptState(Instant now) {
            this.failedAttempts = 0;
            this.lockedUntil = null;
            this.lastSeen = now;
        }
    }

    private final Map<String, AttemptState> attemptsByIp = new ConcurrentHashMap<>();

    @Value("${wmb.auth.attempt-cache-ttl-seconds:3600}")
    private long attemptCacheTtlSeconds;

    public AuthController(
            JwtEncoder jwtEncoder,
            @Value("${wmb.auth.password-hash:}") String passwordHash,
            @Value("${wmb.auth.password:}") String passwordPlain,
            @Value("${wmb.auth.token-ttl-seconds:86400}") long tokenTtlSeconds,
            @Value("${wmb.auth.max-login-attempts:10}") int maxLoginAttempts,
            @Value("${wmb.auth.lock-seconds:300}") long lockSeconds) {
        this.jwtEncoder = jwtEncoder;
        this.passwordHash = passwordHash == null ? "" : passwordHash.trim();
        this.passwordPlain = passwordPlain == null ? "" : passwordPlain;
        this.tokenTtlSeconds = tokenTtlSeconds;
        this.maxLoginAttempts = Math.max(1, maxLoginAttempts);
        this.lockDuration = Duration.ofSeconds(Math.max(1, lockSeconds));
    }

    public record LoginRequest(@NotBlank String password) {
    }

    public record LoginResponse(String accessToken, String tokenType, long expiresIn) {
    }

    @PostMapping("/login")
    public ResponseEntity<?> login(@Valid @RequestBody LoginRequest request, HttpServletRequest httpRequest) {
        if (!isPasswordConfigured()) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Server auth is not configured. Set WMB_PASSWORD_HASH (preferred) or WMB_PASSWORD.");
        }

        Instant now = Instant.now();
        String clientIp = getClientIp(httpRequest);

        cleanupOldEntries(now);

        ResponseEntity<?> blocked = checkLocked(clientIp, now);
        if (blocked != null) {
            return blocked;
        }

        if (!matchesConfiguredPassword(request.password())) {
            return registerFailureAndMaybeLock(clientIp, now);
        }

        resetFailures(clientIp);

        Instant exp = now.plusSeconds(tokenTtlSeconds);

        // Keep claims minimal. Add scopes now so you can re-enable scope checks later if you want.
        JwtClaimsSet claims = JwtClaimsSet.builder()
                .issuer("wmbservice")
                .issuedAt(now)
                .expiresAt(exp)
                .subject("local-user")
                .claim("scope", String.join(" ", List.of("transactions.read", "transactions.write")))
                .build();

        String token = jwtEncoder.encode(JwtEncoderParameters.from(claims)).getTokenValue();
        return ResponseEntity.ok(new LoginResponse(token, "Bearer", tokenTtlSeconds));
    }

    private ResponseEntity<?> checkLocked(String clientIp, Instant now) {
        AttemptState state = attemptsByIp.computeIfAbsent(clientIp, ip -> new AttemptState(now));
        synchronized (state) {
            state.lastSeen = now;
            if (state.lockedUntil != null && now.isBefore(state.lockedUntil)) {
                long retryAfterSeconds = Math.max(1, Duration.between(now, state.lockedUntil).getSeconds());
                return ResponseEntity.status(429)
                        .header("Retry-After", Long.toString(retryAfterSeconds))
                        .body("Too many login attempts. Try again later.");
            }
            if (state.lockedUntil != null && !now.isBefore(state.lockedUntil)) {
                state.lockedUntil = null;
                state.failedAttempts = 0;
            }
            return null;
        }
    }

    private ResponseEntity<?> registerFailureAndMaybeLock(String clientIp, Instant now) {
        AttemptState state = attemptsByIp.computeIfAbsent(clientIp, ip -> new AttemptState(now));
        synchronized (state) {
            state.lastSeen = now;
            state.failedAttempts++;
            if (state.failedAttempts >= maxLoginAttempts) {
                state.lockedUntil = now.plus(lockDuration);
                long retryAfterSeconds = Math.max(1, lockDuration.getSeconds());
                return ResponseEntity.status(429)
                        .header("Retry-After", Long.toString(retryAfterSeconds))
                        .body("Too many login attempts. Try again later.");
            }
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Invalid credentials");
        }
    }

    private void resetFailures(String clientIp) {
        AttemptState state = attemptsByIp.get(clientIp);
        if (state == null) {
            return;
        }
        synchronized (state) {
            state.failedAttempts = 0;
            state.lockedUntil = null;
        }
    }

    private void cleanupOldEntries(Instant now) {
        long ttl = Math.max(60, attemptCacheTtlSeconds);
        Instant cutoff = now.minusSeconds(ttl);
        attemptsByIp.entrySet().removeIf(e -> {
            AttemptState s = e.getValue();
            Instant last = s.lastSeen;
            return last != null && last.isBefore(cutoff);
        });
    }

    private static String getClientIp(HttpServletRequest request) {
        // Trust proxy headers only if you're actually behind a trusted proxy.
        String xff = request.getHeader("X-Forwarded-For");
        if (xff != null && !xff.isBlank()) {
            // format: client, proxy1, proxy2
            String first = xff.split(",")[0].trim();
            if (!first.isBlank()) {
                return first;
            }
        }
        String xRealIp = request.getHeader("X-Real-IP");
        if (xRealIp != null && !xRealIp.isBlank()) {
            return xRealIp.trim();
        }
        return request.getRemoteAddr();
    }

    private boolean isPasswordConfigured() {
        return !passwordHash.isBlank() || !passwordPlain.isBlank();
    }

    private boolean matchesConfiguredPassword(String rawPassword) {
        if (!passwordHash.isBlank()) {
            return passwordEncoder.matches(rawPassword, passwordHash);
        }
        // fallback: plain password (dev convenience)
        return passwordPlain.equals(rawPassword);
    }
}
