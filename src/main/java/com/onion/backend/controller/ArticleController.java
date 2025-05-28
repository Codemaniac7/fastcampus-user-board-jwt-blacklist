package com.onion.backend.controller;


import com.onion.backend.dto.WriteArticleDto;
import com.onion.backend.entity.Article;
import com.onion.backend.entity.User;
import com.onion.backend.service.ArticleService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/boards")
public class ArticleController {

    private final AuthenticationManager authenticationManager;
    private final ArticleService articleService;

    public ArticleController(AuthenticationManager authenticationManager, ArticleService articleService) {
        this.articleService = articleService;
        this.authenticationManager = authenticationManager;
    }

    @PostMapping("/{boardId}/articles")
    public ResponseEntity<Article> writeArticle(@RequestBody WriteArticleDto writeArticleDto){
        return ResponseEntity.ok(articleService.writeArticle(writeArticleDto));
    }

}
