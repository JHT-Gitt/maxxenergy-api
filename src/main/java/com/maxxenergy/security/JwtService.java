package com.maxxenergy.security;

import io.jsonwebtoken.*;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import java.security.Key;
import java.util.Date;

@Service
public class JwtService {
    private final Key key;
    private final long expMs;

//    public JwtService(
//            @Value("${app.jwt.secret}") String secret,
//            @Value("${app.jwt.exp-min}") long expMin) {
//        this.key = Keys.hmacShaKeyFor(secret.getBytes());
//        this.expMs = expMin * 60_000;
//    }
public JwtService(
        @Value("${app.jwt.secret}") String secret,
        @Value("${app.jwt.exp-min}") long expMin) {

    if (secret == null || secret.length() < 32) {
        throw new IllegalStateException("app.jwt.secret must be at least 32 characters");
    }
    this.key = Keys.hmacShaKeyFor(secret.getBytes());
    this.expMs = expMin * 60_000;
}

    public String generate(String username, String role) {
        return Jwts.builder()
                .setSubject(username)
                .claim("role", role)
                .setExpiration(new Date(System.currentTimeMillis() + expMs))
                .signWith(key, SignatureAlgorithm.HS256)
                .compact();
    }

    public Jws<Claims> parse(String token) {
        return Jwts.parserBuilder().setSigningKey(key).build().parseClaimsJws(token);
    }
}
