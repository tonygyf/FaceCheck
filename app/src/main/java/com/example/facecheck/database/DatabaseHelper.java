package com.example.facecheck.database;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.util.Log;

import com.example.facecheck.data.model.Student;

import java.util.ArrayList;
import java.util.List;

import com.example.facecheck.data.model.Teacher;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;

public class DatabaseHelper extends SQLiteOpenHelper {
    private static final String TAG = "DatabaseHelper";
    private static final String DATABASE_NAME = "facecheck.db";
    private static final int DATABASE_VERSION = 4; // 增加版本号以触发数据库重建
    private Context context;

    public DatabaseHelper(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
        this.context = context;
        Log.d(TAG, "数据库初始化完成: " + DATABASE_NAME);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        Log.d(TAG, "创建数据库表结构");
        createAllTables(db);
        insertDefaultTeacher(db); // 插入默认教师数据用于测试
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        Log.d(TAG, "数据库升级: " + oldVersion + " -> " + newVersion);
        // 删除所有旧表
        dropAllTables(db);
        // 重新创建表结构
        onCreate(db);
    }

    /**
     * 创建所有数据库表
     */
    private void createAllTables(SQLiteDatabase db) {
        try {
            // 1. 创建教师表
            createTeacherTable(db);
            // 2. 创建班级表
            createClassroomTable(db);
            // 3. 创建学生表
            createStudentTable(db);
            // 4. 创建人脸特征表
            createFaceEmbeddingTable(db);
            // 5. 创建考勤会话表
            createAttendanceSessionTable(db);
            // 6. 创建考勤结果表
            createAttendanceResultTable(db);
            // 7. 创建照片资源表
            createPhotoAssetTable(db);
            // 8. 创建同步日志表
            createSyncLogTable(db);
            // 9. 创建索引
            createIndexes(db);
            // 10. 创建触发器
            createTriggers(db);
            
            Log.d(TAG, "所有数据库表创建完成");
        } catch (Exception e) {
            Log.e(TAG, "创建数据库表失败", e);
            throw new RuntimeException("数据库初始化失败", e);
        }
    }

    /**
     * 创建教师表
     */
    private void createTeacherTable(SQLiteDatabase db) {
        String sql = "CREATE TABLE IF NOT EXISTS Teacher (" +
                "id INTEGER PRIMARY KEY AUTOINCREMENT, " +
                "name TEXT NOT NULL, " +
                "username TEXT UNIQUE NOT NULL, " +
                "password TEXT NOT NULL, " +
                "email TEXT, " +
                "phone TEXT, " +
                "avatarUri TEXT, " +
                "createdAt TIMESTAMP DEFAULT CURRENT_TIMESTAMP, " +
                "updatedAt TIMESTAMP DEFAULT CURRENT_TIMESTAMP" +
                ")";
        db.execSQL(sql);
        Log.d(TAG, "Teacher表创建完成");
    }

    /**
     * 创建班级表
     */
    private void createClassroomTable(SQLiteDatabase db) {
        String sql = "CREATE TABLE IF NOT EXISTS Classroom (" +
                "id INTEGER PRIMARY KEY AUTOINCREMENT, " +
                "teacherId INTEGER NOT NULL, " +
                "name TEXT NOT NULL, " +
                "year INTEGER NOT NULL, " +
                "semester TEXT, " +
                "courseName TEXT, " +
                "meta TEXT, " +
                "createdAt TIMESTAMP DEFAULT CURRENT_TIMESTAMP, " +
                "FOREIGN KEY (teacherId) REFERENCES Teacher(id) ON DELETE CASCADE" +
                ")";
        db.execSQL(sql);
        Log.d(TAG, "Classroom表创建完成");
    }

