// User.java
package com.cse441.tluprojectexpo.model; // Đảm bảo package này đúng với cấu trúc dự án của bạn

import com.google.firebase.firestore.Exclude;
import com.google.firebase.firestore.PropertyName;
// Bạn có thể cần import java.util.Date nếu sử dụng kiểu Date cho CreatedAt
// import java.util.Date;

public class User {

    @Exclude // userId thường là ID của document, không lưu trữ như một field bên trong document đó
    private String userId;

    // Các trường này sẽ được map với tên field tương ứng trong Firestore
    // thông qua @PropertyName nếu tên biến Java khác tên field Firestore,
    // hoặc tự động map nếu tên biến Java và tên field Firestore giống hệt nhau (phân biệt chữ hoa/thường).
    // Dựa theo CSDL ví dụ, các field trong Firestore có vẻ là chữ hoa ở đầu.
    private String FullName;
    private String Email;
    @PropertyName("Class") // Field trong Firestore là "Class"
    private String UserClass; // Tên biến trong Java
    private String AvatarUrl;

    // Các trường tùy chọn khác từ CSDL ví dụ của bạn mà bạn có thể muốn đưa vào model
    // khi fetch dữ liệu User từ Firestore.
    // private String CreatedAt; // Dữ liệu ví dụ là String, bạn có thể dùng Date nếu muốn convert
    // private Boolean IsLocked;

    /**
     * Constructor mặc định.
     * BẮT BUỘC phải có để Firestore có thể chuyển đổi DocumentSnapshot thành đối tượng User
     * bằng cách sử dụng `documentSnapshot.toObject(User.class)`.
     */
    public User() {
        // Firestore cần constructor này
    }

    /**
     * Constructor để tạo đối tượng User một cách thuận tiện trong code Java,
     * ví dụ như khi tạo `fallbackUser` trong `CreateFragment`.
     *
     * @param userId    ID của người dùng (thường là UID từ Firebase Auth).
     * @param fullName  Họ và tên đầy đủ.
     * @param email     Địa chỉ email.
     * @param userClass Lớp hoặc thông tin tương tự.
     * @param avatarUrl URL ảnh đại diện.
     */
    public User(String userId, String fullName, String email, String userClass, String avatarUrl) {
        this.userId = userId;
        this.FullName = fullName; // Gán cho biến thành viên của class
        this.Email = email;       // Gán cho biến thành viên của class
        this.UserClass = userClass;
        this.AvatarUrl = avatarUrl;
    }

    // --- Getters and Setters ---
    // Các phương thức getter và setter cho phép truy cập và thay đổi giá trị của các trường.
    // Annotation @PropertyName được sử dụng khi tên biến trong Java khác với tên trường
    // trong Firestore, hoặc để đảm bảo mapping chính xác.

    @Exclude // Đánh dấu để không serialize/deserialize trường này với Firestore
    public String getUserId() {
        return userId;
    }

    @Exclude
    public void setUserId(String userId) {
        this.userId = userId;
    }

    @PropertyName("FullName") // Đảm bảo map với field "FullName" trong Firestore
    public String getFullName() {
        return FullName;
    }

    @PropertyName("FullName")
    public void setFullName(String fullName) {
        this.FullName = fullName;
    }

    @PropertyName("Email") // Đảm bảo map với field "Email" trong Firestore
    public String getEmail() {
        return Email;
    }

    @PropertyName("Email")
    public void setEmail(String email) {
        this.Email = email;
    }

    @PropertyName("Class") // Đảm bảo map với field "Class" trong Firestore
    public String getUserClass() {
        return UserClass;
    }

    @PropertyName("Class")
    public void setUserClass(String userClass) {
        this.UserClass = userClass;
    }

    @PropertyName("AvatarUrl") // Đảm bảo map với field "AvatarUrl" trong Firestore
    public String getAvatarUrl() {
        return AvatarUrl;
    }

    @PropertyName("AvatarUrl")
    public void setAvatarUrl(String avatarUrl) {
        this.AvatarUrl = avatarUrl;
    }

    /*
    // Nếu bạn thêm các trường tùy chọn khác:
    @PropertyName("CreatedAt")
    public String getCreatedAt() {
        return CreatedAt;
    }

    @PropertyName("CreatedAt")
    public void setCreatedAt(String createdAt) {
        CreatedAt = createdAt;
    }

    @PropertyName("IsLocked")
    public Boolean getIsLocked() { // Hoặc dùng isIsLocked() nếu getter cho boolean
        return IsLocked;
    }

    @PropertyName("IsLocked")
    public void setIsLocked(Boolean locked) {
        IsLocked = locked;
    }
    */

    // Phương thức toString() hữu ích cho việc gỡ lỗi
    @Override
    public String toString() {
        return "User{" +
                "userId='" + userId + '\'' +
                ", FullName='" + FullName + '\'' +
                ", Email='" + Email + '\'' +
                ", UserClass='" + UserClass + '\'' +
                ", AvatarUrl='" + AvatarUrl + '\'' +
                '}';
    }
}