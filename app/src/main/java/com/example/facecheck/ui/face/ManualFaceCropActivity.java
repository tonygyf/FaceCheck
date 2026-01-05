package com.example.facecheck.ui.face;

import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Matrix;
import android.graphics.Rect;
import android.graphics.RectF;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.CompoundButton;
import com.google.android.material.switchmaterial.SwitchMaterial;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.facecheck.R;
import com.example.facecheck.utils.ImageUtils;

import java.util.ArrayList;
import java.util.List;

public class ManualFaceCropActivity extends AppCompatActivity {

    public static final String EXTRA_IMAGE_URI = "extra_image_uri";
    public static final String RESULT_CROP_RECTS = "result_crop_rects"; // Changed to list

    private TouchImageView ivCropImage;
    private Bitmap originalBitmap;
    private SwitchMaterial switchMode;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_manual_face_crop);

        ivCropImage = findViewById(R.id.ivCropImage);
        switchMode = findViewById(R.id.switchMode);

        switchMode.setOnCheckedChangeListener((buttonView, isChecked) -> {
            ivCropImage.setDrawingMode(isChecked);
        });

        findViewById(R.id.btnCancel).setOnClickListener(v -> finish());
        findViewById(R.id.btnConfirm).setOnClickListener(v -> confirmCrop());
        findViewById(R.id.btnUndo).setOnClickListener(v -> ivCropImage.undoLastRect());
        findViewById(R.id.btnClear).setOnClickListener(v -> ivCropImage.clearRects());

        Uri imageUri = getIntent().getParcelableExtra(EXTRA_IMAGE_URI);
        if (imageUri != null) {
            try {
                // Load max 1600x1600 to match AttendanceActivity
                originalBitmap = ImageUtils.loadAndResizeBitmap(this, imageUri, 1600, 1600);
                if (originalBitmap != null) {
                    ivCropImage.setImageBitmap(originalBitmap);
                } else {
                    Toast.makeText(this, "图片加载失败", Toast.LENGTH_SHORT).show();
                    finish();
                }
            } catch (Exception e) {
                e.printStackTrace();
                Toast.makeText(this, "图片加载异常", Toast.LENGTH_SHORT).show();
                finish();
            }
        } else {
            finish();
        }
    }

    private void confirmCrop() {
        if (originalBitmap == null)
            return;

        List<RectF> rects = ivCropImage.getFaceRects();
        if (rects.isEmpty()) {
            Toast.makeText(this, "请至少框选一个人脸", Toast.LENGTH_SHORT).show();
            return;
        }

        ArrayList<Rect> finalRects = new ArrayList<>();
        for (RectF r : rects) {
            Rect finalRect = new Rect();
            r.round(finalRect);
            if (finalRect.width() > 0 && finalRect.height() > 0) {
                finalRects.add(finalRect);
            }
        }

        if (finalRects.isEmpty()) {
            Toast.makeText(this, "无效的框选区域", Toast.LENGTH_SHORT).show();
            return;
        }

        Intent result = new Intent();
        result.putParcelableArrayListExtra(RESULT_CROP_RECTS, finalRects);
        setResult(RESULT_OK, result);
        finish();
    }
}
