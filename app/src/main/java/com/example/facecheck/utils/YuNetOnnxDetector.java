package com.example.facecheck.utils;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Rect;
import android.util.Log;

import ai.onnxruntime.NodeInfo;
import ai.onnxruntime.OnnxTensor;
import ai.onnxruntime.OnnxValue;
import ai.onnxruntime.OrtEnvironment;
import ai.onnxruntime.OrtSession;
import ai.onnxruntime.TensorInfo;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;

/**
 * YuNet ONNX 人脸检测（基础版）
 * - 直接从 assets/models/face_detection_yunet_2023mar.onnx 加载
 * - 自动探测输入尺寸；默认回退到 320
 * - 输出解码策略：
 *   1) 优先查找形状 [1, N, M] 且 M>=5 的融合输出，按 [x,y,w,h,score,...] 解析；
 *   2) 若无融合输出，则暂不解析（后续根据实际模型结构迭代），返回空列表但不中断流程。
 */
public class YuNetOnnxDetector {
    private static final String TAG = "YuNetOnnxDetector";
    private static final String ASSET_ONNX = "models/face_detection_yunet_2023mar.onnx";

    private final Context context;
    private final float scoreThreshold;
    private final float nmsThreshold;
    private int inputSize;

    private OrtEnvironment env;
    private OrtSession session;
    private String inputName;
    private long[] inputShape;

    public YuNetOnnxDetector(Context context) {
        this(context, 320, 0.6f, 0.5f);
    }

    public YuNetOnnxDetector(Context context, int inputSize, float scoreThreshold, float nmsThreshold) {
        this.context = context.getApplicationContext();
        this.inputSize = inputSize;
        this.scoreThreshold = scoreThreshold;
        this.nmsThreshold = nmsThreshold;
        try {
            byte[] modelBytes = loadAssetBytes(ASSET_ONNX);
            if (modelBytes == null || modelBytes.length == 0) {
                Log.e(TAG, "ONNX 模型未找到或为空: " + ASSET_ONNX);
                return;
            }
            env = OrtEnvironment.getEnvironment();
            session = env.createSession(modelBytes);
            // 探测输入信息
            Map<String, NodeInfo> inMap = session.getInputInfo();
            if (!inMap.isEmpty()) {
                Map.Entry<String, NodeInfo> first = inMap.entrySet().iterator().next();
                inputName = first.getKey();
                TensorInfo ti = (TensorInfo) first.getValue().getInfo();
                inputShape = ti.getShape();
                // 形状可能包含 -1（动态），此时回退到默认 320
                if (inputShape != null && inputShape.length >= 4) {
                    long h = inputShape[2];
                    long w = inputShape[3];
                    if (h > 0 && w > 0) {
                        inputSize = (int) Math.max(h, w);
                    }
                }
            }
            Log.d(TAG, "YuNet ONNX session initialized. inputName=" + inputName + ", inputSize=" + inputSize);
        } catch (Throwable t) {
            Log.e(TAG, "初始化 YuNet ONNX 失败: " + t.getMessage(), t);
            session = null;
        }
    }

    public boolean isReady() {
        return session != null;
    }

