package com.example.facecheck.ui.face;

import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.Rect;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.text.TextUtils;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.facecheck.R;
import com.example.facecheck.adapter.FaceSegmentationAdapter;
import com.example.facecheck.database.DatabaseHelper;
import com.example.facecheck.utils.ImageStorageManager;
import com.example.facecheck.utils.PhotoStorageManager;
import com.example.facecheck.utils.RetinaFaceTFLiteDetector;
import com.example.facecheck.utils.YuNetTFLiteDetector;

import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class FaceMiniDetectActivity extends AppCompatActivity {
    private static final int REQ_CAMERA = 1001;
    private static final int REQ_PICK = 1002;

    private Button btnCamera, btnPick, btnImport;
    private EditText etClassName;
    private RecyclerView recycler;
    private ProgressBar progress;
    private TextView tvStatus;

    private FaceSegmentationAdapter adapter;
    private final List<String> facePaths = new ArrayList<>();
    private Uri currentPhotoUri;
    private ImageStorageManager imageStorageManager;
    private static final int REQ_PERM_CAMERA = 2001;
    private static final int REQ_PERM_READ_MEDIA = 2002;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_face_mini_detect);
        imageStorageManager = new ImageStorageManager(this);
        btnCamera = findViewById(R.id.btnCamera);
        btnPick = findViewById(R.id.btnPick);
        btnImport = findViewById(R.id.btnImport);
        etClassName = findViewById(R.id.etClassName);
        recycler = findViewById(R.id.recyclerFaces);
        progress = findViewById(R.id.progressBar);
        tvStatus = findViewById(R.id.tvStatus);

        recycler.setLayoutManager(new GridLayoutManager(this, 3));
        adapter = new FaceSegmentationAdapter(this, facePaths);
        recycler.setAdapter(adapter);

        btnCamera.setOnClickListener(v -> startCamera());
        btnPick.setOnClickListener(v -> pickPhoto());
        btnImport.setOnClickListener(v -> importSelectedFaces());
    }

    private void startCamera() {
        if (!hasCameraPermission()) {
            requestCameraPermission();
            return;
        }
        try {
            String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(new Date());
            File image = File.createTempFile("mini_" + timeStamp + "_", ".jpg", getExternalFilesDir("Pictures"));
            currentPhotoUri = androidx.core.content.FileProvider.getUriForFile(this, "com.example.facecheck.fileprovider", image);
            Intent intent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
            intent.putExtra(MediaStore.EXTRA_OUTPUT, currentPhotoUri);
            intent.addFlags(Intent.FLAG_GRANT_WRITE_URI_PERMISSION | Intent.FLAG_GRANT_READ_URI_PERMISSION);
            startActivityForResult(intent, REQ_CAMERA);
        } catch (IOException e) {
            Toast.makeText(this, "无法启动相机", Toast.LENGTH_SHORT).show();
        }
    }

    private void pickPhoto() {
        if (!hasReadImagesPermission()) {
            requestReadImagesPermission();
            return;
        }
        Intent intent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
        startActivityForResult(intent, REQ_PICK);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (resultCode != RESULT_OK) return;
        Uri uri = null;
        if (requestCode == REQ_CAMERA) uri = currentPhotoUri;
        else if (requestCode == REQ_PICK && data != null) uri = data.getData();
        if (uri != null) detectFaces(uri);
    }

    private boolean hasCameraPermission() {
        return androidx.core.content.ContextCompat.checkSelfPermission(this, android.Manifest.permission.CAMERA) == android.content.pm.PackageManager.PERMISSION_GRANTED;
    }
    private void requestCameraPermission() {
        androidx.core.app.ActivityCompat.requestPermissions(this, new String[]{android.Manifest.permission.CAMERA}, REQ_PERM_CAMERA);
    }
    private boolean hasReadImagesPermission() {
        if (android.os.Build.VERSION.SDK_INT >= 33) {
            return androidx.core.content.ContextCompat.checkSelfPermission(this, android.Manifest.permission.READ_MEDIA_IMAGES) == android.content.pm.PackageManager.PERMISSION_GRANTED;
        } else {
            return true;
        }
    }
    private void requestReadImagesPermission() {
        if (android.os.Build.VERSION.SDK_INT >= 33) {
            androidx.core.app.ActivityCompat.requestPermissions(this, new String[]{android.Manifest.permission.READ_MEDIA_IMAGES}, REQ_PERM_READ_MEDIA);
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == REQ_PERM_CAMERA) {
            if (grantResults.length > 0 && grantResults[0] == android.content.pm.PackageManager.PERMISSION_GRANTED) {
                startCamera();
            } else {
                Toast.makeText(this, "需要相机权限以拍照", Toast.LENGTH_SHORT).show();
            }
        } else if (requestCode == REQ_PERM_READ_MEDIA) {
            if (grantResults.length > 0 && grantResults[0] == android.content.pm.PackageManager.PERMISSION_GRANTED) {
                pickPhoto();
            } else {
                Toast.makeText(this, "需要读取图片权限以选择照片", Toast.LENGTH_SHORT).show();
            }
        }
    }

    private void detectFaces(Uri uri) {
        progress.setVisibility(View.VISIBLE);
        tvStatus.setText("正在识别人脸...");
        new Thread(() -> {
            try {
                Bitmap bitmap = com.example.facecheck.utils.ImageUtils.loadAndResizeBitmap(FaceMiniDetectActivity.this, uri, 1600, 1600);
                List<Rect> rects = null;
                try {
                    RetinaFaceTFLiteDetector det = new RetinaFaceTFLiteDetector(FaceMiniDetectActivity.this, RetinaFaceTFLiteDetector.Precision.F16);
                    rects = det.detect(bitmap);
                } catch (Throwable ignore) {}
                if (rects == null || rects.isEmpty()) {
                    YuNetTFLiteDetector y = new YuNetTFLiteDetector(FaceMiniDetectActivity.this);
                    rects = y.detect(bitmap);
                }
                if (rects == null || rects.isEmpty()) {
                    runOnUiThread(() -> {
                        progress.setVisibility(View.GONE);
                        tvStatus.setText("未检测到人脸");
                        Toast.makeText(FaceMiniDetectActivity.this, "未检测到人脸", Toast.LENGTH_SHORT).show();
                    });
                    return;
                }
                List<String> paths = new ArrayList<>();
                for (int i = 0; i < rects.size(); i++) {
                    Rect r = rects.get(i);
                    Bitmap fb = crop(bitmap, r, 0.25f);
                    if (fb != null) {
                        String p = imageStorageManager.saveTempImage(fb, "retina_face_" + i);
                        if (p != null) paths.add(p);
                    }
                }
                runOnUiThread(() -> {
                    facePaths.clear();
                    facePaths.addAll(paths);
                    adapter.notifyDataSetChanged();
                    progress.setVisibility(View.GONE);
                    tvStatus.setText("检测到 " + paths.size() + " 个人脸");
                });
            } catch (Throwable t) {
                runOnUiThread(() -> {
                    progress.setVisibility(View.GONE);
                    tvStatus.setText("识别失败");
                    Toast.makeText(FaceMiniDetectActivity.this, "识别失败", Toast.LENGTH_SHORT).show();
                });
            }
        }).start();
    }

    private Bitmap crop(Bitmap src, Rect r, float margin) {
        int w = src.getWidth();
        int h = src.getHeight();
        int mx = (int) (r.width() * margin);
        int my = (int) (r.height() * margin);
        int x = Math.max(0, r.left - mx);
        int y = Math.max(0, r.top - my);
        int rw = Math.min(w - x, r.width() + 2 * mx);
        int rh = Math.min(h - y, r.height() + 2 * my);
        try {
            return Bitmap.createBitmap(src, x, y, rw, rh);
        } catch (Throwable t) {
            return null;
        }
    }

    private void importSelectedFaces() {
        String className = etClassName.getText().toString().trim();
        if (TextUtils.isEmpty(className)) {
            Toast.makeText(this, "请输入班级名称", Toast.LENGTH_SHORT).show();
            return;
        }
        List<String> selected = adapter.getSelectedImagePaths();
        if (selected.isEmpty()) {
            Toast.makeText(this, "请先选择要导入的人脸", Toast.LENGTH_SHORT).show();
            return;
        }
        SharedPreferences prefs = getSharedPreferences("user_prefs", MODE_PRIVATE);
        long teacherId = prefs.getLong("teacher_id", -1);
        if (teacherId == -1) {
            Toast.makeText(this, "登录信息无效", Toast.LENGTH_SHORT).show();
            return;
        }
        DatabaseHelper db = new DatabaseHelper(this);
        int year = Integer.parseInt(new SimpleDateFormat("yyyy", Locale.getDefault()).format(new Date()));
        long classId = db.insertClassroom(teacherId, className, year, "{\"source\":\"mini-detect\"}");
        if (classId == -1) {
            Toast.makeText(this, "创建班级失败", Toast.LENGTH_SHORT).show();
            return;
        }
        File avatarDir = PhotoStorageManager.getAvatarPhotosDir(this);
        int index = 1;
        for (String path : selected) {
            String studentName = "学生" + index;
            String sid = String.valueOf(index);
            String avatarPath = copyToAvatarDir(path, avatarDir, "avatar_" + System.currentTimeMillis() + "_" + index + ".jpg");
            db.insertStudent(classId, studentName, sid, "O", avatarPath);
            index++;
        }
        Toast.makeText(this, "已导入 " + selected.size() + " 人", Toast.LENGTH_SHORT).show();
        finish();
    }

    private String copyToAvatarDir(String srcPath, File dir, String fileName) {
        try {
            File target = new File(dir, fileName);
            java.io.FileInputStream fis = new java.io.FileInputStream(new File(srcPath));
            java.io.FileOutputStream fos = new java.io.FileOutputStream(target);
            byte[] buf = new byte[4096];
            int r;
            while ((r = fis.read(buf)) != -1) fos.write(buf, 0, r);
            fis.close();
            fos.close();
            return target.getAbsolutePath();
        } catch (Throwable t) {
            return null;
        }
    }
}
