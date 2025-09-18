package com.example.ododok.service;

import com.example.ododok.dto.CsvUploadResponse;
import com.example.ododok.entity.*;
import com.example.ododok.exception.CsvProcessingException;
import com.example.ododok.repository.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class QuestionCsvServiceTest {

    @Mock
    private QuestionRepository questionRepository;

    @Mock
    private CompanyRepository companyRepository;

    @Mock
    private CategoryRepository categoryRepository;

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private QuestionCsvService questionCsvService;

    private User adminUser;
    private User regularUser;
    private Company company;
    private Category category;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(questionCsvService, "maxFileSize", 5242880L);
        ReflectionTestUtils.setField(questionCsvService, "maxRows", 1000);
        ReflectionTestUtils.setField(questionCsvService, "upsertKey", "question");

        adminUser = new User();
        adminUser.setUserId(1L);
        adminUser.setRole(UserRole.ADMIN);

        regularUser = new User();
        regularUser.setUserId(2L);
        regularUser.setRole(UserRole.USER);

        company = new Company();
        company.setId(1L);
        company.setName("네이버");

        category = new Category();
        category.setId(1L);
        category.setName("자료구조");
    }

    @Test
    void processCsvFile_Success_WithValidData() throws Exception {
        // Given
        String csvContent = "question,difficulty,year\n" +
                "\"자바에서 HashMap과 TreeMap의 차이점은?\",MEDIUM,2024";

        MockMultipartFile file = new MockMultipartFile(
                "file", "test.csv", "text/csv", csvContent.getBytes()
        );

        when(userRepository.findById(1L)).thenReturn(Optional.of(adminUser));
        when(questionRepository.findByQuestion(anyString())).thenReturn(Optional.empty());
        when(questionRepository.save(any(Question.class))).thenAnswer(invocation -> invocation.getArgument(0));

        // When
        CsvUploadResponse response = questionCsvService.processCsvFile(file, false, 1L);

        // Then
        assertNotNull(response);
        assertEquals(1, response.getSummary().getTotalRows());
        assertEquals(1, response.getSummary().getCreated());
        assertEquals(0, response.getSummary().getUpdated());
        assertEquals(0, response.getSummary().getSkipped());
        assertFalse(response.getSummary().isDryRun());
        assertTrue(response.getErrors().isEmpty());

        verify(questionRepository).save(any(Question.class));
    }

    @Test
    void processCsvFile_DryRun_ShouldNotSaveData() throws Exception {
        // Given
        String csvContent = "question,difficulty,year\n" +
                "\"자바에서 HashMap과 TreeMap의 차이점은?\",MEDIUM,2024";

        MockMultipartFile file = new MockMultipartFile(
                "file", "test.csv", "text/csv", csvContent.getBytes()
        );

        when(userRepository.findById(1L)).thenReturn(Optional.of(adminUser));

        // When
        CsvUploadResponse response = questionCsvService.processCsvFile(file, true, 1L);

        // Then
        assertNotNull(response);
        assertEquals(1, response.getSummary().getTotalRows());
        assertEquals(0, response.getSummary().getCreated());
        assertEquals(0, response.getSummary().getUpdated());
        assertTrue(response.getSummary().isDryRun());

        verify(questionRepository, never()).save(any(Question.class));
    }

    @Test
    void processCsvFile_AccessDenied_WhenUserIsNotAdmin() {
        // Given
        String csvContent = "question,difficulty,year\n\"Test question\",EASY,2024";
        MockMultipartFile file = new MockMultipartFile(
                "file", "test.csv", "text/csv", csvContent.getBytes()
        );

        when(userRepository.findById(2L)).thenReturn(Optional.of(regularUser));

        // When & Then
        assertThrows(AccessDeniedException.class, () -> {
            questionCsvService.processCsvFile(file, false, 2L);
        });
    }

    @Test
    void processCsvFile_ThrowsException_WhenUserNotFound() {
        // Given
        String csvContent = "question,difficulty,year\n\"Test question\",EASY,2024";
        MockMultipartFile file = new MockMultipartFile(
                "file", "test.csv", "text/csv", csvContent.getBytes()
        );

        when(userRepository.findById(999L)).thenReturn(Optional.empty());

        // When & Then
        CsvProcessingException exception = assertThrows(CsvProcessingException.class, () -> {
            questionCsvService.processCsvFile(file, false, 999L);
        });

        assertEquals("사용자를 찾을 수 없습니다.", exception.getMessage());
        assertEquals("USER_NOT_FOUND", exception.getErrorCode());
    }

    @Test
    void processCsvFile_ThrowsException_WhenFileIsEmpty() {
        // Given
        MockMultipartFile file = new MockMultipartFile(
                "file", "test.csv", "text/csv", new byte[0]
        );

        when(userRepository.findById(1L)).thenReturn(Optional.of(adminUser));

        // When & Then
        CsvProcessingException exception = assertThrows(CsvProcessingException.class, () -> {
            questionCsvService.processCsvFile(file, false, 1L);
        });

        assertEquals("업로드된 파일이 비어있습니다.", exception.getMessage());
        assertEquals("EMPTY_FILE", exception.getErrorCode());
    }

    @Test
    void processCsvFile_ThrowsException_WhenInvalidContentType() {
        // Given
        MockMultipartFile file = new MockMultipartFile(
                "file", "test.txt", "text/plain", "content".getBytes()
        );

        when(userRepository.findById(1L)).thenReturn(Optional.of(adminUser));

        // When & Then
        CsvProcessingException exception = assertThrows(CsvProcessingException.class, () -> {
            questionCsvService.processCsvFile(file, false, 1L);
        });

        assertEquals("CSV만 허용됩니다.", exception.getMessage());
        assertEquals("INVALID_CONTENT_TYPE", exception.getErrorCode());
    }

    @Test
    void processCsvFile_ValidationErrors_WithInvalidData() throws Exception {
        // Given
        String csvContent = "question,difficulty,year\n" +
                ",INVALID_DIFFICULTY,abc";

        MockMultipartFile file = new MockMultipartFile(
                "file", "test.csv", "text/csv", csvContent.getBytes()
        );

        when(userRepository.findById(1L)).thenReturn(Optional.of(adminUser));

        // When
        CsvUploadResponse response = questionCsvService.processCsvFile(file, false, 1L);

        // Then
        assertNotNull(response);
        assertEquals(1, response.getSummary().getTotalRows());
        assertEquals(0, response.getSummary().getCreated());
        assertEquals(1, response.getSummary().getSkipped());
        assertFalse(response.getErrors().isEmpty());

        // Should have validation errors for missing question, invalid difficulty, etc.
        assertTrue(response.getErrors().stream()
                .anyMatch(error -> "REQUIRED_FIELD_MISSING".equals(error.getCode())));
    }

    @Test
    void processCsvFile_Update_WhenQuestionExists() throws Exception {
        // Given
        String csvContent = "question,difficulty,year\n" +
                "\"Existing question\",HARD,2024";

        MockMultipartFile file = new MockMultipartFile(
                "file", "test.csv", "text/csv", csvContent.getBytes()
        );

        Question existingQuestion = new Question();
        existingQuestion.setId(1L);
        existingQuestion.setQuestion("Existing question");
        existingQuestion.setDifficulty(1);

        when(userRepository.findById(1L)).thenReturn(Optional.of(adminUser));
        when(questionRepository.findByQuestion("Existing question")).thenReturn(Optional.of(existingQuestion));
        when(questionRepository.save(any(Question.class))).thenAnswer(invocation -> invocation.getArgument(0));

        // When
        CsvUploadResponse response = questionCsvService.processCsvFile(file, false, 1L);

        // Then
        assertNotNull(response);
        assertEquals(1, response.getSummary().getTotalRows());
        assertEquals(0, response.getSummary().getCreated());
        assertEquals(1, response.getSummary().getUpdated());
        assertEquals(0, response.getSummary().getSkipped());

        verify(questionRepository).save(any(Question.class));
    }

    @Test
    void generateSampleCsv_ShouldReturnValidCsvFormat() {
        // When
        String sampleCsv = questionCsvService.generateSampleCsv();

        // Then
        assertNotNull(sampleCsv);
        assertTrue(sampleCsv.contains("question,difficulty,year,company_name,category_name"));
        assertTrue(sampleCsv.contains("자바에서 HashMap과 TreeMap의 차이점은 무엇인가요?"));
        assertTrue(sampleCsv.contains("MEDIUM"));
        assertTrue(sampleCsv.contains("네이버"));
    }
}