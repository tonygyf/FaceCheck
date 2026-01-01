package com.example.facecheck.fragments;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ListView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.example.facecheck.R;
import com.example.facecheck.adapters.StudentCourseAdapter;
import com.example.facecheck.data.model.Course;

import java.util.ArrayList;
import java.util.List;

public class StudentCoursesFragment extends Fragment {

    private StudentCourseAdapter adapter;
    private List<Course> courseList;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
            @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_student_courses, container, false);

        ListView listView = view.findViewById(R.id.list_courses);
        Button btnSelect = view.findViewById(R.id.btn_select_course);

        courseList = generateMockCourses();
        adapter = new StudentCourseAdapter(courseList);
        listView.setAdapter(adapter);

        btnSelect.setOnClickListener(v -> {
            Course selectedCourse = adapter.getSelectedCourse();
            if (selectedCourse == null) {
                Toast.makeText(requireContext(), "请先选择一个课程", Toast.LENGTH_SHORT).show();
                return;
            }

            SharedPreferences prefs = requireContext().getSharedPreferences("student_prefs",
                    android.content.Context.MODE_PRIVATE);
            prefs.edit().putString("selected_course_name", selectedCourse.getName())
                    .putString("selected_course_id", selectedCourse.getId())
                    .apply();

            Toast.makeText(requireContext(), "已选课程: " + selectedCourse.getName() + "\n即将进入考勤系统", Toast.LENGTH_LONG)
                    .show();

            // 可以在这里跳转到考勤页面，或者让用户手动点击中间的打卡按钮
        });

        return view;
    }

    private List<Course> generateMockCourses() {
        List<Course> list = new ArrayList<>();
        list.add(new Course("C001", "高等数学 A(1)", "李教授", "周一 08:30 - 10:10", "教A-101", "基础课程，重点考察微积分。", ""));
        list.add(new Course("C002", "大学物理 B", "王讲师", "周一 14:00 - 15:40", "理B-302", "包含力学与电磁学基础。", ""));
        list.add(new Course("C003", "移动应用开发", "陈工程师", "周二 10:30 - 12:10", "信C-504", "Android应用实战项目。", ""));
        list.add(new Course("C004", "操作系统原理", "刘副教授", "周三 08:30 - 10:10", "教A-205", "深入理解系统内核架构。", ""));
        list.add(new Course("C005", "机器学习入门", "张博士", "周四 16:00 - 17:40", "实训-101", "动手实践深度学习模型。", ""));
        list.add(new Course("C006", "英语听说(三级)", "Sarah Zhang", "周五 10:30 - 12:10", "语言-201", "全英文浸入式教学。", ""));
        return list;
    }
}
