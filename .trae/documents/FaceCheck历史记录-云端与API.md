
# FaceCheck历史记录-云端与API

---
## 原始文件: FaceCheck 云端改造整体计划.md
---

# FaceCheck 云端改造整体计划

## 目标

完成 FaceCheck 从 WebDAV + 本地切换模式迁移到 Cloudflare 云端网页 + 云数据库的完整改造，并以 API 登录与同步为统一入口。

## 范围

- 登录体系改造（账号密码 + 邮箱验证码）
- 数据同步改造（download/upload API）
- 本地缓存与离线策略
- 云端管理端与移动端协作
- 安全与运维配置

## 依赖与输入

- Base URL：`https://omni.gyf123.dpdns.org`
- API Key Header：`X-API-Key`
- 接口文档：`.trae/documents/网络接口规范.md`

## 里程碑

### 1. 架构替换

- 移除 WebDAV 相关逻辑与配置入口
- 新增 Cloudflare API 客户端层
- 对齐本地数据模型与云端数据结构

### 2. 登录与权限

- 接入 `/api/auth/login`
- 接入 `/api/auth/email-code/send` 与 `/api/auth/email-code/verify`
- 登录态持久化与 token/teacherId 管理

### 3. 数据同步

- 首次拉取：`GET /api/sync/download`
- 增量上传：`POST /api/sync/upload`
- 离线缓存与失败重试策略

### 4. 功能联调

- 班级/学生/Embedding/考勤数据全流程联通
- 端到端联调与异常数据回滚机制

### 5. 安全与运维

- API Key 管理与最小权限
- 日志脱敏与错误追踪
- 版本升级与数据迁移方案

## 任务拆解

### 客户端

- 新建 API Service 与数据层仓库
- 接入登录与权限管理
- 同步逻辑替换并兼容离线
- UI 提示与同步状态展示

### 云端

- 确认 API 接口稳定性与返回结构
- 监控与错误日志聚合
- 数据校验与一致性检查

## 风险与应对

- 登录失败率高：增加验证码重试与限流提示
- 同步冲突：客户端以 server 为准，上传失败重试
- 离线数据丢失：本地持久化与上传队列

## 验收标准

- 登录成功率稳定，支持密码与邮箱验证码
- 同步端到端可用，数据一致
- WebDAV 与本地切换完全替换
- README 与接口文档一致

---
## 原始文件: FaceCheck_Cloud_Strategy_Analysis.md
---

# FaceCheck 项目云架构全面分析与演进策略

**文档版本:** 1.0
**创建日期:** 2026-03-14

## 1. 现状分析报告

本文档对 `FaceCheck` 项目（以后端核心 `omniattend-core` 为主）的现有云架构进行全面分析，评估其优势、劣势，并为未来的发展和扩展提供战略规划。

### 1.1. 项目架构分析

- **核心组件**:
  - **`omniattend-core`**: 项目的核心，一个集成了前端与后端的应用。
    - **前端**: 基于 React (Vite) 和 TypeScript 构建的单页应用（SPA），为管理员提供管理界面。
    - **后端**: 基于 TypeScript 的 Serverless 函数，运行在 Cloudflare Workers 环境，负责处理所有业务逻辑和 API 请求。
  - **`FaceCheck` (安卓客户端)**: 原生安卓应用，作为数据采集和用户交互的终端，通过 API 与后端通信。

- **技术栈**:
  - **运行时**: Cloudflare Workers (Edge Computing)。
  - **语言**: TypeScript。
  - **前端框架**: React, Vite, Tailwind CSS。
  - **后端框架**: 无特定框架，采用原生 Workers API 和路由逻辑。
  - **部署工具**: Cloudflare Wrangler CLI。

- **架构评估**:
  - **伸缩性 (Scalability)**: **极高**。基于 Cloudflare 的全球边缘网络，架构本身是无状态和 Serverless 的，能够自动按需扩展以应对流量高峰，无需手动配置服务器容量。
  - **可用性 (Availability)**: **高**。依赖于 Cloudflare 全球网络的冗余和高可用特性。除非 Cloudflare 平台本身出现大规模故障，否则服务可用性有极高的保障。
  - **弹性 (Resilience)**: **中等**。应用对 Cloudflare 生态系统（特别是 D1 数据库）有较强依赖。D1 本身的故障可能影响整个应用。目前缺少跨区域容灾或数据库备份恢复的明确策略。

