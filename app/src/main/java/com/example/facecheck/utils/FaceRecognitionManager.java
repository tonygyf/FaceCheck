package com.example.facecheck.utils;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.PointF;
import android.graphics.Rect;
import android.util.Log;

import com.example.facecheck.database.DatabaseHelper;
import com.example.facecheck.data.model.FaceEmbedding;
import com.example.facecheck.data.model.Student;
import com.google.mlkit.vision.face.Face;
import com.google.mlkit.vision.face.FaceLandmark;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * 人脸识别管理器
 * 负责人脸特征提取、存储和比对
 */
public class FaceRecognitionManager {
    
    private static final String TAG = "FaceRecognitionManager";
    private static final String MODEL_VERSION = "v1.0";
    private static final float SIMILARITY_THRESHOLD = 0.75f; // 相似度阈值
    private static final int FEATURE_VECTOR_SIZE = 256; // 特征向量维度 - 增加特征维度以提高识别精度
    
    private final Context context;
    private final DatabaseHelper databaseHelper;
    private final ImageStorageManager imageStorageManager;
    
    public FaceRecognitionManager(Context context) {
        this.context = context;
        this.databaseHelper = new DatabaseHelper(context);
        this.imageStorageManager = new ImageStorageManager(context);
    }
    
    /**
     * 提取人脸特征向量
     * 基于人脸几何特征、纹理特征和图像特征
     */
    public float[] extractFaceFeatures(Bitmap faceBitmap, Face face) {
        try {
            // 1. 获取人脸关键点
            List<FaceLandmark> landmarks = new ArrayList<>();
            
            // 获取左眼
            FaceLandmark leftEye = face.getLandmark(FaceLandmark.LEFT_EYE);
            if (leftEye != null) landmarks.add(leftEye);
            
            // 获取右眼
            FaceLandmark rightEye = face.getLandmark(FaceLandmark.RIGHT_EYE);
            if (rightEye != null) landmarks.add(rightEye);
            
            // 获取鼻子
            FaceLandmark nose = face.getLandmark(FaceLandmark.NOSE_BASE);
            if (nose != null) landmarks.add(nose);
            
            // 获取左嘴角
            FaceLandmark leftMouth = face.getLandmark(FaceLandmark.MOUTH_LEFT);
            if (leftMouth != null) landmarks.add(leftMouth);
            
            // 获取右嘴角
            FaceLandmark rightMouth = face.getLandmark(FaceLandmark.MOUTH_RIGHT);
            if (rightMouth != null) landmarks.add(rightMouth);
            
            // 获取额外的关键点以增强特征提取
            FaceLandmark leftCheek = face.getLandmark(FaceLandmark.LEFT_CHEEK);
            if (leftCheek != null) landmarks.add(leftCheek);
            
            FaceLandmark rightCheek = face.getLandmark(FaceLandmark.RIGHT_CHEEK);
            if (rightCheek != null) landmarks.add(rightCheek);
            
            // 2. 基于关键点生成增强特征向量
            float[] features = generateEnhancedFeatureVector(faceBitmap, landmarks, face);
            
            // 3. 归一化特征向量
            return normalizeVector(features);
            
        } catch (Exception e) {
            Log.e(TAG, "提取人脸特征失败: " + e.getMessage(), e);
            return null;
        }
    }
    
