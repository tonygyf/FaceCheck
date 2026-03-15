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

        public final String accessToken;
        public final String refreshToken;

        public Success(long userId, String role, String displayName, String accessToken, String refreshToken) {
            this.userId = userId;
            this.role = role;
            this.displayName = displayName;
            this.accessToken = accessToken;
            this.refreshToken = refreshToken;
        }
    }
    
    public static class Error extends LoginUiState {
        public final String message;
        
        public Error(String message) {
            this.message = message;
        }
    }
}