### 1.2. 数据层分析

- **数据存储**:
  - **结构化数据**: Cloudflare D1，一个基于 SQLite 的边缘数据库。用于存储所有核心业务数据，如 `Teacher`, `Student`, `Classroom`, `CheckinTask` 等。
  - **对象存储**: Cloudflare R2，一个兼容 S3 API 的对象存储服务。用于存储用户头像等二进制文件。
  - **缓存**: 当前未明确使用缓存服务（如 Cloudflare KV）。

- **数据模型与模式**:
  - **模型**: `schema.sql` 文件定义了清晰的关系型数据模型，表之间通过外键关联。
  - **读写模式**: 典型的 OLTP（在线事务处理）模式。读操作（如查询列表、详情）频率高于写操作（创建、更新）。写操作集中在考勤提交、信息修改等场景。
  - **一致性要求**: 业务要求强一致性。D1 作为集中式数据库（尽管分布在边缘），能够保证 ACID 特性，满足当前需求。

- **数据迁移评估**:
  - **现状**: 数据已在云端（D1）。
  - **风险**: **供应商锁定（Vendor Lock-in）**是主要风险。由于 D1 基于 SQLite，若未来需要迁移到其他主流数据库（如 PostgreSQL, MySQL），需要进行 SQL 方言的转换和数据导出/导入，复杂度较高。

### 1.3. 外部依赖和服务分析

- **第三方服务列表**:
  1.  **高德地图 API**: 用于前端的地理位置选择和逆地理编码。属于前端依赖，与后端云架构无直接耦合。
  2.  **Google Gemini API**: 用于后端的 AI 智能洞察。通过标准的 HTTPS 请求调用。

- **兼容性评估**:
  - 两种依赖都是通过标准的 HTTP API 集成，与云环境完全兼容。
  - **替代方案**: 高德地图可被 Google Maps API 或其他地图服务替代；Gemini API 可被 OpenAI GPT 系列、Anthropic Claude 等其他大语言模型 API 替代。替换成本主要在于适配 API 的请求和响应格式，工作量可控。

### 1.4. 网络与安全分析

- **网络通信**: 
  - **外部访问**: 所有客户端（Web 前端、安卓 App）通过 HTTPS 访问 Cloudflare Worker 提供的 API 端点。Cloudflare 自动处理 TLS 加密。
  - **内部通信**: 后端服务（Worker, D1, R2）之间的通信在 Cloudflare 的内部网络中进行，延迟低且安全性高。

- **安全机制**:
  - **认证**: 采用基于 `X-API-Key` 的共享密钥认证。这是一种简单有效的机制，但对于复杂的多租户或多权限场景，扩展性有限。
  - **授权**: 当前业务逻辑中未发现复杂的角色或权限控制（RBAC），默认为拥有 API Key 的请求拥有完全权限。
  - **数据加密**: 传输中数据由 HTTPS 保护；静态数据（D1, R2）由 Cloudflare 平台负责加密。
  - **合规性**: 依赖 Cloudflare 平台提供的合规性认证。若业务涉及特定行业（如医疗、金融），需额外评估 Cloudflare 是否满足相关法规。

### 1.5. 配置与环境管理分析

- **配置管理**: 
  - **非敏感配置**: `wrangler.toml` 文件管理，如服务名称、兼容性日期、D1 数据库绑定等。
  - **敏感配置**: `wrangler secret` 命令用于管理敏感信息（如 `API_KEY`），这些信息作为环境变量注入到 Worker 中。这是一种安全的实践。
- **环境隔离**: Wrangler 支持多环境配置（例如 `[env.staging]`, `[env.production]`), 可以为不同环境（开发、预发布、生产）配置不同的数据库、密钥和域名，实现环境隔离。当前项目似乎未使用此高级功能。

### 1.6. 日志、监控与可观测性分析

