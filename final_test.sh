#!/bin/bash

echo "=== 最终测试摄像头 API ==="
echo ""

# 等待服务完全启动
echo "等待服务启动..."
sleep 10

# 1. 检查数据库中的摄像头数据
echo "1. 检查数据库中的摄像头数据..."
docker exec aick-mmp-mysql mysql -uaickuser -paickpassword -e "USE aick_mmp; SELECT id, name, location, status FROM cameras;" 2>/dev/null

# 2. 测试摄像头 API
echo ""
echo "2. 测试摄像头 API..."
curl -s -w "\nHTTP状态码: %{http_code}\n" "http://localhost:8090/api/cameras?page=0&size=10"

# 3. 如果返回 502，尝试直接访问后端
echo ""
echo "3. 尝试直接访问后端服务..."
curl -s -w "\nHTTP状态码: %{http_code}\n" "http://localhost:8080/api/cameras?page=0&size=10"

# 4. 检查服务日志
echo ""
echo "4. 检查服务日志中的错误..."
docker logs aick-mmp-central-1 --tail 20 2>&1 | grep -i "error\|exception\|camera"