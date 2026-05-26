# 🎯 Interview Handbook

AI 모의 면접 및 개발 개념 학습을 돕는 통합 플랫폼, **Interview Handbook**입니다.  
본 프로젝트는 **React 19 (Vite) 프론트엔드**와 **Spring Boot 3.x 비동기 백엔드**로 구성되어 있으며, 초경량 무료 인프라에서의 안정성을 극대화하고 외부 AI 서비스 연동 지연으로 인한 병목을 방지하도록 유기적으로 설계되었습니다.

---

## ✨ 핵심 서비스 기능 (Key Features)

### 1. 🎙️ AI 모의 면접 및 꼬리 질문 피드백 (Interactive Mock Interview)
* **음성 인식 기반 모의 면접**: 브라우저의 `MediaRecorder` API를 이용해 답변 음성을 실시간 녹음하고 고속 전송합니다.
* **2단계 STT 보정 파이프라인 (2-Stage STT)**: 1차 Whisper API로 한글 소리 나는 대로 받아적은 텍스트를, 2차 Gemini API를 거쳐 IT 전문 용어(예: "디비 엔투엔 제이피에이" ➔ "DB N+1 JPA") 복원 및 문맥 맞춤법을 자동으로 보정합니다. (실패 시 원본 Whisper 텍스트 안전 폴백)
* **AI 상세 채점 및 피드백**: 답변 수정 후 제출 시 Google Gemini API를 이용해 정밀한 채점 결과 및 꼬리 질문(Tail Question)을 실시간으로 도출합니다.
* **꼬리 질문 릴레이**: AI가 제안한 꼬리 질문에 꼬리를 물고 연속 답변 및 심화 면접을 진행할 수 있는 상태 기계를 지원합니다.
* **결과 보고서 및 이메일 전송**: 최종 면접 완료 시 대시보드 형태의 보고서와 아코디언 피드백을 제공하며, 결과 보고서를 이메일로 즉시 전송할 수 있습니다.

### 2. 📚 개념 학습 핸드북 (Q&A Handbook)
* **카테고리별 핵심 질문 목록**: Frontend, Backend, Computer Science 등 직군별/분야별 기술 면접 단골 질문과 핵심 모범 답안을 제공합니다.
* **0ms 무지연 화면 전환**: 캐싱 라이브러리(`TanStack Query v5`)를 활용해 한 번 조회된 질문 상세 내용은 브라우저 메모리에 영구 보존하여, 탭 재진입 시 0ms만에 화면을 즉각 렌더링하고 서버 호출을 원천 차단합니다.
* **인공지능 검색 최적화 (GEO/SEO)**: 각 질문 상세 조회 시 Schema.org FAQPage JSON-LD 스키마와 메타 태그를 동적으로 주입하여 Perplexity, ChatGPT Search 등 차세대 AI 검색 엔진에 완벽히 크롤링되도록 구성했습니다.

### 3. 📂 포트폴리오(PDF) 분석 및 맞춤형 질문 생성 (Edge Computing)
* **브라우저 사이드 PDF 파싱**: 대용량 포트폴리오 바이너리 파일을 서버로 전송하지 않고, 프론트엔드의 `PDF.js` 라이브러리를 가동하여 브라우저 내에서 직접 텍스트 노드만을 정밀 파싱합니다. 서버의 네트워크 대역폭과 RAM 부하를 원천적으로 절감합니다.
* **맞춤 기술 질문 도출**: 파싱된 포트폴리오 텍스트의 기술 스택 및 프로젝트 경험을 기반으로 AI가 개인 맞춤형 면접 문항을 동적 추출합니다.

### 4. 📬 맞춤형 데일리 질문 구독 서비스
* **맞춤 분야 구독**: 사용자가 이메일과 함께 원하는 학습 카테고리(`Frontend`, `Backend`, `CS`, `ALL`)를 선택하여 매일 아침 데일리 학습 질문 메일을 받아볼 수 있습니다.

