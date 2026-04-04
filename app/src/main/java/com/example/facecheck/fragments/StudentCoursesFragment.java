package com.example.facecheck.fragments;

import android.Manifest;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.LayoutInflater;
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
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import com.example.facecheck.R;
import com.example.facecheck.api.ApiService;
import com.example.facecheck.api.CheckinSubmitRequest;
import com.example.facecheck.api.CheckinTaskListResponse;
import com.example.facecheck.api.MySubmissionsResponse;
import com.example.facecheck.api.RetrofitClient;
import com.example.facecheck.database.DatabaseHelper;
import com.example.facecheck.ui.checkin.AttendanceTaskComposeBinder;
import com.example.facecheck.ui.checkin.StudentCheckinComposeBinder;
import com.example.facecheck.ui.checkin.StudentCheckinTagComposeBinder;
import com.example.facecheck.ui.task.MapPickerActivity;
import com.example.facecheck.utils.RefreshPolicyManager;
import com.example.facecheck.utils.SessionManager;
import com.google.android.gms.location.LocationServices;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.google.android.material.card.MaterialCardView;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.atomic.AtomicBoolean;

public class StudentCoursesFragment extends Fragment {

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
        if (!forceRefresh && !RefreshPolicyManager.shouldRefresh(requireContext(), refreshKey, RefreshPolicyManager.TTL_HOME_MS)) {
            loadAttendanceSessions();
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
        submissionMap.clear();
        apiService.getMySubmissions(sessionManager.getApiKey(), studentId).enqueue(new retrofit2.Callback<MySubmissionsResponse>() {
            @Override
            public void onResponse(retrofit2.Call<MySubmissionsResponse> call, retrofit2.Response<MySubmissionsResponse> response) {
                if (response.isSuccessful() && response.body() != null && response.body().success && response.body().data != null) {
                    for (MySubmissionsResponse.Item item : response.body().data) {
                        if (!submissionMap.containsKey(item.taskId)) {
                            submissionMap.put(item.taskId, new SubmissionItem(item.id, item.finalResult, item.reason, item.gestureInput, item.passwordInput));
                        }
                    }
                }
                if (next != null) next.run();
            }

            @Override
            public void onFailure(retrofit2.Call<MySubmissionsResponse> call, Throwable t) {
                if (next != null) next.run();
            }
        });
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
                SubmissionItem submission = submissionMap.get(taskId);
                SessionItem item = new SessionItem(taskId, taskClassId, title, status, startAt, endAt, submission,
                        locationLat, locationLng, locationRadiusM, gestureSequence, passwordPlain);
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

        SessionItem(long taskId, long classId, String title, String status, String startAt, String endAt, SubmissionItem submission,
                    Double locationLat, Double locationLng, Integer locationRadiusM, String gestureSequence, String passwordPlain) {
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

        private final SimpleDateFormat sdf = new SimpleDateFormat("MM-dd HH:mm", Locale.getDefault());

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
            String displayTime = item.startAt != null && item.startAt.length() >= 16
                    ? item.startAt.substring(5, 16).replace("T", " ")
                    : sdf.format(new Date());
            holder.tvTime.setText(displayTime);
            holder.tvNote.setText(item.title == null || item.title.isEmpty() ? "老师发布了签到任务" : item.title);
            AttendanceTaskComposeBinder.bind(holder.composeTaskMeta, item.status, item.startAt);
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
            TextView tvTime, tvNote, tvStatus;
            Button btnSignIn;
            ComposeView composeTaskMeta;
            ComposeView composeMethodTags;

            ViewHolder(@NonNull View itemView) {
                super(itemView);
                tvTime = itemView.findViewById(R.id.tv_session_time);
                tvNote = itemView.findViewById(R.id.tv_session_note);
                tvStatus = itemView.findViewById(R.id.tv_session_status);
                composeTaskMeta = itemView.findViewById(R.id.compose_task_meta);
                btnSignIn = itemView.findViewById(R.id.btn_sign_in);
                composeMethodTags = itemView.findViewById(R.id.compose_method_tags);
            }
        }
    }

