package com.onion.backend.jwt;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.security.Key;
import java.util.Date; // java.util.Date 사용
import java.time.Instant;

@Component
public class JwtUtil {

    @Value("${jwt.secret}")
    private String secretKey;

    private static final long EXPIRATION_TIME = 1000 * 60 * 60; // 1 hour in milliseconds

    // 🔹 키 생성
    private Key getSigningKey() {
        return Keys.hmacShaKeyFor(secretKey.getBytes(StandardCharsets.UTF_8));
    }


    // 🔹 JWT 생성
    public String generateToken(String username) {
        // 현재 시간 가져오기
        long currentTimeMillis = System.currentTimeMillis();

        return Jwts.builder()
                .setSubject(username) // 사용자 정보
                .setIssuedAt(new Date(currentTimeMillis)) // 발행 시간
                .setExpiration(new Date(currentTimeMillis + EXPIRATION_TIME)) // 만료 시간
                .signWith(getSigningKey(), SignatureAlgorithm.HS256) // 서명
                .compact(); // JWT 반환
    }


    // 🔹 JWT에서 사용자 이름 추출
    public String extractUsername(String token) {
        return getClaims(token).getSubject();
    }

    // 🔹 토큰 만료 여부 확인
    public boolean isTokenExpired(String token) {
        Date expirationDate = getClaims(token).getExpiration();
        return expirationDate.before(new Date()); // 만료 시간을 현재 시간과 비교
    }

    public boolean validateToken(String token) {
        String extractedUsername = extractUsername(token);
        return (extractedUsername != null && !isTokenExpired(token));
    }


    // 🔹 토큰에서 Claims(정보) 추출
    private Claims getClaims(String token) {
        return Jwts.parserBuilder()
                .setSigningKey(getSigningKey())
                .build()
                .parseClaimsJws(token)
                .getBody();
    }

    public String getUserNameFromToken(String token) {
        Claims claims = Jwts.parserBuilder()
                .setSigningKey(getSigningKey())
                .build()
                .parseClaimsJws(token)
                .getBody();
        return claims.getSubject();
    }

    public Date getExpirationDateFromToken(String token) {
        Claims claims = Jwts.parserBuilder()
                .setSigningKey(getSigningKey())
                .build()
                .parseClaimsJws(token)
                .getBody();
        return claims.getExpiration();
    }

}
