package com.example.facecheck.ui.profile;

import android.app.AlertDialog;
import android.content.ContentValues;
import android.content.DialogInterface;
import android.content.Intent;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.text.TextUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.FileProvider;

import com.bumptech.glide.Glide;
import com.example.facecheck.R;
import com.example.facecheck.database.DatabaseHelper;
import com.example.facecheck.data.model.Teacher;
import com.example.facecheck.ui.auth.LoginActivity;
import com.example.facecheck.utils.PhotoStorageManager;
// import com.example.facecheck.webdav.WebDavManager; // WebDAV功能已移除
import com.airbnb.lottie.LottieAnimationView;
import com.airbnb.lottie.LottieDrawable;
import com.airbnb.lottie.LottieListener;
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

    private CircleImageView profileImageView;
    private TextView usernameTextView;
    private TextView emailTextView;
    private TextView webDavStatusTextView;
    private Button changePhotoButton;
    private Button changeUsernameButton;
    private Button changePasswordButton;
    private Button webDavConfigButton;
    private Button syncNowButton;
    private Button logoutButton;
    private Switch webDavSwitch;
    private ProgressBar progressBar;

    private DatabaseHelper dbHelper;
    private Teacher currentTeacher;
    // private WebDavManager webDavManager; // WebDAV功能已移除
    private String currentPhotoPath;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_user_profile);

        // 初始化视图
        initViews();
        
        // 初始化数据库
        dbHelper = new DatabaseHelper(this);
        
        // 加载当前用户信息
        loadUserData();
        
        // 设置点击事件
        setupClickListeners();
    }

    private void initViews() {
        profileImageView = findViewById(R.id.profile_image);
        usernameTextView = findViewById(R.id.tv_username);
        emailTextView = findViewById(R.id.tv_email);
        webDavStatusTextView = findViewById(R.id.tv_webdav_status);
        changePhotoButton = findViewById(R.id.btn_change_photo);
        changeUsernameButton = findViewById(R.id.btn_change_username);
        changePasswordButton = findViewById(R.id.btn_change_password);
        webDavConfigButton = findViewById(R.id.btn_webdav_config);
        syncNowButton = findViewById(R.id.btn_sync_now);
        logoutButton = findViewById(R.id.btn_logout);
        webDavSwitch = findViewById(R.id.switch_webdav);
        progressBar = findViewById(R.id.progress_bar);
    }

    private void loadUserData() {
        // 获取教师ID从Intent
        long teacherId = getIntent().getLongExtra("teacher_id", -1);
        if (teacherId == -1) {
            Toast.makeText(this, "教师信息无效", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }
        
        // 从数据库查询教师信息
        Cursor cursor = dbHelper.getReadableDatabase().query(
            "Teacher",
            new String[]{"id", "name", "username", "password", "avatarUri", "createdAt", "updatedAt"},
            "id = ?",
            new String[]{String.valueOf(teacherId)},
            null, null, null);
            
        if (cursor != null && cursor.moveToFirst()) {
            long id = cursor.getLong(cursor.getColumnIndexOrThrow("id"));
            String name = cursor.getString(cursor.getColumnIndexOrThrow("name"));
            String username = cursor.getString(cursor.getColumnIndexOrThrow("username"));
            String password = cursor.getString(cursor.getColumnIndexOrThrow("password"));
            String avatarUri = cursor.getString(cursor.getColumnIndexOrThrow("avatarUri"));
            long createdAt = cursor.getLong(cursor.getColumnIndexOrThrow("createdAt"));
            long updatedAt = cursor.getLong(cursor.getColumnIndexOrThrow("updatedAt"));
            cursor.close();
            
            currentTeacher = new Teacher();
            currentTeacher.setId(id);
            currentTeacher.setName(name);
            currentTeacher.setUsername(username);
            currentTeacher.setPassword(password);
            currentTeacher.setAvatarUri(avatarUri);
            currentTeacher.setCreatedAt(createdAt);
            currentTeacher.setUpdatedAt(updatedAt);
            
            // 显示教师信息
            usernameTextView.setText(currentTeacher.getName());
            emailTextView.setText(currentTeacher.getUsername()); // 使用username代替email
            
            // 加载头像
            if (currentTeacher.getAvatarUri() != null && !currentTeacher.getAvatarUri().isEmpty()) {
                File avatarFile = new File(currentTeacher.getAvatarUri());
                if (avatarFile.exists()) {
                    Glide.with(this)
                        .load(avatarFile)
                        .into(profileImageView);
                }
            }
            
            // WebDAV功能已移除
            webDavSwitch.setChecked(false);
            webDavSwitch.setEnabled(false);
            
            // 更新状态显示
            updateWebDavStatus();
        } else {
            // 如果找不到教师，返回登录页面
            Toast.makeText(this, "教师信息加载失败，请重新登录", Toast.LENGTH_SHORT).show();
            navigateToLogin();
        }
    }

    private void setupClickListeners() {
        // 更换头像
        changePhotoButton.setOnClickListener(v -> showImageSourceDialog());
        
        // 修改用户名
        changeUsernameButton.setOnClickListener(v -> showChangeUsernameDialog());
        
        // 修改密码
        changePasswordButton.setEnabled(true);
        changePasswordButton.setText("修改密码");
        changePasswordButton.setOnClickListener(v -> showChangePasswordDialog());
        
        // WebDAV功能已移除
        webDavSwitch.setOnCheckedChangeListener(null);
        webDavSwitch.setEnabled(false);
        
        // WebDAV配置按钮已禁用
        webDavConfigButton.setEnabled(false);
        webDavConfigButton.setText("WebDAV功能已移除");
        
        // 同步按钮已禁用
        syncNowButton.setEnabled(false);
        syncNowButton.setText("同步功能已移除");
        
        // 退出登录
        logoutButton.setOnClickListener(v -> {
            // 退出动画（exit）执行一次后再跳转登录
            playExitAndNavigate();
        });
    }

    private void showImageSourceDialog() {
        String[] options = {"拍照", "从相册选择"};
        
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("选择图片来源");
        builder.setItems(options, (dialog, which) -> {
            if (which == 0) {
                // 拍照
                dispatchTakePictureIntent();
            } else {
                // 从相册选择
                Intent pickPhotoIntent = new Intent(Intent.ACTION_PICK, 
                        MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
                startActivityForResult(pickPhotoIntent, REQUEST_PICK_IMAGE);
            }
        });
        builder.show();
    }

    private void dispatchTakePictureIntent() {
        Intent takePictureIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        if (takePictureIntent.resolveActivity(getPackageManager()) != null) {
            // 创建保存图片的文件
            File photoFile = null;
            try {
                photoFile = createImageFile();
            } catch (IOException ex) {
                Log.e(TAG, "Error creating image file", ex);
                Toast.makeText(this, "无法创建图片文件", Toast.LENGTH_SHORT).show();
            }
            
            if (photoFile != null) {
                Uri photoURI = FileProvider.getUriForFile(this,
                        "com.example.facecheck.fileprovider",
                        photoFile);
                takePictureIntent.putExtra(MediaStore.EXTRA_OUTPUT, photoURI);
                startActivityForResult(takePictureIntent, REQUEST_IMAGE_CAPTURE);
            }
        }
    }

    private File createImageFile() throws IOException {
        // 创建图片文件名
        String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(new Date());
        String imageFileName = "JPEG_" + timeStamp + "_";
        File storageDir = PhotoStorageManager.getAvatarPhotosDir(this);
        File image = File.createTempFile(
                imageFileName,  /* 前缀 */
                ".jpg",         /* 后缀 */
                storageDir      /* 目录 */
        );

        // 保存文件路径以供后续使用
        currentPhotoPath = image.getAbsolutePath();
        return image;
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        
        if (resultCode == RESULT_OK) {
            if (requestCode == REQUEST_IMAGE_CAPTURE) {
                // 拍照返回
                if (currentPhotoPath != null) {
                    // 更新用户头像
                    updateProfileImage(currentPhotoPath);
                }
            } else if (requestCode == REQUEST_PICK_IMAGE && data != null) {
                // 从相册选择返回
                Uri selectedImage = data.getData();
                try {
                    // 创建目标文件
                    File photoFile = createImageFile();
                    currentPhotoPath = photoFile.getAbsolutePath();
                    
                    // 复制选择的图片到应用存储
                    copyUriToFile(selectedImage, photoFile);
                    
                    // 更新用户头像
                    updateProfileImage(currentPhotoPath);
                } catch (IOException e) {
                    Log.e(TAG, "Error handling picked image", e);
                    Toast.makeText(this, "处理图片时出错", Toast.LENGTH_SHORT).show();
                }
            }
        }
    }

    private void copyUriToFile(Uri sourceUri, File destFile) {
        try {
            android.content.ContentResolver contentResolver = getContentResolver();
            java.io.InputStream inputStream = contentResolver.openInputStream(sourceUri);
            java.io.OutputStream outputStream = new java.io.FileOutputStream(destFile);
            
            byte[] buffer = new byte[4096];
            int bytesRead;
            while ((bytesRead = inputStream.read(buffer)) != -1) {
                outputStream.write(buffer, 0, bytesRead);
            }
            
            inputStream.close();
            outputStream.close();
            
        } catch (Exception e) {
            Log.e(TAG, "Error copying image file", e);
            Toast.makeText(this, "复制图片失败: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }

    private void updateProfileImage(String imagePath) {
        // 更新UI
        Glide.with(this)
            .load(new File(imagePath))
            .into(profileImageView);
        
        // 更新Teacher对象的avatarUri字段
        currentTeacher.setAvatarUri(imagePath);
        
        // 保存到数据库
        boolean success = dbHelper.updateTeacher(currentTeacher);
        if (success) {
            Toast.makeText(this, "头像已更新", Toast.LENGTH_SHORT).show();
        } else {
            Toast.makeText(this, "头像更新失败", Toast.LENGTH_SHORT).show();
        }
    }

    private void showChangeUsernameDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("修改用户名");
        
        // 设置对话框布局
        View view = LayoutInflater.from(this).inflate(R.layout.dialog_input, null);
        final EditText input = view.findViewById(R.id.et_input);
        input.setHint("新用户名");
        input.setText(currentTeacher.getName());
        
        builder.setView(view);
        
        // 设置按钮
        builder.setPositiveButton("保存", (dialog, which) -> {
            String newUsername = input.getText().toString().trim();
            if (!TextUtils.isEmpty(newUsername)) {
                // 更新用户名
                currentTeacher.setName(newUsername);

                // 更新教师对象
                dbHelper.updateTeacher(currentTeacher);

                // 更新UI
                usernameTextView.setText(newUsername);
                Toast.makeText(UserProfileActivity.this, "用户名已更新", Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(UserProfileActivity.this, "用户名不能为空", Toast.LENGTH_SHORT).show();
            }
        });
        
        builder.setNegativeButton("取消", (dialog, which) -> dialog.cancel());
        
        builder.show();
    }

    private void showChangePasswordDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("修改密码");
        
        // 设置对话框布局
        View view = LayoutInflater.from(this).inflate(R.layout.dialog_change_password, null);
        final EditText currentPasswordInput = view.findViewById(R.id.et_current_password);
        final EditText newPasswordInput = view.findViewById(R.id.et_new_password);
        final EditText confirmPasswordInput = view.findViewById(R.id.et_confirm_password);
        
        builder.setView(view);
        
        // 设置按钮
        builder.setPositiveButton("保存", null); // 先设为null，后面手动处理点击事件
        
        builder.setNegativeButton("取消", (dialog, which) -> dialog.cancel());
        
        AlertDialog dialog = builder.create();
        dialog.show();
        
        // 手动处理确定按钮点击事件，防止输入验证失败时对话框自动关闭
        dialog.getButton(DialogInterface.BUTTON_POSITIVE).setOnClickListener(v -> {
            String currentPassword = currentPasswordInput.getText().toString();
            String newPassword = newPasswordInput.getText().toString();
            String confirmPassword = confirmPasswordInput.getText().toString();
            
            // 输入校验
            if (TextUtils.isEmpty(currentPassword) || TextUtils.isEmpty(newPassword) || TextUtils.isEmpty(confirmPassword)) {
                Toast.makeText(UserProfileActivity.this, "请输入完整信息", Toast.LENGTH_SHORT).show();
                return;
            }

            if (!currentPassword.equals(currentTeacher.getPassword())) {
                Toast.makeText(UserProfileActivity.this, "当前密码不正确", Toast.LENGTH_SHORT).show();
                return;
            }

            if (newPassword.length() < 6) {
                Toast.makeText(UserProfileActivity.this, "新密码至少6位", Toast.LENGTH_SHORT).show();
                return;
            }

            if (!newPassword.equals(confirmPassword)) {
                Toast.makeText(UserProfileActivity.this, "两次输入的新密码不一致", Toast.LENGTH_SHORT).show();
                return;
            }

            if (newPassword.equals(currentPassword)) {
                Toast.makeText(UserProfileActivity.this, "新密码不能与当前密码相同", Toast.LENGTH_SHORT).show();
                return;
            }

            // 更新密码
            currentTeacher.setPassword(newPassword);
            boolean success = dbHelper.updateTeacher(currentTeacher);
            if (success) {
                Toast.makeText(UserProfileActivity.this, "密码已更新", Toast.LENGTH_SHORT).show();
                dialog.dismiss();
            } else {
                Toast.makeText(UserProfileActivity.this, "密码更新失败", Toast.LENGTH_SHORT).show();
            }
        });
    }

    // WebDAV功能已移除，此方法不再需要
    private void showWebDavConfigDialog() {
        // WebDAV配置功能已禁用
        Toast.makeText(this, "WebDAV功能已移除", Toast.LENGTH_SHORT).show();
    }

    // WebDAV功能已移除，此方法不再需要
    private void initWebDavManager() {
        // WebDAV管理器初始化已禁用
    }

    private void updateWebDavStatus() {
        // WebDAV功能已移除，显示简化状态
        webDavStatusTextView.setText("同步功能: 已禁用");
        webDavStatusTextView.setBackgroundResource(R.drawable.status_background_disconnected);
        webDavConfigButton.setEnabled(false);
        syncNowButton.setEnabled(false);
    }

    // WebDAV功能已移除，此方法不再需要
    private void syncWithWebDav() {
        // WebDAV同步功能已禁用
        Toast.makeText(this, "WebDAV同步功能已移除", Toast.LENGTH_SHORT).show();
    }

    private void navigateToLogin() {
        Intent intent = new Intent(this, LoginActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }

    private void playExitAndNavigate() {
        // 动态添加覆盖层，避免修改现有布局结构（LinearLayout）
        final android.widget.FrameLayout overlay = new android.widget.FrameLayout(this);
        overlay.setLayoutParams(new android.widget.FrameLayout.LayoutParams(
                android.widget.FrameLayout.LayoutParams.MATCH_PARENT,
                android.widget.FrameLayout.LayoutParams.MATCH_PARENT));
        overlay.setClickable(true);
        // 半透明遮罩
        android.view.View dim = new android.view.View(this);
        dim.setLayoutParams(new android.widget.FrameLayout.LayoutParams(
                android.widget.FrameLayout.LayoutParams.MATCH_PARENT,
                android.widget.FrameLayout.LayoutParams.MATCH_PARENT));
        dim.setBackgroundColor(0x80000000);
        overlay.addView(dim);
        // Lottie 视图
        LottieAnimationView lottie = new LottieAnimationView(this);
        android.widget.FrameLayout.LayoutParams lp = new android.widget.FrameLayout.LayoutParams(300, 300);
        lp.gravity = android.view.Gravity.CENTER;
        lottie.setLayoutParams(lp);
        lottie.setAnimation("lottie/exit.json");
        lottie.setRepeatCount(0);
        lottie.setRepeatMode(LottieDrawable.RESTART);
        lottie.setSpeed(1.0f);
        overlay.addView(lottie);
        // 添加到界面
        addContentView(overlay, overlay.getLayoutParams());
        // 播放并在结束后跳转
        lottie.addAnimatorListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationEnd(Animator animation) {
                // 移除覆盖层
                try {
                    ((android.view.ViewGroup) overlay.getParent()).removeView(overlay);
                } catch (Throwable ignored) {}
                navigateToLogin();
            }
        });
        // 保障动画资源加载后再播放，提升稳定性
        lottie.addLottieOnCompositionLoadedListener(composition -> {
            try {
                lottie.playAnimation();
            } catch (Throwable t) {
                android.util.Log.e(TAG, "播放退出动画失败: " + t.getMessage());
                // 失败则直接跳转
                try {
                    ((android.view.ViewGroup) overlay.getParent()).removeView(overlay);
                } catch (Throwable ignored) {}
                navigateToLogin();
            }
        });
        lottie.setFailureListener((LottieListener<Throwable>) throwable -> {
            android.util.Log.e(TAG, "退出动画加载失败", throwable);
            try {
                ((android.view.ViewGroup) overlay.getParent()).removeView(overlay);
            } catch (Throwable ignored) {}
            navigateToLogin();
        });
    }
}