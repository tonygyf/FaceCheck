package com.example.facecheck.utils;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Rect;
import android.util.Log;

import com.example.facecheck.database.DatabaseHelper;
import com.example.facecheck.data.model.FaceEmbedding;
import com.example.facecheck.data.model.Student;

import java.io.File;
import java.io.FileOutputStream;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.ArrayList;
import java.util.List;
import java.util.Arrays;

/**
 * 人脸识别管理器
 * 现已全面迁移至后端推理，此类仅保留图像裁剪和特征上传的胶水代码
 */
public class FaceRecognitionManager {

    private static final String TAG = "FaceRecognitionManager";
    private static final String MODEL_VERSION = "server-onnx";
    private static final int FEATURE_DIM = 128;
    private static final float SIMILARITY_THRESHOLD = 0.6f;

    private final Context context;
    private final DatabaseHelper databaseHelper;
    private final ImageStorageManager imageStorageManager;

    private final int modelOutputDim = FEATURE_DIM;
    private String currentModelVersion = MODEL_VERSION;

    public FaceRecognitionManager(Context context) {
        this.context = context;
        this.databaseHelper = new DatabaseHelper(context);
        this.imageStorageManager = new ImageStorageManager(context);
    }

    public void setSelectedModel(String name) {
        // 本地推理已移除，特征统一由服务端生成
        Log.i(TAG, "Local inference removed. All embeddings are requested from server.");
    }

    public String getCurrentModelVersion() {
        return this.currentModelVersion;
    }

    private float[] createDummyFeatures() {
        float[] dummy = new float[FEATURE_DIM];
        Arrays.fill(dummy, 0.0f);
        return dummy;
    }

    /**
     * 提取人脸特征向量
     * 基于人脸几何特征、纹理特征和图像特征
     */
    public float[] extractFaceFeatures(Bitmap faceBitmap, Rect faceBox) {
        // 保持返回维度兼容，避免历史调用链因空向量崩溃。
        Log.i(TAG, "extractFaceFeatures called, returning dummy features as local inference is removed.");
        return createDummyFeatures();
    }

    public float compareFeatures(float[] feature1, float[] feature2) {
        if (feature1 == null || feature2 == null || feature1.length != feature2.length) {
            return 0f;
        }
        
        float dotProduct = 0.0f;
        float norm1 = 0.0f;
        float norm2 = 0.0f;
        
        for (int i = 0; i < feature1.length; i++) {
            dotProduct += feature1[i] * feature2[i];
            norm1 += feature1[i] * feature1[i];
            norm2 += feature2[i] * feature2[i];
        }
        
        if (norm1 == 0.0f || norm2 == 0.0f) {
            return 0.0f;
        }
        
        return (float) (dotProduct / (Math.sqrt(norm1) * Math.sqrt(norm2)));
    }

    // 旋转位图的辅助方法
    private Bitmap rotateBitmap(Bitmap src, float degrees) {
        try {
            if (src == null)
                return null;
            android.graphics.Matrix m = new android.graphics.Matrix();
            m.postRotate(degrees);
            return Bitmap.createBitmap(src, 0, 0, src.getWidth(), src.getHeight(), m, true);
        } catch (Throwable t) {
            Log.w(TAG, "rotateBitmap failed: " + t.getMessage());
            return src;
        }
    }

    public float calculateSimilarity(float[] vector1, float[] vector2) {
        if (vector1 == null || vector2 == null || vector1.length != vector2.length) {
            return 0.0f;
        }

        float dotProduct = 0.0f;
        float norm1 = 0.0f;
        float norm2 = 0.0f;

        for (int i = 0; i < vector1.length; i++) {
            float a = vector1[i];
            float b = vector2[i];
            if (Float.isNaN(a) || Float.isNaN(b))
                continue;
            dotProduct += a * b;
            norm1 += a * a;
            norm2 += b * b;
        }

        if (Float.isNaN(dotProduct) || Float.isNaN(norm1) || Float.isNaN(norm2)) {
            return 0.0f;
        }

        float n1 = (float) Math.sqrt(norm1);
        float n2 = (float) Math.sqrt(norm2);
        if (n1 == 0.0f || n2 == 0.0f) {
            return 0.0f;
        }
        float sim = dotProduct / (n1 * n2);
        // clamp 到 [-1,1]
        if (sim > 1.0f)
            sim = 1.0f;
        else if (sim < -1.0f)
            sim = -1.0f;

        return sim;
    }

