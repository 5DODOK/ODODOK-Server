package com.example.ododok.service;

import com.example.ododok.dto.QuestionCreateRequest;
import com.example.ododok.dto.QuestionUpdateRequest;
import com.example.ododok.dto.QuestionResponse;
import com.example.ododok.entity.*;
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
class
QuestionServiceTest {

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
    private Company company;
    private Category category;

    @BeforeEach
    void setUp() {
        adminUser = new User();
        adminUser.setUserId(1L);
        adminUser.setRole(UserRole.ADMIN);

        regularUser = new User();
        regularUser.setUserId(2L);
        regularUser.setRole(UserRole.USER);

        company = new Company();
        company.setId(1L);
        company.setName("테스트회사");

        category = new Category();
        category.setId(1L);
        category.setName("테스트카테고리");
    }

    @Test
    void createQuestion_Success_MinimalFields() {
        // Given
        QuestionCreateRequest request = new QuestionCreateRequest();
        request.setQuestion("테스트 질문");
        request.setDifficulty("MEDIUM");

        Question savedQuestion = new Question();
        savedQuestion.setId(1L);
        savedQuestion.setQuestion("테스트 질문");
        savedQuestion.setDifficulty(2);
        savedQuestion.setCreatedBy(1L);
        savedQuestion.setCreatedAt(LocalDateTime.now());

        when(userRepository.findById(1L)).thenReturn(Optional.of(adminUser));
        when(questionRepository.findByQuestion("테스트 질문")).thenReturn(Optional.empty());
        when(questionRepository.save(any(Question.class))).thenReturn(savedQuestion);

        // When
        QuestionResponse response = questionService.createQuestion(request, 1L);

        // Then
        assertNotNull(response);
        assertEquals(1L, response.getId());
        assertEquals("테스트 질문", response.getQuestion());
        assertEquals(2, response.getDifficulty());
        assertEquals(1L, response.getCreatedBy());

        verify(questionRepository).save(any(Question.class));
    }

    @Test
    void createQuestion_Success_AllFields() {
        // Given
        QuestionCreateRequest request = new QuestionCreateRequest();
        request.setQuestion("완전한 테스트 질문");
        request.setDifficulty("HARD");
        request.setYear(2024);
        request.setCompanyId(1L);
        request.setCategoryId(1L);

        Question savedQuestion = new Question();
        savedQuestion.setId(2L);
        savedQuestion.setQuestion("완전한 테스트 질문");
        savedQuestion.setDifficulty(3);
        savedQuestion.setYear(2024);
        savedQuestion.setCompanyId(1L);
        savedQuestion.setCategoryId(1L);
        savedQuestion.setCreatedBy(1L);
        savedQuestion.setCreatedAt(LocalDateTime.now());

        when(userRepository.findById(1L)).thenReturn(Optional.of(adminUser));
        when(companyRepository.existsById(1L)).thenReturn(true);
        when(categoryRepository.existsById(1L)).thenReturn(true);
        when(questionRepository.findByQuestion("완전한 테스트 질문")).thenReturn(Optional.empty());
        when(questionRepository.save(any(Question.class))).thenReturn(savedQuestion);

        // When
        QuestionResponse response = questionService.createQuestion(request, 1L);

        // Then
        assertNotNull(response);
        assertEquals(2L, response.getId());
        assertEquals("완전한 테스트 질문", response.getQuestion());
        assertEquals(3, response.getDifficulty());
        assertEquals(2024, response.getYear());
        assertEquals(1L, response.getCompanyId());
        assertEquals(1L, response.getCategoryId());
        assertEquals(1L, response.getCreatedBy());

        verify(companyRepository).existsById(1L);
        verify(categoryRepository).existsById(1L);
        verify(questionRepository).save(any(Question.class));
    }

    @Test
    void createQuestion_DefaultDifficulty() {
        // Given
        QuestionCreateRequest request = new QuestionCreateRequest();
        request.setQuestion("기본 난이도 질문");
        // difficulty 설정하지 않음

        Question savedQuestion = new Question();
        savedQuestion.setId(3L);
        savedQuestion.setQuestion("기본 난이도 질문");
        savedQuestion.setDifficulty(2); // MEDIUM
        savedQuestion.setCreatedBy(1L);
        savedQuestion.setCreatedAt(LocalDateTime.now());

        when(userRepository.findById(1L)).thenReturn(Optional.of(adminUser));
        when(questionRepository.findByQuestion("기본 난이도 질문")).thenReturn(Optional.empty());
        when(questionRepository.save(any(Question.class))).thenReturn(savedQuestion);

        // When
        QuestionResponse response = questionService.createQuestion(request, 1L);

        // Then
        assertNotNull(response);
        assertEquals(2, response.getDifficulty()); // MEDIUM = 2

        verify(questionRepository).save(argThat(question ->
            question.getDifficulty() == 2
        ));
    }

    @Test
    void createQuestion_AccessDenied_RegularUser() {
        // Given
        QuestionCreateRequest request = new QuestionCreateRequest();
        request.setQuestion("권한 없는 사용자 질문");

        when(userRepository.findById(2L)).thenReturn(Optional.of(regularUser));

        // When & Then
        assertThrows(AccessDeniedException.class, () -> {
            questionService.createQuestion(request, 2L);
        });

        verify(questionRepository, never()).save(any());
    }

    @Test
    void createQuestion_UserNotFound() {
        // Given
        QuestionCreateRequest request = new QuestionCreateRequest();
        request.setQuestion("존재하지 않는 사용자");

        when(userRepository.findById(999L)).thenReturn(Optional.empty());

        // When & Then
        CsvProcessingException exception = assertThrows(CsvProcessingException.class, () -> {
            questionService.createQuestion(request, 999L);
        });

        assertEquals("사용자를 찾을 수 없습니다.", exception.getMessage());
        assertEquals("USER_NOT_FOUND", exception.getErrorCode());
    }

    @Test
    void createQuestion_CompanyNotFound() {
        // Given
        QuestionCreateRequest request = new QuestionCreateRequest();
        request.setQuestion("존재하지 않는 회사 질문");
        request.setCompanyId(999L);

        when(userRepository.findById(1L)).thenReturn(Optional.of(adminUser));
        when(companyRepository.existsById(999L)).thenReturn(false);

        // When & Then
        CsvProcessingException exception = assertThrows(CsvProcessingException.class, () -> {
            questionService.createQuestion(request, 1L);
        });

        assertEquals("연결하려는 회사를 찾을 수 없습니다.", exception.getMessage());
        assertEquals("COMPANY_NOT_FOUND", exception.getErrorCode());
    }

    @Test
    void createQuestion_CategoryNotFound() {
        // Given
        QuestionCreateRequest request = new QuestionCreateRequest();
        request.setQuestion("존재하지 않는 카테고리 질문");
        request.setCategoryId(999L);

        when(userRepository.findById(1L)).thenReturn(Optional.of(adminUser));
        when(categoryRepository.existsById(999L)).thenReturn(false);

        // When & Then
        CsvProcessingException exception = assertThrows(CsvProcessingException.class, () -> {
            questionService.createQuestion(request, 1L);
        });

        assertEquals("연결하려는 카테고리를 찾을 수 없습니다.", exception.getMessage());
        assertEquals("CATEGORY_NOT_FOUND", exception.getErrorCode());
    }

    @Test
    void createQuestion_InvalidDifficulty() {
        // Given
        QuestionCreateRequest request = new QuestionCreateRequest();
        request.setQuestion("잘못된 난이도 질문");
        request.setDifficulty("INVALID");

        when(userRepository.findById(1L)).thenReturn(Optional.of(adminUser));

        // When & Then
        CsvProcessingException exception = assertThrows(CsvProcessingException.class, () -> {
            questionService.createQuestion(request, 1L);
        });

        assertEquals("난이도는 EASY, MEDIUM, HARD 중 하나여야 합니다.", exception.getMessage());
        assertEquals("INVALID_DIFFICULTY", exception.getErrorCode());
    }

    @Test
    void createQuestion_DuplicateQuestion() {
        // Given
        QuestionCreateRequest request = new QuestionCreateRequest();
        request.setQuestion("중복된 질문");

        Question existingQuestion = new Question();
        existingQuestion.setId(1L);
        existingQuestion.setQuestion("중복된 질문");

        when(userRepository.findById(1L)).thenReturn(Optional.of(adminUser));
        when(questionRepository.findByQuestion("중복된 질문")).thenReturn(Optional.of(existingQuestion));

        // When & Then
        CsvProcessingException exception = assertThrows(CsvProcessingException.class, () -> {
            questionService.createQuestion(request, 1L);
        });

        assertEquals("동일한 제목의 질문이 이미 존재합니다.", exception.getMessage());
        assertEquals("DUPLICATE_QUESTION", exception.getErrorCode());

        verify(questionRepository, never()).save(any());
    }

    @Test
    void createQuestion_DifficultyMapping() {
        // Given
        QuestionCreateRequest requestEasy = new QuestionCreateRequest();
        requestEasy.setQuestion("EASY 질문");
        requestEasy.setDifficulty("EASY");

        QuestionCreateRequest requestHard = new QuestionCreateRequest();
        requestHard.setQuestion("HARD 질문");
        requestHard.setDifficulty("hard"); // 소문자도 테스트

        when(userRepository.findById(1L)).thenReturn(Optional.of(adminUser));
        when(questionRepository.findByQuestion(anyString())).thenReturn(Optional.empty());
        when(questionRepository.save(any(Question.class))).thenAnswer(invocation -> invocation.getArgument(0));

        // When & Then - EASY
        questionService.createQuestion(requestEasy, 1L);
        verify(questionRepository).save(argThat(question -> question.getDifficulty() == 1));

        // When & Then - HARD (소문자)
        questionService.createQuestion(requestHard, 1L);
        verify(questionRepository).save(argThat(question -> question.getDifficulty() == 3));
    }

    @Test
    void deleteQuestion_Success() {
        // Given
        Question existingQuestion = new Question();
        existingQuestion.setId(1L);
        existingQuestion.setQuestion("삭제할 질문");

        when(userRepository.findById(1L)).thenReturn(Optional.of(adminUser));
        when(questionRepository.findById(1L)).thenReturn(Optional.of(existingQuestion));

        // When
        questionService.deleteQuestion(1L, 1L);

        // Then
        verify(questionRepository).delete(existingQuestion);
    }

    @Test
    void deleteQuestion_AccessDenied_RegularUser() {
        // Given
        when(userRepository.findById(2L)).thenReturn(Optional.of(regularUser));

        // When & Then
        assertThrows(AccessDeniedException.class, () -> {
            questionService.deleteQuestion(1L, 2L);
        });

        verify(questionRepository, never()).delete(any());
    }

    @Test
    void deleteQuestion_UserNotFound() {
        // Given
        when(userRepository.findById(999L)).thenReturn(Optional.empty());

        // When & Then
        CsvProcessingException exception = assertThrows(CsvProcessingException.class, () -> {
            questionService.deleteQuestion(1L, 999L);
        });

        assertEquals("사용자를 찾을 수 없습니다.", exception.getMessage());
        assertEquals("USER_NOT_FOUND", exception.getErrorCode());

        verify(questionRepository, never()).delete(any());
    }

    @Test
    void deleteQuestion_QuestionNotFound() {
        // Given
        when(userRepository.findById(1L)).thenReturn(Optional.of(adminUser));
        when(questionRepository.findById(999L)).thenReturn(Optional.empty());

        // When & Then
        CsvProcessingException exception = assertThrows(CsvProcessingException.class, () -> {
            questionService.deleteQuestion(999L, 1L);
        });

        assertEquals("대상을 찾을 수 없습니다.", exception.getMessage());
        assertEquals("QUESTION_NOT_FOUND", exception.getErrorCode());

        verify(questionRepository, never()).delete(any());
    }

    // UPDATE TESTS

    @Test
    void updateQuestion_Success_PartialUpdate() {
        // Given
        Question existingQuestion = new Question();
        existingQuestion.setId(1L);
        existingQuestion.setTitle("기존 제목");
        existingQuestion.setQuestion("기존 질문");
        existingQuestion.setContent("기존 내용");
        existingQuestion.setDifficulty(2);
        existingQuestion.setIsPublic(true);
        existingQuestion.setCreatedBy(1L);
        existingQuestion.setCreatedAt(LocalDateTime.now());

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
        assertEquals("기존 질문", response.getQuestion());
        assertEquals("기존 내용", response.getContent());
        assertEquals(3, response.getDifficulty()); // HARD = 3
        assertEquals(Arrays.asList("java", "spring"), response.getTags());
        assertEquals(1L, response.getUpdatedBy());
        assertNotNull(response.getUpdatedAt());

        verify(questionRepository).save(any(Question.class));
    }

    @Test
    void updateQuestion_Success_AllFields() {
        // Given
        Question existingQuestion = new Question();
        existingQuestion.setId(1L);
        existingQuestion.setTitle("기존 제목");
        existingQuestion.setQuestion("기존 질문");
        existingQuestion.setDifficulty(2);
        existingQuestion.setCreatedBy(1L);
        existingQuestion.setCreatedAt(LocalDateTime.now());

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

        verify(questionRepository, never()).save(any());
    }

    @Test
    void updateQuestion_DuplicateTitle() {
        // Given
        Question existingQuestion = new Question();
        existingQuestion.setId(1L);
        existingQuestion.setTitle("기존 제목");

        Question duplicateQuestion = new Question();
        duplicateQuestion.setId(2L);
        duplicateQuestion.setTitle("중복된 제목");

        QuestionUpdateRequest request = new QuestionUpdateRequest();
        request.setTitle("중복된 제목");

        when(userRepository.findById(1L)).thenReturn(Optional.of(adminUser));
        when(questionRepository.findById(1L)).thenReturn(Optional.of(existingQuestion));
        when(questionRepository.findByTitle("중복된 제목")).thenReturn(Optional.of(duplicateQuestion));

        // When & Then
        CsvProcessingException exception = assertThrows(CsvProcessingException.class, () -> {
            questionService.updateQuestion(1L, request, 1L);
        });

        assertEquals("동일한 제목의 질문이 이미 존재합니다.", exception.getMessage());
        assertEquals("DUPLICATE_TITLE", exception.getErrorCode());

        verify(questionRepository, never()).save(any());
    }

    @Test
    void updateQuestion_InvalidDifficulty() {
        // Given
        Question existingQuestion = new Question();
        existingQuestion.setId(1L);
        existingQuestion.setTitle("기존 제목");

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

        verify(questionRepository, never()).save(any());
    }

    @Test
    void updateQuestion_CategoryNotFound() {
        // Given
        Question existingQuestion = new Question();
        existingQuestion.setId(1L);
        existingQuestion.setTitle("기존 제목");

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

        verify(questionRepository, never()).save(any());
    }

    @Test
    void updateQuestion_CompanyNotFound() {
        // Given
        Question existingQuestion = new Question();
        existingQuestion.setId(1L);
        existingQuestion.setTitle("기존 제목");

        QuestionUpdateRequest request = new QuestionUpdateRequest();
        request.setCompanyId(999L);

        when(userRepository.findById(1L)).thenReturn(Optional.of(adminUser));
        when(questionRepository.findById(1L)).thenReturn(Optional.of(existingQuestion));
        when(companyRepository.existsById(999L)).thenReturn(false);

        // When & Then
        CsvProcessingException exception = assertThrows(CsvProcessingException.class, () -> {
            questionService.updateQuestion(1L, request, 1L);
        });

        assertEquals("연결하려는 회사를 찾을 수 없습니다.", exception.getMessage());
        assertEquals("COMPANY_NOT_FOUND", exception.getErrorCode());

        verify(questionRepository, never()).save(any());
    }

    @Test
    void updateQuestion_EmptyTitle() {
        // Given
        Question existingQuestion = new Question();
        existingQuestion.setId(1L);
        existingQuestion.setTitle("기존 제목");

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

        verify(questionRepository, never()).save(any());
    }

    @Test
    void updateQuestion_TooManyTags() {
        // Given
        Question existingQuestion = new Question();
        existingQuestion.setId(1L);
        existingQuestion.setTitle("기존 제목");

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

        verify(questionRepository, never()).save(any());
    }

    @Test
    void updateQuestion_TagTooLong() {
        // Given
        Question existingQuestion = new Question();
        existingQuestion.setId(1L);
        existingQuestion.setTitle("기존 제목");

        QuestionUpdateRequest request = new QuestionUpdateRequest();
        request.setTags(Arrays.asList("very-long-tag-that-exceeds-thirty-characters")); // 30자 초과

        when(userRepository.findById(1L)).thenReturn(Optional.of(adminUser));
        when(questionRepository.findById(1L)).thenReturn(Optional.of(existingQuestion));

        // When & Then
        CsvProcessingException exception = assertThrows(CsvProcessingException.class, () -> {
            questionService.updateQuestion(1L, request, 1L);
        });

        assertEquals("각 태그는 최대 30자까지 허용됩니다.", exception.getMessage());
        assertEquals("TAG_TOO_LONG", exception.getErrorCode());

        verify(questionRepository, never()).save(any());
    }

    @Test
    void updateQuestion_NoTitleChange_NoDuplicateCheck() {
        // Given
        Question existingQuestion = new Question();
        existingQuestion.setId(1L);
        existingQuestion.setTitle("기존 제목");
        existingQuestion.setContent("기존 내용");

        QuestionUpdateRequest request = new QuestionUpdateRequest();
        request.setContent("새로운 내용"); // 제목 변경 없음

        when(userRepository.findById(1L)).thenReturn(Optional.of(adminUser));
        when(questionRepository.findById(1L)).thenReturn(Optional.of(existingQuestion));
        when(questionRepository.save(any(Question.class))).thenAnswer(invocation -> invocation.getArgument(0));

        // When
        QuestionResponse response = questionService.updateQuestion(1L, request, 1L);

        // Then
        assertNotNull(response);
        assertEquals("기존 제목", response.getTitle()); // 변경되지 않음
        assertEquals("새로운 내용", response.getContent()); // 변경됨

        verify(questionRepository, never()).findByTitle(any()); // 중복 검사 안함
        verify(questionRepository).save(any(Question.class));
    }
}