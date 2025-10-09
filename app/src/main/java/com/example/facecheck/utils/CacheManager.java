package com.example.facecheck.utils;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;

import java.io.File;
import java.text.DecimalFormat;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * 缓存管理器
 * 负责清理应用缓存，包括图片缓存、临时文件等
 */
public class CacheManager {
    
    private static final String TAG = "CacheManager";
    private final Context context;
    private final ImageStorageManager imageStorageManager;
    private final ExecutorService executor;
    private final Handler mainHandler;
    
    public interface CacheCleanCallback {
        void onProgress(int progress, String message);
        void onComplete(long freedSpace, String summary);
        void onError(String error);
    }
    
    public CacheManager(Context context) {
        this.context = context;
        this.imageStorageManager = new ImageStorageManager(context);
        this.executor = Executors.newSingleThreadExecutor();
        this.mainHandler = new Handler(Looper.getMainLooper());
    }
    
    /**
     * 清理所有缓存
     */
    public void clearAllCache(CacheCleanCallback callback) {
        executor.execute(() -> {
            try {
                mainHandler.post(() -> callback.onProgress(0, "开始清理缓存..."));
                
                long totalFreedSpace = 0;
                
                // 1. 清理图片缓存
                mainHandler.post(() -> callback.onProgress(10, "清理图片缓存..."));
                totalFreedSpace += imageStorageManager.clearAllCache();
                
                // 2. 清理应用缓存目录
                mainHandler.post(() -> callback.onProgress(30, "清理应用缓存..."));
                totalFreedSpace += clearAppCache();
                
                // 3. 清理临时文件
                mainHandler.post(() -> callback.onProgress(50, "清理临时文件..."));
                totalFreedSpace += clearTempFiles();
                
                // 4. 清理日志文件
                mainHandler.post(() -> callback.onProgress(70, "清理日志文件..."));
                totalFreedSpace += clearLogFiles();
                
                // 5. 清理空目录
                mainHandler.post(() -> callback.onProgress(90, "清理空目录..."));
                cleanEmptyDirectories();
                
                mainHandler.post(() -> callback.onProgress(100, "清理完成"));
                
                final long finalTotalFreedSpace = totalFreedSpace;
                String summary = String.format("清理完成！共释放 %s 空间", 
                    formatFileSize(totalFreedSpace));
                
                final long finalFreedSpace = totalFreedSpace;
                mainHandler.post(() -> callback.onComplete(finalFreedSpace, summary));
                
            } catch (Exception e) {
                mainHandler.post(() -> callback.onError("清理失败: " + e.getMessage()));
            }
        });
    }
    
    /**
     * 清理图片相关缓存
     */
    public void clearImageCache(CacheCleanCallback callback) {
        executor.execute(() -> {
            try {
                mainHandler.post(() -> callback.onProgress(0, "开始清理图片缓存..."));
                
                long freedSpace = imageStorageManager.clearAllCache();
                
                String summary = String.format("图片缓存清理完成！共释放 %s 空间", 
                    formatFileSize(freedSpace));
                
                mainHandler.post(() -> callback.onComplete(freedSpace, summary));
                
            } catch (Exception e) {
                mainHandler.post(() -> callback.onError("清理失败: " + e.getMessage()));
            }
        });
    }
    
    /**
     * 清理临时缓存
     */
    public void clearTempCache(CacheCleanCallback callback) {
        executor.execute(() -> {
            try {
                mainHandler.post(() -> callback.onProgress(0, "开始清理临时缓存..."));
                
                long freedSpace = imageStorageManager.clearTempCache();
                freedSpace += clearTempFiles();
                
                String summary = String.format("临时缓存清理完成！共释放 %s 空间", 
                    formatFileSize(freedSpace));
                
                final long finalFreedSpace = freedSpace;
                mainHandler.post(() -> callback.onComplete(finalFreedSpace, summary));
                
            } catch (Exception e) {
                mainHandler.post(() -> callback.onError("清理失败: " + e.getMessage()));
            }
        });
    }
    
    /**
     * 获取缓存大小
     */
    public void getCacheSize(CacheSizeCallback callback) {
        executor.execute(() -> {
            try {
                long imageCacheSize = imageStorageManager.getCacheSize();
                long appCacheSize = getAppCacheSize();
                long tempFileSize = getTempFilesSize();
                long logFileSize = getLogFilesSize();
                
                long totalSize = imageCacheSize + appCacheSize + tempFileSize + logFileSize;
                
                CacheSizeInfo sizeInfo = new CacheSizeInfo(
                    totalSize,
                    imageCacheSize,
                    appCacheSize,
                    tempFileSize,
                    logFileSize
                );
                
                mainHandler.post(() -> callback.onSizeCalculated(sizeInfo));
                
            } catch (Exception e) {
                mainHandler.post(() -> callback.onError("获取缓存大小失败: " + e.getMessage()));
            }
        });
    }
    
    /**
     * 清理应用缓存
     */
    private long clearAppCache() {
        long freedSpace = 0;
        
        // 清理内部缓存目录
        freedSpace += deleteDirectoryAndGetSize(context.getCacheDir());
        
        // 清理外部缓存目录
        if (context.getExternalCacheDir() != null) {
            freedSpace += deleteDirectoryAndGetSize(context.getExternalCacheDir());
        }
        
        return freedSpace;
    }
    
