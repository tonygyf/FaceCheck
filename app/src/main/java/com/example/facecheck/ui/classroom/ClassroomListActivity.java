package com.example.facecheck.ui.classroom;

import android.content.Intent;
import android.os.Bundle;
import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.example.facecheck.R;
import com.example.facecheck.adapters.ClassroomAdapter;
import com.example.facecheck.data.model.Classroom;
import com.example.facecheck.data.repository.ClassroomRepository;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import java.util.ArrayList;
import java.util.List;

// A simple ViewModel to hold the repository
class ClassroomViewModel extends androidx.lifecycle.ViewModel {
    private ClassroomRepository repository;
    public ClassroomViewModel(Context context) {
        repository = new ClassroomRepository(context);
    }
    public LiveData<List<Classroom>> getClassrooms(long teacherId) {
        return repository.getClassrooms(teacherId);
    }
}

class ClassroomViewModelFactory implements ViewModelProvider.Factory {
    private Context context;
    public ClassroomViewModelFactory(Context context) { this.context = context; }
    @NonNull @Override public <T extends androidx.lifecycle.ViewModel> T create(@NonNull Class<T> modelClass) {
        return (T) new ClassroomViewModel(context);
    }
}

public class ClassroomListActivity extends AppCompatActivity {

    private RecyclerView recyclerView;
    private ClassroomAdapter adapter;
    private ClassroomViewModel viewModel;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_classroom_list);

        recyclerView = findViewById(R.id.recyclerViewClassrooms);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));

        adapter = new ClassroomAdapter(new ArrayList<>());
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

        // Setup ViewModel
        viewModel = new ViewModelProvider(this, new ClassroomViewModelFactory(getApplicationContext()))
                .get(ClassroomViewModel.class);

        // Observe data changes
        // Assuming teacherId is 1 for now
        viewModel.getClassrooms(1).observe(this, classrooms -> {
            adapter.updateClassrooms(classrooms);
        });
    }
}
