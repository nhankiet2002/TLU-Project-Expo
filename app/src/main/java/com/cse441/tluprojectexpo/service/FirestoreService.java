package com.cse441.tluprojectexpo.service;

import android.util.Log;
import androidx.annotation.NonNull;

// Giả sử model Comment của bạn nằm ở đây và tên là Comment
import com.cse441.tluprojectexpo.model.Comment;
import com.cse441.tluprojectexpo.model.Project;
import com.cse441.tluprojectexpo.model.User;
import com.cse441.tluprojectexpo.utils.Constants; // Sử dụng Constants của bạn
import com.google.android.gms.tasks.Task;
import com.google.android.gms.tasks.Tasks;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.ArrayList;
import java.util.HashMap; // Cần cho các hàm fetchCategories/Technologies gốc
import java.util.List;
import java.util.Map;

public class FirestoreService {

    private static final String TAG = "FirestoreService";
    private FirebaseFirestore db;

    public FirestoreService() {
        db = FirebaseFirestore.getInstance();
    }

    // --- INTERFACES CHO CreateProjectActivity (PHẢI GIỮ NGUYÊN CHỮ KÝ) ---
    public interface CategoriesFetchListener {
        void onCategoriesFetched(List<String> categoryNames, Map<String, String> nameToIdMap);
        void onError(String errorMessage);
    }

    public interface UserDetailsFetchListener { // Cũng được dùng bởi ProjectDetailActivity
        void onUserDetailsFetched(User user);
        void onUserNotFound();
        void onError(String errorMessage);
    }

    public interface TechnologyFetchListener {
        void onTechnologiesFetched(List<String> technologyNames, Map<String, String> technologyNameToIdMap);
        void onError(String errorMessage);
    }
    // --- KẾT THÚC INTERFACES CHO CreateProjectActivity ---


