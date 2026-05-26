package com.junha.interview.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.web.reactive.function.client.WebClient;

import static org.assertj.core.api.Assertions.assertThat;

class GeminiServiceTest {

    @Test
    @DisplayName("API 키가 없을 때 Mock 결과값으로 정상 폴백 작동한다")
    void testGeminiFallbackWhenNoApiKey() {
        ObjectMapper objectMapper = new ObjectMapper();
        WebClient webClient = WebClient.builder().build();
        GeminiService geminiService = new GeminiService(webClient, objectMapper);
        
        GeminiService.GeminiEvaluation evaluation = geminiService.evaluateAnswer(
                "Frontend", "JavaScript", "What is closure?", "A closure is...", "User answer"
        );

        assertThat(evaluation).isNotNull();
        assertThat(evaluation.getScore()).isBetween(80, 100);
        assertThat(evaluation.getFeedback()).contains("What is closure?");
        assertThat(evaluation.getTailQuestion()).isNotEmpty();
    }

    @Test
    @DisplayName("유효한 API 키 리스트 추출 검증 (1차, 2차, 3차 키 필터링 및 순서)")
    void testGetAvailableApiKeys() {
        ObjectMapper objectMapper = new ObjectMapper();
        WebClient webClient = WebClient.builder().build();
        GeminiService geminiService = new GeminiService(webClient, objectMapper);

        org.springframework.test.util.ReflectionTestUtils.setField(geminiService, "apiKeyPrimary", "key1");
        org.springframework.test.util.ReflectionTestUtils.setField(geminiService, "apiKeySecondary", "none");
        org.springframework.test.util.ReflectionTestUtils.setField(geminiService, "apiKeyTertiary", "  key3 ");

        // reflection으로 private 메서드 getAvailableApiKeys 호출
        @SuppressWarnings("unchecked")
        java.util.List<String> keys = (java.util.List<String>) org.springframework.test.util.ReflectionTestUtils.invokeMethod(
                geminiService, "getAvailableApiKeys"
        );

        assertThat(keys).containsExactly("key1", "key3");
    }
}