- **现状**: 这是当前架构的**主要薄弱环节**。
  - **日志**: 主要依赖 `console.log`，日志输出到 Cloudflare 仪表盘。缺乏结构化、可搜索的日志系统，问题排查困难。
  - **监控**: 依赖 Cloudflare 提供的基础指标（请求数、CPU 时间、错误率）。缺少业务层面的自定义监控和告警。
  - **可观测性**: 缺乏分布式追踪，无法完整跟踪一个请求从入口到数据库再到外部 API 的完整链路。

### 1.7. 部署与运维流程分析

- **现状**: 
  - **构建**: `npm run build` (Vite) 用于构建前端静态资源。
  - **部署**: 手动执行 `wrangler deploy` 命令进行部署。
  - **CI/CD**: 未发现自动化部署流程。
- **评估**: 当前流程简单直接，适合单人或小团队开发。但随着项目复杂度和团队规模的增长，手动部署容易出错且效率低下，是优化的关键点。

---

## 2. 云架构演进策略与迁移路线图

鉴于项目已在 Cloudflare 云上，本节重点是**架构优化和演进**的路线图。

- **第一阶段：基础强化 (Foundation Hardening) - (短期，1-2周)**
  - **目标**: 提升可观测性和部署效率，降低运维成本。
  - **任务**: 
    1.  **引入结构化日志**: 集成第三方日志服务（如 Logtail, Axiom, Datadog），将所有 `console.log` 替换为结构化日志调用，并附带请求 ID。
    2.  **建立 CI/CD 流水线**: 使用 GitHub Actions 创建一个自动化工作流。当代码合并到 `main` 分支时，自动执行测试、构建和 `wrangler deploy`。
    3.  **配置多环境**: 在 `wrangler.toml` 中配置 `staging` 和 `production` 环境，分别对应不同的 D1 数据库和密钥，实现开发/测试与生产的隔离。

- **第二阶段：服务解耦与韧性增强 (Decoupling & Resilience) - (中期，1-3个月)**
  - **目标**: 降低供应商锁定风险，提升系统韧性。
  - **任务**: 
    1.  **抽象数据层**: 在 `worker.ts` 中创建数据仓库（Repository）层，将所有 D1 数据库的直接查询封装起来。这使得未来更换数据库时，只需修改仓库层的实现，而业务逻辑层无需改动。
    2.  **数据库备份策略**: 研究并实施 D1 数据库的定期备份方案（例如，通过 Wrangler CLI 脚本定时导出到 R2）。
    3.  **引入缓存**: 对于不经常变化但读取频繁的数据（如班级列表），使用 Cloudflare KV 作为缓存层，降低对 D1 的读取压力。

- **第三阶段：高级扩展与安全加固 (Advanced Scaling & Security) - (长期)**
  - **目标**: 应对大规模用户增长和更复杂的业务需求。
  - **任务**: 
    1.  **评估数据库升级**: 当业务发展超出 D1（SQLite）的写并发或复杂查询能力时，启动数据库迁移计划，迁移到能从 Worker 访问的分布式数据库（如 Neon, PlanetScale）。
    2.  **升级认证授权**: 引入更专业的认证服务（如 Auth0, Clerk），或自建基于 JWT 的认证体系，以支持更精细的角色和权限控制（RBAC）。
    3.  **API 网关**: 考虑使用 Cloudflare API Gateway 对 API 进行更精细的速率限制、请求验证和路由管理。

---

## 3. 目标云架构设计草案 (增强版)

'''mermaid
graph TD
    subgraph Clients
        AdminWeb[React Admin Web]
        Android[Android Client]
    end

    subgraph "CI/CD (GitHub Actions)"
        direction LR
        GitRepo[Git Repository] -->|on push| BuildTest[Build & Test]
        BuildTest -->|deploy| WranglerCLI[Wrangler CLI]
    end

    subgraph "Cloudflare Platform"
        direction TB
        WranglerCLI --> Worker[Cloudflare Worker API]
        
        subgraph Observability
            Worker -->|Logs| LoggingService[3rd-Party Logging]
            Worker -->|Metrics| CFAnalytics[Cloudflare Analytics]
        end

        AdminWeb --> Worker
        Android --> Worker
        
        Worker -->|Cache| KV[Cloudflare KV]
        Worker -->|CRUD| DAL[Data Abstraction Layer]
        DAL --> D1[(Cloudflare D1)]
        Worker --> R2[Cloudflare R2]
        Worker -->|HTTPS| ExternalAPIs(External APIs: Gemini, Amap)
        
        D1 -->|Backup| R2
    end
'''

