// ProjectCreationService.java
package com.cse441.tluprojectexpo.repository;

import android.util.Log;
import androidx.annotation.Nullable;
import com.cse441.tluprojectexpo.utils.Constants; // Đảm bảo import Constants
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.WriteBatch;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Dịch vụ xử lý việc tạo mới một dự án và các dữ liệu liên quan trong Firestore.
 * Sử dụng WriteBatch để đảm bảo tính toàn vẹn dữ liệu khi ghi nhiều bản ghi.
 */
public class ProjectCreationService {
    private static final String TAG = "ProjectCreationSvc";
    private FirebaseFirestore db; // Instance của FirebaseFirestore

    /**
     * Listener cho các sự kiện trong quá trình tạo dự án.
     * Được sử dụng để thông báo kết quả của việc tạo dự án cho lớp gọi.
     */
    public interface ProjectCreationListener {
        /**
         * Được gọi khi dự án và tất cả các dữ liệu liên quan (thành viên, lĩnh vực, công nghệ)
         * đã được tạo thành công trong Firestore.
         * @param newProjectId ID của dự án mới vừa được tạo.
         */
        void onProjectCreatedSuccessfully(String newProjectId);

        /**
         * Được gọi khi có lỗi nghiêm trọng xảy ra trong quá trình tạo dự án,
         * khiến cho việc tạo dự án không thành công.
         * @param errorMessage Mô tả lỗi.
         */
        void onProjectCreationFailed(String errorMessage);

        /**
         * Được gọi khi dự án chính đã được tạo thành công, nhưng có một số lỗi không nghiêm trọng
         * xảy ra trong quá trình lưu các dữ liệu phụ (ví dụ: một thành viên cụ thể không lưu được).
         * @param warningMessage Mô tả về lỗi phụ đã xảy ra.
         */
        void onSubTaskError(String warningMessage); // Cho lỗi không nghiêm trọng ở sub-collection
    }

    public ProjectCreationService() {
        db = FirebaseFirestore.getInstance(); // Khởi tạo FirebaseFirestore
    }

