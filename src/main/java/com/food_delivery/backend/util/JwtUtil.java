package com.food_delivery.backend.util;

import com.food_delivery.backend.config.properties.JwtProperties;
import io.jsonwebtoken.*;
import io.jsonwebtoken.security.Keys;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.security.Key;
import java.util.Date;
import java.util.Map;

@Component
@RequiredArgsConstructor

public class JwtUtil {

    private static final String TOKEN_VERSION_CLAIM = "tv";
    private static final String ROLE_CLAIM = "role";

    private final JwtProperties jwtProperties;

    @PostConstruct
    void validateConfiguration() {
        if (jwtProperties.getSecret().getBytes(StandardCharsets.UTF_8).length < 32) {
            throw new IllegalStateException("JWT secret must be at least 32 bytes long");
        }
    }

    private Key getSigningKey() {
        return Keys.hmacShaKeyFor(jwtProperties.getSecret().getBytes(StandardCharsets.UTF_8));
    }


    public String generateToken(String email, int tokenVersion, String role) {
        Date now = new Date();
        return Jwts.builder()
                .setSubject(email)
                .setIssuer(jwtProperties.getIssuer())
                .setIssuedAt(now)
                .setExpiration(new Date(now.getTime() + jwtProperties.getExpirationMillis()))
                .addClaims(Map.of(
                        TOKEN_VERSION_CLAIM, tokenVersion,
                        ROLE_CLAIM, role
                ))
                .signWith(getSigningKey(), SignatureAlgorithm.HS256)
                .compact();
    }

    public String generateToken(String email, int tokenVersion) {
        // Default to USER role if not specified
        return generateToken(email, tokenVersion, "USER");
    }
    public String extractRole(String token) {
        return getClaims(token).get(ROLE_CLAIM, String.class);
    }

    public String extractEmail(String token) {
        return getClaims(token).getSubject();
    }

    public boolean validateToken(String token) {
        try {
            getClaims(token);
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    private Claims getClaims(String token) {
        return Jwts.parserBuilder()
                .requireIssuer(jwtProperties.getIssuer())
                .setSigningKey(getSigningKey())
                .build()
                .parseClaimsJws(token)
                .getBody();
    }

    public int extractTokenVersion(String token) {
        return getClaims(token).get(TOKEN_VERSION_CLAIM, Integer.class);
    }
}
