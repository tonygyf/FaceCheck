package com.example.facecheck.ui.auth;

public class RegisterUiState {
    
    public static class Initial extends RegisterUiState {
    }
    
    public static class Loading extends RegisterUiState {
    }
    
    public static class Success extends RegisterUiState {
        public final String message;

        public Success(String message) {
            this.message = message;
        }
    }
    
    public static class Error extends RegisterUiState {
        public final String message;
        
        public Error(String message) {
            this.message = message;
        }
    }
}
