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
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.jdbc.core.JdbcTemplate;

@Component
@Profile("local")
@RequiredArgsConstructor
@Slf4j
public class DataInitializer implements CommandLineRunner {

    private final QuestionRepository questionRepository;
    private final ResourceLoader resourceLoader;
    private final ObjectMapper objectMapper;
    private final JdbcTemplate jdbcTemplate;

    @Override
    public void run(String... args) throws Exception {
        try {
            log.info("Executing DDL to ensure question.title is TEXT type...");
            jdbcTemplate.execute("ALTER TABLE question ALTER COLUMN title TYPE TEXT");
            log.info("Successfully ensured question.title is TEXT type.");

            try {
                log.info("Correcting polluted titles in DB prior to synchronization...");
                jdbcTemplate.execute("UPDATE question SET title = '최종적 일관성(Eventual Consistency)' WHERE title = 'Generate the representative question' OR title = '{title}'");
                jdbcTemplate.execute("UPDATE question SET title = '유닛 테스트의 정의와 목적' WHERE title = 'cs_cs_432' OR title = '유닛 테스트'");
                jdbcTemplate.execute("UPDATE question SET title = 'var 키워드는 뭔가요?' WHERE title = 'fe_javascript_301'");
                jdbcTemplate.execute("UPDATE question SET title = '자바스크립트의 Truthy와 Falsy 개념' WHERE title = 'fe_javascript_310'");
                jdbcTemplate.execute("UPDATE question SET title = '자바스크립트 프로미스(Promise)의 개념과 동작 원리' WHERE title = 'fe_javascript_366'");
                jdbcTemplate.execute("UPDATE question SET title = '전역 상태 관리 라이브러리의 필요성과 동작 원리' WHERE title = 'fe_react_059'");
                jdbcTemplate.execute("UPDATE question SET title = 'DOM에서 Node와 Element의 차이점' WHERE title = 'fe_react_060'");
                log.info("Successfully corrected polluted titles in DB.");
            } catch (Exception e) {
                log.warn("Could not correct polluted titles (they might have been corrected already, or table doesn't exist yet): {}", e.getMessage());
            }
        } catch (Exception e) {
            log.warn("Could not alter question.title to TEXT (it might already be TEXT, or table doesn't exist yet): {}", e.getMessage());
        }

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
                        .collect(Collectors.toMap(Question::getTitle, q -> q, (q1, q2) -> q1));
                
                List<Question> toSave = new ArrayList<>();
                int updateCount = 0;
                int insertCount = 0;
                
                for (Question newQ : questions) {
                    Question existing = existingMap.get(newQ.getTitle());
                    if (existing != null) {
                        boolean modified = false;
                        if (newQ.getCategory() != null && !newQ.getCategory().equals(existing.getCategory())) {
                            existing.setCategory(newQ.getCategory());
                            modified = true;
                        }
                        if (newQ.getSubject() != null && !newQ.getSubject().equals(existing.getSubject())) {
                            existing.setSubject(newQ.getSubject());
                            modified = true;
                        }
                        if (newQ.getPerfectAnswer() != null && !newQ.getPerfectAnswer().equals(existing.getPerfectAnswer())) {
                            existing.setPerfectAnswer(newQ.getPerfectAnswer());
                            modified = true;
                        }
                        if (newQ.getImportance() != null && !newQ.getImportance().equals(existing.getImportance())) {
                            existing.setImportance(newQ.getImportance());
                            modified = true;
                        }
                        if (newQ.getSummary() != null && !newQ.getSummary().equals(existing.getSummary())) {
                            existing.setSummary(newQ.getSummary());
                            modified = true;
                        }
                        if (newQ.getExplanation() != null && !newQ.getExplanation().equals(existing.getExplanation())) {
                            existing.setExplanation(newQ.getExplanation());
                            modified = true;
                        }
                        if (newQ.getCaveats() != null && !newQ.getCaveats().equals(existing.getCaveats())) {
                            existing.setCaveats(newQ.getCaveats());
                            modified = true;
                        }
                        if (newQ.getTailQuestions() != null && !newQ.getTailQuestions().equals(existing.getTailQuestions())) {
                            existing.setTailQuestions(newQ.getTailQuestions());
                            modified = true;
                        }
                        if (newQ.getReferences() != null && !newQ.getReferences().equals(existing.getReferences())) {
                            existing.setReferences(newQ.getReferences());
                            modified = true;
                        }
                        
                        if (modified) {
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
