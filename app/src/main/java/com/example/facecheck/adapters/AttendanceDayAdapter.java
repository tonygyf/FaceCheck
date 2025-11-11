package com.example.facecheck.adapters;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.facecheck.R;

import java.util.ArrayList;
import java.util.List;

/**
 * 日历页-当天考勤展示适配器：按班级分组，班级头+学生行两种类型
 */
public class AttendanceDayAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

    public static class Item {
        public static final int TYPE_HEADER = 0;
        public static final int TYPE_STUDENT = 1;
        public int type;
        public String className;
        public String studentName;
        public String sid;
        public int presentCount;
        public String timesText; // 如 "08:31, 10:12"

        public static Item header(String className) {
            Item i = new Item();
            i.type = TYPE_HEADER;
            i.className = className;
            return i;
        }

        public static Item student(String className, String studentName, String sid, int presentCount, String timesText) {
            Item i = new Item();
            i.type = TYPE_STUDENT;
            i.className = className;
            i.studentName = studentName;
            i.sid = sid;
            i.presentCount = presentCount;
            i.timesText = timesText;
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
            View v = inflater.inflate(R.layout.item_attendance_day_student, parent, false);
            return new StudentVH(v);
        }
    }

    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
        Item item = items.get(position);
        if (holder instanceof HeaderVH) {
            ((HeaderVH) holder).bind(item);
        } else if (holder instanceof StudentVH) {
            ((StudentVH) holder).bind(item);
        }
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

    static class StudentVH extends RecyclerView.ViewHolder {
        TextView tvStudentName, tvSid, tvPresentCount, tvTimes;
        StudentVH(@NonNull View itemView) {
            super(itemView);
            tvStudentName = itemView.findViewById(R.id.tvStudentName);
            tvSid = itemView.findViewById(R.id.tvSid);
            tvPresentCount = itemView.findViewById(R.id.tvPresentCount);
            tvTimes = itemView.findViewById(R.id.tvTimes);
        }
        void bind(Item item) {
            tvStudentName.setText(item.studentName);
            tvSid.setText(item.sid);
            tvPresentCount.setText("次数: " + item.presentCount);
            tvTimes.setText("时间: " + (item.timesText == null || item.timesText.isEmpty() ? "-" : item.timesText));
            if (item.presentCount == 0) {
                // 零出勤特殊样式：红色、斜体
                tvStudentName.setTextColor(0xFFB00020); // Material 红色近似值
                tvPresentCount.setTextColor(0xFFB00020);
                tvStudentName.setTypeface(tvStudentName.getTypeface(), android.graphics.Typeface.ITALIC);
                tvPresentCount.setTypeface(tvPresentCount.getTypeface(), android.graphics.Typeface.BOLD);
            } else {
                // 恢复默认样式（避免复用污染）
                tvStudentName.setTextColor(0xFF000000);
                tvPresentCount.setTextColor(0xFF000000);
                tvStudentName.setTypeface(null, android.graphics.Typeface.NORMAL);
                tvPresentCount.setTypeface(null, android.graphics.Typeface.BOLD);
            }
        }
    }
}