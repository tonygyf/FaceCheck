package com.example.facecheck.ui.auth;

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
import androidx.lifecycle.ViewModelProvider;

import com.example.facecheck.R;
import com.example.facecheck.MainActivity;

public class LoginActivity extends AppCompatActivity {

    private EditText emailEditText, passwordEditText;
    private Button loginButton;
    private TextView registerTextView;
    private ProgressBar progressBar;
    private LoginViewModel loginViewModel;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        // 初始化ViewModel
        loginViewModel = new ViewModelProvider(this).get(LoginViewModel.class);

        // 初始化视图
        emailEditText = findViewById(R.id.emailEditText);
        passwordEditText = findViewById(R.id.passwordEditText);
        loginButton = findViewById(R.id.loginButton);
        registerTextView = findViewById(R.id.registerTextView);
        progressBar = findViewById(R.id.progressBar);

        // 观察ViewModel状态
        loginViewModel.uiState.observe(this, this::handleUiState);

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
    
    private void handleUiState(LoginUiState state) {
        if (state instanceof LoginUiState.Initial) {
            progressBar.setVisibility(View.GONE);
            loginButton.setEnabled(true);
        } else if (state instanceof LoginUiState.Loading) {
            progressBar.setVisibility(View.VISIBLE);
            loginButton.setEnabled(false);
        } else if (state instanceof LoginUiState.Success) {
            LoginUiState.Success success = (LoginUiState.Success) state;
            progressBar.setVisibility(View.GONE);
            Toast.makeText(LoginActivity.this, "登录成功！", Toast.LENGTH_SHORT).show();
            
            // 跳转到主页面
            Intent intent = new Intent(LoginActivity.this, MainActivity.class);
            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
            intent.putExtra("teacher_id", success.teacherId);
            startActivity(intent);
            finish();
        } else if (state instanceof LoginUiState.Error) {
            LoginUiState.Error error = (LoginUiState.Error) state;
            progressBar.setVisibility(View.GONE);
            loginButton.setEnabled(true);
            Toast.makeText(LoginActivity.this, error.message, Toast.LENGTH_SHORT).show();
        }
    }

    private void loginUser() {
        String username = emailEditText.getText().toString().trim();
        String password = passwordEditText.getText().toString().trim();

        // 验证输入
        if (TextUtils.isEmpty(username)) {
            emailEditText.setError("请输入用户名");
            return;
        }

        if (TextUtils.isEmpty(password)) {
            passwordEditText.setError("请输入密码");
            return;
        }

        // 调用ViewModel进行登录
        loginViewModel.login(username, password);
    }
}