package com.back;

import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
public class Article {
    private long id;
    private String title;
    private String body;
    private boolean isBlind;
    private LocalDateTime createdDate;
    private LocalDateTime modifiedDate;
}
