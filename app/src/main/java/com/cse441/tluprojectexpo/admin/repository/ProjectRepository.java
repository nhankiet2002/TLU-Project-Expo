package com.cse441.tluprojectexpo.admin.repository;

import android.util.Log;
import androidx.annotation.NonNull;
import androidx.lifecycle.MutableLiveData;

import com.cse441.tluprojectexpo.model.Comment;
import com.cse441.tluprojectexpo.model.FeaturedProjectUIModel;
import com.cse441.tluprojectexpo.model.Project;
import com.cse441.tluprojectexpo.model.User;
import com.google.android.gms.tasks.Task;
import com.google.android.gms.tasks.Tasks;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FieldPath;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QuerySnapshot;
import com.google.firebase.firestore.WriteBatch;

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

        Task<QuerySnapshot> projectsTask = db.collection("Projects").whereEqualTo("IsApproved", true).get();
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
     * PHƯƠNG THỨC CHO TRANG FEATURED MANAGEMENT PAGE
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


    // =================================================================================
    // CÁC PHƯƠNG THỨC LỌC VÀ TÌM KIẾM MỚI
    // =================================================================================

    /**
     * Hàm tổng hợp cho việc lọc và tìm kiếm (PHIÊN BẢN NÂNG CẤP).
     * @param searchQuery Chuỗi tìm kiếm theo tên.
     * @param categoryId ID của lĩnh vực cần lọc.
     * @param technologyIds DANH SÁCH ID của các công nghệ cần lọc.
     * @param status Trạng thái cần lọc.
     */
    public MutableLiveData<List<Project>> getFilteredProjects(String searchQuery, String categoryId, List<String> technologyIds, String status) {
        MutableLiveData<List<Project>> liveData = new MutableLiveData<>();

        getProjectIdsByTechnologies(technologyIds).addOnSuccessListener(filteredProjectIds -> {
            if (technologyIds != null && !technologyIds.isEmpty() && filteredProjectIds.isEmpty()) {
                liveData.setValue(new ArrayList<>());
                return;
            }

            Query projectsQuery = buildProjectsQuery(searchQuery, status, filteredProjectIds);

            Task<QuerySnapshot> projectsTask = projectsQuery.whereEqualTo("IsApproved", true).get();
            Task<QuerySnapshot> usersTask = db.collection("Users").get();
            Task<QuerySnapshot> categoriesTask = db.collection("Categories").get();
            Task<QuerySnapshot> technologiesTask = db.collection("Technologies").get();
            Task<QuerySnapshot> projectCategoriesTask = db.collection("ProjectCategories").get();
            Task<QuerySnapshot> projectTechnologiesTask = db.collection("ProjectTechnologies").get();

            Tasks.whenAllSuccess(projectsTask, usersTask, categoriesTask, technologiesTask,
                            projectCategoriesTask, projectTechnologiesTask)
                    .addOnSuccessListener(results -> {
                        Map<String, User> userMap = ((QuerySnapshot) results.get(1)).getDocuments().stream().collect(Collectors.toMap(DocumentSnapshot::getId, doc -> doc.toObject(User.class)));
                        Map<String, String> categoryMap = ((QuerySnapshot) results.get(2)).getDocuments().stream().collect(Collectors.toMap(DocumentSnapshot::getId, doc -> doc.getString("Name")));
                        Map<String, String> technologyMap = ((QuerySnapshot) results.get(3)).getDocuments().stream().collect(Collectors.toMap(DocumentSnapshot::getId, doc -> doc.getString("Name")));
                        Map<String, String> projectToCategoryMap = ((QuerySnapshot) results.get(4)).getDocuments().stream().collect(Collectors.toMap(doc -> doc.getString("ProjectId"), doc -> doc.getString("CategoryId"), (v1, v2) -> v1));
                        Map<String, List<String>> projectToTechsMap = new HashMap<>();
                        for (DocumentSnapshot doc : ((QuerySnapshot) results.get(5)).getDocuments()) {
                            projectToTechsMap.computeIfAbsent(doc.getString("ProjectId"), k -> new ArrayList<>()).add(doc.getString("TechnologyId"));
                        }

                        List<Project> finalProjectList = new ArrayList<>();
                        QuerySnapshot projectsSnapshot = (QuerySnapshot) results.get(0);
                        for (DocumentSnapshot doc : projectsSnapshot.getDocuments()) {
                            Project p = doc.toObject(Project.class);
                            if (p != null) {
                                p.setProjectId(doc.getId());
                                boolean categoryMatch = (categoryId == null || categoryId.isEmpty()) || categoryId.equals(projectToCategoryMap.get(p.getProjectId()));
                                if (categoryMatch) {
                                    populateProjectDetails(p, userMap, categoryMap, technologyMap, projectToCategoryMap, projectToTechsMap);
                                    finalProjectList.add(p);
                                }
                            }
                        }

                        sortProjects(finalProjectList);
                        liveData.setValue(finalProjectList);
                    })
                    .addOnFailureListener(e -> {
                        Log.e(TAG, "Lỗi khi lấy chi tiết dự án: ", e);
                        liveData.setValue(null);
                    });
        }).addOnFailureListener(e -> {
            Log.e(TAG, "Lỗi khi lọc project ID theo công nghệ: ", e);
            liveData.setValue(null);
        });

        return liveData;
    }


    /**
     * Hàm phụ: Lấy danh sách các Project ID dựa trên danh sách Technology ID.
     */
    private Task<List<String>> getProjectIdsByTechnologies(List<String> technologyIds) {
        if (technologyIds == null || technologyIds.isEmpty()) {
            return Tasks.forResult(null);
        }
        if (technologyIds.size() > 10) {
            Log.w(TAG, "Lọc công nghệ vượt quá 10, chỉ lấy 10 mục đầu tiên.");
            technologyIds = technologyIds.subList(0, 10);
        }

        return db.collection("ProjectTechnologies")
                .whereIn("TechnologyId", technologyIds)
                .get()
                .continueWith(task -> {
                    if (!task.isSuccessful()) {
                        throw Objects.requireNonNull(task.getException());
                    }
                    return task.getResult().getDocuments().stream()
                            .map(doc -> doc.getString("ProjectId"))
                            .filter(Objects::nonNull)
                            .distinct()
                            .collect(Collectors.toList());
                });
    }


    /**
     * Hàm phụ: Xây dựng câu truy vấn chính cho collection "Projects".
     */
    private Query buildProjectsQuery(String searchQuery, String status, List<String> projectIds) {
        Query query = db.collection("Projects");
        if (projectIds != null) {
            if (projectIds.isEmpty()) {
                return query.whereEqualTo(FieldPath.documentId(), "impossible_id_to_find");
            }
            if (projectIds.size() > 10) {
                projectIds = projectIds.subList(0, 10);
            }
            query = query.whereIn(FieldPath.documentId(), projectIds);
        }
        if (status != null && !status.isEmpty()) {
            query = query.whereEqualTo("Status", status);
        }
        if (searchQuery != null && !searchQuery.trim().isEmpty()) {
            query = query.whereGreaterThanOrEqualTo("Title", searchQuery)
                    .whereLessThanOrEqualTo("Title", searchQuery + "\uf8ff");
        }
        return query;
    }


    /**
     * Hàm phụ để điền các thông tin chi tiết (tên tác giả, lĩnh vực, công nghệ) vào đối tượng Project.
     */
    private void populateProjectDetails(Project project, Map<String, User> userMap, Map<String, String> categoryMap,
                                        Map<String, String> technologyMap, Map<String, String> projectToCategoryMap,
                                        Map<String, List<String>> projectToTechsMap) {
        User creator = userMap.get(project.getCreatorUserId());
        if (creator != null) {
            project.setCreatorFullName(creator.getFullName());
        }

        String categoryId = projectToCategoryMap.get(project.getProjectId());
        if (categoryId != null) {
            String categoryName = categoryMap.get(categoryId);
            project.setCategoryNames(new ArrayList<>(Collections.singletonList(categoryName != null ? categoryName : "")));
        }

        List<String> techIds = projectToTechsMap.get(project.getProjectId());
        if (techIds != null) {
            List<String> techNames = techIds.stream()
                    .map(technologyMap::get)
                    .filter(Objects::nonNull)
                    .collect(Collectors.toList());
            project.setTechnologyNames(techNames);
        }
    }


    /**
     * Hàm phụ để sắp xếp danh sách dự án.
     */
    private void sortProjects(List<Project> projectList) {
        projectList.sort((p1, p2) -> {
            if (p1.isFeatured() && !p2.isFeatured()) return -1;
            if (!p1.isFeatured() && p2.isFeatured()) return 1;
            if (p1.getCreatedAt() != null && p2.getCreatedAt() != null) {
                return p2.getCreatedAt().compareTo(p1.getCreatedAt());
            }
            return 0;
        });
    }


    //     HÀM XÓA PROJECT HOÀN CHỈNH
    // =================================================================================

    /**
     * Xóa một dự án và TẤT CẢ các dữ liệu liên quan trong các collection khác.
     * @param projectId ID của dự án cần xóa.
     * @param listener Callback để thông báo kết quả.
     */
    public void deleteProject(String projectId, @NonNull OnTaskCompleteListener listener) {
        if (projectId == null || projectId.isEmpty()) {
            listener.onFailure(new IllegalArgumentException("Project ID không hợp lệ."));
            return;
        }

        // Tạo một WriteBatch để thực hiện nhiều thao tác xóa cùng lúc một cách nguyên tử
        WriteBatch batch = db.batch();

        // 1. Thêm thao tác xóa document chính trong "Projects"
        batch.delete(db.collection("Projects").document(projectId));
        Log.d(TAG, "Chuẩn bị xóa: Project " + projectId);

        // Tạo danh sách các Task để tìm và xóa dữ liệu liên quan
        List<Task<Void>> deletionTasks = new ArrayList<>();

        // 2. Thêm Task tìm và xóa trong "ProjectCategories"
        deletionTasks.add(findAndDeleteRelatedDocs("ProjectCategories", "ProjectId", projectId, batch));

        // 3. Thêm Task tìm và xóa trong "ProjectMembers"
        deletionTasks.add(findAndDeleteRelatedDocs("ProjectMembers", "ProjectId", projectId, batch));

        // 4. Thêm Task tìm và xóa trong "ProjectTechnologies"
        deletionTasks.add(findAndDeleteRelatedDocs("ProjectTechnologies", "ProjectId", projectId, batch));

        // 5. Thêm Task tìm và xóa trong "ProjectVotes"
        deletionTasks.add(findAndDeleteRelatedDocs("ProjectVotes", "ProjectId", projectId, batch));

        // 6. Thêm Task tìm và xóa trong "Comments"
        deletionTasks.add(findAndDeleteRelatedDocs("Comments", "ProjectId", projectId, batch));

        // 7. Chờ tất cả các Task tìm kiếm hoàn tất
        Tasks.whenAll(deletionTasks)
                .addOnSuccessListener(aVoid -> {
                    // 8. Sau khi đã thêm tất cả thao tác xóa vào batch, thực thi batch
                    batch.commit()
                            .addOnSuccessListener(aVoid1 -> {
                                Log.d(TAG, "XÓA THÀNH CÔNG dự án và các dữ liệu liên quan của: " + projectId);
                                listener.onSuccess();
                            })
                            .addOnFailureListener(e -> {
                                Log.e(TAG, "Lỗi khi thực thi batch xóa: ", e);
                                listener.onFailure(e);
                            });
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Lỗi khi tìm kiếm dữ liệu liên quan để xóa: ", e);
                    listener.onFailure(e);
                });
    }

    /**
     * Hàm phụ trợ để tìm các document liên quan và thêm thao tác xóa vào batch.
     * @param collectionName Tên của collection cần tìm (ví dụ: "Comments").
     * @param fieldName Tên của trường chứa ProjectID (ví dụ: "ProjectId").
     * @param projectId ID của dự án.
     * @param batch WriteBatch đang được sử dụng.
     * @return Một Task đại diện cho quá trình tìm kiếm.
     */
    private Task<Void> findAndDeleteRelatedDocs(String collectionName, String fieldName, String projectId, WriteBatch batch) {
        return db.collection(collectionName)
                .whereEqualTo(fieldName, projectId)
                .get()
                .onSuccessTask(querySnapshot -> {
                    for (DocumentSnapshot doc : querySnapshot.getDocuments()) {
                        batch.delete(doc.getReference());
                        Log.d(TAG, "Chuẩn bị xóa: " + collectionName + " doc " + doc.getId());
                    }
                    // Trả về một task đã hoàn thành để báo hiệu quá trình này xong
                    return Tasks.forResult(null);
                });
    }


    //     PHƯƠNG THỨC LẤY CHI TIẾT DỰ ÁN - PHIÊN BẢN HOÀN CHỈNH
