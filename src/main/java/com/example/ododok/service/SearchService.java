package com.example.ododok.service;

import com.example.ododok.dto.SearchRequest;
import com.example.ododok.dto.SearchResponse;
import com.example.ododok.entity.Category;
import com.example.ododok.entity.Question;
import com.example.ododok.exception.CsvProcessingException;
import com.example.ododok.repository.CategoryRepository;
import com.example.ododok.repository.QuestionRepository;
import com.example.ododok.repository.CompanyRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional(readOnly = true)
public class SearchService {

    private final QuestionRepository questionRepository;
    private final CompanyRepository companyRepository;
    private final CategoryRepository categoryRepository;

    private static final List<String> VALID_SORT_OPTIONS = List.of("rel", "new", "old");

    private static final Map<String, Integer> DIFFICULTY_MAPPING = Map.of(
            "EASY", 1,
            "MEDIUM", 2,
            "HARD", 3
    );

    public SearchResponse search(SearchRequest request, Long userId) {
        long startTime = System.currentTimeMillis();

        validateSearchRequest(request);

        // company_name 처리
        String companyName = null;
        if (request.getCompanyId() != null) {
            companyName = companyRepository.findById(request.getCompanyId())
                    .map(com.example.ododok.entity.Company::getName)
                    .orElse(null);
        } else if (request.getCompanyName() != null && !request.getCompanyName().trim().isEmpty()) {
            companyName = request.getCompanyName().trim();
        }

        // category (직무) 처리
        Long categoryId = null;
        if (request.getCategoryId() != null) {
            categoryId = request.getCategoryId();
        } else if (request.getCategoryName() != null && !request.getCategoryName().trim().isEmpty()) {
            categoryId = categoryRepository.findByName(request.getCategoryName().trim())
                    .map(Category::getId)
                    .orElse(null);
        }

        // 문제 검색
        SearchResults questionResults = searchQuestions(request, userId, companyName, categoryId);

        // 패싯 계산
        Map<String, Object> facets = calculateFacets(request);

        Map<String, Object> params = new HashMap<>();
        params.put("year", request.getYear());
        params.put("company_name", companyName);
        params.put("category_id", categoryId);
        params.put("interview_type", request.getInterviewType());
        params.put("sort", request.getSort());

        return new SearchResponse(
                params,
                request.getPage(),
                request.getSize(),
                questionResults.total,
                questionResults.results,
                facets
        );
    }

    private void validateSearchRequest(SearchRequest request) {
        if (!VALID_SORT_OPTIONS.contains(request.getSort())) {
            throw new CsvProcessingException("유효하지 않은 쿼리 파라미터입니다.", "INVALID_SORT");
        }
    }

    private SearchResponse createEmptyResponse(SearchRequest request, long startTime, String companyName, Long categoryId) {
        Map<String, Object> params = new HashMap<>();
        params.put("year", request.getYear());
        params.put("company_name", companyName);
        params.put("category_id", categoryId);
        params.put("sort", request.getSort());

        return new SearchResponse(
                params,
                request.getPage(),
                request.getSize(),
                0L,
                new ArrayList<>(),
                calculateFacets(request)
        );
    }

    private SearchResults searchQuestions(SearchRequest request, Long userId, String companyName, Long categoryId) {
        Sort sort = createSort(request.getSort());
        Pageable pageable = PageRequest.of(request.getPage() - 1, request.getSize(), sort);

        // null 값을 안전하게 처리
        String safeKeyword = null;  // 키워드가 없으면 null
        String safeCompanyName = (companyName != null && !companyName.trim().isEmpty()) ? companyName : null;
        String safeInterviewType = (request.getInterviewType() != null && !request.getInterviewType().trim().isEmpty())
                ? request.getInterviewType().trim() : null;

        // 디버깅 로그 추가
        log.info("========== Search Debug ==========");
        log.info("Original interviewType: '{}'", request.getInterviewType());
        log.info("Safe interviewType: '{}'", safeInterviewType);
        log.info("Year: {}", request.getYear());
        log.info("CompanyName: '{}'", safeCompanyName);
        log.info("CategoryId: {}", categoryId);

        Page<Question> questionPage = questionRepository.findByAllFilters(
                safeKeyword,
                null,  // difficulty
                request.getYear(),
                safeCompanyName,
                categoryId,
                safeInterviewType,
                userId,
                pageable);

        log.info("Found {} questions", questionPage.getTotalElements());

        List<SearchResponse.SearchResult> results = questionPage.getContent().stream()
                .map(question -> {
                    SearchResponse.SearchResult result = mapQuestionToSearchResult(question);
                    log.info("Question ID: {}, Title: '{}'", question.getId(), question.getTitle());
                    return result;
                })
                .collect(Collectors.toList());

        log.info("==================================");

        return new SearchResults(results, questionPage.getTotalElements());
    }

