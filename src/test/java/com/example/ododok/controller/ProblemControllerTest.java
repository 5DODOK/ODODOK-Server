package com.example.ododok.controller;

import com.example.ododok.dto.ProblemSubmissionRequest;
import com.example.ododok.dto.ProblemSubmissionResponse;
import com.example.ododok.service.JwtService;
import com.example.ododok.service.ProblemService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
class ProblemControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private ProblemService problemService;

    @MockitoBean
    private JwtService jwtService;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    @DisplayName("문제 제출 - 성공")
    void submitProblem_Success() throws Exception {
        // given
        ProblemSubmissionRequest.Answer answer1 = new ProblemSubmissionRequest.Answer();
        answer1.setQuestionId(1L);
        answer1.setAnswer("A");
        answer1.setTimeSpent(120);

        ProblemSubmissionRequest.Answer answer2 = new ProblemSubmissionRequest.Answer();
        answer2.setQuestionId(2L);
        answer2.setAnswer("B");
        answer2.setTimeSpent(180);

        ProblemSubmissionRequest request = new ProblemSubmissionRequest();
        request.setAnswers(List.of(answer1, answer2));
        request.setTotalTimeSpent(300);
        request.setSubmittedAt("2025-09-23T10:30:00Z");

        ProblemSubmissionResponse response = new ProblemSubmissionResponse(
                "제출 완료!",
                50,
                1,
                100,
                15
        );

        when(jwtService.extractUserId("valid-token")).thenReturn(1L);
        when(problemService.submitProblem(any(ProblemSubmissionRequest.class), anyLong()))
                .thenReturn(response);

        // when & then
        mockMvc.perform(post("/problem")
                        .header("Authorization", "Bearer valid-token")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("문제 제출 - Authorization 헤더 누락")
    void submitProblem_MissingAuthHeader() throws Exception {
        // given
        ProblemSubmissionRequest request = new ProblemSubmissionRequest();
        request.setAnswers(List.of());
        request.setTotalTimeSpent(300);
        request.setSubmittedAt("2025-09-23T10:30:00Z");

        // when & then
        mockMvc.perform(post("/problem")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("문제 제출 - 잘못된 Authorization 헤더")
    void submitProblem_InvalidAuthHeader() throws Exception {
        // given
        ProblemSubmissionRequest request = new ProblemSubmissionRequest();
        request.setAnswers(List.of());
        request.setTotalTimeSpent(300);
        request.setSubmittedAt("2025-09-23T10:30:00Z");

        // when & then
        mockMvc.perform(post("/problem")
                        .header("Authorization", "Invalid token")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("문제 제출 - 유효성 검증 실패 (빈 답안 목록)")
    void submitProblem_EmptyAnswers() throws Exception {
        // given
        ProblemSubmissionRequest request = new ProblemSubmissionRequest();
        request.setAnswers(List.of());
        request.setTotalTimeSpent(300);
        request.setSubmittedAt("2025-09-23T10:30:00Z");

        when(jwtService.extractUserId("valid-token")).thenReturn(1L);

        // when & then
        mockMvc.perform(post("/problem")
                        .header("Authorization", "Bearer valid-token")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("문제 제출 - 유효성 검증 실패 (음수 총 소요시간)")
    void submitProblem_NegativeTotalTime() throws Exception {
        // given
        ProblemSubmissionRequest.Answer answer = new ProblemSubmissionRequest.Answer();
        answer.setQuestionId(1L);
        answer.setAnswer("A");
        answer.setTimeSpent(120);

        ProblemSubmissionRequest request = new ProblemSubmissionRequest();
        request.setAnswers(List.of(answer));
        request.setTotalTimeSpent(-300);
        request.setSubmittedAt("2025-09-23T10:30:00Z");

        when(jwtService.extractUserId("valid-token")).thenReturn(1L);

        // when & then
        mockMvc.perform(post("/problem")
                        .header("Authorization", "Bearer valid-token")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("문제 제출 - 서비스 예외 발생")
    void submitProblem_ServiceException() throws Exception {
        // given
        ProblemSubmissionRequest.Answer answer = new ProblemSubmissionRequest.Answer();
        answer.setQuestionId(1L);
        answer.setAnswer("A");
        answer.setTimeSpent(120);

        ProblemSubmissionRequest request = new ProblemSubmissionRequest();
        request.setAnswers(List.of(answer));
        request.setTotalTimeSpent(300);
        request.setSubmittedAt("2025-09-23T10:30:00Z");

        when(jwtService.extractUserId("valid-token")).thenReturn(1L);
        when(problemService.submitProblem(any(ProblemSubmissionRequest.class), anyLong()))
                .thenThrow(new RuntimeException("사용자를 찾을 수 없습니다."));

        // when & then
        mockMvc.perform(post("/problem")
                        .header("Authorization", "Bearer valid-token")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden());
    }
}