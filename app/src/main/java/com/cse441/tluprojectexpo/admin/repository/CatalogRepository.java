package com.cse441.tluprojectexpo.admin.repository;

import androidx.annotation.NonNull;
import com.cse441.tluprojectexpo.model.Category;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.android.gms.tasks.Tasks;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.ArrayList;
import java.util.List;

public class CatalogRepository {

    public enum CatalogType {
        FIELD("Categories"),
        TECHNOLOGY("Technologies");

        private final String collectionName;

        CatalogType(String collectionName) {
            this.collectionName = collectionName;
        }

        public String getCollectionName() {
            return collectionName;
        }
    }

    private final FirebaseFirestore db = FirebaseFirestore.getInstance();

    // Interface chung để lắng nghe kết quả
    public interface CatalogDataListener {
        void onDataLoaded(List<Category> items);
        void onError(Exception e);
    }

    // Phương thức helper để lấy CollectionReference dựa trên loại
    private CollectionReference getCollection(CatalogType type) {
        return db.collection(type.getCollectionName());
    }

    /**
     * Lấy tất cả các item từ một collection được chỉ định bởi CatalogType.
     */
    public void getAllItems(CatalogType type, CatalogDataListener listener) {
        getCollection(type).orderBy("Name").get().addOnCompleteListener(task -> {
            if (task.isSuccessful()) {
                List<Category> itemList = new ArrayList<>();
                for (QueryDocumentSnapshot document : task.getResult()) {
                    Category item = document.toObject(Category.class);
                    item.setId(document.getId()); // Gán ID
                    itemList.add(item);
                }
                listener.onDataLoaded(itemList);
            } else {
                listener.onError(task.getException());
            }
        });
    }

    /**
     * Thêm một item mới vào collection được chỉ định.
     */
    public void addItem(CatalogType type, Category item, @NonNull OnCompleteListener<Void> listener) {
        getCollection(type).document().set(item).addOnCompleteListener(listener);
    }

    /**
     * Cập nhật một item đã có.
     */
    public void updateItem(CatalogType type, Category item, @NonNull OnCompleteListener<Void> listener) {
        if (item.getId() == null || item.getId().isEmpty()) {
            Task<Void> failedTask = Tasks.forException(new IllegalArgumentException("Item ID must not be null for update."));
            failedTask.addOnCompleteListener(listener);
            return;
        }
        getCollection(type).document(item.getId()).set(item).addOnCompleteListener(listener);
    }

    /**
     * Xóa một item dựa trên ID.
     */
    public void deleteItem(CatalogType type, String itemId, @NonNull OnCompleteListener<Void> listener) {
        if (itemId == null || itemId.isEmpty()) {
            Task<Void> failedTask = Tasks.forException(new IllegalArgumentException("Item ID must not be null for delete."));
            failedTask.addOnCompleteListener(listener);
            return;
        }
        getCollection(type).document(itemId).delete().addOnCompleteListener(listener);
    }
}