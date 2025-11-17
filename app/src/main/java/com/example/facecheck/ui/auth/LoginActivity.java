package com.example.facecheck.ui.auth;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.ViewModelProvider;

import com.example.facecheck.R;
import com.example.facecheck.MainActivity;
import android.content.SharedPreferences;
import com.airbnb.lottie.LottieAnimationView;
import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;

public class LoginActivity extends AppCompatActivity {

    private EditText emailEditText, passwordEditText;
    private Button loginButton;
    private TextView registerTextView;
    private ProgressBar progressBar;
    private LoginViewModel loginViewModel;
    private CheckBox rememberPasswordCheckBox;
    private LottieAnimationView lottieLoginView;
    private View lottieOverlayLogin;
    private boolean navigated = false;

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
        rememberPasswordCheckBox = findViewById(R.id.rememberPasswordCheckBox);
        lottieLoginView = findViewById(R.id.lottieLoginView);
        lottieOverlayLogin = findViewById(R.id.lottieOverlayLogin);

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

        SharedPreferences prefs = getSharedPreferences("user_prefs", MODE_PRIVATE);
        String role = prefs.getString("user_role", "");
        long teacherId = prefs.getLong("teacher_id", -1);
        long studentId = prefs.getLong("student_id", -1);
        if (("teacher".equals(role) && teacherId != -1) || ("student".equals(role) && studentId != -1)) {
            navigated = true;
            Intent intent = new Intent(LoginActivity.this, MainActivity.class);
            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
            startActivity(intent);
            finish();
        }
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
            
            SharedPreferences prefs = getSharedPreferences("user_prefs", MODE_PRIVATE);
            SharedPreferences.Editor ed = prefs.edit();
            ed.putString("user_role", success.role);
            if ("teacher".equals(success.role)) {
                ed.putLong("teacher_id", success.userId).remove("student_id");
            } else {
                ed.putLong("student_id", success.userId).remove("teacher_id");
                // 学生默认进入首页，避免加载教师专用页面
                getSharedPreferences("ui_prefs", MODE_PRIVATE).edit()
                        .putInt("nav_selected_id", R.id.nav_home).apply();
            }
            ed.apply();
            
            // 记住密码逻辑
            if (rememberPasswordCheckBox.isChecked()) {
                prefs.edit()
                    .putBoolean("remember_password", true)
                    .putString("saved_username", emailEditText.getText().toString().trim())
                    .putString("saved_password", passwordEditText.getText().toString().trim())
                    .apply();
            } else {
                prefs.edit()
                    .putBoolean("remember_password", false)
                    .remove("saved_username")
                    .remove("saved_password")
                    .apply();
            }
            // 显示登录成功 Lottie（telegram），执行1次后退出动画再跳转
            if (lottieLoginView != null && lottieOverlayLogin != null) {
                loginButton.setEnabled(false);
                lottieOverlayLogin.setVisibility(View.VISIBLE);
                lottieLoginView.setAnimation("lottie/telegram.json");
                lottieLoginView.setRepeatCount(0);
                lottieLoginView.addAnimatorListener(new AnimatorListenerAdapter() {
                    @Override
                    public void onAnimationEnd(Animator animation) {
                        lottieOverlayLogin.setVisibility(View.GONE);
                if (!navigated) {
                    navigated = true;
                    Intent intent = new Intent(LoginActivity.this, MainActivity.class);
                    intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
                    startActivity(intent);
                    finish();
                }
                    }
                });
                lottieLoginView.playAnimation();
            } else {
                // 回退：无动画视图则直接跳转
                if (!navigated) {
                    navigated = true;
                    Intent intent = new Intent(LoginActivity.this, MainActivity.class);
                    intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
                    startActivity(intent);
                    finish();
                }
            }
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

        // 自动填充记住的用户名和密码
        SharedPreferences prefs = getSharedPreferences("user_prefs", MODE_PRIVATE);
        boolean rememberPassword = prefs.getBoolean("remember_password", false);
        if (rememberPassword) {
            String savedUsername = prefs.getString("saved_username", "");
            String savedPassword = prefs.getString("saved_password", "");
            emailEditText.setText(savedUsername);
            passwordEditText.setText(savedPassword);
            rememberPasswordCheckBox.setChecked(true);
        }

        // 调用ViewModel进行登录
        loginViewModel.login(username, password);
    }
    
    // 移除 onStart 自动跳转，避免多次回调导致循环
}
