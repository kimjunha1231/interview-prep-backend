package com.junha.interview.dto.interview;

import com.junha.interview.domain.InterviewSession;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.time.LocalDateTime;
import java.util.List;

@Getter
@AllArgsConstructor
public class InterviewSessionResponse {
    private Long id;
    private String accessKey;
    private Long memberId;
    private LocalDateTime createdAt;
    private List<QuestionResponse> questions;

    public static InterviewSessionResponse from(InterviewSession session) {
        if (session == null) return null;
        List<QuestionResponse> questionDtos = session.getQuestions() == null ? null :
                session.getQuestions().stream().map(QuestionResponse::from).toList();
        Long memberId = session.getMember() != null ? session.getMember().getId() : null;
        return new InterviewSessionResponse(
                session.getId(),
                session.getAccessKey(),
                memberId,
                session.getCreatedAt(),
                questionDtos
        );
    }
}
