package com.example.ododok.service;

import com.example.ododok.dto.AuthResponse;
import com.example.ododok.entity.User;
import com.example.ododok.entity.UserRole;
import com.example.ododok.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class GoogleOAuthService {

    private final UserRepository userRepository;
    private final JwtService jwtService;
    private final RestTemplate restTemplate = new RestTemplate();

    @Value("${spring.security.oauth2.client.registration.google.client-id}")
    private String clientId;

    @Value("${spring.security.oauth2.client.registration.google.client-secret}")
    private String clientSecret;

    @Value("${spring.security.oauth2.client.registration.google.redirect-uri:http://localhost:3000/oauth/callback}")
    private String redirectUri;

    private static final String GOOGLE_AUTH_URL = "https://accounts.google.com/o/oauth2/v2/auth";
    private static final String GOOGLE_TOKEN_URL = "https://oauth2.googleapis.com/token";
    private static final String GOOGLE_USER_INFO_URL = "https://www.googleapis.com/oauth2/v2/userinfo";

    public String getAuthorizationUrl() {
        String state = UUID.randomUUID().toString();
        String scope = "profile email";

        return GOOGLE_AUTH_URL +
                "?client_id=" + URLEncoder.encode(clientId, StandardCharsets.UTF_8) +
                "&redirect_uri=" + URLEncoder.encode(redirectUri, StandardCharsets.UTF_8) +
                "&scope=" + URLEncoder.encode(scope, StandardCharsets.UTF_8) +
                "&response_type=code" +
                "&state=" + state +
                "&access_type=offline" +
                "&prompt=consent";
    }

    public AuthResponse processCallback(String code, String state) {
        // 1. Exchange code for access token
        String accessToken = getAccessToken(code);

        // 2. Get user info from Google
        Map<String, Object> userInfo = getUserInfo(accessToken);

        // 3. Process user (create or update)
        User user = processUser(userInfo);

        // 4. Generate JWT tokens
        String jwtAccessToken = jwtService.generateAccessToken(user);
        String refreshToken = jwtService.generateRefreshToken(user);

        return AuthResponse.success(jwtAccessToken, refreshToken, user, "로그인 성공");
    }

    private String getAccessToken(String code) {
        HttpHeaders headers = new HttpHeaders();
        headers.set("Content-Type", "application/x-www-form-urlencoded");

        String body = "client_id=" + URLEncoder.encode(clientId, StandardCharsets.UTF_8) +
                "&client_secret=" + URLEncoder.encode(clientSecret, StandardCharsets.UTF_8) +
                "&code=" + URLEncoder.encode(code, StandardCharsets.UTF_8) +
                "&grant_type=authorization_code" +
                "&redirect_uri=" + URLEncoder.encode(redirectUri, StandardCharsets.UTF_8);

        HttpEntity<String> request = new HttpEntity<>(body, headers);
        ResponseEntity<Map> response = restTemplate.postForEntity(GOOGLE_TOKEN_URL, request, Map.class);

        Map<String, Object> responseBody = response.getBody();
        if (responseBody == null || !responseBody.containsKey("access_token")) {
            throw new IllegalArgumentException("Failed to get access token from Google");
        }

        return (String) responseBody.get("access_token");
    }

    private Map<String, Object> getUserInfo(String accessToken) {
        HttpHeaders headers = new HttpHeaders();
        headers.set("Authorization", "Bearer " + accessToken);

        HttpEntity<String> request = new HttpEntity<>(headers);
        ResponseEntity<Map> response = restTemplate.exchange(
                GOOGLE_USER_INFO_URL, HttpMethod.GET, request, Map.class);

        Map<String, Object> userInfo = response.getBody();
        if (userInfo == null) {
            throw new IllegalArgumentException("Failed to get user info from Google");
        }

        return userInfo;
    }

    private User processUser(Map<String, Object> userInfo) {
        String oauthId = (String) userInfo.get("id");
        String email = (String) userInfo.get("email");
        String name = (String) userInfo.get("name");
        String profileImageUrl = (String) userInfo.get("picture");
        String oauthProvider = "google";

        User existingUser = userRepository.findByOauthProviderAndOauthId(oauthProvider, oauthId)
                .orElse(null);

        if (existingUser != null) {
            existingUser.setName(name);
            existingUser.setProfileImageUrl(profileImageUrl);
            existingUser.setUpdatedAt(LocalDateTime.now());
            return userRepository.save(existingUser);
        } else {
            User newUser = new User();
            newUser.setEmail(email);
            newUser.setName(name);
            newUser.setProfileImageUrl(profileImageUrl);
            newUser.setOauthProvider(oauthProvider);
            newUser.setOauthId(oauthId);
            newUser.setRole(determineUserRole(email));
            newUser.setCreatedAt(LocalDateTime.now());
            newUser.setUpdatedAt(LocalDateTime.now());

            return userRepository.save(newUser);
        }
    }

    private UserRole determineUserRole(String email) {
        return email.endsWith("@bssm.hs.kr") ? UserRole.ADMIN : UserRole.USER;
    }
}