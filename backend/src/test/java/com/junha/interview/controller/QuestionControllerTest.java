package com.junha.interview.controller;

import com.junha.interview.domain.Question;
import com.junha.interview.dto.QuestionSummaryDto;
import com.junha.interview.service.QuestionService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(QuestionController.class)
public class QuestionControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private QuestionService questionService;

    @Test
    @DisplayName("GET /api/questions/random API 요청 시 공통 ApiResponse 형식에 맞춰 데이터를 반환한다")
    void testGetRandomQuestionsApi() throws Exception {
        // Given
        Question question = new Question(1L, "Frontend", "JavaScript", "What is closure?", "Closure is...");
        given(questionService.getRandomQuestions("Frontend", "JavaScript", 1))
                .willReturn(List.of(question));

        // When & Then
        mockMvc.perform(get("/api/questions/random")
                        .param("category", "Frontend")
                        .param("subject", "JavaScript")
                        .param("count", "1")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data[0].id").value(1))
                .andExpect(jsonPath("$.data[0].category").value("Frontend"))
                .andExpect(jsonPath("$.data[0].subject").value("JavaScript"))
                .andExpect(jsonPath("$.data[0].title").value("What is closure?"))
                .andExpect(jsonPath("$.data[0].perfectAnswer").value("Closure is..."))
                .andExpect(jsonPath("$.error").isEmpty());
    }

    @Test
    @DisplayName("GET /api/questions/{id} API 요청 시 ID에 해당하는 단일 질문을 반환한다")
    void testGetQuestionByIdApi() throws Exception {
        // Given
        Question question = new Question(1L, "Frontend", "JavaScript", "What is closure?", "Closure is...");
        given(questionService.getQuestionById(1L)).willReturn(question);

        // When & Then
        mockMvc.perform(get("/api/questions/1")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.id").value(1))
                .andExpect(jsonPath("$.data.category").value("Frontend"))
                .andExpect(jsonPath("$.data.subject").value("JavaScript"))
                .andExpect(jsonPath("$.data.title").value("What is closure?"))
                .andExpect(jsonPath("$.data.perfectAnswer").value("Closure is..."))
                .andExpect(jsonPath("$.error").isEmpty());
    }

    @Test
    @DisplayName("GET /api/questions/list API 요청 시 질문 요약 정보 목록을 반환한다")
    void testGetQuestionSummariesApi() throws Exception {
        // Given
        Question question = new Question(1L, "Frontend", "JavaScript", "What is closure?", "Closure is...");
        QuestionSummaryDto summary = new QuestionSummaryDto(question);
        given(questionService.getQuestionSummaries("Frontend", "JavaScript"))
                .willReturn(List.of(summary));

        // When & Then
        mockMvc.perform(get("/api/questions/list")
                        .param("category", "Frontend")
                        .param("subject", "JavaScript")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data[0].id").value(1))
                .andExpect(jsonPath("$.data[0].category").value("Frontend"))
                .andExpect(jsonPath("$.data[0].subject").value("JavaScript"))
                .andExpect(jsonPath("$.data[0].title").value("What is closure?"))
                .andExpect(jsonPath("$.data[0].perfectAnswer").doesNotExist())
                .andExpect(jsonPath("$.error").isEmpty());
    }
}
