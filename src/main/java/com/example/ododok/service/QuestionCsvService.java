package com.example.ododok.service;

import com.example.ododok.dto.CsvUploadResponse;
import com.example.ododok.dto.QuestionCsvRow;
import com.example.ododok.entity.Company;
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
import org.springframework.transaction.annotation.Transactional;
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

    // ✅ title 포함된 헤더 조합 추가
    private static final Set<String> VALID_HEADER_COMBINATIONS = Set.of(
            "question,title,difficulty,year,company_name,category_name",
            "question,difficulty,year,company_name,category_name",
            "question,difficulty,year"
    );

    private static final Map<String, Integer> DIFFICULTY_MAPPING = Map.of(
            "EASY", 1,
            "MEDIUM", 2,
            "HARD", 3
    );

    @Transactional
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
                String[] headers = Arrays.stream(headerLine.split(","))
                        .map(String::trim)
                        .map(String::toLowerCase)
                        .toArray(String[]::new);
                String normalizedHeader = String.join(",", headers);
                if (!VALID_HEADER_COMBINATIONS.contains(normalizedHeader)) {
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
            int rowNumber = i + 2;
            try {
                QuestionCsvRow row = rows.get(i);
                validateRow(row, rowNumber);
                Question question = convertToQuestion(row, userId);

                if (!dryRun) {
                    boolean isUpdate = saveOrUpdateQuestion(question);
                    if (isUpdate) updated++;
                    else created++;
                } else created++;
            } catch (CsvProcessingException e) {
                errors.add(new CsvUploadResponse.ValidationError(rowNumber, e.getErrorCode(), e.getField(), e.getMessage()));
                skipped++;
            } catch (Exception e) {
                log.error("Unexpected error processing row {}", rowNumber, e);
                errors.add(new CsvUploadResponse.ValidationError(rowNumber, "PROCESSING_ERROR", null, "행 처리 중 오류"));
                skipped++;
            }
        }

        return new CsvUploadResponse(
                new CsvUploadResponse.Summary(rows.size(), dryRun ? 0 : created, dryRun ? 0 : updated, skipped, dryRun, upsertKey),
                errors
        );
    }

    private void validateRow(QuestionCsvRow row, int rowNumber) {
        if (row.getQuestion() == null || row.getQuestion().trim().isEmpty())
            throw new CsvProcessingException("질문은 필수입니다.", "REQUIRED_FIELD_MISSING", "question");

        if (row.getQuestion().length() > 200)
            throw new CsvProcessingException("질문은 최대 200자까지 허용됩니다.", "FIELD_TOO_LONG", "question");

        if (row.getDifficulty() != null && !row.getDifficulty().trim().isEmpty()) {
            String difficulty = row.getDifficulty().trim().toUpperCase();
            if (!DIFFICULTY_MAPPING.containsKey(difficulty) && !isNumeric(difficulty))
                throw new CsvProcessingException("허용 라벨은 EASY, MEDIUM, HARD 입니다.", "INVALID_DIFFICULTY_LABEL", "difficulty");
        }

        if (row.getYear() != null && !row.getYear().trim().isEmpty()) {
            try {
                Integer.parseInt(row.getYear().trim());
            } catch (NumberFormatException e) {
                throw new CsvProcessingException("연도는 정수여야 합니다.", "INVALID_YEAR_FORMAT", "year");
            }
        }

        validateForeignKeys(row);
    }

    private void validateForeignKeys(QuestionCsvRow row) {
        if (row.getCompanyName() != null && !row.getCompanyName().trim().isEmpty())
            companyRepository.findByName(row.getCompanyName().trim())
                    .orElseThrow(() -> new CsvProcessingException("해당 회사명을 가진 레코드를 찾을 수 없습니다.", "FK_NOT_FOUND", "company_name"));

        if (row.getCategoryName() != null && !row.getCategoryName().trim().isEmpty())
            categoryRepository.findByName(row.getCategoryName().trim())
                    .orElseThrow(() -> new CsvProcessingException("해당 카테고리명을 가진 레코드를 찾을 수 없습니다.", "FK_NOT_FOUND", "category_name"));
    }

    private Question convertToQuestion(QuestionCsvRow row, Long userId) {
        Question question = new Question();
        question.setQuestion(row.getQuestion().trim());
        question.setCreatedBy(userId);
        question.setIsPublic(true); // CSV로 업로드된 질문은 기본적으로 공개

        // ✅ title 처리 추가
        if (row.getTitle() != null && !row.getTitle().trim().isEmpty())
            question.setTitle(row.getTitle().trim());
        else
            question.setTitle("기술면접"); // 기본값

        // 난이도
        if (row.getDifficulty() != null && !row.getDifficulty().trim().isEmpty()) {
            String difficulty = row.getDifficulty().trim().toUpperCase();
            question.setDifficulty(DIFFICULTY_MAPPING.getOrDefault(difficulty, 2));
        } else question.setDifficulty(2);

        // 연도
        if (row.getYear() != null && !row.getYear().trim().isEmpty())
            question.setYear(Integer.parseInt(row.getYear().trim()));

        // 회사
        if (row.getCompanyName() != null && !row.getCompanyName().trim().isEmpty()) {
            Company company = companyRepository.findByName(row.getCompanyName().trim())
                    .orElseThrow(() -> new CsvProcessingException("해당 회사를 찾을 수 없습니다.", "COMPANY_NOT_FOUND", "company_name"));
            question.setCompany(company);
        }

        // 카테고리
        if (row.getCategoryName() != null && !row.getCategoryName().trim().isEmpty()) {
            Long categoryId = categoryRepository.findByName(row.getCategoryName().trim())
                    .map(c -> c.getId()).orElse(null);
            question.setCategoryId(categoryId);
        }

        return question;
    }

    private boolean saveOrUpdateQuestion(Question question) {
        Optional<Question> existing;
        if ("question".equals(upsertKey)) {
            existing = questionRepository.findByQuestion(question.getQuestion());
        } else {
            String companyName = question.getCompany() != null ? question.getCompany().getName() : null;
            existing = questionRepository.findByQuestionAndYearAndCompanyNameAndCategoryId(
                    question.getQuestion(), question.getYear(), companyName, question.getCategoryId()
            );
        }

        if (existing.isPresent()) {
            Question existingQuestion = existing.get();
            existingQuestion.setQuestion(question.getQuestion());
            existingQuestion.setTitle(question.getTitle()); // ✅ title 업데이트 추가
            existingQuestion.setDifficulty(question.getDifficulty());
            existingQuestion.setYear(question.getYear());
            existingQuestion.setCompany(question.getCompany());
            existingQuestion.setCategoryId(question.getCategoryId());
            questionRepository.save(existingQuestion);
            return true;
        } else {
            questionRepository.save(question);
            return false;
        }
    }

    private boolean isNumeric(String str) {
        try { Integer.parseInt(str); return true; } catch (NumberFormatException e) { return false; }
    }

    // ✅ 샘플 CSV에 title 포함
    public String generateSampleCsv() {
        StringBuilder csv = new StringBuilder();
        csv.append("question,title,difficulty,year,company_name,category_name\n");
        csv.append("\"자바에서 HashMap과 TreeMap의 차이점은 무엇인가요?\",\"기술면접\",MEDIUM,2024,\"네이버\",\"백엔드\"\n");
        csv.append("\"React에서 useState Hook을 사용하는 이유는?\",\"기술면접\",EASY,2024,\"카카오\",\"프론트엔드\"\n");
        csv.append("\"데이터베이스 인덱스의 장단점을 설명하세요\",\"기술면접\",HARD,2023,\"삼성전자\",\"데이터베이스\"\n");
        return csv.toString();
    }
}
