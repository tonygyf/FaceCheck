# FaceCheck Cloudflare Workers + D1 接口方法说明

本说明用于指导在 Cloudflare Workers + D1 上实现与对接 FaceCheck 所需的基础接口。内容涵盖：鉴权与 CORS、时间/响应约定、D1 基本用法，以及核心业务接口（认证/账户、学生、班级、课程、考勤、人脸特征、照片资源）。

## 使用约定
- 基地址：自行在 Cloudflare 路由配置，例如 `https://api.example.com`
- 所有接口均返回 `application/json`
- 时间统一使用毫秒时间戳（Unix ms）
- 鉴权统一使用 `Authorization: Bearer <token>`
- 跨域统一允许：`GET, POST, OPTIONS` 与头 `Content-Type, Authorization`

## 鉴权与 CORS
- 鉴权：后端校验 `Authorization: Bearer <API_TOKEN>`，无或错误则返回 401
- 预检：对 `OPTIONS` 直接返回 204（或空响应）并附带 CORS 头
- 响应头（示例）：
  - `Access-Control-Allow-Origin: *`
  - `Access-Control-Allow-Methods: GET,POST,OPTIONS`
  - `Access-Control-Allow-Headers: Content-Type, Authorization`

## 通用响应与错误
- 成功：`{ ok: true }` 或 `{ data: [...] }`
- 失败：`{ error: "reason" }`
- 错误码：
  - 400 参数错误或校验失败
  - 401 鉴权失败
  - 404 资源不存在或路由不存在
  - 500 服务端错误

## D1 基本方法
- 绑定：在 `wrangler.toml` 使用 `[[d1_databases]]` 绑定，Worker 中通过 `env.DB` 访问
- 查询：
  ```ts
  const { results } = await env.DB
    .prepare("SELECT * FROM Student WHERE id = ?")
    .bind(id)
    .all();
  ```
- 写入：
  ```ts
  await env.DB
    .prepare("INSERT INTO Classroom (name) VALUES (?)")
    .bind(name)
    .run();
  ```
- 单行：
  ```ts
  const row = await env.DB
    .prepare("SELECT * FROM Classroom WHERE id = ?")
    .bind(id)
    .first();
  ```
- 迁移与表结构：建议直接复用本地 `schema.sql`，保持一致性（参考仓库文件：`app/src/main/assets/schema.sql`）
- 唯一约束：学生唯一性可通过 `(classId, sid)` 建唯一索引

## 参数与时间
- 所有时间戳参数（如 `start`, `end`, `decidedAt`, `startedAt`）采用毫秒
- 约定查询范围：`[start, end)`（包含 start，不包含 end）

---

## 接口总览

- 学生（Student）：
  - GET `/api/students?classId=<number>`
  - POST `/api/students`（幂等 upsert）
  - 可选：GET `/api/students/:id`
  - 可选：PUT `/api/students/:id`

- 班级（Classroom）：
  - GET `/api/classrooms`
  - POST `/api/classrooms`
  - 可选：GET `/api/classrooms/:id`
  - 可选：PUT `/api/classrooms/:id`

- 认证（Auth）与账户（Teacher/Student）：
  - POST `/api/auth/login`（教师/学生通用）
  - POST `/api/auth/logout`
  - GET `/api/auth/me`
  - POST `/api/auth/register`（教师注册）
  - GET `/api/teachers/:id` / PUT `/api/teachers/:id`
  - GET `/api/students/:id` / PUT `/api/students/:id`

- 课程（Courses 与选课）：
  - GET `/api/courses` / GET `/api/courses?studentId=<number>`
  - POST `/api/courses/select`（学生选课）
  - GET `/api/courses/selected?studentId=<number>`

- 考勤（Attendance）两部分：
  - Session（考勤场次）：
    - GET `/api/attendance/sessions?classId=<number>&start=<ms>&end=<ms>`
    - POST `/api/attendance/sessions`
    - 可选：GET `/api/attendance/sessions/:id`
    - 可选：PUT `/api/attendance/sessions/:id`（状态更新）
  - Result（学生考勤结果）：
    - GET `/api/attendance?studentId=<number>&start=<ms>&end=<ms>`
    - POST `/api/attendance`
    - 可选：GET `/api/attendance/results?sessionId=<number>`

