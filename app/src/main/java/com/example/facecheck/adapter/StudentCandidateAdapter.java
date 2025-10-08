package com.example.facecheck.adapter;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.RadioButton;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.facecheck.R;
import com.example.facecheck.data.model.Student;

import java.util.List;

public class StudentCandidateAdapter extends RecyclerView.Adapter<StudentCandidateAdapter.ViewHolder> {
    
    private Context context;
    private List<Student> studentList;
    private int selectedPosition = -1;
    private OnStudentClickListener listener;
    
    public interface OnStudentClickListener {
        void onStudentClick(Student student);
    }
    
    public StudentCandidateAdapter(Context context, List<Student> studentList) {
        this.context = context;
        this.studentList = studentList;
    }
    
    public void setOnStudentClickListener(OnStudentClickListener listener) {
        this.listener = listener;
    }
    
    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_student_candidate, parent, false);
        return new ViewHolder(view);
    }
    
    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        Student student = studentList.get(position);
        
        holder.tvStudentName.setText(student.getName());
        holder.tvStudentId.setText(student.getSid());
        holder.rbSelect.setChecked(position == selectedPosition);
        
        // 设置点击监听
        holder.itemView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // 更新选中位置
                int previousPosition = selectedPosition;
                selectedPosition = holder.getAdapterPosition();
                
                // 通知更新
                notifyItemChanged(previousPosition);
                notifyItemChanged(selectedPosition);
                
                // 回调监听
                if (listener != null && selectedPosition != -1) {
                    listener.onStudentClick(studentList.get(selectedPosition));
                }
            }
        });
        
        // RadioButton 点击监听
        holder.rbSelect.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                int previousPosition = selectedPosition;
                selectedPosition = holder.getAdapterPosition();
                
                notifyItemChanged(previousPosition);
                notifyItemChanged(selectedPosition);
                
                if (listener != null && selectedPosition != -1) {
                    listener.onStudentClick(studentList.get(selectedPosition));
                }
            }
        });
    }
    
    @Override
    public int getItemCount() {
        return studentList != null ? studentList.size() : 0;
    }
    
    public Student getSelectedStudent() {
        if (selectedPosition != -1 && selectedPosition < studentList.size()) {
            return studentList.get(selectedPosition);
        }
        return null;
    }
    
    public void clearSelection() {
        int previousPosition = selectedPosition;
        selectedPosition = -1;
        if (previousPosition != -1) {
            notifyItemChanged(previousPosition);
        }
    }
    
    public static class ViewHolder extends RecyclerView.ViewHolder {
        TextView tvStudentName, tvStudentId;
        RadioButton rbSelect;
        
        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            tvStudentName = itemView.findViewById(R.id.tvStudentName);
            tvStudentId = itemView.findViewById(R.id.tvStudentId);
            rbSelect = itemView.findViewById(R.id.rbSelect);
        }
    }
}