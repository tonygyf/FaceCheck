package com.example.facecheck.adapters;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.compose.ui.platform.ComposeView;
import androidx.recyclerview.widget.RecyclerView;

import com.example.facecheck.R;
import com.example.facecheck.data.model.Classroom;
import com.example.facecheck.database.DatabaseHelper;
import com.example.facecheck.ui.classroom.ClassroomCardComposeBinder;
import com.google.android.material.button.MaterialButton;

import java.util.List;

public class ClassroomAdapter extends RecyclerView.Adapter<ClassroomAdapter.ClassroomViewHolder> {

    private List<Classroom> classroomList;
    private final DatabaseHelper dbHelper;
    private OnItemClickListener listener;
    private OnCheckinStatusClickListener checkinStatusListener;

    public interface OnItemClickListener {
        void onItemClick(Classroom classroom);
    }

    public interface OnCheckinStatusClickListener {
        void onCheckinStatusClick(Classroom classroom);
    }

    public void setOnItemClickListener(OnItemClickListener listener) {
        this.listener = listener;
    }

    public void setOnCheckinStatusClickListener(OnCheckinStatusClickListener listener) {
        this.checkinStatusListener = listener;
    }

    public ClassroomAdapter(List<Classroom> classroomList, DatabaseHelper dbHelper) {
        this.classroomList = classroomList;
        this.dbHelper = dbHelper;
    }

    @NonNull
    @Override
    public ClassroomViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.list_item_classroom_card, parent, false);
        return new ClassroomViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ClassroomViewHolder holder, int position) {
        Classroom classroom = classroomList.get(position);
        holder.bind(classroom, listener, checkinStatusListener, dbHelper);
    }

    @Override
    public int getItemCount() {
        return classroomList == null ? 0 : classroomList.size();
    }

    public void updateClassrooms(List<Classroom> newClassrooms) {
        this.classroomList = newClassrooms;
        notifyDataSetChanged();
    }

    static class ClassroomViewHolder extends RecyclerView.ViewHolder {
        TextView tvClassroomName;
        TextView tvClassroomYear;
        TextView tvStudentCount;
        ComposeView composeClassroomMeta;
        MaterialButton btnCheckinStatus;

        ClassroomViewHolder(View itemView) {
            super(itemView);
            tvClassroomName = itemView.findViewById(R.id.tvClassroomName);
            tvClassroomYear = itemView.findViewById(R.id.tvClassroomYear);
            tvStudentCount = itemView.findViewById(R.id.tvStudentCount);
            composeClassroomMeta = itemView.findViewById(R.id.composeClassroomMeta);
            btnCheckinStatus = itemView.findViewById(R.id.btnClassroomCheckinStatus);
        }

        void bind(
                final Classroom classroom,
                final OnItemClickListener listener,
                final OnCheckinStatusClickListener checkinListener,
                DatabaseHelper dbHelper) {
            tvClassroomName.setText(classroom.getName());
            tvClassroomYear.setText(String.format("%d级", classroom.getYear()));
            tvStudentCount.setText(String.format("学生人数: %d", classroom.getStudentCount()));

            int totalTasks = dbHelper.countCheckinTasksForClass(classroom.getId());
            int activeTasks = dbHelper.countActiveCheckinTasksForClass(classroom.getId());
            if (composeClassroomMeta != null) {
                ClassroomCardComposeBinder.bind(composeClassroomMeta, totalTasks, activeTasks);
            }

            itemView.setOnClickListener(v -> {
                if (listener != null) {
                    listener.onItemClick(classroom);
                }
            });

            if (btnCheckinStatus != null) {
                btnCheckinStatus.setOnClickListener(v -> {
                    if (checkinListener != null) {
                        checkinListener.onCheckinStatusClick(classroom);
                    }
                });
            }
        }
    }
}
