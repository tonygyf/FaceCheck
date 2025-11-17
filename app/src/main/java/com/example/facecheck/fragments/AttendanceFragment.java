package com.example.facecheck.fragments;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.CalendarView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.facecheck.R;
import com.example.facecheck.adapters.AttendanceDayAdapter;
import com.example.facecheck.ui.attendance.AttendanceActivity;
import com.example.facecheck.database.DatabaseHelper;


import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;

public class AttendanceFragment extends Fragment {

    private CalendarView calendarView;
    private TextView tvSelectedDate;
    private RecyclerView recyclerView;
    private DatabaseHelper dbHelper;
    private String selectedDate;
    private AttendanceDayAdapter dayAdapter;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_attendance, container, false);
        
        // 初始化数据库
        dbHelper = new DatabaseHelper(getContext());
        
        // 初始化视图
        calendarView = view.findViewById(R.id.calendar_view);
        tvSelectedDate = view.findViewById(R.id.tv_selected_date);
        recyclerView = view.findViewById(R.id.recycler_attendance);
        
        // 设置日历选择监听
        Calendar calendar = Calendar.getInstance();
        selectedDate = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(calendar.getTime());
        tvSelectedDate.setText("选择日期: " + selectedDate);
        
        calendarView.setOnDateChangeListener((view1, year, month, dayOfMonth) -> {
            Calendar selected = Calendar.getInstance();
            selected.set(year, month, dayOfMonth);
            selectedDate = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(selected.getTime());
            tvSelectedDate.setText("选择日期: " + selectedDate);
            loadAttendanceData(selectedDate);
        });
        
        // 设置RecyclerView
        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        dayAdapter = new AttendanceDayAdapter();
        recyclerView.setAdapter(dayAdapter);
        
        // 加载当前日期的考勤数据
        loadAttendanceData(selectedDate);
        
        return view;
    }
    
    @Override
    public void onResume() {
        super.onResume();
        // 每次恢复时刷新数据
        if (selectedDate != null) {
            loadAttendanceData(selectedDate);
        }
    }
    
    private void loadAttendanceData(String date) {
        if (getActivity() == null) return;
        try {
            android.content.SharedPreferences prefs = getActivity().getSharedPreferences("user_prefs", android.content.Context.MODE_PRIVATE);
            String role = prefs.getString("user_role", "teacher");
            long teacherId = prefs.getLong("teacher_id", -1);
            long studentId = prefs.getLong("student_id", -1);

            // 计算当日起止时间戳
            java.text.SimpleDateFormat sdf = new java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.getDefault());
            java.util.Date d = sdf.parse(date);
            java.util.Calendar cal = java.util.Calendar.getInstance();
            cal.setTime(d);
            cal.set(java.util.Calendar.HOUR_OF_DAY, 0);
            cal.set(java.util.Calendar.MINUTE, 0);
            cal.set(java.util.Calendar.SECOND, 0);
            cal.set(java.util.Calendar.MILLISECOND, 0);
            long startTs = cal.getTimeInMillis();
            long endTs = startTs + 24L * 60 * 60 * 1000;

            // 查询当日所有考勤结果（按班级、学生聚合）
            android.database.Cursor cursor;
            if ("student".equals(role) && studentId != -1) {
                cursor = dbHelper.getAttendanceResultsByStudentAndDateRange(studentId, startTs, endTs);
            } else if (teacherId != -1) {
                cursor = dbHelper.getAttendanceResultsByTeacherAndDateRange(teacherId, startTs, endTs);
            } else {
                dayAdapter.updateItems(java.util.Collections.emptyList());
                return;
            }
            java.util.Map<String, java.util.LinkedHashMap<Long, StudentAgg>> grouped = new java.util.LinkedHashMap<>();

            if (cursor != null && cursor.moveToFirst()) {
                do {
                    String className = cursor.getString(cursor.getColumnIndexOrThrow("className"));
                    long resStudentId = cursor.getLong(cursor.getColumnIndexOrThrow("studentId"));
                    String status = cursor.getString(cursor.getColumnIndexOrThrow("status"));
                    long decidedAt = cursor.getLong(cursor.getColumnIndexOrThrow("decidedAt"));

                    java.util.LinkedHashMap<Long, StudentAgg> students = grouped.get(className);
                    if (students == null) {
                        students = new java.util.LinkedHashMap<>();
                        grouped.put(className, students);
                    }

                    StudentAgg agg = students.get(resStudentId);
                    if (agg == null) {
                        // 查询学生基本信息
                        android.database.Cursor sc = dbHelper.getStudentById(resStudentId);
                        String name = "未知";
                        String sid = "";
                        if (sc != null && sc.moveToFirst()) {
                            name = sc.getString(sc.getColumnIndexOrThrow("name"));
                            sid = sc.getString(sc.getColumnIndexOrThrow("sid"));
                            sc.close();
                        } else if (sc != null) {
                            sc.close();
                        }
                        agg = new StudentAgg(name, sid);
                        students.put(resStudentId, agg);
                    }

                    if ("Present".equals(status)) {
                        agg.presentCount++;
                        // 格式化时间为 HH:mm
                        java.text.SimpleDateFormat tf = new java.text.SimpleDateFormat("HH:mm", java.util.Locale.getDefault());
                        String t = tf.format(new java.util.Date(decidedAt));
                        agg.times.add(t);
                    }
                } while (cursor.moveToNext());
                cursor.close();
            } else if (cursor != null) {
                cursor.close();
            }

            // 构建适配器项
            java.util.List<AttendanceDayAdapter.Item> items = new java.util.ArrayList<>();
            for (java.util.Map.Entry<String, java.util.LinkedHashMap<Long, StudentAgg>> entry : grouped.entrySet()) {
                String className = entry.getKey();
                items.add(AttendanceDayAdapter.Item.header(className));
                for (StudentAgg agg : entry.getValue().values()) {
                    String timesText = String.join(", ", agg.times);
                    items.add(AttendanceDayAdapter.Item.student(className, agg.name, agg.sid, agg.presentCount, timesText));
                }
            }

            dayAdapter.updateItems(items);
        } catch (Throwable t) {
            android.util.Log.e("AttendanceFragment", "加载考勤数据失败: " + t.getMessage(), t);
            dayAdapter.updateItems(java.util.Collections.emptyList());
        }
    }

    // 学生聚合结构
    private static class StudentAgg {
        String name;
        String sid;
        int presentCount = 0;
        java.util.List<String> times = new java.util.ArrayList<>();
        StudentAgg(String name, String sid) { this.name = name; this.sid = sid; }
    }
}