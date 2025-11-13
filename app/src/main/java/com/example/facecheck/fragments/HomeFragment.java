package com.example.facecheck.fragments;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Button;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.cardview.widget.CardView;
import androidx.fragment.app.Fragment;
import androidx.appcompat.widget.TooltipCompat;

import com.example.facecheck.R;
import com.example.facecheck.MainActivity;
import com.example.facecheck.database.DatabaseHelper;
import com.example.facecheck.ui.classroom.ClassroomSelectionActivity;
import com.example.facecheck.ui.attendance.AttendanceActivity;
import com.example.facecheck.activity.FaceCorrectionActivity;
import com.example.facecheck.ui.face.FaceEnhancementActivity;
import com.example.facecheck.ui.verify.VerifyFeatureConsistencyActivity;
import androidx.appcompat.widget.TooltipCompat;

public class HomeFragment extends Fragment {
    
    private DatabaseHelper dbHelper;
    private TextView tvClassCount, tvStudentCount, tvAttendanceCount;
    private CardView cardClassroom, cardStudents, cardAttendance, cardQuickAttendance;
    private Button btnSync;
    private static final int PICK_IMAGE_REQUEST = 1001;
    private android.widget.ImageView bannerImage;
    private final int[] bannerRes = new int[]{
            R.drawable.baked_goods_1,
            R.drawable.baked_goods_2,
            R.drawable.baked_goods_3
    };
    private int bannerIndex = 0;
    private final android.os.Handler bannerHandler = new android.os.Handler();
    private final Runnable bannerRunnable = new Runnable() {
        @Override public void run() {
            if (getContext() != null && bannerImage != null) {
                try {
                    com.bumptech.glide.Glide.with(HomeFragment.this)
                            .load(bannerRes[bannerIndex % bannerRes.length])
                            .into(bannerImage);
                    bannerIndex++;
                } catch (Throwable ignore) {}
                bannerHandler.postDelayed(this, 4000);
            }
        }
    };

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_home, container, false);
        
        // 初始化数据库
        dbHelper = new DatabaseHelper(getContext());
        
        // 初始化视图
        bannerImage = view.findViewById(R.id.bannerImage);
        tvClassCount = view.findViewById(R.id.tv_class_count);
        tvStudentCount = view.findViewById(R.id.tv_student_count);
        tvAttendanceCount = view.findViewById(R.id.tv_attendance_count);
        
        cardClassroom = view.findViewById(R.id.card_classroom);
        cardStudents = view.findViewById(R.id.card_students);
        cardAttendance = view.findViewById(R.id.card_attendance);
        cardQuickAttendance = view.findViewById(R.id.card_quick_attendance);
        btnSync = view.findViewById(R.id.btn_sync);
        
        // 设置点击事件
        cardClassroom.setOnClickListener(v -> navigateToClassroom());
        cardStudents.setOnClickListener(v -> navigateToStudents());
        cardAttendance.setOnClickListener(v -> navigateToAttendance());
        cardQuickAttendance.setOnClickListener(v -> navigateToQuickAttendance());
        
        // 人脸修复增强入口
        CardView cardFaceEnhancement = view.findViewById(R.id.card_face_enhancement);
        if (cardFaceEnhancement != null) {
            cardFaceEnhancement.setOnClickListener(v -> {
                Intent intent = new Intent(getActivity(), FaceEnhancementActivity.class);
                startActivity(intent);
            });
        }

        // 验证特征一致性入口
        CardView cardVerifyConsistency = view.findViewById(R.id.card_verify_consistency);
        if (cardVerifyConsistency != null) {
            cardVerifyConsistency.setOnClickListener(v -> {
                Intent intent = new Intent(getActivity(), VerifyFeatureConsistencyActivity.class);
                startActivity(intent);
            });
        }
        
        btnSync.setOnClickListener(v -> syncDatabase());
        
        // 添加Tooltip提示
        TooltipCompat.setTooltipText(cardClassroom, "管理您的班级信息");
        TooltipCompat.setTooltipText(cardStudents, "管理学生信息和人脸数据");
        TooltipCompat.setTooltipText(cardAttendance, "查看和管理考勤记录");
        TooltipCompat.setTooltipText(cardQuickAttendance, "快速开始人脸识别考勤");
        TooltipCompat.setTooltipText(btnSync, "与WebDAV服务器同步数据");
        
        // 加载统计数据
        loadStatistics();
        if (bannerImage != null) {
            try {
                com.bumptech.glide.Glide.with(HomeFragment.this)
                        .load(bannerRes[bannerIndex % bannerRes.length])
                        .into(bannerImage);
            } catch (Throwable ignore) {}
            bannerHandler.removeCallbacks(bannerRunnable);
            bannerHandler.post(bannerRunnable);
        }
        
        return view;
    }
    
    @Override
    public void onResume() {
        super.onResume();
        // 每次恢复时刷新统计数据
        loadStatistics();
        if (bannerImage != null) {
            bannerHandler.removeCallbacks(bannerRunnable);
            bannerHandler.post(bannerRunnable);
        }
    }
    
    private void loadStatistics() {
        // 获取当前登录的教师ID
        SharedPreferences prefs = getActivity().getSharedPreferences("user_prefs", Context.MODE_PRIVATE);
        long teacherId = prefs.getLong("teacher_id", -1);
        
        if (teacherId == -1) {
            // 如果未登录，显示0
            tvClassCount.setText("0");
            tvStudentCount.setText("0");
            tvAttendanceCount.setText("0");
            return;
        }
        
        // 从数据库获取真实统计数据
        int classCount = dbHelper.getClassroomCountByTeacher(teacherId);
        int studentCount = dbHelper.getStudentCountByTeacher(teacherId);
        int attendanceCount = dbHelper.getAttendanceCountByTeacher(teacherId);
        
        // 更新UI
        tvClassCount.setText(String.valueOf(classCount));
        tvStudentCount.setText(String.valueOf(studentCount));
        tvAttendanceCount.setText(String.valueOf(attendanceCount));
    }

    @Override
    public void onPause() {
        super.onPause();
        bannerHandler.removeCallbacks(bannerRunnable);
    }
    
    private void navigateToClassroom() {
        // 切换到班级管理页面
        if (getActivity() != null && getActivity() instanceof MainActivity) {
            MainActivity mainActivity = (MainActivity) getActivity();
            mainActivity.getBottomNavigationView().setSelectedItemId(R.id.nav_classroom);
        }
    }
    
    private void navigateToStudents() {
        // TODO: 切换到学生管理页面
        Toast.makeText(getContext(), "学生管理功能即将上线", Toast.LENGTH_SHORT).show();
    }
    
    private void navigateToAttendance() {
        // 切换到考勤管理页面
        if (getActivity() != null && getActivity() instanceof MainActivity) {
            MainActivity mainActivity = (MainActivity) getActivity();
            mainActivity.getBottomNavigationView().setSelectedItemId(R.id.nav_attendance);
        }
    }
    
    private void navigateToQuickAttendance() {
        // 跳转到班级选择页面，让用户选择班级后开始考勤
        Intent intent = new Intent(getContext(), ClassroomSelectionActivity.class);
        intent.putExtra("mode", "attendance");
        startActivity(intent);
    }
    
    private void syncDatabase() {
        View overlay = showUploadingOverlayWithTimeout();
        new android.os.Handler(android.os.Looper.getMainLooper()).postDelayed(() -> {
            dismissUploadingOverlay(overlay);
        }, 5000);
        Toast.makeText(getContext(), "正在同步数据...", Toast.LENGTH_SHORT).show();
    }
    
    /**
     * 打开图片选择器
     */
    private void openImagePicker() {
        Intent intent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
        intent.setType("image/*");
        startActivityForResult(intent, PICK_IMAGE_REQUEST);
    }

    private View showUploadingOverlayWithTimeout() {
        if (getActivity() == null) return null;
        ViewGroup root = (ViewGroup) getActivity().getWindow().getDecorView();
        View overlay = LayoutInflater.from(getContext()).inflate(R.layout.uploading_overlay, root, false);
        root.addView(overlay);
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
        overlay.setOnTouchListener(new View.OnTouchListener() {
            float downY;
            @Override public boolean onTouch(View v, android.view.MotionEvent e) {
                if (e.getAction() == android.view.MotionEvent.ACTION_DOWN) { downY = e.getY(); return true; }
                if (e.getAction() == android.view.MotionEvent.ACTION_UP) {
                    float dy = e.getY() - downY;
                    if (dy > 50) { dismissUploadingOverlay(overlay); return true; }
                }
                return false;
            }
        });
        new android.os.Handler(android.os.Looper.getMainLooper()).postDelayed(() -> dismissUploadingOverlay(overlay), 5000);
        return overlay;
    }

    private void dismissUploadingOverlay(View overlay) {
        if (overlay == null || getActivity() == null) return;
        ViewGroup root = (ViewGroup) getActivity().getWindow().getDecorView();
        root.removeView(overlay);
    }

    @SuppressWarnings("ClickableViewAccessibility")
    private void setupBannerSwipe() {
        if (bannerImage == null) return;
        bannerImage.setOnTouchListener(new View.OnTouchListener() {
            float downX;
            @Override public boolean onTouch(View v, android.view.MotionEvent event) {
                switch (event.getAction()) {
                    case android.view.MotionEvent.ACTION_DOWN:
                        downX = event.getX();
                        return true;
                    case android.view.MotionEvent.ACTION_UP:
                        float dx = event.getX() - downX;
                        if (Math.abs(dx) > 50) {
                            if (dx < 0) bannerIndex++; else bannerIndex = (bannerIndex - 1 + bannerRes.length) % bannerRes.length;
                            try {
                                com.bumptech.glide.Glide.with(HomeFragment.this)
                                        .load(bannerRes[bannerIndex % bannerRes.length])
                                        .into(bannerImage);
                            } catch (Throwable ignore) {}
                        }
                        return true;
                }
                return false;
            }
        });
    }
    
    @Override
    public void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        
        if (requestCode == PICK_IMAGE_REQUEST && resultCode == getActivity().RESULT_OK && data != null) {
            Uri selectedImageUri = data.getData();
            if (selectedImageUri != null) {
                // 获取图片的真实路径
                String imagePath = getRealPathFromURI(selectedImageUri);
                if (imagePath != null) {
                    // 跳转到人脸修复界面
                    Intent intent = new Intent(getActivity(), FaceCorrectionActivity.class);
                    intent.putExtra("image_path", imagePath);
                    startActivity(intent);
                } else {
                    Toast.makeText(getContext(), "无法获取图片路径", Toast.LENGTH_SHORT).show();
                }
            }
        }
    }
    
    /**
     * 从URI获取真实文件路径
     */
    private String getRealPathFromURI(Uri uri) {
        String[] projection = {MediaStore.Images.Media.DATA};
        android.database.Cursor cursor = getActivity().getContentResolver().query(uri, projection, null, null, null);
        if (cursor != null) {
            int column_index = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DATA);
            cursor.moveToFirst();
            String path = cursor.getString(column_index);
            cursor.close();
            return path;
        }
        return null;
    }
}
