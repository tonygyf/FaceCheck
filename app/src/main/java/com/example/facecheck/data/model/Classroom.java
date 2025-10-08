package com.example.facecheck.data.model;

public class Classroom {
    private long id;
    private long teacherId;
    private String name;
    private int year;
    private String meta;

    public Classroom() {
    }

    public Classroom(long teacherId, String name, int year) {
        this.teacherId = teacherId;
        this.name = name;
        this.year = year;
    }

    public Classroom(long id, long teacherId, String name, int year, String meta) {
        this.id = id;
        this.teacherId = teacherId;
        this.name = name;
        this.year = year;
        this.meta = meta;
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

    public String getMeta() {
        return meta;
    }

    public void setMeta(String meta) {
        this.meta = meta;
    }
}