package com.example.facecheck.ui.classroom;

import com.example.facecheck.utils.FaceRecognitionManager;

import android.content.Intent;
import android.content.pm.PackageManager;
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
import androidx.core.content.ContextCompat;
import androidx.core.content.FileProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.example.facecheck.R;
import com.example.facecheck.adapters.StudentAdapter;
import com.example.facecheck.database.DatabaseHelper;
import com.example.facecheck.data.model.Student;
import com.example.facecheck.sync.SyncManager;
import com.example.facecheck.ui.attendance.AttendanceActivity;
import com.example.facecheck.utils.PhotoStorageManager;
import com.google.android.material.floatingactionbutton.FloatingActionButton;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.FileOutputStream;
import java.util.ArrayList;
import java.util.List;

public class ClassroomActivity extends AppCompatActivity {
    private static final String TAG = "ClassroomActivity";
    
    private DatabaseHelper dbHelper;
    private StudentAdapter studentAdapter;
    private long classroomId;
    private Uri currentPhotoUri;
    private File currentPhotoFile;
    private ImageView dialogAvatarImageView;
    
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
                    // 复制选择的图片到内部存储
                    copyPickedPhotoToInternalStorage(uri);
                    // 处理新的照片
                    processNewPhoto(currentPhotoUri);
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
        dialogAvatarImageView = ivAvatar; // 赋值给成员变量
        dialogAvatarImageView = ivAvatar; // 赋值给成员变量

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
                   
                   // 重置当前照片URI
                   currentPhotoUri = null;
                   currentPhotoFile = null;
                   
