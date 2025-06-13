package com.cse441.tluprojectexpo.model;

import com.google.firebase.firestore.ServerTimestamp;
import java.util.Date;

public class Vote {
    private String id;
    private String projectId;
    private String userId;
    private int type;
    @ServerTimestamp
    private Date timestamp;

    public Vote() {

    }

    public Vote(String projectId, String userId, int type) {
        this.projectId = projectId;
        this.userId = userId;
        this.type = type;

    }

    // Getters
    public String getId() { return id; }
    public String getProjectId() { return projectId; }
    public String getUserId() { return userId; }
    public int getType() { return type; }
    public Date getTimestamp() { return timestamp; }

    // Setters
    public void setId(String id) { this.id = id; }
    public void setProjectId(String projectId) { this.projectId = projectId; }
    public void setUserId(String userId) { this.userId = userId; }
    public void setType(int type) { this.type = type; }
    public void setTimestamp(Date timestamp) { this.timestamp = timestamp; }
}