    public List<Rect> detect(Bitmap source) {
        if (session == null) {
            Log.w(TAG, "YuNet ONNX session 未初始化");
            return new ArrayList<>();
        }
        try {
            float[] chw = preprocessCHW(source, inputSize, inputSize);
            long[] shape = new long[]{1, 3, inputSize, inputSize};

            ByteBuffer bb = ByteBuffer.allocateDirect(chw.length * 4).order(ByteOrder.nativeOrder());
            FloatBuffer fb = bb.asFloatBuffer();
            fb.put(chw);
            fb.flip();
            try (OnnxTensor input = OnnxTensor.createTensor(env, fb, shape)) {
                OrtSession.Result result = session.run(Collections.singletonMap(inputName, input));
                try {
                    // 策略 1：查找融合输出 [1, N, M]
                    List<Rect> rects = new ArrayList<>();
                    float sx = (float) source.getWidth() / inputSize;
                    float sy = (float) source.getHeight() / inputSize;

                    int outCount = result.size();
                    for (int i = 0; i < outCount; i++) {
                        OnnxValue ov = result.get(i);
                        if (!(ov instanceof OnnxTensor)) continue;
                        Object val = ((OnnxTensor) ov).getValue();
                        if (!(val instanceof float[][][])) continue;
                        float[][][] arr = (float[][][]) val;
                        if (arr.length < 1) continue;
                        int n = arr[0].length;
                        if (n <= 0 || arr[0][0] == null) continue;
                        int m = arr[0][0].length;
                        if (m < 5) continue;
                        for (int j = 0; j < n; j++) {
                            float[] row = arr[0][j];
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
                                rects.add(new Rect(left, top, right, bottom));
                            }
                        }
                        break; // 已解析融合输出即可
                    }

                    if (rects.isEmpty()) {
                        Log.w(TAG, "未发现融合输出，当前版本暂不解码多输出（bbox/cls/obj/kps）结构");
                        return new ArrayList<>();
                    }

                    // NMS
                    List<Rect> nmsRects = nonMaxSuppressionRects(rects, nmsThreshold);
                    return nmsRects;
                } finally {
                    result.close();
                }
            }
        } catch (Throwable t) {
            Log.e(TAG, "YuNet ONNX 检测失败: " + t.getMessage(), t);
            return new ArrayList<>();
        }
    }

    private byte[] loadAssetBytes(String assetPath) {
        try (InputStream is = context.getAssets().open(assetPath)) {
            byte[] buf = new byte[is.available()];
            int off = 0;
            int read;
            while ((read = is.read(buf, off, buf.length - off)) != -1) {
                off += read;
            }
            return buf;
        } catch (IOException e) {
            Log.e(TAG, "读取资产失败: " + assetPath + ", " + e.getMessage());
            return null;
        }
    }

    private float[] preprocessCHW(Bitmap src, int targetW, int targetH) {
        Bitmap resized = Bitmap.createScaledBitmap(src, targetW, targetH, true);
        int[] pixels = new int[targetW * targetH];
        resized.getPixels(pixels, 0, targetW, 0, 0, targetW, targetH);
        float[] chw = new float[3 * targetW * targetH];
        int cOffsetR = 0;
        int cOffsetG = targetW * targetH;
        int cOffsetB = 2 * targetW * targetH;
        for (int y = 0; y < targetH; y++) {
            for (int x = 0; x < targetW; x++) {
                int idx = y * targetW + x;
                int color = pixels[idx];
                float r = ((color >> 16) & 0xFF) / 255.0f;
                float g = ((color >> 8) & 0xFF) / 255.0f;
                float b = (color & 0xFF) / 255.0f;
                // NCHW: R,G,B 顺序
                chw[cOffsetR + idx] = r;
                chw[cOffsetG + idx] = g;
                chw[cOffsetB + idx] = b;
            }
        }
        return chw;
    }

    private List<Rect> nonMaxSuppressionRects(List<Rect> rects, float iouThreshold) {
        // 简化：按面积排序后做 IOU 抑制（缺少 score，这里用面积近似顺序）
        List<Rect> sorted = new ArrayList<>(rects);
        Collections.sort(sorted, new Comparator<Rect>() {
            @Override
            public int compare(Rect o1, Rect o2) {
                long a1 = (long) (o1.width()) * o1.height();
                long a2 = (long) (o2.width()) * o2.height();
                return Long.compare(a2, a1);
            }
        });
        List<Rect> picked = new ArrayList<>();
        for (Rect r : sorted) {
            boolean keep = true;
            for (Rect p : picked) {
                if (iou(r, p) > iouThreshold) { keep = false; break; }
            }
            if (keep) picked.add(r);
        }
        return picked;
    }

    private float iou(Rect a, Rect b) {
        int x1 = Math.max(a.left, b.left);
        int y1 = Math.max(a.top, b.top);
        int x2 = Math.min(a.right, b.right);
        int y2 = Math.min(a.bottom, b.bottom);
        int inter = Math.max(0, x2 - x1) * Math.max(0, y2 - y1);
        int areaA = a.width() * a.height();
        int areaB = b.width() * b.height();
        return areaA + areaB == 0 ? 0f : (float) inter / (areaA + areaB - inter + 1e-6f);
    }
}