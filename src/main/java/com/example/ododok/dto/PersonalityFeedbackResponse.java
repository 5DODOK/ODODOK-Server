package com.example.ododok.dto;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class PersonalityFeedbackResponse {

    private Boolean isRelevant;    // 질문과 답변의 연관성 (true/false)
    private String feedback;       // 상세 피드백
    private Integer pointsAwarded; // 지급된 포인트
}