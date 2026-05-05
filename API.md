# AI Context Sync — Backend API 문서

> Spring Boot 3.3 / Java 21 / PostgreSQL 16  
> 기준일: 2026-05-05

---

## 목차

1. [프로젝트 구조](#프로젝트-구조)
2. [기술 스택](#기술-스택)
3. [인증 방식](#인증-방식)
4. [공통 응답 형식](#공통-응답-형식)
5. [API 목록](#api-목록)
   - [Auth](#auth)
   - [Projects](#projects)
   - [Context](#context)
6. [플랜 제한 정책](#플랜-제한-정책)
7. [암호화](#암호화)
8. [에러 코드](#에러-코드)
9. [로컬 실행 방법](#로컬-실행-방법)

---

## 프로젝트 구조

```
src/main/java/org/example/
├── Main.java                      # @SpringBootApplication 진입점
├── config/
│   └── SecurityConfig.java        # Spring Security + JWT 필터 설정
├── controller/
│   ├── AuthController.java        # /api/auth/**
│   ├── ProjectController.java     # /api/projects/**
│   └── ContextController.java     # /api/projects/{id}/context/**
├── service/
│   ├── AuthService.java           # 회원가입/로그인/토큰 재발급
│   ├── ProjectService.java        # 프로젝트 CRUD + 플랜 제한
│   └── ContextService.java        # 컨텍스트 저장/조회/버전 관리
├── entity/
│   ├── User.java                  # users 테이블
│   ├── Project.java               # projects 테이블
│   ├── Context.java               # contexts 테이블 (암호화 저장)
│   ├── ProjectMember.java         # project_members 테이블
│   ├── UserPlan.java              # enum: FREE / PRO / TEAM
│   └── ProjectRole.java           # enum: OWNER / MEMBER / VIEWER
├── repository/
│   ├── UserRepository.java
│   ├── ProjectRepository.java
│   ├── ContextRepository.java
│   └── ProjectMemberRepository.java
├── security/
│   ├── JwtTokenProvider.java      # JWT 생성/검증 (HMAC-SHA384)
│   ├── JwtAuthenticationFilter.java  # 요청마다 토큰 파싱 → SecurityContext 주입
│   └── UserDetailsServiceImpl.java   # Spring Security UserDetails 어댑터
├── dto/
│   ├── auth/   (SignupRequest, LoginRequest, AuthResponse)
│   ├── project/ (ProjectRequest, ProjectResponse)
│   └── context/ (ContextRequest, ContextResponse)
├── exception/
│   ├── ErrorCode.java             # 에러 코드 enum
│   ├── ApiException.java          # 비즈니스 예외
│   └── GlobalExceptionHandler.java # @RestControllerAdvice
└── util/
    └── EncryptionUtil.java        # AES-256-GCM 암호화/복호화
```

---

## 기술 스택

| 항목 | 버전 |
|---|---|
| Java | 21 (OpenJDK) |
| Spring Boot | 3.3.5 |
| Spring Security | 6.x |
| Spring Data JPA | 3.3.x |
| Hibernate | 6.5.x |
| PostgreSQL Driver | 42.x |
| jjwt | 0.12.6 |
| Lombok | (Spring Boot 관리) |
| Gradle | 9.3 |

---

## 인증 방식

**JWT Bearer Token** (HMAC-SHA384)

모든 `/api/auth/**` 이외의 엔드포인트는 `Authorization` 헤더 필수.

```
Authorization: Bearer <accessToken>
```

| 토큰 종류 | 유효 시간 |
|---|---|
| Access Token | 24시간 |
| Refresh Token | 7일 |

토큰 재발급은 `/api/auth/refresh`에 `refreshToken`을 body로 전달.

---

## 공통 응답 형식

**성공**: 각 엔드포인트별 응답 body 반환 (아래 API 목록 참조)

**에러**:
```json
{
  "code": "ERROR_CODE_NAME",
  "message": "에러 설명",
  "status": 400
}
```

---

## API 목록

### Auth

#### POST `/api/auth/signup` — 회원가입

인증 불필요.

**Request**
```json
{
  "email": "user@example.com",
  "password": "password123"
}
```

| 필드 | 타입 | 제약 |
|---|---|---|
| email | string | 이메일 형식, 필수 |
| password | string | 최소 8자, 필수 |

**Response** `201 Created`
```json
{
  "accessToken": "eyJhbGci...",
  "refreshToken": "eyJhbGci...",
  "tokenType": "Bearer"
}
```

---

#### POST `/api/auth/login` — 로그인

인증 불필요.

**Request**
```json
{
  "email": "user@example.com",
  "password": "password123"
}
```

**Response** `200 OK`
```json
{
  "accessToken": "eyJhbGci...",
  "refreshToken": "eyJhbGci...",
  "tokenType": "Bearer"
}
```

---

#### POST `/api/auth/refresh` — 토큰 재발급

인증 불필요.

**Request**
```json
{
  "refreshToken": "eyJhbGci..."
}
```

**Response** `200 OK`
```json
{
  "accessToken": "eyJhbGci...",
  "refreshToken": "eyJhbGci...",
  "tokenType": "Bearer"
}
```

---

### Projects

#### GET `/api/projects` — 내 프로젝트 목록

**Response** `200 OK`
```json
[
  {
    "id": 1,
    "name": "My App",
    "description": "설명",
    "createdAt": "2026-05-05T10:00:00"
  }
]
```

생성일 내림차순 정렬.

---

#### POST `/api/projects` — 프로젝트 생성

**Request**
```json
{
  "name": "My App",
  "description": "선택사항"
}
```

| 필드 | 타입 | 제약 |
|---|---|---|
| name | string | 필수 |
| description | string | 선택 |

**Response** `201 Created`
```json
{
  "id": 1,
  "name": "My App",
  "description": "선택사항",
  "createdAt": "2026-05-05T10:00:00"
}
```

> **Free 플랜**: 프로젝트 3개 초과 시 `403 FREE_PLAN_LIMIT_EXCEEDED`

---

#### DELETE `/api/projects/{id}` — 프로젝트 삭제

본인 프로젝트만 삭제 가능.

**Response** `204 No Content`

---

### Context

#### POST `/api/projects/{projectId}/context` — 컨텍스트 저장

**Request**
```json
{
  "content": "오늘 세션 요약...",
  "source": "claude.ai",
  "label": "auth 모듈 완료"
}
```

| 필드 | 타입 | 제약 |
|---|---|---|
| content | string | 필수. AES-256-GCM 암호화되어 DB 저장 |
| source | string | 선택. 어느 AI 툴에서 저장했는지 (`claude.ai`, `chatgpt` 등) |
| label | string | 선택. 버전 설명 |

저장 시 `version`이 해당 프로젝트 내에서 자동 증가 (max+1).

**Response** `201 Created`
```json
{
  "id": 1,
  "projectName": "My App",
  "content": "오늘 세션 요약...",
  "source": "claude.ai",
  "label": "auth 모듈 완료",
  "savedAt": "2026-05-05T10:00:00",
  "version": 1
}
```

---

#### GET `/api/projects/{projectId}/context/latest` — 최신 컨텍스트 조회

Chrome Extension / MCP 서버가 대화 시작 시 호출하는 핵심 엔드포인트.

**Response** `200 OK`
```json
{
  "id": 5,
  "projectName": "My App",
  "content": "복호화된 컨텍스트 내용",
  "source": "claude.ai",
  "label": "auth 모듈 완료",
  "savedAt": "2026-05-05T10:00:00",
  "version": 5
}
```

---

#### GET `/api/projects/{projectId}/context/history` — 컨텍스트 버전 히스토리

**Response** `200 OK`
```json
[
  {
    "id": 5,
    "projectName": "My App",
    "content": "...",
    "source": "claude.ai",
    "label": "auth 완료",
    "savedAt": "2026-05-05T10:00:00",
    "version": 5
  },
  {
    "id": 4,
    ...
  }
]
```

버전 내림차순 정렬.

> **Free 플랜**: 최근 7일 이내 데이터만 반환.  
> **Pro / Team 플랜**: 전체 히스토리 반환.

---

#### DELETE `/api/projects/{projectId}/context/{contextId}` — 특정 버전 삭제

**Response** `204 No Content`

---

## 플랜 제한 정책

| 정책 | FREE | PRO | TEAM |
|---|---|---|---|
| 프로젝트 수 | 최대 3개 | 무제한 | 무제한 |
| 컨텍스트 히스토리 | 최근 7일 | 전체 | 전체 |
| 팀 공유 | X | X | O |

플랜 업그레이드는 현재 미구현 (DB `users.plan` 컬럼을 직접 수정하거나 추후 결제 API 연동).

---

## 암호화

저장되는 `contexts.content`는 **AES-256-GCM** 으로 암호화.

- 알고리즘: `AES/GCM/NoPadding`
- 키 크기: 256bit (설정값을 SHA-256 해시하여 32바이트 키 도출)
- IV: 저장마다 랜덤 12바이트 생성
- DB 저장 형식: `Base64(IV 12바이트 + 암호문 + GCM 태그)`
- API 응답 시 자동 복호화하여 평문 반환

암호화 키는 `application.yml`의 `app.encryption.key`에서 관리.  
**운영 환경에서는 AWS KMS로 교체 예정.**

---

## 에러 코드

| code | HTTP | 설명 |
|---|---|---|
| `EMAIL_ALREADY_EXISTS` | 409 | 이미 사용 중인 이메일 |
| `INVALID_CREDENTIALS` | 401 | 이메일 또는 비밀번호 불일치 |
| `UNAUTHORIZED` | 401 | 토큰 없음 또는 만료 |
| `USER_NOT_FOUND` | 404 | 사용자를 찾을 수 없음 |
| `PROJECT_NOT_FOUND` | 404 | 프로젝트 없음 또는 접근 권한 없음 |
| `CONTEXT_NOT_FOUND` | 404 | 컨텍스트 없음 |
| `FREE_PLAN_LIMIT_EXCEEDED` | 403 | Free 플랜 프로젝트 3개 초과 |
| `PAID_PLAN_REQUIRED` | 403 | 유료 플랜 전용 기능 |
| `FORBIDDEN` | 403 | 접근 권한 없음 |
| `VALIDATION_ERROR` | 400 | 요청 파라미터 유효성 실패 |

---

## 로컬 실행 방법

### 사전 요구사항

- Java 21
- Docker

### 1. DB 실행

```bash
cd ~/ai-context-sync/docker
docker-compose up -d
```

PostgreSQL 16이 `localhost:5432`에 뜸 (database: `aicontext`, user: `dev`, password: `devpassword`).

### 2. 앱 실행

```bash
cd ~/ai-context-sync/ai-context-sync
./gradlew bootRun
```

`http://localhost:8080` 에서 서빙.

### 3. 동작 확인

```bash
# 회원가입
curl -X POST http://localhost:8080/api/auth/signup \
  -H "Content-Type: application/json" \
  -d '{"email":"me@test.com","password":"password123"}'

# 프로젝트 생성 (토큰 교체 필요)
curl -X POST http://localhost:8080/api/projects \
  -H "Authorization: Bearer <accessToken>" \
  -H "Content-Type: application/json" \
  -d '{"name":"My Project"}'

# 컨텍스트 저장
curl -X POST http://localhost:8080/api/projects/1/context \
  -H "Authorization: Bearer <accessToken>" \
  -H "Content-Type: application/json" \
  -d '{"content":"오늘 구현한 내용...","source":"claude.ai"}'

# 최신 컨텍스트 조회
curl http://localhost:8080/api/projects/1/context/latest \
  -H "Authorization: Bearer <accessToken>"
```

---

## 미구현 / 다음 단계

| 항목 | 상태 |
|---|---|
| `POST /api/projects/{id}/members` (팀 초대) | 미구현 |
| `POST /api/projects/{id}/context/upload` (파일 업로드 + AI 요약) | 미구현 |
| 플랜 업그레이드 / 결제 연동 | 미구현 |
| Chrome Extension | 미구현 |
| MCP Server | 미구현 |
| Web UI | 미구현 |
| AWS KMS 암호화 키 관리 | Phase 2 |
| S3 파일 저장 | Phase 2 |
