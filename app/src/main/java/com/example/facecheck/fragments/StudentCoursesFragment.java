package com.example.facecheck.fragments;

import android.Manifest;
import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.compose.ui.platform.ComposeView;
import androidx.core.content.ContextCompat;
import androidx.core.content.FileProvider;
import androidx.core.widget.NestedScrollView;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewTreeLifecycleOwner;
import androidx.lifecycle.ViewTreeViewModelStoreOwner;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;
import androidx.savedstate.SavedStateRegistryOwner;
import androidx.savedstate.ViewTreeSavedStateRegistryOwner;

import com.example.facecheck.R;
import com.example.facecheck.api.ApiService;
import com.example.facecheck.api.CheckinPhotoUploadResponse;
import com.example.facecheck.api.CheckinSubmitRequest;
import com.example.facecheck.api.CheckinTaskListResponse;
import com.example.facecheck.api.MySubmissionsResponse;
import com.example.facecheck.api.RetrofitClient;
import com.example.facecheck.database.DatabaseHelper;
import com.example.facecheck.ui.checkin.AttendanceTaskComposeBinder;
import com.example.facecheck.ui.checkin.StudentCheckinComposeBinder;
import com.example.facecheck.ui.checkin.StudentCheckinTagComposeBinder;
import com.example.facecheck.ui.face.AvatarCropActivity;
import com.example.facecheck.ui.task.MapPickerActivity;
import com.example.facecheck.utils.Constants;
import com.example.facecheck.utils.ImageUtils;
import com.example.facecheck.utils.PhotoStorageManager;
import com.example.facecheck.utils.RefreshPolicyManager;
import com.example.facecheck.utils.SessionManager;
import com.google.android.gms.location.LocationServices;
import com.google.android.material.bottomsheet.BottomSheetBehavior;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.google.android.material.card.MaterialCardView;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;

import okhttp3.MediaType;
import okhttp3.MultipartBody;
import okhttp3.RequestBody;

public class StudentCoursesFragment extends Fragment {
    private static final int REQUEST_CHECKIN_IMAGE_CAPTURE = 2111;
    private static final int REQUEST_CHECKIN_IMAGE_PICK = 2112;
    private static final int REQUEST_CHECKIN_FACE_CROP = 2113;

    private RecyclerView recyclerView;
    private TextView tvEmptyHint;
    private TextView tvClassName;
    private FloatingActionButton fabRefresh;
    private SwipeRefreshLayout swipeRefreshLayout;

