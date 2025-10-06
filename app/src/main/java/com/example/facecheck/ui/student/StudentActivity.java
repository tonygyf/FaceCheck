package com.example.facecheck.ui.student;

import android.os.Bundle;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.facecheck.R;

public class StudentActivity extends AppCompatActivity {
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main); // 临时使用main布局
        
        // 获取传递的参数
        long studentId = getIntent().getLongExtra("student_id", -1);
        long classId = getIntent().getLongExtra("class_id", -1);
        
        if (studentId != -1) {
            // 编辑现有学生
            setTitle("学生详情");
            Toast.makeText(this, "编辑学生 ID: " + studentId, Toast.LENGTH_SHORT).show();
        } else if (classId != -1) {
            // 添加新学生
            setTitle("添加学生");
            Toast.makeText(this, "添加学生到班级 ID: " + classId, Toast.LENGTH_SHORT).show();
        } else {
            Toast.makeText(this, "参数错误", Toast.LENGTH_SHORT).show();
            finish();
        }
    }
}