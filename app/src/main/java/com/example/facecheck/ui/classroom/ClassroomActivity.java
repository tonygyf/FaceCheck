package com.example.facecheck.ui.classroom;

import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.FileProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.facecheck.R;
import com.example.facecheck.adapters.StudentAdapter;
import com.example.facecheck.database.DatabaseHelper;
import com.example.facecheck.models.Student;
import com.example.facecheck.sync.SyncManager;
import com.example.facecheck.ui.attendance.AttendanceActivity;
import com.google.android.material.floatingactionbutton.FloatingActionButton;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

public class ClassroomActivity extends AppCompatActivity {
    private static final String TAG = "ClassroomActivity";
    
    private DatabaseHelper dbHelper;
    private StudentAdapter studentAdapter;
    private long classroomId;
    private Uri currentPhotoUri;
    
    private RecyclerView recyclerView;
    private FloatingActionButton fabAddStudent;
    private FloatingActionButton fabStartAttendance;
    
    private ActivityResultLauncher<Intent> takePhotoLauncher;
    private ActivityResultLauncher<String> pickPhotoLauncher;
    private Student currentStudent;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_classroom);
        
        // 获取班级ID
        classroomId = getIntent().getLongExtra("classroom_id", -1);
        if (classroomId == -1) {
            Toast.makeText(this, "班级信息无效", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }
        
        // 初始化数据库
        dbHelper = new DatabaseHelper(this);
        
        // 初始化视图
        initViews();
        
        // 初始化照片选择器
        initPhotoLaunchers();
        
        // 加载学生列表
        loadStudents();
    }

    private void initViews() {
        recyclerView = findViewById(R.id.recyclerViewStudents);
        fabAddStudent = findViewById(R.id.fabAddStudent);
        fabStartAttendance = findViewById(R.id.fabStartAttendance);
        
        // 设置RecyclerView
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        studentAdapter = new StudentAdapter(new ArrayList<>());
        recyclerView.setAdapter(studentAdapter);
        
        // 设置点击事件
        fabAddStudent.setOnClickListener(v -> showAddStudentDialog());
        fabStartAttendance.setOnClickListener(v -> startAttendanceSession());
        
        // 设置学生点击事件
        studentAdapter.setOnItemClickListener(student -> showStudentDetailsDialog(student));
    }

    private void initPhotoLaunchers() {
        // 拍照
        takePhotoLauncher = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            result -> {
                if (result.getResultCode() == RESULT_OK && currentPhotoUri != null) {
                    processNewPhoto(currentPhotoUri);
                }
            });
            
        // 选择照片
        pickPhotoLauncher = registerForActivityResult(
            new ActivityResultContracts.GetContent(),
            uri -> {
                if (uri != null) {
                    processNewPhoto(uri);
                }
            });
    }

    private void loadStudents() {
        Cursor cursor = dbHelper.getStudentsByClass(classroomId);
        List<Student> students = new ArrayList<>();
        
        if (cursor != null && cursor.moveToFirst()) {
            do {
                long id = cursor.getLong(cursor.getColumnIndexOrThrow("id"));
                String name = cursor.getString(cursor.getColumnIndexOrThrow("name"));
                String sid = cursor.getString(cursor.getColumnIndexOrThrow("sid"));
                String gender = cursor.getString(cursor.getColumnIndexOrThrow("gender"));
                String avatarUri = cursor.getString(cursor.getColumnIndexOrThrow("avatarUri"));
                long createdAt = cursor.getLong(cursor.getColumnIndexOrThrow("createdAt"));
                
                Student student = new Student(id, classroomId, name, sid, gender, avatarUri, createdAt);
                students.add(student);
            } while (cursor.moveToNext());
            cursor.close();
        }
        
        studentAdapter.updateStudents(students);
    }

    private void showAddStudentDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        View view = LayoutInflater.from(this).inflate(R.layout.dialog_add_student, null);
        
        EditText etName = view.findViewById(R.id.etStudentName);
        EditText etSid = view.findViewById(R.id.etStudentId);
        EditText etGender = view.findViewById(R.id.etGender);
        ImageView ivAvatar = view.findViewById(R.id.ivAvatar);
        
        // 设置头像点击事件
        ivAvatar.setOnClickListener(v -> showPhotoSourceDialog());
        
        builder.setView(view)
               .setTitle("添加学生")
               .setPositiveButton("确定", (dialog, which) -> {
                   String name = etName.getText().toString().trim();
                   String sid = etSid.getText().toString().trim();
                   String gender = etGender.getText().toString().trim();
                   
                   if (TextUtils.isEmpty(name) || TextUtils.isEmpty(sid)) {
                       Toast.makeText(ClassroomActivity.this, "请填写完整信息", Toast.LENGTH_SHORT).show();
                       return;
                   }
                   
                   String avatarUri = currentPhotoUri != null ? currentPhotoUri.toString() : "";
                   long studentId = dbHelper.insertStudent(classroomId, name, sid, gender, avatarUri);
                   
                   if (studentId != -1) {
                       // 添加同步日志
                       dbHelper.insertSyncLog("Student", studentId, "UPSERT", 
                           System.currentTimeMillis(), "PENDING");
                       
                       // 刷新列表
                       loadStudents();
                   }
               })
               .setNegativeButton("取消", null)
               .show();
    }

    private void showPhotoSourceDialog() {
        String[] items = {"拍照", "从相册选择"};
        new AlertDialog.Builder(this)
            .setTitle("选择照片来源")
            .setItems(items, (dialog, which) -> {
                if (which == 0) {
                    takePhoto();
                } else {
                    pickPhoto();
                }
            })
            .show();
    }

    private void takePhoto() {
        Intent intent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        File photoFile = new File(getExternalFilesDir("photos"), 
            "student_" + System.currentTimeMillis() + ".jpg");
            
        currentPhotoUri = FileProvider.getUriForFile(this,
            "com.example.facecheck.fileprovider", photoFile);
            
        intent.putExtra(MediaStore.EXTRA_OUTPUT, currentPhotoUri);
        takePhotoLauncher.launch(intent);
    }

    private void pickPhoto() {
        pickPhotoLauncher.launch("image/*");
    }

    private void processNewPhoto(Uri photoUri) {
        currentPhotoUri = photoUri;
        // TODO: 处理照片，包括人脸检测、特征提取等
    }

    private void showStudentDetailsDialog(Student student) {
        // TODO: 显示学生详细信息，包括头像、考勤记录等
    }

    private void startAttendanceSession() {
        Intent intent = new Intent(this, AttendanceActivity.class);
        intent.putExtra("classroom_id", classroomId);
        startActivity(intent);
    }
}