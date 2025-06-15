package com.cse441.tluprojectexpo.model;
import java.sql.Timestamp;
public class User {
    private String fullName;
    private String email;
    private String passwordHash;
    private String facultyName;
    private String role;
    private Timestamp createdAt;

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


    public Timestamp getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Timestamp createdAt) {
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
}
