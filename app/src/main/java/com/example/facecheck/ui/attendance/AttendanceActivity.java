package com.example.facecheck.ui.attendance;

import android.content.Intent;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.FileProvider;

import com.example.facecheck.R;
import com.example.facecheck.database.DatabaseHelper;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class AttendanceActivity extends AppCompatActivity {
    private static final String TAG = "AttendanceActivity";
    
    private DatabaseHelper dbHelper;
    private long classroomId;
    private Uri currentPhotoUri;
    private long sessionId;
    
    private ImageView ivPreview;
    private Button btnTakePhoto;
    private Button btnStartRecognition;
    private ProgressBar progressBar;
    private TextView tvStatus;
    
    private ActivityResultLauncher<Intent> takePhotoLauncher;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_attendance);
        
        // 获取班级ID
        classroomId = getIntent().getLongExtra("classroom_id", -1);
        if (classroomId == -1) {
            Toast.makeText(this, "班级信息无效", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }
        
        // 初始化数据库
        dbHelper = new DatabaseHelper(this);
        
        // 初始化视图
        initViews();
        
        // 初始化照片选择器
        initPhotoLauncher();
        
        // 创建考勤会话
        createAttendanceSession();
    }

    private void initViews() {
        ivPreview = findViewById(R.id.ivPreview);
        btnTakePhoto = findViewById(R.id.btnTakePhoto);
        btnStartRecognition = findViewById(R.id.btnStartRecognition);
        progressBar = findViewById(R.id.progressBar);
        tvStatus = findViewById(R.id.tvStatus);
        
        btnTakePhoto.setOnClickListener(v -> takePhoto());
        btnStartRecognition.setOnClickListener(v -> startRecognition());
        
        // 初始状态
        btnStartRecognition.setEnabled(false);
        progressBar.setVisibility(View.GONE);
    }

    private void initPhotoLauncher() {
        takePhotoLauncher = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            result -> {
                if (result.getResultCode() == RESULT_OK && currentPhotoUri != null) {
                    try {
                        // 显示预览图
                        Bitmap bitmap = MediaStore.Images.Media.getBitmap(
                            getContentResolver(), currentPhotoUri);
                        ivPreview.setImageBitmap(bitmap);
                        
                        // 保存照片资源记录
                        long photoId = dbHelper.insertPhotoAsset(sessionId, "RAW", 
                            currentPhotoUri.toString(), "");
                            
                        if (photoId != -1) {
                            // 添加同步日志
                            dbHelper.insertSyncLog("PhotoAsset", photoId, "UPSERT", 
                                System.currentTimeMillis(), "PENDING");
                                
                            // 启用识别按钮
                            btnStartRecognition.setEnabled(true);
                        }
                    } catch (Exception e) {
                        e.printStackTrace();
                        Toast.makeText(this, "加载照片失败", Toast.LENGTH_SHORT).show();
                    }
                }
            });
    }

    private void createAttendanceSession() {
        // 创建考勤会话
        sessionId = dbHelper.insertAttendanceSession(classroomId, "", "", "");
            
        if (sessionId != -1) {
            // 添加同步日志
            dbHelper.insertSyncLog("AttendanceSession", sessionId, "UPSERT", 
                System.currentTimeMillis(), "PENDING");
        } else {
            Toast.makeText(this, "创建考勤会话失败", Toast.LENGTH_SHORT).show();
            finish();
        }
    }

    private void takePhoto() {
        Intent intent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        File photoFile = new File(getExternalFilesDir("attendance"), 
            new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault())
                .format(new Date()) + ".jpg");
            
        currentPhotoUri = FileProvider.getUriForFile(this,
            "com.example.facecheck.fileprovider", photoFile);
            
        intent.putExtra(MediaStore.EXTRA_OUTPUT, currentPhotoUri);
        takePhotoLauncher.launch(intent);
    }

    private void startRecognition() {
        // 显示进度条
        progressBar.setVisibility(View.VISIBLE);
        btnStartRecognition.setEnabled(false);
        tvStatus.setText("正在识别...");
        
        // 在后台线程执行识别
        new Thread(() -> {
            try {
                // TODO: 实现人脸检测和识别逻辑
                // 1. 人脸检测
                // 2. 人脸对齐
                // 3. 特征提取
                // 4. 特征比对
                // 5. 生成考勤结果
                
                // 模拟识别过程
                Thread.sleep(2000);
                
                runOnUiThread(() -> {
                    progressBar.setVisibility(View.GONE);
                    tvStatus.setText("识别完成");
                    
                    // 跳转到结果界面
                    Intent intent = new Intent(this, AttendanceResultActivity.class);
                    intent.putExtra("session_id", sessionId);
                    startActivity(intent);
                    finish();
                });
            } catch (Exception e) {
                e.printStackTrace();
                runOnUiThread(() -> {
                    progressBar.setVisibility(View.GONE);
                    btnStartRecognition.setEnabled(true);
                    tvStatus.setText("识别失败");
                    Toast.makeText(this, "识别失败: " + e.getMessage(), 
                        Toast.LENGTH_SHORT).show();
                });
            }
        }).start();
    }
}