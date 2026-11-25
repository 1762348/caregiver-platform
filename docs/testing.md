# 测试与质量

## 总览

| 层 | 工具 | 命令 | 当前结果 |
| --- | --- | --- | --- |
| 后端单元/集成 | JUnit 5 + Spring Boot Test + H2 | `cd backend && ./mvnw verify` | 58 个测试通过 |
| 后端覆盖率 | JaCoCo | `verify` 自动生成 | 行 70.8% / 分支 47.8% |
| 前端单元/组件 | Vitest + @vue/test-utils + jsdom | `cd frontend && npm run test:unit` | 17 个测试通过 |
| 前端静态检查 | TypeScript + ESLint + Prettier | `npm run typecheck && npm run lint && npm run format:check` | 通过 |
| 手工端到端 | 浏览器 | 见下方流程 | 见 P4/P5 验收记录 |

CI 会对以上所有检查设卡：

- 后端：`mvn verify`（含 JaCoCo 覆盖率门槛：行 ≥65%、分支 ≥40%）
- 前端：`npm ci` → `lint` → `format:check` → `test:unit` → `build`

## 后端测试

### 分层

- **纯单元测试**：`RefundCalculatorTest`、`OrderStatusTest`、`IdCardUtilsTest`、`SmsCodeServiceTest`
- **Repository / 业务集成**：`OrderFlowIntegrationTest`、`AdminScheduleStopTest`、`ScheduleQuotaTest`
- **接口与安全**：`AuthFlowTest`、`CatalogApiTest`、`AdminApiTest`、`RoleAccessTest`
- **并发**：`BookingConcurrencyTest`（剩余 1 个名额，6 个并发只允许 1 单成功）
- **迁移**：`MigrationAndSeedTest`（空库自动建表 + 种子数据）

### 测试数据库

- 使用 H2 内存库（MySQL 兼容模式），无需本地 MySQL
- 每次测试上下文启动时执行 Flyway 迁移，同时校验实体与表结构一致（`ddl-auto=validate`）
- 短信验证码在测试中使用固定值 `123456`；多个测试类共享令牌缓存，避免触发发送频控

### 覆盖率

```bash
cd backend
./mvnw verify
# HTML 报告：target/site/jacoco/index.html
```

未覆盖的主要是 DTO/枚举等数据载体与部分异常分支；核心业务（预约、支付、取消、停班、权限）均有覆盖。

## 前端测试

```bash
cd frontend
npm run test:unit        # Vitest
npm run typecheck        # vue-tsc
npm run lint             # ESLint
npm run format:check     # Prettier
```

- `utils/order.spec`：时间格式化、进度节点、护工下一步动作
- `stores/auth.spec`：登录持久化、退出清理、令牌失效
- `views/HomeView.spec`：组件挂载并渲染接口返回的服务类别与后端状态

## 手工端到端清单

启动后端与前端后，用三个演示账号分别验证：

1. **用户**（13800000002，验证码 123456）
   - 首页 → 立即预约 → 选择类别/日期/时段/被照顾人 → 确认预约
   - 订单详情 → 支付（微信/支付宝）→ 状态变为已支付
   - 订单列表筛选 → 取消订单 → 看到退款金额
   - 被照顾人新增/编辑/删除；消息中心已读
2. **护工**（13800000001）
   - 工作台显示今日排班与待处理订单 → 依次「确认出发 → 到达 → 开始 → 完成」
3. **管理员**（13800000000）
   - 服务类别新增/编辑/删除
   - 新增护工（自动开通账号）、启用/停用
   - 排班批量生成、调整名额、停班（提示取消单数与退款金额）

## 已知边界

- 支付、短信为 Mock 实现，不具备真实资金与短信能力
- E2E 未纳入 CI（需要同时启动 MySQL + 前后端），当前以浏览器脚本人工验证
- 覆盖率门槛是最低保障，不替代针对新功能的测试
