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
import android.content.DialogInterface;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.core.content.FileProvider;
import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.widget.FrameLayout;
import androidx.lifecycle.ViewModelProvider;
import android.widget.LinearLayout;

import com.airbnb.lottie.LottieAnimationView;

import com.example.facecheck.R;
import com.example.facecheck.database.DatabaseHelper;
import com.example.facecheck.data.model.Teacher;
import com.example.facecheck.data.model.Student;
import com.example.facecheck.ui.auth.LoginActivity;
import com.example.facecheck.utils.ImageLoader;
import com.example.facecheck.utils.PhotoStorageManager;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.FileOutputStream;
import java.util.Locale;
import java.text.SimpleDateFormat;
import java.util.Date;

import com.example.facecheck.ui.profile.ProfileViewModel;
import com.example.facecheck.ui.profile.ProfileViewModelFactory;

import de.hdodenhof.circleimageview.CircleImageView;

public class ProfileFragment extends Fragment {
    
    private static final String TAG = "ProfileFragment";
    private static final int REQUEST_IMAGE_CAPTURE = 1;
    private static final int REQUEST_PICK_IMAGE = 2;
    
    private CircleImageView profileImageView;
    private TextView usernameTextView;
    private TextView emailTextView;
    private View changePhotoButton;
    private Button changeUsernameButton;
    private Button changePasswordButton;
    private Button logoutButton;
    private ProgressBar progressBar;
    private FrameLayout lottieOverlayLogout;
    private LottieAnimationView lottieLogoutView;
    
    private View itemChangeUsername;
    private View itemChangePassword;
    private View itemLogout;
    
    private Button themeSystemButton;
    private Button themeDarkButton;
    private Button themeLightButton;
    private View itemMoreSettings;
    private View itemAbout;
    
