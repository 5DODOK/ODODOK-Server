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
}