package com.cse441.tluprojectexpo.model;

import com.google.firebase.firestore.ServerTimestamp;
import java.util.Date;

public class Comment {
    private String id;
    private String projectId;
    private String userId;
    private String userName;
    private String userAvatarUrl;
    private String content;
    @ServerTimestamp
    private Date timestamp;

    public Comment() {

    }

    public Comment(String projectId, String userId, String userName, String userAvatarUrl, String content) {
        this.projectId = projectId;
        this.userId = userId;
        this.userName = userName;
        this.userAvatarUrl = userAvatarUrl;
        this.content = content;
    }

    // Getters
    public String getId() { return id; }
    public String getProjectId() { return projectId; }
    public String getUserId() { return userId; }
    public String getUserName() { return userName; }
    public String getUserAvatarUrl() { return userAvatarUrl; }
    public String getContent() { return content; }
    public Date getTimestamp() { return timestamp; }


    public void setId(String id) { this.id = id; }
    public void setProjectId(String projectId) { this.projectId = projectId; }
    public void setUserId(String userId) { this.userId = userId; }
    public void setUserName(String userName) { this.userName = userName; }
    public void setUserAvatarUrl(String userAvatarUrl) { this.userAvatarUrl = userAvatarUrl; }
    public void setContent(String content) { this.content = content; }
    public void setTimestamp(Date timestamp) { this.timestamp = timestamp; }
}