package com.example.facecheck.ui.attendance;

import android.content.Intent;
import android.content.ClipboardManager;
import android.content.ClipData;
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
    // 缓存按 EXIF 方向纠正后的原始整图，用于特征提取
    private Bitmap originalOrientedBitmap;
    private Button btnTakePhoto;
    private Button btnPickImage;
    private Button btnStartRecognition;
    private ProgressBar progressBar;
    private TextView tvStatus;
    private Spinner spinnerModel;
    // 移除检测下拉栏，固定使用 ML Kit 进行检测

    // 模型选择相关：仅保留 MobileFaceNet / FaceNet（均为 float32 新模型）
    private String[] modelOptions = {
            "MobileFaceNet",
            "FaceNet"
    };
    private String selectedModel = "MobileFaceNet";

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
        // 已移除 spinnerDetector

        // 初始化模型选择器
        ArrayAdapter<String> adapter = new ArrayAdapter<>(this,
                android.R.layout.simple_spinner_item, modelOptions);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerModel.setAdapter(adapter);

        spinnerModel.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                selectedModel = modelOptions[position];
                // 将选择同步到识别管理器
                faceRecognitionManager.setSelectedModel(selectedModel);
                Toast.makeText(AttendanceActivity.this,
                        "已选择模型: " + selectedModel, Toast.LENGTH_SHORT).show();
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
                // 不做处理
            }
        });

        // 检测模型固定为 ML Kit，不再提供选择下拉栏

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
                            // 显示预览图（按EXIF方向与合适尺寸加载）并缓存原始位图
                            originalOrientedBitmap = ImageUtils.loadAndResizeBitmap(AttendanceActivity.this,
                                    currentPhotoUri, 1600, 1600);
                            if (originalOrientedBitmap != null) {
                                ivPreview.setImageBitmap(originalOrientedBitmap);
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
                            originalOrientedBitmap = ImageUtils.loadAndResizeBitmap(AttendanceActivity.this,
                                    currentPhotoUri, 1600, 1600);
                            if (originalOrientedBitmap != null) {
                                ivPreview.setImageBitmap(originalOrientedBitmap);
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
                });
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
        if (ContextCompat.checkSelfPermission(this,
                android.Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
            // 请求相机权限
            requestCameraPermissionLauncher.launch(android.Manifest.permission.CAMERA);
        } else {
            // 已有权限，直接拍照
            takePhoto();
        }
    }

    private final ActivityResultLauncher<String> requestCameraPermissionLauncher = registerForActivityResult(
            new ActivityResultContracts.RequestPermission(), isGranted -> {
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
        // 固定使用 ML Kit 进行人脸位置检测（不再展示检测选项）
        tvStatus.setText("正在识别 - 模型: " + selectedModel);
        performMLKitFaceDetection();
    }

    // 删除：YuNet 检测 + MobileFaceNet 嵌入识别流程（已废弃）

    private void showMissingModelDialog(String modelName, String assetPath) {
        String msg = "请将模型文件放入 app/src/main/assets/" + assetPath +
                "\n文件名需完全匹配。";
        new AlertDialog.Builder(this)
                .setTitle(modelName + " 模型未找到")
                .setMessage(msg)
                .setPositiveButton("我知道了", (d, w) -> d.dismiss())
                .setNegativeButton("改用 ML Kit", (d, w) -> {
                    d.dismiss();
                    selectedModel = "MobileFaceNet";
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

    private Bitmap cropFaceWithMargin(Bitmap src, Rect r, float marginRatio) {
        try {
            int cx = r.centerX();
            int cy = r.centerY();
            int w = r.width();
            int h = r.height();
            int margin = (int) (Math.min(w, h) * marginRatio);
            int left = Math.max(0, cx - w / 2 - margin);
            int top = Math.max(0, cy - h / 2 - margin);
            int right = Math.min(src.getWidth(), cx + w / 2 + margin);
            int bottom = Math.min(src.getHeight(), cy + h / 2 + margin);
            int cw = Math.max(1, right - left);
            int ch = Math.max(1, bottom - top);
            return Bitmap.createBitmap(src, left, top, cw, ch);
        } catch (Throwable t) {
            Log.e(TAG, "cropFaceWithMargin failed: " + t.getMessage());
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
                        List<String> faceImagePaths = faceDetectionManager.saveFaceBitmaps(faceBitmaps,
                                String.valueOf(sessionId));

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
     * 使用 YuNet(TFLite) 进行人脸检测，并走统一嵌入与比对流程
     */
    private void performYuNetFaceDetection(boolean singlePrecision) {
        new Thread(() -> {
            try {
                Bitmap src = originalOrientedBitmap;
                if (src == null) {
                    src = ImageUtils.loadAndResizeBitmap(AttendanceActivity.this, currentPhotoUri, 1600, 1600);
                    originalOrientedBitmap = src;
                }
                if (src == null) {
                    runOnUiThread(() -> {
                        progressBar.setVisibility(View.GONE);
                        btnStartRecognition.setEnabled(true);
                        tvStatus.setText("图片加载失败");
                    });
                    return;
                }

                // 资产检查
                final String assetPath = singlePrecision ? "models/yunet_fp32_single.tflite"
                        : "models/yunet_fp16_multi.tflite";
                if (!assetExists(assetPath)) {
                    runOnUiThread(() -> showMissingModelDialog("YuNet TFLite", assetPath));
                    return;
                }

                YuNetTFLiteDetector.ModelVariant variant = singlePrecision
                        ? YuNetTFLiteDetector.ModelVariant.SINGLE_FACE
                        : YuNetTFLiteDetector.ModelVariant.MULTI_FACE;
                YuNetTFLiteDetector detector = new YuNetTFLiteDetector(AttendanceActivity.this, 320, 0.6f, 0.5f,
                        variant);
                List<Rect> rects = detector.detect(src);

                if (rects == null || rects.isEmpty()) {
                    runOnUiThread(() -> {
                        progressBar.setVisibility(View.GONE);
                        btnStartRecognition.setEnabled(true);
                        tvStatus.setText("未检测到人脸(YuNet)");
                        Toast.makeText(AttendanceActivity.this, "未检测到人脸(YuNet)", Toast.LENGTH_SHORT).show();
                    });
                    return;
                }

                // 生成分割位图并保存临时文件
                List<Bitmap> faceBitmaps = new ArrayList<>();
                List<String> facePaths = new ArrayList<>();
                for (int i = 0; i < rects.size(); i++) {
                    Rect r = rects.get(i);
                    Bitmap fb = cropFaceWithMargin(src, r, 0.25f);
                    if (fb != null) {
                        faceBitmaps.add(fb);
                        String p = imageStorageManager.saveTempImage(fb, "yunet_face_" + sessionId + "_" + i + ".jpg");
                        if (p != null)
                            facePaths.add(p);
                    }
                }

                runOnUiThread(() -> {
                    // 简化版分割结果提示
                    showFaceSegmentationResultsSimple(rects.size(), faceBitmaps, facePaths);
                    tvStatus.setText("检测到 " + rects.size() + " 个人脸(YuNet)，正在提取向量...");
                });

                // 提取嵌入向量
                List<float[]> embeddings = new ArrayList<>();
                for (Rect r : rects) {
                    float[] vec = faceRecognitionManager.extractFaceFeatures(src, r);
                    if (vec != null)
                        embeddings.add(vec);
                }

                if (embeddings.isEmpty()) {
                    runOnUiThread(() -> {
                        progressBar.setVisibility(View.GONE);
                        btnStartRecognition.setEnabled(true);
                        tvStatus.setText("向量提取失败(YuNet)");
                        Toast.makeText(AttendanceActivity.this, "向量提取失败(YuNet)", Toast.LENGTH_SHORT).show();
                    });
                    return;
                }

                runOnUiThread(() -> {
                    tvStatus.setText("已生成向量(YuNet)，待确认后继续比对");
                    showEmbeddingsDialog(embeddings, () -> {
                        new Thread(() -> {
                            try {
                                List<FaceRecognitionManager.RecognitionResult> results = faceRecognitionManager
                                        .recognizeImportedVectorsWithinClass(embeddings, classroomId);

                                persistAttendanceResultsForSession(sessionId, results);

                                int recognizedCountTmp = 0;
                                List<String> recognizedNamesTmp = new ArrayList<>();
                                for (FaceRecognitionManager.RecognitionResult r : results) {
                                    if (r.isSuccess()) {
                                        recognizedCountTmp++;
                                        recognizedNamesTmp.add(getStudentNameById(r.getStudentId()));
                                    }
                                }

                                final int finalRecognizedCount = recognizedCountTmp;
                                final ArrayList<String> finalRecognizedNames = new ArrayList<>(recognizedNamesTmp);

                                runOnUiThread(() -> {
                                    tvStatus.setText("识别完成 - 模型: " + selectedModel +
                                            "；检测到 " + rects.size() + " 个人脸，识别出 " + finalRecognizedCount + " 个学生");

                                    Intent intent = new Intent(this, AttendanceResultActivity.class);
                                    intent.putExtra("session_id", sessionId);
                                    intent.putExtra("detected_faces", rects.size());
                                    intent.putExtra("recognized_faces", finalRecognizedCount);
                                    intent.putStringArrayListExtra("recognized_names", finalRecognizedNames);
                                    startActivity(intent);
                                    finish();
                                });
                            } catch (Exception e) {
                                e.printStackTrace();
                                runOnUiThread(() -> {
                                    progressBar.setVisibility(View.GONE);
                                    btnStartRecognition.setEnabled(true);
                                    tvStatus.setText("识别失败(YuNet)");
                                    Toast.makeText(this, "识别失败: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                                });
                            }
                        }).start();
                    });
                });
            } catch (Throwable t) {
                t.printStackTrace();
                runOnUiThread(() -> {
                    progressBar.setVisibility(View.GONE);
                    btnStartRecognition.setEnabled(true);
                    tvStatus.setText("YuNet 检测异常");
                    Toast.makeText(this, "YuNet 检测异常: " + t.getMessage(), Toast.LENGTH_SHORT).show();
                });
            }
        }).start();
    }

    /**
     * 处理检测到的人脸
     */
    private void processDetectedFaces(List<com.google.mlkit.vision.face.Face> faces, List<Bitmap> faceBitmaps,
            List<String> faceImagePaths) {
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
    private void performFaceRecognition(List<com.google.mlkit.vision.face.Face> faces, List<Bitmap> faceBitmaps,
            List<String> faceImagePaths) {
        new Thread(() -> {
            try {
                // 先提取向量用于展示与确认
                List<float[]> embeddings = new ArrayList<>();
                for (int i = 0; i < faces.size(); i++) {
                    // 关键修正：用原始整图 + 检测框坐标系一致进行提取
                    Bitmap src = originalOrientedBitmap;
                    if (src == null) {
                        // 保险：若缓存丢失则即时重新加载
                        src = ImageUtils.loadAndResizeBitmap(AttendanceActivity.this, currentPhotoUri, 1600, 1600);
                        originalOrientedBitmap = src;
                    }
                    float[] vec = (src != null)
                            ? faceRecognitionManager.extractFaceFeatures(src, faces.get(i))
                            : null;
                    if (vec != null) {
                        // 调试日志：检查向量维度、范数与前几维采样，便于与校验页比对
                        float norm2 = 0f;
                        for (float v : vec)
                            norm2 += v * v;
                        float norm = (float) Math.sqrt(norm2);
                        int sampleCount = Math.min(5, vec.length);
                        android.util.Log.d(TAG, "EMB_DEBUG index=" + i +
                                ", dim=" + vec.length +
                                ", norm=" + norm +
                                ", sample=" + java.util.Arrays.toString(java.util.Arrays.copyOf(vec, sampleCount)));
                        embeddings.add(vec);
                    }
                }

                if (embeddings.isEmpty()) {
                    runOnUiThread(() -> {
                        progressBar.setVisibility(View.GONE);
                        btnStartRecognition.setEnabled(true);
                        tvStatus.setText("向量提取失败");
                        Toast.makeText(this, "向量提取失败，请检查图像或模型配置", Toast.LENGTH_SHORT).show();
                    });
                    return;
                }

                runOnUiThread(() -> {
                    tvStatus.setText("已生成向量，待确认后继续比对");
                    showEmbeddingsDialog(embeddings, () -> {
                        // 用户确认后再进行比对（放回子线程执行）
                        new Thread(() -> {
                            try {
                                // 使用通用导入向量的班级内比对方法，直接用已生成的向量
                                List<FaceRecognitionManager.RecognitionResult> results = faceRecognitionManager
                                        .recognizeImportedVectorsWithinClass(embeddings, classroomId);

                                // 将识别结果持久化到数据库（本次会话）
                                persistAttendanceResultsForSession(sessionId, results);

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
                                    tvStatus.setText("识别完成 - 使用模型: " + selectedModel + "，检测到 " + faces.size()
                                            + " 个人脸，识别出 " + finalRecognizedCount + " 个学生");

                                    // 跳转到结果界面，传递识别结果
                                    Intent intent = new Intent(this, AttendanceResultActivity.class);
                                    intent.putExtra("session_id", sessionId);
                                    intent.putExtra("detected_faces", faces.size());
                                    intent.putExtra("recognized_faces", finalRecognizedCount);
                                    intent.putStringArrayListExtra("recognized_names",
                                            new ArrayList<>(finalRecognizedNames));
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
                    });
                });
            } catch (Exception e) {
                e.printStackTrace();
                runOnUiThread(() -> {
                    progressBar.setVisibility(View.GONE);
                    btnStartRecognition.setEnabled(true);
                    tvStatus.setText("向量提取失败");
                    Toast.makeText(this, "向量提取失败: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
            }
        }).start();
    }

    /**
     * 将识别结果写入 AttendanceResult 表：
     * - 识别成功的学生标记为 Present
     * - 本班未被识别到的学生标记为 Absent
     */
    private void persistAttendanceResultsForSession(long sessionId,
            List<FaceRecognitionManager.RecognitionResult> results) {
        try {
            java.util.Set<Long> presentIds = new java.util.HashSet<>();
            for (FaceRecognitionManager.RecognitionResult r : results) {
                if (r.isSuccess()) {
                    presentIds.add(r.getStudentId());
                    dbHelper.insertAttendanceResult(sessionId, r.getStudentId(), "Present", r.getSimilarity(), "AUTO");
                }
            }

            // 将未识别到的本班学生标记为缺勤
            android.database.Cursor cursor = dbHelper.getStudentsByClass(classroomId);
            if (cursor != null && cursor.moveToFirst()) {
                do {
                    long sid = cursor.getLong(cursor.getColumnIndexOrThrow("id"));
                    if (!presentIds.contains(sid)) {
                        dbHelper.insertAttendanceResult(sessionId, sid, "Absent", 0f, "AUTO");
                    }
                } while (cursor.moveToNext());
                cursor.close();
            } else if (cursor != null) {
                cursor.close();
            }
        } catch (Throwable t) {
            android.util.Log.e(TAG, "持久化考勤结果失败: " + t.getMessage(), t);
        }
    }

    /**
     * 显示人脸分割结果
     */
    private void showFaceSegmentationResults(List<com.google.mlkit.vision.face.Face> faces, List<Bitmap> faceBitmaps,
            List<String> faceImagePaths) {
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

            String[] options = { "人脸修正", "人脸增强" };
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
    private void showFaceSegmentationResultsSimple(int faceCount, List<Bitmap> faceBitmaps,
            List<String> faceImagePaths) {
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

            String[] options = { "人脸修正", "人脸增强" };
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
    /**
     * 弹窗展示向量，提供复制 JSON/CSV 的功能，确认后继续比对
     */
    private void showEmbeddingsDialog(List<float[]> embeddings, Runnable onProceed) {
        if (embeddings == null || embeddings.isEmpty()) {
            Toast.makeText(this, "无可展示的向量", Toast.LENGTH_SHORT).show();
            return;
        }

        String previewText = embeddingsToJSON(embeddings);

        android.widget.ScrollView scrollView = new android.widget.ScrollView(this);
        android.widget.TextView tv = new android.widget.TextView(this);
        tv.setText(previewText);
        tv.setTextSize(12f);
        tv.setTextIsSelectable(true);
        int padding = (int) (12 * getResources().getDisplayMetrics().density);
        tv.setPadding(padding, padding, padding, padding);
        scrollView.addView(tv);

        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("向量预览 (" + embeddings.size() + " 张人脸)");
        builder.setView(scrollView);
        builder.setCancelable(false);

        // 先定义按钮，但在显示后重写点击逻辑，避免复制按钮关闭弹窗
        builder.setNegativeButton("复制 CSV", (d, w) -> {
        });
        builder.setNeutralButton("复制 JSON", (d, w) -> {
        });
        builder.setPositiveButton("继续比对", (d, w) -> {
        });

        builder.setOnCancelListener(d -> {
            // 用户取消则终止当前流程
            progressBar.setVisibility(View.GONE);
            btnStartRecognition.setEnabled(true);
            tvStatus.setText("已取消比对");
        });

        AlertDialog dialog = builder.create();
        dialog.setOnShowListener(l -> {
            android.widget.Button btnCsv = dialog.getButton(AlertDialog.BUTTON_NEGATIVE);
            android.widget.Button btnJson = dialog.getButton(AlertDialog.BUTTON_NEUTRAL);
            android.widget.Button btnGo = dialog.getButton(AlertDialog.BUTTON_POSITIVE);

            if (btnCsv != null) {
                btnCsv.setOnClickListener(v -> {
                    copyToClipboard("embeddings_csv", embeddingsToCSV(embeddings));
                    Toast.makeText(this, "已复制 CSV，可继续点击\"继续比对\"", Toast.LENGTH_SHORT).show();
                });
            }

            if (btnJson != null) {
                btnJson.setOnClickListener(v -> {
                    copyToClipboard("embeddings_json", embeddingsToJSON(embeddings));
                    Toast.makeText(this, "已复制 JSON，可继续点击\"继续比对\"", Toast.LENGTH_SHORT).show();
                });
            }

            if (btnGo != null) {
                btnGo.setOnClickListener(v -> {
                    dialog.dismiss();
                    if (onProceed != null)
                        onProceed.run();
                });
            }
        });

        dialog.show();
    }

    private void copyToClipboard(String label, String text) {
        ClipboardManager cm = (ClipboardManager) getSystemService(CLIPBOARD_SERVICE);
        if (cm != null) {
            cm.setPrimaryClip(ClipData.newPlainText(label, text));
        }
    }

    private String embeddingsToJSON(List<float[]> embeddings) {
        StringBuilder sb = new StringBuilder();
        sb.append("[");
        for (int i = 0; i < embeddings.size(); i++) {
            float[] vec = embeddings.get(i);
            sb.append("[");
            for (int j = 0; j < vec.length; j++) {
                sb.append(String.format(java.util.Locale.US, "%.6f", vec[j]));
                if (j < vec.length - 1)
                    sb.append(", ");
            }
            sb.append("]");
            if (i < embeddings.size() - 1)
                sb.append(",\n");
        }
        sb.append("]");
        return sb.toString();
    }

    private String embeddingsToCSV(List<float[]> embeddings) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < embeddings.size(); i++) {
            float[] vec = embeddings.get(i);
            for (int j = 0; j < vec.length; j++) {
                sb.append(String.format(java.util.Locale.US, "%.6f", vec[j]));
                if (j < vec.length - 1)
                    sb.append(",");
            }
            if (i < embeddings.size() - 1)
                sb.append("\n");
        }
        return sb.toString();
    }

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
