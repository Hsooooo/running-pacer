# Changelog - 2026-01-17 (Part 2)

## 🔗 Strava Multi-User Integration

This update connects the previously implemented Authentication System with Strava services, ensuring that Strava data is correctly mapped to the authenticated user.

### 🚀 Features

#### 1. Secure Strava OAuth Flow
- **State Parameter**: The `/oauth/strava/authorize` endpoint now embeds the authenticated user's ID (Base64 encoded) into the OAuth `state` parameter.
- **User Linking**: The callback handler (`/oauth/strava/callback`) decodes the `state` to link the Strava account specifically to the logged-in user, rather than creating a new anonymous user.
- **Account Relinking**: If a Strava account was previously linked to another user, it is now automatically moved to the current user (with a warning log).

#### 2. User-Context Aware API Client
- **StravaApiClient Refactoring**: All methods (`getActivity`, `getAthleteActivities`, etc.) now require a `userId` parameter.
- **Token Isolation**: API calls now strictly use the Access Token belonging to the specified `userId`, preventing cross-user data leakage.

#### 3. Background Job Context
- **IngestJobScheduler**: Updated to utilize the `userId` stored in the `ingest_jobs` table when fetching data from Strava, ensuring background tasks run with the correct user credentials.

#### 4. Admin Tools
- **Sync Controller**: The `/admin/sync/activities` endpoint now requires authentication and syncs data only for the logged-in admin user.

### 🛠 Code Refactoring

- **Controller**: `StravaOAuthController` updated to use `@AuthenticationPrincipal` and `state` validation.
- **Service**: `StravaAuthService` modified to support linking existing users and fetching user-specific tokens.
- **Tests**: Updated `IngestJobSchedulerTest` to reflect the new `StravaApiClient` signature requiring `userId`.

### ⚠️ Breaking Changes
- `StravaApiClient` methods are no longer stateless regarding users; they require `userId`.
- `StravaAuthService.exchangeCodeAndSaveTokens` now requires a `targetUserId`.

### ⏭ Next Steps
- Verify end-to-end flow from Login -> Strava Link -> Data Ingestion.
- Implement UI or CLI instructions for users to initiate the connection.