---

## 4. 潜在代码修改点清单

- **日志系统集成**: 
  - 在 `worker.ts` 中引入日志库，替换所有 `console.log`。
  - 在请求入口处生成唯一 `traceId`，并将其贯穿整个请求链路的日志。
- **数据层抽象**: 
  - 创建 `services/repository.ts` 或类似文件。
  - 将 `worker.ts` 中所有 `env.DB.prepare(...)` 的直接调用，迁移到 `repository.ts` 中，并导出为 `findStudentById`, `createClassroom` 等业务函数。
- **配置外部化**: 
  - 检查代码，确保没有硬编码的配置项（如 URL、默认值），所有配置都应通过 `env` 对象获取。
- **错误处理**: 
  - 统一 API 的错误返回格式，在 `catch` 块中返回结构化的错误信息，并记录详细的错误日志。

---

## 5. 资源需求和成本初步估算

- **当前成本**: **极低或免费**。Cloudflare 的免费套餐覆盖了大量的 Workers 调用、D1 读写次数、R2 存储和操作次数，对于当前项目规模绰绰有余。
- **未来成本**: 
  - **日志服务**: 第三方日志服务的费用通常基于日志量，早期阶段可能每月在 $0 - $20 之间。
  - **数据库升级**: 如果未来迁移到 Neon 或类似服务，预计成本将从每月 $15 - $25 起步，并随用量增加。
  - **Cloudflare 成本**: 随着用户量和 API 调用量的大幅增长，可能会超出免费额度，但其按量付费的价格仍然极具竞争力。

---

## 6. 风险评估和缓解计划

- **风险 1: 供应商锁定 (Vendor Lock-in)**
  - **描述**: 代码与 Cloudflare Workers 的运行时 API 和 D1 的 SQLite 方言深度绑定，迁移到其他云（如 AWS Lambda）成本高。
  - **缓解计划**: 
    1.  严格执行**第二阶段**的**数据层抽象**，这是最重要的解耦措施。
    2.  尽量使用 Web 标准 API（如 `fetch`），避免使用过多 Cloudflare 特有的 API。
    3.  保持对迁移到其他平台的成本和复杂度的清醒认识，并将其作为技术选型时的考量因素。

- **风险 2: D1 数据库的局限性**
  - **描述**: SQLite 在高并发写入和复杂分析查询（JOINs）方面存在性能瓶颈，可能成为未来业务发展的限制。
  - **缓解计划**: 
    1.  在 Cloudflare 仪表盘中密切监控 D1 的查询性能和错误率。
    2.  在**第二阶段**引入缓存（KV），减少对 D1 的读取压力。
    3.  提前进行技术预研，准备好备选的数据库方案（如 Neon），并制定详细的迁移预案。

- **风险 3: 单点故障**

---
## 原始文件: 最新云端改造.md
---

# FaceCheck Android 端 Serverless 升级实施规划（对齐 Workers 接口）

## 1. Summary
- 目标：将当前 Android 的“本地会话签到 + 同步”升级为“Workers 驱动的任务签到 + 班级消息流”，完整覆盖教师/学生注册登录、教师多模式发布签到、学生在班级消息流中浏览并完成签到。
- 架构定位：Android 只对接 Cloudflare Workers API（`X-API-Key + Bearer Token`），D1 为唯一业务真源；本地 SQLite 仅做缓存、离线队列与 UI 加速。
- 交付形态：单 App 双角色（教师/学生），通过登录态角色路由到不同首页；保留现有数据同步能力并与新签到链路并行，最终以新链路为主。

