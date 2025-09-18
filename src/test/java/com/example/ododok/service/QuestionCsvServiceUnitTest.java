package com.example.ododok.service;

import com.example.ododok.dto.CsvUploadResponse;
import com.example.ododok.entity.Company;
import com.example.ododok.entity.Category;
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
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class QuestionCsvServiceUnitTest {

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
    }

    @Test
    void processCsvFile_Success_BasicFlow() throws Exception {
        // Given
        String csvContent = "question,difficulty,year\n\"테스트 질문\",EASY,2024";
        MockMultipartFile file = new MockMultipartFile("file", "test.csv", "text/csv", csvContent.getBytes());

        when(userRepository.findById(1L)).thenReturn(Optional.of(adminUser));
        when(questionRepository.findByQuestion(anyString())).thenReturn(Optional.empty());
        when(questionRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

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

        verify(questionRepository).save(any());
    }

    @Test
    void processCsvFile_AccessDenied_NonAdminUser() {
        // Given
        String csvContent = "question,difficulty,year\n\"테스트 질문\",EASY,2024";
        MockMultipartFile file = new MockMultipartFile("file", "test.csv", "text/csv", csvContent.getBytes());

        when(userRepository.findById(2L)).thenReturn(Optional.of(regularUser));

        // When & Then
        assertThrows(AccessDeniedException.class, () -> {
            questionCsvService.processCsvFile(file, false, 2L);
        });

        verify(questionRepository, never()).save(any());
    }

    @Test
    void processCsvFile_UserNotFound() {
        // Given
        String csvContent = "question,difficulty,year\n\"테스트 질문\",EASY,2024";
        MockMultipartFile file = new MockMultipartFile("file", "test.csv", "text/csv", csvContent.getBytes());

        when(userRepository.findById(999L)).thenReturn(Optional.empty());

        // When & Then
        CsvProcessingException exception = assertThrows(CsvProcessingException.class, () -> {
            questionCsvService.processCsvFile(file, false, 999L);
        });

        assertEquals("사용자를 찾을 수 없습니다.", exception.getMessage());
        assertEquals("USER_NOT_FOUND", exception.getErrorCode());
    }

    @Test
    void processCsvFile_EmptyFile() {
        // Given
        MockMultipartFile file = new MockMultipartFile("file", "test.csv", "text/csv", new byte[0]);

        when(userRepository.findById(1L)).thenReturn(Optional.of(adminUser));

        // When & Then
        CsvProcessingException exception = assertThrows(CsvProcessingException.class, () -> {
            questionCsvService.processCsvFile(file, false, 1L);
        });

        assertEquals("업로드된 파일이 비어있습니다.", exception.getMessage());
        assertEquals("EMPTY_FILE", exception.getErrorCode());
    }

    @Test
    void processCsvFile_InvalidContentType() {
        // Given
        MockMultipartFile file = new MockMultipartFile("file", "test.txt", "text/plain", "content".getBytes());

        when(userRepository.findById(1L)).thenReturn(Optional.of(adminUser));

        // When & Then
        CsvProcessingException exception = assertThrows(CsvProcessingException.class, () -> {
            questionCsvService.processCsvFile(file, false, 1L);
        });

        assertEquals("CSV만 허용됩니다.", exception.getMessage());
        assertEquals("INVALID_CONTENT_TYPE", exception.getErrorCode());
    }

    @Test
    void processCsvFile_InvalidHeaders() {
        // Given
        String csvContent = "invalid,headers\n\"test\",\"data\"";
        MockMultipartFile file = new MockMultipartFile("file", "test.csv", "text/csv", csvContent.getBytes());

        when(userRepository.findById(1L)).thenReturn(Optional.of(adminUser));

        // When & Then
        CsvProcessingException exception = assertThrows(CsvProcessingException.class, () -> {
            questionCsvService.processCsvFile(file, false, 1L);
        });

        assertEquals("CSV 헤더가 사양과 일치하지 않습니다.", exception.getMessage());
        assertEquals("HEADER_MISMATCH", exception.getErrorCode());
    }

    @Test
    void processCsvFile_TooManyRows() {
        // Given
        ReflectionTestUtils.setField(questionCsvService, "maxRows", 2);

        StringBuilder csvContent = new StringBuilder("question,difficulty,year\n");
        for (int i = 1; i <= 3; i++) {
            csvContent.append("\"질문 ").append(i).append("\",EASY,2024\n");
        }

        MockMultipartFile file = new MockMultipartFile("file", "test.csv", "text/csv", csvContent.toString().getBytes());

        when(userRepository.findById(1L)).thenReturn(Optional.of(adminUser));

        // When & Then
        CsvProcessingException exception = assertThrows(CsvProcessingException.class, () -> {
            questionCsvService.processCsvFile(file, false, 1L);
        });

        assertEquals("TOO_MANY_ROWS", exception.getErrorCode());
        assertTrue(exception.getMessage().contains("최대 2행까지 허용됩니다"));
    }

    @Test
    void processCsvFile_DryRun() throws Exception {
        // Given
        String csvContent = "question,difficulty,year\n\"드라이런 테스트\",EASY,2024";
        MockMultipartFile file = new MockMultipartFile("file", "test.csv", "text/csv", csvContent.getBytes());

        when(userRepository.findById(1L)).thenReturn(Optional.of(adminUser));

        // When
        CsvUploadResponse response = questionCsvService.processCsvFile(file, true, 1L);

        // Then
        assertNotNull(response);
        assertEquals(1, response.getSummary().getTotalRows());
        assertEquals(0, response.getSummary().getCreated());
        assertEquals(0, response.getSummary().getUpdated());
        assertEquals(0, response.getSummary().getSkipped());
        assertTrue(response.getSummary().isDryRun());
        assertTrue(response.getErrors().isEmpty());

        verify(questionRepository, never()).save(any());
    }

    @Test
    void processCsvFile_ValidationErrors() throws Exception {
        // Given
        String csvContent = "question,difficulty,year\n" +
                ",INVALID_DIFFICULTY,abc\n" +  // Empty question, invalid difficulty, invalid year
                "\"" + "A".repeat(201) + "\",EASY,2024"; // Too long question

        MockMultipartFile file = new MockMultipartFile("file", "test.csv", "text/csv", csvContent.getBytes());

        when(userRepository.findById(1L)).thenReturn(Optional.of(adminUser));

        // When
        CsvUploadResponse response = questionCsvService.processCsvFile(file, false, 1L);

        // Then
        assertNotNull(response);
        assertEquals(2, response.getSummary().getTotalRows());
        assertEquals(0, response.getSummary().getCreated());
        assertEquals(0, response.getSummary().getUpdated());
        assertEquals(2, response.getSummary().getSkipped());
        assertFalse(response.getErrors().isEmpty());

        // Check first error (empty question)
        assertEquals("REQUIRED_FIELD_MISSING", response.getErrors().get(0).getCode());
        assertEquals("question", response.getErrors().get(0).getField());
        assertEquals(2, response.getErrors().get(0).getRow());

        verify(questionRepository, never()).save(any());
    }

    @Test
    void processCsvFile_UpdateExistingQuestion() throws Exception {
        // Given
        String csvContent = "question,difficulty,year\n\"기존 질문\",HARD,2024";
        MockMultipartFile file = new MockMultipartFile("file", "test.csv", "text/csv", csvContent.getBytes());

        com.example.ododok.entity.Question existingQuestion = new com.example.ododok.entity.Question();
        existingQuestion.setId(1L);
        existingQuestion.setQuestion("기존 질문");
        existingQuestion.setDifficulty(1);

        when(userRepository.findById(1L)).thenReturn(Optional.of(adminUser));
        when(questionRepository.findByQuestion("기존 질문")).thenReturn(Optional.of(existingQuestion));
        when(questionRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        // When
        CsvUploadResponse response = questionCsvService.processCsvFile(file, false, 1L);

        // Then
        assertNotNull(response);
        assertEquals(1, response.getSummary().getTotalRows());
        assertEquals(0, response.getSummary().getCreated());
        assertEquals(1, response.getSummary().getUpdated());
        assertEquals(0, response.getSummary().getSkipped());

        verify(questionRepository).save(any());
    }

    @Test
    void generateSampleCsv_ValidFormat() {
        // When
        String sampleCsv = questionCsvService.generateSampleCsv();

        // Then
        assertNotNull(sampleCsv);
        assertTrue(sampleCsv.startsWith("question,difficulty,year,company_name,category_name"));
        assertTrue(sampleCsv.contains("자바에서 HashMap과 TreeMap의 차이점은 무엇인가요?"));
        assertTrue(sampleCsv.contains("MEDIUM"));
        assertTrue(sampleCsv.contains("네이버"));
        assertTrue(sampleCsv.contains("자료구조"));

        // Check number of lines (header + 3 sample rows)
        String[] lines = sampleCsv.split("\n");
        assertEquals(4, lines.length);
    }
}