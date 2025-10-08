package com.example.facecheck.utils;

import android.content.Context;
import android.net.Uri;
import android.util.Log;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

/**
 * 照片存储管理器
 * 将照片存储到挨着数据库的位置，便于同步和备份
 */
public class PhotoStorageManager {
    private static final String TAG = "PhotoStorageManager";
    
    // 照片存储目录名称
    private static final String PHOTOS_DIR = "photos";
    private static final String AVATARS_DIR = "avatars";
    private static final String ATTENDANCE_DIR = "attendance";
    private static final String PROCESSED_DIR = "processed";
    
    private final Context context;
    private final File baseDir;
    
    public PhotoStorageManager(Context context) {
        this.context = context;
        // 获取数据库所在目录作为基础目录
        File dbFile = context.getDatabasePath("facecheck.db");
        this.baseDir = dbFile.getParentFile();
        
        // 确保基础目录存在
        if (!baseDir.exists()) {
            baseDir.mkdirs();
        }
        
        // 创建子目录
        createDirectories();
        
        Log.d(TAG, "照片存储管理器初始化完成，基础目录: " + baseDir.getAbsolutePath());
    }
    
    /**
     * 创建所有必要的子目录
     */
    private void createDirectories() {
        File photosDir = new File(baseDir, PHOTOS_DIR);
        File avatarsDir = new File(baseDir, AVATARS_DIR);
        File attendanceDir = new File(baseDir, ATTENDANCE_DIR);
        File processedDir = new File(baseDir, PROCESSED_DIR);
        
        photosDir.mkdirs();
        avatarsDir.mkdirs();
        attendanceDir.mkdirs();
        processedDir.mkdirs();
        
        Log.d(TAG, "照片存储目录结构创建完成");
    }
    
    /**
     * 获取头像存储目录
     */
    public File getAvatarDir() {
        return new File(baseDir, AVATARS_DIR);
    }

    /**
     * 获取分割后人脸根目录（实例方法）
     */
    public File getSegmentedFacesRootDir() {
        File dir = new File(baseDir, "segmented_faces");
        if (!dir.exists()) {
            dir.mkdirs();
        }
        return dir;
    }

    /**
     * 获取分割后人脸根目录（静态方法）
     */
    public static File getSegmentedFacesRootDir(Context context) {
        File dbFile = context.getDatabasePath("facecheck.db");
        File baseDir = dbFile.getParentFile();
        File dir = new File(baseDir, "segmented_faces");
        if (!dir.exists()) {
            dir.mkdirs();
        }
        return dir;
    }

    /**
     * 获取处理后的人脸根目录（实例方法）
     */
    public File getProcessedFacesRootDir() {
        File dir = new File(baseDir, "processed_faces");
        if (!dir.exists()) {
            dir.mkdirs();
        }
        return dir;
    }

    /**
     * 获取处理后的人脸根目录（静态方法）
     */
    public static File getProcessedFacesRootDir(Context context) {
        File dbFile = context.getDatabasePath("facecheck.db");
        File baseDir = dbFile.getParentFile();
        File dir = new File(baseDir, "processed_faces");
        if (!dir.exists()) {
            dir.mkdirs();
        }
        return dir;
    }
    
    /**
     * 创建头像照片文件
     */
    public static File createAvatarPhotoFile(Context context) throws IOException {
        String fileName = "avatar_" + System.currentTimeMillis() + ".jpg";
        File dir = getAvatarPhotosDir(context);
        File file = new File(dir, fileName);
        
        if (!file.createNewFile()) {
            throw new IOException("无法创建头像照片文件");
        }
        
        return file;
    }
    
    /**
     * 获取分割后人脸目录（带会话ID）
     */
    public static File getSegmentedFacesDir(Context context, String sessionId) {
        File rootDir = getSegmentedFacesRootDir(context);
        File sessionDir = new File(rootDir, sessionId);
        if (!sessionDir.exists()) {
            sessionDir.mkdirs();
        }
        return sessionDir;
    }
    
