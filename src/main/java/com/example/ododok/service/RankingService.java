package com.example.ododok.service;

import com.example.ododok.dto.RankingResponse;
import com.example.ododok.entity.User;
import com.example.ododok.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.IntStream;

@Service
@RequiredArgsConstructor
public class RankingService {

    private final UserRepository userRepository;

    public RankingResponse getRankings(String currentUserEmail) {
        List<User> allUsers = userRepository.findAllByOrderByPointsDescUserIdAsc();

        AtomicInteger rank = new AtomicInteger(1);
        List<RankingResponse.UserRanking> rankings = allUsers.stream()
                .map(user -> new RankingResponse.UserRanking(
                        rank.getAndIncrement(),
                        user.getName(),
                        user.getPoints()
                ))
                .toList();

        RankingResponse.UserRanking currentUser = null;
        if (currentUserEmail != null) {
            currentUser = IntStream.range(0, allUsers.size())
                    .filter(i -> allUsers.get(i).getEmail().equals(currentUserEmail))
                    .mapToObj(i -> new RankingResponse.UserRanking(
                            i + 1,
                            allUsers.get(i).getName(),
                            allUsers.get(i).getPoints()
                    ))
                    .findFirst()
                    .orElse(null);
        }

        return new RankingResponse(rankings, currentUser);
    }
}