import { createRemoteJWKSet, jwtVerify } from "jose";

interface RateLimiter {
  limit(options: { key: string }): Promise<{ success: boolean }>;
}

interface Env {
  DB: D1Database;
  MEDIA: R2Bucket;
  ADMIN_KEY: string;
  RATE_LIMIT_SALT: string;
  GOOGLE_CLIENT_ID: string;
  MAX_TOTAL_STORAGE_BYTES: string;
  MAX_CHARACTERS_PER_USER: string;
  MAX_USER_STORAGE_BYTES: string;
  AUTH_RATE_LIMITER?: RateLimiter;
  PUBLISH_IP_RATE_LIMITER?: RateLimiter;
  PUBLISH_USER_RATE_LIMITER?: RateLimiter;
  CATALOG_RATE_LIMITER?: RateLimiter;
  MEDIA_RATE_LIMITER?: RateLimiter;
}

interface User {
  id: string;
  username: string;
  role: "member" | "admin";
  googleSub?: string | null;
}

interface CharacterUpload {
  displayName?: unknown;
  tagline?: unknown;
  description?: unknown;
  tags?: unknown;
  nsfw?: unknown;
  config?: unknown;
  avatarBase64?: unknown;
  backgroundBase64?: unknown;
}

const JSON_HEADERS = {
  "content-type": "application/json; charset=utf-8",
  "cache-control": "no-store",
  "x-content-type-options": "nosniff"
};
const MAX_CONFIG_BYTES = 100_000;
const MAX_AVATAR_BYTES = 500_000;
const MAX_BACKGROUND_BYTES = 1_000_000;
const MAX_REQUEST_BYTES = 2_300_000;
const SESSION_LIFETIME_MS = 30 * 86_400_000;
const PENDING_UPLOAD_MAX_AGE_MS = 15 * 60_000;
const GOOGLE_JWKS = createRemoteJWKSet(new URL("https://www.googleapis.com/oauth2/v3/certs"));

class HttpError extends Error {
  constructor(message: string, readonly status: number) {
    super(message);
  }
}

export const onRequest: PagesFunction<Env> = async (context) => {
  const { request, env } = context;
  const url = new URL(request.url);
  const path = url.pathname.replace(/^\/api\/v1\/?/, "").split("/").filter(Boolean);

  try {
    if (request.method === "OPTIONS") return new Response(null, { status: 204 });
    if (request.method === "GET" && path[0] === "health") {
      return json({ ok: true, service: "zora-hub", version: 2 }, 200, "public, max-age=60");
    }
    if (request.method === "POST" && path.join("/") === "auth/google") {
      await enforceBurst(request, env.AUTH_RATE_LIMITER, "auth");
      await enforceIpRateLimit(request, env, "auth", 20, 3600);
      return await googleSignIn(request, env);
    }
    if (request.method === "POST" && path.join("/") === "auth/google/link") {
      await enforceBurst(request, env.AUTH_RATE_LIMITER, "link");
      await enforceIpRateLimit(request, env, "link", 10, 3600);
      return await linkGoogleAccount(request, env);
    }
    if (request.method === "POST" && path.join("/") === "admin/uploads") {
      return await setUploadsEnabled(request, env);
    }
    if (request.method === "GET" && path.join("/") === "admin/storage") {
      return await storageStatus(request, env);
    }
    if (request.method === "GET" && path[0] === "media" && path[1]) {
      return await serveMedia(request, env, path.slice(1).join("/"));
    }

    const optionalUser = await authenticate(request, env);
    if (request.method === "GET" && path.length === 1 && path[0] === "characters") {
      await enforceBurst(request, env.CATALOG_RATE_LIMITER, await ipRateKey(request, env));
      return await listCharacters(request, env, optionalUser);
    }
    if (request.method === "GET" && path[0] === "characters" && path[1] && path.length === 2) {
      return await characterDetail(env, optionalUser, path[1]);
    }

    const user = optionalUser;
    if (!user) return error("Authentication required.", 401);
    if (request.method === "POST" && path.join("/") === "auth/logout") {
      return await logout(request, env);
    }
    if (request.method === "GET" && path.join("/") === "me") {
      return await currentUser(env, user);
    }
    if (request.method === "GET" && path.join("/") === "me/characters") {
      return await ownedCharacters(env, user);
    }
    if (request.method === "POST" && path.length === 1 && path[0] === "characters") {
      await enforceBurst(request, env.PUBLISH_IP_RATE_LIMITER, await ipRateKey(request, env));
      await enforceBurst(request, env.PUBLISH_USER_RATE_LIMITER, user.id);
      await enforceIpRateLimit(request, env, "publish-hour", 15, 3600);
      await enforceIpRateLimit(request, env, "publish-day", 40, 86_400);
      await enforceKeyRateLimit(env, `account:publish-hour:${user.id}`, 10, 3600);
      return await publishCharacter(request, env, user);
    }
    if (path[0] === "characters" && path[1]) {
      if (request.method === "DELETE" && path.length === 2) return await deleteCharacter(env, user, path[1]);
      if (request.method === "POST" && path[2] === "favorite") return await toggleFavorite(env, user, path[1]);
      if (request.method === "POST" && path[2] === "report") return await reportCharacter(request, env, user, path[1]);
    }
    return error("Not found.", 404);
  } catch (cause) {
    const message = cause instanceof Error ? cause.message : "Unexpected error.";
    const status = cause instanceof HttpError
      ? cause.status
      : message.startsWith("Invalid ") || message.startsWith("Missing ") ? 400 : 500;
    return error(status === 500 ? "The hub could not complete that request." : message, status);
  }
};

