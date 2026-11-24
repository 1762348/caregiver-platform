# 贡献指南

感谢你关注「优护 · 社区护工预约平台」。

## 本地开发

1. 后端：`cd backend && mvn spring-boot:run`（需要 JDK 17+ 与 MySQL 8）
2. 前端：`cd frontend && npm install && npm run dev`
3. 测试：`mvn verify` / `npm run test:unit`

## 分支与提交

- 分支：`main` 保持可运行；功能开发走 `feat/xxx`，修复走 `fix/xxx`
- 提交信息遵循 [Conventional Commits](https://www.conventionalcommits.org/)：
  - `feat: 新增护工排班查询接口`
  - `fix: 修复最后一个名额并发超卖`
  - `docs: 补充部署说明`
- 提交前确保 `mvn verify` 与 `npm run build` 通过

## 代码规范

- Java：4 空格缩进，类名大驼峰，方法小驼峰；Controller 只做参数校验与编排，业务放在 Service
- 前端：Vue 3 `<script setup lang="ts">`，组件名大驼峰，目录小写
- 接口统一返回 `ApiResponse<T>`，不要把异常吞掉

## 提交 PR

1. Fork 并创建分支
2. 补齐测试（新功能至少一个单元测试）
3. 更新相关文档
4. 描述里写清楚：改了什么、为什么、怎么验证
