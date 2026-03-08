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
