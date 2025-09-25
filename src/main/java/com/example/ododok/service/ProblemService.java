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

        Map<Long, String> correctAnswerMap = questions.stream()
                .collect(Collectors.toMap(Question::getId, Question::getAnswer));

        int correctAnswers = 0;
        for (ProblemSubmissionRequest.Answer answer : request.getAnswers()) {
            String correctAnswer = correctAnswerMap.get(answer.getQuestionId());
            if (correctAnswer != null && correctAnswer.equals(answer.getAnswer())) {
                correctAnswers++;
            }
        }

        int totalQuestions = request.getAnswers().size();
        int score = (int) Math.round((double) correctAnswers / totalQuestions * 100);
        int pointsEarned = correctAnswers * 100;

        user.setPoints(user.getPoints() + pointsEarned);
        userRepository.save(user);

        List<User> allUsers = userRepository.findAllByOrderByPointsDescUserIdAsc();
        int rank = 1;
        for (int i = 0; i < allUsers.size(); i++) {
            if (allUsers.get(i).getUserId().equals(userId)) {
                rank = i + 1;
                break;
            }
        }

        return new ProblemSubmissionResponse(
                "제출 완료!",
                score,
                correctAnswers,
                pointsEarned,
                rank
        );
    }

    public QuestionListResponse getQuestions(Long categoryId, Long companyId) {
        log.info("Fetching questions with categoryId: {} and companyId: {}", categoryId, companyId);

        List<Question> questions;
        String companyName = null;

        // companyId를 companyName으로 변환
        if (companyId != null) {
            companyName = companyRepository.findById(companyId)
                    .map(company -> company.getName())
                    .orElse(null);
        }

        final String finalCompanyName = companyName;

        if (categoryId != null && finalCompanyName != null) {
            // Both filters applied
            questions = questionRepository.findAll().stream()
                    .filter(q -> q.getIsPublic())
                    .filter(q -> categoryId.equals(q.getCategoryId()))
                    .filter(q -> finalCompanyName.equals(q.getCompanyName()))
                    .collect(Collectors.toList());
        } else if (categoryId != null) {
            // Only category filter
            questions = questionRepository.findAll().stream()
                    .filter(q -> q.getIsPublic())
                    .filter(q -> categoryId.equals(q.getCategoryId()))
                    .collect(Collectors.toList());
        } else if (finalCompanyName != null) {
            // Only company filter
            questions = questionRepository.findAll().stream()
                    .filter(q -> q.getIsPublic())
                    .filter(q -> finalCompanyName.equals(q.getCompanyName()))
                    .collect(Collectors.toList());
        } else {
            // No filters, return all public questions
            questions = questionRepository.findAll().stream()
                    .filter(q -> q.getIsPublic())
                    .collect(Collectors.toList());
        }

        List<QuestionListResponse.QuestionItem> questionItems = questions.stream()
                .map(q -> new QuestionListResponse.QuestionItem(q.getId(), q.getQuestion()))
                .collect(Collectors.toList());

        log.info("Found {} questions matching the criteria", questionItems.size());

        return new QuestionListResponse(questionItems);
    }
}