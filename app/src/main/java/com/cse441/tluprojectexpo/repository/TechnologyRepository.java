package com.cse441.tluprojectexpo.repository;


import android.util.Log;
import androidx.annotation.NonNull;

import com.cse441.tluprojectexpo.ui.Home.model.TechnologyFilterItem; // Sử dụng model đã tạo
import com.cse441.tluprojectexpo.utils.Constants;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.ArrayList;
import java.util.List;

public class TechnologyRepository {
    private static final String TAG = "TechnologyRepository";
    private FirebaseFirestore db;

    public interface TechnologiesLoadListener {
        void onTechnologiesLoaded(List<TechnologyFilterItem> technologies);
        void onError(String message);
    }

    public TechnologyRepository() {
        this.db = FirebaseFirestore.getInstance();
    }

    public void fetchAllTechnologies(@NonNull TechnologiesLoadListener listener) {
        db.collection(Constants.COLLECTION_TECHNOLOGIES)
                .orderBy(Constants.FIELD_NAME)
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    List<TechnologyFilterItem> technologies = new ArrayList<>();
                    for (QueryDocumentSnapshot doc : queryDocumentSnapshots) {
                        String id = doc.getId();
                        String name = doc.getString(Constants.FIELD_NAME);
                        if (id != null && name != null) {
                            technologies.add(new TechnologyFilterItem(id, name));
                        }
                    }
                    listener.onTechnologiesLoaded(technologies);
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Error loading all technologies", e);
                    listener.onError("Lỗi tải công nghệ: " + e.getMessage());
                });
    }
}
