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

        // 질문 내용
        question.setQuestion(request.getQuestion().trim());

        // 면접 타입을 title 필드에 저장
        question.setTitle(request.getInterviewType());

        question.setCreatedBy(userId);

        // Difficulty 매핑
        String difficulty = request.getDifficulty() != null ? request.getDifficulty().toUpperCase() : "MEDIUM";
        question.setDifficulty(DIFFICULTY_MAPPING.get(difficulty));

        // Company 설정
        if (request.getCompanyName() != null && !request.getCompanyName().trim().isEmpty()) {
            String companyName = request.getCompanyName().trim();
            Company company = companyRepository.findByName(companyName)
                    .orElseGet(() -> createNewCompany(companyName));
            question.setCompany(company);
        }

        question.setYear(request.getYear());
        question.setCategoryId(request.getCategoryId());
        question.setIsPublic(true);

        return question;
    }

    private Company createNewCompany(String companyName) {
        Company newCompany = new Company();
        newCompany.setName(companyName);
        return companyRepository.save(newCompany);
    }

    private void checkDuplicateQuestion(Question question) {
        if (questionRepository.findByQuestion(question.getQuestion()).isPresent()) {
            throw new CsvProcessingException("동일한 질문이 이미 존재합니다.", "DUPLICATE_QUESTION");
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

        // 질문 중복 검사 (질문이 변경되는 경우)
        if (request.getQuestion() != null && !request.getQuestion().equals(question.getQuestion())) {
            checkDuplicateQuestionForUpdate(question.getId(), request.getQuestion().trim());
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

        // Difficulty 검증
        if (request.getDifficulty() != null && !DIFFICULTY_MAPPING.containsKey(request.getDifficulty().toUpperCase())) {
            throw new CsvProcessingException("난이도는 EASY, MEDIUM, HARD 중 하나여야 합니다.", "INVALID_DIFFICULTY");
        }

        // Question 공백 검증
        if (request.getQuestion() != null && request.getQuestion().trim().isEmpty()) {
            throw new CsvProcessingException("질문은 공백일 수 없습니다.", "INVALID_QUESTION");
        }

        // Interview Type 검증
        if (request.getInterviewType() != null) {
            if (!request.getInterviewType().equals("기술면접") && !request.getInterviewType().equals("인성면접")) {
                throw new CsvProcessingException("면접 타입은 '기술면접' 또는 '인성면접'이어야 합니다.", "INVALID_INTERVIEW_TYPE");
            }
        }
    }

    private void checkDuplicateQuestionForUpdate(Long currentQuestionId, String newQuestion) {
        questionRepository.findByQuestion(newQuestion).ifPresent(existing -> {
            if (!existing.getId().equals(currentQuestionId)) {
                throw new CsvProcessingException("동일한 질문이 이미 존재합니다.", "DUPLICATE_QUESTION");
            }
        });
    }

    private void applyPartialUpdate(Question question, QuestionUpdateRequest request, Long userId) {
        if (request.getQuestion() != null) {
            question.setQuestion(request.getQuestion().trim());
        }

        if (request.getInterviewType() != null) {
            question.setTitle(request.getInterviewType());
        }

        if (request.getDifficulty() != null) {
            question.setDifficulty(DIFFICULTY_MAPPING.get(request.getDifficulty().toUpperCase()));
        }

        if (request.getCategoryId() != null) {
            question.setCategoryId(request.getCategoryId());
        }

        if (request.getYear() != null) {
            question.setYear(request.getYear());
        }

        if (request.getCompanyName() != null && !request.getCompanyName().trim().isEmpty()) {
            String companyName = request.getCompanyName().trim();
            Company company = companyRepository.findByName(companyName)
                    .orElseGet(() -> createNewCompany(companyName));
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
                null,  // tags
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