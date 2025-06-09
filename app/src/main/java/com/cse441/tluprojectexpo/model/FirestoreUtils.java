package com.cse441.tluprojectexpo.model;

import android.util.Log;

import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.WriteBatch;

public class FirestoreUtils {

    private static final String TAG = "FirestoreUtils";

    /**
     * Xóa một field cụ thể khỏi tất cả các document trong một collection.
     * @param db                    Đối tượng FirebaseFirestore.
     * @param collectionName        Tên của collection.
     * @param fieldToDelete         Tên của field cần xóa.
     */
    public static void deleteFieldFromAllDocuments(FirebaseFirestore db, String collectionName, String fieldToDelete) {

        CollectionReference collectionRef = db.collection(collectionName);

        // 1. Lấy tất cả các document trong collection
        collectionRef.get().addOnCompleteListener(task -> {
            if (task.isSuccessful()) {
                if (task.getResult().isEmpty()) {
                    Log.d(TAG, "Collection '" + collectionName + "' rỗng hoặc không tồn tại.");
                    // Bạn có thể hiển thị Toast ở đây nếu muốn
                    // Toast.makeText(context, "Collection is empty.", Toast.LENGTH_SHORT).show();
                    return;
                }

                // 2. Tạo một WriteBatch để gộp các thao tác ghi
                WriteBatch batch = db.batch();

                // 3. Lặp qua tất cả các document
                for (QueryDocumentSnapshot document : task.getResult()) {
                    // Kiểm tra xem document có chứa field đó không (tùy chọn nhưng an toàn hơn)
                    if (document.contains(fieldToDelete)) {
                        Log.d(TAG, "Lên lịch xóa field '" + fieldToDelete + "' khỏi document: " + document.getId());
                        // 4. Thêm thao tác xóa field vào batch
                        batch.update(document.getReference(), fieldToDelete, FieldValue.delete());
                    }
                }

                // 5. Thực thi batch để áp dụng tất cả các thay đổi
                batch.commit().addOnSuccessListener(aVoid -> {
                    Log.d(TAG, "Thành công! Đã xóa field '" + fieldToDelete + "' khỏi collection '" + collectionName + "'.");
                    // Hiển thị thông báo thành công
                    // Toast.makeText(context, "Field deleted successfully!", Toast.LENGTH_SHORT).show();

                }).addOnFailureListener(e -> {
                    Log.e(TAG, "Lỗi khi thực thi batch xóa field.", e);
                    // Hiển thị thông báo thất bại
                    // Toast.makeText(context, "Failed to delete field.", Toast.LENGTH_SHORT).show();
                });

            } else {
                Log.e(TAG, "Lỗi khi lấy documents từ collection '" + collectionName + "'.", task.getException());
                // Hiển thị thông báo thất bại
                // Toast.makeText(context, "Error getting documents.", Toast.LENGTH_SHORT).show();
            }
        });
    }

}
