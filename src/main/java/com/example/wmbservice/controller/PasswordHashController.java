package com.example.wmbservice.controller;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.web.bind.annotation.*;

/**
 * Utility endpoint to generate a BCrypt hash for a password.
 *
 * Keep this endpoint disabled in production. For a single-user setup, this is
 * convenient during initial setup.
 */
@RestController
@RequestMapping("/auth")
public class PasswordHashController {

    private final BCryptPasswordEncoder encoder = new BCryptPasswordEncoder();

    public record HashRequest(@NotBlank String password) {}

    public record HashResponse(String bcryptHash) {}

    @PostMapping("/hash")
    public ResponseEntity<HashResponse> hash(@Valid @RequestBody HashRequest request) {
        return ResponseEntity.ok(new HashResponse(encoder.encode(request.password())));
    }
}

