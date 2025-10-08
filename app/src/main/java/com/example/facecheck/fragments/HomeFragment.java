package com.example.facecheck.fragments;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Button;
import android.widget.Toast;

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
import androidx.appcompat.widget.TooltipCompat;

public class HomeFragment extends Fragment {
    
    private DatabaseHelper dbHelper;
    private TextView tvClassCount, tvStudentCount, tvAttendanceCount;
    private CardView cardClassroom, cardStudents, cardAttendance, cardQuickAttendance, cardFaceCorrection;
    private Button btnSync;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_home, container, false);
        
        // 初始化数据库
        dbHelper = new DatabaseHelper(getContext());
        
        // 初始化视图
        tvClassCount = view.findViewById(R.id.tv_class_count);
        tvStudentCount = view.findViewById(R.id.tv_student_count);
        tvAttendanceCount = view.findViewById(R.id.tv_attendance_count);
        
        cardClassroom = view.findViewById(R.id.card_classroom);
        cardStudents = view.findViewById(R.id.card_students);
        cardAttendance = view.findViewById(R.id.card_attendance);
        cardQuickAttendance = view.findViewById(R.id.card_quick_attendance);
        cardFaceCorrection = view.findViewById(R.id.card_face_correction);
        btnSync = view.findViewById(R.id.btn_sync);
        
        // 设置点击事件
        cardClassroom.setOnClickListener(v -> navigateToClassroom());
        cardStudents.setOnClickListener(v -> navigateToStudents());
        cardAttendance.setOnClickListener(v -> navigateToAttendance());
        cardQuickAttendance.setOnClickListener(v -> navigateToQuickAttendance());
        cardFaceCorrection.setOnClickListener(v -> startActivity(new Intent(getActivity(), FaceCorrectionActivity.class)));
        btnSync.setOnClickListener(v -> syncDatabase());
        
        // 添加Tooltip提示
        TooltipCompat.setTooltipText(cardClassroom, "管理您的班级信息");
        TooltipCompat.setTooltipText(cardStudents, "管理学生信息和人脸数据");
        TooltipCompat.setTooltipText(cardAttendance, "查看和管理考勤记录");
        TooltipCompat.setTooltipText(cardQuickAttendance, "快速开始人脸识别考勤");
        TooltipCompat.setTooltipText(btnSync, "与WebDAV服务器同步数据");
        
        // 加载统计数据
        loadStatistics();
        
        return view;
    }
    
    @Override
    public void onResume() {
        super.onResume();
        // 每次恢复时刷新统计数据
        loadStatistics();
    }
    
    private void loadStatistics() {
        // TODO: 从数据库加载实际统计数据
        tvClassCount.setText("5");
        tvStudentCount.setText("120");
        tvAttendanceCount.setText("350");
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
        // TODO: 实现WebDAV同步
        Toast.makeText(getContext(), "正在同步数据...", Toast.LENGTH_SHORT).show();
    }
}