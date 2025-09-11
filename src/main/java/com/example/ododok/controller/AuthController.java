package com.example.ododok.controller;

import com.example.ododok.dto.AuthResponse;
import com.example.ododok.entity.User;
import com.example.ododok.service.CustomOAuth2User;
import com.example.ododok.service.JwtService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/auth/oauth")
@RequiredArgsConstructor
public class AuthController {

    private final JwtService jwtService;

    @GetMapping("/google/callback")
    public ResponseEntity<?> googleCallback(@AuthenticationPrincipal OAuth2User oauth2User,
                                          @RequestParam(required = false) String code) {
        try {
            if (code == null || code.isEmpty()) {
                return ResponseEntity.badRequest()
                        .body(Map.of("message", "인증 코드가 필요합니다."));
            }

            if (oauth2User == null) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                        .body(Map.of("message", "유효하지 않은 OAuth 사용자입니다."));
            }

            CustomOAuth2User customUser = (CustomOAuth2User) oauth2User;
            User user = customUser.getUser();

            String accessToken = jwtService.generateAccessToken(user);
            String refreshToken = jwtService.generateRefreshToken(user);

            return ResponseEntity.ok(
                    AuthResponse.success(accessToken, refreshToken, user, "로그인 성공")
            );

        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("message", "소셜 로그인 처리 중 오류가 발생했습니다."));
        }
    }
}