    /**
     * 创建学生表
     */
    private void createStudentTable(SQLiteDatabase db) {
        String sql = "CREATE TABLE IF NOT EXISTS Student (" +
                "id INTEGER PRIMARY KEY AUTOINCREMENT, " +
                "classId INTEGER NOT NULL, " +
                "name TEXT NOT NULL, " +
                "sid TEXT NOT NULL, " +
                "gender TEXT CHECK(gender IN ('M', 'F', 'O')), " +
                "birthDate TEXT, " +
                "email TEXT, " +
                "phone TEXT, " +
                "avatarUri TEXT, " +
                "enrollmentDate TIMESTAMP DEFAULT CURRENT_TIMESTAMP, " +
                "status TEXT DEFAULT 'ACTIVE' CHECK(status IN ('ACTIVE', 'INACTIVE', 'GRADUATED')), " +
                "createdAt TIMESTAMP DEFAULT CURRENT_TIMESTAMP, " +
                "updatedAt TIMESTAMP DEFAULT CURRENT_TIMESTAMP, " +
                "FOREIGN KEY (classId) REFERENCES Classroom(id) ON DELETE CASCADE" +
                ")";
        db.execSQL(sql);
        Log.d(TAG, "Student表创建完成");
    }

    /**
     * 创建人脸特征表
     */
    private void createFaceEmbeddingTable(SQLiteDatabase db) {
        String sql = "CREATE TABLE IF NOT EXISTS FaceEmbedding (" +
                "id INTEGER PRIMARY KEY AUTOINCREMENT, " +
                "studentId INTEGER NOT NULL, " +
                "modelVer TEXT NOT NULL, " +
                "vector BLOB NOT NULL, " +
                "quality REAL CHECK(quality >= 0 AND quality <= 1), " +
                "faceImageUri TEXT, " +
                "isActive INTEGER DEFAULT 1, " +
                "createdAt TIMESTAMP DEFAULT CURRENT_TIMESTAMP, " +
                "FOREIGN KEY (studentId) REFERENCES Student(id) ON DELETE CASCADE" +
                ")";
        db.execSQL(sql);
        Log.d(TAG, "FaceEmbedding表创建完成");
    }

    /**
     * 创建考勤会话表
     */
    private void createAttendanceSessionTable(SQLiteDatabase db) {
        String sql = "CREATE TABLE IF NOT EXISTS AttendanceSession (" +
                "id INTEGER PRIMARY KEY AUTOINCREMENT, " +
                "classId INTEGER NOT NULL, " +
                "teacherId INTEGER NOT NULL, " +
                "startedAt TIMESTAMP DEFAULT CURRENT_TIMESTAMP, " +
                "endedAt TIMESTAMP, " +
                "location TEXT, " +
                "photoUri TEXT, " +
                "note TEXT, " +
                "status TEXT DEFAULT 'ACTIVE' CHECK(status IN ('ACTIVE', 'COMPLETED', 'CANCELLED')), " +
                "attendanceType TEXT DEFAULT 'FACE' CHECK(attendanceType IN ('FACE', 'MANUAL', 'MIXED')), " +
                "FOREIGN KEY (classId) REFERENCES Classroom(id) ON DELETE CASCADE, " +
                "FOREIGN KEY (teacherId) REFERENCES Teacher(id) ON DELETE CASCADE" +
                ")";
        db.execSQL(sql);
        Log.d(TAG, "AttendanceSession表创建完成");
    }

    /**
     * 创建考勤结果表
     */
    private void createAttendanceResultTable(SQLiteDatabase db) {
        String sql = "CREATE TABLE IF NOT EXISTS AttendanceResult (" +
                "id INTEGER PRIMARY KEY AUTOINCREMENT, " +
                "sessionId INTEGER NOT NULL, " +
                "studentId INTEGER NOT NULL, " +
                "status TEXT NOT NULL CHECK(status IN ('Present', 'Absent', 'Late', 'Leave', 'Unknown')), " +
                "score REAL CHECK(score >= 0 AND score <= 1), " +
                "confidence REAL, " +
                "checkInTime TIMESTAMP, " +
                "checkOutTime TIMESTAMP, " +
                "decidedBy TEXT NOT NULL CHECK(decidedBy IN ('AUTO', 'TEACHER', 'SYSTEM')), " +
                "decidedAt TIMESTAMP DEFAULT CURRENT_TIMESTAMP, " +
                "note TEXT, " +
                "FOREIGN KEY (sessionId) REFERENCES AttendanceSession(id) ON DELETE CASCADE, " +
                "FOREIGN KEY (studentId) REFERENCES Student(id) ON DELETE CASCADE" +
                ")";
        db.execSQL(sql);
        Log.d(TAG, "AttendanceResult表创建完成");
    }

