package com.example.ododok.service;

import com.example.ododok.dto.PointResponse;
import com.example.ododok.entity.User;
import com.example.ododok.entity.UserRole;
import com.example.ododok.repository.UserRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PointServiceTest {

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private PointService pointService;

    @Test
    @DisplayName("포인트 조회 - 성공")
    void getUserPoints_Success() {
        // given
        Long userId = 1L;
        User user = createUser(userId, "test@example.com", "테스트유저", 1500);

        when(userRepository.findById(userId)).thenReturn(Optional.of(user));

        // when
        PointResponse response = pointService.getUserPoints(userId);

        // then
        assertThat(response.getPoints()).isEqualTo(1500);
    }

    @Test
    @DisplayName("포인트 조회 - 포인트가 0인 경우")
    void getUserPoints_ZeroPoints() {
        // given
        Long userId = 1L;
        User user = createUser(userId, "test@example.com", "테스트유저", 0);

        when(userRepository.findById(userId)).thenReturn(Optional.of(user));

        // when
        PointResponse response = pointService.getUserPoints(userId);

        // then
        assertThat(response.getPoints()).isEqualTo(0);
    }

    @Test
    @DisplayName("포인트 조회 - 높은 포인트 수")
    void getUserPoints_HighPoints() {
        // given
        Long userId = 1L;
        User user = createUser(userId, "test@example.com", "테스트유저", 999999);

        when(userRepository.findById(userId)).thenReturn(Optional.of(user));

        // when
        PointResponse response = pointService.getUserPoints(userId);

        // then
        assertThat(response.getPoints()).isEqualTo(999999);
    }

    @Test
    @DisplayName("포인트 조회 - 사용자를 찾을 수 없는 경우")
    void getUserPoints_UserNotFound() {
        // given
        Long userId = 999L;

        when(userRepository.findById(userId)).thenReturn(Optional.empty());

        // when & then
        assertThatThrownBy(() -> pointService.getUserPoints(userId))
                .isInstanceOf(RuntimeException.class)
                .hasMessage("사용자를 찾을 수 없습니다.");
    }

    private User createUser(Long userId, String email, String name, Integer points) {
        return new User(userId, email, name, null, null, "google", "1", UserRole.USER, points,
                LocalDateTime.now(), LocalDateTime.now());
    }
}