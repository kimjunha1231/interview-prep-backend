# Interview Handbook - Backend

Java 17 및 Spring Boot 3.x 환경으로 구현된 **Interview Handbook** 비동기 REST API 서버 애플리케이션입니다.
외부 AI 서비스(Gemini, Groq) 연동 지연에 따른 Tomcat 작업 스레드 블로킹 예방 및 초경량 무료 인프라에서의 안정성 향상을 타겟으로 설계되었습니다.

---

## ⚡ 핵심 백엔드 아키텍처 및 특징

### 1. 비동기 비차단 외부 API 파이프라인 (Spring WebFlux WebClient)
- 사용자의 모의 면접 채점 피드백 생성(Gemini API) 및 음성 번역(Groq Whisper API)은 수 초 이상의 네트워크 대기 지연을 유발합니다.
- 동기형 API 호출 도중 톰캣 요청 스레드가 차단(Blocking)되어 결국 커넥션 풀이 고갈되는 장애를 막기 위해, 리액티브 Netty 기반의 `WebClient`를 채택하여 적은 스레드로 높은 처리량(Throughput)을 유지하게 설계했습니다.

### 2. 무중단 3중 API Key Rotation 및 Key-Model Fallback Chain
- Gemini API의 일일 할당량 한계(429 Too Many Requests) 및 API Key 권한 만료(403 Forbidden)가 발생하더라도 서비스가 셧다운되지 않도록 `Primary`, `Secondary`, `Tertiary` 3단계 API Key 회복망을 운영합니다.
- 특정 API Key 호출 중 429/403 예외가 검출되는 런타임 즉시 하위 모델 체인을 호출하는 내부 루프를 스킵(Break)하고 다음 순위 대기 API Key로 스위칭하여 재시도함으로써, 무의미한 지연 시간 발생을 획기적으로 방지합니다.
- 환경변수에 일부 예비 키가 미지정(`none`)되더라도 서버 부트가 차단되지 않고 동적으로 필터링되는 자가 복구형 설계를 적용했습니다.

### 3. 이벤트 기반 비동기 이메일 발송망 (Async Email Dispatch System)
- 면접 완료 보고서의 이메일 전송 SMTP 네트워크 지연(약 1~3초)이 사용자 최종 API 응답 스레드를 가두지 않도록 처리했습니다.
- 면접 저장이 완료되는 순간 `ApplicationEventPublisher`를 통해 `InterviewCompletedEvent`를 발행하고, 메일 발송 전용 스레드 풀(`mailExecutor`)을 할당받은 `@Async` 이벤트 리스너가 이를 비차단으로 수신 및 발송하도록 결합도를 대폭 낮췄습니다.
- 예외 격리(Error Isolation)를 가동하여 메일 전송 오류나 SMTP 미설정(로컬/테스트 환경) 상황에서도 사용자 모의 면접 저장 트랜잭션의 롤백을 차단합니다.

### 4. title 기반 Rich Fields 동적 시딩 및 동기화 (DataInitializer)
- 마크다운으로 관리되는 면접 문항 파일이 컴파일되어 자바 백엔드로 넘어올 때, 기존 DB의 Auto-increment PK 구조를 훼손하지 않기 위해 `title`을 1:1로 매핑하여 수정된 본문이나 속성(`category`, `subject`)만을 단일 트랜잭션 내에서 UPDATE하는 dynamic sync를 구동합니다.
- 타이틀이 깨진 상태로 적재되어 동기화가 실패하고 중복 삽입이 유발되는 것을 해결하기 위해, 시딩 기동 직전에 `jdbcTemplate`을 활용해 DB 내의 구형 오염 질문명들을 한글 정제명으로 정상 보정하는 DDL/DML 전처리 스케줄러를 가동합니다.

---

## 🛠️ 구동 및 빌드 방법

### 1. 로컬 가상 데이터베이스 기동
백엔드는 데이터 영속화를 위해 Supabase PostgreSQL 또는 로컬 PostgreSQL 15를 사용합니다.
```bash
# 프로젝트 루트 폴더에서 실행
docker compose up -d
```

### 2. 로컬 실행
`src/main/resources/application.yml`에 API Key를 바인딩하고 구동합니다.
```bash
./gradlew bootRun
```

### 3. 빌드 및 테스트 검증
테스트 코드를 포함하여 jar 아카이브 컴파일을 검증합니다.
```bash
./gradlew build
```
- 로컬 DB가 가동되지 않은 CI/CD 검증 파이프라인(GitHub Actions) 환경에서는 테스트의 DB 커넥션 예외를 배제하기 위해 `./gradlew build -x test`를 사용합니다.
