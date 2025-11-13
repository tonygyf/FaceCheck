package com.example.facecheck.webdav;

import android.content.Context;
import android.util.Log;

import com.thegrizzlylabs.sardineandroid.Sardine;
import com.thegrizzlylabs.sardineandroid.DavResource;
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
    public static final String ROOT_DIR = "facecheck";
    // 简洁目录结构：数据库文件直接位于 facecheck 根；其余资源为扁平子目录
    private static final String DATA_DIR = "data"; // 可选数据文件夹
    private static final String AVATARS_DIR = "avatars";
    private static final String ATTENDANCE_DIR = "attendance";
    private static final String RESULTS_DIR = "results";
    
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
            
            // 创建扁平子目录
            createDirectoryIfNotExists(rootPath + "/" + DATA_DIR);
            createDirectoryIfNotExists(rootPath + "/" + AVATARS_DIR);
            createDirectoryIfNotExists(rootPath + "/" + ATTENDANCE_DIR);
            createDirectoryIfNotExists(rootPath + "/" + RESULTS_DIR);
            
            return true;
        } catch (IOException e) {
            Log.e(TAG, "Error initializing WebDAV directory structure", e);
            return false;
        }
    }

    // 创建标准结构：root/facecheck, root/facecheck/data/{cover,file}
    public boolean ensureFacecheckStructure() {
        try {
            createDirectoryIfNotExists(rootPath);
            createDirectoryIfNotExists(rootPath + "/" + DATA_DIR);
            createDirectoryIfNotExists(rootPath + "/" + DATA_DIR + "/cover");
            createDirectoryIfNotExists(rootPath + "/" + DATA_DIR + "/file");
            return true;
        } catch (IOException e) {
            Log.e(TAG, "ensureFacecheckStructure failed", e);
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
            byte[] data = new byte[(int) localFile.length()];
            fis.read(data);
            sardine.put(serverUrl + remotePath, data, "application/octet-stream");
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

    // 获取资源修改时间
    public java.util.Date getResourceModified(String remotePath) {
        try {
            String full = serverUrl + remotePath;
            List<DavResource> res = sardine.list(full);
            if (res != null && !res.isEmpty()) {
                // 第一个资源通常为目标
                return res.get(0).getModified();
            }
        } catch (IOException e) {
            Log.e(TAG, "getResourceModified error: " + remotePath, e);
        }
        return null;
    }

    // 上传目录中所有文件到远端目录
    public boolean uploadDirectory(String localDir, String remoteDir) {
        try {
            createDirectoryIfNotExists(serverUrl + remoteDir);
            File dir = new File(localDir);
            File[] files = dir.listFiles();
            if (files == null) return true;
            boolean ok = true;
            for (File f : files) {
                if (f.isFile()) {
                    String remoteRel = remoteDir + "/" + f.getName();
                    ok &= uploadFile(f.getAbsolutePath(), remoteRel);
                }
            }
            return ok;
        } catch (IOException e) {
            Log.e(TAG, "uploadDirectory failed: " + remoteDir, e);
            return false;
        }
    }

    // 下载远端目录所有文件到本地目录
    public boolean downloadDirectory(String remoteDir, String localDir) {
        try {
            List<DavResource> resources = sardine.list(serverUrl + remoteDir);
            File ldir = new File(localDir);
            if (!ldir.exists()) ldir.mkdirs();
            boolean ok = true;
            for (DavResource r : resources) {
                if (r.isDirectory()) continue;
                String name = new File(r.getName()).getName();
                ok &= downloadFile(remoteDir + "/" + name, new File(ldir, name).getAbsolutePath());
            }
            return ok;
        } catch (IOException e) {
            Log.e(TAG, "downloadDirectory failed: " + remoteDir, e);
            return false;
        }
    }

    public String getRootPath() { return rootPath; }

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
            // 优先尝试列举服务器根，部分服务对 exists(rootPath) 返回 404
            sardine.list(serverUrl);
            return true;
        } catch (IOException e1) {
            try {
                return sardine.exists(serverUrl) || sardine.exists(rootPath);
            } catch (IOException e2) {
                Log.e(TAG, "Error testing connection", e2);
                return false;
            }
        }
    }

    // 同步数据库文件
    public boolean syncDatabase(String localDbPath) {
        // 覆盖式上传数据库到根目录：facecheck/database7.db
        String remoteDbPath = ROOT_DIR + "/database7.db";
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
        String remoteDbPath = ROOT_DIR + "/database7.db";
        return downloadFile(remoteDbPath, localDbPath);
    }

    /**
     * 首次连接：若根目录不存在，初始化并上传本地数据库到 facecheck/database7.db
     */
    public boolean initializeIfNeededAndUploadDb(String localDbPath) {
        try {
            if (!exists(rootPath)) {
                if (!initializeDirectoryStructure()) return false;
            }
            return syncDatabase(localDbPath);
        } catch (Throwable t) {
            Log.e(TAG, "Initialize and upload DB failed", t);
            return false;
        }
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
