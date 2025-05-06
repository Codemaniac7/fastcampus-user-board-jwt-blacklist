package com.onion.backend.service;

import com.onion.backend.entity.JwtBlacklist;
import com.onion.backend.jwt.JwtUtil;
import com.onion.backend.repository.JwtBlacklistRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Date;
import java.util.Optional;

@Service
public class JwtBlacklistService {

    private final JwtBlacklistRepository jwtBlacklistRepository;
    private final JwtUtil jwtUtil;

    @Autowired
    public JwtBlacklistService(JwtBlacklistRepository jwtBlacklistRepository, JwtUtil jwtUtil) {
        this.jwtBlacklistRepository = jwtBlacklistRepository;
        this.jwtUtil=jwtUtil;
    }

    public void blacklistToken(String token, LocalDateTime expirationTime, String username) {
        JwtBlacklist jwtBlacklist = new JwtBlacklist();
        jwtBlacklist.setToken(token);
        jwtBlacklist.setExpirationTime(expirationTime);
        jwtBlacklist.setUsername(username);
        jwtBlacklistRepository.save(jwtBlacklist);
    }

    public boolean isTokenBlacklisted(String currentToken) {
        // 블랙리스트 저장소에 현재 토큰이 있는지 직접 확인
        boolean isBlacklisted = jwtBlacklistRepository.existsByToken(currentToken);

        if (isBlacklisted) {
            Optional<JwtBlacklist> blacklistedToken = jwtBlacklistRepository.findByToken(currentToken);
            if (blacklistedToken.isPresent()) {
                // 블랙리스트에 있고, 만료 시간이 아직 지나지 않았는지 확인 (선택 사항)
                if (blacklistedToken.get().getExpirationTime().isAfter(LocalDateTime.now())) {
                    return true; // 블랙리스트에 있고, 아직 만료되지 않았음
                } else {
                    // 블랙리스트에 있지만 만료되었으므로 제거 (선택 사항)
                    jwtBlacklistRepository.delete(blacklistedToken.get());
                    return false; // 더 이상 블랙리스트 처리 안 함
                }
            }
            return true; // 블랙리스트에 존재 (만료 시간 확인 로직이 없다면 바로 true 반환)
        }

        return false; // 블랙리스트에 없음
    }
}
