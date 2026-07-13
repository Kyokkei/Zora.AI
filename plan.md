# Zora Google Sign-In Publishing and R2 Safety Plan

## Summary

Remove invite-code Community registration completely and use Google Sign-In,
but require an account only for publishing and managing Community characters.

Zora remains local-first:

- Guests can create personal characters, chat, browse Community, and import
  Community characters without an account.
- Google Sign-In is required only to publish, edit, unpublish, or delete the
  user's own Community uploads.
- Chat history, API keys, local sessions, and private persona data are never
  synchronized or uploaded by this feature.
- Existing Community publications keep their current ownership during the
  authentication migration.

Add layered abuse controls before opening publishing to every Google account:

- burst limiting at the Worker edge
- exact longer-window limits by Google-backed Zora account and hashed IP
- per-account character and byte quotas
- an atomic application-wide storage reservation before any R2 write
- a remote upload kill switch and storage warning thresholds
- private R2 access, immutable caching, and orphan cleanup

## Product Rules

### Guest capabilities

- Browse and search Community.
- View character details and media.
- Import/download a public Community character.
- Create and use local characters.
- Use local chat normally.
- Cannot publish or manage Community uploads.

### Signed-in capabilities

- All guest capabilities.
- Publish a local character to Community.
- View `My uploads`.
- Delete or unpublish only owned characters.
- Sign out without deleting local characters or chats.

### Explicitly out of scope

- Cloud chat synchronization or backup.
- Uploading chat messages, API credentials, or conversation attachments.
- Invite codes, invite management, recovery codes, or manual publisher approval.
- Paid subscriptions.
- Google Drive storage.
- Anonymous publishing.

## Safety Defaults

Use conservative limits while the Hub is small. Keep these values in Worker
configuration or `hub_state` rather than hard-coding them in the Android UI.

### Publication limits

| Limit | Default |
| --- | ---: |
| Active published characters per account | 5 |
| Successful new publications per account per day | 3 |
| Publish attempts per account per hour | 10 |
| Publish attempts per IP per hour | 15 |
| Publish attempts per IP per day | 40 |

An edit that replaces media counts as an upload attempt and consumes byte quota,
but does not count as a new active character. Limits must be enforced by the
server even if a modified APK calls the API directly.

### Payload and account storage limits

| Content | Maximum |
| --- | ---: |
| Entire HTTP request body | 2.3 MB |
| Avatar image | 500 KB |
| Background image | 1 MB |
| Sanitized config JSON | 100 KB |
| Total stored bytes per account | 8 MB |
| Application-wide accounted R2 bytes | 5 GB |

Accepted image formats remain JPEG, PNG, and WebP. Continue validating file
signatures after base64 decoding instead of trusting MIME types or extensions.
The Android app should resize/compress images before upload, but server limits
remain authoritative.

### Global storage thresholds

- Below 4.0 GB: publishing operates normally.
- At 4.0 GB (80%): expose an admin warning and record a structured alert event.
- At 4.5 GB (90%): reject new publications that add media; downloads continue.
- At 4.75 GB (95%): disable all publishing and revisions automatically.
- At 5.0 GB: hard rejection remains mandatory even if another switch is wrong.

Add `uploads_enabled` to `hub_state`. The owner can set it to `0` through an
admin-only endpoint or the existing admin page, immediately stopping writes
without an Android release. Disabling uploads must never disable browsing,
cached media, or config imports.

## Authentication Design

### Android sign-in

Use Android Credential Manager with Sign in with Google:

- `androidx.credentials:credentials`
- `androidx.credentials:credentials-play-services-auth`
- `com.google.android.libraries.identity.googleid:googleid`

Request only OpenID identity scopes needed for authentication. Do not request
Google Drive, contacts, or other account data.

The app obtains a short-lived Google ID token and sends it once to:

```text
POST /api/v1/auth/google
```

The Hub verifies the token and returns its own random Zora session token. Store
the Zora token locally using the existing DataStore pattern. Do not send the
Google ID token on every Hub request and never store Google access tokens.

### Worker verification

Verify Google ID tokens server-side using a Cloudflare-compatible JWT library
such as `jose` and Google's published JWKS. Cache the JWKS response with its
declared cache lifetime.

Reject the token unless all checks pass:

