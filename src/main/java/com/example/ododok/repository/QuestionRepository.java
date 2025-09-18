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
           "(:companyId IS NULL AND q.companyId IS NULL OR q.companyId = :companyId) AND " +
           "(:categoryId IS NULL AND q.categoryId IS NULL OR q.categoryId = :categoryId)")
    Optional<Question> findByQuestionAndYearAndCompanyIdAndCategoryId(
            @Param("question") String question,
            @Param("year") Integer year,
            @Param("companyId") Long companyId,
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

    // Count methods for facets
    int countByDifficulty(Integer difficulty);
}