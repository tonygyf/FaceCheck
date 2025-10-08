package com.example.facecheck.data.model;

public class SyncLog {
    private long id;
    private String entity;
    private long entityId;
    private String op; // UPSERT, DELETE
    private int version;
    private long ts;
    private String status;

    public SyncLog() {
        this.ts = System.currentTimeMillis();
    }

    public SyncLog(String entity, long entityId, String op, int version, String status) {
        this();
        this.entity = entity;
        this.entityId = entityId;
        this.op = op;
        this.version = version;
        this.status = status;
    }

    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    public String getEntity() {
        return entity;
    }

    public void setEntity(String entity) {
        this.entity = entity;
    }

    public long getEntityId() {
        return entityId;
    }

    public void setEntityId(long entityId) {
        this.entityId = entityId;
    }

    public String getOp() {
        return op;
    }

    public void setOp(String op) {
        this.op = op;
    }

    public int getVersion() {
        return version;
    }

    public void setVersion(int version) {
        this.version = version;
    }

    public long getTs() {
        return ts;
    }

    public void setTs(long ts) {
        this.ts = ts;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }
}