interface Env {
  DB: D1Database;
  MEDIA: R2Bucket;
  ADMIN_KEY: string;
  MAX_TOTAL_STORAGE_BYTES: string;
  MAX_CHARACTERS_PER_USER: string;
}

interface User {
  id: string;
  username: string;
  role: "member" | "admin";
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
      return json({ ok: true, service: "zora-hub", version: 1 }, 200, "public, max-age=60");
    }
    if (request.method === "POST" && path.join("/") === "admin/invites") {
      return createInvite(request, env);
    }
    if (request.method === "POST" && path.join("/") === "auth/register") {
      await enforceRateLimit(request, env, "register", 5, 3600);
      return register(request, env);
    }
    if (request.method === "POST" && path.join("/") === "auth/recover") {
      await enforceRateLimit(request, env, "recover", 5, 3600);
      return recover(request, env);
    }
    if (request.method === "GET" && path[0] === "media" && path[1]) {
      return serveMedia(request, env, path.slice(1).join("/"));
    }

    const optionalUser = await authenticate(request, env);
    if (request.method === "GET" && path.length === 1 && path[0] === "characters") {
      return listCharacters(request, env, optionalUser);
    }
    if (request.method === "GET" && path[0] === "characters" && path[1] && path.length === 2) {
      return characterDetail(env, optionalUser, path[1]);
    }

    const user = optionalUser;
    if (!user) return error("Authentication required.", 401);
    if (request.method === "POST" && path.length === 1 && path[0] === "characters") {
      await enforceRateLimit(request, env, `publish:${user.id}`, 10, 3600);
      return publishCharacter(request, env, user);
    }
    if (path[0] === "characters" && path[1]) {
      if (request.method === "DELETE" && path.length === 2) return deleteCharacter(env, user, path[1]);
      if (request.method === "POST" && path[2] === "favorite") return toggleFavorite(env, user, path[1]);
      if (request.method === "POST" && path[2] === "report") return reportCharacter(request, env, user, path[1]);
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

async function createInvite(request: Request, env: Env): Promise<Response> {
  if (!env.ADMIN_KEY || request.headers.get("x-admin-key") !== env.ADMIN_KEY) {
    return error("Admin authorization failed.", 401);
  }
  const body = await readJson(request);
  const days = clamp(Number(body.expiresInDays) || 30, 1, 365);
  const code = randomToken(18);
  const now = Date.now();
  await env.DB.prepare(
    "INSERT INTO invites(id, code_hash, created_at, expires_at, created_by) VALUES (?, ?, ?, ?, ?)"
  ).bind(crypto.randomUUID(), await sha256(code), now, now + days * 86_400_000, "admin-key").run();
  return json({ code, expiresAt: now + days * 86_400_000 }, 201);
}

async function register(request: Request, env: Env): Promise<Response> {
  const body = await readJson(request);
  const username = cleanUsername(body.username);
  const inviteCode = requiredString(body.inviteCode, "invite code", 128);
  const id = crypto.randomUUID();
  const token = randomToken(32);
  const recoveryCode = randomToken(24);
  const now = Date.now();
  const invite = await env.DB.prepare(
    "SELECT id FROM invites WHERE code_hash = ? AND used_at IS NULL AND (expires_at IS NULL OR expires_at > ?)"
  ).bind(await sha256(inviteCode), now).first<{ id: string }>();
  if (!invite) return error("Invite is invalid, expired, or already used.", 400);
  try {
    await env.DB.prepare("INSERT INTO users(id, username, token_hash, recovery_hash, created_at) VALUES (?, ?, ?, ?, ?)")
      .bind(id, username, await sha256(token), await sha256(recoveryCode), now).run();
    const claim = await env.DB.prepare("UPDATE invites SET used_at = ?, used_by = ? WHERE id = ? AND used_at IS NULL")
      .bind(now, id, invite.id).run();
    if (!claim.meta.changes) {
      await env.DB.prepare("DELETE FROM users WHERE id = ?").bind(id).run();
      return error("Invite is invalid, expired, or already used.", 400);
    }
  } catch {
    await env.DB.prepare("DELETE FROM users WHERE id = ?").bind(id).run();
    return error("That username is unavailable or the invite was already used.", 409);
  }
  return json({ token, recoveryCode, user: { id, username } }, 201);
}

async function recover(request: Request, env: Env): Promise<Response> {
  const body = await readJson(request);
  const recoveryCode = requiredString(body.recoveryCode, "recovery code", 128);
  const user = await env.DB.prepare(
    "SELECT id, username FROM users WHERE recovery_hash = ? AND revoked_at IS NULL"
  ).bind(await sha256(recoveryCode)).first<{ id: string; username: string }>();
  if (!user) return error("Recovery code is invalid.", 401);
  const token = randomToken(32);
  await env.DB.prepare("UPDATE users SET token_hash = ? WHERE id = ?")
    .bind(await sha256(token), user.id).run();
  return json({ token, user });
}

async function authenticate(request: Request, env: Env): Promise<User | null> {
  const auth = request.headers.get("authorization");
  if (!auth?.startsWith("Bearer ")) return null;
  return env.DB.prepare(
    "SELECT id, username, role FROM users WHERE token_hash = ? AND revoked_at IS NULL"
  ).bind(await sha256(auth.slice(7))).first<User>();
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
  const contentLength = Number(request.headers.get("content-length")) || 0;
  if (contentLength > 2_300_000) return error("Upload is too large.", 413);
  const existing = await env.DB.prepare(
    "SELECT COUNT(*) AS count FROM characters WHERE owner_id = ? AND deleted_at IS NULL"
  ).bind(user.id).first<{ count: number }>();
  if ((existing?.count || 0) >= Number(env.MAX_CHARACTERS_PER_USER || 20)) {
    return error("Character publishing limit reached.", 409);
  }

  const body = await readJson(request) as CharacterUpload;
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
  const maxTotal = Number(env.MAX_TOTAL_STORAGE_BYTES || 5_000_000_000);
  const reservation = await env.DB.prepare(
    "UPDATE hub_state SET value = value + ? WHERE key = 'storage_bytes' AND value + ? <= ?"
  ).bind(sizeBytes, sizeBytes, maxTotal).run();
  if (!reservation.meta.changes) return error("The hub storage safety limit has been reached.", 507);

  const id = crypto.randomUUID();
  const revision = 1;
  const prefix = `characters/${id}/r${revision}`;
  const configKey = `${prefix}/config.json`;
  const avatarKey = avatar ? `${prefix}/avatar.${avatar.extension}` : null;
  const backgroundKey = background ? `${prefix}/background.${background.extension}` : null;
  try {
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
      env.DB.prepare("UPDATE users SET storage_bytes = storage_bytes + ? WHERE id = ?").bind(sizeBytes, user.id),
      env.DB.prepare("UPDATE hub_state SET value = value + 1 WHERE key = 'catalog_revision'")
    ]);
    return json({ id, revision }, 201);
  } catch (cause) {
    await Promise.all([configKey, avatarKey, backgroundKey].filter(Boolean).map((key) => env.MEDIA.delete(key!)));
    await env.DB.prepare("UPDATE hub_state SET value = MAX(0, value - ?) WHERE key = 'storage_bytes'")
      .bind(sizeBytes).run();
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
  await enforceRateLimit(request, env, `report:${user.id}`, 5, 86_400);
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

async function enforceRateLimit(request: Request, env: Env, scope: string, limit: number, seconds: number): Promise<void> {
  const ip = request.headers.get("cf-connecting-ip") || "local";
  const bucket = Math.floor(Date.now() / 1000 / seconds);
  const key = await sha256(`${scope}:${ip}`);
  await env.DB.prepare(`
    INSERT INTO rate_limits(key, bucket, count) VALUES (?, ?, 1)
    ON CONFLICT(key) DO UPDATE SET
      bucket = excluded.bucket,
      count = CASE WHEN rate_limits.bucket = excluded.bucket THEN rate_limits.count + 1 ELSE 1 END
  `).bind(key, bucket).run();
  const row = await env.DB.prepare("SELECT count FROM rate_limits WHERE key = ? AND bucket = ?")
    .bind(key, bucket).first<{ count: number }>();
  if ((row?.count || 0) > limit) throw new HttpError("Too many requests. Try again later.", 429);
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
  if (!request.headers.get("content-type")?.toLowerCase().includes("application/json")) throw new Error("Invalid content type.");
  try { return await request.json() as Record<string, any>; } catch { throw new Error("Invalid JSON body."); }
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
  return json({ error: message }, status);
}
