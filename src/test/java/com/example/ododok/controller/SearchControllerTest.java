package com.example.ododok.controller;

import com.example.ododok.dto.SearchResponse;
import com.example.ododok.service.JwtService;
import com.example.ododok.service.SearchService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.ResponseEntity;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class SearchControllerTest {

    @Mock
    private SearchService searchService;

    @Mock
    private JwtService jwtService;

    @InjectMocks
    private SearchController searchController;

    private SearchResponse mockResponse;

    @BeforeEach
    void setUp() {
        // Mock search response
        SearchResponse.SearchResult result1 = new SearchResponse.SearchResult();
        result1.setType("question");
        result1.setId(1L);
        result1.setQuestion("자바에서 HashMap은 무엇인가요?");
        result1.setSnippet("HashMap의 동작 원리에 대해 설명해주세요.");
        result1.setDifficulty(2);
        result1.setScore(12.34);
        result1.setCreatedAt(LocalDateTime.now());

        SearchResponse.SearchResult result2 = new SearchResponse.SearchResult();
        result2.setType("user");
        result2.setId(2L);
        result2.setUsername("testuser");
        result2.setDisplayName("테스트 사용자");
        result2.setSnippet("테스트 사용자");
        result2.setScore(5.67);
        result2.setCreatedAt(LocalDateTime.now());

        Map<String, Map<String, Integer>> facets = new HashMap<>();
        Map<String, Integer> typeFacets = new HashMap<>();
        typeFacets.put("question", 150);
        typeFacets.put("user", 84);
        facets.put("types", typeFacets);

        Map<String, Integer> difficultyFacets = new HashMap<>();
        difficultyFacets.put("EASY", 40);
        difficultyFacets.put("MEDIUM", 85);
        difficultyFacets.put("HARD", 25);
        facets.put("difficulty", difficultyFacets);

        mockResponse = new SearchResponse(
                "자바",
                1,
                20,
                234L,
                17L,
                facets,
                Arrays.asList(result1, result2)
        );
    }

    @Test
    void search_Success_BasicQuery() {
        // Given
        when(jwtService.extractUserId("valid-token")).thenReturn(1L);
        when(searchService.search(any(), eq(1L))).thenReturn(mockResponse);

        // When
        ResponseEntity<SearchResponse> response = searchController.search(
                "자바", 1, 20, "rel", "question,user", null, null, true,
                "Bearer valid-token"
        );

        // Then
        assertEquals(200, response.getStatusCodeValue());
        assertNotNull(response.getBody());
        assertEquals("자바", response.getBody().getQuery());
        assertEquals(1, response.getBody().getPage());
        assertEquals(20, response.getBody().getSize());
        assertEquals(234L, response.getBody().getTotal());
        assertEquals(2, response.getBody().getResults().size());
        assertNotNull(response.getBody().getFacets());

        verify(searchService).search(any(), eq(1L));
    }

    @Test
    void search_Success_EmptyQuery() {
        // Given
        SearchResponse emptyQueryResponse = new SearchResponse(
                "",
                1,
                20,
                500L,
                12L,
                mockResponse.getFacets(),
                mockResponse.getResults()
        );

        when(jwtService.extractUserId("valid-token")).thenReturn(1L);
        when(searchService.search(any(), eq(1L))).thenReturn(emptyQueryResponse);

        // When
        ResponseEntity<SearchResponse> response = searchController.search(
                "", 1, 20, "new", "question", null, null, true,
                "Bearer valid-token"
        );

        // Then
        assertEquals(200, response.getStatusCodeValue());
        assertEquals("", response.getBody().getQuery());
        assertEquals("question", response.getBody().getResults().get(0).getType()); // Verify type is correct
    }

    @Test
    void search_Success_WithAllFilters() {
        // Given
        when(jwtService.extractUserId("valid-token")).thenReturn(1L);
        when(searchService.search(any(), eq(1L))).thenReturn(mockResponse);

        // When
        ResponseEntity<SearchResponse> response = searchController.search(
                "스프링", 2, 10, "old", "question", "HARD", 5L, false,
                "Bearer valid-token"
        );

        // Then
        assertEquals(200, response.getStatusCodeValue());

        // Verify that request was properly constructed
        verify(searchService).search(argThat(request ->
            "스프링".equals(request.getQ()) &&
            request.getPage() == 2 &&
            request.getSize() == 10 &&
            "old".equals(request.getSort()) &&
            request.getTypes().contains("question") &&
            "HARD".equals(request.getDifficulty()) &&
            request.getCategoryId().equals(5L) &&
            !request.isHighlight()
        ), eq(1L));
    }

    @Test
    void search_Success_MultipleTypes() {
        // Given
        when(jwtService.extractUserId("valid-token")).thenReturn(1L);
        when(searchService.search(any(), eq(1L))).thenReturn(mockResponse);

        // When
        ResponseEntity<SearchResponse> response = searchController.search(
                "테스트", 1, 20, "rel", "question,user", null, null, true,
                "Bearer valid-token"
        );

        // Then
        assertEquals(200, response.getStatusCodeValue());

        verify(searchService).search(argThat(request ->
            request.getTypes().contains("question") &&
            request.getTypes().contains("user") &&
            request.getTypes().size() == 2
        ), eq(1L));
    }

    @Test
    void search_InvalidAuthHeader() {
        // When & Then
        RuntimeException exception = assertThrows(RuntimeException.class, () -> {
            searchController.search("test", 1, 20, "rel", "question", null, null, true, null);
        });

        assertEquals("Authorization 헤더가 필요합니다.", exception.getMessage());
        verify(searchService, never()).search(any(), any());
    }

    @Test
    void search_InvalidBearerToken() {
        // When & Then
        RuntimeException exception = assertThrows(RuntimeException.class, () -> {
            searchController.search("test", 1, 20, "rel", "question", null, null, true, "Invalid-token");
        });

        assertEquals("Authorization 헤더가 필요합니다.", exception.getMessage());
        verify(searchService, never()).search(any(), any());
    }

    @Test
    void search_InvalidPageParameter() {
        // Given
        when(jwtService.extractUserId("valid-token")).thenReturn(1L);

        // When & Then
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> {
            searchController.search("test", 0, 20, "rel", "question", null, null, true, "Bearer valid-token");
        });

        assertEquals("페이지는 1 이상이어야 합니다.", exception.getMessage());
        verify(searchService, never()).search(any(), any());
    }

    @Test
    void search_InvalidSizeParameter() {
        // Given
        when(jwtService.extractUserId("valid-token")).thenReturn(1L);

        // When & Then
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> {
            searchController.search("test", 1, 0, "rel", "question", null, null, true, "Bearer valid-token");
        });

        assertEquals("크기는 1~100 사이여야 합니다.", exception.getMessage());
        verify(searchService, never()).search(any(), any());
    }

    @Test
    void search_InvalidSizeParameterTooLarge() {
        // Given
        when(jwtService.extractUserId("valid-token")).thenReturn(1L);

        // When & Then
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> {
            searchController.search("test", 1, 101, "rel", "question", null, null, true, "Bearer valid-token");
        });

        assertEquals("크기는 1~100 사이여야 합니다.", exception.getMessage());
        verify(searchService, never()).search(any(), any());
    }

    @Test
    void search_QueryTooLong() {
        // Given
        when(jwtService.extractUserId("valid-token")).thenReturn(1L);
        String longQuery = "a".repeat(201); // 201 characters

        // When & Then
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> {
            searchController.search(longQuery, 1, 20, "rel", "question", null, null, true, "Bearer valid-token");
        });

        assertEquals("검색어는 최대 200자까지 허용됩니다.", exception.getMessage());
        verify(searchService, never()).search(any(), any());
    }

    @Test
    void search_InvalidSortOption() {
        // Given
        when(jwtService.extractUserId("valid-token")).thenReturn(1L);

        // When & Then
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> {
            searchController.search("test", 1, 20, "invalid", "question", null, null, true, "Bearer valid-token");
        });

        assertEquals("유효하지 않은 정렬 옵션입니다.", exception.getMessage());
        verify(searchService, never()).search(any(), any());
    }

    @Test
    void search_InvalidType() {
        // Given
        when(jwtService.extractUserId("valid-token")).thenReturn(1L);

        // When & Then
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> {
            searchController.search("test", 1, 20, "rel", "invalid", null, null, true, "Bearer valid-token");
        });

        assertEquals("유효하지 않은 타입입니다: invalid", exception.getMessage());
        verify(searchService, never()).search(any(), any());
    }

    @Test
    void search_InvalidDifficulty() {
        // Given
        when(jwtService.extractUserId("valid-token")).thenReturn(1L);

        // When & Then
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> {
            searchController.search("test", 1, 20, "rel", "question", "INVALID", null, true, "Bearer valid-token");
        });

        assertEquals("유효하지 않은 난이도입니다.", exception.getMessage());
        verify(searchService, never()).search(any(), any());
    }

    @Test
    void search_InvalidCategoryId() {
        // Given
        when(jwtService.extractUserId("valid-token")).thenReturn(1L);

        // When & Then
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> {
            searchController.search("test", 1, 20, "rel", "question", null, 0L, true, "Bearer valid-token");
        });

        assertEquals("카테고리 ID는 1 이상이어야 합니다.", exception.getMessage());
        verify(searchService, never()).search(any(), any());
    }

    @Test
    void search_EmptyTypesParameter() {
        // Given
        when(jwtService.extractUserId("valid-token")).thenReturn(1L);
        when(searchService.search(any(), eq(1L))).thenReturn(mockResponse);

        // When
        ResponseEntity<SearchResponse> response = searchController.search(
                "test", 1, 20, "rel", "", null, null, true, "Bearer valid-token"
        );

        // Then
        assertEquals(200, response.getStatusCodeValue());

        // Should default to question,user
        verify(searchService).search(argThat(request ->
            request.getTypes().contains("question") &&
            request.getTypes().contains("user")
        ), eq(1L));
    }

    @Test
    void search_WhitespaceInTypes() {
        // Given
        when(jwtService.extractUserId("valid-token")).thenReturn(1L);
        when(searchService.search(any(), eq(1L))).thenReturn(mockResponse);

        // When
        ResponseEntity<SearchResponse> response = searchController.search(
                "test", 1, 20, "rel", " question , user ", null, null, true, "Bearer valid-token"
        );

        // Then
        assertEquals(200, response.getStatusCodeValue());

        // Should trim whitespace properly
        verify(searchService).search(argThat(request ->
            request.getTypes().contains("question") &&
            request.getTypes().contains("user") &&
            request.getTypes().size() == 2
        ), eq(1L));
    }

    @Test
    void search_DefaultParameters() {
        // Given
        when(jwtService.extractUserId("valid-token")).thenReturn(1L);
        when(searchService.search(any(), eq(1L))).thenReturn(mockResponse);

        // When
        ResponseEntity<SearchResponse> response = searchController.search(
                "", 1, 20, "rel", "question,user", null, null, true, "Bearer valid-token"
        );

        // Then
        assertEquals(200, response.getStatusCodeValue());

        verify(searchService).search(argThat(request ->
            "".equals(request.getQ()) &&
            request.getPage() == 1 &&
            request.getSize() == 20 &&
            "rel".equals(request.getSort()) &&
            request.isHighlight()
        ), eq(1L));
    }

    @Test
    void search_ValidDifficulties() {
        // Given
        when(jwtService.extractUserId("valid-token")).thenReturn(1L);
        when(searchService.search(any(), eq(1L))).thenReturn(mockResponse);

        // Test each valid difficulty
        List<String> validDifficulties = Arrays.asList("EASY", "MEDIUM", "HARD");

        for (String difficulty : validDifficulties) {
            // When
            ResponseEntity<SearchResponse> response = searchController.search(
                    "test", 1, 20, "rel", "question", difficulty, null, true, "Bearer valid-token"
            );

            // Then
            assertEquals(200, response.getStatusCodeValue());
        }

        verify(searchService, times(3)).search(any(), eq(1L));
    }

    @Test
    void search_ValidSortOptions() {
        // Given
        when(jwtService.extractUserId("valid-token")).thenReturn(1L);
        when(searchService.search(any(), eq(1L))).thenReturn(mockResponse);

        // Test each valid sort option
        List<String> validSorts = Arrays.asList("rel", "new", "old", "pop");

        for (String sort : validSorts) {
            // When
            ResponseEntity<SearchResponse> response = searchController.search(
                    "test", 1, 20, sort, "question", null, null, true, "Bearer valid-token"
            );

            // Then
            assertEquals(200, response.getStatusCodeValue());
        }

        verify(searchService, times(4)).search(any(), eq(1L));
    }

    @Test
    void search_HighlightDisabled() {
        // Given
        when(jwtService.extractUserId("valid-token")).thenReturn(1L);
        when(searchService.search(any(), eq(1L))).thenReturn(mockResponse);

        // When
        ResponseEntity<SearchResponse> response = searchController.search(
                "test", 1, 20, "rel", "question", null, null, false, "Bearer valid-token"
        );

        // Then
        assertEquals(200, response.getStatusCodeValue());

        verify(searchService).search(argThat(request ->
            !request.isHighlight()
        ), eq(1L));
    }
}