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
import org.springframework.web.reactive.function.client.WebClientResponseException;

import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
public class GeminiService {

    @Value("${ai.gemini.api-key-primary}")
    private String apiKeyPrimary;

    @Value("${ai.gemini.api-key-secondary}")
    private String apiKeySecondary;

    @Value("${ai.gemini.api-key-tertiary}")
    private String apiKeyTertiary;

    private final WebClient webClient;
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

    // ─── 공통 유틸: 유효한 API Key 리스트 추출 ─────────────────────────
    private List<String> getAvailableApiKeys() {
        List<String> keys = new java.util.ArrayList<>();
        if (isValidKey(apiKeyPrimary)) {
            keys.add(apiKeyPrimary.trim());
        }
        if (isValidKey(apiKeySecondary)) {
            keys.add(apiKeySecondary.trim());
        }
        if (isValidKey(apiKeyTertiary)) {
            keys.add(apiKeyTertiary.trim());
        }
        return keys;
    }

    private boolean isValidKey(String key) {
        return key != null
                && !key.equals("none")
                && !key.trim().isEmpty()
                && !key.startsWith("your_");
    }

    // ─── 공통 유틸: API Key 유효성 검사 ───────────────────────────────
    private boolean isApiKeyConfigured() {
        return !getAvailableApiKeys().isEmpty();
    }

    // ─── 공통 유틸: Gemini API 호출 + API Key Rotation + Model Fallback Chain ─────────────
    /**
     * Gemini API에 프롬프트를 전송하고 응답 텍스트를 추출합니다.
     * 활성화된 API Key 리스트를 순회하며 각 Key마다 MODEL_CHAIN을 돌려 성공할 때까지 시도합니다.
     * 429(할당량 초과) 및 403(권한 오류) 감지 시 즉시 다음 예비 API Key로 우회 전환합니다.
     *
     * @param prompt           전송할 프롬프트 텍스트
     * @param jsonResponseMode true이면 응답 MIME Type을 application/json으로 지정
     * @param operationName    로그 식별용 작업 이름
     * @return 응답 텍스트 또는 모든 Key/모델 실패 시 null
     */
    private String callGeminiApi(String prompt, boolean jsonResponseMode, String operationName) {
        WebClient client = webClient.mutate()
                .baseUrl("https://generativelanguage.googleapis.com").build();

        List<String> activeKeys = getAvailableApiKeys();
        if (activeKeys.isEmpty()) {
            log.warn("No configured Gemini API keys available for {}", operationName);
            return null;
        }

        for (int keyIdx = 0; keyIdx < activeKeys.size(); keyIdx++) {
            String activeKey = activeKeys.get(keyIdx);
            log.info("Starting {} attempt with API Key index: {}/{}", operationName, keyIdx + 1, activeKeys.size());

            for (ModelConfig config : MODEL_CHAIN) {
                try {
                    log.info("Attempting {} using model: {} on API Key index: {}", operationName, config.getName(), keyIdx + 1);

                    Map<String, Object> requestBody;
                    if (jsonResponseMode) {
                        requestBody = Map.of(
                            "contents", List.of(
                                Map.of("parts", List.of(Map.of("text", prompt)))
                            ),
                            "generationConfig", Map.of(
                                "responseMimeType", "application/json"
                            )
                        );
                    } else {
                        requestBody = Map.of(
                            "contents", List.of(
                                Map.of("parts", List.of(Map.of("text", prompt)))
                            )
                        );
                    }

                    String responseJson = client.post()
                        .uri(uriBuilder -> uriBuilder
                            .path("/v1beta/models/" + config.getName() + ":generateContent")
                            .build())
                        .header("x-goog-api-key", activeKey)
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

                    log.info("Successfully completed {} using model: {} with API Key index: {}", operationName, config.getName(), keyIdx + 1);
                    return textResult;

                } catch (WebClientResponseException e) {
                    log.warn("Failed {} with model: {} on API Key index: {} due to HTTP status: {}",
                             operationName, config.getName(), keyIdx + 1, e.getStatusCode());

                    // 429(할당량 소진) 또는 403(인증 실패) 발생 시, 다음 모델을 돌기보다 바로 예비 API Key로 스위칭합니다.
                    if (e.getStatusCode().value() == 429 || e.getStatusCode().value() == 403) {
                        log.warn("API Key index {} returned quota/auth error ({}). Breaking model chain to switch API Key...",
                                 keyIdx + 1, e.getStatusCode());
                        break; // 내부 모델 루프 탈출 -> 다음 API Key 루프로 전이
                    }
                } catch (Exception e) {
                    log.warn("Failed {} with model: {} on API Key index: {} due to unexpected error. Retrying next model...",
                             operationName, config.getName(), keyIdx + 1, e);
                }
            }
        }

        log.error("All models and API keys in the fallback chain failed for {}.", operationName);
        return null;
    }

