package com.example.facecheck.ui.classroom;

import com.example.facecheck.utils.FaceRecognitionManager;
import com.example.facecheck.utils.FaceDetectionManager;
import com.example.facecheck.utils.FaceImageProcessor;

import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.Spinner;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Toast;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.core.content.FileProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.example.facecheck.R;
import com.example.facecheck.adapters.StudentAdapter;
import com.example.facecheck.database.DatabaseHelper;
import com.example.facecheck.data.model.Student;
import com.example.facecheck.sync.SyncManager;
import com.example.facecheck.ui.attendance.AttendanceActivity;
import com.example.facecheck.utils.PhotoStorageManager;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.snackbar.Snackbar;
import com.airbnb.lottie.LottieAnimationView;
import com.airbnb.lottie.LottieDrawable;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.FileOutputStream;
import java.io.OutputStreamWriter;
import java.io.BufferedWriter;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

public class ClassroomActivity extends AppCompatActivity {
    private static final String TAG = "ClassroomActivity";
    
    private DatabaseHelper dbHelper;
    private StudentAdapter studentAdapter;
    private long classroomId;
    private Uri currentPhotoUri;
    private File currentPhotoFile;
    private ImageView dialogAvatarImageView;
    
    private RecyclerView recyclerView;
    private FloatingActionButton fabAddStudent;
    private Button btnExtractAll;
    private Spinner spinnerModel;
    // Lottie 覆盖层
    private android.widget.FrameLayout lottieOverlayContainer;
    private LottieAnimationView lottieView;
    private boolean hadFailure = false;

    private FaceDetectionManager faceDetectionManager;
    private FaceRecognitionManager faceRecognitionManager;

    // 批量处理状态
    private List<Student> studentsWithAvatar = new ArrayList<>();
    private int processIndex = 0;
    // 独立后台执行器：串行处理，防止阻塞主线程和池耗尽
    private ThreadPoolExecutor featureExecutor;
    
    private ActivityResultLauncher<Intent> takePhotoLauncher;
    private ActivityResultLauncher<String> pickPhotoLauncher;
    private Student currentStudent;