    /**
     * 生成增强特征向量
     * 基于人脸几何特征、纹理特征、深度学习特征和高级统计特征
     */
    private float[] generateEnhancedFeatureVector(Bitmap faceBitmap, List<FaceLandmark> landmarks, Face face) {
        float[] features = new float[FEATURE_VECTOR_SIZE];
        
        try {
            // 1. 几何特征 (前80维) - 增强的几何关系和比例特征
            int index = 0;
            
            // 眼距特征
            FaceLandmark leftEye = getLandmarkByType(landmarks, FaceLandmark.LEFT_EYE);
            FaceLandmark rightEye = getLandmarkByType(landmarks, FaceLandmark.RIGHT_EYE);
            if (leftEye != null && rightEye != null) {
                float eyeDistance = calculateDistance(leftEye.getPosition(), rightEye.getPosition());
                features[index++] = eyeDistance / faceBitmap.getWidth(); // 归一化眼距
                
                // 眼睛中心点Y坐标差（面部倾斜特征）
                float eyeYDiff = Math.abs(leftEye.getPosition().y - rightEye.getPosition().y);
                features[index++] = eyeYDiff / faceBitmap.getHeight();
                
                // 新增：眼睛纵横比（眼睛形状特征）
                float eyeAspectRatio = calculateEyeAspectRatio(leftEye, rightEye, faceBitmap);
                features[index++] = eyeAspectRatio;
                
                // 新增：眼睛到面部中心距离比例
                android.graphics.PointF faceCenter = new android.graphics.PointF(faceBitmap.getWidth() / 2f, faceBitmap.getHeight() / 2f);
                float leftEyeToCenter = calculateDistance(leftEye.getPosition(), faceCenter);
                float rightEyeToCenter = calculateDistance(rightEye.getPosition(), faceCenter);
                features[index++] = leftEyeToCenter / faceBitmap.getWidth();
                features[index++] = rightEyeToCenter / faceBitmap.getWidth();
            }
            
            // 眼鼻距特征
            FaceLandmark nose = getLandmarkByType(landmarks, FaceLandmark.NOSE_BASE);
            if (leftEye != null && nose != null) {
                float eyeNoseDistance = calculateDistance(leftEye.getPosition(), nose.getPosition());
                features[index++] = eyeNoseDistance / faceBitmap.getHeight(); // 归一化眼鼻距
                
                // 新增：鼻子到面部中心距离
                android.graphics.PointF faceCenter = new android.graphics.PointF(faceBitmap.getWidth() / 2f, faceBitmap.getHeight() / 2f);
                float noseToCenter = calculateDistance(nose.getPosition(), faceCenter);
                features[index++] = noseToCenter / faceBitmap.getHeight();
            }
            
            // 嘴部特征
            FaceLandmark leftMouth = getLandmarkByType(landmarks, FaceLandmark.MOUTH_LEFT);
            FaceLandmark rightMouth = getLandmarkByType(landmarks, FaceLandmark.MOUTH_RIGHT);
            if (leftMouth != null && rightMouth != null) {
                float mouthWidth = calculateDistance(leftMouth.getPosition(), rightMouth.getPosition());
                features[index++] = mouthWidth / faceBitmap.getWidth(); // 归一化嘴宽
                
                // 嘴部中心点
                android.graphics.PointF mouthCenter = new android.graphics.PointF(
                    (leftMouth.getPosition().x + rightMouth.getPosition().x) / 2,
                    (leftMouth.getPosition().y + rightMouth.getPosition().y) / 2
                );
                
                if (nose != null) {
                    float mouthToNose = calculateDistance(mouthCenter, nose.getPosition());
                    features[index++] = mouthToNose / faceBitmap.getHeight();
                }
                
                // 新增：嘴部纵横比
                FaceLandmark upperLip = getLandmarkByType(landmarks, FaceLandmark.MOUTH_BOTTOM);
                if (upperLip != null) {
                    float mouthHeight = Math.abs(mouthCenter.y - upperLip.getPosition().y);
                    features[index++] = mouthHeight > 0 ? mouthWidth / mouthHeight : 0.0f;
                }
            }
            
            // 面部轮廓特征
            FaceLandmark leftCheek = getLandmarkByType(landmarks, FaceLandmark.LEFT_CHEEK);
            FaceLandmark rightCheek = getLandmarkByType(landmarks, FaceLandmark.RIGHT_CHEEK);
            if (leftCheek != null && rightCheek != null) {
                float cheekDistance = calculateDistance(leftCheek.getPosition(), rightCheek.getPosition());
                features[index++] = cheekDistance / faceBitmap.getWidth();
                
                // 新增：脸颊到面部中心的对称性
                android.graphics.PointF faceCenter = new android.graphics.PointF(faceBitmap.getWidth() / 2f, faceBitmap.getHeight() / 2f);
                float leftCheekToCenter = calculateDistance(leftCheek.getPosition(), faceCenter);
                float rightCheekToCenter = calculateDistance(rightCheek.getPosition(), faceCenter);
                features[index++] = Math.abs(leftCheekToCenter - rightCheekToCenter) / faceBitmap.getWidth();
            }
            
            // 面部比例特征
            android.graphics.Rect boundingBox = face.getBoundingBox();
            features[index++] = (float) boundingBox.width() / boundingBox.height(); // 宽高比
            features[index++] = face.getSmilingProbability() != null ? face.getSmilingProbability() : 0.5f;
            features[index++] = face.getLeftEyeOpenProbability() != null ? face.getLeftEyeOpenProbability() : 0.5f;
            features[index++] = face.getRightEyeOpenProbability() != null ? face.getRightEyeOpenProbability() : 0.5f;
            
            // 新增：面部姿态角度特征
            features[index++] = Math.abs(face.getHeadEulerAngleX()) / 90.0f; // 俯仰角归一化
            features[index++] = Math.abs(face.getHeadEulerAngleY()) / 90.0f; // 偏航角归一化
            features[index++] = Math.abs(face.getHeadEulerAngleZ()) / 90.0f; // 滚转角归一化
            
            // 2. 生成增强纹理特征
            index = generateEnhancedTextureFeatures(faceBitmap, features, index);
            
            // 3. 生成增强图像块特征
        index = generateEnhancedImageBlockFeatures(faceBitmap, features, index);

        // 4. 生成深度学习特征
        index = generateDeepLearningFeatures(faceBitmap, features, index);

        // 5. 填充剩余维度
        while (index < features.length) {
            features[index++] = 0.0f;
        }

    } catch (Exception e) {
        Log.e(TAG, "生成特征向量失败: " + e.getMessage(), e);
        return new float[FEATURE_VECTOR_SIZE]; // 返回零向量
    }

    return features; // 返回完整的特征向量
}

/**
 * 计算眼睛纵横比
 */
private float calculateEyeAspectRatio(FaceLandmark leftEye, FaceLandmark rightEye, Bitmap bitmap) {
    try {
        // 计算眼睛区域的近似纵横比
        float eyeWidth = calculateDistance(leftEye.getPosition(), rightEye.getPosition());
        float eyeHeight = Math.abs(leftEye.getPosition().y - rightEye.getPosition().y) + 10; // 近似高度
        return eyeWidth > 0 ? eyeHeight / eyeWidth : 0.0f;
    } catch (Exception e) {
        Log.e(TAG, "计算眼睛纵横比失败: " + e.getMessage(), e);
        return 0.0f;
    }
}

/**
 * 生成增强纹理特征 (LBP、Gabor和HOG特征)
 */
private int generateEnhancedTextureFeatures(Bitmap faceBitmap, float[] features, int startIndex) {
    int index = startIndex;

    try {
        // LBP特征 (局部二值模式)
        int lbpRadius = 1;
        int lbpNeighbors = 8;
        int lbpBins = 256; // 2^8

        // 计算LBP特征直方图
        float[] lbpHistogram = calculateLBPHistogram(faceBitmap, lbpRadius, lbpNeighbors, lbpBins);

        // 归一化LBP直方图并添加到特征向量
        for (int i = 0; i < Math.min(lbpHistogram.length, 20) && index < features.length; i++) {
            features[index++] = lbpHistogram[i] / (faceBitmap.getWidth() * faceBitmap.getHeight());
        }

        // Gabor特征 (纹理方向特征)
        int gaborOrientations = 8; // 增加方向数量
        int gaborScales = 3; // 增加尺度数量

        for (int scale = 0; scale < gaborScales && index < features.length; scale++) {
            for (int orientation = 0; orientation < gaborOrientations && index < features.length; orientation++) {
                double theta = orientation * Math.PI / gaborOrientations;
                double sigma = 1.0 + scale * 1.5; // 调整sigma值

                float gaborResponse = calculateGaborResponse(faceBitmap, theta, sigma);
                features[index++] = gaborResponse / 255.0f; // 归一化
            }
        }

        // 新增：HOG特征 (方向梯度直方图)
        float[] hogFeatures = calculateHOGFeatures(faceBitmap);
        for (int i = 0; i < Math.min(hogFeatures.length, 16) && index < features.length; i++) {
            features[index++] = hogFeatures[i];
        }

        // 新增：颜色直方图特征
        float[] colorHistogram = calculateColorHistogram(faceBitmap);
        for (int i = 0; i < Math.min(colorHistogram.length, 12) && index < features.length; i++) {
            features[index++] = colorHistogram[i];
        }

    } catch (Exception e) {
        Log.e(TAG, "生成增强纹理特征失败: " + e.getMessage(), e);
        // 填充默认值
        while (index < startIndex + 80 && index < features.length) {
            features[index++] = 0.0f;
        }
    }

    return index;
}

/**
 * 生成增强图像块特征 (多尺度分析)
 */
private int generateEnhancedImageBlockFeatures(Bitmap faceBitmap, float[] features, int startIndex) {
    int index = startIndex;

    try {
        // 多尺度块分析
        int[] scales = {4, 8, 16}; // 不同尺度的块大小

        for (int scaleIndex = 0; scaleIndex < scales.length && index < features.length; scaleIndex++) {
            int blockSize = scales[scaleIndex];
            int blocksX = faceBitmap.getWidth() / blockSize;
            int blocksY = faceBitmap.getHeight() / blockSize;

            // 对每个块提取统计特征
            for (int y = 0; y < blocksY && index < features.length; y++) {
                for (int x = 0; x < blocksX && index < features.length; x++) {
                    float[] blockFeatures = calculateEnhancedBlockFeatures(faceBitmap, 
                        x * blockSize, y * blockSize, blockSize);

                    // 添加块的多种统计特征
                    features[index++] = blockFeatures[0] / 255.0f; // 平均灰度
                    features[index++] = Math.min(blockFeatures[1] / 128.0f, 1.0f); // 标准差
                    features[index++] = blockFeatures[2] / 255.0f; // 最小值
                    features[index++] = blockFeatures[3] / 255.0f; // 最大值
                    features[index++] = blockFeatures[4]; // 偏度
                    features[index++] = blockFeatures[5]; // 峰度
                }
            }
        }

    } catch (Exception e) {
        Log.e(TAG, "生成增强图像块特征失败: " + e.getMessage(), e);
        // 填充默认值
        while (index < startIndex + 80 && index < features.length) {
            features[index++] = 0.0f;
        }
    }

    return index;
}

/**
 * 生成深度学习特征 (模拟CNN特征提取)
 */
private int generateDeepLearningFeatures(Bitmap faceBitmap, float[] features, int startIndex) {
    int index = startIndex;

    try {
        // 模拟CNN特征提取：使用多尺度卷积核响应
        int[] kernelSizes = {3, 5, 7}; // 不同大小的卷积核
        int[] orientations = {0, 45, 90, 135}; // 不同方向

        for (int kernelSize : kernelSizes) {
            for (int orientation : orientations) {
                if (index >= features.length) break;

                float convResponse = calculateConvolutionResponse(faceBitmap, kernelSize, orientation);
                features[index++] = Math.min(convResponse / 255.0f, 1.0f);
            }
        }

        // 模拟池化特征
        float[] poolingFeatures = calculatePoolingFeatures(faceBitmap);
        for (int i = 0; i < Math.min(poolingFeatures.length, 16) && index < features.length; i++) {
            features[index++] = poolingFeatures[i];
        }

    } catch (Exception e) {
        Log.e(TAG, "生成深度学习特征失败: " + e.getMessage(), e);
        // 填充默认值
        while (index < features.length) {
            features[index++] = 0.0f;
        }
    }

    return index;
}
    
