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
            users = new User[]{gson.fromJson(reader, User.class)};
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


    /**
     * Thêm một field mới với giá trị mặc định vào tất cả các document trong một collection.
     * Nếu field đã tồn tại trong một document, giá trị của nó sẽ bị ghi đè.
     *
     * @param db             Đối tượng FirebaseFirestore.
     * @param collectionName Tên của collection cần cập nhật.
     * @param fieldName      Tên của field mới cần thêm.
     * @param defaultValue   Giá trị mặc định cho field mới. Giá trị này có thể là String,
     *                       Number, Boolean, Timestamp, Map, v.v...
     */
    public static void addFieldToAllDocuments(FirebaseFirestore db, String collectionName,
                                              String fieldName, Object defaultValue) {

        // Kiểm tra đầu vào
        if (fieldName == null || fieldName.trim().isEmpty()) {
            Log.e(TAG, "Tên field không được để trống.");
            return;
        }

        CollectionReference collectionRef = db.collection(collectionName);

        // 1. Lấy tất cả các document trong collection
        collectionRef.get().addOnCompleteListener(task -> {
            if (task.isSuccessful()) {
                if (task.getResult().isEmpty()) {
                    Log.d(TAG, "Collection '" + collectionName + "' rỗng hoặc không tồn tại. Không có gì để cập nhật.");
                    return;
                }

                // 2. Tạo một WriteBatch để gộp các thao tác ghi
                WriteBatch batch = db.batch();

                // 3. Lặp qua tất cả các document
                for (QueryDocumentSnapshot document : task.getResult()) {
                    Log.d(TAG, "Lên lịch thêm/cập nhật field '" + fieldName + "' cho document: " + document.getId());
                    // 4. Thêm thao tác update (sẽ tạo field mới nếu chưa có) vào batch
                    batch.update(document.getReference(), fieldName, defaultValue); //lệnh thêm ở đây
                }

                // 5. Thực thi batch để áp dụng tất cả các thay đổi
                batch.commit().addOnSuccessListener(aVoid -> {
                    Log.d(TAG, "Thành công! Đã thêm/cập nhật field '" + fieldName + "' cho collection '" + collectionName + "'.");
                    // Bạn có thể thêm callback hoặc event bus để thông báo cho UI ở đây

                }).addOnFailureListener(e -> {
                    Log.e(TAG, "Lỗi khi thực thi batch update.", e);
                });

            } else {
                Log.e(TAG, "Lỗi khi lấy documents từ collection '" + collectionName + "'.", task.getException());
            }
        });
    }

}