---

## ⚡ 핵심 기술 아키텍처 및 최적화 특징

### 1. 비동기 비차단 외부 API 파이프라인 (Spring WebFlux WebClient)
* 사용자의 면접 채점 피드백(Gemini API) 및 음성 번역(Groq Whisper API) 처리 도중 톰캣 요청 스레드가 차단(Blocking)되어 커넥션 풀이 고갈되는 장애를 막기 위해, 리액티브 Netty 기반의 `WebClient`를 채택하여 높은 처리량(Throughput)을 보장합니다.

### 2. 무중단 3중 API Key Rotation 및 Key-Model Fallback Chain
* Gemini API의 무료 제공 할당량 소진(429 Too Many Requests) 및 API Key 인증 실패(403 Forbidden) 상황에 대응하여 `Primary`, `Secondary`, `Tertiary` 3단계 API Key 로테이션 체계를 운용합니다.
* API Key에 장애 발생 시 하위 모델 체인에서 시간 낭비 없이 즉각 다음 순위 대기 Key로 교체 스위칭하여 재시도하도록 설계되었습니다.

### 3. 이벤트 기반 비동기 이메일 발송망 (Async Email Dispatch System)
* 메일 전송 SMTP 네트워크 지연(1~3초)이 사용자 응답 스레드를 가두지 않도록, `ApplicationEventPublisher` 기반 이벤트 디커플링을 적용하고, 별도 격리된 `mailExecutor` 스레드 풀을 할당하여 `@Async`로 안전하고 신속하게 메일을 비차단 전송합니다.
* 예외 격리(Error Isolation) 체계를 통해 SMTP 오류가 발생하더라도 사용자 면접 저장 트랜잭션의 롤백을 차단합니다.

### 4. N+1 쿼리 방어 및 In-Memory 구독 발송 스케줄러
* 매일 아침 구독자들에게 질문 메일을 발송하는 배치 수행 시, N명의 구독자마다 개별 DB 쿼리를 유발하지 않도록 `Question` 전체를 1회만 조회하여 In-memory Map으로 그룹핑을 완료함으로써 DB 부하를 원천적으로 예방합니다.

### 5. WAI-ARIA 웹 접근성 (a11y) 표준 준수
* 스크린 리더의 구조 해석 직관성을 확보하도록 시맨틱 마크업을 채택하였고, 대화형 아코디언 컴포넌트에 `aria-expanded`/`aria-controls`를 실시간 동기화하여 상태 변화를 리더기에 전파합니다.
* 스켈레톤 로더에는 `aria-busy="true"` 및 `aria-live="polite"`를 선언해 시각적으로 대기 중인 상태를 명확히 인지시킵니다.

---

## 🛠️ 구동 및 빌드 방법

### 1. 로컬 가상 데이터베이스 기동
백엔드의 데이터 영속화를 위해 Supabase PostgreSQL 및 로컬 Docker PostgreSQL을 사용합니다.
```bash
# 프로젝트 루트 폴더에서 Docker 컨테이너 기동
docker compose up -d
```

### 2. 통합 환경 실행 (추천)
루트 경로에 세팅된 통합 DX 스크립트를 활용해 단일 터미널 세션 내에서 프론트엔드와 백엔드 개발 서버를 동시에 기동합니다. (Vite의 API 프록시 설정을 통해 CORS 오류가 자동으로 우회됩니다.)
```bash
# 의존성 패키지 설치
npm install

# 백엔드 + 프론트엔드 동시 실행
npm run dev
```

### 3. 백엔드 개별 실행
```bash
cd backend
./gradlew bootRun
```

### 4. 프론트엔드 개별 실행
```bash
cd frontend
npm install
npm run dev
```

### 5. 빌드 및 테스트 검증
```bash
# 백엔드 빌드
cd backend
./gradlew build

# 프론트엔드 빌드
cd frontend
npm run build
```

