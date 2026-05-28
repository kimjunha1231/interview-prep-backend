package com.junha.interview.service;

import com.junha.interview.domain.Question;
import com.junha.interview.dto.QuestionSummaryDto;
import com.junha.interview.repository.QuestionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class QuestionService {

    private final QuestionRepository questionRepository;

    public List<Question> getRandomQuestions(String category, String subject, Integer count) {
        // count 파라미터 보정 (null이거나 1미만이면 1로, 100을 초과하면 100으로 고정)
        int limit = (count == null || count < 1) ? 1 : Math.min(count, 100);
        
        // category, subject 파라미터가 Null이거나 공백문자일 때 DB 쿼리 필터 우회를 위해 null로 변환
        String categoryParam = (category == null || category.trim().isEmpty()) ? null : category.trim();
        String subjectParam = (subject == null || subject.trim().isEmpty()) ? null : subject.trim();

        return questionRepository.findRandomQuestions(categoryParam, subjectParam, limit);
    }

    @Cacheable(value = "questions", key = "#category + '_' + #subject")
    public List<Question> getQuestions(String category, String subject) {
        String categoryParam = (category == null || category.trim().isEmpty()) ? null : category.trim();
        String subjectParam = (subject == null || subject.trim().isEmpty()) ? null : subject.trim();
        return questionRepository.findQuestionsActive(categoryParam, subjectParam);
    }

    @Cacheable(value = "questionSummaries", key = "#category + '_' + #subject")
    public List<QuestionSummaryDto> getQuestionSummaries(String category, String subject) {
        return getQuestions(category, subject).stream()
                .map(QuestionSummaryDto::new)
                .collect(Collectors.toList());
    }

    @Cacheable(value = "questionDetail", key = "#id")
    public Question getQuestionById(Long id) {
        return questionRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Question not found with id: " + id));
    }
}
