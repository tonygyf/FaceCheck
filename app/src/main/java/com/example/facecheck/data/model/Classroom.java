package com.example.facecheck.data.model;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.TimeZone;

public class Classroom {
    private long id;
    private long teacherId;
    private String name;
    private int year;
    private String semester;
    private String courseName;
    private String meta;
    private int studentCount; // This field is for query results, not a DB column
    private String createdAt; // FIX: Changed from long to String to accept "2026-01-23 02:43:29" format

    public Classroom() {
    }

    public Classroom(long teacherId, String name, int year) {
        this.teacherId = teacherId;
        this.name = name;
        this.year = year;
    }

    // FIX: Constructor now accepts String for createdAt
    public Classroom(long id, long teacherId, String name, int year, String semester, String courseName, String meta, String createdAt) {
        this.id = id;
        this.teacherId = teacherId;
        this.name = name;
        this.year = year;
        this.semester = semester;
        this.courseName = courseName;
        this.meta = meta;
        this.createdAt = createdAt;
    }

    // FIX: Keep a long-based constructor for local DB usage (pass timestamp as long, stored as String)
    public Classroom(long id, long teacherId, String name, int year, String semester, String courseName, String meta, long createdAtMillis) {
        this.id = id;
        this.teacherId = teacherId;
        this.name = name;
        this.year = year;
        this.semester = semester;
        this.courseName = courseName;
        this.meta = meta;
        this.createdAt = String.valueOf(createdAtMillis);
    }

    public Classroom(long id, long teacherId, String name, int year, String meta) {
        this(id, teacherId, name, year, null, null, meta, 0L);
    }

    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    public long getTeacherId() {
        return teacherId;
    }

    public void setTeacherId(long teacherId) {
        this.teacherId = teacherId;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public int getYear() {
        return year;
    }

    public void setYear(int year) {
        this.year = year;
    }

    public String getSemester() {
        return semester;
    }

    public void setSemester(String semester) {
        this.semester = semester;
    }

    public String getCourseName() {
        return courseName;
    }

    public void setCourseName(String courseName) {
        this.courseName = courseName;
    }

    public String getMeta() {
        return meta;
    }

    public void setMeta(String meta) {
        this.meta = meta;
    }

    public int getStudentCount() {
        return studentCount;
    }

    public void setStudentCount(int studentCount) {
        this.studentCount = studentCount;
    }

    // FIX: getter/setter now use String
    public String getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(String createdAt) {
        this.createdAt = createdAt;
    }

    /**
     * 将 createdAt 转换为毫秒时间戳，用于需要数字比较的场景。
     * 支持两种格式：
     *   - "2026-01-23 02:43:29"（服务器返回的日期字符串）
     *   - "1706024609000"（本地存储的毫秒数字符串）
     */
    public long getCreatedAtMillis() {
        if (createdAt == null || createdAt.isEmpty()) return 0L;
        try {
            // 先尝试当纯数字（本地存的毫秒）
            return Long.parseLong(createdAt);
        } catch (NumberFormatException e1) {
            try {
                // 再尝试解析服务器日期格式
                SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US);
                sdf.setTimeZone(TimeZone.getTimeZone("UTC"));
                Date d = sdf.parse(createdAt);
                return d != null ? d.getTime() : 0L;
            } catch (Exception e2) {
                return 0L;
            }
        }
    }
}