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
import com.example.facecheck.activities.ClassroomActivity;

public class ClassroomFragment extends Fragment {

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_classroom, container, false);
        
        Button btnManageClassrooms = view.findViewById(R.id.btnManageClassrooms);
        btnManageClassrooms.setOnClickListener(v -> {
            Intent intent = new Intent(getActivity(), ClassroomActivity.class);
            startActivity(intent);
        });
        
        return view;
    }
}