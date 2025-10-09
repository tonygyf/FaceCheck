package com.example.facecheck.ui.student;

import android.Manifest;
import android.app.AlertDialog;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.core.content.FileProvider;

import com.bumptech.glide.Glide;
import com.example.facecheck.R;
import com.example.facecheck.database.DatabaseHelper;
import com.example.facecheck.data.model.Student;
import com.example.facecheck.utils.PhotoStorageManager;

import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class StudentActivity extends AppCompatActivity {
    
    private static final int REQUEST_IMAGE_CAPTURE = 1;
    private static final int REQUEST_PICK_IMAGE = 2;
    
    private DatabaseHelper dbHelper;
    private PhotoStorageManager photoStorageManager;
    
    private EditText etStudentName, etStudentId;
    private ImageView ivStudentPhoto;
    private Button btnSave, btnTakePhoto, btnChoosePhoto;
    
    private long studentId = -1;
    private long classId = -1;
    private String currentPhotoPath;
    private Student currentStudent;
    
    private ActivityResultLauncher<Intent> takePhotoLauncher;
    private ActivityResultLauncher<String> pickPhotoLauncher;
    private ActivityResultLauncher<String> requestCameraPermissionLauncher;
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main); // 临时使用main布局，后续需要创建专门的布局
        
        // 初始化数据库和照片存储管理器
        dbHelper = new DatabaseHelper(this);
        photoStorageManager = new PhotoStorageManager(this);
        
        // 获取传递的参数
        studentId = getIntent().getLongExtra("student_id", -1);
        classId = getIntent().getLongExtra("class_id", -1);
        
        // 初始化视图（临时）
        initViews();
        
        // 初始化照片选择器
        initPhotoLaunchers();
        
        if (studentId != -1) {
            // 编辑现有学生
            setTitle("学生详情");
            loadStudentData();
        } else if (classId != -1) {
            // 添加新学生
            setTitle("添加学生");
        } else {
            Toast.makeText(this, "参数错误", Toast.LENGTH_SHORT).show();
            finish();
        }
    }
    
    private void initViews() {
        // 临时初始化视图，后续需要创建专门的布局文件
        etStudentName = new EditText(this);
        etStudentId = new EditText(this);
        ivStudentPhoto = new ImageView(this);
        btnSave = new Button(this);
        btnTakePhoto = new Button(this);
        btnChoosePhoto = new Button(this);
        
        etStudentName.setHint("学生姓名");
        etStudentId.setHint("学号");
        btnSave.setText("保存");
        btnTakePhoto.setText("拍照");
        btnChoosePhoto.setText("选择照片");
        
        // 设置点击事件
        btnTakePhoto.setOnClickListener(v -> checkCameraPermissionAndTakePhoto());
        btnChoosePhoto.setOnClickListener(v -> choosePhotoFromGallery());
        btnSave.setOnClickListener(v -> saveStudent());
    }
    
    private void initPhotoLaunchers() {
        // 拍照结果处理
        takePhotoLauncher = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            result -> {
                if (result.getResultCode() == RESULT_OK) {
                    // 拍照成功，处理照片
                    processTakenPhoto();
                }
            }
        );
        
        // 选择照片结果处理
        pickPhotoLauncher = registerForActivityResult(
            new ActivityResultContracts.GetContent(),
            uri -> {
                if (uri != null) {
                    // 处理选择的图片
                    processSelectedPhoto(uri);
                }
            }
        );
        
        // 相机权限请求
        requestCameraPermissionLauncher = registerForActivityResult(
            new ActivityResultContracts.RequestPermission(),
            isGranted -> {
                if (isGranted) {
                    takePhoto();
                } else {
                    Toast.makeText(this, "需要相机权限才能拍照", Toast.LENGTH_SHORT).show();
                }
            }
        );
    }
    
    private void loadStudentData() {
        android.database.Cursor cursor = dbHelper.getStudentById(studentId);
        if (cursor != null && cursor.moveToFirst()) {
            currentStudent = new Student(
                cursor.getLong(cursor.getColumnIndexOrThrow("id")),
                cursor.getLong(cursor.getColumnIndexOrThrow("classId")),
                cursor.getString(cursor.getColumnIndexOrThrow("name")),
                cursor.getString(cursor.getColumnIndexOrThrow("sid")),
                cursor.getString(cursor.getColumnIndexOrThrow("gender")),
                cursor.getString(cursor.getColumnIndexOrThrow("avatarUri")),
                cursor.getLong(cursor.getColumnIndexOrThrow("createdAt"))
            );
            
            etStudentName.setText(currentStudent.getName());
            etStudentId.setText(currentStudent.getSid());
            
            // 加载现有照片
            String avatarUri = cursor.getString(cursor.getColumnIndexOrThrow("avatarUri"));
            if (avatarUri != null && !avatarUri.isEmpty()) {
                File photoFile = new File(avatarUri);
                if (photoFile.exists()) {
                    Glide.with(this)
                        .load(photoFile)
                        .into(ivStudentPhoto);
                }
            }
            cursor.close();
        }
    }
    
    private void checkCameraPermissionAndTakePhoto() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) 
                != PackageManager.PERMISSION_GRANTED) {
            requestCameraPermissionLauncher.launch(Manifest.permission.CAMERA);
        } else {
            takePhoto();
        }
    }
    
    private void takePhoto() {
        try {
            File photoFile = PhotoStorageManager.createAvatarPhotoFile(this);
            currentPhotoPath = photoFile.getAbsolutePath();
            
            Intent intent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
            Uri photoUri = FileProvider.getUriForFile(this,
                "com.example.facecheck.fileprovider", photoFile);
            
            intent.putExtra(MediaStore.EXTRA_OUTPUT, photoUri);
            takePhotoLauncher.launch(intent);
            
        } catch (IOException e) {
            Toast.makeText(this, "无法创建照片文件: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }
    
    private void choosePhotoFromGallery() {
        pickPhotoLauncher.launch("image/*");
    }
    
    private void processTakenPhoto() {
        if (currentPhotoPath != null) {
            File photoFile = new File(currentPhotoPath);
            if (photoFile.exists()) {
                Glide.with(this)
                    .load(photoFile)
                    .into(ivStudentPhoto);
            }
        }
    }
    
    private void processSelectedPhoto(Uri uri) {
        try {
            // 创建目标文件
            File photoFile = PhotoStorageManager.createAvatarPhotoFile(this);
            currentPhotoPath = photoFile.getAbsolutePath();
            
            // 复制选择的图片到应用存储
            copyUriToFile(uri, photoFile);
            
            // 显示图片
            Glide.with(this)
                .load(photoFile)
                .into(ivStudentPhoto);
                
        } catch (IOException e) {
            Toast.makeText(this, "处理图片失败: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }
    
    private void copyUriToFile(Uri sourceUri, File destFile) {
        try {
            android.content.ContentResolver contentResolver = getContentResolver();
            java.io.InputStream inputStream = contentResolver.openInputStream(sourceUri);
            java.io.OutputStream outputStream = new java.io.FileOutputStream(destFile);
            
            byte[] buffer = new byte[4096];
            int bytesRead;
            while ((bytesRead = inputStream.read(buffer)) != -1) {
                outputStream.write(buffer, 0, bytesRead);
            }
            
            inputStream.close();
            outputStream.close();
            
        } catch (Exception e) {
            Toast.makeText(this, "复制图片失败: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }
    
    private void saveStudent() {
        String name = etStudentName.getText().toString().trim();
        String studentIdStr = etStudentId.getText().toString().trim();
        
        if (name.isEmpty() || studentIdStr.isEmpty()) {
            Toast.makeText(this, "请填写完整信息", Toast.LENGTH_SHORT).show();
            return;
        }
        
        try {
            long studentIdLong = Long.parseLong(studentIdStr);
            
            if (studentId != -1) {
                // 更新现有学生
                currentStudent.setName(name);
                currentStudent.setSid(studentIdStr);
                if (currentPhotoPath != null) {
                    currentStudent.setFaceImagePath(currentPhotoPath);
                }
                
                dbHelper.updateStudent(
                    currentStudent.getId(),
                    currentStudent.getClassId(),
                    currentStudent.getName(),
                    currentStudent.getSid(),
                    currentStudent.getGender(),
                    currentPhotoPath
                );
                Toast.makeText(this, "学生信息已更新", Toast.LENGTH_SHORT).show();
            } else {
                // 添加新学生
                Student newStudent = new Student(classId, name, studentIdStr, "未知");
                newStudent.setFaceImagePath(currentPhotoPath);
                
                long newStudentId = dbHelper.insertStudent(classId, name, studentIdStr, "未知", currentPhotoPath);
                if (newStudentId != -1) {
                    Toast.makeText(this, "学生添加成功", Toast.LENGTH_SHORT).show();
                    finish();
                } else {
                    Toast.makeText(this, "添加学生失败", Toast.LENGTH_SHORT).show();
                }
            }
            
        } catch (NumberFormatException e) {
            Toast.makeText(this, "学号格式错误", Toast.LENGTH_SHORT).show();
        }
    }
}