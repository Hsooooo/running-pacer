# Changelog - 2026-01-19

## OAuth per-user MCP (Parallel with API tokens)

### 🚀 Features
- Added OAuth 2.1 authorization server configuration with RSA keys from environment variables.
- Injected `user_id` claim into access tokens for MCP user resolution.
- Enabled JWT resource server for `/sse` and `/mcp/**` while keeping API token fallback.

### 🛠 Technical Changes
- Added Spring Authorization Server dependency.
- Added OAuth2 authorization server schema migration tables.
- Added OAuth configuration properties for issuer and RSA keys.
- Updated MCP user context to resolve user from JWT or API token.

### 📝 Configuration
- New env vars: `OAUTH_ISSUER`, `OAUTH_RSA_PUBLIC_KEY`, `OAUTH_RSA_PRIVATE_KEY`.

### ⏭ Next Steps
- Register ChatGPT app client in `oauth2_registered_client`.
- Validate OAuth flow and MCP tool calls with JWT bearer tokens.
