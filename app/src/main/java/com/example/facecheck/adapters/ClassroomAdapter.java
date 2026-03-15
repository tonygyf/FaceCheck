package com.example.facecheck.adapters;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.example.facecheck.R;
import com.example.facecheck.data.model.Classroom;
import java.util.List;

public class ClassroomAdapter extends RecyclerView.Adapter<ClassroomAdapter.ClassroomViewHolder> {

    private List<Classroom> classroomList;
    private OnItemClickListener listener;

    public interface OnItemClickListener {
        void onItemClick(Classroom classroom);
    }

    public void setOnItemClickListener(OnItemClickListener listener) {
        this.listener = listener;
    }

    public ClassroomAdapter(List<Classroom> classroomList) {
        this.classroomList = classroomList;
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
        holder.bind(classroom, listener);
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

        ClassroomViewHolder(View itemView) {
            super(itemView);
            tvClassroomName = itemView.findViewById(R.id.tvClassroomName);
            tvClassroomYear = itemView.findViewById(R.id.tvClassroomYear);
            tvStudentCount = itemView.findViewById(R.id.tvStudentCount);
        }

        void bind(final Classroom classroom, final OnItemClickListener listener) {
            tvClassroomName.setText(classroom.getName());
            tvClassroomYear.setText(String.format("%d级", classroom.getYear()));
            tvStudentCount.setText(String.format("学生人数: %d", classroom.getStudentCount()));

            itemView.setOnClickListener(v -> {
                if (listener != null) {
                    listener.onItemClick(classroom);
                }
            });
        }
    }
}
