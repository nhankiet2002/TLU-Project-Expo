package com.cse441.tluprojectexpo.ui.Home.data;

import android.util.Log;
import androidx.annotation.Nullable;

import com.cse441.tluprojectexpo.model.Project;
import com.cse441.tluprojectexpo.model.User;
import com.cse441.tluprojectexpo.utils.Constants; // Sử dụng Constants
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

public class ProjectListFetcher {
    private static final String TAG = "ProjectListFetcher";
    private static final int PROJECTS_PER_PAGE = 10;

    private FirebaseFirestore db;
    private CollectionReference projectsRef;
    private CollectionReference usersRef;
    private CollectionReference techRef;
    private CollectionReference projectTechRef;
    private CollectionReference categoriesRef;
    private CollectionReference projectCategoriesRef;

    private DocumentSnapshot lastVisibleDocument = null;
    private boolean isLastPage = false;

    public interface ProjectsFetchListener {
        void onProjectsFetched(List<Project> projects, boolean isLastPage, @Nullable DocumentSnapshot newLastVisible);
        void onFetchFailed(String errorMessage);
        void onNoProjectsFound(); // Khi không có dự án nào khớp
    }

    public ProjectListFetcher() {
        this.db = FirebaseFirestore.getInstance();
        this.projectsRef = db.collection(Constants.COLLECTION_PROJECTS);
        this.usersRef = db.collection(Constants.COLLECTION_USERS);
        this.techRef = db.collection(Constants.COLLECTION_TECHNOLOGIES);
        this.projectTechRef = db.collection(Constants.COLLECTION_PROJECT_TECHNOLOGIES);
        this.categoriesRef = db.collection(Constants.COLLECTION_CATEGORIES);
        this.projectCategoriesRef = db.collection(Constants.COLLECTION_PROJECT_CATEGORIES);
    }

    public void resetPagination() {
        lastVisibleDocument = null;
        isLastPage = false;
    }

    public boolean isLastPage() {
        return isLastPage;
    }

    private Task<List<String>> getProjectIdsByJoinTable(@Nullable String filterId, CollectionReference joinTableRef, String idFieldInJoinTable) {
        if (filterId == null || joinTableRef == null) {
            return Tasks.forResult(null); // Trả về null nếu không có filterId, để phân biệt với trường hợp có filterId nhưng không tìm thấy
        }
        return joinTableRef.whereEqualTo(idFieldInJoinTable, filterId)
                .get()
                .continueWith(task -> {
                    List<String> projectIds = new ArrayList<>();
                    if (task.isSuccessful() && task.getResult() != null) {
                        for (QueryDocumentSnapshot doc : task.getResult()) {
                            String projectId = doc.getString(Constants.FIELD_PROJECT_ID);
                            if (projectId != null) projectIds.add(projectId);
                        }
                    } else {
                        Log.w(TAG, "Error fetching project IDs from join table for " + idFieldInJoinTable, task.getException());
                    }
                    // Quan trọng: Nếu filter được áp dụng và không tìm thấy project nào, trả về list chứa marker
                    if (projectIds.isEmpty()){
                        projectIds.add("NO_PROJECTS_MATCH_THIS_FILTER"); // Marker đặc biệt
                    }
                    return projectIds;
                });
    }


