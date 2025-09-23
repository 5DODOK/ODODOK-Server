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

    private final HuggingFaceService huggingFaceService;

    public FeedbackResponse generateFeedback(FeedbackRequest request) {
        log.info("피드백 생성 요청 - 질문: {}, 답변: {}", request.getQuestion(), request.getUserAnswer());

        try {
            // Hugging Face AI를 통해 피드백 생성
            String fullFeedback = huggingFaceService.generateFeedback(
                request.getQuestion(),
                request.getUserAnswer()
            );

            // 피드백을 메인 피드백과 추가 팁으로 분리
            String mainFeedback = extractMainFeedback(fullFeedback);
            String additionalTip = huggingFaceService.extractAdditionalTip(fullFeedback);

            log.info("피드백 생성 완료 - 사용자: {}", request.getUserAnswer());

            return new FeedbackResponse(mainFeedback, additionalTip);

        } catch (Exception e) {
            log.error("피드백 생성 중 오류 발생", e);
            return createDefaultFeedback();
        }
    }

    private String extractMainFeedback(String fullFeedback) {
        // AI 응답에서 메인 피드백 부분을 추출
        if (fullFeedback == null || fullFeedback.trim().isEmpty()) {
            return "답변해주셔서 감사합니다! 계속해서 학습하시면서 더 나은 답변을 만들어보세요.";
        }

        // 첫 번째 문장들을 메인 피드백으로 사용
        String[] sentences = fullFeedback.split("\\.");
        StringBuilder mainFeedback = new StringBuilder();

        int sentenceCount = 0;
        for (String sentence : sentences) {
            if (sentenceCount >= 2) break; // 최대 2문장까지
            if (!sentence.trim().isEmpty() &&
                !sentence.contains("추가로") &&
                !sentence.contains("팁") &&
                !sentence.contains("알아두면")) {
                mainFeedback.append(sentence.trim()).append(". ");
                sentenceCount++;
            }
        }

        String result = mainFeedback.toString().trim();
        return result.isEmpty() ?
            "좋은 답변이에요! 계속해서 이런 식으로 학습해보세요." : result;
    }

    private FeedbackResponse createDefaultFeedback() {
        return new FeedbackResponse(
            "답변해주셔서 감사합니다! 계속해서 학습하시면서 더 나은 답변을 만들어보세요.",
            "다양한 문제를 풀어보시며 실력을 향상시켜보세요!"
        );
    }
}