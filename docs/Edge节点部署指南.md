# Edge节点部署指南

## 概述

本文档介绍Edge节点的部署流程，包括环境准备、安装配置、AK/SK认证设置和运维监控。

## 部署架构

```
┌─────────────────────────────────────────────────────────────────┐
│                        中央服务 (Central)                        │
│                     http://central:8080                         │
└─────────────────────────────────────────────────────────────────┘
                                  │
                                  │ HTTPS
                                  │ AK/SK认证
                                  ▼
┌─────────────────────────────────────────────────────────────────┐
│                      Edge节点 (Edge Node)                        │
│  ┌─────────────┐  ┌─────────────┐  ┌─────────────┐              │
│  │   Camera 1  │  │   Camera 2  │  │   Camera N  │              │
│  └─────────────┘  └─────────────┘  └─────────────┘              │
│         │                │                │                      │
│         └────────────────┼────────────────┘                      │
│                          ▼                                       │
│              ┌───────────────────────┐                           │
│              │    Edge Service      │                           │
│              │  - 心跳监控          │                           │
│              │  - 视频流管理        │                           │
│              │  - RTSP/RTMP转发     │                           │
│              └───────────────────────┘                           │
└─────────────────────────────────────────────────────────────────┘
```

## 前置条件

### 硬件要求

| 组件 | 最低配置 | 推荐配置 |
|------|----------|----------|
| CPU | 2核 | 4核+ |
| 内存 | 4GB | 8GB+ |
| 磁盘 | 100GB | 256GB+ SSD |
| 网络 | 100Mbps | 1Gbps |
| 摄像头支持 | 4路 | 16-32路 |

### 软件要求

- JDK 17+
- Docker (可选，用于容器化部署)
- FFmpeg (用于视频流处理)
- NTP服务 (用于时间同步)

## 部署步骤

### 1. 获取API凭证

首先，联系系统管理员获取以下凭证：

1. **SystemApp ID**：系统应用标识
2. **Access Key (AK)**：访问密钥，格式为 `ak_xxx`
3. **Secret Key (SK)**：密钥（仅创建时可见，请妥善保存）

### 2. 配置文件

创建或编辑 `config/application.yml`：

```yaml
server:
  port: 8090

spring:
  application:
    name: edge-node

# 中央服务配置
central:
  base-url: http://central-server:8080
  heartbeat:
    interval: 30s
    timeout: 120s

# Edge节点配置
edge:
  node:
    name: Edge-Beijing-01
    location: Beijing, China
    ip: ${HOST_IP:192.168.1.100}
    port: 8090
    max-camera-support: 16

# AK/SK认证配置
security:
  api-key:
    access-key: ak_your_access_key
    secret-key: sk_your_secret_key
```

### 3. 配置签名工具

Edge节点SDK已内置签名工具，配置AK/SK后自动处理：

```java
// 签名服务配置
@Configuration
public class SignatureConfig {
    
    @Value("${security.api-key.access-key}")
    private String accessKey;
    
    @Value("${security.api-key.secret-key}")
    private String secretKey;
    
    @Bean
    public SignatureService signatureService() {
        return new SignatureService(accessKey, secretKey);
    }
}
```

### 4. 启动Edge节点

#### Docker方式

```bash
docker run -d \
  --name edge-node \
  --network host \
  -e CENTRAL_BASE_URL=http://central-server:8080 \
  -e EDGE_NODE_NAME=Edge-Beijing-01 \
  -e EDGE_NODE_LOCATION="Beijing, China" \
  -e API_ACCESS_KEY=ak_your_access_key \
  -e API_SECRET_KEY=sk_your_secret_key \
  -v /opt/edge/config:/app/config \
  -v /opt/edge/logs:/app/logs \
  aick/edge-node:latest
```

#### 直接运行

```bash
java -jar edge-node.jar \
  --spring.config.location=file:/opt/edge/config/application.yml
```

## 心跳机制

### 心跳流程

