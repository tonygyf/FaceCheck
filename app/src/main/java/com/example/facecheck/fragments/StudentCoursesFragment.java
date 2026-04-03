package com.example.facecheck.fragments;

import android.content.Intent;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.facecheck.R;
import com.example.facecheck.api.ApiService;
import com.example.facecheck.api.CheckinSubmitRequest;
import com.example.facecheck.api.CheckinTaskListResponse;
import com.example.facecheck.api.MySubmissionsResponse;
import com.example.facecheck.api.RetrofitClient;
import com.example.facecheck.database.DatabaseHelper;
import com.example.facecheck.utils.SessionManager;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.google.android.material.card.MaterialCardView;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;

public class StudentCoursesFragment extends Fragment {

    private RecyclerView recyclerView;
    private TextView tvEmptyHint;
    private TextView tvClassName;
    private FloatingActionButton fabRefresh;

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
        refreshAllData();

        return view;
    }

    private void initViews(View view) {
        recyclerView = view.findViewById(R.id.recycler_sessions);
        tvEmptyHint = view.findViewById(R.id.tv_empty_hint);
        tvClassName = view.findViewById(R.id.tv_class_name);
        fabRefresh = view.findViewById(R.id.fab_refresh);

        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        adapter = new SignInSessionAdapter();
        recyclerView.setAdapter(adapter);

        fabRefresh.setOnClickListener(v -> {
            Toast.makeText(requireContext(), "刷新中...", Toast.LENGTH_SHORT).show();
            refreshAllData();
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
                        refreshAllData();
                        d.dismiss();
                    })
                    .show();
        });
    }

    private void refreshAllData() {
        syncTasksFromApi(() -> syncMySubmissionsFromApi(this::loadAttendanceSessions));
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
                            submissionMap.put(item.taskId, new SubmissionItem(item.id, item.finalResult, item.reason));
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
                SubmissionItem submission = submissionMap.get(taskId);
                SessionItem item = new SessionItem(taskId, taskClassId, title, status, startAt, endAt, submission);
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
        refreshAllData();
    }

    private static class SessionItem {
        long taskId;
        long classId;
        String title;
        String status;
        String startAt;
        String endAt;
        SubmissionItem submission;

        SessionItem(long taskId, long classId, String title, String status, String startAt, String endAt, SubmissionItem submission) {
            this.taskId = taskId;
            this.classId = classId;
            this.title = title;
            this.status = status;
            this.startAt = startAt;
            this.endAt = endAt;
            this.submission = submission;
        }
    }

    private static class SubmissionItem {
        long submissionId;
        String finalResult;
        String reason;

        SubmissionItem(long submissionId, String finalResult, String reason) {
            this.submissionId = submissionId;
            this.finalResult = finalResult;
            this.reason = reason;
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
            holder.tvType.setText("班级签到");
            holder.tvNote.setText(item.title == null || item.title.isEmpty() ? "老师发布了签到任务" : item.title);

            MaterialCardView card = (MaterialCardView) holder.itemView;
            RecyclerView.LayoutParams lp = (RecyclerView.LayoutParams) card.getLayoutParams();
            if (item.submission != null) {
                lp.setMargins(80, lp.topMargin, 8, lp.bottomMargin);
                card.setCardBackgroundColor(androidx.core.content.ContextCompat.getColor(holder.itemView.getContext(), R.color.surface));
                holder.tvStatus.setText(resolveSubmissionText(item.submission));
                holder.tvStatus.setTextColor(holder.itemView.getContext().getColor(android.R.color.holo_blue_dark));
                holder.btnSignIn.setText("查看");
                holder.btnSignIn.setVisibility(View.VISIBLE);
                holder.btnSignIn.setOnClickListener(v -> showSubmitBottomSheet(item, true));
            } else {
                lp.setMargins(8, lp.topMargin, 80, lp.bottomMargin);
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
            TextView tvTime, tvType, tvNote, tvStatus;
            Button btnSignIn;

            ViewHolder(@NonNull View itemView) {
                super(itemView);
                tvTime = itemView.findViewById(R.id.tv_session_time);
                tvType = itemView.findViewById(R.id.tv_session_type);
                tvNote = itemView.findViewById(R.id.tv_session_note);
                tvStatus = itemView.findViewById(R.id.tv_session_status);
                btnSignIn = itemView.findViewById(R.id.btn_sign_in);
            }
        }
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
        EditText etGesture = content.findViewById(R.id.et_gesture);
        EditText etPassword = content.findViewById(R.id.et_password);
        EditText etReason = content.findViewById(R.id.et_reason);
        Button btnSubmit = content.findViewById(R.id.btn_submit_checkin);

        tvTitle.setText(item.title == null || item.title.isEmpty() ? "签到任务" : item.title);
        tvTaskStatus.setText(item.submission == null ? "待签到" : resolveSubmissionText(item.submission));

        if (readonly) {
            etGesture.setEnabled(false);
            etPassword.setEnabled(false);
            etReason.setEnabled(false);
            btnSubmit.setText("关闭");
            btnSubmit.setOnClickListener(v -> dialog.dismiss());
        } else {
            btnSubmit.setOnClickListener(v -> submitCheckinTask(item, etGesture.getText().toString().trim(),
                    etPassword.getText().toString().trim(),
                    etReason.getText().toString().trim(), dialog));
        }
        dialog.show();
    }

    private void submitCheckinTask(SessionItem item, String gestureInput, String passwordInput, String reason, BottomSheetDialog dialog) {
        CheckinSubmitRequest request = new CheckinSubmitRequest(studentId);
        request.gestureInput = gestureInput.isEmpty() ? null : gestureInput;
        request.passwordInput = passwordInput.isEmpty() ? null : passwordInput;
        request.reason = reason.isEmpty() ? null : reason;
        apiService.submitCheckin(sessionManager.getApiKey(), item.taskId, request)
                .enqueue(new retrofit2.Callback<com.example.facecheck.api.ApiCreateResponse>() {
                    @Override
                    public void onResponse(retrofit2.Call<com.example.facecheck.api.ApiCreateResponse> call,
                                           retrofit2.Response<com.example.facecheck.api.ApiCreateResponse> response) {
                        if (response.isSuccessful()) {
                            Toast.makeText(requireContext(), "提交成功", Toast.LENGTH_SHORT).show();
                            dialog.dismiss();
                            refreshAllData();
                        } else {
                            Toast.makeText(requireContext(), "提交失败", Toast.LENGTH_SHORT).show();
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