    public void fetchProjects(String searchQuery, String sortField, Query.Direction sortDirection,
                              @Nullable String categoryId, @Nullable String technologyId, @Nullable String status,
                              boolean isInitialLoad, ProjectsFetchListener listener) {

        if (isInitialLoad) {
            resetPagination();
        }

        if (isLastPage && !isInitialLoad) {
            if (listener != null) listener.onProjectsFetched(new ArrayList<>(), true, lastVisibleDocument); // Không còn trang để load
            return;
        }

        Task<List<String>> categoryProjectIdsTask = getProjectIdsByJoinTable(categoryId, projectCategoriesRef, Constants.FIELD_CATEGORY_ID);
        Task<List<String>> technologyProjectIdsTask = getProjectIdsByJoinTable(technologyId, projectTechRef, Constants.FIELD_TECHNOLOGY_ID);

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
                    if (finalFilteredProjectIds.isEmpty()) { // Không có project nào chung giữa 2 bộ lọc
                        finalFilteredProjectIds.add("NO_PROJECTS_MATCH_THIS_FILTER");
                    }
                }
            } else if (categoryFilterActive) {
                finalFilteredProjectIds = idsFromCategory; // idsFromCategory đã chứa marker nếu cần
            } else if (technologyFilterActive) {
                finalFilteredProjectIds = idsFromTechnology; // idsFromTechnology đã chứa marker nếu cần
            }
            // Nếu cả hai filter đều không active, finalFilteredProjectIds sẽ là null

            Query query = projectsRef.whereEqualTo("IsApproved", true);

            if (!searchQuery.isEmpty()) {
                query = query.orderBy("IsFeatured", Query.Direction.DESCENDING)
                        .orderBy(Constants.FIELD_PROJECT_ID) // Cần orderBy trước khi whereIn nếu có
                        .orderBy("Title")
                        .whereGreaterThanOrEqualTo("Title", searchQuery)
                        .whereLessThanOrEqualTo("Title", searchQuery + "\uf8ff");
            } else {
                query = query.orderBy("IsFeatured", Query.Direction.DESCENDING)
                        .orderBy(sortField, sortDirection);
                // Nếu sortField không phải là documentId, và bạn sắp dùng whereIn,
                // bạn có thể cần thêm .orderBy(FieldPath.documentId()) để đảm bảo thứ tự nhất quán cho pagination.
                // Tuy nhiên, nếu sortField đã đủ unique, thì có thể không cần.
                if (!sortField.equals(FieldPath.documentId().toString())) {
                    query = query.orderBy(FieldPath.documentId(), sortDirection); // Đảm bảo cursor ổn định
                }
            }


            if (status != null && !status.isEmpty()) {
                query = query.whereEqualTo("Status", status);
            }

            if (finalFilteredProjectIds != null && !finalFilteredProjectIds.isEmpty()) {
                if (finalFilteredProjectIds.contains("NO_PROJECTS_MATCH_THIS_FILTER")) {
                    if (listener != null) listener.onNoProjectsFound();
                    isLastPage = true; // Đánh dấu là trang cuối vì không có gì để load
                    return Tasks.forResult(null); // Trả về task null để dừng chuỗi
                }
                // Firestore giới hạn `whereIn` tới 30 phần tử mỗi lần query.
                // Nếu có nhiều hơn, bạn cần chia thành nhiều query hoặc tìm cách khác.
                // Hiện tại, đơn giản là lấy 30 ID đầu tiên.
                if (finalFilteredProjectIds.size() > 30) {
                    Log.w(TAG, "Lọc theo ID dự án vượt quá giới hạn 30, chỉ lấy 30 ID đầu tiên.");
                    query = query.whereIn(FieldPath.documentId(), finalFilteredProjectIds.subList(0, 30));
                } else {
                    query = query.whereIn(FieldPath.documentId(), finalFilteredProjectIds);
                }
            }


            if (!isInitialLoad && lastVisibleDocument != null) {
                query = query.startAfter(lastVisibleDocument);
            }
            query = query.limit(PROJECTS_PER_PAGE);
            return query.get();

        }).addOnCompleteListener(task -> {
            if (listener == null) return;

            if (task.isSuccessful() && task.getResult() != null) {
                QuerySnapshot querySnapshot = task.getResult();
                if (querySnapshot.isEmpty()) {
                    isLastPage = true;
                    if(isInitialLoad) listener.onNoProjectsFound();
                    else listener.onProjectsFetched(new ArrayList<>(), true, lastVisibleDocument);
                    return;
                }

                List<Project> fetchedProjects = new ArrayList<>();
                List<Task<Void>> tasksToCompleteDetails = new ArrayList<>();

                for (QueryDocumentSnapshot document : querySnapshot) {
                    Project project = document.toObject(Project.class);
                    project.setProjectId(document.getId());
                    fetchedProjects.add(project);

                    // Logic fetch creator, technologies, categories (tương tự như trong HomeFragment gốc)
                    // ... (đã được bao gồm trong HomeFragment gốc, bạn có thể chuyển nó vào đây) ...
                    if (usersRef != null && project.getCreatorUserId() != null && !project.getCreatorUserId().isEmpty()) {
                        Task<Void> userTask = usersRef.document(project.getCreatorUserId()).get()
                                .continueWith(userDocTask -> {
                                    if (userDocTask.isSuccessful() && userDocTask.getResult() != null && userDocTask.getResult().exists()) {
                                        User user = userDocTask.getResult().toObject(User.class);
                                        if (user != null) project.setCreatorFullName(user.getFullName());
                                    } else project.setCreatorFullName(null);
                                    return null;
                                });
                        tasksToCompleteDetails.add(userTask);
                    } else project.setCreatorFullName(null);

                    // Fetch Technologies
                    if (projectTechRef != null && techRef != null) {
                        Task<Void> techTask = projectTechRef.whereEqualTo(Constants.FIELD_PROJECT_ID, project.getProjectId()).get()
                                .continueWithTask(ptQueryTask -> {
                                    if (ptQueryTask.isSuccessful() && ptQueryTask.getResult() != null) {
                                        List<Task<DocumentSnapshot>> techNameTasks = new ArrayList<>();
                                        for (QueryDocumentSnapshot ptDoc : ptQueryTask.getResult()) {
                                            String techId = ptDoc.getString(Constants.FIELD_TECHNOLOGY_ID);
                                            if (techId != null) techNameTasks.add(techRef.document(techId).get());
                                        }
                                        return Tasks.whenAllSuccess(techNameTasks);
                                    } return Tasks.forResult(new ArrayList<>());
                                }).continueWith(techNameResultsTask -> {
                                    if (techNameResultsTask.isSuccessful() && techNameResultsTask.getResult() != null) {
                                        List<String> techNames = ((List<?>) techNameResultsTask.getResult()).stream()
                                                .filter(obj -> obj instanceof DocumentSnapshot)
                                                .map(obj -> (DocumentSnapshot) obj)
                                                .filter(DocumentSnapshot::exists)
                                                .map(techDoc -> techDoc.getString(Constants.FIELD_NAME))
                                                .filter(name -> name != null && !name.isEmpty())
                                                .collect(Collectors.toList());
                                        project.setTechnologyNames(techNames);
                                    } else project.setTechnologyNames(new ArrayList<>());
                                    return null;
                                });
                        tasksToCompleteDetails.add(techTask);
                    } else project.setTechnologyNames(new ArrayList<>());

                    // Fetch Categories
                    if (projectCategoriesRef != null && categoriesRef != null) {
                        Task<Void> categoryTask = projectCategoriesRef.whereEqualTo(Constants.FIELD_PROJECT_ID, project.getProjectId()).get()
                                .continueWithTask(pcQueryTask -> {
                                    if (pcQueryTask.isSuccessful() && pcQueryTask.getResult() != null) {
                                        List<Task<DocumentSnapshot>> catNameTasks = new ArrayList<>();
                                        for (QueryDocumentSnapshot pcDoc : pcQueryTask.getResult()) {
                                            String catId = pcDoc.getString(Constants.FIELD_CATEGORY_ID);
                                            if (catId != null) catNameTasks.add(categoriesRef.document(catId).get());
                                        }
                                        return Tasks.whenAllSuccess(catNameTasks);
                                    } return Tasks.forResult(new ArrayList<>());
                                }).continueWith(catNameResultsTask -> {
                                    if (catNameResultsTask.isSuccessful() && catNameResultsTask.getResult() != null) {
                                        List<String> catNames = ((List<?>) catNameResultsTask.getResult()).stream()
                                                .filter(obj -> obj instanceof DocumentSnapshot)
                                                .map(obj -> (DocumentSnapshot) obj)
                                                .filter(DocumentSnapshot::exists)
                                                .map(catDoc -> catDoc.getString(Constants.FIELD_NAME))
                                                .filter(name -> name != null && !name.isEmpty())
                                                .collect(Collectors.toList());
                                        project.setCategoryNames(catNames);
                                    } else project.setCategoryNames(new ArrayList<>());
                                    return null;
                                });
                        tasksToCompleteDetails.add(categoryTask);
                    } else project.setCategoryNames(new ArrayList<>());

                }

                Tasks.whenAll(tasksToCompleteDetails).addOnCompleteListener(allExtraTasks -> {
                    if (querySnapshot.size() < PROJECTS_PER_PAGE) {
                        isLastPage = true;
                    }
                    DocumentSnapshot newLastVisible = querySnapshot.isEmpty() ? lastVisibleDocument : querySnapshot.getDocuments().get(querySnapshot.size() - 1);
                    lastVisibleDocument = newLastVisible;
                    listener.onProjectsFetched(fetchedProjects, isLastPage, newLastVisible);
                });

            } else { // task not successful or result is null
                Log.e(TAG, "Error getting projects: ", task.getException());
                isLastPage = true; // Giả định là trang cuối khi có lỗi để tránh load vô hạn
                listener.onFetchFailed("Lỗi tải dự án: " + (task.getException() != null ? task.getException().getMessage() : "Unknown error"));
            }
        }).addOnFailureListener(e -> { // Failure from Tasks.whenAllSuccess (pre-fetching IDs)
            Log.e(TAG, "Error in pre-fetching IDs for filters or building query: ", e);
            if (listener != null) listener.onFetchFailed("Lỗi khi áp dụng bộ lọc hoặc xây dựng truy vấn.");
            isLastPage = true;
        });
    }
}