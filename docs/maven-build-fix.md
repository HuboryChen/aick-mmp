# Maven构建依赖问题修复总结

## 问题描述
edge-node-1构建失败，错误信息显示`shared`模块的包不存在：
```
[ERROR] package com.aick.mmp.shared.model does not exist
```

## 根因分析
1. **Maven依赖解析问题**：在构建`edge`模块之前，`shared`模块没有正确安装到本地Maven仓库
2. **构建顺序问题**：需要先构建`parent`模块，然后构建`shared`模块，最后构建`edge`模块
3. **依赖下载问题**：可能需要先下载所有依赖

## 修复内容

### 构建流程优化（最新版本）

利用 Maven 的 `-am` (also-make) 参数自动处理依赖模块构建链，简化 Dockerfile：

```dockerfile
# Central 构建
COPY backend/ ./backend/
WORKDIR /build/backend
RUN mvn clean package -pl aick-mmp-central -am -DskipTests

# Edge 构建
COPY backend/ ./backend/
WORKDIR /build/backend
RUN mvn clean package -pl aick-mmp-edge -am -DskipTests
```

### Maven命令说明
- **`-pl`**：指定要构建的项目列表
- **`-am`**：同时构建指定项目的依赖项目（自动构建 parent → shared → central/edge）
- **`-DskipTests`**：跳过测试以加速构建

### 依赖链自动解析

执行 `mvn clean package -pl aick-mmp-central -am` 时，Maven 自动：

1. 解析 central 的依赖树
2. 按正确顺序构建：parent → shared → central
3. 最终生成 fat jar（Spring Boot 可执行 jar）

## 验证建议
1. 清除之前的构建缓存：`docker system prune -a`
2. 重新构建：`docker-compose build central-1 edge-node-1`
3. 检查构建日志，确认没有编译错误
4. 验证所有模块都能正确构建

## 注意事项
- 确保Maven有正确的网络访问权限下载依赖
- 检查本地Maven仓库权限
- 考虑在CI/CD环境中添加构建缓存优化

---

## 历史版本（已废弃）

<details>
<summary>点击查看旧版构建流程（已简化）</summary>

### 旧版构建流程（冗余，已废弃）

#### Edge构建流程：
1. 先复制所有`pom.xml`文件
2. 使用`mvn dependency:resolve`下载所有依赖
3. 构建并安装`parent`模块：`mvn clean install -pl aick-mmp-parent -am -DskipTests`
4. 构建并安装`shared`模块：`mvn clean install -pl aick-mmp-shared -am -DskipTests`
5. 复制`edge`源代码
6. 构建`edge`模块：`mvn clean package -pl aick-mmp-edge -am -DskipTests`

#### Central构建流程：
1. 先复制所有`pom.xml`文件
2. 使用`mvn dependency:resolve`下载所有依赖
3. 构建并安装`parent`模块：`mvn clean install -pl aick-mmp-parent -am -DskipTests`
4. 构建并安装`shared`模块：`mvn clean install -pl aick-mmp-shared -am -DskipTests`
5. 复制`central`源代码
6. 构建`central`模块：`mvn clean package -pl aick-mmp-central -am -DskipTests`

### 旧版Dockerfile

```dockerfile
# Download all dependencies first
RUN mvn dependency:resolve -DskipTests
# Build parent module first
RUN mvn clean install -pl aick-mmp-parent -am -DskipTests
# Build shared module
RUN mvn clean install -pl aick-mmp-shared -am -DskipTests
# Copy edge source code
COPY backend/aick-mmp-edge/src ./aick-mmp-edge/src
# Build edge module
RUN mvn clean package -pl aick-mmp-edge -am -DskipTests
```

</details>
