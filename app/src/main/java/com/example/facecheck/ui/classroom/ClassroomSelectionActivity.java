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
import com.example.facecheck.ui.classroom.ClassroomCheckinStatusSheet;
import com.example.facecheck.ui.task.PublishTaskActivity;
import java.util.List;

public class ClassroomSelectionActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_simple_recycler_view);

        RecyclerView recyclerView = findViewById(R.id.recyclerView);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));

        DatabaseHelper dbHelper = new DatabaseHelper(this);
        long teacherId = getSharedPreferences("user_prefs", MODE_PRIVATE).getLong("teacher_id", -1);
        List<Classroom> classroomList = dbHelper.getAllClassroomsWithStudentCountAsList(teacherId);

        ClassroomAdapter adapter = new ClassroomAdapter(classroomList, dbHelper);
        adapter.setOnItemClickListener(classroom -> {
            Intent intent = new Intent(this, PublishTaskActivity.class);
            intent.putExtra("CLASS_ID", classroom.getId());
            startActivity(intent);
            finish();
        });
        adapter.setOnCheckinStatusClickListener(
                classroom -> ClassroomCheckinStatusSheet.show(ClassroomSelectionActivity.this, classroom, dbHelper));
        recyclerView.setAdapter(adapter);
    }
}
