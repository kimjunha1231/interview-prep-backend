package com.junha.interview.service;

import com.junha.interview.domain.Question;
import com.junha.interview.repository.QuestionRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@ActiveProfiles("test")
public class EmailServiceIntegrationTest {

    @Autowired
    private EmailService emailService;

    @Autowired
    private QuestionRepository questionRepository;

    @Test
    void testSendSampleEmail() throws Exception {
        List<Question> questions = questionRepository.findAll();
        assertThat(questions).isNotEmpty();

        // 1개 추출
        Question sampleQuestion = questions.get(0);

        // rlawnsgk0610@gmail.com 으로 전송
        emailService.sendDailyQuestionEmail("rlawnsgk0610@gmail.com", sampleQuestion);

        // 비동기 실행이므로 잠깐 대기하여 로그 출력을 확인
        Thread.sleep(2000);
    }
}