    /**
     * 获取处理后的人脸目录（带会话ID）
     */
    public static File getProcessedFacesDir(Context context, String sessionId) {
        File rootDir = getProcessedFacesRootDir(context);
        File sessionDir = new File(rootDir, sessionId);
        if (!sessionDir.exists()) {
            sessionDir.mkdirs();
        }
        return sessionDir;
    }
    
    /**
     * 获取头像照片存储目录（静态方法）
     */
    public static File getAvatarPhotosDir(Context context) {
        File dbFile = context.getDatabasePath("facecheck.db");
        File baseDir = dbFile.getParentFile();
        File dir = new File(baseDir, AVATARS_DIR);
        if (!dir.exists()) {
            dir.mkdirs();
        }
        return dir;
    }
    
    /**
     * 获取考勤照片存储目录（静态方法）
     */
    public static File getAttendancePhotosDir(Context context) {
        File dbFile = context.getDatabasePath("facecheck.db");
        File baseDir = dbFile.getParentFile();
        File dir = new File(baseDir, ATTENDANCE_DIR);
        if (!dir.exists()) {
            dir.mkdirs();
        }
        return dir;
    }
    
    /**
     * 获取考勤照片存储目录
     */
    public File getAttendanceDir() {
        return new File(baseDir, ATTENDANCE_DIR);
    }
    
    /**
     * 获取处理后的照片存储目录
     */
    public File getProcessedDir() {
        return new File(baseDir, PROCESSED_DIR);
    }
    
    /**
     * 获取基础照片目录
     */
    public File getPhotosDir() {
        return new File(baseDir, PHOTOS_DIR);
    }
    
    /**
     * 保存头像文件
     * @param sourceUri 源文件URI
     * @param studentId 学生ID
     * @return 保存后的文件路径
     */
    public String saveAvatar(Uri sourceUri, long studentId) {
        try {
            File avatarDir = getAvatarDir();
            String fileName = "avatar_" + studentId + ".jpg";
            File targetFile = new File(avatarDir, fileName);
            
            // 复制文件
            if (copyFile(sourceUri, targetFile)) {
                Log.d(TAG, "头像保存成功: " + targetFile.getAbsolutePath());
                return targetFile.getAbsolutePath();
            } else {
                Log.e(TAG, "头像保存失败");
                return null;
            }
        } catch (Exception e) {
            Log.e(TAG, "保存头像时发生错误", e);
            return null;
        }
    }
    
    /**
     * 保存考勤照片
     * @param sourceUri 源文件URI
     * @param sessionId 考勤会话ID
     * @param studentId 学生ID（可选，可以为0）
     * @return 保存后的文件路径
     */
    public String saveAttendancePhoto(Uri sourceUri, long sessionId, long studentId) {
        try {
            File attendanceDir = getAttendanceDir();
            String fileName = "attendance_" + sessionId + "_" + studentId + "_" + 
                             System.currentTimeMillis() + ".jpg";
            File targetFile = new File(attendanceDir, fileName);
            
            // 复制文件
            if (copyFile(sourceUri, targetFile)) {
                Log.d(TAG, "考勤照片保存成功: " + targetFile.getAbsolutePath());
                return targetFile.getAbsolutePath();
            } else {
                Log.e(TAG, "考勤照片保存失败");
                return null;
            }
        } catch (Exception e) {
            Log.e(TAG, "保存考勤照片时发生错误", e);
            return null;
        }
    }
    
    /**
     * 保存处理后的照片（人脸对齐、特征提取等）
     * @param sourceFile 源文件
     * @param sessionId 会话ID
     * @param type 照片类型（aligned, detected, feature等）
     * @return 保存后的文件路径
     */
    public String saveProcessedPhoto(File sourceFile, long sessionId, String type) {
        try {
            File processedDir = getProcessedDir();
            String subDir = "session_" + sessionId;
            File sessionDir = new File(processedDir, subDir);
            sessionDir.mkdirs();
            
            String fileName = type + "_" + System.currentTimeMillis() + ".jpg";
            File targetFile = new File(sessionDir, fileName);
            
            // 复制文件
            if (copyFile(sourceFile, targetFile)) {
                Log.d(TAG, "处理照片保存成功: " + targetFile.getAbsolutePath());
                return targetFile.getAbsolutePath();
            } else {
                Log.e(TAG, "处理照片保存失败");
                return null;
            }
        } catch (Exception e) {
            Log.e(TAG, "保存处理照片时发生错误", e);
            return null;
        }
    }
    
