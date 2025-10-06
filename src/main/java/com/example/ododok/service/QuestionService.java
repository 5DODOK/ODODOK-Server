package com.example.ododok.service;

import com.example.ododok.dto.QuestionCreateRequest;
import com.example.ododok.dto.QuestionUpdateRequest;
import com.example.ododok.dto.QuestionResponse;
import com.example.ododok.entity.Company;
import com.example.ododok.entity.Question;
import com.example.ododok.entity.User;
import com.example.ododok.entity.UserRole;
import com.example.ododok.exception.CsvProcessingException;
import com.example.ododok.repository.CategoryRepository;
import com.example.ododok.repository.CompanyRepository;
import com.example.ododok.repository.QuestionRepository;
import com.example.ododok.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class QuestionService {

    private final QuestionRepository questionRepository;
    private final CompanyRepository companyRepository;
    private final CategoryRepository categoryRepository;
    private final UserRepository userRepository;

    private static final Map<String, Integer> DIFFICULTY_MAPPING = Map.of(
            "EASY", 1,
            "MEDIUM", 2,
            "HARD", 3
    );

    public QuestionResponse createQuestion(QuestionCreateRequest request, Long userId) {
        // 사용자 권한 확인
        validateUser(userId);

        // 요청 데이터 검증
        validateCreateRequest(request);

        // Question 엔티티 생성
        Question question = buildQuestion(request, userId);

        // 중복 검사
        checkDuplicateQuestion(question);

        // 저장
        Question savedQuestion = questionRepository.save(question);

        log.info("Question created successfully: id={}, createdBy={}", savedQuestion.getId(), userId);

        return mapToResponse(savedQuestion);
    }

    private void validateUser(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new CsvProcessingException("사용자를 찾을 수 없습니다.", "USER_NOT_FOUND"));

        if (user.getRole() != UserRole.ADMIN) {
            throw new AccessDeniedException("이 작업을 수행할 권한이 없습니다.");
        }
    }

    private void validateCreateRequest(QuestionCreateRequest request) {
        // Company Name 검증
        if (request.getCompanyName() != null && !request.getCompanyName().trim().isEmpty()) {
            // Company name은 자유 입력 가능, 별도 검증 불필요
        }

        // Category ID 검증
        if (request.getCategoryId() != null) {
            if (!categoryRepository.existsById(request.getCategoryId())) {
                throw new CsvProcessingException("연결하려는 카테고리를 찾을 수 없습니다.", "CATEGORY_NOT_FOUND");
            }
        }

        // Difficulty 검증
        if (request.getDifficulty() != null && !DIFFICULTY_MAPPING.containsKey(request.getDifficulty().toUpperCase())) {
            throw new CsvProcessingException("난이도는 EASY, MEDIUM, HARD 중 하나여야 합니다.", "INVALID_DIFFICULTY");
        }
    }

    private Question buildQuestion(QuestionCreateRequest request, Long userId) {
        Question question = new Question();
        question.setQuestion(request.getQuestion().trim());
        question.setTitle(request.getQuestion().trim());
        question.setCreatedBy(userId);

        // Difficulty 매핑
        String difficulty = request.getDifficulty() != null ? request.getDifficulty().toUpperCase() : "MEDIUM";
        question.setDifficulty(DIFFICULTY_MAPPING.get(difficulty));

        // Company 설정
        if (request.getCompanyName() != null && !request.getCompanyName().trim().isEmpty()) {
            Company company = companyRepository.findByName(request.getCompanyName())
                    .orElseGet(() -> {
                        Company newCompany = new Company();
                        newCompany.setName(request.getCompanyName());
                        newCompany.setCreatedAt(LocalDateTime.now());
                        return companyRepository.save(newCompany);
                    });
            question.setCompany(company);
        }

        question.setYear(request.getYear());
        question.setCategoryId(request.getCategoryId());

        return question;
    }

    private void checkDuplicateQuestion(Question question) {
        if (questionRepository.findByQuestion(question.getQuestion()).isPresent()) {
            throw new CsvProcessingException("동일한 제목의 질문이 이미 존재합니다.", "DUPLICATE_QUESTION");
        }
    }

    public QuestionResponse updateQuestion(Long id, QuestionUpdateRequest request, Long userId) {
        // 사용자 권한 확인
        validateUser(userId);

        // 질문 존재 확인
        Question question = questionRepository.findById(id)
                .orElseThrow(() -> new CsvProcessingException("대상을 찾을 수 없습니다.", "QUESTION_NOT_FOUND"));

        // 요청 데이터 검증
        validateUpdateRequest(request);

        // 제목 중복 검사 (제목이 변경되는 경우)
        if (request.getTitle() != null && !request.getTitle().equals(question.getTitle())) {
            checkDuplicateTitle(request.getTitle());
        }

        // 부분 업데이트 적용
        applyPartialUpdate(question, request, userId);

        // 저장
        Question updatedQuestion = questionRepository.save(question);

        log.info("Question updated successfully: id={}, updatedBy={}", id, userId);

        return mapToResponse(updatedQuestion);
    }

    public void deleteQuestion(Long id, Long userId) {
        // 사용자 권한 확인
        validateUser(userId);

        // 질문 존재 확인
        Question question = questionRepository.findById(id)
                .orElseThrow(() -> new CsvProcessingException("대상을 찾을 수 없습니다.", "QUESTION_NOT_FOUND"));

        // 하드 삭제 수행
        questionRepository.delete(question);

        log.info("Question deleted successfully: id={}, deletedBy={}", id, userId);
    }

    private void validateUpdateRequest(QuestionUpdateRequest request) {
        // Category ID 검증
        if (request.getCategoryId() != null) {
            if (!categoryRepository.existsById(request.getCategoryId())) {
                throw new CsvProcessingException("연결하려는 카테고리를 찾을 수 없습니다.", "CATEGORY_NOT_FOUND");
            }
        }

        // Company Name 검증
        if (request.getCompanyName() != null && !request.getCompanyName().trim().isEmpty()) {
            // Company name은 자유 입력 가능, 별도 검증 불필요
        }

        // Difficulty 검증
        if (request.getDifficulty() != null && !DIFFICULTY_MAPPING.containsKey(request.getDifficulty().toUpperCase())) {
            throw new CsvProcessingException("난이도는 EASY, MEDIUM, HARD 중 하나여야 합니다.", "INVALID_DIFFICULTY");
        }

        // Title 공백 검증
        if (request.getTitle() != null && request.getTitle().trim().isEmpty()) {
            throw new CsvProcessingException("제목은 공백일 수 없습니다.", "INVALID_TITLE");
        }

        // Tags 검증
        if (request.getTags() != null) {
            if (request.getTags().size() > 10) {
                throw new CsvProcessingException("태그는 최대 10개까지 허용됩니다.", "TOO_MANY_TAGS");
            }
            for (String tag : request.getTags()) {
                if (tag.length() > 30) {
                    throw new CsvProcessingException("각 태그는 최대 30자까지 허용됩니다.", "TAG_TOO_LONG");
                }
            }
        }
    }

    private void checkDuplicateTitle(String title) {
        if (questionRepository.findByTitle(title.trim()).isPresent()) {
            throw new CsvProcessingException("동일한 제목의 질문이 이미 존재합니다.", "DUPLICATE_TITLE");
        }
    }

    private void applyPartialUpdate(Question question, QuestionUpdateRequest request, Long userId) {
        if (request.getTitle() != null) {
            question.setTitle(request.getTitle().trim());
        }
        if (request.getContent() != null) {
            question.setContent(request.getContent());
        }
        if (request.getTags() != null) {
            question.setTags(request.getTags());
        }
        if (request.getDifficulty() != null) {
            question.setDifficulty(DIFFICULTY_MAPPING.get(request.getDifficulty().toUpperCase()));
        }
        if (request.getAnswer() != null) {
            question.setAnswer(request.getAnswer());
        }
        if (request.getCategoryId() != null) {
            question.setCategoryId(request.getCategoryId());
        }
        if (request.getIsPublic() != null) {
            question.setIsPublic(request.getIsPublic());
        }
        if (request.getYear() != null) {
            question.setYear(request.getYear());
        }

        // Company 업데이트
        if (request.getCompanyName() != null) {
            Company company = companyRepository.findByName(request.getCompanyName())
                    .orElseGet(() -> {
                        Company newCompany = new Company();
                        newCompany.setName(request.getCompanyName());
                        newCompany.setCreatedAt(LocalDateTime.now());
                        return companyRepository.save(newCompany);
                    });
            question.setCompany(company);
        }

        question.setUpdatedBy(userId);
        question.setUpdatedAt(LocalDateTime.now());
    }

    private QuestionResponse mapToResponse(Question question) {
        String companyName = question.getCompany() != null ? question.getCompany().getName() : null;
        return new QuestionResponse(
                question.getId(),
                question.getTitle(),
                question.getQuestion(),
                question.getContent(),
                question.getTags(),
                question.getAnswer(),
                question.getDifficulty(),
                question.getYear(),
                companyName,
                question.getCategoryId(),
                question.getIsPublic(),
                question.getCreatedAt(),
                question.getCreatedBy(),
                question.getUpdatedAt(),
                question.getUpdatedBy()
        );
    }
}