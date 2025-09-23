package com.example.ododok.controller;

import com.example.ododok.dto.PointResponse;
import com.example.ododok.service.JwtService;
import com.example.ododok.service.PointService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/point")
@RequiredArgsConstructor
@Slf4j
public class PointController {

    private final PointService pointService;
    private final JwtService jwtService;

    @GetMapping
    public ResponseEntity<PointResponse> getUserPoints(
            @RequestHeader("Authorization") String authHeader) {

        try {
            Long userId = extractUserIdFromToken(authHeader);
            PointResponse response = pointService.getUserPoints(userId);

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            log.error("Failed to fetch user points", e);
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