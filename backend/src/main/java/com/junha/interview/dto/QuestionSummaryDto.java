package com.junha.interview.dto;

import com.junha.interview.domain.Question;
import lombok.Getter;

/**
 * 핸드북 목록 표시용 경량 DTO.
 * perfectAnswer, explanation, caveats, tailQuestions, references 제외 → 응답 크기 ~20x 감소
 */
@Getter
public class QuestionSummaryDto {

    private final Long id;
    private final String category;
    private final String subject;
    private final String title;
    private final String summary;
    private final Integer importance;

    public QuestionSummaryDto(Question q) {
        this.id = q.getId();
        this.category = q.getCategory();
        this.subject = q.getSubject();
        this.title = q.getTitle();
        this.summary = q.getSummary();
        this.importance = q.getImportance();
    }
}
