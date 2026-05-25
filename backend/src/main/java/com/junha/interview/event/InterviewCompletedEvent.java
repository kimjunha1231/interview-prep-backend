package com.junha.interview.event;

import com.junha.interview.domain.InterviewHistory;
import lombok.Getter;
import java.util.List;

@Getter
public class InterviewCompletedEvent {
    private final Long sessionId;
    private final String email;
    private final List<InterviewHistory> historyList;

    public InterviewCompletedEvent(Long sessionId, String email, List<InterviewHistory> historyList) {
        this.sessionId = sessionId;
        this.email = email;
        this.historyList = historyList;
    }
}
