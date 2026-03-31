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
import android.util.Log;

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
import com.example.facecheck.utils.SessionManager;
import com.example.facecheck.data.repository.ClassroomRepository; // Add this import
import com.example.facecheck.data.repository.ClassroomRepositoryImpl; // Add this import
import com.example.facecheck.data.repository.ApiCallback; // Add this import
import com.example.facecheck.data.model.Classroom; // Add this import for syncAllClassrooms callback

import java.util.List; // Add this import
import androidx.appcompat.widget.TooltipCompat;

public class HomeFragment extends Fragment {

    private DatabaseHelper dbHelper;
    private SessionManager sessionManager;
    private ClassroomRepository classroomRepository;
    private TextView tvClassCount, tvStudentCount, tvAttendanceCount;
    private Button btnSync;
    private static final int PICK_IMAGE_REQUEST = 1001;
    private android.widget.ImageView bannerImage;
    private final int[] bannerRes = new int[] {
            R.drawable.logo_bright,
            R.drawable.facecheck,
            R.drawable.fclogo
    };
    private int bannerIndex = 0;
    private final android.os.Handler bannerHandler = new android.os.Handler();
    private final Runnable bannerRunnable = new Runnable() {
        @Override
        public void run() {
            if (getContext() != null && bannerImage != null) {
                try {
                    com.bumptech.glide.Glide.with(HomeFragment.this)
                            .load(bannerRes[bannerIndex % bannerRes.length])
                            .into(bannerImage);
                    bannerIndex++;
                } catch (Throwable ignore) {
                }
                bannerHandler.postDelayed(this, 4000);
            }
        }
    };

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
            @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_home, container, false);

        // 初始化数据库
        dbHelper = new DatabaseHelper(getContext());
        sessionManager = new SessionManager(getContext());
        classroomRepository = new ClassroomRepositoryImpl(getContext()); // Initialize ClassroomRepository

        // 初始化视图
        bannerImage = view.findViewById(R.id.bannerImage);
        tvClassCount = view.findViewById(R.id.tv_class_count);
        tvStudentCount = view.findViewById(R.id.tv_student_count);
        tvAttendanceCount = view.findViewById(R.id.tv_attendance_count);

        btnSync = view.findViewById(R.id.btn_sync);

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
        TooltipCompat.setTooltipText(btnSync, "与云端同步数据");

        // 加载统计数据
        loadStatistics();
        if (bannerImage != null) {
            try {
                bannerImage.setImageResource(bannerRes[bannerIndex % bannerRes.length]);
                com.bumptech.glide.Glide.with(HomeFragment.this)
                        .load(bannerRes[bannerIndex % bannerRes.length])
                        .into(bannerImage);
            } catch (Throwable ignore) {
            }
            bannerHandler.removeCallbacks(bannerRunnable);
            bannerHandler.post(bannerRunnable);
            setupBannerSwipe();
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
        // 获取当前登录的用户信息
        String role = getActivity().getSharedPreferences("user_prefs", Context.MODE_PRIVATE).getString("user_role", "teacher");
        long teacherId = sessionManager.getTeacherId();
        long studentId = getActivity().getSharedPreferences("user_prefs", Context.MODE_PRIVATE).getLong("student_id", -1);

        if ("student".equals(role)) {
            // 学生视角：优化统计标签并更新UI
            updateStudentHomeUI();

            // 加载学生统计 (Mock数据)
            tvClassCount.setText("6"); // Mock课程数
            tvStudentCount.setText("85%"); // Mock打卡率
            tvAttendanceCount.setText("12"); // Mock本月打卡数
            return;
        }

        if (teacherId == -1) {
            // 如果未登录，显示0
            tvClassCount.setText("0");
            tvStudentCount.setText("0");
            tvAttendanceCount.setText("0");
            return;
        }

        // 首先触发远程同步，这里修改顺序
        classroomRepository.syncAllClassrooms(teacherId, new ApiCallback<List<Classroom>>() {
            @Override
            public void onSuccess(List<Classroom> classroomData) {
                if (!isAdded()) return; // Fragment not attached
                Log.d("HomeFragment", "班级同步成功，开始同步签到任务...");

                // 班级同步成功后，接着同步签到任务
                classroomRepository.syncCheckinTasks(teacherId, new ApiCallback<List<com.example.facecheck.api.CheckinTaskListResponse.CheckinTask>>() {
                    @Override
                    public void onSuccess(List<com.example.facecheck.api.CheckinTaskListResponse.CheckinTask> taskData) {
                        if (!isAdded()) return;
                        Log.d("HomeFragment", "签到任务同步成功，刷新UI。");

                        // 1. 更新本地签到任务
                        deleteAllCheckinTasks();
                        for (com.example.facecheck.api.CheckinTaskListResponse.CheckinTask task : taskData) {
                            insertOrUpdateCheckinTask(task);
                        }

                        // 2. 更新UI统计
                        int classCount = classroomData.size();
                        int studentCount = 0;
                        for (Classroom classroom : classroomData) {
                            studentCount += classroom.getStudentCount();
                        }
                        int activeCheckinTaskCount = getCheckinTaskCountByTeacher(teacherId);

                        tvClassCount.setText(String.valueOf(classCount));
                        tvStudentCount.setText(String.valueOf(studentCount));
                        tvAttendanceCount.setText(String.valueOf(activeCheckinTaskCount));

                        // 更新标签
                        updateLabel(((ViewGroup) tvAttendanceCount.getParent().getParent()), "进行任务");
                    }

                    @Override
                    public void onError(String message) {
                        if (isAdded() && getContext() != null) {
                            Log.e("HomeFragment", "签到任务同步失败: " + message);
                            Toast.makeText(getContext(), "任务同步失败: " + message, Toast.LENGTH_SHORT).show();
                            // 即使任务同步失败，也尝试用本地数据更新UI
                            updateStatisticsFromLocalDb(teacherId);
                        }
                    }
                });
            }

            @Override
            public void onError(String message) {
                if (isAdded() && getContext() != null) {
                    Log.e("HomeFragment", "远程同步失败: " + message + ", 尝试从本地加载。");
                    // 远程同步失败，从本地数据库获取统计数据并更新UI
                    updateStatisticsFromLocalDb(teacherId);
                    Toast.makeText(getContext(), "同步失败，加载本地数据: " + message, Toast.LENGTH_SHORT).show();
                }
            }
        }
        );
    }

    private void updateStatisticsFromLocalDb(long teacherId) {
        // 从数据库获取真实统计数据
        int classCount = dbHelper.getClassroomCountByTeacher(teacherId);
        int studentCount = dbHelper.getStudentCountByTeacher(teacherId);
        int activeCheckinTaskCount = getCheckinTaskCountByTeacher(teacherId);

        // 更新UI
        tvClassCount.setText(String.valueOf(classCount));
        tvStudentCount.setText(String.valueOf(studentCount));
        tvAttendanceCount.setText(String.valueOf(activeCheckinTaskCount));

        // 更新标签
        updateLabel(((ViewGroup) tvAttendanceCount.getParent().getParent()), "进行中任务");
    }
