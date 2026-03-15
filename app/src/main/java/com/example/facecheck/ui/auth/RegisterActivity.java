package com.example.facecheck.ui.auth;

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

import androidx.lifecycle.ViewModelProvider;

import com.example.facecheck.R;

public class RegisterActivity extends AppCompatActivity {

    private EditText nameEditText, usernameEditText, passwordEditText, confirmPasswordEditText;
    private Button registerButton;
    private TextView loginTextView;
    private ProgressBar progressBar;
    private RegisterViewModel registerViewModel;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_register);

        // 初始化ViewModel
        registerViewModel = new ViewModelProvider(this).get(RegisterViewModel.class);

        // 初始化视图
        nameEditText = findViewById(R.id.nameEditText);
        usernameEditText = findViewById(R.id.usernameEditText);
        passwordEditText = findViewById(R.id.passwordEditText);
        confirmPasswordEditText = findViewById(R.id.confirmPasswordEditText);
        registerButton = findViewById(R.id.registerButton);
        loginTextView = findViewById(R.id.loginTextView);
        progressBar = findViewById(R.id.progressBar);

        // 观察UI状态
        registerViewModel.uiState.observe(this, this::handleUiState);

        // 设置注册按钮点击事件
        registerButton.setOnClickListener(v -> registerUser());

        // 设置登录文本点击事件
        loginTextView.setOnClickListener(v -> finish());
    }

    private void handleUiState(RegisterUiState state) {
        if (state instanceof RegisterUiState.Initial) {
            progressBar.setVisibility(View.GONE);
            registerButton.setEnabled(true);
        } else if (state instanceof RegisterUiState.Loading) {
            progressBar.setVisibility(View.VISIBLE);
            registerButton.setEnabled(false);
        } else if (state instanceof RegisterUiState.Success) {
            progressBar.setVisibility(View.GONE);
            Toast.makeText(this, ((RegisterUiState.Success) state).message, Toast.LENGTH_SHORT).show();
            // 注册成功后返回登录页
            Intent intent = new Intent(RegisterActivity.this, LoginActivity.class);
            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
            startActivity(intent);
            finish();
        } else if (state instanceof RegisterUiState.Error) {
            progressBar.setVisibility(View.GONE);
            registerButton.setEnabled(true);
            Toast.makeText(this, ((RegisterUiState.Error) state).message, Toast.LENGTH_SHORT).show();
        }
    }

    private void registerUser() {
        String name = nameEditText.getText().toString().trim();
        String username = usernameEditText.getText().toString().trim();
        String password = passwordEditText.getText().toString().trim();
        String confirmPassword = confirmPasswordEditText.getText().toString().trim();

        // 验证输入
        if (TextUtils.isEmpty(name)) {
            nameEditText.setError("请输入姓名");
            return;
        }

        if (TextUtils.isEmpty(username)) {
            usernameEditText.setError("请输入用户名/邮箱");
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

        // 调用ViewModel进行注册
        registerViewModel.registerTeacher(name, username, password);
    }
}