## 2. Implementation Changes
### A. 客户端架构升级（Android）
- 新增统一网络层：`AuthApi + SyncApi + CheckinApi + FeedApi`，替换散落调用，统一错误码、重试、鉴权、超时、日志脱敏。
- 新增会话管理：本地持久化 `accessToken/refreshToken/userId/role/classId`，App 冷启动先做 token 校验与角色路由。
- 新增本地数据域（DB v10）：
  - `AuthSession`：登录态与角色信息。
  - `CheckinTaskCache`：任务缓存（课堂维度）。
  - `CheckinFeedEvent`：班级消息流缓存（按 `eventId` 递增）。
  - `CheckinSubmissionOutbox`：离线待上传提交。
  - `CheckinDraft`：学生临时输入（手势/口令/GPS）。
- 新增后台任务（WorkManager）：
  - `FeedBackfillWorker`：SSE 断连后补拉消息。
  - `SubmissionOutboxWorker`：离线签到补投递与指数退避重试。
  - `DeltaSyncWorker`：保留 `/api/sync/*` 旧域数据增量同步。

### B. 登录/注册（教师+学生）
- Android 登录页拆为三入口：密码登录、邮箱验证码登录、注册。
- 注册页支持角色切换：
  - 教师注册：姓名、用户名/邮箱、密码。
  - 学生注册：姓名、学号/邮箱、密码、班级加入码（或班级邀请码）。
- 登录成功后按角色分流：
  - 教师：首页进入“签到任务中心”。
  - 学生：首页进入“班级消息流（签到频道）”。
- 权限策略：学生只读任务与个人提交状态；教师可发布/关闭/审核任务并查看当前用户态。

### C. 教师端签到任务中心（多种签到）
- 教师端页面固定 4 区域：发布任务、活动任务、待审核队列、当前用户展示（与云端方案一致）。
- 发布任务使用“约束开关 + 模板”：
  - 模板：课堂标准、户外定位、考试口令、组合约束。
  - 约束首期：时间窗口、位置围栏、手势序列、明文口令（可按开关组合）。
- 教师操作：
  - 发布任务（可直接 ACTIVE）。
  - 关闭任务。
  - 审核异常提交（通过/驳回+备注）。
  - 查看当前用户分布（已签到/待审核/异常/未签到）。

### D. 学生端班级“类聊天室”签到频道
- 学生首页改为“班级消息流”，消息按时间倒序，气泡卡片化展示。
- v1 消息类型（系统消息为主）：
  - 任务发布、任务变更、任务关闭、个人提交结果、教师审核结果。
- 实时机制：
  - 主通道：SSE 持续订阅。
  - 回退：SSE 断开后自动切换 5 秒轮询补拉；恢复后回切 SSE。
- 签到交互：
  - 学生点击任务消息卡 -> 底部弹层填写/采集约束数据（GPS、手势、口令）-> 提交。
  - 提交后立即本地回显“已提交/待审核”，收到服务端结果后更新为“通过/驳回”。

### E. 迁移与兼容
- 旧 `AttendanceSession/AttendanceResult` 继续保留用于历史查看；新任务链路写入由云端联动考勤结果。
- 学生原“自拍签到页”不作为主入口，降级为兼容页（仅当旧任务类型触发时可见）。
- 清理遗留方向：WebDAV 相关入口保持下线状态；“本地备份=主流程”从设置中降级为高级工具，不参与主签到链路。

## 3. API / Interface Alignment（安卓与 Workers 对齐）
- 已有并继续使用：
  - `POST /api/auth/login`
  - `POST /api/auth/email-code/send`
  - `POST /api/auth/email-code/verify`
  - `GET /api/sync/download`
  - `POST /api/sync/upload`
- 新增（本期必须落地）：
  - `POST /api/auth/register`（`role=TEACHER|STUDENT`，统一注册入口）
  - `POST /api/checkin/tasks`
  - `GET /api/checkin/tasks`
  - `POST /api/checkin/tasks/:id/close`
  - `GET /api/checkin/tasks/:id/current-users`
  - `GET /api/checkin/tasks/:id/review-queue`
  - `POST /api/checkin/tasks/:id/submit`
  - `POST /api/checkin/submissions/:id/review`
