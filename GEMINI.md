# GEMINI.md (Knowledge Compounding)

본 문서는 **Interview Handbook (AI 모의 면접 및 개념 학습 플랫폼)** 프로젝트의 아키텍처, 기술적 특징, 그리고 프론트엔드/백엔드 연동 관련 특이사항을 명세하여 향후 원활하게 개발을 이어갈 수 있도록 돕는 지식 축적 가이드입니다.

---

## 🏛️ 1. 아키텍처 개요 (System Architecture)

*   **Frontend**: React (Vite), TypeScript, Tailwind CSS v3
*   **Backend**: Java 17, Spring Boot 3.x, Spring Data JPA, WebFlux WebClient
*   **Database**: Supabase PostgreSQL (무료 클라우드 티어 연동)
*   **External APIs**: Google Gemini API (AI 채점 및 피드백), Groq Whisper API (STT 음성 인식)

---

## 🎨 2. UI/UX & 디자인 사양 (Apple Minimalist Dark)

프로젝트 루트 아래 `frontend/DESIGN.md`에 명시된 Apple 디자인 원칙을 엄격하게 수용하여 흔한 AI 템플릿(AI Slop) 스타일을 전면 배제했습니다.

*   **Color Theme**: 
    *   메인 배경: Pure Black (`#000000`)
    *   엘리베이션 패널: Apple System Gray (`#1C1C1E` / `#2C2C2E`)
    *   핵심 액센트: Action Blue (`#0066CC` - 모든 상호작용 및 파란색 알약 버튼용)
*   **Typography**:
    *   크로스 플랫폼 한글 시각 일관성을 위해 **Pretendard** 폰트를 채택.
    *   경로: `frontend/public/fonts/Pretendard-Regular.ttf`, `Pretendard-Bold.ttf`
    *   `src/index.css` 상단에 `@font-face`로 로컬 정적 서빙하며, Tailwind 테마에 `font-sans` 및 `font-display` 맨 앞 폰트군으로 결합됨.
*   **Elevation**:
    *   버튼, 텍스트, 카드에는 그림자(Shadow)를 주지 않고 오직 1px의 투명 보더(`border border-white/5` 혹은 `border-white/10`)를 적용.
    *   오직 오디오 녹음 버튼 등의 미디어/제품 입체감이 필요한 요소에만 Soft Shadow(`rgba(0,0,0,0.22) 3px 5px 30px`) 1개만 허용.

---

## ⚡ 3. 프론트-백엔드 연동 및 API Proxy

*   **CORS 에러 우회**:
    *   `frontend/vite.config.ts`의 `server.proxy` 설정을 가동하여 브라우저에서 날아오는 `/api` 경로 요청을 백엔드 포트 `http://localhost:8080`으로 포워딩함.
    *   이로써 프론트엔드 측은 상대 경로 `/api/...`로 안전하게 백엔드를 호출함.
*   **오디오 녹음 및 STT 전송**:
    *   브라우저의 `MediaRecorder` API를 활용하여 음성을 캡처하고, 브라우저가 지원하는 MIME Type(오디오 포맷)을 동적으로 선별하여 Blob을 생성.
    *   해당 Blob 데이터를 `FormData`에 실어 `/api/interviews/transcribe` (multipart/form-data)로 쏘아 Groq Whisper API를 통해 0.5초 대의 초고속 한글 STT 텍스트 변환을 제공.
*   **AI 채점 및 꼬리 질문 릴레이**:
    *   답변 수정 후 제출 시 `/api/interviews/sessions/{sessionId}/answers` API를 호출.
    *   로딩 시 모노톤의 `animate-apple-pulse` 스켈레톤 로더를 기동하고 `role="status"` 처리하여 웹 접근성(a11y) 기준을 보장.
    *   응답 데이터의 `tailQuestion`이 존재하는 경우 사용자가 꼬리 질문 답변하기 루프를 계속 돌 수 있도록 상태 기계(State Machine) 설계.

---

## 🚀 4. GEO (Generative Engine Optimization)

