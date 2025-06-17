// User.java
package com.cse441.tluprojectexpo.model; // THAY ĐỔI PACKAGE

import com.google.firebase.firestore.Exclude; // Dùng @Exclude nếu không muốn userId được ghi lại vào Firestore từ model này
import com.google.firebase.firestore.PropertyName;

public class User {
    @Exclude // Không ghi trường này vào Firestore khi dùng user.toObject() rồi ghi lại
    private String userId; // Thêm trường này

    private String FullName;
    @PropertyName("Class")
    private String UserClass; // Đã có từ trước
    private String AvatarUrl;  // Đã có từ trước
    // private List<String> UserRoles; // Thêm trường này nếu bạn lưu trực tiếp ID vai trò trong User
    // Hoặc bạn query UserRoles collection riêng

    // Constructors, Getters, Setters
    public User() {}

    @Exclude
    public String getUserId() { return userId; }
    @Exclude
    public void setUserId(String userId) { this.userId = userId; }

    @PropertyName("FullName")
    public String getFullName() { return FullName; }
    @PropertyName("FullName")
    public void setFullName(String fullName) { FullName = fullName; }

    @PropertyName("Class")
    public String getUserClass() { return UserClass; }
    @PropertyName("Class")
    public void setUserClass(String userClass) { UserClass = userClass; }

    @PropertyName("AvatarUrl")
    public String getAvatarUrl() { return AvatarUrl; }
    @PropertyName("AvatarUrl")
    public void setAvatarUrl(String avatarUrl) { AvatarUrl = avatarUrl; }

    // @PropertyName("UserRoles")
    // public List<String> getUserRoles() { return UserRoles; }
    // @PropertyName("UserRoles")
    // public void setUserRoles(List<String> userRoles) { UserRoles = userRoles; }
}