async function authenticate(request: Request, env: Env): Promise<User | null> {
  const auth = request.headers.get("authorization");
  if (!auth?.startsWith("Bearer ")) return null;
  const tokenHash = await sha256(auth.slice(7));
  const sessionUser = await env.DB.prepare(`
    SELECT u.id, u.username, u.role, u.google_sub AS googleSub
    FROM auth_sessions s
    JOIN users u ON u.id = s.user_id
    WHERE s.token_hash = ? AND s.revoked_at IS NULL AND s.expires_at > ? AND u.revoked_at IS NULL
  `).bind(tokenHash, Date.now()).first<User>();
  if (sessionUser) return sessionUser;
  return env.DB.prepare(
    "SELECT id, username, role, google_sub AS googleSub FROM users WHERE token_hash = ? AND revoked_at IS NULL"
  ).bind(tokenHash).first<User>();
}

async function googleSignIn(request: Request, env: Env): Promise<Response> {
  const body = await readJsonLimited(request, MAX_REQUEST_BYTES);
  const identity = await verifyGoogleIdentity(requiredString(body.idToken, "Google ID token", 20_000), env);
  let user = await env.DB.prepare(
    "SELECT id, username, role, google_sub AS googleSub FROM users WHERE google_sub = ? AND revoked_at IS NULL"
  ).bind(identity.sub).first<User>();
  if (!user) {
    const id = crypto.randomUUID();
    const username = await availableUsername(env, optionalString(body.username, 24) || identity.name || "ZoraUser");
    const placeholderToken = await sha256(randomToken(32));
    const placeholderRecovery = await sha256(randomToken(32));
    await env.DB.prepare(`
      INSERT INTO users(id, username, token_hash, recovery_hash, google_sub, google_avatar_url, created_at)
      VALUES (?, ?, ?, ?, ?, ?, ?)
    `).bind(id, username, placeholderToken, placeholderRecovery, identity.sub, identity.picture, Date.now()).run();
    user = { id, username, role: "member", googleSub: identity.sub };
  } else if (identity.picture) {
    await env.DB.prepare("UPDATE users SET google_avatar_url = ? WHERE id = ?")
      .bind(identity.picture, user.id).run();
  }
  return createHubSession(env, user);
}

async function linkGoogleAccount(request: Request, env: Env): Promise<Response> {
  const legacyUser = await authenticate(request, env);
  if (!legacyUser) return error("Existing Community authorization required.", 401);
  const body = await readJsonLimited(request, MAX_REQUEST_BYTES);
  const identity = await verifyGoogleIdentity(requiredString(body.idToken, "Google ID token", 20_000), env);
  const owner = await env.DB.prepare("SELECT id FROM users WHERE google_sub = ?")
    .bind(identity.sub).first<{ id: string }>();
  if (owner && owner.id !== legacyUser.id) return error("This Google account is already linked.", 409);
  if (legacyUser.googleSub && legacyUser.googleSub !== identity.sub) {
    return error("This Community account is already linked to another Google account.", 409);
  }
  await env.DB.prepare(`
    UPDATE users
    SET google_sub = ?, google_avatar_url = ?, token_hash = ?, recovery_hash = ?
    WHERE id = ?
  `).bind(
    identity.sub,
    identity.picture,
    await sha256(randomToken(32)),
    await sha256(randomToken(32)),
    legacyUser.id
  ).run();
  return createHubSession(env, { ...legacyUser, googleSub: identity.sub });
}

async function verifyGoogleIdentity(idToken: string, env: Env): Promise<{ sub: string; name: string; picture: string }> {
  if (!env.GOOGLE_CLIENT_ID || env.GOOGLE_CLIENT_ID === "CONFIGURE_ME") {
    throw new HttpError("Google Sign-In is not configured yet.", 503);
  }
  try {
    const { payload } = await jwtVerify(idToken, GOOGLE_JWKS, {
      algorithms: ["RS256"],
      audience: env.GOOGLE_CLIENT_ID,
      issuer: ["https://accounts.google.com", "accounts.google.com"]
    });
    if (!payload.sub) throw new Error("Missing subject");
    if (payload.email && payload.email_verified !== true) throw new Error("Email is not verified");
    return {
      sub: payload.sub,
      name: typeof payload.name === "string" ? payload.name : "",
      picture: typeof payload.picture === "string" && payload.picture.startsWith("https://") ? payload.picture : ""
    };
  } catch {
    throw new HttpError("Google authorization is invalid or expired.", 401);
  }
}

