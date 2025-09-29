package com.example.facecheck;

import android.content.Intent;
import android.database.Cursor;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.facecheck.activities.TeacherActivity;
import com.example.facecheck.activities.UserProfileActivity;
import com.example.facecheck.database.DatabaseHelper;
import com.example.facecheck.databinding.ActivityMainBinding;

public class JavaMainActivity extends AppCompatActivity {
    private ActivityMainBinding binding;
    private DatabaseHelper dbHelper;
    private long teacherId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        binding = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        setSupportActionBar(binding.toolbar);

        // 获取教师ID
        teacherId = getIntent().getLongExtra("teacher_id", -1);
        if (teacherId == -1) {
            Toast.makeText(this, "教师信息无效", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        // 初始化数据库
        dbHelper = new DatabaseHelper(this);

        // 加载教师信息
        loadTeacherInfo();

        // 设置点击事件
        binding.fab.setOnClickListener(view -> {
            // 跳转到班级管理界面
            Intent intent = new Intent(JavaMainActivity.this, TeacherActivity.class);
            intent.putExtra("teacher_id", teacherId);
            startActivity(intent);
        });
    }

    private void loadTeacherInfo() {
        Cursor cursor = dbHelper.getReadableDatabase().query(
            "Teacher",
            new String[]{"name", "email"},
            "id = ?",
            new String[]{String.valueOf(teacherId)},
            null, null, null);

        if (cursor != null && cursor.moveToFirst()) {
            String name = cursor.getString(cursor.getColumnIndexOrThrow("name"));
            String email = cursor.getString(cursor.getColumnIndexOrThrow("email"));

            // 设置标题
            binding.toolbar.setTitle("欢迎, " + name);
            cursor.close();
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();

        if (id == R.id.action_profile) {
            // 跳转到个人资料界面
            Intent intent = new Intent(this, UserProfileActivity.class);
            intent.putExtra("teacher_id", teacherId);
            startActivity(intent);
            return true;
        }

        return super.onOptionsItemSelected(item);
    }
}