    /**
     * 计算LBP直方图
     */
    private float[] calculateLBPHistogram(Bitmap bitmap, int radius, int neighbors, int bins) {
        float[] histogram = new float[bins];
        int width = bitmap.getWidth();
        int height = bitmap.getHeight();
        
        for (int y = radius; y < height - radius; y++) {
            for (int x = radius; x < width - radius; x++) {
                int centerPixel = getGrayValue(bitmap.getPixel(x, y));
                int lbpValue = 0;
                
                // 计算LBP值
                for (int n = 0; n < neighbors; n++) {
                    double angle = 2 * Math.PI * n / neighbors;
                    int nx = (int) Math.round(x + radius * Math.cos(angle));
                    int ny = (int) Math.round(y - radius * Math.sin(angle));
                    
                    if (nx >= 0 && nx < width && ny >= 0 && ny < height) {
                        int neighborPixel = getGrayValue(bitmap.getPixel(nx, ny));
                        if (neighborPixel >= centerPixel) {
                            lbpValue += 1 << n;
                        }
                    }
                }
                
                histogram[lbpValue]++;
            }
        }
        
        return histogram;
    }
    
    /**
     * 计算Gabor响应
     */
    private float calculateGaborResponse(Bitmap bitmap, double theta, double sigma) {
        int width = bitmap.getWidth();
        int height = bitmap.getHeight();
        float totalResponse = 0;
        int pixelCount = 0;
        
        // 简化的Gabor滤波器实现
        int kernelSize = (int) (6 * sigma);
        if (kernelSize % 2 == 0) kernelSize++;
        
        for (int y = kernelSize/2; y < height - kernelSize/2; y++) {
            for (int x = kernelSize/2; x < width - kernelSize/2; x++) {
                float gaborValue = applyGaborFilter(bitmap, x, y, theta, sigma, kernelSize);
                totalResponse += Math.abs(gaborValue);
                pixelCount++;
            }
        }
        
        return pixelCount > 0 ? totalResponse / pixelCount : 0;
    }
    
