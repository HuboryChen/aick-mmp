-- 添加录像存储相关字段
ALTER TABLE recordings 
ADD COLUMN IF NOT EXISTS file_size BIGINT DEFAULT NULL,
ADD COLUMN IF NOT EXISTS md5 VARCHAR(32) DEFAULT NULL,
ADD COLUMN IF NOT EXISTS storage_path VARCHAR(500) DEFAULT NULL,
ADD COLUMN IF NOT EXISTS integrity_status VARCHAR(20) DEFAULT 'PENDING',
ADD COLUMN IF NOT EXISTS lock_status BOOLEAN DEFAULT FALSE;

-- 添加索引
CREATE INDEX IF NOT EXISTS idx_recording_integrity_status ON recordings(integrity_status);
CREATE INDEX IF NOT EXISTS idx_recording_lock_status ON recordings(lock_status);
