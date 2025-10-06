package com.example.facecheck.ui.attendance;

import android.database.Cursor;
import android.os.Bundle;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.facecheck.R;
import com.example.facecheck.adapters.AttendanceResultAdapter;
import com.example.facecheck.database.DatabaseHelper;
import com.example.facecheck.models.AttendanceResult;
import com.example.facecheck.models.Student;

import java.util.ArrayList;
import java.util.List;

public class AttendanceResultActivity extends AppCompatActivity {
    private static final String TAG = "AttendanceResultActivity";
    
    private DatabaseHelper dbHelper;
    private AttendanceResultAdapter resultAdapter;
    private long sessionId;
    
    private RecyclerView recyclerViewResults;
    private TextView tvSessionInfo;
    private TextView tvSummary;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_attendance_result);
        
        // 获取会话ID
        sessionId = getIntent().getLongExtra("session_id", -1);
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
        recyclerViewResults.setLayoutManager(new LinearLayoutManager(this));
        resultAdapter = new AttendanceResultAdapter(new ArrayList<>());
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
        int total = results.size();
        int present = 0;
        int absent = 0;
        int unknown = 0;
        
        for (AttendanceResult result : results) {
            switch (result.getStatus()) {
                case "PRESENT":
                    present++;
                    break;
                case "ABSENT":
                    absent++;
                    break;
                default:
                    unknown++;
                    break;
            }
        }
        
        String summary = String.format("总人数: %d, 出勤: %d, 缺勤: %d, 未知: %d", 
            total, present, absent, unknown);
        tvSummary.setText(summary);
    }
}