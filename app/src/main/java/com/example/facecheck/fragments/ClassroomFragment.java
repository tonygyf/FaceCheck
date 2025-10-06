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

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_classroom, container, false);
        
        // 初始化数据库
        dbHelper = new DatabaseHelper(getContext());
        
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
        
        // 模拟数据，实际应从数据库加载
        classroomList.add(new Classroom(1, 1, "计算机科学与技术1班", 2023, ""));
        classroomList.add(new Classroom(2, 1, "软件工程2班", 2023, ""));
        classroomList.add(new Classroom(3, 1, "人工智能3班", 2023, ""));
        classroomList.add(new Classroom(4, 1, "数据科学4班", 2023, ""));
        classroomList.add(new Classroom(5, 1, "网络工程5班", 2023, ""));
        
        // TODO: 从数据库加载实际班级数据
        // Cursor cursor = dbHelper.getClassroomsByTeacher(1);
        // if (cursor != null && cursor.moveToFirst()) {
        //     do {
        //         long id = cursor.getLong(cursor.getColumnIndex("id"));
        //         long teacherId = cursor.getLong(cursor.getColumnIndex("teacherId"));
        //         String name = cursor.getString(cursor.getColumnIndex("name"));
        //         int year = cursor.getInt(cursor.getColumnIndex("year"));
        //         String meta = cursor.getString(cursor.getColumnIndex("meta"));
        //         
        //         classroomList.add(new Classroom(id, teacherId, name, year, meta));
        //     } while (cursor.moveToNext());
        //     cursor.close();
        // }
        
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