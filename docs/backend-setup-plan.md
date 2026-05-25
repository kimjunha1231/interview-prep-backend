# 백엔드 초기 프로젝트 설정 및 데이터 마이그레이션 구현 계획 (Backend Setup Implementation Plan)

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Spring Boot 3.2.5와 Gradle 기반 백엔드 프로젝트 구조를 생성하고, Supabase PostgreSQL 데이터베이스에 연결하여 `questions.json` 시드 데이터를 자동으로 DB에 적재한다.

**Architecture:** 로컬 디렉터리 `/Users/junha/coding/interview-prep/backend`를 단독 Git 리포지토리로 개발하고, Spring Boot의 CommandLineRunner인 `DataInitializer`를 활용해 애플리케이션 시작 시 `questions.json`을 DB로 영속화한다.

**Tech Stack:** Java 17, Spring Boot 3.2.5, Gradle, Spring Data JPA, PostgreSQL Driver, Lombok, Jackson ObjectMapper, Supabase PostgreSQL

---

### Task 1: 백엔드 디렉터리 구성 및 Git 초기화

**Files:**
- Create: `/Users/junha/coding/interview-prep/backend/.gitignore`

- [ ] **Step 1: backend 디렉터리 생성 및 Git 초기화**

Run:
```bash
mkdir -p /Users/junha/coding/interview-prep/backend
cd /Users/junha/coding/interview-prep/backend
git init
```

- [ ] **Step 2: 기본 .gitignore 파일 생성**

Create `/Users/junha/coding/interview-prep/backend/.gitignore` with the following content:
```
.gradle
build/
!gradle-wrapper.jar
.project
.classpath
.settings
.metadata
bin/
tmp/
*.tmp
*.bak
*.swp
*~
.DS_Store
.idea/
*.iws
*.iml
*.ipr
out/
.vscode/
.env
.env.local
```

- [ ] **Step 3: Git에 첫 커밋 추가**

Check `.agent/config.yml` for `auto_commit` setting. If `auto_commit: true` (or default):
```bash
cd /Users/junha/coding/interview-prep/backend
git add .gitignore
git commit -m "chore: initialize backend git repository and gitignore"
```
If `auto_commit: false`: skip commit and staging.

---

### Task 2: Spring Boot 프로젝트 스캐폴딩 다운로드 및 압축 해제

**Files:**
- Create: `/Users/junha/coding/interview-prep/backend/build.gradle`
- Create: `/Users/junha/coding/interview-prep/backend/settings.gradle`
- Create: `/Users/junha/coding/interview-prep/backend/src/main/java/com/junha/interview/InterviewApplication.java`

- [ ] **Step 1: start.spring.io API를 사용해 스캐폴딩 zip 다운로드**

Run:
```bash
curl -G https://start.spring.io/starter.zip \
  -d dependencies=web,data-jpa,postgresql,lombok,validation,webflux \
  -d javaVersion=17 \
  -d bootVersion=3.2.5 \
  -d type=gradle-project \
  -d name=interview \
  -d packageName=com.junha.interview \
  -d groupId=com.junha \
  -d artifactId=interview \
  -o /Users/junha/coding/interview-prep/backend.zip
```

- [ ] **Step 2: 다운로드한 zip 파일 압축 해제**

Run:
```bash
unzip -o /Users/junha/coding/interview-prep/backend.zip -d /Users/junha/coding/interview-prep/temp_backend
mv /Users/junha/coding/interview-prep/temp_backend/interview/* /Users/junha/coding/interview-prep/backend/
mv /Users/junha/coding/interview-prep/temp_backend/interview/.[!.]* /Users/junha/coding/interview-prep/backend/ 2>/dev/null || true
rm -rf /Users/junha/coding/interview-prep/backend.zip /Users/junha/coding/interview-prep/temp_backend
```

- [ ] **Step 3: build.gradle 의존성 및 설정 검증**

Verify content of `/Users/junha/coding/interview-prep/backend/build.gradle` matches exactly:
```groovy
plugins {
	id 'java'
	id 'org.springframework.boot' version '3.2.5'
	id 'io.spring.dependency-management' version '1.1.4'
}

group = 'com.junha'
version = '0.0.1-SNAPSHOT'

java {
	sourceCompatibility = '17'
}

configurations {
	compileOnly {
		extendsFrom annotationProcessor
	}
}

repositories {
	mavenCentral()
}

dependencies {
	implementation 'org.springframework.boot:spring-boot-starter-data-jpa'
	implementation 'org.springframework.boot:spring-boot-starter-validation'
	implementation 'org.springframework.boot:spring-boot-starter-web'
	implementation 'org.springframework.boot:spring-boot-starter-webflux'
	compileOnly 'org.projectlombok:lombok'
	runtimeOnly 'org.postgresql:postgresql'
	annotationProcessor 'org.projectlombok:lombok'
	testImplementation 'org.springframework.boot:spring-boot-starter-test'
	testRuntimeOnly 'org.junit.platform:junit-platform-launcher'
}

tasks.named('test') {
	useJUnitPlatform()
}
```

- [ ] **Step 4: 스캐폴딩 상태 커밋**

Check `.agent/config.yml` for `auto_commit` setting. If `auto_commit: true` (or default):
```bash
cd /Users/junha/coding/interview-prep/backend
git add .
git commit -m "feat: scaffold Spring Boot project with Gradle"
```

---

### Task 3: 데이터베이스 및 기본 프로퍼티 설정

**Files:**
- Modify: `/Users/junha/coding/interview-prep/backend/src/main/resources/application.properties` (삭제 후 application.yml로 대체)
- Create: `/Users/junha/coding/interview-prep/backend/src/main/resources/application.yml`

