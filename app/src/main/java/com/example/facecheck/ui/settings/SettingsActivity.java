package com.example.facecheck.ui.settings;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.core.content.ContextCompat;

import com.example.facecheck.R;
import com.example.facecheck.database.DatabaseHelper;
import com.example.facecheck.sync.SyncManager;
import com.example.facecheck.utils.AsyncExecutor;
import com.example.facecheck.webdav.WebDavManager;
import com.example.facecheck.utils.CacheManager;

public class SettingsActivity extends AppCompatActivity {

    // 主题
    private Button btnThemeSystem, btnThemeDark, btnThemeLight;

    // WebDAV 与同步
    private Switch webdavSwitch;
    private Button webdavConfigButton;
    private Button syncNowButton;
    private TextView webdavStatusTextView;
    private ProgressBar progressBar;

    // 缓存设置
    private CacheManager cacheManager;
    private TextView cacheStatusTextView;
    private Button btnClearAllCache, btnClearImageCache, btnClearTempCache;
    private ProgressBar progressBarCache;

    // 关于
    private View itemAbout;
    private long lastAboutClickTime = 0;
    private int aboutClickCount = 0;

    // 数据与同步
    private DatabaseHelper dbHelper;
    private WebDavManager webDavManager;
    private SyncManager syncManager;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);

        // 主题按钮
        btnThemeSystem = findViewById(R.id.btn_theme_system);
        btnThemeDark = findViewById(R.id.btn_theme_dark);
        btnThemeLight = findViewById(R.id.btn_theme_light);
        initThemeFromPrefs();
        btnThemeSystem.setOnClickListener(v -> applyThemeMode("system"));
        btnThemeDark.setOnClickListener(v -> applyThemeMode("dark"));
        btnThemeLight.setOnClickListener(v -> applyThemeMode("light"));

        // WebDAV 与同步
        webdavSwitch = findViewById(R.id.switch_webdav);
        webdavConfigButton = findViewById(R.id.btn_webdav_config);
        syncNowButton = findViewById(R.id.btn_sync_now);
        webdavStatusTextView = findViewById(R.id.tv_webdav_status);
        progressBar = findViewById(R.id.progress_bar);

        webdavSwitch.setOnCheckedChangeListener((buttonView, isChecked) -> {
            SharedPreferences prefs = getSharedPreferences("webdav_prefs", Context.MODE_PRIVATE);
            prefs.edit().putBoolean("webdav_enabled", isChecked).apply();
            updateWebDavStatus();
        });

        webdavConfigButton.setOnClickListener(v -> showWebDavConfigDialog());
        syncNowButton.setOnClickListener(v -> syncWithWebDav());

        dbHelper = new DatabaseHelper(this);
        initWebDavManager();
        updateWebDavStatus();

        // 缓存设置（直接在设置页提供操作）
        cacheStatusTextView = findViewById(R.id.tv_cache_status);
        btnClearAllCache = findViewById(R.id.btn_clear_all_cache);
        btnClearImageCache = findViewById(R.id.btn_clear_image_cache);
        btnClearTempCache = findViewById(R.id.btn_clear_temp_cache);
        progressBarCache = findViewById(R.id.progress_bar_cache);

        initCacheManager();
        loadCacheSize();
        btnClearAllCache.setOnClickListener(v -> clearAllCache());
        btnClearImageCache.setOnClickListener(v -> clearImageCache());
        btnClearTempCache.setOnClickListener(v -> clearTempCache());

        // 关于入口
        itemAbout = findViewById(R.id.item_about);
        itemAbout.setOnClickListener(v -> {
            long currentTime = System.currentTimeMillis();

            if (currentTime - lastAboutClickTime < 500) {
                aboutClickCount++;
            } else {
                aboutClickCount = 1;
            }
            lastAboutClickTime = currentTime;

            if (aboutClickCount == 3) {
                SharedPreferences demoPrefs = getSharedPreferences("demo_mode_prefs", MODE_PRIVATE);
                boolean currentState = demoPrefs.getBoolean("demo_mode_enabled", false);
                boolean newState = !currentState;
                demoPrefs.edit().putBoolean("demo_mode_enabled", newState).apply();

                String msg = newState ? "演示模式已启用" : "演示模式已关闭";
                Toast.makeText(this, msg, Toast.LENGTH_SHORT).show();
                aboutClickCount = 0;
            } else if (aboutClickCount == 1) {
                String version = "";
                try {
                    version = getPackageManager().getPackageInfo(getPackageName(), 0).versionName;
                } catch (Throwable ignore) {
                }
                new AlertDialog.Builder(this)
                        .setTitle("关于 FaceCheck")
                        .setMessage("版本：" + version)
                        .setPositiveButton("确定", null)
                        .show();
            }
        });
    }

    // ===== 主题 =====
    private void initThemeFromPrefs() {
        SharedPreferences prefs = getSharedPreferences("settings_prefs", MODE_PRIVATE);
        String mode = prefs.getString("theme_mode", "system");
        updateThemeButtons(mode);
    }

    private void applyThemeMode(String mode) {
        SharedPreferences prefs = getSharedPreferences("settings_prefs", MODE_PRIVATE);
        prefs.edit().putString("theme_mode", mode).apply();

        switch (mode) {
            case "dark":
                AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES);
                break;
            case "light":
                AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO);
                break;
            default:
                AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM);
                break;
        }
        updateThemeButtons(mode);
    }

    private void updateThemeButtons(String mode) {
        boolean sys = "system".equals(mode);
        boolean dark = "dark".equals(mode);
        boolean light = "light".equals(mode);

        btnThemeSystem.setBackgroundResource(
                sys ? R.drawable.bg_theme_option_selected : R.drawable.bg_theme_option_unselected);
        btnThemeDark.setBackgroundResource(
                dark ? R.drawable.bg_theme_option_selected : R.drawable.bg_theme_option_unselected);
        btnThemeLight.setBackgroundResource(
                light ? R.drawable.bg_theme_option_selected : R.drawable.bg_theme_option_unselected);

        // 仅通过背景体现选中态
    }

    // ===== WebDAV 与同步 =====
    private void initWebDavManager() {
        SharedPreferences webdavPrefs = getSharedPreferences("webdav_prefs", Context.MODE_PRIVATE);
        String serverUrl = webdavPrefs.getString("webdav_url", "");
        String username = webdavPrefs.getString("webdav_username", "");
        String password = webdavPrefs.getString("webdav_password", "");

        if (!serverUrl.isEmpty() && !username.isEmpty() && !password.isEmpty()) {
            webDavManager = new WebDavManager(this, serverUrl, username, password);
            syncManager = new SyncManager(this, dbHelper, webDavManager);
        } else {
            webDavManager = null;
            syncManager = null;
        }
    }

    private void updateWebDavStatus() {
        SharedPreferences webdavPrefs = getSharedPreferences("webdav_prefs", Context.MODE_PRIVATE);
        boolean isEnabled = webdavPrefs.getBoolean("webdav_enabled", false);
        String serverUrl = webdavPrefs.getString("webdav_url", "");

        webdavSwitch.setChecked(isEnabled);

        if (isEnabled && !serverUrl.isEmpty() && webDavManager != null) {
            webdavStatusTextView.setText("WebDAV 状态: 已启用");
            webdavConfigButton.setEnabled(true);
            syncNowButton.setEnabled(true);
        } else if (isEnabled) {
            webdavStatusTextView.setText("WebDAV 状态: 未配置");
            webdavConfigButton.setEnabled(true);
            syncNowButton.setEnabled(false);
        } else {
            webdavStatusTextView.setText("WebDAV 状态: 已禁用");
            webdavConfigButton.setEnabled(false);
            syncNowButton.setEnabled(false);
        }
    }

    private void showWebDavConfigDialog() {
        View dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_webdav_config, null);
        AlertDialog dialog = new AlertDialog.Builder(this)
                .setView(dialogView)
                .create();

        TextView statusText = dialogView.findViewById(R.id.tv_connection_status);
        Button testButton = dialogView.findViewById(R.id.btn_test_connection);
        Button cancelButton = dialogView.findViewById(R.id.btn_cancel);
        Button saveButton = dialogView.findViewById(R.id.btn_save);

        EditText urlInput = dialogView.findViewById(R.id.et_webdav_url);
        EditText usernameInput = dialogView.findViewById(R.id.et_webdav_username);
        EditText passwordInput = dialogView.findViewById(R.id.et_webdav_password);

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
                statusText.setTextColor(ContextCompat.getColor(this, android.R.color.holo_red_dark));
                statusText.setVisibility(View.VISIBLE);
                return;
            }
            statusText.setText("正在测试连接...");
            statusText.setTextColor(ContextCompat.getColor(this, android.R.color.black));
            statusText.setVisibility(View.VISIBLE);
            AsyncExecutor exec = new AsyncExecutor();
            exec.run(() -> {
                WebDavManager tmp = new WebDavManager(this, url, user, pass);
                return tmp.testConnection();
            }, ok -> {
                statusText.setText(ok ? "连接成功！" : "连接失败，请检查配置");
                statusText.setTextColor(ContextCompat.getColor(this,
                        ok ? android.R.color.holo_green_dark : android.R.color.holo_red_dark));
            }, t -> {
                statusText.setText("连接失败: " + t.getMessage());
                statusText.setTextColor(ContextCompat.getColor(this, android.R.color.holo_red_dark));
            });
        });

        cancelButton.setOnClickListener(v -> dialog.dismiss());

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
            // 重新初始化管理器并刷新状态
            initWebDavManager();
            updateWebDavStatus();
            dialog.dismiss();
        });
        dialog.show();
    }

    private void syncWithWebDav() {
        if (syncManager == null) {
            Toast.makeText(this, "WebDAV未配置", Toast.LENGTH_SHORT).show();
            return;
        }

        progressBar.setVisibility(View.VISIBLE);
        AsyncExecutor exec = new AsyncExecutor();
        exec.run(() -> syncManager.performSync(), success -> {
            progressBar.setVisibility(View.GONE);
            Toast.makeText(this, success ? "同步成功" : "同步失败，请检查网络连接和WebDAV配置", Toast.LENGTH_SHORT).show();
        }, t -> {
            progressBar.setVisibility(View.GONE);
            Toast.makeText(this, "同步出错: " + t.getMessage(), Toast.LENGTH_SHORT).show();
        });
    }

    // ===== 缓存管理 =====
    private void initCacheManager() {
        cacheManager = new CacheManager(this);
    }

    private void loadCacheSize() {
        cacheStatusTextView.setText("缓存: 正在计算大小...");
        cacheManager.getCacheSize(new CacheManager.CacheSizeCallback() {
            @Override
            public void onSizeCalculated(CacheManager.CacheSizeInfo sizeInfo) {
                runOnUiThread(() -> {
                    updateCacheSizeDisplay(sizeInfo);
                });
            }

            @Override
            public void onError(String error) {
                runOnUiThread(() -> cacheStatusTextView.setText("缓存: 获取失败"));
            }
        });
    }

    private void updateCacheSizeDisplay(CacheManager.CacheSizeInfo sizeInfo) {
        String cacheInfo = String.format("缓存: %s (图片: %s)",
                formatFileSize(sizeInfo.totalSize),
                formatFileSize(sizeInfo.imageCacheSize));
        cacheStatusTextView.setText(cacheInfo);
    }

    private void clearAllCache() {
        showCacheCleaningProgress(true);
        cacheManager.clearAllCache(new CacheManager.CacheCleanCallback() {
            @Override
            public void onProgress(int progress, String message) {
                runOnUiThread(() -> {
                    progressBarCache.setProgress(progress);
                    cacheStatusTextView.setText(message);
                });
            }

            @Override
            public void onComplete(long freedSpace, String summary) {
                runOnUiThread(() -> {
                    showCacheCleaningProgress(false);
                    cacheStatusTextView.setText(summary);
                    Toast.makeText(SettingsActivity.this, summary, Toast.LENGTH_LONG).show();
                    loadCacheSize();
                });
            }

            @Override
            public void onError(String error) {
                runOnUiThread(() -> {
                    showCacheCleaningProgress(false);
                    cacheStatusTextView.setText("清理失败: " + error);
                    Toast.makeText(SettingsActivity.this, "清理失败: " + error, Toast.LENGTH_SHORT).show();
                });
            }
        });
    }

    private void clearImageCache() {
        showCacheCleaningProgress(true);
        cacheManager.clearImageCache(new CacheManager.CacheCleanCallback() {
            @Override
            public void onProgress(int progress, String message) {
                runOnUiThread(() -> {
                    progressBarCache.setProgress(progress);
                    cacheStatusTextView.setText(message);
                });
            }

            @Override
            public void onComplete(long freedSpace, String summary) {
                runOnUiThread(() -> {
                    showCacheCleaningProgress(false);
                    cacheStatusTextView.setText(summary);
                    Toast.makeText(SettingsActivity.this, summary, Toast.LENGTH_LONG).show();
                    loadCacheSize();
                });
            }

            @Override
            public void onError(String error) {
                runOnUiThread(() -> {
                    showCacheCleaningProgress(false);
                    cacheStatusTextView.setText("清理失败: " + error);
                    Toast.makeText(SettingsActivity.this, "清理失败: " + error, Toast.LENGTH_SHORT).show();
                });
            }
        });
    }

    private void clearTempCache() {
        showCacheCleaningProgress(true);
        cacheManager.clearTempCache(new CacheManager.CacheCleanCallback() {
            @Override
            public void onProgress(int progress, String message) {
                runOnUiThread(() -> {
                    progressBarCache.setProgress(progress);
                    cacheStatusTextView.setText(message);
                });
            }

            @Override
            public void onComplete(long freedSpace, String summary) {
                runOnUiThread(() -> {
                    showCacheCleaningProgress(false);
                    cacheStatusTextView.setText(summary);
                    Toast.makeText(SettingsActivity.this, summary, Toast.LENGTH_LONG).show();
                    loadCacheSize();
                });
            }

            @Override
            public void onError(String error) {
                runOnUiThread(() -> {
                    showCacheCleaningProgress(false);
                    cacheStatusTextView.setText("清理失败: " + error);
                    Toast.makeText(SettingsActivity.this, "清理失败: " + error, Toast.LENGTH_SHORT).show();
                });
            }
        });
    }

    private void showCacheCleaningProgress(boolean show) {
        progressBarCache.setVisibility(show ? View.VISIBLE : View.GONE);
        if (show) {
            progressBarCache.setIndeterminate(false);
            progressBarCache.setProgress(0);
        }
        btnClearAllCache.setEnabled(!show);
        btnClearImageCache.setEnabled(!show);
        btnClearTempCache.setEnabled(!show);
    }

    private String formatFileSize(long size) {
        if (size <= 0)
            return "0 B";
        final String[] units = new String[] { "B", "KB", "MB", "GB", "TB" };
        int digitGroups = (int) (Math.log10(size) / Math.log10(1024));
        return new java.text.DecimalFormat("#,##0.#").format(size / Math.pow(1024, digitGroups)) + " "
                + units[digitGroups];
    }
}