- 人脸特征（FaceEmbedding）：
  - GET `/api/face/embeddings?studentId=<number>`
  - GET `/api/face/embeddings/by-model?modelVer=<string>`
  - POST `/api/face/embeddings`
  - 可选：PUT `/api/face/embeddings/:id`

- 照片资源（PhotoAsset 元数据）：
  - GET `/api/photo-assets?sessionId=<number>`
  - GET `/api/photo-assets?studentId=<number>`
  - POST `/api/photo-assets`

---

## 学生模块

### GET /api/students
- 说明：按班级获取学生列表
- 鉴权：需要
- 参数：`classId`（number，必填）
- 请求示例：
  ```http
  GET /api/students?classId=12
  Authorization: Bearer <token>
  ```
- 响应示例：
  ```json
  {
    "data": [
      { "id": 1, "classId": 12, "name": "张三", "sid": "20230001", "gender": "M", "avatarUri": null },
      { "id": 2, "classId": 12, "name": "李四", "sid": "20230002", "gender": "F", "avatarUri": "content://..." }
    ]
  }
  ```
- D1 表：`Student (id, classId, name, sid, gender, avatarUri, ...)`

### POST /api/students
- 说明：新增或更新学生（幂等）
- 鉴权：需要
- Body：JSON
  ```json
  {
    "id": 1,
    "classId": 12,
    "sid": "20230001",
    "name": "张三",
    "gender": "M",
    "avatarUri": null
  }
  ```
- 响应：`{ "ok": true }`
- 冲突处理：可用 `ON CONFLICT(id) DO UPDATE`；若以 `(classId, sid)` 唯一约束，可切换该键幂等

### GET /api/students/:id
- 说明：按主键获取学生详情
- 鉴权：需要
- 响应示例：
  ```json
  { "data": { "id": 1, "classId": 12, "name": "张三", "sid": "20230001", "gender": "M", "avatarUri": null } }
  ```

### PUT /api/students/:id
- 说明：更新学生信息（姓名、性别、头像等）
- Body：部分字段可选
  ```json
  { "name": "张三三", "gender": "M", "avatarUri": "content://..." }
  ```

---

## 班级模块

### GET /api/classrooms
- 说明：获取所有班级
- 鉴权：需要
- 请求示例：
  ```http
  GET /api/classrooms
  Authorization: Bearer <token>
  ```
- 响应示例：
  ```json
  {
    "data": [
      { "id": 12, "name": "软件一班" },
      { "id": 13, "name": "软件二班" }
    ]
  }
  ```
- D1 表：`Classroom (id, name, ...)`

### POST /api/classrooms
- 说明：新增或更新班级（幂等）
- Body：
  ```json
  { "id": 12, "name": "软件一班" }
  ```
- 响应：`{ "ok": true }`

### GET /api/classrooms/:id
- 说明：获取单个班级详情
- 响应：
  ```json
  { "data": { "id": 12, "teacherId": 1, "name": "软件一班", "year": 2024, "semester": "秋", "courseName": "移动开发" } }
  ```

### PUT /api/classrooms/:id
- 说明：更新班级信息（名称、学年、课程名等）
- Body：
  ```json
  { "name": "软件一班（实验）", "courseName": "移动开发（实验）" }
  ```
---

## 考勤模块

### GET /api/attendance?studentId&start&end
- 说明：按学生与时间范围查询考勤结果（与本地 `DatabaseHelper.getAttendanceResultsByStudentAndDateRange` 对齐）
- 鉴权：需要
- 参数：
  - `studentId`（number，必填）
  - `start`（ms，必填）
  - `end`（ms，必填）
- 请求示例：
  ```http
  GET /api/attendance?studentId=1001&start=1704038400000&end=1706640000000
  Authorization: Bearer <token>
  ```
