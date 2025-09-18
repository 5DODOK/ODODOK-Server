package com.example.ododok.controller;

import com.example.ododok.exception.CsvProcessingException;
import com.example.ododok.service.JwtService;
import com.example.ododok.service.QuestionService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class QuestionControllerDeleteTest {

    @Mock
    private QuestionService questionService;

    @Mock
    private JwtService jwtService;

    @InjectMocks
    private QuestionController questionController;

    @Test
    void deleteQuestion_Success() {
        // Given
        when(jwtService.extractUserId("valid-token")).thenReturn(1L);
        doNothing().when(questionService).deleteQuestion(1L, 1L);

        // When
        ResponseEntity<Void> response = questionController.deleteQuestion(1L, "Bearer valid-token");

        // Then
        assertEquals(204, response.getStatusCodeValue());
        assertNull(response.getBody());
        verify(questionService).deleteQuestion(1L, 1L);
    }

    @Test
    void deleteQuestion_AccessDenied() {
        // Given
        when(jwtService.extractUserId("valid-token")).thenReturn(2L);
        doThrow(new AccessDeniedException("이 작업을 수행할 권한이 없습니다."))
                .when(questionService).deleteQuestion(1L, 2L);

        // When & Then
        assertThrows(AccessDeniedException.class, () -> {
            questionController.deleteQuestion(1L, "Bearer valid-token");
        });

        verify(questionService).deleteQuestion(1L, 2L);
    }

    @Test
    void deleteQuestion_QuestionNotFound() {
        // Given
        when(jwtService.extractUserId("valid-token")).thenReturn(1L);
        doThrow(new CsvProcessingException("대상을 찾을 수 없습니다.", "QUESTION_NOT_FOUND"))
                .when(questionService).deleteQuestion(999L, 1L);

        // When & Then
        CsvProcessingException exception = assertThrows(CsvProcessingException.class, () -> {
            questionController.deleteQuestion(999L, "Bearer valid-token");
        });

        assertEquals("대상을 찾을 수 없습니다.", exception.getMessage());
        assertEquals("QUESTION_NOT_FOUND", exception.getErrorCode());
        verify(questionService).deleteQuestion(999L, 1L);
    }

    @Test
    void deleteQuestion_UserNotFound() {
        // Given
        when(jwtService.extractUserId("valid-token")).thenReturn(999L);
        doThrow(new CsvProcessingException("사용자를 찾을 수 없습니다.", "USER_NOT_FOUND"))
                .when(questionService).deleteQuestion(1L, 999L);

        // When & Then
        CsvProcessingException exception = assertThrows(CsvProcessingException.class, () -> {
            questionController.deleteQuestion(1L, "Bearer valid-token");
        });

        assertEquals("사용자를 찾을 수 없습니다.", exception.getMessage());
        assertEquals("USER_NOT_FOUND", exception.getErrorCode());
        verify(questionService).deleteQuestion(1L, 999L);
    }

    @Test
    void deleteQuestion_MissingAuthHeader() {
        // When & Then
        RuntimeException exception = assertThrows(RuntimeException.class, () -> {
            questionController.deleteQuestion(1L, null);
        });

        assertEquals("Authorization 헤더가 필요합니다.", exception.getMessage());
        verify(questionService, never()).deleteQuestion(any(), any());
    }

    @Test
    void deleteQuestion_InvalidAuthHeader() {
        // When & Then
        RuntimeException exception = assertThrows(RuntimeException.class, () -> {
            questionController.deleteQuestion(1L, "Invalid-header");
        });

        assertEquals("Authorization 헤더가 필요합니다.", exception.getMessage());
        verify(questionService, never()).deleteQuestion(any(), any());
    }

    @Test
    void deleteQuestion_IdempotentBehavior() {
        // Given - Multiple calls to delete the same resource
        when(jwtService.extractUserId("valid-token")).thenReturn(1L);
        doNothing().when(questionService).deleteQuestion(1L, 1L);

        // When - Call delete twice
        ResponseEntity<Void> response1 = questionController.deleteQuestion(1L, "Bearer valid-token");
        ResponseEntity<Void> response2 = questionController.deleteQuestion(1L, "Bearer valid-token");

        // Then - Both should return 204
        assertEquals(204, response1.getStatusCodeValue());
        assertEquals(204, response2.getStatusCodeValue());
        verify(questionService, times(2)).deleteQuestion(1L, 1L);
    }
}