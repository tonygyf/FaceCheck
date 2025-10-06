package com.example.facecheck.models;

public class PhotoAsset {
    private long id;
    private long sessionId;
    private String type; // RAW, ALIGNED, DEBUG
    private String uri;
    private String meta;

    public PhotoAsset() {
    }

    public PhotoAsset(long sessionId, String type, String uri) {
        this.sessionId = sessionId;
        this.type = type;
        this.uri = uri;
    }

    public PhotoAsset(long id, long sessionId, String type, String uri, String meta) {
        this.id = id;
        this.sessionId = sessionId;
        this.type = type;
        this.uri = uri;
        this.meta = meta;
    }

    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    public long getSessionId() {
        return sessionId;
    }

    public void setSessionId(long sessionId) {
        this.sessionId = sessionId;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getUri() {
        return uri;
    }

    public void setUri(String uri) {
        this.uri = uri;
    }

    public String getMeta() {
        return meta;
    }

    public void setMeta(String meta) {
        this.meta = meta;
    }
}