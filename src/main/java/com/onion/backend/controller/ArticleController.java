package com.onion.backend.controller;


import com.onion.backend.dto.WriteArticleDto;
import com.onion.backend.entity.Article;
import com.onion.backend.entity.User;
import com.onion.backend.service.ArticleService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.web.bind.annotation.*;

import java.util.List;

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

    @GetMapping("/{boardId}/articles")
    public ResponseEntity<List<Article>> getArticle(@PathVariable Long boardId,
                                                    @RequestParam(required = false) Long lastId,
                                                    @RequestParam(required = false) Long firstId){
        if(lastId!=null) {
            return ResponseEntity.ok(articleService.getOldArticle(boardId, lastId));
        }
        if(firstId!=null) {
            return ResponseEntity.ok(articleService.getNewArticle(boardId, firstId));
        }
        return ResponseEntity.ok(articleService.firstGetArticle(boardId));
    }

}
