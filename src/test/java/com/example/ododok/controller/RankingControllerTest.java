package com.example.ododok.controller;

import com.example.ododok.dto.RankingResponse;
import com.example.ododok.service.CustomOAuth2User;
import com.example.ododok.service.RankingService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(RankingController.class)
class RankingControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private RankingService rankingService;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    @DisplayName("랭킹 조회 - 성공")
    @WithMockUser
    void getRankings_Success() throws Exception {
        // given
        List<RankingResponse.UserRanking> rankings = List.of(
                new RankingResponse.UserRanking(1, "김철수", 5000),
                new RankingResponse.UserRanking(2, "이하은", 4500),
                new RankingResponse.UserRanking(3, "오주현", 4200)
        );

        RankingResponse.UserRanking currentUser = new RankingResponse.UserRanking(3, "오주현", 4200);
        RankingResponse response = new RankingResponse(rankings, currentUser);

        when(rankingService.getRankings(any())).thenReturn(response);

        // when & then
        mockMvc.perform(get("/rank"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.rankings").isArray())
                .andExpect(jsonPath("$.rankings[0].rank").value(1))
                .andExpect(jsonPath("$.rankings[0].username").value("김철수"))
                .andExpect(jsonPath("$.rankings[0].points").value(5000))
                .andExpect(jsonPath("$.rankings[1].rank").value(2))
                .andExpect(jsonPath("$.rankings[1].username").value("이하은"))
                .andExpect(jsonPath("$.rankings[1].points").value(4500))
                .andExpect(jsonPath("$.rankings[2].rank").value(3))
                .andExpect(jsonPath("$.rankings[2].username").value("오주현"))
                .andExpect(jsonPath("$.rankings[2].points").value(4200))
                .andExpect(jsonPath("$.currentUser.rank").value(3))
                .andExpect(jsonPath("$.currentUser.username").value("오주현"))
                .andExpect(jsonPath("$.currentUser.points").value(4200));
    }

    @Test
    @DisplayName("랭킹 조회 - 비로그인 사용자")
    @WithMockUser
    void getRankings_NotLoggedIn() throws Exception {
        // given
        List<RankingResponse.UserRanking> rankings = List.of(
                new RankingResponse.UserRanking(1, "김철수", 5000),
                new RankingResponse.UserRanking(2, "이하은", 4500)
        );

        RankingResponse response = new RankingResponse(rankings, null);

        when(rankingService.getRankings(null)).thenReturn(response);

        // when & then
        mockMvc.perform(get("/rank"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.rankings").isArray())
                .andExpect(jsonPath("$.rankings[0].rank").value(1))
                .andExpect(jsonPath("$.rankings[0].username").value("김철수"))
                .andExpect(jsonPath("$.currentUser").doesNotExist());
    }
}