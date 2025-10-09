package com.example.facecheck.adapter;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.facecheck.R;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

/**
 * 人脸分割适配器
 * 用于在RecyclerView中显示分割后的人脸照片
 */
public class FaceSegmentationAdapter extends RecyclerView.Adapter<FaceSegmentationAdapter.FaceViewHolder> {
    
    private Context context;
    private List<String> faceImagePaths;
    private List<Integer> selectedPositions;
    private OnItemClickListener listener;
    
    public interface OnItemClickListener {
        void onItemClick(int position);
        void onItemLongClick(int position);
    }
    
    public FaceSegmentationAdapter(Context context, List<String> faceImagePaths) {
        this.context = context;
        this.faceImagePaths = faceImagePaths;
        this.selectedPositions = new ArrayList<>();
    }
    
    public void setOnItemClickListener(OnItemClickListener listener) {
        this.listener = listener;
    }
    
    @NonNull
    @Override
    public FaceViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_face_segmentation, parent, false);
        return new FaceViewHolder(view);
    }
    
    @Override
    public void onBindViewHolder(@NonNull FaceViewHolder holder, int position) {
        String imagePath = faceImagePaths.get(position);
        File imageFile = new File(imagePath);
        
        if (imageFile.exists()) {
            // 加载图片
            Bitmap bitmap = BitmapFactory.decodeFile(imagePath);
            if (bitmap != null) {
                holder.ivFace.setImageBitmap(bitmap);
            }
        }
        
        holder.tvFaceNumber.setText("人脸 " + (position + 1));
        holder.cbSelect.setChecked(selectedPositions.contains(position));
        
        // 设置点击事件
        holder.itemView.setOnClickListener(v -> {
            if (listener != null) {
                listener.onItemClick(position);
            }
        });
        
        holder.itemView.setOnLongClickListener(v -> {
            if (listener != null) {
                listener.onItemLongClick(position);
            }
            return true;
        });
        
        // 复选框点击事件
        holder.cbSelect.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (isChecked) {
                if (!selectedPositions.contains(position)) {
                    selectedPositions.add(position);
                }
            } else {
                selectedPositions.remove(Integer.valueOf(position));
            }
        });
    }
    
    @Override
    public int getItemCount() {
        return faceImagePaths != null ? faceImagePaths.size() : 0;
    }
    
    /**
     * 切换选择状态
     */
    public void toggleSelection(int position) {
        if (selectedPositions.contains(position)) {
            selectedPositions.remove(Integer.valueOf(position));
        } else {
            selectedPositions.add(position);
        }
        notifyItemChanged(position);
    }
    
    /**
     * 获取选中的位置
     */
    public List<Integer> getSelectedPositions() {
        return new ArrayList<>(selectedPositions);
    }
    
    /**
     * 清除选择状态
     */
    public void clearSelection() {
        selectedPositions.clear();
        notifyDataSetChanged();
    }
    
    /**
     * 获取选中的图片路径
     */
    public List<String> getSelectedImagePaths() {
        List<String> selectedPaths = new ArrayList<>();
        for (int position : selectedPositions) {
            if (position >= 0 && position < faceImagePaths.size()) {
                selectedPaths.add(faceImagePaths.get(position));
            }
        }
        return selectedPaths;
    }
    
    static class FaceViewHolder extends RecyclerView.ViewHolder {
        ImageView ivFace;
        TextView tvFaceNumber;
        CheckBox cbSelect;
        
        public FaceViewHolder(@NonNull View itemView) {
            super(itemView);
            ivFace = itemView.findViewById(R.id.ivFace);
            tvFaceNumber = itemView.findViewById(R.id.tvFaceNumber);
            cbSelect = itemView.findViewById(R.id.cbSelect);
        }
    }
}