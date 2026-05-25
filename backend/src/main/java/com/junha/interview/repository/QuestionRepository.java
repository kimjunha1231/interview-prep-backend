package com.junha.interview.repository;

import com.junha.interview.domain.Question;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.util.List;

@Repository
public interface QuestionRepository extends JpaRepository<Question, Long> {

    @Query(value = "SELECT * FROM question " +
            "WHERE (:category IS NULL OR category = :category) " +
            "AND (:subject IS NULL OR LOWER(subject) = LOWER(:subject)) " +
            "ORDER BY RANDOM() LIMIT :count", 
            nativeQuery = true)
    List<Question> findRandomQuestions(
            @Param("category") String category,
            @Param("subject") String subject,
            @Param("count") int count
    );

    @Query("SELECT q FROM Question q WHERE LOWER(q.subject) IN :subjects")
    List<Question> findQuestionsInSubjects(@Param("subjects") List<String> subjects);

    @Query(value = "SELECT * FROM question " +
            "WHERE (:category IS NULL OR category = :category) " +
            "AND (:subject IS NULL OR LOWER(subject) = LOWER(:subject)) " +
            "ORDER BY id ASC", 
            nativeQuery = true)
    List<Question> findQuestionsActive(
            @Param("category") String category,
            @Param("subject") String subject
    );
}
