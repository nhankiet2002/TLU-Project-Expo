package com.cse441.tluprojectexpo.admin.repository;

import android.util.Log;
import androidx.annotation.NonNull;
import androidx.lifecycle.MutableLiveData;

import com.cse441.tluprojectexpo.model.FeaturedProjectUIModel;
import com.cse441.tluprojectexpo.model.Project;
import com.cse441.tluprojectexpo.model.User;
import com.google.android.gms.tasks.Task;
import com.google.android.gms.tasks.Tasks;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QuerySnapshot;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

public class ProjectRepository {
    private static final String TAG = "ProjectRepository";
    private final FirebaseFirestore db = FirebaseFirestore.getInstance();

    /**
     * PHƯƠNG THỨC CHO TRANG ADMIN HOMEPAGE (LẤY TẤT CẢ) - GIỮ NGUYÊN
     */
    public MutableLiveData<List<Project>> getAllProjectsForAdmin() {
        MutableLiveData<List<Project>> liveData = new MutableLiveData<>();

        Task<QuerySnapshot> projectsTask = db.collection("Projects").get();
        Task<QuerySnapshot> usersTask = db.collection("Users").get();
        Task<QuerySnapshot> categoriesTask = db.collection("Categories").get();
        Task<QuerySnapshot> technologiesTask = db.collection("Technologies").get();
        Task<QuerySnapshot> projectCategoriesTask = db.collection("ProjectCategories").get();
        Task<QuerySnapshot> projectTechnologiesTask = db.collection("ProjectTechnologies").get();

        Tasks.whenAllSuccess(projectsTask, usersTask, categoriesTask, technologiesTask,
                        projectCategoriesTask, projectTechnologiesTask)
                .addOnSuccessListener(results -> {
                    QuerySnapshot projectsSnapshot = (QuerySnapshot) results.get(0);
                    Log.d(TAG, "Tổng số document dự án lấy về: " + projectsSnapshot.size());
                    List<Project> projectList = new ArrayList<>();
                    for (DocumentSnapshot doc : projectsSnapshot.getDocuments()) {
                        Project p = doc.toObject(Project.class);
                        if (p != null) {
                            p.setProjectId(doc.getId());
                            projectList.add(p);
                            Log.d(TAG, "Chuyển đổi THÀNH CÔNG project ID: " + p.getProjectId() + " | Tên: " + p.getTitle());
                        } else {
                            Log.e(TAG, "Chuyển đổi THẤT BẠI cho document ID: " + doc.getId());
                        }
                    }

                    Map<String, User> userMap = ((QuerySnapshot) results.get(1)).getDocuments().stream().collect(Collectors.toMap(DocumentSnapshot::getId, doc -> doc.toObject(User.class)));
                    Map<String, String> categoryMap = ((QuerySnapshot) results.get(2)).getDocuments().stream().collect(Collectors.toMap(DocumentSnapshot::getId, doc -> doc.getString("Name")));
                    Map<String, String> technologyMap = ((QuerySnapshot) results.get(3)).getDocuments().stream().collect(Collectors.toMap(DocumentSnapshot::getId, doc -> doc.getString("Name")));
                    Map<String, String> projectToCategoryMap = ((QuerySnapshot) results.get(4)).getDocuments().stream().collect(Collectors.toMap(doc -> doc.getString("ProjectId"), doc -> doc.getString("CategoryId"), (v1, v2) -> v1));
                    Map<String, List<String>> projectToTechsMap = new HashMap<>();
                    for (DocumentSnapshot doc : ((QuerySnapshot) results.get(5)).getDocuments()) {
                        projectToTechsMap.computeIfAbsent(doc.getString("ProjectId"), k -> new ArrayList<>()).add(doc.getString("TechnologyId"));
                    }

                    for (Project project : projectList) {
                        User creator = userMap.get(project.getCreatorUserId());
                        if (creator != null) project.setCreatorFullName(creator.getFullName());

                        String categoryId = projectToCategoryMap.get(project.getProjectId());
                        if (categoryId != null) {
                            String categoryName = categoryMap.get(categoryId);
                            project.setCategoryNames(new ArrayList<>(List.of(categoryName != null ? categoryName : "")));
                        }

                        List<String> techIds = projectToTechsMap.get(project.getProjectId());
                        if (techIds != null) {
                            List<String> techNames = techIds.stream().map(technologyMap::get).filter(Objects::nonNull).collect(Collectors.toList());
                            project.setTechnologyNames(techNames);
                        }
                    }

                    Collections.sort(projectList, (p1, p2) -> {
                        if (p1.isFeatured() && !p2.isFeatured()) return -1;
                        if (!p1.isFeatured() && p2.isFeatured()) return 1;
                        if (p1.getCreatedAt() != null && p2.getCreatedAt() != null) {
                            return p2.getCreatedAt().compareTo(p1.getCreatedAt());
                        }
                        return 0;
                    });
                    liveData.setValue(projectList);
                }).addOnFailureListener(e -> {
                    Log.e(TAG, "Lỗi khi lấy dữ liệu cho AdminHomePage: ", e);
                    liveData.setValue(null);
                });
        return liveData;
    }

