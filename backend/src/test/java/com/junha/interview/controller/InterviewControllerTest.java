package com.junha.interview.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.junha.interview.domain.InterviewHistory;
import com.junha.interview.domain.InterviewSession;
import com.junha.interview.domain.Question;
import com.junha.interview.service.InterviewService;
import com.junha.interview.service.GroqService;
import com.junha.interview.service.GeminiService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(InterviewController.class)
public class InterviewControllerTest {

    private static final String TEST_ACCESS_KEY = "550e8400-e29b-41d4-a716-446655440000";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private InterviewService interviewService;

    @MockBean
    private GroqService groqService;

    @MockBean
    private GeminiService geminiService;

    @Test
    @DisplayName("POST /api/interviews/sessions API 요청 시 면접 세션을 정상 생성하고 질문 목록을 반환한다")
    void testStartSessionApi() throws Exception {
        // Given
        Question question = new Question(1L, "Frontend", "JavaScript", "What is closure?", "Closure is...");
        InterviewSession session = new InterviewSession(100L, null, LocalDateTime.now());
        session.setAccessKey(TEST_ACCESS_KEY);
        session.setQuestions(List.of(question));

        given(interviewService.startSession(null, "Frontend", "JavaScript", null, 3, null))
                .willReturn(session);

        Map<String, Object> requestBody = new HashMap<>();
        requestBody.put("memberId", null);
        requestBody.put("category", "Frontend");
        requestBody.put("subject", "JavaScript");
        requestBody.put("count", 3);

        // When & Then
        mockMvc.perform(post("/api/interviews/sessions")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(requestBody)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.accessKey").value(TEST_ACCESS_KEY))
                .andExpect(jsonPath("$.data.questions[0].id").value(1))
                .andExpect(jsonPath("$.data.questions[0].title").value("What is closure?"));
    }

    @Test
    @DisplayName("POST /api/interviews/sessions/{accessKey}/answers API 요청 시 답변을 제출하고 피드백 결과를 반환한다")
    void testSubmitAnswerApi() throws Exception {
        // Given
        InterviewSession session = new InterviewSession(100L, null, LocalDateTime.now());
        session.setAccessKey(TEST_ACCESS_KEY);
        Question question = new Question(1L, "Frontend", "JavaScript", "What is closure?", "Closure is...");
        InterviewHistory history = new InterviewHistory(200L, session, question, "My Answer", 90, "Good feedback", "What is scope?");

        given(interviewService.submitAnswer(eq(TEST_ACCESS_KEY), eq(1L), eq("My Answer")))
                .willReturn(history);

        Map<String, Object> requestBody = new HashMap<>();
        requestBody.put("questionId", 1);
        requestBody.put("userAnswer", "My Answer");

        // When & Then
        mockMvc.perform(post("/api/interviews/sessions/" + TEST_ACCESS_KEY + "/answers")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(requestBody)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.id").value(200))
                .andExpect(jsonPath("$.data.userAnswer").value("My Answer"))
                .andExpect(jsonPath("$.data.score").value(90))
                .andExpect(jsonPath("$.data.feedback").value("Good feedback"))
                .andExpect(jsonPath("$.data.tailQuestion").value("What is scope?"));
    }

    @Test
    @DisplayName("GET /api/interviews/sessions/{accessKey}/history API 요청 시 특정 세션의 면접 보고서 이력을 조회한다")
    void testGetSessionHistoryApi() throws Exception {
        // Given
        InterviewSession session = new InterviewSession(100L, null, LocalDateTime.now());
        session.setAccessKey(TEST_ACCESS_KEY);
        Question question = new Question(1L, "Frontend", "JavaScript", "What is closure?", "Closure is...");
        InterviewHistory history = new InterviewHistory(200L, session, question, "My Answer", 90, "Good feedback", "What is scope?");

        given(interviewService.getSessionHistory(eq(TEST_ACCESS_KEY)))
                .willReturn(List.of(history));

        // When & Then
        mockMvc.perform(get("/api/interviews/sessions/" + TEST_ACCESS_KEY + "/history"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data[0].id").value(200))
                .andExpect(jsonPath("$.data[0].score").value(90))
                .andExpect(jsonPath("$.data[0].feedback").value("Good feedback"));
    }

    @Test
    @DisplayName("POST /api/interviews/transcribe API 요청 시 음성을 텍스트로 정상 변환하고 AI로 교정한다")
    void testTranscribeAudioApi() throws Exception {
        // Given
        given(groqService.transcribeAudio(any())).willReturn("Transcribed text");
        given(geminiService.correctTranscribedText("Transcribed text")).willReturn("Corrected transcribed text");

        org.springframework.mock.web.MockMultipartFile mockFile = new org.springframework.mock.web.MockMultipartFile(
                "file", "audio.mp3", "audio/mpeg", "audio data".getBytes()
        );

        // When & Then
        mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart("/api/interviews/transcribe")
                        .file(mockFile))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.text").value("Corrected transcribed text"));
    }
}
