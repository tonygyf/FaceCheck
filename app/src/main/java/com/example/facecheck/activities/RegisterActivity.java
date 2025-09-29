package com.example.facecheck.activities;

import android.content.ContentValues;
import android.content.Intent;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.facecheck.R;
import com.example.facecheck.database.DatabaseHelper;
import com.example.facecheck.utils.CryptoUtils;

public class RegisterActivity extends AppCompatActivity {

    private EditText nameEditText, emailEditText, passwordEditText, confirmPasswordEditText;
    private Button registerButton;
    private TextView loginTextView;
    private ProgressBar progressBar;
    private DatabaseHelper dbHelper;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_register);

        // 初始化数据库
        dbHelper = new DatabaseHelper(this);

        // 初始化视图
        nameEditText = findViewById(R.id.nameEditText);
        emailEditText = findViewById(R.id.emailEditText);
        passwordEditText = findViewById(R.id.passwordEditText);
        confirmPasswordEditText = findViewById(R.id.confirmPasswordEditText);
        registerButton = findViewById(R.id.registerButton);
        loginTextView = findViewById(R.id.loginTextView);
        progressBar = findViewById(R.id.progressBar);

        // 设置注册按钮点击事件
        registerButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                registerUser();
            }
        });

        // 设置登录文本点击事件
        loginTextView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // 返回登录页面
                finish();
            }
        });
    }

    private void registerUser() {
        String name = nameEditText.getText().toString().trim();
        String email = emailEditText.getText().toString().trim();
        String password = passwordEditText.getText().toString().trim();
        String confirmPassword = confirmPasswordEditText.getText().toString().trim();

        // 验证输入
        if (TextUtils.isEmpty(name)) {
            nameEditText.setError("请输入姓名");
            return;
        }

        if (TextUtils.isEmpty(email)) {
            emailEditText.setError("请输入邮箱");
            return;
        }

        // 简单的邮箱格式验证
        if (!android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            emailEditText.setError("请输入有效的邮箱地址");
            return;
        }

        if (TextUtils.isEmpty(password)) {
            passwordEditText.setError("请输入密码");
            return;
        }

        if (password.length() < 6) {
            passwordEditText.setError("密码长度至少6位");
            return;
        }

        if (!password.equals(confirmPassword)) {
            confirmPasswordEditText.setError("两次输入的密码不一致");
            return;
        }

        // 显示进度条
        progressBar.setVisibility(View.VISIBLE);
        registerButton.setEnabled(false);

        // 检查邮箱是否已注册
        SQLiteDatabase db = dbHelper.getReadableDatabase();
        Cursor cursor = db.query(
            "Teacher",
            new String[]{"id"},
            "email = ?",
            new String[]{email},
            null, null, null);

        if (cursor != null && cursor.moveToFirst()) {
            cursor.close();
            progressBar.setVisibility(View.GONE);
            registerButton.setEnabled(true);
            emailEditText.setError("该邮箱已注册");
            return;
        }
        if (cursor != null) {
            cursor.close();
        }

        // 生成 WebDAV 密钥
        String davKey = CryptoUtils.generateRandomKey();
        String davKeyEnc = CryptoUtils.encryptWithPassword(davKey, password);

        // 创建新用户
        ContentValues values = new ContentValues();
        values.put("name", name);
        values.put("email", email);
        values.put("davUrl", "");  // 初始为空
        values.put("davUser", "");  // 初始为空
        values.put("davKeyEnc", davKeyEnc);

        long teacherId = dbHelper.getWritableDatabase().insert("Teacher", null, values);
        if (teacherId != -1) {
            // 注册成功
            progressBar.setVisibility(View.GONE);
            Toast.makeText(RegisterActivity.this, "注册成功！请登录", Toast.LENGTH_SHORT).show();
            
            // 返回登录页面
            Intent intent = new Intent(RegisterActivity.this, LoginActivity.class);
            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
            startActivity(intent);
            finish();
        } else {
            // 注册失败
            progressBar.setVisibility(View.GONE);
            registerButton.setEnabled(true);
            Toast.makeText(RegisterActivity.this, "注册失败，请重试", Toast.LENGTH_SHORT).show();
        }
    }
}