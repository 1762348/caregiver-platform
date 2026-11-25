# 接口文档

- Base URL：`http://localhost:8080`
- 统一前缀：`/api/v1`
- 内容类型：`application/json; charset=utf-8`
- 认证方式：`Authorization: Bearer <token>`

## 统一响应

```json
{
  "code": 200,
  "message": "操作成功",
  "data": {}
}
```

`code` 与 HTTP 状态码保持一致：

| code | 含义 |
| --- | --- |
| 200 | 成功 |
| 400 | 参数错误 / 验证码错误 |
| 401 | 未登录或令牌失效 |
| 403 | 已登录但权限不足 |
| 404 | 资源不存在 |
| 409 | 状态冲突（如名额已满） |
| 429 | 请求过于频繁 |
| 500 | 服务端异常 |

## 认证

### 发送验证码

`POST /api/v1/auth/sms-code`

```json
{ "phone": "13800000002" }
```

- 开发环境固定验证码 `123456`（`app.sms.fixed-code`）
- 同一手机号 60 秒内只能发送一次，否则返回 429

### 登录 / 注册

`POST /api/v1/auth/login`

```json
{ "phone": "13800000002", "code": "123456" }
```

响应：

```json
{
  "code": 200,
  "message": "操作成功",
  "data": {
    "token": "eyJhbGciOiJIUzI1NiJ9...",
    "tokenType": "Bearer",
    "expiresIn": 604800,
    "user": {
      "id": 3,
      "phone": "13800000002",
      "name": "张三",
      "role": "USER",
      "avatar": null,
      "permissions": ["caregiver:view", "order:view"]
    }
  }
}
```

- 手机号未注册时自动创建 `USER` 账号
- 验证码有效期 5 分钟，最多校验 5 次

### 续签令牌

`POST /api/v1/auth/refresh`（需要登录）

返回结构同登录。

### 当前用户

`GET /api/v1/auth/me`（需要登录）

```json
{
  "code": 200,
  "message": "操作成功",
  "data": {
    "id": 3,
    "phone": "13800000002",
    "name": "张三",
    "role": "USER",
    "avatar": null,
    "permissions": ["caregiver:view", "order:view"]
  }
}
```

## 系统

| 接口 | 说明 | 是否需要登录 |
| --- | --- | --- |
| `GET /api/v1/meta` | 服务名称、版本、当前环境 | 否 |
| `GET /actuator/health` | 健康检查 | 否 |

## 角色与权限

| 角色 | 编码 | 默认权限 |
| --- | --- | --- |
| 普通用户 | `USER` | `caregiver:view`、`order:view` |
| 护工 | `CAREGIVER` | `caregiver:view`、`order:view`、`order:edit` |
| 管理员 | `ADMIN` | 全部 7 项 |

权限码随 JWT 的 `perms` 声明下发，同时映射为 Spring Security 的 authority，
后续业务接口可用 `@PreAuthorize("hasRole('ADMIN')")` 或 `hasAuthority('order:edit')` 控制。

## 演示账号

| 手机号 | 角色 | 说明 |
| --- | --- | --- |
| 13800000000 | ADMIN | 平台管理员 |
| 13800000001 | CAREGIVER | 李秀兰 |
| 13800000003 | CAREGIVER | 王芳 |
| 13800000002 | USER | 张三 |

## 服务目录（公开）

| 接口 | 说明 |
| --- | --- |
| `GET /api/v1/categories` | 启用中的服务类别，按基础价格升序 |
| `GET /api/v1/caregivers?categoryId=&page=0&size=10` | 护工分页列表，可按类别筛选 |
| `GET /api/v1/caregivers/{id}` | 护工详情（资质、简介、评分） |
| `GET /api/v1/schedules?categoryId=&date=2026-12-01` | 指定日期可预约排班，过滤已满/已开始 |

排班项示例：

```json
{
  "id": 2,
  "caregiverId": 1,
  "caregiverName": "李秀兰",
  "categoryId": 1,
  "categoryName": "日常照料",
  "serviceDate": "2026-12-01",
  "startTime": "08:00",
  "endTime": "12:00",
  "timeRange": "08:00-12:00",
  "remainingQuota": 3,
  "totalQuota": 3,
  "price": 200.00
}
```

## 被照顾人（需登录）

| 接口 | 说明 |
| --- | --- |
| `GET /api/v1/receivers` | 我的被照顾人列表 |
| `POST /api/v1/receivers` | 新增（最多 10 人，身份证唯一） |
| `PUT /api/v1/receivers/{id}` | 修改 |
| `DELETE /api/v1/receivers/{id}` | 删除（存在订单时拒绝） |

请求体：

```json
{
  "name": "张三",
  "idCard": "50010119900101123X",
  "phone": "13800138000",
  "relation": "本人",
  "health": "高血压",
  "special": "低盐饮食"
}
```

身份证会校验 18 位与校验位，并自动解析性别、出生日期。

## 订单（需登录）

