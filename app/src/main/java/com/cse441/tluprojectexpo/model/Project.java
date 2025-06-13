package com.cse441.tluprojectexpo.model;

public class Project {
    private String id; // Firestore sẽ tự tạo ID cho document, bạn có thể lưu nó vào đây
    private String name;
    private String description;
    private String author;
    private String technology;
    private String imageUrl;
    private long timestamp;

    // Constructor rỗng cần thiết cho Firestore
    public Project() {
    }

    public Project(String name, String description, String author, String technology, String imageUrl) {
        // Bỏ id ở constructor này nếu Firestore tự tạo
        this.name = name;
        this.description = description;
        this.author = author;
        this.technology = technology;
        this.imageUrl = imageUrl;
        this.timestamp = timestamp;
    }

    // Getters
    public String getId() { return id; }
    public String getName() { return name; }
    public String getDescription() { return description; }
    public String getAuthor() { return author; }
    public String getTechnology() { return technology; }
    public String getImageUrl() { return imageUrl; }
    public long getTimestamp() { return timestamp;}

    // Setters (Quan trọng cho Firestore khi đọc dữ liệu)
    public void setId(String id) { this.id = id; }
    public void setName(String name) { this.name = name; }
    public void setDescription(String description) { this.description = description; }
    public void setAuthor(String author) { this.author = author; }
    public void setTechnology(String technology) { this.technology = technology; }
    public void setImageUrl(String imageUrl) { this.imageUrl = imageUrl; }
    public void setTimestamp(long timestamp) { this.timestamp = timestamp; }
}