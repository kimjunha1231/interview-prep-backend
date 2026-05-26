package com.junha.interview.event;

import com.junha.interview.domain.InterviewHistory;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.List;

@Getter
@AllArgsConstructor
public class InterviewCompletedEvent {
    private final Long sessionId;
    private final String email;
    private final List<InterviewHistory> historyList;
}
