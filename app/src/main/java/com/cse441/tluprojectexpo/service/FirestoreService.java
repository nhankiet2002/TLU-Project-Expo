package com.cse441.tluprojectexpo.service;

import android.util.Log;
import androidx.annotation.NonNull; // Thêm import này
import androidx.annotation.Nullable;

import com.cse441.tluprojectexpo.model.User;
import com.cse441.tluprojectexpo.util.Constants;
import com.google.android.gms.tasks.Continuation; // Thêm import này
import com.google.android.gms.tasks.Task;
import com.google.android.gms.tasks.Tasks;
import com.google.firebase.firestore.DocumentReference; // Thêm import này
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.QuerySnapshot;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

public class FirestoreService {

    private static final String TAG = "FirestoreService";
    private FirebaseFirestore db;

    public FirestoreService() {
        db = FirebaseFirestore.getInstance();
    }

    public interface CategoriesFetchListener {
        void onCategoriesFetched(List<String> categoryNames, Map<String, String> nameToIdMap);
        void onError(String errorMessage);
    }

    public interface UserDetailsFetchListener {
        void onUserDetailsFetched(User user);
        void onUserNotFound();
        void onError(String errorMessage);
    }

    public interface TechnologyFetchListener {
        void onTechnologiesFetched(List<String> technologyNames, Map<String, String> technologyNameToIdMap);
        void onError(String errorMessage);
    }