    // 特征模型选择
    private String[] modelOptions = {
        "MobileFaceNet",
        "FaceNet"
    };
    private String selectedModel = "MobileFaceNet";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_classroom);
        
        // 获取班级ID
        classroomId = getIntent().getLongExtra("classroom_id", -1);
        if (classroomId == -1) {
            Toast.makeText(this, "班级信息无效", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }
        
        // 初始化数据库
        dbHelper = new DatabaseHelper(this);

        // 初始化人脸相关管理器
        faceDetectionManager = new FaceDetectionManager(this);
        faceRecognitionManager = new FaceRecognitionManager(this);

        // 初始化受限大小的后台线程池，避免与第三方库共享导致饥饿
        featureExecutor = new ThreadPoolExecutor(
                1, // 单线程串行，避免并发写数据库/文件
                1,
                30, TimeUnit.SECONDS,
                new LinkedBlockingQueue<>(8), // 有界队列，防止无限堆积
                new ThreadPoolExecutor.DiscardOldestPolicy() // 丢弃最旧任务以维持响应
        );
        
        // 初始化视图
        initViews();
        
        // 初始化照片选择器
        initPhotoLaunchers();
        
        // 加载学生列表
        loadStudents();
    }

    private void initViews() {
        recyclerView = findViewById(R.id.recyclerViewStudents);
        fabAddStudent = findViewById(R.id.fabAddStudent);
        btnExtractAll = findViewById(R.id.btnExtractAll);
        spinnerModel = findViewById(R.id.spinnerModel);
        lottieOverlayContainer = findViewById(R.id.lottieOverlayContainer);
        lottieView = findViewById(R.id.lottieView);
        
        // 设置RecyclerView
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        studentAdapter = new StudentAdapter(new ArrayList<>());
        recyclerView.setAdapter(studentAdapter);
        
        // 初始化模型选择器
        if (spinnerModel != null) {
            ArrayAdapter<String> adapter = new ArrayAdapter<>(this,
                android.R.layout.simple_spinner_item, modelOptions);
            adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
            spinnerModel.setAdapter(adapter);
            spinnerModel.setOnItemSelectedListener(new android.widget.AdapterView.OnItemSelectedListener() {
                @Override
                public void onItemSelected(android.widget.AdapterView<?> parent, View view, int position, long id) {
                    selectedModel = modelOptions[position];
                    faceRecognitionManager.setSelectedModel(selectedModel);
                    Toast.makeText(ClassroomActivity.this, "已选择模型: " + selectedModel, Toast.LENGTH_SHORT).show();
                }

                @Override
                public void onNothingSelected(android.widget.AdapterView<?> parent) { }
            });
            // 默认选择
            faceRecognitionManager.setSelectedModel(selectedModel);
        }

        // 设置点击事件
        fabAddStudent.setOnClickListener(v -> showAddStudentDialog());
        if (btnExtractAll != null) {
            btnExtractAll.setOnClickListener(v -> {
                showBinaryScanOverlay();
                startBatchExtraction();
            });
        }
        
        // 设置学生点击事件
        studentAdapter.setOnItemClickListener(student -> showStudentDetailsDialog(student));
    }

    // ============= 批量特征提取入口 =============
    private void startBatchExtraction() {
        Cursor cursor = dbHelper.getStudentsByClass(classroomId);
        studentsWithAvatar.clear();
        processIndex = 0;
        hadFailure = false;

        if (cursor != null && cursor.moveToFirst()) {
            do {
                long id = cursor.getLong(cursor.getColumnIndexOrThrow("id"));
                String name = cursor.getString(cursor.getColumnIndexOrThrow("name"));
                String sid = cursor.getString(cursor.getColumnIndexOrThrow("sid"));
                String gender = cursor.getString(cursor.getColumnIndexOrThrow("gender"));
                String avatarUri = cursor.getString(cursor.getColumnIndexOrThrow("avatarUri"));
                long createdAt = cursor.getLong(cursor.getColumnIndexOrThrow("createdAt"));

                if (!TextUtils.isEmpty(avatarUri)) {
                    Student s = new Student(id, classroomId, name, sid, gender, avatarUri, createdAt);
                    studentsWithAvatar.add(s);
                }
            } while (cursor.moveToNext());
            cursor.close();
        }

        if (studentsWithAvatar.isEmpty()) {
            Toast.makeText(this, "该班级暂无带头像的学生", Toast.LENGTH_SHORT).show();
            return;
        }

        Snackbar.make(recyclerView, "开始批量提取，共 " + studentsWithAvatar.size() + " 人", Snackbar.LENGTH_SHORT).show();
        processNextStudent();
    }

    private void processNextStudent() {
        if (processIndex >= studentsWithAvatar.size()) {
            Snackbar.make(recyclerView, "批量提取完成", Snackbar.LENGTH_LONG).show();
            // 成功则退出 Lottie，否则不退出（按需求）
            if (!hadFailure) {
                hideLottieOverlay();
            }
            return;
        }

        Student student = studentsWithAvatar.get(processIndex);
        processIndex++;

        if (TextUtils.isEmpty(student.getAvatarUri())) {
            processNextStudent();
            return;
        }

        Uri avatar = Uri.parse(student.getAvatarUri());
        try {
            Bitmap original = MediaStore.Images.Media.getBitmap(getContentResolver(), avatar);
            // 检测人脸
            faceDetectionManager.detectFacesFromBitmap(original, new FaceDetectionManager.FaceDetectionCallback() {
                @Override
                public void onSuccess(List<com.google.mlkit.vision.face.Face> faces, List<Bitmap> faceBitmaps) {
                    // 在后台线程执行特征提取与后续存储逻辑，避免阻塞主线程
                    featureExecutor.execute(() -> {
                        try {
                            if (faces == null || faces.isEmpty()) {
                                runOnUiThread(() -> {
                                    Toast.makeText(ClassroomActivity.this, student.getName() + "：未检测到人脸，跳过", Toast.LENGTH_SHORT).show();
                                    hadFailure = true;
                                    processNextStudent();
                                });
                                return;
                            }

                            // 质量评估（使用提取的第一个人脸位图）
                            float quality = 0.0f;
                            if (faceBitmaps != null && !faceBitmaps.isEmpty()) {
                                Bitmap faceBmp = FaceImageProcessor.normalizeFaceImage(faceBitmaps.get(0), 224);
                                quality = FaceImageProcessor.calculateImageQuality(faceBmp);
                            }

                            // 提取传统增强特征（256维，内部会进行裁剪与增强）
                            float[] features = faceRecognitionManager.extractFaceFeatures(original, faces.get(0));
                            if (features == null || features.length == 0) {
                                runOnUiThread(() -> {
                                    Toast.makeText(ClassroomActivity.this, student.getName() + "：特征提取失败", Toast.LENGTH_SHORT).show();
                                    hadFailure = true;
                                    processNextStudent();
                                });
                                return;
                            }

                            

                            // 已存在则询问是否更新（UI 交互放到主线程）
                            final Student targetStudent = student;
                            final float[] targetFeatures = features;
                            final float targetQuality = quality;

                            List<com.example.facecheck.data.model.FaceEmbedding> existing = faceRecognitionManager.getStudentFaceEmbeddings(targetStudent.getId());
                            if (existing != null && !existing.isEmpty()) {
                                runOnUiThread(() -> new AlertDialog.Builder(ClassroomActivity.this)
                                        .setTitle("更新确认")
                                        .setMessage("学生 " + targetStudent.getName() + " 已有人脸特征，是否更新为新结果？")
                                        .setPositiveButton("更新", (d, w) -> {
                                            boolean ok = updateFaceEmbeddingRecord(targetStudent.getId(), targetFeatures, targetQuality);
                                            if (ok) {
                                                appendEmbeddingJson(targetStudent.getId(), targetFeatures, targetQuality);
                                                Toast.makeText(ClassroomActivity.this, targetStudent.getName() + "：已更新", Toast.LENGTH_SHORT).show();
                                            } else {
                                                Toast.makeText(ClassroomActivity.this, targetStudent.getName() + "：更新失败", Toast.LENGTH_SHORT).show();
                                                hadFailure = true;
                                            }
                                            processNextStudent();
                                        })
                                        .setNegativeButton("跳过", (d, w) -> processNextStudent())
                                        .setCancelable(false)
                                        .show());
                            } else {
                                boolean saved = faceRecognitionManager.saveFaceEmbedding(targetStudent.getId(), targetFeatures, targetQuality, null);
                                runOnUiThread(() -> {
                                    if (saved) {
                                        appendEmbeddingJson(targetStudent.getId(), targetFeatures, targetQuality);
                                        Toast.makeText(ClassroomActivity.this, targetStudent.getName() + "：已保存", Toast.LENGTH_SHORT).show();
                                    } else {
                                        Toast.makeText(ClassroomActivity.this, targetStudent.getName() + "：保存失败", Toast.LENGTH_SHORT).show();
                                        hadFailure = true;
                                    }
                                    processNextStudent();
                                });
                            }
                        } catch (Throwable t) {
                            runOnUiThread(() -> {
                                Toast.makeText(ClassroomActivity.this, student.getName() + "：特征提取异常(" + t.getMessage() + ")", Toast.LENGTH_SHORT).show();
                                hadFailure = true;
                                processNextStudent();
                            });
                        }
                    });
                }

                @Override
                public void onFailure(Exception e) {
                    runOnUiThread(() -> {
                        Toast.makeText(ClassroomActivity.this, student.getName() + "：人脸检测失败(" + e.getMessage() + ")", Toast.LENGTH_SHORT).show();
                        hadFailure = true;
                        processNextStudent();
                    });
                }
            });
        } catch (IOException e) {
            Toast.makeText(this, student.getName() + "：头像读取失败", Toast.LENGTH_SHORT).show();
            hadFailure = true;
            processNextStudent();
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (featureExecutor != null) {
            featureExecutor.shutdownNow();
        }
    }

    private void showBinaryScanOverlay() {
        if (lottieOverlayContainer != null && lottieView != null) {
            lottieView.setAnimation("lottie/Binary Scan.json");
            lottieView.setRepeatCount(LottieDrawable.INFINITE);
            lottieOverlayContainer.setVisibility(View.VISIBLE);
            lottieView.playAnimation();
        }
    }

    private void hideLottieOverlay() {
        if (lottieOverlayContainer != null && lottieView != null) {
            try {
                lottieView.cancelAnimation();
            } catch (Throwable ignored) {}
            lottieOverlayContainer.setVisibility(View.GONE);
        }
    }

    private boolean updateFaceEmbeddingRecord(long studentId, float[] features, float quality) {
        try {
            // 仅更新当前所选模型版本的记录，避免跨模型混用
            String currentVer = faceRecognitionManager.getCurrentModelVersion();
            android.database.Cursor c = dbHelper.getFaceEmbeddingsByStudent(studentId);
            if (c != null && c.moveToFirst()) {
                long targetId = -1;
                do {
                    long id = c.getLong(c.getColumnIndexOrThrow("id"));
                    String modelVer = c.getString(c.getColumnIndexOrThrow("modelVer"));
                    if (currentVer != null && currentVer.equals(modelVer)) {
                        targetId = id;
                        break; // 找到当前模型的第一条记录即可
                    }
                } while (c.moveToNext());
                c.close();
                if (targetId != -1) {
                    byte[] vectorBytes = faceRecognitionManager.floatArrayToByteArray(features);
                    return dbHelper.updateFaceEmbeddingById(targetId, vectorBytes, quality);
                }
            }
            // 没有记录则插入
            return faceRecognitionManager.saveFaceEmbedding(studentId, features, quality, null);
        } catch (Exception ex) {
            ex.printStackTrace();
            return false;
        }
    }

    // 以JSON形式追加到 schema.sql（开发资产文件）
    private void appendEmbeddingJson(long studentId, float[] features, float quality) {
        try {
            // 注意：生产环境中assets是只读；此处仅作为开发阶段生成插入语句
            String path = "d:/typer/android_demo/FaceCheck/app/src/main/assets/schema.sql";
            BufferedWriter bw = new BufferedWriter(new OutputStreamWriter(new java.io.FileOutputStream(path, true), StandardCharsets.UTF_8));
            StringBuilder sb = new StringBuilder();
            sb.append("\n-- FaceEmbedding JSON export for student ").append(studentId).append("\n");
            sb.append("/* vector_json: ");
            sb.append("[");
            for (int i = 0; i < features.length; i++) {
                sb.append(String.format(java.util.Locale.US, "%.6f", features[i]));
                if (i < features.length - 1) sb.append(",");
            }
            sb.append("]");
            sb.append(" */\n");
            sb.append("-- quality: ").append(String.format(java.util.Locale.US, "%.4f", quality)).append("\n");
            bw.write(sb.toString());
            bw.flush();
            bw.close();
        } catch (Exception e) {
            // 静默失败：在设备运行时可能不可写
        }
    }

    private void initPhotoLaunchers() {
        // 拍照
        takePhotoLauncher = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            result -> {
                if (result.getResultCode() == RESULT_OK && currentPhotoUri != null) {
                    processNewPhoto(currentPhotoUri);
                }
            });
            
        // 选择照片
        pickPhotoLauncher = registerForActivityResult(
            new ActivityResultContracts.GetContent(),
            uri -> {
                if (uri != null) {
                    // 复制选择的图片到内部存储
                    copyPickedPhotoToInternalStorage(uri);
                    // 处理新的照片
                    processNewPhoto(currentPhotoUri);
                }
            });
    }

    private void loadStudents() {
        Cursor cursor = dbHelper.getStudentsByClass(classroomId);
        List<Student> students = new ArrayList<>();
        
        if (cursor != null && cursor.moveToFirst()) {
            do {
                long id = cursor.getLong(cursor.getColumnIndexOrThrow("id"));
                String name = cursor.getString(cursor.getColumnIndexOrThrow("name"));
                String sid = cursor.getString(cursor.getColumnIndexOrThrow("sid"));
                String gender = cursor.getString(cursor.getColumnIndexOrThrow("gender"));
                String avatarUri = cursor.getString(cursor.getColumnIndexOrThrow("avatarUri"));
                long createdAt = cursor.getLong(cursor.getColumnIndexOrThrow("createdAt"));
                
                Student student = new Student(id, classroomId, name, sid, gender, avatarUri, createdAt);
                students.add(student);
            } while (cursor.moveToNext());
            cursor.close();
        }
        
        studentAdapter.updateStudents(students);
    }

    private void showAddStudentDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        View view = LayoutInflater.from(this).inflate(R.layout.dialog_add_student, null);
        
        EditText etName = view.findViewById(R.id.etStudentName);
        EditText etSid = view.findViewById(R.id.etStudentId);
        EditText etGender = view.findViewById(R.id.etGender);
        ImageView ivAvatar = view.findViewById(R.id.ivAvatar);
        dialogAvatarImageView = ivAvatar; // 赋值给成员变量
        dialogAvatarImageView = ivAvatar; // 赋值给成员变量

        // 设置头像点击事件
        ivAvatar.setOnClickListener(v -> showPhotoSourceDialog());
        
        builder.setView(view)
               .setTitle("添加学生")
               .setPositiveButton("确定", (dialog, which) -> {
                   String name = etName.getText().toString().trim();
                   String sid = etSid.getText().toString().trim();
                   String gender = etGender.getText().toString().trim();
                   
                   if (TextUtils.isEmpty(name) || TextUtils.isEmpty(sid)) {
                       Toast.makeText(ClassroomActivity.this, "请填写完整信息", Toast.LENGTH_SHORT).show();
                       return;
                   }
                   
                   String avatarUri = currentPhotoUri != null ? currentPhotoUri.toString() : "";
                   long studentId = dbHelper.insertStudent(classroomId, name, sid, gender, avatarUri);
                   
                   // 重置当前照片URI
                   currentPhotoUri = null;
                   currentPhotoFile = null;
                   
                   if (studentId != -1) {
                       // 添加同步日志
                       dbHelper.insertSyncLog("Student", studentId, "INSERT", 
                           System.currentTimeMillis(), "PENDING");
                       
                       // 刷新列表
                       loadStudents();
                   }
               })
               .setNegativeButton("取消", null)
               .show();
    }

    private void showPhotoSourceDialog() {
        String[] items = {"拍照", "从相册选择"};
        new AlertDialog.Builder(this)
            .setTitle("选择照片来源")
            .setItems(items, (dialog, which) -> {
                if (which == 0) {
                    checkCameraPermissionAndTakePhoto();
                } else {
                    checkStoragePermissionAndPickPhoto();
                }
            })
            .show();
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

    private void checkStoragePermissionAndPickPhoto() {
        // 检查存储权限（Android 13+使用READ_MEDIA_IMAGES，低版本使用READ_EXTERNAL_STORAGE）
        String permission = android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU
                ? android.Manifest.permission.READ_MEDIA_IMAGES
                : android.Manifest.permission.READ_EXTERNAL_STORAGE;
                
        if (ContextCompat.checkSelfPermission(this, permission) 
                != PackageManager.PERMISSION_GRANTED) {
            // 请求存储权限
            requestStoragePermissionLauncher.launch(permission);
        } else {
            // 已有权限，直接选择照片
            pickPhoto();
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

    private final ActivityResultLauncher<String> requestStoragePermissionLauncher = 
        registerForActivityResult(new ActivityResultContracts.RequestPermission(), isGranted -> {
            if (isGranted) {
                // 权限被授予，选择照片
                pickPhoto();
            } else {
                // 权限被拒绝
                Toast.makeText(this, "需要存储权限才能选择照片", Toast.LENGTH_SHORT).show();
            }
        });

    private void takePhoto() {
        // 使用新的照片存储管理器创建头像照片文件
        try {
            File photoFile = PhotoStorageManager.createAvatarPhotoFile(this);
            
            if (photoFile != null && photoFile.exists()) {
                currentPhotoUri = FileProvider.getUriForFile(this,
                    getApplicationContext().getPackageName() + ".fileprovider",
                    photoFile);
                
                Intent intent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
                intent.putExtra(MediaStore.EXTRA_OUTPUT, currentPhotoUri);
                intent.addFlags(Intent.FLAG_GRANT_WRITE_URI_PERMISSION);
                
                takePhotoLauncher.launch(intent);
            } else {
                Toast.makeText(this, "无法创建照片文件", Toast.LENGTH_SHORT).show();
            }
        } catch (IOException e) {
            e.printStackTrace();
            Toast.makeText(this, "创建照片文件失败: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }

    private void pickPhoto() {
        pickPhotoLauncher.launch("image/*");
    }
    
    private void copyPickedPhotoToInternalStorage(Uri sourceUri) {
        try {
            // 创建内部存储的目标文件
            File targetFile = PhotoStorageManager.createAvatarPhotoFile(this);
            
            if (targetFile != null) {
                // 复制选择的图片到内部存储
                copyUriToFile(sourceUri, targetFile);
                
                // 更新当前照片文件和URI
                currentPhotoFile = targetFile;
                currentPhotoUri = FileProvider.getUriForFile(this,
                        getApplicationContext().getPackageName() + ".fileprovider",
                        targetFile);
            }
        } catch (IOException e) {
            e.printStackTrace();
            Toast.makeText(this, "复制照片失败: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }
    
    private void copyUriToFile(Uri sourceUri, File targetFile) throws IOException {
        InputStream inputStream = getContentResolver().openInputStream(sourceUri);
        FileOutputStream outputStream = new FileOutputStream(targetFile);
        
        byte[] buffer = new byte[4096];
        int length;
        while ((length = inputStream.read(buffer)) > 0) {
            outputStream.write(buffer, 0, length);
        }
        
        inputStream.close();
        outputStream.close();
    }

    private void processNewPhoto(Uri photoUri) {
        currentPhotoUri = photoUri;
        // 显示照片预览
        if (dialogAvatarImageView != null) {
            Glide.with(this)
                .load(photoUri)
                .placeholder(R.drawable.ic_person_placeholder)
                .error(R.drawable.ic_person_placeholder)
                .circleCrop()
                .into(dialogAvatarImageView);
        }

        if (currentStudent != null) {
            // 如果是编辑学生模式，显示确认对话框
            new AlertDialog.Builder(this)
                .setTitle("确认修改头像")
                .setMessage("确定要将这张照片设为 " + currentStudent.getName() + " 的头像吗？")
                .setPositiveButton("确定", (dialog, which) -> {
                    updateStudentAvatar(currentStudent, photoUri);
                })
                .setNegativeButton("取消", null)
                .show();
        }
    }

    private void updateStudentAvatar(Student student, Uri photoUri) {
        // 更新学生头像
        boolean success = dbHelper.updateStudent(student.getId(), student.getClassId(), 
            student.getName(), student.getSid(), student.getGender(), photoUri.toString());
        
        if (success) {
            // 添加同步日志
            dbHelper.insertSyncLog("Student", student.getId(), "UPDATE", 
                System.currentTimeMillis(), "PENDING");
            
            Toast.makeText(this, "头像更新成功", Toast.LENGTH_SHORT).show();
            loadStudents(); // 刷新列表
        } else {
            Toast.makeText(this, "头像更新失败", Toast.LENGTH_SHORT).show();
        }
    }

    private void showStudentDetailsDialog(Student student) {
        currentStudent = student; // 设置当前编辑的学生
        
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        View view = LayoutInflater.from(this).inflate(R.layout.dialog_student_details, null);
        
        EditText etName = view.findViewById(R.id.etStudentName);
        EditText etSid = view.findViewById(R.id.etStudentId);
        EditText etGender = view.findViewById(R.id.etGender);
        ImageView ivAvatar = view.findViewById(R.id.ivAvatar);
        dialogAvatarImageView = ivAvatar; // 赋值给成员变量
        
        // 填充现有数据
        etName.setText(student.getName());
        etSid.setText(student.getSid());
        etGender.setText(student.getGender());
        
        // 加载头像 - 使用 Glide 处理 content:// URI，避免权限问题
        if (student.getAvatarUri() != null && !student.getAvatarUri().isEmpty()) {
            Glide.with(this)
                .load(Uri.parse(student.getAvatarUri()))
                .placeholder(R.drawable.ic_person_placeholder)
                .error(R.drawable.ic_person_placeholder)
                .circleCrop()
                .into(ivAvatar);
        } else {
            ivAvatar.setImageResource(R.drawable.ic_person_placeholder);
        }
        
        // 设置头像点击事件
        ivAvatar.setOnClickListener(v -> showPhotoSourceDialog());
        
        builder.setView(view)
               .setTitle("学生详情")
               .setPositiveButton("保存", (dialog, which) -> {
                   String name = etName.getText().toString().trim();
                   String sid = etSid.getText().toString().trim();
                   String gender = etGender.getText().toString().trim();
                   
                   if (TextUtils.isEmpty(name)) {
                       Toast.makeText(ClassroomActivity.this, "姓名不能为空", Toast.LENGTH_SHORT).show();
                       return;
                   }

                   if (TextUtils.isEmpty(sid)) {
                       Toast.makeText(ClassroomActivity.this, "学号不能为空", Toast.LENGTH_SHORT).show();
                       return;
                   }
                   
                   // 获取新的头像URI，如果 currentPhotoUri 不为空则使用新的，否则使用学生原有的
                   String newAvatarUri = currentPhotoUri != null ? currentPhotoUri.toString() : student.getAvatarUri();

                   // 更新学生信息
                    boolean success = dbHelper.updateStudent(student.getId(), student.getClassId(), 
                        name, sid, gender, newAvatarUri);
                   
                   // 重置 currentPhotoUri 和 currentPhotoFile
                   currentPhotoUri = null;
                   currentPhotoFile = null;
                   
                   if (success) {
                       // 添加同步日志
                       dbHelper.insertSyncLog("Student", student.getId(), "UPDATE", 
                           System.currentTimeMillis(), "PENDING");
                       
                       Toast.makeText(ClassroomActivity.this, "学生信息已更新", Toast.LENGTH_SHORT).show();
                       loadStudents(); // 刷新列表
                   } else {
                       Toast.makeText(ClassroomActivity.this, "更新失败", Toast.LENGTH_SHORT).show();
                   }
               })
               .setNegativeButton("取消", null)
               .setNeutralButton("删除", (dialog, which) -> {
                   // 删除学生
                   showDeleteStudentDialog(student);
               })
               .show();
    }
    
    private void showDeleteStudentDialog(Student student) {
        new AlertDialog.Builder(this)
            .setTitle("确认删除")
            .setMessage("确定要删除学生 " + student.getName() + " 吗？")
            .setPositiveButton("确定", (dialog, which) -> {
                boolean success = dbHelper.deleteStudent(student.getId());
                if (success) {
                    // 添加同步日志
                    dbHelper.insertSyncLog("Student", student.getId(), "DELETE", 
                        System.currentTimeMillis(), "PENDING");
                    
                    Toast.makeText(ClassroomActivity.this, "学生已删除", Toast.LENGTH_SHORT).show();
                    loadStudents(); // 刷新列表
                } else {
                    Toast.makeText(ClassroomActivity.this, "删除失败", Toast.LENGTH_SHORT).show();
                }
            })
            .setNegativeButton("取消", null)
            .show();
    }

    private void startAttendanceSession() {
        Intent intent = new Intent(this, AttendanceActivity.class);
        intent.putExtra("classroom_id", classroomId);
        startActivity(intent);
    }
}