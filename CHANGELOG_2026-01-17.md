# Changelog - 2026-01-17

## 🔐 Authentication & Security Implementation

This release introduces a full-fledged authentication system using Spring Security, OAuth2, and API Tokens, enabling multi-user support and secure access to the MCP server.

### 🚀 Features

#### 1. OAuth2 Authentication
- **Spring Security Integration**: Added `spring-boot-starter-security` and `oauth2-client` dependencies.
- **Social Login**: Implemented Google OAuth2 flow (extensible to Apple/others).
- **Auto-Registration**: Created `CustomOAuth2UserService` to automatically create or update user accounts in the `users` table upon successful social login.

#### 2. API Token System for MCP
- **Token Management**: Implemented `ApiTokenService` to generate, verify, and revoke API tokens.
- **Token Format**: Tokens use a secure format `sk-running-[base64-random]` for easy identification.
- **API Endpoints**: Added `ApiTokenController` to manage tokens:
  - `POST /api/tokens` - Create a new token
  - `GET /api/tokens` - List my tokens
  - `DELETE /api/tokens/{id}` - Revoke a token

#### 3. Secure MCP Context
- **Dynamic User Resolution**: Updated `McpUserContext` to remove hardcoded user ID (`1L`).
- **Token Verification**: The MCP server now extracts the `Authorization: Bearer <token>` header from incoming requests and verifies it against the database to identify the active user.

### 🛠 Database Changes (Flyway V6)

- **Users Table Extension**:
  - Added `email` (VARCHAR, Unique)
  - Added `provider` (VARCHAR, e.g., 'GOOGLE')
  - Added `provider_id` (VARCHAR)
- **New Table**: `api_tokens`
  - Columns: `token_id`, `user_id`, `token`, `name`, `created_at`, `last_used_at`, `expires_at`
  - Indexed by `token` for fast lookup during MCP requests.

### 🔒 Security Configuration

- **CSRF**: Disabled for REST API compatibility.
- **Session Policy**: `IF_REQUIRED` (Stateful for OAuth login, Stateless for API calls preferred but flexible).
- **Public Endpoints**:
  - `/`, `/error`, `/favicon.ico`
  - `/oauth/**`, `/login/**` (Auth flows)
  - `/webhook/**` (Strava callbacks)
  - `/sse`, `/mcp/**` (Currently open at network level, but `McpUserContext` enforces token validity for logic)

### 📝 Configuration Requirements

To run the application, you must now provide OAuth2 credentials in `application.yml` or environment variables:

```yaml
spring:
  security:
    oauth2:
      client:
        registration:
          google:
            client-id: ${GOOGLE_CLIENT_ID}
            client-secret: ${GOOGLE_CLIENT_SECRET}
            scope: email, profile
```

### ⏭ Next Steps
- Connect Strava OAuth flow to the currently logged-in user.
- Implement proper error handling for 401/403 cases in MCP.
- Add Apple Sign-In support.
