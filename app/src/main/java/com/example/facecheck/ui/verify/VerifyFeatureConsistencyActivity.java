package com.example.facecheck.ui.verify;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.example.facecheck.R;
import com.example.facecheck.utils.FaceDetectionManager;
import com.example.facecheck.utils.FaceRecognitionManager;
import com.example.facecheck.utils.ImageUtils;
import com.google.mlkit.vision.face.Face;

import android.graphics.Bitmap;

import java.util.List;

/**
 * 验证特征提取一致性的简单页面
 * 使用同一张原图 bitmap 与同一个 Face 对象，连续调用两次 extractFaceFeatures
 * 比较两次向量的相似度是否为 1.0（或非常接近 1.0）
 */
public class VerifyFeatureConsistencyActivity extends AppCompatActivity {

    private ImageView ivPreview;
    private TextView tvResult;
    private Button btnPickAndVerify;

    private final FaceDetectionManager detectionManager = new FaceDetectionManager(this);
    private final FaceRecognitionManager recognitionManager = new FaceRecognitionManager(this);

    private ActivityResultLauncher<String> pickImageLauncher;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_verify_feature_consistency);

        ivPreview = findViewById(R.id.ivPreview);
        tvResult = findViewById(R.id.tvResult);
        btnPickAndVerify = findViewById(R.id.btnPickAndVerify);

        pickImageLauncher = registerForActivityResult(new ActivityResultContracts.GetContent(), uri -> {
            if (uri != null) {
                startVerification(uri);
            } else {
                Toast.makeText(this, "未选择图片", Toast.LENGTH_SHORT).show();
            }
        });

        btnPickAndVerify.setOnClickListener(v -> pickImageLauncher.launch("image/*"));
    }

    private void startVerification(Uri imageUri) {
        try {
            // 按与检测相同的方式加载并校正EXIF方向，确保坐标一致
            Bitmap orientedBitmap = ImageUtils.loadAndResizeBitmap(this, imageUri, 1600, 1600);
            if (orientedBitmap == null) {
                Toast.makeText(this, "加载图片失败", Toast.LENGTH_SHORT).show();
                return;
            }
            ivPreview.setImageBitmap(orientedBitmap);

            // 先进行一次人脸检测，获取 Face 对象与其在原图上的坐标
            detectionManager.detectFacesFromUri(imageUri, new FaceDetectionManager.FaceDetectionCallback() {
                @Override
                public void onSuccess(List<Face> faces, List<Bitmap> faceBitmaps) {
                    if (faces == null || faces.isEmpty()) {
                        runOnUiThread(() -> tvResult.setText("未检测到人脸，请换一张图片"));
                        return;
                    }

                    Face face = faces.get(0); // 取第一张人脸

                    // 使用同一张原图 orientedBitmap + 同一个 Face 对象，连续两次提取
                    float[] f1 = recognitionManager.extractFaceFeatures(orientedBitmap, face);
                    float[] f2 = recognitionManager.extractFaceFeatures(orientedBitmap, face);

                    if (f1 == null || f2 == null) {
                        runOnUiThread(() -> tvResult.setText("特征提取失败：返回了空向量(可能是裁剪或关键点不足)"));
                        return;
                    }

                    // 直接两次提取的相似度
                    float simDirect = recognitionManager.calculateSimilarity(f1, f2);

                    // 按 FaceRecognitionManager 的入库逻辑，转换为字节再读回向量，并比对
                    byte[] dbBytes = recognitionManager.floatArrayToByteArray(f1);
                    float[] f1RoundTrip = recognitionManager.byteArrayToFloatArray(dbBytes);
                    float simRoundTrip = recognitionManager.calculateSimilarity(f1, f1RoundTrip);

                    String msg = String.format(
                            "两次直接提取相似度：%.6f\n入库/读出回环相似度：%.6f\n结论(直接)：%s\n结论(回环)：%s",
                            simDirect,
                            simRoundTrip,
                            (Math.abs(1.0f - simDirect) < 1e-5f ? "一致(≈1.0)" : "不一致"),
                            (Math.abs(1.0f - simRoundTrip) < 1e-5f ? "一致(≈1.0)" : "不一致")
                    );
                    runOnUiThread(() -> tvResult.setText(msg));
                }

                @Override
                public void onFailure(Exception e) {
                    runOnUiThread(() -> tvResult.setText("人脸检测失败: " + e.getMessage()));
                }
            });

        } catch (Exception e) {
            Toast.makeText(this, "处理图片失败: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }
}
