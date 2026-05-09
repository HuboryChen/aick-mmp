---
name: 简化 Maven 多模块构建流程
overview: 简化 Dockerfile，利用 Maven -am 参数自动传递构建 parent 和 shared 模块，无需在 Dockerfile 中手动处理依赖构建
todos:
  - id: simplify-dockerfile
    content: 简化 Dockerfile 构建流程，使用 mvn -am 自动构建依赖模块
    status: completed
  - id: update-docs
    content: 更新 docs/maven-build-fix.md 文档
    status: completed
    dependencies:
      - simplify-dockerfile
---

## 用户需求

简化 Maven 多模块项目的 Dockerfile 构建流程：

1. parent 和 shared 模块的构建由 central 和 edge 模块传递依赖自动构建
2. 构建可直接运行的 fat 包（Spring Boot 可执行 jar）
3. 不需要在 Dockerfile 中单独处理 parent 和 shared 模块

## 核心改进

利用 Maven 的 `-am` (also-make) 参数自动处理依赖模块构建链，简化 Dockerfile 构建步骤。

## 技术方案

### Maven 构建原理

Maven 的 `-am` (also-make) 参数会自动构建指定模块的所有依赖模块。当前依赖链：

```
central/edge → shared → parent → spring-boot-starter-parent
```

执行 `mvn clean package -pl aick-mmp-central -am` 时，Maven 自动：

1. 解析 central 的依赖树
2. 按正确顺序构建：parent → shared → central
3. 最终生成 fat jar

### 修改文件

**Dockerfile** - 简化构建流程：

- 移除冗余的 `mvn clean install -pl aick-mmp-parent` 和 `mvn clean install -pl aick-mmp-shared`
- 简化为单条命令：`mvn clean package -pl aick-mmp-central -am -DskipTests`
- 中央服务和边缘服务构建流程同步简化

### 优化后的构建流程

```
# Central 构建
COPY backend/ ./backend/
WORKDIR /build/backend
RUN mvn clean package -pl aick-mmp-central -am -DskipTests

# Edge 构建
COPY backend/ ./backend/
WORKDIR /build/backend
RUN mvn clean package -pl aick-mmp-edge -am -DskipTests
```

## 目录结构

修改文件：

```
project-root/
├── Dockerfile              # [MODIFY] 简化构建步骤，使用-am自动构建依赖模块
└── backend/
    ├── pom.xml             # 无需修改（聚合器POM）
    ├── aick-mmp-parent/    # 无需修改
    ├── aick-mmp-shared/    # 无需修改
    ├── aick-mmp-central/   # 无需修改（已配置spring-boot-maven-plugin）
    └── aick-mmp-edge/      # 无需修改（已配置spring-boot-maven-plugin）
```