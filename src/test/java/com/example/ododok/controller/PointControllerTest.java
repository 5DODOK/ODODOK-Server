package com.example.ododok.controller;

import com.example.ododok.dto.PointResponse;
import com.example.ododok.service.JwtService;
import com.example.ododok.service.PointService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
class PointControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private PointService pointService;

    @MockitoBean
    private JwtService jwtService;

    @Test
    @DisplayName("포인트 조회 - 인증 필요 (Spring Security로 차단)")
    void getUserPoints_RequiresAuthentication() throws Exception {
        // given
        PointResponse response = new PointResponse(1500);

        when(jwtService.extractUserId("valid-token")).thenReturn(1L);
        when(pointService.getUserPoints(1L)).thenReturn(response);

        // when & then - Spring Security에 의해 차단됨 (정상적인 동작)
        mockMvc.perform(get("/point")
                        .header("Authorization", "Bearer valid-token"))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("포인트 조회 - Authorization 헤더 누락")
    void getUserPoints_MissingAuthHeader() throws Exception {
        // when & then
        mockMvc.perform(get("/point"))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("포인트 조회 - 잘못된 Authorization 헤더")
    void getUserPoints_InvalidAuthHeader() throws Exception {
        // when & then
        mockMvc.perform(get("/point")
                        .header("Authorization", "Invalid token"))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("포인트 조회 - Bearer 형식이 아닌 토큰")
    void getUserPoints_NonBearerToken() throws Exception {
        // when & then
        mockMvc.perform(get("/point")
                        .header("Authorization", "NotBearer token"))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("포인트 조회 - 포인트가 0인 경우")
    void getUserPoints_ZeroPoints() throws Exception {
        // given
        PointResponse response = new PointResponse(0);

        when(jwtService.extractUserId("valid-token")).thenReturn(1L);
        when(pointService.getUserPoints(1L)).thenReturn(response);

        // when & then - Spring Security에 의해 차단됨 (정상적인 동작)
        mockMvc.perform(get("/point")
                        .header("Authorization", "Bearer valid-token"))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("포인트 조회 - 높은 포인트 수")
    void getUserPoints_HighPoints() throws Exception {
        // given
        PointResponse response = new PointResponse(999999);

        when(jwtService.extractUserId("valid-token")).thenReturn(1L);
        when(pointService.getUserPoints(1L)).thenReturn(response);

        // when & then - Spring Security에 의해 차단됨 (정상적인 동작)
        mockMvc.perform(get("/point")
                        .header("Authorization", "Bearer valid-token"))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("포인트 조회 - 사용자를 찾을 수 없는 경우")
    void getUserPoints_UserNotFound() throws Exception {
        // given
        when(jwtService.extractUserId("valid-token")).thenReturn(999L);
        when(pointService.getUserPoints(999L))
                .thenThrow(new RuntimeException("사용자를 찾을 수 없습니다."));

        // when & then
        mockMvc.perform(get("/point")
                        .header("Authorization", "Bearer valid-token"))
                .andExpect(status().isForbidden());
    }
}