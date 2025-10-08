package com.example.ododok.service;

import com.example.ododok.dto.ProblemSubmissionRequest;
import com.example.ododok.dto.ProblemSubmissionResponse;
import com.example.ododok.dto.QuestionListResponse;
import com.example.ododok.entity.Question;
import com.example.ododok.entity.User;
import com.example.ododok.repository.QuestionRepository;
import com.example.ododok.repository.UserRepository;
import com.example.ododok.repository.CompanyRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class ProblemService {

    private final QuestionRepository questionRepository;
    private final UserRepository userRepository;
    private final CompanyRepository companyRepository;

    @Transactional
    public ProblemSubmissionResponse submitProblem(ProblemSubmissionRequest request, Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("사용자를 찾을 수 없습니다."));

        List<Long> questionIds = request.getAnswers().stream()
                .map(ProblemSubmissionRequest.Answer::getQuestionId)
                .collect(Collectors.toList());

        List<Question> questions = questionRepository.findAllById(questionIds);
        if (questions.size() != questionIds.size()) {
            throw new RuntimeException("일부 문제를 찾을 수 없습니다.");
        }

        int totalQuestions = request.getAnswers().size();

        // 문제 1개당 100포인트씩 지급
        int pointsPerQuestion = 100;
        int pointsEarned = totalQuestions * pointsPerQuestion;

        // 총 점수는 참여도 개념으로 100점 만점 처리
        int score = 100;
        int correctAnswers = totalQuestions;

        // 유저 포인트 업데이트
        user.setPoints(user.getPoints() + pointsEarned);
        userRepository.save(user);

        // 랭크 재계산
        List<User> allUsers = userRepository.findAllByOrderByPointsDescUserIdAsc();
        int rank = 1;
        for (int i = 0; i < allUsers.size(); i++) {
            if (allUsers.get(i).getUserId().equals(userId)) {
                rank = i + 1;
                break;
            }
        }

        return new ProblemSubmissionResponse(
                "제출 완료! 포인트가 지급되었습니다 🎉",
                score,
                correctAnswers,
                pointsEarned,
                rank
        );
    }

    public QuestionListResponse getQuestions(Long categoryId, Long companyId) {
        log.info("Fetching random filtered questions with categoryId: {} and companyId: {}", categoryId, companyId);

        String companyName = null;

        // companyId → companyName 변환
        if (companyId != null) {
            companyName = companyRepository.findById(companyId)
                    .map(company -> company.getName())
                    .orElse(null);
        }

        // DB에서 바로 랜덤 10개만 조회
        Pageable pageable = PageRequest.of(0, 10);
        List<Question> questions = questionRepository.findRandomQuestionsWithFilters(categoryId, companyName, pageable);

        List<QuestionListResponse.QuestionItem> questionItems = questions.stream()
                .map(q -> new QuestionListResponse.QuestionItem(q.getId(), q.getQuestion()))
                .collect(Collectors.toList());

        log.info("🎯 Found {} random filtered questions (max 10)", questionItems.size());

        return new QuestionListResponse(questionItems);
    }
}