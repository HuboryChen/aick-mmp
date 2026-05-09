# Multi-stage: build + runtime for Frontend
# Uses BuildKit --mount=type=cache for npm cache persistence

FROM node:20-alpine AS build
WORKDIR /app

# Install dependencies with npm cache mount
COPY package*.json ./
RUN --mount=type=cache,target=/root/.npm \
    npm install --legacy-peer-deps

# Copy source
COPY public ./public
COPY src ./src

# Build
ENV GENERATE_SOURCEMAP=false
ENV CI=false
ENV SKIP_PREFLIGHT_CHECK=true
RUN npm run build

# ============================================================
# Runtime: nginx
# ============================================================
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
        proxy_pass http://central-lb; \
        proxy_set_header Host \$host; \
        proxy_set_header X-Real-IP \$remote_addr; \
        proxy_set_header X-Forwarded-For \$proxy_add_x_forwarded_for; \
        proxy_set_header X-Forwarded-Proto \$scheme; \
    } \
}" > /etc/nginx/conf.d/default.conf

EXPOSE 80

CMD ["nginx", "-g", "daemon off;"]
