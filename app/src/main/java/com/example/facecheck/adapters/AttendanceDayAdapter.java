package com.example.facecheck.adapters;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.compose.ui.platform.ComposeView;
import androidx.recyclerview.widget.RecyclerView;
import androidx.dynamicanimation.animation.SpringAnimation;
import androidx.dynamicanimation.animation.SpringForce;

import com.example.facecheck.R;
import com.example.facecheck.ui.checkin.AttendanceTaskComposeBinder;

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
        public String taskStartAt;
        public boolean canViewDetail;
        public String detailText;

        public static Item header(String className) {
            Item i = new Item();
            i.type = TYPE_HEADER;
            i.className = className;
            return i;
        }

        public static Item task(String title, String status, String startAt, boolean canViewDetail, String detailText) {
            Item i = new Item();
            i.type = TYPE_TASK;
            i.taskTitle = title;
            i.taskStatus = status;
            i.taskStartAt = startAt;
            i.canViewDetail = canViewDetail;
            i.detailText = detailText;
            return i;
        }
    }

    public interface OnTaskDetailClickListener {
        void onTaskDetailClick(String title, String detailText);
    }

    private final List<Item> items = new ArrayList<>();
    private OnTaskDetailClickListener onTaskDetailClickListener;

    public void setOnTaskDetailClickListener(OnTaskDetailClickListener listener) {
        this.onTaskDetailClickListener = listener;
    }

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

    class TaskVH extends RecyclerView.ViewHolder {
        TextView tvTaskTitle;
        TextView btnTaskDetail;
        ComposeView composeStatus;
        TaskVH(@NonNull View itemView) {
            super(itemView);
            tvTaskTitle = itemView.findViewById(R.id.tvTaskTitle);
            btnTaskDetail = itemView.findViewById(R.id.btnTaskDetail);
            composeStatus = itemView.findViewById(R.id.composeTaskStatus);
        }
        void bind(Item item) {
            tvTaskTitle.setText(item.taskTitle);
            AttendanceTaskComposeBinder.bind(composeStatus, item.taskStatus, item.taskStartAt);
            if (item.canViewDetail) {
                btnTaskDetail.setVisibility(View.VISIBLE);
                btnTaskDetail.setOnClickListener(v -> {
                    if (onTaskDetailClickListener != null) {
                        onTaskDetailClickListener.onTaskDetailClick(item.taskTitle, item.detailText);
                    }
                });
            } else {
                btnTaskDetail.setVisibility(View.GONE);
                btnTaskDetail.setOnClickListener(null);
            }
        }
    }
}
