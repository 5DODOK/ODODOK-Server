package com.example.ododok.controller;

import com.example.ododok.dto.AuthResponse;
import com.example.ododok.service.GoogleOAuthService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureMockMvc
class AuthControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private GoogleOAuthService googleOAuthService;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    @DisplayName("구글 콜백 - 성공")
    void googleCallback_Success() throws Exception {
        // given
        String code = "test-auth-code";
        String state = "test-state";

        AuthResponse.UserInfo userInfo = new AuthResponse.UserInfo(1L, "test@bssm.hs.kr", "테스트유저");
        AuthResponse authResponse = new AuthResponse("access-token", "refresh-token", "bearer", userInfo, "로그인 성공");

        when(googleOAuthService.processCallback(code, state)).thenReturn(authResponse);

        // when & then
        mockMvc.perform(get("/auth/oauth/google/callback")
                        .param("code", code)
                        .param("state", state))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken").value("access-token"))
                .andExpect(jsonPath("$.refreshToken").value("refresh-token"))
                .andExpect(jsonPath("$.tokenType").value("bearer"))
                .andExpect(jsonPath("$.user.userId").value(1))
                .andExpect(jsonPath("$.user.email").value("test@bssm.hs.kr"))
                .andExpect(jsonPath("$.user.name").value("테스트유저"))
                .andExpect(jsonPath("$.message").value("로그인 성공"));
    }

    @Test
    @DisplayName("구글 콜백 - code 누락시 400 에러")
    void googleCallback_MissingCode() throws Exception {
        // when & then
        mockMvc.perform(get("/auth/oauth/google/callback"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("인증 코드가 필요합니다."));
    }

    @Test
    @DisplayName("구글 콜백 - 빈 code시 400 에러")
    void googleCallback_EmptyCode() throws Exception {
        // when & then
        mockMvc.perform(get("/auth/oauth/google/callback")
                        .param("code", ""))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("인증 코드가 필요합니다."));
    }

    @Test
    @DisplayName("구글 콜백 - 유효하지 않은 OAuth 사용자시 401 에러")
    void googleCallback_InvalidOAuthUser() throws Exception {
        // given
        String code = "invalid-code";
        when(googleOAuthService.processCallback(anyString(), any()))
                .thenThrow(new IllegalArgumentException("Invalid OAuth user"));

        // when & then
        mockMvc.perform(get("/auth/oauth/google/callback")
                        .param("code", code))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.message").value("유효하지 않은 OAuth 사용자입니다."));
    }

    @Test
    @DisplayName("구글 콜백 - 서비스 예외 발생시 500 에러")
    void googleCallback_ServiceException() throws Exception {
        // given
        String code = "test-code";
        when(googleOAuthService.processCallback(anyString(), any()))
                .thenThrow(new RuntimeException("Service error"));

        // when & then
        mockMvc.perform(get("/auth/oauth/google/callback")
                        .param("code", code))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.message").value("소셜 로그인 처리 중 오류가 발생했습니다."));
    }
}