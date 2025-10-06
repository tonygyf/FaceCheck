package com.example.facecheck.ui.auth;

public class LoginUiState {
    
    public static class Initial extends LoginUiState {
    }
    
    public static class Loading extends LoginUiState {
    }
    
    public static class Success extends LoginUiState {
        public final long teacherId;
        public final String teacherName;
        
        public Success(long teacherId, String teacherName) {
            this.teacherId = teacherId;
            this.teacherName = teacherName;
        }
    }
    
    public static class Error extends LoginUiState {
        public final String message;
        
        public Error(String message) {
            this.message = message;
        }
    }
}