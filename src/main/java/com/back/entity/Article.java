package com.back.entity;

import lombok.Getter;

import java.time.LocalDateTime;

@Getter
public class Article {
    Long id;
    String title;
    String body;
    LocalDateTime createdDate;
    LocalDateTime modifiedDate;
    boolean isBlind;
}