*   페이지별 동적 SEO 및 AI 검색 봇용 구조화 데이터 주입을 위해 `react-helmet-async` 패키지를 연동.
*   `src/components/SEO.tsx`를 통해, 사용자가 개념 학습 핸드북의 질문을 상세 조회할 때 **Schema.org FAQPage JSON-LD 스키마**를 동적으로 구성하여 브라우저 `<head>` 영역에 `<script type="application/ld+json">` 태그로 밀어 넣음.
*   이를 통해 인공지능 검색 엔진(Perplexity, ChatGPT Search 등)이 크롤링할 때 정밀하게 질의-응답을 파악하고 정보 출처로 cite(인용)하도록 최적화.

---

## 🏗️ 5. React 컴포넌트 아키텍처 & 단일 책임 원칙 (SRP)

모놀리식 뷰의 비대화를 방지하고, 유지보수성과 재사용성을 최대화하기 위해 **Domain-Driven Feature-Based (도메인 피처 기반) 아키텍처**를 수용하여 설계되었습니다. 이는 엄격한 레이어로 인해 오버헤드가 큰 FSD(Feature-Sliced Design) 패턴과 난잡하게 섞이기 쉬운 전통적 `pages/components` 구조의 중간 절충형 구조입니다.

*   **공통 UI 컴포넌트 (`src/components/ui`)**:
    *   `Button.tsx`, `Card.tsx`, `Skeleton.tsx` 등 디자인 규격(DESIGN.md)의 원자적 요소들은 개별 비즈니스 상태를 갖지 않는 **순수 프레젠테이션 컴포넌트**로 구축됨.
    *   props 인터페이스를 통해 variant(Primary, Secondary, Dark-Utility 등) 및 size와 호버 인터랙션 여부를 주입받아 처리.
*   **컴포넌트 분리 및 합성 (SRP & Composition)**:
    *   `InterviewDashboard.tsx` 또는 `HandbookDashboard.tsx`와 같은 피처 메인 진입점은 전역 상태 관리 및 Hook 바인딩(Controller 역할)만 수행.
    *   세부 생명 주기 화면 및 기능 영역은 물리 파일로 완전히 쪼개어 배치:
        *   `InterviewSetup.tsx`: 면접 분야/개수 설정 폼
        *   `InterviewOngoing.tsx`: 녹음 제어 및 STT 인식 뷰
        *   `InterviewFeedback.tsx`: 채점 분석 및 꼬리 질문 선택
        *   `InterviewReport.tsx`: 최종 보고서, 아코디언 피드백 리스트, 이메일 폼
    *   이메일 입력(`emailInput`)과 같이 특정 단계에서만 국한되어 소모되는 내부 로컬 상태는 해당 자식 컴포넌트(`InterviewReport`) 내부에 격리하여 불필요한 부모 리렌더링 파급 효과를 완벽하게 차단함.

---

## 🛠️ 6. CI/CD & 로컬 DX 최적화 가이드

프로젝트 전반의 안정성과 로컬 개발 편의성(Developer Experience)을 보장하기 위해 다음과 같은 검증 체계와 도구를 운영합니다.

*   **GitHub Actions CI 파이프라인**:
    *   경로: `.github/workflows/ci.yml`
    *   역할: push / pull_request 발생 시 백엔드(Gradle 빌드)와 프론트엔드(Lint 검사 및 Vite 빌드)를 병렬 검증하여 항상 배포 가능한 마스터 브랜치를 유지합니다.
*   **루트 통합 DX 구동 스크립트**:
    *   명령어: `npm run dev`
    *   루트 package.json에 `concurrently`를 세팅하여 단일 터미널 세션 내에서 프론트엔드 개발 서버(`5173`)와 백엔드 개발 서버(`8080`)를 동시에 기동합니다.
*   **로컬 가상 데이터베이스 (Docker Compose)**:
    *   명령어: `docker compose up -d`
    *   경로: `docker-compose.yml`
    *   역할: 백엔드 `application.yml`의 로컬 DB 접속 규격(postgres/password)과 100% 매칭되는 PostgreSQL 15 컨테이너를 가상화 기동하여 로컬/오프라인에서의 DB 정합성을 확보합니다.
