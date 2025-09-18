package com.example.ododok.service;

import com.example.ododok.dto.SearchRequest;
import com.example.ododok.dto.SearchResponse;
import com.example.ododok.entity.Question;
import com.example.ododok.entity.Company;
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
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class YearCompanyFilterTest {

    @Mock
    private QuestionRepository questionRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private CompanyRepository companyRepository;

    @InjectMocks
    private SearchService searchService;

    private Question question2025;
    private Question question2024;
    private Company naverCompany;

    @BeforeEach
    void setUp() {
        naverCompany = new Company();
        naverCompany.setId(1L);
        naverCompany.setName("NAVER");
        naverCompany.setCreatedAt(LocalDateTime.now());

        question2025 = new Question();
        question2025.setId(1L);
        question2025.setTitle("2025년 네이버 면접 문제");
        question2025.setQuestion("네이버 2025년 신입 개발자 면접: Java 기초");
        question2025.setContent("Java의 특징에 대해 설명하세요");
        question2025.setDifficulty(2);
        question2025.setYear(2025);
        question2025.setCompanyId(1L);
        question2025.setIsPublic(true);
        question2025.setCreatedBy(1L);
        question2025.setCreatedAt(LocalDateTime.now());

        question2024 = new Question();
        question2024.setId(2L);
        question2024.setTitle("2024년 카카오 면접 문제");
        question2024.setQuestion("카카오 2024년 경력 개발자 면접: 알고리즘");
        question2024.setContent("이진 트리 순회에 대해 설명하세요");
        question2024.setDifficulty(3);
        question2024.setYear(2024);
        question2024.setCompanyId(2L);
        question2024.setIsPublic(true);
        question2024.setCreatedBy(1L);
        question2024.setCreatedAt(LocalDateTime.now());
    }

    @Test
    void search_WithYearFilter_2025() {
        // Given
        SearchRequest request = new SearchRequest();
        request.setQ("");
        request.setYear(2025);
        request.setTypes(List.of("question"));

        Page<Question> questionPage = new PageImpl<>(Arrays.asList(question2025));
        when(questionRepository.findByAllFilters(eq(""), isNull(), eq(2025), isNull(), isNull(), eq(1L), any(Pageable.class)))
                .thenReturn(questionPage);
        when(questionRepository.count()).thenReturn(2L);
        when(questionRepository.countByDifficulty(1)).thenReturn(0);
        when(questionRepository.countByDifficulty(2)).thenReturn(1);
        when(questionRepository.countByDifficulty(3)).thenReturn(1);
        when(questionRepository.findDistinctYears()).thenReturn(List.of(2025, 2024));
        when(questionRepository.findDistinctCompanyIds()).thenReturn(List.of(1L, 2L));
        when(companyRepository.findById(1L)).thenReturn(Optional.of(naverCompany));
        when(userRepository.count()).thenReturn(1L);

        // When
        SearchResponse response = searchService.search(request, 1L);

        // Then
        assertNotNull(response);
        assertEquals(1, response.getResults().size());
        assertEquals(2025, response.getResults().get(0).getYear());
        assertEquals("MEDIUM", response.getResults().get(0).getDifficultyLabel());
        assertEquals("NAVER", response.getResults().get(0).getCompanyName());

        // Verify facets include year and company information
        assertNotNull(response.getFacets());
        assertTrue(response.getFacets().containsKey("year"));
        assertTrue(response.getFacets().containsKey("company"));
    }

    @Test
    void search_WithCompanyId() {
        // Given
        SearchRequest request = new SearchRequest();
        request.setQ("");
        request.setCompanyId(1L);
        request.setTypes(List.of("question"));

        Page<Question> questionPage = new PageImpl<>(Arrays.asList(question2025));
        when(questionRepository.findByAllFilters(eq(""), isNull(), isNull(), eq(1L), isNull(), eq(1L), any(Pageable.class)))
                .thenReturn(questionPage);
        when(questionRepository.count()).thenReturn(2L);
        when(questionRepository.countByDifficulty(1)).thenReturn(0);
        when(questionRepository.countByDifficulty(2)).thenReturn(1);
        when(questionRepository.countByDifficulty(3)).thenReturn(1);
        when(questionRepository.findDistinctYears()).thenReturn(List.of(2025));
        when(questionRepository.findDistinctCompanyIds()).thenReturn(List.of(1L));
        when(questionRepository.countByYear(2025)).thenReturn(1);
        when(questionRepository.countByCompanyId(1L)).thenReturn(1);
        when(companyRepository.findById(1L)).thenReturn(Optional.of(naverCompany));
        when(userRepository.count()).thenReturn(1L);

        // When
        SearchResponse response = searchService.search(request, 1L);

        // Then
        assertNotNull(response);
        assertEquals(1, response.getResults().size());
        assertEquals(1L, response.getResults().get(0).getCompanyId());
        assertEquals("NAVER", response.getResults().get(0).getCompanyName());
    }

    @Test
    void search_WithCompanyName_FoundCompany() {
        // Given
        SearchRequest request = new SearchRequest();
        request.setQ("");
        request.setCompanyName("NAVER");
        request.setTypes(List.of("question"));

        Company foundCompany = new Company();
        foundCompany.setId(1L);
        foundCompany.setName("NAVER");

        Page<Question> questionPage = new PageImpl<>(Arrays.asList(question2025));
        when(companyRepository.findByNameContainingIgnoreCase("NAVER")).thenReturn(List.of(foundCompany));
        when(questionRepository.findByAllFilters(eq(""), isNull(), isNull(), eq(1L), isNull(), eq(1L), any(Pageable.class)))
                .thenReturn(questionPage);
        when(questionRepository.count()).thenReturn(2L);
        when(questionRepository.countByDifficulty(1)).thenReturn(0);
        when(questionRepository.countByDifficulty(2)).thenReturn(1);
        when(questionRepository.countByDifficulty(3)).thenReturn(1);
        when(questionRepository.findDistinctYears()).thenReturn(List.of());
        when(questionRepository.findDistinctCompanyIds()).thenReturn(List.of());
        when(userRepository.count()).thenReturn(1L);

        // When
        SearchResponse response = searchService.search(request, 1L);

        // Then
        assertNotNull(response);
        assertEquals(1, response.getResults().size());
    }

    @Test
    void search_WithCompanyName_NotFound() {
        // Given
        SearchRequest request = new SearchRequest();
        request.setQ("");
        request.setCompanyName("NonExistentCompany");
        request.setTypes(List.of("question"));

        when(companyRepository.findByNameContainingIgnoreCase("NonExistentCompany")).thenReturn(List.of());
        when(questionRepository.count()).thenReturn(2L);
        when(questionRepository.countByDifficulty(1)).thenReturn(0);
        when(questionRepository.countByDifficulty(2)).thenReturn(1);
        when(questionRepository.countByDifficulty(3)).thenReturn(1);
        when(questionRepository.findDistinctYears()).thenReturn(List.of());
        when(questionRepository.findDistinctCompanyIds()).thenReturn(List.of());
        when(userRepository.count()).thenReturn(1L);

        // When
        SearchResponse response = searchService.search(request, 1L);

        // Then
        assertNotNull(response);
        assertEquals(0, response.getResults().size());
        assertEquals(0, response.getTotal());
    }

    @Test
    void search_WithYearAndCompany() {
        // Given
        SearchRequest request = new SearchRequest();
        request.setQ("");
        request.setYear(2025);
        request.setCompanyId(1L);
        request.setTypes(List.of("question"));

        Page<Question> questionPage = new PageImpl<>(Arrays.asList(question2025));
        when(questionRepository.findByAllFilters(eq(""), isNull(), eq(2025), eq(1L), isNull(), eq(1L), any(Pageable.class)))
                .thenReturn(questionPage);
        when(questionRepository.count()).thenReturn(2L);
        when(questionRepository.countByDifficulty(1)).thenReturn(0);
        when(questionRepository.countByDifficulty(2)).thenReturn(1);
        when(questionRepository.countByDifficulty(3)).thenReturn(1);
        when(questionRepository.findDistinctYears()).thenReturn(List.of(2025));
        when(questionRepository.findDistinctCompanyIds()).thenReturn(List.of(1L));
        when(questionRepository.countByYear(2025)).thenReturn(1);
        when(questionRepository.countByCompanyId(1L)).thenReturn(1);
        when(companyRepository.findById(1L)).thenReturn(Optional.of(naverCompany));
        when(userRepository.count()).thenReturn(1L);

        // When
        SearchResponse response = searchService.search(request, 1L);

        // Then
        assertNotNull(response);
        assertEquals(1, response.getResults().size());
        SearchResponse.SearchResult result = response.getResults().get(0);
        assertEquals(2025, result.getYear());
        assertEquals(1L, result.getCompanyId());
        assertEquals("NAVER", result.getCompanyName());
        assertEquals("MEDIUM", result.getDifficultyLabel());

        // Verify the correct method was called with combined filters
        verify(questionRepository).findByAllFilters(eq(""), isNull(), eq(2025), eq(1L), isNull(), eq(1L), any(Pageable.class));
    }
}