    private SearchResponse.SearchResult mapQuestionToSearchResult(Question question) {
        SearchResponse.SearchResult result = new SearchResponse.SearchResult();
        result.setType("question");
        result.setId(question.getId());
        result.setQuestion(question.getQuestion());
        result.setYear(question.getYear());
        result.setCompanyName(question.getCompany().getName());
        result.setCategoryId(question.getCategoryId());
        result.setCategoryName(question.getCategory() != null ? question.getCategory().getName() : null);
        result.setInterviewType(question.getTitle());  // title을 면접 유형으로
        result.setDifficulty(question.getDifficulty());
        result.setDifficultyLabel(getDifficultyLabel(question.getDifficulty()));
        result.setCreatedAt(question.getCreatedAt());
        return result;
    }

    private Sort createSort(String sortOption) {
        return switch (sortOption) {
            case "new" -> Sort.by(Sort.Direction.DESC, "createdAt")
                    .and(Sort.by(Sort.Direction.DESC, "id"));
            case "old" -> Sort.by(Sort.Direction.ASC, "createdAt")
                    .and(Sort.by(Sort.Direction.ASC, "id"));
            default -> Sort.by(Sort.Direction.DESC, "createdAt")
                    .and(Sort.by(Sort.Direction.DESC, "id"));
        };
    }

    private String getDifficultyLabel(Integer difficulty) {
        if (difficulty == null) return null;
        return switch (difficulty) {
            case 1 -> "EASY";
            case 2 -> "MEDIUM";
            case 3 -> "HARD";
            default -> null;
        };
    }

    private Map<String, Object> calculateFacets(SearchRequest request) {
        Map<String, Object> facets = new HashMap<>();

        // 학년도 패싯
        List<Integer> years = questionRepository.findDistinctYears();
        List<Map<String, Object>> yearFacets = new ArrayList<>();
        for (Integer year : years) {
            Map<String, Object> yearFacet = new HashMap<>();
            yearFacet.put("value", year);
            yearFacet.put("count", questionRepository.countByYear(year));
            yearFacets.add(yearFacet);
        }
        facets.put("year", yearFacets);

        // 회사 패싯
        List<String> companyNames = questionRepository.findDistinctCompanyNames();
        List<Map<String, Object>> companyFacets = new ArrayList<>();
        for (String companyName : companyNames) {
            if (companyName != null) {
                Map<String, Object> companyFacet = new HashMap<>();
                Long companyId = companyRepository.findByName(companyName)
                        .map(com.example.ododok.entity.Company::getId)
                        .orElse(null);
                companyFacet.put("id", companyId);
                companyFacet.put("name", companyName);
                companyFacet.put("count", questionRepository.countByCompanyName(companyName));
                companyFacets.add(companyFacet);
            }
        }
        facets.put("company", companyFacets);

        // 직무 카테고리 패싯
        List<Category> categories = categoryRepository.findAll();
        List<Map<String, Object>> categoryFacets = new ArrayList<>();
        for (Category category : categories) {
            Map<String, Object> categoryFacet = new HashMap<>();
            categoryFacet.put("id", category.getId());
            categoryFacet.put("name", category.getName());
            categoryFacet.put("count", questionRepository.countByCategoryId(category.getId()));
            categoryFacets.add(categoryFacet);
        }
        facets.put("category", categoryFacets);

        // 면접 유형 패싯
        List<String> interviewTypes = questionRepository.findDistinctInterviewTypes();
        List<Map<String, Object>> interviewTypeFacets = new ArrayList<>();
        for (String interviewType : interviewTypes) {
            if (interviewType != null) {
                Map<String, Object> interviewTypeFacet = new HashMap<>();
                interviewTypeFacet.put("name", interviewType);
                interviewTypeFacet.put("count", questionRepository.countByInterviewType(interviewType));
                interviewTypeFacets.add(interviewTypeFacet);
            }
        }
        facets.put("interview_type", interviewTypeFacets);

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