package com.junha.interview.scheduler;

import com.junha.interview.common.EmailMaskUtils;
import com.junha.interview.common.EncryptionUtils;
import com.junha.interview.domain.EmailSubscription;
import com.junha.interview.domain.Question;
import com.junha.interview.repository.EmailSubscriptionRepository;
import com.junha.interview.repository.QuestionRepository;
import com.junha.interview.service.EmailService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
@Slf4j
public class DailyQuestionScheduler {

    private final EmailSubscriptionRepository subscriptionRepository;
    private final QuestionRepository questionRepository;
    private final EmailService emailService;
    private final EncryptionUtils encryptionUtils;

    /**
     * 매일 오전 9시에 기동되어 활성화된 구독자 전체에게
     * 데이터베이스 내 무작위 면접 질문 1개를 선정하여 이메일로 자동 전송합니다.
     * cron = "0 0 9 * * ?" (매일 9시 0분 0초)
     */
    @Scheduled(cron = "${app.scheduler.daily-cron:0 0 9 * * ?}", zone = "Asia/Seoul")
    public void sendDailyInterviewQuestion() {
        log.info("Daily Interview Question Scheduler triggered.");

        List<EmailSubscription> subscribers = subscriptionRepository.findAllByActiveTrue();
        if (subscribers.isEmpty()) {
            log.info("No active email subscriptions found. Skipping daily question delivery.");
            return;
        }

        List<Question> allQuestions = questionRepository.findAll();
        if (allQuestions.isEmpty()) {
            log.warn("No interview questions found in the database. Daily delivery aborted.");
            return;
        }

        log.info("Loaded {} total questions for daily challenge distribution.", allQuestions.size());

        for (EmailSubscription sub : subscribers) {
            String decryptedEmail = "unknown";
            try {
                decryptedEmail = encryptionUtils.decrypt(sub.getEncryptedEmail());
                String targetCat = sub.getCategory();
                List<Question> candidates;
                if (targetCat == null || "ALL".equalsIgnoreCase(targetCat) || targetCat.trim().isEmpty()) {
                    candidates = allQuestions;
                } else {
                    List<String> targetSubjects = java.util.Arrays.stream(targetCat.split(","))
                            .map(String::trim)
                            .map(String::toUpperCase)
                            .filter(s -> !s.isEmpty())
                            .collect(Collectors.toList());

                    if (targetSubjects.contains("ALL") || targetSubjects.isEmpty()) {
                        candidates = allQuestions;
                    } else {
                        candidates = allQuestions.stream()
                                .filter(q -> q.getSubject() != null && targetSubjects.contains(q.getSubject().trim().toUpperCase()))
                                .collect(Collectors.toList());
                    }
                }

                if (candidates == null || candidates.isEmpty()) {
                    log.warn("No questions found for category '{}' requested by {}. Falling back to all questions.", targetCat, EmailMaskUtils.mask(decryptedEmail));
                    candidates = allQuestions;
                }
 
                // 무작위로 1개 선정
                List<Question> shuffledCandidates = new ArrayList<>(candidates);
                Collections.shuffle(shuffledCandidates);
                Question targetQuestion = shuffledCandidates.get(0);
 
                log.info("Sending daily question '{}' (Category: {}) to user: {}",
                        targetQuestion.getTitle(), targetQuestion.getCategory(), EmailMaskUtils.mask(decryptedEmail));
 
                // EmailService 내 비동기(@Async) 처리로 인해 메인 스케줄러 루프가 대기하지 않고 즉시 다음 스레드로 넘겨 전송함
                emailService.sendDailyQuestionEmail(decryptedEmail, targetQuestion);
            } catch (Exception e) {
                log.error("Failed to enqueue daily question email for user: {}. Error: {}", EmailMaskUtils.mask(decryptedEmail), e.getMessage(), e);
            }
        }
 
        log.info("All daily question email dispatches enqueued successfully.");
    }

}
