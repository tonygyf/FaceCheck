package com.example.facecheck.fragments;

import android.app.AlertDialog;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.text.TextUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;
import android.content.Intent;
import android.net.Uri;
import android.provider.MediaStore;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.core.content.FileProvider;

import com.bumptech.glide.Glide;
import com.example.facecheck.R;
import com.example.facecheck.database.DatabaseHelper;
import com.example.facecheck.data.model.Teacher;
import com.example.facecheck.ui.auth.LoginActivity;
import com.example.facecheck.webdav.WebDavManager;
import com.example.facecheck.sync.SyncManager;
import com.example.facecheck.ui.settings.CacheSettingsActivity;
import com.example.facecheck.utils.CacheManager;
import com.example.facecheck.utils.PhotoStorageManager;
import androidx.core.content.FileProvider;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.FileOutputStream;
import java.util.Locale;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.io.IOException;

import de.hdodenhof.circleimageview.CircleImageView;

public class ProfileFragment extends Fragment {
    
    private static final String TAG = "ProfileFragment";
    private static final int REQUEST_IMAGE_CAPTURE = 1;
    private static final int REQUEST_PICK_IMAGE = 2;
    
    private CircleImageView profileImageView;
    private TextView usernameTextView;
    private TextView emailTextView;
    private Button changePhotoButton;
    private Button changeUsernameButton;
    private Button changePasswordButton;
    private Button logoutButton;
    private ProgressBar progressBar;
    
    // WebDAV相关视图
    private android.widget.Switch webdavSwitch;
    private Button webdavConfigButton;
    private Button syncNowButton;
    private TextView webdavStatusTextView;
    
    // 缓存设置相关视图
    private Button cacheSettingsButton;
    private TextView cacheStatusTextView;

    // 主题与设置入口
    private Button themeSystemButton;
    private Button themeDarkButton;
    private Button themeLightButton;
    private View itemMoreSettings;
    private View itemAbout;
    
