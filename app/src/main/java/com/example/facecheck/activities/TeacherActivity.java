package com.example.facecheck.activities;

import android.content.ContentValues;
import android.content.Intent;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.facecheck.R;
import com.example.facecheck.adapters.ClassroomAdapter;
import com.example.facecheck.database.DatabaseHelper;
import com.example.facecheck.models.Classroom;
import com.example.facecheck.sync.SyncManager;
import com.example.facecheck.webdav.WebDavManager;
import com.google.android.material.floatingactionbutton.FloatingActionButton;

import java.util.ArrayList;
import java.util.List;

public class TeacherActivity extends AppCompatActivity {
    private static final String TAG = "TeacherActivity";
    
    private DatabaseHelper dbHelper;
    private WebDavManager webDavManager;
    private SyncManager syncManager;
    private ClassroomAdapter classroomAdapter;
    private long teacherId;
    
    private RecyclerView recyclerView;
    private FloatingActionButton fabAddClass;
    private Button btnSync;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_teacher);
        
        // 获取教师ID
        teacherId = getIntent().getLongExtra("teacher_id", -1);
        if (teacherId == -1) {
            Toast.makeText(this, "教师信息无效", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }
        
        // 初始化数据库和同步管理器
        initManagers();
        
        // 初始化视图
        initViews();
        
        // 加载班级列表
        loadClassrooms();
    }

    private void initManagers() {
        dbHelper = new DatabaseHelper(this);
        
        // 从数据库获取教师的WebDAV信息
        SQLiteDatabase db = dbHelper.getReadableDatabase();
        Cursor cursor = db.query("Teacher", 
            new String[]{"davUrl", "davUser", "davKeyEnc"}, 
            "id = ?", 
            new String[]{String.valueOf(teacherId)}, 
            null, null, null);
            
        if (cursor != null && cursor.moveToFirst()) {
            String davUrl = cursor.getString(cursor.getColumnIndexOrThrow("davUrl"));
            String davUser = cursor.getString(cursor.getColumnIndexOrThrow("davUser"));
            String davKeyEnc = cursor.getString(cursor.getColumnIndexOrThrow("davKeyEnc"));
            
            if (!TextUtils.isEmpty(davUrl) && !TextUtils.isEmpty(davUser) && !TextUtils.isEmpty(davKeyEnc)) {
                webDavManager = new WebDavManager(this, davUrl, davUser, davKeyEnc);
                syncManager = new SyncManager(this, dbHelper, webDavManager);
            }
            cursor.close();
        }
    }

    private void initViews() {
        recyclerView = findViewById(R.id.recyclerViewClasses);
        fabAddClass = findViewById(R.id.fabAddClass);
        btnSync = findViewById(R.id.btnSync);
        
        // 设置RecyclerView
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        classroomAdapter = new ClassroomAdapter(new ArrayList<>());
        recyclerView.setAdapter(classroomAdapter);
        
        // 设置点击事件
        fabAddClass.setOnClickListener(v -> showAddClassDialog());
        btnSync.setOnClickListener(v -> performSync());
        
        // 设置班级点击事件
        classroomAdapter.setOnItemClickListener(classroom -> {
            Intent intent = new Intent(TeacherActivity.this, ClassroomActivity.class);
            intent.putExtra("classroom_id", classroom.getId());
            startActivity(intent);
        });
    }

    private void loadClassrooms() {
        List<Cursor> cursors = dbHelper.getClassroomsByTeacher(teacherId);
        List<Classroom> classrooms = new ArrayList<>();
        
        for (Cursor cursor : cursors) {
            if (cursor.moveToFirst()) {
                do {
                    long id = cursor.getLong(cursor.getColumnIndexOrThrow("id"));
                    String name = cursor.getString(cursor.getColumnIndexOrThrow("name"));
                    int year = cursor.getInt(cursor.getColumnIndexOrThrow("year"));
                    String meta = cursor.getString(cursor.getColumnIndexOrThrow("meta"));
                    
                    Classroom classroom = new Classroom(id, teacherId, name, year, meta);
                    classrooms.add(classroom);
                } while (cursor.moveToNext());
            }
            cursor.close();
        }
        
        classroomAdapter.updateClassrooms(classrooms);
    }

    private void showAddClassDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        View view = LayoutInflater.from(this).inflate(R.layout.dialog_add_class, null);
        
        EditText etClassName = view.findViewById(R.id.etClassName);
        EditText etYear = view.findViewById(R.id.etYear);
        
        builder.setView(view)
               .setTitle("添加班级")
               .setPositiveButton("确定", (dialog, which) -> {
                   String className = etClassName.getText().toString().trim();
                   String yearStr = etYear.getText().toString().trim();
                   
                   if (TextUtils.isEmpty(className) || TextUtils.isEmpty(yearStr)) {
                       Toast.makeText(TeacherActivity.this, "请填写完整信息", Toast.LENGTH_SHORT).show();
                       return;
                   }
                   
                   int year = Integer.parseInt(yearStr);
                   long classId = dbHelper.insertClassroom(teacherId, className, year, "");
                   
                   if (classId != -1) {
                       // 添加同步日志
                       dbHelper.insertSyncLog("Classroom", classId, "UPSERT", 
                           System.currentTimeMillis(), "PENDING");
                       
                       // 刷新列表
                       loadClassrooms();
                   }
               })
               .setNegativeButton("取消", null)
               .show();
    }

    private void performSync() {
        if (syncManager == null) {
            Toast.makeText(this, "请先配置WebDAV", Toast.LENGTH_SHORT).show();
            return;
        }
        
        // 显示进度对话框
        AlertDialog progressDialog = new AlertDialog.Builder(this)
            .setTitle("同步中")
            .setMessage("正在同步数据，请稍候...")
            .setCancelable(false)
            .show();
        
        // 在后台线程执行同步
        new Thread(() -> {
            boolean success = syncManager.performSync();
            
            runOnUiThread(() -> {
                progressDialog.dismiss();
                Toast.makeText(TeacherActivity.this, 
                    success ? "同步成功" : "同步失败", 
                    Toast.LENGTH_SHORT).show();
                
                if (success) {
                    loadClassrooms();
                }
            });
        }).start();
    }
}