    /**
     * 应用Gabor滤波器
     */
    private float applyGaborFilter(Bitmap bitmap, int centerX, int centerY, double theta, double sigma, int kernelSize) {
        float response = 0;
        int halfSize = kernelSize / 2;
        
        for (int dy = -halfSize; dy <= halfSize; dy++) {
            for (int dx = -halfSize; dx <= halfSize; dx++) {
                int x = centerX + dx;
                int y = centerY + dy;
                
                if (x >= 0 && x < bitmap.getWidth() && y >= 0 && y < bitmap.getHeight()) {
                    double x_theta = dx * Math.cos(theta) + dy * Math.sin(theta);
                    double y_theta = -dx * Math.sin(theta) + dy * Math.cos(theta);
                    
                    // Gabor核函数
                    double gaussian = Math.exp(-(x_theta*x_theta + y_theta*y_theta) / (2*sigma*sigma));
                    double sinusoid = Math.cos(2*Math.PI*x_theta/4.0); // 波长为4
                    
                    int pixel = getGrayValue(bitmap.getPixel(x, y));
                    response += pixel * gaussian * sinusoid;
                }
            }
        }
        
        return response;
    }
    
    /**
     * 计算增强图像块特征 (包含统计矩)
     */
    private float[] calculateEnhancedBlockFeatures(Bitmap bitmap, int startX, int startY, int blockSize) {
        float[] features = new float[6]; // 均值、标准差、最小值、最大值、偏度、峰度
        
        try {
            int endX = Math.min(startX + blockSize, bitmap.getWidth());
            int endY = Math.min(startY + blockSize, bitmap.getHeight());
            
            List<Integer> grayValues = new ArrayList<>();
            
            // 收集灰度值
            for (int y = startY; y < endY; y++) {
                for (int x = startX; x < endX; x++) {
                    int pixel = bitmap.getPixel(x, y);
                    int gray = (int)(0.299 * Color.red(pixel) + 0.587 * Color.green(pixel) + 0.114 * Color.blue(pixel));
                    grayValues.add(gray);
                }
            }
            
            if (grayValues.isEmpty()) {
                return features; // 返回特征数组
            }
            
            // 计算基本统计特征
            int n = grayValues.size();
            double sum = 0, sum2 = 0, sum3 = 0, sum4 = 0;
            
            for (int gray : grayValues) {
                sum += gray;
                sum2 += gray * gray;
            }
            
            double mean = sum / n;
            features[0] = (float)mean; // 均值
            
            double variance = (sum2 - sum * sum / n) / n;
            features[1] = (float)Math.sqrt(variance); // 标准差
            
            // 计算最小值和最大值
            Collections.sort(grayValues);
            features[2] = grayValues.get(0); // 最小值
            features[3] = grayValues.get(n - 1); // 最大值
            
            // 计算偏度和峰度
            for (int gray : grayValues) {
                double diff = gray - mean;
                double diff2 = diff * diff;
                sum3 += diff2 * diff;
                sum4 += diff2 * diff2;
            }
            
            double skewness = sum3 / (n * Math.pow(variance, 1.5));
            double kurtosis = sum4 / (n * variance * variance) - 3;
            
            features[4] = (float)skewness; // 偏度
            features[5] = (float)kurtosis; // 峰度
            
        } catch (Exception e) {
            Log.e(TAG, "计算增强块特征失败: " + e.getMessage(), e);
        }
        
        return features; // 返回特征数组
    }
    