- 响应示例：
  ```json
  {
    "data": [
      {
        "id": 88,
        "sessionId": 55,
        "studentId": 1001,
        "status": "present",
        "score": 100,
        "decidedAt": 1704124800000,
        "classId": 12,
        "className": "软件一班"
      }
    ]
  }
  ```
- D1 表关联：`AttendanceResult` + `AttendanceSession` + `Classroom`

### POST /api/attendance
- 说明：提交学生考勤结果
- Body：
  ```json
  {
    "sessionId": 55,
    "studentId": 1001,
    "status": "present",
    "score": 100,
    "decidedAt": 1704124800000
  }
  ```
- 响应：`{ "ok": true }`

### GET /api/attendance/results?sessionId
- 说明：按场次获取所有学生考勤结果
- 参数：`sessionId`（number，必填）
- 响应示例：
  ```json
  {
    "data": [
      { "id": 88, "sessionId": 55, "studentId": 1001, "status": "present", "score": 95, "decidedAt": 1704124800000 },
      { "id": 89, "sessionId": 55, "studentId": 1002, "status": "absent", "score": 0, "decidedAt": 1704125800000 }
    ]
  }
  ```

### GET /api/attendance/sessions?classId&start&end
- 说明：班级在时间范围内的考勤场次列表
- 参数：`classId`（number），`start`（ms），`end`（ms）
- 响应示例：
  ```json
  {
    "data": [
      { "id": 55, "classId": 12, "startedAt": 1704124800000, "title": "软件一班-第5次考勤" }
    ]
  }
  ```
- D1 表：`AttendanceSession (id, classId, startedAt, title, ...)`

### POST /api/attendance/sessions
- 说明：创建新的考勤场次
- Body：
  ```json
  { "classId": 12, "startedAt": 1704124800000, "title": "软件一班-第5次考勤" }
  ```
- 响应：`{ "ok": true, "id": <newId> }`

### GET /api/attendance/sessions/:id
- 说明：获取单个场次详情
- 响应：
  ```json
  { "data": { "id": 55, "classId": 12, "startedAt": 1704124800000, "status": "ACTIVE" } }
  ```

### PUT /api/attendance/sessions/:id
- 说明：更新场次状态或补充信息（例如完成/取消）
- Body：
  ```json
  { "status": "COMPLETED", "endedAt": 1704128800000, "note": "已完成清点" }
  ```

---

## 认证与账户模块

### POST /api/auth/login
- 说明：教师/学生统一登录入口
- Body（教师登录）：
  ```json
  { "role": "teacher", "username": "admin", "password": "admin123" }
  ```
- Body（学生登录）：
  ```json
  { "role": "student", "sid": "20230001", "password": "123456" }
  ```
- 响应：
  ```json
  { "ok": true, "token": "<jwt-or-random>", "userId": 1, "role": "teacher" }
  ```

### POST /api/auth/logout
- 说明：后端可返回 `{ ok: true }`，客户端清除本地状态即可

### GET /api/auth/me
- 说明：返回当前用户基本信息（通过 `Authorization` 识别）
- 响应示例（教师）：
  ```json
  { "data": { "id": 1, "name": "管理员", "username": "admin", "email": "admin@example.com", "phone": "13800138000", "avatarUri": null } }
  ```
- 响应示例（学生）：
  ```json
  { "data": { "id": 1001, "classId": 12, "name": "张三", "sid": "20230001", "gender": "M", "avatarUri": null } }
  ```

### POST /api/auth/register
- 说明：仅教师注册（学生通过导入或由教师创建）
- Body：
  ```json
  { "name": "李老师", "username": "lilaoshi", "password": "123456" }
  ```
- 响应：`{ "ok": true, "id": <teacherId> }`

### GET /api/teachers/:id / PUT /api/teachers/:id
- 说明：获取/更新教师信息（含头像、邮箱、电话）
- PUT Body 示例：
  ```json
  { "name": "李老师", "avatarUri": "content://...", "email": "li@example.com", "phone": "13900139000" }
  ```

