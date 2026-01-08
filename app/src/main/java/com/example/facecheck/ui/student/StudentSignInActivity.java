package com.example.facecheck.ui.student;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Rect;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.core.content.FileProvider;

import com.example.facecheck.R;
import com.example.facecheck.database.DatabaseHelper;
import com.example.facecheck.utils.FaceDetectionManager;
import com.example.facecheck.utils.FaceRecognitionManager;
import com.google.mlkit.vision.face.Face;

import java.io.File;
import java.io.InputStream;
import java.util.List;

/**
 * 学生自拍签到活动
 * 用于学生自拍考勤模式下的签到
 */
public class StudentSignInActivity extends AppCompatActivity {

    private static final String TAG = "StudentSignInActivity";

    private long sessionId;
    private long classId;
    private long studentId;

    private ImageView ivPreview;
    private Button btnTakePhoto;
    private Button btnPickImage;
    private Button btnConfirmSignIn;
    private TextView tvStatus;
    private ProgressBar progressBar;

    private DatabaseHelper dbHelper;
    private FaceDetectionManager faceDetectionManager;
    private FaceRecognitionManager faceRecognitionManager;

    private Bitmap currentBitmap;
    private Uri photoUri;

    private ActivityResultLauncher<Intent> takePhotoLauncher;
    private ActivityResultLauncher<String> pickImageLauncher;
    private ActivityResultLauncher<String> requestCameraPermissionLauncher;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_student_sign_in);

        // 获取 Intent 参数
        sessionId = getIntent().getLongExtra("session_id", -1);
        classId = getIntent().getLongExtra("class_id", -1);
        studentId = getIntent().getLongExtra("student_id", -1);

        if (sessionId == -1 || classId == -1 || studentId == -1) {
            Toast.makeText(this, "签到信息无效", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        // 初始化
        dbHelper = new DatabaseHelper(this);
        faceDetectionManager = new FaceDetectionManager(this);
        faceRecognitionManager = new FaceRecognitionManager(this);

        initViews();
        initLaunchers();
    }

    private void initViews() {
        ivPreview = findViewById(R.id.iv_preview);
        btnTakePhoto = findViewById(R.id.btn_take_photo);
        btnPickImage = findViewById(R.id.btn_pick_image);
        btnConfirmSignIn = findViewById(R.id.btn_confirm_sign_in);
        tvStatus = findViewById(R.id.tv_status);
        progressBar = findViewById(R.id.progress_bar);

        // 显示签到信息
        tvStatus.setText("请拍摄或选择您的自拍照进行签到");

        btnTakePhoto.setOnClickListener(v -> checkCameraPermissionAndTakePhoto());
        btnPickImage.setOnClickListener(v -> pickImageFromGallery());
        btnConfirmSignIn.setOnClickListener(v -> performSignIn());

        // 初始禁用确认按钮
        btnConfirmSignIn.setEnabled(false);
    }

    private void initLaunchers() {
        // 相机权限请求
        requestCameraPermissionLauncher = registerForActivityResult(
                new ActivityResultContracts.RequestPermission(),
                isGranted -> {
                    if (isGranted) {
                        takePhoto();
                    } else {
                        Toast.makeText(this, "需要相机权限才能拍照", Toast.LENGTH_SHORT).show();
                    }
                });

        // 拍照结果
        takePhotoLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    if (result.getResultCode() == RESULT_OK && photoUri != null) {
                        loadImage(photoUri);
                    }
                });

        // 图库选择
        pickImageLauncher = registerForActivityResult(
                new ActivityResultContracts.GetContent(),
                uri -> {
                    if (uri != null) {
                        loadImage(uri);
                    }
                });
    }

    private void checkCameraPermissionAndTakePhoto() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
            requestCameraPermissionLauncher.launch(Manifest.permission.CAMERA);
        } else {
            takePhoto();
        }
    }

    private void takePhoto() {
        try {
            File photoFile = new File(getCacheDir(), "selfie_" + System.currentTimeMillis() + ".jpg");
            photoUri = FileProvider.getUriForFile(this,
                    getPackageName() + ".fileprovider", photoFile);

            Intent intent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
            intent.putExtra(MediaStore.EXTRA_OUTPUT, photoUri);
            intent.addFlags(Intent.FLAG_GRANT_WRITE_URI_PERMISSION);
            takePhotoLauncher.launch(intent);
        } catch (Exception e) {
            Log.e(TAG, "拍照失败: " + e.getMessage(), e);
            Toast.makeText(this, "无法打开相机", Toast.LENGTH_SHORT).show();
        }
    }

    private void pickImageFromGallery() {
        try {
            pickImageLauncher.launch("image/*");
        } catch (Exception e) {
            Log.e(TAG, "选择图片失败: " + e.getMessage(), e);
            Toast.makeText(this, "无法打开图库", Toast.LENGTH_SHORT).show();
        }
    }

    private void loadImage(Uri uri) {
        try {
            InputStream is = getContentResolver().openInputStream(uri);
            BitmapFactory.Options opts = new BitmapFactory.Options();
            opts.inSampleSize = 2; // 降低内存占用
            currentBitmap = BitmapFactory.decodeStream(is, null, opts);
            is.close();

            if (currentBitmap != null) {
                ivPreview.setImageBitmap(currentBitmap);
                btnConfirmSignIn.setEnabled(true);
                tvStatus.setText("照片已加载，点击【确认签到】按钮进行人脸验证");
            }
        } catch (Exception e) {
            Log.e(TAG, "加载图片失败: " + e.getMessage(), e);
            Toast.makeText(this, "图片加载失败", Toast.LENGTH_SHORT).show();
        }
    }

    private void performSignIn() {
        if (currentBitmap == null) {
            Toast.makeText(this, "请先拍摄或选择照片", Toast.LENGTH_SHORT).show();
            return;
        }

        // 显示进度
        progressBar.setVisibility(View.VISIBLE);
        btnConfirmSignIn.setEnabled(false);
        tvStatus.setText("正在进行人脸检测与验证...");

        new Thread(() -> {
            try {
                // 1. 人脸检测
                List<Face> faces = faceDetectionManager.detectFacesSync(currentBitmap);

                if (faces == null || faces.isEmpty()) {
                    runOnUiThread(() -> {
                        progressBar.setVisibility(View.GONE);
                        btnConfirmSignIn.setEnabled(true);
                        tvStatus.setText("未检测到人脸，请重新拍摄");
                        Toast.makeText(this, "未检测到人脸", Toast.LENGTH_SHORT).show();
                    });
                    return;
                }

                if (faces.size() > 1) {
                    runOnUiThread(() -> {
                        progressBar.setVisibility(View.GONE);
                        btnConfirmSignIn.setEnabled(true);
                        tvStatus.setText("检测到多张人脸，请确保只有您本人");
                        Toast.makeText(this, "检测到多张人脸", Toast.LENGTH_SHORT).show();
                    });
                    return;
                }

                Face face = faces.get(0);

                // 2. 人脸验证（1:1 比对）
                FaceRecognitionManager.RecognitionResult result = faceRecognitionManager
                        .verifyStudentIdentity(currentBitmap, face, studentId);

                runOnUiThread(() -> {
                    progressBar.setVisibility(View.GONE);

                    if (result.isSuccess() && result.getStudentId() == studentId) {
                        // 验证成功，记录签到
                        saveAttendanceResult(result.getSimilarity());
                    } else {
                        // 验证失败
                        btnConfirmSignIn.setEnabled(true);
                        tvStatus.setText("身份验证失败：" + result.getMessage());
                        Toast.makeText(this, "身份验证失败，请重试", Toast.LENGTH_SHORT).show();
                    }
                });

            } catch (Exception e) {
                Log.e(TAG, "签到过程出错: " + e.getMessage(), e);
                runOnUiThread(() -> {
                    progressBar.setVisibility(View.GONE);
                    btnConfirmSignIn.setEnabled(true);
                    tvStatus.setText("签到出错，请重试");
                    Toast.makeText(this, "签到出错: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
            }
        }).start();
    }

    private void saveAttendanceResult(float similarity) {
        try {
            // 检查是否已有签到记录
            android.database.Cursor cursor = dbHelper.getAttendanceResultsBySession(sessionId);
            boolean alreadySignedIn = false;
            long existingResultId = -1;

            if (cursor != null && cursor.moveToFirst()) {
                do {
                    long sid = cursor.getLong(cursor.getColumnIndexOrThrow("studentId"));
                    if (sid == studentId) {
                        alreadySignedIn = true;
                        existingResultId = cursor.getLong(cursor.getColumnIndexOrThrow("id"));
                        break;
                    }
                } while (cursor.moveToNext());
                cursor.close();
            }

            if (alreadySignedIn) {
                // 更新已有记录
                dbHelper.updateAttendanceResult(existingResultId, "Present", "SELF");
                Toast.makeText(this, "签到成功（更新记录）", Toast.LENGTH_SHORT).show();
            } else {
                // 插入新记录
                long resultId = dbHelper.insertAttendanceResult(sessionId, studentId, "Present", similarity, "SELF");
                if (resultId > 0) {
                    Toast.makeText(this, "签到成功！", Toast.LENGTH_SHORT).show();
                } else {
                    Toast.makeText(this, "签到记录保存失败", Toast.LENGTH_SHORT).show();
                    btnConfirmSignIn.setEnabled(true);
                    return;
                }
            }

            // 显示成功界面
            tvStatus.setText("✓ 签到成功\n相似度: " + String.format("%.1f%%", similarity * 100));
            tvStatus.setTextColor(getColor(android.R.color.holo_green_dark));

            // 延迟返回
            ivPreview.postDelayed(() -> {
                setResult(RESULT_OK);
                finish();
            }, 1500);

        } catch (Exception e) {
            Log.e(TAG, "保存签到记录失败: " + e.getMessage(), e);
            Toast.makeText(this, "保存签到记录失败", Toast.LENGTH_SHORT).show();
            btnConfirmSignIn.setEnabled(true);
        }
    }
}
