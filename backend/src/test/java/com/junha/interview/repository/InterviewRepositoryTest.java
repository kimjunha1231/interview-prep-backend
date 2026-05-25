package com.junha.interview.repository;

import com.junha.interview.domain.Member;
import com.junha.interview.domain.InterviewSession;
import com.junha.interview.domain.InterviewHistory;
import com.junha.interview.domain.Question;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.test.context.ActiveProfiles;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@ActiveProfiles("test")
public class InterviewRepositoryTest {

    @Autowired
    private MemberRepository memberRepository;

    @Autowired
    private InterviewSessionRepository sessionRepository;

    @Autowired
    private InterviewHistoryRepository historyRepository;

    @Autowired
    private QuestionRepository questionRepository;

    private Question question;

    @BeforeEach
    void setUp() {
        historyRepository.deleteAll();
        sessionRepository.deleteAll();
        memberRepository.deleteAll();
        questionRepository.deleteAll();

        question = questionRepository.save(new Question(null, "Frontend", "JavaScript", "What is closure?", "Closure is..."));
    }

    @Test
    @DisplayName("회원, 세션, 이력 정보를 정상적으로 생성하고 저장한다")
    void testSaveInterviewDomain() {
        // 1. Member 저장
        Member member = memberRepository.save(new Member(null, "test@test.com", "Tester", LocalDateTime.now()));
        assertThat(member.getId()).isNotNull();

        // 2. InterviewSession 저장 (회원 매핑)
        InterviewSession session = sessionRepository.save(new InterviewSession(null, member, LocalDateTime.now()));
        assertThat(session.getId()).isNotNull();
        assertThat(session.getMember()).isEqualTo(member);

        // 3. InterviewHistory 저장
        InterviewHistory history = historyRepository.save(new InterviewHistory(null, session, question, "My Answer", 85, "Good feedback", "What is scope?"));
        assertThat(history.getId()).isNotNull();
        assertThat(history.getSession()).isEqualTo(session);
        assertThat(history.getQuestion()).isEqualTo(question);
        assertThat(history.getUserAnswer()).isEqualTo("My Answer");
        assertThat(history.getScore()).isEqualTo(85);
        assertThat(history.getFeedback()).isEqualTo("Good feedback");
        assertThat(history.getTailQuestion()).isEqualTo("What is scope?");
    }

    @Test
    @DisplayName("비회원 게스트 세션도 정상적으로 생성하고 저장할 수 있어야 한다")
    void testSaveGuestSession() {
        // 1. 비회원 InterviewSession 저장 (member = null)
        InterviewSession session = sessionRepository.save(new InterviewSession(null, null, LocalDateTime.now()));
        assertThat(session.getId()).isNotNull();
        assertThat(session.getMember()).isNull();
    }
}
