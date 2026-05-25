package com.junha.interview.config;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.junha.interview.domain.Question;
import com.junha.interview.repository.QuestionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;
import org.springframework.stereotype.Component;

import java.io.InputStream;
import java.util.List;

@Component
@Profile("local")
@RequiredArgsConstructor
@Slf4j
public class DataInitializer implements CommandLineRunner {

    private final QuestionRepository questionRepository;
    private final ResourceLoader resourceLoader;
    private final ObjectMapper objectMapper;

    @Override
    public void run(String... args) throws Exception {
        log.info("Checking questions database for seeding or synchronization...");
        Resource resource = resourceLoader.getResource("classpath:questions.json");
        try (InputStream inputStream = resource.getInputStream()) {
            List<Question> questions = objectMapper.readValue(inputStream, new TypeReference<List<Question>>() {});
            
            if (questionRepository.count() == 0) {
                log.info("Database is empty. Performing fresh seeding of {} questions...", questions.size());
                questionRepository.saveAll(questions);
                log.info("Successfully seeded {} questions into the database.", questions.size());
            } else {
                log.info("Database already contains questions. Starting rich fields synchronization...");
                List<Question> existingQuestions = questionRepository.findAll();
                java.util.Map<String, Question> existingMap = existingQuestions.stream()
                        .collect(java.util.stream.Collectors.toMap(Question::getTitle, q -> q, (q1, q2) -> q1));
                
                java.util.List<Question> toSave = new java.util.ArrayList<>();
                int updateCount = 0;
                int insertCount = 0;
                
                for (Question newQ : questions) {
                    Question existing = existingMap.get(newQ.getTitle());
                    if (existing != null) {
                        // 만약 기존 데이터의 summary나 explanation이 비어있다면, 새로운 리치 필드들을 업데이트해줍니다.
                        if (existing.getSummary() == null || existing.getExplanation() == null) {
                            existing.setImportance(newQ.getImportance());
                            existing.setSummary(newQ.getSummary());
                            existing.setExplanation(newQ.getExplanation());
                            existing.setCaveats(newQ.getCaveats());
                            existing.setTailQuestions(newQ.getTailQuestions());
                            existing.setReferences(newQ.getReferences());
                            toSave.add(existing);
                            updateCount++;
                        }
                    } else {
                        // 새로운 질문인 경우 데이터 추가
                        toSave.add(newQ);
                        insertCount++;
                    }
                }
                
                if (!toSave.isEmpty()) {
                    questionRepository.saveAll(toSave);
                    log.info("Successfully synchronized questions database: {} updated, {} inserted.", updateCount, insertCount);
                } else {
                    log.info("All questions in the database are already up-to-date with rich fields.");
                }
            }
        } catch (Exception e) {
            log.error("Failed to seed or synchronize questions database.", e);
        }
    }
}
