package com.example.facecheck.fragments;

import android.content.Intent;
import android.database.Cursor;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
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
import com.example.facecheck.data.model.Classroom;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import androidx.appcompat.widget.TooltipCompat;


import java.util.ArrayList;
import java.util.Calendar;
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
        fabAddClassroom.setOnClickListener(v -> showAddClassroomDialog());
        
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
    
    private void showAddClassroomDialog() {
        android.app.AlertDialog.Builder builder = new android.app.AlertDialog.Builder(getContext());
        builder.setTitle("添加新班级");
        
        LinearLayout container = new LinearLayout(getContext());
        container.setOrientation(LinearLayout.VERTICAL);
        int padding = (int) (16 * getResources().getDisplayMetrics().density);
        container.setPadding(padding, padding, padding, padding);
        
        final EditText etName = new EditText(getContext());
        etName.setHint("班级名称");
        final EditText etYear = new EditText(getContext());
        etYear.setHint("年份(如 2024)");
        etYear.setInputType(android.text.InputType.TYPE_CLASS_NUMBER);
        
        container.addView(etName);
        container.addView(etYear);
        
        // 默认填入当前年份
        Calendar calendar = Calendar.getInstance();
        etYear.setText(String.valueOf(calendar.get(Calendar.YEAR)));
        
        builder.setView(container);
        builder.setPositiveButton("添加", (d, which) -> {
            String name = etName.getText().toString().trim();
            String yearStr = etYear.getText().toString().trim();
            int year = yearStr.isEmpty() ? calendar.get(Calendar.YEAR) : Integer.parseInt(yearStr);
            if (name.isEmpty()) {
                Toast.makeText(getContext(), "请输入班级名称", Toast.LENGTH_SHORT).show();
                return;
            }
            long id = dbHelper.insertClassroom(teacherId, name, year, null);
            if (id != -1) {
                Toast.makeText(getContext(), "班级创建成功", Toast.LENGTH_SHORT).show();
                loadClassrooms();
            } else {
                Toast.makeText(getContext(), "班级创建失败", Toast.LENGTH_SHORT).show();
            }
        });
        builder.setNegativeButton("取消", null);
        builder.show();
    }
}