## ADDED Requirements

### Requirement: 缩短 Docker Compose 构建时间
系统 SHALL 实现 Docker Compose 构建流程优化，使本地开发时 `docker compose up -d` 的完整重建时间控制在 2 分钟以内。

#### Scenario: 仅后端代码变更时重建
- **WHEN** 开发者修改了 backend 代码后执行 `docker compose up -d --build`
- **THEN** 构建时间 SHALL 不超过 2 分钟

#### Scenario: 前后端代码同时变更时重建
- **WHEN** 开发者同时修改了 backend 和 frontend 代码后执行 `docker compose up -d --build`
- **THEN** central 和 edge SHALL 并行构建
- **THEN** 构建时间 SHALL 不超过前后端各自构建时间的最大值

### Requirement: BuildKit Cache Mount 持久化 Maven 依赖
Maven 仓库 SHALL 使用 Docker BuildKit `--mount=type=cache` 持久化，避免因 pom.xml 变更导致全量重新下载。

#### Scenario: pom.xml 新增依赖时重建
- **WHEN** 开发者在 pom.xml 中新增一个依赖后执行 `docker compose up -d --build`
- **THEN** 仅下载新增的依赖，SHALL NOT 重新下载之前已缓存的依赖

#### Scenario: pom.xml 未变更时重建
- **WHEN** 开发者仅修改 Java 源代码（不修改 pom.xml）后执行 `docker compose up -d --build`
- **THEN** 构建过程 SHALL NOT 执行任何 Maven 依赖下载操作

### Requirement: 缩小 Build Context
每个服务的 Docker build context SHALL 仅包含该服务构建所需的文件，排除无关目录。

#### Scenario: 构建 central 服务
- **WHEN** docker compose 构建 central-1 service
- **THEN** build context 中 SHALL NOT 包含 frontend/ 目录或 edge 模块的源代码
- **THEN** build context 大小 SHALL 不超过 50MB

#### Scenario: 构建 frontend 服务
- **WHEN** docker compose 构建 frontend service
- **THEN** build context 中 SHALL NOT 包含 backend/ 目录
- **THEN** build context 中 SHALL 排除 frontend/node_modules/

### Requirement: 前端构建不强制刷新缓存
前端 Docker 构建 SHALL NOT 使用 `ARG BUILD_DATE` 或其他机制强制 invalidate layer cache。

#### Scenario: 前端代码未变更时重建
- **WHEN** 开发者仅修改后端代码后执行 `docker compose up -d --build`
- **THEN** Docker SHALL 直接复用前端 layer cache，不执行 npm install 或 npm run build

### Requirement: 服务间并行构建
central 服务和 edge 服务的 Docker 镜像 SHALL 能够并行构建。

#### Scenario: 并行构建验证
- **WHEN** 执行 `docker compose build central-1 edge-node-1`
- **THEN** 两个服务的构建过程 SHALL 同时进行（不串行等待）
