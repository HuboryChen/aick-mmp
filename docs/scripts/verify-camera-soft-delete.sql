-- ============================================
-- 摄像头软删除验证脚本
-- 用于验证 deleted_at 和 is_deleted 字段是否正确设置
-- ============================================

-- 1. 查看指定摄像头的软删除状态
-- 替换 {camera_id} 为你要检查的摄像头 ID
SELECT 
    id,
    name,
    deleted_at,
    is_deleted,
    updated_at,
    created_at
FROM cameras
WHERE id = {camera_id};

-- 2. 查看最近被"删除"的摄像头（按更新时间排序）
SELECT 
    id,
    name,
    deleted_at,
    is_deleted,
    updated_at
FROM cameras 
ORDER BY updated_at DESC 
LIMIT 20;

-- 3. 检查是否有不一致的数据（deleted_at 有值但 is_deleted 为 false）
SELECT 
    id,
    name,
    deleted_at,
    is_deleted
FROM cameras 
WHERE deleted_at IS NOT NULL AND is_deleted = false;

-- 4. 检查是否有不一致的数据（is_deleted 为 true 但 deleted_at 为空）
SELECT 
    id,
    name,
    deleted_at,
    is_deleted
FROM cameras 
WHERE is_deleted = true AND deleted_at IS NULL;

-- 5. 验证列表查询是否排除了已删除的摄像头
-- 这个查询应该返回 0 条记录（如果没有软删除的摄像头）
-- 如果返回记录，说明查询过滤有问题
SELECT COUNT(*) as still_visible_count
FROM cameras
WHERE deleted_at IS NOT NULL
   OR is_deleted = true;
