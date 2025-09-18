package com.example.ododok.controller;

import com.example.ododok.dto.CsvUploadResponse;
import com.example.ododok.service.JwtService;
import com.example.ododok.service.QuestionCsvService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.nio.charset.StandardCharsets;

@RestController
@RequestMapping("/question")
@RequiredArgsConstructor
@Slf4j
public class QuestionCsvController {

    private final QuestionCsvService questionCsvService;
    private final JwtService jwtService;

    @PostMapping("/csv")
    public ResponseEntity<CsvUploadResponse> uploadCsv(
            @RequestParam("file") MultipartFile file,
            @RequestParam(value = "dry_run", required = false, defaultValue = "false") boolean dryRun,
            @RequestHeader("Authorization") String authHeader) {

        try {
            Long userId = extractUserIdFromToken(authHeader);
            CsvUploadResponse response = questionCsvService.processCsvFile(file, dryRun, userId);
            return ResponseEntity.ok(response);

        } catch (Exception e) {
            log.error("CSV upload failed", e);
            throw e;
        }
    }

    @GetMapping("/csv/sample")
    public ResponseEntity<String> downloadSampleCsv() {
        String sampleCsv = questionCsvService.generateSampleCsv();

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.parseMediaType("text/csv; charset=UTF-8"));
        headers.setContentDispositionFormData("attachment", "question_sample.csv");

        return ResponseEntity.ok()
                .headers(headers)
                .body(sampleCsv);
    }

    private Long extractUserIdFromToken(String authHeader) {
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            throw new RuntimeException("Authorization 헤더가 필요합니다.");
        }

        String token = authHeader.substring(7);
        return jwtService.extractUserId(token);
    }
}