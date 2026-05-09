# API Key 使用指南

## 概述

本系统支持两种认证方式：
- **JWT认证**：适用于前端用户登录
- **AK/SK认证**：适用于Edge节点和系统间通信

本文档介绍AK/SK（Access Key/Secret Key）认证方式的使用方法。

## 认证原理

### 签名流程

```
┌─────────────┐     1. 构造签名字符串      ┌─────────────────┐
│  Edge节点   │ ─────────────────────────▶ │ StringToSign    │
│             │                            │ = METHOD + \n   │
│             │                            │   PATH + \n     │
│             │                            │   TIMESTAMP     │
└─────────────┘                            └────────┬────────┘
                                                      │
                                                      │ 2. HMAC-SHA256签名
                                                      ▼
┌─────────────┐     4. 发送请求              ┌─────────────────┐
│  中央服务   │ ◀───────────────────────── │ Signature      │
│             │   (Authorization Headers)  │ (Base64编码)    │
└─────────────┘                            └────────┬────────┘
                                                      │
                                                      │ 3. 使用SecretKey加密
                                                      ▼
                                             ┌─────────────────┐
                                             │ Signature =     │
                                             │ Base64(HMAC-     │
                                             │ SHA256(StringTo │
                                             │ Sign, SecretKey))│
                                             └─────────────────┘
```

### 签名字符串构造规则

```
StringToSign = HTTP_METHOD + "\n" + REQUEST_PATH + "\n" + TIMESTAMP

示例:
POST
/api/edge/register
2026-04-05T10:00:00Z
```

## 认证头信息

使用AK/SK认证时，需要在HTTP请求头中包含以下信息：

| 头信息 | 必填 | 描述 |
|--------|------|------|
| X-Access-Key | 是 | 访问密钥ID，格式：`ak_xxx` |
| X-Signature | 是 | 请求签名，Base64编码的HMAC-SHA256结果 |
| X-Timestamp | 是 | 请求时间戳，ISO 8601格式：`2026-04-05T10:00:00Z` |

## 请求示例

### Edge节点注册

```bash
curl -X POST http://central-server/api/edge/register \
  -H "Content-Type: application/json" \
  -H "X-Access-Key: ak_your_access_key" \
  -H "X-Signature: your_base64_signature" \
  -H "X-Timestamp: 2026-04-05T10:00:00Z" \
  -d '{
    "name": "Edge-Beijing-01",
    "location": "Beijing, China",
    "ipAddress": "192.168.1.100",
    "port": 8080,
    "maxCameraSupport": 16
  }'
```

### 签名字符串构造示例（Python）

```python
import hmac
import hashlib
import base64
from datetime import datetime, timezone

def build_string_to_sign(method, path, timestamp):
    """构造签名字符串"""
    return f"{method}\n{path}\n{timestamp}"

def compute_signature(string_to_sign, secret_key):
    """计算HMAC-SHA256签名"""
    signature = hmac.new(
        secret_key.encode('utf-8'),
        string_to_sign.encode('utf-8'),
        hashlib.sha256
    ).digest()
    return base64.b64encode(signature).decode('utf-8')

def sign_request(method, path, access_key, secret_key):
    """生成完整的签名请求头"""
    timestamp = datetime.now(timezone.utc).strftime('%Y-%m-%dT%H:%M:%SZ')
    
    string_to_sign = build_string_to_sign(method, path, timestamp)
    signature = compute_signature(string_to_sign, secret_key)
    
    return {
        'X-Access-Key': access_key,
        'X-Timestamp': timestamp,
        'X-Signature': signature
    }

# 使用示例
headers = sign_request(
    method='POST',
    path='/api/edge/register',
    access_key='ak_your_access_key',
    secret_key='your_secret_key'
)

print(headers)
# {'X-Access-Key': 'ak_your_access_key', 
#  'X-Timestamp': '2026-04-05T10:00:00Z', 
#  'X-Signature': 'base64_encoded_signature'}
```

### 签名字符串构造示例（Java）

