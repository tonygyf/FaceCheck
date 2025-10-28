package com.example.facecheck.ui.face;

import android.content.Intent;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.content.FileProvider;

import com.example.facecheck.R;
import com.example.facecheck.utils.FaceDetectionManager;
import com.example.facecheck.utils.FaceImageProcessor;
import com.example.facecheck.utils.FaceRecognitionManager;
import com.example.facecheck.utils.ImageStorageManager;
import com.google.mlkit.vision.face.Face;
 
import java.io.File;
import java.io.IOException;
import java.util.List;

/**
 * 人脸增强修复Activity
 * 提供独立的人脸修复功能，支持上传照片进行质量增强和特征提取
 */
public class FaceEnhancementActivity extends AppCompatActivity {
    
    private static final int PICK_IMAGE_REQUEST = 1;
    private static final int TAKE_PHOTO_REQUEST = 2;
    
    private ImageView ivOriginalImage, ivEnhancedImage;
    private TextView tvOriginalQuality, tvEnhancedQuality, tvFaceCount, tvFeatureVector;
    private Button btnSelectPhoto, btnTakePhoto, btnEnhanceFace, btnExtractFeatures, btnSaveResult;
    private View comparisonView;
    
    private Bitmap originalBitmap;
    private Bitmap enhancedBitmap;
    private FaceDetectionManager faceDetectionManager;
    private FaceRecognitionManager faceRecognitionManager;
    private ImageStorageManager imageStorageManager;
    private List<Face> detectedFaces;
    private float[] extractedFeatures;
    private Uri capturedImageUri;
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_face_enhancement);
        
        initViews();
        setupToolbar();
        initManagers();
        setupClickListeners();
    }
    
    private void initViews() {
        ivOriginalImage = findViewById(R.id.ivOriginalImage);
        ivEnhancedImage = findViewById(R.id.ivEnhancedImage);
        tvOriginalQuality = findViewById(R.id.tvOriginalQuality);
        tvEnhancedQuality = findViewById(R.id.tvEnhancedQuality);
        tvFaceCount = findViewById(R.id.tvFaceCount);
        tvFeatureVector = findViewById(R.id.tvFeatureVector);
        btnSelectPhoto = findViewById(R.id.btnSelectPhoto);
        btnTakePhoto = findViewById(R.id.btnTakePhoto);
        btnEnhanceFace = findViewById(R.id.btnEnhanceFace);
        btnExtractFeatures = findViewById(R.id.btnExtractFeatures);
        btnSaveResult = findViewById(R.id.btnSaveResult);
        comparisonView = findViewById(R.id.comparisonView);
        
        // 初始状态隐藏对比视图
        comparisonView.setVisibility(View.GONE);
        btnEnhanceFace.setEnabled(false);
        btnExtractFeatures.setEnabled(false);
        btnSaveResult.setEnabled(false);
    }
    
    private void setupToolbar() {
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setTitle("人脸修复增强");
        }
    }
    
    private void initManagers() {
        faceDetectionManager = new FaceDetectionManager(this);
        faceRecognitionManager = new FaceRecognitionManager(this);
        imageStorageManager = new ImageStorageManager(this);
    }
    
    private void setupClickListeners() {
        btnSelectPhoto.setOnClickListener(v -> selectImageFromGallery());
        btnTakePhoto.setOnClickListener(v -> takePhotoWithCamera());
        btnEnhanceFace.setOnClickListener(v -> enhanceFaceImage());
        btnExtractFeatures.setOnClickListener(v -> extractFaceFeatures());
        btnSaveResult.setOnClickListener(v -> saveEnhancedResult());
    }
    
    private void selectImageFromGallery() {
        Intent intent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
        intent.setType("image/*");
        startActivityForResult(intent, PICK_IMAGE_REQUEST);
    }
    
    private void takePhotoWithCamera() {
        Intent intent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        if (intent.resolveActivity(getPackageManager()) != null) {
            try {
                File photoFile = createImageFile();
                capturedImageUri = FileProvider.getUriForFile(this,
                        "com.example.facecheck.fileprovider",
                        photoFile);
                intent.putExtra(MediaStore.EXTRA_OUTPUT, capturedImageUri);
                intent.addFlags(Intent.FLAG_GRANT_WRITE_URI_PERMISSION | Intent.FLAG_GRANT_READ_URI_PERMISSION);
                startActivityForResult(intent, TAKE_PHOTO_REQUEST);
            } catch (Exception e) {
                Toast.makeText(this, "创建照片文件失败: " + e.getMessage(), Toast.LENGTH_SHORT).show();
            }
        } else {
            Toast.makeText(this, "没有可用的相机应用", Toast.LENGTH_SHORT).show();
        }
    }
    
    private void enhanceFaceImage() {
        if (originalBitmap == null || detectedFaces == null || detectedFaces.isEmpty()) {
            Toast.makeText(this, "请先选择包含人脸的图片", Toast.LENGTH_SHORT).show();
            return;
        }
        
        // 确保原图已显示
        if (ivOriginalImage.getDrawable() == null) {
            ivOriginalImage.setImageBitmap(originalBitmap);
        }
        
        // 显示进度对话框
        showLoading("正在修复人脸图像，请稍候...");
        
        // 在后台线程中处理
        new Thread(() -> {
            try {
                // 对检测到的人脸进行修复
                if (detectedFaces.size() > 0) {
                    // 修复第一个人脸（可以扩展为处理多个人脸）
                    enhancedBitmap = FaceImageProcessor.repairFaceImage(originalBitmap);
                    
                    if (enhancedBitmap != null) {
                        // 计算修复后的质量
                        float enhancedQuality = FaceImageProcessor.calculateImageQuality(enhancedBitmap);
                        
                        runOnUiThread(() -> {
                            hideLoading();
                            // 确保原图已显示
                            if (ivOriginalImage.getDrawable() == null) {
                                ivOriginalImage.setImageBitmap(originalBitmap);
                            }
                            
                            // 显示修复结果
                            ivEnhancedImage.setImageBitmap(enhancedBitmap);
                            // 缓存修复后的图片
                            File repairedCache = saveBitmapToInternalCache(enhancedBitmap, "repaired_face.jpg");
                            if (repairedCache != null) {
                                Log.d("FaceEnhancement", "Repaired image saved to cache: " + repairedCache.getAbsolutePath());
                            }
                            tvEnhancedQuality.setText(String.format("修复后质量: %.2f", enhancedQuality));
                            
                            // 显示对比视图
                            comparisonView.setVisibility(View.VISIBLE);
                            
                            // 启用特征提取按钮
                            btnExtractFeatures.setEnabled(true);
                            
                            Toast.makeText(this, "人脸修复完成", Toast.LENGTH_SHORT).show();
                        });
                    } else {
                        runOnUiThread(() -> {
                            hideLoading();
                            Toast.makeText(this, "修复失败：无法处理图像", Toast.LENGTH_SHORT).show();
                        });
                    }
                }
            } catch (Exception e) {
                runOnUiThread(() -> {
                    hideLoading();
                    Toast.makeText(this, "修复失败: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    Log.e("FaceEnhancement", "Face repair error", e);
                });
            }
        }).start();
    }
    
    private void extractFaceFeatures() {
        if (enhancedBitmap == null) {
            Toast.makeText(this, "请先修复人脸图像", Toast.LENGTH_SHORT).show();
            return;
        }

        Toast.makeText(this, "正在提取特征向量...", Toast.LENGTH_SHORT).show();

        faceDetectionManager.detectFacesFromBitmap(enhancedBitmap, new FaceDetectionManager.FaceDetectionCallback() {
            @Override
            public void onSuccess(List<Face> faces, List<Bitmap> faceBitmaps) {
                detectedFaces = faces;
                if (faces == null || faces.isEmpty()) {
                    runOnUiThread(() -> Toast.makeText(FaceEnhancementActivity.this,
                            "修复后的图片未检测到人脸，无法提取特征", Toast.LENGTH_SHORT).show());
                    return;
                }

                new Thread(() -> {
                    try {
                        extractedFeatures = faceRecognitionManager.extractFaceFeatures(enhancedBitmap, faces.get(0));
                        runOnUiThread(() -> {
                            if (extractedFeatures != null && extractedFeatures.length > 0) {
                                StringBuilder featureInfo = new StringBuilder();
                                featureInfo.append(String.format("特征向量维度: %d\n", extractedFeatures.length));
                                featureInfo.append("前5个特征值: ");
                                for (int i = 0; i < Math.min(5, extractedFeatures.length); i++) {
                                    featureInfo.append(String.format("%.3f", extractedFeatures[i]));
                                    if (i < Math.min(4, extractedFeatures.length - 1)) {
                                        featureInfo.append(", ");
                                    }
                                }
                                if (extractedFeatures.length > 5) {
                                    featureInfo.append("...");
                                }
                                tvFeatureVector.setText(featureInfo.toString());
                                btnSaveResult.setEnabled(true);
                                Toast.makeText(FaceEnhancementActivity.this, "特征提取成功！", Toast.LENGTH_SHORT).show();
                                Log.d("FaceEnhancement", "Feature extraction successful - Vector length: " + extractedFeatures.length);
                            } else {
                                Toast.makeText(FaceEnhancementActivity.this, "特征提取失败，请重试", Toast.LENGTH_SHORT).show();
                                Log.e("FaceEnhancement", "Feature extraction failed - returned null");
                            }
                        });
                    } catch (Exception e) {
                        runOnUiThread(() -> {
                            Toast.makeText(FaceEnhancementActivity.this, "特征提取错误: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                            Log.e("FaceEnhancement", "Feature extraction error", e);
                        });
                    }
                }).start();
            }

            @Override
            public void onFailure(Exception e) {
                runOnUiThread(() -> Toast.makeText(FaceEnhancementActivity.this,
                        "修复图人脸检测失败: " + e.getMessage(), Toast.LENGTH_SHORT).show());
            }
        });
    }
    
    private void saveEnhancedResult() {
        if (enhancedBitmap == null || extractedFeatures == null) {
            Toast.makeText(this, "没有可保存的结果", Toast.LENGTH_SHORT).show();
            return;
        }
        
        showLoading("正在保存结果...");
        
        new Thread(() -> {
            try {
                // 保存原始图片
                String originalPath = imageStorageManager.saveTempImage(originalBitmap, "original");
                
                // 保存增强后的图片
                String enhancedPath = imageStorageManager.saveTempImage(enhancedBitmap, "enhanced");
                
                // 同步保存到内部缓存（用于快速预览/缓存规范）
                File originalCache = saveBitmapToInternalCache(originalBitmap, "original_face.jpg");
                File repairedCache = saveBitmapToInternalCache(enhancedBitmap, "repaired_face.jpg");
                if (originalCache != null) {
                    Log.d("FaceEnhancement", "Original image saved to cache: " + originalCache.getAbsolutePath());
                }
                if (repairedCache != null) {
                    Log.d("FaceEnhancement", "Repaired image saved to cache: " + repairedCache.getAbsolutePath());
                }
                
                // 保存特征向量数据（可选）
                String embeddingPath = saveEmbeddingData(extractedFeatures);
                
                runOnUiThread(() -> {
                    hideLoading();
                    String successMessage = "结果已保存！\n原图: " + originalPath + "\n增强图: " + enhancedPath;
                    if (embeddingPath != null) {
                        successMessage += "\n特征数据: " + embeddingPath;
                    }
                    Toast.makeText(this, successMessage, Toast.LENGTH_LONG).show();
                    
                    // 记录日志
                    Log.d("FaceEnhancement", "Save successful - Original: " + originalPath + ", Enhanced: " + enhancedPath);
                    
                    // 重置界面
                    resetUI();
                });
            } catch (Exception e) {
                runOnUiThread(() -> {
                    hideLoading();
                    Toast.makeText(this, "保存失败: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    Log.e("FaceEnhancement", "Save failed", e);
                });
            }
        }).start();
    }
    
    private String saveEmbeddingData(float[] features) {
        try {
            // 将特征向量数据保存为JSON格式
            String jsonData = String.format("{\"quality\":%.2f,\"vector_length\":%d,\"timestamp\":%d}", 
                FaceImageProcessor.calculateImageQuality(enhancedBitmap), features.length, System.currentTimeMillis());
            
            String fileName = "embedding_" + System.currentTimeMillis() + ".json";
            return imageStorageManager.saveTextFile(jsonData, fileName);
        } catch (Exception e) {
            Log.e("FaceEnhancement", "Failed to save embedding data", e);
            return null;
        }
    }
    
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        
        if (resultCode == RESULT_OK) {
            switch (requestCode) {
                case PICK_IMAGE_REQUEST:
                    if (data != null && data.getData() != null) {
                        Uri imageUri = data.getData();
                        processSelectedImage(imageUri);
                    }
                    break;
                    
                case TAKE_PHOTO_REQUEST:
                    if (capturedImageUri != null) {
                        processSelectedImage(capturedImageUri);
                    } else if (data != null && data.getExtras() != null) {
                        Bundle extras = data.getExtras();
                        Bitmap photo = (Bitmap) extras.get("data");
                        if (photo != null) {
                            processCapturedPhoto(photo);
                        }
                    }
                    break;
            }
        }
    }
    
    private void processSelectedImage(Uri imageUri) {
        try {
            originalBitmap = MediaStore.Images.Media.getBitmap(getContentResolver(), imageUri);
            processImage(originalBitmap);
        } catch (IOException e) {
            Toast.makeText(this, "加载图片失败: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }
    
    private void processCapturedPhoto(Bitmap photo) {
        originalBitmap = photo;
        processImage(originalBitmap);
    }
    
    private void processImage(Bitmap bitmap) {
        if (bitmap == null) {
            return;
        }
        
        // 显示原图
        ivOriginalImage.setImageBitmap(bitmap);
        // 缓存原始图片
        File originalCache = saveBitmapToInternalCache(bitmap, "original_face.jpg");
        if (originalCache != null) {
            Log.d("FaceEnhancement", "Original image saved to cache: " + originalCache.getAbsolutePath());
        }
        
        // 计算原图质量
        float originalQuality = FaceImageProcessor.calculateImageQuality(bitmap);
        tvOriginalQuality.setText(String.format("原图质量: %.2f", originalQuality));
        
        // 检测人脸
        Toast.makeText(this, "正在检测人脸...", Toast.LENGTH_SHORT).show();
        
        faceDetectionManager.detectFacesFromBitmap(bitmap, new FaceDetectionManager.FaceDetectionCallback() {
            @Override
            public void onSuccess(List<Face> faces, List<Bitmap> faceBitmaps) {
                runOnUiThread(() -> {
                    detectedFaces = faces;
                    tvFaceCount.setText(String.format("检测到 %d 个人脸", faces.size()));
                    
                    if (faces.size() > 0) {
                        // 启用人脸修复按钮
                        btnEnhanceFace.setEnabled(true);
                        Toast.makeText(FaceEnhancementActivity.this, 
                            "人脸检测完成，可以进行修复", Toast.LENGTH_SHORT).show();
                    } else {
                        Toast.makeText(FaceEnhancementActivity.this, 
                            "未检测到人脸，请选择包含人脸的图片", Toast.LENGTH_SHORT).show();
                    }
                });
            }
            
            @Override
            public void onFailure(Exception e) {
                runOnUiThread(() -> {
                    Toast.makeText(FaceEnhancementActivity.this, 
                        "人脸检测失败: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
            }
        });
    }
    
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            finish();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }
    
    private void showLoading(String message) {
        runOnUiThread(() -> {
            // 这里可以显示一个进度对话框
            Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
        });
    }
    
    private void hideLoading() {
        runOnUiThread(() -> {
            // 这里可以隐藏进度对话框
        });
    }
    
    private void resetUI() {
        runOnUiThread(() -> {
            // 重置界面状态
            originalBitmap = null;
            enhancedBitmap = null;
            detectedFaces = null;
            extractedFeatures = null;
            
            ivOriginalImage.setImageResource(android.R.drawable.ic_menu_gallery);
            ivEnhancedImage.setImageResource(android.R.drawable.ic_menu_gallery);
            tvOriginalQuality.setText("原图质量: --");
            tvEnhancedQuality.setText("修复后质量: --");
            tvFaceCount.setText("检测到 0 个人脸");
            tvFeatureVector.setText("特征向量维度: --");
            
            comparisonView.setVisibility(View.GONE);
            btnEnhanceFace.setEnabled(false);
            btnExtractFeatures.setEnabled(false);
            btnSaveResult.setEnabled(false);
        });
    }

    // 新增：保存Bitmap到内部缓存目录
    private File saveBitmapToInternalCache(Bitmap bitmap, String filename) {
        try {
            File dir = getCacheDir();
            File file = new File(dir, filename);
            java.io.FileOutputStream fos = new java.io.FileOutputStream(file);
            bitmap.compress(Bitmap.CompressFormat.JPEG, 90, fos);
            fos.close();
            return file;
        } catch (Exception e) {
            Log.e("FaceEnhancement", "Failed to save bitmap to cache: " + e.getMessage());
            return null;
        }
    }

    private File createImageFile() {
        File storageDir = getExternalFilesDir(android.os.Environment.DIRECTORY_PICTURES);
        if (storageDir == null) {
            storageDir = getFilesDir();
        }
        return new java.io.File(storageDir, "face_capture_" + System.currentTimeMillis() + ".jpg");
    }
}