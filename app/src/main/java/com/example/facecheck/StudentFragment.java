package com.example.facecheck;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.facecheck.adapters.StudentAdapter;
import com.example.facecheck.models.Student;
import com.example.facecheck.utils.DatabaseHelper;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.tooltip.TooltipDrawable;
import com.google.android.material.tooltip.TooltipUtils;

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
            intent.putExtra("class_id", -1); // 默认值，实际使用时应传入当前选中的班级ID
            startActivity(intent);
        });
        
        // 添加Tooltip提示
        fabAddStudent.setTooltipText("添加新学生");
        
        // 加载学生数据
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
        // TODO: 从数据库加载学生数据
        // 这里暂时使用模拟数据
        studentList.clear();
        
        // 模拟数据
        studentList.add(new Student(1, 1, "张三", "20210001", "男", "", System.currentTimeMillis()));
        studentList.add(new Student(2, 1, "李四", "20210002", "女", "", System.currentTimeMillis()));
        studentList.add(new Student(3, 2, "王五", "20210003", "男", "", System.currentTimeMillis()));
        
        // 通知适配器数据已更新
        adapter.notifyDataSetChanged();
    }
}