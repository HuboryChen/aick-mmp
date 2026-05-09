#!/bin/bash

# AICM-MMP 远程部署脚本
# 用法: ./deploy-remote.sh <远程主机IP> [SSH用户]
# 示例: ./deploy-remote.sh 39.106.143.240 root

set -e

# 配置
REMOTE_HOST=${1:-39.106.143.240}
SSH_USER=${2:-root}
REMOTE_DIR="/root/aick-mmp"
PROJECT_DIR="$(cd "$(dirname "$0")" && pwd)"
IMAGES=("aick-mmp-central:latest" "aick-mmp-edge:latest" "aick-mmp-frontend:latest")

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

# 构建前端镜像
build_frontend() {
    log_info "构建前端镜像..."
    cd ${PROJECT_DIR}/frontend
    docker build -t aick-mmp-frontend:latest .
    cd ${PROJECT_DIR}
}

# 构建 Central 镜像
build_central() {
    log_info "构建 Central 服务镜像..."
    cd ${PROJECT_DIR}
    docker build --target central-runtime -t aick-mmp-central:latest .
}

# 构建 Edge 镜像
build_edge() {
    log_info "构建 Edge 节点镜像..."
    cd ${PROJECT_DIR}
    docker build --target edge-runtime -t aick-mmp-edge:latest .
}

# 保存镜像为 tar 文件
save_images() {
    log_info "保存镜像为 tar 文件..."
    cd ${PROJECT_DIR}
    rm -f aick-mmp-images.tar
    
    # 保存所有自定义镜像
    docker save -o aick-mmp-images.tar \
        aick-mmp-central:latest \
        aick-mmp-edge:latest \
        aick-mmp-frontend:latest
    
    log_info "镜像已保存到 aick-mmp-images.tar"
}

# 传输文件到远程主机
transfer_files() {
    log_info "传输文件到远程主机..."
    
    # 创建远程目录
    ssh ${SSH_USER}@${REMOTE_HOST} "mkdir -p ${REMOTE_DIR}"
    
    # 传输镜像文件
    log_info "传输镜像文件 (可能需要几分钟)..."
    scp ${PROJECT_DIR}/aick-mmp-images.tar ${SSH_USER}@${REMOTE_HOST}:${REMOTE_DIR}/
    
    # 传输 docker-compose 和 nginx 配置
    log_info "传输配置文件..."
    scp ${PROJECT_DIR}/docker-compose.remote.yml ${SSH_USER}@${REMOTE_HOST}:${REMOTE_DIR}/docker-compose.yml
    scp -r ${PROJECT_DIR}/nginx ${SSH_USER}@${REMOTE_HOST}:${REMOTE_DIR}/
    
    log_info "文件传输完成"
}

# 在远程主机上加载镜像并启动服务
deploy_remote() {
    log_info "在远程主机上部署服务..."
    
    ssh ${SSH_USER}@${REMOTE_HOST} << 'EOF'
        set -e
        cd /root/aick-mmp
        
        # 加载镜像
        echo "[INFO] 加载 Docker 镜像..."
        docker load -i aick-mmp-images.tar
        
        # 启动服务
        echo "[INFO] 启动 Docker Compose 服务..."
        docker-compose up -d
        
        # 等待服务启动
        echo "[INFO] 等待服务启动..."
        sleep 30
        
        # 检查服务状态
        echo "[INFO] 检查服务状态..."
        docker-compose ps
        
        echo "[SUCCESS] 部署完成!"
        echo "服务访问地址:"
        echo "  - 前端: http://<your-ip>/"
        echo "  - 中央服务: http://<your-ip>:8080"
        echo "  - 边缘节点: http://<your-ip>:8081"
    EOF
}

# 主流程
main() {
    log_info "开始部署到 ${REMOTE_HOST}..."
    
    # 1. 检查 SSH 连接
    check_ssh
    
    # 2. 构建镜像
    log_warn "开始构建镜像，这可能需要几分钟..."
    build_frontend
    build_central
    build_edge
    
    # 3. 保存镜像
    save_images
    
    # 4. 传输文件
    transfer_files
    
    # 5. 远程部署
    deploy_remote
    
    log_info "部署完成!"
}

main
