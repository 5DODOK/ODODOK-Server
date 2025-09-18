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
    private String query;
    private int page;
    private int size;
    private long total;
    private long tookMs;
    private Map<String, Map<String, Integer>> facets;
    private List<SearchResult> results;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class SearchResult {
        private String type;
        private Long id;
        private String question;
        private String snippet;
        private Integer difficulty;
        private Integer year;
        private Long companyId;
        private Long categoryId;
        private Double score;
        private LocalDateTime createdAt;

        // User 타입 결과를 위한 추가 필드
        private String username;
        private String email;
        private String displayName;
    }
}