package com.example.facecheck.ui.settings;

import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import com.example.facecheck.R;
import com.example.facecheck.utils.CacheManager;

/**
 * 缓存设置页面
 * 提供缓存清理功能
 */
public class CacheSettingsActivity extends AppCompatActivity {
    
    private CacheManager cacheManager;
    private TextView tvCacheSize;
    private TextView tvStatus;
    private ProgressBar progressBar;
    private Button btnClearAllCache;
    private Button btnClearImageCache;
    private Button btnClearTempCache;
    private View loadingView;
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_cache_settings);
        
        initViews();
        initToolbar();
        initCacheManager();
        loadCacheSize();
    }
    
    private void initViews() {
        tvCacheSize = findViewById(R.id.tvCacheSize);
        tvStatus = findViewById(R.id.tvStatus);
        progressBar = findViewById(R.id.progressBar);
        btnClearAllCache = findViewById(R.id.btnClearAllCache);
        btnClearImageCache = findViewById(R.id.btnClearImageCache);
        btnClearTempCache = findViewById(R.id.btnClearTempCache);
        loadingView = findViewById(R.id.loadingView);
        
        btnClearAllCache.setOnClickListener(v -> clearAllCache());
        btnClearImageCache.setOnClickListener(v -> clearImageCache());
        btnClearTempCache.setOnClickListener(v -> clearTempCache());
    }
    
    private void initToolbar() {
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setTitle("缓存管理");
        }
        toolbar.setNavigationOnClickListener(v -> finish());
    }
    
    private void initCacheManager() {
        cacheManager = new CacheManager(this);
    }
    
    private void loadCacheSize() {
        showLoading(true);
        tvStatus.setText("正在计算缓存大小...");
        
        cacheManager.getCacheSize(new CacheManager.CacheSizeCallback() {
            @Override
            public void onSizeCalculated(CacheManager.CacheSizeInfo sizeInfo) {
                runOnUiThread(() -> {
                    showLoading(false);
                    updateCacheSizeDisplay(sizeInfo);
                    tvStatus.setText("缓存大小计算完成");
                });
            }
            
            @Override
            public void onError(String error) {
                runOnUiThread(() -> {
                    showLoading(false);
                    tvStatus.setText("计算失败: " + error);
                    Toast.makeText(CacheSettingsActivity.this, "计算缓存大小失败: " + error, Toast.LENGTH_SHORT).show();
                });
            }
        });
    }
    
    private void updateCacheSizeDisplay(CacheManager.CacheSizeInfo sizeInfo) {
        StringBuilder sb = new StringBuilder();
        sb.append("总缓存大小: ").append(formatFileSize(sizeInfo.totalSize)).append("\n\n");
        sb.append("图片缓存: ").append(formatFileSize(sizeInfo.imageCacheSize)).append("\n");
        sb.append("应用缓存: ").append(formatFileSize(sizeInfo.appCacheSize)).append("\n");
        sb.append("临时文件: ").append(formatFileSize(sizeInfo.tempFileSize)).append("\n");
        sb.append("日志文件: ").append(formatFileSize(sizeInfo.logFileSize));
        
        tvCacheSize.setText(sb.toString());
    }
    
    private void clearAllCache() {
        showCleaningProgress(true);
        tvStatus.setText("正在清理所有缓存...");
        
        cacheManager.clearAllCache(new CacheManager.CacheCleanCallback() {
            @Override
            public void onProgress(int progress, String message) {
                runOnUiThread(() -> {
                    progressBar.setProgress(progress);
                    tvStatus.setText(message);
                });
            }
            
            @Override
            public void onComplete(long freedSpace, String summary) {
                runOnUiThread(() -> {
                    showCleaningProgress(false);
                    tvStatus.setText(summary);
                    Toast.makeText(CacheSettingsActivity.this, summary, Toast.LENGTH_LONG).show();
                    
                    // 重新计算缓存大小
                    loadCacheSize();
                });
            }
            
            @Override
            public void onError(String error) {
                runOnUiThread(() -> {
                    showCleaningProgress(false);
                    tvStatus.setText("清理失败: " + error);
                    Toast.makeText(CacheSettingsActivity.this, "清理失败: " + error, Toast.LENGTH_SHORT).show();
                });
            }
        });
    }
    
    private void clearImageCache() {
        showCleaningProgress(true);
        tvStatus.setText("正在清理图片缓存...");
        
        cacheManager.clearImageCache(new CacheManager.CacheCleanCallback() {
            @Override
            public void onProgress(int progress, String message) {
                runOnUiThread(() -> {
                    progressBar.setProgress(progress);
                    tvStatus.setText(message);
                });
            }
            
            @Override
            public void onComplete(long freedSpace, String summary) {
                runOnUiThread(() -> {
                    showCleaningProgress(false);
                    tvStatus.setText(summary);
                    Toast.makeText(CacheSettingsActivity.this, summary, Toast.LENGTH_LONG).show();
                    
                    // 重新计算缓存大小
                    loadCacheSize();
                });
            }
            
            @Override
            public void onError(String error) {
                runOnUiThread(() -> {
                    showCleaningProgress(false);
                    tvStatus.setText("清理失败: " + error);
                    Toast.makeText(CacheSettingsActivity.this, "清理失败: " + error, Toast.LENGTH_SHORT).show();
                });
            }
        });
    }
    
    private void clearTempCache() {
        showCleaningProgress(true);
        tvStatus.setText("正在清理临时缓存...");
        
        cacheManager.clearTempCache(new CacheManager.CacheCleanCallback() {
            @Override
            public void onProgress(int progress, String message) {
                runOnUiThread(() -> {
                    progressBar.setProgress(progress);
                    tvStatus.setText(message);
                });
            }
            
            @Override
            public void onComplete(long freedSpace, String summary) {
                runOnUiThread(() -> {
                    showCleaningProgress(false);
                    tvStatus.setText(summary);
                    Toast.makeText(CacheSettingsActivity.this, summary, Toast.LENGTH_LONG).show();
                    
                    // 重新计算缓存大小
                    loadCacheSize();
                });
            }
            
            @Override
            public void onError(String error) {
                runOnUiThread(() -> {
                    showCleaningProgress(false);
                    tvStatus.setText("清理失败: " + error);
                    Toast.makeText(CacheSettingsActivity.this, "清理失败: " + error, Toast.LENGTH_SHORT).show();
                });
            }
        });
    }
    
    private void showLoading(boolean show) {
        loadingView.setVisibility(show ? View.VISIBLE : View.GONE);
        btnClearAllCache.setEnabled(!show);
        btnClearImageCache.setEnabled(!show);
        btnClearTempCache.setEnabled(!show);
    }
    
    private void showCleaningProgress(boolean show) {
        progressBar.setVisibility(show ? View.VISIBLE : View.GONE);
        btnClearAllCache.setEnabled(!show);
        btnClearImageCache.setEnabled(!show);
        btnClearTempCache.setEnabled(!show);
        
        if (show) {
            progressBar.setProgress(0);
        }
    }
    
    private String formatFileSize(long size) {
        if (size <= 0) return "0 B";
        
        final String[] units = new String[]{"B", "KB", "MB", "GB", "TB"};
        int digitGroups = (int) (Math.log10(size) / Math.log10(1024));
        
        return new java.text.DecimalFormat("#,##0.#").format(size / Math.pow(1024, digitGroups)) + " " + units[digitGroups];
    }
    
    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (cacheManager != null) {
            cacheManager.cleanup();
        }
    }
}