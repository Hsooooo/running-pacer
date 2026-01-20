# CHANGELOG - 2026-01-20

## 핵심 인사이트 카드 강화 (Web Dashboard)

웹 대시보드의 사용자 경험을 향상시키기 위해 핵심 인사이트 카드를 추가하고 데이터 연동을 완료했습니다.

### 변경 사항

#### 1. Backend & SQL Standardization
- **SQL 표준화 (PostgreSQL)**: 모든 네이티브 쿼리를 PostgreSQL 문법으로 통일했습니다.
  - `RunningQueryService.kt`: 주간 집계 로직을 `date_sub/weekday`에서 `date_trunc('week', ...)`로 변경.
  - 기존의 `ON CONFLICT`, `jsonb`, `SKIP LOCKED` 등 PostgreSQL 특화 기능을 유지하며 MySQL 의존적인 문법을 제거했습니다.
- `DashboardController` 추가: 대시보드 전용 통합 인사이트 API(`/api/dashboard/insights`) 구현.
- `DashboardDtos.kt`: 대시보드 데이터 전송을 위한 DTO(`DashboardInsights`) 정의.
- `RunningToolService`의 기존 분석 로직을 재사용하여 주간 요약, 4주 트렌드, 이상치, 리스크 점수 산출.

#### 2. Strava Integration & Sync Strategy
- **최초 연동 자동 동기화**: Strava 연결 직후 최근 6개월 치 활동을 자동으로 작업 큐에 등록하도록 개선 (`StravaAuthService`).
- **상태 관리**: `strava_user_links` 테이블에 `initial_sync_done` 컬럼을 추가하여 중복 동기화를 방지 (V8 migration).
- **기간 선택형 동기화**: `/admin/sync/activities`에서 `from`, `to` 날짜 파라미터를 지원하여 특정 기간 데이터를 수동으로 재수집 가능하도록 확장.

#### 3. Frontend (home.html & activities.html)
- **Activity Explorer 페이지 신규 추가**: `/activities` 경로에서 전체 활동 목록을 페이징 및 날짜 필터와 함께 조회 가능.
- 대시보드 상단에 4개의 인사이트 카드 배치:
  - **Weekly Distance**: 이번 주 총 주행 거리 및 횟수.
  - **4-Week Trend**: 최근 4주간의 거리 변화율 및 추세 분석.
  - **Anomalies (30d)**: 최근 30일 내 감지된 피로 징후 활동 수.
  - **Condition Risk**: 현재 컨디션 리스크 점수 및 레벨.
- **Sparkline 차트 도입**: Chart.js를 사용하여 최근 4주 트렌드를 시각화하는 미니 라인 차트 구현.
- **상태별 시각화**: 트렌드 개선/하락, 리스크 높음/낮음 등에 따른 색상 지표(Green/Orange/Red) 적용.

#### 4. UI/UX & Interaction Improvements
- **대시보드 ↔ 활동 목록 연결**: 대시보드 내 "Recent Runs" 카드에 "View All" 버튼을 추가하여 전체 활동 목록(`/activities`)으로의 접근성을 개선했습니다.
- **활동 상세 뷰 고도화**: `/activities` 페이지에서 개별 활동 클릭 시 상단에 상세 지표(거리, 시간, 페이스, 심박수)와 페이스/심박 추세 그래프가 즉시 표시되도록 구현했습니다.
- **Custom Sync 모달 도입**: 대시보드 Strava 관리 영역에 기간 선택형 동기화 UI를 모달 형태로 추가하여 사용자 편의성을 높였습니다.

### 효과
- **데이터 일관성**: 모든 DB 쿼리를 PostgreSQL로 표준화하여 운영 환경에서의 안정성 확보.
- **사용자 경험 향상**: 대시보드 인사이트와 더불어 상세 활동 탐색 기능 및 개별 활동 요약을 제공하여 데이터 활용도 증대.
- **효율적 데이터 수집**: 6개월 단위 초기 동기화와 선택적 재수집 기능을 통해 리소스 사용 최적화.
- **가독성 증대**: 텍스트 위주의 정보를 그래프와 색상을 통해 직관적으로 전달.