- signature uses an accepted Google key and `RS256`
- `iss` is an accepted Google issuer
- `aud` equals Zora's configured web/server OAuth client ID
- `exp` is in the future and `iat` is reasonable
- `sub` is present
- `email_verified` is true when an email claim is used

Use Google's immutable `sub` claim as the external account identity. Never use
email address or display name as an ownership key. Store only the minimum profile
needed for Community display: Google subject ID, chosen Zora username/display
name, optional Google avatar URL, creation time, role, and revocation state.

### Zora sessions

Add an `auth_sessions` table containing only hashed opaque tokens:

- `id`
- `user_id`
- `token_hash`
- `created_at`
- `expires_at`
- `revoked_at`
- optional device label

Return the raw token only at sign-in. A sign-out revokes the current session.
Set a finite lifetime, initially 30 days, and silently obtain a fresh Google ID
token when reauthentication is required.

### Existing owner migration

Do not orphan current Community publications.

1. Deploy the schema and Google endpoint while the current owner's existing
   bearer token still works only for ownership linking.
2. Add `POST /api/v1/auth/google/link`.
3. Require both a valid legacy Hub bearer token and a verified Google ID token.
4. Attach `google_sub` to that existing user row instead of creating a new owner.
5. Preserve the same `users.id`, username, and all `characters.owner_id` values.
6. Disable `/auth/register`, `/auth/recover`, and `/admin/invites` in the same
   backend release. They must never be exposed by the Google-enabled app.
7. Remove invite/recovery UI completely; there is no transitional invite screen.
8. After linking succeeds, revoke the legacy bearer token and use only normal
   Google-backed Zora sessions.
9. Keep a documented admin repair command for the initial owner account.

## Database Changes

Create `hub/migrations/0002_google_auth_and_limits.sql`.

### Users

Alter or rebuild `users` safely to support:

- nullable legacy `token_hash` and `recovery_hash` during migration
- `google_sub TEXT UNIQUE`
- optional `google_avatar_url`
- `storage_bytes` retained as the authoritative per-user byte ledger
- `revoked_at` retained for moderation

The migration removes the old `invites` table after the pre-migration backup is
captured. Every invite endpoint is disabled immediately and no app or API flow can
read, create, or redeem an invite. Legacy owner credentials remain temporarily so
the original publisher can link existing uploads to Google ownership.

### New tables/state

- `auth_sessions` for revocable Zora bearer sessions.
- `pending_uploads` for reservation ID, owner, expected bytes, object keys,
  creation time, and status.
- Retain `rate_limits`, but add cleanup support for expired buckets.
- Add `uploads_enabled`, `storage_warning_bytes`, and
  `storage_stop_bytes` to `hub_state`.
- Optionally add a small `daily_usage` table for exact successful publication
  counts instead of mixing successful actions with rejected attempts.

Indexes must cover token hash, Google subject, owner/status, pending-upload age,
and rate-limit expiry so protection queries do not create D1 table scans.

## Rate-Limit Design

Use two complementary systems.

### Burst protection

Add Cloudflare Worker Rate Limiting bindings for inexpensive short-window checks.
Run the IP burst check before JWT verification or JSON parsing. Add a separate
account-keyed check after authentication.

Suggested initial burst limits:

- Authentication: 10 attempts per IP per minute.
- Publish endpoint: 3 attempts per IP per minute.
- Publish endpoint: 3 attempts per account per minute.
- Config imports: 30 per IP per minute.
- Catalog requests: 60 per IP per minute.
- Media requests: 300 per IP per minute, with edge cache checked first.

Worker rate limiting is deliberately approximate and location-scoped, so it must
not be used for storage accounting or daily quotas.

### Exact D1 limits

Continue using D1 counters for hourly/daily policy enforcement. Apply separate
keys rather than the current combined `publish:{user.id}:{ip}` behavior:

```text
account:publish-attempt:{userId}
account:publish-success:{userId}
ip:publish-attempt:{hmacIp}
ip:auth-attempt:{hmacIp}
```

Hash IP addresses with HMAC-SHA256 using a Cloudflare secret such as
`RATE_LIMIT_SALT`; plain SHA-256 is insufficient because the IPv4 space is easy
to enumerate. Never store a raw IP address. Return HTTP `429` with a useful
`Retry-After` header.

Delete expired counter buckets opportunistically and with a scheduled cleanup so
the `rate_limits` table cannot grow forever.

## Safe Upload Transaction

Reorder publishing so no R2 operation occurs until every inexpensive check has
passed:

