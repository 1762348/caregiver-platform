# 设计决策记录

记录与《系统设计说明书》不一致、或文档未覆盖的实现取舍，便于答辩与后续维护时追溯。

## 1. 时段拆分为开始/结束时间

- **文档**：`caregiver_base_shifts.time_range varchar(20)`、`caregiver_schedules.time_range varchar(20)`，形如 `09:00-11:00`
- **实现**：`start_time TIME` + `end_time TIME`
- **原因**：字符串时段无法做索引比较、冲突检测与跨天校验；拆成 TIME 后可直接比较、排序、计算时长
- **展示**：接口层拼装 `HH:mm-HH:mm`，前端无需解析

## 2. 枚举使用大写编码，不使用 MySQL ENUM

| 字段 | 文档取值 | 实现取值 |
| --- | --- | --- |
| `users.role` | `enum('user','caregiver','admin')` | `USER` / `CAREGIVER` / `ADMIN` |
| `caregivers.gender` | `enum('男','女')` | `MALE` / `FEMALE` |
| `caregivers.status` | `enum('启用','停用')` | `ACTIVE` / `INACTIVE` |
| `categories.status` | `enum('启用','停用')` | `ACTIVE` / `INACTIVE` |
| `orders.status` | 文档未列全 | `PENDING_PAYMENT` / `PAID` / `IN_SERVICE` / `COMPLETED` / `CANCELLED` |

- **原因**：MySQL ENUM 修改成本高，且 H2（测试库）不支持完全一致的语义；`VARCHAR + CHECK` 在两种库上行为一致
- **影响**：中文文案由接口层/前端负责翻译，数据库只存稳定编码

## 3. 身份证派生字段改为应用层计算

- **文档**：`receivers.gender`、`receivers.birth` 由数据库生成列自动计算
- **实现**：应用层解析身份证后写入普通字段（`IdCardUtils`，P3 提供），数据库仅做 18 位长度约束
- **原因**：MySQL 的 `STR_TO_DATE` / 生成列语法无法在 H2 上执行，会导致迁移脚本只能跑一种库；且生成列无法处理非法身份证的兜底
- **补偿**：应用层统一校验身份证格式与校验位，写入前计算性别与出生日期

## 4. 新增两张扩展表

文档只有 10 张表，实际实现补充：

- `payment_records`：支付流水。一个订单可有多条（失败重试、退款），支付回调按 `payment_no` 幂等
- `order_status_logs`：订单状态流转日志。对应非功能需求“订单状态变更必须记录完整流转日志”

两张表都通过外键关联 `orders`，订单删除时级联清理。

## 5. 补充的时间与资料字段

- `users.avatar`、`users.updated_at`：个人中心需要头像与更新时间
- `categories.updated_at`、`caregivers.updated_at`：管理端编辑后需要审计时间
- `orders.updated_at`：状态变更时间（精确的每次流转见 `order_status_logs`）

这些字段不影响文档中的核心字段。

## 6. `is_read` 使用 INT 而非 TINYINT(1)

- **文档**：`messages.is_read tinyint(1)`
- **实现**：`INT NOT NULL DEFAULT 0`，实体使用 `Integer`
- **原因**：`ddl-auto=validate` 下，MySQL 的 `TINYINT` 与 Hibernate 的 `Boolean` 映射会出现类型告警；用 INT + Integer 在 MySQL / H2 上都稳定

## 7. 实体使用外键 ID，不使用 JPA 关联对象

- **实现**：`Caregiver.userId`、`Order.scheduleId` 等均为 `Long`，没有 `@ManyToOne`
- **原因**：
  - 避免懒加载异常与 N+1，查询由 Repository 显式控制
  - 与文档给出的表结构一一对应，迁移与排查更直观
  - DTO 组装在 Service 层完成，边界清晰
- **代价**：需要手写少量联表查询（P3 用 JPQL 实现）

## 8. 基础数据与测试账号

`V2__seed.sql` 写入可上线的字典数据与演示账号：

| 手机号 | 角色 | 说明 |
| --- | --- | --- |
| 13800000000 | ADMIN | 平台管理员 |
| 13800000001 | CAREGIVER | 李秀兰（CG0001） |
| 13800000003 | CAREGIVER | 王芳（CG0002） |
| 13800000002 | USER | 张三 |

排班与订单不预置：排班依赖具体日期，由管理端生成（P5）；订单由业务产生。

## 9. Spring Boot 4 的两个迁移注意点

Boot 4 对自动配置做了模块化拆分，踩到两处坑，记录避免后续重复：

- **Flyway**：只引 `flyway-core` 不会触发迁移，需要额外引入 `org.springframework.boot:spring-boot-flyway`
- **Jackson**：默认序列化器已升级为 Jackson 3（`tools.jackson.databind.ObjectMapper`），
  Security 的 401/403 响应处理器必须注入 Jackson 3 的 `ObjectMapper`，注入 Jackson 2 会启动失败

## 10. 认证实现说明

- 令牌使用 HS256，密钥从 `JWT_SECRET` 读取，启动时校验至少 32 字节，避免弱密钥
- 权限码在登录时查库并写入 JWT 的 `perms` 声明；权限调整后需重新登录才会生效（当前取舍，后续可改为按请求加载）
- 验证码存于单机内存（`SmsCodeService`），多实例部署需迁移到 Redis
- 开发环境固定验证码由 `app.sms.fixed-code` 控制，生产必须置空
