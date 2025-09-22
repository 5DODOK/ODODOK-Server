package com.example.ododok.service;

import com.example.ododok.dto.RankingResponse;
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
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RankingServiceTest {

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private RankingService rankingService;

    @Test
    @DisplayName("랭킹 조회 - 로그인 사용자")
    void getRankings_WithCurrentUser() {
        // given
        User user1 = new User(1L, "kim@bssm.hs.kr", "김철수", "kim", "김철수", "google", "1", UserRole.USER, 5000, LocalDateTime.now(), LocalDateTime.now());
        User user2 = new User(2L, "lee@bssm.hs.kr", "이하은", "lee", "이하은", "google", "2", UserRole.USER, 4500, LocalDateTime.now(), LocalDateTime.now());
        User user3 = new User(3L, "oh@bssm.hs.kr", "오주현", "oh", "오주현", "google", "3", UserRole.USER, 4200, LocalDateTime.now(), LocalDateTime.now());

        List<User> users = List.of(user1, user2, user3);
        when(userRepository.findAllByOrderByPointsDescUserIdAsc()).thenReturn(users);

        // when
        RankingResponse result = rankingService.getRankings("oh@bssm.hs.kr");

        // then
        assertThat(result.getRankings()).hasSize(3);
        assertThat(result.getRankings().get(0).getRank()).isEqualTo(1);
        assertThat(result.getRankings().get(0).getUsername()).isEqualTo("김철수");
        assertThat(result.getRankings().get(0).getPoints()).isEqualTo(5000);

        assertThat(result.getRankings().get(1).getRank()).isEqualTo(2);
        assertThat(result.getRankings().get(1).getUsername()).isEqualTo("이하은");
        assertThat(result.getRankings().get(1).getPoints()).isEqualTo(4500);

        assertThat(result.getRankings().get(2).getRank()).isEqualTo(3);
        assertThat(result.getRankings().get(2).getUsername()).isEqualTo("오주현");
        assertThat(result.getRankings().get(2).getPoints()).isEqualTo(4200);

        assertThat(result.getCurrentUser()).isNotNull();
        assertThat(result.getCurrentUser().getRank()).isEqualTo(3);
        assertThat(result.getCurrentUser().getUsername()).isEqualTo("오주현");
        assertThat(result.getCurrentUser().getPoints()).isEqualTo(4200);
    }

    @Test
    @DisplayName("랭킹 조회 - 비로그인 사용자")
    void getRankings_WithoutCurrentUser() {
        // given
        User user1 = new User(1L, "kim@bssm.hs.kr", "김철수", "kim", "김철수", "google", "1", UserRole.USER, 5000, LocalDateTime.now(), LocalDateTime.now());
        User user2 = new User(2L, "lee@bssm.hs.kr", "이하은", "lee", "이하은", "google", "2", UserRole.USER, 4500, LocalDateTime.now(), LocalDateTime.now());

        List<User> users = List.of(user1, user2);
        when(userRepository.findAllByOrderByPointsDescUserIdAsc()).thenReturn(users);

        // when
        RankingResponse result = rankingService.getRankings(null);

        // then
        assertThat(result.getRankings()).hasSize(2);
        assertThat(result.getRankings().get(0).getRank()).isEqualTo(1);
        assertThat(result.getRankings().get(0).getUsername()).isEqualTo("김철수");
        assertThat(result.getRankings().get(0).getPoints()).isEqualTo(5000);

        assertThat(result.getRankings().get(1).getRank()).isEqualTo(2);
        assertThat(result.getRankings().get(1).getUsername()).isEqualTo("이하은");
        assertThat(result.getRankings().get(1).getPoints()).isEqualTo(4500);

        assertThat(result.getCurrentUser()).isNull();
    }

    @Test
    @DisplayName("랭킹 조회 - 존재하지 않는 사용자")
    void getRankings_UserNotFound() {
        // given
        User user1 = new User(1L, "kim@bssm.hs.kr", "김철수", "kim", "김철수", "google", "1", UserRole.USER, 5000, LocalDateTime.now(), LocalDateTime.now());

        List<User> users = List.of(user1);
        when(userRepository.findAllByOrderByPointsDescUserIdAsc()).thenReturn(users);

        // when
        RankingResponse result = rankingService.getRankings("notfound@bssm.hs.kr");

        // then
        assertThat(result.getRankings()).hasSize(1);
        assertThat(result.getCurrentUser()).isNull();
    }
}