package com.example.facecheck.adapters;

import android.content.Intent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.facecheck.R;
import com.example.facecheck.data.model.Classroom;
import com.example.facecheck.ui.attendance.AttendanceActivity;

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
        
        // 添加长按事件，显示操作菜单（查看/开始考勤/重命名）
        holder.itemView.setOnLongClickListener(v -> {
            // 显示选项对话框
            android.app.AlertDialog.Builder builder = new android.app.AlertDialog.Builder(holder.itemView.getContext());
            builder.setTitle("选择操作")
                   .setItems(new String[]{"查看班级", "开始考勤", "重命名班级"}, (dialog, which) -> {
                       if (which == 0) {
                           // 查看班级
                           if (listener != null) {
                               listener.onItemClick(classroom);
                           }
                       } else if (which == 1) {
                           // 开始考勤
                           Intent intent = new Intent(holder.itemView.getContext(), 
                               com.example.facecheck.ui.attendance.AttendanceActivity.class);
                           intent.putExtra("classroom_id", classroom.getId());
                           holder.itemView.getContext().startActivity(intent);
                       } else {
                           // 重命名班级
                           android.widget.EditText editText = new android.widget.EditText(holder.itemView.getContext());
                           editText.setSingleLine(true);
                           editText.setText(classroom.getName());
                           new android.app.AlertDialog.Builder(holder.itemView.getContext())
                               .setTitle("重命名班级")
                               .setView(editText)
                               .setPositiveButton("保存", (d, w) -> {
                                   String newName = editText.getText().toString().trim();
                                   if (!newName.isEmpty()) {
                                       com.example.facecheck.database.DatabaseHelper db = new com.example.facecheck.database.DatabaseHelper(holder.itemView.getContext());
                                       boolean ok = db.updateClassroomName(classroom.getId(), newName);
                                       if (ok) {
                                           classroom.setName(newName);
                                           notifyItemChanged(holder.getAdapterPosition());
                                           android.widget.Toast.makeText(holder.itemView.getContext(), "班级名称已更新", android.widget.Toast.LENGTH_SHORT).show();
                                       } else {
                                           android.widget.Toast.makeText(holder.itemView.getContext(), "更新失败", android.widget.Toast.LENGTH_SHORT).show();
                                       }
                                   } else {
                                       android.widget.Toast.makeText(holder.itemView.getContext(), "名称不能为空", android.widget.Toast.LENGTH_SHORT).show();
                                   }
                               })
                               .setNegativeButton("取消", null)
                               .show();
                       }
                   })
                   .show();
            return true;
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