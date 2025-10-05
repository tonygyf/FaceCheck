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
import com.example.facecheck.activities.AttendanceActivity;
import com.example.facecheck.database.DatabaseHelper;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.tooltip.TooltipCompat;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;

public class AttendanceFragment extends Fragment {

    private CalendarView calendarView;
    private TextView tvSelectedDate;
    private RecyclerView recyclerView;
    private FloatingActionButton fabNewAttendance;
    private DatabaseHelper dbHelper;
    private String selectedDate;

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
        fabNewAttendance = view.findViewById(R.id.fab_new_attendance);
        
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
        
        // 设置添加考勤按钮
        fabNewAttendance.setOnClickListener(v -> {
            Intent intent = new Intent(getActivity(), AttendanceActivity.class);
            intent.putExtra("date", selectedDate);
            startActivity(intent);
        });
        
        // 添加Tooltip提示
        TooltipCompat.setTooltipText(fabNewAttendance, "添加新考勤记录");
        
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
        // TODO: 从数据库加载指定日期的考勤数据
        // 这里先使用空列表，实际应用中应该从数据库加载
    }
}