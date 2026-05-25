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
}
