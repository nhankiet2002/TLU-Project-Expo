package com.cse441.tluprojectexpo.model;

import android.content.Context;
import android.util.Log;

import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.WriteBatch;
import com.google.gson.Gson;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.List;

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



    /**
     * Import dữ liệu người dùng từ file JSON.
     * Firestore sẽ tự động tạo Document ID và trường 'createdAt' nhờ @ServerTimestamp.
     *
     * @param context       Context của ứng dụng.
     * @param db            Đối tượng FirebaseFirestore.
     * @param assetFileName Tên file JSON trong thư mục assets (ví dụ: "users.json").
     */
    public static void importUsersFromJson(Context context, FirebaseFirestore db, String assetFileName) {
        Gson gson = new Gson();
        User[] users;

        // Bước 1: Đọc và parse file JSON trực tiếp vào mảng User[]
        // Không cần model trung gian nữa!
        try (InputStream is = context.getAssets().open(assetFileName);
             InputStreamReader reader = new InputStreamReader(is, StandardCharsets.UTF_8)) {
            users = gson.fromJson(reader, User[].class);
        } catch (IOException e) {
            Log.e(TAG, "Lỗi khi đọc file JSON từ assets: " + assetFileName, e);
            return;
        }

        if (users == null || users.length == 0) {
            Log.w(TAG, "File JSON rỗng hoặc không thể parse.");
            return;
        }

        List<User> userList = Arrays.asList(users);
        CollectionReference collectionRef = db.collection("users");

        // Bước 2: Chia thành các batch để ghi
        int batchSize = 499;
        for (int i = 0; i < userList.size(); i += batchSize) {
            List<User> sublist = userList.subList(i, Math.min(i + batchSize, userList.size()));
            WriteBatch batch = db.batch();

            for (User user : sublist) {
                // Bước 3: Chỉ cần thêm đối tượng user vào batch
                // Firestore sẽ tự xử lý trường id và createdAt
                DocumentReference docRef = collectionRef.document();
                batch.set(docRef, user);
            }

            // Bước 4: Commit batch
            int batchNumber = (i / batchSize) + 1;
            batch.commit()
                    .addOnSuccessListener(aVoid -> Log.d(TAG, "Batch User " + batchNumber + " import thành công!"))
                    .addOnFailureListener(e -> Log.e(TAG, "Lỗi khi import batch User " + batchNumber, e));
        }
        Log.i(TAG, "Đã lên lịch import " + userList.size() + " users.");
    }

}
