package com.example.facecheck.adapters;

import android.content.Intent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.facecheck.R;
import com.example.facecheck.activity.MisrecognitionCorrectionActivity;
import com.example.facecheck.data.model.AttendanceResult;

import java.util.List;

public class AttendanceResultAdapter extends RecyclerView.Adapter<AttendanceResultAdapter.ViewHolder> {
    private List<AttendanceResult> results;
    private OnCorrectionClickListener correctionListener;

    public interface OnCorrectionClickListener {
        void onCorrectionClick(AttendanceResult result);
    }

    public AttendanceResultAdapter(List<AttendanceResult> results) {
        this.results = results;
    }

    public void setOnCorrectionClickListener(OnCorrectionClickListener listener) {
        this.correctionListener = listener;
    }

    public void updateResults(List<AttendanceResult> newResults) {
        this.results = newResults;
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_attendance_result, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        AttendanceResult result = results.get(position);
        holder.bind(result, correctionListener);
    }

    @Override
    public int getItemCount() {
        return results != null ? results.size() : 0;
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        private TextView tvStudentName;
        private TextView tvStudentId;
        private TextView tvStatus;
        private TextView tvScore;
        private TextView tvDecidedBy;
        private Button btnCorrect;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            tvStudentName = itemView.findViewById(R.id.tvStudentName);
            tvStudentId = itemView.findViewById(R.id.tvStudentId);
            tvStatus = itemView.findViewById(R.id.tvStatus);
            tvScore = itemView.findViewById(R.id.tvScore);
            tvDecidedBy = itemView.findViewById(R.id.tvDecidedBy);
            btnCorrect = itemView.findViewById(R.id.btnCorrect);
        }

        public void bind(AttendanceResult result, OnCorrectionClickListener listener) {
            if (result.getStudent() != null) {
                tvStudentName.setText(result.getStudent().getName());
                tvStudentId.setText(result.getStudent().getSid());
            } else {
                tvStudentName.setText("未知学生");
                tvStudentId.setText("");
            }

            tvStatus.setText(getStatusText(result.getStatus()));
            tvScore.setText(String.format("%.2f", result.getScore()));
            tvDecidedBy.setText("判定方式: " + result.getDecidedBy());

            // 设置修正按钮点击监听
            btnCorrect.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    if (listener != null) {
                        listener.onCorrectionClick(result);
                    }
                }
            });
        }

        private String getStatusText(String status) {
            switch (status) {
                case "PRESENT":
                    return "出勤";
                case "ABSENT":
                    return "缺勤";
                default:
                    return "未知";
            }
        }
    }
}