    /**
     * 计算HOG特征 (方向梯度直方图)
     */
    private float[] calculateHOGFeatures(Bitmap bitmap) {
        float[] hogFeatures = new float[16];
        
        try {
            int width = bitmap.getWidth();
            int height = bitmap.getHeight();
            
            // 计算梯度
            float[][] gradientX = new float[height][width];
            float[][] gradientY = new float[height][width];
            
            for (int y = 1; y < height - 1; y++) {
                for (int x = 1; x < width - 1; x++) {
                    // Sobel算子计算梯度
                    int gx = getGrayValue(bitmap.getPixel(x + 1, y)) - getGrayValue(bitmap.getPixel(x - 1, y));
        int gy = getGrayValue(bitmap.getPixel(x, y + 1)) - getGrayValue(bitmap.getPixel(x, y - 1));
                    
                    gradientX[y][x] = gx;
                    gradientY[y][x] = gy;
                }
            }
            
            // 计算方向直方图 (8个方向)
            int[] histogram = new int[8];
            
            for (int y = 1; y < height - 1; y++) {
                for (int x = 1; x < width - 1; x++) {
                    float gx = gradientX[y][x];
                    float gy = gradientY[y][x];
                    
                    float magnitude = (float)Math.sqrt(gx * gx + gy * gy);
                    float orientation = (float)Math.atan2(gy, gx);
                    
                    if (magnitude > 10) { // 阈值过滤
                        int bin = (int)((orientation + Math.PI) * 4 / Math.PI) % 8;
                        histogram[bin] += magnitude;
                    }
                }
            }
            
            // 归一化直方图
            int total = 0;
            for (int h : histogram) total += h;
            
            if (total > 0) {
                for (int i = 0; i < 8; i++) {
                    hogFeatures[i] = (float)histogram[i] / total;
                }
            }
            
            // 添加块级别的HOG特征
            int blockSize = Math.min(width, height) / 4;
            for (int blockY = 0; blockY < 2; blockY++) {
                for (int blockX = 0; blockX < 2; blockX++) {
                    float[] blockHog = calculateBlockHOG(bitmap, 
                        blockX * blockSize, blockY * blockSize, blockSize);
                    
                    int featureIndex = 8 + blockY * 4 + blockX * 2;
                    if (featureIndex < hogFeatures.length) {
                        hogFeatures[featureIndex] = blockHog[0]; // 主方向强度
                        if (featureIndex + 1 < hogFeatures.length) {
                            hogFeatures[featureIndex + 1] = blockHog[1]; // 方向变化
                        }
                    }
                }
            }
            
        } catch (Exception e) {
            Log.e(TAG, "计算HOG特征失败: " + e.getMessage(), e);
        }
        
        return hogFeatures;
    }
    
    /**
     * 计算颜色直方图特征
     */
    private float[] calculateColorHistogram(Bitmap bitmap) {
        float[] colorFeatures = new float[12];
        
        try {
            int[] rHistogram = new int[8];
            int[] gHistogram = new int[8];
            int[] bHistogram = new int[8];
            
            int width = bitmap.getWidth();
            int height = bitmap.getHeight();
            int totalPixels = width * height;
            
            // 统计RGB颜色分布
            for (int y = 0; y < height; y++) {
                for (int x = 0; x < width; x++) {
                    int pixel = bitmap.getPixel(x, y);
                    
                    int r = Color.red(pixel) / 32; // 8个bin
                    int g = Color.green(pixel) / 32;
                    int b = Color.blue(pixel) / 32;
                    
                    rHistogram[r]++;
                    gHistogram[g]++;
                    bHistogram[b]++;
                }
            }
            
            // 归一化直方图
            for (int i = 0; i < 8; i++) {
                colorFeatures[i] = (float)rHistogram[i] / totalPixels;
                colorFeatures[i + 4] = (float)gHistogram[i] / totalPixels;
                colorFeatures[i + 8] = (float)bHistogram[i] / totalPixels;
            }
            
        } catch (Exception e) {
            Log.e(TAG, "计算颜色直方图失败: " + e.getMessage(), e);
        }
        
        return colorFeatures;
    }
    
