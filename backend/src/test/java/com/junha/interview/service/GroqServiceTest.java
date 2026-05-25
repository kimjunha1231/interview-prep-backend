package com.junha.interview.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.web.reactive.function.client.WebClient;

import static org.assertj.core.api.Assertions.assertThat;

class GroqServiceTest {

    @Test
    @DisplayName("API 키가 없을 때 Mock 텍스트 결과값으로 정상 폴백 작동한다")
    void testGroqFallbackWhenNoApiKey() {
        ObjectMapper objectMapper = new ObjectMapper();
        WebClient webClient = WebClient.builder().build();
        GroqService groqService = new GroqService(webClient, objectMapper);

        MockMultipartFile mockFile = new MockMultipartFile(
                "file", "test.mp3", "audio/mpeg", "test audio data".getBytes()
        );

        String transcription = groqService.transcribeAudio(mockFile);

        assertThat(transcription).isEqualTo("임시 음성 인식 텍스트입니다. (Groq API Key 설정 필요)");
    }
}
