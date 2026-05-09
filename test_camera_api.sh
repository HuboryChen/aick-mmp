#!/bin/bash

echo "=== 测试摄像头 API ==="
echo ""

# 1. 首先测试登录（使用正确的路径）
echo "1. 测试登录..."
LOGIN_RESPONSE=$(curl -s -X POST "http://localhost:8080/api/auth/login" \
  -H "Content-Type: application/json" \
  -d '{"username":"testuser","password":"test123"}')

echo "登录响应: $LOGIN_RESPONSE"

# 提取 token
TOKEN=$(echo "$LOGIN_RESPONSE" | grep -o '"token":"[^"]*' | cut -d'"' -f4)

if [ -z "$TOKEN" ]; then
    echo "尝试使用 admin 账户..."
    LOGIN_RESPONSE=$(curl -s -X POST "http://localhost:8080/api/auth/login" \
      -H "Content-Type: application/json" \
      -d '{"username":"admin","password":"admin123"}')
    
    echo "Admin 登录响应: $LOGIN_RESPONSE"
    TOKEN=$(echo "$LOGIN_RESPONSE" | grep -o '"token":"[^"]*' | cut -d'"' -f4)
fi

if [ -z "$TOKEN" ]; then
    echo "错误: 无法获取 JWT token"
    echo "尝试直接测试摄像头 API..."
    
    # 直接测试摄像头 API，看看是否有认证错误
    echo ""
    echo "2. 直接测试摄像头 API (无认证)..."
    API_RESPONSE=$(curl -s -w "\nHTTP状态码: %{http_code}" -X GET "http://localhost:8080/api/cameras?page=0&size=10")
    echo "$API_RESPONSE"
    exit 1
fi

echo "获取到 token: ${TOKEN:0:20}..."

# 2. 测试摄像头 API
echo ""
echo "2. 测试摄像头 API (GET /api/cameras)..."
API_RESPONSE=$(curl -s -w "\n%{http_code}" -X GET "http://localhost:8080/api/cameras?page=0&size=10" \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json")

# 提取状态码和响应体
HTTP_CODE=$(echo "$API_RESPONSE" | tail -n1)
RESPONSE_BODY=$(echo "$API_RESPONSE" | head -n -1)

echo "HTTP 状态码: $HTTP_CODE"
echo "响应体: $RESPONSE_BODY"

# 3. 通过 Nginx 测试
echo ""
echo "3. 通过 Nginx 测试摄像头 API..."
NGINX_RESPONSE=$(curl -s -w "\n%{http_code}" -X GET "http://localhost:8090/api/cameras?page=0&size=10" \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json")

NGINX_HTTP_CODE=$(echo "$NGINX_RESPONSE" | tail -n1)
NGINX_RESPONSE_BODY=$(echo "$NGINX_RESPONSE" | head -n -1)

echo "Nginx HTTP 状态码: $NGINX_HTTP_CODE"
echo "Nginx 响应体: $NGINX_RESPONSE_BODY"

echo ""
echo "=== 测试完成 ==="