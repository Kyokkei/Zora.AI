# Zora Hub

Invite-only community config sharing for Zora.AI, hosted on Cloudflare Pages,
Pages Functions, D1, and private R2 storage.

## Safety limits

- 5 GB application-wide storage ceiling (below R2's free 10 GB-month allowance)
- 20 published characters per account
- 500 KB avatar, 1 MB background, and 100 KB sanitized config
- Private R2 bucket; media is served through immutable revision URLs
- API keys, chat messages, local URIs, and unknown config fields are never accepted
- Invite, publish, recovery, and report endpoints are rate limited

## Local development

```powershell
npm install
npm run check
npm run dev
```

`ADMIN_KEY` belongs in `.dev.vars`, which is ignored by git.

## Production

The committed `wrangler.toml` binds the `zora-hub-db` D1 database and
`zora-hub-media` R2 bucket. Apply migrations and deploy with:

```powershell
npx wrangler d1 migrations apply zora-hub-db --remote
npm run deploy -- --commit-dirty=true
```
