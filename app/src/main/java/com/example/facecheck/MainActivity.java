package com.example.facecheck;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.MenuItem;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.fragment.app.Fragment;

import com.example.facecheck.fragments.AttendanceFragment;
import com.example.facecheck.fragments.ClassroomFragment;
import com.example.facecheck.fragments.HomeFragment;
import com.example.facecheck.fragments.ProfileFragment;
import com.example.facecheck.fragments.StudentFragment;
import com.example.facecheck.ui.auth.LoginActivity;
import com.example.facecheck.utils.WebDAVSyncHelper;
import com.google.android.material.bottomnavigation.BottomNavigationView;

public class MainActivity extends AppCompatActivity {

    private BottomNavigationView bottomNavigationView;
    private long teacherId = -1;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // 设置工具栏
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        getSupportActionBar().setTitle("人脸考勤系统");

        // 获取教师ID
        teacherId = getSharedPreferences("user_prefs", MODE_PRIVATE).getLong("teacher_id", -1);
        if (teacherId == -1) {
            // 如果没有登录，跳转到登录页面
            startActivity(new Intent(this, LoginActivity.class));
            finish();
            return;
        }

        bottomNavigationView = findViewById(R.id.bottom_navigation);
        bottomNavigationView.setOnItemSelectedListener(navListener);

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
                // 显示同步开始的提示
                Toast.makeText(this, "开始备份数据库...", Toast.LENGTH_SHORT).show();
                
                syncHelper.setOnSyncListener(new WebDAVSyncHelper.OnSyncListener() {
                    @Override
                    public void onSyncStarted() {
                        // 同步开始
                    }

                    @Override
                    public void onSyncCompleted(boolean success, String message) {
                        // 同步完成后的处理
                        Toast.makeText(MainActivity.this, message, Toast.LENGTH_SHORT).show();
                    }
                });
                
                // 执行同步
                syncHelper.syncDatabase();
            }
        } catch (Exception e) {
            Toast.makeText(this, "备份初始化失败", Toast.LENGTH_SHORT).show();
        }
    }

    private BottomNavigationView.OnItemSelectedListener navListener =
            new BottomNavigationView.OnItemSelectedListener() {
                @Override
                public boolean onNavigationItemSelected(@NonNull MenuItem item) {
                    Fragment selectedFragment = null;

                    int itemId = item.getItemId();
                    if (itemId == R.id.nav_home) {
                        selectedFragment = new HomeFragment();
                        getSupportActionBar().setTitle("首页");
                    } else if (itemId == R.id.nav_classroom) {
                        selectedFragment = new ClassroomFragment();
                        getSupportActionBar().setTitle("课堂管理");
                    } else if (itemId == R.id.nav_attendance) {
                        selectedFragment = new AttendanceFragment();
                        getSupportActionBar().setTitle("考勤管理");
                    } else if (itemId == R.id.nav_profile) {
                        selectedFragment = new ProfileFragment();
                        getSupportActionBar().setTitle("个人资料");
                    }

                    if (selectedFragment != null) {
                        getSupportFragmentManager().beginTransaction()
                                .replace(R.id.fragment_container, selectedFragment)
                                .commit();
                    }

                    return true;
                }
            };
}