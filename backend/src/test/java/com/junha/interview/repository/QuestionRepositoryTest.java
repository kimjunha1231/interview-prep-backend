package com.junha.interview.repository;

import com.junha.interview.domain.Question;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.test.context.ActiveProfiles;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@ActiveProfiles("test")
public class QuestionRepositoryTest {

    @Autowired
    private QuestionRepository questionRepository;

    @BeforeEach
    void setUp() {
        questionRepository.deleteAll();
        questionRepository.save(new Question(null, "Frontend", "JavaScript", "What is closure?", "Closure is..."));
        questionRepository.save(new Question(null, "Frontend", "JavaScript", "What is hoisting?", "Hoisting is..."));
        questionRepository.save(new Question(null, "Backend", "Java", "What is GC?", "GC is..."));
    }

    @Test
    @DisplayName("카테고리와 과목으로 필터링하고 지정된 개수만큼 랜덤 질문을 조회한다")
    void testFindRandomQuestions() {
        // Given & When
        List<Question> questions = questionRepository.findRandomQuestions("Frontend", "JavaScript", 1);

        // Then
        assertThat(questions).hasSize(1);
        assertThat(questions.get(0).getCategory()).isEqualTo("Frontend");
        assertThat(questions.get(0).getSubject()).isEqualTo("JavaScript");
    }
}