    /**
     * 计算卷积响应 (模拟CNN特征)
     */
    private float calculateConvolutionResponse(Bitmap bitmap, int kernelSize, int orientation) {
        float response = 0;
        
        try {
            int width = bitmap.getWidth();
            int height = bitmap.getHeight();
            
            // 创建方向卷积核
            float[][] kernel = createDirectionalKernel(kernelSize, orientation);
            
            int center = kernelSize / 2;
            int count = 0;
            
            // 应用卷积
            for (int y = center; y < height - center; y += 2) { // 降采样以提高效率
                for (int x = center; x < width - center; x += 2) {
                    float sum = 0;
                    
                    for (int ky = 0; ky < kernelSize; ky++) {
                        for (int kx = 0; kx < kernelSize; kx++) {
                            int pixelX = x + kx - center;
                            int pixelY = y + ky - center;
                            
                            int gray = getGrayValue(bitmap.getPixel(pixelX, pixelY));
                            sum += gray * kernel[ky][kx];
                        }
                    }
                    
                    response += Math.abs(sum);
                    count++;
                }
            }
            
            if (count > 0) {
                response /= count;
            }
            
        } catch (Exception e) {
            Log.e(TAG, "计算卷积响应失败: " + e.getMessage(), e);
        }
        
        return response;
    }
    
    /**
     * 创建方向卷积核
     */
    private float[][] createDirectionalKernel(int size, int orientation) {
        float[][] kernel = new float[size][size];
        
        try {
            int center = size / 2;
            double angle = orientation * Math.PI / 180.0;
            double cos = Math.cos(angle);
            double sin = Math.sin(angle);
            
            // 创建方向滤波器
            for (int y = 0; y < size; y++) {
                for (int x = 0; x < size; x++) {
                    int dx = x - center;
                    int dy = y - center;
                    
                    // 计算方向响应
                    double distance = Math.abs(dx * sin - dy * cos);
                    double response = Math.exp(-distance * distance / (2.0 * center * center));
                    
                    kernel[y][x] = (float)response;
                }
            }
            
            // 归一化
            float sum = 0;
            for (int y = 0; y < size; y++) {
                for (int x = 0; x < size; x++) {
                    sum += kernel[y][x];
                }
            }
            
            if (sum > 0) {
                for (int y = 0; y < size; y++) {
                    for (int x = 0; x < size; x++) {
                        kernel[y][x] /= sum;
                    }
                }
            }
            
        } catch (Exception e) {
            Log.e(TAG, "创建方向卷积核失败: " + e.getMessage(), e);
        }
        
        return kernel;
    }
    
    /**
     * 计算池化特征 (模拟CNN池化)
     */
    private float[] calculatePoolingFeatures(Bitmap bitmap) {
        float[] poolingFeatures = new float[16];
        
        try {
            int width = bitmap.getWidth();
            int height = bitmap.getHeight();
            
            // 最大池化 (2x2区域)
            int poolSize = 2;
            int poolX = width / poolSize;
            int poolY = height / poolSize;
            
            int featureIndex = 0;
            
            for (int y = 0; y < poolY && featureIndex < poolingFeatures.length; y++) {
                for (int x = 0; x < poolX && featureIndex < poolingFeatures.length; x++) {
                    int maxGray = 0;
                    int avgGray = 0;
                    int count = 0;
                    
                    // 在池化区域内找到最大值和平均值
                    for (int py = y * poolSize; py < (y + 1) * poolSize && py < height; py++) {
                        for (int px = x * poolSize; px < (x + 1) * poolSize && px < width; px++) {
                            int gray = getGrayValue(bitmap.getPixel(px, py));
                            maxGray = Math.max(maxGray, gray);
                            avgGray += gray;
                            count++;
                        }
                    }
                    
                    if (count > 0) {
                        avgGray /= count;
                        poolingFeatures[featureIndex++] = maxGray / 255.0f; // 最大池化
                        poolingFeatures[featureIndex++] = avgGray / 255.0f; // 平均池化
                    }
                }
            }
            
        } catch (Exception e) {
            Log.e(TAG, "计算池化特征失败: " + e.getMessage(), e);
        }
        
        return poolingFeatures;
    }
    
    /**
     * 计算块级HOG特征
     */
    private float[] calculateBlockHOG(Bitmap bitmap, int startX, int startY, int blockSize) {
        float[] blockHog = new float[2];
        
        try {
            int endX = Math.min(startX + blockSize, bitmap.getWidth());
            int endY = Math.min(startY + blockSize, bitmap.getHeight());
            
            int[] histogram = new int[8];
            int totalGradients = 0;
            
            // 计算块内的梯度方向
            for (int y = startY + 1; y < endY - 1; y++) {
                for (int x = startX + 1; x < endX - 1; x++) {
                    int gx = getGrayValue(bitmap.getPixel(x + 1, y)) - getGrayValue(bitmap.getPixel(x - 1, y));
        int gy = getGrayValue(bitmap.getPixel(x, y + 1)) - getGrayValue(bitmap.getPixel(x, y - 1));
                    
                    float magnitude = (float)Math.sqrt(gx * gx + gy * gy);
                    
                    if (magnitude > 5) {
                        float orientation = (float)Math.atan2(gy, gx);
                        int bin = (int)((orientation + Math.PI) * 4 / Math.PI) % 8;
                        histogram[bin] += magnitude;
                        totalGradients += magnitude;
                    }
                }
            }
            
            if (totalGradients > 0) {
                // 找到主方向
                int maxBin = 0;
                for (int i = 1; i < 8; i++) {
                    if (histogram[i] > histogram[maxBin]) {
                        maxBin = i;
                    }
                }
                
                blockHog[0] = (float)histogram[maxBin] / totalGradients; // 主方向强度
                
                // 计算方向变化度
                float variation = 0;
                for (int i = 0; i < 8; i++) {
                    float diff = (float)histogram[i] / totalGradients - 0.125f; // 均匀分布的期望值
                    variation += diff * diff;
                }
                blockHog[1] = variation / 8.0f; // 方向变化度
            }
            
        } catch (Exception e) {
            Log.e(TAG, "计算块级HOG特征失败: " + e.getMessage(), e);
        }
        
        return blockHog;
    }
    
