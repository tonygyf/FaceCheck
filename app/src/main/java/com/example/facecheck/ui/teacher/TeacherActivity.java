package com.example.facecheck.ui.teacher;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.view.MenuItem;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import com.bumptech.glide.Glide;
import com.example.facecheck.R;
import com.example.facecheck.database.DatabaseHelper;
import com.example.facecheck.data.model.Teacher;
import com.example.facecheck.utils.PhotoStorageManager;

import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class TeacherActivity extends AppCompatActivity {
    private EditText etName;
    private EditText etEmail;
    private Button btnSave;
    private Button btnChangeAvatar;
    private ImageView ivTeacherAvatar;
    private DatabaseHelper dbHelper;
    private PhotoStorageManager photoStorageManager;
    private long teacherId = -1;
    private Teacher currentTeacher;
    private String currentPhotoPath;
    private Uri currentPhotoUri;
    
    private ActivityResultLauncher<Intent> takePhotoLauncher;
    private ActivityResultLauncher<String> pickPhotoLauncher;
    private ActivityResultLauncher<String> requestCameraPermissionLauncher;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_teacher_edit);

        // 初始化数据库和照片存储管理器
        dbHelper = new DatabaseHelper(this);
        photoStorageManager = new PhotoStorageManager(this);

        // 设置工具栏
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        getSupportActionBar().setTitle("教师信息");

        // 初始化视图
        etName = findViewById(R.id.et_name);
        etEmail = findViewById(R.id.et_username); // 使用username字段
        btnSave = findViewById(R.id.btn_save);
        btnChangeAvatar = findViewById(R.id.btn_change_avatar);
        ivTeacherAvatar = findViewById(R.id.iv_teacher_avatar);

        // 初始化照片选择器
        initPhotoLaunchers();

        // 获取教师ID
        teacherId = getIntent().getLongExtra("teacher_id", -1);
        if (teacherId != -1) {
            // 加载教师信息
            loadTeacherInfo();
        }

        // 设置按钮点击事件
        btnSave.setOnClickListener(v -> saveTeacher());
        btnChangeAvatar.setOnClickListener(v -> showPhotoSourceDialog());
    }
    
    private void initPhotoLaunchers() {
        // 拍照启动器
        takePhotoLauncher = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            result -> {
                if (result.getResultCode() == RESULT_OK) {
                    processNewPhoto(Uri.fromFile(new File(currentPhotoPath)));
                }
            }
        );
        
        // 选择照片启动器
        pickPhotoLauncher = registerForActivityResult(
            new ActivityResultContracts.GetContent(),
            uri -> {
                if (uri != null) {
                    processNewPhoto(uri);
                }
            }
        );
        
        // 相机权限请求启动器
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

    private void loadTeacherInfo() {
        currentTeacher = dbHelper.getTeacherById(teacherId);
        if (currentTeacher != null) {
            etName.setText(currentTeacher.getName());
            etEmail.setText(currentTeacher.getUsername());
            
            // 加载头像
            if (currentTeacher.getAvatarUri() != null && !currentTeacher.getAvatarUri().isEmpty()) {
                File avatarFile = new File(currentTeacher.getAvatarUri());
                if (avatarFile.exists()) {
                    Glide.with(this)
                        .load(avatarFile)
                        .placeholder(R.drawable.ic_person_placeholder)
                        .error(R.drawable.ic_person_placeholder)
                        .circleCrop()
                        .into(ivTeacherAvatar);
                }
            }
        }
    }

    private void saveTeacher() {
        String name = etName.getText().toString().trim();
        String username = etEmail.getText().toString().trim();

        if (name.isEmpty() || username.isEmpty()) {
            Toast.makeText(this, "请填写所有字段", Toast.LENGTH_SHORT).show();
            return;
        }

        // 使用当前教师对象，保留头像信息
        if (currentTeacher == null) {
            currentTeacher = new Teacher();
        }
        currentTeacher.setId(teacherId);
        currentTeacher.setName(name);
        currentTeacher.setUsername(username);
        
        boolean success;
        
        if (teacherId == -1) {
            // 新建教师
            success = dbHelper.addTeacher(currentTeacher);
        } else {
            // 更新教师信息
            success = dbHelper.updateTeacher(currentTeacher);
        }

        if (success) {
            Toast.makeText(this, "保存成功", Toast.LENGTH_SHORT).show();
            finish();
        } else {
            Toast.makeText(this, "保存失败", Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            onBackPressed();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }
    
    private void showPhotoSourceDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("选择头像来源")
                .setItems(new String[]{"拍照", "从相册选择"}, (dialog, which) -> {
                    switch (which) {
                        case 0:
                            checkCameraPermissionAndTakePhoto();
                            break;
                        case 1:
                            pickPhotoLauncher.launch("image/*");
                            break;
                    }
                })
                .setNegativeButton("取消", null)
                .show();
    }
    
    private void checkCameraPermissionAndTakePhoto() {
        requestCameraPermissionLauncher.launch(android.Manifest.permission.CAMERA);
    }
    
    private void takePhoto() {
        Intent takePictureIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        if (takePictureIntent.resolveActivity(getPackageManager()) != null) {
            File photoFile = null;
            try {
                photoFile = createImageFile();
            } catch (IOException ex) {
                Toast.makeText(this, "创建图片文件失败", Toast.LENGTH_SHORT).show();
                return;
            }
            
            if (photoFile != null) {
                currentPhotoUri = Uri.fromFile(photoFile);
                takePictureIntent.putExtra(MediaStore.EXTRA_OUTPUT, currentPhotoUri);
                takePhotoLauncher.launch(takePictureIntent);
            }
        }
    }
    
    private File createImageFile() throws IOException {
        String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(new Date());
        String imageFileName = "TEACHER_AVATAR_" + timeStamp + "_";
        File storageDir = photoStorageManager.getAvatarDir();
        File image = File.createTempFile(imageFileName, ".jpg", storageDir);
        currentPhotoPath = image.getAbsolutePath();
        return image;
    }
    
    private void processNewPhoto(Uri photoUri) {
        // 显示照片预览
        if (ivTeacherAvatar != null) {
            Glide.with(this)
                .load(photoUri)
                .placeholder(R.drawable.ic_person_placeholder)
                .error(R.drawable.ic_person_placeholder)
                .circleCrop()
                .into(ivTeacherAvatar);
        }
        
        // 显示确认对话框
        new AlertDialog.Builder(this)
            .setTitle("确认修改头像")
            .setMessage("确定要将这张照片设为头像吗？")
            .setPositiveButton("确定", (dialog, which) -> {
                updateTeacherAvatar(photoUri);
            })
            .setNegativeButton("取消", null)
            .show();
    }
    
    private void updateTeacherAvatar(Uri photoUri) {
        // 保存头像文件
        String avatarPath = photoStorageManager.saveAvatar(photoUri, teacherId, /* isStudent */ false);
        
        if (avatarPath != null && currentTeacher != null) {
            // 更新教师头像路径
            currentTeacher.setAvatarUri(avatarPath);
            
            // 保存到数据库
            boolean success = dbHelper.updateTeacher(currentTeacher);
            if (success) {
                Toast.makeText(this, "头像更新成功", Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(this, "头像更新失败", Toast.LENGTH_SHORT).show();
            }
        }
    }
}