                   if (studentId != -1) {
                       // 添加同步日志
                       dbHelper.insertSyncLog("Student", studentId, "INSERT", 
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
                    checkCameraPermissionAndTakePhoto();
                } else {
                    checkStoragePermissionAndPickPhoto();
                }
            })
            .show();
    }

    private void checkCameraPermissionAndTakePhoto() {
        // 检查相机权限
        if (ContextCompat.checkSelfPermission(this, android.Manifest.permission.CAMERA) 
                != PackageManager.PERMISSION_GRANTED) {
            // 请求相机权限
            requestCameraPermissionLauncher.launch(android.Manifest.permission.CAMERA);
        } else {
            // 已有权限，直接拍照
            takePhoto();
        }
    }

    private void checkStoragePermissionAndPickPhoto() {
        // 检查存储权限（Android 13+使用READ_MEDIA_IMAGES，低版本使用READ_EXTERNAL_STORAGE）
        String permission = android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU
                ? android.Manifest.permission.READ_MEDIA_IMAGES
                : android.Manifest.permission.READ_EXTERNAL_STORAGE;
                
        if (ContextCompat.checkSelfPermission(this, permission) 
                != PackageManager.PERMISSION_GRANTED) {
            // 请求存储权限
            requestStoragePermissionLauncher.launch(permission);
        } else {
            // 已有权限，直接选择照片
            pickPhoto();
        }
    }

    private final ActivityResultLauncher<String> requestCameraPermissionLauncher = 
        registerForActivityResult(new ActivityResultContracts.RequestPermission(), isGranted -> {
            if (isGranted) {
                // 权限被授予，拍照
                takePhoto();
            } else {
                // 权限被拒绝
                Toast.makeText(this, "需要相机权限才能拍照", Toast.LENGTH_SHORT).show();
            }
        });

    private final ActivityResultLauncher<String> requestStoragePermissionLauncher = 
        registerForActivityResult(new ActivityResultContracts.RequestPermission(), isGranted -> {
            if (isGranted) {
                // 权限被授予，选择照片
                pickPhoto();
            } else {
                // 权限被拒绝
                Toast.makeText(this, "需要存储权限才能选择照片", Toast.LENGTH_SHORT).show();
            }
        });

    private void takePhoto() {
        // 使用新的照片存储管理器创建头像照片文件
        try {
            File photoFile = PhotoStorageManager.createAvatarPhotoFile(this);
            
            if (photoFile != null && photoFile.exists()) {
                currentPhotoUri = FileProvider.getUriForFile(this,
                    getApplicationContext().getPackageName() + ".fileprovider",
                    photoFile);
                
                Intent intent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
                intent.putExtra(MediaStore.EXTRA_OUTPUT, currentPhotoUri);
                intent.addFlags(Intent.FLAG_GRANT_WRITE_URI_PERMISSION);
                
                takePhotoLauncher.launch(intent);
            } else {
                Toast.makeText(this, "无法创建照片文件", Toast.LENGTH_SHORT).show();
            }
        } catch (IOException e) {
            e.printStackTrace();
            Toast.makeText(this, "创建照片文件失败: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }

    private void pickPhoto() {
        pickPhotoLauncher.launch("image/*");
    }
    
    private void copyPickedPhotoToInternalStorage(Uri sourceUri) {
        try {
            // 创建内部存储的目标文件
            File targetFile = PhotoStorageManager.createAvatarPhotoFile(this);
            
            if (targetFile != null) {
                // 复制选择的图片到内部存储
                copyUriToFile(sourceUri, targetFile);
                
                // 更新当前照片文件和URI
                currentPhotoFile = targetFile;
                currentPhotoUri = FileProvider.getUriForFile(this,
                        getApplicationContext().getPackageName() + ".fileprovider",
                        targetFile);
            }
        } catch (IOException e) {
            e.printStackTrace();
            Toast.makeText(this, "复制照片失败: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }
    
    private void copyUriToFile(Uri sourceUri, File targetFile) throws IOException {
        InputStream inputStream = getContentResolver().openInputStream(sourceUri);
        FileOutputStream outputStream = new FileOutputStream(targetFile);
        
        byte[] buffer = new byte[4096];
        int length;
        while ((length = inputStream.read(buffer)) > 0) {
            outputStream.write(buffer, 0, length);
        }
        
        inputStream.close();
        outputStream.close();
    }

    private void processNewPhoto(Uri photoUri) {
        currentPhotoUri = photoUri;
        // 显示照片预览
        if (dialogAvatarImageView != null) {
            Glide.with(this)
                .load(photoUri)
                .placeholder(R.drawable.ic_person_placeholder)
                .error(R.drawable.ic_person_placeholder)
                .circleCrop()
                .into(dialogAvatarImageView);
        }

        if (currentStudent != null) {
            // 如果是编辑学生模式，显示确认对话框
            new AlertDialog.Builder(this)
                .setTitle("确认修改头像")
                .setMessage("确定要将这张照片设为 " + currentStudent.getName() + " 的头像吗？")
                .setPositiveButton("确定", (dialog, which) -> {
                    updateStudentAvatar(currentStudent, photoUri);
                })
                .setNegativeButton("取消", null)
                .show();
        }
    }

    private void updateStudentAvatar(Student student, Uri photoUri) {
        // 更新学生头像
        boolean success = dbHelper.updateStudent(student.getId(), student.getClassId(), 
            student.getName(), student.getSid(), student.getGender(), photoUri.toString());
        
        if (success) {
            // 添加同步日志
            dbHelper.insertSyncLog("Student", student.getId(), "UPDATE", 
                System.currentTimeMillis(), "PENDING");
            
            Toast.makeText(this, "头像更新成功", Toast.LENGTH_SHORT).show();
            loadStudents(); // 刷新列表
        } else {
            Toast.makeText(this, "头像更新失败", Toast.LENGTH_SHORT).show();
        }
    }

    private void showStudentDetailsDialog(Student student) {
        currentStudent = student; // 设置当前编辑的学生
        
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        View view = LayoutInflater.from(this).inflate(R.layout.dialog_student_details, null);
        
        EditText etName = view.findViewById(R.id.etStudentName);
        EditText etSid = view.findViewById(R.id.etStudentId);
        EditText etGender = view.findViewById(R.id.etGender);
        ImageView ivAvatar = view.findViewById(R.id.ivAvatar);
        dialogAvatarImageView = ivAvatar; // 赋值给成员变量
        
        // 填充现有数据
        etName.setText(student.getName());
        etSid.setText(student.getSid());
        etGender.setText(student.getGender());
        
        // 加载头像 - 使用 Glide 处理 content:// URI，避免权限问题
        if (student.getAvatarUri() != null && !student.getAvatarUri().isEmpty()) {
            Glide.with(this)
                .load(Uri.parse(student.getAvatarUri()))
                .placeholder(R.drawable.ic_person_placeholder)
                .error(R.drawable.ic_person_placeholder)
                .circleCrop()
                .into(ivAvatar);
        } else {
            ivAvatar.setImageResource(R.drawable.ic_person_placeholder);
        }
        
        // 设置头像点击事件
        ivAvatar.setOnClickListener(v -> showPhotoSourceDialog());
        
        builder.setView(view)
               .setTitle("学生详情")
               .setPositiveButton("保存", (dialog, which) -> {
                   String name = etName.getText().toString().trim();
                   String gender = etGender.getText().toString().trim();
                   
                   if (TextUtils.isEmpty(name)) {
                       Toast.makeText(ClassroomActivity.this, "姓名不能为空", Toast.LENGTH_SHORT).show();
                       return;
                   }
                   
                   // 获取新的头像URI，如果 currentPhotoUri 不为空则使用新的，否则使用学生原有的
                   String newAvatarUri = currentPhotoUri != null ? currentPhotoUri.toString() : student.getAvatarUri();

                   // 更新学生信息
                    boolean success = dbHelper.updateStudent(student.getId(), student.getClassId(), 
                        name, student.getSid(), gender, newAvatarUri);
                   
                   // 重置 currentPhotoUri 和 currentPhotoFile
                   currentPhotoUri = null;
                   currentPhotoFile = null;
                   
                   if (success) {
                       // 添加同步日志
                       dbHelper.insertSyncLog("Student", student.getId(), "UPDATE", 
                           System.currentTimeMillis(), "PENDING");
                       
                       Toast.makeText(ClassroomActivity.this, "学生信息已更新", Toast.LENGTH_SHORT).show();
                       loadStudents(); // 刷新列表
                   } else {
                       Toast.makeText(ClassroomActivity.this, "更新失败", Toast.LENGTH_SHORT).show();
                   }
               })
               .setNegativeButton("取消", null)
               .setNeutralButton("删除", (dialog, which) -> {
                   // 删除学生
                   showDeleteStudentDialog(student);
               })
               .show();
    }
    
    private void showDeleteStudentDialog(Student student) {
        new AlertDialog.Builder(this)
            .setTitle("确认删除")
            .setMessage("确定要删除学生 " + student.getName() + " 吗？")
            .setPositiveButton("确定", (dialog, which) -> {
                boolean success = dbHelper.deleteStudent(student.getId());
                if (success) {
                    // 添加同步日志
                    dbHelper.insertSyncLog("Student", student.getId(), "DELETE", 
                        System.currentTimeMillis(), "PENDING");
                    
                    Toast.makeText(ClassroomActivity.this, "学生已删除", Toast.LENGTH_SHORT).show();
                    loadStudents(); // 刷新列表
                } else {
                    Toast.makeText(ClassroomActivity.this, "删除失败", Toast.LENGTH_SHORT).show();
                }
            })
            .setNegativeButton("取消", null)
            .show();
    }

    private void startAttendanceSession() {
        Intent intent = new Intent(this, AttendanceActivity.class);
        intent.putExtra("classroom_id", classroomId);
        startActivity(intent);
    }
}