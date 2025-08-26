# AICK-MMP 登录问题解决指南

## 🚨 常见登录问题及解决方案

### 1. 无法连接到后端API

**症状：**
- 前端显示网络连接错误
- 浏览器开发者工具显示无法连接到 `localhost:8080`

**解决方案：**
```bash
# 检查后端服务状态
docker-compose ps

# 如果后端服务未运行，启动它
docker-compose up -d backend-1

# 检查端口是否被占用
lsof -i :8080

# 重启所有服务
docker-compose restart
```

### 2. 用户名或密码错误

**症状：**
- 输入正确的用户名密码仍提示错误
- 后端返回401或500错误

**解决方案：**
```bash
# 检查数据库中的用户数据
docker-compose exec mysql mysql -u aickuser -paickpassword aick_mmp -e "
SELECT username, role, status, enabled FROM users WHERE username='admin';
"

# 如果没有用户数据，重新初始化
docker-compose restart backend-1

# 检查密码加密是否正确（应该是bcrypt格式）
docker-compose exec mysql mysql -u aickuser -paickpassword aick_mmp -e "
SELECT username, password FROM users WHERE username='admin';
"
```

**默认登录凭据：**
- 用户名: `admin`
- 密码: `admin123`

### 3. JWT Token相关问题

**症状：**
- 登录后立即被要求重新登录
- Token验证失败

**解决方案：**
```bash
# 检查JWT配置
grep -A 5 "jwt:" backend/src/main/resources/application.yml

# 测试JWT生成
curl -X POST http://localhost:8080/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{"username":"admin","password":"admin123"}' | jq
```

### 4. CORS跨域问题

**症状：**
- 浏览器控制台显示CORS错误
- OPTIONS请求失败

**解决方案：**
- CORS配置已在 `SecurityConfig.java` 中正确设置
- 如果仍有问题，检查前端请求URL是否正确

### 5. 数据库连接问题

**症状：**
- 后端启动失败
- 无法保存用户数据

**解决方案：**
```bash
# 检查MySQL容器状态
docker-compose logs mysql

# 重启数据库
docker-compose restart mysql

# 手动连接测试
docker-compose exec mysql mysql -u aickuser -paickpassword aick_mmp -e "SELECT 1;"
```

## 🔧 快速修复命令

### 完全重置系统
```bash
cd /path/to/aick-mmp
docker-compose down
docker-compose up --build -d
```

### 仅重启后端服务
```bash
docker-compose restart backend-1 backend-2
```

### 查看实时日志
```bash
docker-compose logs -f backend-1
```

### 测试登录功能
```bash
./scripts/test-login.sh
```

## 📋 检查清单

在报告问题之前，请确认以下几点：

- [ ] 所有Docker容器正在运行 (`docker-compose ps`)
- [ ] MySQL数据库可以连接
- [ ] 后端服务响应健康检查 (`curl http://localhost:8080/actuator/health`)
- [ ] 前端服务可以访问 (`curl http://localhost`)
- [ ] 数据库中存在默认admin用户
- [ ] JWT配置正确
- [ ] 网络端口未被占用

## 🐛 调试技巧

### 1. 查看详细错误信息
```bash
# 后端日志
docker-compose logs backend-1 --tail=100

# 前端网络请求（在浏览器开发者工具中查看Network标签）
```

### 2. 手动API测试
```bash
# 测试登录API
curl -X POST http://localhost:8080/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{"username":"admin","password":"admin123"}' \
  -v

# 测试健康检查
curl http://localhost:8080/actuator/health
```

### 3. 数据库查询
```bash
# 进入数据库
docker-compose exec mysql mysql -u aickuser -paickpassword aick_mmp

# 查看所有用户
SELECT * FROM users;

# 查看用户角色和状态
SELECT username, role, status, enabled, created_at FROM users;
```

## 📞 获取帮助

如果上述解决方案都无法解决问题，请：

1. 运行诊断脚本：`./scripts/test-login.sh`
2. 收集日志信息：`docker-compose logs > logs.txt`
3. 提供错误的详细描述和重现步骤
4. 包含系统环境信息（操作系统、Docker版本等）

---

**最后更新时间：** $(date)
**版本：** v1.0