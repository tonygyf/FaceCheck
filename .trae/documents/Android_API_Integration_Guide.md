
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

```json
{
  "error": "具体的错误信息描述"
}
```

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

  ```json
  {
    "username": "teacher_user",
    "password": "password123"
  }
  ```

  | 字段 | 类型 | 是否必填 | 描述 |
  | --- | --- | --- | --- |
  | `username` | `string` | 是 | 教师的用户名或邮箱。 |
  | `password` | `string` | 是 | 教师的密码。 |

- **响应 (200 OK)**:

  ```json
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
  ```

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

  ```json
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
  ```

- **可能的状态码**: `200`, `401`, `404`

#### 3.2.2. 增量数据上传 (Upload Attendance Data)

- **功能描述**: 将在安卓客户端本地（离线时）创建的考勤会话和结果批量上传到服务器。
- **URL**: `/api/sync/upload`
- **方法**: `POST`
- **请求头**:
  - `Content-Type`: `application/json`
  - `X-API-Key`: `my-secret-api-key`
- **请求体**:

  ```json
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
  ```

- **响应 (200 OK)**:

  ```json
  {
    "success": true,
    "processedSessions": 1,
    "details": [
      { "localStartedAt": "2024-02-01T14:00:00Z", "newSessionId": 501 }
    ]
  }
  ```

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

  ```json
  {
    "newLastSyncTimestamp": 1678886400000,
    "addedStudents": [],
    "updatedStudents": [],
    "deletedStudentIds": [],
    "hasMore": false,
    "totalChanges": 0
  }
  ```
  *详细字段请参考 `SYNC_API.md`* 

#### 3.2.4. 班级数据增量同步 (Delta Sync)

- **功能描述**: 获取自上次同步以来发生变更的班级数据。
- **URL**: `/api/v1/classes/delta`
- **方法**: `GET`
- **请求头**:
  - `X-API-Key`: `my-secret-api-key`
- **查询参数**:

  | 参数 | 类型 | 是否必填 | 描述 |
  | --- | --- | --- | --- |
  | `lastSyncTimestamp` | `number` | 是 | 客户端上次成功同步时的时间戳 (Unix 毫秒)。首次请求传 `0`。 |

- **响应 (200 OK)**:

  ```json
  {
    "newLastSyncTimestamp": 1678886400000,
    "addedClasses": [],
    "updatedClasses": [],
    "deletedClassIds": [],
    "hasMore": false,
    "totalChanges": 0
  }
  ```
  *详细字段请参考 `SYNC_API.md`*

--- 

### 3.3. 考勤任务模块 (Check-in Task)

#### 3.3.1. 提交签到

- **功能描述**: 学生针对一个激活的签到任务提交自己的签到信息。
- **URL**: `/api/checkin/tasks/:id/submit` (e.g., `/api/checkin/tasks/123/submit`)
- **方法**: `POST`
- **请求头**:
  - `Content-Type`: `application/json`
  - `X-API-Key`: `my-secret-api-key`
- **请求体**:

  ```json
  {
    "studentId": 5,
    "passwordInput": "芝麻开门",
    "gestureInput": "1-5-9",
    "lat": 30.12345,
    "lng": 120.54321
  }
  ```

  | 字段 | 类型 | 是否必填 | 描述 |
  | --- | --- | --- | --- |
  | `studentId` | `number` | 是 | 提交签到的学生 ID。 |
  | `passwordInput` | `string` | 否 | 如果任务要求口令，则提交口令。 |
  | `gestureInput` | `string` | 否 | 如果任务要求手势，则提交手势序列。 |
  | `lat` | `number` | 否 | 如果任务要求位置，则提交纬度。 |
  | `lng` | `number` | 否 | 如果任务要求位置，则提交经度。 |

- **响应 (200 OK)**:

  ```json
  {
    "success": true,
    "data": {
      "id": 789 // 新创建的 submission ID
    }
  }
  ```

- **可能的状态码**: `200`, `400`, `404`

---

## 4. 附录

### 4.1. 核心数据类型

*此部分根据 `types.ts` 文件生成，仅列出与安卓端强相关的核心模型。*

#### Student

```typescript
export interface Student {
  id: number;
  classId: number;
  name: string;
  sid: string;
  email?: string;
  gender?: 'M' | 'F' | 'O';
  avatarUri?: string;
  createdAt: string;
}
```

#### Classroom

```typescript
export interface Classroom {
  id: number;
  teacherId: number;
  name: string;
  year: number;
  meta?: string;
}
```

#### FaceEmbedding

```typescript
export interface FaceEmbedding {
  id: number;
  studentId: number;
  modelVer: string;
  vector: Blob; // 在 JSON 中通常为 Base64 编码的字符串
  quality: number;
  createdAt: string;
}
```

#### AttendanceSession

```typescript
export interface AttendanceSession {
  id: number;
  classId: number;
  startedAt: string;
  location?: string;
  photoUri?: string;
  note?: string;
}
```

#### AttendanceResult

```typescript
export interface AttendanceResult {
  id: number;
  sessionId: number;
  studentId: number;
  status: 'Present' | 'Absent' | 'Late' | 'Unknown';
  score: number;
  decidedBy: 'AUTO' | 'TEACHER';
  decidedAt: string;
}
```

