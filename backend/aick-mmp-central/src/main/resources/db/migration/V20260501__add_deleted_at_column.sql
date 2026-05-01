-- V20260501__add_deleted_at_column.sql
-- Add deleted_at column for soft delete support

-- Add deleted_at column if not exists
ALTER TABLE cameras ADD COLUMN IF NOT EXISTS deleted_at TIMESTAMP NULL;

-- Add index for soft delete queries
CREATE INDEX IF NOT EXISTS idx_cameras_deleted_at ON cameras(deleted_at);
