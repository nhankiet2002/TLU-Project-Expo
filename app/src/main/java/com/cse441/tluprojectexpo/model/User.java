// app/src/main/java/com/cse441/tluprojectexpo/model/User.java
package com.cse441.tluprojectexpo.model;

import com.google.firebase.firestore.Exclude;
import com.google.firebase.firestore.PropertyName;

import java.util.Date;
import java.util.List;

public class User {
    private String userId; // UID từ Firebase Authentication
    private String email;
    private String fullName;
    private String role; // Ví dụ: "student", "faculty", "admin"
    private String className; // Thêm trường lớp học nếu cần
    private String avatarUrl; // URL ảnh đại diện (tùy chọn)
    private List<String> permissions; // Danh sách các quyền của người dùng
    private Date createdAt;
    private Date lastLogin;
    private Boolean isEmailVerified;

    // Constructor rỗng cần thiết cho Firebase Firestore
    public User() {
        // Mặc định cho Firestore
    }

    // Constructor với các tham số cơ bản (có thể mở rộng nếu cần)
    public User(String userId, String email, String fullName, String role, String className) {
        this.userId = userId;
        this.email = email;
        this.fullName = fullName;
        this.role = role;
        this.className = className;
        this.createdAt = new Date(); // Gán thời gian tạo khi khởi tạo đối tượng
        this.isEmailVerified = false; // Mặc định là chưa xác thực
    }

    // --- Getters ---
    public String getUserId() {
        return userId;
    }

    public String getEmail() {
        return email;
    }

    // Sử dụng @PropertyName nếu tên trường trong class khác với tên trường trong Firestore
    @PropertyName("fullName") // Ánh xạ đến trường "fullName" trong Firestore
    public String getFullName() {
        return fullName;
    }

    public String getRole() {
        return role;
    }

    @PropertyName("className")
    public String getClassName() {
        return className;
    }

    @PropertyName("avatarUrl")
    public String getAvatarUrl() {
        return avatarUrl;
    }

    public List<String> getPermissions() {
        return permissions;
    }

    public Date getCreatedAt() {
        return createdAt;
    }

    public Date getLastLogin() {
        return lastLogin;
    }

    @PropertyName("isEmailVerified")
    public Boolean getIsEmailVerified() {
        return isEmailVerified;
    }

    // --- Setters ---
    public void setUserId(String userId) {
        this.userId = userId;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    @PropertyName("fullName")
    public void setFullName(String fullName) {
        this.fullName = fullName;
    }

    public void setRole(String role) {
        this.role = role;
    }

    @PropertyName("className")
    public void setClassName(String className) {
        this.className = className;
    }

    @PropertyName("avatarUrl")
    public void setAvatarUrl(String avatarUrl) {
        this.avatarUrl = avatarUrl;
    }

    public void setPermissions(List<String> permissions) {
        this.permissions = permissions;
    }

    public void setCreatedAt(Date createdAt) {
        this.createdAt = createdAt;
    }

    public void setLastLogin(Date lastLogin) {
        this.lastLogin = lastLogin;
    }

    @PropertyName("isEmailVerified")
    public void setIsEmailVerified(Boolean emailVerified) {
        isEmailVerified = emailVerified;
    }

    @Override
    public String toString() {
        return "User{" +
                "userId='" + userId + '\'' +
                ", email='" + email + '\'' +
                ", fullName='" + fullName + '\'' +
                ", role='" + role + '\'' +
                ", className='" + className + '\'' +
                ", avatarUrl='" + avatarUrl + '\'' +
                ", permissions=" + permissions +
                ", createdAt=" + createdAt +
                ", lastLogin=" + lastLogin +
                ", isEmailVerified=" + isEmailVerified +
                '}';
    }
}