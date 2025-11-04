package com.example.facecheck.ui.attendance;

import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.net.Uri;
import android.graphics.Rect;
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
import android.util.Log;

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
import com.example.facecheck.ui.face.FaceEnhancementActivity;

import java.util.ArrayList;
import com.example.facecheck.utils.ImageStorageManager;
import com.example.facecheck.utils.CacheManager;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.io.IOException;

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
    private Button btnPickImage;
    private Button btnStartRecognition;
    private ProgressBar progressBar;
    private TextView tvStatus;
    private Spinner spinnerModel;
    
    // 模型选择相关
    private String[] modelOptions = {
        "YuNet + MobileFaceNet (TFLite)",
        "Google ML Kit (推荐)",
        "TensorFlow Lite + FaceNet",
        "OpenCV DNN"
    };
    private String selectedModel = "YuNet + MobileFaceNet (TFLite)";
    
    private ActivityResultLauncher<Intent> takePhotoLauncher;
    private ActivityResultLauncher<String> pickImageLauncher;

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
        btnPickImage = findViewById(R.id.btnPickImage);
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
        btnPickImage.setOnClickListener(v -> pickImageFromGallery());
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
                        // 显示预览图（按EXIF方向与合适尺寸加载）
                        Bitmap bitmap = ImageUtils.loadAndResizeBitmap(AttendanceActivity.this, currentPhotoUri, 1600, 1600);
                        if (bitmap != null) {
                            ivPreview.setImageBitmap(bitmap);
                        } else {
                            Toast.makeText(this, "预览图加载失败", Toast.LENGTH_SHORT).show();
                        }
                        
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

        // 选择已有图片
        pickImageLauncher = registerForActivityResult(
            new ActivityResultContracts.GetContent(),
            uri -> {
                if (uri != null) {
                    try {
                        currentPhotoUri = uri;
                        Bitmap bitmap = ImageUtils.loadAndResizeBitmap(AttendanceActivity.this, currentPhotoUri, 1600, 1600);
                        if (bitmap != null) {
                            ivPreview.setImageBitmap(bitmap);
                        } else {
                            Toast.makeText(this, "预览图加载失败", Toast.LENGTH_SHORT).show();
                        }

                        long photoId = dbHelper.insertPhotoAsset(sessionId, null, "RAW",
                            currentPhotoUri.toString(), "Imported from gallery");
                        if (photoId != -1) {
                            dbHelper.insertSyncLog("PhotoAsset", photoId, "INSERT",
                                System.currentTimeMillis(), "PENDING");
                            btnStartRecognition.setEnabled(true);
                        }
                    } catch (Exception e) {
                        e.printStackTrace();
                        Toast.makeText(this, "导入图片失败", Toast.LENGTH_SHORT).show();
                    }
                }
            }
        );
    }

    private void pickImageFromGallery() {
        try {
            pickImageLauncher.launch("image/*");
        } catch (Exception e) {
            e.printStackTrace();
            Toast.makeText(this, "无法打开图库", Toast.LENGTH_SHORT).show();
        }
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
            Toast.makeText(this, "请先拍照或导入图片", Toast.LENGTH_SHORT).show();
            return;
        }
        
        // 显示进度条
        progressBar.setVisibility(View.VISIBLE);
        btnStartRecognition.setEnabled(false);
        tvStatus.setText("正在使用 " + selectedModel + " 进行识别...");
        
        // 根据选择的模型执行不同的人脸检测逻辑
        if (selectedModel.equals("YuNet + MobileFaceNet (TFLite)")) {
            performYuNetMobileFaceNetRecognition();
        } else if (selectedModel.equals("Google ML Kit (推荐)")) {
            performMLKitFaceDetection();
        } else {
            // 其他模型暂时使用ML Kit作为默认实现
            performMLKitFaceDetection();
        }
    }

    /**
     * 使用 YuNet 检测 + MobileFaceNet 嵌入进行识别
     */
    private void performYuNetMobileFaceNetRecognition() {
        try {
            Bitmap source = ImageUtils.loadAndResizeBitmap(AttendanceActivity.this, currentPhotoUri, 1600, 1600);
            if (source == null) {
                progressBar.setVisibility(View.GONE);
                btnStartRecognition.setEnabled(true);
                tvStatus.setText("图片加载失败");
                Toast.makeText(this, "图片加载失败", Toast.LENGTH_SHORT).show();
                return;
            }

            // 仅使用 YuNet TFLite（多人脸）进行检测
            List<Rect> rects;
            YuNetTFLiteDetector detector = new YuNetTFLiteDetector(
                this,
                320,
                0.6f,
                0.5f,
                YuNetTFLiteDetector.ModelVariant.MULTI_FACE
            );
            if (!detector.isReady()) {
                progressBar.setVisibility(View.GONE);
                btnStartRecognition.setEnabled(true);
                tvStatus.setText("YuNet 模型未就绪，无法检测");
                showMissingModelDialog("YuNet(fp16, 多人)", "models/yunet_fp16_multi.tflite");
                return;
            }
            rects = detector.detect(source);
            if (rects == null || rects.isEmpty()) {
                progressBar.setVisibility(View.GONE);
                btnStartRecognition.setEnabled(true);
                tvStatus.setText("未检测到人脸");
                Toast.makeText(this, "未检测到人脸，请调整角度或光线", Toast.LENGTH_SHORT).show();
                return;
            }

            // 裁剪人脸位图
            List<Bitmap> faceBitmaps = new ArrayList<>();
            for (Rect r : rects) {
                Bitmap fb = cropFace(source, r);
                if (fb != null) faceBitmaps.add(fb);
            }
            if (faceBitmaps.isEmpty()) {
                progressBar.setVisibility(View.GONE);
                btnStartRecognition.setEnabled(true);
                tvStatus.setText("人脸裁剪失败");
                Toast.makeText(this, "人脸裁剪失败", Toast.LENGTH_SHORT).show();
                return;
            }

            // 保存分割后的人脸图像（沿用已有工具）
            List<String> faceImagePaths = faceDetectionManager.saveFaceBitmaps(faceBitmaps, String.valueOf(sessionId));
            // YuNet 路径不依赖 ML Kit Face 对象，使用简化版提示
            showFaceSegmentationResultsSimple(faceBitmaps.size(), faceBitmaps, faceImagePaths);

            // 嵌入与识别放到子线程
            new Thread(() -> {
                try {
                    MobileFaceNetEmbedder embedder = new MobileFaceNetEmbedder(AttendanceActivity.this);
                    if (embedder == null) {
                        // 防御性：不应为 null，但保留检查
                        runOnUiThread(() -> {
                            progressBar.setVisibility(View.GONE);
                            btnStartRecognition.setEnabled(true);
                            tvStatus.setText("嵌入器创建失败");
                            Toast.makeText(AttendanceActivity.this, "嵌入器创建失败", Toast.LENGTH_SHORT).show();
                        });
                        return;
                    }
                    // 通过首次调用 embed() 前先检查模型是否加载成功
                    if (embedder.embed(Bitmap.createBitmap(112, 112, Bitmap.Config.ARGB_8888)) == null) {
                        runOnUiThread(() -> {
                            progressBar.setVisibility(View.GONE);
                            btnStartRecognition.setEnabled(true);
                            tvStatus.setText("MobileFaceNet 模型未就绪，无法识别");
                            showMissingModelDialog("MobileFaceNet", "models/mobilefacenet.tflite");
                        });
                        return;
                    }
                    // 在生成嵌入前对每张人脸进行质量评估与修复增强
                    List<Bitmap> enhancedBitmaps = new ArrayList<>(faceBitmaps.size());
                    for (int i = 0; i < faceBitmaps.size(); i++) {
                        Bitmap fb = faceBitmaps.get(i);
                        float quality = FaceImageProcessor.calculateImageQuality(fb);
                        Bitmap processed = fb;
                        if (quality < 0.6f) {
                            Bitmap repaired = FaceImageProcessor.repairFaceImage(fb);
                            if (repaired != null) {
                                processed = repaired;
                                // 缓存增强结果用于查看或调试
                                String enhancedPath = imageStorageManager.saveTempImage(processed,
                                        "enhanced_face_" + sessionId + "_" + i + ".jpg");
                                if (enhancedPath != null) {
                                    Log.d(TAG, "YuNet: enhanced face saved: " + enhancedPath);
                                }
                            }
                        }
                        enhancedBitmaps.add(processed);
                    }

                    // 对增强后的人脸位图生成嵌入
                    List<float[]> embeddings = new ArrayList<>();
                    for (Bitmap eb : enhancedBitmaps) {
                        float[] vec = embedder.embed(eb);
                        if (vec != null) embeddings.add(vec);
                    }

                    if (embeddings.isEmpty()) {
                        runOnUiThread(() -> {
                            progressBar.setVisibility(View.GONE);
                            btnStartRecognition.setEnabled(true);
                            tvStatus.setText("嵌入生成失败");
                            Toast.makeText(AttendanceActivity.this, "嵌入生成失败，检查模型文件", Toast.LENGTH_SHORT).show();
                        });
                        return;
                    }

                    List<FaceRecognitionManager.RecognitionResult> results = faceRecognitionManager.recognizeMultipleEmbeddings(embeddings);

                    int recognizedCount = 0;
                    List<String> recognizedStudentNames = new ArrayList<>();
                    for (FaceRecognitionManager.RecognitionResult result : results) {
                        if (result.isSuccess()) {
                            recognizedCount++;
                            String studentName = getStudentNameById(result.getStudentId());
                            recognizedStudentNames.add(studentName);
                        }
                    }

                    final int finalDetectedCount = faceBitmaps.size();
                    final int finalRecognizedCount = recognizedCount;
                    final ArrayList<String> finalRecognizedNames = new ArrayList<>(recognizedStudentNames);
                    runOnUiThread(() -> {
                        tvStatus.setText("识别完成 - 使用模型: " + selectedModel + "，检测到 " + finalDetectedCount + " 个人脸，识别出 " + finalRecognizedCount + " 个学生");
                        Intent intent = new Intent(this, AttendanceResultActivity.class);
                        intent.putExtra("session_id", sessionId);
                        intent.putExtra("detected_faces", finalDetectedCount);
                        intent.putExtra("recognized_faces", finalRecognizedCount);
                        intent.putStringArrayListExtra("recognized_names", finalRecognizedNames);
                        startActivity(intent);
                        finish();
                    });
                } catch (Exception e) {
                    Log.e(TAG, "YuNet+MFN 识别失败: " + e.getMessage(), e);
                    runOnUiThread(() -> {
                        progressBar.setVisibility(View.GONE);
                        btnStartRecognition.setEnabled(true);
                        tvStatus.setText("识别失败");
                        Toast.makeText(this, "识别失败: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    });
                }
            }).start();

        } catch (Throwable t) {
            Log.e(TAG, "YuNet+MFN 流程异常: " + t.getMessage(), t);
            progressBar.setVisibility(View.GONE);
            btnStartRecognition.setEnabled(true);
            tvStatus.setText("识别流程异常");
            Toast.makeText(this, "识别流程异常: " + t.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }

    private void showMissingModelDialog(String modelName, String assetPath) {
        String msg = "请将模型文件放入 app/src/main/assets/" + assetPath +
            "\n文件名需完全匹配。";
        new AlertDialog.Builder(this)
            .setTitle(modelName + " 模型未找到")
            .setMessage(msg)
            .setPositiveButton("我知道了", (d, w) -> d.dismiss())
            .setNegativeButton("改用 ML Kit", (d, w) -> {
                d.dismiss();
                selectedModel = "Google ML Kit (推荐)";
                performMLKitFaceDetection();
            })
            .show();
    }

    private boolean assetExists(String assetPath) {
        try {
            getAssets().open(assetPath).close();
            return true;
        } catch (IOException e) {
            return false;
        }
    }

    private Bitmap cropFace(Bitmap src, Rect r) {
        try {
            int left = Math.max(0, r.left);
            int top = Math.max(0, r.top);
            int right = Math.min(src.getWidth(), r.right);
            int bottom = Math.min(src.getHeight(), r.bottom);
            int w = Math.max(1, right - left);
            int h = Math.max(1, bottom - top);
            return Bitmap.createBitmap(src, left, top, w, h);
        } catch (Throwable t) {
            Log.e(TAG, "cropFace failed: " + t.getMessage());
            return null;
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
                
                // 保存增强后的图像用于后续对比
                String enhancedImagePath = imageStorageManager.saveTempImage(normalizedFace, 
                    "enhanced_face_" + sessionId + "_" + i + ".jpg");
                
                if (enhancedImagePath != null) {
                    Log.d(TAG, "Enhanced face image saved: " + enhancedImagePath);
                }
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
        builder.setMessage("是否查看分割后的人脸照片或进行人脸修复/增强？");

        // 查看分割
        builder.setNegativeButton("查看分割", (dialog, which) -> {
            Intent intent = new Intent(this, FaceSegmentationActivity.class);
            intent.putStringArrayListExtra("face_image_paths", new ArrayList<>(faceImagePaths));
            intent.putExtra("face_count", faces.size());
            startActivity(intent);
        });

        // 人脸修复/增强（子菜单）
        builder.setNeutralButton("人脸处理", (dialog, which) -> {
            if (faceImagePaths.isEmpty()) {
                Toast.makeText(this, "无可处理的人脸图片", Toast.LENGTH_SHORT).show();
                return;
            }
            String imagePath = faceImagePaths.get(0);

            String[] options = {"人脸修正", "人脸增强"};
            new AlertDialog.Builder(this)
                .setTitle("选择处理方式")
                .setItems(options, (d, idx) -> {
                    Intent intent;
                    if (idx == 0) {
                        intent = new Intent(this, FaceCorrectionActivity.class);
                    } else {
                        intent = new Intent(this, FaceEnhancementActivity.class);
                    }
                    intent.putExtra("image_path", imagePath);
                    startActivity(intent);
                })
                .show();
        });

        // 继续识别
        builder.setPositiveButton("继续识别", (dialog, which) -> {
            dialog.dismiss();
        });

        builder.setOnCancelListener(dialog -> {
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
     * 简化版分割结果提示（YuNet 路径）
     */
    private void showFaceSegmentationResultsSimple(int faceCount, List<Bitmap> faceBitmaps, List<String> faceImagePaths) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("检测到 " + faceCount + " 个人脸");
        builder.setMessage("是否查看分割后的人脸照片或进行人脸修复/增强？");

        builder.setNegativeButton("查看分割", (dialog, which) -> {
            Intent intent = new Intent(this, FaceSegmentationActivity.class);
            intent.putStringArrayListExtra("face_image_paths", new ArrayList<>(faceImagePaths));
            intent.putExtra("face_count", faceCount);
            startActivity(intent);
        });

        builder.setNeutralButton("人脸处理", (dialog, which) -> {
            if (faceImagePaths.isEmpty()) {
                Toast.makeText(this, "无可处理的人脸图片", Toast.LENGTH_SHORT).show();
                return;
            }
            String imagePath = faceImagePaths.get(0);

            String[] options = {"人脸修正", "人脸增强"};
            new AlertDialog.Builder(this)
                .setTitle("选择处理方式")
                .setItems(options, (d, idx) -> {
                    Intent intent;
                    if (idx == 0) {
                        intent = new Intent(this, FaceCorrectionActivity.class);
                    } else {
                        intent = new Intent(this, FaceEnhancementActivity.class);
                    }
                    intent.putExtra("image_path", imagePath);
                    startActivity(intent);
                })
                .show();
        });

        builder.setPositiveButton("继续识别", (dialog, which) -> dialog.dismiss());
        builder.setOnCancelListener(dialog -> showPauseDialog());

        builder.create().show();
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