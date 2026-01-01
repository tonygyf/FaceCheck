package com.example.facecheck.adapters;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.RadioButton;
import android.widget.TextView;

import com.example.facecheck.R;
import com.example.facecheck.data.model.Course;

import java.util.List;

public class StudentCourseAdapter extends BaseAdapter {
    private List<Course> courses;
    private int selectedPosition = -1;

    public StudentCourseAdapter(List<Course> courses) {
        this.courses = courses;
    }

    @Override
    public int getCount() {
        return courses.size();
    }

    @Override
    public Object getItem(int position) {
        return courses.get(position);
    }

    @Override
    public long getItemId(int position) {
        return position;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        ViewHolder holder;
        if (convertView == null) {
            convertView = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_student_course, parent, false);
            holder = new ViewHolder();
            holder.tvName = convertView.findViewById(R.id.tv_course_name);
            holder.tvTeacher = convertView.findViewById(R.id.tv_course_teacher);
            holder.tvTime = convertView.findViewById(R.id.tv_course_time);
            holder.tvLocation = convertView.findViewById(R.id.tv_course_location);
            holder.rbSelected = convertView.findViewById(R.id.rb_selected);
            convertView.setTag(holder);
        } else {
            holder = (ViewHolder) convertView.getTag();
        }

        Course course = courses.get(position);
        holder.tvName.setText(course.getName());
        holder.tvTeacher.setText("讲师：" + course.getTeacherName());
        holder.tvTime.setText("时间：" + course.getTime());
        holder.tvLocation.setText("地点：" + course.getLocation());
        holder.rbSelected.setChecked(position == selectedPosition);

        convertView.setOnClickListener(v -> {
            selectedPosition = position;
            notifyDataSetChanged();
        });

        return convertView;
    }

    public int getSelectedPosition() {
        return selectedPosition;
    }

    public Course getSelectedCourse() {
        if (selectedPosition >= 0 && selectedPosition < courses.size()) {
            return courses.get(selectedPosition);
        }
        return null;
    }

    private static class ViewHolder {
        TextView tvName, tvTeacher, tvTime, tvLocation;
        RadioButton rbSelected;
    }
}
