package com.example.facecheck.utils;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.content.res.AssetFileDescriptor;
import android.graphics.PointF;
import android.graphics.Rect;
import android.util.Log;

import com.example.facecheck.database.DatabaseHelper;
import com.example.facecheck.data.model.FaceEmbedding;
import com.example.facecheck.data.model.Student;
import com.google.mlkit.vision.face.Face;
import com.google.mlkit.vision.face.FaceLandmark;

import java.io.File;
import java.io.FileOutputStream;
import java.io.FileInputStream;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Arrays;
import org.tensorflow.lite.Interpreter;
import org.tensorflow.lite.Tensor;
import org.tensorflow.lite.DataType;

/**
 * 人脸识别管理器
 * 负责人脸特征提取、存储和比对
 */
public class FaceRecognitionManager {

    private static final String TAG = "FaceRecognitionManager";
    private static final String MODEL_VERSION = "mfn-new-f32";
    private static final float SIMILARITY_THRESHOLD = 0.75f; // 传统增强特征阈值
    private static final int FEATURE_VECTOR_SIZE = 256; // 特征向量维度 - 增加特征维度以提高识别精度
    private static final boolean DEBUG_SIMILARITY = true; // 相似度调试开关

    private final Context context;
    private final DatabaseHelper databaseHelper;
    private final ImageStorageManager imageStorageManager;
    // MobileFaceNet 模型/维度
    private Interpreter tflite;
    private int modelInputWidth = 112;
    private int modelInputHeight = 112;
    private int modelInputChannels = 3;
    private int modelOutputDim = 128; // 以模型输出为准，常见为128

    // 动态模型选择
    private String currentModelVersion = MODEL_VERSION; // 默认 MobileFaceNet (new/f32)
    private String selectedModelName = "MobileFaceNet"; // 仅用于特征提取模型选择

    public FaceRecognitionManager(Context context) {
        this.context = context;
        this.databaseHelper = new DatabaseHelper(context);
        this.imageStorageManager = new ImageStorageManager(context);
    }

    /**
     * 设置当前使用的特征提取模型
     * 可选："Google ML Kit (推荐)", "MobileFaceNet", "Google FaceNet"
     */
    public void setSelectedModel(String name) {
        if (name == null)
            return;
        String trimmed = name.trim();
        // 废弃除 ML Kit 检测外的旧特征提取模型配置：
        // 若用户选择 ML Kit 作为“特征提取模型”，直接回退到 MobileFaceNet（new/f32）。
        if (trimmed.contains("ML Kit")) {
            Log.w(TAG, "ML Kit 特征提取已废弃，自动回退到 MobileFaceNet(new/f32)");
            this.selectedModelName = "MobileFaceNet";
            this.currentModelVersion = "mfn-new-f32";
        } else if (trimmed.equalsIgnoreCase("MobileFaceNet")) {
            this.selectedModelName = "MobileFaceNet";
            this.currentModelVersion = "mfn-new-f32";
        } else if (trimmed.contains("FaceNet")) {
            this.selectedModelName = "Google FaceNet";
            this.currentModelVersion = "facenet-new-f32"; // 典型 128 维
            this.modelOutputDim = 128; // 按输出张量动态覆盖
        } else {
            // 回退到 MobileFaceNet(new/f32)
            this.selectedModelName = "MobileFaceNet";
            this.currentModelVersion = "mfn-new-f32";
            Log.w(TAG, "Unknown model name: " + name + ", fallback to MobileFaceNet(new/f32)");
        }
    }

    /**
     * 获取当前模型版本（用于区分库中不同模型的嵌入记录）
     */
    public String getCurrentModelVersion() {
        return this.currentModelVersion;
    }

