package com.junha.interview.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
public class GeminiService {

    @Value("${ai.gemini.api-key}")
    private String apiKey;

    private final WebClient.Builder webClientBuilder;
    private final ObjectMapper objectMapper;

    @Getter
    @RequiredArgsConstructor
    public static class ModelConfig {
        private final String name;
        private final boolean useGrounding;
    }

    private static final List<ModelConfig> MODEL_CHAIN = List.of(
        new ModelConfig("gemini-3.5-flash", false),
        new ModelConfig("gemini-3-flash", false),
        new ModelConfig("gemini-3.1-flash-lite", false),
        new ModelConfig("gemini-2.5-flash", false),
        new ModelConfig("gemini-2.5-flash-lite", false),
        new ModelConfig("gemini-2.0-flash", false),
        new ModelConfig("gemini-2-flash", false),
        new ModelConfig("gemini-2.0-flash-lite", false),
        new ModelConfig("gemini-3.1-pro", false),
        new ModelConfig("gemini-2.5-pro", false)
    );

    public GeminiEvaluation evaluateAnswer(String category, String subject, String title, String perfectAnswer, String userAnswer) {
        if (apiKey == null || apiKey.equals("none") || apiKey.trim().isEmpty()) {
            log.warn("Gemini API key is not configured. Falling back to mock evaluation.");
            return getMockEvaluation(title);
        }

        String prompt = String.format(
            "당신은 IT 대기업의 10년 차 이상 시니어 개발자이자 테크 리드 면접관입니다. " +
            "제공된 질문, 모범 답안, 그리고 사용자의 답변을 비교 분석하여 냉철하고도 건설적인 평가를 진행해 주세요.\n\n" +
            "평가 지침:\n" +
            "1. 사용자가 단순히 정의를 암기해서 답변했는지(Textbook Answer), 아니면 기술의 내부 동작 원리와 실제 활용법까지 이해하고 있는지 평가하세요.\n" +
            "2. 피드백에는 다음 3가지 요소를 한국어로 상세히 포함해 주세요:\n" +
            "   - 잘한 점 (두괄식 구성, 핵심 키워드 언급 등)\n" +
            "   - 아쉬운 점 (부정확한 개념 정의, 생략된 메커니즘, 구체적 예시 부족 등)\n" +
            "   - 실무 관점의 조언 및 보완 방향\n" +
            "3. 사용자의 답변에서 언급한 특정 기술 키워드나 설명 방식을 기반으로, 꼬리 질문을 1개 생성해 주세요.\n" +
            "4. 꼬리 질문은 다음 3가지 유형 중 가장 적합한 것을 선택하여 출제해 주세요:\n" +
            "   - [실무 적용 및 아키텍처 설계]: 가상의 실무 대용량 트래픽 상황을 가정하고 해당 기술을 어떻게 적용할 것인지 질문\n" +
            "   - [트레이드오프 및 대안 비교]: 사용자가 언급한 방식 외에 다른 기술적 대안을 제시하며 비교 설명 요구\n" +
            "   - [에지 케이스 및 장애 상황 극복]: 해당 기술 적용 시 발생할 수 있는 부작용(병목, 동시성 오류, 네트워크 유실 등)의 대처 방안 질문\n\n" +
            "응답은 반드시 아래 JSON 구조로만 작성해 주세요. 추가 텍스트나 markdown 코드 블록(예: ```json) 없이 순수 JSON 객체만 반환해야 합니다.\n\n" +
            "JSON 구조:\n" +
            "{\n" +
            "  \"score\": 0~100 사이의 정수 점수,\n" +
            "  \"feedback\": \"잘한 점, 아쉬운 점, 보완할 점을 조리 있게 나열한 종합 피드백\",\n" +
            "  \"tailQuestion\": \"사용자 답변 맞춤형 실무/심화 꼬리 질문\"\n" +
            "}\n\n" +
            "질문 카테고리: %s\n" +
            "질문 과목: %s\n" +
            "질문: %s\n" +
            "모범 답안: %s\n" +
            "사용자 답변: %s",
            category, subject, title, perfectAnswer, userAnswer
        );

        WebClient webClient = webClientBuilder.baseUrl("https://generativelanguage.googleapis.com").build();

        for (ModelConfig config : MODEL_CHAIN) {
            try {
                log.info("Attempting AI evaluation using model: {}", config.getName());

                Map<String, Object> requestBody = Map.of(
                    "contents", List.of(
                        Map.of("parts", List.of(Map.of("text", prompt)))
                    ),
                    "generationConfig", Map.of(
                        "responseMimeType", "application/json"
                    )
                );

                String responseJson = webClient.post()
                    .uri(uriBuilder -> uriBuilder
                        .path("/v1beta/models/" + config.getName() + ":generateContent")
                        .build())
                    .header("x-goog-api-key", apiKey)
                    .contentType(MediaType.APPLICATION_JSON)
                    .bodyValue(requestBody)
                    .retrieve()
                    .bodyToMono(String.class)
                    .block();

                Map<String, Object> responseMap = objectMapper.readValue(responseJson, Map.class);
                List<Map<String, Object>> candidates = (List<Map<String, Object>>) responseMap.get("candidates");
                Map<String, Object> content = (Map<String, Object>) candidates.get(0).get("content");
                List<Map<String, Object>> parts = (List<Map<String, Object>>) content.get("parts");
                String textResult = (String) parts.get(0).get("text");

                GeminiEvaluation evaluation = objectMapper.readValue(textResult, GeminiEvaluation.class);
                log.info("Successfully evaluated answer using model: {}", config.getName());
                return evaluation;

            } catch (org.springframework.web.reactive.function.client.WebClientResponseException e) {
                log.warn("Failed to evaluate with model: {} due to HTTP status: {}. Retrying with next model...",
                         config.getName(), e.getStatusCode());
            } catch (Exception e) {
                log.warn("Failed to evaluate with model: {} due to unexpected error. Retrying with next model...",
                         config.getName(), e);
            }
        }

        log.error("All models in the fallback chain failed. Falling back to mock evaluation.");
        return getMockEvaluation(title);
    }

