package com.example.ododok.service;

import com.example.ododok.dto.FeedbackRequest;
import com.example.ododok.dto.FeedbackResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class FeedbackService {

    private final GeminiService geminiService;

    public FeedbackResponse generateFeedback(FeedbackRequest request) {
        log.info("🧠 피드백 생성 요청 - 질문: {}, 답변: {}", request.getQuestion(), request.getUserAnswer());

        try {
            // ✅ Gemini API를 통해 피드백 생성
            String fullFeedback = geminiService.generateFeedback(
                    request.getQuestion(),
                    request.getUserAnswer()
            );

            log.info("📥 Gemini 전체 응답: {}", fullFeedback);

            // ✅ 메인 피드백 + 추가 팁 분리
            String mainFeedback = extractMainFeedback(fullFeedback);
            String additionalTip = geminiService.extractAdditionalTip(fullFeedback);

            log.info("✅ 피드백 생성 완료 - 메인: {}, 팁: {}", mainFeedback, additionalTip);

            return new FeedbackResponse(mainFeedback, additionalTip);

        } catch (Exception e) {
            log.error("❌ 피드백 생성 중 오류 발생", e);
            return createDefaultFeedback();
        }
    }

    /**
     * Gemini 응답에서 주요 문장만 추출
     */
    private String extractMainFeedback(String fullFeedback) {
        log.info("=== 메인 피드백 추출 시작 ===");

        if (fullFeedback == null || fullFeedback.trim().isEmpty()) {
            log.warn("fullFeedback이 비어 있음");
            return "답변해주셔서 감사합니다! 계속해서 학습하시면서 더 나은 답변을 만들어보세요.";
        }

        // ✅ 문장 단위 분리 (. ! ?)
        String[] sentences = fullFeedback.split("[.!?]");
        StringBuilder mainFeedback = new StringBuilder();

        int count = 0;
        for (String sentence : sentences) {
            String trimmed = sentence.trim();
            if (trimmed.isEmpty()) continue;
            if (count >= 2) break; // 최대 2문장만 사용
            if (!trimmed.contains("추가로") && !trimmed.contains("팁") && !trimmed.contains("참고")) {
                mainFeedback.append(trimmed).append(". ");
                count++;
            }
        }

        String result = mainFeedback.toString().trim();
        log.info("🎯 최종 추출된 메인 피드백: {}", result);

        return result.isEmpty()
                ? "좋은 답변이에요! 계속해서 이런 식으로 학습해보세요."
                : result;
    }

    /**
     * API 호출 실패 시 기본 피드백
     */
    private FeedbackResponse createDefaultFeedback() {
        return new FeedbackResponse(
                "답변해주셔서 감사합니다! 계속해서 학습하시면서 더 나은 답변을 만들어보세요.",
                "다양한 문제를 풀어보시며 실력을 향상시켜보세요!"
        );
    }
}
