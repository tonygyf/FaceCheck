package com.example.facecheck.utils;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Rect;
import android.util.Log;

import org.tensorflow.lite.Interpreter;

import java.io.FileInputStream;
import java.io.IOException;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;
import java.util.ArrayList;
import java.util.List;

public class RetinaFaceTFLiteDetector {
    private static final String TAG = "RetinaFaceTFLite";
    private static final String MODEL_F16 = "models/retinaface/FaceDetector_float16.tflite";
    private static final String MODEL_F32 = "models/retinaface/FaceDetector_float32.tflite";

    private final Context context;
    private Interpreter interpreter;

    public enum Precision { F16, F32 }

    public RetinaFaceTFLiteDetector(Context ctx, Precision p) {
        this.context = ctx.getApplicationContext();
        try {
            String assetPath = p == Precision.F32 ? MODEL_F32 : MODEL_F16;
            MappedByteBuffer buf = loadModelFile(assetPath);
            interpreter = new Interpreter(buf);
        } catch (Throwable t) {
            Log.e(TAG, "load model failed: " + t.getMessage());
            interpreter = null;
        }
    }

    public List<Rect> detect(Bitmap src) {
        if (interpreter == null || src == null) return null;
        try {
            int w = 320, h = 320;
            Bitmap input = Bitmap.createScaledBitmap(src, w, h, true);
            int tensorCount = interpreter.getOutputTensorCount();
            Object[] outputs = new Object[tensorCount];
            for (int i = 0; i < tensorCount; i++) {
                int[] shape = interpreter.getOutputTensor(i).shape();
                int total = 1;
                for (int s : shape) total *= s;
                outputs[i] = new float[total];
            }

            int[] pixels = new int[w * h];
            input.getPixels(pixels, 0, w, 0, 0, w, h);
            float[][][][] in = new float[1][h][w][3];
            for (int y = 0; y < h; y++) {
                for (int x = 0; x < w; x++) {
                    int c = pixels[y * w + x];
                    float r = ((c >> 16) & 0xFF) / 255f;
                    float g = ((c >> 8) & 0xFF) / 255f;
                    float b = (c & 0xFF) / 255f;
                    in[0][y][x][0] = r;
                    in[0][y][x][1] = g;
                    in[0][y][x][2] = b;
                }
            }

            interpreter.runForMultipleInputsOutputs(new Object[]{in}, buildOutputMap(outputs));

            List<Rect> rects = tryDecode(outputs, src.getWidth(), src.getHeight(), w, h);
            if (rects == null || rects.isEmpty()) return null;
            return rects;
        } catch (Throwable t) {
            Log.e(TAG, "detect error: " + t.getMessage());
            return null;
        }
    }

    private java.util.Map<Integer, Object> buildOutputMap(Object[] outputs) {
        java.util.HashMap<Integer, Object> map = new java.util.HashMap<>();
        for (int i = 0; i < outputs.length; i++) map.put(i, outputs[i]);
        return map;
    }

    private List<Rect> tryDecode(Object[] outs, int srcW, int srcH, int inW, int inH) {
        for (Object o : outs) {
            if (o instanceof float[]) {
                float[] arr = (float[]) o;
                List<Rect> rects = new ArrayList<>();
                int stride = 5;
                if (arr.length % stride != 0) continue;
                for (int i = 0; i + 4 < arr.length; i += stride) {
                    float x = arr[i];
                    float y = arr[i + 1];
                    float w = arr[i + 2];
                    float h = arr[i + 3];
                    float s = arr[i + 4];
                    if (s < 0.6f) continue;
                    int rx = Math.round(x / inW * srcW);
                    int ry = Math.round(y / inH * srcH);
                    int rw = Math.round(w / inW * srcW);
                    int rh = Math.round(h / inH * srcH);
                    rects.add(new Rect(rx, ry, rx + rw, ry + rh));
                }
                return rects;
            }
        }
        return null;
    }

    private MappedByteBuffer loadModelFile(String assetPath) throws IOException {
        android.content.res.AssetFileDescriptor afd = context.getAssets().openFd(assetPath);
        FileInputStream fis = new FileInputStream(afd.getFileDescriptor());
        FileChannel channel = fis.getChannel();
        long start = afd.getStartOffset();
        long declaredLength = afd.getDeclaredLength();
        return channel.map(FileChannel.MapMode.READ_ONLY, start, declaredLength);
    }
}
