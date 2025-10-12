package com.example.ododok.service;

import com.example.ododok.dto.PersonalityFeedbackResponse;
import com.example.ododok.dto.TechnicalFeedbackResponse;
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

    /**
     * 기술 면접 답변에 대한 피드백 생성 (논리성, 정확성, 명확성 각 0~5점)
     */
    public TechnicalFeedbackResponse generateTechnicalFeedback(String question, String answer) {
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

                        기술 면접 평가 기준:
                        - 논리성 (0~5점)
                        - 정확성 (0~5점)
                        - 명확성 (0~5점)

                        JSON만 반환. 피드백은 1문장:
                        {"logicScore":점수,"accuracyScore":점수,"clarityScore":점수,"feedback":"1문장"}
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
            String responseText = json
                    .getJSONArray("candidates")
                    .getJSONObject(0)
                    .getJSONObject("content")
                    .getJSONArray("parts")
                    .getJSONObject(0)
                    .getString("text")
                    .trim();

            // JSON 파싱
            JSONObject result = new JSONObject(responseText);
            return new TechnicalFeedbackResponse(
                    result.getInt("logicScore"),
                    result.getInt("accuracyScore"),
                    result.getInt("clarityScore"),
                    result.getString("feedback")
            );

        } catch (Exception e) {
            log.error("기술 면접 피드백 생성 중 오류 발생", e);
            return new TechnicalFeedbackResponse(0, 0, 0, "피드백 생성 실패: " + e.getMessage());
        }
    }

    /**
     * 인성 면접 답변에 대한 피드백 생성 (연관성 분류)
     */
    public PersonalityFeedbackResponse generatePersonalityFeedback(String question, String answer) {
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

                        질문-답변 연관성 판단. 연관 있으면 10점, 없으면 0점.
                        JSON만 반환:
                        {"isRelevant":true/false,"pointsAwarded":10또는0,"feedback":"1문장"}
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
            String responseText = json
                    .getJSONArray("candidates")
                    .getJSONObject(0)
                    .getJSONObject("content")
                    .getJSONArray("parts")
                    .getJSONObject(0)
                    .getString("text")
                    .trim();

            // JSON 파싱
            JSONObject result = new JSONObject(responseText);
            return new PersonalityFeedbackResponse(
                    result.getBoolean("isRelevant"),
                    result.getString("feedback"),
                    result.getInt("pointsAwarded")
            );

        } catch (Exception e) {
            log.error("인성 면접 피드백 생성 중 오류 발생", e);
            return new PersonalityFeedbackResponse(false, "피드백 생성 실패: " + e.getMessage(), 0);
        }
    }

    /**
     * 모든 답변 완료 후 종합 코멘트 생성
     */
    public String generateOverallComment(String interviewType, String allAnswersSummary) {
        RestTemplate restTemplate = new RestTemplate();

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.set("x-goog-api-key", apiKey);

        JSONObject body = new JSONObject();
        JSONArray contents = new JSONArray();
        JSONArray parts = new JSONArray();

        parts.put(new JSONObject().put("text",
                String.format("""
                        %s 면접 종합 평가.
                        %s

                        강점/개선점/학습방향을 2문장으로 간결하게.
                        """, interviewType, allAnswersSummary)
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
            log.error("종합 코멘트 생성 중 오류 발생", e);
            return "종합 코멘트 생성 실패: " + e.getMessage();
        }
    }
}
