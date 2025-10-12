package com.example.ododok.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class ProblemSubmissionResponse {
    private String message;
    private Integer averageScore;        // 기술 면접 평균 점수 (논리성+정확성+명확성 평균)
    private Integer logicScore;          // 기술 면접 논리성 평균 점수
    private Integer accuracyScore;       // 기술 면접 정확성 평균 점수
    private Integer clarityScore;        // 기술 면접 명확성 평균 점수
    private Integer pointsEarned;        // 인성 면접 포인트
    private Integer rank;
    private String overallComment;       // 종합 코멘트
}