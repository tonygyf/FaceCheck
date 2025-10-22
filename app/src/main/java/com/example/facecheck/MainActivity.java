package com.example.facecheck;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import com.google.android.material.appbar.MaterialToolbar;
import androidx.fragment.app.Fragment;
import androidx.annotation.NonNull;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.example.facecheck.R;
import com.example.facecheck.fragments.HomeFragment;
import com.example.facecheck.fragments.ClassroomFragment;
import com.example.facecheck.fragments.AttendanceFragment;
import com.example.facecheck.fragments.ProfileFragment;

import com.example.facecheck.fragments.AttendanceFragment;
import com.example.facecheck.fragments.ClassroomFragment;
import com.example.facecheck.fragments.HomeFragment;
import com.example.facecheck.fragments.ProfileFragment;
import com.example.facecheck.fragments.StudentFragment;
import com.example.facecheck.ui.auth.LoginActivity;
import com.example.facecheck.utils.WebDAVSyncHelper;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.bottomappbar.BottomAppBar;
import androidx.appcompat.content.res.AppCompatResources;
import androidx.core.graphics.drawable.DrawableCompat;
import android.graphics.drawable.Drawable;
import android.widget.ImageButton;
import androidx.core.widget.ImageViewCompat;
import android.content.res.ColorStateList;
import androidx.core.content.ContextCompat;
import android.graphics.Color;
import android.view.View;
import androidx.core.view.WindowCompat;
import androidx.core.view.WindowInsetsControllerCompat;

public class MainActivity extends AppCompatActivity {

    private static final String TAG = "MainActivity";
    private BottomNavigationView bottomNavigationView;
    private BottomAppBar bottomAppBar;
    private long teacherId = -1;
    private ImageButton btnNavHome, btnNavClassroom, btnNavAttendance, btnNavProfile;

    public long getTeacherId() {
        return teacherId;
    }

    public BottomNavigationView getBottomNavigationView() {
        return bottomNavigationView;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // 状态栏透明 + 浅色图标
        // 状态栏透明 + 浅色图标（已改为浅灰配置，移除旧逻辑）
        // WindowInsetsControllerCompat controller = WindowCompat.getInsetsController(getWindow(), getWindow().getDecorView());
        // getWindow().setStatusBarColor(Color.TRANSPARENT);
        // if (controller != null) {
        //     controller.setAppearanceLightStatusBars(false);
        // }

        // 状态栏浅灰 + 深色图标
        WindowInsetsControllerCompat controller = WindowCompat.getInsetsController(getWindow(), getWindow().getDecorView());
        getWindow().setStatusBarColor(ContextCompat.getColor(this, R.color.status_bar_bg));
        if (controller != null) {
            // 使用深色图标，适配浅色/浅灰背景
            controller.setAppearanceLightStatusBars(true);
        }

        // 设置工具栏（透明背景、居中标题）
        MaterialToolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setTitle("人脸考勤系统");
        }

        // 获取教师ID
        teacherId = getSharedPreferences("user_prefs", MODE_PRIVATE).getLong("teacher_id", -1);
        if (teacherId == -1) {
            Intent intent = new Intent(this, LoginActivity.class);
            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
            startActivity(intent);
            finish();
            return;
        }
        Log.d(TAG, "当前登录教师ID: " + teacherId);

        bottomNavigationView = findViewById(R.id.bottom_navigation);
        if (bottomNavigationView != null) {
            bottomNavigationView.setOnItemSelectedListener(navListener);
        }

        bottomAppBar = findViewById(R.id.bottom_app_bar);
        // 自定义左右按钮，绑定点击事件
        btnNavHome = findViewById(R.id.btn_nav_home);
        btnNavClassroom = findViewById(R.id.btn_nav_classroom);
        btnNavAttendance = findViewById(R.id.btn_nav_attendance);
        btnNavProfile = findViewById(R.id.btn_nav_profile);

        if (btnNavHome != null) btnNavHome.setOnClickListener(v -> handleNavigation(R.id.nav_home));
        if (btnNavClassroom != null) btnNavClassroom.setOnClickListener(v -> handleNavigation(R.id.nav_classroom));
        if (btnNavAttendance != null) btnNavAttendance.setOnClickListener(v -> handleNavigation(R.id.nav_attendance));
        if (btnNavProfile != null) btnNavProfile.setOnClickListener(v -> handleNavigation(R.id.nav_profile));

