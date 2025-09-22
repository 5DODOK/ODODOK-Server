package com.example.ododok.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class RankingResponse {

    private List<UserRanking> rankings;
    private UserRanking currentUser;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class UserRanking {
        private Integer rank;
        private String username;
        private Integer points;
    }
}