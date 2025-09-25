package com.example.ododok.service;

import com.example.ododok.dto.CsvUploadResponse;
import com.example.ododok.dto.QuestionCsvRow;
import com.example.ododok.entity.Question;
import com.example.ododok.entity.User;
import com.example.ododok.entity.UserRole;
import com.example.ododok.exception.CsvProcessingException;
import com.example.ododok.repository.CategoryRepository;
import com.example.ododok.repository.CompanyRepository;
import com.example.ododok.repository.QuestionRepository;
import com.example.ododok.repository.UserRepository;
import com.opencsv.bean.CsvToBeanBuilder;
import com.opencsv.bean.HeaderColumnNameMappingStrategy;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.util.*;

@Service
@RequiredArgsConstructor
@Slf4j
public class QuestionCsvService {

    private final QuestionRepository questionRepository;
    private final CompanyRepository companyRepository;
    private final CategoryRepository categoryRepository;
    private final UserRepository userRepository;

    @Value("${csv.upload.max-file-size:5242880}")
    private long maxFileSize;

    @Value("${csv.upload.max-rows:1000}")
    private int maxRows;

    @Value("${csv.upload.upsert-key:question}")
    private String upsertKey;

    private static final Set<String> VALID_HEADER_COMBINATIONS = Set.of(
            "question,difficulty,year,company_name,category_name",
            "question,difficulty,year"
    );

    private static final Map<String, Integer> DIFFICULTY_MAPPING = Map.of(
            "EASY", 1,
            "MEDIUM", 2,
            "HARD", 3
    );

    public CsvUploadResponse processCsvFile(MultipartFile file, boolean dryRun, Long userId) {
        validateUser(userId);
        validateFile(file);
        validateHeaders(file);

        try (Reader reader = new InputStreamReader(file.getInputStream(), StandardCharsets.UTF_8)) {
            List<QuestionCsvRow> rows = parseCsvFile(reader);
            validateRowCount(rows);

            return processRows(rows, dryRun, userId);

        } catch (CsvProcessingException e) {
            throw e;
        } catch (Exception e) {
            log.error("CSV processing failed", e);
            throw new CsvProcessingException("CSV 파일 처리 중 오류가 발생했습니다.", "CSV_PROCESSING_ERROR");
        }
    }

    private void validateUser(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new CsvProcessingException("사용자를 찾을 수 없습니다.", "USER_NOT_FOUND"));