- 新增（消息流）：
  - `GET /api/classes/:classId/feed/stream`（SSE）
  - `GET /api/classes/:classId/feed?afterEventId=:id`（断线补拉）
- 客户端统一类型（必须定义）：
  - `AuthUser`, `AuthSession`, `CheckinTask`, `CheckinConstraint`, `CheckinSubmission`, `CurrentUserItem`, `ReviewAction`, `FeedEvent`.
- 状态枚举固定：
  - 任务：`DRAFT | ACTIVE | CLOSED`
  - 提交：`APPROVED | PENDING_REVIEW | REJECTED`
  - 自动判定：`PASS | FAIL`

## 4. Test Plan（实现验收标准）
- 认证链路：
  - 教师/学生注册成功、重复账号冲突、弱密码拦截、验证码失效、token 过期自动登出。
- 教师任务链路：
  - 发布任务参数校验（时间、半径、手势格式、口令空值）。
  - 任务状态流转（DRAFT->ACTIVE->CLOSED）与不可逆校验。
- 学生签到链路：
  - 单因子失败与多因子失败组合均进入 `PENDING_REVIEW`，失败原因完整可追踪。
  - 重复提交幂等（同任务同学生仅保留最新有效状态）。
- 消息实时链路：
  - SSE 正常推送、断线重连、回退轮询、补拉去重（按 `eventId`）。
  - 弱网离线提交进入 Outbox，联网后自动补投并更新 UI。
- 回归：
  - 现有 `/api/sync/*` 不回退。
  - 现有历史考勤查询可读、统计可读。
- 性能门槛（演示版）：
  - 任务发布到学生可见 < 2 秒（同网络下）。
  - 提交到结果回显 < 3 秒（自动通过场景）。

## 5. Demo Script + Assumptions
- 演示脚本（固定顺序）：
  1. 教师注册登录 -> 进入任务中心 -> 发布“时间+位置+手势+口令”任务。  
  2. 学生注册登录 -> 进入班级消息流 -> 实时收到任务卡片。  
  3. 学生完成签到提交 -> 消息流显示“待审核/通过”。  
  4. 教师端查看“当前用户展示”并执行审核 -> 学生端实时看到审核结果消息。  
  5. 断网后学生提交一次 -> 恢复网络自动补交并状态修正。  
- 已锁定默认项：
  - 双端都支持注册登录（教师+学生）。
  - 班级消息流实时采用 SSE（含轮询回退）。
  - 首期签到模式采用时间/位置/手势/口令四约束组合。
  - 单 App 双角色，不拆分教师端与学生端应用。

---
## 原始文件: Android_API_Integration_Guide.md
---

# OmniAttend 安卓客户端 API 开发对接文档

**版本:** 1.1
**最后更新:** 2026-03-14

## 1. 引言

### 1.1. 文档目的

本文档旨在为 OmniAttend 安卓客户端的开发者提供一份清晰、准确、易于理解的后端 API 接口对接指南。通过本文档，开发者可以了解如何与后端服务进行认证、数据同步、考勤管理等核心交互，确保客户端功能的顺利实现。

### 1.2. 目标读者

本文档主要面向负责 OmniAttend 安卓客户端开发的工程师。

---

## 2. 通用 API 信息

### 2.1. 基础 URL (Base URL)

所有 API 请求的基础路径为：

`https://omni.gyf123.dpdns.org`

### 2.2. 通用认证机制

除登录/注册/健康检查等少数公开接口外，所有需要授权的 API 请求都必须在 HTTP Header 中包含 `X-API-Key`。

- **Header Name**: `X-API-Key`
- **Header Value**: `my-secret-api-key` (这是一个示例，生产环境应使用安全分发的密钥)

### 2.3. 通用错误响应体

当接口调用失败时（HTTP 状态码为 4xx 或 5xx），响应体通常会包含一个标准的错误结构：

'''json
{
  "error": "具体的错误信息描述"
}
'''

| 字段 | 类型 | 描述 |
| --- | --- | --- |
| `error` | `string` | 对错误的详细文本描述。 |

---

## 3. 接口详解

### 3.1. 认证模块 (Authentication)

#### 3.1.1. 教师登录

