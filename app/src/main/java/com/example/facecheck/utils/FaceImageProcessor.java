package com.example.facecheck.utils;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.ColorMatrix;
import android.graphics.ColorMatrixColorFilter;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffXfermode;
import android.graphics.Rect;
import android.graphics.RectF;
import android.renderscript.Allocation;
import android.renderscript.Element;
import android.renderscript.RenderScript;
import android.renderscript.ScriptIntrinsicBlur;
import android.renderscript.ScriptIntrinsicConvolve3x3;
import android.util.Log;

/**
 * 人脸图像处理工具类
 * 提供人脸图像的修正、旋转、缩放、增强等功能
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
        
        Bitmap enhancedBitmap = Bitmap.createBitmap(
                bitmap.getWidth(), bitmap.getHeight(), Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(enhancedBitmap);
        
        // 创建颜色矩阵
        ColorMatrix cm = new ColorMatrix();
        
        // 设置对比度 (1.0 是正常值)
        cm.set(new float[] {
                contrast, 0, 0, 0, brightness * 255,
                0, contrast, 0, 0, brightness * 255,
                0, 0, contrast, 0, brightness * 255,
                0, 0, 0, 1, 0
        });
        
        Paint paint = new Paint();
        paint.setColorFilter(new ColorMatrixColorFilter(cm));
        canvas.drawBitmap(bitmap, 0, 0, paint);
        
        return enhancedBitmap;
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
     * 应用高斯模糊
     * @param context 上下文
     * @param bitmap 原始图像
     * @param radius 模糊半径 (1-25)
     */
    public static Bitmap applyGaussianBlur(Context context, Bitmap bitmap, float radius) {
        if (bitmap == null || context == null) {
            return null;
        }
        
        Bitmap outputBitmap = Bitmap.createBitmap(
                bitmap.getWidth(), bitmap.getHeight(), Bitmap.Config.ARGB_8888);
        
        RenderScript rs = RenderScript.create(context);
        ScriptIntrinsicBlur blurScript = ScriptIntrinsicBlur.create(rs, Element.U8_4(rs));
        
        Allocation allIn = Allocation.createFromBitmap(rs, bitmap);
        Allocation allOut = Allocation.createFromBitmap(rs, outputBitmap);
        
        blurScript.setRadius(radius);
        blurScript.setInput(allIn);
        blurScript.forEach(allOut);
        
        allOut.copyTo(outputBitmap);
        rs.destroy();
        
        return outputBitmap;
    }
    
    /**
     * 应用锐化滤镜
     * @param context 上下文
     * @param bitmap 原始图像
     */
    public static Bitmap applySharpen(Context context, Bitmap bitmap) {
        if (bitmap == null || context == null) {
            return null;
        }
        
        Bitmap outputBitmap = Bitmap.createBitmap(
                bitmap.getWidth(), bitmap.getHeight(), Bitmap.Config.ARGB_8888);
        
        RenderScript rs = RenderScript.create(context);
        ScriptIntrinsicConvolve3x3 convolve = ScriptIntrinsicConvolve3x3.create(rs, Element.U8_4(rs));
        
        // 锐化卷积核
        float[] sharpKernel = {
                0, -1, 0,
                -1, 5, -1,
                0, -1, 0
        };
        
        Allocation allIn = Allocation.createFromBitmap(rs, bitmap);
        Allocation allOut = Allocation.createFromBitmap(rs, outputBitmap);
        
        convolve.setCoefficients(sharpKernel);
        convolve.setInput(allIn);
        convolve.forEach(allOut);
        
        allOut.copyTo(outputBitmap);
        rs.destroy();
        
        return outputBitmap;
    }
    
    /**
     * 应用美颜效果
     * @param context 上下文
     * @param bitmap 原始图像
     * @param smoothLevel 平滑级别 (0.0-1.0)
     * @param brightLevel 亮度级别 (0.0-0.3)
     */
    public static Bitmap applyBeautify(Context context, Bitmap bitmap, float smoothLevel, float brightLevel) {
        if (bitmap == null) {
            return null;
        }
        
        // 步骤1: 平滑皮肤 (轻度高斯模糊)
        Bitmap smoothed = applyGaussianBlur(context, bitmap, smoothLevel * 10f);
        
        // 步骤2: 增强对比度和亮度
        Bitmap enhanced = enhanceImage(smoothed, 1.1f, brightLevel);
        
        return enhanced;
    }
    
    /**
     * 应用完整的人脸修复/增强处理
     * @param context 上下文
     * @param faceBitmap 人脸图像
     * @return 修复后的人脸图像
     */
    public static Bitmap repairAndEnhanceFace(Context context, Bitmap faceBitmap) {
        if (faceBitmap == null || context == null) {
            return null;
        }
        
        try {
            // 步骤1: 标准化人脸尺寸
            Bitmap normalized = normalizeFaceImage(faceBitmap, 512);
            
            // 步骤2: 应用美颜效果
            Bitmap beautified = applyBeautify(context, normalized, 0.3f, 0.1f);
            
            // 步骤3: 应用锐化增强细节
            Bitmap sharpened = applySharpen(context, beautified);
            
            return sharpened;
        } catch (Exception e) {
            Log.e(TAG, "人脸修复失败", e);
            return faceBitmap; // 出错时返回原图
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