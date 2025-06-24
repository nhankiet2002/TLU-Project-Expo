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
import com.google.firebase.firestore.QuerySnapshot;
import com.google.firebase.firestore.WriteBatch;
import com.google.android.gms.tasks.Task;
import com.google.android.gms.tasks.Tasks;
import com.google.firebase.firestore.FieldValue;

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

    public interface ProjectUpdateListener {
        void onProjectUpdated();
        void onError(String errorMessage);
    }

    public interface ProjectCreationListener {
        void onProjectCreated(String newProjectId);
        void onError(String errorMessage);
    }

    public interface UpdateCallback {
        void onSuccess();
        void onError(String errorMessage);
    }
    public interface NotificationUpdateListener { // Có thể dùng chung UpdateCallback
        void onSuccess();
        void onError(String errorMessage);
    }
    // --- KẾT THÚC INTERFACES CHO EditProjectActivity ---

    public void updateNotificationReadStatus(String notificationId, boolean newReadStatus, NotificationUpdateListener listener) {
        if (notificationId == null || notificationId.isEmpty()) {
            if (listener != null) listener.onError("Notification ID không hợp lệ.");
            return;
        }
        if (listener == null) return;

        db.collection(Constants.COLLECTION_NOTIFICATIONS) // Giả sử bạn có Constants.COLLECTION_NOTIFICATIONS
                .document(notificationId)
                .update("isRead", newReadStatus) // "isRead" là tên trường trong Firestore
                .addOnSuccessListener(aVoid -> listener.onSuccess())
                .addOnFailureListener(e -> listener.onError("Lỗi cập nhật trạng thái thông báo: " + e.getMessage()));
    }

    public void deleteNotification(String notificationId, NotificationUpdateListener listener) {
        if (notificationId == null || notificationId.isEmpty()) {
            if (listener != null) listener.onError("Notification ID không hợp lệ.");
            return;
        }
        if (listener == null) return;

        db.collection(Constants.COLLECTION_NOTIFICATIONS) // Giả sử bạn có Constants.COLLECTION_NOTIFICATIONS
                .document(notificationId)
                .delete()
                .addOnSuccessListener(aVoid -> listener.onSuccess())
                .addOnFailureListener(e -> listener.onError("Lỗi xóa thông báo: " + e.getMessage()));
    }
    // --- PHƯƠNG THỨC CHO CreateProjectActivity (NẾU CÓ VÀ CẦN GIỮ LẠI) ---
    public void fetchCategories(CategoriesFetchListener listener) {
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
                    } else {
                        listener.onError(task.getException() != null ? task.getException().getMessage() : "Unknown error");
                    }
                });
    }

    public void fetchUserDetails(String userId, UserDetailsFetchListener listener) { // Dùng chung
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
                        User user = new User();
                        user.setUserId(document.getId());
                        user.setFullName(document.getString("FullName"));
                        user.setAvatarUrl(document.getString("AvatarUrl"));
                        user.setClassName(document.getString("ClassName"));
                        members.add(user);
                        userRoles.put(document.getId(), document.getString("RoleInProject"));
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

    public void createProject(Map<String, Object> projectData, ProjectCreationListener listener) {
        if (projectData == null || listener == null) return;
        db.collection(Constants.COLLECTION_PROJECTS)
                .add(projectData)
                .addOnSuccessListener(documentReference -> listener.onProjectCreated(documentReference.getId()))
                .addOnFailureListener(e -> listener.onError("Lỗi khi tạo dự án: " + e.getMessage()));
    }

    public void updateProject(String projectId, Map<String, Object> projectUpdates, ProjectUpdateListener listener) {
        if (projectId == null || projectId.isEmpty()) {
            if (listener != null) listener.onError("Project ID không hợp lệ.");
            return;
        }
        db.collection(Constants.COLLECTION_PROJECTS).document(projectId)
                .update(projectUpdates)
                .addOnSuccessListener(aVoid -> {
                    if (listener != null) listener.onProjectUpdated();
                })
                .addOnFailureListener(e -> {
                    if (listener != null) listener.onError("Lỗi cập nhật dự án: " + e.getMessage());
                });
    }

    public void updateProjectCategories(String projectId, List<String> newCategoryIds, UpdateCallback callback) {
        updateRelations(projectId, "ProjectCategories", "CategoryId", newCategoryIds, callback);
    }

    public void updateProjectTechnologies(String projectId, List<String> newTechnologyIds, UpdateCallback callback) {
        updateRelations(projectId, "ProjectTechnologies", "TechnologyId", newTechnologyIds, callback);
    }

    public void updateProjectMembers(String projectId, List<User> members, Map<String, String> userRoles, UpdateCallback callback) {
        WriteBatch batch = db.batch();
        db.collection("ProjectMembers").whereEqualTo("ProjectId", projectId).get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    for (DocumentSnapshot doc : queryDocumentSnapshots.getDocuments()) {
                        batch.delete(doc.getReference());
                    }

                    for (User user : members) {
                        if (user.getUserId() == null) continue;
                        Map<String, Object> memberRelationData = new HashMap<>();
                        memberRelationData.put("ProjectId", projectId);
                        memberRelationData.put("UserId", user.getUserId());
                        memberRelationData.put("RoleInProject", userRoles.get(user.getUserId()));
                        
                        batch.set(db.collection("ProjectMembers").document(), memberRelationData);
                    }

                    batch.commit()
                         .addOnSuccessListener(aVoid -> callback.onSuccess())
                         .addOnFailureListener(e -> callback.onError(e.getMessage()));

                }).addOnFailureListener(e -> {
                    callback.onError("Failed to query existing project members: " + e.getMessage());
                });
    }

    private void updateRelations(String projectId, String relationCollectionName, String relationFieldName, List<String> newRelationIds, UpdateCallback callback) {
        WriteBatch batch = db.batch();

        db.collection(relationCollectionName).whereEqualTo("ProjectId", projectId).get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    for (DocumentSnapshot doc : queryDocumentSnapshots.getDocuments()) {
                        batch.delete(doc.getReference());
                    }

                    for (String relationId : newRelationIds) {
                        Map<String, Object> relationData = new HashMap<>();
                        relationData.put("ProjectId", projectId);
                        relationData.put(relationFieldName, relationId);
                        
                        batch.set(db.collection(relationCollectionName).document(), relationData);
                    }

                    batch.commit()
                         .addOnSuccessListener(aVoid -> callback.onSuccess())
                         .addOnFailureListener(e -> callback.onError(e.getMessage()));

                }).addOnFailureListener(e -> {
                    callback.onError("Failed to query existing project relations in " + relationCollectionName + ": " + e.getMessage());
                });
    }
}