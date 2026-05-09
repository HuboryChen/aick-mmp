## Why

当前 `docker compose up -d` 重新构建需要约 20 分钟，严重拖慢开发迭代速度。主要原因是单体 Dockerfile 导致串行构建、1GB 的 build context（含 frontend/node_modules）、以及未使用 Docker BuildKit cache mounts 等高级缓存能力。优化后目标压缩到 2 分钟以内。

## What Changes

- **拆分单体 Dockerfile**：将根目录的 Dockerfile 拆分为 backend 和 frontend 各自的独立 Dockerfile，各服务只需自己的 small build context
- **backend Dockerfile 采用 BuildKit cache mount**：用 `--mount=type=cache,target=/root/.m2` 持久化 Maven 仓库，pom.xml 变更时不需重新下载所有依赖
- **frontend Dockerfile 独立**：前端构建不再与后端捆绑，不碰前端时直接命中 Docker layer cache
- **docker-compose.yml 重构**：各 service 指向新的 Dockerfile，缩小 context 到各自模块目录
- **移除 frontend BUILD_DATE ARG**：不再每次都强制重建前端

## Capabilities

### New Capabilities
- `build-optimization`: Docker Compose 构建流程优化方案，含 Maven 缓存策略、BuildKit cache mount、并行构建等

### Modified Capabilities
无（纯基础设施优化，不修改业务能力）

## Impact

- `docker-compose.yml`：各 service 的 build 配置重写
- `Dockerfile`：根目录 Dockerfile 可保留作为 CI 全量构建入口
- `backend/aick-mmp-central/Dockerfile`：重写为支持 BuildKit cache mount
- `backend/aick-mmp-edge/Dockerfile`：重写为支持 BuildKit cache mount
- `frontend/Dockerfile`：新建独立 Dockerfile
- 开发者体验：本地重建时间从 20 分钟降到 < 2 分钟
- CI/CD：需配置 BuildKit 环境变量 `DOCKER_BUILDKIT=1`
