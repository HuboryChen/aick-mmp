# ============================================================
# Multi-stage Docker build for aick-mmp
# Supports: backend (central/edge) + frontend
# ============================================================

# ---- Common build dependencies (cached, reused) ----
FROM maven:3.9-eclipse-temurin-21 AS base-build
WORKDIR /build

# Copy only pom.xml files first (change infrequently)
COPY backend/pom.xml ./backend/pom.xml
COPY backend/aick-mmp-parent/pom.xml ./backend/aick-mmp-parent/pom.xml
COPY backend/aick-mmp-shared/pom.xml ./backend/aick-mmp-shared/pom.xml
COPY backend/aick-mmp-central/pom.xml ./backend/aick-mmp-central/pom.xml
COPY backend/aick-mmp-edge/pom.xml ./backend/aick-mmp-edge/pom.xml

# Download and cache dependencies (Docker layer cached)
RUN cd /build/backend && mvn dependency:go-offline -B -N || true
RUN cd /build/backend/aick-mmp-parent && mvn dependency:go-offline -B -N || true
RUN cd /build/backend/aick-mmp-shared && mvn dependency:go-offline -B || true

# ============================================================
# Build stage for Central Service
# ============================================================
FROM base-build AS central-build
WORKDIR /build

COPY backend/aick-mmp-parent ./backend/aick-mmp-parent
COPY backend/aick-mmp-shared ./backend/aick-mmp-shared

RUN cd /build/backend/aick-mmp-parent && mvn install -DskipTests -N || true
RUN cd /build/backend/aick-mmp-shared && mvn install -DskipTests

COPY backend/aick-mmp-central ./backend/aick-mmp-central
RUN cd /build/backend && mvn package -pl aick-mmp-central -am -DskipTests

# ============================================================
# Runtime stage for Central Service
# ============================================================
FROM eclipse-temurin:21-jre AS central-runtime
WORKDIR /app

RUN groupadd -r aick && useradd --no-log-init -r -g aick aick

COPY --from=central-build /build/backend/aick-mmp-central/target/aick-mmp-central-*.jar app.jar

RUN chown -R aick:aick /app
USER aick

EXPOSE 8080

HEALTHCHECK --interval=30s --timeout=10s --start-period=60s --retries=3 \
  CMD curl -f http://localhost:8080/api/actuator/health/readiness || exit 1

ENV JAVA_OPTS="-Xmx2g -Xms1g -XX:+UseG1GC"

ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar /app/app.jar"]

# ============================================================
# Build stage for Edge Node
# ============================================================
FROM base-build AS edge-build
WORKDIR /build

COPY backend/aick-mmp-parent ./backend/aick-mmp-parent
COPY backend/aick-mmp-shared ./backend/aick-mmp-shared

RUN cd /build/backend/aick-mmp-parent && mvn install -DskipTests -N || true
RUN cd /build/backend/aick-mmp-shared && mvn install -DskipTests

COPY backend/aick-mmp-edge ./backend/aick-mmp-edge
RUN cd /build/backend && mvn package -pl aick-mmp-edge -am -DskipTests

# ============================================================
# Runtime stage for Edge Node
# ============================================================
FROM eclipse-temurin:21-jre AS edge-runtime
WORKDIR /app

RUN groupadd -r aick && useradd --no-log-init -r -g aick aick

COPY --from=edge-build /build/backend/aick-mmp-edge/target/aick-mmp-edge-*.jar app.jar

RUN chown -R aick:aick /app
USER aick

EXPOSE 8080

HEALTHCHECK --interval=30s --timeout=10s --start-period=30s --retries=3 \
  CMD curl -f http://localhost:8080/api/actuator/health/readiness || exit 1

ENV JAVA_OPTS="-Xmx1g -Xms512m -XX:+UseG1GC"

ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar /app/app.jar"]

# ============================================================
# Build stage for Frontend (独立于后端构建)
# ============================================================
FROM node:20-alpine AS frontend-build
WORKDIR /app

COPY frontend/package*.json ./

# 安装依赖
RUN npm install && npm cache clean --force

# 复制源码
COPY frontend/public ./public
COPY frontend/src ./src

# 设置环境变量
ENV GENERATE_SOURCEMAP=false
ENV CI=false
ENV SKIP_PREFLIGHT_CHECK=true

# 构建
RUN npm run build

# ============================================================
# Runtime stage for Frontend (独立于后端运行时)
# ============================================================
FROM nginx:alpine AS frontend-runtime

# 复制构建产物
COPY --from=frontend-build /app/build /usr/share/nginx/html

# Nginx 配置
RUN rm -f /etc/nginx/conf.d/default.conf
RUN echo 'server { \
    listen 80; \
    server_name localhost; \
    root /usr/share/nginx/html; \
    index index.html; \
    gzip on; \
    gzip_types text/css application/javascript image/svg+xml; \
    location / { \
        try_files $uri $uri/ /index.html; \
    } \
    location /api/ { \
        proxy_pass http://central-lb; \
        proxy_set_header Host $host; \
        proxy_set_header X-Real-IP $remote_addr; \
        proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for; \
        proxy_set_header X-Forwarded-Proto $scheme; \
    } \
}' > /etc/nginx/conf.d/default.conf

EXPOSE 80

CMD ["nginx", "-g", "daemon off;"]