async function createHubSession(env: Env, user: User): Promise<Response> {
  const token = randomToken(32);
  const now = Date.now();
  const expiresAt = now + SESSION_LIFETIME_MS;
  await env.DB.prepare(`
    INSERT INTO auth_sessions(id, user_id, token_hash, created_at, expires_at)
    VALUES (?, ?, ?, ?, ?)
  `).bind(crypto.randomUUID(), user.id, await sha256(token), now, expiresAt).run();
  return json({ token, expiresAt, user: { id: user.id, username: user.username } }, 200);
}

async function logout(request: Request, env: Env): Promise<Response> {
  const auth = request.headers.get("authorization")!;
  await env.DB.prepare("UPDATE auth_sessions SET revoked_at = ? WHERE token_hash = ?")
    .bind(Date.now(), await sha256(auth.slice(7))).run();
  return new Response(null, { status: 204 });
}

async function availableUsername(env: Env, source: string): Promise<string> {
  const base = source.replace(/[^A-Za-z0-9_]/g, "").slice(0, 20) || "ZoraUser";
  for (let attempt = 0; attempt < 20; attempt += 1) {
    const suffix = attempt === 0 ? "" : String(Math.floor(1000 + Math.random() * 9000));
    const candidate = `${base.slice(0, 24 - suffix.length)}${suffix}`;
    const found = await env.DB.prepare("SELECT 1 AS found FROM users WHERE username = ? COLLATE NOCASE")
      .bind(candidate).first();
    if (!found) return candidate;
  }
  return `Zora${crypto.randomUUID().replace(/-/g, "").slice(0, 12)}`;
}

async function currentUser(env: Env, user: User): Promise<Response> {
  const row = await env.DB.prepare(`
    SELECT username, google_avatar_url AS avatarUrl, storage_bytes AS storageBytes,
      (SELECT COUNT(*) FROM characters WHERE owner_id = users.id AND deleted_at IS NULL) AS characterCount
    FROM users WHERE id = ?
  `).bind(user.id).first<Record<string, unknown>>();
  return json({
    id: user.id,
    username: row?.username || user.username,
    avatarUrl: row?.avatarUrl || null,
    storageBytes: Number(row?.storageBytes || 0),
    storageLimitBytes: Number(env.MAX_USER_STORAGE_BYTES || 8_000_000),
    characterCount: Number(row?.characterCount || 0),
    characterLimit: Number(env.MAX_CHARACTERS_PER_USER || 5)
  });
}

async function ownedCharacters(env: Env, user: User): Promise<Response> {
  const result = await env.DB.prepare(`
    SELECT id, revision, display_name AS displayName, tagline, author, description,
      tags_json AS tagsJson, avatar_key AS avatarKey, background_key AS backgroundKey,
      downloads, favorites, nsfw, updated_at AS updatedAt
    FROM characters
    WHERE owner_id = ? AND deleted_at IS NULL
    ORDER BY updated_at DESC
  `).bind(user.id).all<Record<string, unknown>>();
  return json({ characters: result.results.map(publicCharacter) });
}

async function setUploadsEnabled(request: Request, env: Env): Promise<Response> {
  requireAdmin(request, env);
  const body = await readJsonLimited(request, 10_000);
  if (typeof body.enabled !== "boolean") return error("Missing enabled flag.", 400);
  await env.DB.prepare("UPDATE hub_state SET value = ? WHERE key = 'uploads_enabled'")
    .bind(body.enabled ? 1 : 0).run();
  return storageStatus(request, env);
}

async function storageStatus(request: Request, env: Env): Promise<Response> {
  requireAdmin(request, env);
  const state = await env.DB.prepare(`
    SELECT
      MAX(CASE WHEN key = 'storage_bytes' THEN value END) AS storageBytes,
      MAX(CASE WHEN key = 'uploads_enabled' THEN value END) AS uploadsEnabled,
      MAX(CASE WHEN key = 'storage_warning_bytes' THEN value END) AS warningBytes,
      MAX(CASE WHEN key = 'storage_media_stop_bytes' THEN value END) AS mediaStopBytes,
      MAX(CASE WHEN key = 'storage_stop_bytes' THEN value END) AS stopBytes
    FROM hub_state
  `).first<Record<string, unknown>>();
  const pending = await env.DB.prepare("SELECT COUNT(*) AS count, COALESCE(SUM(expected_bytes), 0) AS bytes FROM pending_uploads WHERE status = 'pending'")
    .first<Record<string, unknown>>();
  return json({
    storageBytes: Number(state?.storageBytes || 0),
    hardLimitBytes: Number(env.MAX_TOTAL_STORAGE_BYTES || 5_000_000_000),
    warningBytes: Number(state?.warningBytes || 4_000_000_000),
    mediaStopBytes: Number(state?.mediaStopBytes || 4_500_000_000),
    stopBytes: Number(state?.stopBytes || 4_750_000_000),
    uploadsEnabled: Number(state?.uploadsEnabled || 0) === 1,
    pendingCount: Number(pending?.count || 0),
    pendingBytes: Number(pending?.bytes || 0)
  });
}

