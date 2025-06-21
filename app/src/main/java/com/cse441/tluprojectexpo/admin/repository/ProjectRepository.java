package com.cse441.tluprojectexpo.admin.repository;

import android.util.Log;
import androidx.lifecycle.MutableLiveData;

import com.cse441.tluprojectexpo.model.Category;
import com.cse441.tluprojectexpo.model.FeaturedProjectUIModel;
import com.cse441.tluprojectexpo.model.Project;
import com.cse441.tluprojectexpo.model.User;
import com.google.android.gms.tasks.Task;
import com.google.android.gms.tasks.Tasks;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FieldPath;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QuerySnapshot;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class ProjectRepository {
    private static final String TAG = "ProjectRepository";
    private final FirebaseFirestore db = FirebaseFirestore.getInstance();

    public MutableLiveData<List<FeaturedProjectUIModel>> getFeaturedProjectsWithDetails() {
        MutableLiveData<List<FeaturedProjectUIModel>> liveData = new MutableLiveData<>();

        // STAGE 1: Lấy danh sách ProjectID nổi bật
        db.collection("FeaturedProjects").get().addOnSuccessListener(featuredDocs -> {
            if (featuredDocs.isEmpty()) {
                Log.d(TAG, "STAGE 1: Không tìm thấy dự án nổi bật nào.");
                liveData.setValue(new ArrayList<>());
                return;
            }
            List<String> featuredProjectIds = featuredDocs.getDocuments().stream()
                    .map(doc -> doc.getString("ProjectId"))
                    .collect(Collectors.toList());
            Log.d(TAG, "STAGE 1: Tìm thấy " + featuredProjectIds.size() + " ID dự án nổi bật: " + featuredProjectIds);

            if (featuredProjectIds.isEmpty()) {
                liveData.setValue(new ArrayList<>());
                return;
            }

            // STAGE 2: Dùng danh sách ID để lấy thông tin từ Projects và ProjectCategories
            Task<QuerySnapshot> projectsTask = db.collection("Projects")
                    .whereIn(FieldPath.documentId(), featuredProjectIds)
                    .get();
            Task<QuerySnapshot> projectCategoriesTask = db.collection("ProjectCategories")
                    .whereIn("ProjectId", featuredProjectIds)
                    .get();

            Tasks.whenAllSuccess(projectsTask, projectCategoriesTask).onSuccessTask(results -> {
                QuerySnapshot projectsSnapshot = (QuerySnapshot) results.get(0);
                QuerySnapshot projectCategoriesSnapshot = (QuerySnapshot) results.get(1);
                Log.d(TAG, "STAGE 2: Lấy về " + projectsSnapshot.size() + " projects và " + projectCategoriesSnapshot.size() + " projectCategories.");

                Map<String, Project> projectMap = new HashMap<>();
                Map<String, String> projectToCategoryMap = new HashMap<>();
                List<String> creatorUserIds = new ArrayList<>();
                List<String> categoryIds = new ArrayList<>();

                for (DocumentSnapshot doc : projectsSnapshot.getDocuments()) {
                    Project p = doc.toObject(Project.class);
                    if (p != null) {
                        p.setProjectId(doc.getId());
                        projectMap.put(p.getProjectId(), p);
                        if (p.getCreatorUserId() != null) {
                            creatorUserIds.add(p.getCreatorUserId());
                        }
                    }
                }

                for (DocumentSnapshot doc : projectCategoriesSnapshot.getDocuments()) {
                    String projId = doc.getString("ProjectId");
                    String catId = doc.getString("CategoryId");
                    projectToCategoryMap.put(projId, catId);
                    if (catId != null) {
                        categoryIds.add(catId);
                    }
                }

                // STAGE 3: Lấy thông tin chi tiết của User và Category
                Task<QuerySnapshot> usersTask = creatorUserIds.isEmpty() ? Tasks.forResult(null) : db.collection("Users").whereIn(FieldPath.documentId(), creatorUserIds).get();
                Task<QuerySnapshot> categoriesTask = categoryIds.isEmpty() ? Tasks.forResult(null) : db.collection("Categories").whereIn(FieldPath.documentId(), categoryIds).get();

                return Tasks.whenAllSuccess(usersTask, categoriesTask).onSuccessTask(finalResults -> {
                    QuerySnapshot usersSnapshot = (QuerySnapshot) finalResults.get(0);
                    QuerySnapshot categoriesSnapshot = (QuerySnapshot) finalResults.get(1);

                    Map<String, User> userMap = new HashMap<>();
                    Map<String, Category> categoryMap = new HashMap<>();

                    if (usersSnapshot != null) {
                        Log.d(TAG, "STAGE 3: Lấy về " + usersSnapshot.size() + " users.");
                        for (DocumentSnapshot doc : usersSnapshot.getDocuments()) {
                            User u = doc.toObject(User.class);
                            if (u != null) {
                                u.setUserId(doc.getId());
                                userMap.put(u.getUserId(), u);
                            }
                        }
                    }
                    if (categoriesSnapshot != null) {
                        Log.d(TAG, "STAGE 3: Lấy về " + categoriesSnapshot.size() + " categories.");
                        for (DocumentSnapshot doc : categoriesSnapshot.getDocuments()) {
                            // Annotation @DocumentId sẽ tự động gán doc.getId() vào trường 'id'
                            Category c = doc.toObject(Category.class);
                            if (c != null) {
                                // SỬA Ở ĐÂY: Dùng getId() vì model Category dùng 'id'
                                categoryMap.put(c.getId(), c);
                            }
                        }
                    }

                    // STAGE 4: KẾT HỢP TẤT CẢ DỮ LIỆU
                    List<FeaturedProjectUIModel> finalUIList = new ArrayList<>();
                    for (String projectId : featuredProjectIds) {
                        Project project = projectMap.get(projectId);
                        if (project == null) {
                            Log.w(TAG, "STAGE 4: Bỏ qua vì không tìm thấy project chi tiết cho ID: " + projectId);
                            continue;
                        }

                        FeaturedProjectUIModel uiModel = new FeaturedProjectUIModel();
                        uiModel.setProjectId(project.getProjectId());
                        uiModel.setProjectTitle(project.getTitle());

                        User creator = userMap.get(project.getCreatorUserId());
                        uiModel.setCreatorName(creator != null ? "Bởi: " + creator.getFullName() : "Bởi: Người dùng ẩn danh");

                        String catId = projectToCategoryMap.get(projectId);
                        Category category = categoryMap.get(catId);
                        // SỬA Ở ĐÂY: Dùng getName() vì model Category dùng 'name'
                        uiModel.setCategoryName(category != null ? category.getName() : "Chưa phân loại");

                        finalUIList.add(uiModel);
                    }
                    Log.d(TAG, "STAGE 4: Hoàn thành! Tạo được " + finalUIList.size() + " UI Models.");
                    liveData.postValue(finalUIList);
                    return null;
                });
            }).addOnFailureListener(e -> {
                Log.e(TAG, "STAGE 2 FAILED: Lỗi khi lấy Projects/ProjectCategories", e);
                liveData.postValue(null);
            });
        }).addOnFailureListener(e -> {
            Log.e(TAG, "STAGE 1 FAILED: Lỗi khi lấy FeaturedProjects", e);
            liveData.setValue(null);
        });
        return liveData;
    }

    public void removeFeaturedProject(String projectId, OnTaskCompleteListener listener) {
        db.collection("FeaturedProjects").whereEqualTo("ProjectId", projectId).get()
                .addOnSuccessListener(querySnapshot -> {
                    if (!querySnapshot.isEmpty()) {
                        for (DocumentSnapshot doc : querySnapshot.getDocuments()) {
                            doc.getReference().delete();
                        }
                        listener.onSuccess();
                    } else {
                        listener.onSuccess(); // Vẫn thành công nếu không tìm thấy
                    }
                })
                .addOnFailureListener(listener::onFailure);
    }

    public interface OnTaskCompleteListener {
        void onSuccess();
        void onFailure(Exception e);
    }

    public MutableLiveData<List<Project>> getAllProjectsWithDetails() {
        MutableLiveData<List<Project>> liveData = new MutableLiveData<>();

        // STAGE 1: Lấy tất cả các collection cần thiết cùng lúc
        Task<QuerySnapshot> projectsTask = db.collection("Projects").get();
        Task<QuerySnapshot> usersTask = db.collection("Users").get();
        Task<QuerySnapshot> categoriesTask = db.collection("Categories").get();
        Task<QuerySnapshot> technologiesTask = db.collection("Technologies").get();
        Task<QuerySnapshot> projectCategoriesTask = db.collection("ProjectCategories").get();
        Task<QuerySnapshot> projectTechnologiesTask = db.collection("ProjectTechnologies").get();
        Task<QuerySnapshot> featuredProjectsTask = db.collection("FeaturedProjects").get();

        Tasks.whenAllSuccess(projectsTask, usersTask, categoriesTask, technologiesTask,
                        projectCategoriesTask, projectTechnologiesTask, featuredProjectsTask)
                .addOnSuccessListener(results -> {
                    // --- STAGE 2: CHUYỂN DỮ LIỆU THÔ SANG CÁC MAP ĐỂ TRA CỨU NHANH ---
                    List<Project> projectList = ((QuerySnapshot) results.get(0)).toObjects(Project.class);
                    Map<String, User> userMap = ((QuerySnapshot) results.get(1)).getDocuments().stream().collect(Collectors.toMap(DocumentSnapshot::getId, doc -> doc.toObject(User.class)));
                    Map<String, String> categoryMap = ((QuerySnapshot) results.get(2)).getDocuments().stream().collect(Collectors.toMap(DocumentSnapshot::getId, doc -> doc.getString("Name")));
                    Map<String, String> technologyMap = ((QuerySnapshot) results.get(3)).getDocuments().stream().collect(Collectors.toMap(DocumentSnapshot::getId, doc -> doc.getString("Name")));
                    Map<String, String> projectToCategoryMap = ((QuerySnapshot) results.get(4)).getDocuments().stream().collect(Collectors.toMap(doc -> doc.getString("ProjectId"), doc -> doc.getString("CategoryId"), (v1, v2) -> v1));
                    Map<String, List<String>> projectToTechsMap = new HashMap<>();
                    for (DocumentSnapshot doc : ((QuerySnapshot) results.get(5)).getDocuments()) {
                        projectToTechsMap.computeIfAbsent(doc.getString("ProjectId"), k -> new ArrayList<>()).add(doc.getString("TechnologyId"));
                    }
                    List<String> featuredProjectIds = ((QuerySnapshot) results.get(6)).getDocuments().stream().map(doc -> doc.getString("ProjectId")).collect(Collectors.toList());

                    // --- STAGE 3: KẾT HỢP DỮ LIỆU VÀO DANH SÁCH PROJECT ---
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
                            List<String> techNames = techIds.stream().map(technologyMap::get).collect(Collectors.toList());
                            project.setTechnologyNames(techNames);
                        }

                        project.setFeatured(featuredProjectIds.contains(project.getProjectId()));
                    }
                    liveData.setValue(projectList);
                }).addOnFailureListener(e -> {
                    Log.e(TAG, "Lỗi khi lấy dữ liệu tổng hợp: ", e);
                    liveData.setValue(null);
                });
        return liveData;
    }

    public void addFeaturedProject(String projectId, OnTaskCompleteListener listener) {
        Map<String, Object> data = new HashMap<>();
        data.put("ProjectId", projectId);
        db.collection("FeaturedProjects").add(data)
                .addOnSuccessListener(documentReference -> listener.onSuccess())
                .addOnFailureListener(listener::onFailure);
    }
}