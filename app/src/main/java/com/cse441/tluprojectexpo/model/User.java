package com.cse441.tluprojectexpo.model;
import com.google.firebase.firestore.DocumentId;
import com.google.firebase.firestore.ServerTimestamp;

import java.sql.Timestamp;
import java.util.Date;

public class User {

    @DocumentId
    private String id;
    private String fullName;
    private String email;
    private String passwordHash;
    private String className;
    private String facultyName;
    private String role;

    @ServerTimestamp
    private Date createdAt;

    private Boolean status;

    public User() {

    }

    public User(String fullName, String email, String passwordHash, String facultyName, String role, Timestamp createdAt) {
        this.fullName = fullName;
        this.email = email;
        this.passwordHash = passwordHash;
        this.facultyName = facultyName;
        this.role = role;
        this.createdAt = createdAt;
    }


    public Date getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Date createdAt) {
        this.createdAt = createdAt;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getFacultyName() {
        return facultyName;
    }

    public void setFacultyName(String facultyName) {
        this.facultyName = facultyName;
    }

    public String getFullName() {
        return fullName;
    }

    public void setFullName(String fullName) {
        this.fullName = fullName;
    }

    public String getClassName() {
        return className;
    }

    public void setClassName(String className) {
        this.className = className;
    }

    public String getPasswordHash() {
        return passwordHash;
    }

    public void setPasswordHash(String passwordHash) {
        this.passwordHash = passwordHash;
    }

    public String getRole() {
        return role;
    }

    public void setRole(String role) {
        this.role = role;
    }

    public String getId() {
        return id;
    }

    public void setStatus(Boolean status) {
        this.status = status;
    }

    public Boolean getStatus() {
        return status;
    }
}
