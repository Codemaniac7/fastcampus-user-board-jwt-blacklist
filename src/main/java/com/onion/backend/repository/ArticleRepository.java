package com.onion.backend.repository;

import com.onion.backend.entity.Article;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ArticleRepository extends JpaRepository<Article, Long> {
    List<Article> findTop10ByBoard_IdOrderByCreatedDateDesc(Long boardId);
    List<Article> findTop10ByBoard_IdAndIdLessThanOrderByCreatedDateDesc(Long boardId, Long articleId);
    List<Article> findTop10ByBoard_IdAndIdGreaterThanOrderByCreatedDateDesc(Long boardId, Long articleId);


}
