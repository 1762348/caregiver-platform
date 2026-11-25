# 安全策略

## 支持版本

| 版本 | 支持 |
| --- | --- |
| 1.x | ✅ |
| < 1.0 | ❌ |

## 报告漏洞

请**不要**通过公开 Issue 报告安全漏洞。

推荐方式：使用 GitHub 仓库的 **Security → Advisories → Report a vulnerability** 私密报告，
并尽量包含：

- 受影响的接口或页面
- 复现步骤
- 影响范围（越权、数据泄露、支付绕过等）
- 你的修复建议（可选）

我们会在 7 天内确认，并在修复后于 Release Notes 中致谢（如你愿意）。

## 部署者须知

本项目默认提供 **Mock 支付与 Mock 短信**，仅用于演示：

- 上线前必须替换 `MockPaymentGateway` 与 `SmsSender`
- 必须修改 `JWT_SECRET`、数据库密码与 `MOCK_PAYMENT_SECRET`
- 必须关闭演示用固定验证码（`SMS_FIXED_CODE` 置空）
- 建议在 Nginx/负载均衡层启用 HTTPS，并限制 `/actuator`、Swagger 的访问范围
