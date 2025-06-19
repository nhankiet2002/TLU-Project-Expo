
package com.cse441.tluprojectexpo.model;

import com.google.firebase.firestore.DocumentId;
import com.google.firebase.firestore.Exclude;
import com.google.firebase.firestore.PropertyName;

public class User {

    @DocumentId
    private String userId;

    @Exclude
    private Role role;
    @PropertyName("AvatarUrl")
    private String avatarUrl;

    @PropertyName("Class")
    private String className;

    @PropertyName("Email")
    private String email;

    @PropertyName("FullName")
    private String fullName;

    @PropertyName("IsLocked")
    private boolean isLocked;

    @PropertyName("PasswordHash")
    private String passwordHash;

    public User() {
    }

    public User(String avatarUrl, String className, String email, String fullName, boolean isLocked, String passwordHash) {
        this.avatarUrl = avatarUrl;
        this.className = className;
        this.email = email;
        this.fullName = fullName;
        this.isLocked = isLocked;
        this.passwordHash = passwordHash;
    }


    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    @PropertyName("AvatarUrl")
    public String getAvatarUrl() {
        return avatarUrl;
    }

    @PropertyName("AvatarUrl")
    public void setAvatarUrl(String avatarUrl) {
        this.avatarUrl = avatarUrl;
    }

    @PropertyName("Class")
    public String getClassName() {
        return className;
    }

    @PropertyName("Class")
    public void setClassName(String className) {
        this.className = className;
    }

    @PropertyName("Email")
    public String getEmail() {
        return email;
    }

    @PropertyName("Email")
    public void setEmail(String email) {
        this.email = email;
    }

    @PropertyName("FullName")
    public String getFullName() {
        return fullName;
    }

    @PropertyName("FullName")
    public void setFullName(String fullName) {
        this.fullName = fullName;
    }

    @PropertyName("IsLocked")
    public boolean isLocked() {
        return isLocked;
    }

    @PropertyName("IsLocked")
    public void setLocked(boolean locked) {
        isLocked = locked;
    }


    @PropertyName("PasswordHash")
    public String getPasswordHash() {
        return passwordHash;
    }

    @PropertyName("PasswordHash")
    public void setPasswordHash(String passwordHash) {
        this.passwordHash = passwordHash;
    }

    @Exclude
    public Role getRole() {
        return role;
    }

    @Exclude
    public void setRole(Role role) {
        this.role = role;
    }
}