```
Edge节点                              中央服务
    │                                      │
    │──── POST /api/edge/heartbeat ──────▶│
    │     Headers:                         │
    │     X-Access-Key: ak_xxx            │
    │     X-Signature: xxx                │
    │     X-Timestamp: 2026-04-05T10:00:00Z│
    │     Body:                           │
    │     {                               │
    │       "uuid": "edge-uuid",          │
    │       "status": "ONLINE",           │
    │       "cpuUsage": 45.5,             │
    │       "memoryUsage": 62.3,          │
    │       "diskUsage": 35.2,            │
    │       "cameraCount": 8,             │
    │       "uptime": 86400               │
    │     }                               │
    │                                      │
    │◀──── 200 OK ────────────────────────│
    │     { "serverTime": "2026-04-05..." }
    │                                      │
    │ (每30秒重复)                          │
```

### 自动心跳配置

```yaml
edge:
  heartbeat:
    enabled: true
    interval: 30s      # 发送间隔
    timeout: 120s      # 超时时间（中央服务判断离线）
    retry-count: 3     # 失败重试次数
    auto-start: true   # 启动时自动开始
```

### 手动心跳

```bash
# 手动发送心跳
curl -X POST http://localhost:8090/edge/heartbeat/manual

# 启动自动心跳
curl -X POST http://localhost:8090/edge/heartbeat/start

# 停止自动心跳
curl -X POST http://localhost:8090/edge/heartbeat/stop
```

## 运维监控

### 健康检查

```bash
# Edge节点健康状态
curl http://localhost:8090/edge/heartbeat/health

# Edge节点详细信息
curl http://localhost:8090/edge/heartbeat/status

# 网络指标
curl http://localhost:8090/edge/heartbeat/network-metrics
```

### 日志查看

```bash
# 查看实时日志
tail -f /opt/edge/logs/edge.log

# 查看心跳日志
grep -i heartbeat /opt/edge/logs/edge.log | tail -50

# 查看认证错误日志
grep -i "auth\|signature\|401" /opt/edge/logs/edge.log
```

### 常见问题排查

#### 1. 心跳认证失败

```bash
# 检查AK/SK配置
grep -E "access-key|secret-key" /opt/edge/config/application.yml

# 验证签名
# 访问 http://localhost:8090/test-signature.html 进行测试
```

#### 2. 中央服务连接超时

```bash
# 检查网络连通性
curl -v http://central-server:8080/actuator/health

# 检查防火墙规则
iptables -L -n | grep 8080
```

#### 3. 节点显示离线

检查中央服务日志：
```bash
# 查看节点状态变更日志
grep "edge.*offline\|edge.*online" /opt/central/logs/central.log
```

## Docker Compose部署示例

```yaml
version: '3.8'

services:
  edge-node:
    image: aick/edge-node:latest
    container_name: edge-node-01
    restart: unless-stopped
    network_mode: host
    environment:
      - SPRING_PROFILES_ACTIVE=prod
      - CENTRAL_BASE_URL=http://central-server:8080
      - API_ACCESS_KEY=${API_ACCESS_KEY}
      - API_SECRET_KEY=${API_SECRET_KEY}
      - EDGE_NODE_NAME=${EDGE_NODE_NAME}
      - EDGE_NODE_LOCATION=${EDGE_NODE_LOCATION}
    volumes:
      - ./config:/app/config
      - ./logs:/app/logs
      - /dev:/dev
    logging:
      driver: "json-file"
      options:
        max-size: "50m"
        max-file: "5"
```

## 安全配置

### 启用HTTPS

```yaml
server:
  port: 8443
  ssl:
    enabled: true
    key-store: classpath:keystore.p12
    key-store-password: ${SSL_PASSWORD}
    key-store-type: PKCS12
    key-alias: edge-node
```

### 网络隔离

```bash
# 仅允许与中央服务通信
iptables -A OUTPUT -d central-server -p tcp --dport 8080 -j ACCEPT
iptables -A OUTPUT -j DROP
```

## 备份与恢复

### 配置备份

```bash
# 备份配置文件
cp -r /opt/edge/config /backup/edge-config-$(date +%Y%m%d)

# 备份日志
tar -czf /backup/edge-logs-$(date +%Y%m%d).tar.gz /opt/edge/logs
```

### 恢复配置

```bash
# 恢复配置
cp -r /backup/edge-config-20260405/* /opt/edge/config/

# 重启服务
systemctl restart edge-node
```

## 技术支持

- 管理员邮箱：admin@aick.com
- 技术支持热线：400-xxx-xxxx
- 在线文档：https://docs.aick.com/edge
