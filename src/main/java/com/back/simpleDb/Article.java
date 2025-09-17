package com.back.simpleDb;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class Article {
    private long id;
    private LocalDateTime createdDate;
    private LocalDateTime modifiedDate;
    private String title;
    private String body;

    @JsonProperty("isBlind") // Jackson이 'isBlind'라는 key를 이 필드에 매핑하도록 명시
    private boolean isBlind;
}