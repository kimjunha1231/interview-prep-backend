package com.junha.interview.dto.interview;

import com.junha.interview.domain.InterviewHistory;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class InterviewHistoryResponse {
    private Long id;
    private Long sessionId;
    private Long questionId;
    private String userAnswer;
    private int score;
    private String feedback;
    private String tailQuestion;

    public static InterviewHistoryResponse from(InterviewHistory history) {
        if (history == null) return null;
        return new InterviewHistoryResponse(
                history.getId(),
                history.getSession().getId(),
                history.getQuestion().getId(),
                history.getUserAnswer(),
                history.getScore(),
                history.getFeedback(),
                history.getTailQuestion()
        );
    }
}
