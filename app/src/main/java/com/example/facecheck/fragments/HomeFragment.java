package com.example.facecheck.fragments;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.compose.ui.platform.ComposeView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import com.example.facecheck.R;
import com.example.facecheck.MainActivity;
import com.example.facecheck.database.DatabaseHelper;
import com.example.facecheck.api.ApiService;
import com.example.facecheck.api.MySubmissionsResponse;
import com.example.facecheck.api.RetrofitClient;
import com.example.facecheck.ui.classroom.ClassroomSelectionActivity;
import com.example.facecheck.ui.attendance.AttendanceActivity;
import com.example.facecheck.ui.home.HomeStatsComposeBinder;
import com.example.facecheck.utils.SessionManager;
import com.example.facecheck.data.repository.ClassroomRepository; // Add this import
import com.example.facecheck.data.repository.ClassroomRepositoryImpl; // Add this import
import com.example.facecheck.data.repository.ApiCallback; // Add this import
import com.example.facecheck.data.model.Classroom; // Add this import for syncAllClassrooms callback
import com.example.facecheck.utils.RefreshPolicyManager;

import java.util.List; // Add this import
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class HomeFragment extends Fragment {

    private DatabaseHelper dbHelper;
    private SessionManager sessionManager;
    private ClassroomRepository classroomRepository;
    private ApiService apiService;
    private ComposeView composeHomeStats;
    private SwipeRefreshLayout swipeRefreshLayout;
    private String statOneLabel = "班级数量";
    private String statTwoLabel = "学生数量";
    private String statThreeLabel = "进行中任务";
    private String statOneValue = "0";
    private String statTwoValue = "0";
    private String statThreeValue = "0";
    private String roleLabel = "教师首页";
    private String syncHint = "下拉或点击同步按钮刷新最新数据";

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
            @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_home, container, false);

        // 初始化数据库
        dbHelper = new DatabaseHelper(getContext());
        sessionManager = new SessionManager(getContext());
        classroomRepository = new ClassroomRepositoryImpl(getContext()); // Initialize ClassroomRepository
        apiService = RetrofitClient.getApiService();

        // 初始化视图
        composeHomeStats = view.findViewById(R.id.compose_home_stats);
        renderHomeStats();

        swipeRefreshLayout = view.findViewById(R.id.swipe_refresh_home);
        if (swipeRefreshLayout != null) {
            swipeRefreshLayout.setOnRefreshListener(() -> loadStatistics(true));
        }

        // 加载统计数据
        loadStatistics(false);

        return view;
    }

    @Override
    public void onResume() {
        super.onResume();
        // 每次恢复时刷新统计数据
        loadStatistics(false);
    }

    private void loadStatistics(boolean forceRefresh) {
        // 获取当前登录的用户信息
        String role = getActivity().getSharedPreferences("user_prefs", Context.MODE_PRIVATE).getString("user_role", "teacher");
        long teacherId = sessionManager.getTeacherId();
        long studentId = getActivity().getSharedPreferences("user_prefs", Context.MODE_PRIVATE).getLong("student_id", -1);

//        // 加这两行显示报错
//        Toast.makeText(getContext(), "role=" + role + " studentId=" + studentId, Toast.LENGTH_LONG).show();

        if ("student".equals(role)) {
            roleLabel = "学生首页";
            updateStudentHomeUI();
            // P0: 先本地秒开，再后台增量刷新，避免首页首屏等待远端返回。
            loadStudentStatistics(studentId);
            if (studentId <= 0) {
                if (swipeRefreshLayout != null) swipeRefreshLayout.setRefreshing(false);
                return;
            }
            String studentRefreshKey = "home_stats_student_" + studentId;
            if (!forceRefresh && !RefreshPolicyManager.shouldRefresh(requireContext(), studentRefreshKey, RefreshPolicyManager.TTL_HOME_MS)) {
                if (swipeRefreshLayout != null) swipeRefreshLayout.setRefreshing(false);
                return;
            }
            classroomRepository.syncCheckinTasks(0L, new ApiCallback<List<com.example.facecheck.api.CheckinTaskListResponse.CheckinTask>>() {
                @Override
                public void onSuccess(List<com.example.facecheck.api.CheckinTaskListResponse.CheckinTask> taskData) {
                    if (!isAdded()) return;
                    deleteAllCheckinTasks();
                    for (com.example.facecheck.api.CheckinTaskListResponse.CheckinTask task : taskData) {
                        insertOrUpdateCheckinTask(task);
                    }
                    syncStudentSubmissionsFromApi(studentId, () -> {
                        if (!isAdded()) return;
                        loadStudentStatistics(studentId);
                        RefreshPolicyManager.markRefreshed(requireContext(), studentRefreshKey);
                        markSyncSuccess("学生任务与提交已同步");
                        if (swipeRefreshLayout != null) swipeRefreshLayout.setRefreshing(false);
                    });
                }

                @Override
                public void onError(String message) {
                    if (!isAdded()) return;
//                    Toast.makeText(getContext(), "详细错误: " + message, Toast.LENGTH_LONG).show(); // 改这里
                    loadStudentStatistics(studentId);
                    markSyncFailed("学生同步失败，已显示本地统计");
                    if (swipeRefreshLayout != null) swipeRefreshLayout.setRefreshing(false);
                }
            });
            return;
        }

        roleLabel = "教师首页";
        setTeacherHomeLabels();
        if (teacherId == -1) {
            // 如果未登录，显示0
            setHomeStats("0", "0", "0");
            syncHint = "未登录，无法同步";
            renderHomeStats();
            if (swipeRefreshLayout != null) swipeRefreshLayout.setRefreshing(false);
            return;
        }

        // P0: 教师端同样先读取本地统计，远端结果异步覆盖。
        updateStatisticsFromLocalDb(teacherId);
        String refreshKey = "home_stats_" + teacherId;
        if (!forceRefresh && !RefreshPolicyManager.shouldRefresh(requireContext(), refreshKey, RefreshPolicyManager.TTL_HOME_MS)) {
            if (swipeRefreshLayout != null) swipeRefreshLayout.setRefreshing(false);
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

                        setHomeStats(String.valueOf(classCount), String.valueOf(studentCount), String.valueOf(activeCheckinTaskCount));
                        RefreshPolicyManager.markRefreshed(requireContext(), refreshKey);
                        markSyncSuccess("班级和签到任务已同步");
                        if (swipeRefreshLayout != null) swipeRefreshLayout.setRefreshing(false);
                    }

                    @Override
                    public void onError(String message) {
                        if (isAdded() && getContext() != null) {
                            Log.e("HomeFragment", "签到任务同步失败: " + message);
                            Toast.makeText(getContext(), "任务同步失败: " + message, Toast.LENGTH_SHORT).show();
                            // 即使任务同步失败，也尝试用本地数据更新UI
                            updateStatisticsFromLocalDb(teacherId);
                            markSyncFailed("任务同步失败，已显示本地统计");
                            if (swipeRefreshLayout != null) swipeRefreshLayout.setRefreshing(false);
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
                    markSyncFailed("远端同步失败，已回退本地数据");
                    if (swipeRefreshLayout != null) swipeRefreshLayout.setRefreshing(false);
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
        setHomeStats(String.valueOf(classCount), String.valueOf(studentCount), String.valueOf(activeCheckinTaskCount));
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
        statOneLabel = "我的班级";
        statTwoLabel = "签到完成率";
        statThreeLabel = "本月签到";
        renderHomeStats();
    }

    private void loadStudentStatistics(long studentId) {
        if (studentId <= 0) {
            setHomeStats("0", "0%", "0");
            return;
        }
        long classId = dbHelper.getClassIdByStudentId(studentId);
        int classCount = classId > 0 ? 1 : 0;

        int totalTasks = 0;
        int signedTasks = 0;
        int monthSigned = 0;
        String monthPrefix = new java.text.SimpleDateFormat("yyyy-MM", java.util.Locale.getDefault())
                .format(new java.util.Date());
        android.database.sqlite.SQLiteDatabase db = dbHelper.getReadableDatabase();
        if (classId > 0) {
            android.database.Cursor taskCursor = db.rawQuery(
                    "SELECT COUNT(*) FROM CheckinTask WHERE classId = ?",
                    new String[] { String.valueOf(classId) });
            if (taskCursor != null && taskCursor.moveToFirst()) {
                totalTasks = taskCursor.getInt(0);
                taskCursor.close();
            } else if (taskCursor != null) {
                taskCursor.close();
            }
        }
        android.database.Cursor signedCursor = db.rawQuery(
                    "SELECT COUNT(DISTINCT s.taskId) " +
                            "FROM CheckinSubmission s INNER JOIN CheckinTask t ON t.id = s.taskId " +
                            "WHERE s.studentId = ? AND t.classId = ? " +
                            "AND (s.finalResult = 'APPROVED' OR s.finalResult = 'PENDING_REVIEW')",
                new String[] { String.valueOf(studentId), String.valueOf(classId) });
        if (signedCursor != null && signedCursor.moveToFirst()) {
            signedTasks = signedCursor.getInt(0);
            signedCursor.close();
        } else if (signedCursor != null) {
            signedCursor.close();
        }
        android.database.Cursor monthCursor = db.rawQuery(
                "SELECT COUNT(DISTINCT s.taskId) " +
                        "FROM CheckinSubmission s INNER JOIN CheckinTask t ON t.id = s.taskId " +
                        "WHERE s.studentId = ? AND t.classId = ? AND s.submittedAt LIKE ? " +
                        "AND (s.finalResult = 'APPROVED' OR s.finalResult = 'PENDING_REVIEW')",
                new String[] { String.valueOf(studentId), String.valueOf(classId), monthPrefix + "%" });
        if (monthCursor != null && monthCursor.moveToFirst()) {
            monthSigned = monthCursor.getInt(0);
            monthCursor.close();
        } else if (monthCursor != null) {
            monthCursor.close();
        }
        int rate = totalTasks <= 0 ? 0 : Math.min(100, Math.max(0, (signedTasks * 100) / totalTasks));
        setHomeStats(String.valueOf(classCount), rate + "%", String.valueOf(monthSigned));
    }

    private void syncStudentSubmissionsFromApi(long studentId, Runnable next) {
        if (studentId <= 0) {
            if (next != null) next.run();
            return;
        }
        apiService.getMySubmissions(sessionManager.getApiKey(), studentId)
                .enqueue(new retrofit2.Callback<MySubmissionsResponse>() {
                    @Override
                    public void onResponse(retrofit2.Call<MySubmissionsResponse> call,
                                           retrofit2.Response<MySubmissionsResponse> response) {
                        if (response.isSuccessful() && response.body() != null && response.body().success) {
                            mergeStudentSubmissionsToLocal(studentId, response.body().data);
                        }
                        if (next != null) next.run();
                    }

                    @Override
                    public void onFailure(retrofit2.Call<MySubmissionsResponse> call, Throwable t) {
                        if (next != null) next.run();
                    }
                });
    }

    private void mergeStudentSubmissionsToLocal(long studentId, List<MySubmissionsResponse.Item> submissions) {
        android.database.sqlite.SQLiteDatabase db = dbHelper.getWritableDatabase();
        db.beginTransaction();
        try {
            db.delete("CheckinSubmission", "studentId = ?", new String[] { String.valueOf(studentId) });
            if (submissions != null) {
                for (MySubmissionsResponse.Item item : submissions) {
                    android.content.ContentValues values = new android.content.ContentValues();
                    values.put("id", item.id);
                    values.put("taskId", item.taskId);
                    values.put("studentId", studentId);
                    values.put("submittedAt", item.submittedAt);
                    values.put("gestureInput", item.gestureInput);
                    values.put("passwordInput", item.passwordInput);
                    values.put("finalResult", item.finalResult == null ? "PENDING_REVIEW" : item.finalResult);
                    values.put("reason", item.reason);
                    values.put("isLatest", 1);
                    db.insertWithOnConflict("CheckinSubmission", null, values, android.database.sqlite.SQLiteDatabase.CONFLICT_REPLACE);
                }
            }
            db.setTransactionSuccessful();
        } finally {
            db.endTransaction();
        }
    }

    private void setTeacherHomeLabels() {
        statOneLabel = "班级数量";
        statTwoLabel = "学生数量";
        statThreeLabel = "进行中任务";
        renderHomeStats();
    }

    private void setHomeStats(String one, String two, String three) {
        statOneValue = one == null ? "0" : one;
        statTwoValue = two == null ? "0" : two;
        statThreeValue = three == null ? "0" : three;
        renderHomeStats();
    }

    private void renderHomeStats() {
        if (composeHomeStats == null) return;
        HomeStatsComposeBinder.bind(
                composeHomeStats,
                statOneValue,
                statOneLabel,
                statTwoValue,
                statTwoLabel,
                statThreeValue,
                statThreeLabel,
                roleLabel,
                syncHint,
                this::navigateToClassroom,
                this::navigateToAttendance,
                this::syncDatabase
        );
    }

    private void markSyncSuccess(String message) {
        syncHint = message + " · " + new SimpleDateFormat("HH:mm:ss", Locale.getDefault()).format(new Date());
        renderHomeStats();
    }

    private void markSyncFailed(String message) {
        syncHint = message;
        renderHomeStats();
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

        String role = requireActivity()
                .getSharedPreferences("user_prefs", Context.MODE_PRIVATE)
                .getString("user_role", "teacher");

        if ("student".equals(role)) {
            loadStatistics(true);
            markSyncSuccess("学生数据已触发刷新");
            dismissUploadingOverlay(overlay);
            return;
        }

        long teacherId = sessionManager.getTeacherId();
        if (teacherId == -1) {
            Toast.makeText(getContext(), "请先登录", Toast.LENGTH_SHORT).show();
            markSyncFailed("请先登录后再同步");
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
                loadStatistics(true);
                markSyncSuccess("手动同步完成");
                Toast.makeText(getContext(), "同步成功", Toast.LENGTH_SHORT).show();
                dismissUploadingOverlay(overlay);
            }

            @Override
            public void onError(String message) {
                if (getContext() == null) return;
                markSyncFailed("手动同步失败: " + message);
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

}
