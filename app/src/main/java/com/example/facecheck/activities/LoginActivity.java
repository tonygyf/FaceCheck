package com.example.facecheck.activities;

import android.content.Intent;
import android.database.Cursor;
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

public class LoginActivity extends AppCompatActivity {

    private EditText emailEditText, passwordEditText;
    private Button loginButton;
    private TextView registerTextView;
    private ProgressBar progressBar;
    private DatabaseHelper dbHelper;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        // 初始化数据库
        dbHelper = new DatabaseHelper(this);

        // 初始化视图
        emailEditText = findViewById(R.id.emailEditText);
        passwordEditText = findViewById(R.id.passwordEditText);
        loginButton = findViewById(R.id.loginButton);
        registerTextView = findViewById(R.id.registerTextView);
        progressBar = findViewById(R.id.progressBar);

        // 设置登录按钮点击事件
        loginButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                loginUser();
            }
        });

        // 设置注册文本点击事件
        registerTextView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // 跳转到注册页面
                Intent intent = new Intent(LoginActivity.this, RegisterActivity.class);
                startActivity(intent);
            }
        });
    }

    private void loginUser() {
        String email = emailEditText.getText().toString().trim();
        String password = passwordEditText.getText().toString().trim();

        // 验证输入
        if (TextUtils.isEmpty(email)) {
            emailEditText.setError("请输入邮箱");
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

        // 显示进度条
        progressBar.setVisibility(View.VISIBLE);
        loginButton.setEnabled(false);

        // 查询数据库验证用户
        Cursor cursor = dbHelper.getReadableDatabase().query(
            "Teacher",
            new String[]{"id", "name", "email", "davKeyEnc"},
            "email = ?",
            new String[]{email},
            null, null, null);

        if (cursor != null && cursor.moveToFirst()) {
            long teacherId = cursor.getLong(cursor.getColumnIndexOrThrow("id"));
            String davKeyEnc = cursor.getString(cursor.getColumnIndexOrThrow("davKeyEnc"));
            
            // 验证密码
            String decryptedKey = CryptoUtils.decryptWithPassword(davKeyEnc, password);
            if (decryptedKey != null) {
                // 登录成功
                cursor.close();
                progressBar.setVisibility(View.GONE);
                Toast.makeText(LoginActivity.this, "登录成功！", Toast.LENGTH_SHORT).show();
                
                // 跳转到主页面
                Intent intent = new Intent(LoginActivity.this, com.example.facecheck.MainActivity.class);
                intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
                intent.putExtra("teacher_id", teacherId);
                startActivity(intent);
                finish();
            } else {
                // 密码错误
                cursor.close();
                progressBar.setVisibility(View.GONE);
                loginButton.setEnabled(true);
                Toast.makeText(LoginActivity.this, "密码错误", Toast.LENGTH_SHORT).show();
            }
        } else {
            // 用户不存在
            if (cursor != null) {
                cursor.close();
            }
            progressBar.setVisibility(View.GONE);
            loginButton.setEnabled(true);
            Toast.makeText(LoginActivity.this, "用户不存在", Toast.LENGTH_SHORT).show();
        }
    }
}