    /**
     * 清理临时文件
     */
    private long clearTempFiles() {
        long freedSpace = 0;
        
        // 清理各种临时文件
        String[] tempExtensions = {".tmp", ".temp", ".cache", ".bak"};
        
        // 清理内部存储中的临时文件
        File internalDir = context.getFilesDir();
        freedSpace += deleteFilesByExtension(internalDir, tempExtensions);
        
        // 清理照片存储目录中的临时文件
        File photoDir = PhotoStorageManager.getAttendancePhotosDir(context);
        freedSpace += deleteFilesByExtension(photoDir, tempExtensions);
        
        return freedSpace;
    }
    
    /**
     * 清理日志文件
     */
    private long clearLogFiles() {
        long freedSpace = 0;
        
        String[] logExtensions = {".log", ".txt"};
        
        // 清理内部存储中的日志文件
        File internalDir = context.getFilesDir();
        freedSpace += deleteFilesByExtension(internalDir, logExtensions);
        
        return freedSpace;
    }
    
    /**
     * 清理空目录
     */
    private void cleanEmptyDirectories() {
        // 清理内部存储中的空目录
        File internalDir = context.getFilesDir();
        deleteEmptyDirectories(internalDir);
        
        // 清理照片存储目录中的空目录
        File photoDir = PhotoStorageManager.getAttendancePhotosDir(context);
        deleteEmptyDirectories(photoDir);
    }
    
    /**
     * 获取应用缓存大小
     */
    private long getAppCacheSize() {
        long size = 0;
        
        size += getDirectorySize(context.getCacheDir());
        
        if (context.getExternalCacheDir() != null) {
            size += getDirectorySize(context.getExternalCacheDir());
        }
        
        return size;
    }
    
    /**
     * 获取临时文件大小
     */
    private long getTempFilesSize() {
        long size = 0;
        
        String[] tempExtensions = {".tmp", ".temp", ".cache", ".bak"};
        
        // 计算内部存储中的临时文件大小
        File internalDir = context.getFilesDir();
        size += getFilesSizeByExtension(internalDir, tempExtensions);
        
        // 计算照片存储目录中的临时文件大小
        File photoDir = PhotoStorageManager.getAttendancePhotosDir(context);
        size += getFilesSizeByExtension(photoDir, tempExtensions);
        
        return size;
    }
    
    /**
     * 获取日志文件大小
     */
    private long getLogFilesSize() {
        long size = 0;
        
        String[] logExtensions = {".log", ".txt"};
        
        // 计算内部存储中的日志文件大小
        File internalDir = context.getFilesDir();
        size += getFilesSizeByExtension(internalDir, logExtensions);
        
        return size;
    }
    
    /**
     * 根据扩展名删除文件
     */
    private long deleteFilesByExtension(File directory, String[] extensions) {
        long freedSpace = 0;
        
        File[] files = directory.listFiles();
        if (files != null) {
            for (File file : files) {
                if (file.isFile()) {
                    String fileName = file.getName().toLowerCase();
                    for (String extension : extensions) {
                        if (fileName.endsWith(extension)) {
                            freedSpace += file.length();
                            file.delete();
                            break;
                        }
                    }
                } else if (file.isDirectory()) {
                    freedSpace += deleteFilesByExtension(file, extensions);
                }
            }
        }
        
        return freedSpace;
    }
    
    /**
     * 根据扩展名获取文件大小
     */
    private long getFilesSizeByExtension(File directory, String[] extensions) {
        long size = 0;
        
        File[] files = directory.listFiles();
        if (files != null) {
            for (File file : files) {
                if (file.isFile()) {
                    String fileName = file.getName().toLowerCase();
                    for (String extension : extensions) {
                        if (fileName.endsWith(extension)) {
                            size += file.length();
                            break;
                        }
                    }
                } else if (file.isDirectory()) {
                    size += getFilesSizeByExtension(file, extensions);
                }
            }
        }
        
        return size;
    }
    
    /**
     * 删除空目录
     */
    private void deleteEmptyDirectories(File directory) {
        File[] files = directory.listFiles();
        if (files != null) {
            for (File file : files) {
                if (file.isDirectory()) {
                    deleteEmptyDirectories(file);
                    // 如果目录为空，删除它
                    if (file.listFiles() == null || file.listFiles().length == 0) {
                        file.delete();
                    }
                }
            }
        }
    }
    
    /**
     * 删除目录并获取大小
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
    
    /**
     * 删除目录
     */
    private void deleteDirectory(File dir) {
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
            dir.delete();
        }
    }
    
    /**
     * 格式化文件大小
     */
    private String formatFileSize(long size) {
        if (size <= 0) return "0 B";
        
        final String[] units = new String[]{"B", "KB", "MB", "GB", "TB"};
        int digitGroups = (int) (Math.log10(size) / Math.log10(1024));
        
        return new DecimalFormat("#,##0.#").format(size / Math.pow(1024, digitGroups)) + " " + units[digitGroups];
    }
    
    /**
     * 释放资源
     */
    public void cleanup() {
        if (executor != null && !executor.isShutdown()) {
            executor.shutdown();
        }
    }
    
    /**
     * 缓存大小回调接口
     */
    public interface CacheSizeCallback {
        void onSizeCalculated(CacheSizeInfo sizeInfo);
        void onError(String error);
    }
    
    /**
     * 缓存大小信息
     */
    public static class CacheSizeInfo {
        public final long totalSize;
        public final long imageCacheSize;
        public final long appCacheSize;
        public final long tempFileSize;
        public final long logFileSize;
        
        public CacheSizeInfo(long totalSize, long imageCacheSize, long appCacheSize, 
                           long tempFileSize, long logFileSize) {
            this.totalSize = totalSize;
            this.imageCacheSize = imageCacheSize;
            this.appCacheSize = appCacheSize;
            this.tempFileSize = tempFileSize;
            this.logFileSize = logFileSize;
        }
    }
}