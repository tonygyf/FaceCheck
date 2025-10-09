package com.example.facecheck.ui.attendance;

import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.*;

import androidx.appcompat.app.AlertDialog;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.core.content.FileProvider;

import com.example.facecheck.R;
import com.example.facecheck.database.DatabaseHelper;
import com.example.facecheck.utils.*;
import com.example.facecheck.utils.PhotoStorageManager;

import java.util.ArrayList;
import com.example.facecheck.utils.ImageStorageManager;
import com.example.facecheck.utils.CacheManager;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class AttendanceActivity extends AppCompatActivity {
    private static final String TAG = "AttendanceActivity";
    
    private DatabaseHelper dbHelper;
    private long classroomId;
    private Uri currentPhotoUri;
    private long sessionId;
    private FaceDetectionManager faceDetectionManager;
    private FaceRecognitionManager faceRecognitionManager;
    private ImageStorageManager imageStorageManager;
    private CacheManager cacheManager;
    
    private ImageView ivPreview;
    private Button btnTakePhoto;
    private Button btnStartRecognition;
    private ProgressBar progressBar;
    private TextView tvStatus;
    private Spinner spinnerModel;
    
    // 模型选择相关
    private String[] modelOptions = {
        "Google ML Kit (推荐)",
        "TensorFlow Lite + FaceNet",
        "OpenCV DNN"
    };
    private String selectedModel = "Google ML Kit (推荐)";
    
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
        
        // 初始化人脸检测管理器
        faceDetectionManager = new FaceDetectionManager(this);
        faceRecognitionManager = new FaceRecognitionManager(this);
        imageStorageManager = new ImageStorageManager(this);
        cacheManager = new CacheManager(this);
        
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
        spinnerModel = findViewById(R.id.spinnerModel);
        
        // 初始化模型选择器
        ArrayAdapter<String> adapter = new ArrayAdapter<>(this, 
            android.R.layout.simple_spinner_item, modelOptions);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerModel.setAdapter(adapter);
        
        spinnerModel.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                selectedModel = modelOptions[position];
                Toast.makeText(AttendanceActivity.this, 
                    "已选择模型: " + selectedModel, Toast.LENGTH_SHORT).show();
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
                // 不做处理
            }
        });
        
        btnTakePhoto.setOnClickListener(v -> checkCameraPermissionAndTakePhoto());
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
                        long photoId = dbHelper.insertPhotoAsset(sessionId, null, "RAW", 
                            currentPhotoUri.toString(), "");
                            
                        if (photoId != -1) {
                            // 添加同步日志
                            dbHelper.insertSyncLog("PhotoAsset", photoId, "INSERT", 
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
        sessionId = dbHelper.insertAttendanceSession(classroomId, 1L, "", "", ""); // 默认使用ID为1的教师
            
        if (sessionId != -1) {
            // 添加同步日志
            dbHelper.insertSyncLog("AttendanceSession", sessionId, "INSERT", 
                System.currentTimeMillis(), "PENDING");
        } else {
            Toast.makeText(this, "创建考勤会话失败", Toast.LENGTH_SHORT).show();
            finish();
        }
    }

    private void checkCameraPermissionAndTakePhoto() {
        // 检查相机权限
        if (ContextCompat.checkSelfPermission(this, android.Manifest.permission.CAMERA) 
                != PackageManager.PERMISSION_GRANTED) {
            // 请求相机权限
            requestCameraPermissionLauncher.launch(android.Manifest.permission.CAMERA);
        } else {
            // 已有权限，直接拍照
            takePhoto();
        }
    }

    private final ActivityResultLauncher<String> requestCameraPermissionLauncher = 
        registerForActivityResult(new ActivityResultContracts.RequestPermission(), isGranted -> {
            if (isGranted) {
                // 权限被授予，拍照
                takePhoto();
            } else {
                // 权限被拒绝
                Toast.makeText(this, "需要相机权限才能拍照", Toast.LENGTH_SHORT).show();
            }
        });

    private void takePhoto() {
        Intent intent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        File photoFile = new File(PhotoStorageManager.getAttendancePhotosDir(this), 
            new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault())
                .format(new Date()) + ".jpg");
            
        currentPhotoUri = FileProvider.getUriForFile(this,
            "com.example.facecheck.fileprovider", photoFile);
            
        intent.putExtra(MediaStore.EXTRA_OUTPUT, currentPhotoUri);
        takePhotoLauncher.launch(intent);
    }

    private void startRecognition() {
        if (currentPhotoUri == null) {
            Toast.makeText(this, "请先拍照", Toast.LENGTH_SHORT).show();
            return;
        }
        
        // 显示进度条
        progressBar.setVisibility(View.VISIBLE);
        btnStartRecognition.setEnabled(false);
        tvStatus.setText("正在使用 " + selectedModel + " 进行识别...");
        
        // 根据选择的模型执行不同的人脸检测逻辑
        if (selectedModel.equals("Google ML Kit (推荐)")) {
            performMLKitFaceDetection();
        } else {
            // 其他模型暂时使用ML Kit作为默认实现
            performMLKitFaceDetection();
        }
    }
    
    /**
     * 使用Google ML Kit进行人脸检测
     */
    private void performMLKitFaceDetection() {
        faceDetectionManager.detectFacesFromUri(currentPhotoUri, new FaceDetectionManager.FaceDetectionCallback() {
            @Override
            public void onSuccess(List<com.google.mlkit.vision.face.Face> faces, List<Bitmap> faceBitmaps) {
                runOnUiThread(() -> {
                    if (faces.isEmpty()) {
                        progressBar.setVisibility(View.GONE);
                        btnStartRecognition.setEnabled(true);
                        tvStatus.setText("未检测到人脸");
                        Toast.makeText(AttendanceActivity.this, "未检测到人脸，请重新拍照", Toast.LENGTH_SHORT).show();
                    } else {
                        // 保存分割后的人脸图像
                        List<String> faceImagePaths = faceDetectionManager.saveFaceBitmaps(faceBitmaps, String.valueOf(sessionId));
                        
                        // 显示人脸分割结果
                        showFaceSegmentationResults(faces, faceBitmaps, faceImagePaths);
                        
                        // 处理每个人脸
                        processDetectedFaces(faces, faceBitmaps, faceImagePaths);
                        
                        progressBar.setVisibility(View.GONE);
                        tvStatus.setText("检测到 " + faces.size() + " 个人脸，正在比对...");
                        
                        // 继续后续的人脸比对流程
                        performFaceRecognition(faces, faceBitmaps, faceImagePaths);
                    }
                });
            }
            
            @Override
            public void onFailure(Exception e) {
                runOnUiThread(() -> {
                    progressBar.setVisibility(View.GONE);
                    btnStartRecognition.setEnabled(true);
                    tvStatus.setText("人脸检测失败");
                    Toast.makeText(AttendanceActivity.this, "人脸检测失败: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
            }
        });
    }
    
    /**
     * 处理检测到的人脸
     */
    private void processDetectedFaces(List<com.google.mlkit.vision.face.Face> faces, List<Bitmap> faceBitmaps, List<String> faceImagePaths) {
        // 对每个人脸进行标准化处理
        for (int i = 0; i < faceBitmaps.size(); i++) {
            Bitmap faceBitmap = faceBitmaps.get(i);
            
            // 标准化人脸图像到224x224
            Bitmap normalizedFace = FaceImageProcessor.normalizeFaceImage(faceBitmap, 224);
            
            // 检查图像质量
            float quality = FaceImageProcessor.calculateImageQuality(normalizedFace);
            if (quality < 0.6f) {
                // 质量较差的人脸，进行增强处理
                normalizedFace = FaceImageProcessor.enhanceImage(normalizedFace, 1.2f, 10f);
            }
            
            // 更新faceBitmaps中的图像
            faceBitmaps.set(i, normalizedFace);
        }
    }
    
    /**
     * 执行人脸识别比对
     */
    private void performFaceRecognition(List<com.google.mlkit.vision.face.Face> faces, List<Bitmap> faceBitmaps, List<String> faceImagePaths) {
        new Thread(() -> {
            try {
                // 使用人脸识别管理器进行批量识别
                List<FaceRecognitionManager.RecognitionResult> results = faceRecognitionManager.recognizeMultipleFaces(faceBitmaps, faces);
                
                // 统计识别结果
                int recognizedCount = 0;
                List<String> recognizedStudentNames = new ArrayList<>();
                
                for (FaceRecognitionManager.RecognitionResult result : results) {
                    if (result.isSuccess()) {
                        recognizedCount++;
                        // 获取学生姓名
                        String studentName = getStudentNameById(result.getStudentId());
                        recognizedStudentNames.add(studentName);
                    }
                }
                
                final int finalRecognizedCount = recognizedCount;
                final List<String> finalRecognizedNames = recognizedStudentNames;
                
                runOnUiThread(() -> {
                    tvStatus.setText("识别完成 - 使用模型: " + selectedModel + "，检测到 " + faces.size() + " 个人脸，识别出 " + finalRecognizedCount + " 个学生");
                    
                    // 跳转到结果界面，传递识别结果
                    Intent intent = new Intent(this, AttendanceResultActivity.class);
                    intent.putExtra("session_id", sessionId);
                    intent.putExtra("detected_faces", faces.size());
                    intent.putExtra("recognized_faces", finalRecognizedCount);
                    intent.putStringArrayListExtra("recognized_names", new ArrayList<>(finalRecognizedNames));
                    startActivity(intent);
                    finish();
                });
            } catch (Exception e) {
                e.printStackTrace();
                runOnUiThread(() -> {
                    progressBar.setVisibility(View.GONE);
                    btnStartRecognition.setEnabled(true);
                    tvStatus.setText("识别失败");
                    Toast.makeText(this, "识别失败: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
            }
        }).start();
    }
    
    /**
     * 显示人脸分割结果
     */
    private void showFaceSegmentationResults(List<com.google.mlkit.vision.face.Face> faces, List<Bitmap> faceBitmaps, List<String> faceImagePaths) {
        // 创建对话框显示分割结果
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("检测到 " + faces.size() + " 个人脸");
        builder.setMessage("是否查看分割后的人脸照片或进行人脸修正？");
        
        builder.setPositiveButton("查看分割", (dialog, which) -> {
            // 显示人脸分割结果界面
            Intent intent = new Intent(this, FaceSegmentationActivity.class);
            intent.putStringArrayListExtra("face_image_paths", new ArrayList<>(faceImagePaths));
            intent.putExtra("face_count", faces.size());
            startActivity(intent);
        });
        
        builder.setNeutralButton("人脸修正", (dialog, which) -> {
            // 显示人脸修正界面（选择第一个检测到的人脸）
            if (!faceImagePaths.isEmpty()) {
                Intent intent = new Intent(this, FaceCorrectionActivity.class);
                intent.putExtra("image_path", faceImagePaths.get(0));
                startActivity(intent);
            }
            dialog.dismiss();
        });
        
        builder.setNegativeButton("继续识别", (dialog, which) -> {
            // 用户选择继续识别流程，不显示分割结果
            dialog.dismiss();
        });
        
        builder.setOnCancelListener(dialog -> {
            // 用户点击对话框外部，显示暂停选项
            showPauseDialog();
        });
        
        AlertDialog dialog = builder.create();
        dialog.show();
    }
    
    /**
     * 显示暂停对话框
     */
    private void showPauseDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("识别已暂停");
        builder.setMessage("识别过程已暂停，您可以选择继续或取消");
        
        builder.setPositiveButton("继续识别", (dialog, which) -> {
            // 继续识别流程
            dialog.dismiss();
        });
        
        builder.setNegativeButton("取消识别", (dialog, which) -> {
            // 取消识别，重置状态
            progressBar.setVisibility(View.GONE);
            btnStartRecognition.setEnabled(true);
            tvStatus.setText("识别已取消");
            dialog.dismiss();
        });
        
        AlertDialog dialog = builder.create();
        dialog.show();
    }
    
    /**
     * 根据学生ID获取学生姓名
     */
    private String getStudentNameById(long studentId) {
        // 从数据库获取学生信息
        android.database.Cursor cursor = dbHelper.getStudentById(studentId);
        if (cursor != null && cursor.moveToFirst()) {
            String name = cursor.getString(cursor.getColumnIndexOrThrow("name"));
            cursor.close();
            return name;
        }
        if (cursor != null) {
            cursor.close();
        }
        return "未知学生";
    }
}