    /**
     * 创建照片资源表
     */
    private void createPhotoAssetTable(SQLiteDatabase db) {
        String sql = "CREATE TABLE IF NOT EXISTS PhotoAsset (" +
                "id INTEGER PRIMARY KEY AUTOINCREMENT, " +
                "sessionId INTEGER NOT NULL, " +
                "studentId INTEGER, " +
                "type TEXT NOT NULL CHECK(type IN ('RAW', 'ALIGNED', 'DETECTED', 'FEATURE', 'DEBUG')), " +
                "uri TEXT NOT NULL, " +
                "fileSize INTEGER, " +
                "width INTEGER, " +
                "height INTEGER, " +
                "format TEXT, " +
                "meta TEXT, " +
                "createdAt TIMESTAMP DEFAULT CURRENT_TIMESTAMP, " +
                "FOREIGN KEY (sessionId) REFERENCES AttendanceSession(id) ON DELETE CASCADE, " +
                "FOREIGN KEY (studentId) REFERENCES Student(id) ON DELETE SET NULL" +
                ")";
        db.execSQL(sql);
        Log.d(TAG, "PhotoAsset表创建完成");
    }

    /**
     * 创建同步日志表
     */
    private void createSyncLogTable(SQLiteDatabase db) {
        String sql = "CREATE TABLE IF NOT EXISTS SyncLog (" +
                "id INTEGER PRIMARY KEY AUTOINCREMENT, " +
                "entity TEXT NOT NULL, " +
                "entityId INTEGER NOT NULL, " +
                "op TEXT NOT NULL CHECK(op IN ('INSERT', 'UPDATE', 'DELETE')), " +
                "version INTEGER NOT NULL, " +
                "ts TIMESTAMP DEFAULT CURRENT_TIMESTAMP, " +
                "status TEXT NOT NULL CHECK(status IN ('PENDING', 'SYNCED', 'FAILED', 'CONFLICT'))" +
                ")";
        db.execSQL(sql);
        Log.d(TAG, "SyncLog表创建完成");
    }