function requireAdmin(request: Request, env: Env): void {
  if (!env.ADMIN_KEY || request.headers.get("x-admin-key") !== env.ADMIN_KEY) {
    throw new HttpError("Admin authorization failed.", 401);
  }
}

async function listCharacters(request: Request, env: Env, user: User | null): Promise<Response> {
  const url = new URL(request.url);
  const query = (url.searchParams.get("q") || "").trim().slice(0, 80);
  const tag = (url.searchParams.get("tag") || "").trim().slice(0, 40);
  const cursor = Math.max(0, Number(url.searchParams.get("cursor")) || Date.now() + 1);
  const limit = clamp(Number(url.searchParams.get("limit")) || 20, 1, 40);
  const nsfw = url.searchParams.get("nsfw") === "1";
  const like = `%${escapeLike(query)}%`;
  const tagLike = `%\"${escapeLike(tag)}\"%`;
  const result = await env.DB.prepare(`
    SELECT c.id, c.revision, c.display_name AS displayName, c.tagline, c.author, c.description,
           c.tags_json AS tagsJson, c.avatar_key AS avatarKey, c.background_key AS backgroundKey,
           c.downloads, c.favorites, c.nsfw, c.updated_at AS updatedAt,
           CASE WHEN f.user_id IS NULL THEN 0 ELSE 1 END AS isFavorite
    FROM characters c
    LEFT JOIN favorites f ON f.character_id = c.id AND f.user_id = ?
    WHERE c.visibility = 'public' AND c.deleted_at IS NULL AND c.updated_at < ?
      AND (? = '' OR c.display_name LIKE ? ESCAPE '\\' OR c.tagline LIKE ? ESCAPE '\\')
      AND (? = '' OR c.tags_json LIKE ? ESCAPE '\\')
      AND (? = 1 OR c.nsfw = 0)
    ORDER BY c.updated_at DESC LIMIT ?
  `).bind(user?.id || "", cursor, query, like, like, tag, tagLike, nsfw ? 1 : 0, limit).all<Record<string, unknown>>();
  const items = result.results.map(publicCharacter);
  const nextCursor = items.length === limit ? items[items.length - 1].updatedAt : null;
  return json({ items, nextCursor }, 200, "private, max-age=30");
}

async function characterDetail(env: Env, user: User | null, id: string): Promise<Response> {
  const row = await env.DB.prepare(`
    SELECT c.*, u.username, CASE WHEN f.user_id IS NULL THEN 0 ELSE 1 END AS viewer_favorite
    FROM characters c JOIN users u ON u.id = c.owner_id
    LEFT JOIN favorites f ON f.character_id = c.id AND f.user_id = ?
    WHERE c.id = ? AND c.deleted_at IS NULL AND (c.visibility = 'public' OR c.owner_id = ?)
  `).bind(user?.id || "", id, user?.id || "").first<Record<string, unknown>>();
  if (!row) return error("Character not found.", 404);
  return json({
    id: row.id,
    revision: row.revision,
    displayName: row.display_name,
    tagline: row.tagline,
    author: row.author || row.username,
    description: row.description,
    tags: parseTags(String(row.tags_json)),
    avatarUrl: mediaUrl(row.avatar_key),
    backgroundUrl: mediaUrl(row.background_key),
    configUrl: mediaUrl(row.config_key),
    downloads: row.downloads,
    favorites: row.favorites,
    isFavorite: Boolean(row.viewer_favorite),
    nsfw: Boolean(row.nsfw),
    updatedAt: row.updated_at
  }, 200, "private, max-age=60");
}

