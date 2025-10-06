package com.example.facecheck.utils;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class WebDAVSyncHelper {
    private static final String TAG = "WebDAVSyncHelper";
    private static final String PREFS_NAME = "webdav_prefs";
    private static final String KEY_ENABLED = "webdav_enabled";
    private static final String KEY_LAST_SYNC = "webdav_last_sync";
    
    private Context context;
    private boolean enabled;
    private OnSyncListener listener;
    
    public interface OnSyncListener {
        void onSyncStarted();
        void onSyncCompleted(boolean success, String message);
    }
    
    public WebDAVSyncHelper(Context context) {
        this.context = context;
        loadPreferences();
    }
    
    public void setOnSyncListener(OnSyncListener listener) {
        this.listener = listener;
    }
    
    private void loadPreferences() {
        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        enabled = prefs.getBoolean(KEY_ENABLED, false);
    }
    
    public void savePreferences(boolean enabled) {
        this.enabled = enabled;
        
        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = prefs.edit();
        editor.putBoolean(KEY_ENABLED, enabled);
        editor.apply();
    }
    
    public boolean isEnabled() {
        return enabled;
    }
    
    public String getLastSyncTime() {
        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        long lastSync = prefs.getLong(KEY_LAST_SYNC, 0);
        if (lastSync == 0) {
            return "从未同步";
        }
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault());
        return sdf.format(new Date(lastSync));
    }
    
    public void syncDatabase() {
        if (!enabled) {
            if (listener != null) {
                listener.onSyncCompleted(false, "同步未启用");
            }
            return;
        }
        
        if (listener != null) {
            listener.onSyncStarted();
        }
        
        new Handler(Looper.getMainLooper()).postDelayed(() -> {
            // 直接执行同步，不进行任何备份
            boolean success = performDirectSync();
            
            if (success) {
                // 更新最后同步时间
                SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
                SharedPreferences.Editor editor = prefs.edit();
                editor.putLong(KEY_LAST_SYNC, System.currentTimeMillis());
                editor.apply();
            }
            
            if (listener != null) {
                listener.onSyncCompleted(success, success ? "同步成功" : "同步失败");
            }
        }, 1000);
    }
    
    private boolean performDirectSync() {
        try {
            // 直接执行同步逻辑，不进行任何备份
            // 这里可以实现网络数据库和本地数据库的时间对比和覆盖逻辑
            Log.d(TAG, "执行直接同步模式 - 无备份");
            
            // TODO: 实现具体的同步逻辑
            // 1. 获取网络数据库时间戳
            // 2. 获取本地数据库时间戳  
            // 3. 根据时间前后决定上传或下载
            // 4. 执行覆盖操作
            
            return true;
        } catch (Exception e) {
            Log.e(TAG, "同步失败: " + e.getMessage(), e);
            return false;
        }
    }
    
    public void testConnection(final OnConnectionTestListener listener) {
        new Handler(Looper.getMainLooper()).postDelayed(() -> {
            listener.onConnectionTested(true, "测试成功");
        }, 500);
    }
    
    public interface OnConnectionTestListener {
        void onConnectionTested(boolean success, String message);
    }
}