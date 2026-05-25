package com.junha.interview.listener;

import com.junha.interview.event.InterviewCompletedEvent;
import com.junha.interview.service.EmailService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class EmailListener {

    private final EmailService emailService;

    @Async("mailExecutor")
    @EventListener
    public void handleInterviewCompletedEvent(InterviewCompletedEvent event) {
        log.info("Received InterviewCompletedEvent for Session ID: {} in thread: {}", event.getSessionId(), Thread.currentThread().getName());
        try {
            emailService.sendReportEmail(event.getEmail(), event.getSessionId(), event.getHistoryList());
        } catch (Exception e) {
            log.error("Unhandled exception in EmailListener for Session ID: {}. Error: {}", event.getSessionId(), e.getMessage(), e);
        }
    }
}
