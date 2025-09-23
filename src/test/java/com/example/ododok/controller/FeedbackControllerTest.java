package com.example.ododok.controller;

import com.example.ododok.dto.FeedbackRequest;
import com.example.ododok.dto.FeedbackResponse;
import com.example.ododok.service.FeedbackService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.http.MediaType;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(FeedbackController.class)
class FeedbackControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private FeedbackService feedbackService;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    @DisplayName("피드백 제공 - 성공")
    void provideFeedback_Success() throws Exception {
        // given
        FeedbackRequest request = new FeedbackRequest(
            "호이스팅은 변수와 함수 선언이 스코프 최상단으로 끌어올려지는 것입니다.",
            "JavaScript에서 호이스팅이란?"
        );

        FeedbackResponse expectedResponse = new FeedbackResponse(
            "좋은 답변이에요! 호이스팅에 대해 정확히 이해하고 계시네요.",
            "var, let, const의 호이스팅 차이점도 알아두면 더 좋을 것 같아요!"
        );

        when(feedbackService.generateFeedback(any(FeedbackRequest.class)))
                .thenReturn(expectedResponse);

        // when & then
        mockMvc.perform(post("/feedback")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.feedback").value("좋은 답변이에요! 호이스팅에 대해 정확히 이해하고 계시네요."))
                .andExpect(jsonPath("$.additionalTip").value("var, let, const의 호이스팅 차이점도 알아두면 더 좋을 것 같아요!"));
    }

    @Test
    @DisplayName("피드백 제공 - 잘못된 요청 (빈 답변)")
    void provideFeedback_InvalidRequest_EmptyAnswer() throws Exception {
        // given
        FeedbackRequest request = new FeedbackRequest(
            "",  // 빈 답변
            "JavaScript에서 호이스팅이란?"
        );

        // when & then
        mockMvc.perform(post("/feedback")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("피드백 제공 - 잘못된 요청 (빈 질문)")
    void provideFeedback_InvalidRequest_EmptyQuestion() throws Exception {
        // given
        FeedbackRequest request = new FeedbackRequest(
            "호이스팅은 변수와 함수 선언이 스코프 최상단으로 끌어올려지는 것입니다.",
            ""  // 빈 질문
        );

        // when & then
        mockMvc.perform(post("/feedback")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("피드백 제공 - JSON 형식 오류")
    void provideFeedback_InvalidJson() throws Exception {
        // when & then
        mockMvc.perform(post("/feedback")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{invalid json}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("피드백 제공 - 짧은 답변")
    void provideFeedback_ShortAnswer() throws Exception {
        // given
        FeedbackRequest request = new FeedbackRequest(
            "A",
            "JavaScript에서 호이스팅이란?"
        );

        FeedbackResponse expectedResponse = new FeedbackResponse(
            "간단한 답변이네요! 더 자세히 설명해보시면 좋을 것 같아요.",
            "구체적인 예시를 들어보시면 더 도움이 될 것 같아요!"
        );

        when(feedbackService.generateFeedback(any(FeedbackRequest.class)))
                .thenReturn(expectedResponse);

        // when & then
        mockMvc.perform(post("/feedback")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.feedback").exists())
                .andExpect(jsonPath("$.additionalTip").exists());
    }

    @Test
    @DisplayName("피드백 제공 - 긴 답변")
    void provideFeedback_LongAnswer() throws Exception {
        // given
        FeedbackRequest request = new FeedbackRequest(
            "JavaScript에서 호이스팅(Hoisting)은 변수와 함수 선언이 해당 스코프의 최상단으로 " +
            "끌어올려지는 것처럼 동작하는 JavaScript의 특성입니다. 실제로는 코드가 이동하는 것이 아니라, " +
            "JavaScript 엔진이 코드를 실행하기 전에 변수와 함수 선언을 메모리에 저장하기 때문에 발생합니다.",
            "JavaScript에서 호이스팅이란?"
        );

        FeedbackResponse expectedResponse = new FeedbackResponse(
            "훌륭한 답변이에요! 호이스팅의 개념과 원리를 정확히 설명하셨네요.",
            "var, let, const의 호이스팅 차이점과 TDZ(Temporal Dead Zone)에 대해서도 알아보시면 더 좋을 것 같아요!"
        );

        when(feedbackService.generateFeedback(any(FeedbackRequest.class)))
                .thenReturn(expectedResponse);

        // when & then
        mockMvc.perform(post("/feedback")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.feedback").value("훌륭한 답변이에요! 호이스팅의 개념과 원리를 정확히 설명하셨네요."))
                .andExpect(jsonPath("$.additionalTip").exists());
    }
}