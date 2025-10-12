package com.example.ododok.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Pattern;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class QuestionCreateRequest {

    @NotBlank(message = "질문은 필수입니다.")
    @Size(max = 500, message = "질문은 최대 500자까지 허용됩니다.")
    private String question;

    @Pattern(regexp = "EASY|MEDIUM|HARD", message = "난이도는 EASY, MEDIUM, HARD 중 하나여야 합니다.")
    private String difficulty = "MEDIUM";

    @Min(value = 2000, message = "연도는 2000년 이후여야 합니다.")
    private Integer year;

    @Size(max = 100, message = "회사명은 최대 100자까지 허용됩니다.")
    private String companyName;

    private Long categoryId;

    @NotBlank(message = "면접 타입은 필수입니다.")
    @Pattern(regexp = "기술면접|인성면접", message = "면접 타입은 '기술면접' 또는 '인성면접'이어야 합니다.")
    private String interviewType;
}