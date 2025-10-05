package com.example.facecheck.fragments;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.example.facecheck.R;
import com.example.facecheck.activities.AttendanceActivity;

public class AttendanceFragment extends Fragment {

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_attendance, container, false);
        
        Button btnManageAttendance = view.findViewById(R.id.btnManageAttendance);
        btnManageAttendance.setOnClickListener(v -> {
            Intent intent = new Intent(getActivity(), AttendanceActivity.class);
            startActivity(intent);
        });
        
        return view;
    }
}