*   **프론트엔드 단위 테스트 및 TDD (Vitest)**:
    *   설정: `vitest.config.ts`, `src/test/setup.ts` (jest-dom 전역 바인딩).
    *   명령어: `npm run test`
    *   설명: `jsdom` 기반의 가상 브라우저 인프라를 활용하여 초고속(800ms대) React 컴포넌트 검증을 지원합니다.
*   **Prettier & ESLint 충돌 예방**:
    *   설정: `eslint.config.js` 플랫 컨피그 파일 하단에 `eslint-config-prettier`를 추가하여 코드 분석 룰과 프리티어 포맷 룰의 상호 마스킹 및 충돌 오버헤드를 예방했습니다.
    *   **useEffect 내 setState 경고 우회**: `react-hooks/set-state-in-effect` 경고를 방어하기 위해 Effect 본문 내에서 즉각 동기식으로 상태를 변경하는 대신, `Promise.resolve().then(() => setState(...))` 마이크로태스크 비동기 틱으로 밀고 컴포넌트 소멸 감지용 active 플래그를 결합하여 작성합니다.

---

## ⚡ 7. 비동기 이벤트 기반 이메일 발송망 (Async Email Dispatch System)

*   **스레드 풀 격리 & OOM 방지**:
    *   외부 메일 SMTP 전송 지연(1~3초)이 사용자 응답 스레드를 블로킹하지 않도록, `@EnableAsync` 설정과 함께 Core(5), Max(10), Queue(50) 한도를 갖는 전용 `mailExecutor` 스레드 풀을 구성하여 리소스를 제어합니다.
*   **이벤트 기반 디커플링 (Decoupling)**:
    *   면접 보고서 이메일 발송 요청 시, `InterviewService`가 `ApplicationEventPublisher`를 통해 `InterviewCompletedEvent`를 발행하고, `@Async("mailExecutor") @EventListener`를 정의한 `EmailListener`가 이를 받아 비동기 스레드에서 `EmailService.sendReportEmail`을 수행하여 결합도를 낮췄습니다.
*   **회복성 & 이식성 (Resiliency & Portability)**:
    *   `EmailService`는 `@Autowired(required = false)`를 통해 `JavaMailSender`를 주입받아, SMTP 설정이 없는 로컬 개발/테스트 환경에서도 부트 실패 없이 실행됩니다.
    *   메일 전송 실패 혹은 SMTP 누락 시 `try-catch` 및 로그 폴백 안전망(Error Isolation)을 가동하여 렌더링된 HTML 리포트 내용을 표준 출력 로그에 남기며, 이메일 오류가 전체 프로세스에 영향을 주지 않도록 격리했습니다.

---

## 🛡️ 8. 카테고리 식별자 정규화 레이어 (Category Normalizer Layer)

*   **식별자 불일치 해결**:
    *   프론트엔드에서 모의 면접 생성 시 송신하는 카테고리 식별자(`"FE"`, `"BE"`)와 실제 DB 테이블에 적재된 질문의 카테고리 명칭(`"Frontend"`, `"Backend"`) 간의 Mismatch 문제를 해결하기 위해, 백엔드 서비스 레이어(`InterviewService.java`) 진입점에 Normalizing 방어 로직을 구현했습니다.
    *   이를 통해 상세 과목(`subjects`) 선택 여부와 무관하게 카테고리만으로 질문을 조회하는 폴백 상황에서도 0개의 질문 리스트가 조회되어 프론트엔드가 크래시되는 현상을 원천적으로 차단했습니다.

---

## 🎙️ 9. AI 기반 2차 STT 텍스트 교정 파이프라인 (2-Stage STT Correction)

