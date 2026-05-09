# AICM-MMP 远程部署指南

## 部署方式选择

### 方式一：使用提供的脚本部署（推荐）

你需要先配置 SSH 无密码登录：

```bash
# 1. 生成 SSH 密钥（如果还没有）
ssh-keygen -t rsa

# 2. 复制公钥到远程主机
ssh-copy-id root@39.106.143.240

# 3. 然后运行部署脚本
bash deploy-remote-build.sh 39.106.143.240 root
```

### 方式二：手动部署

如果无法使用 SSH 密钥，请按以下步骤手动部署：

## 手动部署步骤

### 1. 在远程主机上安装必要软件

```bash
# 安装 Docker（如果还没有）
curl -fsSL https://get.docker.com | sh
systemctl enable docker
systemctl start docker

# 安装 Docker Compose
curl -L "https://github.com/docker/compose/releases/download/v2.24.0/docker-compose-$(uname -s)-$(uname -m)" -o /usr/local/bin/docker-compose
chmod +x /usr/local/bin/docker-compose
```

### 2. 创建部署目录并上传文件

```bash
mkdir -p /root/aick-mmp
```

将以下文件上传到 `/root/aick-mmp/`:
- `docker-compose.remote.yml` → `docker-compose.yml`
- `Dockerfile`
- `nginx/central-lb.conf`
- `nginx/edge-lb.conf`

将以下目录上传到 `/root/aick-mmp/`:
- `frontend/` (完整目录)
- `backend/aick-mmp-central/`
- `backend/aick-mmp-edge/`
- `backend/aick-mmp-shared/`

### 3. 构建镜像

```bash
cd /root/aick-mmp

# 构建前端镜像
cd frontend
docker build -t aick-mmp-frontend:latest .
cd ..

# 构建 Central 镜像
docker build --target central-runtime -t aick-mmp-central:latest .

# 构建 Edge 镜像
docker build --target edge-runtime -t aick-mmp-edge:latest .
```

### 4. 启动服务

```bash
cd /root/aick-mmp
docker-compose -f docker-compose.yml up -d
```

### 5. 验证服务

```bash
docker-compose ps
```

## 服务访问地址

部署完成后，可通过以下地址访问服务：

| 服务 | 地址 |
|------|------|
| 前端 | http://39.106.143.240/ |
| 中央服务 API | http://39.106.143.240:8080 |
| 边缘节点 | http://39.106.143.240:8081 |
| 边缘节点 LB | http://39.106.143.240:8083 |

## 端口说明

| 端口 | 服务 |
|------|------|
| 80 | 前端 |
| 3306 | MySQL |
| 6379 | Redis |
| 8080 | 中央服务 LB |
| 8081 | 边缘节点 |
| 8083 | 边缘节点 LB |
| 8088 | Janus Gateway (HTTP) |
| 8188 | Janus Gateway (WebSocket) |
| 8554 | RTSP Server |
| 9092 | Kafka |

## 故障排查

```bash
# 查看所有容器日志
docker-compose logs -f

# 查看特定服务日志
docker-compose logs -f central-1
docker-compose logs -f frontend

# 重启特定服务
docker-compose restart central-1

# 停止所有服务
docker-compose down
```
