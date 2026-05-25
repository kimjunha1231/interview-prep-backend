#!/usr/bin/env python3
"""
questions.json category/subject 정규화 스크립트
- 목표 taxonomy:
    CS       → algorithm | data_structure | database | network | os | computer_architecture | design_pattern | software_engineering
    Frontend → javascript | html_css | react | typescript | nextjs | web_performance
    Backend  → java | spring | system_design | devops
"""

import json
import sys

# ────────────────────────────────────────────────────────────────────
# 항목별 수동 분류 맵 (id → (새 category, 새 subject))
# ────────────────────────────────────────────────────────────────────
REMAP = {
    # ── CS / cs 54개 ────────────────────────────────────────────────
    # Spring/Java 관련
    "cs_cs_150": ("Backend", "spring"),       # @ExceptionHandler
    "cs_cs_156": ("Backend", "spring"),       # RequestBody vs ModelAttribute
    "cs_cs_158": ("Backend", "spring"),       # AutoConfiguration
    "cs_cs_164": ("Backend", "spring"),       # EntityManager
    "cs_cs_193": ("Backend", "spring"),       # Record를 DTO로 사용
    "cs_cs_232": ("Backend", "spring"),       # 의존성 주입(DI)
    "cs_cs_236": ("Backend", "spring"),       # Graceful Shutdown
    "cs_cs_248": ("Backend", "spring"),       # Micrometer
    "cs_cs_253": ("Backend", "spring"),       # @OneToOne Lazy Loading
    "cs_cs_257": ("Backend", "spring"),       # Keep-Alive와 컴포넌트 캐싱 (Spring 문맥)
    "cs_cs_294": ("Backend", "spring"),       # 헬스체크

    # Java 관련
    "cs_cs_170": ("Backend", "java"),         # 얕은 복사와 깊은 복사
    "cs_cs_213": ("Backend", "java"),         # 방어적 복사(Defensive Copy)
    "cs_cs_249": ("Backend", "java"),         # try-with-resources
    "cs_cs_270": ("Backend", "java"),         # 객체 지향 프로그래밍의 4대 특징 (Java 문맥)

    # Database 관련
    "cs_cs_201": ("CS", "database"),          # ACID
    "cs_cs_278": ("CS", "database"),          # 물리 삭제 vs 논리 삭제
    "cs_cs_284": ("CS", "database"),          # NOT IN 쿼리 문제점
    "cs_cs_285": ("CS", "database"),          # Statement vs PreparedStatement

    # Network 관련
    "cs_cs_197": ("CS", "network"),           # 로드 밸런싱
    "cs_cs_216": ("CS", "network"),           # URI, URL, URN
    "cs_cs_375": ("CS", "network"),           # 처리량(Throughput)
    "cs_cs_379": ("CS", "network"),           # 캡슐화/비캡슐화
    "cs_cs_381": ("CS", "network"),           # 로드밸런서
    "cs_cs_382": ("CS", "network"),           # ARP
    "cs_cs_383": ("CS", "network"),           # MAC 주소
    "cs_cs_391": ("CS", "network"),           # 암호화 키 종류
    "cs_cs_393": ("CS", "network"),           # SSE(Server-Sent Events)
    "cs_cs_394": ("CS", "network"),           # SSE 메시지 순서 보장
    "cs_cs_395": ("Frontend", "javascript"),  # EventSource vs Fetch+ReadableStream (클라이언트 API)

    # OS 관련
    "cs_cs_218": ("CS", "os"),                # 시스템 콜(System Call)
    "cs_cs_259": ("CS", "os"),                # 멀티태스킹 시스템의 한계와 병목

    # System Design / Architecture
    "cs_cs_191": ("Backend", "system_design"),# WAS vs 웹 서버
    "cs_cs_199": ("Backend", "system_design"),# 캐싱 전략
    "cs_cs_202": ("Backend", "system_design"),# 스케일 아웃 vs 스케일 업
    "cs_cs_206": ("Backend", "system_design"),# 캐시 스탬피드
    "cs_cs_207": ("Backend", "system_design"),# 시스템 간 비동기 연동
    "cs_cs_208": ("Backend", "system_design"),# CAP 정리
    "cs_cs_215": ("CS", "os"),                # 디스크 접근 시간
    "cs_cs_266": ("CS", "network"),           # PRG 패턴 (Post-Redirect-Get, HTTP 패턴)

    # Web Performance (Frontend 관련)
    "cs_cs_423": ("Frontend", "web_performance"),  # Core Web Vitals
    "cs_cs_424": ("Frontend", "web_performance"),  # LCP
    "cs_cs_425": ("Frontend", "web_performance"),  # FCP
    "cs_cs_427": ("Frontend", "web_performance"),  # INP
    "cs_cs_428": ("Frontend", "web_performance"),  # TTFB

    # Frontend Build Tools
    "cs_cs_436": ("Frontend", "javascript"),  # 웹팩(Webpack)
    "cs_cs_438": ("Frontend", "javascript"),  # 모듈 번들링
    "cs_cs_439": ("Frontend", "javascript"),  # 바벨(Babel)

    # TypeScript
    "cs_cs_441": ("Frontend", "typescript"),  # 타입과 인터페이스의 차이

    # 함수형 프로그래밍 → CS software_engineering
    "cs_cs_222": ("CS", "software_engineering"),   # 함수형 프로그래밍

    # 잘못된/비어 있는 항목
    "cs_cs_283": None,  # "Generate the representative question" - 삭제 대상 (의미없는 항목)
    "cs_cs_432": None,  # "cs_cs_432" - 삭제 대상 (제목이 ID와 동일, 비어 있는 항목)

    # 동기/비동기 → Backend(system_design) 또는 CS?
    "cs_cs_176": ("Backend", "system_design"),  # 동기 외부 서비스 호출 장애 대응
    "cs_cs_177": ("CS", "os"),                  # 동기와 비동기 프로그래밍 (OS/CS 기본 개념)

    # ── CS / 기타 named subjects ────────────────────────────────────
    "cs_cs_195": ("CS", "software_engineering"),  # SOLID 원칙
    "cs_cs_209": ("CS", "software_engineering"),  # 응집도와 결합도
    "cs_cs_220": ("CS", "network"),               # 대칭키 및 비대칭키 (보안/암호화 → 네트워크 범주)
    "cs_cs_240": ("CS", "software_engineering"),  # 테스트 더블
    "cs_cs_244": ("CS", "os"),                    # 경쟁 상태, 동시성 제어
    "cs_cs_261": ("CS", "os"),                    # 가상화(Virtualization)
    "cs_cs_265": ("CS", "os"),                    # 멀티 쓰레딩
    "cs_cs_287": ("CS", "computer_architecture"), # 참조 지역성
    "cs_cs_289": ("CS", "computer_architecture"), # 명령어 파이프라이닝
    "cs_cs_374": ("CS", "network"),               # 지연시간(Latency)

    "cs_cs_416": ("CS", "software_engineering"),  # SDLC
    "cs_cs_417": ("CS", "software_engineering"),  # 폭포수 방법론
    "cs_cs_418": ("CS", "software_engineering"),  # 애자일 방법론
    "cs_cs_419": ("CS", "computer_architecture"), # 캐시의 개념과 활용 (컴퓨터 구조 수준 캐시)

    # CS / Computer Architecture
    "cs_cs_401": ("CS", "computer_architecture"), # 컴퓨터 5대 구성 요소
    "cs_cs_402": ("CS", "computer_architecture"), # CPU 내부 구성
    "cs_cs_403": ("CS", "computer_architecture"), # CPU 동작 원리

    # ── Architecture / Backend Architecture / System Design ─────────
    "cs_cs_271": ("CS", "design_pattern"),         # 널 오브젝트 패턴
    "cs_cs_272": ("Backend", "system_design"),     # 서버리스 아키텍처
    "cs_cs_274": ("Backend", "system_design"),     # Gradle (빌드 도구 → Backend)
    "cs_cs_279": ("CS", "design_pattern"),         # 템플릿 메서드 패턴
    "cs_cs_286": ("Backend", "system_design"),     # 이벤트 소싱(Event Sourcing)
    "cs_cs_292": ("CS", "design_pattern"),         # 싱글턴 패턴
    "cs_cs_293": ("Backend", "system_design"),     # 레이어드 아키텍처

    # ── Design Pattern ───────────────────────────────────────────────
    "cs_cs_230": ("CS", "design_pattern"),         # 전략 패턴

    # ── DevOps ───────────────────────────────────────────────────────
    "cs_cs_234": ("Backend", "devops"),            # CI/CD 파이프라인
    "cs_cs_239": ("Backend", "devops"),            # 무중단 배포
    "cs_cs_252": ("Backend", "devops"),            # Infrastructure as Code(IaC)

    # ── Database (category가 Database인 경우) ────────────────────────
    "cs_cs_178": ("CS", "database"),              # 공유 락 & 배타 락
    "cs_cs_211": ("CS", "database"),              # 교착 상태(Deadlock)
    "cs_cs_238": ("CS", "database"),              # 분산 락(Redis)
    "cs_cs_263": ("CS", "database"),              # 낙관적 락 vs 비관적 락
    "cs_cs_282": ("CS", "database"),              # SQL 인젝션
    "cs_database_187": ("Backend", "spring"),     # @Transactional private 메서드 (Spring AOP)

    # ── Backend / Java (대소문자) ────────────────────────────────────
    "be_java_173": ("Backend", "java"),            # equals and hashCode

    # ── Frontend 잘못된 subject ──────────────────────────────────────
    "fe_javascript_101": ("Frontend", "typescript"),   # never와 unknown 타입
    "fe_javascript_322": ("Frontend", "javascript"),   # 함수 선언문 vs 함수 표현식
    "fe_javascript_329": ("Frontend", "javascript"),   # 생성자 함수 객체 인스턴스 생성

    # ── Front-end / Web Storage ──────────────────────────────────────
    "cs_network_042": ("Frontend", "javascript"),      # localStorage vs sessionStorage (Web API)

    # ── CS / concurrency (소문자) ────────────────────────────────────
    "cs_os_291": ("CS", "os"),                        # 코루틴 vs 스레드

    # ── Frontend / React ────────────────────────────────────────────
    "fe_react_113": ("Frontend", "react"),             # Storybook → react 도구

    # ── Frontend / web-rendering ─────────────────────────────────────
    "fe_react_189": ("Frontend", "react"),             # SSR vs CSR → react 범주

    # ── Network / * ──────────────────────────────────────────────────
    "cs_cs_384": ("CS", "network"),                    # NAT
    "cs_cs_186": ("CS", "network"),                    # 리버스/포워드 프록시

    # ── Programming Paradigm / Generic ───────────────────────────────
    "cs_cs_442": ("Backend", "java"),                  # 제네릭(Generics) → Java 문맥

    # ── QA / Software Engineering / Testing 계열 ─────────────────────
    "cs_cs_179": ("CS", "software_engineering"),       # 단위 테스트 vs 통합 테스트
    "cs_cs_219": ("CS", "software_engineering"),       # TDD
    "cs_cs_233": ("CS", "software_engineering"),       # 코드 커버리지
    "cs_cs_281": ("CS", "software_engineering"),       # 테스트 격리
    "cs_cs_429": ("CS", "software_engineering"),       # 소프트웨어 테스트 정의와 목적
    "cs_cs_430": ("CS", "software_engineering"),       # 소프트웨어 테스트 종류와 특징
    "cs_cs_431": ("CS", "software_engineering"),       # 소프트웨어 테스트 목적과 가치
    "cs_cs_433": ("CS", "software_engineering"),       # 통합 테스트(Integration Testing)
    "cs_cs_434": ("CS", "software_engineering"),       # E2E 테스트
    "cs_cs_435": ("CS", "software_engineering"),       # 테스트 피라미드

    # ── System Design / Architecture ──────────────────────────────────
    "cs_cs_228": ("Backend", "system_design"),         # 단일 장애 지점(SPOF)

    # ── System Monitoring / Observability ──────────────────────────────
    "cs_cs_171": ("Backend", "system_design"),         # 로그와 메트릭 (모니터링)

    # ── Web Performance / Core Web Vitals ─────────────────────────────
    "cs_cs_426": ("Frontend", "web_performance"),      # CLS

    # ── Computer Science / Software Engineering ───────────────────────
    "cs_cs_437": ("CS", "software_engineering"),       # 모듈 설계 원칙

    # ── Concurrency & Parallelism ─────────────────────────────────────
    "cs_cs_198": ("CS", "os"),                         # Concurrency & Parallelism → os
}

