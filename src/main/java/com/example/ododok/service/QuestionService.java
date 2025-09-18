package com.example.ododok.service;

import com.example.ododok.dto.QuestionCreateRequest;
import com.example.ododok.dto.QuestionResponse;
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
        // Company ID 검증
        if (request.getCompanyId() != null) {
            if (!companyRepository.existsById(request.getCompanyId())) {
                throw new CsvProcessingException("연결하려는 회사를 찾을 수 없습니다.", "COMPANY_NOT_FOUND");
            }
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
        question.setCreatedBy(userId);

        // Difficulty 매핑
        String difficulty = request.getDifficulty() != null ? request.getDifficulty().toUpperCase() : "MEDIUM";
        question.setDifficulty(DIFFICULTY_MAPPING.get(difficulty));

        // 선택적 필드 설정
        question.setYear(request.getYear());
        question.setCompanyId(request.getCompanyId());
        question.setCategoryId(request.getCategoryId());

        return question;
    }

    private void checkDuplicateQuestion(Question question) {
        if (questionRepository.findByQuestion(question.getQuestion()).isPresent()) {
            throw new CsvProcessingException("동일한 제목의 질문이 이미 존재합니다.", "DUPLICATE_QUESTION");
        }
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

    private QuestionResponse mapToResponse(Question question) {
        return new QuestionResponse(
                question.getId(),
                question.getQuestion(),
                question.getDifficulty(),
                question.getYear(),
                question.getCompanyId(),
                question.getCategoryId(),
                question.getCreatedAt(),
                question.getCreatedBy()
        );
    }
}