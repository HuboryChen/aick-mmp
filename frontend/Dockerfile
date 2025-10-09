FROM node:20-alpine as dependencies
WORKDIR /app

# 只复制package文件以最大化利用Docker缓存
COPY package*.json ./

# 安装所有依赖（包括开发依赖，因为构建需要它们）
# 使用npm ci以获得更快、更可靠的安装
RUN npm ci && npm cache clean --force

# 构建阶段
FROM node:20-alpine as build
WORKDIR /app

# 从依赖阶段复制已安装的node_modules
COPY --from=dependencies /app/node_modules ./node_modules

# 复制源代码
COPY package*.json ./
COPY public ./public
COPY src ./src

# 设置环境变量以优化构建
ENV GENERATE_SOURCEMAP=false
ENV CI=false
ENV SKIP_PREFLIGHT_CHECK=true

# 执行构建
RUN npm run build

FROM nginx:alpine
COPY --from=build /app/build /usr/share/nginx/html
RUN rm /etc/nginx/conf.d/default.conf
RUN echo "server { \
    listen 80; \
    server_name localhost; \
    root /usr/share/nginx/html; \
    index index.html; \
    gzip on; \
    gzip_types text/css application/javascript image/svg+xml; \
    location / { \
        try_files \$uri \$uri/ /index.html; \
    } \
    location /api/ { \
        proxy_pass http://central-lb/api/; \
        proxy_set_header Host \$host; \
        proxy_set_header X-Real-IP \$remote_addr; \
        proxy_set_header X-Forwarded-For \$proxy_add_x_forwarded_for; \
        proxy_set_header X-Forwarded-Proto \$scheme; \
    } \
}" > /etc/nginx/conf.d/default.conf
EXPOSE 80
CMD ["nginx", "-g", "daemon off;"]