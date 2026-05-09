## Context

当前 Docker 构建流程使用一个根目录的 Dockerfile，通过多阶段构建（multi-stage build）同时产出 central、edge、frontend 三个运行时镜像。构建时 docker compose 将整个项目根目录作为 build context 发给 Docker daemon。

关键限制：
- **串行构建**：单个 Dockerfile 意味着三个服务必须串行完成，`docker compose build` 无法利用 service 级别并行
- **Build Context 过大**：frontend/ 目录含 896MB node_modules，即使只构建 backend 也要发送全部
- **无 BuildKit cache mount**：所有依赖（Maven、npm）缓存都依赖 Docker layer cache，any pom.xml change 会 invalidate 整个 dependency layer
- **前端强制重建**：`ARG BUILD_DATE=$(date +%s)` 确保每次都跑完整 npm install + build

## Goals / Non-Goals

**Goals:**
- 本地 `docker compose up -d` 构建时间从 20 分钟降到 < 2 分钟（仅后端代码变更时）
- Maven 依赖下载使用 BuildKit cache mount，pom 变更时只下载 delta
- 不碰前端代码时，前端构建直接命中 Docker layer cache
- central 和 edge 并行构建
- 保持对 CI/CD 环境的兼容性（docker compose build）

**Non-Goals:**
- 不改变现有服务间通信方式
- 不改变 Docker 镜像的运行时行为（JVM 参数、健康检查等保持不变）
- 不做 CI 特定的构建优化（如 GitHub Actions cache 集成）
- 不引入 docker bake 或其他新工具

## Decisions

### Decision 1：独立 Dockerfile + 缩小 Build Context

**方案**：将 central、edge、frontend 分别使用独立的 Dockerfile，`context` 指向各自模块目录。

```
当前：
  context: .                          # 1GB
  dockerfile: Dockerfile              # 单体文件，target 区分

优化后：
  # central
  context: ./backend                  # 7.7MB
  dockerfile: aick-mmp-central/Dockerfile

  # edge
  context: ./backend                  # 7.7MB
  dockerfile: aick-mmp-edge/Dockerfile

  # frontend
  context: ./frontend                 # 仅源码 + package.json（排除 node_modules）
  dockerfile: Dockerfile
```

**替代方案**：保留单体 Dockerfile，用 `docker build --target` 分开构建。
**选择理由**：独立 Dockerfile 让 `docker compose build` 天然支持 service 级别并行，且 context 大幅缩小。

### Decision 2：BuildKit Cache Mount 替代 Layer Cache

**方案**：在 backend Dockerfile 中使用 `--mount=type=cache,target=/root/.m2` 和 `--mount=type=cache,target=/root/.npm`。

```dockerfile
# 不再需要手动 COPY pom.xml + mvn dependency:go-offline
RUN --mount=type=cache,target=/root/.m2,sharing=locked \
    mvn package -pl aick-mmp-central -am -DskipTests
```

**效果**：
- pom.xml 变更时，只下载新增/变更的依赖
- cache mount 不会随 layer 变更而失效
- cache mount 可设置 `size` 上限防止磁盘膨胀

**替代方案**：保留现在的 `COPY pom.xml + mvn dependency:go-offline` 分层策略。
**选择理由**：layer cache 在 pom 变更时全量失效，cache mount 只增量更新。且省去手动管理 pom.xml 复制顺序的复杂性。

### Decision 3：前端 Dockerfile 独立且移除强制重建

**方案**：
- 新建 `frontend/Dockerfile`，context = `./frontend`
- 使用 `.dockerignore` 排除 node_modules（防止 context 过大）
- 移除 `ARG BUILD_DATE=$(date +%s)`，依赖 Docker layer cache 自动管理

```dockerfile
FROM node:20-alpine AS build
WORKDIR /app

COPY package*.json ./
RUN --mount=type=cache,target=/root/.npm \
    npm install --legacy-peer-deps

COPY public ./public
COPY src ./src

ENV GENERATE_SOURCEMAP=false CI=false SKIP_PREFLIGHT_CHECK=true
RUN npm run build

FROM nginx:alpine
COPY --from=build /app/build /usr/share/nginx/html
...
```

### Decision 4：保留根目录 Dockerfile 作为 CI 全量构建入口

根目录 Dockerfile 不动，CI 需要全量构建时仍然可用。本地开发走新流程。

## Risks / Trade-offs

| Risk | 影响 | 缓解措施 |
|------|------|----------|
| BuildKit cache mount 在 Docker < 18.09 不可用 | 构建失败 | docker-compose.yml 添加 `DOCKER_BUILDKIT=1` 环境变量；项目 CLAUDE.md 注明最低版本要求 |
| Cache mount 磁盘无限增长 | /var/lib/docker 爆满 | 在 Dockerfile 中设置 `--mount=type=cache,size=2g`；定期 `docker builder prune` |
| 首次构建（空 cache）仍然慢 | 首次约 10-15 分钟 | 这是一次性的，第二次起加速；可在 README 中标注首次构建预期时间 |
| 前后端代码同时改动时无法并行 | 构建时间 = max(后端, 前端) | 本身已是最优，总时间取决于耗时最长的那个 |
| 独立 Dockerfile 导致代码重复 | 维护两个 Dockerfile 的成本 | 两个文件的构建逻辑高度一致（只有 target 不同），后续可抽 shared base stage |
