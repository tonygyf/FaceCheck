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

        // WebDAV 配置
        sheetView.findViewById(R.id.btn_webdav_config).setOnClickListener(v -> {
            bottomSheet.dismiss();
            Toast.makeText(requireContext(), "WebDAV 配置（后续接入）", Toast.LENGTH_SHORT).show();
        });

        // 立即同步
        sheetView.findViewById(R.id.btn_sync_now).setOnClickListener(v -> {
            bottomSheet.dismiss();
            Toast.makeText(requireContext(), "立即同步（后续接入）", Toast.LENGTH_SHORT).show();
        });

        // 缓存清理
        sheetView.findViewById(R.id.btn_clear_all_cache).setOnClickListener(v -> {
            Toast.makeText(requireContext(), "清理所有缓存（后续接入）", Toast.LENGTH_SHORT).show();
        });
        sheetView.findViewById(R.id.btn_clear_image_cache).setOnClickListener(v -> {
            Toast.makeText(requireContext(), "清理图片缓存（后续接入）", Toast.LENGTH_SHORT).show();
        });
        sheetView.findViewById(R.id.btn_clear_temp_cache).setOnClickListener(v -> {
            Toast.makeText(requireContext(), "清理临时缓存（后续接入）", Toast.LENGTH_SHORT).show();
        });

        // 关于
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
}
