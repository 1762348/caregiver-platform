# 面试讲解与常见追问

## 简历项目描述（可直接用）

**优护 · 社区护工预约平台**（个人全栈项目）
Spring Boot 4 + MySQL + Vue 3 + TypeScript 的护工预约系统，覆盖用户、护工、管理员三角色。
实现排班查询、预约锁名额、支付回调、取消退款、服务进度与站内消息；用 Flyway 管理 12 张表，
JWT 无状态鉴权，JUnit + Vitest 共 75 个测试，CI 含覆盖率门槛与 Docker 构建，Docker Compose 一键部署。

> 关键词：Spring Boot、JPA、Flyway、Spring Security、JWT、MySQL、Vue 3、TypeScript、Vite、Pinia、Docker、GitHub Actions

## 一分钟讲清楚架构

- 前端：Vue 3 + TS，三套布局（用户端/护工端/管理端），Axios 统一封装，路由按角色守卫
- 后端：Controller → Service → Repository，统一 `ApiResponse`，全局异常处理，Flyway 迁移
- 数据：12 张表，订单状态机 + 状态流转日志 + 支付流水
- 部署：多阶段 Dockerfile，Nginx 托管前端并反代 `/api`，Compose 编排 MySQL/后端/前端

## 五个技术亮点（面试官最可能问）

### 1. 怎么防止预约超卖？

名额扣减不是“先查再改”，而是一条带条件的更新：

```sql
UPDATE caregiver_schedules
   SET remaining_quota = remaining_quota - 1
 WHERE id = ? AND remaining_quota > 0
```

受影响行数为 0 就说明被抢光，直接返回 409。写了一个并发测试：剩余 1 个名额、6 个线程同时下单，
断言只有 1 单成功、名额归零。

**追问：分布式部署还成立吗？** 成立，单条 SQL 的原子性由数据库保证；如果要跨库/跨服务，再引入分布式锁或库存服务。

### 2. 订单状态怎么保证不乱？

`OrderStatus` 枚举里定义合法流转（待支付 → 已支付 → 服务中 → 已完成，待支付/已支付 → 已取消），
Service 在每次变更前校验，并把 `from_status / to_status / operator / remark` 写进 `order_status_logs`。
取消、支付、进度接口都做了前置状态校验，重复操作返回 409。

### 3. 退款规则怎么落地？

服务开始前 2 小时以上全额，2 小时内收 20% 手续费，开始后不可取消。
`RefundCalculator` 负责计算，取消在一个事务里完成：改状态 → 写日志 → 释放名额 → 调退款 → 发消息；
退款失败会整体回滚，避免“订单取消了但钱没退”。停班是管理员强制取消，全额退款。

### 4. 鉴权怎么做的？

手机号 + 验证码登录，服务端签发 HS256 JWT，claims 里带 `sub/role/perms`。
`JwtAuthenticationFilter` 解析后写入 `SecurityContext`，权限码同时作为 authority；
`/api/v1/admin/**` 要求 ADMIN，护工接口要求 CAREGIVER，401/403 都返回统一 JSON。

**追问：令牌泄露/注销怎么办？** 当前是无状态 JWT，注销靠客户端删 token；生产可以加短有效期 + 刷新令牌 + 黑名单（Redis）。

### 5. 测试和工程化做了什么？

- 后端 58 个测试：退款规则、订单全流程、并发抢名额、停班退款、角色隔离、迁移种子
- 覆盖率：JaCoCo 行 70.8% / 分支 47.8%，`verify` 阶段低于阈值直接失败
- 前端 17 个测试：工具函数、认证 store、首页组件；ESLint + Prettier + `vue-tsc` 全绿
- CI 三个 Job：后端 `mvn verify`、前端 `lint/format/test/build`、Docker 镜像构建
- 打 tag 自动推 GHCR 镜像并创建 Release

## 可能的追问与回答

**为什么用 JPA 而不是 MyBatis？**
业务以单表 CRUD 和简单关联为主，JPA 开发快、迁移方便；复杂查询用 `@Query`。如果 SQL 很重，会换 MyBatis。

**为什么用 H2 做测试？**
测试不依赖外部 MySQL，CI 更快；Flyway 在 H2 的 MySQL 模式下真实执行，`ddl-auto=validate` 保证实体与表一致。
生产仍是 MySQL 8。

**验证码为什么存内存？**
单机演示足够；多实例会换 Redis，并加发送频控与图形验证码。

**支付是 Mock 的，算完成吗？**
支付网关抽象了 `charge/refund/verifyCallback`，Mock 实现把流水、回调验签、幂等、退款链路都跑通；
接真实微信/支付宝只需新增实现类，业务代码不动。

**如果要做分布式/高并发？**
名额更新已经是数据库原子操作；进一步可以做库存预热、Redis 限流、消息队列异步通知、读写分离。

**为什么管理端用 Element Plus，用户端用 Vant？**
用户端是移动优先，Vant 更贴合；管理端是桌面表格操作，Element Plus 更合适。管理端路由懒加载，不影响用户端首屏。

## 演示时的注意事项

- 用固定验证码 `123456` 的演示环境，不要公网暴露管理员账号
- 提前准备好待支付订单、待服务订单，避免现场等数据
- 录屏按 [demo-script.md](demo-script.md) 走，控制在 2–3 分钟
