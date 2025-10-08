package com.example.facecheck.utils;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Matrix;

/**
 * 人脸图像处理工具类
 * 提供人脸图像的修正、旋转、缩放等功能
 */
public class FaceImageProcessor {
    
    private static final String TAG = "FaceImageProcessor";
    
    /**
     * 标准化人脸图像
     * 将人脸图像调整为标准尺寸并优化质量
     */
    public static Bitmap normalizeFaceImage(Bitmap faceBitmap, int targetSize) {
        if (faceBitmap == null) {
            return null;
        }
        
        // 确保图像是正方形
        int width = faceBitmap.getWidth();
        int height = faceBitmap.getHeight();
        int size = Math.min(width, height);
        
        // 裁剪为正方形
        Bitmap squareBitmap;
        if (width != height) {
            int x = (width - size) / 2;
            int y = (height - size) / 2;
            squareBitmap = Bitmap.createBitmap(faceBitmap, x, y, size, size);
        } else {
            squareBitmap = faceBitmap;
        }
        
        // 调整到目标尺寸
        return Bitmap.createScaledBitmap(squareBitmap, targetSize, targetSize, true);
    }
    
    /**
     * 旋转图像
     */
    public static Bitmap rotateImage(Bitmap bitmap, float degrees) {
        if (bitmap == null || degrees == 0) {
            return bitmap;
        }
        
        Matrix matrix = new Matrix();
        matrix.postRotate(degrees);
        
        return Bitmap.createBitmap(bitmap, 0, 0, bitmap.getWidth(), bitmap.getHeight(), 
                matrix, true);
    }
    
    /**
     * 镜像翻转图像
     */
    public static Bitmap flipImage(Bitmap bitmap, boolean horizontal, boolean vertical) {
        if (bitmap == null) {
            return null;
        }
        
        Matrix matrix = new Matrix();
        if (horizontal) {
            matrix.preScale(-1, 1);
        }
        if (vertical) {
            matrix.preScale(1, -1);
        }
        
        return Bitmap.createBitmap(bitmap, 0, 0, bitmap.getWidth(), bitmap.getHeight(), 
                matrix, true);
    }
    
    /**
     * 增强图像对比度和亮度
     */
    public static Bitmap enhanceImage(Bitmap bitmap, float contrast, float brightness) {
        if (bitmap == null) {
            return null;
        }
        
        // 这里可以实现更复杂的图像增强算法
        // 目前返回原图，后续可以添加OpenCV等库进行高级处理
        return bitmap;
    }
    
    /**
     * 计算图像质量分数
     * 基于清晰度、对比度等指标
     */
    public static float calculateImageQuality(Bitmap bitmap) {
        if (bitmap == null) {
            return 0f;
        }
        
        // 简单的质量评估：基于图像尺寸
        int width = bitmap.getWidth();
        int height = bitmap.getHeight();
        
        // 推荐尺寸为 224x224 到 512x512
        if (width < 100 || height < 100) {
            return 0.3f; // 质量较差
        } else if (width < 224 || height < 224) {
            return 0.6f; // 质量一般
        } else if (width <= 512 && height <= 512) {
            return 0.9f; // 质量良好
        } else {
            return 0.8f; // 质量较好但可能过大
        }
    }
    
    /**
     * 检查图像是否适合人脸识别
     */
    public static boolean isImageSuitableForRecognition(Bitmap bitmap, float minQuality) {
        if (bitmap == null) {
            return false;
        }
        
        float quality = calculateImageQuality(bitmap);
        return quality >= minQuality;
    }
}