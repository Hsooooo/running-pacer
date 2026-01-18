# Changelog - 2026-01-18

## 🛡️ Security & Onboarding Improvements

This update focuses on stabilizing the build, securing MCP endpoints, and providing a user-friendly onboarding experience for connecting Strava accounts and managing API tokens.

### 🚀 Features

#### 1. User Onboarding Flow
- **Web UI**: Added Thymeleaf-based web interface.
  - `/login`: Clean login page with Google Sign-In button.
  - `/`: User dashboard showing connection status and token management.
- **Dashboard Features**:
  - **Strava Status**: Visual indicator of Strava connection status with "Connect" button.
  - **Token Management**: UI to create, list, and revoke API tokens for MCP clients directly from the browser.

#### 2. MCP Security Enforcement
- **ApiTokenFilter**: Implemented a security filter that intercepts requests to `/sse` and `/mcp/**`.
- **Authentication**: Requires a valid `Authorization: Bearer <token>` header for all MCP operations.
- **Context Injection**: Valid tokens automatically inject the correct user context into the MCP session.

### 🛠 Technical Improvements

#### 1. Database Migration (MySQL → PostgreSQL)
- **Engine Switch**: Migrated database from MySQL 8.4 to PostgreSQL 16 (Alpine) to align with Render deployment environment.
- **Schema Updates**: Converted all Flyway migration scripts (V1-V6) to PostgreSQL syntax (AUTO_INCREMENT -> GENERATED ALWAYS AS IDENTITY, DATETIME -> TIMESTAMP, JSON -> JSONB).
- **Driver**: Replaced MySQL driver with `org.postgresql:postgresql` and updated Flyway dependency.
- **Docker Compose**: Updated local development stack to use `postgres:16-alpine`.

#### 2. Build & Test Stabilization
- **Mockito-Kotlin**: Introduced `mockito-kotlin` library to resolve issues with Kotlin non-null types and Mockito's `any()` matchers.
- **Test Fixes**: Refactored `IngestJobSchedulerTest` and `DbJobProducerTest` to pass successfully.

#### 3. Repository Extensions
- **StravaUserLinksRepository**: Added `existsByUserId` method to support dashboard status checks.

### 📝 Configuration Updates
- **Dependencies**: Added `spring-boot-starter-thymeleaf` and `mockito-kotlin`.
- **SecurityConfig**: Updated filter chain to apply `ApiTokenFilter` before Username/Password authentication and protect MCP routes.

### ⏭ Next Steps
- Verify the end-to-end flow with a real Strava account.
- Add error handling for Strava API quota limits.
