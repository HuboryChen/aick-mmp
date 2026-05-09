## 1. 重写 backend Dockerfile（Cache Mount + 独立构建）

- [x] 1.1 重写 `backend/aick-mmp-central/Dockerfile`：使用 `--mount=type=cache,target=/root/.m2` 替换手动 `COPY pom.xml + mvn dependency:go-offline` 策略；直接 build 当前模块
- [x] 1.2 重写 `backend/aick-mmp-edge/Dockerfile`：同上 pattern，改为构建 aick-mmp-edge
- [x] 1.3 验证：在 backend 目录下执行 `docker build -f aick-mmp-central/Dockerfile --no-cache .`，确认首次构建正确

## 2. 新建独立 Frontend Dockerfile

- [x] 2.1 重写 `frontend/Dockerfile`，使用 BuildKit cache mount 加速 npm install
- [x] 2.2 更新 `frontend/.dockerignore`，排除 node_modules、.git 等不必要文件
- [x] 2.3 移除根目录 Dockerfile 中的 `ARG BUILD_DATE=$(date +%s)` 强制刷新
- [x] 2.4 验证：在 frontend 目录下执行 `docker build .`，确认构建成功且 cache 正常工作

## 3. 更新 docker-compose.yml

- [x] 3.1 修改 `central-1` service：`context: ./backend`, `dockerfile: aick-mmp-central/Dockerfile`
- [x] 3.2 修改 `edge-node-1` service：`context: ./backend`, `dockerfile: aick-mmp-edge/Dockerfile`
- [x] 3.3 修改 `frontend` service：`context: ./frontend`, `dockerfile: Dockerfile`
- [x] 3.4 移除 central-1 和 edge-node-1 对其他服务的不必要 depends_on（保留对 mysql/redis 等基础设施的依赖）
- [x] 3.5 确保 docker compose 构建时 central 和 edge 不相互等待（已满足）

## 4. 验证与文档

- [x] 4.1 执行 `docker compose build`，确认所有服务构建成功且时间在 2 分钟内（有缓存时）
- [x] 4.2 修改一个 Java 文件后重建，确认编译是增量的
- [x] 4.3 更新根目录 `Dockerfile`：移除 `ARG BUILD_DATE`，保留作为 CI 全量构建入口
- [x] 4.4 更新 `README.md` 构建相关章节，标注优化后的构建指引
