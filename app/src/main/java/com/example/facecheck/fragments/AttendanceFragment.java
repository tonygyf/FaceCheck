package com.example.facecheck.fragments;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CalendarView;
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
        recyclerView = view.findViewById(R.id.recycler_attendance);

        // 初始化时设置并显示当前日期
        Calendar calendar = Calendar.getInstance();
        selectedDate = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(calendar.getTime());
        tvSelectedDate.setText("当前选择: " + selectedDate);

        calendarView.setOnDateChangeListener((view1, year, month, dayOfMonth) -> {
            Calendar selected = Calendar.getInstance();
            selected.set(year, month, dayOfMonth);
            selectedDate = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(selected.getTime());
            // 确保每次选择日期都更新文本
            tvSelectedDate.setText("当前选择: " + selectedDate);
            loadAttendanceData(selectedDate);
            updateCalendarDecorators();
        });

        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        dayAdapter = new AttendanceDayAdapter();
        recyclerView.setAdapter(dayAdapter);

        loadAttendanceData(selectedDate);
        updateCalendarDecorators();

        return view;
    }

    @Override
    public void onResume() {
        super.onResume();
        if (selectedDate != null) {
            loadAttendanceData(selectedDate);
            updateCalendarDecorators();
        }
    }

    private void loadAttendanceData(String date) {
        if (getActivity() == null) return;
        try {
//            先禁用teacherid，因为teacherid不再是之前存储方法了可能是sessionmanager存储的
//            long teacherId = getActivity().getSharedPreferences("user_prefs", android.content.Context.MODE_PRIVATE).getLong("teacher_id", -1);
//            if (teacherId == -1) {
//                dayAdapter.updateItems(java.util.Collections.emptyList());
//                return;
//            }

            android.database.Cursor cursor = dbHelper.getCheckinTasksByDate(date);
//            toast调试
//            int count = cursor == null ? -1 : cursor.getCount();
//            android.widget.Toast.makeText(getContext(), "查到记录数: " + count + " 日期:" + date, android.widget.Toast.LENGTH_LONG).show();

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
        java.util.Map<Integer, Integer> dates = new java.util.HashMap<>();
        dates.put(10, android.graphics.Color.RED);
        dates.put(15, android.graphics.Color.BLUE);
        decoratorView.setDates(dates);
    }
}
