package com.example.ododok.controller;

import com.example.ododok.dto.SearchRequest;
import com.example.ododok.dto.SearchResponse;
import com.example.ododok.service.JwtService;
import com.example.ododok.service.SearchService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Arrays;
import java.util.List;

@RestController
@RequestMapping("/search")
@RequiredArgsConstructor
@Slf4j
public class SearchController {

    private final SearchService searchService;
    private final JwtService jwtService;

    @GetMapping
    public ResponseEntity<SearchResponse> search(
            @RequestParam(value = "q", defaultValue = "") String q,
            @RequestParam(value = "page", defaultValue = "1") int page,
            @RequestParam(value = "size", defaultValue = "20") int size,
            @RequestParam(value = "sort", defaultValue = "rel") String sort,
            @RequestParam(value = "types", defaultValue = "question,user") String types,
            @RequestParam(value = "difficulty", required = false) String difficulty,
            @RequestParam(value = "category_id", required = false) Long categoryId,
            @RequestParam(value = "year", required = false) Integer year,
            @RequestParam(value = "company_id", required = false) Long companyId,
            @RequestParam(value = "company_name", required = false) String companyName,
            @RequestParam(value = "highlight", defaultValue = "true") boolean highlight,
            @RequestHeader("Authorization") String authHeader) {

        try {
            Long userId = extractUserIdFromToken(authHeader);

            // 요청 DTO 생성
            SearchRequest request = new SearchRequest();
            request.setQ(q);
            request.setPage(page);
            request.setSize(size);
            request.setSort(sort);
            request.setTypes(parseTypes(types));
            request.setDifficulty(difficulty);
            request.setCategoryId(categoryId);
            request.setYear(year);
            request.setCompanyId(companyId);
            request.setCompanyName(companyName);
            request.setHighlight(highlight);

            // 요청 파라미터 검증
            validateSearchRequest(request);

            // 검색 실행
            SearchResponse response = searchService.search(request, userId);

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            log.error("Search failed", e);
            throw e;
        }
    }

    private Long extractUserIdFromToken(String authHeader) {
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            throw new RuntimeException("Authorization 헤더가 필요합니다.");
        }

        String token = authHeader.substring(7);
        return jwtService.extractUserId(token);
    }

    private List<String> parseTypes(String types) {
        if (types == null || types.isEmpty()) {
            return List.of("question", "user");
        }
        return Arrays.asList(types.split(","))
                .stream()
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .toList();
    }

    private void validateSearchRequest(SearchRequest request) {
        // 페이지 범위 검증
        if (request.getPage() < 1) {
            throw new IllegalArgumentException("페이지는 1 이상이어야 합니다.");
        }

        // 크기 범위 검증
        if (request.getSize() < 1 || request.getSize() > 100) {
            throw new IllegalArgumentException("크기는 1~100 사이여야 합니다.");
        }

        // 검색어 길이 검증
        if (request.getQ().length() > 200) {
            throw new IllegalArgumentException("검색어는 최대 200자까지 허용됩니다.");
        }

        // 정렬 옵션 검증
        List<String> validSorts = List.of("rel", "new", "old", "pop");
        if (!validSorts.contains(request.getSort())) {
            throw new IllegalArgumentException("유효하지 않은 정렬 옵션입니다.");
        }

        // 타입 검증
        List<String> validTypes = List.of("question", "user");
        for (String type : request.getTypes()) {
            if (!validTypes.contains(type)) {
                throw new IllegalArgumentException("유효하지 않은 타입입니다: " + type);
            }
        }

        // 난이도 검증
        if (request.getDifficulty() != null) {
            List<String> validDifficulties = List.of("EASY", "MEDIUM", "HARD");
            if (!validDifficulties.contains(request.getDifficulty())) {
                throw new IllegalArgumentException("유효하지 않은 난이도입니다.");
            }
        }

        // 카테고리 ID 검증
        if (request.getCategoryId() != null && request.getCategoryId() < 1) {
            throw new IllegalArgumentException("카테고리 ID는 1 이상이어야 합니다.");
        }

        // 학년도 검증
        if (request.getYear() != null && (request.getYear() < 2000 || request.getYear() > 2026)) {
            throw new IllegalArgumentException("학년도는 2000년에서 2026년 사이여야 합니다.");
        }

        // 회사 ID 검증
        if (request.getCompanyId() != null && request.getCompanyId() < 1) {
            throw new IllegalArgumentException("회사 ID는 1 이상이어야 합니다.");
        }

        // 회사명 길이 검증
        if (request.getCompanyName() != null && request.getCompanyName().length() > 100) {
            throw new IllegalArgumentException("회사명은 최대 100자까지 허용됩니다.");
        }
    }
}