package com.example.ododok.service;

import com.example.ododok.dto.SearchRequest;
import com.example.ododok.dto.SearchResponse;
import com.example.ododok.entity.Question;
import com.example.ododok.entity.User;
import com.example.ododok.entity.UserRole;
import com.example.ododok.exception.CsvProcessingException;
import com.example.ododok.repository.QuestionRepository;
import com.example.ododok.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional(readOnly = true)
public class SearchService {

    private final QuestionRepository questionRepository;
    private final UserRepository userRepository;

    private static final List<String> VALID_SORT_OPTIONS = List.of("rel", "new", "old", "pop");
    private static final List<String> VALID_TYPES = List.of("question", "user");
    private static final List<String> VALID_DIFFICULTIES = List.of("EASY", "MEDIUM", "HARD");

    private static final Map<String, Integer> DIFFICULTY_MAPPING = Map.of(
            "EASY", 1,
            "MEDIUM", 2,
            "HARD", 3
    );

    public SearchResponse search(SearchRequest request, Long userId) {
        long startTime = System.currentTimeMillis();

        // 요청 검증
        validateSearchRequest(request);

        // 검색 실행
        List<SearchResponse.SearchResult> results = new ArrayList<>();
        long totalCount = 0;

        if (request.getTypes().contains("question")) {
            SearchResults questionResults = searchQuestions(request, userId);
            results.addAll(questionResults.results);
            totalCount += questionResults.total;
        }

        if (request.getTypes().contains("user")) {
            SearchResults userResults = searchUsers(request, userId);
            results.addAll(userResults.results);
            totalCount += userResults.total;
        }

        // 결과 정렬 및 페이징
        results = applySortingAndPaging(results, request);

        // 패싯 계산
        Map<String, Map<String, Integer>> facets = calculateFacets(request);

        long tookMs = System.currentTimeMillis() - startTime;

        return new SearchResponse(
                request.getQ(),
                request.getPage(),
                request.getSize(),
                totalCount,
                tookMs,
                facets,
                results
        );
    }

    private void validateSearchRequest(SearchRequest request) {
        if (!VALID_SORT_OPTIONS.contains(request.getSort())) {
            throw new CsvProcessingException("유효하지 않은 정렬 옵션입니다.", "INVALID_SORT");
        }

        for (String type : request.getTypes()) {
            if (!VALID_TYPES.contains(type)) {
                throw new CsvProcessingException("유효하지 않은 타입입니다.", "INVALID_TYPE");
            }
        }

        if (request.getDifficulty() != null && !VALID_DIFFICULTIES.contains(request.getDifficulty())) {
            throw new CsvProcessingException("유효하지 않은 난이도입니다.", "INVALID_DIFFICULTY");
        }
    }

    private SearchResults searchQuestions(SearchRequest request, Long userId) {
        // 정렬 설정
        Sort sort = createSort(request.getSort());
        Pageable pageable = PageRequest.of(request.getPage() - 1, request.getSize(), sort);

        Page<Question> questionPage;

        if (request.getQ().isEmpty()) {
            // 전체 검색
            questionPage = questionRepository.findAllByIsPublicTrueOrCreatedBy(true, userId, pageable);
        } else {
            // 텍스트 검색
            questionPage = searchQuestionsByText(request, userId, pageable);
        }

        List<SearchResponse.SearchResult> results = questionPage.getContent().stream()
                .map(question -> mapQuestionToSearchResult(question, request))
                .collect(Collectors.toList());

        return new SearchResults(results, questionPage.getTotalElements());
    }

    private Page<Question> searchQuestionsByText(SearchRequest request, Long userId, Pageable pageable) {
        String searchTerm = "%" + request.getQ().toLowerCase() + "%";
        Integer difficultyInt = request.getDifficulty() != null ?
                DIFFICULTY_MAPPING.get(request.getDifficulty()) : null;

        if (difficultyInt != null && request.getCategoryId() != null) {
            return questionRepository.findByTextAndDifficultyAndCategory(
                    searchTerm, difficultyInt, request.getCategoryId(), userId, pageable);
        } else if (difficultyInt != null) {
            return questionRepository.findByTextAndDifficulty(searchTerm, difficultyInt, userId, pageable);
        } else if (request.getCategoryId() != null) {
            return questionRepository.findByTextAndCategory(searchTerm, request.getCategoryId(), userId, pageable);
        } else {
            return questionRepository.findByText(searchTerm, userId, pageable);
        }
    }

    private SearchResults searchUsers(SearchRequest request, Long userId) {
        // 사용자 검색은 기본 구현 (확장 가능)
        Sort sort = Sort.by(Sort.Direction.DESC, "createdAt");
        Pageable pageable = PageRequest.of(request.getPage() - 1, request.getSize(), sort);

        Page<User> userPage;

        if (request.getQ().isEmpty()) {
            userPage = userRepository.findAll(pageable);
        } else {
            String searchTerm = request.getQ().toLowerCase();
            userPage = userRepository.findByUsernameContainingIgnoreCase(searchTerm, pageable);
        }

        List<SearchResponse.SearchResult> results = userPage.getContent().stream()
                .map(user -> mapUserToSearchResult(user, request))
                .collect(Collectors.toList());

        return new SearchResults(results, userPage.getTotalElements());
    }