- **功能描述**: 使用用户名/邮箱和密码进行登录，成功后获取教师信息和用于后续请求的认证 Token。
- **URL**: `/api/auth/login`
- **方法**: `POST`
- **请求头**:
  - `Content-Type`: `application/json`
- **请求体**:

  '''json
  {
    "username": "teacher_user",
    "password": "password123"
  }
  '''

  | 字段 | 类型 | 是否必填 | 描述 |
  | --- | --- | --- | --- |
  | `username` | `string` | 是 | 教师的用户名或邮箱。 |
  | `password` | `string` | 是 | 教师的密码。 |

- **响应 (200 OK)**:

  '''json
  {
    "success": true,
    "data": {
      "id": 1,
      "username": "teacher_user",
      "email": "teacher@example.com",
      "name": "王老师",
      "avatarUri": "/avatars/teacher1.jpg",
      "role": "teacher",
      "token": "a-random-uuid-string"
    }
  }
  '''

- **可能的状态码**: `200`, `400`, `401`

--- 

### 3.2. 数据同步模块 (Data Sync)

#### 3.2.1. 全量数据下载 (Download Sync Data)

- **功能描述**: 在用户首次登录或需要全量刷新数据时，一次性拉取与该教师相关的所有核心数据，包括班级、学生、人脸特征和近期的考勤记录。
- **URL**: `/api/sync/download`
- **方法**: `GET`
- **请求头**:
  - `X-API-Key`: `my-secret-api-key`
- **查询参数**:

  | 参数 | 类型 | 是否必填 | 描述 |
  | --- | --- | --- | --- |
  | `teacherId` | `number` | 是 | 当前登录的教师 ID。 |

- **响应 (200 OK)**:

  '''json
  {
    "classrooms": [
      { "id": 101, "name": "CS 101", "year": 2024, "teacherId": 1 }
    ],
    "students": [
      { "id": 5, "classId": 101, "name": "Alice", "sid": "S12345", "gender": "F", "avatarUri": "..." }
    ],
    "embeddings": [
      { "id": 20, "studentId": 5, "vector": "base64...", "quality": 0.95, "modelVer": "v1" }
    ],
    "sessions": [
      { "id": 500, "classId": 101, "startedAt": "2024-02-01T09:00:00Z", "location": "Room 303" }
    ],
    "results": [
      { "id": 1001, "sessionId": 500, "studentId": 5, "status": "Present", "score": 0.98, "decidedBy": "AUTO" }
    ]
  }
  '''

- **可能的状态码**: `200`, `401`, `404`

#### 3.2.2. 增量数据上传 (Upload Attendance Data)

- **功能描述**: 将在安卓客户端本地（离线时）创建的考勤会话和结果批量上传到服务器。
- **URL**: `/api/sync/upload`
- **方法**: `POST`
- **请求头**:
  - `Content-Type`: `application/json`
  - `X-API-Key`: `my-secret-api-key`
- **请求体**:

  '''json
  {
    "teacherId": 1,
    "sessions": [
      {
        "classId": 101,
        "startedAt": "2024-02-01T14:00:00Z",
        "location": "Lab 2",
        "note": "Offline session upload",
        "results": [
          {
            "studentId": 5,
            "status": "Present",
            "score": 0.92,
            "decidedBy": "AUTO",
            "decidedAt": "2024-02-01T14:05:00Z"
          }
        ]
      }
    ]
  }
  '''

- **响应 (200 OK)**:

  '''json
  {
    "success": true,
    "processedSessions": 1,
    "details": [
      { "localStartedAt": "2024-02-01T14:00:00Z", "newSessionId": 501 }
    ]
  }
  '''

- **可能的状态码**: `200`, `400`, `401`

#### 3.2.3. 学生数据增量同步 (Delta Sync)

- **功能描述**: 获取自上次同步以来发生变更的学生数据。
- **URL**: `/api/v1/students/delta`
- **方法**: `GET`
- **请求头**:
  - `X-API-Key`: `my-secret-api-key`
