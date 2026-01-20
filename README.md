# Running Pacer

[![Build Status](https://img.shields.io/badge/build-passing-brightgreen)](https://github.com/hansu/running-pacer)
[![Spring Boot](https://img.shields.io/badge/Spring%20Boot-4.0.1-brightgreen)](https://spring.io/projects/spring-boot)
[![Kotlin](https://img.shields.io/badge/Kotlin-1.9.22-purple)](https://kotlinlang.org/)
[![License](https://img.shields.io/badge/license-MIT-blue.svg)](LICENSE)

Running Pacer는 Strava API와 연동된 러닝 활동 데이터 분석 서비스입니다. Model Context Protocol (MCP) 서버를 통해 러닝 성능, 추세 및 이상 징후에 대한 구조화된 통찰력을 제공하며, AI 에이전트가 사용자의 러닝 데이터와 상호작용할 수 있도록 지원합니다.

---

## 목차

- [프로젝트 개요](#프로젝트-개요)
- [기술 스택](#기술-스택)
- [사전 요구 사항](#사전-요구-사항)
- [설치 및 설정](#설치-및-설정)
- [실행 방법](#실행-방법)
  - [Docker (권장)](#docker-권장)
  - [로컬 실행](#로컬-실행)
- [빌드 및 테스트](#빌드-및-테스트)
- [설정](#설정)
- [Strava 연동](#strava-연동)
- [유용한 API](#유용한-api)
- [MCP 서버 사용법](#mcp-서버-사용법)
- [프로젝트 구조](#프로젝트-구조)
- [라이선스](#라이선스)

---

## 프로젝트 개요

Running Pacer는 사용자의 Strava 러닝 데이터와 AI 기반 분석 도구 사이의 가교 역할을 합니다. 웹훅(webhook)을 통해 자동으로 활동 데이터를 수집하고, 이를 구조화된 형식으로 처리하여 MCP 서버를 통해 외부에 노출합니다.

**데이터 흐름:**
1. **웹훅 수신**: Strava 활동 발생 시 실시간 알림 수신 및 DB 저장.
2. **백그라운드 작업**: 30초 주기로 스케줄러가 Pending 상태의 작업을 가져와 Strava API에서 상세 데이터(Activity, Laps, Streams) 조회.
3. **데이터 처리**: 활동 정보를 Upsert하고 주간/일간 통계(`daily_stats`)를 증분 집계.
4. **인사이트 제공**: VDOT, TSS, 트렌드 분석 등을 통해 MCP 도구로 AI 에이전트에게 데이터 노출.

**주요 기능:**
- **Strava 연동**: 실시간 활동 업데이트를 위한 OAuth2 흐름 및 웹훅 처리.
- **데이터 분석**: 성능, 주간/월간 추세 및 이상 징후 감지(예: 페이스 대비 높은 심박수)에 대한 구조화된 분석.
- **MCP 서버**: 도구(tools), 리소스(resources) 및 프롬프트(prompts)를 지원하는 내장 MCP 서버를 통해 AI 에이전트가 러닝 데이터를 조회하고 분석할 수 있도록 지원.
- **하이브리드 인증**: AI 에이전트 연동을 위한 전용 API 토큰(`sk-running-`) 및 표준 OAuth 2.1(JWT) 지원.

---

## 기술 스택

- **언어**: Kotlin (JDK 17)
- **프레임워크**: Spring Boot 4.0.1 (Spring Authorization Server 포함)
- **데이터베이스**: PostgreSQL (운영/코드 기준), MySQL 8.4 (로컬 테스트용)
  - *주의: 현재 코드는 PostgreSQL의 `ON CONFLICT` 문법을 사용하고 있어 로컬 개발 시 호환성에 주의가 필요합니다.*
- **마이그레이션**: Flyway
- **빌드 시스템**: Gradle (Kotlin DSL)
- **컨테이너화**: Docker & Docker Compose
- **프로토콜**: SSE 전송 방식을 사용하는 Model Context Protocol (MCP)

---

## 사전 요구 사항

시작하기 전에 다음 항목이 설치되어 있는지 확인하십시오.
- [JDK 17](https://www.oracle.com/java/technologies/downloads/#java17)
- [Docker & Docker Compose](https://docs.docker.com/get-docker/)
- API 자격 증명을 얻기 위한 [Strava 개발자 계정](https://www.strava.com/settings/api).

---

## 설치 및 설정

1. **저장소 클론:**
   ```bash
   git clone https://github.com/hansu/running-pacer.git
   cd running-pacer
   ```

2. **환경 변수 설정:**
   `.env` 파일을 생성하거나 터미널에서 다음 변수를 내보냅니다.
   ```bash
   export STRAVA_CLIENT_ID=your_client_id
   export STRAVA_CLIENT_SECRET=your_client_secret
   export STRAVA_WEBHOOK_VERIFY_TOKEN=your_verify_token
   ```

---

## 실행 방법

### Docker (권장)
Docker Compose를 사용하여 전체 스택(API + MySQL)을 실행하는 가장 쉬운 방법입니다.
```bash
docker-compose up --build
```
- API는 `http://localhost:8080`에서 사용할 수 있습니다.
- MySQL은 `3306` 포트를 통해 접속 가능합니다.

### 로컬 실행
Spring Boot 애플리케이션을 로컬에서 직접 실행하려는 경우:
1. MySQL 8.4 인스턴스가 `localhost:3306`에서 실행 중인지 확인합니다.
2. `local` 프로필을 사용하여 애플리케이션을 실행합니다.
   ```bash
   ./gradlew bootRun --args='--spring.profiles.active=local'
   ```

---

## 빌드 및 테스트

- **프로젝트 빌드:**
  ```bash
  ./gradlew build
  ```
- **테스트 실행:**
  ```bash
  ./gradlew test
  ```
- **알려진 이슈 (Known Issues):**
  - `IngestJobSchedulerTest`의 일부 테스트 실패.
  - 데이터베이스 호환성: 코드가 PostgreSQL `ON CONFLICT`를 사용하여 MySQL 환경에서 에러 발생 가능.
  - 통계 집계: `daily_stats` 집계 시 사용자 시간대가 아닌 UTC 기준을 사용하여 날짜별 오차 발생 가능.
  - 보안: `RegisteredClientInitializer` 내 OAuth 클라이언트 시크릿 하드코딩 및 `SecurityConfig`의 넓은 허용 범위.
  - 상세 내용은 [docs/roadmap.md](docs/roadmap.md)를 참조하십시오.
- **실행 가능한 JAR 생성:**
  ```bash
  ./gradlew bootJar
  ```
  생성된 JAR 파일은 `build/libs/` 디렉토리에 위치합니다.

---

## 설정

이 프로젝트는 환경별 설정을 위해 Spring 프로필을 사용합니다.

| 파일 | 프로필 | 설명 |
|------|---------|-------------|
| `application.yml` | Default | 기본 설정 및 Strava API 엔드포인트. |
| `application-local.yml` | `local` | 로컬 개발용 (`localhost`에 연결). |
| `application-docker.yml` | `docker` | Docker 컨테이너 내부용 (`mysql` 서비스에 연결). |

### 주요 환경 변수
- `STRAVA_CLIENT_ID`: Strava API Client ID.
- `STRAVA_CLIENT_SECRET`: Strava API Client Secret.
- `STRAVA_WEBHOOK_VERIFY_TOKEN`: Strava 웹훅 검증을 위한 비밀 문자열.

---

## Strava 연동

이 애플리케이션은 다음과 같은 Strava 흐름을 구현합니다.
- **OAuth 콜백**: `GET /oauth/strava/callback`에서 인증 코드 교환을 처리합니다.
- **웹훅 검증**: `GET /webhook/strava`는 Strava에서 웹훅 엔드포인트를 검증하는 데 사용됩니다.
- **웹훅 이벤트 처리**: `POST /webhook/strava`에서 새로운 활동 또는 수정된 활동에 대한 실시간 업데이트를 수신합니다.

---

## 유용한 API

인증 및 데이터 관리를 위한 추가 API 엔드포인트입니다.

### Strava OAuth 인증 URL 조회
사용자를 Strava 인증 페이지로 리다이렉트하기 위한 URL을 생성합니다.

- **엔드포인트**: `GET /oauth/strava/authorize`
- **응답 형식**:
  ```json
  {
    "url": "https://www.strava.com/oauth/authorize?..."
  }
  ```

### 활동 데이터 수동 동기화 (Admin용)
Strava에서 활동 데이터를 수동으로 가져와서 동기화합니다. 초기 데이터 로드나 누락된 데이터를 보정할 때 유용합니다.

- **엔드포인트**: `POST /admin/sync/activities`
- **쿼리 파라미터**:
  - `page` (Integer, 기본값: 1): 가져올 페이지 번호.
  - `perPage` (Integer, 기본값: 30): 페이지당 활동 수.
  - `after` (Long, 선택 사항): 특정 시점(Epoch timestamp) 이후의 활동만 가져옵니다.
  - `before` (Long, 선택 사항): 특정 시점(Epoch timestamp) 이전의 활동만 가져옵니다.
- **응답 형식**:
  ```json
  {
    "enqueuedCount": 5,
    "message": "Enqueued 5 activities for ingestion (page=1, perPage=30)"
  }
  ```

---

## MCP 서버 사용법

Running Pacer는 **SSE (Server-Sent Events)** 전송 방식을 사용하여 MCP 서버를 노출합니다.

- **SSE 엔드포인트**: `http://localhost:8080/sse`
- **메시지 엔드포인트**: `http://localhost:8080/mcp/message`

### 클라이언트 연결 가이드

#### 0. 인증 토큰 준비
MCP 서버에 연결하려면 API 토큰이 필요합니다.
- 웹 대시보드 로그인 후 API 토큰 관리 메뉴에서 생성하거나, DB의 `api_tokens` 테이블에서 직접 확인 가능합니다.
- 토큰 형식: `sk-running-xxxxx...`
- 연결 시 `Authorization: Bearer sk-running-xxxxx` 헤더를 사용합니다.

#### 1. OpenCode (또는 VS Code) 설정
프로젝트 루트에 `.opencode/config.json` 파일을 생성하거나 수정합니다:

```json
{
  "mcpServers": {
    "running-pacer": {
      "type": "sse",
      "url": "http://localhost:8080/sse"
    }
  }
}
```

설정 후 IDE를 재시작하면 AI 어시스턴트가 자동으로 러닝 데이터 도구를 사용할 수 있게 됩니다.

#### 2. Claude Desktop 설정
Claude Desktop은 현재 SSE를 직접 지원하지 않으므로, [mcp-proxy](https://github.com/mcp-proxy)와 같은 브릿지가 필요할 수 있습니다. 또는 `stdio` 모드를 지원하도록 `RunningPacerMcpServer`를 확장해야 합니다.

### 사용 가능한 도구 (Tools)

AI 에이전트가 쉽게 활용할 수 있도록 목적 기반으로 통합된 2개의 핵심 도구를 제공합니다.

#### 1. `running_query` (데이터 조회)
러닝 활동 기록, 기간 요약, 이상치 등 "사실 데이터(Fact)"를 조회합니다.

- **mode**:
  - `activities`: 특정 기간의 활동 목록 조회
  - `summary`: 기간 내 총 거리, 평균 페이스 등 요약
  - `anomalies`: 심박수/페이스 이상 패턴 감지

**사용 예시:**
> "지난 달 내 러닝 기록 보여줘" -> `running_query(mode="activities", ...)`
> "이번 주 총 얼마나 뛰었어?" -> `running_query(mode="summary", ...)`

#### 2. `running_insight` (분석 및 코칭)
데이터를 기반으로 한 비교, 추세 분석, 목표 설정 및 코칭 "인사이트(Insight)"를 제공합니다.

- **mode**:
  - `coaching`: 현재 컨디션(VDOT, TSS) 기반 훈련 조언 및 주간 계획
  - `set_goal`: 레이스 목표(날짜, 거리, 목표시간) 설정
  - `compare`: 두 기간 간의 성과 비교
  - `trend`: 특정 지표(거리/페이스/심박)의 변화 추세
  - `performance`: 종합 성과 분석

**사용 예시:**
> "다음 훈련 어떻게 해야 할까?" -> `running_insight(mode="coaching")`
> "내년 4월 서울마라톤 풀코스 목표 설정해줘 (목표 3시간 30분)" -> `running_insight(mode="set_goal", ...)`
> "지난 달이랑 이번 달 비교해줘" -> `running_insight(mode="compare", ...)`

### 사용 가능한 리소스 (Resources)
- `running://activities/recent`: 최근 30일 동안의 활동 목록.
- `running://summary/week`: 이번 주의 요약 통계.
- `running://summary/month`: 이번 달의 요약 통계.

---

## 프로젝트 구조

```text
src/main/kotlin/io/hansu/pacer/
├── config/             # Spring 설정 (HTTP, Jackson, MCP)
├── controller/         # Strava OAuth 및 웹훅 컨트롤러
├── domain/             # JPA 엔티티 및 리포지토리
├── dto/                # API 및 분석을 위한 데이터 전송 객체(DTO)
├── mcp/                # MCP 서버, 도구, 리소스 및 프롬프트
│   ├── tools/          # MCP 도구 구현체
│   ├── resources/      # MCP 리소스 정의
│   └── prompts/        # MCP 프롬프트 템플릿
├── service/            # 비즈니스 로직 (분석, 수집, 조회)
└── util/               # 유틸리티 클래스 및 Strava API 클라이언트
```

---

## 라이선스

이 프로젝트는 MIT 라이선스에 따라 라이선스가 부여됩니다. 자세한 내용은 [LICENSE](LICENSE) 파일을 참조하십시오.
