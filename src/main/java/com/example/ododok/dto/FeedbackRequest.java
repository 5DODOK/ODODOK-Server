package com.example.ododok.dto;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import jakarta.validation.constraints.NotBlank;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class FeedbackRequest {

    @NotBlank(message = "사용자 답변은 필수입니다.")
    private String userAnswer;

    @NotBlank(message = "질문은 필수입니다.")
    private String question;
}