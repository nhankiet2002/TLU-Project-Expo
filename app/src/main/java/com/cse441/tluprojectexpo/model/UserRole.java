// PATH: com/cse441/tluprojectexpo/model/UserRole.java
package com.cse441.tluprojectexpo.model;

import com.google.firebase.Timestamp;
import com.google.firebase.firestore.PropertyName;

public class UserRole {
    // Đảm bảo "userId" ở đây khớp với tên trường trong Firestore (nếu nó là 'userId' thường)
    // Nếu trong Firestore nó là "UserId" (U hoa), bạn cần đổi `@PropertyName("userId")` thành `@PropertyName("UserId")`
    @PropertyName("UserId") // <-- Giả định Firestore dùng "UserId" (U hoa)
    private String userId;

    // Đảm bảo "RoleId" ở đây khớp với tên trường trong Firestore
    @PropertyName("RoleId") // <-- Giả định Firestore dùng "RoleId" (R hoa, I thường)
    private String roleId; // <-- Giữ tên biến là roleId cho nhất quán

    @PropertyName("assignedAt")
    private Timestamp assignedAt;

    // Constructor rỗng (REQUIRED for Firestore toObject())
    public UserRole() {}

    // Constructor CÓ ĐỐI SỐ (REQUIRED for new UserRole(userId, roleId))
    public UserRole(String userId, String roleId) {
        this.userId = userId;
        this.roleId = roleId;
        this.assignedAt = Timestamp.now(); // Initialize assignedAt
    }

    // Getters and Setters cho userId (đảm bảo @PropertyName khớp)
    @PropertyName("UserId")
    public String getUserId() { return userId; }

    @PropertyName("UserId")
    public void setUserId(String userId) { this.userId = userId; }

    // Getters and Setters cho roleId (đảm bảo @PropertyName khớp)
    @PropertyName("RoleId") // <-- Phải là RoleId
    public String getRoleId() { return roleId; } // <-- Trả về biến roleId

    @PropertyName("RoleId") // <-- Phải là RoleId
    public void setRoleId(String roleId) { this.roleId = roleId; } // <-- Nhận và gán cho biến roleId

    // Getters and Setters cho assignedAt
    @PropertyName("assignedAt")
    public Timestamp getAssignedAt() { return assignedAt; }

    @PropertyName("assignedAt")
    public void setAssignedAt(Timestamp assignedAt) { this.assignedAt = assignedAt; }
}