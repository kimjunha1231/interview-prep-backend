# 🏗️ Interview Prep - Backend Design Specification

본 문서는 면접 질문 학습 및 AI 모의 면접 서비스의 백엔드(Spring Boot) 상세 설계서입니다.

---

## 🛠️ 기술 스택 (Tech Stack)

| 구분 | 기술 스택 | 상세 설정 및 선택 사유 |
| :--- | :--- | :--- |
| **언어 및 프레임워크** | Java 17, Spring Boot 3.2.5 | 톰캣 기본 탑재 및 엔터프라이즈급 API 제공 안정성 |
| **빌드 도구** | Gradle (Groovy DSL) | 의존성 관리 및 빠른 빌드 속도 |
| **데이터베이스** | PostgreSQL (Supabase) | 평생 무료 PostgreSQL 데이터베이스 활용 |
| **ORM / JPA** | Spring Data JPA / Hibernate | 데이터베이스 객체 매핑 및 생산성 확보 |
| **비동기 / AI 연동** | WebClient (Spring WebFlux) | 외부 AI API 호출 시 톰캣 스레드 차단 방지 |
| **유틸리티** | Lombok, Spring Validation | 코드 축소 및 DTO 필드 유효성 검증 |

---

## 📂 디렉터리 구조 및 패키지 설계

```
/Users/junha/coding/interview-prep/backend/
├── src/
│   ├── main/
│   │   ├── java/com/junha/interview/
│   │   │   ├── InterviewApplication.java      # 메인 구동 클래스
│   │   │   ├── config/                        # Async, WebClient, DB 커넥션 설정
│   │   │   ├── controller/                    # REST API 컨트롤러
│   │   │   ├── service/                       # 비즈니스 서비스 로직
│   │   │   ├── repository/                    # JPA 레포지토리 인터페이스
│   │   │   └── domain/                        # JPA 엔티티 및 DTO
│   │   └── resources/
│   │       ├── application.yml                # Supabase DB 및 외부 API 키 주입 설정
│   │       └── questions.json                 # 초기 적재용 면접 질문 데이터 (Seed)
├── build.gradle
└── settings.gradle
```

---

## 🗄️ 데이터베이스 스키마 및 JPA 매핑 (Supabase)

Supabase에서 정의한 테이블 구조에 대응하는 JPA 엔티티 설계입니다.

### 1. `question` (면접 질문 풀)
```sql
CREATE TABLE question (
    id BIGSERIAL PRIMARY KEY,
    category VARCHAR(50) NOT NULL,    -- Frontend, Backend 등
    subject VARCHAR(50) NOT NULL,     -- JavaScript, Java 등
    title VARCHAR(255) NOT NULL,      -- 질문 내용
    perfect_answer TEXT NOT NULL      -- AI 생성 모범 답안
);
```

### 2. `member` (회원 테이블)
```sql
CREATE TABLE member (
    id BIGSERIAL PRIMARY KEY,
    email VARCHAR(100) UNIQUE NOT NULL,
    nickname VARCHAR(50) NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP
);
```

### 3. `interview_session` (모의 면접 세션)
```sql
CREATE TABLE interview_session (
    id BIGSERIAL PRIMARY KEY,
    member_id BIGINT REFERENCES member(id) ON DELETE CASCADE,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP
);
```

### 4. `interview_history` (세션별 개별 응답 & 피드백 이력)
```sql
CREATE TABLE interview_history (
    id BIGSERIAL PRIMARY KEY,
    session_id BIGINT REFERENCES interview_session(id) ON DELETE CASCADE,
    question_id BIGINT REFERENCES question(id),
    user_answer TEXT NOT NULL,
    score INT NOT NULL,
    feedback TEXT NOT NULL,
    tail_question VARCHAR(255)
);
```

---

## 🚀 Phase 1: 초기 설정 및 기동 목표

### 1. Spring Boot 기본 뼈대 생성
- `start.spring.io`를 활용하여 Gradle 프로젝트 스캐폴딩.
- 필수 의존성 추가: `web`, `data-jpa`, `postgresql`, `lombok`, `validation`, `webflux`.

### 2. Supabase DB 연동 검증
- `application.yml`에 Supabase Connection Info 매핑.
- 로컬 기동 시 DB 커넥션 맺어지는지 콘솔 로그 확인.

### 3. Seed 데이터 자동 적재 (DataInitializer)
- 스프링이 부팅될 때 `src/main/resources/questions.json`을 탐색.
- DB의 `question` 테이블 레코드 개수가 0개이면 JSON 데이터를 읽어 전체 파싱 후 `SaveAll` 처리.

---

## 🚀 Phase 2: 랜덤 질문 조회 API 개발

### 1. 기능 사양 (Requirements)
- **과도한 데이터 조회 제한**: 메모리 보호를 위해 조회 개수 `count`는 **최대 10개**로 제한하며, 그 이상의 값이 들어오거나 값이 비어있는 경우 다음과 같이 보정한다.
  - `count` 미지정 또는 `1` 미만: `1`로 처리
  - `count > 10`: `10`으로 강제 제한 (Math.min 처리)
- **필터링 조건**: `category`와 `subject` 파라미터는 선택 사항(Optional)이며, 빈 문자열이나 누락된 상태일 경우 필터링 조건에서 제외하여 전체 데이터를 대상으로 무작위 추출한다.

### 2. API 엔드포인트 설계
- **Method & Path**: `GET /api/questions/random`
- **Query Parameters**:
  - `category` (String, Optional)
  - `subject` (String, Optional)
  - `count` (Integer, Optional, 기본값: 1)

### 3. 데이터베이스 쿼리 설계 (Native Query)
- JPA에서 RANDOM 정렬 및 LIMIT을 데이터베이스 수준에서 고속으로 수행하기 위해 Native Query를 사용한다. 
- PostgreSQL 및 로컬 테스트용 H2의 `RANDOM()` 문법 호환성을 갖춘 쿼리로 작성한다.

```sql
SELECT * FROM question 
WHERE (:category IS NULL OR category = :category)
  AND (:subject IS NULL OR subject = :subject)
ORDER BY RANDOM() LIMIT :count
```

### 4. 핵심 아키텍처 및 TDD 검증 계획
- **Repository**: `@Query(nativeQuery = true)`를 적용한 랜덤 추출 메소드 선언.
- **Service**: 컨트롤러 요청을 받아 파라미터 유효성 검증(Null/Empty 처리 및 Count 10개 강제 제약)을 처리한 뒤 Repository 호출.
- **Controller**: REST API 엔드포인트 매핑 및 서비스 연동.
- **TDD (Test-Driven Development)**:
  - `QuestionControllerTest`를 작성하여 `MockMvc` 기반으로 필터링 및 갯수 제한 비즈니스 로직에 대한 **실패 테스트(RED)**를 먼저 작성한다.
  - 실제 API 구현 코드를 완성하여 테스트를 **성공(GREEN)** 상태로 만들고 리팩토링을 수행한다.

