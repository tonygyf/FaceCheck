package com.example.facecheck.fragments;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CalendarView;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.facecheck.R;
import com.example.facecheck.adapters.AttendanceDayAdapter;
import com.example.facecheck.database.DatabaseHelper;
import com.example.facecheck.ui.checkin.CalendarDecoratorView;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Locale;

public class AttendanceFragment extends Fragment {

    private CalendarView calendarView;
    private CalendarDecoratorView decoratorView;
    private TextView tvSelectedDate;
    private ImageView ivPrevMonthIndicator, ivNextMonthIndicator;
    private RecyclerView recyclerView;
    private DatabaseHelper dbHelper;
    private String selectedDate;
    private AttendanceDayAdapter dayAdapter;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_attendance, container, false);

        dbHelper = new DatabaseHelper(getContext());

        calendarView = view.findViewById(R.id.calendar_view);
        decoratorView = view.findViewById(R.id.calendar_decorator_view);
        tvSelectedDate = view.findViewById(R.id.tv_selected_date);
        ivPrevMonthIndicator = view.findViewById(R.id.iv_prev_month_indicator);
        ivNextMonthIndicator = view.findViewById(R.id.iv_next_month_indicator);
        recyclerView = view.findViewById(R.id.recycler_attendance);

        Calendar calendar = Calendar.getInstance();
        selectedDate = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(calendar.getTime());
        tvSelectedDate.setText("当前选择: " + selectedDate);

        calendarView.setOnDateChangeListener((view1, year, month, dayOfMonth) -> {
            Calendar selected = Calendar.getInstance();
            selected.set(year, month, dayOfMonth);
            selectedDate = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(selected.getTime());
            tvSelectedDate.setText("当前选择: " + selectedDate);
            loadAttendanceData(selectedDate);
            updateCalendarDecorators();
            updateMonthArrowDecorators();
        });

        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        dayAdapter = new AttendanceDayAdapter();
        recyclerView.setAdapter(dayAdapter);

        loadAttendanceData(selectedDate);
        updateCalendarDecorators();
        updateMonthArrowDecorators();

        return view;
    }

    @Override
    public void onResume() {
        super.onResume();
        if (selectedDate != null) {
            loadAttendanceData(selectedDate);
            updateCalendarDecorators();
            updateMonthArrowDecorators();
        }
    }

    private void loadAttendanceData(String date) {
        if (getActivity() == null) return;
        try {
//            先不区分teacher
//            long teacherId = getActivity().getSharedPreferences("user_prefs", android.content.Context.MODE_PRIVATE).getLong("teacher_id", -1);
//            if (teacherId == -1) {
//                dayAdapter.updateItems(java.util.Collections.emptyList());
//                return;
//            }

            android.database.Cursor cursor = dbHelper.getCheckinTasksByDate(date);
            java.util.Map<String, java.util.List<com.example.facecheck.data.model.CheckinTask>> groupedTasks = new java.util.LinkedHashMap<>();
            if (cursor != null && cursor.moveToFirst()) {
                do {
                    long classId = cursor.getLong(cursor.getColumnIndexOrThrow("classId"));
                    String className = dbHelper.getClassNameById(classId);
                    if (className == null) className = "未知班级";

                    com.example.facecheck.data.model.CheckinTask task = new com.example.facecheck.data.model.CheckinTask();
                    task.setId(cursor.getLong(cursor.getColumnIndexOrThrow("id")));
                    task.setTitle(cursor.getString(cursor.getColumnIndexOrThrow("title")));
                    task.setStatus(cursor.getString(cursor.getColumnIndexOrThrow("status")));
                    task.setStartAt(cursor.getString(cursor.getColumnIndexOrThrow("startAt")));
                    task.setEndAt(cursor.getString(cursor.getColumnIndexOrThrow("endAt")));

                    if (!groupedTasks.containsKey(className)) {
                        groupedTasks.put(className, new java.util.ArrayList<>());
                    }
                    groupedTasks.get(className).add(task);
                } while (cursor.moveToNext());
                cursor.close();
            }

            java.util.List<AttendanceDayAdapter.Item> items = new java.util.ArrayList<>();
            for (java.util.Map.Entry<String, java.util.List<com.example.facecheck.data.model.CheckinTask>> entry : groupedTasks.entrySet()) {
                items.add(AttendanceDayAdapter.Item.header(entry.getKey()));
                for (com.example.facecheck.data.model.CheckinTask task : entry.getValue()) {
                    items.add(AttendanceDayAdapter.Item.task(task.getTitle(), task.getStatus()));
                }
            }
            dayAdapter.updateItems(items);
        } catch (Throwable t) {
            android.util.Log.e("AttendanceFragment", "加载签到任务失败: " + t.getMessage(), t);
            dayAdapter.updateItems(java.util.Collections.emptyList());
        }
    }

    private void updateCalendarDecorators() {
        if (getContext() == null) return;

        Calendar cal = Calendar.getInstance();
        cal.setTimeInMillis(calendarView.getDate());
        String yearMonth = new SimpleDateFormat("yyyy-MM", Locale.getDefault()).format(cal.getTime());

        android.database.Cursor cursor = dbHelper.getTaskStatusForMonth(yearMonth);
        java.util.Map<Integer, Integer> dateStatusMap = new java.util.HashMap<>();

        if (cursor != null && cursor.moveToFirst()) {
            do {
                String startAt = cursor.getString(cursor.getColumnIndexOrThrow("startAt"));
                String status = cursor.getString(cursor.getColumnIndexOrThrow("status"));

                try {
                    Calendar taskCal = Calendar.getInstance();
                    // 兼容 "2026-03-23T06:17:22.144Z" 和 "2026-03-23 06:17:22" 两种格式
                    String normalized = startAt.replace("T", " ");
                    if (normalized.contains(".")) {
                        normalized = normalized.substring(0, normalized.indexOf("."));
                    }
                    if (normalized.endsWith("Z")) {
                        normalized = normalized.substring(0, normalized.length() - 1);
                    }
                    taskCal.setTime(new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).parse(normalized));
                    int dayOfMonth = taskCal.get(Calendar.DAY_OF_MONTH);

                    boolean isActive = "ACTIVE".equalsIgnoreCase(status);
                    Integer currentColor = dateStatusMap.get(dayOfMonth);

                    if (currentColor == null || currentColor != android.graphics.Color.RED) {
                        dateStatusMap.put(dayOfMonth, isActive ? android.graphics.Color.RED : android.graphics.Color.BLUE);
                    }

                } catch (java.text.ParseException e) {
                    android.util.Log.e("AttendanceFragment", "解析日期失败: " + startAt, e);
                }
            } while (cursor.moveToNext());
            cursor.close();
        }

        decoratorView.setDates(dateStatusMap);
    }

    private void updateMonthArrowDecorators() {
        if (getContext() == null) return;

        Calendar cal = Calendar.getInstance();
        cal.setTimeInMillis(calendarView.getDate());

        cal.add(Calendar.MONTH, -1);
        String prevYearMonth = new SimpleDateFormat("yyyy-MM", Locale.getDefault()).format(cal.getTime());
        ivPrevMonthIndicator.setVisibility(dbHelper.hasActiveTasksInMonth(prevYearMonth) ? View.VISIBLE : View.GONE);

        cal.add(Calendar.MONTH, 2);
        String nextYearMonth = new SimpleDateFormat("yyyy-MM", Locale.getDefault()).format(cal.getTime());
        ivNextMonthIndicator.setVisibility(dbHelper.hasActiveTasksInMonth(nextYearMonth) ? View.VISIBLE : View.GONE);
    }
}