    /**
     * 加载当前选择的特征提取模型（new 目录下的 tflite），懒加载一次。
     */
    private void ensureInterpreterLoaded() {
        if (tflite != null)
            return;
        try {
            String assetPath;
            if ("Google FaceNet".equals(selectedModelName)) {
                assetPath = "models/new/facenet_float32.tflite";
            } else {
                // 默认 MobileFaceNet(new/f32)
                assetPath = "models/new/mobilefacenet_float32.tflite";
            }
            MappedByteBuffer buffer = loadModelFile(assetPath);
            tflite = new Interpreter(buffer);

            // 动态解析输入（考虑 FaceNet 多输入场景，选择 4D 输入作为图像入口）
            int inCount = tflite.getInputTensorCount();
            int imgIndex = -1;
            for (int i = 0; i < inCount; i++) {
                Tensor t = tflite.getInputTensor(i);
                int[] shape = t.shape();
                if (shape != null && shape.length >= 4) {
                    imgIndex = i;
                    this.modelInputHeight = shape[1];
                    this.modelInputWidth = shape[2];
                    this.modelInputChannels = shape[3];
                    break;
                }
            }
            if (imgIndex == -1) {
                // 回退：根据模型类型给默认尺寸
                if ("Google FaceNet".equals(selectedModelName)) {
                    this.modelInputWidth = 160;
                    this.modelInputHeight = 160;
                    this.modelInputChannels = 3;
                } else {
                    this.modelInputWidth = 112;
                    this.modelInputHeight = 112;
                    this.modelInputChannels = 3;
                }
            }

            // 动态解析输出维度（选择首个二维 FLOAT32 输出作为嵌入向量）
            int outCount = tflite.getOutputTensorCount();
            int outDim = -1;
            for (int i = 0; i < outCount; i++) {
                Tensor o = tflite.getOutputTensor(i);
                int[] os = o.shape();
                if (o.dataType() == DataType.FLOAT32 && os != null && os.length >= 2) {
                    outDim = os[os.length - 1];
                    break;
                }
            }
            this.modelOutputDim = (outDim > 0) ? outDim : 128;

            Log.i(TAG, String.format(
                    "Model loaded: %s, input=%dx%dx%d, outputDim=%d, inputs=%d, outputs=%d",
                    selectedModelName, modelInputWidth, modelInputHeight, modelInputChannels,
                    modelOutputDim, inCount, outCount));
        } catch (Exception e) {
            Log.e(TAG, "加载模型失败: " + e.getMessage(), e);
            tflite = null;
        }
    }

    /**
     * 从 assets 加载并映射 .tflite 模型
     */
    private MappedByteBuffer loadModelFile(String assetPath) throws java.io.IOException {
        AssetFileDescriptor afd = context.getAssets().openFd(assetPath);
        FileInputStream fis = new FileInputStream(afd.getFileDescriptor());
        FileChannel channel = fis.getChannel();
        long startOffset = afd.getStartOffset();
        long declaredLength = afd.getDeclaredLength();
        return channel.map(FileChannel.MapMode.READ_ONLY, startOffset, declaredLength);
    }

    /**
     * 运行 MobileFaceNet 推理，返回未归一化的特征向量
     */
    private float[] runMobileFaceNet(Bitmap input) {
        if (tflite == null) {
            Log.w(TAG, "TFLite interpreter is null, cannot run inference");
            return null;
        }
        // 根据模型期望尺寸做缩放
        int w = input.getWidth();
        int h = input.getHeight();
        if (w != modelInputWidth || h != modelInputHeight) {
            input = Bitmap.createScaledBitmap(input, modelInputWidth, modelInputHeight, true);
            w = modelInputWidth;
            h = modelInputHeight;
        }

        // 打印一次输入张量规格
        Tensor inTensor = tflite.getInputTensor(0);
        int[] inShape = inTensor.shape();
        DataType inType = inTensor.dataType();
        Log.i(TAG, "MFN input shape=" + java.util.Arrays.toString(inShape) + ", dtype=" + inType);

        // 根据数据类型构造输入 Buffer
        boolean useFloat32 = (inType == DataType.FLOAT32);
        int[] pixels = new int[w * h];
        input.getPixels(pixels, 0, w, 0, 0, w, h);

        ByteBuffer inBuffer;
        if (useFloat32) {
            inBuffer = ByteBuffer.allocateDirect(w * h * modelInputChannels * 4);
            inBuffer.order(ByteOrder.nativeOrder());
            for (int y = 0; y < h; y++) {
                for (int x = 0; x < w; x++) {
                    int c = pixels[y * w + x];
                    float r = ((c >> 16) & 0xFF);
                    float g = ((c >> 8) & 0xFF);
                    float b = (c & 0xFF);
                    // 常见归一化：[-1,1]
                    inBuffer.putFloat((r - 127.5f) / 128f);
                    inBuffer.putFloat((g - 127.5f) / 128f);
                    inBuffer.putFloat((b - 127.5f) / 128f);
                }
            }
        } else if (inType == DataType.UINT8) {
            inBuffer = ByteBuffer.allocateDirect(w * h * modelInputChannels);
            inBuffer.order(ByteOrder.nativeOrder());
            for (int y = 0; y < h; y++) {
                for (int x = 0; x < w; x++) {
                    int c = pixels[y * w + x];
                    int r = (c >> 16) & 0xFF;
                    int g = (c >> 8) & 0xFF;
                    int b = c & 0xFF;
                    inBuffer.put((byte) r);
                    inBuffer.put((byte) g);
                    inBuffer.put((byte) b);
                }
            }
        } else {
            Log.e(TAG, "Unsupported input data type: " + inType);
            return null;
        }

        float[][] out = new float[1][modelOutputDim];
        try {
            tflite.run(inBuffer, out);
        } catch (Throwable t) {
            Log.e(TAG, "MobileFaceNet inference failed: " + t.getMessage(), t);
            return null;
        }
        return out[0];
    }