    /**
     * PHƯƠNG THỨC CHO TRANG FEATURED MANAGEMENT PAGE - SỬA LẠI HOÀN CHỈNH
     */
    public MutableLiveData<List<FeaturedProjectUIModel>> getFeaturedProjectsForManagement() {
        MutableLiveData<List<FeaturedProjectUIModel>> liveData = new MutableLiveData<>();

        // 1. Chỉ lấy các project có IsFeatured = true
        Task<QuerySnapshot> featuredProjectsTask = db.collection("Projects").whereEqualTo("IsFeatured", true).get();
        // Vẫn cần lấy users và categories để lấy tên
        Task<QuerySnapshot> usersTask = db.collection("Users").get();
        Task<QuerySnapshot> categoriesTask = db.collection("Categories").get();
        Task<QuerySnapshot> projectCategoriesTask = db.collection("ProjectCategories").get();

        Tasks.whenAllSuccess(featuredProjectsTask, usersTask, categoriesTask, projectCategoriesTask)
                .addOnSuccessListener(results -> {
                    // 2. Lấy dữ liệu thô
                    QuerySnapshot projectsSnapshot = (QuerySnapshot) results.get(0);
                    Map<String, User> userMap = ((QuerySnapshot) results.get(1)).getDocuments().stream().collect(Collectors.toMap(DocumentSnapshot::getId, doc -> doc.toObject(User.class)));
                    Map<String, String> categoryMap = ((QuerySnapshot) results.get(2)).getDocuments().stream().collect(Collectors.toMap(DocumentSnapshot::getId, doc -> doc.getString("Name")));
                    Map<String, String> projectToCategoryMap = ((QuerySnapshot) results.get(3)).getDocuments().stream().collect(Collectors.toMap(doc -> doc.getString("ProjectId"), doc -> doc.getString("CategoryId"), (v1, v2) -> v1));

                    // 3. Chuyển đổi và kết hợp dữ liệu
                    List<FeaturedProjectUIModel> uiModelList = new ArrayList<>();
                    for (DocumentSnapshot doc : projectsSnapshot.getDocuments()) {
                        Project project = doc.toObject(Project.class);
                        if (project != null) {
                            project.setProjectId(doc.getId());

                            // Lấy tên người tạo
                            User creator = userMap.get(project.getCreatorUserId());
                            String authorName = (creator != null) ? creator.getFullName() : "Không xác định";

                            // Lấy tên lĩnh vực
                            String categoryId = projectToCategoryMap.get(project.getProjectId());
                            String categoryName = (categoryId != null) ? categoryMap.get(categoryId) : "Chưa có";
                            if (categoryName == null) categoryName = "Chưa có";

                            // Tạo UI model để hiển thị
                            FeaturedProjectUIModel uiModel = new FeaturedProjectUIModel(
                                    project.getProjectId(),
                                    project.getTitle(),
                                    authorName,
                                    categoryName, // Truyền tên lĩnh vực
                                    project.getThumbnailUrl(), // Thêm thumbnail
                                    project.getVoteCount() // Thêm vote count
                            );
                            uiModelList.add(uiModel);
                        }
                    }
                    liveData.setValue(uiModelList);
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Lỗi khi lấy dự án nổi bật: ", e);
                    liveData.setValue(null);
                });

        return liveData;
    }


    /**
     * PHƯƠNG THỨC CẬP NHẬT TRẠNG THÁI NỔI BẬT - GIỮ NGUYÊN
     */
    public void setProjectFeaturedStatus(String projectId, boolean isFeatured, @NonNull OnTaskCompleteListener listener) {
        if (projectId == null || projectId.isEmpty()) {
            listener.onFailure(new IllegalArgumentException("Project ID không hợp lệ"));
            return;
        }
        db.collection("Projects").document(projectId)
                .update("IsFeatured", isFeatured)
                .addOnSuccessListener(aVoid -> listener.onSuccess())
                .addOnFailureListener(listener::onFailure);
    }

    public interface OnTaskCompleteListener {
        void onSuccess();
        void onFailure(Exception e);
    }
}