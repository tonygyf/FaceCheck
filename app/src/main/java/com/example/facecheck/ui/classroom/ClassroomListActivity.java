package com.example.facecheck.ui.classroom;

import android.content.Intent;
import android.os.Bundle;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.example.facecheck.R;
import com.example.facecheck.adapters.ClassroomAdapter;
import com.example.facecheck.data.model.Classroom;
import com.example.facecheck.database.DatabaseHelper;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import java.util.ArrayList;
import java.util.List;

public class ClassroomListActivity extends AppCompatActivity {

    private DatabaseHelper dbHelper;
    private RecyclerView recyclerView;
    private ClassroomAdapter adapter;
    private List<Classroom> classroomList = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_classroom_list);

        dbHelper = new DatabaseHelper(this);
        recyclerView = findViewById(R.id.recyclerViewClassrooms);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));

        adapter = new ClassroomAdapter(classroomList);
        adapter.setOnItemClickListener(classroom -> {
            Intent intent = new Intent(ClassroomListActivity.this, ClassroomActivity.class);
            intent.putExtra("classroom_id", classroom.getId());
            intent.putExtra("classroom_name", classroom.getName());
            startActivity(intent);
        });
        recyclerView.setAdapter(adapter);

        FloatingActionButton fab = findViewById(R.id.fabAddClassroom);
        fab.setOnClickListener(view -> {
            // TODO: Implement add classroom dialog
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        loadClassrooms();
    }

    private void loadClassrooms() {
        // Assuming teacherId is 1 for now
        List<Classroom> updatedList = dbHelper.getAllClassroomsWithStudentCountAsList(1);
        classroomList.clear();
        classroomList.addAll(updatedList);
        adapter.notifyDataSetChanged();
    }
}
