package com.onion.backend.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

@Entity
@Getter
@Setter
@NoArgsConstructor  // 기본 생성자
@AllArgsConstructor // 모든 필드를 포함한 생성자
@Table(name = "users")
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;  // User의 고유 ID

    @Column(nullable = false)
    private String username;  // 사용자의 ID (로그인 ID)

    @JsonIgnore
    @Column(nullable = false)
    private String password;  // 비밀번호

    @Column(nullable = false)
    @JsonIgnore
    private String email;  // 이메일 (로그인 시 사용할 수 있음)

    @Column(nullable = false)
    private String role = "USER";  // 기본 role은 USER로 설정

    @CreationTimestamp
    @Column(insertable = true)
    private LocalDateTime createdAt;  // 계정 생성일 (자동으로 입력)

    @UpdateTimestamp
    @Column(nullable = false)
    private LocalDateTime updatedAt;  // 계정 갱신일 (수정 시 자동 갱신)

    private LocalDateTime lastLogin;  // 최근 로그인 날짜

    // Lombok을 통해 생성자, getter, setter 자동 생성
}
