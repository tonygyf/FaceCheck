package com.example.facecheck.ui.settings;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import com.example.facecheck.R;
import com.example.facecheck.webdav.WebDavManager;
import com.example.facecheck.utils.AsyncExecutor;

public class MoreSettingsActivity extends AppCompatActivity {

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_more_settings);

        findViewById(R.id.item_sync).setOnClickListener(v -> showWebDavConfigDialog());
        findViewById(R.id.item_appearance).setOnClickListener(v -> Toast.makeText(this, "外观设置在主设置页切换", Toast.LENGTH_SHORT).show());
        findViewById(R.id.item_reading).setOnClickListener(v -> Toast.makeText(this, "阅读设置待后续完善", Toast.LENGTH_SHORT).show());
        findViewById(R.id.item_tts).setOnClickListener(v -> Toast.makeText(this, "朗读设置待后续完善", Toast.LENGTH_SHORT).show());
        findViewById(R.id.item_translate).setOnClickListener(v -> Toast.makeText(this, "翻译设置待后续完善", Toast.LENGTH_SHORT).show());
        findViewById(R.id.item_ai).setOnClickListener(v -> Toast.makeText(this, "AI 设置待后续完善", Toast.LENGTH_SHORT).show());
        findViewById(R.id.item_storage).setOnClickListener(v -> Toast.makeText(this, "存储设置（含缓存）待后续整合", Toast.LENGTH_SHORT).show());
        findViewById(R.id.item_advanced).setOnClickListener(v -> Toast.makeText(this, "高级设置待后续完善", Toast.LENGTH_SHORT).show());
        findViewById(R.id.item_about_fc).setOnClickListener(v -> {
            String version = "";
            try {
                version = getPackageManager().getPackageInfo(getPackageName(), 0).versionName;
            } catch (Throwable ignore) {}
            new AlertDialog.Builder(this)
                    .setTitle("关于 FaceCheck")
                    .setMessage("版本：" + version)
                    .setPositiveButton("确定", null)
                    .show();
        });
    }

    private void showWebDavConfigDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        View dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_webdav_config, null);
        builder.setView(dialogView);

        TextView statusText = dialogView.findViewById(R.id.tv_connection_status);
        Button testButton = dialogView.findViewById(R.id.btn_test_connection);
        Button cancelButton = dialogView.findViewById(R.id.btn_cancel);
        Button saveButton = dialogView.findViewById(R.id.btn_save);

        android.widget.EditText urlInput = dialogView.findViewById(R.id.et_webdav_url);
        android.widget.EditText usernameInput = dialogView.findViewById(R.id.et_webdav_username);
        android.widget.EditText passwordInput = dialogView.findViewById(R.id.et_webdav_password);

        SharedPreferences prefs = getSharedPreferences("webdav_prefs", Context.MODE_PRIVATE);
        urlInput.setText(prefs.getString("webdav_url", ""));
        usernameInput.setText(prefs.getString("webdav_username", ""));
        passwordInput.setText(prefs.getString("webdav_password", ""));

        testButton.setOnClickListener(v -> {
            String url = urlInput.getText().toString().trim();
            String user = usernameInput.getText().toString().trim();
            String pass = passwordInput.getText().toString().trim();
            if (url.isEmpty() || user.isEmpty() || pass.isEmpty()) {
                statusText.setText("请填写所有字段");
                statusText.setTextColor(getResources().getColor(android.R.color.holo_red_dark));
                statusText.setVisibility(View.VISIBLE);
                return;
            }
            statusText.setText("正在测试连接...");
            statusText.setTextColor(getResources().getColor(android.R.color.black));
            statusText.setVisibility(View.VISIBLE);
            AsyncExecutor exec = new AsyncExecutor();
            exec.run(() -> {
                WebDavManager tmp = new WebDavManager(this, url, user, pass);
                return tmp.testConnection();
            }, ok -> {
                statusText.setText(ok ? "连接成功！" : "连接失败，请检查配置");
                statusText.setTextColor(getResources().getColor(ok ? android.R.color.holo_green_dark : android.R.color.holo_red_dark));
            }, t -> {
                statusText.setText("连接失败: " + t.getMessage());
                statusText.setTextColor(getResources().getColor(android.R.color.holo_red_dark));
            });
        });

        cancelButton.setOnClickListener(v -> {
            AlertDialog d = builder.create();
            d.dismiss();
        });

        saveButton.setOnClickListener(v -> {
            String url = urlInput.getText().toString().trim();
            String user = usernameInput.getText().toString().trim();
            String pass = passwordInput.getText().toString().trim();
            if (url.isEmpty() || user.isEmpty() || pass.isEmpty()) {
                Toast.makeText(this, "请填写所有字段", Toast.LENGTH_SHORT).show();
                return;
            }
            prefs.edit()
                    .putString("webdav_url", url)
                    .putString("webdav_username", user)
                    .putString("webdav_password", pass)
                    .apply();
            Toast.makeText(this, "WebDAV配置已保存", Toast.LENGTH_SHORT).show();
            AlertDialog d = builder.create();
            d.dismiss();
        });

        builder.create().show();
    }
}