    // ─── 비즈니스 로직: AI 답변 채점 ──────────────────────────────────
    public GeminiEvaluation evaluateAnswer(String category, String subject, String title, String perfectAnswer, String userAnswer) {
        if (!isApiKeyConfigured()) {
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

        String textResult = callGeminiApi(prompt, true, "AI evaluation");
        if (textResult != null) {
            try {
                return objectMapper.readValue(textResult, GeminiEvaluation.class);
            } catch (Exception e) {
                log.warn("Failed to parse evaluation JSON. Falling back to mock.", e);
            }
        }

        return getMockEvaluation(title);
    }

    private GeminiEvaluation getMockEvaluation(String title) {
        int score = (int) (Math.random() * 21) + 80;
        String feedback = "제출하신 답변에 대한 피드백입니다. '" + title + "' 주제에 대해 논리적으로 답변하셨습니다. 실무 예시와 함께 동작 원리를 더 구체적으로 기술하면 더욱 좋습니다.";
        String tailQuestion = "꼬리 질문: 해당 동작 원리나 설계 방식을 선택했을 때 발생할 수 있는 병목 현상과, 이를 극복하기 위한 트레이드오프는 무엇인가요?";
        return new GeminiEvaluation(score, feedback, tailQuestion);
    }

    // ─── 비즈니스 로직: 포트폴리오 기반 질문 생성 ──────────────────────
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
        if (!isApiKeyConfigured()) {
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

        String textResult = callGeminiApi(prompt, true, "Portfolio question generation");
        if (textResult != null) {
            try {
                return objectMapper.readValue(
                    textResult,
                    objectMapper.getTypeFactory().constructCollectionType(List.class, PortfolioQuestion.class)
                );
            } catch (Exception e) {
                log.warn("Failed to parse portfolio questions JSON. Falling back to mock.", e);
            }
        }

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

    // ─── 비즈니스 로직: STT 텍스트 교정 ──────────────────────────────
    public String correctTranscribedText(String rawText) {
        if (rawText == null || rawText.trim().isEmpty()) {
            return rawText;
        }
        if (!isApiKeyConfigured()) {
            log.warn("Gemini API key is not configured. Skipping STT correction fallback to raw text.");
            return rawText;
        }

        String prompt = String.format(
            "당신은 음성 인식(STT) 오류 교정 전문가입니다. " +
            "아래 텍스트는 한국어 기술 면접 음성을 Whisper STT로 변환한 원본입니다.\n\n" +
            "【절대 금지】\n" +
            "- 사용자가 말한 단어나 어구를 다른 단어로 교체하거나 재해석하는 것 (예: '처리량'을 '처리속도'로, '바로 지금'을 '또 다음을'처럼 의미가 달라지는 모든 교체 금지)\n" +
            "- 문장 순서를 바꾸거나, 내용을 요약하거나, 문장을 합치거나 나누는 것\n" +
            "- 원본에 없는 단어나 내용을 새로 추가하는 것\n\n" +
            "【허용되는 교정만 수행】\n" +
            "1. STT 오인식으로 발생한 IT 기술 용어의 발음 표기를 원래 표기로 복원\n" +
            "   (예: '제이피에이' → JPA, '스프링부트' → Spring Boot, '씨오알에스' → CORS)\n" +
            "2. 명백한 띄어쓰기 오류 수정 (단, 단어 자체는 변경 불가)\n" +
            "3. '어..', '음..', '그..' 등 의미 없는 필러 단어만 제거 (실제 발화 내용은 보존)\n\n" +
            "응답 규격:\n" +
            "설명 없이 교정된 텍스트만 그대로 반환하세요. 교정할 내용이 없으면 원본을 그대로 반환하세요.\n\n" +
            "STT 원본:\n%s",
            rawText
        );

        String correctedText = callGeminiApi(prompt, false, "STT text correction");
        if (correctedText != null) {
            return correctedText.trim();
        }

        return rawText;
    }

    // ─── DTO ──────────────────────────────────────────────────────────
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
