package com.cse441.tluprojectexpo.model;
import com.google.firebase.firestore.PropertyName;
public class UserRole {
    @PropertyName("UserId")
    private String userId;

    @PropertyName("RoleId")
    private String roleId;

    public UserRole() {}

    public UserRole(String userId, String roleId) {
        this.userId = userId;
        this.roleId = roleId;
    }

    // Getters and Setters
    @PropertyName("UserId")
    public String getUserId() { return userId; }

    @PropertyName("UserId")
    public void setUserId(String userId) { this.userId = userId; }

    @PropertyName("RoleId")
    public String getRoleId() { return roleId; }

    @PropertyName("RoleId")
    public void setRoleId(String roleId) { this.roleId = roleId; }

}
