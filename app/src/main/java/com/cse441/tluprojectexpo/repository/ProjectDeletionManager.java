package com.cse441.tluprojectexpo.repository;

import com.google.android.gms.tasks.Task;
import com.google.android.gms.tasks.Tasks;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.QuerySnapshot;
import com.google.firebase.firestore.WriteBatch;

public class ProjectDeletionManager {

    private final FirebaseFirestore db;

    public interface DeleteCallback {
        void onSuccess();
        void onError(String errorMessage);
    }

    public ProjectDeletionManager() {
        this.db = FirebaseFirestore.getInstance();
    }

    public void deleteProject(String projectId, DeleteCallback callback) {
        if (projectId == null || projectId.isEmpty()) {
            if (callback != null) callback.onError("Project ID không hợp lệ.");
            return;
        }

        WriteBatch batch = db.batch();

        // 1. Delete the main project document
        batch.delete(db.collection("Projects").document(projectId));

        // Define tasks to find related documents to delete
        Task<QuerySnapshot> deleteMembersTask = db.collection("ProjectMembers").whereEqualTo("ProjectId", projectId).get();
        Task<QuerySnapshot> deleteCategoriesTask = db.collection("ProjectCategories").whereEqualTo("ProjectId", projectId).get();
        Task<QuerySnapshot> deleteTechnologiesTask = db.collection("ProjectTechnologies").whereEqualTo("ProjectId", projectId).get();
        Task<QuerySnapshot> deleteCommentsTask = db.collection("Comments").whereEqualTo("ProjectId", projectId).get();
        Task<QuerySnapshot> deleteVotesTask = db.collection("Votes").whereEqualTo("ProjectId", projectId).get();

        Tasks.whenAll(deleteMembersTask, deleteCategoriesTask, deleteTechnologiesTask, deleteCommentsTask, deleteVotesTask)
            .addOnSuccessListener(aVoid -> {
                try {
                    // Add deletion of related documents to the batch
                    for (QueryDocumentSnapshot doc : deleteMembersTask.getResult()) {
                        batch.delete(doc.getReference());
                    }
                    for (QueryDocumentSnapshot doc : deleteCategoriesTask.getResult()) {
                        batch.delete(doc.getReference());
                    }
                    for (QueryDocumentSnapshot doc : deleteTechnologiesTask.getResult()) {
                        batch.delete(doc.getReference());
                    }
                    for (QueryDocumentSnapshot doc : deleteCommentsTask.getResult()) {
                        batch.delete(doc.getReference());
                    }
                     for (QueryDocumentSnapshot doc : deleteVotesTask.getResult()) {
                        batch.delete(doc.getReference());
                    }

                    // All deletions are now in the batch, commit them
                    batch.commit()
                         .addOnSuccessListener(result -> {
                             if (callback != null) callback.onSuccess();
                         })
                         .addOnFailureListener(e -> {
                             if (callback != null) callback.onError("Lỗi khi thực hiện xóa hàng loạt: " + e.getMessage());
                         });
                } catch (Exception e) {
                     if (callback != null) callback.onError("Lỗi khi xử lý kết quả truy vấn xóa: " + e.getMessage());
                }
            })
            .addOnFailureListener(e -> {
                if (callback != null) callback.onError("Lỗi khi truy vấn dữ liệu liên quan để xóa: " + e.getMessage());
            });
    }
} 