package com.example.facecheck.fragments;

import android.app.Activity;
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
    
    // WebDAV与缓存相关入口已收敛到设置页，个人页不再持有视图引用

    // 主题与设置入口
    private Button themeSystemButton;
    private Button themeDarkButton;
    private Button themeLightButton;
    private View itemMoreSettings;
    private View itemAbout;
    
    private DatabaseHelper dbHelper;
    private Teacher currentTeacher;
    private String currentPhotoPath;
    
    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_profile, container, false);
        
        // 初始化视图
        initViews(view);
        
        dbHelper = new DatabaseHelper(requireContext());
        
        // 加载当前用户信息
        loadUserData();
        
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
        
        // 个人页不再初始化 WebDAV/缓存相关视图

        // 主题选择与入口视图
        themeSystemButton = view.findViewById(R.id.btn_theme_system);
        themeDarkButton = view.findViewById(R.id.btn_theme_dark);
        themeLightButton = view.findViewById(R.id.btn_theme_light);
        itemMoreSettings = view.findViewById(R.id.item_more_settings);
        itemAbout = view.findViewById(R.id.item_about);

        // 初始化主题状态（当布局存在主题按钮时）
        if (themeSystemButton != null && themeDarkButton != null && themeLightButton != null) {
            initThemeFromPrefs();
        }
    }
    
    private void loadUserData() {
        // 从SharedPreferences获取当前登录的教师ID
        SharedPreferences prefs = requireContext().getSharedPreferences("user_prefs", Context.MODE_PRIVATE);
        long teacherId = prefs.getLong("teacher_id", -1);
        
        if (teacherId == -1) {
            Toast.makeText(requireContext(), "教师信息无效", Toast.LENGTH_SHORT).show();
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
            if (isAdded()) {
                if (usernameTextView != null) usernameTextView.setText(currentTeacher.getName());
                if (emailTextView != null) emailTextView.setText(currentTeacher.getUsername());
            }
            
            // 加载头像
            if (currentTeacher.getAvatarUri() != null && !currentTeacher.getAvatarUri().isEmpty()) {
                File avatarFile = new File(currentTeacher.getAvatarUri());
                if (avatarFile.exists()) {
                    if (isAdded() && profileImageView != null) {
                        Glide.with(this)
                            .load(avatarFile)
                            .into(profileImageView);
                    }
                }
            }
            
        } else {
            // 如果找不到教师，返回登录页面
            Toast.makeText(requireContext(), "教师信息加载失败，请重新登录", Toast.LENGTH_SHORT).show();
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
        
        // 个人页不再处理 WebDAV/缓存入口，统一在设置页管理
        
        // 退出登录
        logoutButton.setOnClickListener(v -> {
            // 清除登录状态并返回登录页面
            navigateToLogin();
        });

        // 主题切换（当布局存在主题按钮时）
        if (themeSystemButton != null) themeSystemButton.setOnClickListener(v -> applyThemeMode("system"));
        if (themeDarkButton != null) themeDarkButton.setOnClickListener(v -> applyThemeMode("dark"));
        if (themeLightButton != null) themeLightButton.setOnClickListener(v -> applyThemeMode("light"));

        // 更多设置入口
        itemMoreSettings.setOnClickListener(v -> {
            try {
                Intent intent = new Intent(requireContext(), com.example.facecheck.ui.settings.MoreSettingsActivity.class);
                startActivity(intent);
            } catch (Throwable t) {
                Toast.makeText(requireContext(), "更多设置暂不可用", Toast.LENGTH_SHORT).show();
            }
        });

        // 关于入口
        itemAbout.setOnClickListener(v -> {
            String versionName = "";
            try {
                versionName = requireContext().getPackageManager()
                        .getPackageInfo(requireContext().getPackageName(), 0).versionName;
            } catch (Throwable ignore) {}

            new AlertDialog.Builder(requireContext())
                    .setTitle("关于 FaceCheck")
                    .setMessage("版本：" + versionName + "\n\nFaceCheck 用于课堂人脸识别与考勤。")
                    .setPositiveButton("确定", null)
                    .show();
        });
    }

    private void initThemeFromPrefs() {
        SharedPreferences prefs = requireContext().getSharedPreferences("settings_prefs", Context.MODE_PRIVATE);
        String mode = prefs.getString("theme_mode", "system");
        updateThemeButtons(mode);
    }

    private void applyThemeMode(String mode) {
        SharedPreferences prefs = requireContext().getSharedPreferences("settings_prefs", Context.MODE_PRIVATE);
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
        // 选中状态更新：背景与左侧√图标（当布局存在主题按钮时）
        if (themeSystemButton == null || themeDarkButton == null || themeLightButton == null) return;

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
        AlertDialog.Builder builder = new AlertDialog.Builder(requireContext());
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
        if (takePictureIntent.resolveActivity(requireContext().getPackageManager()) != null) {
            File photoFile = null;
            try {
                photoFile = createImageFile();
            } catch (IOException ex) {
                ex.printStackTrace();
            }
            if (photoFile != null) {
                Uri photoURI = FileProvider.getUriForFile(requireContext(),
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
        File storageDir = requireContext().getExternalFilesDir("Pictures");
        File image = File.createTempFile(
            imageFileName,
            ".jpg",
            storageDir
        );
        currentPhotoPath = image.getAbsolutePath();
        return image;
    }
    
    private void showChangeUsernameDialog() {
        if (currentTeacher == null) {
            Toast.makeText(requireContext(), "教师信息未加载", Toast.LENGTH_SHORT).show();
            return;
        }
        AlertDialog.Builder builder = new AlertDialog.Builder(requireContext());
        builder.setTitle("修改用户名");
        
        final EditText input = new EditText(requireContext());
        input.setHint("请输入新的用户名");
        input.setText(currentTeacher.getUsername());
        builder.setView(input);
        
        builder.setPositiveButton("确定", (dialog, which) -> {
            String newUsername = input.getText().toString().trim();
            if (TextUtils.isEmpty(newUsername)) {
                Toast.makeText(requireContext(), "用户名不能为空", Toast.LENGTH_SHORT).show();
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
                if (emailTextView != null) emailTextView.setText(newUsername);
                Toast.makeText(requireContext(), "用户名修改成功", Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(requireContext(), "用户名修改失败", Toast.LENGTH_SHORT).show();
            }
        });
        
        builder.setNegativeButton("取消", null);
        builder.show();
    }
    
    
    
    
    
    
    
    private void navigateToLogin() {
        // 清除SharedPreferences中的登录状态
        SharedPreferences prefs = requireContext().getSharedPreferences("user_prefs", Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = prefs.edit();
        editor.remove("teacher_id");
        editor.apply();
        
        // 跳转到登录页面
        Intent intent = new Intent(requireContext(), LoginActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        if (getActivity() != null) getActivity().finish();
    }
    
    @Override
    public void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        
        if (resultCode == Activity.RESULT_OK) {
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
            File storageDir = requireContext().getExternalFilesDir("Pictures");
            File image = File.createTempFile(
                imageFileName,
                ".jpg",
                storageDir
            );
            
            // 使用PhotoStorageManager的saveAvatar方法保存头像
            if (currentTeacher == null) return null;
            PhotoStorageManager photoStorageManager = new PhotoStorageManager(requireContext());
            String savedPath = photoStorageManager.saveAvatar(uri, currentTeacher.getId());
            if (savedPath != null) {
                return savedPath;
            } else {
                // 如果saveAvatar失败，使用手动复制
                try (InputStream inputStream = requireContext().getContentResolver().openInputStream(uri);
                     FileOutputStream outputStream = new FileOutputStream(image)) {
                    if (inputStream == null) return null;
                    byte[] buffer = new byte[4096];
                    int length;
                    while ((length = inputStream.read(buffer)) > 0) {
                        outputStream.write(buffer, 0, length);
                    }
                    return image.getAbsolutePath();
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }
    }
    
    private void updateProfileImage(String photoPath) {
        if (!isAdded() || profileImageView == null) return;
        Glide.with(this)
            .load(photoPath)
            .into(profileImageView);
        
        // 更新教师对象
        if (currentTeacher != null) {
            currentTeacher.setAvatarUri(photoPath);
            
            // 保存到数据库
            boolean success = dbHelper.updateTeacher(currentTeacher);
            if (success) {
                Toast.makeText(requireContext(), "头像更新成功", Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(requireContext(), "头像更新失败", Toast.LENGTH_SHORT).show();
            }
        }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        profileImageView = null;
        usernameTextView = null;
        emailTextView = null;
        changePhotoButton = null;
        changeUsernameButton = null;
        changePasswordButton = null;
        logoutButton = null;
        progressBar = null;
        themeSystemButton = null;
        themeDarkButton = null;
        themeLightButton = null;
        itemMoreSettings = null;
        itemAbout = null;
    }
}
