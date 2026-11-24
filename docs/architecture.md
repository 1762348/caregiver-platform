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

## 关键业务设计（P3 落地）

1. **名额原子扣减**：`UPDATE caregiver_schedules SET remaining_quota = remaining_quota - 1 WHERE id = ? AND remaining_quota > 0`，受影响行数为 0 即失败，杜绝超卖
2. **订单状态机**：`PENDING_PAYMENT → PAID → IN_SERVICE → COMPLETED`，`PENDING_PAYMENT → CANCELLED`，非法流转直接拒绝
3. **支付超时**：下单 30 分钟未支付自动取消并释放名额（定时任务 + 查询时惰性补偿）
4. **取消规则**：服务开始前 2 小时免费取消；2 小时内收取 20% 手续费；开始后不可取消
5. **幂等**：支付回调按 `payment_no` 去重，取消/完成接口校验前置状态
6. **审计**：订单状态流转写入 `order_status_logs`，支付结果写入 `payment_records`

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
- 不做即时通讯，服务进度采用轮询（30s）而非 WebSocket
- 不在本期实现评价体系与优惠券
