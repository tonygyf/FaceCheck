package com.example.facecheck.api;

public class ChangeStudentUsernameRequest {
    private final long studentId;
    private final String name;

    public ChangeStudentUsernameRequest(long studentId, String name) {
        this.studentId = studentId;
        this.name = name;
    }
}
