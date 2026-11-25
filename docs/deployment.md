# 部署指南

## 架构

```
浏览器
  │  :80
  ▼
Nginx（web 容器）
  ├─ /            前端静态资源（SPA 回退）
  ├─ /api/        → backend:8080
  ├─ /swagger-ui/ → backend:8080
  └─ /actuator/   → backend:8080
                    │
                    ▼
              backend（Spring Boot）
                    │ JDBC
                    ▼
              mysql:8.0（数据卷 mysql-data）
```

## 前置条件

- Docker Engine 24+ 与 Docker Compose v2
- 服务器建议 2 核 4G 起步；开放 80 端口（HTTPS 需 443）

## 快速部署

```bash
cp .env.example .env
# 编辑 .env：至少修改 DB_PASSWORD、JWT_SECRET
# 生成密钥：openssl rand -base64 48

docker compose up -d --build
```

启动后：

| 地址 | 说明 |
| --- | --- |
| <http://localhost> | 前端（用户端 / 护工端 / 管理后台） |
| <http://localhost:8080/swagger-ui.html> | 接口调试（演示环境建议保留，生产可关闭） |
| <http://localhost:8080/actuator/health> | 健康检查 |

首次启动时会自动执行 Flyway 迁移（建表 + 种子数据）。

### 演示账号

| 手机号 | 角色 |
| --- | --- |
| 13800000002 | 普通用户 |
| 13800000001 / 13800000003 | 护工 |
| 13800000000 | 管理员 |

验证码：

- 在 `.env` 中设置 `SMS_FIXED_CODE=123456` 后可直接用固定码登录（仅限演示）
- 留空时为随机码，通过 `docker compose logs backend | grep "Mock 短信"` 查看
- 真实短信服务需要实现 `SmsSender` 适配器（当前仅 Mock）

## 环境变量

| 变量 | 必填 | 说明 |
| --- | --- | --- |
| `DB_PASSWORD` | ✅ | MySQL root 密码，同时用于后端连接 |
| `JWT_SECRET` | ✅ | JWT 签名密钥，至少 32 字节 |
| `JWT_EXPIRE_MINUTES` | | 令牌有效期，默认 10080（7 天） |
| `SMS_FIXED_CODE` | | 固定验证码，生产必须留空 |
| `PUBLIC_ORIGIN` | | 对外访问地址，用于 CORS，例如 `https://caregiver.example.com` |
| `MOCK_PAYMENT_SECRET` | | Mock 支付回调签名密钥 |

## 生产安全清单

- [ ] 修改 `DB_PASSWORD`、`JWT_SECRET`、`MOCK_PAYMENT_SECRET`，不要使用示例值
- [ ] `SMS_FIXED_CODE` 置空，并接入真实短信服务
- [ ] 配置 HTTPS（Nginx 前置 443 或云负载均衡），`PUBLIC_ORIGIN` 改为 https 域名
- [ ] 如不需要在线调试，移除 `backend` 的 `8080:8080` 端口映射并关闭 Swagger 路由
- [ ] 数据库不要暴露到公网（compose 未映射 3306）
- [ ] 定期备份 `mysql-data` 卷
- [ ] 接入真实支付前，替换 `MockPaymentGateway` 并完成对账/退款测试

## 数据备份与恢复

```bash
# 备份
docker compose exec -T mysql sh -c \
  'exec mysqldump -uroot -p"$MYSQL_ROOT_PASSWORD" --databases caregiver_platform' \
  > backup-$(date +%F).sql

# 恢复
docker compose exec -T mysql sh -c \
  'exec mysql -uroot -p"$MYSQL_ROOT_PASSWORD" caregiver_platform' < backup-2026-01-01.sql
```

数据卷位置：`docker volume inspect caregiver-platform_mysql-data`。

## 升级

```bash
git pull
docker compose up -d --build
# Flyway 会在后端启动时自动执行新增迁移
```

回滚：切换回上一个 tag 重新构建，或使用 GHCR 上对应版本的镜像
（`ghcr.io/<owner>/caregiver-platform/backend:<tag>`）。

## 云服务器部署要点

1. 安装 Docker 与 Compose 插件
2. 拉取代码，按“快速部署”启动
3. 安全组放行 80/443；不要放行 3306
4. 域名解析到服务器后，用 Cloudflare / Certbot / 云负载均衡配置 HTTPS
5. 将 `PUBLIC_ORIGIN` 改为 `https://你的域名`，重启 backend

## 本地非 Docker 开发

```bash
# 后端（需要本机 MySQL 8）
cd backend
./mvnw spring-boot:run

# 前端
cd frontend
npm install
npm run dev   # http://localhost:5173，代理 /api 到 8080
```

## 故障排查

| 现象 | 排查 |
| --- | --- |
| `backend` 反复重启 | `docker compose logs backend`；确认 MySQL 健康、`DB_PASSWORD` 一致 |
| 页面 502 | backend 未就绪或崩溃；检查 `/actuator/health` |
| 登录提示验证码错误 | 演示需设置 `SMS_FIXED_CODE=123456` 并重启 backend |
| 跨域错误 | `PUBLIC_ORIGIN` 与实际访问地址不一致 |
| Flyway 报 “non-empty schema” | 使用了旧库且没有历史表；备份后重建数据库 |
| 80 端口被占用 | 修改 compose 中 `web.ports` 映射，如 `8081:80` |

## 镜像与发布

- 打 tag 触发 Release 工作流：构建 `backend` / `frontend` 镜像并推送到 GHCR，同时创建 GitHub Release
- 也可以在 CI 的 `Docker images` 任务中确认镜像可构建（不推送）

## 已知限制

- 支付、短信为 Mock 实现，不具备真实资金与短信能力
- 单实例部署；多实例需要把验证码缓存迁移到 Redis，并配置共享 JWT 密钥
- 未包含 APK 打包（可后续用 Capacitor 封装前端）
