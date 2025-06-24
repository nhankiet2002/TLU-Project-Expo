package com.cse441.tluprojectexpo.repository;

import android.util.Log;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.cse441.tluprojectexpo.model.Project;
import com.cse441.tluprojectexpo.model.User;
// KHÔNG import com.cse441.tluprojectexpo.utils.Constants; // THEO YÊU CẦU
import com.google.android.gms.tasks.Task;
import com.google.android.gms.tasks.Tasks;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FieldPath;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.QuerySnapshot;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

public class ProjectRepository {
    private static final String TAG = "ProjectRepository";
    private static final int PROJECTS_PER_PAGE = 10;

    // --- TỰ ĐỊNH NGHĨA CÁC HẰNG SỐ CẦN THIẾT CHO REPOSITORY NÀY ---
    // (Phải khớp với file Constants.java bạn đang giữ và cấu trúc Firestore)
    private static final String COLLECTION_PROJECTS = "Projects";
    private static final String COLLECTION_USERS = "Users";
    private static final String COLLECTION_CATEGORIES = "Categories";
    private static final String COLLECTION_TECHNOLOGIES = "Technologies";
    private static final String COLLECTION_PROJECT_CATEGORIES = "ProjectCategories";
    private static final String COLLECTION_PROJECT_TECHNOLOGIES = "ProjectTechnologies";
    private static final String COLLECTION_PROJECT_MEMBERS = "ProjectMembers"; // Cho fetchProjectMembers

    private static final String FIELD_PROJECT_ID_IN_JOINS = "ProjectId"; // Tên field ProjectId trong các bảng join
    private static final String FIELD_CATEGORY_ID_IN_JOINS = "CategoryId";
    private static final String FIELD_TECHNOLOGY_ID_IN_JOINS = "TechnologyId";
    private static final String FIELD_USER_ID_IN_MEMBERS = "UserId"; // Trong ProjectMembers
    private static final String FIELD_ROLE_IN_PROJECT_MEMBERS = "RoleInProject"; // Trong ProjectMembers
    private static final String FIELD_NAME_IN_CAT_TECH = "Name"; // Tên field "Name" trong Categories/Technologies

    // Fields trong Projects collection (phải khớp với @PropertyName trong Project.java hoặc tên field Firestore)
    private static final String FIELD_IS_APPROVED_IN_PROJECTS = "IsApproved";
    private static final String FIELD_IS_FEATURED_IN_PROJECTS = "IsFeatured";
    private static final String FIELD_TITLE_IN_PROJECTS = "Title"; // Quan trọng cho tìm kiếm
    private static final String FIELD_STATUS_IN_PROJECTS = "Status";
    private static final String FIELD_CREATED_AT_IN_PROJECTS = "CreatedAt"; // Cho sắp xếp
    private static final String FIELD_VOTE_COUNT_IN_PROJECTS = "VoteCount"; // Cho sắp xếp
    private static final String FIELD_CREATOR_USER_ID_IN_PROJECTS = "CreatorUserId"; // Cho sắp xếp

    // Fields trong Users collection (phải khớp với @PropertyName trong User.java hoặc tên field Firestore)
    private static final String FIELD_FULL_NAME_IN_USERS = "FullName";
    private static final String FIELD_AVATAR_URL_IN_USERS = "AvatarUrl";

    private static final String DEFAULT_MEMBER_ROLE_VALUE = "Thành viên"; // Giá trị mặc định
    // --- KẾT THÚC TỰ ĐỊNH NGHĨA HẰNG SỐ ---


    private FirebaseFirestore db;
    private UserRepository userRepository;

    public static class ProjectListResult {
        public List<Project> projects;
        public boolean isLastPage;
        public DocumentSnapshot newLastVisible;
        public ProjectListResult(List<Project> projects, boolean isLastPage, @Nullable DocumentSnapshot newLastVisible) {
            this.projects = projects; this.isLastPage = isLastPage; this.newLastVisible = newLastVisible;
        }
    }
    public interface ProjectsListFetchListener {
        void onProjectsFetched(ProjectListResult result);
        void onFetchFailed(String errorMessage);
        void onNoProjectsFound();
    }

