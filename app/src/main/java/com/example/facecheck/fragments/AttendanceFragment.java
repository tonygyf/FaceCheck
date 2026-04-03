package com.example.facecheck.fragments;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.compose.ui.platform.ComposeView;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.facecheck.R;
import com.example.facecheck.adapters.AttendanceDayAdapter;
import com.example.facecheck.database.DatabaseHelper;
import com.example.facecheck.ui.checkin.AttendanceCalendarActionListener;
import com.example.facecheck.ui.checkin.AttendanceCalendarComposeBinder;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Locale;

public class AttendanceFragment extends Fragment {

    private ComposeView calendarComposeView;
    private RecyclerView recyclerView;
    private DatabaseHelper dbHelper;
    private String selectedDate;
    private AttendanceDayAdapter dayAdapter;
    private final Calendar displayCalendar = Calendar.getInstance();
    private int selectedDayOfMonth = 1;
    private boolean groupedByStatus = false;
    private int statusFilterMode = 0;
    private String role = "teacher";
    private long studentId = -1;
    private long studentClassId = -1;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_attendance, container, false);
        dbHelper = new DatabaseHelper(getContext());
        calendarComposeView = view.findViewById(R.id.calendar_compose_view);
        recyclerView = view.findViewById(R.id.recycler_attendance);
        if (getActivity() != null) {
            android.content.SharedPreferences prefs = getActivity().getSharedPreferences("user_prefs", android.content.Context.MODE_PRIVATE);
            role = prefs.getString("user_role", "teacher");
            studentId = prefs.getLong("student_id", -1);
            if ("student".equals(role) && studentId > 0) {
                studentClassId = dbHelper.getClassIdByStudentId(studentId);
            }
        }

        Calendar now = Calendar.getInstance();
        displayCalendar.set(Calendar.YEAR, now.get(Calendar.YEAR));
        displayCalendar.set(Calendar.MONTH, now.get(Calendar.MONTH));
        selectedDayOfMonth = now.get(Calendar.DAY_OF_MONTH);

        selectedDate = formatDate(displayCalendar.get(Calendar.YEAR), displayCalendar.get(Calendar.MONTH), selectedDayOfMonth);

        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        dayAdapter = new AttendanceDayAdapter();
        recyclerView.setAdapter(dayAdapter);

        refreshAttendanceList();
        renderCalendar();
        return view;
    }

    @Override
    public void onResume() {
        super.onResume();
        if (selectedDate != null) {
            refreshAttendanceList();
            renderCalendar();
        }
    }

    private void onSelectDay(int dayOfMonth) {
        selectedDayOfMonth = dayOfMonth;
        selectedDate = formatDate(displayCalendar.get(Calendar.YEAR), displayCalendar.get(Calendar.MONTH), selectedDayOfMonth);
        refreshAttendanceList();
        renderCalendar();
    }

    private void shiftMonth(int offset) {
        displayCalendar.add(Calendar.MONTH, offset);
        selectedDayOfMonth = resolvePreferredDayForCurrentMonth();
        selectedDate = formatDate(displayCalendar.get(Calendar.YEAR), displayCalendar.get(Calendar.MONTH), selectedDayOfMonth);
        refreshAttendanceList();
        renderCalendar();
    }

    private void onYearMonthPicked(int year, int month) {
        displayCalendar.set(Calendar.YEAR, year);
        displayCalendar.set(Calendar.MONTH, month);
        selectedDayOfMonth = resolvePreferredDayForCurrentMonth();
        selectedDate = formatDate(displayCalendar.get(Calendar.YEAR), displayCalendar.get(Calendar.MONTH), selectedDayOfMonth);
        refreshAttendanceList();
        renderCalendar();
    }

    private void onToggleGroupMode() {
        groupedByStatus = !groupedByStatus;
        if (groupedByStatus) {
            statusFilterMode = 0;
        }
        refreshAttendanceList();
        renderCalendar();
    }

    private void onCycleStatusFilter() {
        statusFilterMode = (statusFilterMode + 1) % 3;
        refreshAttendanceList();
        renderCalendar();
    }

    private void renderCalendar() {
        if (calendarComposeView == null || getContext() == null) return;
        java.util.Map<Integer, Integer> dateStatusMap = loadDateStatusMapForCurrentMonth();
        boolean prevMonthHasActive = hasActiveTasksInRelativeMonth(-1);
        boolean nextMonthHasActive = hasActiveTasksInRelativeMonth(1);

        AttendanceCalendarComposeBinder.bind(
                calendarComposeView,
                displayCalendar.get(Calendar.YEAR),
                displayCalendar.get(Calendar.MONTH),
                selectedDayOfMonth,
                dateStatusMap,
                prevMonthHasActive,
                nextMonthHasActive,
                groupedByStatus,
                statusFilterMode,
                new AttendanceCalendarActionListener() {
                    @Override
                    public void onPrevMonthClick() {
                        shiftMonth(-1);
                    }

                    @Override
                    public void onNextMonthClick() {
                        shiftMonth(1);
                    }

                    @Override
                    public void onDayClick(int dayOfMonth) {
                        onSelectDay(dayOfMonth);
                    }

                    @Override
                    public void onYearMonthPicked(int year, int month) {
                        AttendanceFragment.this.onYearMonthPicked(year, month);
                    }

                    @Override
                    public void onToggleGroupMode() {
                        AttendanceFragment.this.onToggleGroupMode();
                    }

                    @Override
                    public void onCycleStatusFilter() {
                        AttendanceFragment.this.onCycleStatusFilter();
                    }
                }
        );
    }

    private void refreshAttendanceList() {
        if (groupedByStatus) {
            loadAttendanceDataByStatus();
        } else {
            loadAttendanceDataByDate(selectedDate);
        }
    }

    private java.util.Map<Integer, Integer> loadDateStatusMapForCurrentMonth() {
        String yearMonth = new SimpleDateFormat("yyyy-MM", Locale.getDefault()).format(displayCalendar.getTime());
        if ("student".equals(role) && studentClassId <= 0) {
            return new java.util.HashMap<>();
        }
        android.database.Cursor cursor;
        if ("student".equals(role) && studentClassId > 0) {
            cursor = dbHelper.getTaskStatusForMonthAndClass(yearMonth, studentClassId);
        } else {
            cursor = dbHelper.getTaskStatusForMonth(yearMonth);
        }
        java.util.Map<Integer, Integer> dateStatusMap = new java.util.HashMap<>();

        if (cursor != null && cursor.moveToFirst()) {
            do {
                String startAt = cursor.getString(cursor.getColumnIndexOrThrow("startAt"));
                String status = cursor.getString(cursor.getColumnIndexOrThrow("status"));
                try {
                    String normalized = startAt.replace("T", " ");
                    if (normalized.contains(".")) normalized = normalized.substring(0, normalized.indexOf("."));
                    if (normalized.endsWith("Z")) normalized = normalized.substring(0, normalized.length() - 1);
                    Calendar taskCal = Calendar.getInstance();
                    taskCal.setTime(new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).parse(normalized));
                    int dayOfMonth = taskCal.get(Calendar.DAY_OF_MONTH);
                    boolean isActive = "ACTIVE".equalsIgnoreCase(status);
                    Integer currentColor = dateStatusMap.get(dayOfMonth);
                    if (currentColor == null || currentColor != android.graphics.Color.RED) {
                        dateStatusMap.put(dayOfMonth, isActive ? android.graphics.Color.RED : android.graphics.Color.BLUE);
                    }
                } catch (java.text.ParseException e) {
                    android.util.Log.e("AttendanceFragment", "解析日期失败: " + startAt, e);
                }
            } while (cursor.moveToNext());
            cursor.close();
        }
        return dateStatusMap;
    }

    private int resolvePreferredDayForCurrentMonth() {
        String yearMonth = new SimpleDateFormat("yyyy-MM", Locale.getDefault()).format(displayCalendar.getTime());
        if ("student".equals(role) && studentClassId <= 0) {
            return 1;
        }
        android.database.Cursor cursor;
        if ("student".equals(role) && studentClassId > 0) {
            cursor = dbHelper.getTaskStatusForMonthAndClass(yearMonth, studentClassId);
        } else {
            cursor = dbHelper.getTaskStatusForMonth(yearMonth);
        }
        Integer minActiveDay = null;
        Integer minClosedDay = null;
        int maxDay = displayCalendar.getActualMaximum(Calendar.DAY_OF_MONTH);

        if (cursor != null && cursor.moveToFirst()) {
            do {
                String startAt = cursor.getString(cursor.getColumnIndexOrThrow("startAt"));
                String status = cursor.getString(cursor.getColumnIndexOrThrow("status"));
                try {
                    String normalized = startAt.replace("T", " ");
                    if (normalized.contains(".")) normalized = normalized.substring(0, normalized.indexOf("."));
                    if (normalized.endsWith("Z")) normalized = normalized.substring(0, normalized.length() - 1);
                    Calendar taskCal = Calendar.getInstance();
                    taskCal.setTime(new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).parse(normalized));
                    int dayOfMonth = taskCal.get(Calendar.DAY_OF_MONTH);
                    if (dayOfMonth < 1 || dayOfMonth > maxDay) continue;

                    if ("ACTIVE".equalsIgnoreCase(status)) {
                        if (minActiveDay == null || dayOfMonth < minActiveDay) {
                            minActiveDay = dayOfMonth;
                        }
                    } else if ("CLOSED".equalsIgnoreCase(status)) {
                        if (minClosedDay == null || dayOfMonth < minClosedDay) {
                            minClosedDay = dayOfMonth;
                        }
                    }
                } catch (java.text.ParseException e) {
                    android.util.Log.e("AttendanceFragment", "解析日期失败: " + startAt, e);
                }
            } while (cursor.moveToNext());
            cursor.close();
        }

        if (minActiveDay != null) return minActiveDay;
        if (minClosedDay != null) return minClosedDay;
        return 1;
    }

    private boolean hasActiveTasksInRelativeMonth(int offset) {
        Calendar cal = (Calendar) displayCalendar.clone();
        cal.add(Calendar.MONTH, offset);
        String yearMonth = new SimpleDateFormat("yyyy-MM", Locale.getDefault()).format(cal.getTime());
        if ("student".equals(role) && studentClassId <= 0) {
            return false;
        }
        if ("student".equals(role) && studentClassId > 0) {
            return dbHelper.hasActiveTasksInMonthByClass(yearMonth, studentClassId);
        }
        return dbHelper.hasActiveTasksInMonth(yearMonth);
    }

    private String formatDate(int year, int month, int dayOfMonth) {
        Calendar selected = Calendar.getInstance();
        selected.set(year, month, dayOfMonth, 0, 0, 0);
        selected.set(Calendar.MILLISECOND, 0);
        return new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(selected.getTime());
    }

    private void loadAttendanceDataByDate(String date) {
        if (getActivity() == null) return;
        try {
            android.database.Cursor cursor;
            if ("student".equals(role) && studentClassId > 0) {
                cursor = dbHelper.getCheckinTasksByDateAndClass(date, studentClassId);
            } else {
                cursor = dbHelper.getCheckinTasksByDate(date);
            }
            java.util.Map<String, java.util.List<com.example.facecheck.data.model.CheckinTask>> groupedTasks = new java.util.LinkedHashMap<>();
            if (cursor != null && cursor.moveToFirst()) {
                do {
                    long classId = cursor.getLong(cursor.getColumnIndexOrThrow("classId"));
                    String className = dbHelper.getClassNameById(classId);
                    if (className == null) className = "未知班级";

                    com.example.facecheck.data.model.CheckinTask task = new com.example.facecheck.data.model.CheckinTask();
                    task.setId(cursor.getLong(cursor.getColumnIndexOrThrow("id")));
                    task.setTitle(cursor.getString(cursor.getColumnIndexOrThrow("title")));
                    task.setStatus(cursor.getString(cursor.getColumnIndexOrThrow("status")));
                    task.setStartAt(cursor.getString(cursor.getColumnIndexOrThrow("startAt")));
                    task.setEndAt(cursor.getString(cursor.getColumnIndexOrThrow("endAt")));

                    if (!groupedTasks.containsKey(className)) {
                        groupedTasks.put(className, new java.util.ArrayList<>());
                    }
                    groupedTasks.get(className).add(task);
                } while (cursor.moveToNext());
                cursor.close();
            }

            java.util.List<AttendanceDayAdapter.Item> items = new java.util.ArrayList<>();
            for (java.util.Map.Entry<String, java.util.List<com.example.facecheck.data.model.CheckinTask>> entry : groupedTasks.entrySet()) {
                items.add(AttendanceDayAdapter.Item.header(entry.getKey()));
                for (com.example.facecheck.data.model.CheckinTask task : entry.getValue()) {
                    items.add(AttendanceDayAdapter.Item.task(task.getTitle(), task.getStatus()));
                }
            }
            dayAdapter.updateItems(items);
        } catch (Throwable t) {
            android.util.Log.e("AttendanceFragment", "加载签到任务失败: " + t.getMessage(), t);
            dayAdapter.updateItems(java.util.Collections.emptyList());
        }
    }

    private void loadAttendanceDataByStatus() {
        if (getActivity() == null) return;
        try {
            android.database.Cursor cursor;
            if ("student".equals(role) && studentClassId > 0) {
                cursor = dbHelper.getAllCheckinTasksByClass(studentClassId);
            } else {
                cursor = dbHelper.getAllCheckinTasks();
            }
            java.util.Map<String, java.util.List<TaskRow>> activeTasksByClass = new java.util.LinkedHashMap<>();
            java.util.Map<String, java.util.List<TaskRow>> closedTasksByClass = new java.util.LinkedHashMap<>();

            if (cursor != null && cursor.moveToFirst()) {
                do {
                    long classId = cursor.getLong(cursor.getColumnIndexOrThrow("classId"));
                    String className = dbHelper.getClassNameById(classId);
                    if (className == null) className = "未知班级";

                    String title = cursor.getString(cursor.getColumnIndexOrThrow("title"));
                    String status = cursor.getString(cursor.getColumnIndexOrThrow("status"));
                    String startAt = cursor.getString(cursor.getColumnIndexOrThrow("startAt"));
                    String day = extractDay(startAt);
                    TaskRow row = new TaskRow(day + " " + title, status, startAt);

                    if ("ACTIVE".equalsIgnoreCase(status)) {
                        if (!activeTasksByClass.containsKey(className)) {
                            activeTasksByClass.put(className, new java.util.ArrayList<>());
                        }
                        activeTasksByClass.get(className).add(row);
                    } else if ("CLOSED".equalsIgnoreCase(status)) {
                        if (!closedTasksByClass.containsKey(className)) {
                            closedTasksByClass.put(className, new java.util.ArrayList<>());
                        }
                        closedTasksByClass.get(className).add(row);
                    }
                } while (cursor.moveToNext());
                cursor.close();
            }

            java.util.List<AttendanceDayAdapter.Item> items = new java.util.ArrayList<>();
            if (statusFilterMode == 0 || statusFilterMode == 1) {
                appendStatusSection(items, "ACTIVE", activeTasksByClass);
            }
            if (statusFilterMode == 0 || statusFilterMode == 2) {
                appendStatusSection(items, "CLOSED", closedTasksByClass);
            }
            dayAdapter.updateItems(items);
        } catch (Throwable t) {
            android.util.Log.e("AttendanceFragment", "按状态加载签到任务失败: " + t.getMessage(), t);
            dayAdapter.updateItems(java.util.Collections.emptyList());
        }
    }

    private void appendStatusSection(java.util.List<AttendanceDayAdapter.Item> items, String status, java.util.Map<String, java.util.List<TaskRow>> tasksByClass) {
        if (tasksByClass == null || tasksByClass.isEmpty()) return;

        items.add(AttendanceDayAdapter.Item.header(status));

        java.util.List<String> sortedClassNames = new java.util.ArrayList<>(tasksByClass.keySet());
        java.util.Collections.sort(sortedClassNames);

        for (String className : sortedClassNames) {
            java.util.List<TaskRow> rows = tasksByClass.get(className);
            if (rows == null || rows.isEmpty()) continue;

            items.add(AttendanceDayAdapter.Item.header(className));

            java.util.Comparator<TaskRow> desc = (a, b) -> {
                String left = a.sortKey == null ? "" : a.sortKey;
                String right = b.sortKey == null ? "" : b.sortKey;
                return right.compareTo(left);
            };
            java.util.Collections.sort(rows, desc);

            for (TaskRow row : rows) {
                items.add(AttendanceDayAdapter.Item.task(row.title, row.status));
            }
        }
    }

    private String extractDay(String startAt) {
        if (startAt == null) return "";
        String normalized = startAt.replace("T", " ");
        if (normalized.contains(".")) normalized = normalized.substring(0, normalized.indexOf("."));
        if (normalized.endsWith("Z")) normalized = normalized.substring(0, normalized.length() - 1);
        if (normalized.length() >= 10) {
            return normalized.substring(0, 10);
        }
        return normalized;
    }

    private static class TaskRow {
        final String title;
        final String status;
        final String sortKey;

        TaskRow(String title, String status, String sortKey) {
            this.title = title;
            this.status = status;
            this.sortKey = sortKey;
        }
    }
}
