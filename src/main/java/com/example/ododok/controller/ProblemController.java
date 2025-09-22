package com.example.ododok.controller;

import com.example.ododok.dto.ProblemSubmissionRequest;
import com.example.ododok.dto.ProblemSubmissionResponse;
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