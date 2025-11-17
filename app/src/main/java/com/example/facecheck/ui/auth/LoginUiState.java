package com.example.facecheck.ui.auth;

public class LoginUiState {
    
    public static class Initial extends LoginUiState {
    }
    
    public static class Loading extends LoginUiState {
    }
    
    public static class Success extends LoginUiState {
        public final long userId;
        public final String role; // "teacher" 或 "student"
        public final String displayName;

        public Success(long userId, String role, String displayName) {
            this.userId = userId;
            this.role = role;
            this.displayName = displayName;
        }
    }
    
    public static class Error extends LoginUiState {
        public final String message;
        
        public Error(String message) {
            this.message = message;
        }
    }
}
