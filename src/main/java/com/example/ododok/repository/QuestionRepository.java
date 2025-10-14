package com.example.ododok.repository;

import com.example.ododok.entity.Question;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface QuestionRepository extends JpaRepository<Question, Long> {

    Optional<Question> findByQuestion(String question);

    Optional<Question> findByTitle(String title);

    @Query("SELECT q FROM Question q " +
            "WHERE q.isPublic = true " +
            "AND (:categoryId IS NULL OR q.categoryId = :categoryId) " +
            "AND (:companyName IS NULL OR q.company.name = :companyName) " +
            "ORDER BY FUNCTION('RANDOM')")
    List<Question> findRandomQuestionsWithFilters(
            @Param("categoryId") Long categoryId,
            @Param("companyName") String companyName,
            org.springframework.data.domain.Pageable pageable
    );

    // company.name으로 수정
    @Query("SELECT q FROM Question q WHERE q.question = :question AND " +
            "(:year IS NULL OR q.year = :year) AND " +
            "(:companyName IS NULL OR q.company.name = :companyName) AND " +
            "(:categoryId IS NULL OR q.categoryId = :categoryId)")
    Optional<Question> findByQuestionAndYearAndCompanyNameAndCategoryId(
            @Param("question") String question,
            @Param("year") Integer year,
            @Param("companyName") String companyName,
            @Param("categoryId") Long categoryId);

    // Search methods
    @Query("SELECT q FROM Question q WHERE (q.isPublic = true OR q.createdBy = :userId)")
    org.springframework.data.domain.Page<Question> findAllByIsPublicTrueOrCreatedBy(
            @Param("userId") Long userId,
            org.springframework.data.domain.Pageable pageable);

    @Query("SELECT q FROM Question q WHERE " +
            "(LOWER(q.question) LIKE :searchTerm OR LOWER(q.content) LIKE :searchTerm OR LOWER(q.title) LIKE :searchTerm) " +
            "AND (q.isPublic = true OR q.createdBy = :userId)")
    org.springframework.data.domain.Page<Question> findByText(
            @Param("searchTerm") String searchTerm,
            @Param("userId") Long userId,
            org.springframework.data.domain.Pageable pageable);

    @Query("SELECT q FROM Question q WHERE " +
            "(LOWER(q.question) LIKE :searchTerm OR LOWER(q.content) LIKE :searchTerm OR LOWER(q.title) LIKE :searchTerm) " +
            "AND q.difficulty = :difficulty " +
            "AND (q.isPublic = true OR q.createdBy = :userId)")
    org.springframework.data.domain.Page<Question> findByTextAndDifficulty(
            @Param("searchTerm") String searchTerm,
            @Param("difficulty") Integer difficulty,
            @Param("userId") Long userId,
            org.springframework.data.domain.Pageable pageable);

    @Query("SELECT q FROM Question q WHERE " +
            "(LOWER(q.question) LIKE :searchTerm OR LOWER(q.content) LIKE :searchTerm OR LOWER(q.title) LIKE :searchTerm) " +
            "AND q.categoryId = :categoryId " +
            "AND (q.isPublic = true OR q.createdBy = :userId)")
    org.springframework.data.domain.Page<Question> findByTextAndCategory(
            @Param("searchTerm") String searchTerm,
            @Param("categoryId") Long categoryId,
            @Param("userId") Long userId,
            org.springframework.data.domain.Pageable pageable);

    @Query("SELECT q FROM Question q WHERE " +
            "(LOWER(q.question) LIKE :searchTerm OR LOWER(q.content) LIKE :searchTerm OR LOWER(q.title) LIKE :searchTerm) " +
            "AND q.difficulty = :difficulty " +
            "AND q.categoryId = :categoryId " +
            "AND (q.isPublic = true OR q.createdBy = :userId)")
    org.springframework.data.domain.Page<Question> findByTextAndDifficultyAndCategory(
            @Param("searchTerm") String searchTerm,
            @Param("difficulty") Integer difficulty,
            @Param("categoryId") Long categoryId,
            @Param("userId") Long userId,
            org.springframework.data.domain.Pageable pageable);

    // company.name으로 수정
    @Query("SELECT q FROM Question q WHERE " +
            "(:searchTerm = '' OR LOWER(q.question) LIKE :searchTerm OR LOWER(q.content) LIKE :searchTerm OR LOWER(q.title) LIKE :searchTerm) " +
            "AND (:year IS NULL OR q.year = :year) " +
            "AND (:companyName IS NULL OR q.company.name = :companyName) " +
            "AND (q.isPublic = true OR q.createdBy = :userId)")
    org.springframework.data.domain.Page<Question> findByFilters(
            @Param("searchTerm") String searchTerm,
            @Param("year") Integer year,
            @Param("companyName") String companyName,
            @Param("userId") Long userId,
            org.springframework.data.domain.Pageable pageable);

    @Query("SELECT q FROM Question q JOIN q.company c WHERE " +
            "(CAST(:searchText AS string) IS NULL OR " +
            "LOWER(q.question) LIKE LOWER(CONCAT('%', CAST(:searchText AS string), '%')) OR " +
            "LOWER(q.content) LIKE LOWER(CONCAT('%', CAST(:searchText AS string), '%'))) " +
            "AND (:difficulty IS NULL OR q.difficulty = :difficulty) " +
            "AND (:year IS NULL OR q.year = :year) " +
            "AND (CAST(:companyName AS string) IS NULL OR " +
            "LOWER(c.name) LIKE LOWER(CONCAT('%', CAST(:companyName AS string), '%'))) " +
            "AND (:categoryId IS NULL OR q.categoryId = :categoryId) " +
            "AND (CAST(:interviewType AS string) IS NULL OR q.title = CAST(:interviewType AS string)) " +
            "AND (q.isPublic = true OR q.createdBy = :userId)")
    org.springframework.data.domain.Page<Question> findByAllFilters(
            @Param("searchText") String searchText,
            @Param("difficulty") Integer difficulty,
            @Param("year") Integer year,
            @Param("companyName") String companyName,
            @Param("categoryId") Long categoryId,
            @Param("interviewType") String interviewType,
            @Param("userId") Long userId,
            org.springframework.data.domain.Pageable pageable);

    // Count methods - company.name으로 수정
    int countByDifficulty(Integer difficulty);

    @Query("SELECT COUNT(q) FROM Question q WHERE q.year = :year")
    int countByYear(@Param("year") Integer year);

    @Query("SELECT COUNT(q) FROM Question q WHERE q.company.name = :companyName")
    int countByCompanyName(@Param("companyName") String companyName);

    @Query("SELECT COUNT(q) FROM Question q WHERE q.categoryId = :categoryId")
    Long countByCategoryId(@Param("categoryId") Long categoryId);

    @Query("SELECT DISTINCT q.title FROM Question q WHERE q.title IS NOT NULL ORDER BY q.title")
    List<String> findDistinctInterviewTypes();

    @Query("SELECT COUNT(q) FROM Question q WHERE q.title = :interviewType")
    Long countByInterviewType(@Param("interviewType") String interviewType);

    @Query("SELECT DISTINCT q.year FROM Question q WHERE q.year IS NOT NULL ORDER BY q.year DESC")
    java.util.List<Integer> findDistinctYears();

    @Query("SELECT DISTINCT q.company.name FROM Question q WHERE q.company IS NOT NULL")
    java.util.List<String> findDistinctCompanyNames();

    // CSV 업로드 성능 최적화를 위한 배치 조회
    List<Question> findAllByQuestionIn(List<String> questions);
}