- **查询参数**:

  | 参数 | 类型 | 是否必填 | 描述 |
  | --- | --- | --- | --- |
  | `lastSyncTimestamp` | `number` | 是 | 客户端上次成功同步时的时间戳 (Unix 毫秒)。首次请求传 `0`。 |

- **响应 (200 OK)**:

  '''json
  {
    "newLastSyncTimestamp": 1678886400000,
    "addedStudents": [],
  }
  '''

---
## 原始文件: 网络接口规范.md
---

# OmniAttend Android Sync API Interface

This document defines the API endpoints used by the Android application to synchronize data with the Cloudflare D1 backend.

## Base URL
`https://omni.gyf123.dpdns.org`

## Authentication
All requests must include the API Key header:
- **Header Name**: `X-API-Key`
- **Header Value**: `(Your API Secret)`

---

## 1. Download Sync Data (Pull)
Fetch all necessary data for the teacher's device after login. This includes Classrooms, Students, Face Embeddings, and recent Attendance History.

- **Endpoint**: `GET /api/sync/download`
- **Parameters**:
  - `teacherId` (Required): The ID of the logged-in teacher (e.g., from Login response).

### Request Example
'''http
GET /api/sync/download?teacherId=1 HTTP/1.1
X-API-Key: my-secret-api-key
'''

### Response Example
'''json
{
  "classrooms": [
    { "id": 101, "name": "CS 101", "year": 2024, "teacherId": 1 }
  ],
  "students": [
    { "id": 5, "classId": 101, "name": "Alice", "sid": "S12345", "gender": "F", "avatarUri": "..." }
  ],
  "embeddings": [
    { "id": 20, "studentId": 5, "vector": "base64...", "quality": 0.95, "modelVer": "v1" }
  ],
  "sessions": [
    { "id": 500, "classId": 101, "startedAt": "2024-02-01T09:00:00Z", "location": "Room 303" }
  ],
  "results": [
    { "id": 1001, "sessionId": 500, "studentId": 5, "status": "Present", "score": 0.98, "decidedBy": "AUTO" }
  ]
}
'''

---

## 2. Upload Attendance Data (Push)
Upload locally recorded attendance sessions and results to the server. Supports batch upload.

- **Endpoint**: `POST /api/sync/upload`
- **Content-Type**: `application/json`

### Request Payload Structure
The payload should contain a `sessions` array. Each session object contains its metadata and a nested `results` array.

'''json
{
  "teacherId": 1,
  "sessions": [
    {
      "classId": 101,
      "startedAt": "2024-02-01T14:00:00Z",
      "location": "Lab 2",
      "note": "Offline session upload",
      "results": [
        {
          "studentId": 5,
          "status": "Present",
          "score": 0.92,
          "decidedBy": "AUTO",
          "decidedAt": "2024-02-01T14:05:00Z"
        },
        {
          "studentId": 6,
          "status": "Absent",
          "score": 0.0,
          "decidedBy": "TEACHER",
          "decidedAt": "2024-02-01T14:10:00Z"
        }
      ]
    }
  ]
}
'''

### Response Example
'''json
{
  "success": true,
  "processedSessions": 1,
  "details": [
    { "localStartedAt": "2024-02-01T14:00:00Z", "newSessionId": 501 }
  ]
}
'''

---

## Data Models

### Student
| Field | Type | Description |
|-------|------|-------------|
| id | Integer | Unique Server ID |
| classId | Integer | Foreign Key to Classroom |
| name | String | Student Name |
| sid | String | Student ID Number |
| gender | String | 'M', 'F', 'O' |
| avatarUri | String | URL or Path to Avatar |

### FaceEmbedding
| Field | Type | Description |
|-------|------|-------------|
| studentId | Integer | Foreign Key to Student |
| vector | Blob/Base64 | Face feature vector |
| quality | Float | Quality score (0.0 - 1.0) |
| modelVer | String | Model version (e.g. "mobileface_v1") |

### AttendanceResult
| Field | Type | Description |
|-------|------|-------------|
| status | String | 'Present', 'Absent', 'Unknown' |
| score | Float | Confidence score (0.0 - 1.0) |
| decidedBy | String | 'AUTO' (AI) or 'TEACHER' (Manual) |