# ────────────────────────────────────────────────────────────────────
# 대상 파일 목록 (동일한 정규화를 두 파일 모두에 적용)
# ────────────────────────────────────────────────────────────────────
TARGET_FILES = [
    "/Users/junha/coding/interview-prep/backend/src/main/resources/questions.json",
    "/Users/junha/coding/Crawling/questions.json",
]

delete_ids = {k for k, v in REMAP.items() if v is None}

for filepath in TARGET_FILES:
    print(f"\n{'='*60}")
    print(f"처리 중: {filepath}")
    print('='*60)

    with open(filepath, "r", encoding="utf-8") as f:
        data = json.load(f)

    remapped = 0
    deleted  = 0
    skipped  = 0

    result = []
    for item in data:
        id_ = item["id"]
        if id_ in delete_ids:
            deleted += 1
            print(f"[DELETE] {id_}: {item['title']}")
            continue
        if id_ in REMAP:
            new_cat, new_sub = REMAP[id_]
            old = f"{item['category']} / {item['subject']}"
            item["category"] = new_cat
            item["subject"]  = new_sub
            print(f"[REMAP]  {id_}: {old} → {new_cat} / {new_sub}")
            remapped += 1
        else:
            skipped += 1
        result.append(item)

    with open(filepath, "w", encoding="utf-8") as f:
        json.dump(result, f, ensure_ascii=False, indent=2)

    print(f"\n✅ 완료: remapped={remapped}, deleted={deleted}, skipped(정상)={skipped}")
    print(f"   총 항목 수: {len(result)}")

print("\n🎉 모든 파일 정규화 완료!")