    /**
     * 创建索引
     */
    private void createIndexes(SQLiteDatabase db) {
        // 教师表索引
        db.execSQL("CREATE INDEX IF NOT EXISTS idx_teacher_username ON Teacher(username)");
        db.execSQL("CREATE INDEX IF NOT EXISTS idx_teacher_email ON Teacher(email)");
        
        // 班级表索引
        db.execSQL("CREATE INDEX IF NOT EXISTS idx_classroom_teacher ON Classroom(teacherId)");
        db.execSQL("CREATE INDEX IF NOT EXISTS idx_classroom_year ON Classroom(year)");
        
        // 学生表索引
        db.execSQL("CREATE INDEX IF NOT EXISTS idx_student_class ON Student(classId)");
        db.execSQL("CREATE INDEX IF NOT EXISTS idx_student_sid ON Student(sid)");
        db.execSQL("CREATE INDEX IF NOT EXISTS idx_student_status ON Student(status)");
        
        // 人脸特征索引
        db.execSQL("CREATE INDEX IF NOT EXISTS idx_face_embedding_student ON FaceEmbedding(studentId)");
        db.execSQL("CREATE INDEX IF NOT EXISTS idx_face_embedding_active ON FaceEmbedding(isActive)");
        
        // 考勤会话索引
        db.execSQL("CREATE INDEX IF NOT EXISTS idx_attendance_session_class ON AttendanceSession(classId)");
        db.execSQL("CREATE INDEX IF NOT EXISTS idx_attendance_session_teacher ON AttendanceSession(teacherId)");
        db.execSQL("CREATE INDEX IF NOT EXISTS idx_attendance_session_status ON AttendanceSession(status)");
        db.execSQL("CREATE INDEX IF NOT EXISTS idx_attendance_session_started ON AttendanceSession(startedAt)");
        
        // 考勤结果索引
        db.execSQL("CREATE INDEX IF NOT EXISTS idx_attendance_result_session ON AttendanceResult(sessionId)");
        db.execSQL("CREATE INDEX IF NOT EXISTS idx_attendance_result_student ON AttendanceResult(studentId)");
        db.execSQL("CREATE INDEX IF NOT EXISTS idx_attendance_result_status ON AttendanceResult(status)");
        db.execSQL("CREATE INDEX IF NOT EXISTS idx_attendance_result_time ON AttendanceResult(checkInTime)");
        
        // 照片资源索引
        db.execSQL("CREATE INDEX IF NOT EXISTS idx_photo_asset_session ON PhotoAsset(sessionId)");
        db.execSQL("CREATE INDEX IF NOT EXISTS idx_photo_asset_student ON PhotoAsset(studentId)");
        db.execSQL("CREATE INDEX IF NOT EXISTS idx_photo_asset_type ON PhotoAsset(type)");
        
        // 同步日志索引
        db.execSQL("CREATE INDEX IF NOT EXISTS idx_sync_log_entity ON SyncLog(entity, entityId)");
        db.execSQL("CREATE INDEX IF NOT EXISTS idx_sync_log_status ON SyncLog(status)");
        db.execSQL("CREATE INDEX IF NOT EXISTS idx_sync_log_ts ON SyncLog(ts)");
        
        Log.d(TAG, "所有索引创建完成");
    }

    /**
     * 创建触发器
     */
    private void createTriggers(SQLiteDatabase db) {
        // 更新教师表的updatedAt字段
        db.execSQL("CREATE TRIGGER IF NOT EXISTS update_teacher_timestamp " +
                "AFTER UPDATE ON Teacher " +
                "BEGIN " +
                "UPDATE Teacher SET updatedAt = CURRENT_TIMESTAMP WHERE id = NEW.id; " +
                "END");
        
        // 更新班级表的updatedAt字段
        db.execSQL("CREATE TRIGGER IF NOT EXISTS update_classroom_timestamp " +
                "AFTER UPDATE ON Classroom " +
                "BEGIN " +
                "UPDATE Classroom SET updatedAt = CURRENT_TIMESTAMP WHERE id = NEW.id; " +
                "END");
        
        // 更新学生表的updatedAt字段
        db.execSQL("CREATE TRIGGER IF NOT EXISTS update_student_timestamp " +
                "AFTER UPDATE ON Student " +
                "BEGIN " +
                "UPDATE Student SET updatedAt = CURRENT_TIMESTAMP WHERE id = NEW.id; " +
                "END");
        
        Log.d(TAG, "所有触发器创建完成");
    }

    /**
     * 删除所有表（用于升级）
     */
    private void dropAllTables(SQLiteDatabase db) {
        db.execSQL("DROP TABLE IF EXISTS SyncLog");
        db.execSQL("DROP TABLE IF EXISTS PhotoAsset");
        db.execSQL("DROP TABLE IF EXISTS AttendanceResult");
        db.execSQL("DROP TABLE IF EXISTS AttendanceSession");
        db.execSQL("DROP TABLE IF EXISTS FaceEmbedding");
        db.execSQL("DROP TABLE IF EXISTS Student");
        db.execSQL("DROP TABLE IF EXISTS Classroom");
        db.execSQL("DROP TABLE IF EXISTS Teacher");
        Log.d(TAG, "所有旧表已删除");
    }

