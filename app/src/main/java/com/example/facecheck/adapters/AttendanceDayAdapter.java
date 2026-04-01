package com.example.facecheck.adapters;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import androidx.dynamicanimation.animation.SpringAnimation;
import androidx.dynamicanimation.animation.SpringForce;

import com.example.facecheck.R;

import java.util.ArrayList;
import java.util.List;

/**
 * 日历页-当天考勤展示适配器：按班级分组，班级头+学生行两种类型
 */
public class AttendanceDayAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

    public static class Item {
        public static final int TYPE_HEADER = 0;
        public static final int TYPE_TASK = 1;

        public int type;
        public String className;
        public String taskTitle;
        public String taskStatus;

        public static Item header(String className) {
            Item i = new Item();
            i.type = TYPE_HEADER;
            i.className = className;
            return i;
        }

        public static Item task(String title, String status) {
            Item i = new Item();
            i.type = TYPE_TASK;
            i.taskTitle = title;
            i.taskStatus = status;
            return i;
        }
    }

    private final List<Item> items = new ArrayList<>();

    public void updateItems(List<Item> newItems) {
        items.clear();
        if (newItems != null) items.addAll(newItems);
        notifyDataSetChanged();
    }

    @Override
    public int getItemViewType(int position) {
        return items.get(position).type;
    }

    @NonNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        LayoutInflater inflater = LayoutInflater.from(parent.getContext());
        if (viewType == Item.TYPE_HEADER) {
            View v = inflater.inflate(R.layout.item_attendance_day_header, parent, false);
            return new HeaderVH(v);
        } else {
            View v = inflater.inflate(R.layout.item_checkin_task, parent, false);
            return new TaskVH(v);
        }
    }

    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
        Item item = items.get(position);
        if (holder instanceof HeaderVH) {
            ((HeaderVH) holder).bind(item);
        } else if (holder instanceof TaskVH) {
            ((TaskVH) holder).bind(item);
        }
        // ... (保留动画)
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    static class HeaderVH extends RecyclerView.ViewHolder {
        TextView tvClassName;
        HeaderVH(@NonNull View itemView) {
            super(itemView);
            tvClassName = itemView.findViewById(R.id.tvClassName);
        }
        void bind(Item item) {
            tvClassName.setText(item.className);
        }
    }

    static class TaskVH extends RecyclerView.ViewHolder {
        TextView tvTaskTitle, tvTaskStatus;
        TaskVH(@NonNull View itemView) {
            super(itemView);
            tvTaskTitle = itemView.findViewById(R.id.tvTaskTitle);
            tvTaskStatus = itemView.findViewById(R.id.tvTaskStatus);
        }
        void bind(Item item) {
            tvTaskTitle.setText(item.taskTitle);
            tvTaskStatus.setText(item.taskStatus);
            // 根据状态设置不同颜色
            if ("ACTIVE".equalsIgnoreCase(item.taskStatus)) {
                tvTaskStatus.setTextColor(0xFF4CAF50); // Green
            } else {
                tvTaskStatus.setTextColor(0xFF757575); // Grey
            }
        }
    }
}
