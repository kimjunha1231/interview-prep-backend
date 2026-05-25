package com.junha.interview.service;

import com.junha.interview.domain.InterviewHistory;
import com.junha.interview.domain.InterviewSession;
import com.junha.interview.domain.Member;
import com.junha.interview.domain.Question;
import com.junha.interview.repository.InterviewHistoryRepository;
import com.junha.interview.repository.InterviewSessionRepository;
import com.junha.interview.repository.MemberRepository;
import com.junha.interview.repository.QuestionRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
public class InterviewServiceTest {

    @InjectMocks
    private InterviewService interviewService;

    @Mock
    private MemberRepository memberRepository;

    @Mock
    private InterviewSessionRepository sessionRepository;

    @Mock
    private InterviewHistoryRepository historyRepository;

    @Mock
    private QuestionRepository questionRepository;

    @Mock
    private GeminiService geminiService;

    @Test
    @DisplayName("비회원 게스트로 면접 세션을 정상적으로 시작한다")
    void testStartSessionForGuest() {
        // Given
        Question question = new Question(1L, "Frontend", "JavaScript", "What is closure?", "Closure is...");
        given(questionRepository.findRandomQuestions("Frontend", "JavaScript", 1))
                .willReturn(List.of(question));
        given(sessionRepository.save(any(InterviewSession.class)))
                .willAnswer(invocation -> {
                    InterviewSession session = invocation.getArgument(0);
                    return new InterviewSession(100L, session.getMember(), session.getCreatedAt());
                });

        // When
        InterviewSession session = interviewService.startSession(null, "Frontend", "JavaScript", 1);

        // Then
        assertThat(session.getId()).isEqualTo(100L);
        assertThat(session.getMember()).isNull();
        verify(sessionRepository).save(any(InterviewSession.class));
    }

    @Test
    @DisplayName("존재하지 않는 회원 ID로 세션 시작을 시도하면 예외를 던진다")
    void testStartSessionWithInvalidMemberId() {
        // Given
        given(memberRepository.findById(999L)).willReturn(Optional.empty());

        // When & Then
        assertThatThrownBy(() -> interviewService.startSession(999L, "Frontend", "JavaScript", 1))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("존재하지 않는 회원입니다");
    }

    @Test
    @DisplayName("답변을 정상적으로 제출하고 가상 피드백 결과를 반환받으며 DB에 저장한다")
    void testSubmitAnswer() {
        // Given
        InterviewSession session = new InterviewSession(100L, null, LocalDateTime.now());
        session.setAccessKey("test-uuid-key");
        Question question = new Question(1L, "Frontend", "JavaScript", "What is closure?", "Closure is...");

        given(sessionRepository.findByAccessKey("test-uuid-key")).willReturn(Optional.of(session));
        given(questionRepository.findById(1L)).willReturn(Optional.of(question));
        given(geminiService.evaluateAnswer(any(), any(), any(), any(), any()))
                .willReturn(new GeminiService.GeminiEvaluation(90, "Good feedback", "Tail question"));
        given(historyRepository.save(any(InterviewHistory.class)))
                .willAnswer(invocation -> {
                    InterviewHistory history = invocation.getArgument(0);
                    return new InterviewHistory(200L, history.getSession(), history.getQuestion(),
                            history.getUserAnswer(), history.getScore(), history.getFeedback(), history.getTailQuestion());
                });

        // When
        InterviewHistory history = interviewService.submitAnswer("test-uuid-key", 1L, "My Answer");

        // Then
        assertThat(history.getId()).isEqualTo(200L);
        assertThat(history.getUserAnswer()).isEqualTo("My Answer");
        assertThat(history.getScore()).isEqualTo(90);
        assertThat(history.getFeedback()).isEqualTo("Good feedback");
        assertThat(history.getTailQuestion()).isEqualTo("Tail question");
        verify(historyRepository).save(any(InterviewHistory.class));
    }

    @Test
    @DisplayName("존재하지 않는 세션 ID로 답변 제출 시 예외를 던진다")
    void testSubmitAnswerWithInvalidSession() {
        // Given
        given(sessionRepository.findByAccessKey("invalid-key")).willReturn(Optional.empty());

        // When & Then
        assertThatThrownBy(() -> interviewService.submitAnswer("invalid-key", 1L, "My Answer"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("존재하지 않는 면접 세션입니다");
    }
}
