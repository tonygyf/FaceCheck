package com.example.facecheck.data.model;

import java.util.List;

public class ClassroomDeltaSyncResponse {
    private long newLastSyncTimestamp;
    private List<Classroom> addedClasses;
    private List<Classroom> updatedClasses;
    private List<Long> deletedClassIds;
    private boolean hasMore;
    private int totalChanges;

    public long getNewLastSyncTimestamp() {
        return newLastSyncTimestamp;
    }

    public void setNewLastSyncTimestamp(long newLastSyncTimestamp) {
        this.newLastSyncTimestamp = newLastSyncTimestamp;
    }

    public List<Classroom> getAddedClasses() {
        return addedClasses;
    }

    public void setAddedClasses(List<Classroom> addedClasses) {
        this.addedClasses = addedClasses;
    }

    public List<Classroom> getUpdatedClasses() {
        return updatedClasses;
    }

    public void setUpdatedClasses(List<Classroom> updatedClasses) {
        this.updatedClasses = updatedClasses;
    }

    public List<Long> getDeletedClassIds() {
        return deletedClassIds;
    }

    public void setDeletedClassIds(List<Long> deletedClassIds) {
        this.deletedClassIds = deletedClassIds;
    }

    public boolean isHasMore() {
        return hasMore;
    }

    public void setHasMore(boolean hasMore) {
        this.hasMore = hasMore;
    }

    public int getTotalChanges() {
        return totalChanges;
    }

    public void setTotalChanges(int totalChanges) {
        this.totalChanges = totalChanges;
    }
}
