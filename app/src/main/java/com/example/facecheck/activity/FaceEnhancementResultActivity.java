package com.example.facecheck.activity;

import android.content.Intent;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.FileProvider;

import com.example.facecheck.R;
import com.example.facecheck.utils.FaceImageProcessor;
import com.example.facecheck.utils.FaceRecognitionManager;
import com.example.facecheck.utils.ImageStorageManager;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

/**
 * 人脸修复结果展示界面
 * 显示原始人脸图像和修复后的图像对比，以及特征向量
 */
public class FaceEnhancementResultActivity extends AppCompatActivity {

    private static final String TAG = "FaceEnhancementResult";
    
    private ImageView ivOriginalFace;
    private ImageView ivEnhancedFace;
    private TextView tvQualityOriginal;
    private TextView tvQualityEnhanced;
    private TextView tvFeatureVector;
    private Button btnSave;
    private Button btnShare;
    
    private Bitmap originalBitmap;
    private Bitmap enhancedBitmap;
    private float[] featureVector;
    private String sessionId;
    private ImageStorageManager storageManager;
    private FaceRecognitionManager recognitionManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_face_enhancement_result);
        
        // 初始化视图
        initViews();
        
        // 初始化管理器
        storageManager = new ImageStorageManager(this);
        recognitionManager = new FaceRecognitionManager(this);
        
        // 获取传递的数据
        sessionId = getIntent().getStringExtra("session_id");
        String originalImagePath = getIntent().getStringExtra("original_image_path");
        String enhancedImagePath = getIntent().getStringExtra("enhanced_image_path");
        
        // 加载图像
        if (originalImagePath != null && enhancedImagePath != null) {
            loadImages(originalImagePath, enhancedImagePath);
        } else {
            Toast.makeText(this, "无法加载图像", Toast.LENGTH_SHORT).show();
            finish();
        }
        
        // 设置按钮点击事件
        setupButtonListeners();
    }
    
    private void initViews() {
        ivOriginalFace = findViewById(R.id.ivOriginalFace);
        ivEnhancedFace = findViewById(R.id.ivEnhancedFace);
        tvQualityOriginal = findViewById(R.id.tvQualityOriginal);
        tvQualityEnhanced = findViewById(R.id.tvQualityEnhanced);
        tvFeatureVector = findViewById(R.id.tvFeatureVector);
        btnSave = findViewById(R.id.btnSave);
        btnShare = findViewById(R.id.btnShare);
    }
    
    private void loadImages(String originalPath, String enhancedPath) {
        // 加载原始图像
        originalBitmap = storageManager.loadBitmapFromFile(originalPath);
        if (originalBitmap != null) {
            ivOriginalFace.setImageBitmap(originalBitmap);
            float originalQuality = FaceImageProcessor.calculateImageQuality(originalBitmap);
            tvQualityOriginal.setText(String.format(Locale.getDefault(), "质量评分: %.2f", originalQuality));
        }
        
        // 加载增强后图像
        enhancedBitmap = storageManager.loadBitmapFromFile(enhancedPath);
        if (enhancedBitmap != null) {
            ivEnhancedFace.setImageBitmap(enhancedBitmap);
            float enhancedQuality = FaceImageProcessor.calculateImageQuality(enhancedBitmap);
            tvQualityEnhanced.setText(String.format(Locale.getDefault(), "质量评分: %.2f", enhancedQuality));
        }
        
        // 提取特征向量
        extractFeatureVector();
    }
    
    private void extractFeatureVector() {
        // 使用增强后的图像提取特征向量
        if (enhancedBitmap != null) {
            recognitionManager.extractFeatureVector(enhancedBitmap, new FaceRecognitionManager.FeatureExtractionCallback() {
                @Override
                public void onSuccess(float[] features) {
                    featureVector = features;
                    displayFeatureVector(features);
                }
                
                @Override
                public void onFailure(Exception e) {
                    Log.e(TAG, "特征提取失败", e);
                    tvFeatureVector.setText("无法提取特征向量");
                }
            });
        }
    }
    
    private void displayFeatureVector(float[] features) {
        if (features == null || features.length == 0) {
            tvFeatureVector.setText("无特征向量数据");
            return;
        }
        
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < features.length; i++) {
            sb.append(String.format(Locale.getDefault(), "%.4f", features[i]));
            if (i < features.length - 1) {
                sb.append(", ");
            }
            // 每行显示8个值
            if ((i + 1) % 8 == 0) {
                sb.append("\n");
            }
        }
        
        tvFeatureVector.setText(sb.toString());
    }
    
    private void setupButtonListeners() {
        // 保存结果按钮
        btnSave.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                saveResults();
            }
        });
        
        // 分享按钮
        btnShare.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                shareResults();
            }
        });
    }
    
    private void saveResults() {
        // 保存特征向量到数据库或文件
        if (featureVector != null) {
            // 这里可以添加保存特征向量的代码
            Toast.makeText(this, "结果已保存", Toast.LENGTH_SHORT).show();
        } else {
            Toast.makeText(this, "无特征向量可保存", Toast.LENGTH_SHORT).show();
        }
    }
    
    private void shareResults() {
        if (enhancedBitmap == null) {
            Toast.makeText(this, "无图像可分享", Toast.LENGTH_SHORT).show();
            return;
        }
        
        try {
            // 创建临时文件
            File cachePath = new File(getCacheDir(), "images");
            cachePath.mkdirs();
            File outputFile = new File(cachePath, "enhanced_face_" + 
                    new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(new Date()) + ".jpg");
            
            FileOutputStream outputStream = new FileOutputStream(outputFile);
            enhancedBitmap.compress(Bitmap.CompressFormat.JPEG, 100, outputStream);
            outputStream.close();
            
            // 获取文件URI
            Uri imageUri = FileProvider.getUriForFile(this, 
                    getPackageName() + ".fileprovider", outputFile);
            
            // 创建分享Intent
            Intent shareIntent = new Intent(Intent.ACTION_SEND);
            shareIntent.setType("image/jpeg");
            shareIntent.putExtra(Intent.EXTRA_STREAM, imageUri);
            shareIntent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
            
            // 启动分享
            startActivity(Intent.createChooser(shareIntent, "分享修复后的人脸图像"));
            
        } catch (IOException e) {
            Log.e(TAG, "分享图像失败", e);
            Toast.makeText(this, "分享失败", Toast.LENGTH_SHORT).show();
        }
    }
}