package com.example.facecheck.fragments;

import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.NumberPicker;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.facecheck.R;
import com.example.facecheck.adapters.ClassroomAdapter;
import com.example.facecheck.data.model.Classroom;
import com.example.facecheck.ui.classroom.ClassroomActivity;
import com.example.facecheck.ui.classroom.ClassroomViewModel;
import com.example.facecheck.ui.classroom.ClassroomViewModelFactory;
import com.example.facecheck.utils.SessionManager;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import androidx.appcompat.widget.TooltipCompat;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;

public class ClassroomFragment extends Fragment {

    private RecyclerView recyclerView;
    private ClassroomAdapter adapter;
    private ClassroomViewModel viewModel;
    private SessionManager sessionManager;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_classroom, container, false);

        sessionManager = new SessionManager(requireContext());

        setupRecyclerView(view);
        setupViewModel();
        setupFab(view);

        return view;
    }

    @Override
    public void onResume() {
        super.onResume();
        // KEY-POINT: This is the entry point for triggering classroom data synchronization each time the fragment becomes visible.
        // It ensures the classroom list is always up-to-date with the server.
        viewModel.loadClassrooms();
    }

    private void setupRecyclerView(View view) {
        recyclerView = view.findViewById(R.id.recycler_classrooms);
        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        adapter = new ClassroomAdapter(new ArrayList<>());
        adapter.setOnItemClickListener(classroom -> {
            Intent intent = new Intent(getActivity(), ClassroomActivity.class);
            intent.putExtra("classroom_id", classroom.getId());
            startActivity(intent);
        });
        recyclerView.setAdapter(adapter);
    }

    private void setupViewModel() {
        viewModel = new ViewModelProvider(this, new ClassroomViewModelFactory(requireActivity().getApplication()))
                .get(ClassroomViewModel.class);

        viewModel.classrooms.observe(getViewLifecycleOwner(), classrooms -> {
            adapter.updateClassrooms(classrooms);
        });

        viewModel.errorMessage.observe(getViewLifecycleOwner(), message -> {
            if (message != null && !message.isEmpty()) {
                Toast.makeText(getContext(), message, Toast.LENGTH_LONG).show();
                viewModel.clearErrorMessage(); // Consume the message
            }
        });

        viewModel.classroomCreated.observe(getViewLifecycleOwner(), isCreated -> {
            if (isCreated != null && isCreated) {
                Toast.makeText(getContext(), "班级创建成功！", Toast.LENGTH_SHORT).show();
                viewModel.clearClassroomCreatedEvent(); // Consume the event
            }
        });

        // Initial load
        viewModel.loadClassrooms();
    }

    private void setupFab(View view) {
        FloatingActionButton fabAddClassroom = view.findViewById(R.id.fab_add_classroom);
        TooltipCompat.setTooltipText(fabAddClassroom, "添加新班级");
        fabAddClassroom.setOnClickListener(v -> showAddClassroomDialog());
    }

    private void showAddClassroomDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(requireContext());
        builder.setTitle("添加新班级");

        LinearLayout layout = new LinearLayout(requireContext());
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setPadding(50, 20, 50, 20);

        final EditText classNameInput = new EditText(requireContext());
        classNameInput.setHint("班级名称 (例如：计算机科学2025级1班)");
        layout.addView(classNameInput);

        final NumberPicker yearPicker = new NumberPicker(requireContext());
        int currentYear = Calendar.getInstance().get(Calendar.YEAR);
        yearPicker.setMinValue(currentYear - 5);
        yearPicker.setMaxValue(currentYear + 5);
        yearPicker.setValue(currentYear);
        layout.addView(yearPicker);

        builder.setView(layout);

        builder.setPositiveButton("添加", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                String className = classNameInput.getText().toString().trim();
                int classYear = yearPicker.getValue();
                if (!className.isEmpty()) {
                    viewModel.createClassroom(className, classYear);
                } else {
                    Toast.makeText(getContext(), "班级名称不能为空", Toast.LENGTH_SHORT).show();
                }
            }
        });
        builder.setNegativeButton("取消", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                dialog.cancel();
            }
        });

        builder.show();
    }
}
