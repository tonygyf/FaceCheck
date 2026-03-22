package com.example.facecheck.ui.profile;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.text.TextUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.FileProvider;

import com.airbnb.lottie.LottieAnimationView;
import com.airbnb.lottie.LottieListener;
import com.bumptech.glide.Glide;
import com.example.facecheck.R;
import com.example.facecheck.database.DatabaseHelper;
import com.example.facecheck.data.model.Teacher;
import com.example.facecheck.ui.auth.LoginActivity;
import com.example.facecheck.utils.PhotoStorageManager;
import com.example.facecheck.utils.SessionManager;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;

import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

import de.hdodenhof.circleimageview.CircleImageView;

public class UserProfileActivity extends AppCompatActivity {

    private static final String TAG = "UserProfileActivity";
    private static final int REQUEST_IMAGE_CAPTURE = 1;
    private static final int REQUEST_PICK_IMAGE = 2;

    // 头像
    private CircleImageView profileImageView;
    // 文字
    private android.widget.TextView usernameTextView;
    private android.widget.TextView emailTextView;
    // 列表项（新布局用LinearLayout）
    private View btnChangePhoto;       // ImageView id=btn_change_photo
    private LinearLayout itemChangeUsername;
    private LinearLayout itemChangePassword;
    private LinearLayout itemMoreSettings;
    private LinearLayout itemLogout;
    // 进度条
    private ProgressBar progressBar;
    // Lottie退出遮罩（布局内已有）
    private FrameLayout lottieOverlayLogout;
    private LottieAnimationView lottieLogoutView;

