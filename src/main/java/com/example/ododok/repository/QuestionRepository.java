package com.example.ododok.repository;

import com.example.ododok.entity.Question;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface QuestionRepository extends JpaRepository<Question, Long> {

    Optional<Question> findByQuestion(String question);

    Optional<Question> findByTitle(String title);

    @Query("SELECT q FROM Question q WHERE q.question = :question AND " +
           "(:year IS NULL AND q.year IS NULL OR q.year = :year) AND " +
           "(:companyName IS NULL AND q.companyName IS NULL OR q.companyName = :companyName) AND " +
           "(:categoryId IS NULL AND q.categoryId IS NULL OR q.categoryId = :categoryId)")
    Optional<Question> findByQuestionAndYearAndCompanyNameAndCategoryId(
            @Param("question") String question,
            @Param("year") Integer year,
            @Param("companyName") String companyName,
            @Param("categoryId") Long categoryId);

    // Search methods
    @Query("SELECT q FROM Question q WHERE (q.isPublic = true OR q.createdBy = :userId)")
    org.springframework.data.domain.Page<Question> findAllByIsPublicTrueOrCreatedBy(
            @Param("isPublic") boolean isPublic,
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

    // New search methods with year and company filters
    @Query("SELECT q FROM Question q WHERE " +
           "(:searchTerm = '' OR LOWER(q.question) LIKE :searchTerm OR LOWER(q.content) LIKE :searchTerm OR LOWER(q.title) LIKE :searchTerm) " +
           "AND (:year IS NULL OR q.year = :year) " +
           "AND (:companyName IS NULL OR q.companyName = :companyName) " +
           "AND (q.isPublic = true OR q.createdBy = :userId)")
    org.springframework.data.domain.Page<Question> findByFilters(
            @Param("searchTerm") String searchTerm,
            @Param("year") Integer year,
            @Param("companyName") String companyName,
            @Param("userId") Long userId,
            org.springframework.data.domain.Pageable pageable);

    @Query("SELECT q FROM Question q WHERE " +
           "(:searchTerm = '' OR LOWER(q.question) LIKE :searchTerm OR LOWER(q.content) LIKE :searchTerm OR LOWER(q.title) LIKE :searchTerm) " +
           "AND (:difficulty IS NULL OR q.difficulty = :difficulty) " +
           "AND (:year IS NULL OR q.year = :year) " +
           "AND (:companyName IS NULL OR q.companyName = :companyName) " +
           "AND (:categoryId IS NULL OR q.categoryId = :categoryId) " +
           "AND (q.isPublic = true OR q.createdBy = :userId)")
    org.springframework.data.domain.Page<Question> findByAllFilters(
            @Param("searchTerm") String searchTerm,
            @Param("difficulty") Integer difficulty,
            @Param("year") Integer year,
            @Param("companyName") String companyName,
            @Param("categoryId") Long categoryId,
            @Param("userId") Long userId,
            org.springframework.data.domain.Pageable pageable);

    // Count methods for facets
    int countByDifficulty(Integer difficulty);

    @Query("SELECT COUNT(q) FROM Question q WHERE q.year = :year")
    int countByYear(@Param("year") Integer year);

    @Query("SELECT COUNT(q) FROM Question q WHERE q.companyName = :companyName")
    int countByCompanyName(@Param("companyName") String companyName);

    @Query("SELECT DISTINCT q.year FROM Question q WHERE q.year IS NOT NULL ORDER BY q.year DESC")
    java.util.List<Integer> findDistinctYears();

    @Query("SELECT DISTINCT q.companyName FROM Question q WHERE q.companyName IS NOT NULL")
    java.util.List<String> findDistinctCompanyNames();
}