    /**
     * 复制文件从URI到目标文件
     */
    private boolean copyFile(Uri sourceUri, File targetFile) {
        InputStream inputStream = null;
        OutputStream outputStream = null;
        
        try {
            inputStream = context.getContentResolver().openInputStream(sourceUri);
            outputStream = new FileOutputStream(targetFile);
            
            byte[] buffer = new byte[4096];
            int length;
            while ((length = inputStream.read(buffer)) > 0) {
                outputStream.write(buffer, 0, length);
            }
            
            return true;
        } catch (IOException e) {
            Log.e(TAG, "文件复制失败", e);
            return false;
        } finally {
            try {
                if (inputStream != null) inputStream.close();
                if (outputStream != null) outputStream.close();
            } catch (IOException e) {
                Log.e(TAG, "关闭流失败", e);
            }
        }
    }
    
    /**
     * 复制文件（File到File）
     */
    private boolean copyFile(File sourceFile, File targetFile) {
        FileInputStream inputStream = null;
        FileOutputStream outputStream = null;
        
        try {
            inputStream = new FileInputStream(sourceFile);
            outputStream = new FileOutputStream(targetFile);
            
            byte[] buffer = new byte[4096];
            int length;
            while ((length = inputStream.read(buffer)) > 0) {
                outputStream.write(buffer, 0, length);
            }
            
            return true;
        } catch (IOException e) {
            Log.e(TAG, "文件复制失败", e);
            return false;
        } finally {
            try {
                if (inputStream != null) inputStream.close();
                if (outputStream != null) outputStream.close();
            } catch (IOException e) {
                Log.e(TAG, "关闭流失败", e);
            }
        }
    }
    
    /**
     * 删除头像文件
     */
    public boolean deleteAvatar(long studentId) {
        File avatarDir = getAvatarDir();
        String fileName = "avatar_" + studentId + ".jpg";
        File avatarFile = new File(avatarDir, fileName);
        
        if (avatarFile.exists()) {
            boolean deleted = avatarFile.delete();
            Log.d(TAG, "头像文件删除" + (deleted ? "成功" : "失败") + ": " + avatarFile.getAbsolutePath());
            return deleted;
        }
        
        return true;
    }
    
    /**
     * 删除学生的所有相关照片
     */
    public boolean deleteStudentPhotos(long studentId) {
        boolean success = true;
        
        // 删除头像
        success &= deleteAvatar(studentId);
        
        // 删除考勤照片（需要遍历查找）
        File attendanceDir = getAttendanceDir();
        File[] attendanceFiles = attendanceDir.listFiles((dir, name) -> 
            name.contains("_" + studentId + "_"));
        
        if (attendanceFiles != null) {
            for (File file : attendanceFiles) {
                success &= file.delete();
                Log.d(TAG, "删除考勤照片: " + file.getAbsolutePath());
            }
        }
        
        return success;
    }
    
    /**
     * 获取存储基础路径（用于调试和同步）
     */
    public String getStorageBasePath() {
        return baseDir.getAbsolutePath();
    }
    
    /**
     * 获取所有照片文件的总大小（字节）
     */
    public long getTotalStorageSize() {
        long totalSize = 0;
        totalSize += getDirSize(getPhotosDir());
        totalSize += getDirSize(getAvatarDir());
        totalSize += getDirSize(getAttendanceDir());
        totalSize += getDirSize(getProcessedDir());
        return totalSize;
    }
    
    /**
     * 获取目录大小
     */
    private long getDirSize(File dir) {
        if (!dir.exists()) return 0;
        
        long size = 0;
        File[] files = dir.listFiles();
        if (files != null) {
            for (File file : files) {
                if (file.isFile()) {
                    size += file.length();
                } else if (file.isDirectory()) {
                    size += getDirSize(file);
                }
            }
        }
        return size;
    }
}