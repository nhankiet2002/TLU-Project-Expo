package com.cse441.tluprojectexpo.Project.util;


import android.util.Log;

import com.cse441.tluprojectexpo.model.User; // Thêm import User
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.QuerySnapshot;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Dịch vụ tập trung các hàm fetch dữ liệu từ Firestore.
 */
public class FirestoreFetchService {

    private static final String TAG = "FirestoreFetchSvc"; // Thay đổi TAG
    private FirebaseFirestore db;

    public FirestoreFetchService() {
        db = FirebaseFirestore.getInstance();
    }

    /**
     * Listener cho kết quả fetch danh sách categories.
     */
    public interface CategoriesFetchListener {
        void onCategoriesFetched(List<String> categoryNames, Map<String, String> nameToIdMap);
        void onError(String errorMessage);
    }

    /**
     * Listener cho kết quả fetch chi tiết một User.
     */
    public interface UserDetailsFetchListener {
        void onUserDetailsFetched(User user);
        void onUserNotFound();
        void onError(String errorMessage);
    }


    /**
     * Lấy danh sách tên các Lĩnh vực (Categories) từ Firestore.
     * Sắp xếp theo trường "Name".
     * @param listener Callback để nhận kết quả.
     */
    public void fetchCategories(CategoriesFetchListener listener) {
        if (listener == null) {
            Log.e(TAG, "CategoriesFetchListener cannot be null.");
            return;
        }
        db.collection("Categories").orderBy("Name").get()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        QuerySnapshot querySnapshot = task.getResult();
                        List<String> names = new ArrayList<>();
                        Map<String, String> nameToId = new HashMap<>();
                        if (querySnapshot != null && !querySnapshot.isEmpty()) {
                            for (QueryDocumentSnapshot document : querySnapshot) {
                                String name = document.getString("Name");
                                String id = document.getId();
                                if (name != null && !name.isEmpty() && id != null) {
                                    names.add(name);
                                    nameToId.put(name, id);
                                }
                            }
                        }
                        Log.d(TAG, "Categories fetched: " + names.size());
                        listener.onCategoriesFetched(names, nameToId);
                    } else {
                        Log.w(TAG, "Error fetching categories: ", task.getException());
                        listener.onError("Lỗi tải lĩnh vực: " + (task.getException() != null ? task.getException().getMessage() : "Lỗi không xác định"));
                    }
                });
    }

    /**
     * Lấy chi tiết thông tin của một User từ Firestore bằng UserId.
     * @param userId ID của User cần lấy.
     * @param listener Callback để nhận kết quả.
     */
    public void fetchUserDetails(String userId, UserDetailsFetchListener listener) {
        if (userId == null || userId.isEmpty()) {
            if (listener != null) listener.onError("User ID không hợp lệ.");
            return;
        }
        if (listener == null) {
            Log.e(TAG, "UserDetailsFetchListener cannot be null.");
            return;
        }

        db.collection("Users").document(userId).get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) {
                        User user = documentSnapshot.toObject(User.class);
                        if (user != null) {
                            user.setUserId(documentSnapshot.getId()); // Gán ID từ document
                            Log.d(TAG, "User details fetched for: " + userId);
                            listener.onUserDetailsFetched(user);
                        } else {
                            Log.e(TAG, "Failed to parse user object for: " + userId);
                            listener.onError("Lỗi phân tích dữ liệu người dùng.");
                        }
                    } else {
                        Log.w(TAG, "User document not found for ID: " + userId);
                        listener.onUserNotFound();
                    }
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Error fetching user details for ID: " + userId, e);
                    listener.onError("Lỗi tải thông tin người dùng: " + e.getMessage());
                });
    }

    // Bạn có thể thêm các hàm fetch khác ở đây (ví dụ: fetch ProjectStatuses nếu không hardcode)
}
