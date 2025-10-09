package com.example.facecheck.utils;

import android.content.Context;
import android.graphics.Bitmap;
import android.net.Uri;
import android.provider.MediaStore;
import android.util.Log;

import com.example.facecheck.utils.PhotoStorageManager;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

/**
 * 图片存储管理器
 * 负责管理原照片和分割后的人脸照片的存储、清理和缓存管理
 */
public class ImageStorageManager {
    
    private static final String TAG = "ImageStorageManager";
    private final Context context;
    
    // 存储目录名称
    private static final String DIR_ORIGINAL_PHOTOS = "original_photos";
    private static final String DIR_SEGMENTED_FACES = "segmented_faces";
    private static final String DIR_TEMP_CACHE = "temp_cache";
    private static final String DIR_PROCESSED_FACES = "processed_faces";
    
    public ImageStorageManager(Context context) {
        this.context = context;
    }
    
    /**
     * 从文件路径加载位图
     * @param filePath 文件路径
     * @return 位图对象，如果加载失败则返回null
     */
    public Bitmap loadBitmapFromFile(String filePath) {
        try {
            File file = new File(filePath);
            if (!file.exists()) {
                Log.e(TAG, "文件不存在: " + filePath);
                return null;
            }
            return MediaStore.Images.Media.getBitmap(context.getContentResolver(), Uri.fromFile(file));
        } catch (IOException e) {
            Log.e(TAG, "加载图片失败: " + e.getMessage());
            return null;
        }
    }
    
    /**
     * 获取原始照片存储目录（使用内部存储）
     */
    public File getOriginalPhotosDir() {
        return PhotoStorageManager.getAttendancePhotosDir(context);
    }
    
    /**
     * 获取分割后人脸存储目录（使用内部存储）
     */
    public File getSegmentedFacesDir(String sessionId) {
        return PhotoStorageManager.getSegmentedFacesDir(context, sessionId);
    }
    
    /**
     * 获取处理后的人脸存储目录（使用内部存储）
     */
    public File getProcessedFacesDir(String sessionId) {
        return PhotoStorageManager.getProcessedFacesDir(context, sessionId);
    }
    
    /**
     * 获取临时缓存目录
     */
    public File getTempCacheDir() {
        File dir = new File(context.getExternalCacheDir(), DIR_TEMP_CACHE);
        if (!dir.exists()) {
            dir.mkdirs();
        }
        return dir;
    }
    
    /**
     * 保存原始照片
     */
    public String saveOriginalPhoto(Bitmap bitmap, String sessionId) {
        try {
            File dir = getOriginalPhotosDir();
            String fileName = generateFileName("original", sessionId, "jpg");
            File file = new File(dir, fileName);
            
            FileOutputStream fos = new FileOutputStream(file);
            bitmap.compress(Bitmap.CompressFormat.JPEG, 90, fos);
            fos.close();
            
            return file.getAbsolutePath();
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }
    }
    
    /**
     * 保存分割后的人脸
     */
    public String saveSegmentedFace(Bitmap faceBitmap, String sessionId, int faceIndex) {
        try {
            File dir = getSegmentedFacesDir(sessionId);
            String fileName = generateFileName("face" + faceIndex, sessionId, "jpg");
            File file = new File(dir, fileName);
            
            FileOutputStream fos = new FileOutputStream(file);
            faceBitmap.compress(Bitmap.CompressFormat.JPEG, 90, fos);
            fos.close();
            
            return file.getAbsolutePath();
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }
    }
    
    /**
     * 保存处理后的人脸
     */
    public String saveProcessedFace(Bitmap processedFace, String sessionId, int faceIndex) {
        try {
            File dir = getProcessedFacesDir(sessionId);
            String fileName = generateFileName("processed_face" + faceIndex, sessionId, "jpg");
            File file = new File(dir, fileName);
            
            FileOutputStream fos = new FileOutputStream(file);
            processedFace.compress(Bitmap.CompressFormat.JPEG, 90, fos);
            fos.close();
            
            return file.getAbsolutePath();
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }
    }
    
    /**
     * 保存临时图片
     */
    public String saveTempImage(Bitmap bitmap, String prefix) {
        try {
            File dir = getTempCacheDir();
            String fileName = prefix + "_" + System.currentTimeMillis() + ".jpg";
            File file = new File(dir, fileName);
            
            FileOutputStream fos = new FileOutputStream(file);
            bitmap.compress(Bitmap.CompressFormat.JPEG, 80, fos);
            fos.close();
            
            return file.getAbsolutePath();
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }
    }
    
    /**
     * 获取会话的所有相关图片
     */
    public List<String> getSessionImages(String sessionId) {
        List<String> imagePaths = new ArrayList<>();
        
        // 获取原始照片
        File originalDir = getOriginalPhotosDir();
        File[] originalFiles = originalDir.listFiles((dir, name) -> name.contains(sessionId));
        if (originalFiles != null) {
            for (File file : originalFiles) {
                imagePaths.add(file.getAbsolutePath());
            }
        }
        
        // 获取分割后的人脸
        File segmentedDir = getSegmentedFacesDir(sessionId);
        File[] segmentedFiles = segmentedDir.listFiles();
        if (segmentedFiles != null) {
            for (File file : segmentedFiles) {
                imagePaths.add(file.getAbsolutePath());
            }
        }
        
        // 获取处理后的人脸
        File processedDir = getProcessedFacesDir(sessionId);
        File[] processedFiles = processedDir.listFiles();
        if (processedFiles != null) {
            for (File file : processedFiles) {
                imagePaths.add(file.getAbsolutePath());
            }
        }
        
        return imagePaths;
    }
    
