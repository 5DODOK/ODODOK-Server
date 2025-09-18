package com.example.ododok.dto;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import java.time.LocalDateTime;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class QuestionResponse {
    private Long id;
    private String title;
    private String question;
    private String content;
    private List<String> tags;
    private String answer;
    private Integer difficulty;
    private Integer year;
    private Long companyId;
    private Long categoryId;
    private Boolean isPublic;
    private LocalDateTime createdAt;
    private Long createdBy;
    private LocalDateTime updatedAt;
    private Long updatedBy;
}