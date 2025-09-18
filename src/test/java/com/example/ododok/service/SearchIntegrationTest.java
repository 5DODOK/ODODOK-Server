package com.example.ododok.service;

import com.example.ododok.dto.SearchRequest;
import com.example.ododok.dto.SearchResponse;
import com.example.ododok.entity.Question;
import com.example.ododok.entity.User;
import com.example.ododok.entity.UserRole;
import com.example.ododok.repository.QuestionRepository;
import com.example.ododok.repository.UserRepository;
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
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class SearchIntegrationTest {

    @Mock
    private QuestionRepository questionRepository;

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private SearchService searchService;

    private Question publicQuestion;
    private Question privateQuestion;
    private Question userOwnQuestion;
    private User testUser;
    private User adminUser;

    @BeforeEach
    void setUp() {
        // Public question
        publicQuestion = new Question();
        publicQuestion.setId(1L);
        publicQuestion.setTitle("공개 자바 질문");
        publicQuestion.setQuestion("자바에서 HashMap은 무엇인가요?");
        publicQuestion.setContent("자바의 특징과 장점에 대해 설명해주세요");
        publicQuestion.setDifficulty(2);
        publicQuestion.setIsPublic(true);
        publicQuestion.setCreatedBy(1L);
        publicQuestion.setCreatedAt(LocalDateTime.now().minusDays(1));

        // Private question by another user
        privateQuestion = new Question();
        privateQuestion.setId(2L);
        privateQuestion.setTitle("비공개 스프링 질문");
        privateQuestion.setQuestion("스프링 고급 내용");
        privateQuestion.setContent("스프링 AOP에 대해 설명하세요");
        privateQuestion.setDifficulty(3);
        privateQuestion.setIsPublic(false);
        privateQuestion.setCreatedBy(2L);
        privateQuestion.setCreatedAt(LocalDateTime.now().minusDays(2));

        // User's own question (private)
        userOwnQuestion = new Question();
        userOwnQuestion.setId(3L);
        userOwnQuestion.setTitle("내 질문");
        userOwnQuestion.setQuestion("내가 만든 비공개 질문");
        userOwnQuestion.setContent("내 개인적인 질문입니다");
        userOwnQuestion.setDifficulty(1);
        userOwnQuestion.setIsPublic(false);
        userOwnQuestion.setCreatedBy(3L);
        userOwnQuestion.setCreatedAt(LocalDateTime.now());

        // Test user
        testUser = new User();
        testUser.setUserId(1L);
        testUser.setUsername("javadeveloper");
        testUser.setDisplayName("자바 개발자");
        testUser.setEmail("java@example.com");
        testUser.setRole(UserRole.USER);
        testUser.setCreatedAt(LocalDateTime.now().minusDays(30));

        // Admin user
        adminUser = new User();
        adminUser.setUserId(2L);
        adminUser.setUsername("admin");
        adminUser.setDisplayName("관리자");
        adminUser.setEmail("admin@example.com");
        adminUser.setRole(UserRole.ADMIN);
        adminUser.setCreatedAt(LocalDateTime.now().minusDays(60));
    }

    @Test
    void search_PrivacyControl_UserCanSeeOwnPrivateQuestions() {
        // Given
        SearchRequest request = new SearchRequest();
        request.setQ("");
        request.setTypes(List.of("question"));

        // User 3 should see public questions + their own private questions
        Page<Question> questionPage = new PageImpl<>(Arrays.asList(publicQuestion, userOwnQuestion));
        when(questionRepository.findAllByIsPublicTrueOrCreatedBy(eq(true), eq(3L), any(Pageable.class)))
                .thenReturn(questionPage);
        when(questionRepository.count()).thenReturn(3L);
        when(questionRepository.countByDifficulty(1)).thenReturn(1);
        when(questionRepository.countByDifficulty(2)).thenReturn(1);
        when(questionRepository.countByDifficulty(3)).thenReturn(1);
        when(userRepository.count()).thenReturn(2L);

        // When
        SearchResponse response = searchService.search(request, 3L);

        // Then
        assertEquals(2, response.getResults().size());

        // Verify that both public and user's own private question are included
        List<Long> resultIds = response.getResults().stream()
                .map(SearchResponse.SearchResult::getId)
                .toList();
        assertTrue(resultIds.contains(1L)); // Public question
        assertTrue(resultIds.contains(3L)); // User's own private question
        assertFalse(resultIds.contains(2L)); // Should not contain other user's private question
    }

    @Test
    void search_TextSearch_CrossFieldMatching() {
        // Given
        SearchRequest request = new SearchRequest();
        request.setQ("자바");
        request.setTypes(List.of("question"));

        // Should find questions with "자바" in title, question, or content
        Page<Question> questionPage = new PageImpl<>(Arrays.asList(publicQuestion));
        when(questionRepository.findByText(eq("%자바%"), eq(1L), any(Pageable.class)))
                .thenReturn(questionPage);
        when(questionRepository.count()).thenReturn(3L);
        when(questionRepository.countByDifficulty(1)).thenReturn(1);
        when(questionRepository.countByDifficulty(2)).thenReturn(1);
        when(questionRepository.countByDifficulty(3)).thenReturn(1);
        when(userRepository.count()).thenReturn(2L);

        // When
        SearchResponse response = searchService.search(request, 1L);

        // Then
        assertEquals(1, response.getResults().size());
        assertEquals("자바에서 HashMap은 무엇인가요?", response.getResults().get(0).getQuestion());

        // Verify search was performed with correct parameters
        verify(questionRepository).findByText(eq("%자바%"), eq(1L), any(Pageable.class));
    }

    @Test
    void search_CombinedFilters_DifficultyAndCategory() {
        // Given
        SearchRequest request = new SearchRequest();
        request.setQ("스프링");
        request.setDifficulty("HARD");
        request.setCategoryId(5L);
        request.setTypes(List.of("question"));

        Page<Question> questionPage = new PageImpl<>(Arrays.asList(privateQuestion));
        when(questionRepository.findByTextAndDifficultyAndCategory(
                eq("%스프링%"), eq(3), eq(5L), eq(2L), any(Pageable.class)))
                .thenReturn(questionPage);
        when(questionRepository.count()).thenReturn(3L);
        when(questionRepository.countByDifficulty(1)).thenReturn(1);
        when(questionRepository.countByDifficulty(2)).thenReturn(1);
        when(questionRepository.countByDifficulty(3)).thenReturn(1);
        when(userRepository.count()).thenReturn(2L);

        // When
        SearchResponse response = searchService.search(request, 2L);

        // Then
        assertEquals(1, response.getResults().size());
        assertEquals(3, response.getResults().get(0).getDifficulty());

        // Verify combined filter was used
        verify(questionRepository).findByTextAndDifficultyAndCategory(
                eq("%스프링%"), eq(3), eq(5L), eq(2L), any(Pageable.class));
    }

    @Test
    void search_MultiType_QuestionsAndUsers() {
        // Given
        SearchRequest request = new SearchRequest();
        request.setQ("java");
        request.setTypes(List.of("question", "user"));

        // Mock question search
        Page<Question> questionPage = new PageImpl<>(Arrays.asList(publicQuestion));
        when(questionRepository.findByText(eq("%java%"), eq(1L), any(Pageable.class)))
                .thenReturn(questionPage);

        // Mock user search
        Page<User> userPage = new PageImpl<>(Arrays.asList(testUser));
        when(userRepository.findByUsernameContainingIgnoreCase(eq("java"), any(Pageable.class)))
                .thenReturn(userPage);

        when(questionRepository.count()).thenReturn(3L);
        when(questionRepository.countByDifficulty(1)).thenReturn(1);
        when(questionRepository.countByDifficulty(2)).thenReturn(1);
        when(questionRepository.countByDifficulty(3)).thenReturn(1);
        when(userRepository.count()).thenReturn(2L);

        // When
        SearchResponse response = searchService.search(request, 1L);

        // Then
        assertEquals(2, response.getResults().size());

        // Verify we have both types in results
        List<String> resultTypes = response.getResults().stream()
                .map(SearchResponse.SearchResult::getType)
                .toList();
        assertTrue(resultTypes.contains("question"));
        assertTrue(resultTypes.contains("user"));
    }

    @Test
    void search_Sorting_RelevanceVsTime() {
        // Given - test relevance sorting
        SearchRequest relRequest = new SearchRequest();
        relRequest.setQ("자바");
        relRequest.setSort("rel");
        relRequest.setTypes(List.of("question"));

        Page<Question> questionPage = new PageImpl<>(Arrays.asList(publicQuestion, userOwnQuestion));
        when(questionRepository.findByText(eq("%자바%"), eq(1L), any(Pageable.class)))
                .thenReturn(questionPage);
        when(questionRepository.count()).thenReturn(3L);
        when(questionRepository.countByDifficulty(1)).thenReturn(1);
        when(questionRepository.countByDifficulty(2)).thenReturn(1);
        when(questionRepository.countByDifficulty(3)).thenReturn(1);
        when(userRepository.count()).thenReturn(2L);

        // When
        SearchResponse response = searchService.search(relRequest, 1L);

        // Then
        assertEquals(2, response.getResults().size());

        // Results should be sorted by relevance (score)
        SearchResponse.SearchResult first = response.getResults().get(0);
        SearchResponse.SearchResult second = response.getResults().get(1);
        assertTrue(first.getScore() >= second.getScore());
    }

    @Test
    void search_Highlighting_EnabledVsDisabled() {
        // Given - with highlighting
        SearchRequest highlightRequest = new SearchRequest();
        highlightRequest.setQ("자바");
        highlightRequest.setHighlight(true);
        highlightRequest.setTypes(List.of("question"));

        Page<Question> questionPage = new PageImpl<>(Arrays.asList(publicQuestion));
        when(questionRepository.findByText(eq("%자바%"), eq(1L), any(Pageable.class)))
                .thenReturn(questionPage);
        when(questionRepository.count()).thenReturn(3L);
        when(questionRepository.countByDifficulty(1)).thenReturn(1);
        when(questionRepository.countByDifficulty(2)).thenReturn(1);
        when(questionRepository.countByDifficulty(3)).thenReturn(1);
        when(userRepository.count()).thenReturn(2L);

        // When
        SearchResponse highlightResponse = searchService.search(highlightRequest, 1L);

        // Then
        SearchResponse.SearchResult result = highlightResponse.getResults().get(0);
        assertNotNull(result.getSnippet());

        // Test without highlighting
        highlightRequest.setHighlight(false);
        SearchResponse noHighlightResponse = searchService.search(highlightRequest, 1L);

        SearchResponse.SearchResult noHighlightResult = noHighlightResponse.getResults().get(0);
        assertNotNull(noHighlightResult.getSnippet());
        // When highlighting is disabled, snippet should not contain <em> tags
        assertFalse(noHighlightResult.getSnippet().contains("<em>"));
    }

    @Test
    void search_Pagination_CorrectSlicing() {
        // Given
        SearchRequest request = new SearchRequest();
        request.setQ("");
        request.setPage(2);
        request.setSize(1);
        request.setTypes(List.of("question"));

        // Mock more questions than page size - need enough results for pagination
        Page<Question> questionPage = new PageImpl<>(
                Arrays.asList(publicQuestion, privateQuestion),
                org.springframework.data.domain.PageRequest.of(0, 10), // Get more results from repo
                2 // total elements - match actual content size
        );
        when(questionRepository.findAllByIsPublicTrueOrCreatedBy(eq(true), eq(1L), any(Pageable.class)))
                .thenReturn(questionPage);
        when(questionRepository.count()).thenReturn(3L);
        when(questionRepository.countByDifficulty(1)).thenReturn(1);
        when(questionRepository.countByDifficulty(2)).thenReturn(1);
        when(questionRepository.countByDifficulty(3)).thenReturn(1);
        when(userRepository.count()).thenReturn(2L);

        // When
        SearchResponse response = searchService.search(request, 1L);

        // Then
        assertEquals(2, response.getPage());
        assertEquals(1, response.getSize());
        assertEquals(2, response.getTotal()); // Total elements from PageImpl
        assertEquals(1, response.getResults().size()); // Only one result per page
    }

    @Test
    void search_Facets_AccurateTypeCounts() {
        // Given
        SearchRequest request = new SearchRequest();
        request.setQ("");
        request.setTypes(List.of("question", "user"));

        Page<Question> questionPage = new PageImpl<>(Arrays.asList(publicQuestion));
        when(questionRepository.findAllByIsPublicTrueOrCreatedBy(eq(true), eq(1L), any(Pageable.class)))
                .thenReturn(questionPage);

        Page<User> userPage = new PageImpl<>(Arrays.asList(testUser, adminUser));
        when(userRepository.findAll(any(Pageable.class)))
                .thenReturn(userPage);

        when(questionRepository.count()).thenReturn(150L);
        when(questionRepository.countByDifficulty(1)).thenReturn(40);
        when(questionRepository.countByDifficulty(2)).thenReturn(85);
        when(questionRepository.countByDifficulty(3)).thenReturn(25);
        when(userRepository.count()).thenReturn(84L);

        // When
        SearchResponse response = searchService.search(request, 1L);

        // Then
        assertNotNull(response.getFacets());

        // Check type facets
        Map<String, Integer> typeFacets = response.getFacets().get("types");
        assertEquals(150, typeFacets.get("question"));
        assertEquals(84, typeFacets.get("user"));

        // Check difficulty facets
        Map<String, Integer> difficultyFacets = response.getFacets().get("difficulty");
        assertEquals(40, difficultyFacets.get("EASY"));
        assertEquals(85, difficultyFacets.get("MEDIUM"));
        assertEquals(25, difficultyFacets.get("HARD"));
    }

    @Test
    void search_PerformanceTracking_TookMsPositive() {
        // Given
        SearchRequest request = new SearchRequest();
        request.setQ("performance");
        request.setTypes(List.of("question"));

        Page<Question> questionPage = new PageImpl<>(Arrays.asList(publicQuestion));
        when(questionRepository.findByText(eq("%performance%"), eq(1L), any(Pageable.class)))
                .thenReturn(questionPage);
        when(questionRepository.count()).thenReturn(3L);
        when(questionRepository.countByDifficulty(1)).thenReturn(1);
        when(questionRepository.countByDifficulty(2)).thenReturn(1);
        when(questionRepository.countByDifficulty(3)).thenReturn(1);
        when(userRepository.count()).thenReturn(2L);

        // When
        SearchResponse response = searchService.search(request, 1L);

        // Then
        assertTrue(response.getTookMs() >= 0);
        assertNotNull(response.getQuery());
        assertEquals("performance", response.getQuery());
    }
}