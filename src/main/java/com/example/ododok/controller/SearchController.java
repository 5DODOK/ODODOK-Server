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
            @RequestParam(value = "year", required = false) Integer year,
            @RequestParam(value = "company_id", required = false) Long companyId,
            @RequestParam(value = "company_name", required = false) String companyName,
            @RequestParam(value = "category_id", required = false) Long categoryId,
            @RequestParam(value = "sort", defaultValue = "rel") String sort,
            @RequestParam(value = "page", defaultValue = "1") int page,
            @RequestParam(value = "size", defaultValue = "20") int size,
            @RequestHeader("Authorization") String authHeader) {

        try {
            Long userId = extractUserIdFromToken(authHeader);

            // 요청 DTO 생성
            SearchRequest request = new SearchRequest();
            request.setYear(year);
            request.setCompanyId(companyId);
            request.setCompanyName(companyName);
            request.setCategoryId(categoryId);
            request.setSort(sort);
            request.setPage(page);
            request.setSize(size);

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


    private void validateSearchRequest(SearchRequest request) {
        // 페이지 범위 검증
        if (request.getPage() < 1) {
            throw new IllegalArgumentException("페이지는 1 이상이어야 합니다.");
        }

        // 크기 범위 검증
        if (request.getSize() < 1 || request.getSize() > 100) {
            throw new IllegalArgumentException("크기는 1~100 사이여야 합니다.");
        }

        // 정렬 옵션 검증
        List<String> validSorts = List.of("rel", "new", "old");
        if (!validSorts.contains(request.getSort())) {
            throw new IllegalArgumentException("유효하지 않은 정렬 옵션입니다.");
        }

        // 카테고리 ID 검증
        if (request.getCategoryId() != null && request.getCategoryId() < 1) {
            throw new IllegalArgumentException("카테고리 ID는 1 이상이어야 합니다.");
        }

        // 학년도 검증
        if (request.getYear() != null && (request.getYear() < 2000 || request.getYear() > java.time.Year.now().getValue() + 1)) {
            throw new IllegalArgumentException("유효하지 않은 쿼리 파라미터입니다.");
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