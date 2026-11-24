# 开发路线图

每个阶段都以「可验证的完成标准」收尾，避免出现“代码写完但跑不起来”的状态。

## P0 基建 ✅

- [x] 仓库结构：`backend` / `frontend` / `docs` / `.github`
- [x] 后端骨架：Spring Boot 4、分环境配置、统一响应与异常、`/api/v1/meta`
- [x] 前端骨架：Vue 3 + TS + Vite + Pinia + Router + Vant + Axios 封装
- [x] 首页 / 登录 / 订单 / 我的 / 404 页面骨架
- [x] CI：后端 `mvn verify`、前端 `npm ci && test && build`
- [x] README、架构文档、贡献指南、MIT 许可证

**验收**：干净环境 `mvn verify` 与 `npm run build` 通过；前端首页能显示后端连接状态。

## P1 数据层

- [ ] Flyway `V1__schema.sql`：10 张业务表 + `payment_records`、`order_status_logs`
- [ ] `V2__seed.sql`：服务类别、权限、测试账号、示例护工与排班
- [ ] 实体 / Repository 重写，字段与设计文档对齐
- [ ] 测试：Repository 集成测试（H2）+ 迁移脚本可重复执行
- [ ] 打开 `spring.flyway.enabled`，`ddl-auto` 改为 `validate`

**验收**：空库执行迁移后表结构完整；重复执行不报错；H2 与 MySQL 均通过。

## P2 认证授权

- [ ] 短信验证码：`SmsSender` 接口 + Mock 实现（验证码固定/随机，日志输出）
- [ ] 登录 / 刷新令牌 / 当前用户接口
- [ ] Spring Security + JWT 过滤器 + 三角色（user / caregiver / admin）
- [ ] 权限注解与越权测试
- [ ] 前端登录联调、路由守卫、令牌持久化

**验收**：未登录 401、越权 403、三种角色可登录并拿到对应权限。

## P3 核心业务

- [ ] 服务类别查询、护工列表与详情、排班查询
- [ ] 预约下单（名额原子扣减、同人同时段唯一）
- [ ] 支付（Mock 网关 + 回调 + 幂等）、超时自动取消
- [ ] 取消与退款计算（2 小时 / 20% 规则）
- [ ] 服务进度状态机 + 护工状态更新
- [ ] 站内消息与未读数
- [ ] 单元测试 + 并发抢名额测试

**验收**：脚本化跑通“登录→查排班→预约→支付→进度→完成/取消”；并发抢最后名额仅 1 单成功。

## P4 用户端页面

- [ ] 登录 / 首页 / 服务列表 / 排班选择 / 预约四步向导
- [ ] 支付流程 / 订单列表与详情 / 取消流程
- [ ] 服务进度页（30s 轮询）
- [ ] 被照顾人管理 / 个人中心 / 消息中心

**验收**：浏览器真机尺寸下完成全流程，无 mock 数据残留。

## P5 护工端与管理端

- [ ] 护工端：我的排班、今日订单、状态更新、消息
- [ ] 管理端：服务类别 CRUD、护工管理、排班批量维护、名额调整、停班处理
- [ ] 管理端接入 Element Plus（懒加载，不影响移动端体积）

**验收**：管理员改排班后用户端即时可见；停班后相关订单进入改约/退款流程。

## P6 质量

- [ ] 核心 Service 覆盖率 ≥ 70%
- [ ] 前端组件与工具函数测试
- [ ] E2E 冒烟（Playwright，可选）
- [ ] 统一日志、错误码文档、Swagger 完整

**验收**：CI 全绿，覆盖率达标，`docs/api.md` 与实际接口一致。

## P7 部署与发布

- [ ] 后端 / 前端 Dockerfile + `docker-compose.yml`（mysql + backend + nginx）
- [ ] GitHub Actions 构建镜像并推送 GHCR
- [ ] 生产 Profile、健康检查、备份脚本
- [ ] 截图、演示地址、v1.0.0 Release
- [ ] Capacitor 打包 Android APK（可选）

**验收**：新机器 `docker compose up -d` 一键可用；Release 附带部署说明与截图。