    /**
     * Tạo một dự án mới cùng với các thông tin liên quan như thành viên, lĩnh vực (categories), và công nghệ.
     *
     * @param projectData           Dữ liệu chính của dự án (Map<String, Object>).
     *                              Phải chứa các trường như Title, Description, Status, ThumbnailUrl, CreatorUserId, v.v.
     *                              Các trường ProjectUrl và DemoUrl cũng được mong đợi trong map này.
     * @param membersData           Danh sách thông tin thành viên (List<Map<String, Object>>).
     *                              Mỗi Map trong danh sách chứa {@link Constants#FIELD_USER_ID} và {@link Constants#FIELD_ROLE_IN_PROJECT}.
     * @param selectedCategoryIds   Danh sách các ID của lĩnh vực (categories) đã được chọn cho dự án.
     *                              Có thể null hoặc rỗng nếu không có lĩnh vực nào được chọn.
     * @param selectedTechnologyIds Danh sách các ID của công nghệ đã được chọn cho dự án.
     *                              Có thể null hoặc rỗng nếu không có công nghệ nào được chọn.
     * @param listener              Callback để nhận thông báo kết quả của quá trình tạo dự án.
     */
    public void createNewProject(
            Map<String, Object> projectData,
            List<Map<String, Object>> membersData,
            @Nullable List<String> selectedCategoryIds, // THAY ĐỔI: Nhận List<String> cho category IDs
            List<String> selectedTechnologyIds,
            ProjectCreationListener listener
    ) {
        // Kiểm tra dữ liệu đầu vào cơ bản
        if (projectData == null || projectData.isEmpty()) {
            if (listener != null) listener.onProjectCreationFailed("Dữ liệu dự án không hợp lệ hoặc rỗng.");
            return;
        }
        if (listener == null) {
            Log.e(TAG, "ProjectCreationListener không được null. Không thể báo cáo kết quả.");
            return;
        }

        // Lấy tham chiếu đến collection "Projects" và tạo một document mới với ID tự sinh
        CollectionReference projectsRef = db.collection(Constants.COLLECTION_PROJECTS);
        DocumentReference newProjectRef = projectsRef.document(); // Firestore tự tạo ID
        String newProjectId = newProjectRef.getId(); // Lấy ID của dự án mới

        // (Tùy chọn) Bạn có thể thêm ProjectId vào chính projectData nếu cấu trúc DB của bạn yêu cầu
        // projectData.put(Constants.FIELD_PROJECT_ID, newProjectId); // Thường thì ID của document đã là ProjectId

        WriteBatch batch = db.batch(); // Tạo một WriteBatch để thực hiện nhiều thao tác ghi một cách nguyên tử

        // 1. Thêm (set) document dự án chính vào batch
        batch.set(newProjectRef, projectData);
        Log.d(TAG, "Đã thêm dự án chính vào batch với ID: " + newProjectId);

        // 2. Thêm thông tin thành viên vào collection "ProjectMembers"
        if (membersData != null && !membersData.isEmpty()) {
            CollectionReference projectMembersRef = db.collection(Constants.COLLECTION_PROJECT_MEMBERS);
            for (Map<String, Object> member : membersData) {
                // Kiểm tra tính hợp lệ của dữ liệu thành viên
                if (member.get(Constants.FIELD_USER_ID) == null || member.get(Constants.FIELD_ROLE_IN_PROJECT) == null) {
                    Log.w(TAG, "Bỏ qua thành viên không hợp lệ (thiếu UserId hoặc RoleInProject): " + member.toString());
                    // (Tùy chọn) Gọi listener.onSubTaskError() nếu muốn thông báo về lỗi này
                    continue; // Bỏ qua thành viên này và tiếp tục với những người khác
                }
                // Tạo ID ngẫu nhiên cho document trong ProjectMembers để đảm bảo tính duy nhất
                DocumentReference newMemberDocRef = projectMembersRef.document("pm_" + UUID.randomUUID().toString().substring(0, 12));
                Map<String, Object> memberDocData = new HashMap<>();
                memberDocData.put(Constants.FIELD_PROJECT_ID, newProjectId); // Liên kết với dự án
                memberDocData.put(Constants.FIELD_USER_ID, member.get(Constants.FIELD_USER_ID));
                memberDocData.put(Constants.FIELD_ROLE_IN_PROJECT, member.get(Constants.FIELD_ROLE_IN_PROJECT));
                batch.set(newMemberDocRef, memberDocData); // Thêm thao tác set vào batch
                Log.d(TAG, "Đã thêm thành viên " + member.get(Constants.FIELD_USER_ID) + " vào batch cho dự án " + newProjectId);
            }
        }

        // 3. THAY ĐỔI: Thêm thông tin các lĩnh vực (categories) vào collection "ProjectCategories"
        // Mỗi lĩnh vực được chọn sẽ tạo một bản ghi liên kết riêng.
        if (selectedCategoryIds != null && !selectedCategoryIds.isEmpty()) {
            CollectionReference projectCategoriesRef = db.collection(Constants.COLLECTION_PROJECT_CATEGORIES);
            for (String categoryId : selectedCategoryIds) {
                if (categoryId == null || categoryId.isEmpty()) {
                    Log.w(TAG, "Bỏ qua CategoryId không hợp lệ.");
                    continue;
                }
                DocumentReference newProjectCategoryDocRef = projectCategoriesRef.document("pc_" + UUID.randomUUID().toString().substring(0, 12));
                Map<String, Object> pcData = new HashMap<>();
                pcData.put(Constants.FIELD_PROJECT_ID, newProjectId);
                pcData.put(Constants.FIELD_CATEGORY_ID, categoryId);
                batch.set(newProjectCategoryDocRef, pcData);
                Log.d(TAG, "Đã thêm lĩnh vực " + categoryId + " vào batch cho dự án " + newProjectId);
            }
        }
        // Lưu ý: Nếu bạn muốn lưu danh sách CategoryIds trực tiếp vào document "Projects" dưới dạng một mảng,
        // bạn cần đảm bảo `projectData` đã chứa trường đó trước khi gọi `batch.set(newProjectRef, projectData);` ở bước 1.
        // Ví dụ: `projectData.put("CategoryIds", selectedCategoryIds);`

        // 4. Thêm thông tin công nghệ vào collection "ProjectTechnologies"
        if (selectedTechnologyIds != null && !selectedTechnologyIds.isEmpty()) {
            CollectionReference projectTechRef = db.collection(Constants.COLLECTION_PROJECT_TECHNOLOGIES);
            for (String techId : selectedTechnologyIds) {
                if (techId == null || techId.isEmpty()) {
                    Log.w(TAG, "Bỏ qua TechnologyId không hợp lệ.");
                    continue;
                }
                DocumentReference newProjectTechDocRef = projectTechRef.document("pt_" + UUID.randomUUID().toString().substring(0, 12));
                Map<String, Object> ptData = new HashMap<>();
                ptData.put(Constants.FIELD_PROJECT_ID, newProjectId);
                ptData.put(Constants.FIELD_TECHNOLOGY_ID, techId);
                batch.set(newProjectTechDocRef, ptData);
                Log.d(TAG, "Đã thêm công nghệ " + techId + " vào batch cho dự án " + newProjectId);
            }
        }

        // Thực thi tất cả các thao tác ghi trong batch
        batch.commit()
                .addOnSuccessListener(aVoid -> {
                    Log.i(TAG, "Dự án và các dữ liệu liên quan đã được tạo thành công bằng batch. Project ID: " + newProjectId);
                    listener.onProjectCreatedSuccessfully(newProjectId); // Thông báo thành công

                    // Gửi notification mời thành viên (trừ người tạo)
                    String creatorUserId = (String) projectData.get(Constants.FIELD_CREATOR_USER_ID);
                    String creatorFullName = (String) projectData.get(Constants.FIELD_FULL_NAME);
                    String creatorAvatarUrl = (String) projectData.get(Constants.FIELD_AVATAR_URL);
                    String projectTitle = (String) projectData.get(Constants.FIELD_NAME); // hoặc "Title" nếu đúng key
                    if (membersData != null) {
                        for (Map<String, Object> member : membersData) {
                            String memberUserId = (String) member.get(Constants.FIELD_USER_ID);
                            String memberRole = (String) member.get(Constants.FIELD_ROLE_IN_PROJECT);
                            if (memberUserId != null && !memberUserId.equals(creatorUserId)) {
                                NotificationRepository notificationRepository = new NotificationRepository();
                                notificationRepository.createProjectInvitation(
                                    memberUserId, // recipientUserId
                                    creatorUserId, // actorUserId
                                    creatorFullName != null ? creatorFullName : "Người tạo dự án", // actorFullName
                                    creatorAvatarUrl != null ? creatorAvatarUrl : "", // actorAvatarUrl
                                    newProjectId, // targetProjectId
                                    projectTitle != null ? projectTitle : "Dự án mới", // targetProjectTitle
                                    memberRole != null ? memberRole : Constants.DEFAULT_MEMBER_ROLE, // invitationRole
                                    new NotificationRepository.NotificationActionListener() {
                                        @Override
                                        public void onSuccess() { }
                                        @Override
                                        public void onError(String errorMessage) { }
                                    }
                                );
                            }
                        }
                    }
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Lỗi khi tạo dự án bằng batch commit", e);
                    listener.onProjectCreationFailed("Tạo dự án thất bại: " + e.getMessage()); // Thông báo thất bại
                });
    }
}

