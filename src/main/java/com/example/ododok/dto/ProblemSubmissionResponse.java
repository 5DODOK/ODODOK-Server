package com.example.ododok.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class ProblemSubmissionResponse {
    private String message;
    private Integer score;
    private Integer correctAnswers;
    private Integer pointsEarned;
    private Integer rank;
}