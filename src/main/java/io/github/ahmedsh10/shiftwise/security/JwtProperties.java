package io.github.ahmedsh10.shiftwise.security;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "shiftwise.jwt")
public record JwtProperties(String secret, long ttlMinutes) {

    public JwtProperties {
        if (secret == null || secret.length() < 32) {
            throw new IllegalStateException("shiftwise.jwt.secret must be at least 32 characters");
        }
        if (ttlMinutes <= 0) {
            ttlMinutes = 480;
        }
    }
}