async function publishCharacter(request: Request, env: Env, user: User): Promise<Response> {
  await cleanupExpiredPendingUploads(env);
  const hub = await hubUploadState(env);
  if (!hub.uploadsEnabled) return error("Community publishing is temporarily paused.", 503);
  if (hub.storageBytes >= hub.stopBytes) return error("Community publishing is temporarily full.", 507);

  const contentLength = Number(request.headers.get("content-length")) || 0;
  if (contentLength > MAX_REQUEST_BYTES) return error("Upload is too large.", 413);
  const existing = await env.DB.prepare(
    "SELECT COUNT(*) AS count FROM characters WHERE owner_id = ? AND deleted_at IS NULL"
  ).bind(user.id).first<{ count: number }>();
  if ((existing?.count || 0) >= Number(env.MAX_CHARACTERS_PER_USER || 5)) {
    return error("Character publishing limit reached.", 409);
  }

  const body = await readJsonLimited(request, MAX_REQUEST_BYTES) as CharacterUpload;
  const displayName = requiredString(body.displayName, "display name", 72);
  const tagline = optionalString(body.tagline, 180);
  const description = optionalString(body.description, 1000);
  const tags = cleanTags(body.tags);
  const config = sanitizeConfig(body.config, displayName, tagline, tags);
  const configBytes = new TextEncoder().encode(JSON.stringify(config));
  if (configBytes.byteLength > MAX_CONFIG_BYTES) return error("Configuration is too large.", 413);
  const avatar = decodeImage(body.avatarBase64, MAX_AVATAR_BYTES, "avatar");
  const background = decodeImage(body.backgroundBase64, MAX_BACKGROUND_BYTES, "background");
  const sizeBytes = configBytes.byteLength + (avatar?.bytes.byteLength || 0) + (background?.bytes.byteLength || 0);
  if (hub.storageBytes >= hub.mediaStopBytes && (avatar || background)) {
    return error("Community media publishing is temporarily paused while storage is near capacity.", 507);
  }

  const dailySlot = await consumeCounter(env, `account:publish-success:${user.id}`, 3, 86_400);
  const userLimit = Number(env.MAX_USER_STORAGE_BYTES || 8_000_000);
  const userReservation = await env.DB.prepare(`
    UPDATE users SET storage_bytes = storage_bytes + ?
    WHERE id = ? AND storage_bytes + ? <= ?
  `).bind(sizeBytes, user.id, sizeBytes, userLimit).run();
  if (!userReservation.meta.changes) {
    await releaseCounter(env, dailySlot);
    return error("Your Community storage limit has been reached.", 507);
  }

  const maxTotal = Number(env.MAX_TOTAL_STORAGE_BYTES || 5_000_000_000);
  const reservation = await env.DB.prepare(
    "UPDATE hub_state SET value = value + ? WHERE key = 'storage_bytes' AND value + ? <= ?"
  ).bind(sizeBytes, sizeBytes, maxTotal).run();
  if (!reservation.meta.changes) {
    await env.DB.prepare("UPDATE users SET storage_bytes = MAX(0, storage_bytes - ?) WHERE id = ?")
      .bind(sizeBytes, user.id).run();
    await releaseCounter(env, dailySlot);
    return error("The hub storage safety limit has been reached.", 507);
  }

  const id = crypto.randomUUID();
  const revision = 1;
  const prefix = `characters/${id}/r${revision}`;
  const configKey = `${prefix}/config.json`;
  const avatarKey = avatar ? `${prefix}/avatar.${avatar.extension}` : null;
  const backgroundKey = background ? `${prefix}/background.${background.extension}` : null;
  try {
    await env.DB.prepare(`
      INSERT INTO pending_uploads(id, owner_id, expected_bytes, config_key, avatar_key, background_key, created_at)
      VALUES (?, ?, ?, ?, ?, ?, ?)
    `).bind(id, user.id, sizeBytes, configKey, avatarKey, backgroundKey, Date.now()).run();
    await env.MEDIA.put(configKey, configBytes, { httpMetadata: { contentType: "application/json" } });
    if (avatar && avatarKey) await env.MEDIA.put(avatarKey, avatar.bytes, { httpMetadata: { contentType: avatar.contentType } });
    if (background && backgroundKey) await env.MEDIA.put(backgroundKey, background.bytes, { httpMetadata: { contentType: background.contentType } });
    const now = Date.now();
    await env.DB.batch([
      env.DB.prepare(`
        INSERT INTO characters(id, owner_id, revision, display_name, tagline, author, description,
          tags_json, config_key, avatar_key, background_key, size_bytes, nsfw, created_at, updated_at)
        VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
      `).bind(id, user.id, revision, displayName, tagline, user.username, description, JSON.stringify(tags),
        configKey, avatarKey, backgroundKey, sizeBytes, body.nsfw === true ? 1 : 0, now, now),
      env.DB.prepare("UPDATE hub_state SET value = value + 1 WHERE key = 'catalog_revision'"),
      env.DB.prepare("DELETE FROM pending_uploads WHERE id = ?").bind(id)
    ]);
    return json({ id, revision }, 201);
  } catch (cause) {
    await Promise.all([configKey, avatarKey, backgroundKey].filter(Boolean).map((key) => env.MEDIA.delete(key!)));
    await env.DB.prepare("UPDATE hub_state SET value = MAX(0, value - ?) WHERE key = 'storage_bytes'")
      .bind(sizeBytes).run();
    await env.DB.batch([
      env.DB.prepare("UPDATE users SET storage_bytes = MAX(0, storage_bytes - ?) WHERE id = ?").bind(sizeBytes, user.id),
      env.DB.prepare("DELETE FROM pending_uploads WHERE id = ?").bind(id)
    ]);
    await releaseCounter(env, dailySlot);
    throw cause;
  }
}