*   **음독 기술 용어 복원 및 맞춤법 보정**:
    *   Whisper STT가 한글 소리 나는 대로 변환해 버린 IT 전문 용어(예: "디비 엔투엔 제이피에이" -> "DB N+1 JPA")를 복원하고, 맞춤법 및 불필요한 반복어(어, 음)를 정제하기 위해 `GeminiService.correctTranscribedText` 비차단 교정 레이어를 설계했습니다.
    *   사용자의 말실수 정제에 집중하고, AI가 임의로 면접 기술 논리를 창작(Hallucination)하여 문장에 덧붙이지 않도록 시스템 프롬프트를 보장했습니다.
*   **회복성 폴백 (Fallback)**:
    *   API Key가 누락되었거나 Gemini 호출 실패(429 등)가 발생하는 런타임 상황에서도, 시스템 전체 오류로 전파시키지 않고 1차 Whisper 원본 STT 텍스트를 그대로 반환하는 안전 폴백 구조를 적용했습니다.

---

## 📬 10. 데일리 구독 분야 선택 및 비동기 스케줄러 연동

*   **맞춤 카테고리 영속화**:
    *   `EmailSubscription` 엔티티에 `category` 속성(기본값 `"ALL"`)을 신설하여 원하는 분야(`Frontend`, `Backend`, `CS`, `ALL`)의 질문만 선택적으로 받아볼 수 있도록 구조를 확장했습니다.
*   **In-Memory 캐싱 및 비동기 발송 루프**:
    *   `Question` 전체를 1회만 조회하여 In-memory Map으로 그룹핑을 완료함으로써 구독자 순회 시 발생하는 N+1 DB 커리 부하를 방어했습니다.
    *   구독자마다 설정된 카테고리를 추출하여 매칭 질문을 무작위 발송하고, 메일 발송 로직 자체는 전용 `mailExecutor` 스레드 풀에 위임(`@Async`)하여 메일 서버의 대기 지연이 전체 스케줄러 배치 기동을 블로킹하지 않도록 장애를 예방했습니다.

---

## 💾 11. Supabase PostgreSQL 및 Connection Pooler 설정

*   **IPv4 호환 Connection Pooler 사용**:
    *   Supabase Direct Connection Domain은 IPv6-only 주소이므로 IPv4 전용 로컬망에서는 `NoRouteToHostException`이 발생합니다.
    *   따라서 IPv4 환경 호환을 지원하는 **Regional Connection Pooler Host**(`aws-1-ap-northeast-2.pooler.supabase.com`)와 포트 `5432`를 사용합니다.
    *   유저명 형식은 반드시 풀러 라우팅 규격에 맞춘 `postgres.[project_id]`를 준수해야 합니다.
*   **Prepared Statement 충돌 방지 (`prepareThreshold=0`)**:
    *   트랜잭션 풀링 모드 하에서 커넥션이 무작위로 여러 세션에 공유/교체되므로, 드라이버 기본 설정인 서버 사이드 prepared statement 이름(`S_1`)이 다른 세션과 충돌하여 롤백을 야기하는 오류가 발생합니다.
    *   이를 방지하기 위해 JDBC URL의 파라미터에 `prepareThreshold=0` 설정을 필수 매핑하여 드라이버 레벨에서 prepared statement 캐싱을 비활성화 처리합니다.
*   **JPA Auto DDL 충돌 복구 전략**:
    *   JPA의 `ddl-auto: update`는 기존 PostgreSQL 테이블의 컬럼 타입 변경 캐스팅(Casting)이나 FK 제약 조건 수정을 자동으로 소화하지 못합니다.
    *   초기 적재 단계에서는 `ddl-auto: create` 설정을 적용하여 모든 기존 테이블 구조를 깨끗하게 재생성한 후 `DataInitializer`를 통해 `questions.json`을 다시 시딩(Seeding)하고, 완료된 즉시 `ddl-auto: update`로 복귀 및 고정하는 방식으로 대응합니다.

---

## ⚡ 12. TanStack Query (React Query v5) 캐싱 가이드

