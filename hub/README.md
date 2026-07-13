# Zora Hub

Public community config discovery for Zora.AI, hosted on Cloudflare Pages,
Pages Functions, D1, and private R2 storage. Browsing and importing are anonymous;
publishing and upload management require Google Sign-In.

## Safety limits

- 5 GB application-wide storage ceiling (below R2's free 10 GB-month allowance)
- 5 published characters and 8 MB stored data per Google-backed account
- 500 KB avatar, 1 MB background, and 100 KB sanitized config
- 3 successful publications per account per day
- independent account and HMAC-hashed IP rate limits
- Private R2 bucket; media is served through immutable revision URLs
- 4 GB warning, 4.5 GB media pause, 4.75 GB upload stop, and 5 GB hard ceiling
- remotely switchable upload kill switch; browsing/import remain available
- API keys, chat messages, local URIs, and unknown config fields are never accepted
- Authentication, publish, catalog, media, and report endpoints are rate limited

Invite and recovery-code endpoints were removed. Existing legacy ownership can be
linked once to Google without changing character IDs or R2 object ownership.

## Local development

```powershell
npm install
npm run check
npm run dev
```

`ADMIN_KEY` belongs in `.dev.vars`, which is ignored by git.
`RATE_LIMIT_SALT` is a Cloudflare secret. `GOOGLE_CLIENT_ID` is the non-secret web
OAuth client ID used as the Google ID-token audience.

## Production

The committed `wrangler.toml` binds the `zora-hub-db` D1 database and
`zora-hub-media` R2 bucket. Apply migrations and deploy with:

```powershell
npx wrangler d1 migrations apply zora-hub-db --remote
npm run deploy -- --commit-dirty=true
```
