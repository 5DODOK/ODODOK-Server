package com.example.ododok.dto;

import jakarta.validation.constraints.Size;
import jakarta.validation.constraints.Min;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
public class QuestionUpdateRequest {

    @Size(max = 200, message = "제목은 최대 200자까지 허용됩니다.")
    private String title;

    @Size(max = 10000, message = "본문은 최대 10,000자까지 허용됩니다.")
    private String content;

    @Size(max = 10, message = "태그는 최대 10개까지 허용됩니다.")
    private List<@Size(max = 30, message = "각 태그는 최대 30자까지 허용됩니다.") String> tags;

    private String difficulty; // EASY, MEDIUM, HARD

    private String answer;

    @Min(value = 1, message = "카테고리 ID는 1 이상이어야 합니다.")
    private Long categoryId;

    private Boolean isPublic;

    private Integer year;

    @Size(max = 100, message = "회사명은 최대 100자까지 허용됩니다.")
    private String companyName;
}