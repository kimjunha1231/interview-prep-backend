package com.junha.interview;

import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.autoconfigure.web.reactive.AutoConfigureWebTestClient;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.reactive.server.WebTestClient;

import java.util.HashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureWebTestClient
@ActiveProfiles("local")
class InterviewApplicationTests {

	@Autowired
	private WebTestClient webTestClient;

	@Value("${springdoc.api-docs.path}")
	private String apiDocsPath;

	@Test
	void contextLoads() {
		assertThat(apiDocsPath).isEqualTo("/api-docs");
	}

	@Test
	void shouldLoadSwaggerApiDocs() {
		webTestClient.get().uri("/api-docs")
				.exchange()
				.expectStatus().isOk()
				.expectBody()
				.jsonPath("$.openapi").exists();
	}

	@Test
	void shouldExecuteMockInterviewWorkflow() {
		// 1. 면접 세션 시작 (POST /api/interviews/sessions)
		Map<String, Object> startRequest = new HashMap<>();
		startRequest.put("memberId", null);
		startRequest.put("category", "Frontend");
		startRequest.put("subject", "JavaScript");
		startRequest.put("count", 2);

		byte[] startResponse = webTestClient.post().uri("/api/interviews/sessions")
				.contentType(MediaType.APPLICATION_JSON)
				.bodyValue(startRequest)
				.exchange()
				.expectStatus().isOk()
				.expectBody()
				.jsonPath("$.success").isEqualTo(true)
				.jsonPath("$.data.id").exists()
				.jsonPath("$.data.accessKey").exists()
				.jsonPath("$.data.questions").isArray()
				.jsonPath("$.data.questions[0].id").exists()
				.returnResult().getResponseBody();

		// JSON 응답에서 accessKey 및 첫 번째 questionId 추출
		String responseStr = new String(startResponse);
		String accessKey = JsonPath.parse(responseStr).read("$.data.accessKey", String.class);
		Long questionId = JsonPath.parse(responseStr).read("$.data.questions[0].id", Long.class);

		// 2. 답변 제출 (POST /api/interviews/sessions/{accessKey}/answers)
		Map<String, Object> answerRequest = new HashMap<>();
		answerRequest.put("questionId", questionId);
		answerRequest.put("userAnswer", "Closure is a feature in JavaScript where an inner function has access to the outer function's scope.");

		webTestClient.post().uri("/api/interviews/sessions/" + accessKey + "/answers")
				.contentType(MediaType.APPLICATION_JSON)
				.bodyValue(answerRequest)
				.exchange()
				.expectStatus().isOk()
				.expectBody()
				.jsonPath("$.success").isEqualTo(true)
				.jsonPath("$.data.id").exists()
				.jsonPath("$.data.userAnswer").isEqualTo("Closure is a feature in JavaScript where an inner function has access to the outer function's scope.")
				.jsonPath("$.data.score").isNumber()
				.jsonPath("$.data.feedback").isNotEmpty()
				.jsonPath("$.data.tailQuestion").isNotEmpty();

		// 3. 면접 보고서 조회 (GET /api/interviews/sessions/{accessKey}/history)
		webTestClient.get().uri("/api/interviews/sessions/" + accessKey + "/history")
				.exchange()
				.expectStatus().isOk()
				.expectBody()
				.jsonPath("$.success").isEqualTo(true)
				.jsonPath("$.data").isArray()
				.jsonPath("$.data[0].score").isNumber()
				.jsonPath("$.data[0].feedback").isNotEmpty();
	}
}
