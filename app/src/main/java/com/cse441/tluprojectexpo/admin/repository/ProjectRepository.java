
package com.cse441.tluprojectexpo.admin.repository;

import android.util.Log;
import androidx.annotation.NonNull;
import androidx.lifecycle.MutableLiveData;

import com.cse441.tluprojectexpo.model.Category;
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
     * PHƯƠNG THỨC CHO TRANG ADMIN HOMEPAGE (LẤY TẤT CẢ)
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
                    // --- STAGE 2: Xử lý dữ liệu thô vào các Map ---
                    QuerySnapshot projectsSnapshot = (QuerySnapshot) results.get(0);
                    // Tạo danh sách Project và gán ID ngay lập tức
                    Log.d(TAG, "Tổng số document dự án lấy về: " + projectsSnapshot.size());
                    List<Project> projectList = new ArrayList<>();
                    for (DocumentSnapshot doc : projectsSnapshot.getDocuments()) {
                        Project p = doc.toObject(Project.class);
                        if (p != null) {
                            p.setProjectId(doc.getId()); // <-- SỬA LỖI QUAN TRỌNG NHẤT
                            projectList.add(p);
                            // Log để xác nhận project được thêm vào
                            Log.d(TAG, "Chuyển đổi THÀNH CÔNG project ID: " + p.getProjectId() + " | Tên: " + p.getTitle());
                        }else {
                            // Đây là log quan trọng nhất, nó sẽ cho bạn biết document nào bị lỗi
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

                    // --- STAGE 3: Kết hợp dữ liệu vào danh sách Project ---
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

                    // --- STAGE 4: Sắp xếp danh sách ---
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
     * PHƯƠNG THỨC CHO TRANG FEATURED MANAGEMENT PAGE
     */
    public MutableLiveData<List<FeaturedProjectUIModel>> getFeaturedProjectsForManagement() {
        // Logic này cần được cập nhật tương tự nếu bạn dùng nó
        MutableLiveData<List<FeaturedProjectUIModel>> liveData = new MutableLiveData<>();
        // Tạm thời trả về rỗng
        liveData.setValue(new ArrayList<>());
        return liveData;
    }

    /**
     * PHƯƠNG THỨC CẬP NHẬT TRẠNG THÁI NỔI BẬT
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