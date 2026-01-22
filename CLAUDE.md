# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## 빌드 및 실행 명령어

```bash
# 프로젝트 빌드
./gradlew build

# 테스트 실행
./gradlew test

# 단일 테스트 클래스 실행
./gradlew test --tests "io.hansu.pacer.service.WebhookServiceTest"

# 로컬 개발 실행 (localhost:3306에 MySQL 8.4 필요)
./gradlew bootRun --args='--spring.profiles.active=local'

# Docker 실행 (PostgreSQL + 앱)
docker-compose up --build

# 실행 가능한 JAR 생성
./gradlew bootJar
```

## 아키텍처 개요

Running Pacer는 Strava 러닝 데이터와 AI 에이전트를 Model Context Protocol (MCP)을 통해 연결하는 Spring Boot 4.0.1 (Kotlin) 애플리케이션입니다.

### 핵심 데이터 흐름

1. **Strava OAuth** → 사용자 인증 → `strava_tokens` 테이블에 토큰 저장
2. **Webhook 이벤트** → Strava가 `/webhook/strava`로 POST → `ingest_jobs` 항목 생성
3. **Job 처리** → `IngestJobScheduler`가 30초마다 실행 → Strava API에서 활동 상세 정보 조회 → `activities`, `laps`, `activity_streams` 테이블에 저장
4. **MCP 접근** → AI 에이전트가 SSE (`/sse`)로 연결 → `running_query`, `running_insight` 도구로 데이터 조회

### 주요 서비스

- `StravaAuthService` - 토큰 교환 및 갱신
- `StravaIngestService` - Strava 데이터 파싱 및 저장
- `RunningQueryService` - 데이터 조회 (활동, 요약)
- `RunningAnalysisService` - 성능 분석 (비교, 추세, 이상치)
- `CoachingService` - VDOT/TSS 기반 훈련 추천
- `IngestJobScheduler` - 백그라운드 작업 처리기

### MCP 통합

AI 에이전트에 노출되는 두 가지 주요 도구:
- `running_query` - 데이터 조회 (activities, summary, anomalies)
- `running_insight` - 분석 (coaching, compare, trend, set_goal, performance)

사용자 컨텍스트 추출: `McpUserContext.kt`에서 JWT 클레임 또는 API 토큰으로 사용자 ID 추출

### 데이터베이스

- 운영: PostgreSQL 16
- 로컬 개발: MySQL 8.4
- 마이그레이션: Flyway (`src/main/resources/db/migration/`)

### 인증 방식

- **웹 로그인**: Google OAuth2 (웹 대시보드용)
- **MCP 접근**: API 토큰 또는 OAuth 2.1 인가 서버의 JWT

## 환경 변수

필수:
- `STRAVA_CLIENT_ID`, `STRAVA_CLIENT_SECRET`, `STRAVA_WEBHOOK_VERIFY_TOKEN`
- `OAUTH_RSA_PUBLIC_KEY`, `OAUTH_RSA_PRIVATE_KEY` (PEM 형식, base64 인코딩)

선택:
- `GOOGLE_CLIENT_ID`, `GOOGLE_CLIENT_SECRET` (웹 로그인용)
- `DATABASE_URL`, `DB_USERNAME`, `DB_PASSWORD`

## 알려진 이슈

- `IngestJobSchedulerTest`의 일부 테스트 실패
- MCP 사용자 컨텍스트가 JWT 추출 실패 시 첫 번째 사용자로 폴백 (임시 처리)
