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
```http
GET /api/sync/download?teacherId=1 HTTP/1.1
X-API-Key: my-secret-api-key
```

### Response Example
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

---

## 2. Upload Attendance Data (Push)
Upload locally recorded attendance sessions and results to the server. Supports batch upload.

- **Endpoint**: `POST /api/sync/upload`
- **Content-Type**: `application/json`

### Request Payload Structure
The payload should contain a `sessions` array. Each session object contains its metadata and a nested `results` array.

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
```

### Response Example
```json
{
  "success": true,
  "processedSessions": 1,
  "details": [
    { "localStartedAt": "2024-02-01T14:00:00Z", "newSessionId": 501 }
  ]
}
```

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
