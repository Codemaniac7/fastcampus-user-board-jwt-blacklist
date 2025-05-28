package com.onion.backend.dto;


import com.onion.backend.entity.User;
import lombok.Getter;

@Getter
public class WriteArticleDto {
    Long boardId;
    String title;
    String content;
}
