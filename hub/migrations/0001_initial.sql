PRAGMA foreign_keys = ON;

CREATE TABLE users (
    id TEXT PRIMARY KEY,
    username TEXT NOT NULL COLLATE NOCASE UNIQUE,
    token_hash TEXT NOT NULL UNIQUE,
    recovery_hash TEXT NOT NULL UNIQUE,
    role TEXT NOT NULL DEFAULT 'member' CHECK (role IN ('member', 'admin')),
    storage_bytes INTEGER NOT NULL DEFAULT 0 CHECK (storage_bytes >= 0),
    created_at INTEGER NOT NULL,
    revoked_at INTEGER
);

CREATE TABLE invites (
    id TEXT PRIMARY KEY,
    code_hash TEXT NOT NULL UNIQUE,
    created_at INTEGER NOT NULL,
    expires_at INTEGER,
    used_at INTEGER,
    used_by TEXT REFERENCES users(id),
    created_by TEXT
);

CREATE TABLE characters (
    id TEXT PRIMARY KEY,
    owner_id TEXT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    revision INTEGER NOT NULL DEFAULT 1,
    display_name TEXT NOT NULL,
    tagline TEXT NOT NULL DEFAULT '',
    author TEXT NOT NULL DEFAULT '',
    description TEXT NOT NULL DEFAULT '',
    tags_json TEXT NOT NULL DEFAULT '[]',
    config_key TEXT NOT NULL,
    avatar_key TEXT,
    background_key TEXT,
    size_bytes INTEGER NOT NULL CHECK (size_bytes >= 0),
    downloads INTEGER NOT NULL DEFAULT 0,
    favorites INTEGER NOT NULL DEFAULT 0,
    nsfw INTEGER NOT NULL DEFAULT 0 CHECK (nsfw IN (0, 1)),
    visibility TEXT NOT NULL DEFAULT 'public' CHECK (visibility IN ('public', 'unlisted', 'hidden')),
    created_at INTEGER NOT NULL,
    updated_at INTEGER NOT NULL,
    deleted_at INTEGER
);

CREATE INDEX characters_public_updated_idx
ON characters(visibility, deleted_at, updated_at DESC);
CREATE INDEX characters_owner_idx ON characters(owner_id, deleted_at);

CREATE TABLE favorites (
    user_id TEXT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    character_id TEXT NOT NULL REFERENCES characters(id) ON DELETE CASCADE,
    created_at INTEGER NOT NULL,
    PRIMARY KEY (user_id, character_id)
);

CREATE TABLE reports (
    id TEXT PRIMARY KEY,
    reporter_id TEXT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    character_id TEXT NOT NULL REFERENCES characters(id) ON DELETE CASCADE,
    reason TEXT NOT NULL,
    created_at INTEGER NOT NULL,
    status TEXT NOT NULL DEFAULT 'open' CHECK (status IN ('open', 'resolved', 'dismissed')),
    UNIQUE (reporter_id, character_id)
);

CREATE TABLE hub_state (
    key TEXT PRIMARY KEY,
    value INTEGER NOT NULL
);
INSERT INTO hub_state(key, value) VALUES ('storage_bytes', 0);
INSERT INTO hub_state(key, value) VALUES ('catalog_revision', 1);

CREATE TABLE rate_limits (
    key TEXT PRIMARY KEY,
    bucket INTEGER NOT NULL,
    count INTEGER NOT NULL
);