    /**
     * 运行 FaceNet 推理（固定图像标准化 + 额外输入 phase_train=false, batch_size=1）
     */
    private float[] runFaceNet(Bitmap input) {
        if (tflite == null) {
            Log.w(TAG, "TFLite interpreter is null, cannot run FaceNet inference");
            return null;
        }
        // 缩放到模型输入尺寸
        int w = input.getWidth();
        int h = input.getHeight();
        if (w != modelInputWidth || h != modelInputHeight) {
            input = Bitmap.createScaledBitmap(input, modelInputWidth, modelInputHeight, true);
            w = modelInputWidth;
            h = modelInputHeight;
        }

        // 固定图像标准化（fixed image standardization）：(x - 127.5) / 128
        int[] pixels = new int[w * h];
        input.getPixels(pixels, 0, w, 0, 0, w, h);
        ByteBuffer inBuffer = ByteBuffer.allocateDirect(w * h * modelInputChannels * 4);
        inBuffer.order(ByteOrder.nativeOrder());
        for (int y = 0; y < h; y++) {
            for (int x = 0; x < w; x++) {
                int c = pixels[y * w + x];
                float r = ((c >> 16) & 0xFF);
                float g = ((c >> 8) & 0xFF);
                float b = (c & 0xFF);
                inBuffer.putFloat((r - 127.5f) / 128f);
                inBuffer.putFloat((g - 127.5f) / 128f);
                inBuffer.putFloat((b - 127.5f) / 128f);
            }
        }

        // 组合多输入
        int inCount = tflite.getInputTensorCount();
        Object[] inputs = new Object[inCount];
        int imageIndex = -1;
        for (int i = 0; i < inCount; i++) {
            Tensor t = tflite.getInputTensor(i);
            DataType dt = t.dataType();
            int[] shape = t.shape();
            if (shape != null && shape.length >= 4 && dt == DataType.FLOAT32 && imageIndex == -1) {
                imageIndex = i;
                inputs[i] = inBuffer;
            } else if (dt == DataType.BOOL) {
                inputs[i] = new boolean[] { false };
            } else if (dt == DataType.INT32) {
                inputs[i] = new int[] { 1 };
            } else if (dt == DataType.FLOAT32 && (shape == null || shape.length <= 1)) {
                inputs[i] = new float[] { 1.0f };
            } else {
                // 不常用输入，给一个合理的默认值
                inputs[i] = new float[] { 0.0f };
            }
        }
        if (imageIndex == -1 && inCount > 0) {
            inputs[0] = inBuffer; // 回退：将图像作为第一个输入
        }

        // 选取首个二维 FLOAT32 输出作为嵌入向量
        int outCount = tflite.getOutputTensorCount();
        java.util.Map<Integer, Object> outputs = new java.util.HashMap<>();
        int mainOutIdx = -1;
        float[][] mainOut = null;
        for (int i = 0; i < outCount; i++) {
            Tensor o = tflite.getOutputTensor(i);
            int[] os = o.shape();
            if (o.dataType() == DataType.FLOAT32 && os != null && os.length >= 2) {
                outputs.put(i, new float[os[0]][os[os.length - 1]]);
                if (mainOutIdx == -1) {
                    mainOutIdx = i;
                    mainOut = (float[][]) outputs.get(i);
                }
            }
        }
        if (mainOut == null) {
            // 回退：假设第一个输出是嵌入
            Tensor o0 = tflite.getOutputTensor(0);
            int[] os0 = o0.shape();
            outputs.put(0, new float[os0[0]][os0[os0.length - 1]]);
            mainOutIdx = 0;
            mainOut = (float[][]) outputs.get(0);
        }

        try {
            tflite.runForMultipleInputsOutputs(inputs, outputs);
        } catch (Throwable t) {
            Log.e(TAG, "FaceNet inference failed: " + t.getMessage(), t);
            return null;
        }
        return (mainOut != null && mainOut.length > 0) ? mainOut[0] : null;
    }

