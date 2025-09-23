package com.example.ododok.dto;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class QuestionListResponse {

    private List<QuestionItem> questions;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class QuestionItem {
        private Long questionId;
        private String question;
    }
}