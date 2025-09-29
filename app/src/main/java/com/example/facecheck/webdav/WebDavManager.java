package com.example.facecheck.webdav;

import android.content.Context;
import android.util.Log;

import com.thegrizzlylabs.sardineandroid.Sardine;
import com.thegrizzlylabs.sardineandroid.impl.OkHttpSardine;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

public class WebDavManager {
    private static final String TAG = "WebDavManager";
    
    // WebDAV目录结构
    private static final String ROOT_DIR = "facecheck";
    private static final String DATABASE_DIR = "database";
    private static final String PHOTOS_DIR = "photos";
    private static final String AVATARS_DIR = "photos/avatars";
    private static final String ATTENDANCE_DIR = "photos/attendance";
    private static final String RESULTS_DIR = "photos/results";
    
    private final Context context;
    private final String serverUrl;
    private final String username;
    private final String password;
    private final Sardine sardine;
    private final String rootPath;

    public WebDavManager(Context context, String serverUrl, String username, String password) {
        this.context = context;
        this.serverUrl = serverUrl.endsWith("/") ? serverUrl : serverUrl + "/";
        this.username = username;
        this.password = password;
        this.sardine = new OkHttpSardine();
        this.sardine.setCredentials(username, password);
        this.rootPath = this.serverUrl + ROOT_DIR;
    }

    // 初始化WebDAV目录结构
    public boolean initializeDirectoryStructure() {
        try {
            // 创建根目录
            createDirectoryIfNotExists(rootPath);
            
            // 创建子目录
            createDirectoryIfNotExists(rootPath + "/" + DATABASE_DIR);
            createDirectoryIfNotExists(rootPath + "/" + PHOTOS_DIR);
            createDirectoryIfNotExists(rootPath + "/" + AVATARS_DIR);
            createDirectoryIfNotExists(rootPath + "/" + ATTENDANCE_DIR);
            createDirectoryIfNotExists(rootPath + "/" + RESULTS_DIR);
            
            return true;
        } catch (IOException e) {
            Log.e(TAG, "Error initializing WebDAV directory structure", e);
            return false;
        }
    }

    // 创建目录（如果不存在）
    private void createDirectoryIfNotExists(String path) throws IOException {
        if (!exists(path)) {
            sardine.createDirectory(path);
        }
    }

    // 检查路径是否存在
    public boolean exists(String path) {
        try {
            return sardine.exists(path);
        } catch (IOException e) {
            Log.e(TAG, "Error checking path existence: " + path, e);
            return false;
        }
    }

    // 上传文件
    public boolean uploadFile(String localPath, String remotePath) {
        try {
            File localFile = new File(localPath);
            if (!localFile.exists()) {
                Log.e(TAG, "Local file does not exist: " + localPath);
                return false;
            }

            FileInputStream fis = new FileInputStream(localFile);
            sardine.put(serverUrl + remotePath, fis);
            fis.close();
            return true;
        } catch (IOException e) {
            Log.e(TAG, "Error uploading file: " + localPath, e);
            return false;
        }
    }

    // 下载文件
    public boolean downloadFile(String remotePath, String localPath) {
        try {
            InputStream is = sardine.get(serverUrl + remotePath);
            File localFile = new File(localPath);
            
            // 确保父目录存在
            File parentDir = localFile.getParentFile();
            if (parentDir != null && !parentDir.exists()) {
                parentDir.mkdirs();
            }

            FileOutputStream fos = new FileOutputStream(localFile);
            byte[] buffer = new byte[8192];
            int bytesRead;
            while ((bytesRead = is.read(buffer)) != -1) {
                fos.write(buffer, 0, bytesRead);
            }
            
            fos.close();
            is.close();
            return true;
        } catch (IOException e) {
            Log.e(TAG, "Error downloading file: " + remotePath, e);
            return false;
        }
    }

    // 列出目录内容
    public List<String> listDirectory(String path) {
        try {
            List<String> files = new ArrayList<>();
            String fullPath = serverUrl + path;
            sardine.list(fullPath).forEach(resource -> {
                String resourcePath = resource.getPath();
                // 移除服务器URL前缀，只保留相对路径
                if (resourcePath.startsWith(serverUrl)) {
                    resourcePath = resourcePath.substring(serverUrl.length());
                }
                if (!resource.isDirectory()) {
                    files.add(resourcePath);
                }
            });
            return files;
        } catch (IOException e) {
            Log.e(TAG, "Error listing directory: " + path, e);
            return new ArrayList<>();
        }
    }

    // 删除文件
    public boolean deleteFile(String path) {
        try {
            sardine.delete(serverUrl + path);
            return true;
        } catch (IOException e) {
            Log.e(TAG, "Error deleting file: " + path, e);
            return false;
        }
    }

    // 测试连接
    public boolean testConnection() {
        try {
            return sardine.exists(rootPath);
        } catch (IOException e) {
            Log.e(TAG, "Error testing connection", e);
            return false;
        }
    }

    // 同步数据库文件
    public boolean syncDatabase(String localDbPath) {
        String remoteDbPath = ROOT_DIR + "/" + DATABASE_DIR + "/facecheck.db";
        return uploadFile(localDbPath, remoteDbPath);
    }

    // 同步头像
    public boolean syncAvatar(String localPath, String fileName) {
        String remotePath = ROOT_DIR + "/" + AVATARS_DIR + "/" + fileName;
        return uploadFile(localPath, remotePath);
    }

    // 同步考勤照片
    public boolean syncAttendancePhoto(String localPath, String fileName) {
        String remotePath = ROOT_DIR + "/" + ATTENDANCE_DIR + "/" + fileName;
        return uploadFile(localPath, remotePath);
    }

    // 同步识别结果
    public boolean syncResultPhoto(String localPath, String fileName) {
        String remotePath = ROOT_DIR + "/" + RESULTS_DIR + "/" + fileName;
        return uploadFile(localPath, remotePath);
    }

    // 获取数据库文件
    public boolean fetchDatabase(String localDbPath) {
        String remoteDbPath = ROOT_DIR + "/" + DATABASE_DIR + "/facecheck.db";
        return downloadFile(remoteDbPath, localDbPath);
    }

    // 获取头像
    public boolean fetchAvatar(String fileName, String localPath) {
        String remotePath = ROOT_DIR + "/" + AVATARS_DIR + "/" + fileName;
        return downloadFile(remotePath, localPath);
    }

    // 获取考勤照片
    public boolean fetchAttendancePhoto(String fileName, String localPath) {
        String remotePath = ROOT_DIR + "/" + ATTENDANCE_DIR + "/" + fileName;
        return downloadFile(remotePath, localPath);
    }

    // 获取识别结果
    public boolean fetchResultPhoto(String fileName, String localPath) {
        String remotePath = ROOT_DIR + "/" + RESULTS_DIR + "/" + fileName;
        return downloadFile(remotePath, localPath);
    }
}