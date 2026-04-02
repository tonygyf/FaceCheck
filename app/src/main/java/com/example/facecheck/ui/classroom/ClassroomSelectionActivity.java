package com.example.facecheck.ui.classroom;

import android.content.Intent;
import android.database.Cursor;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.facecheck.R;
import com.example.facecheck.adapters.ClassroomAdapter;
import com.example.facecheck.database.DatabaseHelper;
import com.example.facecheck.data.model.Classroom;
import com.example.facecheck.ui.attendance.AttendanceActivity;

import java.util.ArrayList;
import java.util.List;

public class ClassroomSelectionActivity extends AppCompatActivity {

    private DatabaseHelper dbHelper;
    private RecyclerView recyclerView;
    private ClassroomAdapter adapter;
    private List<Classroom> classroomList;
    private TextView tvTitle;
    private Button btnCancel;
    private String mode; // "attendance" 或其他模式
    private String defaultType; // "FACE" 或 "MANUAL", 如果有值则跳过选择
    private long teacherId;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_classroom_selection);

        // 获取模式参数
        mode = getIntent().getStringExtra("mode");
        if (mode == null) {
            mode = "attendance"; // 默认模式
        }

        defaultType = getIntent().getStringExtra("default_type");

        // 获取教师ID
        teacherId = getSharedPreferences("user_prefs", MODE_PRIVATE).getLong("teacher_id", -1);
        if (teacherId == -1) {
            Toast.makeText(this, "教师信息无效", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        // 初始化数据库
        dbHelper = new DatabaseHelper(this);

        // 初始化视图
        initViews();

        // 加载班级数据
        loadClassrooms();
    }

    // ... (initViews and loadClassrooms are unchanged) ...

    private void initViews() {
        recyclerView = findViewById(R.id.recycler_classrooms);
        tvTitle = findViewById(R.id.tv_title);
        btnCancel = findViewById(R.id.btn_cancel);

        // 设置标题
        if ("attendance".equals(mode)) {
            if ("FACE".equals(defaultType)) {
                tvTitle.setText("选择班级 - 教师照片考勤");
            } else if ("MANUAL".equals(defaultType)) {
                tvTitle.setText("选择班级 - 发布自拍签到");
            } else {
                tvTitle.setText("选择班级 - 快速考勤");
            }
        } else {
            tvTitle.setText("选择班级");
        }

        // 设置取消按钮
        btnCancel.setOnClickListener(v -> finish());

        // 设置RecyclerView
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        classroomList = new ArrayList<>();
        adapter = new ClassroomAdapter(classroomList);
        recyclerView.setAdapter(adapter);

        // 设置适配器点击事件
        adapter.setOnItemClickListener(classroom -> {
            onClassroomSelected(classroom);
        });
    }

    private void loadClassrooms() {
        List<Classroom> newClassrooms = dbHelper.getAllClassroomsWithStudentCountAsList(teacherId);
        classroomList.clear();
        classroomList.addAll(newClassrooms);

        // 通知适配器数据已更新
        adapter.notifyDataSetChanged();

        // 如果没有班级，显示提示
        if (classroomList.isEmpty()) {
            Toast.makeText(this, "暂无班级，请先创建班级", Toast.LENGTH_SHORT).show();
        }
    }

    private void onClassroomSelected(Classroom classroom) {
        if ("create_checkin_task".equals(mode)) {
            Intent intent = new Intent(this, com.example.facecheck.ui.checkin.CreateCheckinTaskActivity.class);
            intent.putExtra("CLASS_ID", classroom.getId());
            intent.putExtra("CLASS_NAME", classroom.getName());
            startActivity(intent);
            finish(); // 关闭选择页面
        } else if ("attendance".equals(mode)) {
            // 如果指定了默认类型，直接使用
            if (defaultType != null && !defaultType.isEmpty()) {
                Intent intent = new Intent(this, AttendanceActivity.class);
                intent.putExtra("classroom_id", classroom.getId());
                intent.putExtra("classroom_name", classroom.getName());
                intent.putExtra("attendance_type", defaultType);
                startActivity(intent);
                finish();
                return;
            }

            // 否则弹出考勤方式选择对话框
            new androidx.appcompat.app.AlertDialog.Builder(this)
                    .setTitle("请选择考勤方式")
                    .setItems(new String[] { "教师照片考勤 (默认)", "学生自拍考勤" }, (dialog, which) -> {
                        String type = (which == 0) ? "FACE" : "MANUAL";
                        Intent intent = new Intent(this, AttendanceActivity.class);
                        intent.putExtra("classroom_id", classroom.getId());
                        intent.putExtra("classroom_name", classroom.getName());
                        intent.putExtra("attendance_type", type);
                        startActivity(intent);
                        finish(); // 关闭选择页面
                    })
                    .setNegativeButton("取消", null)
                    .show();
        } else {
            // 其他模式的处理
            Intent resultIntent = new Intent();
            resultIntent.putExtra("classroom_id", classroom.getId());
            resultIntent.putExtra("classroom_name", classroom.getName());
            setResult(RESULT_OK, resultIntent);
            finish();
        }
    }
}