package com.example.facecheck.ui.teacher;

import android.os.Bundle;
import android.view.MenuItem;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import com.example.facecheck.R;
import com.example.facecheck.database.DatabaseHelper;
import com.example.facecheck.data.model.Teacher;

public class TeacherActivity extends AppCompatActivity {
    private EditText etName;
    private EditText etEmail;
    private Button btnSave;
    private DatabaseHelper dbHelper;
    private long teacherId = -1;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_teacher_edit);

        // 初始化数据库
        dbHelper = new DatabaseHelper(this);

        // 设置工具栏
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        getSupportActionBar().setTitle("教师信息");

        // 初始化视图
        etName = findViewById(R.id.et_name);
        etEmail = findViewById(R.id.et_username); // 使用username字段
        btnSave = findViewById(R.id.btn_save);

        // 获取教师ID
        teacherId = getIntent().getLongExtra("teacher_id", -1);
        if (teacherId != -1) {
            // 加载教师信息
            loadTeacherInfo();
        }

        // 设置保存按钮点击事件
        btnSave.setOnClickListener(v -> saveTeacher());
    }

    private void loadTeacherInfo() {
        Teacher teacher = dbHelper.getTeacherById(teacherId);
        if (teacher != null) {
            etName.setText(teacher.getName());
            etEmail.setText(teacher.getUsername());
        }
    }

    private void saveTeacher() {
        String name = etName.getText().toString().trim();
        String username = etEmail.getText().toString().trim();

        if (name.isEmpty() || username.isEmpty()) {
            Toast.makeText(this, "请填写所有字段", Toast.LENGTH_SHORT).show();
            return;
        }

        Teacher teacher = new Teacher();
        teacher.setId(teacherId);
        teacher.setName(name);
        teacher.setUsername(username);
        
        boolean success;
        
        if (teacherId == -1) {
            // 新建教师
            success = dbHelper.addTeacher(teacher);
        } else {
            // 更新教师信息
            success = dbHelper.updateTeacher(teacher);
        }

        if (success) {
            Toast.makeText(this, "保存成功", Toast.LENGTH_SHORT).show();
            finish();
        } else {
            Toast.makeText(this, "保存失败", Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            onBackPressed();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }
}