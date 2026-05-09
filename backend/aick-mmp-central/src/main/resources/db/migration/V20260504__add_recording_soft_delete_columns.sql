-- V20260504__add_recording_soft_delete_columns.sql
-- Add soft delete and orphaned tracking columns to recordings table
-- Part of improve-camera-module-integrity change

-- Add soft delete flag
ALTER TABLE recordings ADD COLUMN IF NOT EXISTS is_deleted BOOLEAN DEFAULT FALSE;

-- Add deleted_at timestamp (for cleanup audit)
ALTER TABLE recordings ADD COLUMN IF NOT EXISTS deleted_at TIMESTAMP NULL;

-- Add orphaned_at timestamp (when camera was deleted)
ALTER TABLE recordings ADD COLUMN IF NOT EXISTS orphaned_at TIMESTAMP NULL;

-- Add orphaned_by column (who triggered the orphaning, typically camera_id)
ALTER TABLE recordings ADD COLUMN IF NOT EXISTS orphaned_by BIGINT NULL;

-- Add index for soft delete queries (high efficiency boolean index)
CREATE INDEX IF NOT EXISTS idx_recordings_is_deleted ON recordings(is_deleted);

-- Add composite index for cleanup tasks (soft deleted + deleted_at)
CREATE INDEX IF NOT EXISTS idx_recordings_deleted_at ON recordings(is_deleted, deleted_at);

-- Add index for orphaned recordings queries
CREATE INDEX IF NOT EXISTS idx_recordings_orphaned_at ON recordings(orphaned_at);