    /**
     * 提取人脸特征向量
     * 基于人脸几何特征、纹理特征和图像特征
     */
    public float[] extractFaceFeatures(Bitmap faceBitmap, Face face) {
        if (faceBitmap == null) {
            Log.e(TAG, "extractFaceFeatures: faceBitmap == null");
            return null;
        }
        try {
            Log.d(TAG, "extractFaceFeatures: bitmap w=" + faceBitmap.getWidth() + ", h=" + faceBitmap.getHeight()
                    + ", euler=(" + face.getHeadEulerAngleX() + "," + face.getHeadEulerAngleY() + ","
                    + face.getHeadEulerAngleZ() + ")");
            // 针对不同模型的输入尺寸：
            // - ML Kit 路径使用任意统一尺寸生成基础特征（此处沿用当前设置）
            // - 其他深度模型根据 interpreter 的输入动态解析
            final int MODEL_W = modelInputWidth;
            final int MODEL_H = modelInputHeight;

            Rect bbox = face.getBoundingBox();
            if (bbox == null) {
                Log.w(TAG, "no bounding box");
                dumpBitmapForDebug(faceBitmap, "no_bbox");
                return null;
            }

            int left = Math.max(0, bbox.left);
            int top = Math.max(0, bbox.top);
            int right = Math.min(faceBitmap.getWidth(), bbox.right);
            int bottom = Math.min(faceBitmap.getHeight(), bbox.bottom);
            int w = right - left, h = bottom - top;
            if (w <= 0 || h <= 0) {
                Log.w(TAG, "invalid crop box left=" + left + " top=" + top + " w=" + w + " h=" + h);
                dumpBitmapForDebug(faceBitmap, "invalid_crop");
                return null;
            }

            Bitmap crop = Bitmap.createBitmap(faceBitmap, left, top, w, h);
            // 对齐：根据人脸滚转角（Z 轴）将裁剪区域旋转到水平，提高跨照片一致性
            float roll = face.getHeadEulerAngleZ();
            Bitmap alignedCrop = (Math.abs(roll) > 1.0f) ? rotateBitmap(crop, -roll) : crop;
            if (alignedCrop != crop) {
                Log.d(TAG, String.format("apply roll alignment: z=%.2f, crop=%dx%d", roll, w, h));
            }
            Bitmap input = Bitmap.createScaledBitmap(alignedCrop, MODEL_W, MODEL_H, true);

            // 打印常见关键点状态
            int present = 0;
            int[] types = { FaceLandmark.LEFT_EYE, FaceLandmark.RIGHT_EYE, FaceLandmark.NOSE_BASE,
                    FaceLandmark.MOUTH_LEFT, FaceLandmark.MOUTH_RIGHT };
            for (int t : types) {
                FaceLandmark lm = face.getLandmark(t);
                if (lm != null) {
                    present++;
                    Log.d(TAG, "landmark " + t + " at " + lm.getPosition().x + "," + lm.getPosition().y);
                } else {
                    Log.w(TAG, "landmark missing " + t);
                }
            }
            if (present < 2) {
                Log.w(TAG, "too few landmarks: " + present);
                dumpBitmapForDebug(input, "few_landmarks");
                return null;
            }

            // 废弃 ML Kit 嵌入，统一走深度模型（MobileFaceNet / FaceNet）
            ensureInterpreterLoaded();
            float[] features;
            if ("Google FaceNet".equals(selectedModelName)) {
                features = runFaceNet(input);
            } else {
                features = runMobileFaceNet(input);
            }
            if (features == null) {
                Log.e(TAG, "inference returned null");
                dumpBitmapForDebug(input, "infer_null");
                return null;
            }
            float[] normFeat = normalizeVector(features);
            if (normFeat != null && normFeat.length > 0) {
                int sampleCount = Math.min(5, normFeat.length);
                Log.d(TAG, "EMB_DEBUG dim=" + normFeat.length + ", sample="
                        + java.util.Arrays.toString(java.util.Arrays.copyOf(normFeat, sampleCount)));
            }
            return normFeat;

        } catch (Exception e) {
            Log.e(TAG, "提取人脸特征失败: " + e.getMessage(), e);
            dumpBitmapForDebug(faceBitmap, "exception");
            return null;
        }
    }