//下面不用teacherid筛了
    private int getCheckinTaskCountByTeacher(long teacherId) {
        android.database.sqlite.SQLiteDatabase db = dbHelper.getReadableDatabase();
        String countQuery = "SELECT COUNT(*) FROM CheckinTask WHERE status = 'ACTIVE'";
        android.database.Cursor cursor = db.rawQuery(countQuery, null);
        int count = 0;
        if (cursor != null) {
            if (cursor.moveToFirst()) {
                count = cursor.getInt(0);
            }
            cursor.close();
        }
        return count;
    }

    private void updateStudentHomeUI() {
        // 更新统计卡片的描述文字
        ViewGroup statsLayout = (ViewGroup) tvClassCount.getParent().getParent().getParent();
        if (statsLayout != null && statsLayout.getChildCount() >= 3) {
            // 班级数量 -> 我的课程
            updateLabel(statsLayout.getChildAt(0), "我的课程");
            // 学生数量 -> 打卡率
            updateLabel(statsLayout.getChildAt(1), "打卡率");
            // 考勤记录 -> 本月打卡
            updateLabel(statsLayout.getChildAt(2), "本月打卡");
        }

        // 快捷入口已移除
    }

    private void updateLabel(View card, String text) {
        if (card instanceof ViewGroup) {
            ViewGroup vg = (ViewGroup) card;
            for (int i = 0; i < vg.getChildCount(); i++) {
                View child = vg.getChildAt(i);
                if (child instanceof ViewGroup) {
                    updateLabel(child, text);
                } else if (child instanceof TextView) {
                    TextView tv = (TextView) child;
                    if (!tv.getText().toString().matches("\\d+.*")) {
                        tv.setText(text);
                    }
                }
            }
        }
    }

    private void findAndSetText(ViewGroup vg, String text) {
        for (int i = 0; i < vg.getChildCount(); i++) {
            View child = vg.getChildAt(i);
            if (child instanceof TextView) {
                TextView tv = (TextView) child;
                if ("考勤记录".equals(tv.getText().toString())) {
                    tv.setText(text);
                }
            } else if (child instanceof ViewGroup) {
                findAndSetText((ViewGroup) child, text);
            }
        }
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
        long teacherId = sessionManager.getTeacherId();
        if (teacherId == -1) {
            Toast.makeText(getContext(), "请先登录", Toast.LENGTH_SHORT).show();
            dismissUploadingOverlay(overlay);
            return;
        }

        classroomRepository.syncCheckinTasks(teacherId, new ApiCallback<List<com.example.facecheck.api.CheckinTaskListResponse.CheckinTask>>() {
            @Override
            public void onSuccess(List<com.example.facecheck.api.CheckinTaskListResponse.CheckinTask> data) {
                if (getContext() == null) return;
                // Sync successful, update local database
                deleteAllCheckinTasks();
                for (com.example.facecheck.api.CheckinTaskListResponse.CheckinTask task : data) {
                    insertOrUpdateCheckinTask(task);
                }
                // Refresh UI
                loadStatistics();
                Toast.makeText(getContext(), "同步成功", Toast.LENGTH_SHORT).show();
                dismissUploadingOverlay(overlay);
            }

            @Override
            public void onError(String message) {
                if (getContext() == null) return;
                Toast.makeText(getContext(), "同步失败: " + message, Toast.LENGTH_SHORT).show();
                dismissUploadingOverlay(overlay);
            }
        });
    }

    private void insertOrUpdateCheckinTask(com.example.facecheck.api.CheckinTaskListResponse.CheckinTask task) {
        android.database.sqlite.SQLiteDatabase db = dbHelper.getWritableDatabase();
        android.content.ContentValues values = new android.content.ContentValues();
        values.put("id", task.id);
        values.put("classId", task.classId);
        values.put("teacherId", task.teacherId);
        values.put("title", task.title);
        values.put("startAt", task.startAt);
        values.put("endAt", task.endAt);
        values.put("status", task.status);
        values.put("locationLat", task.locationLat);
        values.put("locationLng", task.locationLng);
        values.put("locationRadiusM", task.locationRadiusM);
        values.put("gestureSequence", task.gestureSequence);
        values.put("passwordPlain", task.passwordPlain);
        values.put("createdAt", task.createdAt);
        db.insertWithOnConflict("CheckinTask", null, values, android.database.sqlite.SQLiteDatabase.CONFLICT_REPLACE);
    }

    private void deleteAllCheckinTasks() {
        android.database.sqlite.SQLiteDatabase db = dbHelper.getWritableDatabase();
        db.delete("CheckinTask", null, null);
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
        if (getActivity() == null)
            return null;
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
        } catch (Throwable ignore) {
        }
        overlay.setOnTouchListener(new View.OnTouchListener() {
            float downY;

            @Override
            public boolean onTouch(View v, android.view.MotionEvent e) {
                if (e.getAction() == android.view.MotionEvent.ACTION_DOWN) {
                    downY = e.getY();
                    return true;
                }
                if (e.getAction() == android.view.MotionEvent.ACTION_UP) {
                    float dy = e.getY() - downY;
                    if (dy > 50) {
                        dismissUploadingOverlay(overlay);
                        return true;
                    }
                }
                return false;
            }
        });
        new android.os.Handler(android.os.Looper.getMainLooper()).postDelayed(() -> dismissUploadingOverlay(overlay),
                5000);
        return overlay;
    }

    private void dismissUploadingOverlay(View overlay) {
        if (overlay == null || getActivity() == null)
            return;
        ViewGroup root = (ViewGroup) getActivity().getWindow().getDecorView();
        root.removeView(overlay);
    }

    @SuppressWarnings("ClickableViewAccessibility")
    private void setupBannerSwipe() {
        if (bannerImage == null)
            return;
        bannerImage.setOnTouchListener(new View.OnTouchListener() {
            float downX;

            @Override
            public boolean onTouch(View v, android.view.MotionEvent event) {
                switch (event.getAction()) {
                    case android.view.MotionEvent.ACTION_DOWN:
                        downX = event.getX();
                        return true;
                    case android.view.MotionEvent.ACTION_UP:
                        float dx = event.getX() - downX;
                        if (Math.abs(dx) > 50) {
                            if (dx < 0)
                                bannerIndex++;
                            else
                                bannerIndex = (bannerIndex - 1 + bannerRes.length) % bannerRes.length;
                            try {
                                com.bumptech.glide.Glide.with(HomeFragment.this)
                                        .load(bannerRes[bannerIndex % bannerRes.length])
                                        .into(bannerImage);
                            } catch (Throwable ignore) {
                            }
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
        String[] projection = { MediaStore.Images.Media.DATA };
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
