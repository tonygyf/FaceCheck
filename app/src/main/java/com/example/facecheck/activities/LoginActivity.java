package com.example.facecheck.activities;

import android.content.Intent;
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

public class LoginActivity extends AppCompatActivity {

    private EditText emailEditText, passwordEditText;
    private Button loginButton;
    private TextView registerTextView;
    private ProgressBar progressBar;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

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

        // 模拟登录过程（这里应该连接到你的后端API）
        // 暂时使用简单的验证逻辑
        if (email.equals("test@example.com") && password.equals("123456")) {
            // 登录成功
            progressBar.setVisibility(View.GONE);
            Toast.makeText(LoginActivity.this, "登录成功！", Toast.LENGTH_SHORT).show();
            
            // 跳转到主页面
            Intent intent = new Intent(LoginActivity.this, com.example.facecheck.MainActivity.class);
            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
            startActivity(intent);
            finish();
        } else {
            // 登录失败
            progressBar.setVisibility(View.GONE);
            loginButton.setEnabled(true);
            Toast.makeText(LoginActivity.this, "邮箱或密码错误", Toast.LENGTH_SHORT).show();
        }
    }
}