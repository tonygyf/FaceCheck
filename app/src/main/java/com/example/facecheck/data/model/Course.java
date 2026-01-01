package com.example.facecheck.data.model;

public class Course {
    private String id;
    private String name;
    private String teacherName;
    private String time;
    private String location;
    private String iconUrl;
    private String description;
    private boolean isSelected;

    public Course(String id, String name, String teacherName, String time, String location, String description,
            String iconUrl) {
        this.id = id;
        this.name = name;
        this.teacherName = teacherName;
        this.time = time;
        this.location = location;
        this.description = description;
        this.iconUrl = iconUrl;
        this.isSelected = false;
    }

    // Getters and Setters
    public String getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public String getTeacherName() {
        return teacherName;
    }

    public String getTime() {
        return time;
    }

    public String getLocation() {
        return location;
    }

    public String getIconUrl() {
        return iconUrl;
    }

    public String getDescription() {
        return description;
    }

    public boolean isSelected() {
        return isSelected;
    }

    public void setSelected(boolean selected) {
        isSelected = selected;
    }
}
