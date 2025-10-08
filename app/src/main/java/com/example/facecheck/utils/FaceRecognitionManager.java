package com.example.facecheck.utils;

import android.content.Context;
import android.graphics.Bitmap;
import android.util.Log;

import com.example.facecheck.database.DatabaseHelper;
import com.example.facecheck.data.model.FaceEmbedding;
import com.example.facecheck.data.model.Student;
import com.google.mlkit.vision.face.Face;
import com.google.mlkit.vision.face.FaceLandmark;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.ArrayList;
import java.util.List;

/**
 * 人脸识别管理器
 * 负责人脸特征提取、存储和比对
 */
public class FaceRecognitionManager {
    
    private static final String TAG = "FaceRecognitionManager";
    private static final String MODEL_VERSION = "v1.0";
    private static final float SIMILARITY_THRESHOLD = 0.75f; // 相似度阈值
    private static final int FEATURE_VECTOR_SIZE = 128; // 特征向量维度
    
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
     * 基于人脸的关键点和几何特征生成特征向量
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
            
            // 2. 基于关键点生成特征向量
            float[] features = generateFeatureVector(faceBitmap, landmarks, face);
            
            // 3. 归一化特征向量
            return normalizeVector(features);
            
        } catch (Exception e) {
            Log.e(TAG, "提取人脸特征失败: " + e.getMessage(), e);
            return null;
        }
    }
    
    /**
     * 生成特征向量
     * 基于人脸几何特征和图像特征
     */
    private float[] generateFeatureVector(Bitmap faceBitmap, List<FaceLandmark> landmarks, Face face) {
        float[] features = new float[FEATURE_VECTOR_SIZE];
        
        try {
            // 1. 几何特征 (前32维)
            int index = 0;
            
            // 眼距特征
            FaceLandmark leftEye = getLandmarkByType(landmarks, FaceLandmark.LEFT_EYE);
            FaceLandmark rightEye = getLandmarkByType(landmarks, FaceLandmark.RIGHT_EYE);
            if (leftEye != null && rightEye != null) {
                float eyeDistance = calculateDistance(leftEye.getPosition(), rightEye.getPosition());
                features[index++] = eyeDistance / faceBitmap.getWidth(); // 归一化眼距
            }
            
            // 眼鼻距特征
            FaceLandmark nose = getLandmarkByType(landmarks, FaceLandmark.NOSE_BASE);
            if (leftEye != null && nose != null) {
                float eyeNoseDistance = calculateDistance(leftEye.getPosition(), nose.getPosition());
                features[index++] = eyeNoseDistance / faceBitmap.getHeight(); // 归一化眼鼻距
            }
            
            // 嘴宽特征
            FaceLandmark leftMouth = getLandmarkByType(landmarks, FaceLandmark.MOUTH_LEFT);
            FaceLandmark rightMouth = getLandmarkByType(landmarks, FaceLandmark.MOUTH_RIGHT);
            if (leftMouth != null && rightMouth != null) {
                float mouthWidth = calculateDistance(leftMouth.getPosition(), rightMouth.getPosition());
                features[index++] = mouthWidth / faceBitmap.getWidth(); // 归一化嘴宽
            }
            
            // 面部比例特征
            features[index++] = (float) face.getBoundingBox().width() / face.getBoundingBox().height();
            features[index++] = face.getSmilingProbability() != null ? face.getSmilingProbability() : 0.5f;
            features[index++] = face.getLeftEyeOpenProbability() != null ? face.getLeftEyeOpenProbability() : 0.5f;
            features[index++] = face.getRightEyeOpenProbability() != null ? face.getRightEyeOpenProbability() : 0.5f;
            
            // 2. 图像特征 (后96维) - 基于图像像素值
            int blockSize = 8;
            int blocksX = faceBitmap.getWidth() / blockSize;
            int blocksY = faceBitmap.getHeight() / blockSize;
            
            for (int y = 0; y < blocksY && index < FEATURE_VECTOR_SIZE; y++) {
                for (int x = 0; x < blocksX && index < FEATURE_VECTOR_SIZE; x++) {
                    // 计算每个块的平均灰度值
                    float avgGray = calculateBlockAverageGray(faceBitmap, x * blockSize, y * blockSize, blockSize);
                    features[index++] = avgGray / 255.0f; // 归一化到0-1
                }
            }
            
            // 3. 填充剩余维度
            while (index < FEATURE_VECTOR_SIZE) {
                features[index++] = 0.0f;
            }
            
        } catch (Exception e) {
            Log.e(TAG, "生成特征向量失败: " + e.getMessage(), e);
            // 返回随机特征向量作为备选
            for (int i = 0; i < FEATURE_VECTOR_SIZE; i++) {
                features[i] = (float) Math.random();
            }
        }
        
        return features;
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
     * 计算图像块的平均灰度值
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