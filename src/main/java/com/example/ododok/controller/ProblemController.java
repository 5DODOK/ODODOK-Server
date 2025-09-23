package com.example.ododok.controller;

import com.example.ododok.dto.ProblemSubmissionRequest;
import com.example.ododok.dto.ProblemSubmissionResponse;
import com.example.ododok.dto.QuestionListResponse;
import com.example.ododok.service.JwtService;
import com.example.ododok.service.ProblemService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/problem")
@RequiredArgsConstructor
@Slf4j
public class ProblemController {

    private final ProblemService problemService;
    private final JwtService jwtService;

    @GetMapping
    public ResponseEntity<QuestionListResponse> getQuestions(
            @RequestParam(required = false) Long category,
            @RequestParam(required = false) Long company) {

        try {
            QuestionListResponse response = problemService.getQuestions(category, company);
            return ResponseEntity.ok(response);

        } catch (Exception e) {
            log.error("Failed to fetch questions", e);
            throw e;
        }
    }

    @PostMapping
    public ResponseEntity<ProblemSubmissionResponse> submitProblem(
            @Valid @RequestBody ProblemSubmissionRequest request,
            @RequestHeader("Authorization") String authHeader) {

        try {
            Long userId = extractUserIdFromToken(authHeader);
            ProblemSubmissionResponse response = problemService.submitProblem(request, userId);

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            log.error("Problem submission failed", e);
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