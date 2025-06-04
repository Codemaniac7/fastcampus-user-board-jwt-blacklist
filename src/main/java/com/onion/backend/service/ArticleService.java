package com.onion.backend.service;

import com.onion.backend.dto.EditArticleDto;
import com.onion.backend.dto.WriteArticleDto;
import com.onion.backend.entity.Article;
import com.onion.backend.entity.Board;
import com.onion.backend.entity.User;
import com.onion.backend.exception.ForbiddenException;
import com.onion.backend.exception.RateLimitException;
import com.onion.backend.exception.ResourceNotFoundException;
import com.onion.backend.repository.ArticleRepository;
import com.onion.backend.repository.BoardRepository;
import com.onion.backend.repository.UserRepository;
import jakarta.transaction.Transactional;
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
        return articleRepository.findTop10ByBoard_IdAndIsDeletedFalseOrderByCreatedDateDesc(boardId);
    }

    public List<Article> getOldArticle(Long boardId, Long articleId) {
        return articleRepository.findTop10ByBoard_IdAndIdLessThanAndIsDeletedFalseOrderByCreatedDateDesc(boardId, articleId);
    }

    public List<Article> getNewArticle(Long boardId, Long articleId) {
        return articleRepository.findTop10ByBoard_IdAndIdGreaterThanAndIsDeletedFalseOrderByCreatedDateDesc(boardId, articleId);
    }

    public Article editArticle(Long boardId, Long articleId, EditArticleDto dto) {
        // 1. 인증된 사용자 정보 가져오기
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        // UserDetails 타입이 아닐 수 있는 경우를 대비하여 더 안전하게 캐스팅 또는 검사
        UserDetails userDetails = (UserDetails) authentication.getPrincipal();

        // 2. 권한 검사 (Rate Limit) - 중복 제거 및 메서드 초반에 한 번만 호출
        if (!this.canEditArticle(articleId)) {
            throw new RateLimitException("글 수정이 불가능합니다. (요청 횟수 제한 초과)");
        }

        // 3. Optional 사용 개선: orElseThrow 활용 및 변수 할당
        // .isEmpty() 체크 후 .get() 대신 .orElseThrow()로 간결하게 처리
        User author = userRepository.findByUsername(userDetails.getUsername())
                .orElseThrow(() -> new ResourceNotFoundException("인증된 사용자를 찾을 수 없습니다."));

        Board board = boardRepository.findById(boardId)
                .orElseThrow(() -> new ResourceNotFoundException("게시판을 찾을 수 없습니다."));

        Article article = articleRepository.findById(articleId)
                .orElseThrow(() -> new ResourceNotFoundException("게시글을 찾을 수 없습니다."));


        // 4. 게시글 작성자 일치 여부 확인
        if (!article.getAuthor().equals(author)) {
            throw new ForbiddenException("게시글 작성자만 수정할 수 있습니다.");
        }

        // 5. DTO에 따른 필드 업데이트
        if (dto.getTitle() != null) {
            article.setTitle(dto.getTitle());
        }
        if (dto.getContent() != null) {
            article.setContent(dto.getContent());
        }

        // 6. 업데이트 시간 설정 및 저장
        article.setUpdatedDate(LocalDateTime.now());
        articleRepository.save(article); // 마찬가지로 article.get() 대신 article 변수 사용

        return article;
    }

    public boolean canWriteArticle() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        UserDetails userDetails = (UserDetails)authentication.getPrincipal();
        Optional<Article> latestArticle = articleRepository.findFirstByAuthor_UsernameAndIsDeletedFalseOrderByCreatedDateDesc(userDetails.getUsername());
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

    @Transactional // 데이터 변경 작업이므로 트랜잭션 적용
    public boolean deleteArticle(Long boardId, Long articleId) {
        // 1. 현재 로그인한 사용자 정보 가져오기
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        // UserDetails 타입이 아닐 경우를 대비한 방어 로직 (필요 시 추가)
        if (!(authentication.getPrincipal() instanceof UserDetails)) {
            // 예를 들어, 익명 사용자거나 예상치 못한 타입일 경우 처리
            throw new ForbiddenException("인증된 사용자 정보가 유효하지 않습니다.");
        }
        UserDetails userDetails = (UserDetails) authentication.getPrincipal();

        // 2. 사용자(작성자) 엔티티 조회 (Optional.orElseThrow 사용)
        User currentUser = userRepository.findByUsername(userDetails.getUsername())
                .orElseThrow(() -> new ResourceNotFoundException("인증된 사용자를 찾을 수 없습니다."));

        // 3. 게시판 엔티티 조회 (Optional.orElseThrow 사용)
        // boardId가 유효한 게시판인지 확인 (선택 사항이지만, editArticle과 일관성을 위해 포함)
        Board board = boardRepository.findById(boardId)
                .orElseThrow(() -> new ResourceNotFoundException("게시판을 찾을 수 없습니다."));

        // 4. 삭제하려는 게시글 엔티티 조회 (Optional.orElseThrow 사용)
        Article article = articleRepository.findById(articleId)
                .orElseThrow(() -> new ResourceNotFoundException("삭제할 게시글을 찾을 수 없습니다."));

        // 5. 게시글의 게시판 일치 여부 확인 (논리적 유효성 검사)
        // URL의 boardId와 게시글의 실제 boardId가 일치하는지 확인
        if (!article.getBoard().getId().equals(boardId)) {
            throw new ForbiddenException("게시글이 해당 게시판에 속하지 않습니다.");
        }

        // 6. 게시글 작성자 일치 여부 확인 (권한 부여)
        // User 객체에 equals()와 hashCode()가 올바르게 오버라이드 되어 있어야 합니다.
        if (!article.getAuthor().equals(currentUser)) {
            throw new ForbiddenException("게시글 작성자만 삭제할 수 있습니다.");
        }

        // 7. 게시글 삭제
        article.setDeleted(true);
        articleRepository.save(article);

        // 8. 삭제 성공 반환
        return true;
    }
}