async function deleteCharacter(env: Env, user: User, id: string): Promise<Response> {
  const row = await env.DB.prepare(
    "SELECT owner_id, config_key, avatar_key, background_key, size_bytes FROM characters WHERE id = ? AND deleted_at IS NULL"
  ).bind(id).first<Record<string, unknown>>();
  if (!row) return error("Character not found.", 404);
  if (row.owner_id !== user.id && user.role !== "admin") return error("You do not own this character.", 403);
  const now = Date.now();
  await env.DB.batch([
    env.DB.prepare("UPDATE characters SET deleted_at = ?, visibility = 'hidden' WHERE id = ?").bind(now, id),
    env.DB.prepare("UPDATE hub_state SET value = MAX(0, value - ?) WHERE key = 'storage_bytes'").bind(row.size_bytes),
    env.DB.prepare("UPDATE users SET storage_bytes = MAX(0, storage_bytes - ?) WHERE id = ?").bind(row.size_bytes, row.owner_id),
    env.DB.prepare("UPDATE hub_state SET value = value + 1 WHERE key = 'catalog_revision'")
  ]);
  await Promise.all([row.config_key, row.avatar_key, row.background_key]
    .filter((key): key is string => typeof key === "string" && key.length > 0)
    .map((key) => env.MEDIA.delete(key)));
  return new Response(null, { status: 204 });
}

async function toggleFavorite(env: Env, user: User, id: string): Promise<Response> {
  const existing = await env.DB.prepare(
    "SELECT 1 AS found FROM favorites WHERE user_id = ? AND character_id = ?"
  ).bind(user.id, id).first();
  if (existing) {
    await env.DB.batch([
      env.DB.prepare("DELETE FROM favorites WHERE user_id = ? AND character_id = ?").bind(user.id, id),
      env.DB.prepare("UPDATE characters SET favorites = MAX(0, favorites - 1) WHERE id = ?").bind(id)
    ]);
    return json({ favorite: false });
  }
  try {
    await env.DB.batch([
      env.DB.prepare("INSERT INTO favorites(user_id, character_id, created_at) VALUES (?, ?, ?)").bind(user.id, id, Date.now()),
      env.DB.prepare("UPDATE characters SET favorites = favorites + 1 WHERE id = ? AND deleted_at IS NULL").bind(id)
    ]);
    return json({ favorite: true });
  } catch {
    return error("Character not found.", 404);
  }
}

async function reportCharacter(request: Request, env: Env, user: User, id: string): Promise<Response> {
  await enforceKeyRateLimit(env, `account:report:${user.id}`, 5, 86_400);
  await enforceIpRateLimit(request, env, "report", 20, 86_400);
  const reason = requiredString((await readJson(request)).reason, "report reason", 500);
  try {
    await env.DB.prepare(
      "INSERT INTO reports(id, reporter_id, character_id, reason, created_at) VALUES (?, ?, ?, ?, ?)"
    ).bind(crypto.randomUUID(), user.id, id, reason, Date.now()).run();
    return json({ reported: true }, 201);
  } catch {
    return error("This character was already reported or no longer exists.", 409);
  }
}

async function serveMedia(request: Request, env: Env, encodedKey: string): Promise<Response> {
  const key = decodeURIComponent(encodedKey);
  if (!key.startsWith("characters/") || key.includes("..")) return error("Invalid media path.", 400);
  const cache = (caches as unknown as { default: Cache }).default;
  const cacheKey = new Request(request.url, { method: "GET" });
  const cached = await cache.match(cacheKey);
  if (cached) return cached;
  await enforceBurst(request, env.MEDIA_RATE_LIMITER, await ipRateKey(request, env));
  const object = await env.MEDIA.get(key);
  if (!object) return error("Media not found.", 404);
  const headers = new Headers();
  object.writeHttpMetadata(headers);
  headers.set("etag", object.httpEtag);
  headers.set("cache-control", "public, max-age=31536000, immutable");
  headers.set("x-content-type-options", "nosniff");
  const response = new Response(object.body, { headers });
  await cache.put(cacheKey, response.clone());
  return response;
}

async function enforceBurst(request: Request, limiter: RateLimiter | undefined, key: string): Promise<void> {
  if (!limiter) return;
  const result = await limiter.limit({ key });
  if (!result.success) throw new HttpError("Too many requests. Try again shortly.", 429);
}

async function ipRateKey(request: Request, env: Env): Promise<string> {
  const ip = request.headers.get("cf-connecting-ip") || "local";
  return hmacSha256(env.RATE_LIMIT_SALT || env.ADMIN_KEY || "local-development", ip);
}

async function enforceIpRateLimit(request: Request, env: Env, scope: string, limit: number, seconds: number): Promise<void> {
  const ipKey = await ipRateKey(request, env);
  await enforceKeyRateLimit(env, `ip:${scope}:${ipKey}`, limit, seconds);
}

async function enforceKeyRateLimit(env: Env, scope: string, limit: number, seconds: number): Promise<void> {
  await consumeCounter(env, scope, limit, seconds);
}

interface CounterReservation {
  key: string;
  bucket: number;
}

