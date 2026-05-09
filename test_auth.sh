#!/bin/bash

echo "=== 测试认证 API ==="
echo ""

# 1. 测试直接访问 central-1 容器
echo "1. 测试直接访问 central-1 容器..."
docker exec aick-mmp-central-1 curl -s -o /dev/null -w "HTTP状态码: %{http_code}\n" http://localhost:8080/api/auth/login 2>/dev/null || echo "无法访问"

# 2. 测试通过 Docker 网络访问
echo ""
echo "2. 测试通过 Docker 网络访问..."
# 获取 central-1 容器的 IP
CENTRAL_IP=$(docker inspect -f '{{range .NetworkSettings.Networks}}{{.IPAddress}}{{end}}' aick-mmp-central-1)
echo "central-1 IP: $CENTRAL_IP"
curl -s -o /dev/null -w "HTTP状态码: %{http_code}\n" http://$CENTRAL_IP:8080/api/auth/login 2>/dev/null || echo "无法访问"

# 3. 测试通过 Nginx 访问
echo ""
echo "3. 测试通过 Nginx 访问..."
curl -s -o /dev/null -w "HTTP状态码: %{http_code}\n" http://localhost:8090/api/auth/login 2>/dev/null || echo "无法访问"

# 4. 测试带请求体的登录请求
echo ""
echo "4. 测试带请求体的登录请求..."
curl -s -X POST http://localhost:8090/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{"username":"admin","password":"admin123"}' \
  -o /dev/null \
  -w "HTTP状态码: %{http_code}\n"

# 5. 检查容器日志
echo ""
echo "5. 检查 central-1 容器日志..."
docker logs aick-mmp-central-1 --tail 5 2>&1 | grep -i "auth\|login\|403\|filter"