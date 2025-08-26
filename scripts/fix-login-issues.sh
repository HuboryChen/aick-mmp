#!/bin/bash

# AICK-MMP 登录问题修复脚本
# 作者: AICK团队
# 日期: $(date)

set -e

echo "🚀 开始诊断和修复AICK-MMP登录问题..."

# 检查Docker容器状态
echo "📋 检查Docker容器状态..."
docker-compose ps

# 检查MySQL连接
echo "🔍 检查MySQL数据库连接..."
docker-compose exec mysql mysql -u root -prootpassword -e "SELECT 1;" || {
    echo "❌ MySQL数据库连接失败，正在重启MySQL容器..."
    docker-compose restart mysql
    sleep 10
}

# 检查数据库和表
echo "🔍 检查用户表和数据..."
docker-compose exec mysql mysql -u aickuser -paickpassword aick_mmp -e "
    SHOW TABLES;
    SELECT COUNT(*) as user_count FROM users;
    SELECT username, role, status, enabled FROM users LIMIT 5;
" || {
    echo "❌ 用户表不存在或数据有问题"
    exit 1
}

# 检查后端日志
echo "📄 检查后端服务日志..."
docker-compose logs backend-1 --tail=20

# 测试登录API
echo "🧪 测试登录API..."
curl -X POST http://localhost:8080/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{"username":"admin","password":"admin123"}' \
  -w "\nHTTP状态码: %{http_code}\n" || {
    echo "❌ 登录API测试失败"
}

# 检查前端连接
echo "🌐 检查前端服务..."
curl -I http://localhost:80 || {
    echo "❌ 前端服务无法访问"
}

echo "✅ 诊断完成！"
echo ""
echo "🔧 如果发现问题，请尝试以下解决方案："
echo "1. 重启所有服务: docker-compose restart"
echo "2. 重新构建并启动: docker-compose up --build -d"
echo "3. 清理并重新部署: docker-compose down && docker-compose up -d"
echo "4. 检查网络连接: docker network ls"
echo "5. 查看详细日志: docker-compose logs -f"