    private SearchResponse.SearchResult mapQuestionToSearchResult(Question question, SearchRequest request) {
        SearchResponse.SearchResult result = new SearchResponse.SearchResult();
        result.setType("question");
        result.setId(question.getId());
        result.setQuestion(question.getQuestion());
        result.setSnippet(createSnippet(question, request));
        result.setDifficulty(question.getDifficulty());
        result.setYear(question.getYear());
        result.setCompanyId(question.getCompanyId());
        result.setCategoryId(question.getCategoryId());
        result.setScore(calculateScore(question, request));
        result.setCreatedAt(question.getCreatedAt());
        return result;
    }

    private SearchResponse.SearchResult mapUserToSearchResult(User user, SearchRequest request) {
        SearchResponse.SearchResult result = new SearchResponse.SearchResult();
        result.setType("user");
        result.setId(user.getUserId());
        result.setUsername(user.getUsername());
        result.setEmail(user.getEmail());
        result.setDisplayName(user.getDisplayName());
        result.setSnippet(createUserSnippet(user, request));
        result.setScore(calculateUserScore(user, request));
        result.setCreatedAt(user.getCreatedAt());
        return result;
    }

    private String createSnippet(Question question, SearchRequest request) {
        String content = question.getContent() != null ? question.getContent() : question.getQuestion();
        String snippet = content.length() > 160 ? content.substring(0, 157) + "..." : content;

        if (request.isHighlight() && !request.getQ().isEmpty()) {
            snippet = highlightSearchTerms(snippet, request.getQ());
        }

        return snippet;
    }

    private String createUserSnippet(User user, SearchRequest request) {
        String snippet = user.getDisplayName() != null ? user.getDisplayName() : user.getUsername();

        if (request.isHighlight() && !request.getQ().isEmpty()) {
            snippet = highlightSearchTerms(snippet, request.getQ());
        }

        return snippet;
    }

    private String highlightSearchTerms(String text, String searchTerm) {
        if (searchTerm == null || searchTerm.isEmpty()) {
            return text;
        }

        String[] terms = searchTerm.split("\\s+");
        String result = text;

        for (String term : terms) {
            if (!term.isEmpty()) {
                result = result.replaceAll("(?i)" + term, "<em>$0</em>");
            }
        }

        return result;
    }

    private Double calculateScore(Question question, SearchRequest request) {
        double score = 1.0;

        // 텍스트 매칭 점수
        if (!request.getQ().isEmpty()) {
            String searchTerm = request.getQ().toLowerCase();
            String questionText = (question.getQuestion() + " " +
                    (question.getContent() != null ? question.getContent() : "")).toLowerCase();

            if (questionText.contains(searchTerm)) {
                score += 10.0;
            }
        }

        // 최신성 점수
        if ("new".equals(request.getSort())) {
            long daysSinceCreation = java.time.Duration.between(question.getCreatedAt(), LocalDateTime.now()).toDays();
            score += Math.max(0, 30 - daysSinceCreation) * 0.1;
        }

        return score;
    }

    private Double calculateUserScore(User user, SearchRequest request) {
        double score = 1.0;

        // 사용자 역할 점수
        if (user.getRole() == UserRole.ADMIN) {
            score += 5.0;
        }

        return score;
    }

    private Sort createSort(String sortOption) {
        return switch (sortOption) {
            case "new" -> Sort.by(Sort.Direction.DESC, "createdAt");
            case "old" -> Sort.by(Sort.Direction.ASC, "createdAt");
            case "pop" -> Sort.by(Sort.Direction.DESC, "id"); // 임시: 인기도 지표 없음
            default -> Sort.by(Sort.Direction.DESC, "createdAt"); // rel 기본값
        };
    }

    private List<SearchResponse.SearchResult> applySortingAndPaging(
            List<SearchResponse.SearchResult> results, SearchRequest request) {

        // 점수순 정렬 (rel인 경우)
        if ("rel".equals(request.getSort())) {
            results.sort((a, b) -> Double.compare(b.getScore(), a.getScore()));
        }

        // 페이징 적용
        int start = (request.getPage() - 1) * request.getSize();
        int end = Math.min(start + request.getSize(), results.size());

        if (start >= results.size()) {
            return new ArrayList<>();
        }

        return results.subList(start, end);
    }

    private Map<String, Map<String, Integer>> calculateFacets(SearchRequest request) {
        Map<String, Map<String, Integer>> facets = new HashMap<>();

        // 타입 패싯
        Map<String, Integer> typeFacets = new HashMap<>();
        typeFacets.put("question", (int) questionRepository.count());
        typeFacets.put("user", (int) userRepository.count());
        facets.put("types", typeFacets);

        // 난이도 패싯
        Map<String, Integer> difficultyFacets = new HashMap<>();
        difficultyFacets.put("EASY", questionRepository.countByDifficulty(1));
        difficultyFacets.put("MEDIUM", questionRepository.countByDifficulty(2));
        difficultyFacets.put("HARD", questionRepository.countByDifficulty(3));
        facets.put("difficulty", difficultyFacets);

        return facets;
    }

    private static class SearchResults {
        final List<SearchResponse.SearchResult> results;
        final long total;

        SearchResults(List<SearchResponse.SearchResult> results, long total) {
            this.results = results;
            this.total = total;
        }
    }
}