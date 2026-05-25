package com.junha.interview.service;

import com.junha.interview.domain.InterviewHistory;
import com.junha.interview.domain.InterviewSession;
import com.junha.interview.domain.Member;
import com.junha.interview.domain.Question;
import com.junha.interview.repository.InterviewHistoryRepository;
import com.junha.interview.repository.InterviewSessionRepository;
import com.junha.interview.repository.MemberRepository;
import com.junha.interview.event.InterviewCompletedEvent;
import com.junha.interview.repository.QuestionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class InterviewService {

    private final MemberRepository memberRepository;
    private final InterviewSessionRepository sessionRepository;
    private final InterviewHistoryRepository historyRepository;
    private final QuestionRepository questionRepository;
    private final GeminiService geminiService;
    private final ApplicationEventPublisher eventPublisher;

    @Transactional
    public InterviewSession startSession(Long memberId, String category, String subject, int count) {
        return startSession(memberId, category, subject, count, null);
    }

    @Transactional
    public InterviewSession startSession(Long memberId, String category, String subject, int count, String portfolioText) {
        return startSession(memberId, category, subject, null, count, portfolioText);
    }

    @Transactional
    public InterviewSession startSession(Long memberId, String category, String subject, List<String> subjects, int count, String portfolioText) {
        Member member = null;
        if (memberId != null) {
            member = memberRepository.findById(memberId)
                    .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 회원입니다."));
        }

        // 카테고리 명칭 보정 (FE -> Frontend, BE -> Backend)
        String dbCategory = category;
        if ("FE".equalsIgnoreCase(category)) {
            dbCategory = "Frontend";
        } else if ("BE".equalsIgnoreCase(category)) {
            dbCategory = "Backend";
        }

        List<Question> questions;
        if ("PORTFOLIO".equalsIgnoreCase(dbCategory) && portfolioText != null && !portfolioText.trim().isEmpty()) {
            // 포트폴리오 기반 실시간 생성 문항 적용
            List<GeminiService.PortfolioQuestion> generated = geminiService.generateQuestionsFromPortfolio(portfolioText, count);
            questions = generated.stream().map(gq -> {
                Question q = new Question();
                q.setCategory(gq.getCategory());
                q.setSubject(gq.getSubject());
                q.setTitle(gq.getTitle());
                q.setPerfectAnswer(gq.getPerfectAnswer());
                return questionRepository.save(q);
            }).toList();
        } else {
            // 일반 카테고리 랜덤 문항 적용
            if (subjects != null && !subjects.isEmpty()) {
                List<String> lowerSubjects = subjects.stream()
                        .map(String::toLowerCase)
                        .toList();
                List<Question> candidates = questionRepository.findQuestionsInSubjects(lowerSubjects);
                java.util.List<Question> shuffled = new java.util.ArrayList<>(candidates);
                java.util.Collections.shuffle(shuffled);
                questions = shuffled.stream().limit(count).toList();
            } else {
                questions = questionRepository.findRandomQuestions(dbCategory, subject, count);
            }
        }

        InterviewSession session = new InterviewSession(null, member, LocalDateTime.now());
        InterviewSession savedSession = sessionRepository.save(session);
        savedSession.setQuestions(questions);
        return savedSession;
    }

    @Transactional
    public InterviewHistory submitAnswer(String accessKey, Long questionId, String userAnswer) {
        InterviewSession session = sessionRepository.findByAccessKey(accessKey)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 면접 세션입니다."));

        Question question = questionRepository.findById(questionId)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 질문입니다."));

        // 실제 Gemini AI 채점 서비스 호출
        GeminiService.GeminiEvaluation evaluation = geminiService.evaluateAnswer(
                question.getCategory(),
                question.getSubject(),
                question.getTitle(),
                question.getPerfectAnswer(),
                userAnswer
        );

        InterviewHistory history = new InterviewHistory(
                null,
                session,
                question,
                userAnswer,
                evaluation.getScore(),
                evaluation.getFeedback(),
                evaluation.getTailQuestion()
        );
        return historyRepository.save(history);
    }

    public List<InterviewHistory> getSessionHistory(String accessKey) {
        InterviewSession session = sessionRepository.findByAccessKey(accessKey)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 면접 세션입니다."));
        return historyRepository.findBySessionId(session.getId());
    }

    @Transactional
    public void sendReportEmail(String accessKey, String email) {
        if (email == null || email.trim().isEmpty()) {
            throw new IllegalArgumentException("이메일 주소는 필수입니다.");
        }

        InterviewSession session = sessionRepository.findByAccessKey(accessKey)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 면접 세션입니다."));

        if (session.isEmailSent()) {
            throw new IllegalArgumentException("이미 이메일 보고서가 발송된 세션입니다. 중복 발송할 수 없습니다.");
        }

        List<InterviewHistory> historyList = historyRepository.findBySessionId(session.getId());
        if (historyList.isEmpty()) {
            throw new IllegalArgumentException("해당 세션의 면접 내역이 존재하지 않습니다.");
        }

        session.setEmailSent(true);
        sessionRepository.save(session);

        // 비동기 이벤트 발행
        eventPublisher.publishEvent(new InterviewCompletedEvent(session.getId(), email, historyList));
    }
}
