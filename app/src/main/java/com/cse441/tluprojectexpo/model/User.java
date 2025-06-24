// User.java
package com.cse441.tluprojectexpo.model; // Đảm bảo package này đúng với cấu trúc dự án của bạn

import com.google.firebase.firestore.DocumentId;
import com.google.firebase.firestore.Exclude;
import com.google.firebase.firestore.PropertyName;

import java.io.Serializable;

public class User implements Serializable {

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
        this.role = new Role("User");
    }

    public User(String avatarUrl, String className, String email, String fullName, boolean isLocked, String passwordHash) {
        this.avatarUrl = avatarUrl;
        this.className = className;
        this.email = email;
        this.fullName = fullName;
        this.isLocked = isLocked;
        this.passwordHash = passwordHash;
        this.role = new Role("User");
    }

    public User(String finalUserIdToUse, String nameToUse, String finalEmailForFallback, String s, String s1) {
        this.userId = finalUserIdToUse;
        this.fullName = nameToUse;
        this.email = finalEmailForFallback;
        this.className = s;
        this.avatarUrl = s1;
        this.isLocked = false; // Mặc định không khóa
        this.passwordHash = ""; // Mặc định không có mật khẩu
        this.role = new Role("User");
    }


    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
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

    @PropertyName("AvatarUrl")
    public void setAvatarUrl(String avatarUrl){
        this.avatarUrl = avatarUrl;
    }

    @PropertyName("AvatarUrl")
    public String getAvatarUrl(){
        return avatarUrl;
    }

}