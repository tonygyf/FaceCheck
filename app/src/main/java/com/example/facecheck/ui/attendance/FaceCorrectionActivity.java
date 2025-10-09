package com.example.facecheck.ui.attendance;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.facecheck.R;
import com.example.facecheck.utils.FaceImageProcessor;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;

/**
 * 人脸修正界面
 * 提供人脸图像质量检测和修正功能
 */
public class FaceCorrectionActivity extends AppCompatActivity {
    
    private ImageView ivOriginal, ivCorrected;
    private TextView tvQuality, tvCorrectionInfo;
    private Button btnCorrect, btnSave, btnCancel;
    
    private String originalImagePath;
    private Bitmap originalBitmap;
    private Bitmap correctedBitmap;
    private boolean isCorrected = false;
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_face_correction);
        
        // 获取传递的图片路径
        originalImagePath = getIntent().getStringExtra("image_path");
        
        if (originalImagePath == null || originalImagePath.isEmpty()) {
            Toast.makeText(this, "没有提供图片路径", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }
        
        initViews();
        loadOriginalImage();
    }
    
    private void initViews() {
        ivOriginal = findViewById(R.id.ivOriginal);
        ivCorrected = findViewById(R.id.ivCorrected);
        tvQuality = findViewById(R.id.tvQuality);
        tvCorrectionInfo = findViewById(R.id.tvCorrectionInfo);
        btnCorrect = findViewById(R.id.btnCorrect);
        btnSave = findViewById(R.id.btnSave);
        btnCancel = findViewById(R.id.btnCancel);
        
        btnCorrect.setOnClickListener(v -> performCorrection());
        btnSave.setOnClickListener(v -> saveCorrectedImage());
        btnCancel.setOnClickListener(v -> finish());
        
        // 初始状态隐藏修正后的图片和保存按钮
        ivCorrected.setVisibility(View.GONE);
        btnSave.setVisibility(View.GONE);
        tvCorrectionInfo.setVisibility(View.GONE);
    }
    
    private void loadOriginalImage() {
        File imageFile = new File(originalImagePath);
        if (!imageFile.exists()) {
            Toast.makeText(this, "图片文件不存在", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }
        
        originalBitmap = BitmapFactory.decodeFile(originalImagePath);
        if (originalBitmap == null) {
            Toast.makeText(this, "无法加载图片", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }
        
        ivOriginal.setImageBitmap(originalBitmap);
        
        // 检测图像质量
        float quality = FaceImageProcessor.calculateImageQuality(originalBitmap);
        tvQuality.setText(String.format("图像质量评分: %.2f/1.0", quality));
        
        // 根据质量显示建议
        if (quality < 0.5f) {
            tvQuality.setTextColor(getResources().getColor(android.R.color.holo_red_dark));
            tvQuality.append(" (质量较差，建议修正)");
            btnCorrect.setEnabled(true);
        } else if (quality < 0.7f) {
            tvQuality.setTextColor(getResources().getColor(android.R.color.holo_orange_dark));
            tvQuality.append(" (质量一般，可选择修正)");
            btnCorrect.setEnabled(true);
        } else {
            tvQuality.setTextColor(getResources().getColor(android.R.color.holo_green_dark));
            tvQuality.append(" (质量良好)");
            btnCorrect.setEnabled(false);
        }
    }
    
    private void performCorrection() {
        if (originalBitmap == null) {
            Toast.makeText(this, "原始图片未加载", Toast.LENGTH_SHORT).show();
            return;
        }
        
        try {
            // 显示进度
            tvCorrectionInfo.setVisibility(View.VISIBLE);
            tvCorrectionInfo.setText("正在修正图像...");
            
            // 执行图像修正
            correctedBitmap = FaceImageProcessor.enhanceImage(originalBitmap, 1.2f, 10f);
            
            if (correctedBitmap != null) {
                // 显示修正后的图片
                ivCorrected.setImageBitmap(correctedBitmap);
                ivCorrected.setVisibility(View.VISIBLE);
                
                // 重新评估质量
                float correctedQuality = FaceImageProcessor.calculateImageQuality(correctedBitmap);
                tvCorrectionInfo.setText(String.format("修正完成 - 新质量评分: %.2f/1.0", correctedQuality));
                
                // 显示保存按钮
                btnSave.setVisibility(View.VISIBLE);
                isCorrected = true;
                
                Toast.makeText(this, "图像修正完成", Toast.LENGTH_SHORT).show();
            } else {
                tvCorrectionInfo.setText("修正失败");
                Toast.makeText(this, "图像修正失败", Toast.LENGTH_SHORT).show();
            }
            
        } catch (Exception e) {
            tvCorrectionInfo.setText("修正出错: " + e.getMessage());
            Toast.makeText(this, "修正出错: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }
    
    private void saveCorrectedImage() {
        if (correctedBitmap == null) {
            Toast.makeText(this, "没有修正后的图片", Toast.LENGTH_SHORT).show();
            return;
        }
        
        try {
            // 生成新的文件路径
            String correctedPath = originalImagePath.replace(".jpg", "_corrected.jpg")
                                                   .replace(".png", "_corrected.png");
            
            File correctedFile = new File(correctedPath);
            FileOutputStream fos = new FileOutputStream(correctedFile);
            
            // 保存为JPEG格式，质量95%
            correctedBitmap.compress(Bitmap.CompressFormat.JPEG, 95, fos);
            fos.close();
            
            Toast.makeText(this, "修正后的图片已保存", Toast.LENGTH_SHORT).show();
            
            // 返回结果给调用者
            getIntent().putExtra("corrected_image_path", correctedPath);
            getIntent().putExtra("is_corrected", true);
            setResult(RESULT_OK, getIntent());
            
            finish();
            
        } catch (IOException e) {
            Toast.makeText(this, "保存失败: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }
    
    @Override
    public void onBackPressed() {
        if (isCorrected) {
            // 如果有修正但未保存，提示用户
            android.app.AlertDialog.Builder builder = new android.app.AlertDialog.Builder(this);
            builder.setTitle("未保存修正");
            builder.setMessage("您已经修正了图像但未保存，是否要离开？");
            builder.setPositiveButton("离开", (dialog, which) -> finish());
            builder.setNegativeButton("取消", null);
            builder.show();
        } else {
            super.onBackPressed();
        }
    }
    
    @Override
    protected void onDestroy() {
        super.onDestroy();
        // 释放位图资源
        if (originalBitmap != null && !originalBitmap.isRecycled()) {
            originalBitmap.recycle();
        }
        if (correctedBitmap != null && !correctedBitmap.isRecycled()) {
            correctedBitmap.recycle();
        }
    }
}