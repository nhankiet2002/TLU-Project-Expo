package com.cse441.tluprojectexpo.ui.detailproject.repository;


import android.util.Log;
import androidx.annotation.NonNull;

import com.cse441.tluprojectexpo.model.Project;
import com.cse441.tluprojectexpo.model.User; // Cần cho UserShortInfo
import com.cse441.tluprojectexpo.utils.Constants;
import com.google.android.gms.tasks.Task;
import com.google.android.gms.tasks.Tasks;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.ArrayList;
import java.util.List;

public class ProjectRepository {
    private static final String TAG = "ProjectRepository";
    private FirebaseFirestore db;

    public interface ProjectDetailsListener {
        void onProjectFetched(Project project);
        void onProjectNotFound();
        void onError(String errorMessage);
    }

    public interface ProjectRelatedListListener<T> {
        void onListFetched(List<T> items);
        void onListEmpty();
        void onError(String errorMessage);
    }

    public ProjectRepository() {
        this.db = FirebaseFirestore.getInstance();
    }

    public void fetchProjectDetails(String projectId, @NonNull ProjectDetailsListener listener) {
        if (projectId == null || projectId.isEmpty()) {
            listener.onError("Project ID không hợp lệ.");
            return;
        }
        db.collection(Constants.COLLECTION_PROJECTS).document(projectId).get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) {
                        Project project = documentSnapshot.toObject(Project.class);
                        if (project != null) {
                            project.setProjectId(documentSnapshot.getId());
                            listener.onProjectFetched(project);
                        } else {
                            listener.onError("Lỗi khi đọc dữ liệu dự án.");
                        }
                    } else {
                        listener.onProjectNotFound();
                    }
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Lỗi tải dự án: " + projectId, e);
                    listener.onError("Lỗi tải dự án: " + e.getMessage());
                });
    }

    public void fetchCategoriesForProject(String projectId, @NonNull ProjectRelatedListListener<String> listener) {
        db.collection(Constants.COLLECTION_PROJECT_CATEGORIES)
                .whereEqualTo(Constants.FIELD_PROJECT_ID, projectId)
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    if (queryDocumentSnapshots.isEmpty()) {
                        listener.onListEmpty(); return;
                    }
                    List<Task<DocumentSnapshot>> categoryTasks = new ArrayList<>();
                    for (QueryDocumentSnapshot doc : queryDocumentSnapshots) {
                        String categoryId = doc.getString(Constants.FIELD_CATEGORY_ID);
                        if (categoryId != null) {
                            categoryTasks.add(db.collection(Constants.COLLECTION_CATEGORIES).document(categoryId).get());
                        }
                    }
                    if (categoryTasks.isEmpty()) { listener.onListEmpty(); return; }
                    Tasks.whenAllSuccess(categoryTasks).addOnSuccessListener(list -> {
                        List<String> categoryNames = new ArrayList<>();
                        for (Object snapshot : list) {
                            DocumentSnapshot catSnap = (DocumentSnapshot) snapshot;
                            if (catSnap.exists() && catSnap.getString(Constants.FIELD_NAME) != null) {
                                categoryNames.add(catSnap.getString(Constants.FIELD_NAME));
                            }
                        }
                        listener.onListFetched(categoryNames);
                    }).addOnFailureListener(e -> listener.onError("Lỗi lấy chi tiết lĩnh vực: " + e.getMessage()));
                }).addOnFailureListener(e -> {
                    Log.e(TAG, "Lỗi tải lĩnh vực của dự án: " + projectId, e);
                    listener.onError("Lỗi tải lĩnh vực của dự án: " + e.getMessage());
                });
    }

    public void fetchTechnologiesForProject(String projectId, @NonNull ProjectRelatedListListener<String> listener) {
        db.collection(Constants.COLLECTION_PROJECT_TECHNOLOGIES)
                .whereEqualTo(Constants.FIELD_PROJECT_ID, projectId)
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    if (queryDocumentSnapshots.isEmpty()) {
                        listener.onListEmpty(); return;
                    }
                    List<Task<DocumentSnapshot>> techTasks = new ArrayList<>();
                    for (QueryDocumentSnapshot doc : queryDocumentSnapshots) {
                        String technologyId = doc.getString(Constants.FIELD_TECHNOLOGY_ID);
                        if (technologyId != null) {
                            techTasks.add(db.collection(Constants.COLLECTION_TECHNOLOGIES).document(technologyId).get());
                        }
                    }
                    if (techTasks.isEmpty()) { listener.onListEmpty(); return; }
                    Tasks.whenAllSuccess(techTasks).addOnSuccessListener(list -> {
                        List<String> techNames = new ArrayList<>();
                        for (Object snapshot : list) {
                            DocumentSnapshot techSnap = (DocumentSnapshot) snapshot;
                            if (techSnap.exists() && techSnap.getString(Constants.FIELD_NAME) != null) {
                                techNames.add(techSnap.getString(Constants.FIELD_NAME));
                            }
                        }
                        listener.onListFetched(techNames);
                    }).addOnFailureListener(e -> listener.onError("Lỗi lấy chi tiết công nghệ: " + e.getMessage()));
                }).addOnFailureListener(e -> {
                    Log.e(TAG, "Lỗi tải công nghệ của dự án: " + projectId, e);
                    listener.onError("Lỗi tải công nghệ của dự án: " + e.getMessage());
                });
    }

    public void fetchProjectMembers(String projectId, @NonNull ProjectRelatedListListener<Project.UserShortInfo> listener) {
        db.collection(Constants.COLLECTION_PROJECT_MEMBERS)
                .whereEqualTo(Constants.FIELD_PROJECT_ID, projectId)
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    if (queryDocumentSnapshots.isEmpty()) {
                        listener.onListEmpty(); return;
                    }
                    List<Task<DocumentSnapshot>> userTasks = new ArrayList<>();
                    List<String> roles = new ArrayList<>();
                    for (QueryDocumentSnapshot doc : queryDocumentSnapshots) {
                        String userId = doc.getString(Constants.FIELD_USER_ID);
                        String role = doc.getString(Constants.FIELD_ROLE_IN_PROJECT);
                        if (userId != null) {
                            userTasks.add(db.collection(Constants.COLLECTION_USERS).document(userId).get());
                            roles.add(role != null ? role : Constants.DEFAULT_MEMBER_ROLE);
                        }
                    }
                    if (userTasks.isEmpty()) { listener.onListEmpty(); return; }
                    Tasks.whenAllSuccess(userTasks).addOnSuccessListener(list -> {
                        List<Project.UserShortInfo> memberInfos = new ArrayList<>();
                        for (int i = 0; i < list.size(); i++) {
                            DocumentSnapshot userSnap = (DocumentSnapshot) list.get(i);
                            if (userSnap.exists()) {
                                User user = userSnap.toObject(User.class);
                                if (user != null) {
                                    memberInfos.add(new Project.UserShortInfo(
                                            userSnap.getId(), user.getFullName(),
                                            user.getAvatarUrl(), roles.get(i)
                                    ));
                                }
                            }
                        }
                        listener.onListFetched(memberInfos);
                    }).addOnFailureListener(e -> listener.onError("Lỗi lấy chi tiết thành viên: " + e.getMessage()));
                }).addOnFailureListener(e -> {
                    Log.e(TAG, "Lỗi tải thành viên dự án: " + projectId, e);
                    listener.onError("Lỗi tải thành viên dự án: " + e.getMessage());
                });
    }
}
