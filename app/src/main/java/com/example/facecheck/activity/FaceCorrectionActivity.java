package com.example.facecheck.activity;

import android.content.Intent;
import android.os.Bundle;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.facecheck.R;
import com.example.facecheck.adapter.CorrectionRecordAdapter;
import com.example.facecheck.data.model.CorrectionRecord;
import com.example.facecheck.database.DatabaseHelper;
import com.example.facecheck.utils.FaceDataBackupManager;

import java.io.File;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

public class FaceCorrectionActivity extends AppCompatActivity implements CorrectionRecordAdapter.OnCorrectionClickListener {
    
    private DatabaseHelper databaseHelper;
    private FaceDataBackupManager backupManager;
    private RecyclerView rvCorrectionRecords;
    private TextView tvTotalFaces, tvPendingCorrections, tvCorrectionSuccessRate;
    private Button btnFixMisrecognition, btnBackupRestore, btnUpdateFaceData;
    private CorrectionRecordAdapter adapter;
    private List<CorrectionRecord> correctionRecords;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_face_correction_management);

        initViews();
        setupToolbar();
        initDatabase();
        setupRecyclerView();
        loadCorrectionData();
        setupClickListeners();
    }

    private void initViews() {
        rvCorrectionRecords = findViewById(R.id.rvRecentCorrections);
        tvTotalFaces = findViewById(R.id.tvTotalFaces);
        tvPendingCorrections = findViewById(R.id.tvPendingCorrections);
        tvCorrectionSuccessRate = findViewById(R.id.tvCorrectionRate);
        btnFixMisrecognition = findViewById(R.id.btnCorrectMisrecognition);
        btnBackupRestore = findViewById(R.id.btnBackupRestore);
        btnUpdateFaceData = findViewById(R.id.btnUpdateFaceData);
    }

    private void setupToolbar() {
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setTitle("人脸修正恢复");
        }
    }

    private void initDatabase() {
        databaseHelper = new DatabaseHelper(this);
        backupManager = new FaceDataBackupManager(this);
    }

    private void setupRecyclerView() {
        correctionRecords = new ArrayList<>();
        adapter = new CorrectionRecordAdapter(this, correctionRecords);
        rvCorrectionRecords.setLayoutManager(new LinearLayoutManager(this));
        rvCorrectionRecords.setAdapter(adapter);
    }

    private void loadCorrectionData() {
        // 加载需要修正的记录
        correctionRecords.clear();
        
        // 获取最近需要修正的记录（这里模拟一些数据）
        List<CorrectionRecord> records = getRecentCorrectionRecords();
        correctionRecords.addAll(records);
        
        // 更新统计信息
        updateStatistics();
        
        adapter.notifyDataSetChanged();
    }

    private List<CorrectionRecord> getRecentCorrectionRecords() {
        List<CorrectionRecord> records = new ArrayList<>();
        
        // 这里应该从数据库获取真实的需要修正的记录
        // 暂时模拟一些数据用于测试
        records.add(new CorrectionRecord(1, "张三", "2024001", "识别错误", 
                "2024-01-15 14:30", "人脸相似度过低"));
        records.add(new CorrectionRecord(2, "李四", "2024002", "数据异常", 
                "2024-01-15 15:20", "特征向量异常"));
        records.add(new CorrectionRecord(3, "王五", "2024003", "图像质量差", 
                "2024-01-15 16:10", "图像模糊需要重新采集"));
        
        return records;
    }

    private void updateStatistics() {
        // 获取统计信息
        int totalFaces = databaseHelper.getAllStudents().size(); // 总人脸数
        int pendingCorrections = correctionRecords.size(); // 待修正数
        
        // 计算修正成功率（这里模拟数据）
        int totalCorrections = 15; // 总修正数
        int successfulCorrections = 12; // 成功修正数
        double successRate = totalCorrections > 0 ? (successfulCorrections * 100.0 / totalCorrections) : 0;
        
        tvTotalFaces.setText(String.valueOf(totalFaces));
        tvPendingCorrections.setText(String.valueOf(pendingCorrections));
        tvCorrectionSuccessRate.setText(String.format("%.1f%%", successRate));
    }

    private void setupClickListeners() {
        btnFixMisrecognition.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // 跳转到错误识别修正界面
                Intent intent = new Intent(FaceCorrectionActivity.this, MisrecognitionCorrectionActivity.class);
                startActivity(intent);
            }
        });

        btnBackupRestore.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // 执行备份/恢复功能
                performBackupRestore();
            }
        });

        btnUpdateFaceData.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // 更新人脸数据
                updateFaceData();
            }
        });
    }

    private void performBackupRestore() {
        // 显示备份/恢复选项对话框
        showBackupRestoreDialog();
    }

    private void showBackupRestoreDialog() {
        androidx.appcompat.app.AlertDialog.Builder builder = new androidx.appcompat.app.AlertDialog.Builder(this);
        builder.setTitle("备份/恢复人脸数据")
               .setItems(new String[]{"备份人脸数据", "恢复人脸数据", "查看备份文件"}, (dialog, which) -> {
                   switch (which) {
                       case 0:
                           performBackup();
                           break;
                       case 1:
                           performRestore();
                           break;
                       case 2:
                           showBackupFiles();
                           break;
                   }
               })
               .show();
    }

    private void performBackup() {
        // 执行备份操作
        Toast.makeText(this, "正在备份人脸数据...", Toast.LENGTH_SHORT).show();
        
        new Thread(() -> {
            String backupPath = backupManager.backupFaceData();
            runOnUiThread(() -> {
                if (backupPath != null) {
                    backupManager.showBackupResult(true, "备份文件: " + backupPath);
                } else {
                    backupManager.showBackupResult(false, "备份失败");
                }
            });
        }).start();
    }

    private void performRestore() {
        // 获取备份文件列表
        List<String> backupFiles = backupManager.getBackupFiles();
        
        if (backupFiles.isEmpty()) {
            Toast.makeText(this, "没有找到备份文件", Toast.LENGTH_SHORT).show();
            return;
        }
        
        // 显示备份文件选择对话框
        String[] fileNames = new String[backupFiles.size()];
        for (int i = 0; i < backupFiles.size(); i++) {
            File file = new File(backupFiles.get(i));
            fileNames[i] = file.getName();
        }
        
        androidx.appcompat.app.AlertDialog.Builder builder = new androidx.appcompat.app.AlertDialog.Builder(this);
        builder.setTitle("选择备份文件")
               .setItems(fileNames, (dialog, which) -> {
                   String selectedFile = backupFiles.get(which);
                   confirmRestore(selectedFile);
               })
               .show();
    }

    private void confirmRestore(String backupFilePath) {
        androidx.appcompat.app.AlertDialog.Builder builder = new androidx.appcompat.app.AlertDialog.Builder(this);
        builder.setTitle("确认恢复")
               .setMessage("恢复操作将覆盖现有的人脸数据，是否继续？")
               .setPositiveButton("确定", (dialog, which) -> {
                   executeRestore(backupFilePath);
               })
               .setNegativeButton("取消", null)
               .show();
    }

    private void executeRestore(String backupFilePath) {
        Toast.makeText(this, "正在恢复人脸数据...", Toast.LENGTH_SHORT).show();
        
        new Thread(() -> {
            boolean success = backupManager.restoreFaceData(backupFilePath);
            runOnUiThread(() -> {
                if (success) {
                    backupManager.showRestoreResult(true, "恢复成功");
                    loadCorrectionData(); // 重新加载数据
                } else {
                    backupManager.showRestoreResult(false, "恢复失败");
                }
            });
        }).start();
    }

    private void showBackupFiles() {
        List<String> backupFiles = backupManager.getBackupFiles();
        
        if (backupFiles.isEmpty()) {
            Toast.makeText(this, "没有找到备份文件", Toast.LENGTH_SHORT).show();
            return;
        }
        
        StringBuilder message = new StringBuilder("备份文件列表:\n\n");
        for (String filePath : backupFiles) {
            File file = new File(filePath);
            message.append(file.getName()).append("\n");
            message.append("大小: ").append(file.length() / 1024).append(" KB\n");
            message.append("修改时间: ").append(new Date(file.lastModified())).append("\n\n");
        }
        
        androidx.appcompat.app.AlertDialog.Builder builder = new androidx.appcompat.app.AlertDialog.Builder(this);
        builder.setTitle("备份文件")
               .setMessage(message.toString())
               .setPositiveButton("确定", null)
               .show();
    }

    private void updateFaceData() {
        // 这里实现更新人脸数据逻辑
        Toast.makeText(this, "正在更新人脸数据...", Toast.LENGTH_SHORT).show();
        
        // 模拟操作完成
        new android.os.Handler().postDelayed(new Runnable() {
            @Override
            public void run() {
                Toast.makeText(FaceCorrectionActivity.this, "人脸数据更新完成", Toast.LENGTH_SHORT).show();
                loadCorrectionData(); // 重新加载数据
            }
        }, 3000);
    }

    @Override
    protected void onResume() {
        super.onResume();
        loadCorrectionData(); // 重新加载数据
    }

    @Override
    public void onCorrectClick(CorrectionRecord record) {
        // 处理修正点击事件
        Toast.makeText(this, "正在修正 " + record.getStudentName() + " 的记录", Toast.LENGTH_SHORT).show();
        
        // 这里可以跳转到具体的修正界面
        Intent intent = new Intent(this, MisrecognitionCorrectionActivity.class);
        intent.putExtra("student_name", record.getStudentName());
        intent.putExtra("student_id", record.getStudentId());
        startActivity(intent);
    }

    @Override
    public void onIgnoreClick(CorrectionRecord record) {
        // 处理忽略点击事件
        Toast.makeText(this, "已忽略 " + record.getStudentName() + " 的修正请求", Toast.LENGTH_SHORT).show();
        
        // 从列表中移除
        correctionRecords.remove(record);
        adapter.notifyDataSetChanged();
        updateStatistics();
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