*   **정적 질문 데이터셋 영구 캐싱**:
    *   개념 학습 핸드북의 질문 데이터는 정적인 리소스로 실시간 동기화가 불필요합니다.
    *   따라서 `useQuery` 호출 시 `staleTime: Infinity` 및 `gcTime: Infinity` 설정을 명시적으로 부여하여 한 번 패치된 데이터를 브라우저 탭 세션 동안 완벽하게 메모리에 캐싱하고, 탭 재진입 시 API 재요청을 원천 차단하여 즉각적인 렌더링(0ms)을 제공합니다.
*   **Query Key 설계 일관성**:
    *   각 탭의 subject key(예: `JAVASCRIPT`, `REACT`)를 쿼리 키 `["questions", selectedSubjectKey]` 형태로 매핑하여, 각 탭 단위의 데이터 병합 및 가공 로직이 queryFn 내에서 격리 수행되도록 단순화했습니다.
*   **React 19 호환성**:
    *   Vite 빌드 파이프라인에서 React 19와 리액트 쿼리 v5 간의 Peer Dependency 정합성을 검증 완료하였으며, 컴포넌트 렌더링 생명 주기가 완전히 리액트 쿼리에 의해 안전하게 제어되고 있습니다.

---

## 🎨 13. 화이트 모드 및 반응형 UI 디자인 가이드

*   **반응형 테마 지원**:
    *   화이트 모드(라이트 모드)와 다크 모드에 모두 대응하기 위해 배경색 및 보더선 색상은 명시적 다크/라이트 CSS 클래스를 통해 바인딩되어야 합니다.
    *   예: `bg-apple-canvas-parchment dark:bg-apple-surface-black border-black/10 dark:border-white/10`
*   **텍스트 대비 및 가독성 (a11y)**:
    *   화이트 모드의 흰 배경에서 `#cccccc` (기존 `text-apple-body-muted`)과 같은 흐릿한 회색 계열이나 노란색 경고 문구는 명도 대비 부족으로 가독성이 심각하게 저하됩니다.
    *   이에 따라 텍스트 성격에 맞추어 `text-gray-500 dark:text-apple-body-muted`, `text-amber-600 dark:text-yellow-500`과 같이 명시적으로 분리하여 가독성을 보완해야 합니다.
*   **공용 컴포넌트의 다이나믹 테마 유연성**:
    *   `Button.tsx` 등의 공용 UI 컴포넌트 variant 중 `dark-utility` 같은 유틸리티 요소도 단일 테마에 하드코딩되지 않고 테마 전환에 유기적으로 연동되도록 반응형 클래스 처리를 거쳐야 합니다.

---

## ⚡ 14. Google Gemini API Key Rotation (3중 다중화)

*   **API Key Rotation을 통한 AI 무중단 서빙**:
    *   구글 Gemini API의 무료 일일 할당량 소진 또는 순간 트래픽 제한(429 Too Many Requests), API Key 만료/인증 에러(403 Forbidden)가 발생할 때 서비스가 마비되는 것을 막기 위해 API Key 3중 로테이션 체계를 가동합니다.
*   **환경변수 바인딩**:
    *   `GEMINI_API_KEY_PRIMARY` (기존 `GEMINI_API_KEY`와 호환 폴백), `GEMINI_API_KEY_SECONDARY`, `GEMINI_API_KEY_TERTIARY` 세 개의 키를 주입받을 수 있도록 `application.yml`에 설정되어 있습니다.
    *   키 필드가 지정되지 않거나 `none` 값일 경우에는 자동으로 필터링 및 제외되어, 가용한 키 개수에 따라 동적으로 이중화/단일 모드로 폴백 기동됩니다.
*   **이중 루프 기반의 Key-Model Fallback Chain**:
    *   `callGeminiApi` 메서드 내부에서 `[API Key 루프] - [MODEL_CHAIN 루프]`의 이중 구조가 실행됩니다.
    *   특정 API Key로 요청 중 429(할당량 초과) 혹은 403(인증 에러) 에러 발생 시, 무의미하게 하위 모델 체인을 호출하지 않고 즉시 내부 루프를 탈출(`break`)하여 다음 대기 키로 교체 스위칭 후 최상위 모델부터 재요청을 즉시 전개합니다.