    private List<StudentCheckinTagComposeBinder.TagItem> buildTagItems(SessionItem item) {
        List<StudentCheckinTagComposeBinder.TagItem> tags = new ArrayList<>();
        tags.add(new StudentCheckinTagComposeBinder.TagItem("基础签到", false));
        if (item.requiresLocation()) tags.add(new StudentCheckinTagComposeBinder.TagItem("定位签到", true));
        if (item.requiresGesture()) tags.add(new StudentCheckinTagComposeBinder.TagItem("手势签到", true));
        if (item.requiresPassword()) tags.add(new StudentCheckinTagComposeBinder.TagItem("密码签到", true));
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
        BottomSheetDialog dialog = new BottomSheetDialog(requireContext());
        View content = LayoutInflater.from(requireContext()).inflate(R.layout.dialog_student_checkin_submit, null, false);
        dialog.setContentView(content);
        if (dialog.getWindow() != null) {
            dialog.getWindow().setDimAmount(0.45f);
            dialog.getWindow().addFlags(WindowManager.LayoutParams.FLAG_DIM_BEHIND);
        }
        recyclerView.setAlpha(0.82f);
        dialog.setOnDismissListener(d -> recyclerView.setAlpha(1f));

        TextView tvTitle = content.findViewById(R.id.tv_task_title);
        TextView tvTaskStatus = content.findViewById(R.id.tv_task_status);
        TextView tvMethodHint = content.findViewById(R.id.tv_method_hint);
        TextView tvLocationRequired = content.findViewById(R.id.tv_location_required);
        TextView tvLocationCoords = content.findViewById(R.id.tv_location_coords);
        ComposeView composeRequiredTags = content.findViewById(R.id.compose_required_tags);
        View cardLocationRequired = content.findViewById(R.id.card_location_required);
        Button btnOpenMapPreview = content.findViewById(R.id.btn_open_map_preview);
        ComposeView composeGesturePad = content.findViewById(R.id.compose_gesture_pad);
        EditText etGesture = content.findViewById(R.id.et_gesture);
        LinearLayout layoutPasswordRow = content.findViewById(R.id.layout_password_row);
        EditText etPassword = content.findViewById(R.id.et_password);
        Button btnTogglePasswordVisibility = content.findViewById(R.id.btn_toggle_password_visibility);
        EditText etReason = content.findViewById(R.id.et_reason);
        Button btnSubmit = content.findViewById(R.id.btn_submit_checkin);

        tvTitle.setText(item.title == null || item.title.isEmpty() ? "签到任务" : item.title);
        tvTaskStatus.setText(item.submission == null ? "待签到" : resolveSubmissionText(item.submission));
        StudentCheckinTagComposeBinder.bind(composeRequiredTags, buildTagItems(item));
        tvMethodHint.setText(resolveMethodHint(item));
        cardLocationRequired.setVisibility(item.requiresLocation() ? View.VISIBLE : View.GONE);
        if (item.requiresLocation() && item.locationRadiusM != null) {
            tvLocationRequired.setText("本任务要求地理位置签到（半径 " + item.locationRadiusM + " 米），提交时将自动尝试定位。");
        }
        if (item.requiresLocation()) {
            String lat = item.locationLat == null ? "-" : String.format(Locale.getDefault(), "%.6f", item.locationLat);
            String lng = item.locationLng == null ? "-" : String.format(Locale.getDefault(), "%.6f", item.locationLng);
            tvLocationCoords.setText("中心点: " + lat + ", " + lng);
            btnOpenMapPreview.setOnClickListener(v -> openMapPreview(item));
        }

        etGesture.setVisibility(item.requiresGesture() ? View.VISIBLE : View.GONE);
        layoutPasswordRow.setVisibility(item.requiresPassword() ? View.VISIBLE : View.GONE);
        composeGesturePad.setVisibility(item.requiresGesture() ? View.VISIBLE : View.GONE);

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
            if (item.requiresGesture()) {
                StudentCheckinComposeBinder.bindGesturePad(
                        composeGesturePad,
                        true,
                        item.submission == null ? "" : item.submission.gestureInput,
                        null
                );
            }
            btnSubmit.setText("关闭");
            btnSubmit.setOnClickListener(v -> dialog.dismiss());
        } else {
            btnTogglePasswordVisibility.setVisibility(View.GONE);
            if (item.requiresGesture()) {
                StudentCheckinComposeBinder.bindGesturePad(
                        composeGesturePad,
                        false,
                        null,
                        sequence -> etGesture.setText(sequence)
                );
            }
            btnSubmit.setOnClickListener(v -> submitCheckinTask(item, etGesture.getText().toString().trim(),
                    etPassword.getText().toString().trim(),
                    etReason.getText().toString().trim(), dialog));
        }
        dialog.show();
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
        return "本任务要求: " + android.text.TextUtils.join(" + ", methods);
    }

    private void submitCheckinTask(SessionItem item, String gestureInput, String passwordInput, String reason, BottomSheetDialog dialog) {
        if (item.requiresGesture() && TextUtils.isEmpty(gestureInput)) {
            Toast.makeText(requireContext(), "该任务要求手势签到，请先绘制手势", Toast.LENGTH_SHORT).show();
            return;
        }
        if (item.requiresPassword() && TextUtils.isEmpty(passwordInput)) {
            Toast.makeText(requireContext(), "该任务要求签到密码，请先填写密码", Toast.LENGTH_SHORT).show();
            return;
        }

        CheckinSubmitRequest request = new CheckinSubmitRequest(studentId);
        request.gestureInput = gestureInput.isEmpty() ? null : gestureInput;
        request.passwordInput = passwordInput.isEmpty() ? null : passwordInput;
        request.reason = reason.isEmpty() ? null : reason;

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
        values.put("createdAt", task.createdAt);
        db.insertWithOnConflict("CheckinTask", null, values, android.database.sqlite.SQLiteDatabase.CONFLICT_REPLACE);
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
