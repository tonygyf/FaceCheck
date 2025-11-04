package com.example.facecheck.utils;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Rect;
import android.util.Log;

import org.tensorflow.lite.Interpreter;

import java.io.FileInputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

/**
 * YuNet TFLite 人脸检测（简化版）
 * - 期望模型文件位于 assets/models 下：
 *   - yunet_fp16_multi.tflite  多人脸（考勤/群体）优先使用 fp16 以提升速度
 *   - yunet_fp32_single.tflite 单人脸（精细检测）优先使用 fp32 以提升精度
 * - 输入默认 320x320，输出解析为 [x, y, w, h, score, ...]
 * 注意：不同 YuNet 转换版本的输出格式存在差异，必要时按实际模型调整 decode。
 */
public class YuNetTFLiteDetector {
    private static final String TAG = "YuNetTFLiteDetector";

    public enum ModelVariant { SINGLE_FACE, MULTI_FACE }

    private static final String ASSET_YUNET_MULTI = "models/yunet_fp16_multi.tflite";
    private static final String ASSET_YUNET_SINGLE = "models/yunet_fp32_single.tflite";

    private final Context context;
    private Interpreter interpreter;
    private final int inputSize;
    private final float scoreThreshold;
    private final float nmsThreshold;
    private final ModelVariant variant;

    public YuNetTFLiteDetector(Context context) {
        this(context, 320, 0.6f, 0.5f, ModelVariant.MULTI_FACE);
    }

    public YuNetTFLiteDetector(Context context, int inputSize, float scoreThreshold, float nmsThreshold) {
        this(context, inputSize, scoreThreshold, nmsThreshold, ModelVariant.MULTI_FACE);
    }

    public YuNetTFLiteDetector(Context context, int inputSize, float scoreThreshold, float nmsThreshold, ModelVariant variant) {
        this.context = context.getApplicationContext();
        this.inputSize = inputSize;
        this.scoreThreshold = scoreThreshold;
        this.nmsThreshold = nmsThreshold;
        this.variant = variant;
        try {
            String assetPath = (variant == ModelVariant.SINGLE_FACE) ? ASSET_YUNET_SINGLE : ASSET_YUNET_MULTI;
            MappedByteBuffer model = loadModelFile(assetPath);
            interpreter = new Interpreter(model);
            Log.d(TAG, "YuNet TFLite interpreter initialized");
        } catch (IOException e) {
            Log.e(TAG, "YuNet model load failed: " + e.getMessage());
            interpreter = null;
        } catch (Throwable t) {
            Log.e(TAG, "YuNet interpreter init error: " + t.getMessage());
            interpreter = null;
        }
    }

    private MappedByteBuffer loadModelFile(String assetPath) throws IOException {
        // 优先使用内存映射（要求资产未压缩），失败则回退到复制到缓存文件
        try {
            android.content.res.AssetFileDescriptor afd = context.getAssets().openFd(assetPath);
            try (FileInputStream fis = afd.createInputStream(); FileChannel channel = fis.getChannel()) {
                long startOffset = afd.getStartOffset();
                long declaredLength = afd.getLength();
                return channel.map(FileChannel.MapMode.READ_ONLY, startOffset, declaredLength);
            }
        } catch (IOException openFdErr) {
            Log.w(TAG, "openFd failed, fallback to cached copy: " + openFdErr.getMessage());
            java.io.File outFile = new java.io.File(context.getCacheDir(), assetPath.replace('/', '_'));
            if (!outFile.exists()) {
                try (java.io.InputStream is = context.getAssets().open(assetPath);
                     java.io.FileOutputStream fos = new java.io.FileOutputStream(outFile)) {
                    byte[] buf = new byte[8192];
                    int r;
                    while ((r = is.read(buf)) != -1) fos.write(buf, 0, r);
                }
            }
            try (FileInputStream fis = new FileInputStream(outFile); FileChannel channel = fis.getChannel()) {
                return channel.map(FileChannel.MapMode.READ_ONLY, 0, outFile.length());
            }
        }
    }

