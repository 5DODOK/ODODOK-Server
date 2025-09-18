package com.example.ododok.controller;

import com.example.ododok.dto.AuthResponse;
import com.example.ododok.service.GoogleOAuthService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/auth/oauth")
@RequiredArgsConstructor
public class AuthController {

    private final GoogleOAuthService googleOAuthService;

    @GetMapping("/google")
    public ResponseEntity<?> googleLogin() {
        try {
            String authUrl = googleOAuthService.getAuthorizationUrl();
            return ResponseEntity.status(HttpStatus.FOUND)
                    .header("Location", authUrl)
                    .build();
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("message", "소셜 로그인 처리 중 오류가 발생했습니다."));
        }
    }

    @GetMapping("/google/callback")
    public ResponseEntity<?> googleCallback(@RequestParam(required = false) String code,
                                          @RequestParam(required = false) String state) {
        try {
            if (code == null || code.isEmpty()) {
                return ResponseEntity.badRequest()
                        .body(Map.of("message", "인증 코드가 필요합니다."));
            }

            AuthResponse response = googleOAuthService.processCallback(code, state);
            return ResponseEntity.ok(response);

        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("message", "유효하지 않은 OAuth 사용자입니다."));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("message", "소셜 로그인 처리 중 오류가 발생했습니다."));
        }
    }
}