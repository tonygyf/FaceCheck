package com.example.facecheck.ui.face;

import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.RectF;
import android.graphics.drawable.BitmapDrawable;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.example.facecheck.R;
import com.example.facecheck.utils.ImageUtils;
import com.example.facecheck.utils.PhotoStorageManager;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class AvatarCropActivity extends AppCompatActivity {

    public static final String EXTRA_SOURCE_PATH = "extra_source_path";
    public static final String EXTRA_CROP_SCENE = "extra_crop_scene";
    public static final String CROP_SCENE_AVATAR = "avatar";
    public static final String CROP_SCENE_CHECKIN = "checkin";
    public static final String RESULT_CROPPED_PATH = "result_cropped_path";

    private TouchImageView cropImageView;
    private CircleCropOverlayView overlayView;
    private Bitmap sourceBitmap;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_avatar_crop);

        cropImageView = findViewById(R.id.ivAvatarCrop);
        overlayView = findViewById(R.id.avatarCropOverlay);
        Button btnCancel = findViewById(R.id.btnAvatarCropCancel);
        Button btnConfirm = findViewById(R.id.btnAvatarCropConfirm);

        cropImageView.setDrawingMode(false);

        String sourcePath = getIntent().getStringExtra(EXTRA_SOURCE_PATH);
        String cropScene = getIntent().getStringExtra(EXTRA_CROP_SCENE);
        if (cropScene == null || cropScene.trim().isEmpty()) {
            cropScene = CROP_SCENE_AVATAR;
        }
        if (sourcePath == null || sourcePath.trim().isEmpty()) {
            Toast.makeText(this, "图片路径无效", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        sourceBitmap = ImageUtils.loadAndResizeBitmap(sourcePath, 1600, 1600);
        if (sourceBitmap == null) {
            Toast.makeText(this, "图片加载失败", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }
        cropImageView.setImageBitmap(sourceBitmap);

        cropImageView.post(() -> {
            int size = (int) (Math.min(cropImageView.getWidth(), cropImageView.getHeight()) * 0.72f);
            overlayView.setCropSize(size);
        });

        btnCancel.setOnClickListener(v -> finish());
        String finalCropScene = cropScene;
        btnConfirm.setOnClickListener(v -> confirmCrop(finalCropScene));
    }

    private void confirmCrop(String cropScene) {
        if (cropImageView.getDrawable() == null) {
            Toast.makeText(this, "裁剪失败：图片未加载", Toast.LENGTH_SHORT).show();
            return;
        }

        RectF viewCropRect = overlayView.getCropRectInView(cropImageView.getWidth(), cropImageView.getHeight());
        RectF bitmapCropRect = cropImageView.mapViewRectToBitmapRect(viewCropRect);
        if (bitmapCropRect == null) {
            Toast.makeText(this, "裁剪失败：坐标转换异常", Toast.LENGTH_SHORT).show();
            return;
        }

        Bitmap displayBitmap = ((BitmapDrawable) cropImageView.getDrawable()).getBitmap();
        int left = Math.max(0, Math.round(bitmapCropRect.left));
        int top = Math.max(0, Math.round(bitmapCropRect.top));
        int right = Math.min(displayBitmap.getWidth(), Math.round(bitmapCropRect.right));
        int bottom = Math.min(displayBitmap.getHeight(), Math.round(bitmapCropRect.bottom));
        int width = Math.max(0, right - left);
        int height = Math.max(0, bottom - top);
        if (width <= 0 || height <= 0) {
            Toast.makeText(this, "裁剪区域无效", Toast.LENGTH_SHORT).show();
            return;
        }

        int size = Math.min(width, height);
        Bitmap square = Bitmap.createBitmap(displayBitmap, left, top, size, size);
        boolean isCheckinScene = CROP_SCENE_CHECKIN.equalsIgnoreCase(cropScene);
        File outDir = isCheckinScene ? PhotoStorageManager.getAttendancePhotosDir(this) : PhotoStorageManager.getAvatarPhotosDir(this);
        String ts = new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(new Date());
        File outFile = new File(outDir, (isCheckinScene ? "checkin_face_crop_" : "avatar_crop_") + ts + (isCheckinScene ? ".jpg" : ".png"));
        boolean saved;
        if (isCheckinScene) {
            saved = ImageUtils.saveBitmapToFile(square, outFile);
        } else {
            Bitmap output = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888);
            Canvas canvas = new Canvas(output);
            Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
            paint.setColor(Color.WHITE);
            canvas.drawColor(Color.TRANSPARENT);
            float radius = size / 2f;
            canvas.drawCircle(radius, radius, radius, paint);
            paint.setXfermode(new android.graphics.PorterDuffXfermode(android.graphics.PorterDuff.Mode.SRC_IN));
            canvas.drawBitmap(square, 0, 0, paint);
            paint.setXfermode(null);
            saved = ImageUtils.saveBitmapToPngFile(output, outFile);
            output.recycle();
        }
        square.recycle();
        if (!saved) {
            Toast.makeText(this, "保存裁剪头像失败", Toast.LENGTH_SHORT).show();
            return;
        }

        Intent result = new Intent();
        result.putExtra(RESULT_CROPPED_PATH, outFile.getAbsolutePath());
        setResult(RESULT_OK, result);
        finish();
    }

}