    /**
     * 插入默认教师数据（用于测试）
     */
    private void insertDefaultTeacher(SQLiteDatabase db) {
        try {
            ContentValues values = new ContentValues();
            values.put("name", "管理员");
            values.put("username", "admin");
            values.put("password", "admin123");
            values.put("email", "admin@example.com");
            values.put("phone", "13800138000");
            
            long adminId = db.insert("Teacher", null, values);
            Log.d(TAG, "默认管理员教师插入成功，ID: " + adminId);
            
            // 再插入一个测试教师
            values.clear();
            values.put("name", "李老师");
            values.put("username", "lilaoshi");
            values.put("password", "123456");
            values.put("email", "li@example.com");
            values.put("phone", "13900139000");
            
            long teacherId = db.insert("Teacher", null, values);
            Log.d(TAG, "测试教师插入成功，ID: " + teacherId);
            
            // 插入测试班级数据
            insertDefaultClassrooms(db, adminId);
            insertDefaultClassrooms(db, teacherId);
            
        } catch (Exception e) {
            Log.e(TAG, "插入默认教师数据失败", e);
        }
    }
    
    /**
     * 插入默认班级数据（用于测试）
     */
    private void insertDefaultClassrooms(SQLiteDatabase db, long teacherId) {
        try {
            ContentValues values = new ContentValues();
            values.put("teacherId", teacherId);
            values.put("name", "计算机科学2024级1班");
            values.put("year", 2024);
            values.put("semester", "秋季学期");
            values.put("courseName", "软件工程");
            values.put("meta", "{\"building\":\"教学楼A\",\"room\":\"A101\",\"capacity\":45}");
            
            long classId1 = db.insert("Classroom", null, values);
            Log.d(TAG, "测试班级1插入成功，ID: " + classId1);
            
            values.clear();
            values.put("teacherId", teacherId);
            values.put("name", "计算机科学2024级2班");
            values.put("year", 2024);
            values.put("semester", "秋季学期");
            values.put("courseName", "数据结构");
            values.put("meta", "{\"building\":\"教学楼B\",\"room\":\"B201\",\"capacity\":50}");
            
            long classId2 = db.insert("Classroom", null, values);
            Log.d(TAG, "测试班级2插入成功，ID: " + classId2);
            
            // 插入测试学生数据
            insertDefaultStudents(db, classId1);
            insertDefaultStudents(db, classId2);
            
        } catch (Exception e) {
            Log.e(TAG, "插入默认班级数据失败", e);
        }
    }
    
    /**
     * 插入默认学生数据（用于测试）
     */
    private void insertDefaultStudents(SQLiteDatabase db, long classId) {
        try {
            String[] names = {"张三", "李四", "王五", "赵六", "钱七", "孙八", "周九", "吴十"};
            String[] sids = {"2024001", "2024002", "2024003", "2024004", "2024005", "2024006", "2024007", "2024008"};
            
            for (int i = 0; i < names.length; i++) {
                ContentValues values = new ContentValues();
                values.put("classId", classId);
                values.put("name", names[i]);
                values.put("sid", sids[i]);
                values.put("gender", i % 2 == 0 ? "M" : "F");
                values.put("birthDate", "2002-01-" + String.format("%02d", i + 1));
                values.put("email", sids[i] + "@student.edu.cn");
                values.put("phone", "1500000000" + i);
                values.put("status", "ACTIVE");
                
                long studentId = db.insert("Student", null, values);
                Log.d(TAG, "测试学生插入成功，ID: " + studentId + ", 姓名: " + names[i]);
            }
            
        } catch (Exception e) {
            Log.e(TAG, "插入默认学生数据失败", e);
        }
    }

    // ============= 教师相关操作 =============
    
    public Teacher getTeacherById(long id) {
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = db.query("Teacher", 
                new String[]{"id", "name", "username", "password", "email", "phone", "avatarUri", "createdAt", "updatedAt"},
                "id = ?",
                new String[]{String.valueOf(id)},
                null, null, null);

        Teacher teacher = null;
        if (cursor != null && cursor.moveToFirst()) {
            teacher = new Teacher(
                    cursor.getLong(cursor.getColumnIndexOrThrow("id")),
                    cursor.getString(cursor.getColumnIndexOrThrow("name")),
                    cursor.getString(cursor.getColumnIndexOrThrow("username")),
                    cursor.getString(cursor.getColumnIndexOrThrow("password")),
                    cursor.getLong(cursor.getColumnIndexOrThrow("createdAt")),
                    cursor.getLong(cursor.getColumnIndexOrThrow("updatedAt"))
            );
            cursor.close();
        }
        return teacher;
    }

