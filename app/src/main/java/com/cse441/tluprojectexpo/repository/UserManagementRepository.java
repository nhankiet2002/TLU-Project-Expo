package com.cse441.tluprojectexpo.repository;

import android.util.Log;
import androidx.annotation.NonNull;

import com.cse441.tluprojectexpo.model.Role;
import com.cse441.tluprojectexpo.model.User;
import com.cse441.tluprojectexpo.model.UserRole;
import com.google.android.gms.tasks.Task;
import com.google.android.gms.tasks.Tasks;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QuerySnapshot;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class UserManagementRepository {

    private static final String TAG = "UserRepository";
    private static final String USERS_COLLECTION = "Users";
    private static final String ROLES_COLLECTION = "Roles";
    private static final String USER_ROLES_COLLECTION = "UserRoles";

    private final FirebaseFirestore db;

    // Interface để trả kết quả về cho ViewModel hoặc Activity/Fragment
    public interface OnUsersDataChangedListener {
        void onUsersLoaded(List<User> userList);
        void onError(Exception e);
    }

    public interface OnTaskCompleteListener {
        void onSuccess();
        void onFailure(Exception e);
    }

    // Constructor
    public UserManagementRepository() {
        this.db = FirebaseFirestore.getInstance();
    }

    /**
     * Lấy danh sách tất cả người dùng, đã được ghép nối với thông tin Role của họ.
     * @param listener Callback để trả về danh sách user hoàn chỉnh hoặc lỗi.
     */
    public void getAllUsersWithRoles(OnUsersDataChangedListener listener) {
        // Tạo các task để lấy dữ liệu từ 3 collection cùng lúc
        Task<QuerySnapshot> usersTask = db.collection(USERS_COLLECTION).get();
        Task<QuerySnapshot> userRolesTask = db.collection(USER_ROLES_COLLECTION).get();
        Task<QuerySnapshot> rolesTask = db.collection(ROLES_COLLECTION).get();

        // Gộp các task lại và chờ tất cả hoàn thành
        Task<List<QuerySnapshot>> allTasks = Tasks.whenAllSuccess(usersTask, userRolesTask, rolesTask);

        allTasks.addOnSuccessListener(querySnapshots -> {
            // querySnapshots là một List chứa kết quả của 3 task theo đúng thứ tự
            QuerySnapshot usersSnapshot = querySnapshots.get(0);
            QuerySnapshot userRolesSnapshot = querySnapshots.get(1);
            QuerySnapshot rolesSnapshot = querySnapshots.get(2);

            // Xử lý và ghép nối dữ liệu
            List<User> processedUsers = processAndJoinData(usersSnapshot, userRolesSnapshot, rolesSnapshot);

            // Trả kết quả về qua listener
            listener.onUsersLoaded(processedUsers);

        }).addOnFailureListener(e -> {
            Log.e(TAG, "Lỗi khi lấy dữ liệu từ nhiều collection", e);
            listener.onError(e);
        });
    }

    /**
     * Phương thức private để xử lý logic ghép nối dữ liệu.
     * @return Danh sách User đã được điền thông tin Role.
     */
    private List<User> processAndJoinData(QuerySnapshot usersSnapshot, QuerySnapshot userRolesSnapshot, QuerySnapshot rolesSnapshot) {
        // Map<roleId, Role>
        Map<String, Role> roleMap = new HashMap<>();
        for (Role role : rolesSnapshot.toObjects(Role.class)) {
            roleMap.put(role.getRoleId(), role);
        }

        // Map<userId, List<roleId>>
        Map<String, String> userToRoleIdMap = new HashMap<>();
        for (UserRole userRole : userRolesSnapshot.toObjects(UserRole.class)) {
            userToRoleIdMap.put(userRole.getUserId(), userRole.getRoleId());
        }

        // Ghép nối dữ liệu
        List<User> finalUserList = new ArrayList<>();
        for (User user : usersSnapshot.toObjects(User.class)) {
            String roleId = userToRoleIdMap.get(user.getUserId());

            if (roleId != null) {
                Role role = roleMap.get(roleId);
                if(role != null){
                    user.setRole(role);
                }
            }
            finalUserList.add(user);
        }
        return finalUserList;
    }

    /**
     * Cập nhật trạng thái khóa (IsLocked) của một người dùng.
     * @param userId ID của người dùng cần cập nhật.
     * @param isLocked Trạng thái khóa mới (true hoặc false).
     * @param listener Callback để thông báo kết quả.
     */
    public void updateUserLockState(String userId, boolean isLocked, @NonNull OnTaskCompleteListener listener) {
        if (userId == null || userId.isEmpty()) {
            listener.onFailure(new IllegalArgumentException("User ID không được rỗng"));
            return;
        }

        db.collection(USERS_COLLECTION).document(userId)
                .update("IsLocked", isLocked)
                .addOnSuccessListener(aVoid -> listener.onSuccess())
                .addOnFailureListener(listener::onFailure);
    }

    // Bạn có thể thêm các phương thức khác ở đây, ví dụ:
    // public void deleteUser(String userId, OnTaskCompleteListener listener) { ... }
    // public void addUser(User user, OnTaskCompleteListener listener) { ... }
    // public void updateUser(User user, OnTaskCompleteListener listener) { ... }
}