    // --- PHƯƠNG THỨC CHO CreateProjectActivity (PHẢI GIỮ NGUYÊN CHỮ KÝ VÀ LOGIC) ---
    public void fetchCategories(CategoriesFetchListener listener) {
        if (listener == null) {
            Log.e(TAG, "CategoriesFetchListener không được null.");
            return;
        }
        db.collection(Constants.COLLECTION_CATEGORIES).orderBy(Constants.FIELD_NAME).get()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        com.google.firebase.firestore.QuerySnapshot querySnapshot = task.getResult();
                        List<String> names = new ArrayList<>();
                        Map<String, String> nameToId = new HashMap<>();
                        if (querySnapshot != null && !querySnapshot.isEmpty()) {
                            for (QueryDocumentSnapshot document : querySnapshot) {
                                String name = document.getString(Constants.FIELD_NAME);
                                String id = document.getId();
                                if (name != null && !name.isEmpty() && id != null) {
                                    names.add(name);
                                    nameToId.put(name, id);
                                }
                            }
                        }
                        Log.d(TAG, "Categories fetched (for CreateProject): " + names.size());
                        listener.onCategoriesFetched(names, nameToId);
                    } else {
                        Log.w(TAG, "Lỗi khi lấy categories (for CreateProject): ", task.getException());
                        listener.onError("Lỗi tải lĩnh vực: " + (task.getException() != null ? task.getException().getMessage() : "Lỗi không xác định"));
                    }
                });
    }

    public void fetchUserDetails(String userId, UserDetailsFetchListener listener) { // Dùng chung
        if (userId == null || userId.isEmpty()) {
            if (listener != null) listener.onError("User ID không hợp lệ.");
            return;
        }
        if (listener == null) {
            Log.e(TAG, "UserDetailsFetchListener không được null.");
            return;
        }
        db.collection(Constants.COLLECTION_USERS).document(userId).get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) {
                        User user = documentSnapshot.toObject(User.class);
                        if (user != null) {
                            user.setUserId(documentSnapshot.getId());
                            Log.d(TAG, "User details fetched for: " + userId);
                            listener.onUserDetailsFetched(user);
                        } else {
                            Log.e(TAG, "Không thể phân tích đối tượng User cho: " + userId);
                            if (listener != null) listener.onError("Lỗi phân tích dữ liệu người dùng.");
                        }
                    } else {
                        Log.w(TAG, "Không tìm thấy document User với ID: " + userId);
                        if (listener != null) listener.onUserNotFound();
                    }
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Lỗi khi lấy chi tiết User cho ID: " + userId, e);
                    if (listener != null) listener.onError("Lỗi tải thông tin người dùng: " + e.getMessage());
                });
    }

    public void fetchTechnologies(TechnologyFetchListener listener) {
        if (listener == null) {
            Log.e(TAG, "TechnologyFetchListener không được null.");
            return;
        }
        db.collection(Constants.COLLECTION_TECHNOLOGIES)
                .orderBy(Constants.FIELD_NAME)
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    List<String> names = new ArrayList<>();
                    Map<String, String> nameToIdMap = new HashMap<>();
                    if (queryDocumentSnapshots.isEmpty()) {
                        Log.d(TAG, "Collection " + Constants.COLLECTION_TECHNOLOGIES + " rỗng hoặc không tìm thấy (for CreateProject).");
                    }
                    for (QueryDocumentSnapshot document : queryDocumentSnapshots) {
                        String name = document.getString(Constants.FIELD_NAME);
                        String id = document.getId();
                        if (name != null && !name.isEmpty() && id != null) {
                            names.add(name);
                            nameToIdMap.put(name, id);
                        } else {
                            Log.w(TAG, "Document " + document.getId() + " trong " + Constants.COLLECTION_TECHNOLOGIES + " có trường Name null hoặc rỗng, hoặc id null (for CreateProject).");
                        }
                    }
                    Log.d(TAG, "Technologies fetched (for CreateProject): " + names.size());
                    listener.onTechnologiesFetched(names, nameToIdMap);
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Lỗi khi lấy tên công nghệ (for CreateProject)", e);
                    listener.onError("Lỗi tải danh sách công nghệ: " + e.getMessage());
                });
    }
    // --- KẾT THÚC PHƯƠNG THỨC CHO CreateProjectActivity ---


    // --- CÁC LISTENER VÀ PHƯƠNG THỨC MỚI CHO PROJECT DETAIL ---
    // (Các interface và phương thức như ProjectDetailsFetchListener, fetchProjectDetails,
    // fetchCreatorDetails (sử dụng UserDetailsFetchListener đã có), fetchCourseDetails,
    // fetchCategoriesForProject, fetchTechnologiesForProject, fetchProjectMembers,
    // fetchProjectComments, checkIfUserUpvoted sẽ nằm ở đây, như đã cung cấp ở câu trả lời trước)
    public interface ProjectDetailsFetchListener {
        void onProjectFetched(Project project);
        void onProjectNotFound();
        void onError(String errorMessage);
    }

    public interface CourseDetailsListener {
        void onCourseFetched(String courseName);
        void onCourseNotFound();
        void onError(String errorMessage);
    }

    public interface ProjectRelatedListListener<T> {
        void onListFetched(List<T> items);
        void onListEmpty();
        void onError(String errorMessage);
    }

    public interface UpvoteStatusListener {
        void onStatusChecked(boolean hasVoted);
    }

    public void fetchProjectDetails(String projectId, ProjectDetailsFetchListener listener) {
        if (projectId == null || projectId.isEmpty()) {
            if (listener != null) listener.onError("Project ID không hợp lệ.");
            return;
        }
        db.collection(Constants.COLLECTION_PROJECTS).document(projectId).get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) {
                        Project project = documentSnapshot.toObject(Project.class);
                        if (project != null) {
                            project.setProjectId(documentSnapshot.getId());
                            if (listener != null) listener.onProjectFetched(project);
                        } else {
                            if (listener != null) listener.onError("Lỗi khi đọc dữ liệu dự án.");
                        }
                    } else {
                        if (listener != null) listener.onProjectNotFound();
                    }
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Lỗi tải dự án: " + projectId, e);
                    if (listener != null) listener.onError("Lỗi tải dự án: " + e.getMessage());
                });
    }

    // fetchCreatorDetails sẽ dùng chung UserDetailsFetchListener
    // public void fetchCreatorDetails(String creatorId, UserDetailsFetchListener listener) { ... } // Đã có ở trên là fetchUserDetails


    public void fetchCourseDetails(String courseId, CourseDetailsListener listener) {
        if (courseId == null || courseId.isEmpty()) {
            if (listener != null) listener.onCourseNotFound();
            return;
        }
        db.collection(Constants.COLLECTION_COURSES).document(courseId).get()
                .addOnSuccessListener(courseDoc -> {
                    if (courseDoc.exists() && courseDoc.getString(Constants.FIELD_COURSE_NAME) != null) {
                        if (listener != null) listener.onCourseFetched(courseDoc.getString(Constants.FIELD_COURSE_NAME));
                    } else {
                        if (listener != null) listener.onCourseNotFound();
                    }
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Lỗi tải môn học: " + courseId, e);
                    if (listener != null) listener.onError("Lỗi tải môn học: " + e.getMessage());
                });
    }

    public void fetchCategoriesForProject(String projectId, ProjectRelatedListListener<String> listener) {
        db.collection(Constants.COLLECTION_PROJECT_CATEGORIES)
                .whereEqualTo(Constants.FIELD_PROJECT_ID, projectId)
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    if (queryDocumentSnapshots.isEmpty()) {
                        if (listener != null) listener.onListEmpty();
                        return;
                    }
                    List<Task<DocumentSnapshot>> categoryTasks = new ArrayList<>();
                    for (QueryDocumentSnapshot doc : queryDocumentSnapshots) {
                        String categoryId = doc.getString(Constants.FIELD_CATEGORY_ID);
                        if (categoryId != null) {
                            categoryTasks.add(db.collection(Constants.COLLECTION_CATEGORIES).document(categoryId).get());
                        }
                    }
                    if (categoryTasks.isEmpty()) {
                        if (listener != null) listener.onListEmpty(); return;
                    }
                    Tasks.whenAllSuccess(categoryTasks).addOnSuccessListener(list -> {
                        List<String> categoryNames = new ArrayList<>();
                        for (Object snapshot : list) {
                            DocumentSnapshot catSnap = (DocumentSnapshot) snapshot;
                            if (catSnap.exists() && catSnap.getString(Constants.FIELD_NAME) != null) {
                                categoryNames.add(catSnap.getString(Constants.FIELD_NAME));
                            }
                        }
                        if (listener != null) listener.onListFetched(categoryNames);
                    }).addOnFailureListener(e -> {
                        if (listener != null) listener.onError("Lỗi lấy chi tiết lĩnh vực: " + e.getMessage());
                    });
                }).addOnFailureListener(e -> {
                    Log.e(TAG, "Lỗi tải lĩnh vực của dự án: " + projectId, e);
                    if (listener != null) listener.onError("Lỗi tải lĩnh vực của dự án: " + e.getMessage());
                });
    }

    public void fetchTechnologiesForProject(String projectId, ProjectRelatedListListener<String> listener) {
        db.collection(Constants.COLLECTION_PROJECT_TECHNOLOGIES)
                .whereEqualTo(Constants.FIELD_PROJECT_ID, projectId)
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    if (queryDocumentSnapshots.isEmpty()) {
                        if (listener != null) listener.onListEmpty();
                        return;
                    }
                    List<Task<DocumentSnapshot>> techTasks = new ArrayList<>();
                    for (QueryDocumentSnapshot doc : queryDocumentSnapshots) {
                        String technologyId = doc.getString(Constants.FIELD_TECHNOLOGY_ID);
                        if (technologyId != null) {
                            techTasks.add(db.collection(Constants.COLLECTION_TECHNOLOGIES).document(technologyId).get());
                        }
                    }
                    if (techTasks.isEmpty()) {
                        if (listener != null) listener.onListEmpty(); return;
                    }
                    Tasks.whenAllSuccess(techTasks).addOnSuccessListener(list -> {
                        List<String> techNames = new ArrayList<>();
                        for (Object snapshot : list) {
                            DocumentSnapshot techSnap = (DocumentSnapshot) snapshot;
                            if (techSnap.exists() && techSnap.getString(Constants.FIELD_NAME) != null) {
                                techNames.add(techSnap.getString(Constants.FIELD_NAME));
                            }
                        }
                        if (listener != null) listener.onListFetched(techNames);
                    }).addOnFailureListener(e -> {
                        if (listener != null) listener.onError("Lỗi lấy chi tiết công nghệ: " + e.getMessage());
                    });
                }).addOnFailureListener(e -> {
                    Log.e(TAG, "Lỗi tải công nghệ của dự án: " + projectId, e);
                    if (listener != null) listener.onError("Lỗi tải công nghệ của dự án: " + e.getMessage());
                });
    }

    public void fetchProjectMembers(String projectId, ProjectRelatedListListener<Project.UserShortInfo> listener) {
        db.collection(Constants.COLLECTION_PROJECT_MEMBERS)
                .whereEqualTo(Constants.FIELD_PROJECT_ID, projectId)
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    if (queryDocumentSnapshots.isEmpty()) {
                        if (listener != null) listener.onListEmpty();
                        return;
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
                    if (userTasks.isEmpty()) {
                        if (listener != null) listener.onListEmpty(); return;
                    }
                    Tasks.whenAllSuccess(userTasks).addOnSuccessListener(list -> {
                        List<Project.UserShortInfo> memberInfos = new ArrayList<>();
                        for (int i = 0; i < list.size(); i++) {
                            DocumentSnapshot userSnap = (DocumentSnapshot) list.get(i);
                            if (userSnap.exists()) {
                                User user = userSnap.toObject(User.class);
                                if (user != null) {
                                    memberInfos.add(new Project.UserShortInfo(
                                            userSnap.getId(),
                                            user.getFullName(), // Giả sử User model có getFullName()
                                            user.getAvatarUrl(), // Giả sử User model có getAvatarUrl()
                                            roles.get(i)
                                    ));
                                }
                            }
                        }
                        if (listener != null) listener.onListFetched(memberInfos);
                    }).addOnFailureListener(e -> {
                        if (listener != null) listener.onError("Lỗi lấy chi tiết thành viên: " + e.getMessage());
                    });
                }).addOnFailureListener(e -> {
                    Log.e(TAG, "Lỗi tải thành viên dự án: " + projectId, e);
                    if (listener != null) listener.onError("Lỗi tải thành viên dự án: " + e.getMessage());
                });
    }

    public void fetchProjectComments(String projectId, ProjectRelatedListListener<Comment> listener) {
        db.collection(Constants.COLLECTION_COMMENTS)
                .whereEqualTo(Constants.FIELD_PROJECT_ID, projectId)
                .orderBy(Constants.FIELD_CREATED_AT, Query.Direction.DESCENDING)
                .limit(50)
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    if (queryDocumentSnapshots.isEmpty()) {
                        if (listener != null) listener.onListEmpty();
                        return;
                    }
                    List<Comment> fetchedComments = new ArrayList<>();
                    List<Task<Void>> userLoadTasks = new ArrayList<>();

                    for (QueryDocumentSnapshot doc : queryDocumentSnapshots) {
                        Comment comment = doc.toObject(Comment.class); // Sử dụng model Comment của bạn
                        if (comment == null) continue;

                        // Giả sử model Comment có các setter này
                        comment.setCommentId(doc.getId());
                        fetchedComments.add(comment);

                        // Giả sử model Comment có getUserId() trả về AuthorUserId
                        if (comment.getUserId() != null && !comment.getUserId().isEmpty()) {
                            Task<Void> userTask = db.collection(Constants.COLLECTION_USERS).document(comment.getUserId()).get()
                                    .continueWithTask(task -> {
                                        if (task.isSuccessful()) {
                                            DocumentSnapshot userDoc = task.getResult();
                                            if (userDoc != null && userDoc.exists()) {
                                                User user = userDoc.toObject(User.class);
                                                if (user != null) {
                                                    comment.setUserName(user.getFullName());
                                                    comment.setUserAvatarUrl(user.getAvatarUrl());
                                                }
                                            }
                                        } else {
                                            Log.e(TAG, "Lỗi tải user cho comment: " + comment.getUserId(), task.getException());
                                        }
                                        return Tasks.forResult(null);
                                    });
                            userLoadTasks.add(userTask);
                        }
                    }
                    Tasks.whenAllComplete(userLoadTasks).addOnCompleteListener(allTasks -> {
                        if (listener != null) listener.onListFetched(fetchedComments);
                    });
                }).addOnFailureListener(e -> {
                    Log.e(TAG, "Lỗi tải bình luận cho dự án: " + projectId, e);
                    if (listener != null) listener.onError("Lỗi tải bình luận: " + e.getMessage());
                });
    }

    public void checkIfUserUpvoted(String projectId, String userId, @NonNull UpvoteStatusListener listener) {
        if (projectId == null || userId == null) {
            listener.onStatusChecked(false);
            return;
        }
        db.collection(Constants.COLLECTION_PROJECT_VOTES).document(projectId)
                .collection(Constants.SUB_COLLECTION_VOTERS).document(userId)
                .get()
                .addOnSuccessListener(documentSnapshot -> listener.onStatusChecked(documentSnapshot.exists()))
                .addOnFailureListener(e -> {
                    Log.w(TAG, "Lỗi kiểm tra trạng thái upvote cho dự án: " + projectId, e);
                    listener.onStatusChecked(false);
                });
    }
    // --- KẾT THÚC CÁC PHƯƠNG THỨC MỚI ---
}