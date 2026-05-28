package com.junha.interview.service;

import com.junha.interview.domain.Question;
import com.junha.interview.dto.QuestionSummaryDto;
import com.junha.interview.repository.QuestionRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Collections;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class QuestionServiceTest {

    @Mock
    private QuestionRepository questionRepository;

    @InjectMocks
    private QuestionService questionService;

    @Test
    @DisplayName("count가 비어있거나 1 미만일 경우 기본값 1로 제한하고, 100을 초과하면 100으로 제한한다")
    void testGetRandomQuestionsCountBoundary() {
        // Given
        when(questionRepository.findRandomQuestions(any(), any(), anyInt()))
                .thenReturn(Collections.emptyList());

        // When - count가 null일 때
        questionService.getRandomQuestions("Frontend", "JavaScript", null);
        verify(questionRepository).findRandomQuestions("Frontend", "JavaScript", 1);

        // When - count가 1 미만일 때
        questionService.getRandomQuestions("Frontend", "JavaScript", -5);
        verify(questionRepository, times(2)).findRandomQuestions("Frontend", "JavaScript", 1);

        // When - count가 100을 초과할 때
        questionService.getRandomQuestions("Frontend", "JavaScript", 120);
        verify(questionRepository).findRandomQuestions("Frontend", "JavaScript", 100);
    }

    @Test
    @DisplayName("빈 문자열인 카테고리와 과목은 Null로 보정되어 레포지토리에 전달된다")
    void testGetRandomQuestionsEmptyParamsHandling() {
        // Given
        when(questionRepository.findRandomQuestions(any(), any(), anyInt()))
                .thenReturn(Collections.emptyList());

        // When
        questionService.getRandomQuestions("  ", "", 5);

        // Then
        verify(questionRepository).findRandomQuestions(null, null, 5);
    }

    @Test
    @DisplayName("getQuestionSummaries API 요청 시 질문 목록을 Summary Dto 형태로 매핑하여 반환한다")
    void testGetQuestionSummaries() {
        // Given
        Question question = new Question(1L, "Frontend", "JavaScript", "What is closure?", "Closure is...");
        when(questionRepository.findQuestionsActive("Frontend", "JavaScript"))
                .thenReturn(List.of(question));

        // When
        List<QuestionSummaryDto> summaries = questionService.getQuestionSummaries("Frontend", "JavaScript");

        // Then
        assertThat(summaries).hasSize(1);
        assertThat(summaries.get(0).getId()).isEqualTo(1L);
        assertThat(summaries.get(0).getCategory()).isEqualTo("Frontend");
        assertThat(summaries.get(0).getSubject()).isEqualTo("JavaScript");
        assertThat(summaries.get(0).getTitle()).isEqualTo("What is closure?");
    }
}
