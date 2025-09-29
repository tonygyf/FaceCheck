package com.example.facecheck.adapters;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.facecheck.R;
import com.example.facecheck.models.Classroom;

import java.util.List;

public class ClassroomAdapter extends RecyclerView.Adapter<ClassroomAdapter.ViewHolder> {
    private List<Classroom> classrooms;
    private OnItemClickListener listener;

    public interface OnItemClickListener {
        void onItemClick(Classroom classroom);
    }

    public ClassroomAdapter(List<Classroom> classrooms) {
        this.classrooms = classrooms;
    }

    public void setOnItemClickListener(OnItemClickListener listener) {
        this.listener = listener;
    }

    public void updateClassrooms(List<Classroom> newClassrooms) {
        this.classrooms = newClassrooms;
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_classroom, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        Classroom classroom = classrooms.get(position);
        holder.tvClassName.setText(classroom.getName());
        holder.tvYear.setText(String.valueOf(classroom.getYear()));
        
        holder.itemView.setOnClickListener(v -> {
            if (listener != null) {
                listener.onItemClick(classroom);
            }
        });
    }

    @Override
    public int getItemCount() {
        return classrooms.size();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        TextView tvClassName;
        TextView tvYear;

        ViewHolder(View itemView) {
            super(itemView);
            tvClassName = itemView.findViewById(R.id.tvClassName);
            tvYear = itemView.findViewById(R.id.tvYear);
        }
    }
}