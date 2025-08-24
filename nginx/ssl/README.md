# SSL证书配置说明

## 安全注意事项
本目录存放边缘节点和中心节点负载均衡器所需的TLS证书文件。在生产环境中，您必须使用由可信CA颁发的证书，替换当前的自签名证书占位文件。

## 文件列表
- `cert.pem`: TLS证书文件
- `key.pem`: 私钥文件

## 生成自签名证书（仅开发测试用）
```bash
# 生成2048位RSA密钥和自签名证书，有效期365天
openssl req -x509 -newkey rsa:2048 -nodes -keyout key.pem -out cert.pem -days 365 -subj '/CN=localhost'
```

## 生产环境建议
1. 从可信CA获取证书（如Let's Encrypt）
2. 配置自动续期机制
3. 定期轮换密钥
4. 保护私钥文件权限（仅root可读取）