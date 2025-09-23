package com.example.ododok.service;

import com.example.ododok.dto.FeedbackRequest;
import com.example.ododok.dto.FeedbackResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class FeedbackServiceTest {

    @Mock
    private HuggingFaceService huggingFaceService;

    @InjectMocks
    private FeedbackService feedbackService;

    private FeedbackRequest feedbackRequest;

    @BeforeEach
    void setUp() {
        feedbackRequest = new FeedbackRequest(
            "호이스팅은 변수와 함수 선언이 스코프 최상단으로 끌어올려지는 것입니다.",
            "JavaScript에서 호이스팅이란?"
        );
    }

    @Test
    @DisplayName("피드백 생성 - 성공")
    void generateFeedback_Success() {
        // given
        String mockAiFeedback = "좋은 답변이에요! 호이스팅에 대해 정확히 이해하고 계시네요. 추가로 var, let, const의 차이점도 알아두면 좋을 것 같아요!";
        when(huggingFaceService.generateFeedback(anyString(), anyString()))
                .thenReturn(mockAiFeedback);
        when(huggingFaceService.extractAdditionalTip(anyString()))
                .thenReturn("var, let, const의 차이점도 알아두면 좋을 것 같아요!");

        // when
        FeedbackResponse response = feedbackService.generateFeedback(feedbackRequest);

        // then
        assertThat(response).isNotNull();
        assertThat(response.getFeedback()).contains("좋은 답변");
        assertThat(response.getAdditionalTip()).contains("var, let, const");
    }

    @Test
    @DisplayName("피드백 생성 - Hugging Face 서비스 오류시 기본 피드백 반환")
    void generateFeedback_HuggingFaceError_ReturnsDefault() {
        // given
        when(huggingFaceService.generateFeedback(anyString(), anyString()))
                .thenThrow(new RuntimeException("API 오류"));

        // when
        FeedbackResponse response = feedbackService.generateFeedback(feedbackRequest);

        // then
        assertThat(response).isNotNull();
        assertThat(response.getFeedback()).contains("답변해주셔서 감사합니다");
        assertThat(response.getAdditionalTip()).contains("다양한 문제를 풀어보시며");
    }

    @Test
    @DisplayName("피드백 생성 - 빈 AI 응답시 기본 피드백 사용")
    void generateFeedback_EmptyAiResponse_UseDefault() {
        // given
        when(huggingFaceService.generateFeedback(anyString(), anyString()))
                .thenReturn("");
        when(huggingFaceService.extractAdditionalTip(anyString()))
                .thenReturn("계속해서 다양한 문제를 풀어보시며 실력을 향상시켜보세요!");

        // when
        FeedbackResponse response = feedbackService.generateFeedback(feedbackRequest);

        // then
        assertThat(response).isNotNull();
        assertThat(response.getFeedback()).contains("답변해주셔서 감사합니다");
        assertThat(response.getAdditionalTip()).isNotBlank();
    }

    @Test
    @DisplayName("피드백 생성 - 다양한 답변 길이 처리")
    void generateFeedback_VariousAnswerLengths() {
        // given
        FeedbackRequest shortAnswer = new FeedbackRequest("A", "JavaScript에서 호이스팅이란?");
        String mockFeedback = "간단한 답변이네요. 더 자세히 설명해보시면 좋을 것 같아요.";

        when(huggingFaceService.generateFeedback(anyString(), anyString()))
                .thenReturn(mockFeedback);
        when(huggingFaceService.extractAdditionalTip(anyString()))
                .thenReturn("구체적인 예시를 들어보시면 더 좋을 것 같아요!");

        // when
        FeedbackResponse response = feedbackService.generateFeedback(shortAnswer);

        // then
        assertThat(response).isNotNull();
        assertThat(response.getFeedback()).isNotBlank();
        assertThat(response.getAdditionalTip()).isNotBlank();
    }
}