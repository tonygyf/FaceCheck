package com.example.facecheck.fragments;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.example.facecheck.R;

public class StudentCoursesFragment extends Fragment {

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_student_courses, container, false);

        ListView listView = view.findViewById(R.id.list_courses);
        Button btnSelect = view.findViewById(R.id.btn_select_course);

        String[] fakeCourses = new String[]{"高数A(周二 8:00)", "线性代数(周四 10:00)", "英语听说(周一 14:00)", "计算机导论(周三 16:00)"};
        ArrayAdapter<String> adapter = new ArrayAdapter<>(requireContext(), android.R.layout.simple_list_item_single_choice, fakeCourses);
        listView.setAdapter(adapter);
        listView.setChoiceMode(ListView.CHOICE_MODE_SINGLE);

        btnSelect.setOnClickListener(v -> {
            int pos = listView.getCheckedItemPosition();
            if (pos == ListView.INVALID_POSITION) {
                Toast.makeText(requireContext(), "请先选择课程", Toast.LENGTH_SHORT).show();
                return;
            }
            String course = fakeCourses[pos];
            SharedPreferences prefs = requireContext().getSharedPreferences("student_prefs", android.content.Context.MODE_PRIVATE);
            prefs.edit().putString("selected_course", course).apply();
            Toast.makeText(requireContext(), "已选: " + course + "（演示）", Toast.LENGTH_SHORT).show();
        });

        return view;
    }
}

