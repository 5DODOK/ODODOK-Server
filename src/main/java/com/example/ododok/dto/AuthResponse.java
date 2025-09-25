package com.example.ododok.dto;

import com.example.ododok.entity.User;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class AuthResponse {
    private String accessToken;
    private String refreshToken;
    private String tokenType = "bearer";
    private UserInfo user;
    private String message;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class UserInfo {
        private Long userId;
        private String email;
        private String name;
        private String profileImageUrl;

        public static UserInfo from(User user) {
            return new UserInfo(user.getUserId(), user.getEmail(), user.getName(), user.getProfileImageUrl());
        }
    }

    public static AuthResponse success(String accessToken, String refreshToken, User user, String message) {
        return new AuthResponse(accessToken, refreshToken, "bearer", UserInfo.from(user), message);
    }
}