    private DatabaseHelper dbHelper;
    private Teacher currentTeacher;
    private String currentPhotoPath;
    private ProfileViewModel profileViewModel;
    
    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_profile, container, false);
        
        initViews(view);
        
        if (getContext() != null) {
            dbHelper = new DatabaseHelper(getContext());
        }
        
        loadUserData();
        setupClickListeners();
        setupViewModel();
        
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
        lottieOverlayLogout = view.findViewById(R.id.lottieOverlayLogout);
        lottieLogoutView = view.findViewById(R.id.lottieLogoutView);
        
        themeSystemButton = view.findViewById(R.id.btn_theme_system);
        themeDarkButton = view.findViewById(R.id.btn_theme_dark);
        themeLightButton = view.findViewById(R.id.btn_theme_light);
        itemMoreSettings = view.findViewById(R.id.item_more_settings);
        itemAbout = view.findViewById(R.id.item_about);
        
        itemChangeUsername = view.findViewById(R.id.item_change_username);
        itemChangePassword = view.findViewById(R.id.item_change_password);
        itemLogout = view.findViewById(R.id.item_logout);

        if (themeSystemButton != null && themeDarkButton != null && themeLightButton != null) {
            initThemeFromPrefs();
        }
    }
    
    private void loadUserData() {
        if (getContext() == null) return;
        SharedPreferences prefs = requireContext().getSharedPreferences("user_prefs", Context.MODE_PRIVATE);
        String role = prefs.getString("user_role", "teacher");
        long teacherId = prefs.getLong("teacher_id", -1);
        long studentId = prefs.getLong("student_id", -1);

        if ("student".equals(role) && studentId != -1) {
            Cursor c = dbHelper.getStudentById(studentId);
            if (c != null && c.moveToFirst()) {
                String name = c.getString(c.getColumnIndexOrThrow("name"));
                String sid = c.getString(c.getColumnIndexOrThrow("sid"));
                String avatarUri = c.getString(c.getColumnIndexOrThrow("avatarUri"));
                long updatedAt = c.getLong(c.getColumnIndexOrThrow("updatedAt"));
                c.close();
                if (isAdded()) {
                    if (usernameTextView != null) usernameTextView.setText(name);
                    if (emailTextView != null) emailTextView.setText(sid);
                    if (avatarUri != null && !avatarUri.isEmpty() && profileImageView != null) {
                        ImageLoader.loadAvatar(getContext(), avatarUri, profileImageView, String.valueOf(updatedAt));
                    }
                    if (changePhotoButton != null) { changePhotoButton.setEnabled(false); }
                    if (changeUsernameButton != null) { changeUsernameButton.setVisibility(View.GONE); }
                    if (changePasswordButton != null) { changePasswordButton.setVisibility(View.GONE); }
                    if (itemChangeUsername != null) { itemChangeUsername.setVisibility(View.GONE); }
                    if (itemChangePassword != null) { itemChangePassword.setVisibility(View.GONE); }
                }
            } else if (c != null) {
                c.close();
                Toast.makeText(requireContext(), "学生信息加载失败", Toast.LENGTH_SHORT).show();
            }
            return;
        }

        if ("teacher".equals(role) && teacherId != -1) {
            currentTeacher = dbHelper.getTeacherById(teacherId);
            if (currentTeacher != null) {
                if (isAdded()) {
                    if (usernameTextView != null) usernameTextView.setText(currentTeacher.getName());
                    if (emailTextView != null) emailTextView.setText(currentTeacher.getUsername());
                    if (currentTeacher.getAvatarUri() != null && !currentTeacher.getAvatarUri().isEmpty()) {
                        ImageLoader.loadAvatar(getContext(), currentTeacher.getAvatarUri(), profileImageView, String.valueOf(currentTeacher.getUpdatedAt()));
                    }
                }
            } else {
                Toast.makeText(requireContext(), "教师信息加载失败", Toast.LENGTH_SHORT).show();
            }
            return;
        }

        Toast.makeText(requireContext(), "登录信息无效，请重新登录", Toast.LENGTH_SHORT).show();
        navigateToLogin();
    }
    
    private void setupClickListeners() {
        // 让头像本身和旁边的按钮都可以触发更换头像
        if (profileImageView != null) {
            profileImageView.setOnClickListener(v -> showImageSourceDialog());
        }
        if (changePhotoButton != null) {
            changePhotoButton.setOnClickListener(v -> showImageSourceDialog());
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
                Toast.makeText(getContext(), "更多设置开发中", Toast.LENGTH_SHORT).show());
        }
        // 退出登录
        if (itemLogout != null) {
            itemLogout.setOnClickListener(v -> showLogoutAnimation());
        }
    }
    private void initThemeFromPrefs() {
        if (getContext() == null) return;
        SharedPreferences prefs = requireContext().getSharedPreferences("settings_prefs", Context.MODE_PRIVATE);
        String mode = prefs.getString("theme_mode", "system");
        updateThemeButtons(mode);
    }

    private void applyThemeMode(String mode) {
        if (getContext() == null) return;
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
        themeSystemButton.setSelected("system".equals(mode));
        themeDarkButton.setSelected("dark".equals(mode));
        themeLightButton.setSelected("light".equals(mode));
    }
    private void showImageSourceDialog() {
        new AlertDialog.Builder(requireContext())
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
        if (getActivity() != null && intent.resolveActivity(getActivity().getPackageManager()) != null) {
            try {
                File photoFile = createImageFile();
                Uri photoURI = FileProvider.getUriForFile(requireContext(),
                        "com.example.facecheck.fileprovider", photoFile);
                intent.putExtra(MediaStore.EXTRA_OUTPUT, photoURI);
                startActivityForResult(intent, REQUEST_IMAGE_CAPTURE);
            } catch (IOException ex) {
                Toast.makeText(getContext(), "无法创建图片文件", Toast.LENGTH_SHORT).show();
            }
        }
    }

    private File createImageFile() throws IOException {
        String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(new Date());
        File storageDir = PhotoStorageManager.getAvatarPhotosDir(requireContext());
        File image = File.createTempFile("JPEG_" + timeStamp + "_", ".jpg", storageDir);
        currentPhotoPath = image.getAbsolutePath();
        return image;
    }

    private void showChangeUsernameDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(requireContext());
        builder.setTitle("修改用户名");

        LinearLayout layout = new LinearLayout(requireContext());
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setPadding(50, 20, 50, 20);

        final EditText usernameInput = new EditText(requireContext());
        usernameInput.setHint("新的用户名");
        if (currentTeacher != null) {
            usernameInput.setText(currentTeacher.getName());
        }
        layout.addView(usernameInput);

        builder.setView(layout);

        builder.setPositiveButton("确认修改", (dialog, which) -> {
            String newName = usernameInput.getText().toString().trim();
            if (newName.isEmpty()) {
                Toast.makeText(getContext(), "用户名不能为空", Toast.LENGTH_SHORT).show();
                return;
            }
            profileViewModel.changeUsername(newName);
        });
        builder.setNegativeButton("取消", (dialog, which) -> dialog.cancel());

        builder.show();
    }

    private void setupViewModel() {
        profileViewModel = new ViewModelProvider(this, new ProfileViewModelFactory(requireActivity().getApplication()))
                .get(ProfileViewModel.class);

        profileViewModel.passwordChangeSuccess.observe(getViewLifecycleOwner(), success -> {
            if (success != null) {
                if (success) {
                    Toast.makeText(getContext(), "密码修改成功！", Toast.LENGTH_SHORT).show();
                } 
                profileViewModel.clearPasswordChangeStatus(); // Reset status
            }
        });

        profileViewModel.errorMessage.observe(getViewLifecycleOwner(), error -> {
            if (error != null && !error.isEmpty()) {
                Toast.makeText(getContext(), "操作失败: " + error, Toast.LENGTH_LONG).show();
                profileViewModel.clearErrorMessage();
            }
        });

        profileViewModel.usernameChangeSuccess.observe(getViewLifecycleOwner(), success -> {
            if (success != null) {
                if (success) {
                    Toast.makeText(getContext(), "用户名修改成功！", Toast.LENGTH_SHORT).show();
                    loadUserData(); // 重新加载以显示新名称
                } else {
                    // 错误消息已由 errorMessage LiveData 处理
                }
            }
        });

        profileViewModel.avatarUploadSuccess.observe(getViewLifecycleOwner(), success -> {
            if (success != null) {
                progressBar.setVisibility(View.GONE);
                if (success) {
                    Toast.makeText(getContext(), "头像上传成功！", Toast.LENGTH_SHORT).show();
                    // Reload user data to show the new avatar
                    loadUserData();
                } else {
                    Toast.makeText(getContext(), "头像上传失败", Toast.LENGTH_SHORT).show();
                }
            }
        });
    }

    private void showChangePasswordDialog() { 
        AlertDialog.Builder builder = new AlertDialog.Builder(requireContext());
        builder.setTitle("修改密码");

        LinearLayout layout = new LinearLayout(requireContext());
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setPadding(50, 20, 50, 20);

        final EditText oldPasswordInput = new EditText(requireContext());
        oldPasswordInput.setHint("旧密码");
        oldPasswordInput.setInputType(android.text.InputType.TYPE_CLASS_TEXT | android.text.InputType.TYPE_TEXT_VARIATION_PASSWORD);
        layout.addView(oldPasswordInput);

        final EditText newPasswordInput = new EditText(requireContext());
        newPasswordInput.setHint("新密码");
        newPasswordInput.setInputType(android.text.InputType.TYPE_CLASS_TEXT | android.text.InputType.TYPE_TEXT_VARIATION_PASSWORD);
        layout.addView(newPasswordInput);

        builder.setView(layout);

        builder.setPositiveButton("确认修改", (dialog, which) -> {
            String oldPassword = oldPasswordInput.getText().toString();
            String newPassword = newPasswordInput.getText().toString();
            if (oldPassword.isEmpty() || newPassword.isEmpty()) {
                Toast.makeText(getContext(), "密码不能为空", Toast.LENGTH_SHORT).show();
                return;
            }
            profileViewModel.changePassword(oldPassword, newPassword);
        });
        builder.setNegativeButton("取消", (dialog, which) -> dialog.cancel());

        builder.show();
    }
    private void navigateToLogin() {
        if (getActivity() != null) {
            Intent intent = new Intent(getActivity(), LoginActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(intent);
            getActivity().finish();
        }
    }
    
    @Override
    public void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (resultCode != Activity.RESULT_OK) return;

        if (requestCode == REQUEST_IMAGE_CAPTURE) {
            if (currentPhotoPath != null) {
                updateProfileImage(currentPhotoPath);
            }
        } else if (requestCode == REQUEST_PICK_IMAGE && data != null && data.getData() != null) {
            Uri imageUri = data.getData();
            String filePath = copyUriToFile(imageUri);
            if (filePath != null) {
                updateProfileImage(filePath);
            }
        }
    }
    
    private String copyUriToFile(Uri uri) {
        if (getContext() == null) return null;
        try (InputStream inputStream = getContext().getContentResolver().openInputStream(uri)) {
            File file = createImageFile();
            try (FileOutputStream outputStream = new FileOutputStream(file)) {
                byte[] buffer = new byte[1024];
                int len;
                while ((len = inputStream.read(buffer)) > 0) {
                    outputStream.write(buffer, 0, len);
                }
                return file.getAbsolutePath();
            }
        } catch (IOException e) {
            Log.e(TAG, "Failed to copy URI to file", e);
            Toast.makeText(getContext(), "无法复制图片文件", Toast.LENGTH_SHORT).show();
            return null;
        }
    }
    
    private void updateProfileImage(String photoPath) {
        progressBar.setVisibility(View.VISIBLE);
        profileViewModel.uploadAvatar(photoPath);
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
    }

    private void showLogoutAnimation() {
        if (lottieOverlayLogout == null || lottieLogoutView == null) return;

        lottieOverlayLogout.setVisibility(View.VISIBLE);
        lottieLogoutView.setSpeed(0.8f); // 调整动画速度
        lottieLogoutView.playAnimation();

        lottieLogoutView.addAnimatorListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationEnd(Animator animation) {
                if (getContext() == null) return;
                // Clear user session
                SharedPreferences prefs = requireContext().getSharedPreferences("user_prefs", Context.MODE_PRIVATE);
                prefs.edit().clear().apply();

                // Navigate to login
                navigateToLogin();
            }
        });
    }
}
