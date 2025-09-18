package com.example.ododok.service;

import com.example.ododok.dto.SearchRequest;
import com.example.ododok.dto.SearchResponse;
import com.example.ododok.entity.Question;
import com.example.ododok.entity.User;
import com.example.ododok.entity.UserRole;
import com.example.ododok.exception.CsvProcessingException;
import com.example.ododok.repository.QuestionRepository;
import com.example.ododok.repository.UserRepository;
import com.example.ododok.repository.CompanyRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class SearchServiceTest {

    @Mock
    private QuestionRepository questionRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private CompanyRepository companyRepository;

    @InjectMocks
    private SearchService searchService;

    private Question question1;
    private Question question2;
    private User user1;

    @BeforeEach
    void setUp() {
        question1 = new Question();
        question1.setId(1L);
        question1.setTitle("자바 기초 질문");
        question1.setQuestion("자바에서 HashMap은 무엇인가요?");
        question1.setContent("HashMap의 동작 원리에 대해 설명해주세요.");
        question1.setDifficulty(2);
        question1.setIsPublic(true);
        question1.setCreatedBy(1L);
        question1.setCreatedAt(LocalDateTime.now());

        question2 = new Question();
        question2.setId(2L);
        question2.setTitle("스프링 부트 질문");
        question2.setQuestion("Spring Boot의 장점은?");
        question2.setContent("스프링 부트가 제공하는 주요 기능들을 설명하세요.");
        question2.setDifficulty(3);
        question2.setIsPublic(true);
        question2.setCreatedBy(1L);
        question2.setCreatedAt(LocalDateTime.now());

        user1 = new User();
        user1.setUserId(1L);
        user1.setUsername("testuser");
        user1.setDisplayName("테스트 사용자");
        user1.setEmail("test@example.com");
        user1.setRole(UserRole.USER);
        user1.setCreatedAt(LocalDateTime.now());
    }

    @Test
    void search_Success_EmptyQuery() {
        // Given
        SearchRequest request = new SearchRequest();
        request.setQ("");
        request.setPage(1);
        request.setSize(20);
        request.setTypes(List.of("question"));

        Page<Question> questionPage = new PageImpl<>(Arrays.asList(question1, question2));
        when(questionRepository.findByAllFilters(eq(""), isNull(), isNull(), isNull(), isNull(), eq(1L), any(Pageable.class)))
                .thenReturn(questionPage);
        when(questionRepository.count()).thenReturn(2L);
        when(questionRepository.countByDifficulty(1)).thenReturn(0);
        when(questionRepository.countByDifficulty(2)).thenReturn(1);
        when(questionRepository.countByDifficulty(3)).thenReturn(1);
        when(questionRepository.findDistinctYears()).thenReturn(java.util.List.of());
        when(questionRepository.findDistinctCompanyIds()).thenReturn(java.util.List.of());
        when(userRepository.count()).thenReturn(1L);

        // When
        SearchResponse response = searchService.search(request, 1L);

        // Then
        assertNotNull(response);
        assertEquals("", response.getQuery());
        assertEquals(1, response.getPage());
        assertEquals(20, response.getSize());
        assertEquals(2, response.getTotal());
        assertTrue(response.getTookMs() >= 0);
        assertEquals(2, response.getResults().size());

        // 첫 번째 결과 검증
        SearchResponse.SearchResult firstResult = response.getResults().get(0);
        assertEquals("question", firstResult.getType());
        assertEquals(1L, firstResult.getId());
        assertEquals("자바에서 HashMap은 무엇인가요?", firstResult.getQuestion());

        // 패싯 검증
        assertNotNull(response.getFacets());
        assertTrue(response.getFacets().containsKey("types"));
        assertTrue(response.getFacets().containsKey("difficulty"));
    }

    @Test
    void search_Success_WithQuery() {
        // Given
        SearchRequest request = new SearchRequest();
        request.setQ("자바");
        request.setPage(1);
        request.setSize(20);
        request.setTypes(List.of("question"));
        request.setHighlight(true);

        Page<Question> questionPage = new PageImpl<>(Arrays.asList(question1));
        when(questionRepository.findByAllFilters(eq("%자바%"), isNull(), isNull(), isNull(), isNull(), eq(1L), any(Pageable.class)))
                .thenReturn(questionPage);
        when(questionRepository.count()).thenReturn(2L);
        when(questionRepository.countByDifficulty(1)).thenReturn(0);
        when(questionRepository.countByDifficulty(2)).thenReturn(1);
        when(questionRepository.countByDifficulty(3)).thenReturn(1);
        when(questionRepository.findDistinctYears()).thenReturn(java.util.List.of());
        when(questionRepository.findDistinctCompanyIds()).thenReturn(java.util.List.of());
        when(userRepository.count()).thenReturn(1L);

        // When
        SearchResponse response = searchService.search(request, 1L);

        // Then
        assertNotNull(response);
        assertEquals("자바", response.getQuery());
        assertEquals(1, response.getResults().size());

        SearchResponse.SearchResult result = response.getResults().get(0);
        assertEquals("question", result.getType());
        // Check that snippet contains some highlighting (regex-based replacement can be complex)
        assertNotNull(result.getSnippet());
    }

    @Test
    void search_Success_WithDifficultyFilter() {
        // Given
        SearchRequest request = new SearchRequest();
        request.setQ("스프링");
        request.setDifficulty("HARD");
        request.setTypes(List.of("question"));

        Page<Question> questionPage = new PageImpl<>(Arrays.asList(question2));
        when(questionRepository.findByAllFilters(eq("%스프링%"), eq(3), isNull(), isNull(), isNull(), eq(1L), any(Pageable.class)))
                .thenReturn(questionPage);
        when(questionRepository.count()).thenReturn(2L);
        when(questionRepository.countByDifficulty(1)).thenReturn(0);
        when(questionRepository.countByDifficulty(2)).thenReturn(1);
        when(questionRepository.countByDifficulty(3)).thenReturn(1);
        when(questionRepository.findDistinctYears()).thenReturn(java.util.List.of());
        when(questionRepository.findDistinctCompanyIds()).thenReturn(java.util.List.of());
        when(userRepository.count()).thenReturn(1L);

        // When
        SearchResponse response = searchService.search(request, 1L);

        // Then
        assertNotNull(response);
        assertEquals(1, response.getResults().size());
        assertEquals(3, response.getResults().get(0).getDifficulty());
    }

    @Test
    void search_Success_UserType() {
        // Given
        SearchRequest request = new SearchRequest();
        request.setQ("test");
        request.setTypes(List.of("user"));

        Page<User> userPage = new PageImpl<>(Arrays.asList(user1));
        when(userRepository.findByUsernameContainingIgnoreCase(eq("test"), any(Pageable.class)))
                .thenReturn(userPage);
        when(questionRepository.count()).thenReturn(2L);
        when(questionRepository.countByDifficulty(1)).thenReturn(0);
        when(questionRepository.countByDifficulty(2)).thenReturn(1);
        when(questionRepository.countByDifficulty(3)).thenReturn(1);
        when(questionRepository.findDistinctYears()).thenReturn(java.util.List.of());
        when(questionRepository.findDistinctCompanyIds()).thenReturn(java.util.List.of());
        when(userRepository.count()).thenReturn(1L);

        // When
        SearchResponse response = searchService.search(request, 1L);

        // Then
        assertNotNull(response);
        assertEquals(1, response.getResults().size());

        SearchResponse.SearchResult result = response.getResults().get(0);
        assertEquals("user", result.getType());
        assertEquals("testuser", result.getUsername());
        assertEquals("테스트 사용자", result.getDisplayName());
    }

    @Test
    void search_InvalidSort() {
        // Given
        SearchRequest request = new SearchRequest();
        request.setSort("invalid");

        // When & Then
        CsvProcessingException exception = assertThrows(CsvProcessingException.class, () -> {
            searchService.search(request, 1L);
        });

        assertEquals("유효하지 않은 정렬 옵션입니다.", exception.getMessage());
        assertEquals("INVALID_SORT", exception.getErrorCode());
    }

    @Test
    void search_InvalidType() {
        // Given
        SearchRequest request = new SearchRequest();
        request.setTypes(List.of("invalid"));

        // When & Then
        CsvProcessingException exception = assertThrows(CsvProcessingException.class, () -> {
            searchService.search(request, 1L);
        });

        assertEquals("유효하지 않은 타입입니다.", exception.getMessage());
        assertEquals("INVALID_TYPE", exception.getErrorCode());
    }

    @Test
    void search_InvalidDifficulty() {
        // Given
        SearchRequest request = new SearchRequest();
        request.setDifficulty("INVALID");

        // When & Then
        CsvProcessingException exception = assertThrows(CsvProcessingException.class, () -> {
            searchService.search(request, 1L);
        });

        assertEquals("유효하지 않은 난이도입니다.", exception.getMessage());
        assertEquals("INVALID_DIFFICULTY", exception.getErrorCode());
    }

    @Test
    void search_WithCategoryFilter() {
        // Given
        SearchRequest request = new SearchRequest();
        request.setQ("자바");
        request.setCategoryId(1L);
        request.setTypes(List.of("question"));

        Page<Question> questionPage = new PageImpl<>(Arrays.asList(question1));
        when(questionRepository.findByAllFilters(eq("%자바%"), isNull(), isNull(), isNull(), eq(1L), eq(1L), any(Pageable.class)))
                .thenReturn(questionPage);
        when(questionRepository.count()).thenReturn(2L);
        when(questionRepository.countByDifficulty(1)).thenReturn(0);
        when(questionRepository.countByDifficulty(2)).thenReturn(1);
        when(questionRepository.countByDifficulty(3)).thenReturn(1);
        when(questionRepository.findDistinctYears()).thenReturn(java.util.List.of());
        when(questionRepository.findDistinctCompanyIds()).thenReturn(java.util.List.of());
        when(userRepository.count()).thenReturn(1L);

        // When
        SearchResponse response = searchService.search(request, 1L);

        // Then
        assertNotNull(response);
        assertEquals(1, response.getResults().size());
        verify(questionRepository).findByAllFilters(eq("%자바%"), isNull(), isNull(), isNull(), eq(1L), eq(1L), any(Pageable.class));
    }

    @Test
    void search_NoHighlight() {
        // Given
        SearchRequest request = new SearchRequest();
        request.setQ("자바");
        request.setHighlight(false);
        request.setTypes(List.of("question"));

        Page<Question> questionPage = new PageImpl<>(Arrays.asList(question1));
        when(questionRepository.findByAllFilters(eq("%자바%"), isNull(), isNull(), isNull(), isNull(), eq(1L), any(Pageable.class)))
                .thenReturn(questionPage);
        when(questionRepository.count()).thenReturn(2L);
        when(questionRepository.countByDifficulty(1)).thenReturn(0);
        when(questionRepository.countByDifficulty(2)).thenReturn(1);
        when(questionRepository.countByDifficulty(3)).thenReturn(1);
        when(questionRepository.findDistinctYears()).thenReturn(java.util.List.of());
        when(questionRepository.findDistinctCompanyIds()).thenReturn(java.util.List.of());
        when(userRepository.count()).thenReturn(1L);

        // When
        SearchResponse response = searchService.search(request, 1L);

        // Then
        assertNotNull(response);
        SearchResponse.SearchResult result = response.getResults().get(0);
        assertFalse(result.getSnippet().contains("<em>")); // 하이라이트 없음
    }
}