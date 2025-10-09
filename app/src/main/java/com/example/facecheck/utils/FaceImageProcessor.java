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
        
        // 创建新的Bitmap用于处理
        Bitmap enhancedBitmap = bitmap.copy(bitmap.getConfig(), true);
        
        // 获取图像的宽度和高度
        int width = enhancedBitmap.getWidth();
        int height = enhancedBitmap.getHeight();
        
        // 遍历每个像素进行增强处理
        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                int pixel = enhancedBitmap.getPixel(x, y);
                
                // 提取RGB分量
                int red = (pixel >> 16) & 0xFF;
                int green = (pixel >> 8) & 0xFF;
                int blue = pixel & 0xFF;
                
                // 应用对比度和亮度调整
                red = adjustColorComponent(red, contrast, brightness);
                green = adjustColorComponent(green, contrast, brightness);
                blue = adjustColorComponent(blue, contrast, brightness);
                
                // 重新组合像素
                int newPixel = (0xFF << 24) | (red << 16) | (green << 8) | blue;
                enhancedBitmap.setPixel(x, y, newPixel);
            }
        }
        
        return enhancedBitmap;
    }
    
    /**
     * 调整颜色分量
     */
    private static int adjustColorComponent(int component, float contrast, float brightness) {
        // 应用对比度调整
        float adjusted = (component - 128) * contrast + 128;
        // 应用亮度调整
        adjusted += brightness * 255;
        
        // 确保值在0-255范围内
        return Math.max(0, Math.min(255, Math.round(adjusted)));
    }
    
    /**
     * 应用锐化滤镜
     */
    public static Bitmap sharpenImage(Bitmap bitmap, float strength) {
        if (bitmap == null) {
            return null;
        }
        
        // 简单的锐化核
        float[] sharpenKernel = {
            0, -strength, 0,
            -strength, 1 + 4 * strength, -strength,
            0, -strength, 0
        };
        
        return applyConvolutionFilter(bitmap, sharpenKernel, 3);
    }
    
    /**
     * 应用卷积滤波器
     */
    private static Bitmap applyConvolutionFilter(Bitmap bitmap, float[] kernel, int kernelSize) {
        if (bitmap == null || kernel.length != kernelSize * kernelSize) {
            return bitmap;
        }
        
        int width = bitmap.getWidth();
        int height = bitmap.getHeight();
        Bitmap result = Bitmap.createBitmap(width, height, bitmap.getConfig());
        
        int halfSize = kernelSize / 2;
        
        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                float red = 0, green = 0, blue = 0;
                
                // 应用卷积核
                for (int ky = 0; ky < kernelSize; ky++) {
                    for (int kx = 0; kx < kernelSize; kx++) {
                        int pixelX = x + kx - halfSize;
                        int pixelY = y + ky - halfSize;
                        
                        // 边界处理：使用边缘像素
                        pixelX = Math.max(0, Math.min(width - 1, pixelX));
                        pixelY = Math.max(0, Math.min(height - 1, pixelY));
                        
                        int pixel = bitmap.getPixel(pixelX, pixelY);
                        float weight = kernel[ky * kernelSize + kx];
                        
                        red += ((pixel >> 16) & 0xFF) * weight;
                        green += ((pixel >> 8) & 0xFF) * weight;
                        blue += (pixel & 0xFF) * weight;
                    }
                }
                
                // 确保颜色值在有效范围内
                int newRed = Math.max(0, Math.min(255, Math.round(red)));
                int newGreen = Math.max(0, Math.min(255, Math.round(green)));
                int newBlue = Math.max(0, Math.min(255, Math.round(blue)));
                
                int newPixel = (0xFF << 24) | (newRed << 16) | (newGreen << 8) | newBlue;
                result.setPixel(x, y, newPixel);
            }
        }
        
        return result;
    }
    
    /**
     * 综合人脸修复处理
     * @param faceBitmap 输入的人脸图像
     * @return 修复后的图像
     */
    public static Bitmap repairFaceImage(Bitmap faceBitmap) {
        return repairFaceImage(faceBitmap, 0.8f); // 默认使用80%质量修复
    }
    
    /**
     * 综合人脸修复处理（带质量参数）
     * @param faceBitmap 输入的人脸图像
     * @param quality 修复质量 (0.0-1.0)，影响修复强度
     * @return 修复后的图像
     */
    public static Bitmap repairFaceImage(Bitmap faceBitmap, float quality) {
        if (faceBitmap == null) {
            return null;
        }
        
        // 根据质量参数调整修复强度
        float contrastFactor = 1.0f + (quality * 0.5f); // 1.0-1.5
        float brightnessFactor = quality * 0.2f; // 0-0.2
        float sharpnessFactor = quality * 0.5f; // 0-0.5
        float blurRadius = Math.max(0.5f, 2.0f - quality); // 2.0-0.5，质量越高，模糊越少
        
        // 步骤1：增强对比度和亮度（不干扰人脸特征）
        Bitmap enhanced = enhanceImage(faceBitmap, contrastFactor, brightnessFactor);
        
        // 步骤2：应用轻度锐化（增强细节但不过度）
        Bitmap sharpened = sharpenImage(enhanced, sharpnessFactor);
        
        // 步骤3：降噪处理（轻度模糊）
        Bitmap denoised = applyGaussianBlur(sharpened, blurRadius);
        
        return denoised;
    }
    
    /**
     * 高斯模糊处理
     */
    private static Bitmap applyGaussianBlur(Bitmap bitmap, float radius) {
        if (bitmap == null || radius <= 0) {
            return bitmap;
        }
        
        // 简单的高斯核
        float[] gaussianKernel = {
            1/16f, 2/16f, 1/16f,
            2/16f, 4/16f, 2/16f,
            1/16f, 2/16f, 1/16f
        };
        
        return applyConvolutionFilter(bitmap, gaussianKernel, 3);
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