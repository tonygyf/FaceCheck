package com.example.facecheck.fragments;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.example.facecheck.R;
import com.example.facecheck.ui.profile.UserProfileActivity;

public class ProfileFragment extends Fragment {

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_profile, container, false);
        
        Button btnProfile = view.findViewById(R.id.btnProfile);
        btnProfile.setOnClickListener(v -> {
            // 从SharedPreferences获取当前登录的教师ID
            SharedPreferences prefs = getActivity().getSharedPreferences("user_prefs", Context.MODE_PRIVATE);
            long teacherId = prefs.getLong("teacher_id", -1);
            
            Intent intent = new Intent(getActivity(), UserProfileActivity.class);
            intent.putExtra("teacher_id", teacherId);
            startActivity(intent);
        });
        
        return view;
    }
}