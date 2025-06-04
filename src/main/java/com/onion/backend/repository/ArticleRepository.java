package com.onion.backend.repository;

import com.onion.backend.entity.Article;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface ArticleRepository extends JpaRepository<Article, Long> {

    // 1. 최신 10개 게시글 조회 (첫 로딩 시)
    List<Article> findTop10ByBoard_IdAndIsDeletedFalseOrderByCreatedDateDesc(Long boardId);

    // 2. 이전 10개 게시글 조회 (스크롤 등)
    List<Article> findTop10ByBoard_IdAndIdLessThanAndIsDeletedFalseOrderByCreatedDateDesc(Long boardId, Long articleId);

    // 3. 이후 10개 게시글 조회 (새 글 새로고침 등)
    List<Article> findTop10ByBoard_IdAndIdGreaterThanAndIsDeletedFalseOrderByCreatedDateDesc(Long boardId, Long articleId);

    // 4. 특정 사용자가 작성한 가장 최신 게시글 조회 (작성 가능 여부 체크용)
    Optional<Article> findFirstByAuthor_UsernameAndIsDeletedFalseOrderByCreatedDateDesc(String username);

    // Optional<Article> findTopByAuthorUsernameOrderByUpdatedDateDesc(String username);
}
