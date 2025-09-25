package com.example.ododok.controller;

import com.example.ododok.dto.AuthResponse;
import com.example.ododok.service.GoogleOAuthService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import jakarta.servlet.http.HttpServletResponse;
import java.time.Duration;
import java.util.Map;

@RestController
@RequestMapping("/auth/oauth")
@RequiredArgsConstructor
public class AuthController {
    private final GoogleOAuthService googleOAuthService;

    // 1단계: Google 인증 URL 반환
    @GetMapping("/google")
    public ResponseEntity<Map<String, String>> getGoogleAuthUrl() {
        try {
            String authUrl = googleOAuthService.getAuthorizationUrl();
            return ResponseEntity.ok(Map.of("authUrl", authUrl));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("message", "인증 URL 생성 중 오류가 발생했습니다."));
        }
    }

    // 2단계: 프론트엔드에서 code 받아서 토큰 처리
    @PostMapping("/google/callback")
    public ResponseEntity<?> googleCallback(@RequestBody Map<String, String> request,
                                            HttpServletResponse response) {
        try {
            String code = request.get("code");
            String state = request.get("state");

            if (code == null || code.isEmpty()) {
                return ResponseEntity.badRequest()
                        .body(Map.of("message", "인증 코드가 필요합니다."));
            }

            AuthResponse authResponse = googleOAuthService.processCallback(code, state);

            // Access Token을 Authorization 헤더에 설정
            HttpHeaders headers = new HttpHeaders();
            headers.set("Authorization", "Bearer " + authResponse.getAccessToken());

            // Refresh Token을 HttpOnly 쿠키에 설정
            ResponseCookie refreshTokenCookie = ResponseCookie.from("refreshToken", authResponse.getRefreshToken())
                    .httpOnly(true)
                    .secure(false) // 개발환경에서는 false
                    .sameSite("Lax")
                    .maxAge(Duration.ofDays(7))
                    .path("/")
                    .build();

            response.addHeader("Set-Cookie", refreshTokenCookie.toString());

            // 사용자 정보만 JSON으로 반환
            Map<String, Object> responseBody = Map.of(
                    "user", authResponse.getUser(),
                    "message", "로그인 성공"
            );

            return ResponseEntity.ok()
                    .headers(headers)
                    .body(responseBody);

        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("message", "유효하지 않은 OAuth 사용자입니다."));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("message", "소셜 로그인 처리 중 오류가 발생했습니다."));
        }
    }
}