-- 修复摄像头状态为NULL的记录
UPDATE cameras SET status = 'OFFLINE' WHERE status IS NULL;
