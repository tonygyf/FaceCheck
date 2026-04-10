package com.example.facecheck.utils;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Rect;
import android.net.Uri;
import android.util.Log;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * 人脸检测管理器
 * 本地检测引擎已移除，当前仅提供基于整图的兜底框选能力。
 */
public class FaceDetectionManager {

    private static final String TAG = "FaceDetectionManager";
    private final Context context;
    private final ImageStorageManager storageManager;

    // 人脸检测结果回调
    public interface FaceDetectionCallback {
        void onSuccess(List<Rect> faces, List<Bitmap> faceBitmaps);

        void onFailure(Exception e);
    }

    public FaceDetectionManager(Context context) {
        this.context = context;
        this.storageManager = new ImageStorageManager(context);
    }

    /**
     * 从URI检测人脸（使用EXIF方向一致的Bitmap进行检测与分割，避免低分辨率缩略图导致识别失败）
     */
    public void detectFacesFromUri(Uri imageUri, FaceDetectionCallback callback) {
        try {
            Bitmap orientedBitmap = ImageUtils.loadAndResizeBitmap(context, imageUri, 1600, 1600);
            if (orientedBitmap == null) {
                callback.onFailure(new IOException("无法加载原始图片用于检测"));
                return;
            }
            List<Rect> faces = detectFacesSync(orientedBitmap);
            List<Bitmap> faceBitmaps = extractFaceBitmaps(orientedBitmap, faces);
            callback.onSuccess(faces, faceBitmaps);
        } catch (Exception e) {
            callback.onFailure(e);
        }
    }

    /**
     * 从Bitmap检测人脸
     */
    public void detectFacesFromBitmap(Bitmap bitmap, FaceDetectionCallback callback) {
        try {
            List<Rect> faces = detectFacesSync(bitmap);
            List<Bitmap> faceBitmaps = extractFaceBitmaps(bitmap, faces);
            callback.onSuccess(faces, faceBitmaps);
        } catch (Exception e) {
            callback.onFailure(e);
        }
    }

    /**
     * 同步版人脸检测（阻塞当前线程，需在工作线程调用）
     */
    public List<Rect> detectFacesSync(Bitmap bitmap) {
        if (bitmap == null)
            return new ArrayList<>();
        List<Rect> result = new ArrayList<>();
        int w = bitmap.getWidth();
        int h = bitmap.getHeight();
        if (w <= 0 || h <= 0) {
            return result;
        }
        // 无本地检测时，默认返回整图中心区域作为单人脸兜底框。
        int bw = (int) (w * 0.7f);
        int bh = (int) (h * 0.7f);
        int left = (w - bw) / 2;
        int top = (h - bh) / 2;
        result.add(new Rect(left, top, left + bw, top + bh));
        return result;
    }

    /**
     * 从人脸区域提取单个人脸Bitmap
     */
    private List<Bitmap> extractFaceBitmaps(Bitmap originalBitmap, List<Rect> faces) {
        List<Bitmap> faceBitmaps = new ArrayList<>();

        for (Rect bounds : faces) {

            // 确保边界在图片范围内
            int left = Math.max(0, bounds.left);
            int top = Math.max(0, bounds.top);
            int right = Math.min(originalBitmap.getWidth(), bounds.right);
            int bottom = Math.min(originalBitmap.getHeight(), bounds.bottom);

            // 添加一些边距，确保完整包含人脸
            int width = right - left;
            int height = bottom - top;
            int margin = Math.min(width, height) / 4;

            left = Math.max(0, left - margin);
            top = Math.max(0, top - margin);
            right = Math.min(originalBitmap.getWidth(), right + margin);
            bottom = Math.min(originalBitmap.getHeight(), bottom + margin);

            try {
                int w = right - left;
                int h = bottom - top;
                if (w <= 0 || h <= 0) {
                    Log.w(TAG, "skip invalid face bounds: " + (bounds != null ? bounds.toShortString() : "null")
                            + ", computed w=" + w + ", h=" + h);
                    continue;
                }
                Bitmap faceBitmap = Bitmap.createBitmap(originalBitmap, left, top, w, h);
                faceBitmaps.add(faceBitmap);
            } catch (Exception e) {
                // 如果提取失败，跳过这个人脸
                e.printStackTrace();
            }
        }

        return faceBitmaps;
    }

    /**
     * 在Bitmap上绘制人脸边界框
     */
    public Bitmap drawFaceBounds(Bitmap bitmap, List<Rect> faces) {
        Bitmap mutableBitmap = bitmap.copy(Bitmap.Config.ARGB_8888, true);
        Canvas canvas = new Canvas(mutableBitmap);
        Paint paint = new Paint();

        paint.setColor(Color.RED);
        paint.setStyle(Paint.Style.STROKE);
        paint.setStrokeWidth(5f);

        Paint textPaint = new Paint();
        textPaint.setColor(Color.RED);
        textPaint.setTextSize(40f);
        textPaint.setStyle(Paint.Style.FILL);

        int faceIndex = 1;
        for (Rect bounds : faces) {
            canvas.drawRect(bounds, paint);

            // 绘制人脸编号
            canvas.drawText("Face " + faceIndex, bounds.left, bounds.top - 10, textPaint);
            faceIndex++;
        }

        return mutableBitmap;
    }

    /**
     * 保存人脸图片到本地（使用ImageStorageManager）
     */
    public List<String> saveFaceBitmaps(List<Bitmap> faceBitmaps, String sessionId) {
        List<String> faceImagePaths = new ArrayList<>();

        for (int i = 0; i < faceBitmaps.size(); i++) {
            String imagePath = storageManager.saveSegmentedFace(faceBitmaps.get(i), sessionId, i);
            if (imagePath != null) {
                faceImagePaths.add(imagePath);
            }
        }

        return faceImagePaths;
    }

    /**
     * 保存原始照片（使用ImageStorageManager）
     */
    public String saveOriginalPhoto(Bitmap originalBitmap, String sessionId) {
        return storageManager.saveOriginalPhoto(originalBitmap, sessionId);
    }

    /**
     * 保存处理后的人脸（使用ImageStorageManager）
     */
    public List<String> saveProcessedFaces(List<Bitmap> processedFaces, String sessionId) {
        List<String> processedImagePaths = new ArrayList<>();

        for (int i = 0; i < processedFaces.size(); i++) {
            String imagePath = storageManager.saveProcessedFace(processedFaces.get(i), sessionId, i);
            if (imagePath != null) {
                processedImagePaths.add(imagePath);
            }
        }

        return processedImagePaths;
    }

    /**
     * 获取会话的所有图片
     */
    public List<String> getSessionImages(String sessionId) {
        return storageManager.getSessionImages(sessionId);
    }

    /**
     * 删除会话的所有图片
     */
    public boolean deleteSessionImages(String sessionId) {
        return storageManager.deleteSessionImages(sessionId);
    }

    /**
     * 清理释放资源
     */
    public void cleanup() {
        Log.i(TAG, "cleanup: no-op after local detector removal");
    }
}
