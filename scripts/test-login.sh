#!/bin/bash

# AICK-MMP 登录功能测试脚本
echo "🧪 开始测试AICK-MMP登录功能..."

# 设置基础URL
BASE_URL="http://localhost:8080"
FRONTEND_URL="http://localhost"

# 颜色定义
GREEN='\033[0;32m'
RED='\033[0;31m'
YELLOW='\033[1;33m'
NC='\033[0m' # No Color

# 测试函数
test_endpoint() {
    local url=$1
    local expected_status=$2
    local description=$3
    
    echo -n "Testing $description... "
    
    status=$(curl -s -o /dev/null -w "%{http_code}" "$url")
    
    if [ "$status" = "$expected_status" ]; then
        echo -e "${GREEN}✓ PASS${NC} (Status: $status)"
        return 0
    else
        echo -e "${RED}✗ FAIL${NC} (Expected: $expected_status, Got: $status)"
        return 1
    fi
}

# 测试带数据的POST请求
test_login() {
    local username=$1
    local password=$2
    local description=$3
    
    echo -n "Testing $description... "
    
    response=$(curl -s -w "\n%{http_code}" -X POST "$BASE_URL/api/auth/login" \
        -H "Content-Type: application/json" \
        -d "{\"username\":\"$username\",\"password\":\"$password\"}")
    
    # 分离响应体和状态码
    status=$(echo "$response" | tail -1)
    body=$(echo "$response" | head -n -1)
    
    if [ "$status" = "200" ]; then
        # 检查是否包含token
        if echo "$body" | grep -q "token"; then
            echo -e "${GREEN}✓ PASS${NC} (Login successful, token received)"
            # 提取token用于后续测试
            TOKEN=$(echo "$body" | grep -o '"token":"[^"]*"' | cut -d'"' -f4)
            return 0
        else
            echo -e "${RED}✗ FAIL${NC} (Status 200 but no token in response)"
            echo "Response: $body"
            return 1
        fi
    else
        echo -e "${RED}✗ FAIL${NC} (Status: $status)"
        echo "Response: $body"
        return 1
    fi
}

# 测试认证端点
test_authenticated_endpoint() {
    local token=$1
    local description=$2
    
    echo -n "Testing $description... "
    
    if [ -z "$token" ]; then
        echo -e "${RED}✗ FAIL${NC} (No token available)"
        return 1
    fi
    
    response=$(curl -s -w "\n%{http_code}" -X GET "$BASE_URL/api/auth/me" \
        -H "Authorization: Bearer $token")
    
    status=$(echo "$response" | tail -1)
    body=$(echo "$response" | head -n -1)
    
    if [ "$status" = "200" ]; then
        echo -e "${GREEN}✓ PASS${NC} (Authenticated request successful)"
        return 0
    else
        echo -e "${RED}✗ FAIL${NC} (Status: $status)"
        echo "Response: $body"
        return 1
    fi
}

echo "==================== 服务状态检查 ===================="

# 检查后端服务状态
test_endpoint "$BASE_URL/actuator/health" "200" "Backend Health Check"

# 检查前端服务状态  
test_endpoint "$FRONTEND_URL" "200" "Frontend Service"

echo ""
echo "==================== API端点测试 ===================="

# 测试登录API - 错误凭据
echo -n "Testing Login with invalid credentials... "
response=$(curl -s -w "\n%{http_code}" -X POST "$BASE_URL/api/auth/login" \
    -H "Content-Type: application/json" \
    -d '{"username":"invalid","password":"invalid"}')

status=$(echo "$response" | tail -1)
if [ "$status" = "401" ] || [ "$status" = "400" ] || [ "$status" = "500" ]; then
    echo -e "${GREEN}✓ PASS${NC} (Correctly rejected invalid credentials)"
else
    echo -e "${RED}✗ FAIL${NC} (Status: $status, should reject invalid credentials)"
fi

# 测试登录API - 正确凭据
test_login "admin" "admin123" "Login with valid credentials (admin/admin123)"

# 如果登录成功，测试认证端点
if [ ! -z "$TOKEN" ]; then
    test_authenticated_endpoint "$TOKEN" "Get current user with valid token"
    
    # 测试token验证端点
    echo -n "Testing Token validation... "
    response=$(curl -s -w "\n%{http_code}" -X POST "$BASE_URL/api/auth/validate" \
        -d "token=$TOKEN")
    status=$(echo "$response" | tail -1)
    
    if [ "$status" = "200" ]; then
        echo -e "${GREEN}✓ PASS${NC}"
    else
        echo -e "${RED}✗ FAIL${NC} (Status: $status)"
    fi
fi

echo ""
echo "==================== 数据库检查 ===================="

# 检查用户数据
echo "Checking user data in database..."
docker-compose exec -T mysql mysql -u aickuser -paickpassword aick_mmp -e "
SELECT 
    username, 
    email, 
    role, 
    status, 
    enabled,
    created_at 
FROM users 
ORDER BY created_at DESC 
LIMIT 5;
" 2>/dev/null || echo -e "${RED}✗ Cannot connect to database${NC}"

echo ""
echo "==================== 总结 ===================="

echo -e "${YELLOW}如果测试失败，请检查以下几点：${NC}"
echo "1. 确保所有Docker容器正在运行: docker-compose ps"
echo "2. 检查后端日志: docker-compose logs backend-1"
echo "3. 检查数据库连接: docker-compose logs mysql"
echo "4. 验证网络配置: docker network ls"
echo "5. 重启服务: docker-compose restart"

echo ""
echo "🔧 常用调试命令："
echo "- 查看所有日志: docker-compose logs -f"
echo "- 重建容器: docker-compose up --build -d"
echo "- 进入后端容器: docker-compose exec backend-1 bash"
echo "- 进入MySQL: docker-compose exec mysql mysql -u aickuser -paickpassword aick_mmp"

echo -e "\n${GREEN}测试完成！${NC}"