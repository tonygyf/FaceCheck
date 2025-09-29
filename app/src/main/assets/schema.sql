-- 教师表
CREATE TABLE IF NOT EXISTS Teacher (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    name TEXT NOT NULL,
    email TEXT UNIQUE NOT NULL,
    davUrl TEXT,
    davUser TEXT,
    davKeyEnc TEXT,  -- 加密存储的WebDAV密钥
    createdAt TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updatedAt TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- 班级表
CREATE TABLE IF NOT EXISTS Classroom (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    teacherId INTEGER NOT NULL,
    name TEXT NOT NULL,
    year INTEGER NOT NULL,  -- 学年
    meta TEXT,  -- JSON格式的元数据
    FOREIGN KEY (teacherId) REFERENCES Teacher(id) ON DELETE CASCADE
);

-- 学生表
CREATE TABLE IF NOT EXISTS Student (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    classId INTEGER NOT NULL,
    name TEXT NOT NULL,
    sid TEXT NOT NULL,  -- 学号
    gender TEXT CHECK(gender IN ('M', 'F', 'O')),  -- M:男, F:女, O:其他
    avatarUri TEXT,  -- 头像文件URI
    createdAt TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (classId) REFERENCES Classroom(id) ON DELETE CASCADE
);

-- 人脸特征表
CREATE TABLE IF NOT EXISTS FaceEmbedding (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    studentId INTEGER NOT NULL,
    modelVer TEXT NOT NULL,  -- 模型版本
    vector BLOB NOT NULL,  -- 人脸特征向量
    quality REAL CHECK(quality >= 0 AND quality <= 1),  -- 质量分数
    createdAt TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (studentId) REFERENCES Student(id) ON DELETE CASCADE
);

-- 考勤会话表
CREATE TABLE IF NOT EXISTS AttendanceSession (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    classId INTEGER NOT NULL,
    startedAt TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    location TEXT,  -- 可选的位置信息（如GPS坐标）
    photoUri TEXT,  -- 原始照片URI
    note TEXT,  -- 备注
    FOREIGN KEY (classId) REFERENCES Classroom(id) ON DELETE CASCADE
);

-- 考勤结果表
CREATE TABLE IF NOT EXISTS AttendanceResult (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    sessionId INTEGER NOT NULL,
    studentId INTEGER NOT NULL,
    status TEXT NOT NULL CHECK(status IN ('Present', 'Absent', 'Unknown')),
    score REAL CHECK(score >= 0 AND score <= 1),  -- 匹配分数
    decidedBy TEXT NOT NULL CHECK(decidedBy IN ('AUTO', 'TEACHER')),
    decidedAt TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (sessionId) REFERENCES AttendanceSession(id) ON DELETE CASCADE,
    FOREIGN KEY (studentId) REFERENCES Student(id) ON DELETE CASCADE
);

-- 照片资源表
CREATE TABLE IF NOT EXISTS PhotoAsset (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    sessionId INTEGER NOT NULL,
    type TEXT NOT NULL CHECK(type IN ('RAW', 'ALIGNED', 'DEBUG')),
    uri TEXT NOT NULL,  -- 照片文件URI
    meta TEXT,  -- JSON格式的元数据（如调试信息）
    FOREIGN KEY (sessionId) REFERENCES AttendanceSession(id) ON DELETE CASCADE
);

-- 同步日志表
CREATE TABLE IF NOT EXISTS SyncLog (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    entity TEXT NOT NULL,  -- 实体名称
    entityId INTEGER NOT NULL,  -- 实体ID
    op TEXT NOT NULL CHECK(op IN ('UPSERT', 'DELETE')),
    version INTEGER NOT NULL,  -- 版本号
    ts TIMESTAMP DEFAULT CURRENT_TIMESTAMP,  -- 时间戳
    status TEXT NOT NULL  -- 同步状态
);

-- 创建索引
CREATE INDEX IF NOT EXISTS idx_classroom_teacher ON Classroom(teacherId);
CREATE INDEX IF NOT EXISTS idx_student_class ON Student(classId);
CREATE INDEX IF NOT EXISTS idx_face_student ON FaceEmbedding(studentId);
CREATE INDEX IF NOT EXISTS idx_attendance_class ON AttendanceSession(classId);
CREATE INDEX IF NOT EXISTS idx_result_session ON AttendanceResult(sessionId);
CREATE INDEX IF NOT EXISTS idx_result_student ON AttendanceResult(studentId);
CREATE INDEX IF NOT EXISTS idx_photo_session ON PhotoAsset(sessionId);
CREATE INDEX IF NOT EXISTS idx_sync_entity ON SyncLog(entity, entityId);

-- 创建触发器：更新Teacher表的updatedAt字段
CREATE TRIGGER IF NOT EXISTS update_teacher_timestamp 
AFTER UPDATE ON Teacher
BEGIN
    UPDATE Teacher SET updatedAt = CURRENT_TIMESTAMP WHERE id = NEW.id;
END;