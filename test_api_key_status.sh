#!/bin/bash

echo "=========================================="
echo "API Key 状态更新功能测试"
echo "=========================================="

# 登录获取 Token
echo -e "\n[1/6] 登录获取 Token..."
LOGIN_RESPONSE=$(curl -s -X POST http://localhost/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{"username":"admin","password":"admin123"}')

TOKEN=$(echo $LOGIN_RESPONSE | python3 -c "import json, sys; print(json.load(sys.stdin)['token'])" 2>/dev/null)

if [ -z "$TOKEN" ]; then
  echo "❌ 登录失败"
  echo "$LOGIN_RESPONSE"
  exit 1
fi

echo "✅ 登录成功"
echo "Token: ${TOKEN:0:50}..."

# 创建新的 API Key
echo -e "\n[2/6] 创建新的 API Key..."
CREATE_RESPONSE=$(curl -s -X POST http://localhost/api/api-keys/me \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" \
  -d '{"name":"测试启用禁用功能","status":"ENABLED"}')

KEY_ID=$(echo $CREATE_RESPONSE | python3 -c "import json, sys; data=json.load(sys.stdin); print(data.get('accessKey', 'error'))" 2>/dev/null)

echo "✅ API Key 创建成功"
echo "$CREATE_RESPONSE" | python3 -m json.tool

# 获取 API Key ID
echo -e "\n[3/6] 获取 API Key ID..."
KEY_ID=$(curl -s http://localhost/api/api-keys/me \
  -H "Authorization: Bearer $TOKEN" | \
  python3 -c "import json, sys; keys=json.load(sys.stdin); print([k['id'] for k in keys if '测试启用禁用功能' in k['name']][0])" 2>/dev/null)

if [ -z "$KEY_ID" ]; then
  echo "❌ 获取 API Key ID 失败"
  exit 1
fi

echo "✅ API Key ID: $KEY_ID"

# 测试禁用功能
echo -e "\n[4/6] 测试禁用功能..."
HTTP_CODE=$(curl -s -o /dev/null -w "%{http_code}" -X PUT "http://localhost/api/api-keys/me/$KEY_ID/status" \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" \
  -d '{"status":"DISABLED"}')

if [ "$HTTP_CODE" = "200" ]; then
  echo "✅ 禁用请求成功 (HTTP $HTTP_CODE)"
else
  echo "❌ 禁用请求失败 (HTTP $HTTP_CODE)"
fi

# 验证状态
echo -e "\n[5/6] 验证状态已更新为 DISABLED..."
STATUS=$(curl -s http://localhost/api/api-keys/me \
  -H "Authorization: Bearer $TOKEN" | \
  python3 -c "import json, sys; keys=json.load(sys.stdin); print([k['status'] for k in keys if k['id']==$KEY_ID][0])" 2>/dev/null)

if [ "$STATUS" = "DISABLED" ]; then
  echo "✅ 状态验证成功: $STATUS"
else
  echo "❌ 状态验证失败: $STATUS (期望: DISABLED)"
fi

# 测试重新启用功能
echo -e "\n[6/6] 测试重新启用功能..."
HTTP_CODE=$(curl -s -o /dev/null -w "%{http_code}" -X PUT "http://localhost/api/api-keys/me/$KEY_ID/status" \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" \
  -d '{"status":"ENABLED"}')

if [ "$HTTP_CODE" = "200" ]; then
  echo "✅ 启用请求成功 (HTTP $HTTP_CODE)"
else
  echo "❌ 启用请求失败 (HTTP $HTTP_CODE)"
fi

# 最终验证
STATUS=$(curl -s http://localhost/api/api-keys/me \
  -H "Authorization: Bearer $TOKEN" | \
  python3 -c "import json, sys; keys=json.load(sys.stdin); print([k['status'] for k in keys if k['id']==$KEY_ID][0])" 2>/dev/null)

if [ "$STATUS" = "ENABLED" ]; then
  echo "✅ 状态验证成功: $STATUS"
else
  echo "❌ 状态验证失败: $STATUS (期望: ENABLED)"
fi

echo -e "\n=========================================="
echo "测试完成！"
echo "=========================================="