    private DatabaseHelper dbHelper;
    private Teacher currentTeacher;
    private SessionManager sessionManager;
    private boolean isNavigating = false;
    private String currentPhotoPath;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_user_profile);
        initViews();
        dbHelper = new DatabaseHelper(this);
        sessionManager = new SessionManager(this);
        loadUserData();
        setupClickListeners();
    }

    private void initViews() {
        profileImageView    = findViewById(R.id.profile_image);
        usernameTextView    = findViewById(R.id.tv_username);
        emailTextView       = findViewById(R.id.tv_email);
        btnChangePhoto      = findViewById(R.id.btn_change_photo);
        itemChangeUsername  = findViewById(R.id.item_change_username);
        itemChangePassword  = findViewById(R.id.item_change_password);
        itemMoreSettings    = findViewById(R.id.item_more_settings);
        itemLogout          = findViewById(R.id.item_logout);
        progressBar         = findViewById(R.id.progress_bar);
        lottieOverlayLogout = findViewById(R.id.lottieOverlayLogout);
        lottieLogoutView    = findViewById(R.id.lottieLogoutView);
    }

    private void loadUserData() {
        long teacherId = getIntent().getLongExtra("teacher_id", -1);
        if (teacherId == -1) {
            Toast.makeText(this, "教师信息无效", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        Cursor cursor = dbHelper.getReadableDatabase().query(
            "Teacher",
            new String[]{"id", "name", "username", "password", "avatarUri", "createdAt", "updatedAt"},
            "id = ?",
            new String[]{String.valueOf(teacherId)},
            null, null, null);

        if (cursor != null && cursor.moveToFirst()) {
            currentTeacher = new Teacher();
            currentTeacher.setId(cursor.getLong(cursor.getColumnIndexOrThrow("id")));
            currentTeacher.setName(cursor.getString(cursor.getColumnIndexOrThrow("name")));
            currentTeacher.setUsername(cursor.getString(cursor.getColumnIndexOrThrow("username")));
            currentTeacher.setPassword(cursor.getString(cursor.getColumnIndexOrThrow("password")));
            currentTeacher.setAvatarUri(cursor.getString(cursor.getColumnIndexOrThrow("avatarUri")));
            currentTeacher.setCreatedAt(cursor.getLong(cursor.getColumnIndexOrThrow("createdAt")));
            currentTeacher.setUpdatedAt(cursor.getLong(cursor.getColumnIndexOrThrow("updatedAt")));
            cursor.close();

            usernameTextView.setText(currentTeacher.getName());
            emailTextView.setText(currentTeacher.getUsername());

            if (currentTeacher.getAvatarUri() != null && !currentTeacher.getAvatarUri().isEmpty()) {
                File avatarFile = new File(currentTeacher.getAvatarUri());
                if (avatarFile.exists()) {
                    Glide.with(this).load(avatarFile).into(profileImageView);
                }
            }
        } else {
            Toast.makeText(this, "教师信息加载失败，请重新登录", Toast.LENGTH_SHORT).show();
            navigateToLogin();
        }
    }

    private void setupClickListeners() {
        // 更换头像
        if (btnChangePhoto != null) {
            btnChangePhoto.setOnClickListener(v -> showImageSourceDialog());
        }
        // 修改用户名
        if (itemChangeUsername != null) {
            itemChangeUsername.setOnClickListener(v -> showChangeUsernameDialog());
        }
        // 修改密码
        if (itemChangePassword != null) {
            itemChangePassword.setOnClickListener(v -> showChangePasswordDialog());
        }
        // 更多设置（暂留空）
        if (itemMoreSettings != null) {
            itemMoreSettings.setOnClickListener(v ->
                Toast.makeText(this, "更多设置开发中", Toast.LENGTH_SHORT).show());
        }
        // 退出登录
        if (itemLogout != null) {
            itemLogout.setOnClickListener(v -> playExitAndNavigate());
        }
    }

    // ===================== 图片相关 =====================

    private void showImageSourceDialog() {
        new AlertDialog.Builder(this)
            .setTitle("选择图片来源")
            .setItems(new String[]{"拍照", "从相册选择"}, (dialog, which) -> {
                if (which == 0) dispatchTakePictureIntent();
                else {
                    Intent pick = new Intent(Intent.ACTION_PICK,
                            MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
                    startActivityForResult(pick, REQUEST_PICK_IMAGE);
                }
            }).show();
    }

    private void dispatchTakePictureIntent() {
        Intent intent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        if (intent.resolveActivity(getPackageManager()) != null) {
            try {
                File photoFile = createImageFile();
                Uri photoURI = FileProvider.getUriForFile(this,
                        "com.example.facecheck.fileprovider", photoFile);
                intent.putExtra(MediaStore.EXTRA_OUTPUT, photoURI);
                startActivityForResult(intent, REQUEST_IMAGE_CAPTURE);
            } catch (IOException ex) {
                Toast.makeText(this, "无法创建图片文件", Toast.LENGTH_SHORT).show();
            }
        }
    }

    private File createImageFile() throws IOException {
        String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(new Date());
        File storageDir = PhotoStorageManager.getAvatarPhotosDir(this);
        File image = File.createTempFile("JPEG_" + timeStamp + "_", ".jpg", storageDir);
        currentPhotoPath = image.getAbsolutePath();
        return image;
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (resultCode != RESULT_OK) return;

        if (requestCode == REQUEST_IMAGE_CAPTURE && currentPhotoPath != null) {
            updateProfileImage(currentPhotoPath);
        } else if (requestCode == REQUEST_PICK_IMAGE && data != null) {
            Uri selectedImage = data.getData();
            try {
                File photoFile = createImageFile();
                copyUriToFile(selectedImage, photoFile);
                updateProfileImage(currentPhotoPath);
            } catch (IOException e) {
                Toast.makeText(this, "处理图片时出错", Toast.LENGTH_SHORT).show();
            }
        }
    }

    private void copyUriToFile(Uri sourceUri, File destFile) {
        try {
            java.io.InputStream in = getContentResolver().openInputStream(sourceUri);
            java.io.OutputStream out = new java.io.FileOutputStream(destFile);
            byte[] buf = new byte[4096];
            int len;
            while ((len = in.read(buf)) != -1) out.write(buf, 0, len);
            in.close();
            out.close();
        } catch (Exception e) {
            Toast.makeText(this, "复制图片失败", Toast.LENGTH_SHORT).show();
        }
    }

    private void updateProfileImage(String imagePath) {
        Glide.with(this).load(new File(imagePath)).into(profileImageView);
        currentTeacher.setAvatarUri(imagePath);
        boolean ok = dbHelper.updateTeacher(currentTeacher);
        Toast.makeText(this, ok ? "头像已更新" : "头像更新失败", Toast.LENGTH_SHORT).show();
    }

    // ===================== 对话框 =====================

    private void showChangeUsernameDialog() {
        View view = LayoutInflater.from(this).inflate(R.layout.dialog_input, null);
        EditText input = view.findViewById(R.id.et_input);
        input.setHint("新用户名");
        input.setText(currentTeacher.getName());

        new AlertDialog.Builder(this)
            .setTitle("修改用户名")
            .setView(view)
            .setPositiveButton("保存", (d, w) -> {
                String name = input.getText().toString().trim();
                if (!TextUtils.isEmpty(name)) {
                    currentTeacher.setName(name);
                    dbHelper.updateTeacher(currentTeacher);
                    usernameTextView.setText(name);
                    Toast.makeText(this, "用户名已更新", Toast.LENGTH_SHORT).show();
                } else {
                    Toast.makeText(this, "用户名不能为空", Toast.LENGTH_SHORT).show();
                }
            })
            .setNegativeButton("取消", null)
            .show();
    }

    private void showChangePasswordDialog() {
        View view = LayoutInflater.from(this).inflate(R.layout.dialog_change_password, null);
        EditText etCurrent = view.findViewById(R.id.et_current_password);
        EditText etNew     = view.findViewById(R.id.et_new_password);
        EditText etConfirm = view.findViewById(R.id.et_confirm_password);

        AlertDialog dialog = new AlertDialog.Builder(this)
            .setTitle("修改密码")
            .setView(view)
            .setPositiveButton("保存", null)
            .setNegativeButton("取消", null)
            .create();
        dialog.show();

        dialog.getButton(DialogInterface.BUTTON_POSITIVE).setOnClickListener(v -> {
            String cur  = etCurrent.getText().toString();
            String nw   = etNew.getText().toString();
            String conf = etConfirm.getText().toString();

            if (TextUtils.isEmpty(cur) || TextUtils.isEmpty(nw) || TextUtils.isEmpty(conf)) {
                Toast.makeText(this, "请输入完整信息", Toast.LENGTH_SHORT).show(); return;
            }
            if (!cur.equals(currentTeacher.getPassword())) {
                Toast.makeText(this, "当前密码不正确", Toast.LENGTH_SHORT).show(); return;
            }
            if (nw.length() < 6) {
                Toast.makeText(this, "新密码至少6位", Toast.LENGTH_SHORT).show(); return;
            }
            if (!nw.equals(conf)) {
                Toast.makeText(this, "两次输入不一致", Toast.LENGTH_SHORT).show(); return;
            }
            if (nw.equals(cur)) {
                Toast.makeText(this, "新密码不能与旧密码相同", Toast.LENGTH_SHORT).show(); return;
            }
            currentTeacher.setPassword(nw);
            boolean ok = dbHelper.updateTeacher(currentTeacher);
            Toast.makeText(this, ok ? "密码已更新" : "密码更新失败", Toast.LENGTH_SHORT).show();
            if (ok) dialog.dismiss();
        });
    }

    // ===================== 导航 =====================

    private void navigateToLogin() {
        sessionManager.logout();
        Intent intent = new Intent(this, LoginActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }

    private void playExitAndNavigate() {
        if (isNavigating) return;
        // 显示布局内自带的 overlay
        lottieOverlayLogout.setVisibility(View.VISIBLE);

        // 超时保底 3秒
        new android.os.Handler(android.os.Looper.getMainLooper())
            .postDelayed(this::doNavigate, 3000);

        lottieLogoutView.addAnimatorListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationEnd(Animator animation) {
                doNavigate();
            }
        });
        lottieLogoutView.setFailureListener((LottieListener<Throwable>) t -> {
            Log.e(TAG, "退出动画加载失败", t);
            doNavigate();
        });
        lottieLogoutView.playAnimation();
    }

    private void doNavigate() {
        if (isNavigating) return;
        isNavigating = true;
        lottieOverlayLogout.setVisibility(View.GONE);
        navigateToLogin();
    }
}