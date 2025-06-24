package com.cse441.tluprojectexpo.service;

// Import model Comment, Project, User của bạn
import com.cse441.tluprojectexpo.model.Comment;
import com.cse441.tluprojectexpo.model.Project;
import com.cse441.tluprojectexpo.model.User;
import com.cse441.tluprojectexpo.model.LinkItem;
import com.cse441.tluprojectexpo.utils.Constants;

import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.DocumentSnapshot;

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

    // --- INTERFACES CHO EditProjectActivity ---
    public interface ProjectFetchListener {
        void onProjectFetched(Project project);
        void onError(String errorMessage);
    }

    public interface ProjectMembersFetchListener {
        void onMembersFetched(List<User> members, Map<String, String> userRoles);
        void onError(String errorMessage);
    }

    public interface ProjectLinksFetchListener {
        void onLinksFetched(List<LinkItem> links);
        void onError(String errorMessage);
    }

    public interface ProjectMediaFetchListener {
        void onMediaFetched(List<Map<String, String>> mediaList);
        void onError(String errorMessage);
    }
    // --- KẾT THÚC INTERFACES CHO EditProjectActivity ---

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

    // --- PHƯƠNG THỨC CHO EditProjectActivity ---
    public void getProjectById(String projectId, ProjectFetchListener listener) {
        if (projectId == null || projectId.isEmpty()) {
            if (listener != null) listener.onError("Project ID không hợp lệ.");
            return;
        }
        if (listener == null) return;

        db.collection(Constants.COLLECTION_PROJECTS).document(projectId).get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) {
                        Project project = documentSnapshot.toObject(Project.class);
                        if (project != null) {
                            project.setProjectId(documentSnapshot.getId());
                            listener.onProjectFetched(project);
                        } else {
                            listener.onError("Lỗi phân tích dữ liệu dự án.");
                        }
                    } else {
                        listener.onError("Dự án không tồn tại.");
                    }
                })
                .addOnFailureListener(e -> listener.onError("Lỗi tải dự án: " + e.getMessage()));
    }

    public void getProjectMembers(String projectId, ProjectMembersFetchListener listener) {
        if (projectId == null || projectId.isEmpty()) {
            if (listener != null) listener.onError("Project ID không hợp lệ.");
            return;
        }
        if (listener == null) return;

        db.collection(Constants.COLLECTION_PROJECTS).document(projectId)
                .collection("members").get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    List<User> members = new ArrayList<>();
                    Map<String, String> userRoles = new HashMap<>();
                    
                    for (QueryDocumentSnapshot document : queryDocumentSnapshots) {
                        String userId = document.getString("userId");
                        String role = document.getString("role");
                        String displayName = document.getString("displayName");
                        String email = document.getString("email");
                        
                        if (userId != null) {
                            User user = new User();
                            user.setUserId(userId);
                            user.setFullName(displayName);
                            user.setEmail(email);
                            members.add(user);
                            userRoles.put(userId, role != null ? role : "Thành viên");
                        }
                    }
                    
                    listener.onMembersFetched(members, userRoles);
                })
                .addOnFailureListener(e -> listener.onError("Lỗi tải thành viên dự án: " + e.getMessage()));
    }

    public void getProjectLinks(String projectId, ProjectLinksFetchListener listener) {
        if (projectId == null || projectId.isEmpty()) {
            if (listener != null) listener.onError("Project ID không hợp lệ.");
            return;
        }
        if (listener == null) return;

        db.collection(Constants.COLLECTION_PROJECTS).document(projectId)
                .collection("links").get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    List<LinkItem> links = new ArrayList<>();
                    
                    for (QueryDocumentSnapshot document : queryDocumentSnapshots) {
                        String platform = document.getString("platform");
                        String url = document.getString("url");
                        
                        if (platform != null && url != null) {
                            LinkItem link = new LinkItem(platform, url);
                            links.add(link);
                        }
                    }
                    
                    listener.onLinksFetched(links);
                })
                .addOnFailureListener(e -> listener.onError("Lỗi tải liên kết dự án: " + e.getMessage()));
    }

    public void getProjectMedia(String projectId, ProjectMediaFetchListener listener) {
        if (projectId == null || projectId.isEmpty()) {
            if (listener != null) listener.onError("Project ID không hợp lệ.");
            return;
        }
        if (listener == null) return;

        db.collection(Constants.COLLECTION_PROJECTS).document(projectId)
                .collection("media").get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    List<Map<String, String>> mediaList = new ArrayList<>();
                    
                    for (QueryDocumentSnapshot document : queryDocumentSnapshots) {
                        String url = document.getString("url");
                        String resourceType = document.getString("resourceType");
                        
                        if (url != null) {
                            Map<String, String> mediaItem = new HashMap<>();
                            mediaItem.put("url", url);
                            mediaItem.put("resourceType", resourceType != null ? resourceType : "image");
                            mediaList.add(mediaItem);
                        }
                    }
                    
                    listener.onMediaFetched(mediaList);
                })
                .addOnFailureListener(e -> listener.onError("Lỗi tải media dự án: " + e.getMessage()));
    }
    // --- KẾT THÚC PHƯƠNG THỨC CHO EditProjectActivity ---
}