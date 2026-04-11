# FaceCheck - 智能人脸识别考勤系统

[![Android](https://img.shields.io/badge/Android-6.0%2B-brightgreen.svg)](https://www.android.com/)
[![License](https://img.shields.io/badge/License-MIT-blue.svg)](LICENSE)
[![Kotlin](https://img.shields.io/badge/Kotlin-100%25-orange.svg)](https://kotlinlang.org/)

<p align="center">
  <img src="app/src/main/res/drawable/facecheck.png" alt="FaceCheck Logo" width="100" height="100"/>
  &nbsp;&nbsp;&nbsp;
  <img src="app/src/main/res/drawable/fclogo.png" alt="FaceCheck" width="600" height="200"/>
</p>

## 📱 项目简介

FaceCheck 是面向教育场景的智能人脸识别考勤系统，现已切换为 **Cloudflare 云端网页 + 云数据库** 架构，使用 API 登录与云端同步替代 WebDAV 与本地切换模式。

### ✨ 核心特性

- **🎯 高精度人脸识别**：支持多人脸检测与识别
- **📊 智能考勤管理**：完整考勤流程与统计
- **☁️ 云端数据同步**：基于 Cloudflare API 的统一数据源
- **🔐 API 登录**：支持账号密码与邮箱验证码登录
- **📱 现代化界面**：Material Design 设计

## 🏗️ 技术架构

### 核心技术栈

| 技术 | 用途 |
|------|------|
| **Server-side Face Service** | 人脸检测与特征提取（云端） |
| **SQLite** | 本地缓存 |
| **Cloudflare Workers** | API 服务 |
| **Cloudflare D1** | 云数据库 |
| **React + Vite** | 云端网页管理端 |
| **MVVM 架构** | 应用架构模式 |

### 云端架构

```
Android 客户端
  ↕ API
Cloudflare Workers
  ↕
Cloudflare D1
```

## 🔐 登录与 API

### Base URL
`https://omni.gyf123.dpdns.org`

### 认证
所有请求需携带 API Key：
- Header: `X-API-Key`

### 登录接口

| 端点 | 方法 | 说明 |
|------|------|------|
| `/api/auth/login` | POST | 账号密码登录 |
| `/api/auth/email-code/send` | POST | 发送邮箱验证码 |
| `/api/auth/email-code/verify` | POST | 验证码登录 |

### 同步接口

| 端点 | 方法 | 说明 |
|------|------|------|
| `/api/sync/download` | GET | 拉取班级/学生/Embedding/考勤 |
| `/api/sync/upload` | POST | 上传考勤会话与结果 |

## 🚀 快速开始

### 环境要求

- **Android Studio**: Arctic Fox 或更高版本
- **最低 Android 版本**: API 23
- **目标 Android 版本**: API 34
- **Java/Kotlin**: JDK 11 或更高版本

### 构建与运行

```bash
./gradlew assembleDebug
./gradlew installDebug
```

### 运行前配置

需要在 App 内配置以下参数（由后端提供）：

- API Base URL：`https://omni.gyf123.dpdns.org`
- API Key：`X-API-Key`

## 📦 同步流程

1. 登录成功获取 teacherId
2. `GET /api/sync/download` 拉取完整数据
3. 本地缓存供离线使用
4. 考勤结束后 `POST /api/sync/upload` 上传会话与结果

## ✅ 兼容性与性能

- Android 6.0 - Android 14
- 人脸识别 < 500ms（单张）
- 支持多班级/多学生规模扩展

## 🧭 迁移说明

- WebDAV 同步与本地切换模式已弃用
- 所有数据以 Cloudflare D1 为唯一来源
- 账号登录与同步接口以 API 为准

## 📖 文档索引

- [网络接口规范](.trae/documents/网络接口规范.md)
- [云端改造整体计划](.trae/documents/FaceCheck 云端改造整体计划.md)

## 🧩 系统总体设计（更新）

### 1) 角色定位（在三仓协同中的职责）

- `FaceCheck`（本仓）负责 Android 端交互、任务执行、离线缓存与同步触发。
- `omniattend-core` 提供业务 API（鉴权、班级/学生、签到任务、审核、统计、文件上传、模型配置）。
- `mobilefacenet-server` 提供人脸向量提取能力（`/embed/*`），由后端调用完成模板注册与签到核验。

### 2) Android 端核心数据流

1. 登录：教师走 `auth/login`，学生走 `login/student`，均携带 `X-API-Key`。
2. 拉取任务：学生端拉取 `checkin/tasks`，并拉取 `checkin/submissions/my` 同步个人最新提交状态。
3. 本地落库：任务与提交写入 SQLite（`CheckinTask`、`CheckinSubmission`），支持离线查看与重进恢复。
4. 发起签到：按任务约束收集手势/口令/定位；若启用人脸则先上传附件到 `checkin/photos/upload`。
5. 提交结果：调用 `checkin/tasks/{id}/submit`，服务端补充人脸核验得分与通过状态。
6. 状态回显：客户端刷新任务列表，展示 `APPROVED / PENDING_REVIEW / REJECTED` 并支持申诉。

### 3) 本地存储与同步边界

- 本地库作为“运行态缓存 + 弱离线能力”，不作为全局权威数据源。
- 云端 D1 为最终权威源，客户端通过 API 拉取最新任务和提交结果。
- 人脸签到附件本地暂存后上传，服务端返回 `photoKey`，客户端保存可访问 URI 用于后续提交。

### 4) 关键实现说明（与当前代码一致）

- 接口入口：`ApiService.BASE_URL = https://omni.gyf123.dpdns.org/api/`
- 网络层：Retrofit + Gson，统一通过 `RetrofitClient` 创建实例。
- 学生签到页：`StudentCoursesFragment` 已实现多方式混合签到（定位/手势/口令/人脸）与底部面板提交流程。
- 数据层：`DatabaseHelper` 当前版本包含 `CheckinTask.faceRequired`、`CheckinTask.faceMinScore`，用于人脸门槛控制。

## 📄 许可证

本项目采用 MIT 许可证 - 查看 [LICENSE](LICENSE) 文件了解详情。
