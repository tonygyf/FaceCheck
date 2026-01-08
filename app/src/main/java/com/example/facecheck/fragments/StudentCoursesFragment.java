package com.example.facecheck.fragments;

import android.content.Intent;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.facecheck.R;
import com.example.facecheck.database.DatabaseHelper;
import com.example.facecheck.ui.student.StudentSignInActivity;
import com.google.android.material.floatingactionbutton.FloatingActionButton;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

/**
 * 学生签到页面
 * 显示学生班级的活跃考勤会话，允许学生进行自拍签到
 */
public class StudentCoursesFragment extends Fragment {

    private RecyclerView recyclerView;
    private TextView tvEmptyHint;
    private TextView tvClassName;
    private FloatingActionButton fabRefresh;

    private DatabaseHelper dbHelper;
    private long studentId = -1;
    private long classId = -1;
    private SignInSessionAdapter adapter;
    private List<SessionItem> sessionItems = new ArrayList<>();

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
            @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_student_sign_in, container, false);

        dbHelper = new DatabaseHelper(requireContext());

        // 从 SharedPreferences 获取学生信息
        SharedPreferences prefs = requireContext().getSharedPreferences("user_prefs",
                android.content.Context.MODE_PRIVATE);
        // user_id 在 LoginActivity 写入的是 "student_id" 或 "teacher_id", 这里 keys 可能是
        // "user_id" 如果之前有逻辑?
        // 检查 LoginActivity: ed.putLong("student_id", success.userId)
        // Check student_id key.
        studentId = prefs.getLong("student_id", -1); // changed from user_id to student_id based on LoginActivity
        classId = prefs.getLong("class_id", -1);

        initViews(view);
        loadAttendanceSessions();

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
            loadAttendanceSessions();
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

        // 显示班级名称
        String className = dbHelper.getClassroomNameById(classId);
        if (tvClassName != null) {
            tvClassName.setText(className != null ? className : "未知班级");
        }

        // 获取该班级的所有活跃 MANUAL 类型考勤会话
        Cursor cursor = dbHelper.getActiveManualAttendanceSessionsByClass(classId);
        if (cursor != null && cursor.moveToFirst()) {
            do {
                long sessionId = cursor.getLong(cursor.getColumnIndexOrThrow("id"));
                long sessionClassId = cursor.getLong(cursor.getColumnIndexOrThrow("classId"));
                long startedAt = cursor.getLong(cursor.getColumnIndexOrThrow("startedAt"));
                String attendanceType = cursor.getString(cursor.getColumnIndexOrThrow("attendanceType"));
                String note = cursor.getString(cursor.getColumnIndexOrThrow("note"));

                // 检查学生是否已签到
                boolean signedIn = dbHelper.isStudentSignedIn(sessionId, studentId);

                SessionItem item = new SessionItem(sessionId, sessionClassId, startedAt, attendanceType, note,
                        signedIn);
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
        recyclerView.setVisibility(View.GONE);
    }

    @Override
    public void onResume() {
        super.onResume();
        loadAttendanceSessions();
    }

    // ========== 内部数据类 ==========

    private static class SessionItem {
        long sessionId;
        long classId;
        long startedAt;
        String attendanceType;
        String note;
        boolean signedIn;

        SessionItem(long sessionId, long classId, long startedAt, String attendanceType, String note,
                boolean signedIn) {
            this.sessionId = sessionId;
            this.classId = classId;
            this.startedAt = startedAt;
            this.attendanceType = attendanceType;
            this.note = note;
            this.signedIn = signedIn;
        }
    }

    // ========== Adapter ==========

    private class SignInSessionAdapter extends RecyclerView.Adapter<SignInSessionAdapter.ViewHolder> {

        private final SimpleDateFormat sdf = new SimpleDateFormat("MM-dd HH:mm", Locale.getDefault());

        @NonNull
        @Override
        public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View v = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.item_sign_in_session, parent, false);
            return new ViewHolder(v);
        }

        @Override
        public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
            SessionItem item = sessionItems.get(position);

            holder.tvTime.setText(sdf.format(new Date(item.startedAt)));
            holder.tvType.setText("MANUAL".equals(item.attendanceType) ? "自拍签到" : "照片考勤");
            holder.tvNote.setText(item.note != null && !item.note.isEmpty() ? item.note : "教师发起的考勤任务");

            if (item.signedIn) {
                holder.tvStatus.setText("✓ 已签到");
                holder.tvStatus.setTextColor(holder.itemView.getContext().getColor(android.R.color.holo_green_dark));
                holder.btnSignIn.setVisibility(View.GONE);
            } else {
                holder.tvStatus.setText("待签到");
                holder.tvStatus.setTextColor(holder.itemView.getContext().getColor(android.R.color.holo_orange_dark));
                holder.btnSignIn.setVisibility(View.VISIBLE);
                holder.btnSignIn.setOnClickListener(v -> startSignIn(item));
            }
        }

        @Override
        public int getItemCount() {
            return sessionItems.size();
        }

        class ViewHolder extends RecyclerView.ViewHolder {
            TextView tvTime, tvType, tvNote, tvStatus;
            View btnSignIn;

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

    private void startSignIn(SessionItem item) {
        Intent intent = new Intent(requireContext(), StudentSignInActivity.class);
        intent.putExtra("session_id", item.sessionId);
        intent.putExtra("class_id", item.classId);
        intent.putExtra("student_id", studentId);
        startActivity(intent);
    }
}
