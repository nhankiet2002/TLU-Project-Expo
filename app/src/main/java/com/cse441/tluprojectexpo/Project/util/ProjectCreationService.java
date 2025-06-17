package com.cse441.tluprojectexpo.Project.util;

import android.util.Log;

import androidx.annotation.Nullable;

import com.google.android.gms.tasks.Task;
import com.google.android.gms.tasks.Tasks;
import com.google.firebase.firestore.FirebaseFirestore;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Dịch vụ xử lý toàn bộ logic tạo một dự án mới và các dữ liệu liên quan trên Firestore.
 */
public class ProjectCreationService {
    private static final String TAG = "ProjectCreationSvc"; // Thay đổi TAG
    private FirebaseFirestore db;
    private FirestoreHelper firestoreHelper; // Để xử lý technology

    public ProjectCreationService() {
        this.db = FirebaseFirestore.getInstance();
        this.firestoreHelper = new FirestoreHelper(); // FirestoreHelper sẽ tự new FirebaseFirestore()
    }

    /**
     * Listener cho kết quả của quá trình tạo dự án.
     */
    public interface ProjectCreationListener {
        void onProjectCreatedSuccessfully(String newProjectId);
        void onProjectCreationFailed(String errorMessage);
        void onSubTaskError(String warningMessage); // Thông báo nếu project chính tạo OK nhưng sub-task lỗi
    }

    /**
     * Tạo một dự án mới và tất cả các document liên quan.
     *
     * @param projectData         Dữ liệu cho document Project chính (đã bao gồm ThumbnailUrl, ImageUrl, ProjectUrl, DemoUrl, VideoUrl).
     * @param membersData         Danh sách Map chứa UserId và RoleInProject.
     * @param categoryId          ID của Category (nếu có).
     * @param technologyNames     Danh sách tên các công nghệ.
     * @param listener            Callback để nhận kết quả.
     */
    public void createNewProject(Map<String, Object> projectData,
                                 List<Map<String, Object>> membersData,
                                 @Nullable String categoryId, // Có thể là null nếu không chọn category
                                 List<String> technologyNames,
                                 ProjectCreationListener listener) {

        if (listener == null) {
            Log.e(TAG, "ProjectCreationListener cannot be null.");
            return;
        }
        if (projectData == null || projectData.isEmpty()) {
            listener.onProjectCreationFailed("Dữ liệu dự án không hợp lệ.");
            return;
        }

        // Bước 1: Tạo document Project chính
        db.collection("Projects").add(projectData)
                .addOnSuccessListener(documentReference -> {
                    String newProjectId = documentReference.getId();
                    Log.d(TAG, "Project document created successfully with ID: " + newProjectId);

                    List<Task<?>> subTasks = new ArrayList<>();

                    // Bước 2: Tạo ProjectMembers
                    if (membersData != null) {
                        for (Map<String, Object> memberMap : membersData) {
                            if (memberMap.get("UserId") != null) {
                                Map<String, Object> finalMemberData = new HashMap<>(memberMap); // Tạo bản sao để thêm ProjectId
                                finalMemberData.put("ProjectId", newProjectId);
                                String memberDocId = newProjectId + "_" + memberMap.get("UserId");
                                subTasks.add(db.collection("ProjectMembers").document(memberDocId).set(finalMemberData));
                            }
                        }
                    }

                    // Bước 3: Tạo ProjectCategories
                    if (categoryId != null && !categoryId.isEmpty()) {
                        Map<String, Object> projectCategoryData = new HashMap<>();
                        projectCategoryData.put("ProjectId", newProjectId);
                        projectCategoryData.put("CategoryId", categoryId);
                        String projectCategoryDocId = newProjectId + "_" + categoryId;
                        subTasks.add(db.collection("ProjectCategories").document(projectCategoryDocId).set(projectCategoryData));
                    }

                    // Bước 4: Xử lý Technologies và tạo ProjectTechnologies
                    if (technologyNames != null) {
                        for (String techName : technologyNames) {
                            if (techName != null && !techName.trim().isEmpty()) {
                                // FirestoreHelper sẽ xử lý việc tìm hoặc tạo Technology, sau đó tạo link ProjectTechnology
                                subTasks.add(firestoreHelper.processTechnology(newProjectId, techName.trim()));
                            }
                        }
                    }

                    // Nếu không có sub-tasks, báo thành công ngay
                    if (subTasks.isEmpty()) {
                        listener.onProjectCreatedSuccessfully(newProjectId);
                        return;
                    }

                    // Đợi tất cả các sub-tasks hoàn thành
                    Tasks.whenAllComplete(subTasks)
                            .addOnCompleteListener(allTasksResult -> {
                                boolean allSubTasksSuccessful = true;
                                if (allTasksResult.getResult() != null) { // Kiểm tra getResult() null
                                    for (Task<?> task : allTasksResult.getResult()) {
                                        if (!task.isSuccessful()) {
                                            allSubTasksSuccessful = false;
                                            Log.e(TAG, "A sub-task failed during project creation: ", task.getException());
                                        }
                                    }
                                } else {
                                    allSubTasksSuccessful = false; // Coi như lỗi nếu không có kết quả
                                    Log.e(TAG, "allTasksResult.getResult() was null, implying sub-task failures or no tasks.");
                                }


                                if (allSubTasksSuccessful) {
                                    listener.onProjectCreatedSuccessfully(newProjectId);
                                } else {
                                    // Project chính đã được tạo, nhưng một số thông tin phụ có thể lỗi
                                    listener.onSubTaskError("Dự án đã được tạo, nhưng có lỗi khi lưu một số thông tin chi tiết (thành viên, lĩnh vực, hoặc công nghệ). Bạn có thể cần chỉnh sửa dự án sau.");
                                }
                            });

                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Failed to create main Project document", e);
                    listener.onProjectCreationFailed("Lỗi tạo tài liệu dự án chính: " + e.getMessage());
                });
    }
}
