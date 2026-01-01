package com.example.facecheck.adapters;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.RadioButton;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.facecheck.R;
import com.example.facecheck.data.model.Course;

import java.util.List;

public class StudentCourseAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

    private static final int TYPE_ITEM = 0;
    private static final int TYPE_FOOTER = 1;

    private List<Course> courses;
    private int selectedPosition = -1;
    private OnConfirmClickListener onConfirmClickListener;

    public interface OnConfirmClickListener {
        void onConfirm();
    }

    public StudentCourseAdapter(List<Course> courses, OnConfirmClickListener listener) {
        this.courses = courses;
        this.onConfirmClickListener = listener;
    }

    @Override
    public int getItemViewType(int position) {
        if (position == courses.size()) {
            return TYPE_FOOTER;
        }
        return TYPE_ITEM;
    }

    @NonNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        if (viewType == TYPE_FOOTER) {
            View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_course_footer, parent, false);
            return new FooterViewHolder(view);
        }
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_student_course, parent, false);
        return new CourseViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
        if (holder instanceof CourseViewHolder) {
            Course course = courses.get(position);
            CourseViewHolder vh = (CourseViewHolder) holder;

            vh.tvName.setText(course.getName());
            vh.tvTeacher.setText("讲师：" + course.getTeacherName());
            vh.tvTime.setText("时间：" + course.getTime());
            vh.tvLocation.setText("地点：" + course.getLocation());
            vh.rbSelected.setChecked(position == selectedPosition);

            vh.itemView.setOnClickListener(v -> {
                int previous = selectedPosition;
                selectedPosition = holder.getAdapterPosition();

                notifyItemChanged(previous);
                notifyItemChanged(selectedPosition);
            });
            vh.rbSelected.setOnClickListener(v -> {
                int previous = selectedPosition;
                selectedPosition = holder.getAdapterPosition();

                notifyItemChanged(previous);
                notifyItemChanged(selectedPosition);
            });
        } else if (holder instanceof FooterViewHolder) {
            FooterViewHolder vh = (FooterViewHolder) holder;
            vh.btnConfirm.setOnClickListener(v -> {
                if (onConfirmClickListener != null) {
                    onConfirmClickListener.onConfirm();
                }
            });
        }
    }

    @Override
    public int getItemCount() {
        return courses.size() + 1; // +1 for Footer
    }

    public Course getSelectedCourse() {
        if (selectedPosition >= 0 && selectedPosition < courses.size()) {
            return courses.get(selectedPosition);
        }
        return null;
    }

    static class CourseViewHolder extends RecyclerView.ViewHolder {
        TextView tvName, tvTeacher, tvTime, tvLocation;
        RadioButton rbSelected;

        CourseViewHolder(View itemView) {
            super(itemView);
            tvName = itemView.findViewById(R.id.tv_course_name);
            tvTeacher = itemView.findViewById(R.id.tv_course_teacher);
            tvTime = itemView.findViewById(R.id.tv_course_time);
            tvLocation = itemView.findViewById(R.id.tv_course_location);
            rbSelected = itemView.findViewById(R.id.rb_selected);
        }
    }

    static class FooterViewHolder extends RecyclerView.ViewHolder {
        Button btnConfirm;

        FooterViewHolder(View itemView) {
            super(itemView);
            btnConfirm = itemView.findViewById(R.id.btn_select_course);
        }
    }
}
