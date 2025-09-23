package com.example.ododok.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@Slf4j
public class HuggingFaceService {

    @Value("${huggingface.api.token}")
    private String apiToken;

    @Value("${huggingface.api.model}")
    private String model;

    private final RestTemplate restTemplate = new RestTemplate();

    public String generateFeedback(String question, String userAnswer) {
        try {
            String prompt = createFeedbackPrompt(question, userAnswer);

            // Hugging Face API 호출
            String url = "https://api-inference.huggingface.co/models/" + model;

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.setBearerAuth(apiToken);

            Map<String, Object> requestBody = new HashMap<>();
            requestBody.put("inputs", prompt);
            requestBody.put("parameters", Map.of(
                "max_length", 200,
                "temperature", 0.7,
                "do_sample", true
            ));

            HttpEntity<Map<String, Object>> request = new HttpEntity<>(requestBody, headers);

            ResponseEntity<List> response = restTemplate.postForEntity(url, request, List.class);

            if (response.getStatusCode() == HttpStatus.OK && response.getBody() != null) {
                List<Map<String, Object>> results = response.getBody();
                if (!results.isEmpty()) {
                    Map<String, Object> result = results.get(0);
                    return (String) result.get("generated_text");
                }
            }

            log.warn("Hugging Face API 호출 실패, 기본 응답 반환");
            return generateDefaultFeedback();

        } catch (Exception e) {
            log.error("Hugging Face API 호출 중 오류 발생", e);
            return generateDefaultFeedback();
        }
    }

    private String createFeedbackPrompt(String question, String userAnswer) {
        return String.format(
            "질문: %s\n\n사용자 답변: %s\n\n위 답변에 대해 건설적이고 친근한 피드백을 제공해주세요. " +
            "좋은 점을 먼저 언급하고, 개선할 점이나 추가로 알면 좋을 내용을 제안해주세요.",
            question, userAnswer
        );
    }

    private String generateDefaultFeedback() {
        return "답변해주셔서 감사합니다! 계속해서 학습하시면서 더 나은 답변을 만들어보세요.";
    }

    public String extractAdditionalTip(String fullFeedback) {
        // AI 응답에서 추가 팁 부분을 추출하는 로직
        if (fullFeedback.contains("추가로") || fullFeedback.contains("팁")) {
            String[] sentences = fullFeedback.split("\\.");
            for (String sentence : sentences) {
                if (sentence.contains("추가로") || sentence.contains("팁") || sentence.contains("알아두면")) {
                    return sentence.trim() + ".";
                }
            }
        }
        return "계속해서 다양한 문제를 풀어보시며 실력을 향상시켜보세요!";
    }
}