    private float[] normalizeVector(float[] input) {
        if (input == null || input.length == 0) {
            return new float[0];
        }
        float norm2 = 0f;
        for (float v : input) {
            if (!Float.isNaN(v) && !Float.isInfinite(v)) {
                norm2 += v * v;
            }
        }
        if (norm2 <= 0f) {
            return Arrays.copyOf(input, input.length);
        }
        float norm = (float) Math.sqrt(norm2);
        float[] out = new float[input.length];
        for (int i = 0; i < input.length; i++) {
            float v = input[i];
            out[i] = (Float.isNaN(v) || Float.isInfinite(v)) ? 0f : (v / norm);
        }
        return out;
    }

    public byte[] floatArrayToByteArray(float[] floatArray) {
        ByteBuffer buffer = ByteBuffer.allocate(floatArray.length * 4);
        buffer.order(ByteOrder.LITTLE_ENDIAN);
        for (float f : floatArray) {
            buffer.putFloat(f);
        }
        return buffer.array();
    }

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

    public boolean saveFaceEmbedding(long studentId, float[] features, float quality, String faceImagePath) {
        try {
            if (features == null) return false;
            byte[] vectorBytes = floatArrayToByteArray(features);
            long result = databaseHelper.insertFaceEmbedding(studentId, currentModelVersion, vectorBytes, quality);
            return result != -1;
        } catch (Exception e) {
            Log.e(TAG, "保存人脸特征失败: " + e.getMessage(), e);
            return false;
        }
    }

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

    public RecognitionResult verifyStudentIdentity(Bitmap faceBitmap, Rect faceBox, long studentId) {
        return new RecognitionResult(-1, 0f, "Local inference removed");
    }

