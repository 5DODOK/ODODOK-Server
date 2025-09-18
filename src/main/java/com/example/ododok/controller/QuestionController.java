package com.example.ododok.controller;

import com.example.ododok.dto.QuestionCreateRequest;
import com.example.ododok.dto.QuestionResponse;
import com.example.ododok.service.JwtService;
import com.example.ododok.service.QuestionService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.net.URI;

@RestController
@RequestMapping("/question")
@RequiredArgsConstructor
@Slf4j
public class QuestionController {

    private final QuestionService questionService;
    private final JwtService jwtService;

    @PostMapping
    public ResponseEntity<QuestionResponse> createQuestion(
            @Valid @RequestBody QuestionCreateRequest request,
            @RequestHeader("Authorization") String authHeader) {

        try {
            Long userId = extractUserIdFromToken(authHeader);
            QuestionResponse response = questionService.createQuestion(request, userId);

            // Location 헤더 설정
            URI location = URI.create("/question/" + response.getId());

            return ResponseEntity.created(location).body(response);

        } catch (Exception e) {
            log.error("Question creation failed", e);
            throw e;
        }
    }

    private Long extractUserIdFromToken(String authHeader) {
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            throw new RuntimeException("Authorization 헤더가 필요합니다.");
        }

        String token = authHeader.substring(7);
        return jwtService.extractUserId(token);
    }
}