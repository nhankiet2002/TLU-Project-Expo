// PATH: com/cse441/tluprojectexpo/model/UserRole.java
package com.cse441.tluprojectexpo.model;

import com.google.firebase.Timestamp; // Import Timestamp
import com.google.firebase.firestore.PropertyName;

public class UserRole {
    @PropertyName("userId")
    private String userId;

    // Sửa lại thành 'role' kiểu String
    @PropertyName("role")
    private String role; // <-- Đã sửa từ roleId thành role

    @PropertyName("assignedAt")
    private Timestamp assignedAt;

    public UserRole() {}

    // Getters and Setters cho userId
    @PropertyName("UserId")
    public String getUserId() { return userId; }

    @PropertyName("UserId")
    public void setUserId(String userId) { this.userId = userId; }

    // Getters and Setters cho role (đã sửa)
    @PropertyName("RoleId")
    public String getRole() { return role; }

    @PropertyName("RoleId")
    public void setRole(String role) { this.role = role; }

}