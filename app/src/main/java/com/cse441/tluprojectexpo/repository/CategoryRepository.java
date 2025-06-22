package com.cse441.tluprojectexpo.repository;

import android.util.Log;
import androidx.annotation.NonNull;

import com.cse441.tluprojectexpo.ui.Home.model.CategoryFilterItem; // Sử dụng model đã tạo
import com.cse441.tluprojectexpo.utils.Constants;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.ArrayList;
import java.util.List;

public class CategoryRepository {
    private static final String TAG = "CategoryRepository";
    private FirebaseFirestore db;

    public interface CategoriesLoadListener {
        void onCategoriesLoaded(List<CategoryFilterItem> categories);
        void onError(String message);
    }

    public CategoryRepository() {
        this.db = FirebaseFirestore.getInstance();
    }

    public void fetchAllCategories(@NonNull CategoriesLoadListener listener) {
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
                    listener.onCategoriesLoaded(categories);
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Error loading all categories", e);
                    listener.onError("Lỗi tải danh mục: " + e.getMessage());
                });
    }
}