    private GeminiEvaluation getMockEvaluation(String title) {
        int score = (int) (Math.random() * 21) + 80;
        String feedback = "제출하신 답변에 대한 피드백입니다. '" + title + "' 주제에 대해 논리적으로 답변하셨습니다. 실무 예시와 함께 동작 원리를 더 구체적으로 기술하면 더욱 좋습니다.";
        String tailQuestion = "꼬리 질문: 해당 동작 원리나 설계 방식을 선택했을 때 발생할 수 있는 병목 현상과, 이를 극복하기 위한 트레이드오프는 무엇인가요?";
        return new GeminiEvaluation(score, feedback, tailQuestion);
    }

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    public static class PortfolioQuestion {
        private String title;
        private String perfectAnswer;
        private String category;
        private String subject;
    }

    public List<PortfolioQuestion> generateQuestionsFromPortfolio(String portfolioText, int count) {
        if (apiKey == null || apiKey.equals("none") || apiKey.trim().isEmpty()) {
            log.warn("Gemini API key is not configured. Falling back to mock portfolio questions.");
            return getMockPortfolioQuestions(count);
        }

        String prompt = String.format(
            "당신은 IT 대기업의 10년 차 이상 시니어 개발자이자 테크 리드 면접관입니다. " +
            "제공된 사용자의 개발 포트폴리오 내용을 분석하여, 이 사람의 실무 역량, 아키텍처 이해도, 트레이드오프 고민 수준을 " +
            "가장 깊이 있게 검증할 수 있는 정밀 기술 면접 질문 %d개를 출제하고 모범 답안을 작성해 주세요.\n\n" +
            "출제 지침:\n" +
            "1. 포트폴리오에 언급된 특정 프로젝트, 기술 스택(예: Spring, React, Redis, Kafka, Docker 등) 및 구현 방식을 적극 반영해 질문을 뽑으세요.\n" +
            "2. 모범 답안(`perfectAnswer`)은 실무 관점에서 면접자가 반드시 언급해야 하는 핵심 개념 구조와 논리를 간결하고 명확하게 한국어로 작성해 주세요.\n" +
            "3. 모든 질문의 `category`는 \"PORTFOLIO\", `subject`는 \"포트폴리오 분석\"으로 고정하세요.\n\n" +
            "응답은 반드시 아래 JSON 구조의 배열로만 작성해 주세요. 추가 텍스트나 markdown 코드 블록(예: ```json) 없이 순수 JSON 배열만 반환해야 합니다.\n\n" +
            "JSON 구조:\n" +
            "[\n" +
            "  {\n" +
            "    \"title\": \"출제할 면접 질문 내용\",\n" +
            "    \"perfectAnswer\": \"시니어 관점의 모범 답변 가이드라인\",\n" +
            "    \"category\": \"PORTFOLIO\",\n" +
            "    \"subject\": \"포트폴리오 분석\"\n" +
            "  }\n" +
            "]\n\n" +
            "포트폴리오 텍스트 본문:\n%s",
            count, portfolioText
        );

        WebClient webClient = webClientBuilder.baseUrl("https://generativelanguage.googleapis.com").build();

        for (ModelConfig config : MODEL_CHAIN) {
            try {
                log.info("Attempting Portfolio question generation using model: {}", config.getName());

                Map<String, Object> requestBody = Map.of(
                    "contents", List.of(
                        Map.of("parts", List.of(Map.of("text", prompt)))
                    ),
                    "generationConfig", Map.of(
                        "responseMimeType", "application/json"
                    )
                );

                String responseJson = webClient.post()
                    .uri(uriBuilder -> uriBuilder
                        .path("/v1beta/models/" + config.getName() + ":generateContent")
                        .build())
                    .header("x-goog-api-key", apiKey)
                    .contentType(MediaType.APPLICATION_JSON)
                    .bodyValue(requestBody)
                    .retrieve()
                    .bodyToMono(String.class)
                    .block();

                Map<String, Object> responseMap = objectMapper.readValue(responseJson, Map.class);
                List<Map<String, Object>> candidates = (List<Map<String, Object>>) responseMap.get("candidates");
                Map<String, Object> content = (Map<String, Object>) candidates.get(0).get("content");
                List<Map<String, Object>> parts = (List<Map<String, Object>>) content.get("parts");
                String textResult = (String) parts.get(0).get("text");

                List<PortfolioQuestion> questions = objectMapper.readValue(
                    textResult, 
                    objectMapper.getTypeFactory().constructCollectionType(List.class, PortfolioQuestion.class)
                );
                log.info("Successfully generated portfolio questions using model: {}", config.getName());
                return questions;

            } catch (Exception e) {
                log.warn("Failed to generate questions with model: {} due to error. Retrying next...", config.getName(), e);
            }
        }

        log.error("All models failed for portfolio question generation. Falling back to mock.");
        return getMockPortfolioQuestions(count);
    }

