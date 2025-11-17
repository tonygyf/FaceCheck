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
    private ImageButton btnNavHome, btnNavClassroom, btnNavAttendance, btnNavProfile, btnNavSettings, btnNavCourses;

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
        // 初始标题在后续按所选页面设置；不在此硬编码，避免主题切换后标题回退

        SharedPreferences userPrefs = getSharedPreferences("user_prefs", MODE_PRIVATE);
        String role = userPrefs.getString("user_role", "teacher");
        long studentId = userPrefs.getLong("student_id", -1);
        teacherId = userPrefs.getLong("teacher_id", -1);
        boolean loggedIn = ("teacher".equals(role) && teacherId != -1) || ("student".equals(role) && studentId != -1);
        if (!loggedIn) {
            Intent intent = new Intent(this, LoginActivity.class);
            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
            startActivity(intent);
            finish();
            return;
        }
        if ("teacher".equals(role)) {
            Log.d(TAG, "当前登录教师ID: " + teacherId);
        } else {
            Log.d(TAG, "当前登录学生ID: " + studentId);
        }

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
        btnNavSettings = findViewById(R.id.btn_nav_settings);
        btnNavCourses = findViewById(R.id.btn_nav_courses);

        if (btnNavHome != null) btnNavHome.setOnClickListener(v -> handleNavigation(R.id.nav_home));
        if (btnNavClassroom != null) btnNavClassroom.setOnClickListener(v -> handleNavigation(R.id.nav_classroom));
        if (btnNavAttendance != null) btnNavAttendance.setOnClickListener(v -> handleNavigation(R.id.nav_attendance));
        if (btnNavProfile != null) btnNavProfile.setOnClickListener(v -> handleNavigation(R.id.nav_profile));
        if (btnNavCourses != null) btnNavCourses.setOnClickListener(v -> handleNavigation(R.id.nav_courses));
        if (btnNavSettings != null) btnNavSettings.setOnClickListener(v -> {
            int selectedColor = ContextCompat.getColor(this, R.color.primary);
            if (btnNavSettings != null) {
                ImageViewCompat.setImageTintList(btnNavSettings, ColorStateList.valueOf(selectedColor));
            }
            try {
                Fragment settingsFragment = new com.example.facecheck.fragments.SettingsFragment();
                getSupportFragmentManager().beginTransaction()
                        .replace(R.id.fragment_container, settingsFragment)
                        .addToBackStack("settings")
                        .commit();
                if (getSupportActionBar() != null) getSupportActionBar().setTitle("设置");
                updateBottomBarButtonsState(R.id.nav_settings);
                getSharedPreferences("ui_prefs", MODE_PRIVATE)
                        .edit().putInt("nav_selected_id", R.id.nav_settings).apply();
            } catch (Throwable t) {
                Toast.makeText(this, "设置页打开失败", Toast.LENGTH_SHORT).show();
            }
        });

        // 根据角色适配可见按钮
        role = getSharedPreferences("user_prefs", MODE_PRIVATE).getString("user_role", "teacher");
        if ("student".equals(role)) {
            if (btnNavClassroom != null) btnNavClassroom.setVisibility(View.GONE);
            if (btnNavAttendance != null) btnNavAttendance.setVisibility(View.VISIBLE);
            if (btnNavCourses != null) btnNavCourses.setVisibility(View.VISIBLE);
        } else {
            if (btnNavCourses != null) btnNavCourses.setVisibility(View.GONE);
        }

        // 恢复上次选中的导航项（默认首页）
        int savedNav = getSharedPreferences("ui_prefs", MODE_PRIVATE)
                .getInt("nav_selected_id", R.id.nav_home);
        handleNavigation(savedNav);
        if (getSupportFragmentManager().findFragmentById(R.id.fragment_container) == null) {
            handleNavigation(R.id.nav_home);
        }
        showFirstLaunchWelcomeIfNeeded();

        // 中间拍照打卡按钮
        FloatingActionButton fabCameraPunch = findViewById(R.id.fabCameraPunch);
        if (fabCameraPunch != null) {
            fabCameraPunch.setOnClickListener(v -> {
                try {
                    Intent intent = new Intent(MainActivity.this, com.example.facecheck.ui.face.FaceMiniDetectActivity.class);
                    startActivity(intent);
                } catch (Throwable t) {
                    Toast.makeText(MainActivity.this, "打开小人脸识别失败", Toast.LENGTH_SHORT).show();
                }
            });
        }

        // 默认展示已恢复的导航页，移除强制首页，避免覆盖用户选择

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
        String role = getSharedPreferences("user_prefs", MODE_PRIVATE).getString("user_role", "teacher");
        if (itemId == R.id.nav_home) {
            selectedFragment = new HomeFragment();
            if (getSupportActionBar() != null) getSupportActionBar().setTitle("首页");
            Log.d(TAG, "切换到首页");
        } else if (itemId == R.id.nav_classroom) {
            if ("student".equals(role)) {
                Toast.makeText(this, "课堂为教师专用，已为你打开首页", Toast.LENGTH_SHORT).show();
                selectedFragment = new HomeFragment();
            } else {
            selectedFragment = new ClassroomFragment();
            if (getSupportActionBar() != null) getSupportActionBar().setTitle("课堂管理");
            Log.d(TAG, "切换到课堂");
            }
        } else if (itemId == R.id.nav_attendance) {
            selectedFragment = new AttendanceFragment();
            if (getSupportActionBar() != null) getSupportActionBar().setTitle("考勤管理");
            Log.d(TAG, "切换到考勤");
        } else if (itemId == R.id.nav_profile) {
            selectedFragment = new ProfileFragment();
            if (getSupportActionBar() != null) getSupportActionBar().setTitle("个人资料");
            Log.d(TAG, "切换到我的");
        } else if (itemId == R.id.nav_settings) {
            selectedFragment = new com.example.facecheck.fragments.SettingsFragment();
            if (getSupportActionBar() != null) getSupportActionBar().setTitle("设置");
            Log.d(TAG, "切换到设置");
        } else if (itemId == R.id.nav_courses) {
            selectedFragment = new com.example.facecheck.fragments.StudentCoursesFragment();
            if (getSupportActionBar() != null) getSupportActionBar().setTitle("选课");
            Log.d(TAG, "切换到选课");
        }

        if (selectedFragment != null) {
            getSupportFragmentManager().beginTransaction()
                    .replace(R.id.fragment_container, selectedFragment)
                    .commit();
            updateBottomBarButtonsState(itemId);
            // 记住当前导航项，主题切换重建后恢复
            getSharedPreferences("ui_prefs", MODE_PRIVATE)
                    .edit().putInt("nav_selected_id", itemId).apply();
        }
    }

    private void showFirstLaunchWelcomeIfNeeded() {
        SharedPreferences prefs = getSharedPreferences("ui_prefs", MODE_PRIVATE);
        boolean shown = prefs.getBoolean("first_launch_welcome_shown", false);
        if (shown) return;
        String version = "";
        try {
            version = getPackageManager().getPackageInfo(getPackageName(), 0).versionName;
        } catch (Throwable ignore) {}
        new androidx.appcompat.app.AlertDialog.Builder(this)
                .setTitle("FaceCheck")
                .setMessage("版本：" + version + "\n\n人脸考勤·课堂管理·快速打卡\n简约现代的考勤体验")
                .setPositiveButton("开始使用", (d, w) -> {
                    getSharedPreferences("ui_prefs", MODE_PRIVATE)
                            .edit().putBoolean("first_launch_welcome_shown", true).apply();
                })
                .show();
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
        if (btnNavSettings != null)
            ImageViewCompat.setImageTintList(btnNavSettings,
                    ColorStateList.valueOf(selectedId == R.id.nav_settings ? selectedColor : defaultColor));
        if (btnNavCourses != null)
            ImageViewCompat.setImageTintList(btnNavCourses,
                    ColorStateList.valueOf(selectedId == R.id.nav_courses ? selectedColor : defaultColor));
    }
}