---

## 课程与选课模块

### GET /api/courses
- 说明：获取课程列表（全量或分页）
- 可选参数：`teacherId` / `studentId` 用于过滤关联课程
- 响应示例：
  ```json
  {
    "data": [
      { "id": "C001", "name": "高等数学 A(1)", "teacherName": "李教授", "time": "周一 08:30 - 10:10", "location": "教A-101", "description": "基础课程", "iconUrl": "" }
    ]
  }
  ```

### GET /api/courses/selected?studentId
- 说明：获取某学生已选课程
- 响应示例：
  ```json
  { "data": [ { "id": "C001", "name": "高等数学 A(1)" } ] }
  ```

### POST /api/courses/select
- 说明：学生确认选课（可幂等）
- Body：
  ```json
  { "studentId": 1001, "courseId": "C003" }
  ```
- 响应：`{ "ok": true }`
- 数据层提示：若当前 schema 无课程表，建议在 D1 新增：`Course`、`StudentCourseSelection` 两表；或先以 JSON 存储简化

---

## 人脸特征模块（FaceEmbedding）

### GET /api/face/embeddings?studentId
- 说明：获取某学生的全部特征，按质量降序
- 响应示例：
  ```json
  {
    "data": [
      { "id": 10, "studentId": 1001, "modelVer": "MobileFaceNet", "quality": 0.92, "faceImageUri": "r2://...", "isActive": 1 }
    ]
  }
  ```

### GET /api/face/embeddings/by-model?modelVer
- 说明：获取指定模型版本的全部特征（可过滤 isActive=1）

### POST /api/face/embeddings
- 说明：新增/激活学生人脸特征
- Body：
  ```json
  { "studentId": 1001, "modelVer": "MobileFaceNet", "vector": "<base64>", "quality": 0.95, "faceImageUri": "r2://face/1001.jpg", "isActive": 1 }
  ```
- 响应：`{ "ok": true, "id": <embeddingId> }`
- 存储建议：`vector` 为 BLOB（base64 入参，服务端转为 BLOB）；`faceImageUri` 存对象存储（R2），D1 仅存元数据

### PUT /api/face/embeddings/:id
- 说明：更新元信息（例如 isActive、faceImageUri）
- Body 示例：
  ```json
  { "isActive": 0 }
  ```

---

## 照片资源模块（PhotoAsset）

### GET /api/photo-assets?sessionId
- 说明：按场次获取图片元数据（RAW/ALIGNED/DETECTED/FEATURE/DEBUG）
- 响应示例：
  ```json
  { "data": [ { "id": 1, "sessionId": 55, "type": "RAW", "uri": "r2://raw/xxx.jpg", "width": 1920, "height": 1080, "format": "jpg" } ] }
  ```

### GET /api/photo-assets?studentId
- 说明：按学生获取关联图片元数据

### POST /api/photo-assets
- 说明：记录图片元数据（文件请先上传至 R2，再写入 D1 元数据）
- Body：
-  ```json
  { "sessionId": 55, "studentId": 1001, "type": "ALIGNED", "uri": "r2://aligned/xxx.jpg", "width": 256, "height": 256, "format": "jpg", "meta": "{\"align\":\"ok\"}" }
  ```
- 响应：`{ "ok": true, "id": <assetId> }`

---

## Android 对接建议
- 统一设置 5s 超时，失败重试 1 次
- 请求头始终包含 `Authorization`
- GET 使用查询参数；POST 使用 JSON body
- 响应处理统一按 `{ ok } / { data } / { error }` 解析

## 表结构参考
- 请参考项目文件：[schema.sql](file:///d:/typer/android_demo/FaceCheck/app/src/main/assets/schema.sql)
- 学生唯一约束建议：`CREATE UNIQUE INDEX IF NOT EXISTS idx_student_unique ON Student(classId, sid);`

## 备注
- 图片/头像等二进制不建议存 D1；建议接入 Cloudflare R2（对象存储），此文档暂不包含 R2 上传接口，后续如需我可按你的决策补充。
