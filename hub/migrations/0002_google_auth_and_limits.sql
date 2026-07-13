ALTER TABLE users ADD COLUMN google_sub TEXT;
ALTER TABLE users ADD COLUMN google_avatar_url TEXT;

CREATE UNIQUE INDEX users_google_sub_idx
ON users(google_sub)
WHERE google_sub IS NOT NULL;

CREATE TABLE auth_sessions (
    id TEXT PRIMARY KEY,
    user_id TEXT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    token_hash TEXT NOT NULL UNIQUE,
    created_at INTEGER NOT NULL,
    expires_at INTEGER NOT NULL,
    revoked_at INTEGER
);

CREATE INDEX auth_sessions_user_idx
ON auth_sessions(user_id, revoked_at, expires_at);

CREATE TABLE pending_uploads (
    id TEXT PRIMARY KEY,
    owner_id TEXT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    expected_bytes INTEGER NOT NULL CHECK (expected_bytes >= 0),
    config_key TEXT NOT NULL,
    avatar_key TEXT,
    background_key TEXT,
    created_at INTEGER NOT NULL,
    status TEXT NOT NULL DEFAULT 'pending' CHECK (status IN ('pending', 'complete'))
);

CREATE INDEX pending_uploads_created_idx
ON pending_uploads(status, created_at);

ALTER TABLE rate_limits ADD COLUMN expires_at INTEGER NOT NULL DEFAULT 0;
CREATE INDEX rate_limits_expires_idx ON rate_limits(expires_at);

INSERT OR IGNORE INTO hub_state(key, value) VALUES ('uploads_enabled', 1);
INSERT OR IGNORE INTO hub_state(key, value) VALUES ('storage_warning_bytes', 4000000000);
INSERT OR IGNORE INTO hub_state(key, value) VALUES ('storage_media_stop_bytes', 4500000000);
INSERT OR IGNORE INTO hub_state(key, value) VALUES ('storage_stop_bytes', 4750000000);

DROP TABLE invites;