    private DatabaseHelper dbHelper;
    private Teacher currentTeacher;
    private WebDavManager webDavManager;
    private SyncManager syncManager;
    private String currentPhotoPath;
    
    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_profile, container, false);
        
        // 初始化视图
        initViews(view);
        
        // 初始化数据库
        dbHelper = new DatabaseHelper(getActivity());
        
        // 初始化WebDAV管理器
        initWebDavManager();
        
        // 加载当前用户信息
        loadUserData();
        
        // 更新WebDAV状态
        updateWebDavStatus();
        
        // 更新缓存状态
        updateCacheStatus();
        
        // 设置点击事件
        setupClickListeners();
        
        return view;
    }
    
    private void initViews(View view) {
        profileImageView = view.findViewById(R.id.profile_image);
        usernameTextView = view.findViewById(R.id.tv_username);
        emailTextView = view.findViewById(R.id.tv_email);
        changePhotoButton = view.findViewById(R.id.btn_change_photo);
        changeUsernameButton = view.findViewById(R.id.btn_change_username);
        changePasswordButton = view.findViewById(R.id.btn_change_password);
        logoutButton = view.findViewById(R.id.btn_logout);
        progressBar = view.findViewById(R.id.progress_bar);
        
        // 初始化WebDAV相关视图
        webdavSwitch = view.findViewById(R.id.switch_webdav);
        webdavConfigButton = view.findViewById(R.id.btn_webdav_config);
        syncNowButton = view.findViewById(R.id.btn_sync_now);
        webdavStatusTextView = view.findViewById(R.id.tv_webdav_status);
        
        // 初始化缓存设置相关视图
        cacheSettingsButton = view.findViewById(R.id.btn_cache_settings);
        cacheStatusTextView = view.findViewById(R.id.tv_cache_status);

        // 主题选择与入口视图
        themeSystemButton = view.findViewById(R.id.btn_theme_system);
        themeDarkButton = view.findViewById(R.id.btn_theme_dark);
        themeLightButton = view.findViewById(R.id.btn_theme_light);
        itemMoreSettings = view.findViewById(R.id.item_more_settings);
        itemAbout = view.findViewById(R.id.item_about);

        // 初始化主题状态
        initThemeFromPrefs();
    }
    
    private void loadUserData() {
        // 从SharedPreferences获取当前登录的教师ID
        SharedPreferences prefs = getActivity().getSharedPreferences("user_prefs", Context.MODE_PRIVATE);
        long teacherId = prefs.getLong("teacher_id", -1);
        
        if (teacherId == -1) {
            Toast.makeText(getActivity(), "教师信息无效", Toast.LENGTH_SHORT).show();
            navigateToLogin();
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
            
        } else {
            // 如果找不到教师，返回登录页面
            Toast.makeText(getActivity(), "教师信息加载失败，请重新登录", Toast.LENGTH_SHORT).show();
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
        webdavSwitch.setOnCheckedChangeListener((buttonView, isChecked) -> {
            SharedPreferences prefs = getActivity().getSharedPreferences("webdav_prefs", Context.MODE_PRIVATE);
            SharedPreferences.Editor editor = prefs.edit();
            editor.putBoolean("webdav_enabled", isChecked);
            editor.apply();
            
            updateWebDavStatus();
        });
        
        // WebDAV配置
        webdavConfigButton.setOnClickListener(v -> showWebDavConfigDialog());
        
        // 立即同步
        syncNowButton.setOnClickListener(v -> syncWithWebDav());
        
        // 缓存设置
        cacheSettingsButton.setOnClickListener(v -> {
            Intent intent = new Intent(getActivity(), CacheSettingsActivity.class);
            startActivity(intent);
        });
        
        // 退出登录
        logoutButton.setOnClickListener(v -> {
            // 清除登录状态并返回登录页面
            navigateToLogin();
        });

        // 主题切换
        themeSystemButton.setOnClickListener(v -> applyThemeMode("system"));
        themeDarkButton.setOnClickListener(v -> applyThemeMode("dark"));
        themeLightButton.setOnClickListener(v -> applyThemeMode("light"));

        // 更多设置入口
        itemMoreSettings.setOnClickListener(v -> {
            try {
                Intent intent = new Intent(getActivity(), com.example.facecheck.ui.settings.MoreSettingsActivity.class);
                startActivity(intent);
            } catch (Throwable t) {
                Toast.makeText(getActivity(), "更多设置暂不可用", Toast.LENGTH_SHORT).show();
            }
        });

        // 关于入口
        itemAbout.setOnClickListener(v -> {
            String versionName = "";
            try {
                versionName = getActivity().getPackageManager()
                        .getPackageInfo(getActivity().getPackageName(), 0).versionName;
            } catch (Throwable ignore) {}

            new AlertDialog.Builder(getActivity())
                    .setTitle("关于 FaceCheck")
                    .setMessage("版本：" + versionName + "\n\nFaceCheck 用于课堂人脸识别与考勤。")
                    .setPositiveButton("确定", null)
                    .show();
        });
    }

    private void initThemeFromPrefs() {
        SharedPreferences prefs = getActivity().getSharedPreferences("settings_prefs", Context.MODE_PRIVATE);
        String mode = prefs.getString("theme_mode", "system");
        updateThemeButtons(mode);
    }

    private void applyThemeMode(String mode) {
        SharedPreferences prefs = getActivity().getSharedPreferences("settings_prefs", Context.MODE_PRIVATE);
        prefs.edit().putString("theme_mode", mode).apply();

        switch (mode) {
            case "dark":
                AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES);
                break;
            case "light":
                AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO);
                break;
            default:
                AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM);
                break;
        }
        updateThemeButtons(mode);
    }

    private void updateThemeButtons(String mode) {
        // 选中状态更新：背景与左侧√图标
        boolean sys = "system".equals(mode);
        boolean dark = "dark".equals(mode);
        boolean light = "light".equals(mode);

        themeSystemButton.setBackgroundResource(sys ? R.drawable.bg_theme_option_selected : R.drawable.bg_theme_option_unselected);
        themeDarkButton.setBackgroundResource(dark ? R.drawable.bg_theme_option_selected : R.drawable.bg_theme_option_unselected);
        themeLightButton.setBackgroundResource(light ? R.drawable.bg_theme_option_selected : R.drawable.bg_theme_option_unselected);

        themeSystemButton.setCompoundDrawablesWithIntrinsicBounds(sys ? R.drawable.ic_check_16 : 0, 0, 0, 0);
        themeDarkButton.setCompoundDrawablesWithIntrinsicBounds(dark ? R.drawable.ic_check_16 : 0, 0, 0, 0);
        themeLightButton.setCompoundDrawablesWithIntrinsicBounds(light ? R.drawable.ic_check_16 : 0, 0, 0, 0);
    }
    
    private void showImageSourceDialog() {
        String[] options = {"拍照", "从相册选择"};
        
        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
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
        if (takePictureIntent.resolveActivity(getActivity().getPackageManager()) != null) {
            File photoFile = null;
            try {
                photoFile = createImageFile();
            } catch (IOException ex) {
                ex.printStackTrace();
            }
            if (photoFile != null) {
                Uri photoURI = FileProvider.getUriForFile(getActivity(),
                    "com.example.facecheck.fileprovider",
                    photoFile);
                takePictureIntent.putExtra(MediaStore.EXTRA_OUTPUT, photoURI);
                startActivityForResult(takePictureIntent, REQUEST_IMAGE_CAPTURE);
            }
        }
    }
    
    private File createImageFile() throws IOException {
        String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(new Date());
        String imageFileName = "JPEG_" + timeStamp + "_";
        File storageDir = getActivity().getExternalFilesDir("Pictures");
        File image = File.createTempFile(
            imageFileName,
            ".jpg",
            storageDir
        );
        currentPhotoPath = image.getAbsolutePath();
        return image;
    }
    
    private void showChangeUsernameDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        builder.setTitle("修改用户名");
        
        final EditText input = new EditText(getActivity());
        input.setHint("请输入新的用户名");
        input.setText(currentTeacher.getUsername());
        builder.setView(input);
        
        builder.setPositiveButton("确定", (dialog, which) -> {
            String newUsername = input.getText().toString().trim();
            if (TextUtils.isEmpty(newUsername)) {
                Toast.makeText(getActivity(), "用户名不能为空", Toast.LENGTH_SHORT).show();
                return;
            }
            
            // 更新数据库
            ContentValues values = new ContentValues();
            values.put("username", newUsername);
            values.put("updatedAt", System.currentTimeMillis());
            
            int rows = dbHelper.getWritableDatabase().update(
                "Teacher", 
                values, 
                "id = ?", 
                new String[]{String.valueOf(currentTeacher.getId())}
            );
            
            if (rows > 0) {
                currentTeacher.setUsername(newUsername);
                emailTextView.setText(newUsername);
                Toast.makeText(getActivity(), "用户名修改成功", Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(getActivity(), "用户名修改失败", Toast.LENGTH_SHORT).show();
            }
        });
        
        builder.setNegativeButton("取消", null);
        builder.show();
    }
    
    private void initWebDavManager() {
        SharedPreferences webdavPrefs = getActivity().getSharedPreferences("webdav_prefs", Context.MODE_PRIVATE);
        String serverUrl = webdavPrefs.getString("webdav_url", "");
        String username = webdavPrefs.getString("webdav_username", "");
        String password = webdavPrefs.getString("webdav_password", "");
        
        if (!serverUrl.isEmpty() && !username.isEmpty() && !password.isEmpty()) {
            webDavManager = new WebDavManager(getActivity(), serverUrl, username, password);
            syncManager = new SyncManager(getActivity(), dbHelper, webDavManager);
        }
    }
    
    private void updateWebDavStatus() {
        SharedPreferences webdavPrefs = getActivity().getSharedPreferences("webdav_prefs", Context.MODE_PRIVATE);
        boolean isEnabled = webdavPrefs.getBoolean("webdav_enabled", false);
        String serverUrl = webdavPrefs.getString("webdav_url", "");
        
        webdavSwitch.setChecked(isEnabled);
        
        if (isEnabled && !serverUrl.isEmpty() && webDavManager != null) {
            webdavStatusTextView.setText("WebDAV 状态: 已启用");
            webdavStatusTextView.setBackgroundResource(R.drawable.status_background_connected);
            webdavConfigButton.setEnabled(true);
            syncNowButton.setEnabled(true);
        } else if (isEnabled) {
            webdavStatusTextView.setText("WebDAV 状态: 未配置");
            webdavStatusTextView.setBackgroundResource(R.drawable.status_background_warning);
            webdavConfigButton.setEnabled(true);
            syncNowButton.setEnabled(false);
        } else {
            webdavStatusTextView.setText("WebDAV 状态: 已禁用");
            webdavStatusTextView.setBackgroundResource(R.drawable.status_background_disconnected);
            webdavConfigButton.setEnabled(false);
            syncNowButton.setEnabled(false);
        }
    }
    
    private void updateCacheStatus() {
        // 创建缓存管理器
        CacheManager cacheManager = new CacheManager(getActivity());
        
        // 在后台线程获取缓存大小
        new Thread(() -> {
            cacheManager.getCacheSize(new CacheManager.CacheSizeCallback() {
                @Override
                public void onSizeCalculated(CacheManager.CacheSizeInfo sizeInfo) {
                    getActivity().runOnUiThread(() -> {
                        String cacheInfo = String.format("缓存: %s (图片: %s)", 
                            formatFileSize(sizeInfo.totalSize),
                            formatFileSize(sizeInfo.imageCacheSize));
                        cacheStatusTextView.setText(cacheInfo);
                        cacheManager.cleanup(); // 清理资源
                    });
                }
                
                @Override
                public void onError(String error) {
                    getActivity().runOnUiThread(() -> {
                        cacheStatusTextView.setText("缓存: 获取失败");
                        cacheManager.cleanup(); // 清理资源
                    });
                }
            });
        }).start();
    }
    
    private String formatFileSize(long size) {
        if (size <= 0) return "0 B";
        
        final String[] units = new String[]{"B", "KB", "MB", "GB", "TB"};
        int digitGroups = (int) (Math.log10(size) / Math.log10(1024));
        
        return new java.text.DecimalFormat("#,##0.#").format(size / Math.pow(1024, digitGroups)) + " " + units[digitGroups];
    }
    
    private void showWebDavConfigDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        
        View dialogView = LayoutInflater.from(getActivity()).inflate(R.layout.dialog_webdav_config, null);
        builder.setView(dialogView);
        
        EditText urlInput = dialogView.findViewById(R.id.et_webdav_url);
        EditText usernameInput = dialogView.findViewById(R.id.et_webdav_username);
        EditText passwordInput = dialogView.findViewById(R.id.et_webdav_password);
        TextView statusText = dialogView.findViewById(R.id.tv_connection_status);
        Button testButton = dialogView.findViewById(R.id.btn_test_connection);
        Button cancelButton = dialogView.findViewById(R.id.btn_cancel);
        Button saveButton = dialogView.findViewById(R.id.btn_save);
        
        // 加载已保存的配置
        SharedPreferences webdavPrefs = getActivity().getSharedPreferences("webdav_prefs", Context.MODE_PRIVATE);
        urlInput.setText(webdavPrefs.getString("webdav_url", ""));
        usernameInput.setText(webdavPrefs.getString("webdav_username", ""));
        passwordInput.setText(webdavPrefs.getString("webdav_password", ""));
        
        // 测试连接按钮
        testButton.setOnClickListener(v -> {
            String url = urlInput.getText().toString().trim();
            String username = usernameInput.getText().toString().trim();
            String password = passwordInput.getText().toString().trim();
            
            if (url.isEmpty() || username.isEmpty() || password.isEmpty()) {
                statusText.setText("请填写所有字段");
                statusText.setTextColor(getResources().getColor(android.R.color.holo_red_dark));
                statusText.setVisibility(View.VISIBLE);
                return;
            }
            
            // 显示测试状态
            statusText.setText("正在测试连接...");
            statusText.setTextColor(getResources().getColor(android.R.color.black));
            statusText.setVisibility(View.VISIBLE);
            
            // 在后台线程测试连接
            new Thread(() -> {
                WebDavManager tempManager = new WebDavManager(getActivity(), url, username, password);
                boolean isConnected = tempManager.testConnection();
                
                getActivity().runOnUiThread(() -> {
                    if (isConnected) {
                        statusText.setText("连接成功！");
                        statusText.setTextColor(getResources().getColor(android.R.color.holo_green_dark));
                    } else {
                        statusText.setText("连接失败，请检查配置");
                        statusText.setTextColor(getResources().getColor(android.R.color.holo_red_dark));
                    }
                });
            }).start();
        });
        
        // 取消按钮
        cancelButton.setOnClickListener(v -> {
            // 关闭对话框
            android.app.AlertDialog dialog = (android.app.AlertDialog) builder.create();
            dialog.dismiss();
        });
        
        // 保存按钮
        saveButton.setOnClickListener(v -> {
            String url = urlInput.getText().toString().trim();
            String username = usernameInput.getText().toString().trim();
            String password = passwordInput.getText().toString().trim();
            
            if (url.isEmpty() || username.isEmpty() || password.isEmpty()) {
                Toast.makeText(getActivity(), "请填写所有字段", Toast.LENGTH_SHORT).show();
                return;
            }
            
            // 保存配置
            SharedPreferences.Editor editor = webdavPrefs.edit();
            editor.putString("webdav_url", url);
            editor.putString("webdav_username", username);
            editor.putString("webdav_password", password);
            editor.apply();
            
            // 重新初始化WebDAV管理器
            initWebDavManager();
            updateWebDavStatus();
            
            Toast.makeText(getActivity(), "WebDAV配置已保存", Toast.LENGTH_SHORT).show();
            
            // 关闭对话框
            android.app.AlertDialog dialog = (android.app.AlertDialog) builder.create();
            dialog.dismiss();
        });
        
        builder.create().show();
    }
    
    private void syncWithWebDav() {
        if (syncManager == null) {
            Toast.makeText(getActivity(), "WebDAV未配置", Toast.LENGTH_SHORT).show();
            return;
        }
        
        progressBar.setVisibility(View.VISIBLE);
        
        new Thread(() -> {
            try {
                // 执行同步
                boolean success = syncManager.performSync();
                
                getActivity().runOnUiThread(() -> {
                    progressBar.setVisibility(View.GONE);
                    if (success) {
                        Toast.makeText(getActivity(), "同步成功", Toast.LENGTH_SHORT).show();
                    } else {
                        Toast.makeText(getActivity(), "同步失败，请检查网络连接和WebDAV配置", Toast.LENGTH_SHORT).show();
                    }
                });
            } catch (Exception e) {
                getActivity().runOnUiThread(() -> {
                    progressBar.setVisibility(View.GONE);
                    Toast.makeText(getActivity(), "同步出错: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
            }
        }).start();
    }
    
    private void navigateToLogin() {
        // 清除SharedPreferences中的登录状态
        SharedPreferences prefs = getActivity().getSharedPreferences("user_prefs", Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = prefs.edit();
        editor.remove("teacher_id");
        editor.apply();
        
        // 跳转到登录页面
        Intent intent = new Intent(getActivity(), LoginActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        getActivity().finish();
    }
    
    @Override
    public void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        
        if (resultCode == getActivity().RESULT_OK) {
            if (requestCode == REQUEST_IMAGE_CAPTURE) {
                // 处理拍照结果
                if (currentPhotoPath != null) {
                    updateProfileImage(currentPhotoPath);
                }
            } else if (requestCode == REQUEST_PICK_IMAGE && data != null) {
                // 处理相册选择结果
                Uri selectedImageUri = data.getData();
                if (selectedImageUri != null) {
                    String photoPath = copyUriToFile(selectedImageUri);
                    if (photoPath != null) {
                        updateProfileImage(photoPath);
                    }
                }
            }
        }
    }
    
    private String copyUriToFile(Uri uri) {
        try {
            String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(new Date());
            String imageFileName = "JPEG_" + timeStamp + "_";
            File storageDir = getActivity().getExternalFilesDir("Pictures");
            File image = File.createTempFile(
                imageFileName,
                ".jpg",
                storageDir
            );
            
            // 使用PhotoStorageManager的saveAvatar方法保存头像
            PhotoStorageManager photoStorageManager = new PhotoStorageManager(getActivity());
            String savedPath = photoStorageManager.saveAvatar(uri, currentTeacher.getId());
            if (savedPath != null) {
                return savedPath;
            } else {
                // 如果saveAvatar失败，使用手动复制
                InputStream inputStream = getActivity().getContentResolver().openInputStream(uri);
                FileOutputStream outputStream = new FileOutputStream(image);
                
                byte[] buffer = new byte[4096];
                int length;
                while ((length = inputStream.read(buffer)) > 0) {
                    outputStream.write(buffer, 0, length);
                }
                
                inputStream.close();
                outputStream.close();
                
                return image.getAbsolutePath();
            }
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }
    }
    
    private void updateProfileImage(String photoPath) {
        // 更新UI
        Glide.with(this)
            .load(photoPath)
            .into(profileImageView);
        
        // 更新教师对象
        if (currentTeacher != null) {
            currentTeacher.setAvatarUri(photoPath);
            
            // 保存到数据库
            boolean success = dbHelper.updateTeacher(currentTeacher);
            if (success) {
                Toast.makeText(getActivity(), "头像更新成功", Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(getActivity(), "头像更新失败", Toast.LENGTH_SHORT).show();
            }
        }
    }
}