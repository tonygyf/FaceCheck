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
    
    public Teacher login(String username, String password) {
        SQLiteDatabase db = databaseHelper.getReadableDatabase();
        Cursor cursor = db.query(
            "Teacher",
            new String[]{"id", "name", "username", "password"},
            "username = ?",
            new String[]{username},
            null, null, null
        );
        
        Teacher teacher = null;
        if (cursor != null && cursor.moveToFirst()) {
            long teacherId = cursor.getLong(cursor.getColumnIndexOrThrow("id"));
            String storedPassword = cursor.getString(cursor.getColumnIndexOrThrow("password"));
            String teacherName = cursor.getString(cursor.getColumnIndexOrThrow("name"));
            String teacherUsername = cursor.getString(cursor.getColumnIndexOrThrow("username"));
            
            if (password.equals(storedPassword)) {
                teacher = new Teacher(teacherId, teacherName, teacherUsername, storedPassword, 
                                   "", "", "", "", System.currentTimeMillis(), System.currentTimeMillis());
            }
            cursor.close();
        }
        
        return teacher;
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
        Teacher teacher = new Teacher(0, name, username, password, 
                               "", "", "", "", System.currentTimeMillis(), System.currentTimeMillis());
        return databaseHelper.addTeacher(teacher);
    }
    
    public Teacher getTeacherById(long teacherId) {
        return databaseHelper.getTeacherById(teacherId);
    }
    
    public boolean updateTeacher(Teacher teacher) {
        return databaseHelper.updateTeacher(teacher);
    }
}