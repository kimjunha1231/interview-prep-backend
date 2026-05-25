# 🚀 배포 가이드 (Deployment Guide)

본 문서는 **Interview Handbook** 플랫폼의 백엔드(Spring Boot)와 프론트엔드(React + Nginx)를 실제 상용 환경에 배포하는 방법을 기술합니다. 

인프라 예산 및 관리 편의성에 따라 **두 가지 대안** 중 하나를 선택할 수 있습니다.

---

## 🏛️ [대안 1] Docker Compose 기반 단일 가상 서버 배포 (EC2, Lightsail 등)
> 하나의 Linux 가상 서버(예: AWS EC2 t3.micro 등)에 Docker를 설치하고, 프론트엔드와 백엔드를 컨테이너화하여 동시에 띄우는 가장 범용적인 방식입니다. (Supabase RDB를 외부 RDB로 사용하므로 가상 서버 내부 DB는 생략)

### 1단계: 배포 서버 환경 설정
1. Linux 서버에 접속한 후 Docker와 Docker Compose를 설치합니다.
2. 프로젝트 루트 폴더를 생성하고, `docker-compose.yml` 및 `.env` 파일을 서버에 업로드합니다.

### 2단계: `.env` 파일 작성 (서버용)
서버에 `.env` 파일을 생성하고 아래 양식에 맞추어 실제 값을 입력합니다. (도메인이 아직 결정되지 않았다면 임시로 서버 IP를 기입하세요.)

```bash
# 1. 외부 DB (Supabase 서울 리전 Connection Pooler 사용)
DB_HOST=aws-1-ap-northeast-2.pooler.supabase.com
DB_PORT=5432
DB_NAME=postgres
DB_USER=postgres.your_project_id
DB_PASSWORD=your_supabase_password

# 2. 이메일 SMTP 연동 (Gmail 등)
MAIL_USERNAME=your_gmail@gmail.com
MAIL_PASSWORD=your_gmail_app_password

# 3. AI 및 외부 API
GEMINI_API_KEY=your_gemini_api_key
GROQ_API_KEY=your_groq_api_key

# 4. 서비스 도메인 정보 (도메인 미결정 시 http://서버IP 형태로 기입)
FRONTEND_URL=https://your-domain.com
BACKEND_URL=https://api.your-domain.com
```

### 3단계: `docker-compose.yml` 정리
Supabase 클라우드 RDB를 사용하므로 로컬 `postgres` DB 컨테이너는 필요하지 않습니다. 주석 처리를 완료한 최종 배포용 `docker-compose.yml` 규격입니다.

```yaml
version: '3.8'

services:
  backend:
    build:
      context: ./backend
      dockerfile: Dockerfile
    container_name: interview-prep-backend
    ports:
      - '8080:8080'
    environment:
      - DB_HOST=${DB_HOST}
      - DB_PORT=${DB_PORT}
      - DB_NAME=${DB_NAME}
      - DB_USER=${DB_USER}
      - DB_PASSWORD=${DB_PASSWORD}
      - GEMINI_API_KEY=${GEMINI_API_KEY}
      - GROQ_API_KEY=${GROQ_API_KEY}
      - MAIL_USERNAME=${MAIL_USERNAME}
      - MAIL_PASSWORD=${MAIL_PASSWORD}
      - FRONTEND_URL=${FRONTEND_URL}
      - BACKEND_URL=${BACKEND_URL}
    restart: always
    networks:
      - interview-network

  frontend:
    build:
      context: ./frontend
      dockerfile: Dockerfile
    container_name: interview-prep-frontend
    ports:
      - '80:80'
    depends_on:
      - backend
    restart: always
    networks:
      - interview-network

networks:
  interview-network:
    driver: bridge
```

### 4단계: 컨테이너 빌드 및 백그라운드 구동
서버 터미널에서 다음 명령어를 실행하여 이미지 빌드 및 배포를 완료합니다.
```bash
# 백그라운드로 빌드 및 컨테이너 기동
docker compose up -d --build
```

---

## ☁️ [대안 2] Vercel + PaaS 클라우드 하이브리드 배포 (추천: 비용 $0/month)
> 프론트엔드를 Vercel에 올려 글로벌 초고속 CDN 혜택을 받고 백엔드는 Render.com 또는 Railway.app 등의 PaaS(Platform as a Service) 컨테이너 구동망에 업로드하는 구조입니다.
> 서버 장비의 CPU/메모리 부하 및 Nginx SSL 인증서 갱신 오버헤드가 완전히 면제되어 비용과 관리 오버헤드를 모두 $0로 낮출 수 있습니다.

### 💻 프론트엔드 배포 (Vercel)
1. **GitHub 연동**: GitHub에 `frontend/` 폴더 소스 코드를 푸시합니다.
2. **Vercel 가입 및 프로젝트 생성**: [Vercel](https://vercel.com)에 로그인 후 `Import Project`로 GitHub 레포지토리를 연동합니다.
3. **빌드 설정 지정**:
   * Root Directory: `frontend`
   * Framework Preset: `Vite`
   * Build Command: `npm run build`
   * Output Directory: `dist`
4. **Proxy 우회 핫패치**:
   * 리액트는 배포 환경(Vercel)에서 상대 경로 `/api`를 처리할 수 없으므로 Vercel 서버의 API 라우팅 설정이 필요합니다.
   * `frontend/vercel.json` 파일을 아래 내용으로 신규 생성하여 리포지토리에 커밋하면 백엔드 주소로 API 요청이 즉각 프록시 라우팅됩니다.

#### [NEW] `frontend/vercel.json`
```json
{
  "rewrites": [
    {
      "source": "/api/:path*",
      "destination": "https://your-backend-url.com/api/:path*"
    }
  ]
}
```

### ⚙️ 백엔드 배포 (Render.com 또는 Railway.app)
1. PaaS 서비스에 가입한 후 GitHub 백엔드 소스를 가져옵니다.
2. Root Directory를 `backend`로 지정합니다.
3. Build Command로 `./gradlew bootJar`를 지정하고, Start Command로 `java -jar build/libs/*.jar`를 입력합니다.
4. PaaS 콘솔의 **Environment Variables** 탭에 로컬 `.env`에 등록해 둔 키-값 변수들을 동일하게 기입합니다.
   * 필수: `SPRING_PROFILES_ACTIVE=prod`를 기입하여 Swagger 비활성화 및 DDL none 설정을 작동시킵니다.
5. 배포 완료 시 제공되는 URL(예: `https://interview-api.onrender.com`)을 복사하여 Vercel `vercel.json`의 `destination` 자리에 매핑합니다.

---

## 🔒 운영 배포 직후 검증 사항
1. **API 통신 확인**: 개념 학습 질문 탭이 잘 로드되는지 확인 (TanStack Query 및 CORS가 잘 작동하는지 검증).
2. **음성 인식 테스트**: 마이크 녹음 및 Groq STT 2차 정제 텍스트가 정상 추출되는지 확인.
3. **이메일 배달 및 해제 링크**: 면접 완료 보고서 수신 후 이메일 내 "정답 및 상세 해설 보기" 버튼 클릭 시 운영 도메인의 Handbook 상세화면으로 포커스 전환되는지 검사.
