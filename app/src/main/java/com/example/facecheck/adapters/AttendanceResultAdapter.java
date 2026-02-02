package com.example.facecheck.adapters;

import android.content.Intent;
import android.graphics.Color;
import androidx.dynamicanimation.animation.SpringAnimation;
import androidx.dynamicanimation.animation.SpringForce;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;
import android.widget.ImageView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.facecheck.R;
import com.example.facecheck.activity.MisrecognitionCorrectionActivity;
import com.example.facecheck.data.model.AttendanceResult;
import com.bumptech.glide.Glide;

import java.util.List;

public class AttendanceResultAdapter extends RecyclerView.Adapter<AttendanceResultAdapter.ViewHolder> {
    private List<AttendanceResult> results;
    private OnCorrectionClickListener correctionListener;
    private boolean failuresMode = false;

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
    
    public void setFailuresMode(boolean failuresMode) {
        this.failuresMode = failuresMode;
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
        private ImageView ivAvatar;
        private TextView tvStudentName;
        private TextView tvStudentId;
        private TextView tvStatus;
        private TextView tvScore;
        private TextView tvDecidedBy;
        private Button btnCorrect;
        private final int defaultScoreColor;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            ivAvatar = itemView.findViewById(R.id.ivAvatar);
            tvStudentName = itemView.findViewById(R.id.tvStudentName);
            tvStudentId = itemView.findViewById(R.id.tvStudentId);
            tvStatus = itemView.findViewById(R.id.tvStatus);
            tvScore = itemView.findViewById(R.id.tvScore);
            tvDecidedBy = itemView.findViewById(R.id.tvDecidedBy);
            btnCorrect = itemView.findViewById(R.id.btnCorrect);
            defaultScoreColor = tvScore.getCurrentTextColor();
        }

        public void bind(AttendanceResult result, OnCorrectionClickListener listener) {
            if (result.getStudent() != null) {
                tvStudentName.setText(result.getStudent().getName());
                tvStudentId.setText(result.getStudent().getSid());
                String uri = result.getStudent().getAvatarUri();
                if (uri != null && !uri.isEmpty()) {
                    Glide.with(itemView.getContext())
                            .load(uri)
                            .placeholder(R.drawable.ic_person)
                            .into(ivAvatar);
                } else {
                    ivAvatar.setImageResource(R.drawable.ic_person);
                }
            } else {
                tvStudentName.setText("未知学生");
                tvStudentId.setText("");
                ivAvatar.setImageResource(R.drawable.ic_person);
            }

            String status = result.getStatus();
            tvStatus.setText(getStatusText(status));
            float score = result.getScore();
            tvScore.setText(String.format("%.2f", score));
            boolean isPresent = "PRESENT".equalsIgnoreCase(status) || "Present".equalsIgnoreCase(status);
            if (isPresent) {
                tvDecidedBy.setText("判定方式: " + result.getDecidedBy());
            } else {
                tvDecidedBy.setText("原因: 相似度不足");
            }

            if (!isPresent && score >= 0.75f) {
                tvScore.setTextColor(Color.RED);
            } else {
                tvScore.setTextColor(defaultScoreColor);
            }
            
            // 失败视图模式下，对每个条目做轻微弹簧缩放
            AttendanceResultAdapter adapter = null;
            try {
                RecyclerView rv = (RecyclerView) itemView.getParent();
                RecyclerView.Adapter<?> a = rv.getAdapter();
                if (a instanceof AttendanceResultAdapter) {
                    adapter = (AttendanceResultAdapter) a;
                }
            } catch (Throwable ignored) {}
            if (adapter != null && adapter.failuresMode) {
                itemView.setScaleX(0.95f);
                itemView.setScaleY(0.95f);
                SpringAnimation ax = new SpringAnimation(itemView, SpringAnimation.SCALE_X, 1.0f);
                SpringAnimation ay = new SpringAnimation(itemView, SpringAnimation.SCALE_Y, 1.0f);
                ax.getSpring().setDampingRatio(SpringForce.DAMPING_RATIO_MEDIUM_BOUNCY);
                ax.getSpring().setStiffness(SpringForce.STIFFNESS_LOW);
                ay.getSpring().setDampingRatio(SpringForce.DAMPING_RATIO_MEDIUM_BOUNCY);
                ay.getSpring().setStiffness(SpringForce.STIFFNESS_LOW);
                ax.start();
                ay.start();
            } else if (isPresent) {
                itemView.setScaleX(0.98f);
                itemView.setScaleY(0.98f);
                SpringAnimation ax2 = new SpringAnimation(itemView, SpringAnimation.SCALE_X, 1.0f);
                SpringAnimation ay2 = new SpringAnimation(itemView, SpringAnimation.SCALE_Y, 1.0f);
                ax2.getSpring().setDampingRatio(SpringForce.DAMPING_RATIO_MEDIUM_BOUNCY);
                ax2.getSpring().setStiffness(SpringForce.STIFFNESS_LOW);
                ay2.getSpring().setDampingRatio(SpringForce.DAMPING_RATIO_MEDIUM_BOUNCY);
                ay2.getSpring().setStiffness(SpringForce.STIFFNESS_LOW);
                ax2.start();
                ay2.start();
            }

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
            if (status == null) return "未知";
            switch (status) {
                case "Present":
                case "PRESENT":
                    return "出勤";
                case "Absent":
                case "ABSENT":
                    return "缺勤";
                default:
                    return "未知";
            }
        }
    }
}
