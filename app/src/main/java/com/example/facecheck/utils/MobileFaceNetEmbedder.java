package com.example.facecheck.utils;

import android.content.Context;
import android.graphics.Bitmap;
import android.util.Log;

import org.tensorflow.lite.Interpreter;

import java.io.FileInputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;

/**
 * MobileFaceNet TFLite 嵌入提取器
 * - 期望模型文件位于 assets/models/mobilefacenet.tflite
 * - 输入 112x112 RGB，输出 128 维向量（常见配置）
 */
public class MobileFaceNetEmbedder {
    private static final String TAG = "MobileFaceNetEmbedder";

    private final Context context;
    private Interpreter interpreter;
    private final int inputW = 112;
    private final int inputH = 112;
    private final int inputC = 3;
    private final ByteBuffer inputBuffer;
    private final int[] pixelCache;

    public MobileFaceNetEmbedder(Context context) {
        this.context = context.getApplicationContext();
        // 预分配输入缓冲与像素缓存
        inputBuffer = ByteBuffer.allocateDirect(inputW * inputH * inputC * 4).order(ByteOrder.LITTLE_ENDIAN);
        pixelCache = new int[inputW * inputH];
        try {
            MappedByteBuffer model = loadModelFile("models/mobilefacenet.tflite");
            Interpreter.Options options = new Interpreter.Options();
            int cpu = Runtime.getRuntime().availableProcessors();
            options.setNumThreads(Math.max(1, Math.min(4, cpu > 0 ? cpu - 1 : 1)));
            interpreter = new Interpreter(model, options);
            interpreter.allocateTensors();
            Log.d(TAG, "MobileFaceNet TFLite interpreter initialized");
        } catch (IOException e) {
            Log.e(TAG, "MobileFaceNet model load failed: " + e.getMessage());
            interpreter = null;
        } catch (Throwable t) {
            Log.e(TAG, "MobileFaceNet interpreter init error: " + t.getMessage());
            interpreter = null;
        }
    }

    private MappedByteBuffer loadModelFile(String assetPath) throws IOException {
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

    private void preprocessIntoBuffer(Bitmap src) {
        Bitmap resized = Bitmap.createScaledBitmap(src, inputW, inputH, true);
        inputBuffer.clear();
        resized.getPixels(pixelCache, 0, inputW, 0, 0, inputW, inputH);
        // MobileFaceNet 常用归一化到 [-1,1]
        for (int p : pixelCache) {
            float r = (((p >> 16) & 0xFF) / 127.5f) - 1.0f;
            float g = (((p >> 8) & 0xFF) / 127.5f) - 1.0f;
            float b = ((p & 0xFF) / 127.5f) - 1.0f;
            inputBuffer.putFloat(r);
            inputBuffer.putFloat(g);
            inputBuffer.putFloat(b);
        }
        inputBuffer.rewind();
    }

    /**
     * 提取 128 维嵌入；若模型未加载，返回 null。
     */
    public float[] embed(Bitmap face) {
        if (interpreter == null) {
            Log.w(TAG, "MobileFaceNet interpreter not initialized");
            return null;
        }
        try {
            preprocessIntoBuffer(face);
            int[] outShape = interpreter.getOutputTensor(0).shape(); // e.g., [1, 128]
            int dim = (outShape.length >= 2) ? outShape[1] : 128;
            float[][] output = new float[1][dim];
            interpreter.run(inputBuffer, output);
            // 归一化到单位向量
            float[] vec = output[0];
            float norm = 0f;
            for (float v : vec) norm += v * v;
            norm = (float) Math.sqrt(norm);
            if (norm > 0f) {
                for (int i = 0; i < vec.length; i++) vec[i] /= norm;
            }
            return vec;
        } catch (Throwable t) {
            Log.e(TAG, "MobileFaceNet embed failed: " + t.getMessage(), t);
            return null;
        }
    }

    public boolean isReady() {
        return interpreter != null;
    }
}