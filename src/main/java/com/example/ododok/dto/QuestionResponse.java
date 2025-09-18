package com.example.ododok.dto;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class QuestionResponse {
    private Long id;
    private String question;
    private Integer difficulty;
    private Integer year;
    private Long companyId;
    private Long categoryId;
    private LocalDateTime createdAt;
    private Long createdBy;
}