package com.example.ododok.service;

import com.example.ododok.dto.ProblemSubmissionRequest;
import com.example.ododok.dto.ProblemSubmissionResponse;
import com.example.ododok.dto.QuestionListResponse;
import com.example.ododok.dto.TechnicalFeedbackResponse;
import com.example.ododok.dto.PersonalityFeedbackResponse;
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
    private final GeminiService geminiService;

    @Transactional
    public ProblemSubmissionResponse submitProblem(ProblemSubmissionRequest request, Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("사용자를 찾을 수 없습니다."));

        List<Long> questionIds = request.getAnswers().stream()
                .map(ProblemSubmissionRequest.Answer::getQuestionId)
                .collect(Collectors.toList());

        Map<Long, Question> questionMap = questionRepository.findAllById(questionIds).stream()
                .collect(Collectors.toMap(Question::getId, q -> q));

        if (questionMap.size() != questionIds.size()) {
            throw new RuntimeException("일부 문제를 찾을 수 없습니다.");
        }

        int totalPointsEarned = 0;
        int totalLogicScore = 0;
        int totalAccuracyScore = 0;
        int totalClarityScore = 0;
        int technicalCount = 0;

        // 각 답변에 대해 면접 타입별로 처리 (저장하지 않고 즉시 채점만)
        for (ProblemSubmissionRequest.Answer answerReq : request.getAnswers()) {
            Question question = questionMap.get(answerReq.getQuestionId());
            String interviewType = question.getTitle();

            if ("TECHNICAL".equals(interviewType)) {
                // 기술 면접: 논리성, 정확성, 명확성 점수 계산
                TechnicalFeedbackResponse feedback = geminiService.generateTechnicalFeedback(
                        question.getQuestion(), answerReq.getAnswer());

                // 점수 합산
                totalLogicScore += feedback.getLogicScore();
                totalAccuracyScore += feedback.getAccuracyScore();
                totalClarityScore += feedback.getClarityScore();
                technicalCount++;

                // 기술 면접은 점수로만 평가, 포인트 미지급

            } else if ("PERSONALITY".equals(interviewType)) {
                // 인성 면접: 연관성 판단 및 포인트 지급
                PersonalityFeedbackResponse feedback = geminiService.generatePersonalityFeedback(
                        question.getQuestion(), answerReq.getAnswer());

                totalPointsEarned += feedback.getPointsAwarded();
            } else {
                // 면접 타입이 지정되지 않은 경우 기본 100포인트 지급
                totalPointsEarned += 100;
            }
        }

        // 유저 포인트 업데이트 (인성 면접 포인트만)
        user.setPoints(user.getPoints() + totalPointsEarned);
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

        // 종합 코멘트 생성 (모든 답변 완료 시)
        String overallComment = generateOverallCommentForSubmission(request, questionMap);

        return new ProblemSubmissionResponse(
                "제출 완료! " + (totalPointsEarned > 0 ? "포인트가 지급되었습니다 🎉" : "평가가 완료되었습니다!"),
                technicalCount > 0 ? (totalLogicScore + totalAccuracyScore + totalClarityScore) / (technicalCount * 3) : null,
                technicalCount > 0 ? totalLogicScore / technicalCount : null,
                technicalCount > 0 ? totalAccuracyScore / technicalCount : null,
                technicalCount > 0 ? totalClarityScore / technicalCount : null,
                totalPointsEarned,
                rank,
                overallComment
        );
    }

    private String generateOverallCommentForSubmission(ProblemSubmissionRequest request, Map<Long, Question> questionMap) {
        // 면접 타입 확인
        String interviewType = questionMap.values().stream()
                .map(Question::getTitle)
                .filter(type -> type != null)
                .findFirst()
                .orElse("일반");

        // 답변 요약 생성
        StringBuilder summary = new StringBuilder();
        request.getAnswers().forEach(ans -> {
            Question q = questionMap.get(ans.getQuestionId());
            summary.append("Q: ").append(q.getQuestion()).append("\n");
            summary.append("A: ").append(ans.getAnswer()).append("\n\n");
        });

        return geminiService.generateOverallComment(interviewType, summary.toString());
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