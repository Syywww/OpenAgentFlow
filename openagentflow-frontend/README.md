# OpenAgentFlow-Java 前端

基于 Vue 3、TypeScript 和 Vite 的企业级 AI Agent 控制台，已接入 OpenAgentFlow-Java 后端真实接口，覆盖 Agent、RAG、MCP、工作流、Trace、评测、治理和运营能力。

## 常用命令

```bash
npm ci
npm run dev
npm run test:unit
npm run build
npm run test:e2e
```

默认开发地址为 `http://localhost:5173`，后端接口地址由 `VITE_API_BASE_URL` 配置，未设置时使用 `http://localhost:8080/api`。

Playwright 覆盖登录页、JWT 本地状态、默认工作空间选择和权限菜单裁剪；Vitest 覆盖分页等公共前端规则。
