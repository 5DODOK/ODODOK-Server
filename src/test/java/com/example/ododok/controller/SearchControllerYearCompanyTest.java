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
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class SearchControllerYearCompanyTest {

    @Mock
    private SearchService searchService;

    @Mock
    private JwtService jwtService;

    @InjectMocks
    private SearchController searchController;

    private SearchResponse mockResponse;

    @BeforeEach
    void setUp() {
        // Create mock search result for year/company filtering
        SearchResponse.SearchResult result = new SearchResponse.SearchResult();
        result.setType("question");
        result.setId(1L);
        result.setQuestion("네이버 2025년 면접: Java 기초");
        result.setSnippet("Java의 특징에 대해 설명하세요");
        result.setDifficulty(2);
        result.setDifficultyLabel("MEDIUM");
        result.setYear(2025);
        result.setCompanyId(1L);
        result.setCompanyName("NAVER");
        result.setScore(8.5);
        result.setCreatedAt(LocalDateTime.now());

        Map<String, Map<String, Integer>> facets = new HashMap<>();
        Map<String, Integer> yearFacets = new HashMap<>();
        yearFacets.put("2025", 15);
        yearFacets.put("2024", 20);
        facets.put("year", yearFacets);

        Map<String, Integer> companyFacets = new HashMap<>();
        companyFacets.put("1:NAVER", 15);
        companyFacets.put("2:Kakao", 10);
        facets.put("company", companyFacets);

        mockResponse = new SearchResponse(
                "",
                1,
                20,
                35L,
                12L,
                facets,
                Arrays.asList(result)
        );
    }

    @Test
    void search_WithYearFilter() {
        // Given
        when(jwtService.extractUserId("valid-token")).thenReturn(1L);
        when(searchService.search(any(), eq(1L))).thenReturn(mockResponse);

        // When
        ResponseEntity<SearchResponse> response = searchController.search(
                "", 1, 20, "rel", "question", null, null, 2025, null, null, true,
                "Bearer valid-token"
        );

        // Then
        assertEquals(200, response.getStatusCodeValue());
        assertNotNull(response.getBody());
        assertEquals(1, response.getBody().getResults().size());

        SearchResponse.SearchResult result = response.getBody().getResults().get(0);
        assertEquals(2025, result.getYear());
        assertEquals("MEDIUM", result.getDifficultyLabel());
        assertEquals("NAVER", result.getCompanyName());

        // Verify year facets
        assertTrue(response.getBody().getFacets().containsKey("year"));
        assertEquals(15, response.getBody().getFacets().get("year").get("2025"));

        verify(searchService).search(any(), eq(1L));
    }

    @Test
    void search_WithCompanyId() {
        // Given
        when(jwtService.extractUserId("valid-token")).thenReturn(1L);
        when(searchService.search(any(), eq(1L))).thenReturn(mockResponse);

        // When
        ResponseEntity<SearchResponse> response = searchController.search(
                "", 1, 20, "rel", "question", null, null, null, 1L, null, true,
                "Bearer valid-token"
        );

        // Then
        assertEquals(200, response.getStatusCodeValue());
        assertNotNull(response.getBody());
        assertEquals(1, response.getBody().getResults().size());

        SearchResponse.SearchResult result = response.getBody().getResults().get(0);
        assertEquals(1L, result.getCompanyId());
        assertEquals("NAVER", result.getCompanyName());

        verify(searchService).search(any(), eq(1L));
    }

    @Test
    void search_WithCompanyName() {
        // Given
        when(jwtService.extractUserId("valid-token")).thenReturn(1L);
        when(searchService.search(any(), eq(1L))).thenReturn(mockResponse);

        // When
        ResponseEntity<SearchResponse> response = searchController.search(
                "", 1, 20, "rel", "question", null, null, null, null, "NAVER", true,
                "Bearer valid-token"
        );

        // Then
        assertEquals(200, response.getStatusCodeValue());
        assertNotNull(response.getBody());
        assertEquals(1, response.getBody().getResults().size());

        SearchResponse.SearchResult result = response.getBody().getResults().get(0);
        assertEquals("NAVER", result.getCompanyName());

        // Verify company facets
        assertTrue(response.getBody().getFacets().containsKey("company"));
        assertEquals(15, response.getBody().getFacets().get("company").get("1:NAVER"));

        verify(searchService).search(any(), eq(1L));
    }

    @Test
    void search_WithYearAndCompany() {
        // Given
        when(jwtService.extractUserId("valid-token")).thenReturn(1L);
        when(searchService.search(any(), eq(1L))).thenReturn(mockResponse);

        // When
        ResponseEntity<SearchResponse> response = searchController.search(
                "Java", 1, 10, "new", "question", "MEDIUM", null, 2025, 1L, null, false,
                "Bearer valid-token"
        );

        // Then
        assertEquals(200, response.getStatusCodeValue());
        assertNotNull(response.getBody());

        // Verify that request was properly constructed with all filters
        verify(searchService).search(argThat(request ->
            "Java".equals(request.getQ()) &&
            request.getPage() == 1 &&
            request.getSize() == 10 &&
            "new".equals(request.getSort()) &&
            request.getTypes().contains("question") &&
            "MEDIUM".equals(request.getDifficulty()) &&
            request.getYear().equals(2025) &&
            request.getCompanyId().equals(1L) &&
            !request.isHighlight()
        ), eq(1L));
    }

    @Test
    void search_InvalidYearParameter() {
        // Given
        when(jwtService.extractUserId("valid-token")).thenReturn(1L);

        // When & Then
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> {
            searchController.search("test", 1, 20, "rel", "question", null, null, 1999, null, null, true, "Bearer valid-token");
        });

        assertEquals("학년도는 2000년에서 2026년 사이여야 합니다.", exception.getMessage());
        verify(searchService, never()).search(any(), any());
    }

    @Test
    void search_InvalidCompanyId() {
        // Given
        when(jwtService.extractUserId("valid-token")).thenReturn(1L);

        // When & Then
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> {
            searchController.search("test", 1, 20, "rel", "question", null, null, null, 0L, null, true, "Bearer valid-token");
        });

        assertEquals("회사 ID는 1 이상이어야 합니다.", exception.getMessage());
        verify(searchService, never()).search(any(), any());
    }
}