    public void fetchCategories(CategoriesFetchListener listener) {
        // ... (Giữ nguyên)
        if (listener == null) {
            Log.e(TAG, "CategoriesFetchListener không được null.");
            return;
        }
        db.collection(Constants.COLLECTION_CATEGORIES).orderBy(Constants.FIELD_NAME).get()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        QuerySnapshot querySnapshot = task.getResult();
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
                        Log.d(TAG, "Categories fetched: " + names.size());
                        listener.onCategoriesFetched(names, nameToId);
                    } else {
                        Log.w(TAG, "Lỗi khi lấy categories: ", task.getException());
                        listener.onError("Lỗi tải lĩnh vực: " + (task.getException() != null ? task.getException().getMessage() : "Lỗi không xác định"));
                    }
                });
    }

    public void fetchUserDetails(String userId, UserDetailsFetchListener listener) {
        // ... (Giữ nguyên)
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
                            listener.onError("Lỗi phân tích dữ liệu người dùng.");
                        }
                    } else {
                        Log.w(TAG, "Không tìm thấy document User với ID: " + userId);
                        listener.onUserNotFound();
                    }
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Lỗi khi lấy chi tiết User cho ID: " + userId, e);
                    listener.onError("Lỗi tải thông tin người dùng: " + e.getMessage());
                });
    }

    public void fetchTechnologies(TechnologyFetchListener listener) {
        // ... (Giữ nguyên)
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
                        Log.d(TAG, "Collection " + Constants.COLLECTION_TECHNOLOGIES + " rỗng hoặc không tìm thấy.");
                    }
                    for (QueryDocumentSnapshot document : queryDocumentSnapshots) {
                        String name = document.getString(Constants.FIELD_NAME);
                        String id = document.getId();
                        if (name != null && !name.isEmpty() && id != null) {
                            names.add(name);
                            nameToIdMap.put(name, id);
                        } else {
                            Log.w(TAG, "Document " + document.getId() + " trong " + Constants.COLLECTION_TECHNOLOGIES + " có trường Name null hoặc rỗng, hoặc id null.");
                        }
                    }
                    Log.d(TAG, "Technologies fetched: " + names.size() + ". Names: " + names.toString());
                    listener.onTechnologiesFetched(names, nameToIdMap);
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Lỗi khi lấy tên công nghệ", e);
                    listener.onError("Lỗi tải danh sách công nghệ: " + e.getMessage());
                });
    }

    /**
     * Xử lý một công nghệ: tìm kiếm theo tên, nếu không tồn tại thì tạo mới.
     * Sau đó, liên kết công nghệ này với một dự án.
     *
     * @param projectId ID của dự án.
     * @param techName Tên của công nghệ.
     * @return Task<String> trả về ID của công nghệ (dù là tìm thấy hay mới tạo) nếu thành công,
     *         hoặc Task thất bại nếu có lỗi.
     */
    public Task<String> findOrCreateTechnologyAndLinkToProject(String projectId, String techName) {
        // Bước 1: Tìm kiếm công nghệ theo tên
        return db.collection(Constants.COLLECTION_TECHNOLOGIES)
                .whereEqualTo(Constants.FIELD_NAME, techName)
                .limit(1)
                .get()
                .continueWithTask(new Continuation<QuerySnapshot, Task<String>>() {
                    @Override
                    public Task<String> then(@NonNull Task<QuerySnapshot> task) throws Exception {
                        if (!task.isSuccessful() || task.getResult() == null) {
                            Log.e(TAG, "Lỗi khi tìm công nghệ: " + techName, task.getException());
                            throw Objects.requireNonNull(task.getException(), "Lỗi tìm công nghệ: task exception null");
                        }

                        if (!task.getResult().isEmpty()) {
                            // Công nghệ đã tồn tại
                            String techId = task.getResult().getDocuments().get(0).getId();
                            Log.d(TAG, "Công nghệ '" + techName + "' đã tồn tại với ID: " + techId);
                            // Bước 2a: Liên kết công nghệ đã có với dự án và trả về techId
                            return addProjectTechnologyLink(projectId, techId, techName)
                                    .continueWithTask(new Continuation<Void, Task<String>>() {
                                        @Override
                                        public Task<String> then(@NonNull Task<Void> linkTask) throws Exception {
                                            if (!linkTask.isSuccessful()) {
                                                throw Objects.requireNonNull(linkTask.getException(), "Lỗi liên kết công nghệ đã có: linkTask exception null");
                                            }
                                            return Tasks.forResult(techId); // Thành công, trả về techId
                                        }
                                    });
                        } else {
                            // Công nghệ chưa tồn tại, tạo mới
                            Log.d(TAG, "Công nghệ '" + techName + "' chưa tồn tại, đang tạo mới...");
                            Map<String, Object> newTechData = new HashMap<>();
                            newTechData.put(Constants.FIELD_NAME, techName);
                            return db.collection(Constants.COLLECTION_TECHNOLOGIES).add(newTechData)
                                    .continueWithTask(new Continuation<DocumentReference, Task<String>>() {
                                        @Override
                                        public Task<String> then(@NonNull Task<DocumentReference> creationTask) throws Exception {
                                            if (!creationTask.isSuccessful() || creationTask.getResult() == null) {
                                                Log.e(TAG, "Lỗi khi tạo công nghệ mới: " + techName, creationTask.getException());
                                                throw Objects.requireNonNull(creationTask.getException(), "Lỗi tạo công nghệ mới: creationTask exception null");
                                            }
                                            String newTechId = creationTask.getResult().getId();
                                            Log.d(TAG, "Đã tạo công nghệ mới '" + techName + "' với ID: " + newTechId);
                                            // Bước 2b: Liên kết công nghệ mới tạo với dự án và trả về newTechId
                                            return addProjectTechnologyLink(projectId, newTechId, techName)
                                                    .continueWithTask(new Continuation<Void, Task<String>>() {
                                                        @Override
                                                        public Task<String> then(@NonNull Task<Void> linkTask) throws Exception {
                                                            if (!linkTask.isSuccessful()) {
                                                                throw Objects.requireNonNull(linkTask.getException(), "Lỗi liên kết công nghệ mới: linkTask exception null");
                                                            }
                                                            return Tasks.forResult(newTechId); // Thành công, trả về newTechId
                                                        }
                                                    });
                                        }
                                    });
                        }
                    }
                });
    }


    /**
     * Tạo một bản ghi liên kết giữa một dự án và một công nghệ trong collection "ProjectTechnologies".
     *
     * @param projectId ID của dự án.
     * @param technologyId ID của công nghệ.
     * @param techNameForLog Tên công nghệ (chỉ dùng để log).
     * @return Task<Void> cho biết thao tác set dữ liệu đã hoàn thành (hoặc thất bại).
     */
    private Task<Void> addProjectTechnologyLink(String projectId, String technologyId, String techNameForLog) {
        Map<String, Object> projectTechData = new HashMap<>();
        projectTechData.put(Constants.FIELD_PROJECT_ID, projectId);
        projectTechData.put(Constants.FIELD_TECHNOLOGY_ID, technologyId);

        // Tạo ID document duy nhất cho liên kết, ví dụ: "projectId_technologyId"
        // Điều này giúp tránh việc tạo nhiều liên kết trùng lặp cho cùng một cặp project-technology
        // và dễ dàng kiểm tra hoặc cập nhật nếu cần.
        String documentId = projectId + "_" + technologyId;
        DocumentReference docRef = db.collection(Constants.COLLECTION_PROJECT_TECHNOLOGIES).document(documentId);

        return docRef.set(projectTechData) // Sử dụng set() để ghi đè nếu document đã tồn tại (hoặc tạo mới nếu chưa)
                .addOnSuccessListener(aVoid -> Log.d(TAG, "Đã thêm/cập nhật liên kết ProjectTechnology cho dự án " + projectId + " và công nghệ " + techNameForLog + " (ID: " + technologyId + ") với docID: " + documentId))
                .addOnFailureListener(e -> Log.e(TAG, "Lỗi khi thêm/cập nhật liên kết ProjectTechnology cho: " + techNameForLog + " với docID: " + documentId, e));
    }
}