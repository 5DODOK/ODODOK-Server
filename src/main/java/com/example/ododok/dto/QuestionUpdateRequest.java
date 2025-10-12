package com.example.ododok.dto;

import jakarta.validation.constraints.Size;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Pattern;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
public class QuestionUpdateRequest {

    @Size(max = 500, message = "질문은 최대 500자까지 허용됩니다.")
    private String question;

    @Pattern(regexp = "EASY|MEDIUM|HARD", message = "난이도는 EASY, MEDIUM, HARD 중 하나여야 합니다.")
    private String difficulty;

    @Min(value = 1, message = "카테고리 ID는 1 이상이어야 합니다.")
    private Long categoryId;

    private Integer year;

    @Size(max = 100, message = "회사명은 최대 100자까지 허용됩니다.")
    private String companyName;

    @Pattern(regexp = "기술면접|인성면접", message = "면접 타입은 '기술면접' 또는 '인성면접'이어야 합니다.")
    private String interviewType;
}