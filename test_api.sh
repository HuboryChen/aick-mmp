#!/bin/bash

echo "=== 测试摄像头 API ==="
echo ""

# 1. 首先登录获取 JWT token
echo "1. 登录获取 JWT token..."
LOGIN_RESPONSE=$(curl -s -X POST "http://localhost:8090/auth/login" \
  -H "Content-Type: application/json" \
  -d '{"username":"admin","password":"admin123"}')

echo "登录响应: $LOGIN_RESPONSE"

# 提取 token
TOKEN=$(echo "$LOGIN_RESPONSE" | grep -o '"token":"[^"]*' | cut -d'"' -f4)

if [ -z "$TOKEN" ]; then
    echo "无法获取 token，尝试使用默认测试用户..."
    # 创建测试用户
    curl -s -X POST "http://localhost:8090/api/users" \
      -H "Content-Type: application/json" \
      -d '{"username":"testuser","password":"test123","email":"test@test.com","role":"OPERATOR"}' > /dev/null
    
    # 再次尝试登录
    LOGIN_RESPONSE=$(curl -s -X POST "http://localhost:8090/auth/login" \
      -H "Content-Type: application/json" \
      -d '{"username":"testuser","password":"test123"}')
    
    TOKEN=$(echo "$LOGIN_RESPONSE" | grep -o '"token":"[^"]*' | cut -d'"' -f4)
fi

if [ -z "$TOKEN" ]; then
    echo "错误: 无法获取 JWT token"
    exit 1
fi

echo "获取到 token: ${TOKEN:0:20}..."

# 2. 测试摄像头 API
echo ""
echo "2. 测试摄像头 API (GET /api/cameras)..."
API_RESPONSE=$(curl -s -w "\n%{http_code}" -X GET "http://localhost:8090/api/cameras?page=0&size=10" \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json")

# 提取状态码和响应体
HTTP_CODE=$(echo "$API_RESPONSE" | tail -n1)
RESPONSE_BODY=$(echo "$API_RESPONSE" | head -n -1)

echo "HTTP 状态码: $HTTP_CODE"
echo "响应体: $RESPONSE_BODY"

# 3. 检查数据库中的摄像头数据
echo ""
echo "3. 检查数据库中的摄像头数据..."
docker exec aick-mmp-mysql mysql -uaickuser -paickpassword -e "USE aick_mmp; SELECT id, name, location, status FROM cameras;" 2>/dev/null || echo "无法查询数据库"

# 4. 如果表为空，插入测试数据
echo ""
echo "4. 检查是否需要插入测试数据..."
CAMERA_COUNT=$(docker exec aick-mmp-mysql mysql -uaickuser -paickpassword -sN -e "USE aick_mmp; SELECT COUNT(*) FROM cameras;" 2>/dev/null || echo "0")

if [ "$CAMERA_COUNT" -eq "0" ]; then
    echo "摄像头表为空，插入测试数据..."
    
    # 检查是否有 edge_node
    EDGE_NODE_COUNT=$(docker exec aick-mmp-mysql mysql -uaickuser -paickpassword -sN -e "USE aick_mmp; SELECT COUNT(*) FROM edge_nodes;" 2>/dev/null || echo "0")
    
    if [ "$EDGE_NODE_COUNT" -eq "0" ]; then
        echo "先插入 edge_node 数据..."
        docker exec aick-mmp-mysql mysql -uaickuser -paickpassword -e "USE aick_mmp; \
          INSERT INTO edge_nodes (name, ip, port, location, max_camera_support, status, uuid) VALUES \
          ('test-node-1', '192.168.1.100', 8083, '测试区域', 10, 'ONLINE', 'test-uuid-001');" 2>/dev/null
    fi
    
    # 插入摄像头数据
    echo "插入摄像头数据..."
    docker exec aick-mmp-mysql mysql -uaickuser -paickpassword -e "USE aick_mmp; \
      INSERT INTO cameras (name, location, protocol, connection_url, edge_node_id, resolution, status, is_enabled) VALUES \
      ('测试摄像头1', '入口大厅', 'RTSP', 'rtsp://test-camera-1:554/stream', 1, '1920x1080', 'OFFLINE', 1), \
      ('测试摄像头2', '停车场', 'RTSP', 'rtsp://test-camera-2:554/stream', 1, '1280x720', 'ONLINE', 1);" 2>/dev/null
    
    echo "测试数据已插入"
fi

echo ""
echo "=== 测试完成 ==="