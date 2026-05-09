-- V20260411__migrate_system_api_keys.sql
-- Migrate existing system API keys to system_apps table

-- Copy system API key credentials to system_apps
UPDATE system_apps sa
SET 
    sa.app_key = ak.access_key,
    sa.encrypted_secret = ak.encrypted_secret
FROM api_keys ak
WHERE ak.app_id = sa.id 
    AND ak.key_type = 'SYSTEM'
    AND sa.owner_type = 'SYSTEM';
