package com.example.facecheck;

import android.app.Application;
import android.os.StrictMode;
import android.util.Log;
import android.content.SharedPreferences;
import androidx.appcompat.app.AppCompatDelegate;
import java.lang.reflect.Method;

public class FaceCheckApp extends Application {
    @Override
    public void onCreate() {
        super.onCreate();
        initAmapPrivacy();
        try {
            SharedPreferences prefs = getSharedPreferences("settings_prefs", MODE_PRIVATE);
            String mode = prefs.getString("theme_mode", "system");
            if ("dark".equals(mode)) {
                AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES);
            } else if ("light".equals(mode)) {
                AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO);
            } else {
                AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM);
            }
        } catch (Throwable ignore) {}
        // 后台修复学生sid唯一性
        new Thread(() -> {
            try {
                com.example.facecheck.database.DatabaseHelper dbh = new com.example.facecheck.database.DatabaseHelper(this);
                dbh.ensureStudentSidUniqueIndex();
            } catch (Throwable t) {
                Log.w("FaceCheckApp", "修复学生sid唯一性失败: " + t.getMessage());
            }
        }).start();
        if (BuildConfig.DEBUG) {
            try {
                StrictMode.setThreadPolicy(new StrictMode.ThreadPolicy.Builder()
                        .detectAll()
                        .penaltyLog()
                        .build());
                StrictMode.setVmPolicy(new StrictMode.VmPolicy.Builder()
                        .detectAll()
                        .penaltyLog()
                        .build());
            } catch (Throwable t) {
                Log.w("FaceCheckApp", "StrictMode setup failed: " + t.getMessage());
            }

            // 可选：在调试构建禁用部分远程上报，防止噪音或阻塞
            try {
                Class<?> analyticsCls = Class.forName("com.google.firebase.analytics.FirebaseAnalytics");
                Object inst = analyticsCls.getMethod("getInstance", android.content.Context.class).invoke(null, this);
                analyticsCls.getMethod("setAnalyticsCollectionEnabled", boolean.class).invoke(inst, false);
            } catch (Throwable ignore) {}

            try {
                Class<?> crashCls = Class.forName("com.google.firebase.crashlytics.FirebaseCrashlytics");
                Object inst = crashCls.getMethod("getInstance").invoke(null);
                crashCls.getMethod("setCrashlyticsCollectionEnabled", boolean.class).invoke(inst, false);
            } catch (Throwable ignore) {}
        }
    }

    private void initAmapPrivacy() {
        // 高德地图/检索/定位 SDK 都要求在调用任何接口前先完成隐私合规设置，否则会直接抛异常。
        setAmapPrivacyByReflection("com.amap.api.maps2d.MapsInitializer");
        setAmapPrivacyByReflection("com.amap.api.services.core.ServiceSettings");
        setAmapPrivacyByReflection("com.amap.api.location.AMapLocationClient");
    }

    private void setAmapPrivacyByReflection(String className) {
        try {
            Class<?> clazz = Class.forName(className);
            invokeStaticIfExists(clazz, "updatePrivacyShow",
                    new Class<?>[]{android.content.Context.class, boolean.class, boolean.class},
                    new Object[]{this, true, true});
            invokeStaticIfExists(clazz, "updatePrivacyAgree",
                    new Class<?>[]{android.content.Context.class, boolean.class},
                    new Object[]{this, true});
        } catch (Throwable t) {
            Log.w("FaceCheckApp", "AMap privacy init skipped for " + className + ": " + t.getMessage());
        }
    }

    private void invokeStaticIfExists(Class<?> clazz, String methodName, Class<?>[] argTypes, Object[] args) {
        try {
            Method m = clazz.getMethod(methodName, argTypes);
            m.invoke(null, args);
        } catch (NoSuchMethodException ignore) {
            // 不同 SDK 版本方法签名可能变化，忽略即可
        } catch (Throwable t) {
            Log.w("FaceCheckApp", "AMap privacy method failed: " + clazz.getSimpleName() + "." + methodName);
        }
    }
}