    /**
     * 识别单个人脸
     * 返回最相似的学生ID和相似度
     */
    public RecognitionResult recognizeFace(Bitmap faceBitmap, Rect faceBox) {
        try {
            float[] queryFeatures = extractFaceFeatures(faceBitmap, faceBox);
            if (queryFeatures == null) {
                return new RecognitionResult(-1, 0.0f, "特征提取失败");
            }

            List<Student> allStudents = databaseHelper.getAllStudents();
            float bestSimilarity = 0.0f;
            long bestStudentId = -1;

            for (Student student : allStudents) {
                List<FaceEmbedding> embeddings = getStudentFaceEmbeddings(student.getId());

                for (FaceEmbedding embedding : embeddings) {
                    if (!currentModelVersion.equals(embedding.getModelVer()))
                        continue;
                    float[] storedFeatures = byteArrayToFloatArray(embedding.getVector());
                    if (storedFeatures != null && storedFeatures.length == queryFeatures.length) {
                        float similarity = calculateSimilarity(queryFeatures, storedFeatures);

                        if (similarity > bestSimilarity) {
                            bestSimilarity = similarity;
                            bestStudentId = student.getId();
                        }
                    }
                }
            }

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
    public List<RecognitionResult> recognizeMultipleFaces(List<Bitmap> faceBitmaps, List<Rect> faceBoxes) {
        List<RecognitionResult> results = new ArrayList<>();

        for (int i = 0; i < faceBitmaps.size(); i++) {
            RecognitionResult result = recognizeFace(faceBitmaps.get(i), faceBoxes.get(i));
            results.add(result);
        }

        return filterBestResultsPerStudent(results);
    }

    // 删除：使用 MobileFaceNet 嵌入进行单人脸识别（已废弃）

    // 删除：批量使用 MobileFaceNet 嵌入进行识别（已废弃）

    // 删除：批量使用 MobileFaceNet 嵌入进行识别（限制在指定班级，已废弃）

    /**
     * 通用：批量使用已导入的人脸向量进行识别（限制在指定班级），不做增强
     * 说明：
     * - 输入向量维度应与库中统一维度一致（当前为 256）
     * - 仅与指定班级的学生嵌入比对
     * - 使用通用阈值 SIMILARITY_THRESHOLD
     */
    public List<RecognitionResult> recognizeImportedVectorsWithinClass(List<float[]> importedVectors,
            long classroomId) {
        List<RecognitionResult> results = new ArrayList<>();
        if (importedVectors == null || importedVectors.isEmpty())
            return results;

        // 取本班全部学生ID
        final java.util.HashSet<Long> allowedIds = new java.util.HashSet<>();
        android.database.Cursor sc = null;
        try {
            sc = databaseHelper.getStudentsByClass(classroomId);
            if (sc != null && sc.moveToFirst()) {
                do {
                    long sid = sc.getLong(sc.getColumnIndexOrThrow("id"));
                    allowedIds.add(sid);
                } while (sc.moveToNext());
            }
        } catch (Throwable t) {
            android.util.Log.e(TAG, "获取班级学生失败: " + t.getMessage(), t);
        } finally {
            if (sc != null)
                sc.close();
        }

        // 预取库中统一模型版本的嵌入，并按本班过滤
        android.database.Cursor cursor = null;
        final List<Long> studentIds = new ArrayList<>();
        final List<float[]> storedEmbeddings = new ArrayList<>();
        try {
            cursor = databaseHelper.getAllFaceEmbeddingsByModel(currentModelVersion);
            if (cursor != null && cursor.moveToFirst()) {
                do {
                    long studentId = cursor.getLong(cursor.getColumnIndexOrThrow("studentId"));
                    if (!allowedIds.contains(studentId))
                        continue; // 只比对本班学生
                    byte[] vecBytes = cursor.getBlob(cursor.getColumnIndexOrThrow("vector"));
                    float[] vec = byteArrayToFloatArray(vecBytes);
                    if (vec != null && vec.length > 0) {
                        studentIds.add(studentId);
                        storedEmbeddings.add(vec);
                    }
                } while (cursor.moveToNext());
            }
        } catch (Throwable t) {
            android.util.Log.e(TAG, "批量预取嵌入失败: " + t.getMessage(), t);
        } finally {
            if (cursor != null)
                cursor.close();
        }

        // 逐个导入向量进行比对
        for (float[] queryVector : importedVectors) {
            if (queryVector == null || queryVector.length == 0) {
                results.add(new RecognitionResult(-1, 0f, "向量为空"));
                continue;
            }

            // 可选：归一化，避免不同来源的尺度差异（若外部已归一化，可注释）
            float[] normalized = normalizeVector(queryVector);

            float bestSim = 0f;
            long bestStudentId = -1;
            for (int i = 0; i < storedEmbeddings.size(); i++) {
                float[] ref = storedEmbeddings.get(i);
                if (ref != null && ref.length == normalized.length) {
                    float sim = calculateSimilarity(normalized, ref);
                    if (sim > bestSim) {
                        bestSim = sim;
                        bestStudentId = studentIds.get(i);
                    }
                }
            }

            android.util.Log.d(TAG, "Manual recognition: best student ID=" + bestStudentId + ", best sim=" + bestSim
                    + ", threshold=0.6");

            if (bestSim >= 0.6f) {
                results.add(new RecognitionResult(bestStudentId, bestSim, "识别成功"));
            } else {
                results.add(new RecognitionResult(-1, bestSim, "未匹配到合适学生"));
            }
        }
        return filterBestResultsPerStudent(results);
    }

    /**
     * 过滤识别结果，确保每个学生 ID 在结果列表中只出现一次（保留相似度最高的那个）。
     * 对于未识别成功（studentId = -1）的结果，全部保留。
     */
    private List<RecognitionResult> filterBestResultsPerStudent(List<RecognitionResult> results) {
        if (results == null || results.isEmpty())
            return results;

        java.util.Map<Long, RecognitionResult> bestResultsMap = new java.util.HashMap<>();
        List<RecognitionResult> finalResults = new ArrayList<>();

        for (RecognitionResult result : results) {
            if (result.getStudentId() == -1) {
                // 未识别成功的人脸，保留占位
                finalResults.add(result);
                continue;
            }

            long sid = result.getStudentId();
            if (!bestResultsMap.containsKey(sid) || result.getSimilarity() > bestResultsMap.get(sid).getSimilarity()) {
                bestResultsMap.put(sid, result);
            }
        }

        // 将每个学生的最佳结果添加到最终列表
        finalResults.addAll(bestResultsMap.values());
        return finalResults;
    }

    /**
     * 使用检测框提取的图像进行批量识别（限制在指定班级）
     */
    public List<RecognitionResult> recognizeMultipleFacesWithinClass(List<Bitmap> faceBitmaps,
            List<Rect> faces, long classroomId) {
        List<RecognitionResult> results = new ArrayList<>();
        if (faceBitmaps == null || faces == null)
            return results;
        int count = Math.min(faceBitmaps.size(), faces.size());

        // 取本班全部学生ID
        final java.util.HashSet<Long> allowedIds = new java.util.HashSet<>();
        android.database.Cursor sc = null;
        try {
            sc = databaseHelper.getStudentsByClass(classroomId);
            if (sc != null && sc.moveToFirst()) {
                do {
                    long sid = sc.getLong(sc.getColumnIndexOrThrow("id"));
                    allowedIds.add(sid);
                } while (sc.moveToNext());
            }
        } catch (Throwable t) {
            android.util.Log.e(TAG, "获取班级学生失败: " + t.getMessage(), t);
        } finally {
            if (sc != null)
                sc.close();
        }

        // 逐人脸比对，仅与本班学生嵌入比对
        for (int i = 0; i < count; i++) {
            Bitmap b = faceBitmaps.get(i);
            Rect f = faces.get(i);

            float[] queryFeatures = extractFaceFeatures(b, f);
            if (queryFeatures == null) {
                results.add(new RecognitionResult(-1, 0f, "特征提取失败"));
                continue;
            }

            float bestSimilarity = 0.0f;
            long bestStudentId = -1;

            // 遍历本班学生的所有嵌入
            android.database.Cursor cur = null;
            try {
                cur = databaseHelper.getAllFaceEmbeddingsByModel(currentModelVersion);
                if (cur != null && cur.moveToFirst()) {
                    do {
                        long sid = cur.getLong(cur.getColumnIndexOrThrow("studentId"));
                        if (!allowedIds.contains(sid))
                            continue;
                        byte[] vecBytes = cur.getBlob(cur.getColumnIndexOrThrow("vector"));
                        float[] stored = byteArrayToFloatArray(vecBytes);
                        if (stored != null && stored.length == queryFeatures.length) {
                            float sim = calculateSimilarity(queryFeatures, stored);
                            if (sim > bestSimilarity) {
                                bestSimilarity = sim;
                                bestStudentId = sid;
                            }
                        }
                    } while (cur.moveToNext());
                }
            } catch (Throwable t) {
                android.util.Log.e(TAG, "比对失败: " + t.getMessage(), t);
            } finally {
                if (cur != null)
                    cur.close();
            }

            if (bestSimilarity >= SIMILARITY_THRESHOLD) {
                results.add(new RecognitionResult(bestStudentId, bestSimilarity, "识别成功"));
            } else {
                results.add(new RecognitionResult(-1, bestSimilarity, "未找到匹配的学生"));
            }
        }
        return filterBestResultsPerStudent(results);
    }

    /**
     * 一键校验：遍历库内当前模型版本的全部人脸向量，检查维度与范数，并导出日志到文件
     * 返回导出日志文件的绝对路径；若失败返回 null
     */
    public String validateAllEmbeddingsAndExport(android.content.Context context) {
        android.database.Cursor cursor = null;
        StringBuilder sb = new StringBuilder();
        long total = 0, dimMismatch = 0, zeroNorm = 0, nearUnit = 0, nanOrInf = 0;
        // 以当前加载的模型输出维度为准
        final int expectedDim = modelOutputDim;

        sb.append("FaceEmbedding Validation Report\n")
                .append("modelVer=").append(currentModelVersion).append('\n')
                .append("expectedDim=").append(expectedDim).append('\n')
                .append("time=").append(System.currentTimeMillis()).append("\n\n");

        try {
            cursor = databaseHelper.getAllFaceEmbeddingsByModel(currentModelVersion);
            if (cursor != null && cursor.moveToFirst()) {
                do {
                    long id = cursor.getLong(cursor.getColumnIndexOrThrow("id"));
                    long studentId = cursor.getLong(cursor.getColumnIndexOrThrow("studentId"));
                    byte[] vecBytes = cursor.getBlob(cursor.getColumnIndexOrThrow("vector"));
                    float quality = 0f;
                    int qIdx = cursor.getColumnIndex("quality");
                    if (qIdx >= 0)
                        quality = cursor.getFloat(qIdx);

                    int dim = (vecBytes == null ? 0 : vecBytes.length / 4);

                    // 直接解析 BLOB 为 float[]（不做归一化），用于真实库数据的范数校验
                    float[] vec = new float[dim];
                    boolean hasNanOrInf = false;
                    if (vecBytes != null && vecBytes.length % 4 == 0) {
                        java.nio.ByteBuffer buffer = java.nio.ByteBuffer.wrap(vecBytes)
                                .order(java.nio.ByteOrder.LITTLE_ENDIAN);
                        for (int i = 0; i < dim; i++) {
                            float v = buffer.getFloat();
                            vec[i] = v;
                            if (java.lang.Float.isNaN(v) || java.lang.Float.isInfinite(v)) {
                                hasNanOrInf = true;
                            }
                        }
                    }

                    float norm2 = 0f;
                    int nonZero = 0;
                    int zeroRun = 0, maxZeroRun = 0;
                    for (int i = 0; i < vec.length; i++) {
                        float v = vec[i];
                        norm2 += v * v;
                        if (v != 0f) {
                            nonZero++;
                            zeroRun = 0;
                        } else {
                            zeroRun++;
                            if (zeroRun > maxZeroRun)
                                maxZeroRun = zeroRun;
                        }
                    }
                    float norm = (float) Math.sqrt(norm2);

                    total++;
                    if (dim != expectedDim)
                        dimMismatch++;
                    if (norm == 0f)
                        zeroNorm++;
                    float unitDiff = Math.abs(norm - 1f);
                    if (unitDiff <= 1e-3f)
                        nearUnit++;
                    if (hasNanOrInf)
                        nanOrInf++;

                    sb.append("id=").append(id)
                            .append(", studentId=").append(studentId)
                            .append(", dim=").append(dim)
                            .append(", norm=").append(norm)
                            .append(", nonZero=").append(nonZero)
                            .append(", maxZeroRun=").append(maxZeroRun)
                            .append(", quality=").append(quality);

                    if (hasNanOrInf)
                        sb.append(", flags=NAN_OR_INF");
                    if (dim != expectedDim)
                        sb.append(", flags=DIM_MISMATCH");
                    if (norm == 0f)
                        sb.append(", flags=ZERO_NORM");
                    else if (unitDiff > 1e-3f)
                        sb.append(", flags=NOT_UNIT");
                    sb.append('\n');
                } while (cursor.moveToNext());
            } else {
                sb.append("No embeddings found for modelVer=").append(currentModelVersion).append('\n');
            }
        } catch (Throwable t) {
            android.util.Log.e(TAG, "validateAllEmbeddingsAndExport failed: " + t.getMessage(), t);
            return null;
        } finally {
            if (cursor != null)
                cursor.close();
        }

        sb.append("\nSummary: total=").append(total)
                .append(", dimMismatch=").append(dimMismatch)
                .append(", zeroNorm=").append(zeroNorm)
                .append(", nearUnit=").append(nearUnit)
                .append(", nanOrInf=").append(nanOrInf)
                .append('\n');

        try {
            java.io.File outDir = context.getExternalFilesDir("reports");
            if (outDir != null && !outDir.exists())
                outDir.mkdirs();
            java.io.File outFile = new java.io.File(outDir,
                    "embedding-validation-" + System.currentTimeMillis() + ".txt");
            java.io.FileOutputStream fos = new java.io.FileOutputStream(outFile);
            fos.write(sb.toString().getBytes("UTF-8"));
            fos.flush();
            fos.close();
            android.util.Log.i(TAG, "Validation report exported: " + outFile.getAbsolutePath());
            return outFile.getAbsolutePath();
        } catch (Exception e) {
            android.util.Log.e(TAG, "Failed to write validation report: " + e.getMessage(), e);
            return null;
        }
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
            // 识别方法在低于各自阈值时都会返回 studentId = -1；
            // 这里只需判断是否存在有效学生ID即可。
            return studentId != -1;
        }
    }
}
