package com.example.ododok.service;

import com.example.ododok.dto.ProblemSubmissionRequest;
import com.example.ododok.dto.ProblemSubmissionResponse;
import com.example.ododok.entity.Question;
import com.example.ododok.entity.User;
import com.example.ododok.entity.UserRole;
import com.example.ododok.repository.QuestionRepository;
import com.example.ododok.repository.UserRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ProblemServiceTest {

    @Mock
    private QuestionRepository questionRepository;

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private ProblemService problemService;

    @Test
    @DisplayName("문제 제출 - 모든 답안이 정답인 경우")
    void submitProblem_AllCorrect() {
        // given
        Long userId = 1L;
        User user = createUser(userId, "test@example.com", "테스트유저", 0);

        ProblemSubmissionRequest.Answer answer1 = createAnswer(1L, "A", 120);
        ProblemSubmissionRequest.Answer answer2 = createAnswer(2L, "B", 180);
        ProblemSubmissionRequest request = createRequest(List.of(answer1, answer2), 300, "2025-09-23T10:30:00Z");

        Question question1 = createQuestion(1L, "A");
        Question question2 = createQuestion(2L, "B");
        List<Question> questions = List.of(question1, question2);

        User updatedUser = createUser(userId, "test@example.com", "테스트유저", 200);
        List<User> allUsers = List.of(updatedUser, createUser(2L, "other@example.com", "다른유저", 150));

        when(userRepository.findById(userId)).thenReturn(Optional.of(user));
        when(questionRepository.findAllById(anyList())).thenReturn(questions);
        when(userRepository.save(any(User.class))).thenReturn(updatedUser);
        when(userRepository.findAllByOrderByPointsDescUserIdAsc()).thenReturn(allUsers);

        // when
        ProblemSubmissionResponse response = problemService.submitProblem(request, userId);

        // then
        assertThat(response.getMessage()).isEqualTo("제출 완료!");
        assertThat(response.getScore()).isEqualTo(100);
        assertThat(response.getCorrectAnswers()).isEqualTo(2);
        assertThat(response.getPointsEarned()).isEqualTo(200);
        assertThat(response.getRank()).isEqualTo(1);

        verify(userRepository).save(any(User.class));
    }

    @Test
    @DisplayName("문제 제출 - 일부 답안이 정답인 경우")
    void submitProblem_PartiallyCorrect() {
        // given
        Long userId = 1L;
        User user = createUser(userId, "test@example.com", "테스트유저", 0);

        ProblemSubmissionRequest.Answer answer1 = createAnswer(1L, "A", 120);
        ProblemSubmissionRequest.Answer answer2 = createAnswer(2L, "C", 180);
        ProblemSubmissionRequest request = createRequest(List.of(answer1, answer2), 300, "2025-09-23T10:30:00Z");

        Question question1 = createQuestion(1L, "A");
        Question question2 = createQuestion(2L, "B");
        List<Question> questions = List.of(question1, question2);

        User updatedUser = createUser(userId, "test@example.com", "테스트유저", 100);
        List<User> allUsers = List.of(createUser(2L, "other@example.com", "다른유저", 500), updatedUser);

        when(userRepository.findById(userId)).thenReturn(Optional.of(user));
        when(questionRepository.findAllById(anyList())).thenReturn(questions);
        when(userRepository.save(any(User.class))).thenReturn(updatedUser);
        when(userRepository.findAllByOrderByPointsDescUserIdAsc()).thenReturn(allUsers);

        // when
        ProblemSubmissionResponse response = problemService.submitProblem(request, userId);

        // then
        assertThat(response.getMessage()).isEqualTo("제출 완료!");
        assertThat(response.getScore()).isEqualTo(50);
        assertThat(response.getCorrectAnswers()).isEqualTo(1);
        assertThat(response.getPointsEarned()).isEqualTo(100);
        assertThat(response.getRank()).isEqualTo(2);
    }

    @Test
    @DisplayName("문제 제출 - 모든 답안이 오답인 경우")
    void submitProblem_AllIncorrect() {
        // given
        Long userId = 1L;
        User user = createUser(userId, "test@example.com", "테스트유저", 0);

        ProblemSubmissionRequest.Answer answer1 = createAnswer(1L, "C", 120);
        ProblemSubmissionRequest.Answer answer2 = createAnswer(2L, "D", 180);
        ProblemSubmissionRequest request = createRequest(List.of(answer1, answer2), 300, "2025-09-23T10:30:00Z");

        Question question1 = createQuestion(1L, "A");
        Question question2 = createQuestion(2L, "B");
        List<Question> questions = List.of(question1, question2);

        User updatedUser = createUser(userId, "test@example.com", "테스트유저", 0);
        List<User> allUsers = List.of(createUser(2L, "other@example.com", "다른유저", 100), updatedUser);

        when(userRepository.findById(userId)).thenReturn(Optional.of(user));
        when(questionRepository.findAllById(anyList())).thenReturn(questions);
        when(userRepository.save(any(User.class))).thenReturn(updatedUser);
        when(userRepository.findAllByOrderByPointsDescUserIdAsc()).thenReturn(allUsers);

        // when
        ProblemSubmissionResponse response = problemService.submitProblem(request, userId);

        // then
        assertThat(response.getMessage()).isEqualTo("제출 완료!");
        assertThat(response.getScore()).isEqualTo(0);
        assertThat(response.getCorrectAnswers()).isEqualTo(0);
        assertThat(response.getPointsEarned()).isEqualTo(0);
        assertThat(response.getRank()).isEqualTo(2);
    }

    @Test
    @DisplayName("문제 제출 - 사용자를 찾을 수 없는 경우")
    void submitProblem_UserNotFound() {
        // given
        Long userId = 999L;
        ProblemSubmissionRequest.Answer answer = createAnswer(1L, "A", 120);
        ProblemSubmissionRequest request = createRequest(List.of(answer), 120, "2025-09-23T10:30:00Z");

        when(userRepository.findById(userId)).thenReturn(Optional.empty());

        // when & then
        assertThatThrownBy(() -> problemService.submitProblem(request, userId))
                .isInstanceOf(RuntimeException.class)
                .hasMessage("사용자를 찾을 수 없습니다.");

        verify(questionRepository, never()).findAllById(anyList());
        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    @DisplayName("문제 제출 - 일부 문제를 찾을 수 없는 경우")
    void submitProblem_QuestionsNotFound() {
        // given
        Long userId = 1L;
        User user = createUser(userId, "test@example.com", "테스트유저", 0);

        ProblemSubmissionRequest.Answer answer1 = createAnswer(1L, "A", 120);
        ProblemSubmissionRequest.Answer answer2 = createAnswer(2L, "B", 180);
        ProblemSubmissionRequest request = createRequest(List.of(answer1, answer2), 300, "2025-09-23T10:30:00Z");

        Question question1 = createQuestion(1L, "A");
        List<Question> questions = List.of(question1);

        when(userRepository.findById(userId)).thenReturn(Optional.of(user));
        when(questionRepository.findAllById(anyList())).thenReturn(questions);

        // when & then
        assertThatThrownBy(() -> problemService.submitProblem(request, userId))
                .isInstanceOf(RuntimeException.class)
                .hasMessage("일부 문제를 찾을 수 없습니다.");

        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    @DisplayName("문제 제출 - 기존 포인트에 추가되는지 확인")
    void submitProblem_PointsAccumulated() {
        // given
        Long userId = 1L;
        User user = createUser(userId, "test@example.com", "테스트유저", 500);

        ProblemSubmissionRequest.Answer answer = createAnswer(1L, "A", 120);
        ProblemSubmissionRequest request = createRequest(List.of(answer), 120, "2025-09-23T10:30:00Z");

        Question question = createQuestion(1L, "A");
        List<Question> questions = List.of(question);

        User updatedUser = createUser(userId, "test@example.com", "테스트유저", 600);
        List<User> allUsers = List.of(updatedUser);

        when(userRepository.findById(userId)).thenReturn(Optional.of(user));
        when(questionRepository.findAllById(anyList())).thenReturn(questions);
        when(userRepository.save(any(User.class))).thenReturn(updatedUser);
        when(userRepository.findAllByOrderByPointsDescUserIdAsc()).thenReturn(allUsers);

        // when
        ProblemSubmissionResponse response = problemService.submitProblem(request, userId);

        // then
        assertThat(response.getPointsEarned()).isEqualTo(100);
        verify(userRepository).save(argThat(savedUser ->
            savedUser.getPoints().equals(600)
        ));
    }

    private User createUser(Long userId, String email, String name, Integer points) {
        return new User(userId, email, name, null, null, "google", "1", UserRole.USER, points,
                LocalDateTime.now(), LocalDateTime.now());
    }

    private Question createQuestion(Long id, String answer) {
        Question question = new Question();
        question.setId(id);
        question.setAnswer(answer);
        return question;
    }

    private ProblemSubmissionRequest.Answer createAnswer(Long questionId, String answer, Integer timeSpent) {
        ProblemSubmissionRequest.Answer answerObj = new ProblemSubmissionRequest.Answer();
        answerObj.setQuestionId(questionId);
        answerObj.setAnswer(answer);
        answerObj.setTimeSpent(timeSpent);
        return answerObj;
    }

    private ProblemSubmissionRequest createRequest(List<ProblemSubmissionRequest.Answer> answers,
                                                  Integer totalTimeSpent, String submittedAt) {
        ProblemSubmissionRequest request = new ProblemSubmissionRequest();
        request.setAnswers(answers);
        request.setTotalTimeSpent(totalTimeSpent);
        request.setSubmittedAt(submittedAt);
        return request;
    }
}