    public boolean addTeacher(Teacher teacher) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put("name", teacher.getName());
        values.put("username", teacher.getUsername());
        values.put("password", teacher.getPassword());
        values.put("createdAt", teacher.getCreatedAt());
        values.put("updatedAt", teacher.getUpdatedAt());

        long id = db.insert("Teacher", null, values);
        return id != -1;
    }

    public boolean updateTeacher(Teacher teacher) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put("name", teacher.getName());
        values.put("username", teacher.getUsername());
        values.put("password", teacher.getPassword());
        values.put("updatedAt", System.currentTimeMillis());

        int rowsAffected = db.update("Teacher", values,
                "id = ?", new String[]{String.valueOf(teacher.getId())});
        return rowsAffected > 0;
    }

    public Cursor getTeacherByUsername(String username) {
        SQLiteDatabase db = this.getReadableDatabase();
        return db.query("Teacher", null, "username = ?", new String[]{username}, null, null, null);
    }

    // ============= 班级相关操作 =============
    
    public long insertClassroom(long teacherId, String name, int year, String meta) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put("teacherId", teacherId);
        values.put("name", name);
        values.put("year", year);
        values.put("meta", meta);
        values.put("createdAt", System.currentTimeMillis());
        return db.insert("Classroom", null, values);
    }

    public Cursor getClassroomsByTeacher(long teacherId) {
        SQLiteDatabase db = this.getReadableDatabase();
        return db.query("Classroom", null, "teacherId = ?", 
                new String[]{String.valueOf(teacherId)}, null, null, "year DESC, name");
    }

    // ============= 学生相关操作 =============
    
    public long insertStudent(long classId, String name, String sid, String gender, String avatarUri) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put("classId", classId);
        values.put("name", name);
        values.put("sid", sid);
        values.put("gender", gender);
        values.put("avatarUri", avatarUri);
        values.put("createdAt", System.currentTimeMillis());
        return db.insert("Student", null, values);
    }

    public Cursor getStudentsByClass(long classId) {
        SQLiteDatabase db = this.getReadableDatabase();
        return db.query("Student", null, "classId = ?", 
                new String[]{String.valueOf(classId)}, null, null, "name");
    }

    public Cursor getStudentById(long studentId) {
        SQLiteDatabase db = this.getReadableDatabase();
        return db.query("Student", null, "id = ?", 
                new String[]{String.valueOf(studentId)}, null, null, null);
    }

    /**
     * 更新学生信息
     */
    public boolean updateStudent(long studentId, long classId, String name, String sid, String gender, String avatarUri) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put("classId", classId);
        values.put("name", name);
        values.put("sid", sid);
        values.put("gender", gender);
        values.put("avatarUri", avatarUri);
        values.put("updatedAt", System.currentTimeMillis());
        
        int result = db.update("Student", values, "id = ?", 
                              new String[]{String.valueOf(studentId)});
        return result > 0;
    }

    /**
     * 删除学生
     */
    public boolean deleteStudent(long studentId) {
        SQLiteDatabase db = this.getWritableDatabase();
        
        // 首先删除相关的考勤结果
        db.delete("AttendanceResult", "studentId = ?", 
                 new String[]{String.valueOf(studentId)});
        
        // 删除相关的人脸特征数据
        db.delete("FaceEmbedding", "studentId = ?", 
                 new String[]{String.valueOf(studentId)});
        
        // 删除相关的照片资源
        db.delete("PhotoAsset", "studentId = ?", 
                 new String[]{String.valueOf(studentId)});
        
        // 最后删除学生记录
        int result = db.delete("Student", "id = ?", 
                              new String[]{String.valueOf(studentId)});
        
        return result > 0;
    }

    public List<Student> getAllStudents() {
        List<Student> students = new ArrayList<>();
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = db.query("Student", null, null, null, null, null, "name");
        
        if (cursor != null && cursor.moveToFirst()) {
            do {
                long id = cursor.getLong(cursor.getColumnIndexOrThrow("id"));
                long classId = cursor.getLong(cursor.getColumnIndexOrThrow("classId"));
                String name = cursor.getString(cursor.getColumnIndexOrThrow("name"));
                String sid = cursor.getString(cursor.getColumnIndexOrThrow("sid"));
                String gender = cursor.getString(cursor.getColumnIndexOrThrow("gender"));
                String avatarUri = cursor.getString(cursor.getColumnIndexOrThrow("avatarUri"));
                long createdAt = cursor.getLong(cursor.getColumnIndexOrThrow("createdAt"));
                
                Student student = new Student(id, classId, name, sid, gender, avatarUri, createdAt);
                students.add(student);
            } while (cursor.moveToNext());
            cursor.close();
        }
        
        return students;
    }

    public List<Student> getStudentsByTeacher(long teacherId) {
        List<Student> students = new ArrayList<>();
        SQLiteDatabase db = this.getReadableDatabase();
        
        // 通过班级表关联查询该教师的所有学生，按学号升序排列
        String query = "SELECT s.* FROM Student s " +
                      "INNER JOIN Classroom c ON s.classId = c.id " +
                      "WHERE c.teacherId = ? " +
                      "ORDER BY CAST(s.sid AS INTEGER) ASC";
        
        Cursor cursor = db.rawQuery(query, new String[]{String.valueOf(teacherId)});
        
        if (cursor != null && cursor.moveToFirst()) {
            do {
                long id = cursor.getLong(cursor.getColumnIndexOrThrow("id"));
                long classId = cursor.getLong(cursor.getColumnIndexOrThrow("classId"));
                String name = cursor.getString(cursor.getColumnIndexOrThrow("name"));
                String sid = cursor.getString(cursor.getColumnIndexOrThrow("sid"));
                String gender = cursor.getString(cursor.getColumnIndexOrThrow("gender"));
                String avatarUri = cursor.getString(cursor.getColumnIndexOrThrow("avatarUri"));
                long createdAt = cursor.getLong(cursor.getColumnIndexOrThrow("createdAt"));
                
                Student student = new Student(id, classId, name, sid, gender, avatarUri, createdAt);
                students.add(student);
            } while (cursor.moveToNext());
            cursor.close();
        }
        
        return students;
    }

    // ============= 人脸特征相关操作 =============
    
    public long insertFaceEmbedding(long studentId, String modelVer, byte[] vector, float quality) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put("studentId", studentId);
        values.put("modelVer", modelVer);
        values.put("vector", vector);
        values.put("quality", quality);
        values.put("createdAt", System.currentTimeMillis());
        return db.insert("FaceEmbedding", null, values);
    }

    public Cursor getFaceEmbeddingsByStudent(long studentId) {
        SQLiteDatabase db = this.getReadableDatabase();
        return db.query("FaceEmbedding", null, "studentId = ?", 
                new String[]{String.valueOf(studentId)}, null, null, "quality DESC");
    }

    // ============= 考勤会话相关操作 =============
    
    public long insertAttendanceSession(long classId, long teacherId, String location, String photoUri, String note) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put("classId", classId);
        values.put("teacherId", teacherId);
        values.put("location", location);
        values.put("photoUri", photoUri);
        values.put("note", note);
        values.put("startedAt", System.currentTimeMillis());
        return db.insert("AttendanceSession", null, values);
    }

    // ============= 考勤结果相关操作 =============
    
    public long insertAttendanceResult(long sessionId, long studentId, String status, 
                                     float score, String decidedBy) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put("sessionId", sessionId);
        values.put("studentId", studentId);
        values.put("status", status);
        values.put("score", score);
        values.put("decidedBy", decidedBy);
        values.put("decidedAt", System.currentTimeMillis());
        return db.insert("AttendanceResult", null, values);
    }

    public boolean updateAttendanceResult(long id, String status, String decidedBy) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put("status", status);
        values.put("decidedBy", decidedBy);
        values.put("decidedAt", System.currentTimeMillis());
        return db.update("AttendanceResult", values, "id = ?", 
                new String[]{String.valueOf(id)}) > 0;
    }

    public Cursor getAttendanceResultsBySession(long sessionId) {
        SQLiteDatabase db = this.getReadableDatabase();
        return db.query("AttendanceResult", null, "sessionId = ?", 
                new String[]{String.valueOf(sessionId)}, null, null, "decidedAt DESC");
    }

    // ============= 照片资源相关操作 =============
    
    public long insertPhotoAsset(long sessionId, Long studentId, String type, String uri, String meta) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put("sessionId", sessionId);
        if (studentId != null) {
            values.put("studentId", studentId);
        }
        values.put("type", type);
        values.put("uri", uri);
        values.put("meta", meta);
        values.put("createdAt", System.currentTimeMillis());
        return db.insert("PhotoAsset", null, values);
    }

    // ============= 同步日志相关操作 =============
    
    public long insertSyncLog(String entity, long entityId, String op, long version, String status) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put("entity", entity);
        values.put("entityId", entityId);
        values.put("op", op);
        values.put("version", version);
        values.put("status", status);
        return db.insert("SyncLog", null, values);
    }

    public Cursor getPendingSyncLogs() {
        SQLiteDatabase db = this.getReadableDatabase();
        return db.query("SyncLog", null, "status = ?", 
                new String[]{"PENDING"}, null, null, "ts ASC");
    }

    public boolean updateSyncLogStatus(long id, String status) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put("status", status);
        return db.update("SyncLog", values, "id = ?", 
                new String[]{String.valueOf(id)}) > 0;
    }

    /**
     * 更新学生人脸数据
     */
    public boolean updateStudentFaceData(Student student) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put("faceFeatures", student.getFaceFeatures());
        values.put("faceImagePath", student.getFaceImagePath());
        
        int result = db.update("Student", values, "id = ?", 
                              new String[]{String.valueOf(student.getId())});
        db.close();
        
        return result > 0;
    }

    // ============= 统计相关操作 =============
    
    /**
     * 获取指定教师的班级数量
     */
    public int getClassroomCountByTeacher(long teacherId) {
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = db.rawQuery("SELECT COUNT(*) FROM Classroom WHERE teacherId = ?", 
                                   new String[]{String.valueOf(teacherId)});
        int count = 0;
        if (cursor != null && cursor.moveToFirst()) {
            count = cursor.getInt(0);
            cursor.close();
        }
        return count;
    }
    
    /**
     * 获取指定教师的学生数量
     */
    public int getStudentCountByTeacher(long teacherId) {
        SQLiteDatabase db = this.getReadableDatabase();
        String query = "SELECT COUNT(*) FROM Student s " +
                      "INNER JOIN Classroom c ON s.classId = c.id " +
                      "WHERE c.teacherId = ?";
        Cursor cursor = db.rawQuery(query, new String[]{String.valueOf(teacherId)});
        int count = 0;
        if (cursor != null && cursor.moveToFirst()) {
            count = cursor.getInt(0);
            cursor.close();
        }
        return count;
    }
    
    /**
     * 获取指定教师的考勤记录数量
     */
    public int getAttendanceCountByTeacher(long teacherId) {
        SQLiteDatabase db = this.getReadableDatabase();
        String query = "SELECT COUNT(*) FROM AttendanceResult ar " +
                      "INNER JOIN AttendanceSession a ON ar.sessionId = a.id " +
                      "WHERE a.teacherId = ?";
        Cursor cursor = db.rawQuery(query, new String[]{String.valueOf(teacherId)});
        int count = 0;
        if (cursor != null && cursor.moveToFirst()) {
            count = cursor.getInt(0);
            cursor.close();
        }
        return count;
    }
}