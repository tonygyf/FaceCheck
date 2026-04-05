package com.example.facecheck.api;

public class ChangeStudentPasswordRequest {
    private final long studentId;
    private final String oldPassword;
    private final String newPassword;

    public ChangeStudentPasswordRequest(long studentId, String oldPassword, String newPassword) {
        this.studentId = studentId;
        this.oldPassword = oldPassword;
        this.newPassword = newPassword;
    }
}
