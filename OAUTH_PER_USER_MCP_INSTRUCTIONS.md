## 작업: OAuth 2.1 Authorization Server 구축 (ChatGPT MCP 앱 연동용)

### 목표
현재 API 토큰 기반 MCP 인증을 OAuth 2.1 per-user 방식으로 전환하여,
ChatGPT 웹 앱에서 사용자별 개인화된 MCP 서비스를 제공한다.

### 전제 조건
- Spring Boot 4.0.1 + Kotlin
- 기존 Google OAuth2 Client 로그인 유지
- MCP SSE 엔드포인트: `/sse`, `/mcp/message`

### 작업 목록

#### Phase 1: 의존성 및 DB 스키마
1. `build.gradle.kts`에 `spring-security-oauth2-authorization-server` 추가
2. Flyway 마이그레이션 파일 생성:
   - `oauth2_registered_client` 테이블
   - `oauth2_authorization` 테이블
   - `oauth2_authorization_consent` 테이블

#### Phase 2: Authorization Server 설정
1. `config/AuthorizationServerConfig.kt` 생성
   - `SecurityFilterChain` for authorization server
   - `JdbcRegisteredClientRepository` 빈
   - `JdbcOAuth2AuthorizationService` 빈
   - `JWKSource` (RSA 키 생성/로드)
   - `AuthorizationServerSettings` (issuer 설정)
2. `application.yml`에 issuer URL 추가

#### Phase 3: Resource Server 통합
1. `SecurityConfig.kt` 수정
   - `/sse`, `/mcp/**` 경로에 `oauth2ResourceServer().jwt()` 적용
   - 기존 `oauth2Login()`은 웹 로그인용으로 유지
2. `ApiTokenFilter.kt` 제거 또는 비활성화 (또는 fallback으로 유지)

#### Phase 4: 사용자 식별 로직 변경
1. `McpUserContext.kt` 수정
   - `SecurityContextHolder`에서 `Jwt` 추출
   - `user_id` claim으로 userId 반환
2. JWT 생성 시 `user_id` claim 포함하도록 커스터마이징
   - `OAuth2TokenCustomizer<JwtEncodingContext>` 빈 추가

#### Phase 5: 클라이언트 등록 및 테스트
1. ChatGPT 앱용 클라이언트 사전 등록 (DB insert 또는 admin API)
   - `client_id`, `client_secret`, `redirect_uris`, `scopes`
2. 로컬 테스트:
   - OAuth flow 수동 테스트 (authorize → callback → token)
   - MCP Inspector로 Bearer 토큰 사용 확인
3. ChatGPT 앱 연결 테스트

#### Phase 6: (선택) 기존 API 토큰 병행 운영
1. `ApiTokenFilter`를 OAuth 실패 시 fallback으로 동작하도록 수정
2. 전환 완료 후 API 토큰 로직 제거

### 주의사항
- HTTPS 필수 (프로덕션)
- `offline_access` scope로 refresh token 발급 필수
- RSA 키는 환경변수 또는 Vault로 관리
- 기존 웹 로그인(Google OAuth)과 MCP OAuth는 분리된 flow

### 예상 결과
- `/.well-known/openid-configuration` 정상 노출
- ChatGPT 앱에서 OAuth 연결 → 사용자별 MCP 도구 호출 가능
- 각 사용자는 본인 러닝 데이터만 조회
