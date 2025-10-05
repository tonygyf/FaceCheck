package com.example.facecheck.activities;

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
import com.example.facecheck.models.Teacher;
import com.example.facecheck.webdav.WebDavManager;

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
    private WebDavManager webDavManager;
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
            new String[]{"id", "name", "email", "davUrl", "davUser", "davKeyEnc"},
            "id = ?",
            new String[]{String.valueOf(teacherId)},
            null, null, null);
            
        if (cursor != null && cursor.moveToFirst()) {
            currentTeacher = new Teacher();
            currentTeacher.setId(cursor.getLong(cursor.getColumnIndexOrThrow("id")));
            currentTeacher.setName(cursor.getString(cursor.getColumnIndexOrThrow("name")));
            currentTeacher.setEmail(cursor.getString(cursor.getColumnIndexOrThrow("email")));
            currentTeacher.setDavUrl(cursor.getString(cursor.getColumnIndexOrThrow("davUrl")));
            currentTeacher.setDavUser(cursor.getString(cursor.getColumnIndexOrThrow("davUser")));
            currentTeacher.setDavKeyEnc(cursor.getString(cursor.getColumnIndexOrThrow("davKeyEnc")));
            cursor.close();
            
            // 显示教师信息
            usernameTextView.setText(currentTeacher.getName());
            emailTextView.setText(currentTeacher.getEmail());
            
            // 设置WebDAV开关状态（基于davUrl是否为空）
            webDavSwitch.setChecked(currentTeacher.getDavUrl() != null && !currentTeacher.getDavUrl().isEmpty());
            
            // 更新WebDAV状态显示
            updateWebDavStatus();
            
            // 如果WebDAV已启用，初始化WebDAV管理器
            if (currentTeacher.getDavUrl() != null && !currentTeacher.getDavUrl().isEmpty()) {
                initWebDavManager();
            }
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
        
        // 修改密码（Teacher模型没有密码字段，暂时禁用此功能）
        changePasswordButton.setEnabled(false);
        changePasswordButton.setText("密码管理不可用");
        
        // WebDAV开关
        webDavSwitch.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (isChecked && (currentTeacher.getDavUrl() == null || currentTeacher.getDavUrl().isEmpty())) {
                // 如果开启WebDAV，显示配置对话框
                showWebDavConfigDialog();
            } else if (!isChecked && currentTeacher.getDavUrl() != null && !currentTeacher.getDavUrl().isEmpty()) {
                // 如果关闭WebDAV，更新教师设置
                currentTeacher.setDavUrl(null);
                currentTeacher.setDavUser(null);
                currentTeacher.setDavKeyEnc(null);
                
                ContentValues values = new ContentValues();
                values.putNull("davUrl");
                values.putNull("davUser");
                values.putNull("davKeyEnc");
                dbHelper.updateTeacher(currentTeacher.getId(), values);
                
                updateWebDavStatus();
            }
        });
        
        // WebDAV配置
        webDavConfigButton.setOnClickListener(v -> showWebDavConfigDialog());
        
        // 立即同步
        syncNowButton.setOnClickListener(v -> {
            if (currentTeacher.getDavUrl() != null && !currentTeacher.getDavUrl().isEmpty()) {
                syncWithWebDav();
            } else {
                Toast.makeText(this, "请先启用WebDAV同步", Toast.LENGTH_SHORT).show();
            }
        });
        
        // 退出登录
        logoutButton.setOnClickListener(v -> {
            // 清除登录状态并返回登录页面
            navigateToLogin();
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
        File storageDir = getExternalFilesDir(Environment.DIRECTORY_PICTURES);
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
                    Bitmap bitmap = MediaStore.Images.Media.getBitmap(getContentResolver(), selectedImage);
                    // 保存位图到文件
                    File photoFile = createImageFile();
                    // 这里需要实现将位图保存到文件的代码
                    // ...
                    
                    // 更新用户头像
                    updateProfileImage(currentPhotoPath);
                } catch (IOException e) {
                    Log.e(TAG, "Error handling picked image", e);
                    Toast.makeText(this, "处理图片时出错", Toast.LENGTH_SHORT).show();
                }
            }
        }
    }

    private void updateProfileImage(String imagePath) {
        // 更新UI
        Glide.with(this)
            .load(new File(imagePath))
            .into(profileImageView);
        
        // Teacher模型没有头像字段，这里仅显示新头像
        // 如果需要头像功能，需要在Teacher模型中添加头像字段，或创建单独的头像管理功能
        // 当前仅显示新头像，不保存到数据库
        
        // 同步到WebDAV（如果启用）
        // Teacher模型没有头像字段，不同步头像文件
        // if (currentTeacher.getDavUrl() != null && !currentTeacher.getDavUrl().isEmpty()) {
        //     // 同步头像文件到WebDAV
        //     syncProfileImageToWebDav();
        // }
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

                ContentValues values = new ContentValues();
                values.put("name", newUsername);
                dbHelper.updateTeacher(currentTeacher.getId(), values);

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
            
            // Teacher模型没有密码字段，此功能已禁用
            Toast.makeText(UserProfileActivity.this, "密码管理功能暂不可用", Toast.LENGTH_SHORT).show();
            dialog.dismiss();
        });
    }

    private void showWebDavConfigDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        
        // 设置对话框布局
        View view = LayoutInflater.from(this).inflate(R.layout.dialog_webdav_config, null);
        final EditText urlInput = view.findViewById(R.id.et_webdav_url);
        final EditText usernameInput = view.findViewById(R.id.et_webdav_username);
        final EditText passwordInput = view.findViewById(R.id.et_webdav_password);
        final Button testButton = view.findViewById(R.id.btn_test_connection);
        final Button cancelButton = view.findViewById(R.id.btn_cancel);
        final Button saveButton = view.findViewById(R.id.btn_save);
        final TextView statusTextView = view.findViewById(R.id.tv_connection_status);
        
        // 如果已有WebDAV配置，填充现有数据
        if (currentTeacher.getDavUrl() != null && !currentTeacher.getDavUrl().isEmpty()) {
            urlInput.setText(currentTeacher.getDavUrl());
            usernameInput.setText(currentTeacher.getDavUser());
            // 密码字段保持为空，出于安全考虑不显示加密后的密码
        }
        
        builder.setView(view);
        
        AlertDialog dialog = builder.create();
        
        // 测试连接按钮
        testButton.setOnClickListener(v -> {
            String url = urlInput.getText().toString().trim();
            String username = usernameInput.getText().toString().trim();
            String password = passwordInput.getText().toString().trim();
            
            if (TextUtils.isEmpty(url) || TextUtils.isEmpty(username) || TextUtils.isEmpty(password)) {
                Toast.makeText(UserProfileActivity.this, "请填写完整的WebDAV信息", Toast.LENGTH_SHORT).show();
                return;
            }
            
            // 显示进度条
            progressBar.setVisibility(View.VISIBLE);
            statusTextView.setVisibility(View.VISIBLE);
            statusTextView.setText("正在测试连接...");
            
            // 创建临时WebDAV管理器进行测试
            WebDavManager tempManager = new WebDavManager(this, url, username, password);
            
            // 在后台线程中测试连接
            new Thread(() -> {
                final boolean connected = tempManager.testConnection();
                
                // 在UI线程中更新结果
                runOnUiThread(() -> {
                    progressBar.setVisibility(View.GONE);
                    
                    if (connected) {
                        statusTextView.setText("连接成功！");
                        statusTextView.setTextColor(getResources().getColor(android.R.color.holo_green_dark));
                    } else {
                        statusTextView.setText("连接失败，请检查配置");
                        statusTextView.setTextColor(getResources().getColor(android.R.color.holo_red_dark));
                    }
                });
            }).start();
        });
        
        // 取消按钮
        cancelButton.setOnClickListener(v -> {
            dialog.dismiss();
            // 如果是首次启用WebDAV但取消了配置，将开关切回关闭状态
            if (currentTeacher.getDavUrl() == null || currentTeacher.getDavUrl().isEmpty()) {
                webDavSwitch.setChecked(false);
            }
        });
        
        // 保存按钮
        saveButton.setOnClickListener(v -> {
            String url = urlInput.getText().toString().trim();
            String username = usernameInput.getText().toString().trim();
            String password = passwordInput.getText().toString().trim();
            
            if (TextUtils.isEmpty(url) || TextUtils.isEmpty(username) || TextUtils.isEmpty(password)) {
                Toast.makeText(UserProfileActivity.this, "请填写完整的WebDAV信息", Toast.LENGTH_SHORT).show();
                return;
            }
            
            // 更新教师WebDAV配置
            currentTeacher.setDavUrl(url);
            currentTeacher.setDavUser(username);
            currentTeacher.setDavKeyEnc(password); // 实际应用中应该加密存储
            
            // 保存到数据库
            ContentValues values = new ContentValues();
            values.put("davUrl", url);
            values.put("davUser", username);
            values.put("davKeyEnc", password); // 实际应用中应该加密存储
            dbHelper.updateTeacher(currentTeacher.getId(), values);
            
            // 初始化WebDAV管理器
            initWebDavManager();
            
            // 更新UI
            webDavSwitch.setChecked(true);
            updateWebDavStatus();
            
            dialog.dismiss();
            
            // 询问是否立即同步
            new AlertDialog.Builder(UserProfileActivity.this)
                .setTitle("WebDAV配置已保存")
                .setMessage("是否立即初始化WebDAV文件夹结构并同步数据？")
                .setPositiveButton("是", (d, w) -> syncWithWebDav())
                .setNegativeButton("稍后", null)
                .show();
        });
        
        dialog.show();
    }

    private void initWebDavManager() {
        if (currentTeacher.getDavUrl() != null && !currentTeacher.getDavUrl().isEmpty() &&
            currentTeacher.getDavUser() != null && !currentTeacher.getDavUser().isEmpty() &&
            currentTeacher.getDavKeyEnc() != null && !currentTeacher.getDavKeyEnc().isEmpty()) {
            webDavManager = new WebDavManager(
                this,
                currentTeacher.getDavUrl(),
                currentTeacher.getDavUser(),
                currentTeacher.getDavKeyEnc()
            );
        }
    }

    private void updateWebDavStatus() {
        if (currentTeacher.getDavUrl() != null && !currentTeacher.getDavUrl().isEmpty()) {
            webDavStatusTextView.setText("WebDAV 状态: 已配置");
            webDavStatusTextView.setBackgroundResource(R.drawable.status_background_connected);
            webDavConfigButton.setEnabled(true);
            syncNowButton.setEnabled(true);
        } else {
            webDavStatusTextView.setText("WebDAV 状态: 未连接");
            webDavStatusTextView.setBackgroundResource(R.drawable.status_background_disconnected);
            webDavConfigButton.setEnabled(false);
            syncNowButton.setEnabled(false);
        }
    }

    private void syncWithWebDav() {
        if (webDavManager == null || currentTeacher.getDavUrl() == null || currentTeacher.getDavUrl().isEmpty()) {
            Toast.makeText(this, "WebDAV未配置", Toast.LENGTH_SHORT).show();
            return;
        }
        
        // 显示进度条
        progressBar.setVisibility(View.VISIBLE);
        
        // 在后台线程中执行同步
        new Thread(() -> {
            boolean success = false;
            
            // 测试连接
            if (webDavManager.testConnection()) {
                // 初始化文件夹结构
                if (webDavManager.initializeDirectoryStructure()) {
                    // 同步数据库文件
                    // 这里需要实现数据库同步逻辑
                    // ...
                    
                    success = true;
                }
            }
            
            // 在UI线程中更新结果
            final boolean finalSuccess = success;
            runOnUiThread(() -> {
                progressBar.setVisibility(View.GONE);
                
                if (finalSuccess) {
                    Toast.makeText(UserProfileActivity.this, "同步成功", Toast.LENGTH_SHORT).show();
                    updateWebDavStatus();
                } else {
                    Toast.makeText(UserProfileActivity.this, "同步失败，请检查网络和WebDAV配置", Toast.LENGTH_SHORT).show();
                }
            });
        }).start();
    }

    private void navigateToLogin() {
        Intent intent = new Intent(this, LoginActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }
}