        if (user.getRole() != UserRole.ADMIN) {
            throw new AccessDeniedException("이 작업을 수행할 권한이 없습니다.");
        }
    }

    private void validateFile(MultipartFile file) {
        if (file.isEmpty()) {
            throw new CsvProcessingException("업로드된 파일이 비어있습니다.", "EMPTY_FILE");
        }

        if (file.getSize() > maxFileSize) {
            throw new CsvProcessingException("업로드 가능한 최대 크기를 초과했습니다.", "FILE_SIZE_EXCEEDED");
        }

        String contentType = file.getContentType();
        if (contentType == null || (!contentType.equals("text/csv") && !contentType.equals("application/csv"))) {
            throw new CsvProcessingException("CSV만 허용됩니다.", "INVALID_CONTENT_TYPE");
        }
    }

    private List<QuestionCsvRow> parseCsvFile(Reader reader) {
        try {
            HeaderColumnNameMappingStrategy<QuestionCsvRow> strategy = new HeaderColumnNameMappingStrategy<>();
            strategy.setType(QuestionCsvRow.class);

            return new CsvToBeanBuilder<QuestionCsvRow>(reader)
                    .withMappingStrategy(strategy)
                    .withIgnoreLeadingWhiteSpace(true)
                    .build()
                    .parse();

        } catch (Exception e) {
            throw new CsvProcessingException("유효하지 않은 CSV 형식입니다.", "INVALID_CSV_FORMAT");
        }
    }

    private void validateHeaders(MultipartFile file) {
        try (Reader reader = new InputStreamReader(file.getInputStream(), StandardCharsets.UTF_8)) {
            Scanner scanner = new Scanner(reader);
            if (scanner.hasNextLine()) {
                String headerLine = scanner.nextLine().trim();

                // Split and clean each header
                String[] headers = Arrays.stream(headerLine.split(","))
                        .map(String::trim)
                        .map(String::toLowerCase)
                        .toArray(String[]::new);

                String normalizedHeader = String.join(",", headers);

                boolean isValidCombination = VALID_HEADER_COMBINATIONS.contains(normalizedHeader);

                if (!isValidCombination) {
                    throw new CsvProcessingException("CSV 헤더가 사양과 일치하지 않습니다.", "HEADER_MISMATCH");
                }
            }
        } catch (CsvProcessingException e) {
            throw e;
        } catch (Exception e) {
            throw new CsvProcessingException("헤더 검증 중 오류가 발생했습니다.", "HEADER_VALIDATION_ERROR");
        }
    }

    private void validateRowCount(List<QuestionCsvRow> rows) {
        if (rows.size() > maxRows) {
            throw new CsvProcessingException(
                    String.format("최대 %d행까지 허용됩니다. 현재: %d행", maxRows, rows.size()),
                    "TOO_MANY_ROWS"
            );
        }
    }

    private CsvUploadResponse processRows(List<QuestionCsvRow> rows, boolean dryRun, Long userId) {
        List<CsvUploadResponse.ValidationError> errors = new ArrayList<>();
        int created = 0, updated = 0, skipped = 0;

        for (int i = 0; i < rows.size(); i++) {
            int rowNumber = i + 2; // Header is row 1, data starts from row 2
            try {
                QuestionCsvRow row = rows.get(i);
                validateRow(row, rowNumber);

                Question question = convertToQuestion(row, userId);

                if (!dryRun) {
                    boolean isUpdate = saveOrUpdateQuestion(question);
                    if (isUpdate) {
                        updated++;
                    } else {
                        created++;
                    }
                } else {
                    created++; // For dry run, count as potential creation
                }

            } catch (CsvProcessingException e) {
                errors.add(new CsvUploadResponse.ValidationError(
                        rowNumber, e.getErrorCode(), e.getField(), e.getMessage()
                ));
                skipped++;
            } catch (Exception e) {
                log.error("Unexpected error processing row {}", rowNumber, e);
                errors.add(new CsvUploadResponse.ValidationError(
                        rowNumber, "PROCESSING_ERROR", null, "행 처리 중 예상치 못한 오류가 발생했습니다."
                ));
                skipped++;
            }
        }

        CsvUploadResponse.Summary summary = new CsvUploadResponse.Summary(
                rows.size(),
                dryRun ? 0 : created,
                dryRun ? 0 : updated,
                skipped,
                dryRun,
                upsertKey
        );

        return new CsvUploadResponse(summary, errors);
    }

    private void validateRow(QuestionCsvRow row, int rowNumber) {
        // Question validation
        if (row.getQuestion() == null || row.getQuestion().trim().isEmpty()) {
            throw new CsvProcessingException("질문은 필수입니다.", "REQUIRED_FIELD_MISSING", "question");
        }
        if (row.getQuestion().length() > 200) {
            throw new CsvProcessingException("질문은 최대 200자까지 허용됩니다.", "FIELD_TOO_LONG", "question");
        }

        // Difficulty validation
        if (row.getDifficulty() != null && !row.getDifficulty().trim().isEmpty()) {
            String difficulty = row.getDifficulty().trim().toUpperCase();
            if (!DIFFICULTY_MAPPING.containsKey(difficulty) && !isNumeric(difficulty)) {
                throw new CsvProcessingException("허용 라벨은 EASY, MEDIUM, HARD 입니다.", "INVALID_DIFFICULTY_LABEL", "difficulty");
            }
        }

        // Year validation
        if (row.getYear() != null && !row.getYear().trim().isEmpty()) {
            try {
                Integer.parseInt(row.getYear().trim());
            } catch (NumberFormatException e) {
                throw new CsvProcessingException("연도는 정수여야 합니다.", "INVALID_YEAR_FORMAT", "year");
            }
        }

        // Mutual exclusion validation
        validateMutualExclusion(row);

        // FK validation
        validateForeignKeys(row);
    }

    private void validateMutualExclusion(QuestionCsvRow row) {
        // company_id와 category_id 지원 제거로 상호 배타 검증 불필요
        // company_name과 category_name만 지원
    }

    private void validateForeignKeys(QuestionCsvRow row) {
        // Company validation - company_name만 지원
        if (row.getCompanyName() != null && !row.getCompanyName().trim().isEmpty()) {
            if (!companyRepository.findByName(row.getCompanyName().trim()).isPresent()) {
                throw new CsvProcessingException("해당 회사명을 가진 레코드를 찾을 수 없습니다.", "FK_NOT_FOUND", "company_name");
            }
        }

        // Category validation - category_name만 지원
        if (row.getCategoryName() != null && !row.getCategoryName().trim().isEmpty()) {
            if (!categoryRepository.findByName(row.getCategoryName().trim()).isPresent()) {
                throw new CsvProcessingException("해당 카테고리명을 가진 레코드를 찾을 수 없습니다.", "FK_NOT_FOUND", "category_name");
            }
        }
    }

    private Question convertToQuestion(QuestionCsvRow row, Long userId) {
        Question question = new Question();
        question.setQuestion(row.getQuestion().trim());
        question.setCreatedBy(userId);

        // Difficulty mapping
        if (row.getDifficulty() != null && !row.getDifficulty().trim().isEmpty()) {
            String difficulty = row.getDifficulty().trim().toUpperCase();
            if (DIFFICULTY_MAPPING.containsKey(difficulty)) {
                question.setDifficulty(DIFFICULTY_MAPPING.get(difficulty));
            } else if (isNumeric(difficulty)) {
                question.setDifficulty(Integer.parseInt(difficulty));
            }
        } else {
            question.setDifficulty(2); // Default MEDIUM
        }

        // Year
        if (row.getYear() != null && !row.getYear().trim().isEmpty()) {
            question.setYear(Integer.parseInt(row.getYear().trim()));
        }

        // Company - company_name 직접 저장
        if (row.getCompanyName() != null && !row.getCompanyName().trim().isEmpty()) {
            question.setCompanyName(row.getCompanyName().trim());
        }

        // Category - category_name으로 category_id 조회
        if (row.getCategoryName() != null && !row.getCategoryName().trim().isEmpty()) {
            Long categoryId = categoryRepository.findByName(row.getCategoryName().trim())
                    .map(category -> category.getId())
                    .orElse(null);
            question.setCategoryId(categoryId);
        }

        return question;
    }

    private boolean saveOrUpdateQuestion(Question question) {
        Optional<Question> existing;

        if ("question".equals(upsertKey)) {
            existing = questionRepository.findByQuestion(question.getQuestion());
        } else {
            existing = questionRepository.findByQuestionAndYearAndCompanyNameAndCategoryId(
                    question.getQuestion(),
                    question.getYear(),
                    question.getCompanyName(),
                    question.getCategoryId()
            );
        }

        if (existing.isPresent()) {
            Question existingQuestion = existing.get();
            existingQuestion.setQuestion(question.getQuestion());
            existingQuestion.setDifficulty(question.getDifficulty());
            existingQuestion.setYear(question.getYear());
            existingQuestion.setCompanyName(question.getCompanyName());
            existingQuestion.setCategoryId(question.getCategoryId());
            questionRepository.save(existingQuestion);
            return true; // Updated
        } else {
            questionRepository.save(question);
            return false; // Created
        }
    }

    private boolean isNumeric(String str) {
        try {
            Integer.parseInt(str);
            return true;
        } catch (NumberFormatException e) {
            return false;
        }
    }

    public String generateSampleCsv() {
        StringBuilder csv = new StringBuilder();
        csv.append("question,difficulty,year,company_name,category_name\n");
        csv.append("\"자바에서 HashMap과 TreeMap의 차이점은 무엇인가요?\",MEDIUM,2024,\"네이버\",\"자료구조\"\n");
        csv.append("\"React에서 useState Hook을 사용하는 이유는?\",EASY,2024,\"카카오\",\"프론트엔드\"\n");
        csv.append("\"데이터베이스 인덱스의 장단점을 설명하세요\",HARD,2023,\"삼성전자\",\"데이터베이스\"\n");
        return csv.toString();
    }
}