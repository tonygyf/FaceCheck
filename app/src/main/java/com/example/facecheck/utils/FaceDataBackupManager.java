package com.example.facecheck.utils;

import android.content.Context;
import android.os.Environment;
import android.util.Log;
import android.widget.Toast;

import com.example.facecheck.database.DatabaseHelper;
import com.example.facecheck.data.model.Student;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FilenameFilter;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class FaceDataBackupManager {
    
    private static final String TAG = "FaceDataBackupManager";
    private static final String BACKUP_DIR = "FaceCheck/Backups";
    private static final String BACKUP_FILE_PREFIX = "face_data_backup_";
    private static final String BACKUP_FILE_SUFFIX = ".json";
    
    private Context context;
    private DatabaseHelper databaseHelper;
    
    public FaceDataBackupManager(Context context) {
        this.context = context;
        this.databaseHelper = new DatabaseHelper(context);
    }
    
    /**
     * 备份人脸数据到文件
     */
    public String backupFaceData() {
        try {
            // 获取所有学生数据
            List<Student> students = databaseHelper.getAllStudents();
            
            // 创建JSON对象存储备份数据
            JSONObject backupData = new JSONObject();
            backupData.put("backup_time", new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(new Date()));
            backupData.put("backup_version", "1.0");
            backupData.put("total_students", students.size());
            
            // 创建学生数组
            JSONArray studentsArray = new JSONArray();
            for (Student student : students) {
                JSONObject studentObj = new JSONObject();
                studentObj.put("id", student.getId());
                studentObj.put("name", student.getName());
                studentObj.put("student_id", student.getSid());
                studentObj.put("class_id", student.getClassId());
                studentObj.put("face_features", student.getFaceFeatures()); // 人脸特征向量
                studentObj.put("face_image_path", student.getFaceImagePath()); // 人脸图片路径
                studentsArray.put(studentObj);
            }
            backupData.put("students", studentsArray);
            
            // 创建备份文件
            String backupFileName = BACKUP_FILE_PREFIX + 
                    new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(new Date()) + 
                    BACKUP_FILE_SUFFIX;
            
            File backupDir = new File(Environment.getExternalStorageDirectory(), BACKUP_DIR);
            if (!backupDir.exists()) {
                backupDir.mkdirs();
            }
            
            File backupFile = new File(backupDir, backupFileName);
            
            // 写入文件
            FileOutputStream fos = new FileOutputStream(backupFile);
            fos.write(backupData.toString(2).getBytes()); // 格式化的JSON
            fos.close();
            
            Log.i(TAG, "Face data backup successful: " + backupFile.getAbsolutePath());
            return backupFile.getAbsolutePath();
            
        } catch (JSONException | IOException e) {
            Log.e(TAG, "Face data backup failed", e);
            return null;
        }
    }
    
    /**
     * 从备份文件恢复人脸数据
     */
    public boolean restoreFaceData(String backupFilePath) {
        try {
            File backupFile = new File(backupFilePath);
            if (!backupFile.exists()) {
                Log.e(TAG, "Backup file not found: " + backupFilePath);
                return false;
            }
            
            // 读取备份文件
            FileInputStream fis = new FileInputStream(backupFile);
            byte[] buffer = new byte[(int) backupFile.length()];
            fis.read(buffer);
            fis.close();
            
            String backupContent = new String(buffer);
            JSONObject backupData = new JSONObject(backupContent);
            
            // 解析备份数据
            String backupTime = backupData.getString("backup_time");
            String backupVersion = backupData.getString("backup_version");
            int totalStudents = backupData.getInt("total_students");
            
            Log.i(TAG, "Restoring backup from: " + backupTime + ", version: " + backupVersion);
            
            // 获取学生数组
            JSONArray studentsArray = backupData.getJSONArray("students");
            
            // 清空现有数据（可选）
            // databaseHelper.clearAllStudents(); // 如果需要完全替换
            
            // 恢复学生数据
            int restoredCount = 0;
            for (int i = 0; i < studentsArray.length(); i++) {
                JSONObject studentObj = studentsArray.getJSONObject(i);
                
                Student student = new Student();
                student.setId(studentObj.getInt("id"));
                student.setName(studentObj.getString("name"));
                student.setSid(studentObj.getString("student_id"));
                student.setClassId(studentObj.getInt("class_id"));
                student.setFaceFeatures(studentObj.getString("face_features"));
                student.setFaceImagePath(studentObj.getString("face_image_path"));
                
                // 更新或插入学生数据
                databaseHelper.updateStudentFaceData(student);
                restoredCount++;
            }
            
            Log.i(TAG, "Face data restore successful, restored " + restoredCount + " students");
            return true;
            
        } catch (JSONException | IOException e) {
            Log.e(TAG, "Face data restore failed", e);
            return false;
        }
    }
    
    /**
     * 获取所有备份文件列表
     */
    public List<String> getBackupFiles() {
        List<String> backupFiles = new ArrayList<>();
        
        try {
            File backupDir = new File(Environment.getExternalStorageDirectory(), BACKUP_DIR);
            if (backupDir.exists() && backupDir.isDirectory()) {
                File[] files = backupDir.listFiles(new FilenameFilter() {
                    @Override
                    public boolean accept(File dir, String name) {
                        return name.startsWith(BACKUP_FILE_PREFIX) && name.endsWith(BACKUP_FILE_SUFFIX);
                    }
                });
                
                if (files != null) {
                    for (File file : files) {
                        backupFiles.add(file.getAbsolutePath());
                    }
                }
            }
        } catch (Exception e) {
            Log.e(TAG, "Error getting backup files", e);
        }
        
        return backupFiles;
    }
    
    /**
     * 删除备份文件
     */
    public boolean deleteBackupFile(String backupFilePath) {
        try {
            File backupFile = new File(backupFilePath);
            if (backupFile.exists()) {
                boolean deleted = backupFile.delete();
                Log.i(TAG, "Backup file deleted: " + backupFilePath + ", success: " + deleted);
                return deleted;
            }
            return false;
        } catch (Exception e) {
            Log.e(TAG, "Error deleting backup file", e);
            return false;
        }
    }
    
    /**
     * 显示备份操作结果
     */
    public void showBackupResult(boolean success, String message) {
        if (success) {
            Toast.makeText(context, "备份成功: " + message, Toast.LENGTH_LONG).show();
        } else {
            Toast.makeText(context, "备份失败: " + message, Toast.LENGTH_LONG).show();
        }
    }
    
    /**
     * 显示恢复操作结果
     */
    public void showRestoreResult(boolean success, String message) {
        if (success) {
            Toast.makeText(context, "恢复成功: " + message, Toast.LENGTH_LONG).show();
        } else {
            Toast.makeText(context, "恢复失败: " + message, Toast.LENGTH_LONG).show();
        }
    }
}