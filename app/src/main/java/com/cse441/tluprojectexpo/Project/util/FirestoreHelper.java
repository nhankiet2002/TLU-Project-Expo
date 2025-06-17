// Các hàm tương tác với Firestore.
package com.cse441.tluprojectexpo.Project.util;


import android.util.Log;
import com.google.android.gms.tasks.Task;
import com.google.android.gms.tasks.Tasks;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.QuerySnapshot;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

public class FirestoreHelper {

    private static final String TAG = "FirestoreHelper";
    private FirebaseFirestore db;

    public FirestoreHelper() {
        db = FirebaseFirestore.getInstance();
    }

    public interface FirestoreDataListener<T> {
        void onSuccess(T data);
        void onFailure(Exception e);
    }

    public interface CategoriesFetchListener {
        void onCategoriesFetched(List<String> categoryNames, Map<String, String> nameToIdMap);
        void onError(String errorMessage);
    }

    public void fetchCategories(CategoriesFetchListener listener) {
        db.collection("Categories").orderBy("Name").get()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        QuerySnapshot querySnapshot = task.getResult();
                        List<String> names = new ArrayList<>();
                        Map<String, String> nameToId = new HashMap<>();
                        if (querySnapshot != null && !querySnapshot.isEmpty()) {
                            for (QueryDocumentSnapshot document : querySnapshot) {
                                String name = document.getString("Name");
                                String id = document.getId();
                                if (name != null && !name.isEmpty() && id != null) {
                                    names.add(name);
                                    nameToId.put(name, id);
                                }
                            }
                        }
                        listener.onCategoriesFetched(names, nameToId);
                    } else {
                        Log.w(TAG, "Error fetching categories: ", task.getException());
                        listener.onError("Lỗi tải lĩnh vực: " + (task.getException() != null ? task.getException().getMessage() : "Unknown error"));
                    }
                });
    }


    public Task<Void> processTechnology(String projectId, String techName) {
        return db.collection("Technologies").whereEqualTo("Name", techName).limit(1).get()
                .continueWithTask(task -> {
                    if (!task.isSuccessful() || task.getResult() == null) {
                        Log.e(TAG, "Error finding technology: " + techName, task.getException());
                        return Tasks.forException(Objects.requireNonNull(task.getException(), "Task exception was null for finding tech"));
                    }
                    if (!task.getResult().isEmpty()) {
                        String techId = task.getResult().getDocuments().get(0).getId();
                        return addProjectTechnologyLink(projectId, techId, techName);
                    } else {
                        Map<String, Object> newTechData = new HashMap<>();
                        newTechData.put("Name", techName);
                        return db.collection("Technologies").add(newTechData)
                                .continueWithTask(creationTask -> {
                                    if (!creationTask.isSuccessful() || creationTask.getResult() == null) {
                                        Log.e(TAG, "Error creating new technology: " + techName, creationTask.getException());
                                        return Tasks.forException(Objects.requireNonNull(creationTask.getException(), "Task exception was null for creating tech"));
                                    }
                                    return addProjectTechnologyLink(projectId, creationTask.getResult().getId(), techName);
                                });
                    }
                });
    }

    public Task<Void> addProjectTechnologyLink(String projectId, String technologyId, String techNameForLog) {
        Map<String, Object> projectTechData = new HashMap<>();
        projectTechData.put("ProjectId", projectId);
        projectTechData.put("TechnologyId", technologyId);
        return db.collection("ProjectTechnologies").document(projectId + "_" + technologyId).set(projectTechData)
                .addOnSuccessListener(aVoid -> Log.d(TAG, "ProjectTechnology link added for: " + techNameForLog))
                .addOnFailureListener(e -> Log.e(TAG, "Error adding ProjectTechnology link for: " + techNameForLog, e));
    }

    // Thêm các hàm khác nếu cần (ví dụ: saveProject, saveProjectMembers, saveProjectCategories...)
}
