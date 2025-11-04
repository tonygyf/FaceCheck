package com.example.facecheck.sync;

import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.util.Log;

import com.example.facecheck.database.DatabaseHelper;
import com.example.facecheck.webdav.WebDavManager;

import java.io.File;
import java.util.List;

public class SyncManager {
    private static final String TAG = "SyncManager";
    
    private final Context context;
    private final DatabaseHelper dbHelper;
    private final WebDavManager webDavManager;
    private final String localDbPath;

    public SyncManager(Context context, DatabaseHelper dbHelper, WebDavManager webDavManager) {
        this.context = context;
        this.dbHelper = dbHelper;
        this.webDavManager = webDavManager;
        this.localDbPath = context.getDatabasePath("facecheck.db").getAbsolutePath();
    }

    public enum SyncMode { UPLOAD, DOWNLOAD }

    /**
     * 简洁同步：仅初始化目录并选择上传或下载数据库；覆盖式，无备份。
     */
    public boolean performSimpleSync(SyncMode mode) {
        // 首次连接时初始化目录结构
        if (!webDavManager.initializeDirectoryStructure()) {
            Log.e(TAG, "Failed to initialize WebDAV directory structure");
            return false;
        }
        if (mode == SyncMode.UPLOAD) {
            return webDavManager.syncDatabase(localDbPath);
        } else {
            return webDavManager.fetchDatabase(localDbPath);
        }
    }

    // 执行同步操作
    public boolean performSync() {
        // 1. 初始化WebDAV目录结构
        if (!webDavManager.initializeDirectoryStructure()) {
            Log.e(TAG, "Failed to initialize WebDAV directory structure");
            return false;
        }

        // 2. 处理待同步的日志
        Cursor pendingLogs = dbHelper.getPendingSyncLogs();
        if (pendingLogs != null && pendingLogs.moveToFirst()) {
            do {
                long id = pendingLogs.getLong(pendingLogs.getColumnIndexOrThrow("id"));
                String entity = pendingLogs.getString(pendingLogs.getColumnIndexOrThrow("entity"));
                long entityId = pendingLogs.getLong(pendingLogs.getColumnIndexOrThrow("entityId"));
                String op = pendingLogs.getString(pendingLogs.getColumnIndexOrThrow("op"));
                
                // 根据实体类型和操作类型执行同步
                boolean success = syncEntity(entity, entityId, op);
                
                // 更新同步状态
                if (success) {
                    dbHelper.updateSyncLogStatus(id, "COMPLETED");
                } else {
                    dbHelper.updateSyncLogStatus(id, "FAILED");
                }
            } while (pendingLogs.moveToNext());
            pendingLogs.close();
        }

        // 3. 同步数据库文件
        return webDavManager.syncDatabase(localDbPath);
    }

    // 同步单个实体
    private boolean syncEntity(String entity, long entityId, String op) {
        switch (entity) {
            case "Student":
                return syncStudent(entityId, op);
            case "PhotoAsset":
                return syncPhotoAsset(entityId, op);
            case "AttendanceSession":
                return syncAttendanceSession(entityId, op);
            default:
                Log.w(TAG, "Unknown entity type: " + entity);
                return false;
        }
    }

    // 同步学生信息（包括头像）
    private boolean syncStudent(long studentId, String op) {
        SQLiteDatabase db = dbHelper.getReadableDatabase();
        Cursor cursor = db.query("Student", null, "id = ?", 
            new String[]{String.valueOf(studentId)}, null, null, null);
        
        if (cursor != null && cursor.moveToFirst()) {
            String avatarUri = cursor.getString(cursor.getColumnIndexOrThrow("avatarUri"));
            cursor.close();
            
            if (avatarUri != null && !avatarUri.isEmpty()) {
                File avatarFile = new File(avatarUri);
                if (avatarFile.exists()) {
                    // 上传头像文件
                    return webDavManager.syncAvatar(avatarUri, avatarFile.getName());
                }
            }
        }
        return true;
    }

    // 同步照片资源
    private boolean syncPhotoAsset(long photoAssetId, String op) {
        SQLiteDatabase db = dbHelper.getReadableDatabase();
        Cursor cursor = db.query("PhotoAsset", null, "id = ?", 
            new String[]{String.valueOf(photoAssetId)}, null, null, null);
        
        if (cursor != null && cursor.moveToFirst()) {
            String type = cursor.getString(cursor.getColumnIndexOrThrow("type"));
            String uri = cursor.getString(cursor.getColumnIndexOrThrow("uri"));
            cursor.close();
            
            if (uri != null && !uri.isEmpty()) {
                File photoFile = new File(uri);
                if (photoFile.exists()) {
                    // 根据照片类型选择同步方法
                    switch (type) {
                        case "RAW":
                        case "ALIGNED":
                            return webDavManager.syncAttendancePhoto(uri, photoFile.getName());
                        case "DEBUG":
                            return webDavManager.syncResultPhoto(uri, photoFile.getName());
                        default:
                            Log.w(TAG, "Unknown photo type: " + type);
                            return false;
                    }
                }
            }
        }
        return true;
    }

    // 同步考勤会话
    private boolean syncAttendanceSession(long sessionId, String op) {
        SQLiteDatabase db = dbHelper.getReadableDatabase();
        Cursor cursor = db.query("AttendanceSession", null, "id = ?", 
            new String[]{String.valueOf(sessionId)}, null, null, null);
        
        if (cursor != null && cursor.moveToFirst()) {
            String photoUri = cursor.getString(cursor.getColumnIndexOrThrow("photoUri"));
            cursor.close();
            
            if (photoUri != null && !photoUri.isEmpty()) {
                File photoFile = new File(photoUri);
                if (photoFile.exists()) {
                    // 上传考勤照片
                    return webDavManager.syncAttendancePhoto(photoUri, photoFile.getName());
                }
            }
        }
        return true;
    }

    // 从远程获取数据
    public boolean fetchRemoteData() {
        // 1. 下载远程数据库文件
        if (!webDavManager.fetchDatabase(localDbPath)) {
            Log.e(TAG, "Failed to fetch remote database");
            return false;
        }

        // 2. 同步头像文件
        SQLiteDatabase db = dbHelper.getReadableDatabase();
        Cursor studentCursor = db.query("Student", new String[]{"avatarUri"}, 
            "avatarUri IS NOT NULL", null, null, null, null);
        
        if (studentCursor != null) {
            while (studentCursor.moveToNext()) {
                String avatarUri = studentCursor.getString(0);
                if (avatarUri != null && !avatarUri.isEmpty()) {
                    File avatarFile = new File(avatarUri);
                    webDavManager.fetchAvatar(avatarFile.getName(), avatarUri);
                }
            }
            studentCursor.close();
        }

        // 3. 同步考勤照片
        Cursor photoCursor = db.query("PhotoAsset", new String[]{"type", "uri"}, 
            "uri IS NOT NULL", null, null, null, null);
        
        if (photoCursor != null) {
            while (photoCursor.moveToNext()) {
                String type = photoCursor.getString(0);
                String uri = photoCursor.getString(1);
                
                if (uri != null && !uri.isEmpty()) {
                    File photoFile = new File(uri);
                    switch (type) {
                        case "RAW":
                        case "ALIGNED":
                            webDavManager.fetchAttendancePhoto(photoFile.getName(), uri);
                            break;
                        case "DEBUG":
                            webDavManager.fetchResultPhoto(photoFile.getName(), uri);
                            break;
                    }
                }
            }
            photoCursor.close();
        }

        return true;
    }
}