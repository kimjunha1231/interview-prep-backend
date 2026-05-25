package com.junha.interview.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.junha.interview.service.SubscriptionService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(SubscriptionController.class)
public class SubscriptionControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private SubscriptionService subscriptionService;

    @Test
    @DisplayName("이메일 구독 신청 - 성공")
    void subscribeSuccess() throws Exception {
        String email = "test@example.com";
        SubscriptionController.SubscribeRequest request = new SubscriptionController.SubscribeRequest(email);

        mockMvc.perform(post("/api/subscriptions/subscribe")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk());

        Mockito.verify(subscriptionService).subscribe(email, "ALL");
    }

    @Test
    @DisplayName("이메일 구독 신청 (카테고리 지정) - 성공")
    void subscribeWithCategorySuccess() throws Exception {
        String email = "test@example.com";
        String category = "FE";
        SubscriptionController.SubscribeRequest request = new SubscriptionController.SubscribeRequest(email, category);

        mockMvc.perform(post("/api/subscriptions/subscribe")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk());

        Mockito.verify(subscriptionService).subscribe(email, category);
    }

    @Test
    @DisplayName("이메일 구독 해제 (POST) - 성공")
    void unsubscribePostSuccess() throws Exception {
        String email = "test@example.com";
        SubscriptionController.UnsubscribeRequest request = new SubscriptionController.UnsubscribeRequest(email);

        mockMvc.perform(post("/api/subscriptions/unsubscribe")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk());

        Mockito.verify(subscriptionService).unsubscribe(email);
    }

    @Test
    @DisplayName("이메일 구독 해제 - 성공 시 HTML 반환")
    void unsubscribeSuccess() throws Exception {
        String token = "my-email-hash-token";

        mockMvc.perform(get("/api/subscriptions/unsubscribe")
                        .param("token", token))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.TEXT_HTML));

        Mockito.verify(subscriptionService, Mockito.atLeastOnce()).unsubscribeByHash(token);
    }
}
