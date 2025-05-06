package com.onion.backend.controller;

import com.onion.backend.dto.LoginRequest;
import com.onion.backend.dto.SignUpUser;
import com.onion.backend.entity.User;
import com.onion.backend.jwt.JwtUtil;
import com.onion.backend.service.CustomUserDetailsService;
import com.onion.backend.service.JwtBlacklistService;
import com.onion.backend.service.UserService;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Collections;
import java.util.Date;
import java.util.List;

@RestController
@RequestMapping("/api/users")
@SecurityRequirement(name = "BearerAuth") // 🔹 기본적으로 모든 메서드에 JWT 인증 적용
public class UserController {
    private final Logger logger = LoggerFactory.getLogger(this.getClass());
    private final UserService userService;
    private final JwtUtil jwtUtil;
    private final AuthenticationManager authenticationManager;
    private final CustomUserDetailsService userDetailsService;
    private final JwtBlacklistService jwtBlacklistService;

    @Autowired
    public UserController(AuthenticationManager authenticationManager, UserService userService, JwtUtil jwtUtil, CustomUserDetailsService userDetailsService, JwtBlacklistService jwtBlacklistService) {
        this.userService = userService;
        this.jwtUtil = jwtUtil;
        this.authenticationManager = authenticationManager;
        this.userDetailsService = userDetailsService;
        this.jwtBlacklistService=jwtBlacklistService;
    }

    @GetMapping
    public ResponseEntity<List<User>> getUsers() {
        return ResponseEntity.ok(userService.getUsers());
    }

    // 유저 생성 API
    @PostMapping("/signUp")
    public ResponseEntity<User> createUser(@RequestBody SignUpUser signUpUser) {
        User createdUser = userService.createUser(signUpUser);
        return ResponseEntity.ok(createdUser);
    }

    // 유저 삭제 API
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteUser(@Parameter(description = "ID of the user to delete")  @PathVariable Long id) {
        boolean isDeleted = userService.deleteUser(id);

        if (isDeleted) {
            return ResponseEntity.noContent().build(); // 204 No Content 응답
        } else {
            return ResponseEntity.notFound().build(); // 404 Not Found 응답
        }
    }

    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody LoginRequest loginRequest, HttpServletResponse response) {
        try {
            Authentication authentication = authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(
                            loginRequest.getUsername(),
                            loginRequest.getPassword()
                    )
            );
            SecurityContextHolder.getContext().setAuthentication(authentication);
            String token = jwtUtil.generateToken(authentication.getName());
            Cookie cookie = new Cookie("onion_token", token);
            cookie.setHttpOnly(true);
            cookie.setPath("/");
            cookie.setMaxAge(60 * 60);
            response.addCookie(cookie);
            return ResponseEntity.ok(Collections.singletonMap("token", token));
        } catch (BadCredentialsException e) {
            return ResponseEntity.status(401).body("Invalid username or password");
        } catch (UsernameNotFoundException e) {
            return ResponseEntity.status(404).body("User not found");
        } catch (Exception e) {
            e.printStackTrace(); // 서버 로그에 예외 출력
            return ResponseEntity.status(500).body("Authentication failed: " + e.getMessage());
        }
    }


    @PostMapping("/token/validation")
    @ResponseStatus(HttpStatus.OK)
    public void jwtValidate(@RequestParam("token") String token) {
            if(!jwtUtil.validateToken(token)) {
                throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Token is not valid");
            }
    }

    @PostMapping("/logout")
    public void logout(HttpServletResponse response) {
        Cookie cookie = new Cookie("onion_token", null);
        cookie.setHttpOnly(true);
        cookie.setPath("/");
        cookie.setMaxAge(0);
        response.addCookie(cookie);
    }


    @PostMapping("/logout/all")
    public void logout(@RequestParam(required = false) String requestToken, HttpServletResponse response, @CookieValue(name = "onion_token", required = false) String cookieToken) {
        String token = requestToken!=null ? requestToken : cookieToken;
        LocalDateTime expirationTime = null;
        String username = null;
        if (token != null) {
            try {
                expirationTime = jwtUtil.getExpirationDateFromToken(token).toInstant()
                        .atZone(ZoneId.systemDefault()).toLocalDateTime();
                username = jwtUtil.getUserNameFromToken(token);
                jwtBlacklistService.blacklistToken(token, expirationTime, username);
                logger.info("블랙리스트에 토큰 추가 - 사용자: {}, 만료 시간: {}", username, expirationTime);
            } catch (Exception e) {
                logger.warn("토큰 처리 중 오류 발생: {}", e.getMessage());
            }
        } else {
            logger.info("요청에 토큰이 없어 블랙리스트 처리를 건너뜁니다.");
        }

        Cookie cookie = new Cookie("onion_token", null);
        cookie.setHttpOnly(true);
        cookie.setPath("/");
        cookie.setMaxAge(0);
        response.addCookie(cookie);

        logger.info("onion_token 쿠키 삭제 시도 - MaxAge: {}, Expires: 0", cookie.getMaxAge());
    }

}
