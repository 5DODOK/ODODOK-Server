package com.example.ododok.service;

import com.example.ododok.dto.QuestionUpdateRequest;
import com.example.ododok.dto.QuestionResponse;
import com.example.ododok.entity.Question;
import com.example.ododok.entity.User;
import com.example.ododok.entity.UserRole;
import com.example.ododok.exception.CsvProcessingException;
import com.example.ododok.repository.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.access.AccessDeniedException;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class QuestionUpdateServiceTest {

    @Mock
    private QuestionRepository questionRepository;

    @Mock
    private CompanyRepository companyRepository;

    @Mock
    private CategoryRepository categoryRepository;

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private QuestionService questionService;

    private User adminUser;
    private User regularUser;
    private Question existingQuestion;

    @BeforeEach
    void setUp() {
        adminUser = new User();
        adminUser.setUserId(1L);
        adminUser.setRole(UserRole.ADMIN);

        regularUser = new User();
        regularUser.setUserId(2L);
        regularUser.setRole(UserRole.USER);

        existingQuestion = new Question();
        existingQuestion.setId(1L);
        existingQuestion.setTitle("기존 제목");
        existingQuestion.setQuestion("기존 질문");
        existingQuestion.setContent("기존 내용");
        existingQuestion.setDifficulty(2);
        existingQuestion.setIsPublic(true);
        existingQuestion.setCreatedBy(1L);
        existingQuestion.setCreatedAt(LocalDateTime.now());
    }

    @Test
    void updateQuestion_Success_PartialUpdate() {
        // Given
        QuestionUpdateRequest request = new QuestionUpdateRequest();
        request.setTitle("수정된 제목");
        request.setDifficulty("HARD");
        request.setTags(Arrays.asList("java", "spring"));

        when(userRepository.findById(1L)).thenReturn(Optional.of(adminUser));
        when(questionRepository.findById(1L)).thenReturn(Optional.of(existingQuestion));
        when(questionRepository.findByTitle("수정된 제목")).thenReturn(Optional.empty());
        when(questionRepository.save(any(Question.class))).thenAnswer(invocation -> invocation.getArgument(0));

        // When
        QuestionResponse response = questionService.updateQuestion(1L, request, 1L);

        // Then
        assertNotNull(response);
        assertEquals("수정된 제목", response.getTitle());
        assertEquals("기존 질문", response.getQuestion()); // 변경되지 않음
        assertEquals("기존 내용", response.getContent()); // 변경되지 않음
        assertEquals(3, response.getDifficulty()); // HARD = 3
        assertEquals(Arrays.asList("java", "spring"), response.getTags());
        assertEquals(1L, response.getUpdatedBy());
        assertNotNull(response.getUpdatedAt());

        verify(questionRepository).save(any(Question.class));
    }

    @Test
    void updateQuestion_Success_AllFields() {
        // Given
        QuestionUpdateRequest request = new QuestionUpdateRequest();
        request.setTitle("완전히 새로운 제목");
        request.setContent("완전히 새로운 내용");
        request.setTags(Arrays.asList("algorithm", "data-structure"));
        request.setDifficulty("EASY");
        request.setAnswer("새로운 답변");
        request.setCategoryId(2L);
        request.setIsPublic(false);
        request.setYear(2024);
        request.setCompanyId(3L);

        when(userRepository.findById(1L)).thenReturn(Optional.of(adminUser));
        when(questionRepository.findById(1L)).thenReturn(Optional.of(existingQuestion));
        when(questionRepository.findByTitle("완전히 새로운 제목")).thenReturn(Optional.empty());
        when(categoryRepository.existsById(2L)).thenReturn(true);
        when(companyRepository.existsById(3L)).thenReturn(true);
        when(questionRepository.save(any(Question.class))).thenAnswer(invocation -> invocation.getArgument(0));

        // When
        QuestionResponse response = questionService.updateQuestion(1L, request, 1L);

        // Then
        assertNotNull(response);
        assertEquals("완전히 새로운 제목", response.getTitle());
        assertEquals("완전히 새로운 내용", response.getContent());
        assertEquals(Arrays.asList("algorithm", "data-structure"), response.getTags());
        assertEquals(1, response.getDifficulty()); // EASY = 1
        assertEquals("새로운 답변", response.getAnswer());
        assertEquals(2L, response.getCategoryId());
        assertEquals(false, response.getIsPublic());
        assertEquals(2024, response.getYear());
        assertEquals(3L, response.getCompanyId());

        verify(categoryRepository).existsById(2L);
        verify(companyRepository).existsById(3L);
        verify(questionRepository).save(any(Question.class));
    }

    @Test
    void updateQuestion_AccessDenied_RegularUser() {
        // Given
        QuestionUpdateRequest request = new QuestionUpdateRequest();
        request.setTitle("수정 시도");

        when(userRepository.findById(2L)).thenReturn(Optional.of(regularUser));

        // When & Then
        assertThrows(AccessDeniedException.class, () -> {
            questionService.updateQuestion(1L, request, 2L);
        });

        verify(questionRepository, never()).save(any());
    }

    @Test
    void updateQuestion_QuestionNotFound() {
        // Given
        QuestionUpdateRequest request = new QuestionUpdateRequest();
        request.setTitle("수정할 제목");

        when(userRepository.findById(1L)).thenReturn(Optional.of(adminUser));
        when(questionRepository.findById(999L)).thenReturn(Optional.empty());

        // When & Then
        CsvProcessingException exception = assertThrows(CsvProcessingException.class, () -> {
            questionService.updateQuestion(999L, request, 1L);
        });

        assertEquals("대상을 찾을 수 없습니다.", exception.getMessage());
        assertEquals("QUESTION_NOT_FOUND", exception.getErrorCode());
    }

    @Test
    void updateQuestion_DuplicateTitle() {
        // Given
        QuestionUpdateRequest request = new QuestionUpdateRequest();
        request.setTitle("중복된 제목");

        Question duplicateQuestion = new Question();
        duplicateQuestion.setId(2L);
        duplicateQuestion.setTitle("중복된 제목");

        when(userRepository.findById(1L)).thenReturn(Optional.of(adminUser));
        when(questionRepository.findById(1L)).thenReturn(Optional.of(existingQuestion));
        when(questionRepository.findByTitle("중복된 제목")).thenReturn(Optional.of(duplicateQuestion));

        // When & Then
        CsvProcessingException exception = assertThrows(CsvProcessingException.class, () -> {
            questionService.updateQuestion(1L, request, 1L);
        });

        assertEquals("동일한 제목의 질문이 이미 존재합니다.", exception.getMessage());
        assertEquals("DUPLICATE_TITLE", exception.getErrorCode());
    }

    @Test
    void updateQuestion_InvalidDifficulty() {
        // Given
        QuestionUpdateRequest request = new QuestionUpdateRequest();
        request.setDifficulty("INVALID");

        when(userRepository.findById(1L)).thenReturn(Optional.of(adminUser));
        when(questionRepository.findById(1L)).thenReturn(Optional.of(existingQuestion));

        // When & Then
        CsvProcessingException exception = assertThrows(CsvProcessingException.class, () -> {
            questionService.updateQuestion(1L, request, 1L);
        });

        assertEquals("난이도는 EASY, MEDIUM, HARD 중 하나여야 합니다.", exception.getMessage());
        assertEquals("INVALID_DIFFICULTY", exception.getErrorCode());
    }

    @Test
    void updateQuestion_CategoryNotFound() {
        // Given
        QuestionUpdateRequest request = new QuestionUpdateRequest();
        request.setCategoryId(999L);

        when(userRepository.findById(1L)).thenReturn(Optional.of(adminUser));
        when(questionRepository.findById(1L)).thenReturn(Optional.of(existingQuestion));
        when(categoryRepository.existsById(999L)).thenReturn(false);

        // When & Then
        CsvProcessingException exception = assertThrows(CsvProcessingException.class, () -> {
            questionService.updateQuestion(1L, request, 1L);
        });

        assertEquals("연결하려는 카테고리를 찾을 수 없습니다.", exception.getMessage());
        assertEquals("CATEGORY_NOT_FOUND", exception.getErrorCode());
    }

    @Test
    void updateQuestion_EmptyTitle() {
        // Given
        QuestionUpdateRequest request = new QuestionUpdateRequest();
        request.setTitle("   "); // 공백만 있는 제목

        when(userRepository.findById(1L)).thenReturn(Optional.of(adminUser));
        when(questionRepository.findById(1L)).thenReturn(Optional.of(existingQuestion));

        // When & Then
        CsvProcessingException exception = assertThrows(CsvProcessingException.class, () -> {
            questionService.updateQuestion(1L, request, 1L);
        });

        assertEquals("제목은 공백일 수 없습니다.", exception.getMessage());
        assertEquals("INVALID_TITLE", exception.getErrorCode());
    }

    @Test
    void updateQuestion_TooManyTags() {
        // Given
        QuestionUpdateRequest request = new QuestionUpdateRequest();
        request.setTags(Arrays.asList("tag1", "tag2", "tag3", "tag4", "tag5",
                                     "tag6", "tag7", "tag8", "tag9", "tag10", "tag11")); // 11개

        when(userRepository.findById(1L)).thenReturn(Optional.of(adminUser));
        when(questionRepository.findById(1L)).thenReturn(Optional.of(existingQuestion));

        // When & Then
        CsvProcessingException exception = assertThrows(CsvProcessingException.class, () -> {
            questionService.updateQuestion(1L, request, 1L);
        });

        assertEquals("태그는 최대 10개까지 허용됩니다.", exception.getMessage());
        assertEquals("TOO_MANY_TAGS", exception.getErrorCode());
    }
}