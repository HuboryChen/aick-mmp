#!/bin/bash

# AICM-MMP 远程部署脚本 (远程构建版本)
# 此脚本在远程主机上构建镜像，无需本地 Docker
# 用法: ./deploy-remote-build.sh <远程主机IP> [SSH用户]

set -e

# 配置
REMOTE_HOST=${1:-39.106.143.240}
SSH_USER=${2:-root}
REMOTE_DIR="/root/aick-mmp"
PROJECT_DIR="$(cd "$(dirname "$0")" && pwd)"

# 颜色输出
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
NC='\033[0m'

log_info() {
    echo -e "${GREEN}[INFO]${NC} $1"
}

log_warn() {
    echo -e "${YELLOW}[WARN]${NC} $1"
}

log_error() {
    echo -e "${RED}[ERROR]${NC} $1"
}

# 检查 SSH 连接
check_ssh() {
    log_info "检查 SSH 连接..."
    if ssh -o ConnectTimeout=5 -o StrictHostKeyChecking=no ${SSH_USER}@${REMOTE_HOST} "echo ok" &>/dev/null; then
        log_info "SSH 连接成功"
    else
        log_error "无法连接到 ${SSH_USER}@${REMOTE_HOST}"
        exit 1
    fi
}

# 传输项目文件到远程主机
transfer_project() {
    log_info "传输项目文件到远程主机..."
    
    # 创建远程目录
    ssh ${SSH_USER}@${REMOTE_HOST} "mkdir -p ${REMOTE_DIR}"
    
    # 传输 docker-compose 配置
    log_info "传输 docker-compose 配置..."
    scp ${PROJECT_DIR}/docker-compose.remote.yml ${SSH_USER}@${REMOTE_HOST}:${REMOTE_DIR}/docker-compose.yml
    scp ${PROJECT_DIR}/Dockerfile ${SSH_USER}@${REMOTE_HOST}:${REMOTE_DIR}/Dockerfile
    
    # 传输 nginx 配置
    log_info "传输 nginx 配置..."
    ssh ${SSH_USER}@${REMOTE_HOST} "mkdir -p ${REMOTE_DIR}/nginx"
    scp ${PROJECT_DIR}/nginx/central-lb.conf ${SSH_USER}@${REMOTE_HOST}:${REMOTE_DIR}/nginx/
    scp ${PROJECT_DIR}/nginx/edge-lb.conf ${SSH_USER}@${REMOTE_HOST}:${REMOTE_DIR}/nginx/
    
    # 传输前端文件
    log_info "传输前端文件..."
    ssh ${SSH_USER}@${REMOTE_HOST} "mkdir -p ${REMOTE_DIR}/frontend"
    scp -r ${PROJECT_DIR}/frontend/* ${SSH_USER}@${REMOTE_HOST}:${REMOTE_DIR}/frontend/
    scp ${PROJECT_DIR}/frontend/Dockerfile ${SSH_USER}@${REMOTE_HOST}:${REMOTE_DIR}/frontend/
    
    # 传输后端文件
    log_info "传输后端文件..."
    ssh ${SSH_USER}@${REMOTE_HOST} "mkdir -p ${REMOTE_DIR}/backend"
    scp -r ${PROJECT_DIR}/backend/aick-mmp-parent ${SSH_USER}@${REMOTE_HOST}:${REMOTE_DIR}/backend/
    scp -r ${PROJECT_DIR}/backend/aick-mmp-shared ${SSH_USER}@${REMOTE_HOST}:${REMOTE_DIR}/backend/
    scp -r ${PROJECT_DIR}/backend/aick-mmp-central ${SSH_USER}@${REMOTE_HOST}:${REMOTE_DIR}/backend/
    scp -r ${PROJECT_DIR}/backend/aick-mmp-edge ${SSH_USER}@${REMOTE_HOST}:${REMOTE_DIR}/backend/
    scp ${PROJECT_DIR}/backend/pom.xml ${SSH_USER}@${REMOTE_HOST}:${REMOTE_DIR}/backend/

    log_info "项目文件传输完成"}

# 在远程主机上构建镜像
build_on_remote() {
    log_info "在远程主机上构建镜像..."
    
    ssh ${SSH_USER}@${REMOTE_HOST} << 'EOF'
        set -e
        cd /root/aick-mmp
        
        echo "[INFO] 检查远程 Docker 环境..."
        docker --version
        docker-compose --version || docker compose version
        
        # 构建前端镜像
        echo "[INFO] 构建前端镜像..."
        cd frontend
        docker build -t aick-mmp-frontend:latest .
        cd ..
        
        # 构建 Central 镜像
        echo "[INFO] 构建 Central 服务镜像..."
        docker build --target central-runtime -t aick-mmp-central:latest .
        
        # 构建 Edge 镜像
        echo "[INFO] 构建 Edge 节点镜像..."
        docker build --target edge-runtime -t aick-mmp-edge:latest .
        
        echo "[INFO] 所有镜像构建完成!"
        docker images | grep aick-mmp
    EOF
}

# 在远程主机上启动服务
start_services() {
    log_info "在远程主机上启动服务..."
    
    ssh ${SSH_USER}@${REMOTE_HOST} << 'EOF'
        set -e
        cd /root/aick-mmp
        
        # 启动服务
        echo "[INFO] 启动 Docker Compose 服务..."
        docker-compose -f docker-compose.remote.yml up -d
        
        # 等待服务启动
        echo "[INFO] 等待服务启动 (60秒)..."
        sleep 60
        
        # 检查服务状态
        echo "[INFO] 检查服务状态..."
        docker-compose -f docker-compose.remote.yml ps
        
        echo "[SUCCESS] 部署完成!"
        echo ""
        echo "服务访问地址:"
        echo "  - 前端: http://<your-ip>/"
        echo "  - 中央服务: http://<your-ip>:8080"
        echo "  - 边缘节点: http://<your-ip>:8081"
        echo "  - 边缘节点LB: http://<your-ip>:8083"
    EOF
}

# 主流程
main() {
    log_info "开始部署到 ${REMOTE_HOST}..."
    
    # 1. 检查 SSH 连接
    check_ssh
    
    # 2. 传输项目文件
    transfer_project
    
    # 3. 远程构建镜像
    log_warn "开始构建镜像，这可能需要较长时间..."
    build_on_remote
    
    # 4. 启动服务
    start_services
    
    log_info "部署完成!"
}

main
