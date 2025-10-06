package com.example.facecheck.database;

import android.content.ContentValues;
import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.util.Log;

import com.example.facecheck.data.model.Teacher;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;

public class DatabaseHelper extends SQLiteOpenHelper {
    private static final String TAG = "DatabaseHelper";
    private static final String DATABASE_NAME = "fc.db";
    private static final int DATABASE_VERSION = 1;
    private Context context;

    public DatabaseHelper(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
        this.context = context;
        
        // 检查数据库是否存在，如果不存在则从assets复制
        File dbFile = context.getDatabasePath(DATABASE_NAME);
        if (!dbFile.exists()) {
            copyDatabaseFromAssets();
        }
    }
    
    private boolean copyDatabaseFromAssets() {
        try {
            // 创建目标目录
            File dbDir = context.getDatabasePath(DATABASE_NAME).getParentFile();
            if (!dbDir.exists()) {
                dbDir.mkdirs();
            }
            
            // 尝试从assets复制fc.db
            try {
                InputStream inputStream = context.getAssets().open(DATABASE_NAME);
                File outFile = context.getDatabasePath(DATABASE_NAME);
                FileOutputStream outputStream = new FileOutputStream(outFile);
                
                byte[] buffer = new byte[1024];
                int length;
                while ((length = inputStream.read(buffer)) > 0) {
                    outputStream.write(buffer, 0, length);
                }
                
                outputStream.flush();
                outputStream.close();
                inputStream.close();
                return true;
            } catch (IOException e) {
                // 如果fc.db不存在，则回退到使用schema.sql创建数据库
                Log.e(TAG, "复制fc.db失败，将使用schema.sql创建数据库", e);
                return false;
            }
        } catch (Exception e) {
            Log.e(TAG, "初始化数据库失败", e);
            return false;
        }
    }

    public Teacher getTeacherById(long id) {
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = db.query("Teacher", 
            new String[]{"id", "name", "username", "password"},
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
                "", // email
                "", // davUrl
                "", // davUser
                "", // davKeyEnc
                System.currentTimeMillis(), // createdAt
                System.currentTimeMillis()  // updatedAt
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

        long id = db.insert("Teacher", null, values);
        return id != -1;
    }

    public boolean updateTeacher(Teacher teacher) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put("name", teacher.getName());
        values.put("username", teacher.getUsername());
        values.put("password", teacher.getPassword());

        int rowsAffected = db.update("Teacher", values,
            "id = ?", new String[]{String.valueOf(teacher.getId())});
        return rowsAffected > 0;
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        try {
            // 从schema.sql创建数据库表
            Log.d(TAG, "Creating database tables from schema.sql");
            // 从assets中读取schema.sql文件
            BufferedReader reader = new BufferedReader(
                new InputStreamReader(context.getAssets().open("schema.sql"))
            );
            
            StringBuilder sql = new StringBuilder();
            String line;
            
            // 读取文件内容
            while ((line = reader.readLine()) != null) {
                // 跳过注释和空行
                if (line.startsWith("--") || line.trim().isEmpty()) {
                    continue;
                }
                sql.append(line);
                sql.append(" ");
                
                // 如果是完整的SQL语句，则执行
                if (line.trim().endsWith(";")) {
                    db.execSQL(sql.toString());
                    sql.setLength(0);
                }
            }
            
            reader.close();
            Log.d(TAG, "Successfully created database tables from schema.sql");
        } catch (IOException ex) {
            Log.e(TAG, "Error reading schema.sql", ex);
            throw new RuntimeException("Failed to create database from schema.sql", ex);
        }
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        // 数据库升级逻辑
    }

    // 教师相关操作
    public Cursor getTeacherByUsername(String username) {
        SQLiteDatabase db = this.getReadableDatabase();
        return db.query("Teacher", null, "username = ?", new String[]{username}, null, null, null);
    }

    // 班级相关操作
    public long insertClassroom(long teacherId, String name, int year, String meta) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put("teacherId", teacherId);
        values.put("name", name);
        values.put("year", year);
        values.put("meta", meta);
        return db.insert("Classroom", null, values);
    }

    public Cursor getClassroomsByTeacher(long teacherId) {
        SQLiteDatabase db = this.getReadableDatabase();
        return db.query("Classroom", null, "teacherId = ?", 
            new String[]{String.valueOf(teacherId)}, null, null, "year DESC, name");
    }

    // 学生相关操作
    public long insertStudent(long classId, String name, String sid, String gender, String avatarUri) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put("classId", classId);
        values.put("name", name);
        values.put("sid", sid);
        values.put("gender", gender);
        values.put("avatarUri", avatarUri);
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

    // 人脸特征相关操作
    public long insertFaceEmbedding(long studentId, String modelVer, byte[] vector, float quality) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put("studentId", studentId);
        values.put("modelVer", modelVer);
        values.put("vector", vector);
        values.put("quality", quality);
        return db.insert("FaceEmbedding", null, values);
    }

    public Cursor getFaceEmbeddingsByStudent(long studentId) {
        SQLiteDatabase db = this.getReadableDatabase();
        return db.query("FaceEmbedding", null, "studentId = ?", 
            new String[]{String.valueOf(studentId)}, null, null, "quality DESC");
    }

    // 考勤会话相关操作
    public long insertAttendanceSession(long classId, String location, String photoUri, String note) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put("classId", classId);
        values.put("location", location);
        values.put("photoUri", photoUri);
        values.put("note", note);
        return db.insert("AttendanceSession", null, values);
    }

    // 考勤结果相关操作
    public long insertAttendanceResult(long sessionId, long studentId, String status, 
                                     float score, String decidedBy) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put("sessionId", sessionId);
        values.put("studentId", studentId);
        values.put("status", status);
        values.put("score", score);
        values.put("decidedBy", decidedBy);
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

    // 照片资源相关操作
    public long insertPhotoAsset(long sessionId, String type, String uri, String meta) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put("sessionId", sessionId);
        values.put("type", type);
        values.put("uri", uri);
        values.put("meta", meta);
        return db.insert("PhotoAsset", null, values);
    }

    // 同步日志相关操作
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
}