package com.example.facecheck.data.model;

public class Classroom {
    private long id;
    private long teacherId;
    private String name;
    private int year;
    private String semester;
    private String courseName;
    private String meta;
    private int studentCount; // This field is for query results, not a DB column
    private long createdAt; // New field to store creation timestamp from server

    public Classroom() {
    }

    public Classroom(long teacherId, String name, int year) {
        this.teacherId = teacherId;
        this.name = name;
        this.year = year;
    }

    public Classroom(long id, long teacherId, String name, int year, String semester, String courseName, String meta, long createdAt) {
        this.id = id;
        this.teacherId = teacherId;
        this.name = name;
        this.year = year;
        this.semester = semester;
        this.courseName = courseName;
        this.meta = meta;
        this.createdAt = createdAt;
    }

    public Classroom(long id, long teacherId, String name, int year, String meta) {
        this(id, teacherId, name, year, null, null, meta, 0);
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

    public long getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(long createdAt) {
        this.createdAt = createdAt;
    }
}