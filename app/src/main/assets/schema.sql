/* 教师表 */
CREATE TABLE IF NOT EXISTS Teacher (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    name TEXT NOT NULL,
    username TEXT UNIQUE NOT NULL,
    password TEXT NOT NULL,
    avatarUri TEXT,
    createdAt TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updatedAt TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

/* 班级表 */
CREATE TABLE IF NOT EXISTS Classroom (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    teacherId INTEGER NOT NULL,
    name TEXT NOT NULL,
    year INTEGER NOT NULL,
    meta TEXT,
    FOREIGN KEY (teacherId) REFERENCES Teacher(id) ON DELETE CASCADE
);

/* 学生表 */
CREATE TABLE IF NOT EXISTS Student (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    classId INTEGER NOT NULL,
    name TEXT NOT NULL,
    sid TEXT NOT NULL,
    gender TEXT CHECK(gender IN ('M', 'F', 'O')),
    avatarUri TEXT,
    createdAt TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (classId) REFERENCES Classroom(id) ON DELETE CASCADE
);

/* 人脸特征表 */
CREATE TABLE IF NOT EXISTS FaceEmbedding (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    studentId INTEGER NOT NULL,
    modelVer TEXT NOT NULL,
    vector BLOB NOT NULL,
    quality REAL CHECK(quality >= 0 AND quality <= 1),
    createdAt TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (studentId) REFERENCES Student(id) ON DELETE CASCADE
);

/* 考勤会话表 */
CREATE TABLE IF NOT EXISTS AttendanceSession (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    classId INTEGER NOT NULL,
    startedAt TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    location TEXT,
    photoUri TEXT,
    note TEXT,
    FOREIGN KEY (classId) REFERENCES Classroom(id) ON DELETE CASCADE
);

/* 考勤结果表 */
CREATE TABLE IF NOT EXISTS AttendanceResult (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    sessionId INTEGER NOT NULL,
    studentId INTEGER NOT NULL,
    status TEXT NOT NULL CHECK(status IN ('Present', 'Absent', 'Unknown')),
    score REAL CHECK(score >= 0 AND score <= 1),
    decidedBy TEXT NOT NULL CHECK(decidedBy IN ('AUTO', 'TEACHER')),
    decidedAt TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (sessionId) REFERENCES AttendanceSession(id) ON DELETE CASCADE,
    FOREIGN KEY (studentId) REFERENCES Student(id) ON DELETE CASCADE
);

/* 照片资源表 */
CREATE TABLE IF NOT EXISTS PhotoAsset (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    sessionId INTEGER NOT NULL,
    type TEXT NOT NULL CHECK(type IN ('RAW', 'ALIGNED', 'DEBUG')),
    uri TEXT NOT NULL,
    meta TEXT,
    FOREIGN KEY (sessionId) REFERENCES AttendanceSession(id) ON DELETE CASCADE
);

/* 同步日志表 */
CREATE TABLE IF NOT EXISTS SyncLog (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    entity TEXT NOT NULL,
    entityId INTEGER NOT NULL,
    op TEXT NOT NULL CHECK(op IN ('UPSERT', 'DELETE')),
    version INTEGER NOT NULL,
    ts TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    status TEXT NOT NULL
);

/* 创建索引 */
CREATE INDEX IF NOT EXISTS idx_classroom_teacher ON Classroom(teacherId);
CREATE INDEX IF NOT EXISTS idx_student_class ON Student(classId);
CREATE INDEX IF NOT EXISTS idx_face_student ON FaceEmbedding(studentId);
CREATE INDEX IF NOT EXISTS idx_attendance_class ON AttendanceSession(classId);
CREATE INDEX IF NOT EXISTS idx_result_session ON AttendanceResult(sessionId);
CREATE INDEX IF NOT EXISTS idx_result_student ON AttendanceResult(studentId);
CREATE INDEX IF NOT EXISTS idx_photo_session ON PhotoAsset(sessionId);
CREATE INDEX IF NOT EXISTS idx_sync_entity ON SyncLog(entity, entityId);

/* 创建触发器：更新Teacher表的updatedAt字段 */
CREATE TRIGGER IF NOT EXISTS update_teacher_timestamp 
AFTER UPDATE ON Teacher
BEGIN
    UPDATE Teacher SET updatedAt = CURRENT_TIMESTAMP WHERE id = NEW.id;
END;

/*
开发导出说明：
当在课堂页面使用“一键提取向量”批量处理时，系统会将每个学生的向量以JSON行的形式追加到本文件末尾（仅开发调试用途，不影响数据库建表）。
每行示例：
{"studentId":1,"vector":[0.123,-0.045,...],"quality":0.86,"model":"v1","createdAt":"2025-10-30 10:00:00"}
*/