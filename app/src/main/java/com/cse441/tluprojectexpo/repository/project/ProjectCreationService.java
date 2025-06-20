package com.cse441.tluprojectexpo.repository.project;


import android.util.Log;
import androidx.annotation.Nullable;

import com.cse441.tluprojectexpo.util.Constants; // Sử dụng hằng số
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.WriteBatch;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class ProjectCreationService {
    private static final String TAG = "ProjectCreationSvc";
    private FirebaseFirestore db;

    public interface ProjectCreationListener {
        void onProjectCreatedSuccessfully(String newProjectId);
        void onProjectCreationFailed(String errorMessage);
        void onSubTaskError(String warningMessage);
    }

    public ProjectCreationService() {
        db = FirebaseFirestore.getInstance();
    }

    public void createNewProject(
            Map<String, Object> projectData,
            List<Map<String, Object>> membersData,
            @Nullable String categoryId,
            List<String> selectedTechnologyIds,
            ProjectCreationListener listener
    ) {
        if (projectData == null || projectData.isEmpty()) {
            listener.onProjectCreationFailed("Dữ liệu dự án không hợp lệ.");
            return;
        }

        CollectionReference projectsRef = db.collection(Constants.COLLECTION_PROJECTS);
        DocumentReference newProjectRef = projectsRef.document();
        String newProjectId = newProjectRef.getId();
        // projectData.put(Constants.FIELD_PROJECT_ID, newProjectId); // ID của document đã là ProjectId

        WriteBatch batch = db.batch();
        batch.set(newProjectRef, projectData);
        Log.d(TAG, "Đã thêm dự án chính vào batch với ID: " + newProjectId);

        if (membersData != null && !membersData.isEmpty()) {
            CollectionReference projectMembersRef = db.collection(Constants.COLLECTION_PROJECT_MEMBERS);
            for (Map<String, Object> member : membersData) {
                if (member.get(Constants.FIELD_USER_ID) == null || member.get(Constants.FIELD_ROLE_IN_PROJECT) == null) {
                    Log.w(TAG, "Bỏ qua thành viên không hợp lệ: " + member.toString());
                    continue;
                }
                DocumentReference newMemberDocRef = projectMembersRef.document("pm_" + UUID.randomUUID().toString().substring(0,12));
                Map<String, Object> memberDocData = new HashMap<>();
                memberDocData.put(Constants.FIELD_PROJECT_ID, newProjectId);
                memberDocData.put(Constants.FIELD_USER_ID, member.get(Constants.FIELD_USER_ID));
                memberDocData.put(Constants.FIELD_ROLE_IN_PROJECT, member.get(Constants.FIELD_ROLE_IN_PROJECT));
                batch.set(newMemberDocRef, memberDocData);
                Log.d(TAG, "Đã thêm thành viên " + member.get(Constants.FIELD_USER_ID) + " vào batch cho dự án " + newProjectId);
            }
        }

        if (categoryId != null && !categoryId.isEmpty()) {
            CollectionReference projectCategoriesRef = db.collection(Constants.COLLECTION_PROJECT_CATEGORIES);
            DocumentReference newProjectCategoryDocRef = projectCategoriesRef.document("pc_" + UUID.randomUUID().toString().substring(0,12));
            Map<String, Object> pcData = new HashMap<>();
            pcData.put(Constants.FIELD_PROJECT_ID, newProjectId);
            pcData.put(Constants.FIELD_CATEGORY_ID, categoryId);
            batch.set(newProjectCategoryDocRef, pcData);
            Log.d(TAG, "Đã thêm category " + categoryId + " vào batch cho dự án " + newProjectId);
        }

        if (selectedTechnologyIds != null && !selectedTechnologyIds.isEmpty()) {
            CollectionReference projectTechRef = db.collection(Constants.COLLECTION_PROJECT_TECHNOLOGIES);
            for (String techId : selectedTechnologyIds) {
                if (techId == null || techId.isEmpty()) continue;
                DocumentReference newProjectTechDocRef = projectTechRef.document("pt_" + UUID.randomUUID().toString().substring(0,12));
                Map<String, Object> ptData = new HashMap<>();
                ptData.put(Constants.FIELD_PROJECT_ID, newProjectId);
                ptData.put(Constants.FIELD_TECHNOLOGY_ID, techId);
                batch.set(newProjectTechDocRef, ptData);
                Log.d(TAG, "Đã thêm công nghệ " + techId + " vào batch cho dự án " + newProjectId);
            }
        }

        batch.commit()
                .addOnSuccessListener(aVoid -> {
                    Log.i(TAG, "Dự án và các dữ liệu liên quan đã được tạo thành công. Project ID: " + newProjectId);
                    listener.onProjectCreatedSuccessfully(newProjectId);
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Lỗi khi tạo dự án bằng batch commit", e);
                    listener.onProjectCreationFailed("Tạo dự án thất bại: " + e.getMessage());
                });
    }
}