    /**
     * 提取人脸特征向量（YuNet/其它检测输出：使用 Rect 裁剪）
     */
    public float[] extractFaceFeatures(Bitmap sourceBitmap, Rect bbox) {
        if (sourceBitmap == null) {
            Log.e(TAG, "extractFaceFeatures(Rect): sourceBitmap == null");
            return null;
        }
        if (bbox == null) {
            Log.e(TAG, "extractFaceFeatures(Rect): bbox == null");
            dumpBitmapForDebug(sourceBitmap, "rect_null");
            return null;
        }
        try {
            // 统一裁剪并缩放到当前模型的输入尺寸
            int left = Math.max(0, bbox.left);
            int top = Math.max(0, bbox.top);
            int right = Math.min(sourceBitmap.getWidth(), bbox.right);
            int bottom = Math.min(sourceBitmap.getHeight(), bbox.bottom);
            int w = right - left;
            int h = bottom - top;
            if (w <= 0 || h <= 0) {
                Log.w(TAG, "extractFaceFeatures(Rect): invalid crop w=" + w + ", h=" + h);
                dumpBitmapForDebug(sourceBitmap, "rect_invalid");
                return null;
            }

            ensureInterpreterLoaded();
            Bitmap crop = Bitmap.createBitmap(sourceBitmap, left, top, w, h);
            Bitmap input = Bitmap.createScaledBitmap(crop, modelInputWidth, modelInputHeight, true);

            // 废弃 ML Kit 嵌入，统一走深度模型（MobileFaceNet / FaceNet）
            float[] features = "Google FaceNet".equals(selectedModelName)
                    ? runFaceNet(input)
                    : runMobileFaceNet(input);
            if (features == null) {
                Log.e(TAG, "Inference returned null (Rect)");
                dumpBitmapForDebug(input, "infer_null_rect");
                return null;
            }
            float[] normFeat = normalizeVector(features);
            if (normFeat != null && normFeat.length > 0) {
                int sampleCount = Math.min(5, normFeat.length);
                Log.d(TAG, "EMB_DEBUG(rect) dim=" + normFeat.length + ", sample="
                        + java.util.Arrays.toString(java.util.Arrays.copyOf(normFeat, sampleCount)));
            }
            return normFeat;

        } catch (Throwable t) {
            Log.e(TAG, "提取人脸特征失败(Rect): " + t.getMessage(), t);
            dumpBitmapForDebug(sourceBitmap, "exception_rect");
            return null;
        }
    }

