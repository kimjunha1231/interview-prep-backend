package com.junha.interview.service;

import com.junha.interview.domain.EmailSubscription;
import com.junha.interview.repository.EmailSubscriptionRepository;
import com.junha.interview.common.EncryptionUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.regex.Pattern;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
@Slf4j
public class SubscriptionService {

    private final EmailSubscriptionRepository subscriptionRepository;
    private final EncryptionUtils encryptionUtils;

    private static final Pattern EMAIL_PATTERN = Pattern.compile(
            "^[A-Za-z0-9+_.-]+@(.+)$"
    );

    @Transactional
    public void subscribe(String email) {
        subscribe(email, "ALL");
    }

    @Transactional
    public void subscribe(String email, String category) {
        if (email == null || email.trim().isEmpty()) {
            throw new IllegalArgumentException("이메일 주소는 필수 입력값입니다.");
        }
        String normalizedEmail = email.trim().toLowerCase();
        if (!EMAIL_PATTERN.matcher(normalizedEmail).matches()) {
            throw new IllegalArgumentException("올바른 이메일 형식이 아닙니다.");
        }

        // 카테고리 정규화
        String dbCategory = "ALL";
        if (category != null) {
            String temp = category.trim().toUpperCase();
            if ("FE".equals(temp) || "FRONTEND".equals(temp)) {
                dbCategory = "Frontend";
            } else if ("BE".equals(temp) || "BACKEND".equals(temp)) {
                dbCategory = "Backend";
            } else if ("CS".equals(temp)) {
                dbCategory = "CS";
            }
        }

        String emailHash = encryptionUtils.hash(normalizedEmail);
        String encryptedEmail = encryptionUtils.encrypt(normalizedEmail);

        Optional<EmailSubscription> existingOpt = subscriptionRepository.findByEmailHash(emailHash);
        if (existingOpt.isPresent()) {
            EmailSubscription existing = existingOpt.get();
            existing.setCategory(dbCategory);
            existing.setEncryptedEmail(encryptedEmail); // Update encrypted email just in case the key changed
            if (existing.isActive()) {
                subscriptionRepository.save(existing);
                log.info("Successfully updated subscription category to {} for email: {}", dbCategory, maskEmail(normalizedEmail));
            } else {
                existing.setActive(true);
                subscriptionRepository.save(existing);
                log.info("Successfully re-activated subscription for email: {} with category: {}", maskEmail(normalizedEmail), dbCategory);
            }
        } else {
            EmailSubscription newSubscription = new EmailSubscription(
                    null,
                    emailHash,
                    encryptedEmail,
                    dbCategory,
                    true,
                    LocalDateTime.now()
            );
            subscriptionRepository.save(newSubscription);
            log.info("Successfully registered new subscription for email: {} with category: {}", maskEmail(normalizedEmail), dbCategory);
        }
    }

    @Transactional
    public void unsubscribe(String email) {
        if (email == null || email.trim().isEmpty()) {
            throw new IllegalArgumentException("이메일 주소는 필수입니다.");
        }
        String normalizedEmail = email.trim().toLowerCase();
        String emailHash = encryptionUtils.hash(normalizedEmail);
        EmailSubscription subscription = subscriptionRepository.findByEmailHash(emailHash)
                .orElseThrow(() -> new IllegalArgumentException("등록되지 않은 이메일 주소입니다."));

        if (!subscription.isActive()) {
            log.warn("Subscription for email {} is already inactive.", maskEmail(normalizedEmail));
            return;
        }

        subscription.setActive(false);
        subscriptionRepository.save(subscription);
        log.info("Successfully unsubscribed email: {}", maskEmail(normalizedEmail));
    }

    @Transactional
    public void unsubscribeByHash(String emailHash) {
        if (emailHash == null || emailHash.trim().isEmpty()) {
            throw new IllegalArgumentException("구독 해제 토큰이 누락되었습니다.");
        }
        EmailSubscription subscription = subscriptionRepository.findByEmailHash(emailHash)
                .orElseThrow(() -> new IllegalArgumentException("올바르지 않은 구독 해제 토큰이거나 등록되지 않은 정보입니다."));

        if (!subscription.isActive()) {
            log.warn("Subscription for hash {} is already inactive.", emailHash);
            return;
        }

        subscription.setActive(false);
        subscriptionRepository.save(subscription);
        log.info("Successfully unsubscribed by token hash: {}", emailHash);
    }


    private String maskEmail(String email) {
        if (email == null || !email.contains("@")) {
            return email;
        }
        int atIndex = email.indexOf("@");
        String local = email.substring(0, atIndex);
        String domain = email.substring(atIndex);
        if (local.length() <= 2) {
            return local + "***" + domain;
        }
        return local.substring(0, 2) + "***" + domain;
    }
}

