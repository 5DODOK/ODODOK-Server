package com.example.ododok.controller;

import com.example.ododok.dto.RankingResponse;
import com.example.ododok.service.CustomOAuth2User;
import com.example.ododok.service.RankingService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/rank")
@RequiredArgsConstructor
public class RankingController {

    private final RankingService rankingService;

    @GetMapping
    public ResponseEntity<RankingResponse> getRankings(@AuthenticationPrincipal CustomOAuth2User user) {
        String currentUserEmail = user != null ? user.getEmail() : null;
        RankingResponse response = rankingService.getRankings(currentUserEmail);
        return ResponseEntity.ok(response);
    }
}