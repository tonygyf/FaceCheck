package com.example.facecheck.data.model;

import java.util.List;

public class SyncDownloadResponse {
    private List<Classroom> classrooms;
    private List<Student> students;
    private List<FaceEmbedding> embeddings;
    private List<AttendanceSession> sessions;
    private List<AttendanceResult> results;

    public List<Classroom> getClassrooms() {
        return classrooms;
    }

    public void setClassrooms(List<Classroom> classrooms) {
        this.classrooms = classrooms;
    }

    public List<Student> getStudents() {
        return students;
    }

    public void setStudents(List<Student> students) {
        this.students = students;
    }

    public List<FaceEmbedding> getEmbeddings() {
        return embeddings;
    }

    public void setEmbeddings(List<FaceEmbedding> embeddings) {
        this.embeddings = embeddings;
    }

    public List<AttendanceSession> getSessions() {
        return sessions;
    }

    public void setSessions(List<AttendanceSession> sessions) {
        this.sessions = sessions;
    }

    public List<AttendanceResult> getResults() {
        return results;
    }

    public void setResults(List<AttendanceResult> results) {
        this.results = results;
    }
}