    /**
     * 计算图像块的特征 (原始方法)
     */
    private float[] calculateBlockFeatures(Bitmap bitmap, int startX, int startY, int blockSize) {
        float[] features = new float[2]; // [平均灰度, 标准差]
        
        int sum = 0;
        int sumSquared = 0;
        int pixelCount = 0;
        
        for (int y = startY; y < startY + blockSize && y < bitmap.getHeight(); y++) {
            for (int x = startX; x < startX + blockSize && x < bitmap.getWidth(); x++) {
                int grayValue = getGrayValue(bitmap.getPixel(x, y));
                sum += grayValue;
                sumSquared += grayValue * grayValue;
                pixelCount++;
            }
        }
        
        if (pixelCount > 0) {
            float mean = (float) sum / pixelCount;
            float variance = (float) sumSquared / pixelCount - mean * mean;
            
            features[0] = mean; // 平均灰度
            features[1] = (float) Math.sqrt(Math.max(variance, 0)); // 标准差
        }
        
        return features; // 返回特征数组
    }
    
    /**
     * 获取像素灰度值
     */
    private int getGrayValue(int pixel) {
        int red = (pixel >> 16) & 0xFF;
        int green = (pixel >> 8) & 0xFF;
        int blue = pixel & 0xFF;
        return (red + green + blue) / 3;
    }
    
    /**
     * 计算两点间的欧氏距离
     */
    private float calculateDistance(android.graphics.PointF p1, android.graphics.PointF p2) {
        float dx = p1.x - p2.x;
        float dy = p1.y - p2.y;
        return (float) Math.sqrt(dx * dx + dy * dy);
    }
    
    /**
     * 根据类型获取关键点
     */
    private FaceLandmark getLandmarkByType(List<FaceLandmark> landmarks, int type) {
        for (FaceLandmark landmark : landmarks) {
            if (landmark.getLandmarkType() == type) {
                return landmark;
            }
        }
        return null;
    }
    
    /**
     * 计算图像块的平均灰度值 (保留原有方法用于兼容性)
     */
    private float calculateBlockAverageGray(Bitmap bitmap, int startX, int startY, int blockSize) {
        long sum = 0;
        int count = 0;
        
        for (int y = startY; y < startY + blockSize && y < bitmap.getHeight(); y++) {
            for (int x = startX; x < startX + blockSize && x < bitmap.getWidth(); x++) {
                int pixel = bitmap.getPixel(x, y);
                int gray = (int) (0.299 * ((pixel >> 16) & 0xFF) + 
                                 0.587 * ((pixel >> 8) & 0xFF) + 
                                 0.114 * (pixel & 0xFF));
                sum += gray;
                count++;
            }
        }
        
        return count > 0 ? (float) sum / count : 0.0f;
    }
    
    /**
     * 归一化向量
     */
    private float[] normalizeVector(float[] vector) {
        float norm = 0.0f;
        for (float v : vector) {
            norm += v * v;
        }
        norm = (float) Math.sqrt(norm);
        
        if (norm > 0) {
            for (int i = 0; i < vector.length; i++) {
                vector[i] /= norm;
            }
        }
        
        return vector;
    }
    
    /**
     * 计算两个特征向量的余弦相似度
     */
    public float calculateSimilarity(float[] vector1, float[] vector2) {
        if (vector1 == null || vector2 == null || vector1.length != vector2.length) {
            return 0.0f;
        }
        
        float dotProduct = 0.0f;
        float norm1 = 0.0f;
        float norm2 = 0.0f;
        
        for (int i = 0; i < vector1.length; i++) {
            dotProduct += vector1[i] * vector2[i];
            norm1 += vector1[i] * vector1[i];
            norm2 += vector2[i] * vector2[i];
        }
        
        float denominator = (float) (Math.sqrt(norm1) * Math.sqrt(norm2));
        return denominator > 0 ? dotProduct / denominator : 0.0f;
    }
    
    /**
     * 将特征向量转换为字节数组（用于数据库存储）
     */
    public byte[] floatArrayToByteArray(float[] floatArray) {
        ByteBuffer buffer = ByteBuffer.allocate(floatArray.length * 4);
        buffer.order(ByteOrder.LITTLE_ENDIAN);
        for (float f : floatArray) {
            buffer.putFloat(f);
        }
        return buffer.array();
    }
    
