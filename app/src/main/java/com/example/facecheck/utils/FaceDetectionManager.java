package com.example.facecheck.utils;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Rect;
import android.net.Uri;
import android.provider.MediaStore;
import android.util.Log;

import com.google.mlkit.vision.common.InputImage;
import com.google.mlkit.vision.face.Face;
import com.google.mlkit.vision.face.FaceDetection;
import com.google.mlkit.vision.face.FaceDetector;
import com.google.mlkit.vision.face.FaceDetectorOptions;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * 人脸检测管理器
 * 使用Google ML Kit进行多人脸检测和分割
 */
public class FaceDetectionManager {
    
    private static final String TAG = "FaceDetectionManager";
    private final FaceDetector faceDetector;
    private final Context context;
    private final ImageStorageManager storageManager;
    
    // 人脸检测结果回调
    public interface FaceDetectionCallback {
        void onSuccess(List<Face> faces, List<Bitmap> faceBitmaps);
        void onFailure(Exception e);
    }
    
    public FaceDetectionManager(Context context) {
        this.context = context;
        this.storageManager = new ImageStorageManager(context);
        
        // 配置人脸检测器选项
        FaceDetectorOptions options = new FaceDetectorOptions.Builder()
                .setPerformanceMode(FaceDetectorOptions.PERFORMANCE_MODE_FAST)
                .setLandmarkMode(FaceDetectorOptions.LANDMARK_MODE_ALL)
                .setContourMode(FaceDetectorOptions.CONTOUR_MODE_ALL)
                .setClassificationMode(FaceDetectorOptions.CLASSIFICATION_MODE_ALL)
                .enableTracking()
                .build();
        
        faceDetector = FaceDetection.getClient(options);
    }
    
    /**
     * 从URI检测人脸
     */
    public void detectFacesFromUri(Uri imageUri, FaceDetectionCallback callback) {
        try {
            Bitmap bitmap = MediaStore.Images.Media.getBitmap(context.getContentResolver(), imageUri);
            detectFacesFromBitmap(bitmap, callback);
        } catch (IOException e) {
            callback.onFailure(e);
        }
    }
    
    /**
     * 从Bitmap检测人脸
     */
    public void detectFacesFromBitmap(Bitmap bitmap, FaceDetectionCallback callback) {
        InputImage image = InputImage.fromBitmap(bitmap, 0);
        
        faceDetector.process(image)
                .addOnSuccessListener(faces -> {
                    // 成功检测到人脸
                    List<Bitmap> faceBitmaps = extractFaceBitmaps(bitmap, faces);
                    callback.onSuccess(faces, faceBitmaps);
                })
                .addOnFailureListener(callback::onFailure);
    }
    
    /**
     * 从人脸区域提取单个人脸Bitmap
     */
    private List<Bitmap> extractFaceBitmaps(Bitmap originalBitmap, List<Face> faces) {
        List<Bitmap> faceBitmaps = new ArrayList<>();
        
        for (Face face : faces) {
            Rect bounds = face.getBoundingBox();
            
            // 确保边界在图片范围内
            int left = Math.max(0, bounds.left);
            int top = Math.max(0, bounds.top);
            int right = Math.min(originalBitmap.getWidth(), bounds.right);
            int bottom = Math.min(originalBitmap.getHeight(), bounds.bottom);
            
            // 添加一些边距，确保完整包含人脸
            int width = right - left;
            int height = bottom - top;
            int margin = Math.min(width, height) / 4;
            
            left = Math.max(0, left - margin);
            top = Math.max(0, top - margin);
            right = Math.min(originalBitmap.getWidth(), right + margin);
            bottom = Math.min(originalBitmap.getHeight(), bottom + margin);
            
            try {
                int w = right - left;
                int h = bottom - top;
                if (w <= 0 || h <= 0) {
                    Log.w(TAG, "skip invalid face bounds: " + (bounds != null ? bounds.toShortString() : "null")
                            + ", computed w=" + w + ", h=" + h);
                    continue;
                }
                Bitmap faceBitmap = Bitmap.createBitmap(originalBitmap, left, top, w, h);
                faceBitmaps.add(faceBitmap);
            } catch (Exception e) {
                // 如果提取失败，跳过这个人脸
                e.printStackTrace();
            }
        }
        
        return faceBitmaps;
    }
    
    /**
     * 在Bitmap上绘制人脸边界框
     */
    public Bitmap drawFaceBounds(Bitmap bitmap, List<Face> faces) {
        Bitmap mutableBitmap = bitmap.copy(Bitmap.Config.ARGB_8888, true);
        Canvas canvas = new Canvas(mutableBitmap);
        Paint paint = new Paint();
        
        paint.setColor(Color.RED);
        paint.setStyle(Paint.Style.STROKE);
        paint.setStrokeWidth(5f);
        
        Paint textPaint = new Paint();
        textPaint.setColor(Color.RED);
        textPaint.setTextSize(40f);
        textPaint.setStyle(Paint.Style.FILL);
        
        int faceIndex = 1;
        for (Face face : faces) {
            Rect bounds = face.getBoundingBox();
            canvas.drawRect(bounds, paint);
            
            // 绘制人脸编号
            canvas.drawText("Face " + faceIndex, bounds.left, bounds.top - 10, textPaint);
            faceIndex++;
        }
        
        return mutableBitmap;
    }
    
    /**
     * 保存人脸图片到本地（使用ImageStorageManager）
     */
    public List<String> saveFaceBitmaps(List<Bitmap> faceBitmaps, String sessionId) {
        List<String> faceImagePaths = new ArrayList<>();
        
        for (int i = 0; i < faceBitmaps.size(); i++) {
            String imagePath = storageManager.saveSegmentedFace(faceBitmaps.get(i), sessionId, i);
            if (imagePath != null) {
                faceImagePaths.add(imagePath);
            }
        }
        
        return faceImagePaths;
    }
    
    /**
     * 保存原始照片（使用ImageStorageManager）
     */
    public String saveOriginalPhoto(Bitmap originalBitmap, String sessionId) {
        return storageManager.saveOriginalPhoto(originalBitmap, sessionId);
    }
    
    /**
     * 保存处理后的人脸（使用ImageStorageManager）
     */
    public List<String> saveProcessedFaces(List<Bitmap> processedFaces, String sessionId) {
        List<String> processedImagePaths = new ArrayList<>();
        
        for (int i = 0; i < processedFaces.size(); i++) {
            String imagePath = storageManager.saveProcessedFace(processedFaces.get(i), sessionId, i);
            if (imagePath != null) {
                processedImagePaths.add(imagePath);
            }
        }
        
        return processedImagePaths;
    }
    
    /**
     * 获取会话的所有图片
     */
    public List<String> getSessionImages(String sessionId) {
        return storageManager.getSessionImages(sessionId);
    }
    
    /**
     * 删除会话的所有图片
     */
    public boolean deleteSessionImages(String sessionId) {
        return storageManager.deleteSessionImages(sessionId);
    }
    
    /**
     * 清理释放资源
     */
    public void cleanup() {
        faceDetector.close();
    }
}