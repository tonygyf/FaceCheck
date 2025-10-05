package com.example.facecheck;

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

        // 获取传递的教师ID
        if (getIntent().hasExtra("teacher_id")) {
            teacherId = getIntent().getLongExtra("teacher_id", -1);
        }

        bottomNavigationView = findViewById(R.id.bottom_navigation);
        bottomNavigationView.setOnItemSelectedListener(navListener);

        // 默认显示首页
        if (savedInstanceState == null) {
            getSupportFragmentManager().beginTransaction()
                    .replace(R.id.fragment_container, new HomeFragment())
                    .commit();
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