-- V20260411__merge_system_app_key.sql
-- Add credentials fields to system_apps table

-- Add app_key field (if not exists from previous schema)
ALTER TABLE system_apps ADD COLUMN IF NOT EXISTS app_key VARCHAR(64) UNIQUE;

-- Add encrypted_secret field for storing system app credentials
ALTER TABLE system_apps ADD COLUMN IF NOT EXISTS encrypted_secret VARCHAR(256);

-- Add last_used_at field for tracking usage
ALTER TABLE system_apps ADD COLUMN IF NOT EXISTS last_used_at DATETIME;

-- Create index for app_key lookup
CREATE INDEX IF NOT EXISTS idx_system_apps_app_key ON system_apps(app_key);