```java
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Base64;

public class SignatureUtil {
    
    public static String buildStringToSign(String method, String path, String timestamp) {
        return method + "\n" + path + "\n" + timestamp;
    }
    
    public static String computeSignature(String stringToSign, String secretKey) {
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            SecretKeySpec keySpec = new SecretKeySpec(
                secretKey.getBytes(StandardCharsets.UTF_8), "HmacSHA256"
            );
            mac.init(keySpec);
            byte[] signature = mac.doFinal(stringToSign.getBytes(StandardCharsets.UTF_8));
            return Base64.getEncoder().encodeToString(signature);
        } catch (Exception e) {
            throw new RuntimeException("Failed to compute signature", e);
        }
    }
    
    public static Map<String, String> signRequest(String method, String path, 
                                                   String accessKey, String secretKey) {
        String timestamp = Instant.now().toString();
        String stringToSign = buildStringToSign(method, path, timestamp);
        String signature = computeSignature(stringToSign, secretKey);
        
        Map<String, String> headers = new HashMap<>();
        headers.put("X-Access-Key", accessKey);
        headers.put("X-Timestamp", timestamp);
        headers.put("X-Signature", signature);
        
        return headers;
    }
}
```

## API Key管理

### 管理接口（需JWT认证）

| 接口 | 方法 | 描述 | 权限 |
|------|------|------|------|
| `/api-keys/me` | GET | 获取当前用户的API Key列表 | 登录用户 |
| `/api-keys/me` | POST | 为当前用户创建API Key | 登录用户 |
| `/api-keys/me/{id}/status` | PUT | 更新API Key状态 | 登录用户 |
| `/api-keys/me/{id}` | DELETE | 删除API Key | 登录用户 |
| `/api-keys/system` | GET | 获取系统级API Key列表 | 管理员 |
| `/api-keys/system` | POST | 创建系统级API Key | 管理员 |
| `/api-keys/system/{id}/status` | PUT | 更新系统级API Key状态 | 管理员 |
| `/api-keys/system/{id}` | DELETE | 删除系统级API Key | 管理员 |

### 创建API Key请求体

```json
{
  "name": "Production Edge Key",
  "description": "用于生产环境的Edge节点",
  "type": "SYSTEM",
  "expiresAt": "2027-01-01T00:00:00Z"
}
```

### 响应体

```json
{
  "id": 1,
  "name": "Production Edge Key",
  "accessKey": "ak_aick_xxxxxxxxxxxx",
  "secretKey": "sk_xxxxxxxxxxxx",  // 仅在创建时返回一次
  "type": "SYSTEM",
  "status": "ENABLED",
  "expiresAt": "2027-01-01T00:00:00Z",
  "createdAt": "2026-04-05T10:00:00Z"
}
```

## 安全建议

### 1. Secret Key保护

- **绝不**在代码库中硬编码Secret Key
- 使用环境变量或密钥管理服务（如AWS Secrets Manager）
- 在传输过程中使用HTTPS

### 2. 密钥轮换

- 建议定期更换Secret Key
- 创建新Key后，逐步迁移服务
- 旧Key在确认无误后删除

### 3. 权限控制

- 为不同用途创建独立的API Key
- Edge节点只授予必要的权限（如EDGE_REGISTER）
- 定期审计未使用的API Key

### 4. 时间戳容差

- 服务器接受±5分钟的请求时间戳
- 确保Edge节点时间同步（使用NTP）

## 错误处理

### 常见错误码

| HTTP状态码 | 错误信息 | 原因 |
|-----------|----------|------|
| 401 | Invalid access key format | Access Key格式错误 |
| 401 | Invalid access key | Access Key不存在 |
| 401 | Timestamp expired or invalid | 时间戳超出容差范围 |
| 401 | Invalid signature | 签名验证失败 |
| 403 | API key is disabled | API Key已被禁用 |
| 403 | API key has expired | API Key已过期 |
| 403 | System app does not have EDGE_REGISTER permission | System App缺少必要权限 |
| 429 | Rate limit exceeded | 请求频率超限 |

### 错误响应示例

```json
{
  "message": "Invalid signature",
  "timestamp": "2026-04-05T10:00:00Z",
  "path": "/api/edge/register"
}
```

## 集成测试

使用提供的测试脚本验证配置：

```bash
# 测试API Key认证
./test_api.sh

# 测试签名前端工具
./frontend/test-signature.html
```

## 技术支持

如有问题，请联系系统管理员或查看：
- [架构文档](../ARCHITECTURE.md)
- [Edge节点部署指南](./Edge节点部署指南.md)
