package com.example.facecheck.api;

public class ApiCreateResponse {
    private boolean ok;
    private boolean success;
    private Long id;
    private String error;

    public boolean isOk() {
        return ok || success;
    }

    public void setOk(boolean ok) {
        this.ok = ok;
    }

    public boolean isSuccess() {
        return success;
    }

    public void setSuccess(boolean success) {
        this.success = success;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getError() {
        return error;
    }

    public void setError(String error) {
        this.error = error;
    }
}