    // Listener cho fetchProjectDetails
    public interface ProjectDetailsListener {
        void onProjectFetched(Project project);
        void onProjectNotFound();
        void onError(String errorMessage);
    }
    // Listener cho fetchCategoriesForProject, etc.
    public interface ProjectRelatedListListener<T> {
        void onListFetched(List<T> items);
        void onListEmpty();
        void onError(String errorMessage);
    }

    public ProjectRepository() {
        this.db = FirebaseFirestore.getInstance();
        this.userRepository = new UserRepository(); // Giả sử UserRepository không cần truyền db
    }

    // --- CÁC PHƯƠNG THỨC CHO ProjectDetailActivity (SỬ DỤNG HẰNG SỐ ĐỊNH NGHĨA Ở TRÊN) ---
    public void fetchProjectDetails(String projectId, @NonNull ProjectDetailsListener listener) {
        if (projectId == null || projectId.isEmpty()) {
            listener.onError("Project ID không hợp lệ."); return;
        }
        db.collection(COLLECTION_PROJECTS).document(projectId).get()
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
        db.collection(COLLECTION_PROJECT_CATEGORIES)
                .whereEqualTo(FIELD_PROJECT_ID_IN_JOINS, projectId)
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    if (queryDocumentSnapshots.isEmpty()) { listener.onListEmpty(); return; }
                    List<Task<DocumentSnapshot>> categoryTasks = new ArrayList<>();
                    for (QueryDocumentSnapshot doc : queryDocumentSnapshots) {
                        String categoryId = doc.getString(FIELD_CATEGORY_ID_IN_JOINS);
                        if (categoryId != null) {
                            categoryTasks.add(db.collection(COLLECTION_CATEGORIES).document(categoryId).get());
                        }
                    }
                    if (categoryTasks.isEmpty()) { listener.onListEmpty(); return; }
                    Tasks.whenAllSuccess(categoryTasks).addOnSuccessListener(list -> {
                        List<String> categoryNames = new ArrayList<>();
                        for (Object snapshot : list) {
                            DocumentSnapshot catSnap = (DocumentSnapshot) snapshot;
                            if (catSnap.exists() && catSnap.getString(FIELD_NAME_IN_CAT_TECH) != null) {
                                categoryNames.add(catSnap.getString(FIELD_NAME_IN_CAT_TECH));
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
        db.collection(COLLECTION_PROJECT_TECHNOLOGIES)
                .whereEqualTo(FIELD_PROJECT_ID_IN_JOINS, projectId)
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    if (queryDocumentSnapshots.isEmpty()) { listener.onListEmpty(); return; }
                    List<Task<DocumentSnapshot>> techTasks = new ArrayList<>();
                    for (QueryDocumentSnapshot doc : queryDocumentSnapshots) {
                        String technologyId = doc.getString(FIELD_TECHNOLOGY_ID_IN_JOINS);
                        if (technologyId != null) {
                            techTasks.add(db.collection(COLLECTION_TECHNOLOGIES).document(technologyId).get());
                        }
                    }
                    if (techTasks.isEmpty()) { listener.onListEmpty(); return; }
                    Tasks.whenAllSuccess(techTasks).addOnSuccessListener(list -> {
                        List<String> techNames = new ArrayList<>();
                        for (Object snapshot : list) {
                            DocumentSnapshot techSnap = (DocumentSnapshot) snapshot;
                            if (techSnap.exists() && techSnap.getString(FIELD_NAME_IN_CAT_TECH) != null) {
                                techNames.add(techSnap.getString(FIELD_NAME_IN_CAT_TECH));
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
        db.collection(COLLECTION_PROJECT_MEMBERS)
                .whereEqualTo(FIELD_PROJECT_ID_IN_JOINS, projectId)
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    if (queryDocumentSnapshots.isEmpty()) { listener.onListEmpty(); return; }
                    List<Task<DocumentSnapshot>> userTasks = new ArrayList<>();
                    List<String> roles = new ArrayList<>();
                    for (QueryDocumentSnapshot doc : queryDocumentSnapshots) {
                        String userId = doc.getString(FIELD_USER_ID_IN_MEMBERS);
                        String role = doc.getString(FIELD_ROLE_IN_PROJECT_MEMBERS);
                        if (userId != null) {
                            userTasks.add(db.collection(COLLECTION_USERS).document(userId).get());
                            roles.add(role != null ? role : DEFAULT_MEMBER_ROLE_VALUE);
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
                                            user.getAvatarUrl(), roles.get(i), user.getClassName()
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
    // --- KẾT THÚC PHƯƠNG THỨC CHO ProjectDetailActivity ---


    // --- PHƯƠNG THỨC CHO HomeFragment (SỬ DỤNG HẰNG SỐ ĐỊNH NGHĨA Ở TRÊN) ---
    private Task<List<String>> getProjectIdsByJoinTableForHome(@Nullable String filterId, String collectionName, String idFieldInJoinTable) {
        if (filterId == null) {
            return Tasks.forResult(null);
        }
        CollectionReference joinTableRef = db.collection(collectionName);
        return joinTableRef.whereEqualTo(idFieldInJoinTable, filterId)
                .get()
                .continueWith(task -> {
                    List<String> projectIds = new ArrayList<>();
                    if (task.isSuccessful() && task.getResult() != null) {
                        for (QueryDocumentSnapshot doc : task.getResult()) {
                            String projectIdVal = doc.getString(FIELD_PROJECT_ID_IN_JOINS); // Dùng hằng số
                            if (projectIdVal != null) projectIds.add(projectIdVal);
                        }
                    } else {
                        Log.w(TAG, "Error fetching project IDs from " + collectionName, task.getException());
                    }
                    if (projectIds.isEmpty()){
                        projectIds.add("NO_PROJECTS_MATCH_THIS_FILTER");
                    }
                    return projectIds;
                });
    }

    public void fetchProjectsList(String searchQuery, String sortField, Query.Direction sortDirection,
                                  @Nullable String categoryId, @Nullable String technologyId, @Nullable String status,
                                  @Nullable DocumentSnapshot lastVisibleDocForPagination,
                                  @NonNull ProjectsListFetchListener listener) {

        Task<List<String>> categoryProjectIdsTask = getProjectIdsByJoinTableForHome(categoryId, COLLECTION_PROJECT_CATEGORIES, FIELD_CATEGORY_ID_IN_JOINS);
        Task<List<String>> technologyProjectIdsTask = getProjectIdsByJoinTableForHome(technologyId, COLLECTION_PROJECT_TECHNOLOGIES, FIELD_TECHNOLOGY_ID_IN_JOINS);

        Tasks.whenAllSuccess(categoryProjectIdsTask, technologyProjectIdsTask).onSuccessTask(results -> {
            List<String> idsFromCategory = (List<String>) results.get(0);
            List<String> idsFromTechnology = (List<String>) results.get(1);
            List<String> finalFilteredProjectIds = null;

            boolean categoryFilterActive = idsFromCategory != null;
            boolean technologyFilterActive = idsFromTechnology != null;

            if (categoryFilterActive && technologyFilterActive) {
                if (idsFromCategory.contains("NO_PROJECTS_MATCH_THIS_FILTER") || idsFromTechnology.contains("NO_PROJECTS_MATCH_THIS_FILTER")) {
                    finalFilteredProjectIds = Collections.singletonList("NO_PROJECTS_MATCH_THIS_FILTER");
                } else {
                    finalFilteredProjectIds = new ArrayList<>(idsFromCategory);
                    finalFilteredProjectIds.retainAll(idsFromTechnology);
                    if (finalFilteredProjectIds.isEmpty()) {
                        finalFilteredProjectIds.add("NO_PROJECTS_MATCH_THIS_FILTER");
                    }
                }
            } else if (categoryFilterActive) {
                finalFilteredProjectIds = idsFromCategory;
            } else if (technologyFilterActive) {
                finalFilteredProjectIds = idsFromTechnology;
            }

            Query query = db.collection(COLLECTION_PROJECTS).whereEqualTo(FIELD_IS_APPROVED_IN_PROJECTS, true);

            if (!searchQuery.isEmpty()) {
                query = query.orderBy(FIELD_IS_FEATURED_IN_PROJECTS, Query.Direction.DESCENDING)
                        .orderBy(FIELD_TITLE_IN_PROJECTS)
                        .whereGreaterThanOrEqualTo(FIELD_TITLE_IN_PROJECTS, searchQuery)
                        .whereLessThanOrEqualTo(FIELD_TITLE_IN_PROJECTS, searchQuery + "\uf8ff");
            } else {
                // Đảm bảo sortField là một trong các hằng số đã định nghĩa
                String actualSortField = FIELD_CREATED_AT_IN_PROJECTS; // Mặc định
                if (FIELD_TITLE_IN_PROJECTS.equals(sortField) || FIELD_VOTE_COUNT_IN_PROJECTS.equals(sortField)) {
                    actualSortField = sortField;
                }
                query = query.orderBy(FIELD_IS_FEATURED_IN_PROJECTS, Query.Direction.DESCENDING)
                        .orderBy(actualSortField, sortDirection);
                if (!actualSortField.equals(FieldPath.documentId().toString())) {
                    query = query.orderBy(FieldPath.documentId(), sortDirection);
                }
            }

            if (status != null && !status.isEmpty()) {
                query = query.whereEqualTo(FIELD_STATUS_IN_PROJECTS, status);
            }

            if (finalFilteredProjectIds != null && !finalFilteredProjectIds.isEmpty()) {
                if (finalFilteredProjectIds.contains("NO_PROJECTS_MATCH_THIS_FILTER")) {
                    listener.onNoProjectsFound();
                    return Tasks.forResult(null);
                }
                if (finalFilteredProjectIds.size() > 30) { // Giới hạn của Firestore cho 'in' query
                    query = query.whereIn(FieldPath.documentId(), finalFilteredProjectIds.subList(0, 30));
                } else {
                    query = query.whereIn(FieldPath.documentId(), finalFilteredProjectIds);
                }
            }

            if (lastVisibleDocForPagination != null) {
                query = query.startAfter(lastVisibleDocForPagination);
            }
            query = query.limit(PROJECTS_PER_PAGE);
            return query.get();

        }).addOnCompleteListener(task -> {
            if (!task.isSuccessful() || task.getResult() == null) {
                Log.e(TAG, "Error fetching projects list: ", task.getException());
                listener.onFetchFailed("Lỗi tải danh sách dự án: " + (task.getException() != null ? task.getException().getMessage() : "Unknown"));
                return;
            }

            QuerySnapshot querySnapshot = task.getResult();
            if (querySnapshot.isEmpty()) {
                if (lastVisibleDocForPagination == null) {
                    listener.onNoProjectsFound();
                } else {
                    listener.onProjectsFetched(new ProjectListResult(new ArrayList<>(), true, lastVisibleDocForPagination));
                }
                return;
            }

            List<Project> fetchedProjects = new ArrayList<>();
            List<Task<Void>> detailTasks = new ArrayList<>();
            DocumentSnapshot newLastVisible = querySnapshot.getDocuments().get(querySnapshot.size() - 1);
            boolean isLastPageResult = querySnapshot.size() < PROJECTS_PER_PAGE;

            for (QueryDocumentSnapshot document : querySnapshot) {
                Project project = document.toObject(Project.class);
                project.setProjectId(document.getId());
                fetchedProjects.add(project);

                // Fetch Creator
                if (project.getCreatorUserId() != null && !project.getCreatorUserId().isEmpty()) {
                    Task<Void> creatorTask = fetchUserDetailsAsyncForHome(project.getCreatorUserId())
                            .onSuccessTask(user -> {
                                if (user != null) project.setCreatorFullName(user.getFullName());
                                return Tasks.forResult(null);
                            });
                    detailTasks.add(creatorTask);
                }
                // Fetch Categories Names
                Task<Void> categoriesTask = fetchCategoriesForProjectAsyncForHome(project.getProjectId())
                        .onSuccessTask(categoryNames -> {
                            project.setCategoryNames(categoryNames);
                            return Tasks.forResult(null);
                        });
                detailTasks.add(categoriesTask);

                // Fetch Technologies Names
                Task<Void> technologiesTask = fetchTechnologiesForProjectAsyncForHome(project.getProjectId())
                        .onSuccessTask(technologyNames -> {
                            project.setTechnologyNames(technologyNames);
                            return Tasks.forResult(null);
                        });
                detailTasks.add(technologiesTask);
            }

            Tasks.whenAll(detailTasks).addOnCompleteListener(allDetailsTask -> {
                listener.onProjectsFetched(new ProjectListResult(fetchedProjects, isLastPageResult, newLastVisible));
            });

        }).addOnFailureListener(e -> {
            Log.e(TAG, "Error in pre-fetching IDs for filters: ", e);
            listener.onFetchFailed("Lỗi khi áp dụng bộ lọc.");
        });
    }

    // Helper async versions for internal use by fetchProjectsList
    private Task<User> fetchUserDetailsAsyncForHome(String userId) {
        return db.collection(COLLECTION_USERS).document(userId).get()
                .continueWith(task -> {
                    if (task.isSuccessful() && task.getResult() != null && task.getResult().exists()) {
                        User user = task.getResult().toObject(User.class);
                        if (user != null) user.setUserId(task.getResult().getId());
                        return user;
                    }
                    return null;
                });
    }

    private Task<List<String>> fetchCategoriesForProjectAsyncForHome(String projectId) {
        return db.collection(COLLECTION_PROJECT_CATEGORIES)
                .whereEqualTo(FIELD_PROJECT_ID_IN_JOINS, projectId).get()
                .onSuccessTask(queryDocumentSnapshots -> {
                    if (queryDocumentSnapshots.isEmpty()) return Tasks.forResult(new ArrayList<>());
                    List<Task<DocumentSnapshot>> tasks = new ArrayList<>();
                    for(QueryDocumentSnapshot doc : queryDocumentSnapshots) {
                        String catId = doc.getString(FIELD_CATEGORY_ID_IN_JOINS);
                        if (catId != null) tasks.add(db.collection(COLLECTION_CATEGORIES).document(catId).get());
                    }
                    return Tasks.whenAllSuccess(tasks);
                }).continueWith(task -> {
                    List<String> names = new ArrayList<>();
                    if (task.isSuccessful() && task.getResult() != null) {
                        List<Object> snapshots = task.getResult();
                        for(Object obj : snapshots) {
                            if (obj instanceof DocumentSnapshot) {
                                DocumentSnapshot snap = (DocumentSnapshot) obj;
                                if (snap.exists()) names.add(snap.getString(FIELD_NAME_IN_CAT_TECH));
                            }
                        }
                    }
                    return names;
                });
    }

    private Task<List<String>> fetchTechnologiesForProjectAsyncForHome(String projectId) {
        return db.collection(COLLECTION_PROJECT_TECHNOLOGIES)
                .whereEqualTo(FIELD_PROJECT_ID_IN_JOINS, projectId).get()
                .onSuccessTask(queryDocumentSnapshots -> {
                    if (queryDocumentSnapshots.isEmpty()) return Tasks.forResult(new ArrayList<>());
                    List<Task<DocumentSnapshot>> tasks = new ArrayList<>();
                    for(QueryDocumentSnapshot doc : queryDocumentSnapshots) {
                        String techId = doc.getString(FIELD_TECHNOLOGY_ID_IN_JOINS);
                        if (techId != null) tasks.add(db.collection(COLLECTION_TECHNOLOGIES).document(techId).get());
                    }
                    return Tasks.whenAllSuccess(tasks);
                }).continueWith(task -> {
                    List<String> names = new ArrayList<>();
                    if (task.isSuccessful() && task.getResult() != null) {
                        List<Object> snapshots = task.getResult();
                        for(Object obj : snapshots) {
                            if (obj instanceof DocumentSnapshot) {
                                DocumentSnapshot snap = (DocumentSnapshot) obj;
                                if (snap.exists()) names.add(snap.getString(FIELD_NAME_IN_CAT_TECH));
                            }
                        }
                    }
                    return names;
                });
    }
}