    private List<PortfolioQuestion> getMockPortfolioQuestions(int count) {
        List<PortfolioQuestion> mocks = List.of(
            new PortfolioQuestion(
                "포트폴리오에 언급된 프로젝트 아키텍처에서 특정 기술(예: Redis Cache)을 도입할 때 고려한 트레이드오프와 성능 개선 수치에 대해 설명해 주세요.",
                "Redis 도입 시 메모리 리소스 한계와 캐시 삼천포(Cache Stampede) 현상 방지를 위해 TTL 정책 및 무중단 동기화 설정을 고려해야 하며, 데이터 일관성 깨짐 방지 전략을 제시해야 합니다.",
                "PORTFOLIO",
                "포트폴리오 분석"
            ),
            new PortfolioQuestion(
                "포트폴리오 프로젝트 중 발생한 가장 큰 병목 현상이나 장애 상황은 무엇이었으며, 이를 인지하고 해결하기 위해 어떤 트러블슈팅 단계를 거쳤나요?",
                "부하 테스트 툴을 통한 모니터링, 데이터베이스 슬로우 쿼리 분석, 커넥션 풀 누수 또는 인덱스 튜닝 등을 통해 병목을 규명하고 적절한 튜닝 조치를 취했음을 기술해야 합니다.",
                "PORTFOLIO",
                "포트폴리오 분석"
            ),
            new PortfolioQuestion(
                "포트폴리오 프로젝트의 인프라 설계(예: Docker, Nginx, CI/CD)를 선택하게 된 이유와, 가상 환경을 통한 배포 효율성 강화 포인트를 설명해 주세요.",
                "컨테이너 고립 구동을 통한 인프라 이식성 확보, Nginx 리버스 프록시를 활용한 보안 격리 및 CORS 해결, 그리고 빌드 오프로딩을 통한 서버 가용성 확보 등을 논리적으로 어필해야 합니다.",
                "PORTFOLIO",
                "포트폴리오 분석"
            )
        );
        return mocks.subList(0, Math.min(count, mocks.size()));
    }

