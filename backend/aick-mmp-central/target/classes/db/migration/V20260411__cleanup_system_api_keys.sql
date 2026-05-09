-- V20260411__cleanup_system_api_keys.sql
-- Remove SYSTEM type records from api_keys table after migration

-- Delete all SYSTEM type API keys (credentials are now in system_apps)
DELETE FROM api_keys WHERE key_type = 'SYSTEM';
