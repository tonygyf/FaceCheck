package com.example.facecheck.fragments;

import android.content.Intent;
import android.database.Cursor;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.facecheck.R;
import com.example.facecheck.ui.classroom.ClassroomActivity;
import com.example.facecheck.adapters.ClassroomAdapter;
import com.example.facecheck.database.DatabaseHelper;
import com.example.facecheck.models.Classroom;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import androidx.appcompat.widget.TooltipCompat;


import java.util.ArrayList;
import java.util.List;

public class ClassroomFragment extends Fragment {

    private RecyclerView recyclerView;
    private ClassroomAdapter adapter;
    private List<Classroom> classroomList;
    private DatabaseHelper dbHelper;
    private FloatingActionButton fabAddClassroom;
    private long teacherId;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_classroom, container, false);
        
        // 初始化数据库
        dbHelper = new DatabaseHelper(getContext());
        
        // 获取教师ID
        teacherId = getActivity().getSharedPreferences("user_prefs", 0).getLong("teacher_id", -1);
        
        // 初始化视图
        recyclerView = view.findViewById(R.id.recycler_classrooms);
        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        
        fabAddClassroom = view.findViewById(R.id.fab_add_classroom);
        fabAddClassroom.setOnClickListener(v -> {
            Intent intent = new Intent(getActivity(), ClassroomActivity.class);
            intent.putExtra("action", "add");
            startActivity(intent);
        });
        
        // 添加Tooltip提示
        TooltipCompat.setTooltipText(fabAddClassroom, "添加新班级");
        
        // 加载班级数据
        loadClassrooms();
        
        return view;
    }
    
    @Override
    public void onResume() {
        super.onResume();
        // 每次恢复时刷新数据
        loadClassrooms();
    }
    
    private void loadClassrooms() {
        classroomList = new ArrayList<>();
        
        // 从数据库加载实际班级数据
        Cursor cursor = dbHelper.getClassroomsByTeacher(teacherId);
        if (cursor != null && cursor.moveToFirst()) {
            do {
                long id = cursor.getLong(cursor.getColumnIndexOrThrow("id"));
                long teacherId = cursor.getLong(cursor.getColumnIndexOrThrow("teacherId"));
                String name = cursor.getString(cursor.getColumnIndexOrThrow("name"));
                int year = cursor.getInt(cursor.getColumnIndexOrThrow("year"));
                String meta = cursor.getString(cursor.getColumnIndexOrThrow("meta"));
                
                classroomList.add(new Classroom(id, teacherId, name, year, meta));
            } while (cursor.moveToNext());
            cursor.close();
        }
        
        adapter = new ClassroomAdapter(classroomList);
        adapter.setOnItemClickListener(classroom -> {
            // 班级点击事件
            Intent intent = new Intent(getActivity(), ClassroomActivity.class);
            intent.putExtra("classroom_id", classroom.getId());
            intent.putExtra("action", "edit");
            startActivity(intent);
        });
        
        recyclerView.setAdapter(adapter);
    }
}