<<<<<<< nghia
package com.cse441.tluprojectexpo.model;

import com.google.firebase.Timestamp;
import com.google.firebase.firestore.DocumentId;
import com.google.firebase.firestore.Exclude;
import com.google.firebase.firestore.PropertyName;
import com.cse441.tluprojectexpo.model.User;

import java.util.List;

public class User {

    // @DocumentId giúp tự động lấy ID của document (ví dụ: "user_001")
    // và gán vào trường này khi bạn đọc dữ liệu.
    @DocumentId
    private String userId;

    @Exclude
    private Role role;
    @PropertyName("AvatarUrl")
    private String avatarUrl;

    @PropertyName("Class")
    private String className; // Đổi tên biến vì "class" là từ khóa trong Java

    @PropertyName("CreatedAt")
    private Timestamp createdAt; // Firestore timestamp tương ứng với com.google.firebase.Timestamp

    @PropertyName("Email")
    private String email;

    @PropertyName("FullName")
    private String fullName;

    @PropertyName("IsLocked")
    private boolean isLocked;

    @PropertyName("PasswordHash")
    private String passwordHash;

    // --- Constructors ---

    // Constructor rỗng là BẮT BUỘC để Firestore có thể tự động
    // chuyển đổi DocumentSnapshot thành đối tượng User.
    public User() {
    }

    // Constructor đầy đủ để bạn tiện khởi tạo đối tượng trong code.
    public User(String avatarUrl, String className, Timestamp createdAt, String email, String fullName, boolean isLocked, String passwordHash) {
        this.avatarUrl = avatarUrl;
        this.className = className;
        this.createdAt = createdAt;
        this.email = email;
        this.fullName = fullName;
        this.isLocked = isLocked;
        this.passwordHash = passwordHash;
    }


    // --- Getters and Setters ---
    // Getters và Setters cũng là BẮT BUỘC để Firestore hoạt động.

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

    @PropertyName("CreatedAt")
    public Timestamp getCreatedAt() {
        return createdAt;
    }

    @PropertyName("CreatedAt")
    public void setCreatedAt(Timestamp createdAt) {
        this.createdAt = createdAt;
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
=======
// User.java
package com.cse441.tluprojectexpo.model; // THAY ĐỔI PACKAGE

import com.google.firebase.firestore.Exclude; // Dùng @Exclude nếu không muốn userId được ghi lại vào Firestore từ model này
import com.google.firebase.firestore.PropertyName;

import java.util.Date;

public class User {
    @Exclude // Không ghi trường này vào Firestore khi dùng user.toObject() rồi ghi lại
    private String userId; // Thêm trường này

    private String FullName;
    @PropertyName("Class")
    private String UserClass; // Đã có từ trước
    private String AvatarUrl;  // Đã có từ trước
    // private List<String> UserRoles; // Thêm trường này nếu bạn lưu trực tiếp ID vai trò trong User
    // Hoặc bạn query UserRoles collection riêng
    private String Email; // Thêm trường Email
    // @ServerTimestamp // Bỏ chú thích nếu bạn muốn Firebase tự động điền timestamp khi ghi vào DB
    private Date CreatedAt; // Thêm trường CreatedAt
    private Boolean IsLocked; // Thêm trường IsLocked (mặc định false)
    private String PasswordHash; // Thêm trường PasswordHash

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


    @PropertyName("Email")
    public String getEmail() { return Email; }
    @PropertyName("Email")
    public void setEmail(String email) { Email = email; }

    @PropertyName("CreatedAt")
    public Date getCreatedAt() { return CreatedAt; }
    @PropertyName("CreatedAt")
    public void setCreatedAt(Date createdAt) { CreatedAt = createdAt; }

    @PropertyName("IsLocked")
    public Boolean getIsLocked() { return IsLocked; }
    @PropertyName("IsLocked")
    public void setIsLocked(Boolean isLocked) { IsLocked = isLocked; }

    @PropertyName("PasswordHash")
    public String getPasswordHash() { return PasswordHash; }
    @PropertyName("PasswordHash")
    public void setPasswordHash(String passwordHash) { PasswordHash = passwordHash; }
    // @PropertyName("UserRoles")
    // public List<String> getUserRoles() { return UserRoles; }
    // @PropertyName("UserRoles")
    // public void setUserRoles(List<String> userRoles) { UserRoles = userRoles; }
>>>>>>> main
}