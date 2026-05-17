# Legacy Root Dockerfile — Build Strategy Reference

> 根层 `Dockerfile` 已于 2026-05-17 删除。此文档记录其有价值的构建策略，供参考。

## Multi-stage Build Structure

原 Dockerfile 使用三级多阶段构建：

```
base-build          ← Maven 3.9 + Temurin 21，预下载依赖
  ├── central-build → central-runtime  (eclipse-temurin:21-jre)
  ├── edge-build    → edge-runtime     (eclipse-temurin:21-jre)
  └── frontend-build → frontend-runtime (nginx:alpine)
```

## 依赖缓存策略

利用 Docker layer cache，先复制 pom.xml 再下载依赖，避免源码变更失效缓存：

```
COPY backend/pom.xml ./backend/pom.xml
COPY backend/aick-mmp-parent/pom.xml ./backend/aick-mmp-parent/pom.xml
...
RUN cd /build/backend && mvn dependency:go-offline -B -N || true
```

## 健康检查和 JVM 配置

- Central: `-Xmx2g -Xms1g -XX:+UseG1GC`
- Edge: `-Xmx1g -Xms512m -XX:+UseG1GC`
- Health check readiness endpoint: `/api/actuator/health/readiness`

## 当前使用的 Dockerfile

| 服务 | Dockerfile 位置 |
|------|----------------|
| central | `backend/aick-mmp-central/Dockerfile` |
| edge | `backend/aick-mmp-edge/Dockerfile` |
| frontend | `frontend/Dockerfile` |
| ai-service | `aick-mmp-ai/Dockerfile` |
