package com.example.wmbservice.config;

import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.NoSuchAlgorithmException;
import java.security.interfaces.RSAPrivateKey;
import java.security.interfaces.RSAPublicKey;
import java.util.UUID;

import com.nimbusds.jose.JOSEException;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.oauth2.jwt.NimbusJwtEncoder;

import com.nimbusds.jose.jwk.JWKSet;
import com.nimbusds.jose.jwk.RSAKey;
import com.nimbusds.jose.jwk.source.ImmutableJWKSet;
import com.nimbusds.jose.jwk.source.JWKSource;
import com.nimbusds.jose.proc.SecurityContext;

@Configuration
public class JwtConfig {

    /**
     * In-memory RSA keypair for signing JWTs.
     * NOTE: tokens become invalid on app restart; move to a persisted keystore for production.
     */
    @Bean
    RSAKey rsaJwk() throws NoSuchAlgorithmException {
        KeyPairGenerator kpg = KeyPairGenerator.getInstance("RSA");
        kpg.initialize(2048);

        KeyPair kp = kpg.generateKeyPair();
        RSAPublicKey publicKey = (RSAPublicKey) kp.getPublic();
        RSAPrivateKey privateKey = (RSAPrivateKey) kp.getPrivate();

        return new RSAKey.Builder(publicKey)
                .privateKey(privateKey)
                .keyID(UUID.randomUUID().toString())
                .build();
    }

    @Bean
    JwtEncoder jwtEncoder(RSAKey rsaJwk) {
        JWKSource<SecurityContext> jwks = new ImmutableJWKSet<>(new JWKSet(rsaJwk));
        return new NimbusJwtEncoder(jwks);
    }

    @Bean
    JwtDecoder jwtDecoder(RSAKey rsaJwk) throws JOSEException {
        return NimbusJwtDecoder.withPublicKey(rsaJwk.toRSAPublicKey()).build();
    }
}
