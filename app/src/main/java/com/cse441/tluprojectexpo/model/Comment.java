package com.cse441.tluprojectexpo.model;

import com.google.firebase.Timestamp;
import com.google.firebase.firestore.Exclude;
import com.google.firebase.firestore.PropertyName;

public class Comment {
    @Exclude
    private String commentId;

    @PropertyName("ProjectId") // Khớp với Firestore
    private String projectId;

    @PropertyName("AuthorUserId") // Khớp với Firestore
    private String userId;

    @PropertyName("Content") // Khớp với Firestore
    private String text;

    @PropertyName("CreatedAt") // Khớp với Firestore
    private Timestamp timestamp;

    @Exclude
    private String userName; // Sẽ được load sau
    @Exclude
    private String userAvatarUrl; // Sẽ được load sau

    public Comment() {
        // Firestore cần constructor rỗng
    }

    // Constructor để tạo comment mới khi người dùng post (thông tin user có sẵn)
    public Comment(String projectId, String userId, String userName, String userAvatarUrl, String text, Timestamp timestamp) {
        this.projectId = projectId;
        this.userId = userId;
        this.userName = userName; // Dùng để hiển thị ngay
        this.userAvatarUrl = userAvatarUrl; // Dùng để hiển thị ngay
        this.text = text;
        this.timestamp = timestamp;
    }

    // Getters and Setters
    @Exclude
    public String getCommentId() { return commentId; }
    @Exclude
    public void setCommentId(String commentId) { this.commentId = commentId; }

    public String getProjectId() { return projectId; }
    public void setProjectId(String projectId) { this.projectId = projectId; }

    // Sử dụng @PropertyName trong getter/setter để đảm bảo mapping đúng
    // ngay cả khi tên biến Java (userId) khác tên field Firestore (AuthorUserId)
    @PropertyName("AuthorUserId")
    public String getUserId() { return userId; }
    @PropertyName("AuthorUserId")
    public void setUserId(String userId) { this.userId = userId; }

    @PropertyName("Content")
    public String getText() { return text; }
    @PropertyName("Content")
    public void setText(String text) { this.text = text; }

    @PropertyName("CreatedAt")
    public Timestamp getTimestamp() { return timestamp; }
    @PropertyName("CreatedAt")
    public void setTimestamp(Timestamp timestamp) { this.timestamp = timestamp; }

    @Exclude
    public String getUserName() { return userName; }
    @Exclude
    public void setUserName(String userName) { this.userName = userName; }

    @Exclude
    public String getUserAvatarUrl() { return userAvatarUrl; }
    @Exclude
    public void setUserAvatarUrl(String userAvatarUrl) { this.userAvatarUrl = userAvatarUrl; }
}