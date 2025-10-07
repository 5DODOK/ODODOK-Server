package com.example.ododok.service;

import lombok.extern.slf4j.Slf4j;
import org.json.JSONArray;
import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

@Slf4j
@Service
public class GeminiService {

    @Value("${gemini.api.key}")
    private String apiKey;

    private static final String GEMINI_URL =
            "https://generativelanguage.googleapis.com/v1beta/models/gemini-2.5-flash:generateContent";

    public String generateFeedback(String question, String answer) {
        RestTemplate restTemplate = new RestTemplate();

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.set("x-goog-api-key", apiKey);

        JSONObject body = new JSONObject();
        JSONArray contents = new JSONArray();
        JSONArray parts = new JSONArray();

        parts.put(new JSONObject().put("text",
                String.format("""
                        질문: %s
                        답변: %s
                        이 답변에 대해 2~3문장으로 간결하고 구체적인 피드백을 작성해주세요.
                        좋은 점과 개선할 점을 함께 제시하세요.
                        """, question, answer)
        ));

        contents.put(new JSONObject().put("parts", parts));
        body.put("contents", contents);

        HttpEntity<String> entity = new HttpEntity<>(body.toString(), headers);

        try {
            ResponseEntity<String> response = restTemplate.exchange(
                    GEMINI_URL,
                    HttpMethod.POST,
                    entity,
                    String.class
            );

            JSONObject json = new JSONObject(response.getBody());
            return json
                    .getJSONArray("candidates")
                    .getJSONObject(0)
                    .getJSONObject("content")
                    .getJSONArray("parts")
                    .getJSONObject(0)
                    .getString("text")
                    .trim();

        } catch (Exception e) {
            log.error("Gemini API 호출 중 오류 발생", e);
            return "Gemini 피드백 생성 실패: " + e.getMessage();
        }
    }

    public String extractAdditionalTip(String feedback) {
        if (feedback == null) return "꾸준히 다양한 질문을 연습해보세요!";
        if (feedback.contains("제안") || feedback.contains("보완")) {
            return feedback.substring(feedback.indexOf("제안"));
        }
        return "추가로, 답변에 구체적인 예시를 덧붙이면 더 좋습니다.";
    }
}
