package com.example.ododok.service;

import com.example.ododok.dto.PointResponse;
import com.example.ododok.entity.User;
import com.example.ododok.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class PointService {

    private final UserRepository userRepository;

    public PointResponse getUserPoints(Long userId) {
        log.info("Fetching points for user ID: {}", userId);

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("사용자를 찾을 수 없습니다."));

        log.info("User {} has {} points", userId, user.getPoints());

        return new PointResponse(user.getPoints());
    }
}