    private DatabaseHelper dbHelper;
    private ApiService apiService;
    private SessionManager sessionManager;
    private long studentId = -1;
    private long classId = -1;
    private String studentSid;
    private SignInSessionAdapter adapter;
    private List<SessionItem> sessionItems = new ArrayList<>();
    private List<ClassItem> classItems = new ArrayList<>();
    private final HashMap<Long, SubmissionItem> submissionMap = new HashMap<>();
    private final HashMap<Long, String> checkinFacePhotoPathMap = new HashMap<>();
    private String pendingCheckinPhotoPath;
    private long pendingCheckinPhotoTaskId = -1L;
    private TextView activeFaceAttachmentStatusView;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
            @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_student_sign_in, container, false);

        dbHelper = new DatabaseHelper(requireContext());
        apiService = RetrofitClient.getApiService();
        sessionManager = new SessionManager(requireContext());

        SharedPreferences prefs = requireContext().getSharedPreferences("user_prefs",
                android.content.Context.MODE_PRIVATE);
        studentId = prefs.getLong("student_id", -1);
        classId = prefs.getLong("class_id", -1);

        initViews(view);
        loadStudentInfo();
        refreshAllData(false);

        return view;
    }

    private void initViews(View view) {
        recyclerView = view.findViewById(R.id.recycler_sessions);
        tvEmptyHint = view.findViewById(R.id.tv_empty_hint);
        tvClassName = view.findViewById(R.id.tv_class_name);
        fabRefresh = view.findViewById(R.id.fab_refresh);
        swipeRefreshLayout = view.findViewById(R.id.swipe_refresh);

        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        adapter = new SignInSessionAdapter();
        recyclerView.setAdapter(adapter);

        if (swipeRefreshLayout != null) {
            swipeRefreshLayout.setColorSchemeResources(R.color.primary);
            swipeRefreshLayout.setOnRefreshListener(() -> refreshAllData(true));
        }

        fabRefresh.setOnClickListener(v -> {
            Toast.makeText(requireContext(), "刷新中...", Toast.LENGTH_SHORT).show();
            refreshAllData(true);
        });
        
        tvClassName.setOnClickListener(v -> {
            if (classItems.size() <= 1) return;
            String[] names = new String[classItems.size()];
            int checked = 0;
            for (int i = 0; i < classItems.size(); i++) {
                names[i] = classItems.get(i).className;
                if (classItems.get(i).classId == classId) checked = i;
            }
            new AlertDialog.Builder(requireContext())
                    .setTitle("切换班级")
                    .setSingleChoiceItems(names, checked, (d, which) -> {
                        classId = classItems.get(which).classId;
                        requireContext().getSharedPreferences("user_prefs", android.content.Context.MODE_PRIVATE)
                                .edit().putLong("class_id", classId).apply();
                        refreshAllData(true);
                        d.dismiss();
                    })
                    .show();
        });
    }

    private void refreshAllData(boolean forceRefresh) {
        if (!isAdded()) return;
        String refreshKey = "student_courses_" + studentId;
        boolean shouldSync = forceRefresh
                || RefreshPolicyManager.shouldRefresh(requireContext(), refreshKey, RefreshPolicyManager.TTL_HOME_MS);
        loadSubmissionCacheFromLocal();
        loadAttendanceSessions();
        if (!shouldSync) {
            stopRefreshing();
            return;
        }
        syncTasksFromApi(() -> syncMySubmissionsFromApi(() -> {
            if (isAdded()) {
                RefreshPolicyManager.markRefreshed(requireContext(), refreshKey);
            }
            loadAttendanceSessions();
            stopRefreshing();
        }));
    }

    private void stopRefreshing() {
        if (swipeRefreshLayout != null) {
            swipeRefreshLayout.setRefreshing(false);
        }
    }

    private void syncTasksFromApi(Runnable next) {
        apiService.getCheckinTasks(sessionManager.getApiKey(), 0L).enqueue(new retrofit2.Callback<CheckinTaskListResponse>() {
            @Override
            public void onResponse(retrofit2.Call<CheckinTaskListResponse> call, retrofit2.Response<CheckinTaskListResponse> response) {
                if (response.isSuccessful() && response.body() != null && response.body().success && response.body().data != null) {
                    for (CheckinTaskListResponse.CheckinTask task : response.body().data) {
                        insertOrUpdateCheckinTask(task);
                    }
                }
                if (next != null) next.run();
            }

            @Override
            public void onFailure(retrofit2.Call<CheckinTaskListResponse> call, Throwable t) {
                if (next != null) next.run();
            }
        });
    }

    private void syncMySubmissionsFromApi(Runnable next) {
        apiService.getMySubmissions(sessionManager.getApiKey(), studentId).enqueue(new retrofit2.Callback<MySubmissionsResponse>() {
            @Override
            public void onResponse(retrofit2.Call<MySubmissionsResponse> call, retrofit2.Response<MySubmissionsResponse> response) {
                if (response.isSuccessful() && response.body() != null && response.body().success && response.body().data != null) {
                    submissionMap.clear();
                    Set<Long> syncedTaskIds = new HashSet<>();
                    for (MySubmissionsResponse.Item item : response.body().data) {
                        if (syncedTaskIds.add(item.taskId)) {
                            submissionMap.put(item.taskId, new SubmissionItem(item.id, item.finalResult, item.reason, item.gestureInput, item.passwordInput));
                        }
                        dbHelper.upsertLatestCheckinSubmissionFromApi(
                                item.id,
                                item.taskId,
                                studentId,
                                item.submittedAt,
                                item.finalResult,
                                item.reason,
                                item.gestureInput,
                                item.passwordInput
                        );
                    }
                    loadSubmissionCacheFromLocal();
                }
                if (next != null) next.run();
            }

            @Override
            public void onFailure(retrofit2.Call<MySubmissionsResponse> call, Throwable t) {
                if (next != null) next.run();
            }
        });
    }

    private void loadSubmissionCacheFromLocal() {
        submissionMap.clear();
        if (studentId <= 0) return;
        Cursor cursor = dbHelper.getLatestCheckinSubmissionsByStudent(studentId);
        if (cursor != null && cursor.moveToFirst()) {
            do {
                long taskId = cursor.getLong(cursor.getColumnIndexOrThrow("taskId"));
                if (submissionMap.containsKey(taskId)) {
                    continue;
                }
                long submissionId = cursor.getLong(cursor.getColumnIndexOrThrow("id"));
                String finalResult = cursor.getString(cursor.getColumnIndexOrThrow("finalResult"));
                String reason = cursor.getString(cursor.getColumnIndexOrThrow("reason"));
                String gestureInput = cursor.getString(cursor.getColumnIndexOrThrow("gestureInput"));
                String passwordInput = cursor.getString(cursor.getColumnIndexOrThrow("passwordInput"));
                submissionMap.put(taskId, new SubmissionItem(submissionId, finalResult, reason, gestureInput, passwordInput));
            } while (cursor.moveToNext());
            cursor.close();
        } else if (cursor != null) {
            cursor.close();
        }
    }

    private void loadAttendanceSessions() {
        sessionItems.clear();

        if (classId <= 0) {
            showEmptyState("未找到班级信息");
            if (tvClassName != null)
                tvClassName.setText("未加入班级");
            return;
        }

        String className = dbHelper.getClassNameById(classId);
        if (tvClassName != null) {
            tvClassName.setText(className != null ? className : "未知班级");
        }

        Cursor cursor = dbHelper.getAllCheckinTasksByClass(classId);
        if (cursor != null && cursor.moveToFirst()) {
            do {
                long taskId = cursor.getLong(cursor.getColumnIndexOrThrow("id"));
                long taskClassId = cursor.getLong(cursor.getColumnIndexOrThrow("classId"));
                String title = cursor.getString(cursor.getColumnIndexOrThrow("title"));
                String status = cursor.getString(cursor.getColumnIndexOrThrow("status"));
                String startAt = cursor.getString(cursor.getColumnIndexOrThrow("startAt"));
                String endAt = cursor.getString(cursor.getColumnIndexOrThrow("endAt"));
                Double locationLat = cursor.isNull(cursor.getColumnIndexOrThrow("locationLat")) ? null : cursor.getDouble(cursor.getColumnIndexOrThrow("locationLat"));
                Double locationLng = cursor.isNull(cursor.getColumnIndexOrThrow("locationLng")) ? null : cursor.getDouble(cursor.getColumnIndexOrThrow("locationLng"));
                Integer locationRadiusM = cursor.isNull(cursor.getColumnIndexOrThrow("locationRadiusM")) ? null : cursor.getInt(cursor.getColumnIndexOrThrow("locationRadiusM"));
                String gestureSequence = cursor.getString(cursor.getColumnIndexOrThrow("gestureSequence"));
                String passwordPlain = cursor.getString(cursor.getColumnIndexOrThrow("passwordPlain"));
                Integer faceRequired = getNullableInt(cursor, "faceRequired");
                Double faceMinScore = getNullableDouble(cursor, "faceMinScore");
                SubmissionItem submission = submissionMap.get(taskId);
                SessionItem item = new SessionItem(taskId, taskClassId, title, status, startAt, endAt, submission,
                        locationLat, locationLng, locationRadiusM, gestureSequence, passwordPlain, faceRequired, faceMinScore);
                sessionItems.add(item);
            } while (cursor.moveToNext());
            cursor.close();
        }

        if (sessionItems.isEmpty()) {
            showEmptyState("暂无待签到的考勤任务");
        } else {
            tvEmptyHint.setVisibility(View.GONE);
            recyclerView.setVisibility(View.VISIBLE);
        }

        adapter.notifyDataSetChanged();
    }

    private void showEmptyState(String message) {
        tvEmptyHint.setText(message);
        tvEmptyHint.setVisibility(View.VISIBLE);
        recyclerView.setVisibility(View.VISIBLE);
    }

    @Override
    public void onResume() {
        super.onResume();
        if (getActivity() != null && getActivity().getActionBar() != null) {
            getActivity().getActionBar().hide();
        }
        refreshAllData(false);
    }

    private static class SessionItem {
        long taskId;
        long classId;
        String title;
        String status;
        String startAt;
        String endAt;
        SubmissionItem submission;
        Double locationLat;
        Double locationLng;
        Integer locationRadiusM;
        String gestureSequence;
        String passwordPlain;
        Integer faceRequired;
        Double faceMinScore;

        SessionItem(long taskId, long classId, String title, String status, String startAt, String endAt, SubmissionItem submission,
                    Double locationLat, Double locationLng, Integer locationRadiusM, String gestureSequence, String passwordPlain,
                    Integer faceRequired, Double faceMinScore) {
            this.taskId = taskId;
            this.classId = classId;
            this.title = title;
            this.status = status;
            this.startAt = startAt;
            this.endAt = endAt;
            this.submission = submission;
            this.locationLat = locationLat;
            this.locationLng = locationLng;
            this.locationRadiusM = locationRadiusM;
            this.gestureSequence = gestureSequence;
            this.passwordPlain = passwordPlain;
            this.faceRequired = faceRequired;
            this.faceMinScore = faceMinScore;
        }

        boolean requiresLocation() {
            return locationLat != null && locationLng != null;
        }

        boolean requiresGesture() {
            return gestureSequence != null && !gestureSequence.trim().isEmpty();
        }

        boolean requiresPassword() {
            return passwordPlain != null && !passwordPlain.trim().isEmpty();
        }

        boolean requiresFace() {
            return faceRequired != null && faceRequired == 1;
        }
    }

    private static class SubmissionItem {
        long submissionId;
        String finalResult;
        String reason;
        String gestureInput;
        String passwordInput;

        SubmissionItem(long submissionId, String finalResult, String reason, String gestureInput, String passwordInput) {
            this.submissionId = submissionId;
            this.finalResult = finalResult;
            this.reason = reason;
            this.gestureInput = gestureInput;
            this.passwordInput = passwordInput;
        }
    }

    private class SignInSessionAdapter extends RecyclerView.Adapter<SignInSessionAdapter.ViewHolder> {
        private static final int TYPE_EMPTY = 100;

        @NonNull
        @Override
        public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            if (viewType == TYPE_EMPTY) {
                View v = new View(parent.getContext());
                int h = parent.getResources().getDisplayMetrics().heightPixels + 200;
                v.setLayoutParams(new RecyclerView.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT, h));
                return new ViewHolder(v);
            }
            View v = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.item_sign_in_session, parent, false);
            return new ViewHolder(v);
        }

        @Override
        public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
            if (getItemViewType(position) == TYPE_EMPTY) return;
            SessionItem item = sessionItems.get(position);
            holder.tvTime.setText(item.title == null || item.title.isEmpty() ? "老师发布了签到任务" : item.title);
            holder.tvStart.setText(formatTaskDateTime(item.startAt));
            holder.tvEnd.setText(formatTaskEndTime(item.startAt, item.endAt));
            AttendanceTaskComposeBinder.bind(holder.composeTaskMeta, item.status, null);
            StudentCheckinTagComposeBinder.bind(holder.composeMethodTags, buildTagItems(item));

            MaterialCardView card = (MaterialCardView) holder.itemView;
            RecyclerView.LayoutParams lp = (RecyclerView.LayoutParams) card.getLayoutParams();
            if (item.submission != null) {
                lp.setMargins(0, lp.topMargin, 0, lp.bottomMargin);
                card.setCardBackgroundColor(androidx.core.content.ContextCompat.getColor(holder.itemView.getContext(), R.color.surface));
                holder.tvStatus.setText(resolveSubmissionText(item.submission));
                holder.tvStatus.setTextColor(holder.itemView.getContext().getColor(android.R.color.holo_blue_dark));
                holder.btnSignIn.setText("查看");
                holder.btnSignIn.setVisibility(View.VISIBLE);
                holder.btnSignIn.setOnClickListener(v -> showSubmitBottomSheet(item, true));
            } else {
                lp.setMargins(0, lp.topMargin, 0, lp.bottomMargin);
                card.setCardBackgroundColor(androidx.core.content.ContextCompat.getColor(holder.itemView.getContext(), android.R.color.white));
                if ("ACTIVE".equalsIgnoreCase(item.status)) {
                    holder.tvStatus.setText("老师邀请你完成签到");
                    holder.tvStatus.setTextColor(holder.itemView.getContext().getColor(android.R.color.holo_orange_dark));
                    holder.btnSignIn.setText("去签到");
                    holder.btnSignIn.setVisibility(View.VISIBLE);
                    holder.btnSignIn.setOnClickListener(v -> showSubmitBottomSheet(item, false));
                } else {
                    holder.tvStatus.setText("任务已结束");
                    holder.tvStatus.setTextColor(holder.itemView.getContext().getColor(android.R.color.darker_gray));
                    holder.btnSignIn.setVisibility(View.GONE);
                }
            }
            card.setLayoutParams(lp);
        }

        @Override
        public int getItemCount() {
            return sessionItems.isEmpty() ? 1 : sessionItems.size();
        }
        
        @Override
        public int getItemViewType(int position) {
            return sessionItems.isEmpty() ? TYPE_EMPTY : 0;
        }

        class ViewHolder extends RecyclerView.ViewHolder {
            TextView tvTime, tvStart, tvEnd, tvStatus;
            Button btnSignIn;
            ComposeView composeTaskMeta;
            ComposeView composeMethodTags;

            ViewHolder(@NonNull View itemView) {
                super(itemView);
                tvTime = itemView.findViewById(R.id.tv_session_time);
                tvStart = itemView.findViewById(R.id.tv_session_start);
                tvEnd = itemView.findViewById(R.id.tv_session_end);
                tvStatus = itemView.findViewById(R.id.tv_session_status);
                composeTaskMeta = itemView.findViewById(R.id.compose_task_meta);
                btnSignIn = itemView.findViewById(R.id.btn_sign_in);
                composeMethodTags = itemView.findViewById(R.id.compose_method_tags);
            }
        }
    }

    private String formatTaskDateTime(String raw) {
        Date d = parseTaskDate(raw);
        if (d == null) return "-";
        SimpleDateFormat to = new SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault());
        return to.format(d);
    }

    private String formatTaskEndTime(String startRaw, String endRaw) {
        Date start = parseTaskDate(startRaw);
        Date end = parseTaskDate(endRaw);
        if (end == null) return "-";
        if (start == null) {
            return new SimpleDateFormat("HH:mm", Locale.getDefault()).format(end);
        }

        Calendar startCal = Calendar.getInstance();
        startCal.setTime(start);
        Calendar endCal = Calendar.getInstance();
        endCal.setTime(end);
        if (startCal.get(Calendar.YEAR) != endCal.get(Calendar.YEAR)) {
            return new SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault()).format(end);
        }
        if (startCal.get(Calendar.MONTH) != endCal.get(Calendar.MONTH)) {
            return new SimpleDateFormat("MM-dd HH:mm", Locale.getDefault()).format(end);
        }
        if (startCal.get(Calendar.DAY_OF_MONTH) != endCal.get(Calendar.DAY_OF_MONTH)) {
            return new SimpleDateFormat("dd HH:mm", Locale.getDefault()).format(end);
        }
        return new SimpleDateFormat("HH:mm", Locale.getDefault()).format(end);
    }

    private Date parseTaskDate(String raw) {
        if (raw == null || raw.trim().isEmpty()) return null;
        String normalized = raw.trim().replace("T", " ");
        if (normalized.endsWith("Z")) {
            normalized = normalized.substring(0, normalized.length() - 1);
        }
        if (normalized.contains(".")) {
            normalized = normalized.substring(0, normalized.indexOf('.'));
        }
        SimpleDateFormat from = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault());
        SimpleDateFormat fromMinute = new SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault());
        try {
            return from.parse(normalized);
        } catch (ParseException ignored) {
        }
        try {
            return fromMinute.parse(normalized);
        } catch (ParseException ignored) {
        }
        return null;
    }

    private List<StudentCheckinTagComposeBinder.TagItem> buildTagItems(SessionItem item) {
        List<StudentCheckinTagComposeBinder.TagItem> tags = new ArrayList<>();
        int methodCount = 0;
        if (item.requiresLocation()) methodCount++;
        if (item.requiresGesture()) methodCount++;
        if (item.requiresPassword()) methodCount++;
        if (item.requiresFace()) methodCount++;
        tags.add(new StudentCheckinTagComposeBinder.TagItem(methodCount > 1 ? "混合签到" : "基础签到", false));
        if (item.requiresLocation()) tags.add(new StudentCheckinTagComposeBinder.TagItem("定位", true));
        if (item.requiresGesture()) tags.add(new StudentCheckinTagComposeBinder.TagItem("手势", true));
        if (item.requiresPassword()) tags.add(new StudentCheckinTagComposeBinder.TagItem("密码", true));
        if (item.requiresFace()) tags.add(new StudentCheckinTagComposeBinder.TagItem("人脸", true));
        return tags;
    }

    private String resolveSubmissionText(SubmissionItem submission) {
        if ("APPROVED".equalsIgnoreCase(submission.finalResult)) {
            return "已通过";
        }
        if ("PENDING_REVIEW".equalsIgnoreCase(submission.finalResult)) {
            return submission.reason == null || submission.reason.isEmpty() ? "待审核" : "待审核：" + submission.reason;
        }
        if ("REJECTED".equalsIgnoreCase(submission.finalResult)) {
            return submission.reason == null || submission.reason.isEmpty() ? "未通过，可申诉" : "未通过：" + submission.reason;
        }
        return "已提交";
    }

    private void showSubmitBottomSheet(SessionItem item, boolean readonly) {
        if (!isAdded()) return;
        try {
            if (getActivity() == null || getActivity().isFinishing() || getActivity().isDestroyed()) return;
            BottomSheetDialog dialog = new BottomSheetDialog(requireContext());
            dialog.setOnShowListener(d -> {
                BottomSheetDialog bottomSheetDialog = (BottomSheetDialog) d;
                View bottomSheet = bottomSheetDialog.findViewById(
                        com.google.android.material.R.id.design_bottom_sheet);

                if (bottomSheet != null) {
                    bottomSheet.setBackgroundResource(R.drawable.bg_bottom_sheet_rounded);
                }
            });

            View content = LayoutInflater.from(requireContext()).inflate(R.layout.dialog_student_checkin_submit, null, false);
            // ComposeView 在 BottomSheetDialog 中可能拿不到 owner，先给内容根视图绑定。
            applyViewTreeOwners(content);
            dialog.setContentView(content);
            if (dialog.getWindow() != null) {
                dialog.getWindow().setDimAmount(0.45f);
                dialog.getWindow().addFlags(WindowManager.LayoutParams.FLAG_DIM_BEHIND);
            }
            if (recyclerView != null) {
                recyclerView.setAlpha(0.82f);
            }
            dialog.setOnDismissListener(d -> {
                if (recyclerView != null) recyclerView.setAlpha(1f);
                activeFaceAttachmentStatusView = null;
                View bottomSheet = dialog.findViewById(com.google.android.material.R.id.design_bottom_sheet);
                if (bottomSheet != null) {
                    BottomSheetBehavior.from(bottomSheet).setDraggable(true);
                }
            });

            TextView tvTitle = content.findViewById(R.id.tv_task_title);
            TextView tvTaskStatus = content.findViewById(R.id.tv_task_status);
            TextView tvMethodHint = content.findViewById(R.id.tv_method_hint);
            TextView tvLocationRequired = content.findViewById(R.id.tv_location_required);
            TextView tvLocationCoords = content.findViewById(R.id.tv_location_coords);
            ComposeView composeRequiredTags = content.findViewById(R.id.compose_required_tags);
            View cardLocationRequired = content.findViewById(R.id.card_location_required);
            View cardFaceAttachment = content.findViewById(R.id.card_face_attachment);
            TextView tvFaceAttachmentStatus = content.findViewById(R.id.tv_face_attachment_status);
            Button btnFaceAttachment = content.findViewById(R.id.btn_face_attachment);
            Button btnOpenMapPreview = content.findViewById(R.id.btn_open_map_preview);
            ComposeView composeGesturePad = content.findViewById(R.id.compose_gesture_pad);
            View dragHandle = content.findViewById(R.id.view_drag_handle);
            NestedScrollView scrollCheckinMethods = content.findViewById(R.id.scroll_checkin_methods);
            EditText etGesture = content.findViewById(R.id.et_gesture);
            LinearLayout layoutPasswordRow = content.findViewById(R.id.layout_password_row);
            EditText etPassword = content.findViewById(R.id.et_password);
            Button btnTogglePasswordVisibility = content.findViewById(R.id.btn_toggle_password_visibility);
            EditText etReason = content.findViewById(R.id.et_reason);
            Button btnSubmit = content.findViewById(R.id.btn_submit_checkin);

            if (tvTitle == null || tvTaskStatus == null || tvMethodHint == null || tvLocationRequired == null
                    || tvLocationCoords == null || composeRequiredTags == null || cardLocationRequired == null
                    || cardFaceAttachment == null || tvFaceAttachmentStatus == null || btnFaceAttachment == null
                    || btnOpenMapPreview == null || composeGesturePad == null || etGesture == null
                    || layoutPasswordRow == null || etPassword == null || btnTogglePasswordVisibility == null
                    || etReason == null || btnSubmit == null) {
                Toast.makeText(requireContext(), "打开签到表单失败，请重试", Toast.LENGTH_SHORT).show();
                dialog.dismiss();
                return;
            }

            tvTitle.setText(item.title == null || item.title.isEmpty() ? "签到任务" : item.title);
            tvTaskStatus.setText(item.submission == null ? "待签到" : resolveSubmissionText(item.submission));
            tvMethodHint.setText(resolveMethodHint(item));
            cardLocationRequired.setVisibility(item.requiresLocation() ? View.VISIBLE : View.GONE);
            if (item.requiresLocation() && item.locationRadiusM != null) {
                tvLocationRequired.setText("本任务要求地理位置签到（半径 " + item.locationRadiusM + " 米），提交时将自动尝试定位。");
            }
            if (item.requiresLocation()) {
                String lat = item.locationLat == null ? "-" : String.format(Locale.getDefault(), "%.6f", item.locationLat);
                String lng = item.locationLng == null ? "-" : String.format(Locale.getDefault(), "%.6f", item.locationLng);
                tvLocationCoords.setText("中心点: " + lat + ", " + lng);
                btnOpenMapPreview.setText("查看签到范围（" + lat + ", " + lng + "）");
                btnOpenMapPreview.setOnClickListener(v -> openMapPreview(item));
            }

            cardFaceAttachment.setVisibility(item.requiresFace() ? View.VISIBLE : View.GONE);
            activeFaceAttachmentStatusView = item.requiresFace() ? tvFaceAttachmentStatus : null;
            if (item.requiresFace()) {
                String existingFacePhoto = checkinFacePhotoPathMap.get(item.taskId);
                tvFaceAttachmentStatus.setText(TextUtils.isEmpty(existingFacePhoto) ? "尚未上传人脸签到附件" : ("已选择附件: " + new File(existingFacePhoto).getName()));
                btnFaceAttachment.setVisibility(readonly ? View.GONE : View.VISIBLE);
                btnFaceAttachment.setOnClickListener(v -> startCheckinFaceAttachFlow(item));
            }

            etGesture.setVisibility(readonly && item.requiresGesture() ? View.VISIBLE : View.GONE);
            layoutPasswordRow.setVisibility(item.requiresPassword() ? View.VISIBLE : View.GONE);
            composeGesturePad.setVisibility(item.requiresGesture() ? View.VISIBLE : View.GONE);
            composeGesturePad.setOnTouchListener((v, event) -> {
                int action = event.getActionMasked();
                boolean drawing = action == MotionEvent.ACTION_DOWN || action == MotionEvent.ACTION_MOVE;
                boolean release = action == MotionEvent.ACTION_UP || action == MotionEvent.ACTION_CANCEL;
                if (scrollCheckinMethods != null) {
                    if (drawing) {
                        scrollCheckinMethods.requestDisallowInterceptTouchEvent(true);
                    } else if (release) {
                        scrollCheckinMethods.requestDisallowInterceptTouchEvent(false);
                    }
                }
                View bottomSheet = dialog.findViewById(com.google.android.material.R.id.design_bottom_sheet);
                if (bottomSheet != null) {
                    BottomSheetBehavior<View> behavior = BottomSheetBehavior.from(bottomSheet);
                    if (drawing) {
                        behavior.setDraggable(false);
                    }
                }
                return false;
            });

            if (readonly) {
                etGesture.setText(item.submission == null || item.submission.gestureInput == null ? "" : item.submission.gestureInput);
                etPassword.setText(item.submission == null || item.submission.passwordInput == null ? "" : item.submission.passwordInput);
                etReason.setText(item.submission == null || item.submission.reason == null ? "" : item.submission.reason);
                etPassword.setInputType(android.text.InputType.TYPE_CLASS_TEXT | android.text.InputType.TYPE_TEXT_VARIATION_PASSWORD);
                etGesture.setEnabled(false);
                etPassword.setEnabled(false);
                etReason.setEnabled(false);
                boolean[] passwordVisible = new boolean[] { false };
                if (item.requiresPassword()) {
                    btnTogglePasswordVisibility.setVisibility(View.VISIBLE);
                    btnTogglePasswordVisibility.setText("显示");
                    btnTogglePasswordVisibility.setOnClickListener(v -> {
                        passwordVisible[0] = !passwordVisible[0];
                        if (passwordVisible[0]) {
                            etPassword.setInputType(android.text.InputType.TYPE_CLASS_TEXT | android.text.InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD);
                            btnTogglePasswordVisibility.setText("隐藏");
                        } else {
                            etPassword.setInputType(android.text.InputType.TYPE_CLASS_TEXT | android.text.InputType.TYPE_TEXT_VARIATION_PASSWORD);
                            btnTogglePasswordVisibility.setText("显示");
                        }
                        etPassword.setSelection(etPassword.getText() == null ? 0 : etPassword.getText().length());
                    });
                } else {
                    btnTogglePasswordVisibility.setVisibility(View.GONE);
                }
                btnSubmit.setText("关闭");
                btnSubmit.setOnClickListener(v -> dialog.dismiss());
            } else {
                btnTogglePasswordVisibility.setVisibility(View.GONE);
                btnSubmit.setOnClickListener(v -> submitCheckinTask(item, etGesture.getText().toString().trim(),
                        etPassword.getText().toString().trim(),
                        etReason.getText().toString().trim(), checkinFacePhotoPathMap.get(item.taskId), dialog));
            }

            dialog.show();
            View bottomSheet = dialog.findViewById(com.google.android.material.R.id.design_bottom_sheet);
            if (bottomSheet != null) {
                BottomSheetBehavior<View> behavior = BottomSheetBehavior.from(bottomSheet);
                behavior.setSkipCollapsed(true);
                behavior.setState(BottomSheetBehavior.STATE_EXPANDED);
                behavior.setDraggable(false);
                if (dragHandle != null) {
                    dragHandle.setOnTouchListener((v, event) -> {
                        int action = event.getActionMasked();
                        if (action == MotionEvent.ACTION_DOWN || action == MotionEvent.ACTION_MOVE) {
                            behavior.setDraggable(true);
                        } else if (action == MotionEvent.ACTION_UP || action == MotionEvent.ACTION_CANCEL) {
                            v.post(() -> behavior.setDraggable(false));
                        }
                        return false;
                    });
                }
            }
            // show 后 Material 会再包裹一层容器，需再次给窗口树补齐 owner。
            if (dialog.getWindow() != null) {
                View decor = dialog.getWindow().getDecorView();
                applyViewTreeOwners(decor);
                View contentRoot = decor.findViewById(android.R.id.content);
                if (contentRoot != null) applyViewTreeOwners(contentRoot);
                int containerId = requireContext().getResources().getIdentifier("container", "id", requireContext().getPackageName());
                if (containerId != 0) {
                    View container = decor.findViewById(containerId);
                    if (container != null) applyViewTreeOwners(container);
                }
            }
            applyViewTreeOwners(composeRequiredTags);
            applyViewTreeOwners(composeGesturePad);

            content.post(() -> {
                if (!isAdded() || !dialog.isShowing() || content.getWindowToken() == null) return;
                try {
                    StudentCheckinTagComposeBinder.bind(composeRequiredTags, buildTagItems(item));
                } catch (Exception e) {
                    composeRequiredTags.setVisibility(View.GONE);
                }
                if (item.requiresGesture()) {
                    try {
                        StudentCheckinComposeBinder.bindGesturePad(
                                composeGesturePad,
                                readonly,
                                readonly && item.submission != null ? item.submission.gestureInput : null,
                                readonly ? null : sequence -> {
                                    etGesture.setText(sequence);
                                    return kotlin.Unit.INSTANCE;
                                }
                        );
                    } catch (Exception e) {
                        // 降级为文本输入，避免 Compose 初始化异常导致闪退
                        composeGesturePad.setVisibility(View.GONE);
                        etGesture.setVisibility(View.VISIBLE);
                    }
                }
            });
        } catch (Throwable e) {
            Toast.makeText(requireContext(), "打开签到表单失败: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }

    private void applyViewTreeOwners(View view) {
        if (view == null || !isAdded()) return;
        ViewTreeLifecycleOwner.set(view, getViewLifecycleOwner());
        ViewTreeViewModelStoreOwner.set(view, this);
        SavedStateRegistryOwner savedStateOwner = null;
        if (getViewLifecycleOwner() instanceof SavedStateRegistryOwner) {
            savedStateOwner = (SavedStateRegistryOwner) getViewLifecycleOwner();
        } else if (this instanceof SavedStateRegistryOwner) {
            savedStateOwner = this;
        }
        if (savedStateOwner != null) {
            ViewTreeSavedStateRegistryOwner.set(view, savedStateOwner);
        }
    }

    private void openMapPreview(SessionItem item) {
        if (!isAdded() || item.locationLat == null || item.locationLng == null) return;
        Intent intent = new Intent(requireContext(), MapPickerActivity.class);
        intent.putExtra("readonly", true);
        intent.putExtra("preset_lat", item.locationLat);
        intent.putExtra("preset_lng", item.locationLng);
        intent.putExtra("preset_radius_m", item.locationRadiusM == null ? -1 : item.locationRadiusM);
        startActivity(intent);
    }

    private String resolveMethodHint(SessionItem item) {
        List<String> methods = new ArrayList<>();
        methods.add("基础签到");
        if (item.requiresLocation()) methods.add("定位");
        if (item.requiresGesture()) methods.add("手势");
        if (item.requiresPassword()) methods.add("密码");
        if (item.requiresFace()) {
            if (item.faceMinScore != null) {
                methods.add("人脸(阈值" + String.format(Locale.getDefault(), "%.2f", item.faceMinScore) + ")");
            } else {
                methods.add("人脸");
            }
        }
        return "本任务要求: " + android.text.TextUtils.join(" + ", methods);
    }

    private void submitCheckinTask(SessionItem item, String gestureInput, String passwordInput, String reason, String facePhotoPath, BottomSheetDialog dialog) {
        if (item.requiresGesture() && TextUtils.isEmpty(gestureInput)) {
            Toast.makeText(requireContext(), "该任务要求手势签到，请先绘制手势", Toast.LENGTH_SHORT).show();
            return;
        }
        if (item.requiresPassword() && TextUtils.isEmpty(passwordInput)) {
            Toast.makeText(requireContext(), "该任务要求签到密码，请先填写密码", Toast.LENGTH_SHORT).show();
            return;
        }
        if (item.requiresFace() && TextUtils.isEmpty(facePhotoPath)) {
            Toast.makeText(requireContext(), "该任务要求人脸签到，请先上传人脸附件", Toast.LENGTH_SHORT).show();
            return;
        }

        CheckinSubmitRequest request = new CheckinSubmitRequest(studentId);
        request.gestureInput = gestureInput.isEmpty() ? null : gestureInput;
        request.passwordInput = passwordInput.isEmpty() ? null : passwordInput;
        request.reason = reason.isEmpty() ? null : reason;
        if (item.requiresFace()) {
            request.faceVerifyPassed = Boolean.FALSE;
            request.faceVerifyScore = 0D;
        }

        if (item.requiresFace()) {
            uploadCheckinFaceAttachmentThenSubmit(item, facePhotoPath, request, dialog);
            return;
        }
        fillLocationThenSubmit(item, request, dialog);
    }

    private void fillLocationThenSubmit(SessionItem item, CheckinSubmitRequest request, BottomSheetDialog dialog) {
        AtomicBoolean hasSubmitted = new AtomicBoolean(false);
        if (ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            LocationServices.getFusedLocationProviderClient(requireContext())
                    .getLastLocation()
                    .addOnSuccessListener(location -> {
                        if (hasSubmitted.get()) return;
                        if (location != null) {
                            request.lat = location.getLatitude();
                            request.lng = location.getLongitude();
                        }
                        hasSubmitted.set(true);
                        submitCheckinRequest(item, request, dialog);
                    })
                    .addOnFailureListener(e -> {
                        if (hasSubmitted.get()) return;
                        hasSubmitted.set(true);
                        submitCheckinRequest(item, request, dialog);
                    });
            return;
        }
        submitCheckinRequest(item, request, dialog);
    }

    private void uploadCheckinFaceAttachmentThenSubmit(SessionItem item, String facePhotoPath, CheckinSubmitRequest request, BottomSheetDialog dialog) {
        File prepared = prepareFaceAttachmentUploadFile(facePhotoPath, item.taskId);
        if (prepared == null || !prepared.exists()) {
            Toast.makeText(requireContext(), "人脸附件处理失败，请重新上传", Toast.LENGTH_SHORT).show();
            return;
        }

        String desiredKey = buildCheckinPhotoKey(item.taskId, studentId);
        RequestBody taskIdBody = RequestBody.create(MediaType.parse("text/plain"), String.valueOf(item.taskId));
        RequestBody studentIdBody = RequestBody.create(MediaType.parse("text/plain"), String.valueOf(studentId));
        RequestBody keyBody = RequestBody.create(MediaType.parse("text/plain"), desiredKey);
        RequestBody requestFile = RequestBody.create(MediaType.parse(detectImageMediaType(prepared.getAbsolutePath())), prepared);
        MultipartBody.Part filePart = MultipartBody.Part.createFormData("file", prepared.getName(), requestFile);

        apiService.uploadCheckinPhoto(sessionManager.getApiKey(), taskIdBody, studentIdBody, keyBody, filePart)
                .enqueue(new retrofit2.Callback<CheckinPhotoUploadResponse>() {
                    @Override
                    public void onResponse(retrofit2.Call<CheckinPhotoUploadResponse> call, retrofit2.Response<CheckinPhotoUploadResponse> response) {
                        CheckinPhotoUploadResponse body = response.body();
                        if (response.isSuccessful() && body != null && body.isOk() && !TextUtils.isEmpty(body.getKey())) {
                            request.photoKey = body.getKey();
                            request.photoUri = Constants.CDN_BASE_URL + body.getKey();
                            fillLocationThenSubmit(item, request, dialog);
                            return;
                        }
                        String err = body != null ? body.getError() : null;
                        if (TextUtils.isEmpty(err)) err = "上传失败";
                        Toast.makeText(requireContext(), "人脸附件上传失败: " + err, Toast.LENGTH_SHORT).show();
                    }

                    @Override
                    public void onFailure(retrofit2.Call<CheckinPhotoUploadResponse> call, Throwable t) {
                        Toast.makeText(requireContext(), "人脸附件上传异常: " + t.getMessage(), Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private void startCheckinFaceAttachFlow(SessionItem item) {
        if (!isAdded()) return;
        pendingCheckinPhotoTaskId = item.taskId;
        new AlertDialog.Builder(requireContext())
                .setTitle("选择人脸附件来源")
                .setItems(new String[]{"拍照", "从相册选择"}, (d, which) -> {
                    if (which == 0) {
                        openCheckinCamera();
                    } else {
                        openCheckinGallery();
                    }
                })
                .setNegativeButton("取消", null)
                .show();
    }

    private void openCheckinCamera() {
        if (!isAdded()) return;
        if (ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
            Toast.makeText(requireContext(), "请先授予相机权限", Toast.LENGTH_SHORT).show();
            return;
        }
        try {
            File photoFile = File.createTempFile(
                    "checkin_face_" + System.currentTimeMillis(),
                    ".jpg",
                    PhotoStorageManager.getAttendancePhotosDir(requireContext())
            );
            pendingCheckinPhotoPath = photoFile.getAbsolutePath();
            Uri photoUri = FileProvider.getUriForFile(requireContext(), requireContext().getPackageName() + ".fileprovider", photoFile);
            Intent intent = new Intent(android.provider.MediaStore.ACTION_IMAGE_CAPTURE);
            intent.putExtra(android.provider.MediaStore.EXTRA_OUTPUT, photoUri);
            intent.addFlags(Intent.FLAG_GRANT_WRITE_URI_PERMISSION);
            startActivityForResult(intent, REQUEST_CHECKIN_IMAGE_CAPTURE);
        } catch (Throwable t) {
            Toast.makeText(requireContext(), "打开相机失败: " + t.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }

    private void openCheckinGallery() {
        if (!isAdded()) return;
        Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
        intent.setType("image/*");
        startActivityForResult(Intent.createChooser(intent, "选择人脸附件"), REQUEST_CHECKIN_IMAGE_PICK);
    }

    private void startCheckinFaceCrop(String sourcePath) {
        if (!isAdded() || TextUtils.isEmpty(sourcePath)) return;
        Intent intent = new Intent(requireContext(), AvatarCropActivity.class);
        intent.putExtra(AvatarCropActivity.EXTRA_SOURCE_PATH, sourcePath);
        intent.putExtra(AvatarCropActivity.EXTRA_CROP_SCENE, AvatarCropActivity.CROP_SCENE_CHECKIN);
        startActivityForResult(intent, REQUEST_CHECKIN_FACE_CROP);
    }

    private String copyUriToTempImage(Uri uri) {
        if (!isAdded()) return null;
        try (InputStream inputStream = requireContext().getContentResolver().openInputStream(uri)) {
            if (inputStream == null) return null;
            File target = File.createTempFile(
                    "checkin_face_pick_" + System.currentTimeMillis(),
                    ".jpg",
                    PhotoStorageManager.getAttendancePhotosDir(requireContext())
            );
            try (FileOutputStream outputStream = new FileOutputStream(target)) {
                byte[] buffer = new byte[4096];
                int len;
                while ((len = inputStream.read(buffer)) > 0) {
                    outputStream.write(buffer, 0, len);
                }
                return target.getAbsolutePath();
            }
        } catch (Throwable t) {
            return null;
        }
    }

    private File prepareFaceAttachmentUploadFile(String sourcePath, long taskId) {
        if (!isAdded() || TextUtils.isEmpty(sourcePath)) return null;
        File src = new File(sourcePath);
        if (!src.exists()) return null;
        try {
            android.graphics.Bitmap bitmap = ImageUtils.loadAndResizeBitmap(sourcePath, 1024, 1024);
            if (bitmap == null) return src;
            File out = new File(
                    PhotoStorageManager.getAttendancePhotosDir(requireContext()),
                    "checkin_face_upload_" + taskId + "_" + studentId + "_" + System.currentTimeMillis() + ".jpg"
            );
            boolean saved = ImageUtils.saveBitmapToFile(bitmap, out);
            bitmap.recycle();
            return saved ? out : src;
        } catch (Throwable t) {
            return src;
        }
    }

    private String buildCheckinPhotoKey(long taskId, long studentId) {
        return "records/" + taskId + "/" + studentId + "/face_" + System.currentTimeMillis() + ".jpg";
    }

    private String detectImageMediaType(String filePath) {
        String lower = filePath == null ? "" : filePath.toLowerCase(Locale.ROOT);
        if (lower.endsWith(".png")) return "image/png";
        if (lower.endsWith(".webp")) return "image/webp";
        return "image/jpeg";
    }

    private void submitCheckinRequest(SessionItem item, CheckinSubmitRequest request, BottomSheetDialog dialog) {
        apiService.submitCheckin(sessionManager.getApiKey(), item.taskId, request)
                .enqueue(new retrofit2.Callback<com.example.facecheck.api.ApiCreateResponse>() {
                    @Override
                    public void onResponse(retrofit2.Call<com.example.facecheck.api.ApiCreateResponse> call,
                                           retrofit2.Response<com.example.facecheck.api.ApiCreateResponse> response) {
                        com.example.facecheck.api.ApiCreateResponse body = response.body();
                        if (response.isSuccessful() && (body == null || body.isOk())) {
                            Toast.makeText(requireContext(), "提交成功", Toast.LENGTH_SHORT).show();
                            dialog.dismiss();
                            refreshAllData(true);
                        } else {
                            String bodyError = body != null ? body.getError() : null;
                            String rawError = null;
                            try {
                                if (response.errorBody() != null) rawError = response.errorBody().string();
                            } catch (Exception ignored) {
                            }
                            Toast.makeText(requireContext(), "提交失败: " + (bodyError != null && !bodyError.isEmpty() ? bodyError : (rawError != null && !rawError.isEmpty() ? rawError : "服务器返回异常")), Toast.LENGTH_LONG).show();
                        }
                    }

                    @Override
                    public void onFailure(retrofit2.Call<com.example.facecheck.api.ApiCreateResponse> call, Throwable t) {
                        Toast.makeText(requireContext(), "网络异常: " + t.getMessage(), Toast.LENGTH_SHORT).show();
                    }
                });
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (!isAdded() || resultCode != Activity.RESULT_OK) return;

        if (requestCode == REQUEST_CHECKIN_IMAGE_CAPTURE) {
            if (!TextUtils.isEmpty(pendingCheckinPhotoPath)) {
                startCheckinFaceCrop(pendingCheckinPhotoPath);
            }
            return;
        }

        if (requestCode == REQUEST_CHECKIN_IMAGE_PICK) {
            if (data != null && data.getData() != null) {
                String localPath = copyUriToTempImage(data.getData());
                if (!TextUtils.isEmpty(localPath)) {
                    startCheckinFaceCrop(localPath);
                } else {
                    Toast.makeText(requireContext(), "读取图片失败", Toast.LENGTH_SHORT).show();
                }
            }
            return;
        }

        if (requestCode == REQUEST_CHECKIN_FACE_CROP) {
            if (data == null) return;
            String croppedPath = data.getStringExtra(AvatarCropActivity.RESULT_CROPPED_PATH);
            if (TextUtils.isEmpty(croppedPath) || pendingCheckinPhotoTaskId <= 0) {
                return;
            }
            checkinFacePhotoPathMap.put(pendingCheckinPhotoTaskId, croppedPath);
            if (activeFaceAttachmentStatusView != null) {
                activeFaceAttachmentStatusView.setText("已选择附件: " + new File(croppedPath).getName());
            }
            Toast.makeText(requireContext(), "人脸附件已就绪", Toast.LENGTH_SHORT).show();
        }
    }

    private void insertOrUpdateCheckinTask(CheckinTaskListResponse.CheckinTask task) {
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
        values.put("faceRequired", task.faceRequired != null ? task.faceRequired : 0);
        values.put("faceMinScore", task.faceMinScore);
        values.put("createdAt", task.createdAt);
        db.insertWithOnConflict("CheckinTask", null, values, android.database.sqlite.SQLiteDatabase.CONFLICT_REPLACE);
    }

    private Integer getNullableInt(Cursor cursor, String column) {
        int idx = cursor.getColumnIndex(column);
        if (idx < 0 || cursor.isNull(idx)) return null;
        return cursor.getInt(idx);
    }

    private Double getNullableDouble(Cursor cursor, String column) {
        int idx = cursor.getColumnIndex(column);
        if (idx < 0 || cursor.isNull(idx)) return null;
        return cursor.getDouble(idx);
    }

    private void loadStudentInfo() {
        classItems.clear();
        if (studentId <= 0) return;
        Cursor c = dbHelper.getStudentById(studentId);
        if (c != null && c.moveToFirst()) {
            studentSid = c.getString(c.getColumnIndexOrThrow("sid"));
            if (classId <= 0) {
                classId = c.getLong(c.getColumnIndexOrThrow("classId"));
            }
            c.close();
        } else if (c != null) {
            c.close();
        }
        if (studentSid != null && !studentSid.isEmpty()) {
            Cursor cc = dbHelper.getClassroomsByStudentSid(studentSid);
            if (cc != null && cc.moveToFirst()) {
                do {
                    long cid = cc.getLong(cc.getColumnIndexOrThrow("classId"));
                    String cname = cc.getString(cc.getColumnIndexOrThrow("className"));
                    classItems.add(new ClassItem(cid, cname));
                } while (cc.moveToNext());
                cc.close();
            } else if (cc != null) {
                cc.close();
            }
        }
        if (!classItems.isEmpty()) {
            boolean found = false;
            for (ClassItem item : classItems) {
                if (item.classId == classId) {
                    found = true;
                    break;
                }
            }
            if (!found) {
                classId = classItems.get(0).classId;
            }
        }
    }

    private static class ClassItem {
        long classId;
        String className;
        ClassItem(long classId, String className) {
            this.classId = classId;
            this.className = className;
        }
    }
}
