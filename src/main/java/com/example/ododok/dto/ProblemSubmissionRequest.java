package com.example.ododok.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.Data;

import java.util.List;

@Data
public class ProblemSubmissionRequest {

    @NotEmpty(message = "답안이 비어있을 수 없습니다")
    @Valid
    private List<Answer> answers;

    @NotNull(message = "총 소요시간이 필요합니다")
    @Positive(message = "총 소요시간은 양수여야 합니다")
    private Integer totalTimeSpent;

    @NotNull(message = "제출시간이 필요합니다")
    private String submittedAt;

    @Data
    public static class Answer {
        @NotNull(message = "문제 ID가 필요합니다")
        @Positive(message = "문제 ID는 양수여야 합니다")
        private Long questionId;

        @NotNull(message = "답안이 필요합니다")
        private String answer;

        @NotNull(message = "소요시간이 필요합니다")
        @Positive(message = "소요시간은 양수여야 합니다")
        private Integer timeSpent;
    }
}