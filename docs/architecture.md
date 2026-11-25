# 架构说明

## 总览

```
┌──────────────────────┐        HTTPS / JSON        ┌──────────────────────┐
│  Vue 3 前端（移动优先） │ ─────────────────────────▶ │  Spring Boot 4 后端   │
│  Vant + Pinia + Axios │ ◀───────────────────────── │  REST /api/v1/**     │
└──────────────────────┘                            └──────────┬───────────┘
                                                               │ JDBC
                                                    ┌──────────▼───────────┐
                                                    │  MySQL 8（生产/开发）  │
                                                    │  H2（自动化测试）      │
                                                    └──────────────────────┘
```

- 前后端分离，前端静态资源由 Nginx 托管（P7），`/api` 反向代理到后端
- 后端无状态，认证使用 JWT（P2），便于横向扩容
- 数据库结构由 Flyway 管理（P1），禁止 `ddl-auto=update`

## 后端分层

```
com.youhu.caregiver
├─ common
│  ├─ api          ApiResponse 统一响应
│  └─ exception    BusinessException + 全局异常处理
├─ config          安全、跨域、OpenAPI、Jackson 配置
├─ security        JWT 过滤器、登录用户上下文（P2）
├─ user            账号与认证
├─ category        服务类别
├─ caregiver       护工、基础班次、排班
├─ receiver        被照顾人档案
├─ order           预约、状态机、取消退款
├─ payment         支付网关抽象与 Mock 实现（P3）
├─ message         站内通知
└─ admin           管理端接口（P5）
```

约定：

- **Controller** 只负责参数绑定、权限校验与编排，不写业务规则
- **Service** 承载业务规则与事务边界，返回领域对象或 DTO
- **Repository** 只做数据访问，不包含业务判断
- 对外统一返回 `ApiResponse<T>`，异常由 `GlobalExceptionHandler` 转换

## 关键业务设计（已落地）

1. **名额原子扣减**：`UPDATE caregiver_schedules SET remaining_quota = remaining_quota - 1 WHERE id = ? AND remaining_quota > 0`，受影响行数为 0 即失败，杜绝超卖（`ScheduleQuotaTest` 覆盖）
2. **订单状态机**：`PENDING_PAYMENT → PAID → IN_SERVICE → COMPLETED`，`PENDING_PAYMENT / PAID → CANCELLED`，非法流转直接拒绝
3. **支付超时**：下单 30 分钟未支付，定时任务自动取消并释放名额；支付接口也会校验是否超时
4. **取消规则**：服务开始前 2 小时免费取消；2 小时内收取 20% 手续费；开始后不可取消。已支付订单退款写支付流水，未支付订单只释放名额
5. **幂等**：支付回调按 `payment_no` 去重；取消/进度接口校验前置状态，重复操作返回 409
6. **审计**：订单状态流转写入 `order_status_logs`，支付结果写入 `payment_records`
7. **服务进度**：护工按“出发 → 到达 → 开始 → 完成”顺序更新，用户侧由时间戳派生 `stage`（待出发/路上/已到达/服务中/已完成/已取消）
8. **消息**：预约、支付、取消与进度节点写入站内消息，支持未读数与已读状态

## 认证与授权

- **登录**：手机号 + 短信验证码；未注册自动创建 `USER` 账号。开发环境由 `MockSmsSender` 输出固定验证码
- **令牌**：HS256 JWT，默认 7 天；claims 包含 `sub`（用户 ID）、`role`、`perms`、`phone`
- **鉴权**：`JwtAuthenticationFilter` 解析令牌并写入 `SecurityContext`；
  `ROLE_<角色>` 与权限码同时作为 authority，接口可用 `hasRole` / `hasAuthority` 控制
- **无状态**：不创建 Session，便于水平扩展；令牌续签走 `POST /api/v1/auth/refresh`
- **统一错误**：未登录 401、权限不足 403，均返回 `ApiResponse` JSON，避免前端拿到 HTML 错误页
- **公开接口**：登录相关、目录查询（类别/护工/排班）、支付回调、`/api/v1/meta`、Actuator 健康检查、CORS 预检

> 验证码目前存于单机内存（`SmsCodeService`），多实例部署时需替换为 Redis；
> 生产环境必须关闭固定验证码，并实现真实短信服务商适配器。

## 前端结构

- **三套布局**：
  - 用户端 `MobileLayout`：480px 手机壳层 + 底部导航（首页 / 订单 / 我的）
  - 护工端 `CaregiverLayout`：同样手机壳层，底部导航（工作台 / 我的订单）
  - 管理端 `AdminLayout`：全宽桌面布局 + 左侧导航，Element Plus 组件按路由懒加载
- **路由与角色**：路由 `meta.roles` 声明可访问角色，全局守卫在登录后按角色分流
  （ADMIN → `/admin/categories`，CAREGIVER → `/caregiver`，USER → `/`）；越权访问跳回各自首页
- **状态**：`auth` store 负责令牌与用户信息（localStorage 持久化，刷新后调用 `/auth/me` 恢复）；页面数据由各页面直接调用 API
- **请求层**：`api/http.ts` 统一注入 Bearer Token、解包 `ApiResponse`、401 自动清理并跳转登录
- **交互**：支付用 ActionSheet；取消/完成用确认弹窗；服务进度每 15 秒轮询，离开页面清理定时器
- **样式**：CSS 变量 + scoped 样式，用户/护工端主色 `#0891b2`，管理端深色侧边栏 + Element Plus

## 数据模型

已落地 12 张表（设计文档 10 张 + 2 张审计扩展），完整字段与 ER 图见 [数据库说明](database.md)：

`users` · `caregivers` · `categories` · `caregiver_base_shifts` · `caregiver_schedules`
`receivers` · `orders` · `messages` · `permissions` · `role_permissions`
`payment_records` · `order_status_logs`

与设计文档的差异（时段拆分、枚举编码、身份证派生字段等）统一记录在
[设计决策记录](design-decisions.md)，避免“文档与实现两张皮”。

## 环境与配置

| 环境 | Profile | 数据库 | 用途 |
| --- | --- | --- | --- |
| 本地开发 | `dev` | MySQL 8 | 联调 |
| 自动化测试 | `test` | H2（MySQL 模式） | CI，无外部依赖 |
| 生产 | `prod`（P7） | MySQL 8 | 容器部署 |

## 非目标

- 不做真实支付清结算（仅适配器 + 回调流程）
- 不做即时通讯，服务进度采用轮询（30 秒）而非 WebSocket
- 不在本期实现评价体系与优惠券

> 支付与短信为可替换的 Mock 适配器；上线前需完成资质申请、压测与安全评估。
