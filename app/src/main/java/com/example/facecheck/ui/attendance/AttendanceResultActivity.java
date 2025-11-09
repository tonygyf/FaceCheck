package com.example.facecheck.ui.attendance;

import android.content.Intent;
import android.database.Cursor;
import android.os.Bundle;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.facecheck.R;
import com.example.facecheck.activity.MisrecognitionCorrectionActivity;
import com.example.facecheck.adapters.AttendanceResultAdapter;
import com.example.facecheck.database.DatabaseHelper;
import com.example.facecheck.data.model.AttendanceResult;
import com.example.facecheck.data.model.Student;

import java.util.ArrayList;
import java.util.List;

public class AttendanceResultActivity extends AppCompatActivity {
    private static final String TAG = "AttendanceResultActivity";
    
    private DatabaseHelper dbHelper;
    private AttendanceResultAdapter resultAdapter;
    private long sessionId;
    private int detectedFacesExtra;
    private int recognizedFacesExtra;
    
    private RecyclerView recyclerViewResults;
    private TextView tvSessionInfo;
    private TextView tvSummary;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_attendance_result);
        
        // 获取会话ID
        sessionId = getIntent().getLongExtra("session_id", -1);
        detectedFacesExtra = getIntent().getIntExtra("detected_faces", 0);
        recognizedFacesExtra = getIntent().getIntExtra("recognized_faces", 0);
        if (sessionId == -1) {
            Toast.makeText(this, "会话信息无效", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }
        
        // 初始化数据库
        dbHelper = new DatabaseHelper(this);
        
        // 初始化视图
        initViews();
        
        // 加载考勤结果
        loadAttendanceResults();
    }

    private void initViews() {
        recyclerViewResults = findViewById(R.id.recyclerViewResults);
        tvSessionInfo = findViewById(R.id.tvSessionInfo);
        tvSummary = findViewById(R.id.tvSummary);
        
        // 设置RecyclerView
        recyclerViewResults.setLayoutManager(new LinearLayoutManager(this));// 设置适配器
        resultAdapter = new AttendanceResultAdapter(new ArrayList<>());
        resultAdapter.setOnCorrectionClickListener(new AttendanceResultAdapter.OnCorrectionClickListener() {
            @Override
            public void onCorrectionClick(AttendanceResult result) {
                // 跳转到修正界面
                Intent intent = new Intent(AttendanceResultActivity.this, MisrecognitionCorrectionActivity.class);
                if (result.getStudent() != null) {
                    intent.putExtra("student_name", result.getStudent().getName());
                    intent.putExtra("student_id", result.getStudent().getSid());
                }
                startActivityForResult(intent, 1001);
            }
        });
        recyclerViewResults.setAdapter(resultAdapter);
    }

    private void loadAttendanceResults() {
        List<AttendanceResult> results = new ArrayList<>();
        
        // 查询考勤结果
        Cursor cursor = dbHelper.getAttendanceResultsBySession(sessionId);
        if (cursor != null && cursor.moveToFirst()) {
            do {
                long id = cursor.getLong(cursor.getColumnIndexOrThrow("id"));
                long studentId = cursor.getLong(cursor.getColumnIndexOrThrow("studentId"));
                String status = cursor.getString(cursor.getColumnIndexOrThrow("status"));
                float score = cursor.getFloat(cursor.getColumnIndexOrThrow("score"));
                String decidedBy = cursor.getString(cursor.getColumnIndexOrThrow("decidedBy"));
                long decidedAt = cursor.getLong(cursor.getColumnIndexOrThrow("decidedAt"));
                
                // 获取学生信息
                Student student = getStudentById(studentId);
                if (student != null) {
                    AttendanceResult result = new AttendanceResult(id, sessionId, studentId, 
                        status, score, decidedBy, decidedAt);
                    result.setStudent(student);
                    results.add(result);
                }
            } while (cursor.moveToNext());
            cursor.close();
        }
        
        // 更新适配器
        resultAdapter.updateResults(results);
        
        // 更新统计信息
        updateSummary(results);
    }

    private Student getStudentById(long studentId) {
        Cursor cursor = dbHelper.getStudentById(studentId);
        if (cursor != null && cursor.moveToFirst()) {
            long id = cursor.getLong(cursor.getColumnIndexOrThrow("id"));
            long classId = cursor.getLong(cursor.getColumnIndexOrThrow("classId"));
            String name = cursor.getString(cursor.getColumnIndexOrThrow("name"));
            String sid = cursor.getString(cursor.getColumnIndexOrThrow("sid"));
            String gender = cursor.getString(cursor.getColumnIndexOrThrow("gender"));
            String avatarUri = cursor.getString(cursor.getColumnIndexOrThrow("avatarUri"));
            long createdAt = cursor.getLong(cursor.getColumnIndexOrThrow("createdAt"));
            
            cursor.close();
            return new Student(id, classId, name, sid, gender, avatarUri, createdAt);
        }
        if (cursor != null) {
            cursor.close();
        }
        return null;
    }

    private void updateSummary(List<AttendanceResult> results) {
        int total = results.size(); // 本班总人数（本次会话已写入 Present/Absent）
        int present = 0;
        int absent = 0;
        
        for (AttendanceResult result : results) {
            String status = result.getStatus();
            if ("Present".equals(status)) {
                present++;
            } else if ("Absent".equals(status)) {
                absent++;
            } else {
                // 其他状态（如 Late/Leave），不计入上述三类
            }
        }
        // 未知人数来源于：检测到的人脸数量 - 成功识别的人脸数量（可能是非本班人脸或未达阈值）
        int unknown = Math.max(0, detectedFacesExtra - recognizedFacesExtra);
        
        String summary = String.format("总人数: %d, 出勤: %d, 缺勤: %d, 未知: %d", 
            total, present, absent, unknown);
        tvSummary.setText(summary);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == 1001 && resultCode == RESULT_OK) {
            // 修正完成后刷新数据
            loadAttendanceResults();
            Toast.makeText(this, "修正已应用", Toast.LENGTH_SHORT).show();
        }
    }
}