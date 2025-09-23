package com.example.ododok.controller;

import com.example.ododok.dto.FeedbackRequest;
import com.example.ododok.dto.FeedbackResponse;
import com.example.ododok.service.FeedbackService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/feedback")
@RequiredArgsConstructor
@Validated
@Slf4j
public class FeedbackController {

    private final FeedbackService feedbackService;

    @PostMapping
    public ResponseEntity<FeedbackResponse> provideFeedback(
            @Valid @RequestBody FeedbackRequest request) {

        try {
            log.info("피드백 요청 수신 - 질문: {}", request.getQuestion());

            FeedbackResponse response = feedbackService.generateFeedback(request);

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            log.error("피드백 제공 중 오류 발생", e);
            throw e;
        }
    }
}