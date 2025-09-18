package com.example.ododok.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import jakarta.validation.constraints.Min;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class QuestionCreateRequest {

    @NotBlank(message = "질문은 필수입니다.")
    @Size(max = 200, message = "질문은 최대 200자까지 허용됩니다.")
    private String question;

    private String difficulty = "MEDIUM"; // 기본값 MEDIUM

    @Min(value = 1900, message = "연도는 1900년 이후여야 합니다.")
    private Integer year;

    private Long companyId;

    private Long categoryId;
}