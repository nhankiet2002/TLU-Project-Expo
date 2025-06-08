package com.cse441.tluprojectexpo.model;

// Trong package model của bạn, ví dụ: com.cse441.tluprojectexpo.model
import com.google.firebase.firestore.PropertyName; // Import annotation này

public class Member {
    private String userId; // Sẽ được gán từ ID của document

    @PropertyName("fullName") // Ánh xạ tới trường 'fullName' trên Firestore
    private String name; // Trong class, ta dùng 'name' cho tiện

    @PropertyName("avatarUrl")
    private String avatarUrl;

    @PropertyName("className")
    private String className;

    // Constructor rỗng cần thiết cho Firestore
    public Member() {
    }

    // Constructor với các tham số (tùy chọn, nhưng hữu ích)
    public Member(String userId, String name, String avatarUrl, String className) {
        this.userId = userId;
        this.name = name;
        this.avatarUrl = avatarUrl;
        this.className = className;
    }

    // Getters
    public String getUserId() { return userId; }

    @PropertyName("fullName") // Getter cũng cần annotation nếu tên khác
    public String getName() { return name; }

    @PropertyName("avatarUrl")
    public String getAvatarUrl() { return avatarUrl; }

    @PropertyName("className")
    public String getClassName() { return className; }

    // Setters (cũng cần thiết nếu bạn tạo đối tượng và set thủ công, hoặc Firestore cần)
    public void setUserId(String userId) { this.userId = userId; }

    @PropertyName("fullName") // Setter cũng cần annotation
    public void setName(String name) { this.name = name; }

    @PropertyName("avatarUrl")
    public void setAvatarUrl(String avatarUrl) { this.avatarUrl = avatarUrl; }

    @PropertyName("className")
    public void setClassName(String className) { this.className = className; }

    // (Tùy chọn) Ghi đè toString() để debug dễ hơn
    @Override
    public String toString() {
        return "Member{" +
                "userId='" + userId + '\'' +
                ", name='" + name + '\'' +
                ", avatarUrl='" + avatarUrl + '\'' +
                ", className='" + className + '\'' +
                '}';
    }
}