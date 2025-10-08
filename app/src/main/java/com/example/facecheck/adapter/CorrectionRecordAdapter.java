package com.example.facecheck.adapter;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.facecheck.R;
import com.example.facecheck.data.model.CorrectionRecord;

import java.util.List;

public class CorrectionRecordAdapter extends RecyclerView.Adapter<CorrectionRecordAdapter.ViewHolder> {

    private Context context;
    private List<CorrectionRecord> records;
    private OnCorrectionClickListener listener;

    public interface OnCorrectionClickListener {
        void onCorrectClick(CorrectionRecord record);
        void onIgnoreClick(CorrectionRecord record);
    }

    public CorrectionRecordAdapter(Context context, List<CorrectionRecord> records) {
        this.context = context;
        this.records = records;
        this.listener = (OnCorrectionClickListener) context;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_correction_record, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        CorrectionRecord record = records.get(position);
        
        // 设置学生头像（暂时使用默认头像）
        holder.ivFaceImage.setImageResource(R.drawable.ic_person);
        
        // 设置学生信息
        holder.tvStudentName.setText(record.getStudentName());
        holder.tvStudentId.setText("学号: " + record.getStudentId());
        holder.tvCorrectionType.setText("修正类型: " + record.getCorrectionType());
        holder.tvCorrectionTime.setText("时间: " + record.getCorrectionTime());
        holder.tvDescription.setText("描述: " + record.getDescription());
        
        // 设置按钮点击事件
        holder.btnCorrect.setOnClickListener(v -> {
            if (listener != null) {
                listener.onCorrectClick(record);
            }
        });
        
        holder.btnIgnore.setOnClickListener(v -> {
            if (listener != null) {
                listener.onIgnoreClick(record);
            }
        });
    }

    @Override
    public int getItemCount() {
        return records != null ? records.size() : 0;
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        ImageView ivFaceImage;
        TextView tvStudentName, tvStudentId, tvCorrectionType, tvCorrectionTime, tvDescription;
        Button btnCorrect, btnIgnore;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            ivFaceImage = itemView.findViewById(R.id.ivFaceImage);
            tvStudentName = itemView.findViewById(R.id.tvStudentName);
            tvStudentId = itemView.findViewById(R.id.tvStudentId);
            tvCorrectionType = itemView.findViewById(R.id.tvCorrectionType);
            tvCorrectionTime = itemView.findViewById(R.id.tvCorrectionTime);
            tvDescription = itemView.findViewById(R.id.tvDescription);
            btnCorrect = itemView.findViewById(R.id.btnCorrect);
            btnIgnore = itemView.findViewById(R.id.btnIgnore);
        }
    }
}