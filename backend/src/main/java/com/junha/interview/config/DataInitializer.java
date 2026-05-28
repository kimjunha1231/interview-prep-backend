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

            // Fix legacy 'cs' subject values to correct specific subjects
            try {
                log.info("Correcting legacy 'cs' subject values to specific subjects...");
                int devopsUpdated = jdbcTemplate.update(
                    "UPDATE question SET subject = 'devops' WHERE subject = 'cs' AND title IN (" +
                    "'Docker와 컨테이너화의 개념', 'CI/CD 파이프라인의 개념', 'Kubernetes 오케스트레이션', " +
                    "'인프라 코드(Infrastructure as Code)', 'GitOps 배포 전략', " +
                    "'블루-그린 배포(Blue-Green Deployment)', '카나리 배포(Canary Deployment)')"
                );
                int systemDesignUpdated = jdbcTemplate.update(
                    "UPDATE question SET subject = 'system_design' WHERE subject = 'cs' AND title IN (" +
                    "'마이크로서비스 아키텍처(MSA)란?', 'API 게이트웨이의 역할', '서비스 메시(Service Mesh)의 개념', " +
                    "'이벤트 드리븐 아키텍처(EDA)란?', '메시지 큐(Message Queue)의 동작 원리', " +
                    "'CQRS 패턴이란?', '사가(Saga) 패턴', '분산 트랜잭션 처리', " +
                    "'서킷 브레이커(Circuit Breaker) 패턴', '최종적 일관성(Eventual Consistency)', " +
                    "'CAP 정리(CAP Theorem)', '샤딩(Sharding)과 파티셔닝', " +
                    "'로드 밸런서의 종류와 알고리즘', 'CDN(Content Delivery Network)의 동작 원리', " +
                    "'WebSocket과 HTTP Polling의 차이')"
                );
                int softwareEngUpdated = jdbcTemplate.update(
                    "UPDATE question SET subject = 'software_engineering' WHERE subject = 'cs' AND title IN (" +
                    "'객체지향 프로그래밍(OOP)의 4대 원칙', 'SOLID 원칙이란?', " +
                    "'디자인 패턴의 개념과 분류', '싱글톤 패턴(Singleton Pattern)', " +
                    "'팩토리 패턴(Factory Pattern)', '전략 패턴(Strategy Pattern)', " +
                    "'옵저버 패턴(Observer Pattern)', '의존성 주입(Dependency Injection)', " +
                    "'테스트 주도 개발(TDD)이란?', '유닛 테스트의 정의와 목적', " +
                    "'RESTful API 설계 원칙', 'GraphQL vs REST API', " +
                    "'클린 코드의 원칙', '리팩토링이란?', " +
                    "'애자일(Agile) 방법론', 'Git 브랜치 전략', " +
                    "'코드 리뷰의 목적과 best practices', '기술 부채(Technical Debt)', " +
                    "'소프트웨어 아키텍처 패턴', 'DDD(도메인 주도 설계)', " +
                    "'함수형 프로그래밍의 특징', '동시성과 병렬성의 차이', " +
                    "'메모리 관리와 가비지 컬렉션', '시간 복잡도와 공간 복잡도')"
                );
                // Any remaining 'cs' subjects that don't match specific titles → software_engineering
                int remainingUpdated = jdbcTemplate.update(
                    "UPDATE question SET subject = 'software_engineering' WHERE subject = 'cs'"
                );
                log.info("Subject correction complete: devops={}, system_design={}, software_engineering={}, remaining->software_engineering={}",
                    devopsUpdated, systemDesignUpdated, softwareEngUpdated, remainingUpdated);
            } catch (Exception e) {
                log.warn("Could not correct legacy 'cs' subjects: {}", e.getMessage());
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
