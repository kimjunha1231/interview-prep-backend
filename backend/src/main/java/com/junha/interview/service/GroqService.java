package com.junha.interview.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.client.MultipartBodyBuilder;
import org.springframework.stereotype.Service;
import org.springframework.util.MultiValueMap;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.reactive.function.client.WebClient;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
public class GroqService {

    @Value("${ai.groq.api-key}")
    private String apiKey;

    private final WebClient webClient;
    private final ObjectMapper objectMapper;

    private boolean isApiKeyConfigured() {
        return apiKey != null
                && !apiKey.equals("none")
                && !apiKey.trim().isEmpty()
                && !apiKey.startsWith("your_");
    }

    public String transcribeAudio(MultipartFile file) {
        if (!isApiKeyConfigured()) {
            log.warn("Groq API key is not configured. Falling back to mock transcription.");
            return "임시 음성 인식 텍스트입니다. (Groq API Key 설정 필요)";
        }

        try {
            WebClient client = webClient.mutate().baseUrl("https://api.groq.com/openai/v1").build();

            MultipartBodyBuilder bodyBuilder = new MultipartBodyBuilder();
            bodyBuilder.part("file", file.getResource());
            bodyBuilder.part("model", "whisper-large-v3");
            bodyBuilder.part("language", "ko");
            bodyBuilder.part("response_format", "json");

            MultiValueMap<String, HttpEntity<?>> multipartBody = bodyBuilder.build();

            String responseJson = client.post()
                .uri("/audio/transcriptions")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + apiKey)
                .contentType(MediaType.MULTIPART_FORM_DATA)
                .bodyValue(multipartBody)
                .retrieve()
                .bodyToMono(String.class)
                .block();

            Map<String, Object> responseMap = objectMapper.readValue(responseJson, Map.class);
            return (String) responseMap.get("text");
        } catch (Exception e) {
            log.error("Error occurred during Groq audio transcription. Falling back to mock transcription.", e);
            return "임시 음성 인식 텍스트입니다. (호출 에러 발생)";
        }
    }
}
