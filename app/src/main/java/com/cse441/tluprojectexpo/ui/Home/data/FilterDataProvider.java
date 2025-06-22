package com.cse441.tluprojectexpo.ui.Home.data;

import android.util.Log;

import com.cse441.tluprojectexpo.ui.Home.data.model.CategoryFilterItem;
import com.cse441.tluprojectexpo.ui.Home.data.model.TechnologyFilterItem;
import com.cse441.tluprojectexpo.util.Constants; // Sử dụng Constants
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.ArrayList;
import java.util.List;

public class FilterDataProvider {
    private static final String TAG = "FilterDataProvider";
    private FirebaseFirestore db;

    public interface CategoriesLoadListener {
        void onCategoriesLoaded(List<CategoryFilterItem> categories);
        void onError(String message);
    }

    public interface TechnologiesLoadListener {
        void onTechnologiesLoaded(List<TechnologyFilterItem> technologies);
        void onError(String message);
    }

    public FilterDataProvider() {
        this.db = FirebaseFirestore.getInstance();
    }

    public void loadCategories(CategoriesLoadListener listener) {
        db.collection(Constants.COLLECTION_CATEGORIES)
                .orderBy(Constants.FIELD_NAME)
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    List<CategoryFilterItem> categories = new ArrayList<>();
                    for (QueryDocumentSnapshot doc : queryDocumentSnapshots) {
                        String id = doc.getId();
                        String name = doc.getString(Constants.FIELD_NAME);
                        if (id != null && name != null) {
                            categories.add(new CategoryFilterItem(id, name));
                        }
                    }
                    if (listener != null) listener.onCategoriesLoaded(categories);
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Error loading categories for filter", e);
                    if (listener != null) listener.onError("Lỗi tải danh mục: " + e.getMessage());
                });
    }

    public void loadTechnologies(TechnologiesLoadListener listener) {
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
                    if (listener != null) listener.onTechnologiesLoaded(technologies);
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Error loading technologies for filter", e);
                    if (listener != null) listener.onError("Lỗi tải công nghệ: " + e.getMessage());
                });
    }
}