package com.onion.backend.service;

import com.onion.backend.dto.EditArticleDto;
import com.onion.backend.dto.WriteArticleDto;
import com.onion.backend.entity.Article;
import com.onion.backend.entity.Board;
import com.onion.backend.entity.User;
import com.onion.backend.exception.RateLimitException;
import com.onion.backend.exception.ResourceNotFoundException;
import com.onion.backend.repository.ArticleRepository;
import com.onion.backend.repository.BoardRepository;
import com.onion.backend.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Service
public class ArticleService {
    private final BoardRepository boardRepository;
    private final ArticleRepository articleRepository;
    private final UserRepository userRepository;

    @Autowired
    public ArticleService(BoardRepository boardRepository, ArticleRepository articleRepository, UserRepository userRepository) {
        this.boardRepository = boardRepository;
        this.articleRepository = articleRepository;
        this.userRepository = userRepository;
    }

    public Article writeArticle(Long boardId, WriteArticleDto dto) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        UserDetails userDetails = (UserDetails)authentication.getPrincipal();
        if(!this.canWriteArticle()) {
            throw new RateLimitException("글 작성이 불가능합니다.");
        }
        Optional<User> author = userRepository.findByUsername(userDetails.getUsername());
        Optional<Board> board = boardRepository.findById(boardId);
        if(author.isEmpty()) {
            throw new ResourceNotFoundException("article writing is restricted by rate limit");
        }
        if(board.isEmpty()) {
            throw new ResourceNotFoundException("board not found");
        }
        Article article = new Article();
        article.setBoard(board.get());
        article.setAuthor(author.get());
        article.setTitle(dto.getTitle());
        article.setContent(dto.getContent());
        articleRepository.save(article);
        return article;
    }

    public List<Article> firstGetArticle(Long boardId) {
        return articleRepository.findTop10ByBoard_IdOrderByCreatedDateDesc(boardId);
    }

    public List<Article> getOldArticle(Long boardId, Long articleId) {
        return articleRepository.findTop10ByBoard_IdAndIdLessThanOrderByCreatedDateDesc(boardId, articleId);
    }

    public List<Article> getNewArticle(Long boardId, Long articleId) {
        return articleRepository.findTop10ByBoard_IdAndIdGreaterThanOrderByCreatedDateDesc(boardId, articleId);
    }

    public Article editArticle(Long boardId, Long articleId, EditArticleDto dto) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        UserDetails userDetails = (UserDetails)authentication.getPrincipal();
        if (!this.canEditArticle(articleId)) {
            throw new RateLimitException("글 수정이 불가능합니다.");
        }
        Optional<User> author = userRepository.findByUsername(userDetails.getUsername());
        Optional<Board> board = boardRepository.findById(boardId);
        if(author.isEmpty()) {
            throw new ResourceNotFoundException("author not found");
        }
        if(board.isEmpty()) {
            throw new ResourceNotFoundException("board not found");
        }
        Optional<Article> article = articleRepository.findById(articleId);
        if(article.isEmpty()) {
            throw new ResourceNotFoundException("article not found");
        }
        if(dto.getTitle() != null) {
            article.get().setTitle(dto.getTitle());
        }
        if(dto.getContent() != null) {
            article.get().setContent(dto.getContent());
        }
        article.get().setUpdatedDate(LocalDateTime.now());

        articleRepository.save(article.get());
        return article.get();
    }

    public boolean canWriteArticle() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        UserDetails userDetails = (UserDetails)authentication.getPrincipal();
        Optional<Article> latestArticle = articleRepository.findFirstByAuthor_UsernameOrderByCreatedDateDesc(userDetails.getUsername());
        return latestArticle
                .map(article -> article.getCreatedDate().isBefore(LocalDateTime.now().minusMinutes(5)))
                .orElse(true); // 아예 글이 없으면 작성 가능
    }

    public boolean canEditArticle(Long articleId) {
        Optional<Article> article = articleRepository.findById(articleId);
        return article.map(a -> {
            // 수정한 적이 없다면 언제든지 수정 가능
            if (a.getUpdatedDate() == null) return true;

            // 수정한 시점 기준으로 10분이 지나야 다시 수정 가능
            return a.getUpdatedDate().isBefore(LocalDateTime.now().minusMinutes(10));
        }).orElse(false);
    }




}
