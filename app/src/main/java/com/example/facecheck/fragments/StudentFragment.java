package com.example.facecheck.fragments;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.facecheck.R;
import com.example.facecheck.ui.student.StudentActivity;
import com.example.facecheck.adapters.StudentAdapter;
import com.example.facecheck.database.DatabaseHelper;
import com.example.facecheck.models.Student;
import com.google.android.material.floatingactionbutton.FloatingActionButton;

import java.util.ArrayList;
import java.util.List;

public class StudentFragment extends Fragment {
    private DatabaseHelper dbHelper;
    private RecyclerView recyclerView;
    private StudentAdapter adapter;
    private List<Student> studentList;
    private FloatingActionButton fabAddStudent;
    private long teacherId;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_student, container, false);
        
        // 初始化数据库
        dbHelper = new DatabaseHelper(getContext());
        
        // 获取教师ID
        teacherId = getActivity().getSharedPreferences("user_prefs", 0).getLong("teacher_id", -1);
        
        // 初始化视图
        recyclerView = view.findViewById(R.id.recycler_students);
        fabAddStudent = view.findViewById(R.id.fab_add_student);
        
        // 设置RecyclerView
        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        studentList = new ArrayList<>();
        adapter = new StudentAdapter(studentList);
        recyclerView.setAdapter(adapter);
        
        // 设置点击事件
        adapter.setOnItemClickListener(student -> {
            // 跳转到学生详情/编辑页面
            Intent intent = new Intent(getActivity(), StudentActivity.class);
            intent.putExtra("student_id", student.getId());
            startActivity(intent);
        });
        
        // 设置FAB点击事件
        fabAddStudent.setOnClickListener(v -> {
            // 跳转到添加学生页面
            Intent intent = new Intent(getActivity(), StudentActivity.class);
            intent.putExtra("teacher_id", teacherId);
            startActivity(intent);
        });
        
        // 加载数据
        loadStudents();
        
        return view;
    }
    
    @Override
    public void onResume() {
        super.onResume();
        // 刷新数据
        loadStudents();
    }
    
    private void loadStudents() {
        // 清空列表
        studentList.clear();
        
        // TODO: 从数据库加载学生数据
        // 这里先添加一些测试数据
        studentList.add(new Student(1, "张三", "S001", "男"));
        studentList.add(new Student(2, "李四", "S002", "女"));
        studentList.add(new Student(3, "王五", "S003", "男"));
        
        // 通知适配器数据已更改
        adapter.notifyDataSetChanged();
    }
}