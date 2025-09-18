package com.example.ododok.controller;

import com.example.ododok.dto.QuestionCreateRequest;
import com.example.ododok.dto.QuestionResponse;
import com.example.ododok.exception.CsvProcessingException;
import com.example.ododok.service.JwtService;
import com.example.ododok.service.QuestionService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.time.LocalDateTime;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@ExtendWith(MockitoExtension.class)
class QuestionControllerTest {

    private MockMvc mockMvc;

    @Mock
    private QuestionService questionService;

    @Mock
    private JwtService jwtService;

    @InjectMocks
    private QuestionController questionController;

    private ObjectMapper objectMapper;

    private QuestionCreateRequest validRequest;
    private QuestionResponse validResponse;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper();
        mockMvc = MockMvcBuilders.standaloneSetup(questionController).build();

        validRequest = new QuestionCreateRequest();
        validRequest.setQuestion("테스트 질문");
        validRequest.setDifficulty("MEDIUM");

        validResponse = new QuestionResponse(
                1L,
                "테스트 질문",
                2,
                null,
                null,
                null,
                LocalDateTime.now(),
                1L
        );
    }

    @Test
    void createQuestion_Success() throws Exception {
        // Given
        when(jwtService.extractUserId("valid-token")).thenReturn(1L);
        when(questionService.createQuestion(any(QuestionCreateRequest.class), eq(1L)))
                .thenReturn(validResponse);

        // When & Then
        mockMvc.perform(post("/question")
                .header("Authorization", "Bearer valid-token")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(validRequest)))
                .andExpect(status().isCreated())
                .andExpect(header().string("Location", "/question/1"))
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.question").value("테스트 질문"))
                .andExpect(jsonPath("$.difficulty").value(2));

        verify(questionService).createQuestion(any(QuestionCreateRequest.class), eq(1L));
    }


    @Test
    void createQuestion_AccessDenied() throws Exception {
        // Given
        when(jwtService.extractUserId("valid-token")).thenReturn(1L);
        when(questionService.createQuestion(any(QuestionCreateRequest.class), eq(1L)))
                .thenThrow(new AccessDeniedException("이 작업을 수행할 권한이 없습니다."));

        // When & Then
        mockMvc.perform(post("/question")
                .header("Authorization", "Bearer valid-token")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(validRequest)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("ACCESS_DENIED"))
                .andExpect(jsonPath("$.message").value("이 작업을 수행할 권한이 없습니다."));
    }

    @Test
    void createQuestion_MissingAuthHeader() throws Exception {
        // When & Then
        mockMvc.perform(post("/question")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(validRequest)))
                .andExpect(status().isInternalServerError());

        verify(questionService, never()).createQuestion(any(), any());
    }

    @Test
    void deleteQuestion_Success() throws Exception {
        // Given
        when(jwtService.extractUserId("valid-token")).thenReturn(1L);
        doNothing().when(questionService).deleteQuestion(1L, 1L);

        // When & Then
        mockMvc.perform(delete("/question/1")
                .header("Authorization", "Bearer valid-token"))
                .andExpect(status().isNoContent());

        verify(questionService).deleteQuestion(1L, 1L);
    }

    @Test
    void deleteQuestion_AccessDenied() throws Exception {
        // Given
        when(jwtService.extractUserId("valid-token")).thenReturn(2L);
        doThrow(new AccessDeniedException("이 작업을 수행할 권한이 없습니다."))
                .when(questionService).deleteQuestion(1L, 2L);

        // When & Then
        mockMvc.perform(delete("/question/1")
                .header("Authorization", "Bearer valid-token"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("ACCESS_DENIED"))
                .andExpect(jsonPath("$.message").value("이 작업을 수행할 권한이 없습니다."));
    }

    @Test
    void deleteQuestion_QuestionNotFound() throws Exception {
        // Given
        when(jwtService.extractUserId("valid-token")).thenReturn(1L);
        doThrow(new CsvProcessingException("대상을 찾을 수 없습니다.", "QUESTION_NOT_FOUND"))
                .when(questionService).deleteQuestion(999L, 1L);

        // When & Then
        mockMvc.perform(delete("/question/999")
                .header("Authorization", "Bearer valid-token"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("QUESTION_NOT_FOUND"))
                .andExpect(jsonPath("$.message").value("대상을 찾을 수 없습니다."));
    }

    @Test
    void deleteQuestion_UserNotFound() throws Exception {
        // Given
        when(jwtService.extractUserId("valid-token")).thenReturn(999L);
        doThrow(new CsvProcessingException("사용자를 찾을 수 없습니다.", "USER_NOT_FOUND"))
                .when(questionService).deleteQuestion(1L, 999L);

        // When & Then
        mockMvc.perform(delete("/question/1")
                .header("Authorization", "Bearer valid-token"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("USER_NOT_FOUND"))
                .andExpect(jsonPath("$.message").value("사용자를 찾을 수 없습니다."));
    }

    @Test
    void deleteQuestion_MissingAuthHeader() throws Exception {
        // When & Then
        mockMvc.perform(delete("/question/1"))
                .andExpect(status().isInternalServerError());

        verify(questionService, never()).deleteQuestion(any(), any());
    }

    @Test
    void deleteQuestion_InvalidIdFormat() throws Exception {
        // Given
        when(jwtService.extractUserId("valid-token")).thenReturn(1L);

        // When & Then
        mockMvc.perform(delete("/question/invalid-id")
                .header("Authorization", "Bearer valid-token"))
                .andExpect(status().isBadRequest());

        verify(questionService, never()).deleteQuestion(any(), any());
    }

    @Test
    void deleteQuestion_InvalidAuthHeader() throws Exception {
        // When & Then
        mockMvc.perform(delete("/question/1")
                .header("Authorization", "Invalid-header"))
                .andExpect(status().isInternalServerError());

        verify(questionService, never()).deleteQuestion(any(), any());
    }
}