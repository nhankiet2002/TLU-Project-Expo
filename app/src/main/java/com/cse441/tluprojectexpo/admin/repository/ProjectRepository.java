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
}