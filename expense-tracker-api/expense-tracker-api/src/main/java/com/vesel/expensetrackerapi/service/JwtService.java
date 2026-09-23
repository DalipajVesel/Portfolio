package com.vesel.expensetrackerapi.service;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;

@Service
public class JwtService {

    // one day in milliseconds
    private static final long VALID_FOR = 24 * 60 * 60 * 1000L;

    private final SecretKey key;

    public JwtService(@Value("${jwt.secret}") String secret) {
        this.key = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
    }

    public String createToken(String username) {
        Date now = new Date();
        Date until = new Date(now.getTime() + VALID_FOR);

        return Jwts.builder()
                .subject(username)
                .issuedAt(now)
                .expiration(until)
                .signWith(key)
                .compact();
    }

    // gives back the username, or null if the token is fake, changed or expired
    public String readUsername(String token) {
        try {
            Claims claims = Jwts.parser()
                    .verifyWith(key)
                    .build()
                    .parseSignedClaims(token)
                    .getPayload();
            return claims.getSubject();
        } catch (Exception e) {
            return null;
        }
    }
}
