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

## 待补充

- 服务类别 / 护工 / 排班查询：P3
- 订单、支付、取消、进度、消息：P3
- 管理端接口：P5
- OpenAPI（Swagger UI）：P6 接入后自动生成
