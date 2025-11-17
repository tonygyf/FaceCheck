package com.example.facecheck.fragments;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.fragment.app.Fragment;

import com.example.facecheck.R;
import com.google.android.material.bottomsheet.BottomSheetDialog;

public class SettingsFragment extends Fragment {

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_settings, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        // 主题按钮
        Button btnThemeSystem = view.findViewById(R.id.btn_theme_system);
        Button btnThemeDark = view.findViewById(R.id.btn_theme_dark);
        Button btnThemeLight = view.findViewById(R.id.btn_theme_light);
        initThemeFromPrefs();
        btnThemeSystem.setOnClickListener(v -> applyThemeMode("system"));
        btnThemeDark.setOnClickListener(v -> applyThemeMode("dark"));
        btnThemeLight.setOnClickListener(v -> applyThemeMode("light"));

        // 更多设置入口
        View itemMoreSettings = view.findViewById(R.id.item_more_settings);
        itemMoreSettings.setOnClickListener(v -> showMoreSettingsBottomSheet());

        // 关于 FaceCheck（移到主页面）
        View itemAbout = view.findViewById(R.id.item_about);
        itemAbout.setOnClickListener(v -> {
            String version = "";
            try {
                version = requireContext().getPackageManager().getPackageInfo(requireContext().getPackageName(), 0).versionName;
            } catch (Throwable ignore) {}
            new androidx.appcompat.app.AlertDialog.Builder(requireContext())
                    .setTitle("关于 FaceCheck")
                    .setMessage("版本：" + version + "\n\n人脸考勤·课堂管理·快速打卡\n简约现代的考勤体验")
                    .setPositiveButton("确定", null)
                    .show();
        });
    }

    private void initThemeFromPrefs() {
        SharedPreferences prefs = requireContext().getSharedPreferences("settings_prefs", Context.MODE_PRIVATE);
        String mode = prefs.getString("theme_mode", "system");
        updateThemeButtons(mode);
    }

    private void applyThemeMode(String mode) {
        SharedPreferences prefs = requireContext().getSharedPreferences("settings_prefs", Context.MODE_PRIVATE);
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
        requireContext().getSharedPreferences("ui_prefs", Context.MODE_PRIVATE)
                .edit().putInt("nav_selected_id", R.id.nav_settings).apply();
    }

    private void updateThemeButtons(String mode) {
        boolean sys = "system".equals(mode);
        boolean dark = "dark".equals(mode);
        boolean light = "light".equals(mode);

        if (getView() == null) return;
        Button btnThemeSystem = getView().findViewById(R.id.btn_theme_system);
        Button btnThemeDark = getView().findViewById(R.id.btn_theme_dark);
        Button btnThemeLight = getView().findViewById(R.id.btn_theme_light);

        btnThemeSystem.setBackgroundResource(sys ? R.drawable.bg_theme_option_selected : R.drawable.bg_theme_option_unselected);
        btnThemeDark.setBackgroundResource(dark ? R.drawable.bg_theme_option_selected : R.drawable.bg_theme_option_unselected);
        btnThemeLight.setBackgroundResource(light ? R.drawable.bg_theme_option_selected : R.drawable.bg_theme_option_unselected);

        btnThemeSystem.setCompoundDrawablesWithIntrinsicBounds(sys ? R.drawable.ic_check_16 : 0, 0, 0, 0);
        btnThemeDark.setCompoundDrawablesWithIntrinsicBounds(dark ? R.drawable.ic_check_16 : 0, 0, 0, 0);
        btnThemeLight.setCompoundDrawablesWithIntrinsicBounds(light ? R.drawable.ic_check_16 : 0, 0, 0, 0);
    }

    private void showMoreSettingsBottomSheet() {
        BottomSheetDialog bottomSheet = new BottomSheetDialog(requireContext());
        View sheetView = LayoutInflater.from(requireContext()).inflate(R.layout.bottom_sheet_more_settings, null);
        bottomSheet.setContentView(sheetView);

        // WebDAV 开关状态与持久化
        android.widget.Switch switchWebDav = sheetView.findViewById(R.id.switch_webdav);
        android.content.SharedPreferences webdavPrefs = requireContext().getSharedPreferences("webdav_prefs", Context.MODE_PRIVATE);
        boolean enabled = webdavPrefs.getBoolean("webdav_enabled", false);
        switchWebDav.setChecked(enabled);
        switchWebDav.setOnCheckedChangeListener((btn, isChecked) -> {
            webdavPrefs.edit().putBoolean("webdav_enabled", isChecked).apply();
            android.widget.Toast.makeText(requireContext(), isChecked ? "WebDAV 已启用" : "WebDAV 已禁用", android.widget.Toast.LENGTH_SHORT).show();
        });

        // 计算缓存大小并更新显示
        android.widget.TextView tvCache = sheetView.findViewById(R.id.tv_cache_status);
        android.widget.ProgressBar pbCache = sheetView.findViewById(R.id.progress_bar_cache);
        if (tvCache != null) {
            tvCache.setText("缓存: 正在计算大小...");
            com.example.facecheck.utils.CacheManager cm = new com.example.facecheck.utils.CacheManager(requireContext());
            cm.getCacheSize(new com.example.facecheck.utils.CacheManager.CacheSizeCallback() {
                @Override public void onSizeCalculated(com.example.facecheck.utils.CacheManager.CacheSizeInfo sizeInfo) {
                    String text = String.format("缓存: %s (图片: %s)",
                            formatFileSize(sizeInfo.totalSize),
                            formatFileSize(sizeInfo.imageCacheSize));
                    tvCache.setText(text);
                }
                @Override public void onError(String error) {
                    tvCache.setText("缓存: 获取失败");
                }
            });
        }

        // WebDAV 配置
        sheetView.findViewById(R.id.btn_webdav_config).setOnClickListener(v -> {
            bottomSheet.dismiss();
            showWebDavConfigDialog();
        });

        // 立即同步
        sheetView.findViewById(R.id.btn_sync_now).setOnClickListener(v -> {
            bottomSheet.dismiss();
            checkAndPromptWebDavSync(true);
        });

        // 缓存清理
        sheetView.findViewById(R.id.btn_clear_all_cache).setOnClickListener(v -> {
            bottomSheet.dismiss();
            runClearAllCache();
        });
        sheetView.findViewById(R.id.btn_clear_image_cache).setOnClickListener(v -> {
            bottomSheet.dismiss();
            runClearImageCache();
        });
        sheetView.findViewById(R.id.btn_clear_temp_cache).setOnClickListener(v -> {
            bottomSheet.dismiss();
            runClearTempCache();
        });

        // 关于（保留弹层入口，但主页面已有）
        sheetView.findViewById(R.id.item_about).setOnClickListener(v -> {
            bottomSheet.dismiss();
            String version = "";
            try {
                version = requireContext().getPackageManager().getPackageInfo(requireContext().getPackageName(), 0).versionName;
            } catch (Throwable ignore) {}
            new androidx.appcompat.app.AlertDialog.Builder(requireContext())
                    .setTitle("关于 FaceCheck")
                    .setMessage("版本：" + version)
                    .setPositiveButton("确定", null)
                    .show();
        });

        bottomSheet.show();
    }

    @Override
    public void onResume() {
        super.onResume();
        scheduleWebDavPeriodicCheck();
    }

    private final android.os.Handler uiHandler = new android.os.Handler(android.os.Looper.getMainLooper());
    private Runnable periodicRunnable;
    private View currentOverlay;

    private void scheduleWebDavPeriodicCheck() {
        if (!isAdded()) return;
        android.content.SharedPreferences prefs = requireContext().getSharedPreferences("webdav_prefs", Context.MODE_PRIVATE);
        boolean enabled = prefs.getBoolean("webdav_enabled", false);
        if (!enabled) return;
        String url = prefs.getString("webdav_url", "");
        String user = prefs.getString("webdav_username", "");
        String pass = prefs.getString("webdav_password", "");
        if (url.isEmpty() || user.isEmpty() || pass.isEmpty()) return;
        if (periodicRunnable != null) uiHandler.removeCallbacks(periodicRunnable);
        periodicRunnable = () -> {
            if (!isAdded()) return;
            checkAndPromptWebDavSync(false);
        };
        uiHandler.postDelayed(periodicRunnable, 5000);
    }

    private void checkAndPromptWebDavSync(boolean forcePrompt) {
        if (!isAdded()) return;
        android.content.SharedPreferences prefs = requireContext().getSharedPreferences("webdav_prefs", Context.MODE_PRIVATE);
        String url = prefs.getString("webdav_url", "");
        String user = prefs.getString("webdav_username", "");
        String pass = prefs.getString("webdav_password", "");
        com.example.facecheck.database.DatabaseHelper dbh = new com.example.facecheck.database.DatabaseHelper(requireContext());
        String localDbPath = dbh.getDatabaseAbsolutePath();
        long localTs = new java.io.File(localDbPath).lastModified();

        new Thread(() -> {
            android.content.Context ctx = getContext();
            if (ctx == null) return;
            com.example.facecheck.webdav.WebDavManager mgr = new com.example.facecheck.webdav.WebDavManager(ctx, url, user, pass);
            java.util.Date remoteDbMod = mgr.getResourceModified(com.example.facecheck.webdav.WebDavManager.ROOT_DIR + "/database7.db");
            long remoteTs = remoteDbMod != null ? remoteDbMod.getTime() : 0;
            long delta = Math.abs(remoteTs - localTs);
            if (forcePrompt || delta > 60_000) {
                String msg = String.format("本地DB: %s\n云端DB: %s\n选择同步方向", formatTime(localTs), formatTime(remoteTs));
                new android.os.Handler(android.os.Looper.getMainLooper()).post(() -> {
                    if (!isAdded()) return;
                    final View overlay = showUploadingOverlayWithTimeout();
                    new android.app.AlertDialog.Builder(requireContext())
                            .setTitle("WebDAV 差异检测")
                            .setMessage(msg)
                            .setPositiveButton("上传到云端", (d, w) -> {
                                new Thread(() -> {
                                    boolean ok = mgr.ensureFacecheckStructure();
                                    ok &= mgr.syncDatabase(localDbPath);
                                    java.io.File avatarDir = com.example.facecheck.utils.PhotoStorageManager.getAvatarPhotosDir(requireContext());
                                    ok &= mgr.uploadDirectory(avatarDir.getAbsolutePath(), com.example.facecheck.webdav.WebDavManager.ROOT_DIR + "/data/cover");
                                    java.io.File attDir = com.example.facecheck.utils.PhotoStorageManager.getAttendancePhotosDir(requireContext());
                                    ok &= mgr.uploadDirectory(attDir.getAbsolutePath(), com.example.facecheck.webdav.WebDavManager.ROOT_DIR + "/data/file");
                                    if (ok) prefs.edit().putLong("webdav_last_sync", System.currentTimeMillis()).apply();
                                    final boolean success = ok;
                                    new android.os.Handler(android.os.Looper.getMainLooper()).post(() -> {
                                        if (!isAdded()) return;
                                        dismissUploadingOverlay(overlay);
                                        android.widget.Toast.makeText(requireContext(), success ? "上传完成" : "上传失败", android.widget.Toast.LENGTH_SHORT).show();
                                    });
                                }).start();
                            })
                            .setNegativeButton("同步到本地", (d, w) -> {
                                new Thread(() -> {
                                    boolean ok = mgr.ensureFacecheckStructure();
                                    ok &= mgr.fetchDatabase(localDbPath);
                                    java.io.File avatarDir = com.example.facecheck.utils.PhotoStorageManager.getAvatarPhotosDir(requireContext());
                                    ok &= mgr.downloadDirectory(com.example.facecheck.webdav.WebDavManager.ROOT_DIR + "/data/cover", avatarDir.getAbsolutePath());
                                    java.io.File attDir = com.example.facecheck.utils.PhotoStorageManager.getAttendancePhotosDir(requireContext());
                                    ok &= mgr.downloadDirectory(com.example.facecheck.webdav.WebDavManager.ROOT_DIR + "/data/file", attDir.getAbsolutePath());
                                    if (ok) prefs.edit().putLong("webdav_last_sync", System.currentTimeMillis()).apply();
                                    final boolean success = ok;
                                    new android.os.Handler(android.os.Looper.getMainLooper()).post(() -> {
                                        if (!isAdded()) return;
                                        dismissUploadingOverlay(overlay);
                                        android.widget.Toast.makeText(requireContext(), success ? "下载并同步到本地完成" : "下载失败", android.widget.Toast.LENGTH_SHORT).show();
                                    });
                                }).start();
                            })
                            .show();
                });
            }
        }).start();
    }

    private String formatFileSize(long size) {
        if (size <= 0) return "0 B";
        final String[] units = new String[]{"B", "KB", "MB", "GB", "TB"};
        int digitGroups = (int) (Math.log10(size) / Math.log10(1024));
        return new java.text.DecimalFormat("#,##0.#").format(size / Math.pow(1024, digitGroups)) + " " + units[digitGroups];
    }

    private void runClearAllCache() {
        com.example.facecheck.utils.CacheManager cm = new com.example.facecheck.utils.CacheManager(requireContext());
        cm.clearAllCache(new com.example.facecheck.utils.CacheManager.CacheCleanCallback() {
            @Override public void onProgress(int progress, String message) {
                Toast.makeText(requireContext(), message, Toast.LENGTH_SHORT).show();
            }
            @Override public void onComplete(long freedSpace, String summary) {
                Toast.makeText(requireContext(), summary, Toast.LENGTH_LONG).show();
            }
            @Override public void onError(String error) {
                Toast.makeText(requireContext(), error, Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void runClearImageCache() {
        com.example.facecheck.utils.CacheManager cm = new com.example.facecheck.utils.CacheManager(requireContext());
        cm.clearImageCache(new com.example.facecheck.utils.CacheManager.CacheCleanCallback() {
            @Override public void onProgress(int progress, String message) {
                Toast.makeText(requireContext(), message, Toast.LENGTH_SHORT).show();
            }
            @Override public void onComplete(long freedSpace, String summary) {
                Toast.makeText(requireContext(), summary, Toast.LENGTH_LONG).show();
            }
            @Override public void onError(String error) {
                Toast.makeText(requireContext(), error, Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void runClearTempCache() {
        com.example.facecheck.utils.CacheManager cm = new com.example.facecheck.utils.CacheManager(requireContext());
        cm.clearTempCache(new com.example.facecheck.utils.CacheManager.CacheCleanCallback() {
            @Override public void onProgress(int progress, String message) {
                Toast.makeText(requireContext(), message, Toast.LENGTH_SHORT).show();
            }
            @Override public void onComplete(long freedSpace, String summary) {
                Toast.makeText(requireContext(), summary, Toast.LENGTH_LONG).show();
            }
            @Override public void onError(String error) {
                Toast.makeText(requireContext(), error, Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void showWebDavConfigDialog() {
        android.app.AlertDialog.Builder builder = new android.app.AlertDialog.Builder(requireContext());
        View dialogView = LayoutInflater.from(requireContext()).inflate(R.layout.dialog_webdav_config, null);
        builder.setView(dialogView);
        android.app.AlertDialog dialog = builder.create();

        android.widget.EditText etUrl = dialogView.findViewById(R.id.et_webdav_url);
        android.widget.EditText etUser = dialogView.findViewById(R.id.et_webdav_username);
        android.widget.EditText etPass = dialogView.findViewById(R.id.et_webdav_password);
        android.widget.TextView tvStatus = dialogView.findViewById(R.id.tv_connection_status);

        // 预填已保存配置
        android.content.SharedPreferences prefs = requireContext().getSharedPreferences("webdav_prefs", Context.MODE_PRIVATE);
        etUrl.setText(prefs.getString("webdav_url", ""));
        etUser.setText(prefs.getString("webdav_username", ""));
        etPass.setText(prefs.getString("webdav_password", ""));

        dialogView.findViewById(R.id.btn_test_connection).setOnClickListener(v -> {
            String url = etUrl.getText().toString().trim();
            String user = etUser.getText().toString().trim();
            String pass = etPass.getText().toString().trim();
            if (url.isEmpty() || user.isEmpty() || pass.isEmpty()) {
                tvStatus.setVisibility(View.VISIBLE);
                tvStatus.setText("请完整填写服务器、用户名、密码");
                return;
            }
            tvStatus.setVisibility(View.VISIBLE);
            tvStatus.setText("正在测试连接...");
            new Thread(() -> {
                com.example.facecheck.webdav.WebDavManager mgr = new com.example.facecheck.webdav.WebDavManager(requireContext(), url, user, pass);
                boolean ok = mgr.testConnection();
                new android.os.Handler(android.os.Looper.getMainLooper()).post(() -> {
                    tvStatus.setText(ok ? "测试成功" : "测试失败");
                });
            }).start();
        });

        dialogView.findViewById(R.id.btn_cancel).setOnClickListener(v -> dialog.dismiss());
        dialogView.findViewById(R.id.btn_save).setOnClickListener(v -> {
            String url = etUrl.getText().toString().trim();
            String user = etUser.getText().toString().trim();
            String pass = etPass.getText().toString().trim();
            if (url.isEmpty() || user.isEmpty() || pass.isEmpty()) {
                tvStatus.setVisibility(View.VISIBLE);
                tvStatus.setText("请完整填写服务器、用户名、密码");
                return;
            }
            android.content.SharedPreferences p = requireContext().getSharedPreferences("webdav_prefs", Context.MODE_PRIVATE);
            p.edit()
                    .putString("webdav_url", url)
                    .putString("webdav_username", user)
                    .putString("webdav_password", pass)
                    .putBoolean("webdav_enabled", true)
                    .apply();
            tvStatus.setVisibility(View.VISIBLE);
            tvStatus.setText("配置已保存并启用同步");
        });

        dialog.show();
    }

    private void runWebDavSync() {
        android.content.SharedPreferences prefs = requireContext().getSharedPreferences("webdav_prefs", Context.MODE_PRIVATE);
        boolean enabled = prefs.getBoolean("webdav_enabled", false);
        String url = prefs.getString("webdav_url", "");
        String user = prefs.getString("webdav_username", "");
        String pass = prefs.getString("webdav_password", "");
        if (!enabled || url.isEmpty() || user.isEmpty() || pass.isEmpty()) {
            Toast.makeText(requireContext(), "请先在 WebDAV 配置中启用并填写完整信息", Toast.LENGTH_SHORT).show();
            return;
        }

        com.example.facecheck.webdav.WebDavManager mgr = new com.example.facecheck.webdav.WebDavManager(requireContext(), url, user, pass);
        boolean rootExists = mgr.exists(mgr.getRootPath());
        com.example.facecheck.database.DatabaseHelper dbh = new com.example.facecheck.database.DatabaseHelper(requireContext());
        String localDbPath = dbh.getDatabaseAbsolutePath();
        long localTs = new java.io.File(localDbPath).lastModified();

        if (!rootExists) {
            new android.app.AlertDialog.Builder(requireContext())
                    .setTitle("初始化 WebDAV")
                    .setMessage("未检测到 facecheck 目录，是否创建并上传本地数据？")
                    .setPositiveButton("初始化并上传", (d, w) -> {
                        new Thread(() -> {
                            boolean ok = mgr.ensureFacecheckStructure();
                            ok &= mgr.syncDatabase(localDbPath);
                            // 上传头像到 data/cover；考勤到 data/file
                            java.io.File avatarDir = com.example.facecheck.utils.PhotoStorageManager.getAvatarPhotosDir(requireContext());
                            ok &= mgr.uploadDirectory(avatarDir.getAbsolutePath(), com.example.facecheck.webdav.WebDavManager.ROOT_DIR + "/data/cover");
                            java.io.File attDir = com.example.facecheck.utils.PhotoStorageManager.getAttendancePhotosDir(requireContext());
                            ok &= mgr.uploadDirectory(attDir.getAbsolutePath(), com.example.facecheck.webdav.WebDavManager.ROOT_DIR + "/data/file");
                            if (ok) prefs.edit().putLong("webdav_last_sync", System.currentTimeMillis()).apply();
                            final boolean success = ok;
                            new android.os.Handler(android.os.Looper.getMainLooper()).post(() ->
                                    Toast.makeText(requireContext(), success ? "初始化并上传完成" : "初始化失败", Toast.LENGTH_SHORT).show()
                            );
                        }).start();
                    })
                    .setNegativeButton("取消", null)
                    .show();
            return;
        }

        java.util.Date remoteDbMod = mgr.getResourceModified(com.example.facecheck.webdav.WebDavManager.ROOT_DIR + "/database7.db");
        long remoteTs = remoteDbMod != null ? remoteDbMod.getTime() : 0;
        String msg = String.format("本地DB: %s\n云端DB: %s\n选择同步方向", formatTime(localTs), formatTime(remoteTs));
        final View overlay = showUploadingOverlayWithTimeout();
        new android.app.AlertDialog.Builder(requireContext())
                .setTitle("选择同步方向")
                .setMessage(msg)
                .setPositiveButton("上传到云端", (d, w) -> {
                    new Thread(() -> {
                        boolean ok = mgr.ensureFacecheckStructure();
                        ok &= mgr.syncDatabase(localDbPath);
                        java.io.File avatarDir = com.example.facecheck.utils.PhotoStorageManager.getAvatarPhotosDir(requireContext());
                        ok &= mgr.uploadDirectory(avatarDir.getAbsolutePath(), com.example.facecheck.webdav.WebDavManager.ROOT_DIR + "/data/cover");
                        java.io.File attDir = com.example.facecheck.utils.PhotoStorageManager.getAttendancePhotosDir(requireContext());
                        ok &= mgr.uploadDirectory(attDir.getAbsolutePath(), com.example.facecheck.webdav.WebDavManager.ROOT_DIR + "/data/file");
                        if (ok) prefs.edit().putLong("webdav_last_sync", System.currentTimeMillis()).apply();
                        final boolean success = ok;
                        new android.os.Handler(android.os.Looper.getMainLooper()).post(() -> {
                                dismissUploadingOverlay(overlay);
                                Toast.makeText(requireContext(), success ? "上传完成" : "上传失败", Toast.LENGTH_SHORT).show();
                        });
                    }).start();
                })
                .setNegativeButton("同步到本地", (d, w) -> {
                    new Thread(() -> {
                        boolean ok = mgr.ensureFacecheckStructure();
                        ok &= mgr.fetchDatabase(localDbPath);
                        java.io.File avatarDir = com.example.facecheck.utils.PhotoStorageManager.getAvatarPhotosDir(requireContext());
                        ok &= mgr.downloadDirectory(com.example.facecheck.webdav.WebDavManager.ROOT_DIR + "/data/cover", avatarDir.getAbsolutePath());
                        java.io.File attDir = com.example.facecheck.utils.PhotoStorageManager.getAttendancePhotosDir(requireContext());
                        ok &= mgr.downloadDirectory(com.example.facecheck.webdav.WebDavManager.ROOT_DIR + "/data/file", attDir.getAbsolutePath());
                        if (ok) prefs.edit().putLong("webdav_last_sync", System.currentTimeMillis()).apply();
                        final boolean success = ok;
                        new android.os.Handler(android.os.Looper.getMainLooper()).post(() -> {
                                dismissUploadingOverlay(overlay);
                                Toast.makeText(requireContext(), success ? "下载并同步到本地完成" : "下载失败", Toast.LENGTH_SHORT).show();
                        });
                    }).start();
                })
                .show();
    }

    private String formatTime(long ts) {
        if (ts <= 0) return "未知";
        java.text.SimpleDateFormat sdf = new java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss", java.util.Locale.getDefault());
        return sdf.format(new java.util.Date(ts));
    }

    private View showUploadingOverlayWithTimeout() {
        if (!isAdded() || getActivity() == null) return null;
        ViewGroup root = (ViewGroup) getActivity().getWindow().getDecorView();
        View overlay = LayoutInflater.from(getContext()).inflate(R.layout.uploading_overlay, root, false);
        root.addView(overlay);
        currentOverlay = overlay;
        com.airbnb.lottie.LottieAnimationView lav = overlay.findViewById(R.id.lottieUploading);
        android.view.View spinner = overlay.findViewById(R.id.progressFallback);
        try {
            com.airbnb.lottie.LottieCompositionFactory.fromAsset(getContext(), "lottie/Uploading to cloud.json")
                    .addListener(comp -> {
                        spinner.setVisibility(android.view.View.GONE);
                        lav.setComposition(comp);
                        lav.setRenderMode(com.airbnb.lottie.RenderMode.AUTOMATIC);
                        lav.setRepeatCount(com.airbnb.lottie.LottieDrawable.INFINITE);
                        lav.playAnimation();
                    });
        } catch (Throwable ignore) {}
        // 手势下滑关闭
        overlay.setOnTouchListener(new android.view.View.OnTouchListener() {
            float downY;
            @Override public boolean onTouch(android.view.View v, android.view.MotionEvent e) {
                if (e.getAction() == android.view.MotionEvent.ACTION_DOWN) { downY = e.getY(); return true; }
                if (e.getAction() == android.view.MotionEvent.ACTION_UP) {
                    float dy = e.getY() - downY;
                    if (dy > 50) { dismissUploadingOverlay(overlay); return true; }
                }
                return false;
            }
        });
        uiHandler.postDelayed(() -> dismissUploadingOverlay(overlay), 5000);
        return overlay;
    }

    private void dismissUploadingOverlay(View overlay) {
        if (overlay == null) return;
        if (!isAdded() || getActivity() == null) return;
        ViewGroup root = (ViewGroup) getActivity().getWindow().getDecorView();
        try { root.removeView(overlay); } catch (Throwable ignore) {}
        if (overlay == currentOverlay) currentOverlay = null;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (periodicRunnable != null) uiHandler.removeCallbacks(periodicRunnable);
        if (currentOverlay != null) dismissUploadingOverlay(currentOverlay);
    }
}