1. Reject if `uploads_enabled` is false.
2. Apply the IP burst limiter.
3. Reject a declared `Content-Length` above 2.3 MB.
4. Authenticate the Zora session.
5. Apply account burst and exact account/IP window limits.
6. Read the request with an actual byte-limited reader; do not rely only on
   `Content-Length`, which may be missing or false.
7. Sanitize config and validate decoded image signatures and exact byte sizes.
8. Check active-character and successful-publication limits.
9. Atomically reserve expected bytes against both the account's 8 MB allowance
   and the global 5 GB allowance, recording a `pending_uploads` row.
10. Write only the reserved config/avatar/background keys to private R2.
11. Insert the character and mark the reservation complete.
12. On failure, delete any written objects and release both byte reservations.

Use conditional SQL updates such as `... WHERE storage_bytes + ? <= ?` and verify
`meta.changes`. This prevents concurrent requests from both passing a read-then-
write quota check.

If a Worker terminates between reservation and finalization, a scheduled cleanup
must delete pending R2 keys and release reservations older than 15 minutes. A
leaked reservation may temporarily reduce publishing capacity, but must never let
actual R2 usage exceed the cap.

## R2 and Download Protection

- Keep `zora-hub-media` private and keep public `r2.dev` access disabled.
- Never include R2 API credentials or presigned credentials in the APK.
- Continue serving assets through `/api/v1/media/...`.
- Keep immutable revision URLs and one-year edge caching for published assets.
- Check the Cloudflare cache before calling `MEDIA.get()`.
- Do not expose arbitrary object keys; accept only validated character paths.
- Keep config, avatar, and background deletion tied to character ownership.
- Periodically reconcile D1's byte ledger against known character and pending
  object records, and report discrepancies in the admin page.

While the Hub remains on Workers Free, its 100,000 request/day ceiling is also a
last-resort traffic brake. Do not upgrade to Workers Paid without revisiting CPU,
request, D1, and R2 spending controls.

## Android UI Changes

Primary files:

- `app/build.gradle.kts`
- `app/src/main/java/com/yozora/aichat/ui/chat/ChatViewModel.kt`
- `app/src/main/java/com/yozora/aichat/ui/CompanionChatApp.kt`
- English and Vietnamese string resources

Changes:

- Delete username, invite-code, and recovery-code dialogs and replace them with a
  standard `Sign in with Google` action.
- Keep the Community Discover pane public and account-free.
- Change `Register to publish` to `Sign in to publish`.
- When a guest taps Publish, explain that Google is used only for upload
  ownership; local characters and chats stay on-device.
- Show the signed-in Zora username/avatar and a Sign out action.
- Add `My uploads` filtering/management for the authenticated owner.
- Present specific server errors for rate limit, character quota, account storage
  quota, global storage pause, invalid image, and expired authentication.
- Do not imply that chats are backed up after signing in.

Keep OAuth client IDs in generated resources or BuildConfig. A client ID is not a
secret. Keep any rate-limit salt, admin key, and server-only configuration in
Cloudflare secrets.

## Cloudflare/API Changes

Primary files:

- `hub/functions/api/v1/[[path]].ts`
- `hub/wrangler.toml`
- `hub/migrations/0002_google_auth_and_limits.sql`
- `hub/public/admin.js`
- `hub/README.md`

New or changed endpoints:

```text
POST /api/v1/auth/google        Google token exchange/sign-in
POST /api/v1/auth/google/link   Link an existing legacy owner
POST /api/v1/auth/logout        Revoke current Zora session
GET  /api/v1/me                 Current profile and quota usage
GET  /api/v1/me/characters      Owned Community uploads
POST /api/v1/characters         Authenticated publishing, stricter limits
POST /api/v1/admin/uploads      Enable/disable publishing
GET  /api/v1/admin/storage      Accounted storage and pending reservations
```

Existing public endpoints for catalog, detail, media, and config import remain
anonymous. Existing owner authorization remains mandatory for deletion and future
editing.

## Manual Setup Required From Owner

These steps require access to the owner's Google and Cloudflare dashboards:

1. Create or select a Google Cloud project for Zora.AI.
2. Configure the OAuth consent screen with app name, support contact, privacy
   policy/homepage details requested by Google, and only basic identity scopes.
3. Create an Android OAuth client for package `com.yozora.aichat` with the SHA-1
   fingerprint of the certificate used to sign the published APK.