| 接口 | 说明 |
| --- | --- |
| `POST /api/v1/orders` | 预约下单，锁定名额，返回待支付订单 |
| `GET /api/v1/orders?status=&page=0&size=10` | 我的订单（可按状态筛选） |
| `GET /api/v1/orders/{id}` | 订单详情（含服务进度） |
| `POST /api/v1/orders/{id}/pay` | 支付，`{"channel":"WECHAT"}` / `ALIPAY` / `MOCK` |
| `POST /api/v1/orders/{id}/cancel` | 取消，`{"reason":"临时有事"}`，返回退款金额 |

下单请求：

```json
{ "scheduleId": 2, "receiverId": 1 }
```

订单状态与用户可见阶段：

| status | 含义 | stage（派生） |
| --- | --- | --- |
| `PENDING_PAYMENT` | 待支付（30 分钟内有效） | `PENDING_PAYMENT` |
| `PAID` | 已支付，等待服务 | `PENDING_DEPARTURE` / `ON_THE_WAY` / `ARRIVED` |
| `IN_SERVICE` | 服务中 | `IN_SERVICE` |
| `COMPLETED` | 已完成 | `COMPLETED` |
| `CANCELLED` | 已取消 | `CANCELLED` |

取消退款规则：

- 服务开始前 2 小时以上：全额退款
- 服务开始前 2 小时内：退还 80%（收取 20% 手续费）
- 服务已开始：不可取消
- 未支付订单取消：不产生退款，直接释放名额

取消响应示例：

```json
{
  "code": 200,
  "message": "操作成功",
  "data": {
    "order": { "id": 1, "status": "CANCELLED", "stage": "CANCELLED", "...": "..." },
    "refundAmount": 160.00,
    "feeApplied": true
  }
}
```

## 护工端（需 `CAREGIVER` 角色）

| 接口 | 说明 |
| --- | --- |
| `GET /api/v1/caregiver/me` | 护工个人资料（工号、类别、评分） |
| `GET /api/v1/caregiver/schedules?from=&to=` | 自己的排班（默认未来 14 天） |
| `GET /api/v1/caregiver/orders` | 我的服务订单 |
| `POST /api/v1/caregiver/orders/{id}/depart` | 确认出发 |
| `POST /api/v1/caregiver/orders/{id}/arrive` | 确认到达 |
| `POST /api/v1/caregiver/orders/{id}/start` | 开始服务（订单转为 `IN_SERVICE`） |
| `POST /api/v1/caregiver/orders/{id}/complete` | 完成服务（订单转为 `COMPLETED`） |

动作必须按顺序执行，重复或跳步返回 409；只能操作分配给自己的订单。

## 管理端（需 `ADMIN` 角色）

### 服务类别

| 接口 | 说明 |
| --- | --- |
| `GET /api/v1/admin/categories` | 全部类别（含停用，附带护工数） |
| `POST /api/v1/admin/categories` | 新增 |
| `PUT /api/v1/admin/categories/{id}` | 修改 |
| `DELETE /api/v1/admin/categories/{id}` | 删除（仍有护工时 409） |

### 护工

| 接口 | 说明 |
| --- | --- |
| `GET /api/v1/admin/caregivers?keyword=&status=&page=&size=` | 搜索/分页 |
| `POST /api/v1/admin/caregivers` | 新增护工并自动开通护工账号 |
| `PUT /api/v1/admin/caregivers/{id}` | 修改 |
| `POST /api/v1/admin/caregivers/{id}/status?status=ACTIVE` | 启用/停用 |

### 排班与名额

| 接口 | 说明 |
| --- | --- |
| `GET /api/v1/admin/schedules?caregiverId=&from=&to=` | 查询护工排班 |
| `POST /api/v1/admin/schedules` | 新增单条排班 |
| `POST /api/v1/admin/schedules/generate` | 按基础时段批量生成（`caregiverId/from/to/totalQuota`） |
| `PUT /api/v1/admin/schedules/{id}` | 修改时段/名额/价格/状态 |
| `POST /api/v1/admin/schedules/{id}/quota` | 调整剩余名额 `{remainingQuota}` |
| `POST /api/v1/admin/schedules/{id}/stop` | 停班：关闭排班并全额退款取消订单 |
| `DELETE /api/v1/admin/schedules/{id}` | 删除（已有订单时 409，建议改为停班） |

停班响应：

```json
{ "scheduleId": 12, "cancelledOrders": 2, "refundTotal": 400.00 }
```

## 消息（需登录）

| 接口 | 说明 |
| --- | --- |
| `GET /api/v1/messages?page=0&size=10` | 消息分页 |
| `GET /api/v1/messages/unread-count` | 未读数 |
| `POST /api/v1/messages/{id}/read` | 标记单条已读 |
| `POST /api/v1/messages/read-all` | 全部标记已读 |

## 支付回调（公开，签名校验）

`POST /api/v1/payments/callback`

```json
{ "paymentNo": "PAY...", "success": true, "signature": "HMAC-SHA256 结果" }
```

- 签名密钥：`app.payment.mock-secret`
- 同一 `paymentNo` 重复回调直接返回成功（幂等）
- Mock 支付通过同步接口完成，此入口保留给真实网关

## 待补充

- OpenAPI（Swagger UI）：P6 接入后自动生成
