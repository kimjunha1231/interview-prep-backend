package com.junha.interview.dto.interview;

import com.junha.interview.domain.Question;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class QuestionResponse {
    private Long id;
    private String category;
    private String subject;
    private String title;
    private String perfectAnswer;

    public static QuestionResponse from(Question question) {
        if (question == null) return null;
        return new QuestionResponse(
                question.getId(),
                question.getCategory(),
                question.getSubject(),
                question.getTitle(),
                question.getPerfectAnswer()
        );
    }
}