    /**
     * 删除会话的所有图片
     */
    public boolean deleteSessionImages(String sessionId) {
        boolean success = true;
        
        // 删除分割后的人脸目录
        File segmentedDir = getSegmentedFacesDir(sessionId);
        if (segmentedDir.exists()) {
            success &= deleteDirectory(segmentedDir);
        }
        
        // 删除处理后的人脸目录
        File processedDir = getProcessedFacesDir(sessionId);
        if (processedDir.exists()) {
            success &= deleteDirectory(processedDir);
        }
        
        // 删除原始照片中与该会话相关的文件
        File originalDir = getOriginalPhotosDir();
        File[] originalFiles = originalDir.listFiles((dir, name) -> name.contains(sessionId));
        if (originalFiles != null) {
            for (File file : originalFiles) {
                success &= file.delete();
            }
        }
        
        return success;
    }
    
    /**
     * 清理临时缓存
     */
    public long clearTempCache() {
        File tempDir = getTempCacheDir();
        return deleteDirectoryAndGetSize(tempDir);
    }
    
    /**
     * 清理所有缓存
     */
    public long clearAllCache() {
        long totalSize = 0;
        
        // 清理临时缓存
        totalSize += clearTempCache();
        
        // 清理原始照片（保留最近7天的）
        totalSize += clearOldOriginalPhotos(7);
        
        // 清理分割后人脸（保留最近3天的）
        totalSize += clearOldSegmentedFaces(3);
        
        return totalSize;
    }
    
    /**
     * 清理旧的原始照片
     */
    private long clearOldOriginalPhotos(int daysToKeep) {
        File originalDir = getOriginalPhotosDir();
        long cutoffTime = System.currentTimeMillis() - (daysToKeep * 24 * 60 * 60 * 1000L);
        
        long totalSize = 0;
        File[] files = originalDir.listFiles();
        if (files != null) {
            for (File file : files) {
                if (file.lastModified() < cutoffTime) {
                    totalSize += file.length();
                    file.delete();
                }
            }
        }
        
        return totalSize;
    }
    
    /**
     * 清理旧的分割后人脸
     */
    private long clearOldSegmentedFaces(int daysToKeep) {
        long totalSize = 0;
        File segmentedRootDir = PhotoStorageManager.getSegmentedFacesRootDir(context);
        
        if (segmentedRootDir.exists()) {
            File[] sessionDirs = segmentedRootDir.listFiles();
            if (sessionDirs != null) {
                long cutoffTime = System.currentTimeMillis() - (daysToKeep * 24 * 60 * 60 * 1000L);
                
                for (File sessionDir : sessionDirs) {
                    if (sessionDir.lastModified() < cutoffTime) {
                        totalSize += deleteDirectoryAndGetSize(sessionDir);
                    }
                }
            }
        }
        
        return totalSize;
    }
    
    /**
     * 获取缓存大小
     */
    public long getCacheSize() {
        long totalSize = 0;
        
        // 临时缓存大小
        totalSize += getDirectorySize(getTempCacheDir());
        
        // 原始照片大小
        totalSize += getDirectorySize(getOriginalPhotosDir());
        
        // 分割后人脸大小
        File segmentedRootDir = PhotoStorageManager.getSegmentedFacesRootDir(context);
        totalSize += getDirectorySize(segmentedRootDir);
        
        // 处理后人脸大小
        File processedRootDir = PhotoStorageManager.getProcessedFacesRootDir(context);
        totalSize += getDirectorySize(processedRootDir);
        
        return totalSize;
    }
    
    /**
     * 生成文件名
     */
    private String generateFileName(String prefix, String sessionId, String extension) {
        SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault());
        String timestamp = sdf.format(new Date());
        return prefix + "_" + sessionId + "_" + timestamp + "." + extension;
    }
    
    /**
     * 删除目录
     */
    private boolean deleteDirectory(File dir) {
        if (dir.exists()) {
            File[] files = dir.listFiles();
            if (files != null) {
                for (File file : files) {
                    if (file.isDirectory()) {
                        deleteDirectory(file);
                    } else {
                        file.delete();
                    }
                }
            }
            return dir.delete();
        }
        return true;
    }
    
    /**
     * 删除目录并返回大小
     */
    private long deleteDirectoryAndGetSize(File dir) {
        long size = getDirectorySize(dir);
        deleteDirectory(dir);
        return size;
    }
    
    /**
     * 获取目录大小
     */
    private long getDirectorySize(File dir) {
        long size = 0;
        if (dir.exists()) {
            File[] files = dir.listFiles();
            if (files != null) {
                for (File file : files) {
                    if (file.isFile()) {
                        size += file.length();
                    } else if (file.isDirectory()) {
                        size += getDirectorySize(file);
                    }
                }
            }
        }
        return size;
    }
}