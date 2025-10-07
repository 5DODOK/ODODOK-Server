package com.example.ododok.service;

import com.example.ododok.dto.SearchRequest;
import com.example.ododok.dto.SearchResponse;
import com.example.ododok.entity.Question;
import com.example.ododok.exception.CsvProcessingException;
import com.example.ododok.repository.QuestionRepository;
import com.example.ododok.repository.UserRepository;
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

    private static final List<String> VALID_SORT_OPTIONS = List.of("rel", "new", "old");

    private static final Map<String, Integer> DIFFICULTY_MAPPING = Map.of(
            "EASY", 1,
            "MEDIUM", 2,
            "HARD", 3
    );

    public SearchResponse search(SearchRequest request, Long userId) {
        long startTime = System.currentTimeMillis();

        // 요청 검증
        validateSearchRequest(request);

        // company_name 필터 직접 사용 (변환 불필요)
        String companyName = null;
        if (request.getCompanyId() != null) {
            // company_id가 주어진 경우 company 이름으로 변환
            companyName = companyRepository.findById(request.getCompanyId())
                    .map(com.example.ododok.entity.Company::getName)
                    .orElse(null);
        } else if (request.getCompanyName() != null && !request.getCompanyName().trim().isEmpty()) {
            // company_name이 주어진 경우 직접 사용
            companyName = request.getCompanyName().trim();
        }

        // 문제만 검색
        SearchResults questionResults = searchQuestions(request, userId, companyName);

        // 패싯 계산
        Map<String, Object> facets = calculateFacets(request);

        long tookMs = System.currentTimeMillis() - startTime;

        return new SearchResponse(
                Map.of(
                    "year", request.getYear() != null ?  request.getYear() : 0,
                    "company_name", companyName != null ? companyName : "",
                    "sort", request.getSort()
                ),
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

    private SearchResponse createEmptyResponse(SearchRequest request, long startTime, String companyName) {
        return new SearchResponse(
            Map.of(
                "year", request.getYear()  != null ?  request.getYear() : 0,
                "company_name", companyName != null ? companyName : "",
                "sort", request.getSort()
            ),
            request.getPage(),
            request.getSize(),
            0L,
            new ArrayList<>(),
            calculateFacets(request)
        );
    }

    private SearchResults searchQuestions(SearchRequest request, Long userId, String companyName) {
        // 정렬 설정
        Sort sort = createSort(request.getSort());
        Pageable pageable = PageRequest.of(request.getPage() - 1, request.getSize(), sort);

        // 필터링을 사용하여 검색 (텍스트 검색 제거)
        Page<Question> questionPage = questionRepository.findByAllFilters(
                "", // 텍스트 검색 제거
                null, // 난이도 필터 제거
                request.getYear(),
                companyName,
                request.getCategoryId(),
                userId,
                pageable);

        List<SearchResponse.SearchResult> results = questionPage.getContent().stream()
                .map(this::mapQuestionToSearchResult)
                .collect(Collectors.toList());

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
        result.setDifficulty(question.getDifficulty());
        result.setDifficultyLabel(getDifficultyLabel(question.getDifficulty()));
        result.setCreatedAt(question.getCreatedAt());
        return result;
    }




    private Sort createSort(String sortOption) {
        return switch (sortOption) {
            case "new" -> Sort.by(Sort.Direction.DESC, "createdAt");
            case "old" -> Sort.by(Sort.Direction.ASC, "createdAt");
            default -> Sort.by(Sort.Direction.DESC, "createdAt"); // rel 기본값
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
                // company_name 기반이므로 id는 company 테이블에서 조회
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