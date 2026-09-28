# 前端（Vue 3 + TypeScript + Vite）

考研学习工作台的前端。产品范围与业务规则见根目录 `PLAN.md`，工程约定见 `AGENTS.md`，
目录落点见 `docs/STRUCTURE.md`。后端是仓库内的单一 Spring Boot 应用，所有接口在 `/api` 下。

## 环境要求

- Node.js ≥ 20（Vite 6 要求）
- pnpm（本项目唯一的包管理器；`package.json` 已声明 `packageManager`，未提交锁文件，首次安装会生成）

## 命令

```bash
# 1. 安装依赖（在 frontend/ 目录执行）
pnpm install

# 2. 本地开发（默认 http://localhost:5173，/api 代理到 http://127.0.0.1:8080）
pnpm dev

# 3. 类型检查（严格模式）
pnpm typecheck

# 4. 构建（先类型检查再打包到 dist/）
pnpm build

# 5. 预览构建产物
pnpm preview

# 6. 单元测试（Vitest + jsdom，不需要后端）
pnpm test
```

配置：复制 `.env.example` 为 `.env`（只放无敏感信息的变量）。可用变量：

- `VITE_API_BASE`：接口前缀，默认 `/api`。
- `VITE_APP_TITLE`：页面标题，可选。

## 目录说明（与 docs/STRUCTURE.md 一致）

```text
src/
├── api/           通用 HTTP 客户端（axios 实例、CSRF、错误归一化）
├── assets/        参与打包的静态资源
├── components/    跨功能通用组件（AmountUnit、EmptyState、OfflineNotice）
├── composables/   跨功能组合式函数（网络状态、展示格式化）
├── features/      各业务功能：api / components / views（+ 功能内 types）
├── layouts/       应用主布局
├── router/        路由与登录守卫
├── stores/        全局会话（session）与科目单位缓存（catalog）
├── styles/        全局样式
├── types/         跨功能通用类型（ApiError、PageResult、科目、单位……）
├── offline/       IndexedDB 快照读写（唯一的数据缓存入口）
└── pwa/           Service Worker 注册与更新提示
tests/
├── unit/          Vitest 单元与组件测试
├── e2e/           Playwright 说明（尚未接入运行环境）
└── fixtures/      无敏感信息的测试数据
```

## 关键约定

1. **CSRF 与凭证**：所有请求 `withCredentials: true`。写操作自动读取 `XSRF-TOKEN` Cookie
   并回填 `X-XSRF-TOKEN` 请求头；遇到 403 会重新获取令牌并**只重试一次**（后端 CSRF 失败与权限不足
   都返回 403，无法从错误码区分，因此不能无限重试）。实现集中在 `src/api/http.ts`。
2. **登录与退出**：`GET /api/auth/me` 未登录返回 200 + `authenticated:false`；路由守卫据此跳转登录页。
   退出登录即使接口调用失败，也会清空本地离线快照（个人数据不留在浏览器）。
3. **离线只读**：只缓存“今日、本周、进度概览”三个切片 + 用户名与同步时间（`src/offline/`），
   全部读写都必须通过 `useOfflineSnapshotStore`。第一版不建离线写队列：离线时写入会被明确拒绝，
   并保留表单内容供联网后重试。Service Worker 只预缓存应用外壳，`workbox.runtimeCaching` 为空，
   绝不缓存 `/api/**`。
4. **统计口径**：进度、剩余量、额度校验全部由后端计算，前端只格式化展示。
   不同单位不求和、不生成总体百分比；学习时长、内容完成量、掌握情况分开显示。
   预算/额度为空表示“未设置”，不能当作 0。
5. **并发与防重**：修改类请求都带上从服务端读取的 `version`，冲突（409）时提示刷新而不是静默覆盖；
   学习记录创建时附带 `clientRequestId`（配合后端防重），并在提交中禁用按钮。
6. **显式导入**：业务代码显式导入 Vue / Element Plus 的 API，`unplugin-auto-import` 与
   `unplugin-vue-components` 只用于按需解析且 `dts: false`，这样干净检出（没有生成 d.ts）时
   `vue-tsc --noEmit` 依然可用。Element Plus 的全局组件类型来自 `element-plus/global`（见 `env.d.ts`）。
7. **Element Plus 输入组件的绑定**：数值/日期/下拉组件使用 `:model-value` + `@update:model-value`
   而不是 `v-model`，因为这些组件的 `modelValue` 是较宽的联合类型，`v-model` 在 `vue-tsc` 下会报类型不兼容。

## 与后端接口的对应关系

- 账号：`GET /api/auth/csrf`、`POST /api/auth/login`、`GET /api/auth/me`、`POST /api/auth/logout`（204）
- 设置：`/api/settings/subjects`、`/api/settings/units`、`/api/settings/study`（PUT 需带 `version`）
- 其余模块（plan / learning / today / progress / review / memo / target）的路径集中在各
  `src/features/<module>/api/index.ts`，字段类型在同目录 `types.ts`。
  这些接口由并行开发实现，若字段命名或返回结构有差异，只需改对应功能的 `api` 与 `types` 文件。

## 验收要点（前端相关）

- 未登录无法读取学习数据；退出后本地缓存被清理。
- 保存失败不误报成功，并保留当前表单输入。
- 不同单位不混加，复习不重复增加首次学习量。
- 离线只能查看规定范围，且显示最后同步时间。
- 未完成任务不会自动出现在次日安排中。
