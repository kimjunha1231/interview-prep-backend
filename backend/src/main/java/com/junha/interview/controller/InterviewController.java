package com.junha.interview.controller;

import com.junha.interview.common.ApiResponse;
import com.junha.interview.domain.InterviewHistory;
import com.junha.interview.domain.InterviewSession;
import com.junha.interview.dto.interview.*;
import com.junha.interview.service.InterviewService;
import com.junha.interview.service.GroqService;
import com.junha.interview.service.GeminiService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@RestController
@RequestMapping("/api/interviews")
@RequiredArgsConstructor
public class InterviewController {

    private final InterviewService interviewService;
    private final GroqService groqService;
    private final GeminiService geminiService;

    @PostMapping("/sessions")
    public ApiResponse<InterviewSessionResponse> startSession(@RequestBody @Valid StartSessionRequest request) {
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
        @RequestBody @Valid SubmitAnswerRequest request
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
        @RequestBody @Valid EmailReportRequest request
    ) {
        interviewService.sendReportEmail(accessKey, request.getEmail());
        return ApiResponse.success(null);
    }
}
