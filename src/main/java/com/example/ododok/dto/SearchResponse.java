package com.example.ododok.dto;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class SearchResponse {
    private Map<String, Object> query;
    private int page;
    private int size;
    private long total;
    private List<SearchResult> results;
    private Map<String, Object> facets;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class SearchResult {
        private String type;
        private Long id;
        private String question;
        private Integer year;
        private String companyName;
        private Long categoryId;
        private String categoryName;
        private String interviewType;
        private Integer difficulty;
        private String difficultyLabel; // EASY, MEDIUM, HARD
        private LocalDateTime createdAt;
    }
}