- [ ] **Step 1: application.properties 삭제 및 application.yml 생성**

Run:
```bash
rm -f /Users/junha/coding/interview-prep/backend/src/main/resources/application.properties
```

Create `/Users/junha/coding/interview-prep/backend/src/main/resources/application.yml`:
```yaml
spring:
  datasource:
    url: jdbc:postgresql://${DB_HOST:localhost}:${DB_PORT:5432}/${DB_NAME:postgres}?sslmode=require
    username: ${DB_USER:postgres}
    password: ${DB_PASSWORD:password}
    driver-class-name: org.postgresql.Driver
  jpa:
    hibernate:
      ddl-auto: update
    show-sql: true
    properties:
      hibernate:
        format_sql: true
        dialect: org.hibernate.dialect.PostgreSQLDialect
```

- [ ] **Step 2: 설정 파일 상태 커밋**

Check `.agent/config.yml` for `auto_commit` setting. If `auto_commit: true` (or default):
```bash
cd /Users/junha/coding/interview-prep/backend
git add src/main/resources/
git commit -m "config: configure database connection parameters via environment variables"
```

---

### Task 4: JPA Entity, Repository 정의 및 Seed JSON 복사

**Files:**
- Create: `/Users/junha/coding/interview-prep/backend/src/main/java/com/junha/interview/domain/Question.java`
- Create: `/Users/junha/coding/interview-prep/backend/src/main/java/com/junha/interview/repository/QuestionRepository.java`
- Create (Copy): `/Users/junha/coding/interview-prep/backend/src/main/resources/questions.json`

- [ ] **Step 1: questions.json 복사**

Run:
```bash
cp /Users/junha/coding/Crawling/questions.json /Users/junha/coding/interview-prep/backend/src/main/resources/questions.json
```

- [ ] **Step 2: Question JPA 엔티티 생성**

Create `/Users/junha/coding/interview-prep/backend/src/main/java/com/junha/interview/domain/Question.java`:
```java
package com.junha.interview.domain;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

@Entity
@Table(name = "question")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class Question {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 50)
    private String category;

    @Column(nullable = false, length = 50)
    private String subject;

    @Column(nullable = false, length = 255)
    private String title;

    @Column(name = "perfect_answer", nullable = false, columnDefinition = "TEXT")
    private String perfectAnswer;
}
```

- [ ] **Step 3: QuestionRepository 생성**

Create `/Users/junha/coding/interview-prep/backend/src/main/java/com/junha/interview/repository/QuestionRepository.java`:
```java
package com.junha.interview.repository;

import com.junha.interview.domain.Question;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface QuestionRepository extends JpaRepository<Question, Long> {
}
```

- [ ] **Step 4: 상태 커밋**

Check `.agent/config.yml` for `auto_commit` setting. If `auto_commit: true` (or default):
```bash
cd /Users/junha/coding/interview-prep/backend
git add .
git commit -m "feat: add Question entity, repository and seed data json"
```

---

### Task 5: Seed Data 자동 적재 Initializer 구현 및 구동 확인

**Files:**
- Create: `/Users/junha/coding/interview-prep/backend/src/main/java/com/junha/interview/config/DataInitializer.java`
- Modify: `/Users/junha/coding/interview-prep/backend/src/test/java/com/junha/interview/InterviewApplicationTests.java` (테스트 환경에서도 기동되도록 Mock 설정 처리)

- [ ] **Step 1: DataInitializer 구현**

Create `/Users/junha/coding/interview-prep/backend/src/main/java/com/junha/interview/config/DataInitializer.java`:
```java
package com.junha.interview.config;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.junha.interview.domain.Question;
import com.junha.interview.repository.QuestionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;
import org.springframework.stereotype.Component;

import java.io.InputStream;
import java.util.List;

@Component
@RequiredArgsConstructor
@Slf4j
public class DataInitializer implements CommandLineRunner {

    private final QuestionRepository questionRepository;
    private final ResourceLoader resourceLoader;
    private final ObjectMapper objectMapper;

    @Override
    public void run(String... args) throws Exception {
        if (questionRepository.count() == 0) {
            log.info("Starting database seeding for questions...");
            Resource resource = resourceLoader.getResource("classpath:questions.json");
            try (InputStream inputStream = resource.getInputStream()) {
                List<Question> questions = objectMapper.readValue(inputStream, new TypeReference<List<Question>>() {});
                questionRepository.saveAll(questions);
                log.info("Successfully seeded {} questions into Supabase DB.", questions.size());
            } catch (Exception e) {
                log.error("Failed to seed question database.", e);
            }
        } else {
            log.info("Questions database already has data. Skipping initialization.");
        }
    }
}
```

- [ ] **Step 2: DB 로컬 환경변수 세팅 및 실행 검증**

사용자에게 받은 연결정보 환경변수를 export하고 gradlew 빌드 및 부트를 수행합니다.
(주의: 로컬 DB 연결이 정상적이어야 CommandLineRunner가 동작합니다.)

Run:
```bash
cd /Users/junha/coding/interview-prep/backend
./gradlew bootRun
```
Expected: `Successfully seeded 354 questions into Supabase DB.` 로그 확인.

- [ ] **Step 3: 테스트 코드 실행 검증**

Run:
```bash
cd /Users/junha/coding/interview-prep/backend
./gradlew test
```
Expected: BUILD SUCCESSFUL

- [ ] **Step 4: 최종 커밋**

Check `.agent/config.yml` for `auto_commit` setting. If `auto_commit: true` (or default):
```bash
cd /Users/junha/coding/interview-prep/backend
git add .
git commit -m "feat: implement DataInitializer for automatic database seeding and verification"
```