        // 初始高亮首页并设置图标颜色状态
        updateBottomBarButtonsState(R.id.nav_home);

        // 中间拍照打卡按钮
        FloatingActionButton fabCameraPunch = findViewById(R.id.fabCameraPunch);
        if (fabCameraPunch != null) {
            fabCameraPunch.setOnClickListener(v -> {
                Toast.makeText(MainActivity.this, "拍照打卡（后续接入）", Toast.LENGTH_SHORT).show();
            });
        }

        // 默认显示首页
        if (savedInstanceState == null) {
            getSupportFragmentManager().beginTransaction()
                    .replace(R.id.fragment_container, new HomeFragment())
                    .commit();
        }

        // 初始化WebDAV同步
        initWebDAVSync();
    }

    private void initWebDAVSync() {
        try {
            WebDAVSyncHelper syncHelper = new WebDAVSyncHelper(this);
            if (syncHelper.isEnabled()) {
                Toast.makeText(this, "开始同步数据库...", Toast.LENGTH_SHORT).show();
                syncHelper.setOnSyncListener(new WebDAVSyncHelper.OnSyncListener() {
                    @Override
                    public void onSyncStarted() {}
                    @Override
                    public void onSyncCompleted(boolean success, String message) {
                        Toast.makeText(MainActivity.this, message, Toast.LENGTH_SHORT).show();
                    }
                });
                syncHelper.syncDatabase();
            }
        } catch (Exception e) {
            Toast.makeText(this, "同步初始化失败", Toast.LENGTH_SHORT).show();
        }
    }

    private BottomNavigationView.OnItemSelectedListener navListener =
            new BottomNavigationView.OnItemSelectedListener() {
                @Override
                public boolean onNavigationItemSelected(@NonNull MenuItem item) {
                    handleNavigation(item.getItemId());
                    return true;
                }
            };

    private void handleNavigation(int itemId) {
        Fragment selectedFragment = null;
        if (itemId == R.id.nav_home) {
            selectedFragment = new HomeFragment();
            if (getSupportActionBar() != null) getSupportActionBar().setTitle("首页");
            Log.d(TAG, "切换到首页");
        } else if (itemId == R.id.nav_classroom) {
            selectedFragment = new ClassroomFragment();
            if (getSupportActionBar() != null) getSupportActionBar().setTitle("课堂管理");
            Log.d(TAG, "切换到课堂");
        } else if (itemId == R.id.nav_attendance) {
            selectedFragment = new AttendanceFragment();
            if (getSupportActionBar() != null) getSupportActionBar().setTitle("考勤管理");
            Log.d(TAG, "切换到考勤");
        } else if (itemId == R.id.nav_profile) {
            selectedFragment = new ProfileFragment();
            if (getSupportActionBar() != null) getSupportActionBar().setTitle("个人资料");
            Log.d(TAG, "切换到我的");
        }

        if (selectedFragment != null) {
            getSupportFragmentManager().beginTransaction()
                    .replace(R.id.fragment_container, selectedFragment)
                    .commit();
            updateBottomBarButtonsState(itemId);
        }
    }

    // 显式设置左右按钮的选中与未选中颜色
    private void updateBottomBarButtonsState(int selectedId) {
        int selectedColor = ContextCompat.getColor(this, R.color.primary); // 默认用较中性蓝色，避免紫色
        int defaultColor = ContextCompat.getColor(this, R.color.grey_600);

        if (btnNavHome != null)
            ImageViewCompat.setImageTintList(btnNavHome,
                    ColorStateList.valueOf(selectedId == R.id.nav_home ? selectedColor : defaultColor));
        if (btnNavClassroom != null)
            ImageViewCompat.setImageTintList(btnNavClassroom,
                    ColorStateList.valueOf(selectedId == R.id.nav_classroom ? selectedColor : defaultColor));
        if (btnNavAttendance != null)
            ImageViewCompat.setImageTintList(btnNavAttendance,
                    ColorStateList.valueOf(selectedId == R.id.nav_attendance ? selectedColor : defaultColor));
        if (btnNavProfile != null)
            ImageViewCompat.setImageTintList(btnNavProfile,
                    ColorStateList.valueOf(selectedId == R.id.nav_profile ? selectedColor : defaultColor));
    }
}