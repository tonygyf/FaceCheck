package com.example.facecheck.activity;

import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.facecheck.R;
import com.example.facecheck.adapter.StudentCandidateAdapter;
import com.example.facecheck.database.DatabaseHelper;
import com.example.facecheck.data.model.Student;
import com.example.facecheck.utils.FaceRecognitionManager;

import java.util.ArrayList;
import java.util.List;
import android.util.Log;

public class MisrecognitionCorrectionActivity extends AppCompatActivity {
    
    private DatabaseHelper databaseHelper;
    private FaceRecognitionManager faceRecognitionManager;
    
    private ImageView ivOriginalFace;
    private TextView tvOriginalStudentName, tvOriginalStudentId, tvConfidence;
    private RadioGroup rgCorrectionReason;
    private RadioButton rbWrongRecognition, rbSimilarFace, rbPoorQuality, rbOther;
    private EditText etDetailedReason;
    private RecyclerView rvStudentCandidates;
    private Button btnCancel, btnSubmitCorrection;
    
    private StudentCandidateAdapter candidateAdapter;
    private List<Student> studentCandidates;
    private Student selectedStudent;
    private String originalStudentName;
    private String originalStudentId;
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_misrecognition_correction);
        
        initViews();
        setupToolbar();
        initDatabase();
        loadIntentData();
        setupRecyclerView();
        loadStudentCandidates();
        setupClickListeners();
    }
    
    private void initViews() {
        ivOriginalFace = findViewById(R.id.ivOriginalFace);
        tvOriginalStudentName = findViewById(R.id.tvOriginalStudentName);
        tvOriginalStudentId = findViewById(R.id.tvOriginalStudentId);
        tvConfidence = findViewById(R.id.tvOriginalConfidence);
        rgCorrectionReason = findViewById(R.id.rgCorrectionReason);
        rbWrongRecognition = findViewById(R.id.rbReason1);
        rbSimilarFace = findViewById(R.id.rbReason2);
        rbPoorQuality = findViewById(R.id.rbReason3);
        rbOther = findViewById(R.id.rbReason4);
        etDetailedReason = findViewById(R.id.etDetailedReason);
        rvStudentCandidates = findViewById(R.id.rvStudentCandidates);
        btnCancel = findViewById(R.id.btnCancel);
        btnSubmitCorrection = findViewById(R.id.btnSubmitCorrection);
    }
    
    private void setupToolbar() {
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setTitle("错误识别修正");
        }
    }
    
    private void initDatabase() {
        databaseHelper = new DatabaseHelper(this);
        faceRecognitionManager = new FaceRecognitionManager(this);
    }
    
    private void loadIntentData() {
        Intent intent = getIntent();
        originalStudentName = intent.getStringExtra("student_name");
        originalStudentId = intent.getStringExtra("student_id");
        
        // 显示原始识别结果（模拟数据）
        tvOriginalStudentName.setText("识别为: " + (originalStudentName != null ? originalStudentName : "未知"));
        tvOriginalStudentId.setText("学号: " + (originalStudentId != null ? originalStudentId : "未知"));
        tvConfidence.setText("置信度: 0.75"); // 模拟置信度
        
        // 设置默认头像
        ivOriginalFace.setImageResource(R.drawable.ic_person);
    }
    
    private void setupRecyclerView() {
        studentCandidates = new ArrayList<>();
        candidateAdapter = new StudentCandidateAdapter(this, studentCandidates);
        rvStudentCandidates.setLayoutManager(new LinearLayoutManager(this));
        rvStudentCandidates.setAdapter(candidateAdapter);
        
        // 设置候选学生点击监听
        candidateAdapter.setOnStudentClickListener(new StudentCandidateAdapter.OnStudentClickListener() {
            @Override
            public void onStudentClick(Student student) {
                selectedStudent = student;
                // 更新UI显示选中的学生
                updateSelectedStudentUI();
            }
        });
    }
    
    private void loadStudentCandidates() {
        // 加载所有学生作为候选
        studentCandidates.clear();
        List<Student> allStudents = databaseHelper.getAllStudents();
        studentCandidates.addAll(allStudents);
        candidateAdapter.notifyDataSetChanged();
    }
    
    private void updateSelectedStudentUI() {
        if (selectedStudent != null) {
            // 可以在这里添加选中学生的视觉反馈
            Toast.makeText(this, "已选择: " + selectedStudent.getName(), Toast.LENGTH_SHORT).show();
        }
    }
    
    private void setupClickListeners() {
        btnCancel.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finish();
            }
        });
        
        btnSubmitCorrection.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                submitCorrection();
            }
        });
    }
    
    private void submitCorrection() {
        // 验证输入
        if (selectedStudent == null) {
            Toast.makeText(this, "请选择正确的学生", Toast.LENGTH_SHORT).show();
            return;
        }
        
        int selectedReasonId = rgCorrectionReason.getCheckedRadioButtonId();
        if (selectedReasonId == -1) {
            Toast.makeText(this, "请选择修正原因", Toast.LENGTH_SHORT).show();
            return;
        }
        
        String detailedReason = etDetailedReason.getText().toString().trim();
        if (detailedReason.isEmpty()) {
            Toast.makeText(this, "请输入详细说明", Toast.LENGTH_SHORT).show();
            return;
        }
        
        // 获取选择的修正原因
        String correctionReason = "";
        if (selectedReasonId == R.id.rbReason1) {
            correctionReason = "光线问题导致识别错误";
        } else if (selectedReasonId == R.id.rbReason2) {
            correctionReason = "角度问题导致识别错误";
        } else if (selectedReasonId == R.id.rbReason3) {
            correctionReason = "表情变化导致识别错误";
        } else if (selectedReasonId == R.id.rbReason4) {
            correctionReason = "其他原因";
        }
        
        // 执行修正操作
        performCorrection(selectedStudent, correctionReason, detailedReason);
    }
    
    private void performCorrection(Student correctStudent, String reason, String detailedReason) {
        // 显示进度
        Toast.makeText(this, "正在提交修正...", Toast.LENGTH_SHORT).show();
        
        // 模拟修正操作
        new android.os.Handler().postDelayed(new Runnable() {
            @Override
            public void run() {
                // 这里可以执行实际的人脸数据更新操作
                boolean success = updateFaceRecognitionData(correctStudent, reason, detailedReason);
                
                if (success) {
                    Toast.makeText(MisrecognitionCorrectionActivity.this, 
                                   "修正成功！已将识别结果更新为: " + correctStudent.getName(), 
                                   Toast.LENGTH_LONG).show();
                    setResult(RESULT_OK);
                    finish();
                } else {
                    Toast.makeText(MisrecognitionCorrectionActivity.this, 
                                   "修正失败，请重试", Toast.LENGTH_SHORT).show();
                }
            }
        }, 2000);
    }
    
    private boolean updateFaceRecognitionData(Student correctStudent, String reason, String detailedReason) {
        // 这里实现实际的人脸识别数据更新逻辑
        // 可以更新人脸特征向量、重新训练模型等
        
        try {
            // 记录修正日志
            logCorrection(originalStudentId, correctStudent.getSid(), reason, detailedReason);
            
            // 更新人脸特征（如果需要）
            // faceRecognitionManager.updateStudentFaceFeatures(correctStudent);
            
            return true;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }
    
    private void logCorrection(String originalStudentId, String correctStudentId, 
                              String reason, String detailedReason) {
        // 记录修正日志到数据库
        // 可以创建一个修正日志表来记录所有的修正操作
        Log.d("Correction", String.format("Correction logged: %s -> %s, reason: %s, detail: %s", 
                                          originalStudentId, correctStudentId, reason, detailedReason));
    }
    
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            finish();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }
}