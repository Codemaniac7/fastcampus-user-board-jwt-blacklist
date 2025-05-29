package com.onion.backend.repository;

import com.onion.backend.entity.Article;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface ArticleRepository extends JpaRepository<Article, Long> {
    List<Article> findTop10ByBoard_IdOrderByCreatedDateDesc(Long boardId);
    List<Article> findTop10ByBoard_IdAndIdLessThanOrderByCreatedDateDesc(Long boardId, Long articleId);
    List<Article> findTop10ByBoard_IdAndIdGreaterThanOrderByCreatedDateDesc(Long boardId, Long articleId);

    Optional<Article> findFirstByAuthor_UsernameOrderByCreatedDateDesc(String username);
    Optional<Article> findTopByAuthorUsernameOrderByUpdatedDateDesc(String username);
}