    /**
     * 识别单个人脸（YuNet Rect 路径）
     */
    public RecognitionResult recognizeFace(Bitmap sourceBitmap, Rect bbox) {
        try {
            float[] queryFeatures = extractFaceFeatures(sourceBitmap, bbox);
            if (queryFeatures == null) {
                return new RecognitionResult(-1, 0.0f, "特征提取失败(Rect)");
            }

            List<Student> allStudents = databaseHelper.getAllStudents();
            float bestSimilarity = 0.0f;
            long bestStudentId = -1;

            for (Student student : allStudents) {
                List<FaceEmbedding> embeddings = getStudentFaceEmbeddings(student.getId());
                for (FaceEmbedding embedding : embeddings) {
                    if (!currentModelVersion.equals(embedding.getModelVer()))
                        continue;
                    float[] stored = byteArrayToFloatArray(embedding.getVector());
                    if (stored != null && stored.length == queryFeatures.length) {
                        float sim = calculateSimilarity(queryFeatures, stored);
                        if (sim > bestSimilarity) {
                            bestSimilarity = sim;
                            bestStudentId = student.getId();
                        }
                    }
                }
            }

            if (bestStudentId != -1 && bestSimilarity >= 0.6f) {
                return new RecognitionResult(bestStudentId, bestSimilarity, "识别成功(Rect)");
            } else {
                return new RecognitionResult(-1, bestSimilarity, "未识别到匹配(Rect)");
            }
        } catch (Throwable t) {
            Log.e(TAG, "识别失败(Rect): " + t.getMessage(), t);
            return new RecognitionResult(-1, 0.0f, "识别异常(Rect)");
        }
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

    // 辅助：把调试图片写到 app-specific external dir
    private void dumpBitmapForDebug(Bitmap b, String tag) {
        try {
            if (b == null)
                return;
            File dir = context.getExternalFilesDir("face_debug");
            if (dir == null)
                return;
            if (!dir.exists())
                dir.mkdirs();
            File f = new File(dir, "dump_" + tag + "_" + System.currentTimeMillis() + ".png");
            FileOutputStream fos = new FileOutputStream(f);
            b.compress(Bitmap.CompressFormat.PNG, 100, fos);
            fos.flush();
            fos.close();
            Log.d(TAG, "dumped debug bitmap to " + f.getAbsolutePath());
        } catch (Exception ex) {
            Log.e(TAG, "dumpBitmapForDebug failed", ex);
        }
    }

    /**
     * 生成基础特征向量（无增强）：16x16 网格块灰度均值，共 256 维
     */
    private float[] generateBaseFeatures(Bitmap faceBitmap) {
        if (faceBitmap == null)
            return null;
        try {
            int grid = 16;
            int bw = Math.max(1, faceBitmap.getWidth() / grid);
            int bh = Math.max(1, faceBitmap.getHeight() / grid);
            float[] features = new float[FEATURE_VECTOR_SIZE];

            int idx = 0;
            for (int gy = 0; gy < grid; gy++) {
                for (int gx = 0; gx < grid; gx++) {
                    int startX = gx * bw;
                    int startY = gy * bh;
                    int endX = Math.min(startX + bw, faceBitmap.getWidth());
                    int endY = Math.min(startY + bh, faceBitmap.getHeight());

                    long sum = 0;
                    int count = 0;
                    for (int y = startY; y < endY; y++) {
                        for (int x = startX; x < endX; x++) {
                            int pixel = faceBitmap.getPixel(x, y);
                            int gray = (int) (0.299f * ((pixel >> 16) & 0xFF)
                                    + 0.587f * ((pixel >> 8) & 0xFF)
                                    + 0.114f * (pixel & 0xFF));
                            sum += gray;
                            count++;
                        }
                    }
                    float avg = count > 0 ? (float) sum / count : 0f;
                    features[idx++] = avg / 255.0f; // 归一到 [0,1]，后续再做向量归一化
                    if (idx >= FEATURE_VECTOR_SIZE)
                        break;
                }
                if (idx >= FEATURE_VECTOR_SIZE)
                    break;
            }

            // 若不足256维，补零（极少发生）
            while (idx < FEATURE_VECTOR_SIZE) {
                features[idx++] = 0f;
            }
            return features;
        } catch (Exception e) {
            Log.e(TAG, "generateBaseFeatures failed: " + e.getMessage(), e);
            return null;
        }
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
        // 已移除增强纹理特征逻辑，直接返回
        return startIndex;
    }

    /**
     * 生成增强图像块特征 (多尺度分析)
     */
    private int generateEnhancedImageBlockFeatures(Bitmap faceBitmap, float[] features, int startIndex) {
        // 已移除增强图像块特征逻辑，直接返回
        return startIndex;
    }

    /**
     * 生成深度学习特征 (模拟CNN特征提取)
     */
    private int generateDeepLearningFeatures(Bitmap faceBitmap, float[] features, int startIndex) {
        // 已移除深度学习增强特征逻辑，直接返回
        return startIndex;
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
        if (kernelSize % 2 == 0)
            kernelSize++;

        for (int y = kernelSize / 2; y < height - kernelSize / 2; y++) {
            for (int x = kernelSize / 2; x < width - kernelSize / 2; x++) {
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
    private float applyGaborFilter(Bitmap bitmap, int centerX, int centerY, double theta, double sigma,
            int kernelSize) {
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
                    double gaussian = Math.exp(-(x_theta * x_theta + y_theta * y_theta) / (2 * sigma * sigma));
                    double sinusoid = Math.cos(2 * Math.PI * x_theta / 4.0); // 波长为4

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
                    int gray = (int) (0.299 * Color.red(pixel) + 0.587 * Color.green(pixel)
                            + 0.114 * Color.blue(pixel));
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
            features[0] = (float) mean; // 均值

            double variance = (sum2 - sum * sum / n) / n;
            features[1] = (float) Math.sqrt(variance); // 标准差

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

            features[4] = (float) skewness; // 偏度
            features[5] = (float) kurtosis; // 峰度

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

                    float magnitude = (float) Math.sqrt(gx * gx + gy * gy);
                    float orientation = (float) Math.atan2(gy, gx);

                    if (magnitude > 10) { // 阈值过滤
                        int bin = (int) ((orientation + Math.PI) * 4 / Math.PI) % 8;
                        histogram[bin] += magnitude;
                    }
                }
            }

            // 归一化直方图
            int total = 0;
            for (int h : histogram)
                total += h;

            if (total > 0) {
                for (int i = 0; i < 8; i++) {
                    hogFeatures[i] = (float) histogram[i] / total;
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
        // 每个通道压缩为4个bin，共12维
        float[] colorFeatures = new float[12];

        try {
            int[] rHistogram = new int[8];
            int[] gHistogram = new int[8];
            int[] bHistogram = new int[8];

            int width = bitmap.getWidth();
            int height = bitmap.getHeight();
            int totalPixels = Math.max(1, width * height); // 防止除零

            // 统计RGB颜色分布
            for (int y = 0; y < height; y++) {
                for (int x = 0; x < width; x++) {
                    int pixel = bitmap.getPixel(x, y);

                    int r = Color.red(pixel) / 32; // 0..7
                    int g = Color.green(pixel) / 32; // 0..7
                    int b = Color.blue(pixel) / 32; // 0..7

                    // 保险起见做边界夹取
                    r = Math.max(0, Math.min(7, r));
                    g = Math.max(0, Math.min(7, g));
                    b = Math.max(0, Math.min(7, b));

                    rHistogram[r]++;
                    gHistogram[g]++;
                    bHistogram[b]++;
                }
            }

            // 将8个bin按相邻两两合并为4个bin，避免越界写入
            for (int i = 0; i < 4; i++) {
                int rSum = rHistogram[2 * i] + rHistogram[2 * i + 1];
                int gSum = gHistogram[2 * i] + gHistogram[2 * i + 1];
                int bSum = bHistogram[2 * i] + bHistogram[2 * i + 1];

                colorFeatures[i] = rSum / (float) totalPixels;
                colorFeatures[i + 4] = gSum / (float) totalPixels;
                colorFeatures[i + 8] = bSum / (float) totalPixels;
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

                    kernel[y][x] = (float) response;
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

                    float magnitude = (float) Math.sqrt(gx * gx + gy * gy);

                    if (magnitude > 5) {
                        float orientation = (float) Math.atan2(gy, gx);
                        int bin = (int) ((orientation + Math.PI) * 4 / Math.PI) % 8;
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

                blockHog[0] = (float) histogram[maxBin] / totalGradients; // 主方向强度

                // 计算方向变化度
                float variation = 0;
                for (int i = 0; i < 8; i++) {
                    float diff = (float) histogram[i] / totalGradients - 0.125f; // 均匀分布的期望值
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
     * 计算两个特征向量的余弦相似度（鲁棒版，含调试日志）
     */
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
            android.util.Log.e(TAG,
                    "calculateSimilarity: NaN detected dot/n1/n2 -> " + dotProduct + ", " + norm1 + ", " + norm2);
            return 0.0f;
        }

        float n1 = (float) Math.sqrt(norm1);
        float n2 = (float) Math.sqrt(norm2);
        if (n1 == 0.0f || n2 == 0.0f) {
            if (DEBUG_SIMILARITY) {
                android.util.Log.w(TAG, "calculateSimilarity: zero norm detected n1=" + n1 + " n2=" + n2);
            }
            return 0.0f;
        }
        float sim = dotProduct / (n1 * n2);
        // clamp 到 [-1,1]
        if (sim > 1.0f)
            sim = 1.0f;
        else if (sim < -1.0f)
            sim = -1.0f;

        if (DEBUG_SIMILARITY) {
            int sampleCount = Math.min(5, vector1.length);
            android.util.Log.d(TAG, "SIM_DEBUG query=" + Arrays.toString(Arrays.copyOf(vector1, sampleCount)));
            android.util.Log.d(TAG, "SIM_DEBUG stored=" + Arrays.toString(Arrays.copyOf(vector2, sampleCount)));
            android.util.Log.d(TAG, "SIM_DEBUG dot=" + dotProduct + " n1=" + norm1 + " n2=" + norm2 + " sim=" + sim);
        }

        return sim;
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
        // 统一形式：读取后的向量做一次归一化，兼容历史未归一化数据
        return normalizeVector(floatArray);
    }

    /**
     * 保存人脸特征到数据库
     */
    public boolean saveFaceEmbedding(long studentId, float[] features, float quality, String faceImagePath) {
        try {
            // 使用当前模型的输出维度进行校验，避免与旧模型混用
            if (features == null || features.length != modelOutputDim) {
                Log.w(TAG, "saveFaceEmbedding: invalid features length=" + (features == null ? -1 : features.length)
                        + ", expected=" + modelOutputDim + ", modelVer=" + currentModelVersion);
                return false;
            }
            // 零向量保护：避免把无效向量写库
            float norm = 0f;
            for (float v : features)
                norm += v * v;
            if (norm == 0f) {
                Log.w(TAG, "saveFaceEmbedding: zero-norm vector, skip saving");
                return false;
            }
            // 统一形式：保存前再次归一化，确保库内均为单位向量
            float[] normalized = normalizeVector(features);
            byte[] vectorBytes = floatArrayToByteArray(normalized);
            long result = databaseHelper.insertFaceEmbedding(studentId, currentModelVersion, vectorBytes, quality);
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
     * 验证特定学生的身份（1:1 比对）
     */
    public RecognitionResult verifyStudentIdentity(Bitmap faceBitmap, Face face, long studentId) {
        float[] queryFeatures = extractFaceFeatures(faceBitmap, face);
        if (queryFeatures == null) {
            return new RecognitionResult(-1, 0f, "特征提取失败");
        }

        android.database.Cursor cursor = null;
        float bestSim = 0f;

        try {
            cursor = databaseHelper.getFaceEmbeddingsByStudent(studentId);
            if (cursor != null && cursor.moveToFirst()) {
                do {
                    String modelVer = cursor.getString(cursor.getColumnIndexOrThrow("modelVer"));
                    if (!currentModelVersion.equals(modelVer))
                        continue;

                    byte[] vecBytes = cursor.getBlob(cursor.getColumnIndexOrThrow("vector"));
                    float[] ref = byteArrayToFloatArray(vecBytes);

                    if (ref != null && ref.length == queryFeatures.length) {
                        float sim = calculateSimilarity(queryFeatures, ref);
                        if (sim > bestSim) {
                            bestSim = sim;
                        }
                    }
                } while (cursor.moveToNext());
            }
        } catch (Exception e) {
            Log.e(TAG, "verifyStudentIdentity error: " + e.getMessage(), e);
        } finally {
            if (cursor != null)
                cursor.close();
        }

        if (bestSim >= 0.6f) {
            return new RecognitionResult(studentId, bestSim, "验证成功");
        } else {
            return new RecognitionResult(-1, bestSim, "验证失败，相似度过低 (" + bestSim + ")");
        }
    }

    /**
     * 识别单个人脸
     * 返回最相似的学生ID和相似度
     */
    public RecognitionResult recognizeFace(Bitmap faceBitmap, Face face) {
        try {
            float[] queryFeatures = extractFaceFeatures(faceBitmap, face);
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
    public List<RecognitionResult> recognizeMultipleFaces(List<Bitmap> faceBitmaps, List<Face> faces) {
        List<RecognitionResult> results = new ArrayList<>();

        for (int i = 0; i < faceBitmaps.size(); i++) {
            RecognitionResult result = recognizeFace(faceBitmaps.get(i), faces.get(i));
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
     * 使用 ML Kit 提取的图像进行批量识别（限制在指定班级）
     */
    public List<RecognitionResult> recognizeMultipleFacesWithinClass(List<Bitmap> faceBitmaps,
            List<com.google.mlkit.vision.face.Face> faces, long classroomId) {
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
            com.google.mlkit.vision.face.Face f = faces.get(i);

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