async function consumeCounter(env: Env, scope: string, limit: number, seconds: number): Promise<CounterReservation> {
  const bucket = Math.floor(Date.now() / 1000 / seconds);
  const key = await sha256(scope);
  const expiresAt = (bucket + 1) * seconds * 1000;
  await env.DB.prepare(`
    INSERT INTO rate_limits(key, bucket, count, expires_at) VALUES (?, ?, 1, ?)
    ON CONFLICT(key) DO UPDATE SET
      bucket = excluded.bucket,
      count = CASE WHEN rate_limits.bucket = excluded.bucket THEN rate_limits.count + 1 ELSE 1 END,
      expires_at = excluded.expires_at
  `).bind(key, bucket, expiresAt).run();
  const row = await env.DB.prepare("SELECT count FROM rate_limits WHERE key = ? AND bucket = ?")
    .bind(key, bucket).first<{ count: number }>();
  if ((row?.count || 0) > limit) throw new HttpError("Too many requests. Try again later.", 429);
  return { key, bucket };
}

async function releaseCounter(env: Env, reservation: CounterReservation): Promise<void> {
  await env.DB.prepare("UPDATE rate_limits SET count = MAX(0, count - 1) WHERE key = ? AND bucket = ?")
    .bind(reservation.key, reservation.bucket).run();
}

async function hmacSha256(secret: string, value: string): Promise<string> {
  const key = await crypto.subtle.importKey(
    "raw",
    new TextEncoder().encode(secret),
    { name: "HMAC", hash: "SHA-256" },
    false,
    ["sign"]
  );
  const signature = await crypto.subtle.sign("HMAC", key, new TextEncoder().encode(value));
  return [...new Uint8Array(signature)].map((byte) => byte.toString(16).padStart(2, "0")).join("");
}

async function hubUploadState(env: Env): Promise<{
  storageBytes: number;
  uploadsEnabled: boolean;
  mediaStopBytes: number;
  stopBytes: number;
}> {
  const row = await env.DB.prepare(`
    SELECT
      MAX(CASE WHEN key = 'storage_bytes' THEN value END) AS storageBytes,
      MAX(CASE WHEN key = 'uploads_enabled' THEN value END) AS uploadsEnabled,
      MAX(CASE WHEN key = 'storage_media_stop_bytes' THEN value END) AS mediaStopBytes,
      MAX(CASE WHEN key = 'storage_stop_bytes' THEN value END) AS stopBytes
    FROM hub_state
  `).first<Record<string, unknown>>();
  return {
    storageBytes: Number(row?.storageBytes || 0),
    uploadsEnabled: Number(row?.uploadsEnabled || 0) === 1,
    mediaStopBytes: Number(row?.mediaStopBytes || 4_500_000_000),
    stopBytes: Number(row?.stopBytes || 4_750_000_000)
  };
}

async function cleanupExpiredPendingUploads(env: Env): Promise<void> {
  const expired = await env.DB.prepare(`
    SELECT id, owner_id AS ownerId, expected_bytes AS expectedBytes, config_key AS configKey,
      avatar_key AS avatarKey, background_key AS backgroundKey
    FROM pending_uploads
    WHERE status = 'pending' AND created_at < ?
    LIMIT 20
  `).bind(Date.now() - PENDING_UPLOAD_MAX_AGE_MS).all<Record<string, unknown>>();
  for (const row of expired.results) {
    const keys = [row.configKey, row.avatarKey, row.backgroundKey]
      .filter((key): key is string => typeof key === "string" && key.length > 0);
    await Promise.all(keys.map((key) => env.MEDIA.delete(key)));
    const bytes = Number(row.expectedBytes || 0);
    await env.DB.batch([
      env.DB.prepare("UPDATE users SET storage_bytes = MAX(0, storage_bytes - ?) WHERE id = ?")
        .bind(bytes, row.ownerId),
      env.DB.prepare("UPDATE hub_state SET value = MAX(0, value - ?) WHERE key = 'storage_bytes'").bind(bytes),
      env.DB.prepare("DELETE FROM pending_uploads WHERE id = ?").bind(row.id)
    ]);
  }
  if (Math.random() < 0.05) {
    await env.DB.prepare("DELETE FROM rate_limits WHERE expires_at < ?").bind(Date.now()).run();
  }
}

function sanitizeConfig(raw: unknown, displayName: string, tagline: string, tags: string[]): Record<string, unknown> {
  const source = raw && typeof raw === "object" && !Array.isArray(raw) ? raw as Record<string, unknown> : {};
  const allowedStrings = [
    "instructionMode", "beginnerRole", "beginnerStyle", "beginnerLimits", "instructionPrompt",
    "storyLore", "openingMessage", "backgroundJson"
  ];
  const config: Record<string, unknown> = {
    format: "zora.config.share",
    version: 1,
    displayName,
    tagline,
    traits: tags
  };
  for (const key of allowedStrings) config[key] = optionalString(source[key], key === "instructionPrompt" || key === "storyLore" ? 32_000 : 8_000);
  for (const key of ["avatarScale", "avatarOffsetX", "avatarOffsetY", "temperature"]) {
    const value = Number(source[key]);
    if (Number.isFinite(value)) config[key] = value;
  }
  return config;
}

