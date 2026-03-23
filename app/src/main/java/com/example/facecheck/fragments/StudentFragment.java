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
import com.example.facecheck.data.repository.StudentRepository;
import com.example.facecheck.ui.student.StudentActivity;
import com.example.facecheck.adapters.StudentAdapter;
import com.example.facecheck.database.DatabaseHelper;
import com.example.facecheck.data.model.Student;
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
    // 在类顶部把 DatabaseHelper 声明改为同时持有 StudentRepository：
    private StudentRepository studentRepository;
    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_student, container, false);

// 在 onCreateView 里，原来 dbHelper 初始化的地方改为：
        dbHelper = new DatabaseHelper(getContext());
        studentRepository = new StudentRepository(getContext());
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
        studentList.clear();
        if (teacherId == -1) {
            adapter.notifyDataSetChanged();
            return;
        }

        // 1. 获取该教师的所有班级ID
        List<Long> classIds = dbHelper.getClassroomIdsByTeacher(teacherId);

        // 2. 为每个班级获取学生，并聚合结果
        if (classIds.isEmpty()) {
            adapter.notifyDataSetChanged();
            return;
        }

        for (long classId : classIds) {
            studentRepository.getStudentsByClass(classId, new StudentRepository.StudentsCallback() {
                @Override
                public void onSuccess(List<Student> students) {
                    // 注意：这里会有多次回调，需要处理好UI更新
                    // 一个简单的策略是，每次都重新加载所有数据
                    reloadAllStudentsFromDb();
                }

                @Override
                public void onError(String message) {
                    // 简单处理，可以添加更复杂的错误提示
                }
            });
        }
    }

    private void reloadAllStudentsFromDb() {
        if (teacherId != -1) {
            List<Student> allStudents = dbHelper.getStudentsByTeacher(teacherId);
            studentList.clear();
            studentList.addAll(allStudents);
            // 在UI线程上更新
            if (getActivity() != null) {
                getActivity().runOnUiThread(() -> adapter.notifyDataSetChanged());
            }
        }
    }
}