4. Create the web/server OAuth client ID used as the ID-token audience.
5. Provide the non-secret client ID to the Android build configuration.
6. Add `GOOGLE_CLIENT_ID` and `RATE_LIMIT_SALT` to Cloudflare configuration,
   using a Wrangler secret for the salt.
7. Apply the D1 migration and deploy the Hub.
8. Link the current legacy publisher account to the owner's Google account. Invite
   registration is already disabled and is not used for this link.

Important: the current release build uses the debug signing configuration. Google
Sign-In matches package name plus signing certificate, so the registered SHA-1
must match the APK users actually install. Do not change signing identity casually
after rollout because Android updates and Google authentication both depend on it.

## Work Codex Can Implement

- Android Credential Manager integration and UI.
- Google ID-token exchange and Zora session handling.
- D1 migration and ownership-preserving link endpoint.
- Account/IP rate limiting and HMAC IP keys.
- Per-account/global byte reservation and pending-upload cleanup.
- Admin upload kill switch and storage status UI.
- English/Vietnamese strings.
- Hub type-check/build, Android release build, API smoke tests, and release assets.

Codex cannot create or accept the Google Cloud consent screen on the owner's behalf
without the owner's authenticated dashboard interaction, but can guide those steps
and verify the resulting client IDs and certificate fingerprints.

## Rollout Order

1. Record a D1 backup/export and current R2 object inventory.
2. Add migration, Google verification, sessions, quota state, and the one-time
   existing-owner link.
3. Deploy the Hub with every invite and recovery endpoint disabled.
4. Smoke-test anonymous browse/import and the one-time existing-owner link.
5. Build the Android Google Sign-In UI and publish flow.
6. Install/test on two devices: one guest and one Google-signed-in publisher.
7. Link the current owner account and verify existing uploads remain manageable.
8. Revoke the linked owner's legacy token; only Google-backed sessions remain.
9. Monitor D1 counters, pending uploads, and accounted R2 bytes for at least one
   release cycle before removing legacy schema fields.

## Test Plan

### Authentication

- Valid Google sign-in creates or returns the same Zora user.
- Repeated sign-in with the same Google `sub` never creates duplicate owners.
- Forged, expired, wrong-audience, and wrong-issuer tokens return `401`.
- Sign out revokes the Zora session.
- Existing owner links to Google without changing character ownership.
- A different Google account cannot link an already-linked legacy owner.

### Guest behavior

- Fresh device can browse, search, view media, and import without login.
- Guest can create and chat with local characters.
- Guest Publish opens the sign-in explanation instead of uploading.
- No chat message or API key appears in Hub payloads.

### Limits and R2 safety

- Account hourly/daily limits return `429` with `Retry-After`.
- Different accounts on one IP eventually hit the IP ceiling.
- One account changing IP still hits its account ceiling.
- Sixth active character is rejected before R2 access.
- Upload exceeding the 8 MB account ledger is rejected before R2 access.
- Global 90%/95% thresholds produce the intended degraded/paused behavior.
- `uploads_enabled = 0` immediately blocks publishing while downloads work.
- Missing or dishonest `Content-Length` cannot bypass actual body limits.
- Simultaneous uploads cannot exceed account or global byte limits.
- Failed R2 writes release reservations and remove partial objects.
- Cleanup releases abandoned reservations older than 15 minutes.
- Expired rate-limit rows and orphaned media are removed safely.

### Build and deployment

```powershell
cd hub
npm run check
npm run build
npx wrangler d1 migrations apply zora-hub-db --remote
npm run deploy -- --commit-dirty=true

cd ..
.\gradlew.bat assembleZoraRelease
```

Do not apply the production migration or deploy automatically until the Google
OAuth client IDs are configured and a D1 backup has been taken.

## Acceptance Criteria

- Signing in is never required for local creation, chat, Community browse, or
  Community import.
- Publishing requires a server-verified Google-backed Zora session.
- Each publication is owned by a stable Google-backed user ID.
- Existing uploads retain ownership through migration.
- Both account and IP limits are active and independently tested.
- Per-account and global byte quotas are enforced before R2 writes.
- The owner can stop uploads remotely without blocking downloads.
- R2 remains private and no server credential exists in the APK.
- The application cannot account for more than 5 GB of Hub media without an
  intentional configuration change.
- No cloud chat-sync behavior is introduced.