    public String correctTranscribedText(String rawText) {
        if (rawText == null || rawText.trim().isEmpty()) {
            return rawText;
        }
        if (apiKey == null || apiKey.equals("none") || apiKey.trim().isEmpty()) {
            log.warn("Gemini API key is not configured. Skipping STT correction fallback to raw text.");
            return rawText;
        }

        String prompt = String.format(
            "당신은 테크니컬 라이터이자 음성 인식(STT) 교정 전문가입니다. " +
            "제공된 텍스트는 모의 기술 면접 도중 사용자의 한글 답변 음성을 Whisper API로 1차 인식한 결과입니다.\n\n" +
            "교정 지침:\n" +
            "1. 음성 인식 특성상 뭉개지거나 잘못 기입된 전공 기술 전문 용어를 올바른 공식 영문 약어나 용어로 복원해 주세요.\n" +
            "   (예: 제이피에이 -> JPA, 스프링부트 -> Spring Boot, 리덕스 -> Redux, 씨오알에스 -> CORS, 디아이 -> DI 등)\n" +
            "2. 어투가 심하게 어색한 띄어쓰기나 오타, 맞춤법을 매끄러운 한글 구어체로 교정해 주세요.\n" +
            "3. 말실수(예: '어..', '음..', '그..', '아니 그게')나 불필요한 반복어는 정제해 주세요.\n" +
            "4. 가장 중요: 사용자가 원래 발화하지 않은 새로운 기술 논리나 핵심 주장을 창작(Hallucination)하여 임의로 문장에 덧붙이지 마십시오. " +
            "오직 발음 왜곡으로 인한 오인식 및 오타 교정만 수행하십시오.\n\n" +
            "응답 규격:\n" +
            "추가적인 설명이나 markdown 코드 블록 없이, 오직 교정된 최종 한글 텍스트 문장만 반환해 주세요.\n\n" +
            "음성인식 원본 텍스트:\n%s",
            rawText
        );

        WebClient webClient = webClientBuilder.baseUrl("https://generativelanguage.googleapis.com").build();

        for (ModelConfig config : MODEL_CHAIN) {
            try {
                log.info("Attempting STT text correction using model: {}", config.getName());

                Map<String, Object> requestBody = Map.of(
                    "contents", List.of(
                        Map.of("parts", List.of(Map.of("text", prompt)))
                    )
                );

                String responseJson = webClient.post()
                    .uri(uriBuilder -> uriBuilder
                        .path("/v1beta/models/" + config.getName() + ":generateContent")
                        .build())
                    .header("x-goog-api-key", apiKey)
                    .contentType(MediaType.APPLICATION_JSON)
                    .bodyValue(requestBody)
                    .retrieve()
                    .bodyToMono(String.class)
                    .block();

                Map<String, Object> responseMap = objectMapper.readValue(responseJson, Map.class);
                List<Map<String, Object>> candidates = (List<Map<String, Object>>) responseMap.get("candidates");
                Map<String, Object> content = (Map<String, Object>) candidates.get(0).get("content");
                List<Map<String, Object>> parts = (List<Map<String, Object>>) content.get("parts");
                String correctedText = (String) parts.get(0).get("text");

                if (correctedText != null) {
                    correctedText = correctedText.trim();
                    log.info("Successfully corrected STT text using model: {}", config.getName());
                    return correctedText;
                }

            } catch (Exception e) {
                log.warn("Failed to correct STT text with model: {} due to error. Retrying next...", config.getName(), e);
            }
        }

        log.error("All models failed for STT text correction. Falling back to raw text.");
        return rawText;
    }

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    public static class GeminiEvaluation {
        private int score;
        private String feedback;
        private String tailQuestion;
    }
}
