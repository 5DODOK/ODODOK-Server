package com.example.ododok.dto;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class TechnicalFeedbackResponse {

    private Integer logicScore;      // 논리성 점수 (0~5)
    private Integer accuracyScore;   // 정확성 점수 (0~5)
    private Integer clarityScore;    // 명확성 점수 (0~5)
    private String feedback;         // 상세 피드백
}