package com.example.facecheck.data.repository;

import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;

import com.example.facecheck.data.model.Teacher;
import com.example.facecheck.database.DatabaseHelper;

public class UserRepository {
    
    private final DatabaseHelper databaseHelper;
    
    public UserRepository(Context context) {
        this.databaseHelper = new DatabaseHelper(context);
    }
    
    public static class UserLoginResult {
        public final long userId;
        public final String role; // "teacher" 或 "student"
        public final String displayName;
        public UserLoginResult(long userId, String role, String displayName) {
            this.userId = userId;
            this.role = role;
            this.displayName = displayName;
        }
    }

    public UserLoginResult loginAny(String username, String password) {
        SQLiteDatabase db = databaseHelper.getReadableDatabase();
        // 先尝试教师
        Cursor cursor = db.query(
                "Teacher",
                new String[]{"id", "name", "username", "password"},
                "username = ?",
                new String[]{username},
                null, null, null
        );
        if (cursor != null && cursor.moveToFirst()) {
            long teacherId = cursor.getLong(cursor.getColumnIndexOrThrow("id"));
            String storedPassword = cursor.getString(cursor.getColumnIndexOrThrow("password"));
            String teacherName = cursor.getString(cursor.getColumnIndexOrThrow("name"));
            cursor.close();
            if (password.equals(storedPassword)) {
                return new UserLoginResult(teacherId, "teacher", teacherName);
            }
        } else if (cursor != null) {
            cursor.close();
        }

        // 再尝试学生：使用学号 sid 登录
        Cursor sc = db.query(
                "Student",
                new String[]{"id", "name", "sid", "password"},
                "sid = ?",
                new String[]{username},
                null, null, null
        );
        if (sc != null && sc.moveToFirst()) {
            long studentId = sc.getLong(sc.getColumnIndexOrThrow("id"));
            String storedPassword = sc.getString(sc.getColumnIndexOrThrow("password"));
            String studentName = sc.getString(sc.getColumnIndexOrThrow("name"));
            sc.close();
            if (password.equals(storedPassword)) {
                return new UserLoginResult(studentId, "student", studentName);
            }
        } else if (sc != null) {
            sc.close();
        }

        return null;
    }
    
    public boolean registerTeacher(String name, String username, String password) {
        SQLiteDatabase db = databaseHelper.getWritableDatabase();
        
        // 检查用户名是否已存在
        Cursor cursor = db.query(
            "Teacher",
            new String[]{"id"},
            "username = ?",
            new String[]{username},
            null, null, null
        );
        
        boolean exists = cursor != null && cursor.moveToFirst();
        if (cursor != null) {
            cursor.close();
        }
        
        if (exists) {
            return false; // 用户名已存在
        }
        
        // 插入新教师
        Teacher teacher = new Teacher(0, name, username, password, "",
                               System.currentTimeMillis(), System.currentTimeMillis());
        return databaseHelper.addTeacher(teacher);
    }
    
    public Teacher getTeacherById(long teacherId) {
        return databaseHelper.getTeacherById(teacherId);
    }
    
    public boolean updateTeacher(Teacher teacher) {
        return databaseHelper.updateTeacher(teacher);
    }
}