function decodeImage(value: unknown, maxBytes: number, label: string): { bytes: Uint8Array; contentType: string; extension: string } | null {
  if (value == null || value === "") return null;
  if (typeof value !== "string") throw new Error(`Invalid ${label} image.`);
  const match = value.match(/^(?:data:(image\/(?:jpeg|png|webp));base64,)?([A-Za-z0-9+/=\r\n]+)$/);
  if (!match) throw new Error(`Invalid ${label} image.`);
  const binary = atob(match[2].replace(/\s/g, ""));
  if (binary.length > maxBytes) throw new Error(`Invalid ${label} image: file is too large.`);
  const bytes = Uint8Array.from(binary, (character) => character.charCodeAt(0));
  const detected = detectImage(bytes);
  if (!detected) throw new Error(`Invalid ${label} image format.`);
  return { bytes, ...detected };
}

function detectImage(bytes: Uint8Array): { contentType: string; extension: string } | null {
  if (bytes.length >= 3 && bytes[0] === 0xff && bytes[1] === 0xd8 && bytes[2] === 0xff) return { contentType: "image/jpeg", extension: "jpg" };
  if (bytes.length >= 8 && bytes[0] === 0x89 && bytes[1] === 0x50 && bytes[2] === 0x4e && bytes[3] === 0x47) return { contentType: "image/png", extension: "png" };
  if (bytes.length >= 12 && String.fromCharCode(...bytes.slice(0, 4)) === "RIFF" && String.fromCharCode(...bytes.slice(8, 12)) === "WEBP") {
    return { contentType: "image/webp", extension: "webp" };
  }
  return null;
}

function publicCharacter(row: Record<string, unknown>): Record<string, unknown> {
  return {
    id: row.id,
    revision: row.revision,
    displayName: row.displayName,
    tagline: row.tagline,
    author: row.author,
    description: row.description,
    tags: parseTags(String(row.tagsJson)),
    avatarUrl: mediaUrl(row.avatarKey),
    backgroundUrl: mediaUrl(row.backgroundKey),
    downloads: row.downloads,
    favorites: row.favorites,
    isFavorite: Boolean(row.isFavorite),
    nsfw: Boolean(row.nsfw),
    updatedAt: row.updatedAt
  };
}

function mediaUrl(key: unknown): string | null {
  return typeof key === "string" && key ? `/api/v1/media/${key.split("/").map(encodeURIComponent).join("/")}` : null;
}

function cleanUsername(value: unknown): string {
  const username = requiredString(value, "username", 24);
  if (!/^[A-Za-z0-9_]{3,24}$/.test(username)) throw new Error("Invalid username: use 3-24 letters, numbers, or underscores.");
  return username;
}

function cleanTags(value: unknown): string[] {
  if (!Array.isArray(value)) return [];
  return [...new Set(value.map((tag) => optionalString(tag, 40)).filter(Boolean))].slice(0, 12);
}

function parseTags(value: string): string[] {
  try { return cleanTags(JSON.parse(value)); } catch { return []; }
}

function requiredString(value: unknown, name: string, maxLength: number): string {
  const result = optionalString(value, maxLength);
  if (!result) throw new Error(`Missing ${name}.`);
  return result;
}

function optionalString(value: unknown, maxLength: number): string {
  return typeof value === "string" ? value.trim().slice(0, maxLength) : "";
}

async function readJson(request: Request): Promise<Record<string, any>> {
  return readJsonLimited(request, 100_000);
}

async function readJsonLimited(request: Request, maximumBytes: number): Promise<Record<string, any>> {
  if (!request.headers.get("content-type")?.toLowerCase().includes("application/json")) throw new Error("Invalid content type.");
  const declared = Number(request.headers.get("content-length")) || 0;
  if (declared > maximumBytes) throw new HttpError("Request body is too large.", 413);
  const bytes = new Uint8Array(await request.arrayBuffer());
  if (bytes.byteLength > maximumBytes) throw new HttpError("Request body is too large.", 413);
  try { return JSON.parse(new TextDecoder().decode(bytes)) as Record<string, any>; } catch { throw new Error("Invalid JSON body."); }
}

async function sha256(value: string): Promise<string> {
  const bytes = await crypto.subtle.digest("SHA-256", new TextEncoder().encode(value));
  return [...new Uint8Array(bytes)].map((byte) => byte.toString(16).padStart(2, "0")).join("");
}

function randomToken(bytes: number): string {
  const value = crypto.getRandomValues(new Uint8Array(bytes));
  return btoa(String.fromCharCode(...value)).replace(/\+/g, "-").replace(/\//g, "_").replace(/=+$/, "");
}

function escapeLike(value: string): string {
  return value.replace(/[\\%_]/g, (match) => `\\${match}`);
}

function clamp(value: number, minimum: number, maximum: number): number {
  return Math.min(maximum, Math.max(minimum, Math.trunc(value)));
}

function json(body: unknown, status = 200, cacheControl = "no-store"): Response {
  return new Response(JSON.stringify(body), { status, headers: { ...JSON_HEADERS, "cache-control": cacheControl } });
}

function error(message: string, status: number): Response {
  const response = json({ error: message }, status);
  if (status === 429) response.headers.set("retry-after", "60");
  return response;
}
