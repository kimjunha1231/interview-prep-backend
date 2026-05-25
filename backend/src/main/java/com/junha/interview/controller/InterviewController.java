package com.junha.interview.controller;

import com.junha.interview.common.ApiResponse;
import com.junha.interview.domain.InterviewHistory;
import com.junha.interview.domain.InterviewSession;
import com.junha.interview.domain.Question;
import com.junha.interview.service.InterviewService;
import com.junha.interview.service.GroqService;
import com.junha.interview.service.GeminiService;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDateTime;
import java.util.List;

@RestController
@RequestMapping("/api/interviews")
@RequiredArgsConstructor
public class InterviewController {

    private final InterviewService interviewService;
    private final GroqService groqService;
    private final GeminiService geminiService;

    @PostMapping("/sessions")
    public ApiResponse<InterviewSessionResponse> startSession(@RequestBody StartSessionRequest request) {
        int count = request.getCount() != null ? request.getCount() : 3;
        InterviewSession session = interviewService.startSession(
                request.getMemberId(),
                request.getCategory(),
                request.getSubject(),
                request.getSubjects(),
                count,
                request.getPortfolioText()
        );
        return ApiResponse.success(InterviewSessionResponse.from(session));
    }

    @PostMapping("/sessions/{accessKey}/answers")
    public ApiResponse<InterviewHistoryResponse> submitAnswer(
        @PathVariable String accessKey,
        @RequestBody SubmitAnswerRequest request
    ) {
        InterviewHistory history = interviewService.submitAnswer(
                accessKey,
                request.getQuestionId(),
                request.getUserAnswer()
        );
        return ApiResponse.success(InterviewHistoryResponse.from(history));
    }

    @GetMapping("/sessions/{accessKey}/history")
    public ApiResponse<List<InterviewHistoryResponse>> getSessionHistory(@PathVariable String accessKey) {
        List<InterviewHistory> histories = interviewService.getSessionHistory(accessKey);
        List<InterviewHistoryResponse> response = histories.stream()
                .map(InterviewHistoryResponse::from)
                .toList();
        return ApiResponse.success(response);
    }

    @PostMapping(value = "/transcribe", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ApiResponse<TranscriptionResponse> transcribeAudio(@RequestParam("file") MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("업로드된 음성 파일이 비어 있습니다.");
        }
        
        // 10MB 용량 초과 검증 (10 * 1024 * 1024 bytes)
        if (file.getSize() > 10 * 1024 * 1024) {
            throw new IllegalArgumentException("업로드 가능한 최대 음성 파일 크기는 10MB입니다.");
        }

        // MIME Type 검증 (Content-Type이 audio/로 시작해야 함)
        String contentType = file.getContentType();
        if (contentType == null || !contentType.startsWith("audio/")) {
            throw new IllegalArgumentException("올바른 오디오 파일 형식이 아닙니다.");
        }

        String transcribedText = groqService.transcribeAudio(file);
        // Gemini 2차 교정 적용 (발음 오인식 및 IT 전문 용어 보정)
        String correctedText = geminiService.correctTranscribedText(transcribedText);
        return ApiResponse.success(new TranscriptionResponse(correctedText));
    }

    @PostMapping("/sessions/{accessKey}/report/email")
    public ApiResponse<Void> sendEmailReport(
        @PathVariable String accessKey,
        @RequestBody EmailReportRequest request
    ) {
        interviewService.sendReportEmail(accessKey, request.getEmail());
        return ApiResponse.success(null);
    }

    @Getter
    @AllArgsConstructor
    public static class TranscriptionResponse {
        private String text;
    }

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    public static class StartSessionRequest {
        private Long memberId;
        private String category;
        private String subject;
        private List<String> subjects;
        private Integer count;
        private String portfolioText;
    }

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    public static class SubmitAnswerRequest {
        private Long questionId;
        private String userAnswer;
    }

    @Getter
    @AllArgsConstructor
    public static class QuestionResponse {
        private Long id;
        private String category;
        private String subject;
        private String title;
        private String perfectAnswer;

        public static QuestionResponse from(Question question) {
            if (question == null) return null;
            return new QuestionResponse(
                    question.getId(),
                    question.getCategory(),
                    question.getSubject(),
                    question.getTitle(),
                    question.getPerfectAnswer()
            );
        }
    }

    @Getter
    @AllArgsConstructor
    public static class InterviewSessionResponse {
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
            return new InterviewSessionResponse(session.getId(), session.getAccessKey(), memberId, session.getCreatedAt(), questionDtos);
        }
    }

    @Getter
    @AllArgsConstructor
    public static class InterviewHistoryResponse {
        private Long id;
        private Long sessionId;
        private Long questionId;
        private String userAnswer;
        private int score;
        private String feedback;
        private String tailQuestion;

        public static InterviewHistoryResponse from(InterviewHistory history) {
            if (history == null) return null;
            return new InterviewHistoryResponse(
                    history.getId(),
                    history.getSession().getId(),
                    history.getQuestion().getId(),
                    history.getUserAnswer(),
                    history.getScore(),
                    history.getFeedback(),
                    history.getTailQuestion()
            );
        }
    }

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    public static class EmailReportRequest {
        private String email;
    }
}
