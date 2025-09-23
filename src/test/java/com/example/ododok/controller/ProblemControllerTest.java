package com.example.ododok.controller;

import com.example.ododok.dto.ProblemSubmissionRequest;
import com.example.ododok.dto.ProblemSubmissionResponse;
import com.example.ododok.dto.QuestionListResponse;
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
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
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

    @Test
    @DisplayName("문제 목록 조회 - 필터 없음")
    void getQuestions_NoFilters() throws Exception {
        // given
        QuestionListResponse.QuestionItem question1 = new QuestionListResponse.QuestionItem(1L, "JavaScript에서 호이스팅(Hoisting)이 무엇인지 설명하고, var, let, const의 호이스팅 차이점을 예시와 함께 설명해주세요.");
        QuestionListResponse.QuestionItem question2 = new QuestionListResponse.QuestionItem(2L, "React의 Virtual DOM이 무엇인지 설명하고, 실제 DOM과의 차이점 및 성능상 이점을 설명해주세요.");
        QuestionListResponse response = new QuestionListResponse(List.of(question1, question2));

        when(problemService.getQuestions(null, null)).thenReturn(response);

        // when & then
        mockMvc.perform(get("/problem"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.questions").isArray())
                .andExpect(jsonPath("$.questions.length()").value(2))
                .andExpect(jsonPath("$.questions[0].questionId").value(1))
                .andExpect(jsonPath("$.questions[0].question").value("JavaScript에서 호이스팅(Hoisting)이 무엇인지 설명하고, var, let, const의 호이스팅 차이점을 예시와 함께 설명해주세요."))
                .andExpect(jsonPath("$.questions[1].questionId").value(2))
                .andExpect(jsonPath("$.questions[1].question").value("React의 Virtual DOM이 무엇인지 설명하고, 실제 DOM과의 차이점 및 성능상 이점을 설명해주세요."));
    }

    @Test
    @DisplayName("문제 목록 조회 - 카테고리 필터 적용")
    void getQuestions_WithCategoryFilter() throws Exception {
        // given
        QuestionListResponse.QuestionItem question1 = new QuestionListResponse.QuestionItem(1L, "데이터베이스에서 트랜잭션(Transaction)이란 무엇이며, ACID 속성에 대해 설명해주세요.");
        QuestionListResponse response = new QuestionListResponse(List.of(question1));

        when(problemService.getQuestions(1L, null)).thenReturn(response);

        // when & then
        mockMvc.perform(get("/problem").param("category", "1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.questions").isArray())
                .andExpect(jsonPath("$.questions.length()").value(1))
                .andExpect(jsonPath("$.questions[0].questionId").value(1))
                .andExpect(jsonPath("$.questions[0].question").value("데이터베이스에서 트랜잭션(Transaction)이란 무엇이며, ACID 속성에 대해 설명해주세요."));
    }

    @Test
    @DisplayName("문제 목록 조회 - 회사 필터 적용")
    void getQuestions_WithCompanyFilter() throws Exception {
        // given
        QuestionListResponse.QuestionItem question1 = new QuestionListResponse.QuestionItem(2L, "RESTful API 설계 원칙에 대해 설명하고, GET과 POST의 차이점을 예시와 함께 설명해주세요.");
        QuestionListResponse response = new QuestionListResponse(List.of(question1));

        when(problemService.getQuestions(null, 2L)).thenReturn(response);

        // when & then
        mockMvc.perform(get("/problem").param("company", "2"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.questions").isArray())
                .andExpect(jsonPath("$.questions.length()").value(1))
                .andExpect(jsonPath("$.questions[0].questionId").value(2))
                .andExpect(jsonPath("$.questions[0].question").value("RESTful API 설계 원칙에 대해 설명하고, GET과 POST의 차이점을 예시와 함께 설명해주세요."));
    }

    @Test
    @DisplayName("문제 목록 조회 - 카테고리와 회사 필터 모두 적용")
    void getQuestions_WithBothFilters() throws Exception {
        // given
        QuestionListResponse.QuestionItem question1 = new QuestionListResponse.QuestionItem(3L, "객체지향 프로그래밍의 4가지 특징에 대해 설명해주세요.");
        QuestionListResponse response = new QuestionListResponse(List.of(question1));

        when(problemService.getQuestions(1L, 2L)).thenReturn(response);

        // when & then
        mockMvc.perform(get("/problem")
                        .param("category", "1")
                        .param("company", "2"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.questions").isArray())
                .andExpect(jsonPath("$.questions.length()").value(1))
                .andExpect(jsonPath("$.questions[0].questionId").value(3))
                .andExpect(jsonPath("$.questions[0].question").value("객체지향 프로그래밍의 4가지 특징에 대해 설명해주세요."));
    }

    @Test
    @DisplayName("문제 목록 조회 - 빈 결과")
    void getQuestions_EmptyResult() throws Exception {
        // given
        QuestionListResponse response = new QuestionListResponse(List.of());

        when(problemService.getQuestions(999L, 999L)).thenReturn(response);

        // when & then
        mockMvc.perform(get("/problem")
                        .param("category", "999")
                        .param("company", "999"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.questions").isArray())
                .andExpect(jsonPath("$.questions.length()").value(0));
    }
}