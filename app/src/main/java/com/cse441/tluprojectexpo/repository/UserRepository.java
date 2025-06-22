package com.cse441.tluprojectexpo.repository;


import android.util.Log;
import androidx.annotation.NonNull;

import com.cse441.tluprojectexpo.model.User;
import com.cse441.tluprojectexpo.utils.Constants;
import com.google.firebase.firestore.FirebaseFirestore;

public class UserRepository {
    private static final String TAG = "UserRepository";
    private FirebaseFirestore db;

    public interface UserDetailsFetchListener {
        void onUserDetailsFetched(User user);
        void onUserNotFound();
        void onError(String errorMessage);
    }

    public UserRepository() {
        this.db = FirebaseFirestore.getInstance();
    }

    public void fetchUserDetails(String userId, @NonNull UserDetailsFetchListener listener) {
        if (userId == null || userId.isEmpty()) {
            listener.onError("User ID không hợp lệ.");
            return;
        }
        db.collection(Constants.COLLECTION_USERS).document(userId).get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) {
                        User user = documentSnapshot.toObject(User.class);
                        if (user != null) {
                            user.setUserId(documentSnapshot.getId()); // Gán ID cho đối tượng
                            listener.onUserDetailsFetched(user);
                        } else {
                            listener.onError("Lỗi phân tích dữ liệu người dùng.");
                        }
                    } else {
                        listener.onUserNotFound();
                    }
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Lỗi tải thông tin người dùng: " + userId, e);
                    listener.onError("Lỗi tải thông tin người dùng: " + e.getMessage());
                });
    }
}
