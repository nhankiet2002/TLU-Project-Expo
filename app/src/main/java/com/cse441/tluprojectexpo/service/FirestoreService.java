package com.cse441.tluprojectexpo.service;


// Import model Comment, Project, User của bạn

import com.cse441.tluprojectexpo.model.User;
import com.cse441.tluprojectexpo.utils.Constants;

import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class FirestoreService {

    private static final String TAG = "FirestoreService";
    private FirebaseFirestore db;

    public FirestoreService() {
        db = FirebaseFirestore.getInstance();
    }

    // --- INTERFACES CHO CreateProjectActivity (NẾU CÓ VÀ CẦN GIỮ LẠI) ---
    public interface CategoriesFetchListener {
        void onCategoriesFetched(List<String> categoryNames, Map<String, String> nameToIdMap);
        void onError(String errorMessage);
    }

    public interface UserDetailsFetchListener { // Dùng chung
        void onUserDetailsFetched(User user);
        void onUserNotFound();
        void onError(String errorMessage);
    }

    public interface TechnologyFetchListener {
        void onTechnologiesFetched(List<String> technologyNames, Map<String, String> technologyNameToIdMap);
        void onError(String errorMessage);
    }
    // --- KẾT THÚC INTERFACES CHO CreateProjectActivity ---


    // --- PHƯƠNG THỨC CHO CreateProjectActivity (NẾU CÓ VÀ CẦN GIỮ LẠI) ---
    public void fetchCategories(CategoriesFetchListener listener) {
        // (Giữ lại code gốc của bạn cho phương thức này nếu CreateProjectActivity đang dùng)
        // Ví dụ:
        if (listener == null) return;
        db.collection(Constants.COLLECTION_CATEGORIES).orderBy(Constants.FIELD_NAME).get()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful() && task.getResult() != null) {
                        List<String> names = new ArrayList<>();
                        Map<String, String> nameToId = new HashMap<>();
                        for (QueryDocumentSnapshot document : task.getResult()) {
                            String name = document.getString(Constants.FIELD_NAME);
                            String id = document.getId();
                            if (name != null && !name.isEmpty() && id != null) {
                                names.add(name);
                                nameToId.put(name, id);
                            }
                        }
                        listener.onCategoriesFetched(names, nameToId);
                    }
                });
    }

    public void fetchUserDetails(String userId, UserDetailsFetchListener listener) { // Dùng chung
        // (Giữ lại code gốc của bạn cho phương thức này)
        // Ví dụ:
        if (userId == null || userId.isEmpty()) { if (listener != null) listener.onError("User ID không hợp lệ."); return; }
        if (listener == null) return;
        db.collection(Constants.COLLECTION_USERS).document(userId).get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) {
                        User user = documentSnapshot.toObject(User.class);
                        if (user != null) {
                            user.setUserId(documentSnapshot.getId());
                            listener.onUserDetailsFetched(user);
                        } else {
                            listener.onError("Lỗi phân tích dữ liệu người dùng.");
                        }
                    } else {
                        listener.onUserNotFound();
                    }
                })
                .addOnFailureListener(e -> listener.onError("Lỗi tải thông tin người dùng: " + e.getMessage()));
    }

    public void fetchTechnologies(TechnologyFetchListener listener) {
        // (Giữ lại code gốc của bạn cho phương thức này nếu CreateProjectActivity đang dùng)
        if (listener == null) return;
        db.collection(Constants.COLLECTION_TECHNOLOGIES).orderBy(Constants.FIELD_NAME).get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    List<String> names = new ArrayList<>();
                    Map<String, String> nameToIdMap = new HashMap<>();
                    for (QueryDocumentSnapshot document : queryDocumentSnapshots) {
                        String name = document.getString(Constants.FIELD_NAME);
                        String id = document.getId();
                        if (name != null && !name.isEmpty() && id != null) {
                            names.add(name);
                            nameToIdMap.put(name, id);
                        }
                    }
                    listener.onTechnologiesFetched(names, nameToIdMap);
                })
                .addOnFailureListener(e -> listener.onError("Lỗi tải công nghệ: " + e.getMessage()));
    }
    // --- KẾT THÚC PHƯƠNG THỨC CHO CreateProjectActivity ---

}