// =================================================================================
    public MutableLiveData<Project> getProjectDetailsById(String projectId) {
        MutableLiveData<Project> projectLiveData = new MutableLiveData<>();

        if (projectId == null || projectId.isEmpty()) {
            projectLiveData.setValue(null);
            return projectLiveData;
        }

        // --- STAGE 1: Lấy tất cả các dữ liệu cần thiết một cách song song ---
        // Lấy document project chính
        Task<DocumentSnapshot> projectTask = db.collection("Projects").document(projectId).get();

        // Lấy thông tin phụ liên quan đến project này
        Task<QuerySnapshot> projectCategoriesTask = db.collection("ProjectCategories").whereEqualTo("ProjectId", projectId).get();
        Task<QuerySnapshot> projectTechnologiesTask = db.collection("ProjectTechnologies").whereEqualTo("ProjectId", projectId).get();
        Task<QuerySnapshot> projectMembersTask = db.collection("ProjectMembers").whereEqualTo("ProjectId", projectId).get();
        Task<QuerySnapshot> commentsTask = db.collection("Comments").whereEqualTo("ProjectId", projectId).orderBy("CreatedAt").get();

        // Lấy các bảng tra cứu (lookup tables)
        Task<QuerySnapshot> usersTask = db.collection("Users").get();
        Task<QuerySnapshot> categoriesTask = db.collection("Categories").get();
        Task<QuerySnapshot> technologiesTask = db.collection("Technologies").get();

        // --- STAGE 2: Kết hợp tất cả các task ---
        Tasks.whenAllSuccess(projectTask, projectCategoriesTask, projectTechnologiesTask, projectMembersTask, commentsTask,
                        usersTask, categoriesTask, technologiesTask)
                .addOnSuccessListener(results -> {
                    // --- STAGE 3: Chuyển đổi dữ liệu thô sang đối tượng và các Map ---
                    DocumentSnapshot projectDoc = (DocumentSnapshot) results.get(0);
                    if (!projectDoc.exists()) {
                        Log.e(TAG, "Không tìm thấy dự án với ID: " + projectId);
                        projectLiveData.setValue(null);
                        return;
                    }

                    Project project = projectDoc.toObject(Project.class);
                    if (project == null) {
                        projectLiveData.setValue(null);
                        return;
                    }
                    project.setProjectId(projectDoc.getId());

                    // Lấy kết quả từ các task
                    QuerySnapshot pCategoriesSnap = (QuerySnapshot) results.get(1);
                    QuerySnapshot pTechsSnap = (QuerySnapshot) results.get(2);
                    QuerySnapshot pMembersSnap = (QuerySnapshot) results.get(3);
                    QuerySnapshot commentsSnap = (QuerySnapshot) results.get(4);

                    // Tạo các map để tra cứu
                    Map<String, User> userMap = ((QuerySnapshot) results.get(5)).getDocuments().stream().collect(Collectors.toMap(DocumentSnapshot::getId, doc -> doc.toObject(User.class)));
                    Map<String, String> categoryMap = ((QuerySnapshot) results.get(6)).getDocuments().stream().collect(Collectors.toMap(DocumentSnapshot::getId, doc -> doc.getString("Name")));
                    Map<String, String> technologyMap = ((QuerySnapshot) results.get(7)).getDocuments().stream().collect(Collectors.toMap(DocumentSnapshot::getId, doc -> doc.getString("Name")));

                    // --- STAGE 4: Điền các thông tin chi tiết vào đối tượng Project ---

                    // 4.1. Điền tên người tạo
                    User creator = userMap.get(project.getCreatorUserId());
                    if (creator != null) {
                        project.setCreatorFullName(creator.getFullName());
                    }

                    // 4.2. Điền tên lĩnh vực
                    if (!pCategoriesSnap.isEmpty()) {
                        String categoryId = pCategoriesSnap.getDocuments().get(0).getString("CategoryId");
                        String categoryName = categoryMap.get(categoryId);
                        if (categoryName != null) {
                            project.setCategoryNames(new ArrayList<>(Collections.singletonList(categoryName)));
                        }
                    }

                    // 4.3. Điền danh sách tên công nghệ
                    List<String> techNames = pTechsSnap.getDocuments().stream()
                            .map(doc -> doc.getString("TechnologyId"))
                            .filter(Objects::nonNull)
                            .map(technologyMap::get)
                            .filter(Objects::nonNull)
                            .collect(Collectors.toList());
                    project.setTechnologyNames(techNames);

                    // --- STAGE 4: Điền thông tin chi tiết ---

                    // 4.4 Điền thông tin thành viên (tái sử dụng từ code cũ của bạn)
                    project.setProjectMembersInfo(pMembersSnap.getDocuments().stream()
                            .map(doc -> {
                                User user = userMap.get(doc.getString("UserId"));
                                return user != null ? new Project.UserShortInfo(user.getUserId(), user.getFullName(), user.getAvatarUrl(), doc.getString("RoleInProject")) : null;
                            })
                            .filter(Objects::nonNull).collect(Collectors.toList()));

                    // 4.5 Xây dựng cây bình luận
                    List<Comment> allComments = new ArrayList<>();
                    Map<String, Comment> commentMap = new HashMap<>();

                    for (DocumentSnapshot doc : commentsSnap.getDocuments()) {
                        Comment comment = doc.toObject(Comment.class);
                        if (comment != null) {
                            comment.setCommentId(doc.getId());
                            User author = userMap.get(comment.getUserId());
                            if (author != null) {
                                comment.setUserName(author.getFullName());
                                comment.setUserAvatarUrl(author.getAvatarUrl());
                            }
                            allComments.add(comment);
                            commentMap.put(comment.getCommentId(), comment);
                        }
                    }

                    List<Comment> rootComments = new ArrayList<>();
                    for (Comment comment : allComments) {
                        if (comment.getParentCommentId() != null && commentMap.containsKey(comment.getParentCommentId())) {
                            commentMap.get(comment.getParentCommentId()).addReply(comment);
                        } else {
                            rootComments.add(comment);
                        }
                    }
                    project.setComments(rootComments); // << Bạn cần thêm trường và setter này vào model Project


                    // --- STAGE 5: Trả về đối tượng Project đã hoàn chỉnh ---
                    projectLiveData.setValue(project);

                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Lỗi khi lấy chi tiết dự án: ", e);
                    projectLiveData.setValue(null);
                });

        return projectLiveData;
    }


    public MutableLiveData<List<Project>> getUnapprovedProjects() {
        MutableLiveData<List<Project>> liveData = new MutableLiveData<>();

        // 1. Lấy các project có IsApproved = false và sắp xếp theo ngày tạo
        Task<QuerySnapshot> projectsTask = db.collection("Projects")
                .whereEqualTo("IsApproved", false)
                .orderBy("CreatedAt", Query.Direction.DESCENDING)
                .get();

        // 2. Vẫn cần các bảng tra cứu
        Task<QuerySnapshot> usersTask = db.collection("Users").get();
        Task<QuerySnapshot> categoriesTask = db.collection("Categories").get();
        Task<QuerySnapshot> projectCategoriesTask = db.collection("ProjectCategories").get();

        Tasks.whenAllSuccess(projectsTask, usersTask, categoriesTask, projectCategoriesTask)
                .addOnSuccessListener(results -> {
                    // 3. Xử lý dữ liệu
                    QuerySnapshot projectsSnapshot = (QuerySnapshot) results.get(0);
                    Map<String, User> userMap = ((QuerySnapshot) results.get(1)).getDocuments().stream().collect(Collectors.toMap(DocumentSnapshot::getId, doc -> doc.toObject(User.class)));
                    Map<String, String> categoryMap = ((QuerySnapshot) results.get(2)).getDocuments().stream().collect(Collectors.toMap(DocumentSnapshot::getId, doc -> doc.getString("Name")));
                    Map<String, String> projectToCategoryMap = ((QuerySnapshot) results.get(3)).getDocuments().stream().collect(Collectors.toMap(doc -> doc.getString("ProjectId"), doc -> doc.getString("CategoryId"), (v1, v2) -> v1));

                    List<Project> projectList = new ArrayList<>();
                    for (DocumentSnapshot doc : projectsSnapshot.getDocuments()) {
                        Project p = doc.toObject(Project.class);
                        if (p != null) {
                            p.setProjectId(doc.getId());

                            // Điền thông tin phụ
                            User creator = userMap.get(p.getCreatorUserId());
                            if (creator != null) {
                                p.setCreatorFullName(creator.getFullName());
                            }
                            String categoryId = projectToCategoryMap.get(p.getProjectId());
                            if (categoryId != null) {
                                String categoryName = categoryMap.get(categoryId);
                                p.setCategoryNames(new ArrayList<>(Collections.singletonList(categoryName != null ? categoryName : "")));
                            }
                            projectList.add(p);
                        }
                    }
                    liveData.setValue(projectList);
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Lỗi khi lấy danh sách dự án chờ duyệt: ", e);
                    liveData.setValue(null);
                });


        return liveData;
    }

    public void setProjectApprovalStatus(String projectId, boolean isApproved, @NonNull OnTaskCompleteListener listener) {
        if (projectId == null || projectId.isEmpty()) {
            listener.onFailure(new IllegalArgumentException("Project ID không hợp lệ"));
            return;
        }
        db.collection("Projects").document(projectId)
                .update("IsApproved", isApproved)
                .addOnSuccessListener(aVoid -> listener.onSuccess())
                .addOnFailureListener(listener::onFailure);
    }
}