    private ByteBuffer preprocess(Bitmap src) {
        Bitmap resized = Bitmap.createScaledBitmap(src, inputSize, inputSize, true);
        ByteBuffer buffer = ByteBuffer.allocateDirect(inputSize * inputSize * 3 * 4);
        buffer.order(ByteOrder.LITTLE_ENDIAN);
        int[] pixels = new int[inputSize * inputSize];
        resized.getPixels(pixels, 0, inputSize, 0, 0, inputSize, inputSize);
        for (int i = 0; i < pixels.length; i++) {
            int p = pixels[i];
            float r = ((p >> 16) & 0xFF) / 255.0f;
            float g = ((p >> 8) & 0xFF) / 255.0f;
            float b = (p & 0xFF) / 255.0f;
            buffer.putFloat(r);
            buffer.putFloat(g);
            buffer.putFloat(b);
        }
        buffer.rewind();
        return buffer;
    }

    public static class Detection {
        public final Rect bbox;
        public final float score;
        public Detection(Rect bbox, float score) { this.bbox = bbox; this.score = score; }
    }

    /**
     * 运行检测并返回原图坐标下的候选框
     */
    public List<Rect> detect(Bitmap source) {
        if (interpreter == null) {
            Log.w(TAG, "YuNet interpreter not initialized; returning empty detections");
            return new ArrayList<>();
        }
        try {
            ByteBuffer input = preprocess(source);
            interpreter.allocateTensors();
            int[] outShape = interpreter.getOutputTensor(0).shape(); // e.g., [1, N, 15]
            int n = outShape.length >= 2 ? outShape[1] : 0;
            int m = outShape.length >= 3 ? outShape[2] : 0;
            if (n <= 0 || m < 5) {
                Log.e(TAG, "Unexpected YuNet output shape: " + java.util.Arrays.toString(outShape));
                return new ArrayList<>();
            }

            float[][][] output = new float[1][n][m];
            // 单输入、单输出
            interpreter.run(input, output);

            // 解析候选框（假设 [x, y, w, h, score, ...] 都为输入尺寸尺度）
            List<Detection> dets = new ArrayList<>();
            float sx = (float) source.getWidth() / inputSize;
            float sy = (float) source.getHeight() / inputSize;

            for (int i = 0; i < n; i++) {
                float[] row = output[0][i];
                float score = row[4];
                if (score < scoreThreshold) continue;
                float x = row[0];
                float y = row[1];
                float w = row[2];
                float h = row[3];
                int left = Math.max(0, Math.round(x * sx));
                int top = Math.max(0, Math.round(y * sy));
                int right = Math.min(source.getWidth(), Math.round((x + w) * sx));
                int bottom = Math.min(source.getHeight(), Math.round((y + h) * sy));
                if (right > left && bottom > top) {
                    dets.add(new Detection(new Rect(left, top, right, bottom), score));
                }
            }

            // NMS
            List<Detection> nmsDets = nonMaxSuppression(dets, nmsThreshold);
            List<Rect> rects = new ArrayList<>();
            for (Detection d : nmsDets) rects.add(d.bbox);
            return rects;
        } catch (Throwable t) {
            Log.e(TAG, "YuNet detect failed: " + t.getMessage(), t);
            return new ArrayList<>();
        }
    }

    private List<Detection> nonMaxSuppression(List<Detection> dets, float iouThresh) {
        if (dets.isEmpty()) return dets;
        List<Detection> res = new ArrayList<>();
        List<Detection> sorted = new ArrayList<>(dets);
        Collections.sort(sorted, Comparator.comparingDouble(d -> -d.score));
        boolean[] removed = new boolean[sorted.size()];
        for (int i = 0; i < sorted.size(); i++) {
            if (removed[i]) continue;
            Detection a = sorted.get(i);
            res.add(a);
            for (int j = i + 1; j < sorted.size(); j++) {
                if (removed[j]) continue;
                Detection b = sorted.get(j);
                if (iou(a.bbox, b.bbox) > iouThresh) {
                    removed[j] = true;
                }
            }
        }
        return res;
    }

    private float iou(Rect a, Rect b) {
        int x1 = Math.max(a.left, b.left);
        int y1 = Math.max(a.top, b.top);
        int x2 = Math.min(a.right, b.right);
        int y2 = Math.min(a.bottom, b.bottom);
        int inter = Math.max(0, x2 - x1) * Math.max(0, y2 - y1);
        int areaA = Math.max(0, a.right - a.left) * Math.max(0, a.bottom - a.top);
        int areaB = Math.max(0, b.right - b.left) * Math.max(0, b.bottom - b.top);
        int union = areaA + areaB - inter;
        return union > 0 ? ((float) inter / union) : 0f;
    }

    public boolean isReady() {
        return interpreter != null;
    }
}