    /**
     * 将字节数组转换为特征向量
     */
    public float[] byteArrayToFloatArray(byte[] byteArray) {
        if (byteArray.length % 4 != 0) {
            return new float[0];
        }
        
        float[] floatArray = new float[byteArray.length / 4];
        ByteBuffer buffer = ByteBuffer.wrap(byteArray);
        buffer.order(ByteOrder.LITTLE_ENDIAN);
        for (int i = 0; i < floatArray.length; i++) {
            floatArray[i] = buffer.getFloat();
        }
        return floatArray;
    }
    
    /**
     * 保存人脸特征到数据库
     */
    public boolean saveFaceEmbedding(long studentId, float[] features, float quality, String faceImagePath) {
        try {
            byte[] vectorBytes = floatArrayToByteArray(features);
            long result = databaseHelper.insertFaceEmbedding(studentId, MODEL_VERSION, vectorBytes, quality);
            return result != -1;
        } catch (Exception e) {
            Log.e(TAG, "保存人脸特征失败: " + e.getMessage(), e);
            return false;
        }
    }
    
    /**
     * 从数据库获取学生的人脸特征
     */
    public List<FaceEmbedding> getStudentFaceEmbeddings(long studentId) {
        List<FaceEmbedding> embeddings = new ArrayList<>();
        
        try {
            android.database.Cursor cursor = databaseHelper.getFaceEmbeddingsByStudent(studentId);
            if (cursor != null && cursor.moveToFirst()) {
                do {
                    long id = cursor.getLong(cursor.getColumnIndexOrThrow("id"));
                    String modelVer = cursor.getString(cursor.getColumnIndexOrThrow("modelVer"));
                    byte[] vector = cursor.getBlob(cursor.getColumnIndexOrThrow("vector"));
                    float quality = cursor.getFloat(cursor.getColumnIndexOrThrow("quality"));
                    long createdAt = cursor.getLong(cursor.getColumnIndexOrThrow("createdAt"));
                    
                    FaceEmbedding embedding = new FaceEmbedding(id, studentId, modelVer, vector, quality, createdAt);
                    embeddings.add(embedding);
                } while (cursor.moveToNext());
                
                cursor.close();
            }
        } catch (Exception e) {
            Log.e(TAG, "获取学生人脸特征失败: " + e.getMessage(), e);
        }
        
        return embeddings;
    }
    
    /**
     * 识别单个人脸
     * 返回最相似的学生ID和相似度
     */
    public RecognitionResult recognizeFace(Bitmap faceBitmap, Face face) {
        try {
            // 1. 提取待识别人脸的特征
            float[] queryFeatures = extractFaceFeatures(faceBitmap, face);
            if (queryFeatures == null) {
                return new RecognitionResult(-1, 0.0f, "特征提取失败");
            }
            
            // 2. 获取所有学生的人脸特征
            List<Student> allStudents = databaseHelper.getAllStudents();
            float bestSimilarity = 0.0f;
            long bestStudentId = -1;
            
            // 3. 与每个人脸特征进行比对
            for (Student student : allStudents) {
                List<FaceEmbedding> embeddings = getStudentFaceEmbeddings(student.getId());
                
                for (FaceEmbedding embedding : embeddings) {
                    float[] storedFeatures = byteArrayToFloatArray(embedding.getVector());
                    if (storedFeatures.length == queryFeatures.length) {
                        float similarity = calculateSimilarity(queryFeatures, storedFeatures);
                        
                        if (similarity > bestSimilarity) {
                            bestSimilarity = similarity;
                            bestStudentId = student.getId();
                        }
                    }
                }
            }
            
            // 4. 判断是否识别成功
            if (bestSimilarity >= SIMILARITY_THRESHOLD) {
                return new RecognitionResult(bestStudentId, bestSimilarity, "识别成功");
            } else {
                return new RecognitionResult(-1, bestSimilarity, "未找到匹配的学生");
            }
            
        } catch (Exception e) {
            Log.e(TAG, "人脸识别失败: " + e.getMessage(), e);
            return new RecognitionResult(-1, 0.0f, "识别异常: " + e.getMessage());
        }
    }
    
    /**
     * 批量识别多个人脸
     */
    public List<RecognitionResult> recognizeMultipleFaces(List<Bitmap> faceBitmaps, List<Face> faces) {
        List<RecognitionResult> results = new ArrayList<>();
        
        for (int i = 0; i < faceBitmaps.size(); i++) {
            RecognitionResult result = recognizeFace(faceBitmaps.get(i), faces.get(i));
            results.add(result);
        }
        
        return results;
    }
    
    /**
     * 清理资源
     */
    public void cleanup() {
        // 清理数据库连接等资源
        if (databaseHelper != null) {
            databaseHelper.close();
        }
    }
    
    /**
     * 识别结果类
     */
    public static class RecognitionResult {
        private final long studentId;
        private final float similarity;
        private final String message;
        
        public RecognitionResult(long studentId, float similarity, String message) {
            this.studentId = studentId;
            this.similarity = similarity;
            this.message = message;
        }
        
        public long getStudentId() {
            return studentId;
        }
        
        public float getSimilarity() {
            return similarity;
        }
        
        public String getMessage() {
            return message;
        }
        
        public boolean isSuccess() {
            return studentId != -1 && similarity >= SIMILARITY_THRESHOLD;
        }
    }
}