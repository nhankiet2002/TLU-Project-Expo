package com.cse441.tluprojectexpo.admin.utils;

import android.util.Log;
import androidx.lifecycle.MutableLiveData;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QuerySnapshot;
import java.util.ArrayList;
import java.util.List;

public class SearchUtil {

    private static final String TAG = "UtilSearch";
    private static final FirebaseFirestore db = FirebaseFirestore.getInstance();

    /**
     * Phương thức tìm kiếm chung trên một collection Firestore.
     * Tìm kiếm các document có một trường cụ thể bắt đầu bằng một chuỗi cho trước (không phân biệt hoa-thường).
     * Yêu cầu: Firestore phải có một trường tương ứng đã được chuyển sang chữ thường.
     *
     * @param searchQuery       Chuỗi người dùng nhập để tìm kiếm.
     * @param collectionName    Tên của collection cần tìm (ví dụ: "Projects", "Users").
     * @param fieldNameToSearch Tên của trường chứa dữ liệu đã được chuyển sang chữ thường (ví dụ: "title_lowercase").
     * @param resultClass       Lớp của đối tượng model kết quả (ví dụ: Project.class).
     * @param <T>               Kiểu dữ liệu của model.
     * @return                  Một MutableLiveData chứa danh sách kết quả.
     */
    public static <T> MutableLiveData<List<T>> searchByField(
            String searchQuery,
            String collectionName,
            String fieldNameToSearch,
            Class<T> resultClass) {

        MutableLiveData<List<T>> liveData = new MutableLiveData<>();

        // Nếu chuỗi tìm kiếm rỗng, trả về danh sách rỗng ngay lập tức
        if (searchQuery == null || searchQuery.trim().isEmpty()) {
            liveData.setValue(new ArrayList<>());
            return liveData;
        }

        // Chuyển chuỗi tìm kiếm sang chữ thường để có thể so sánh
        String lowerCaseQuery = searchQuery.toLowerCase().trim();

        // Xây dựng câu truy vấn
        Query query = db.collection(collectionName)
                .whereGreaterThanOrEqualTo(fieldNameToSearch, lowerCaseQuery)
                .whereLessThanOrEqualTo(fieldNameToSearch, lowerCaseQuery + "\uf8ff")
                .limit(25); // Giới hạn kết quả để tối ưu hiệu năng

        // Thực thi truy vấn
        query.get().addOnCompleteListener(task -> {
            if (task.isSuccessful()) {
                QuerySnapshot snapshot = task.getResult();
                if (snapshot != null) {
                    // Tự động chuyển đổi kết quả sang danh sách các đối tượng model
                    List<T> results = snapshot.toObjects(resultClass);
                    liveData.setValue(results);
                    Log.d(TAG, "Tìm thấy " + results.size() + " kết quả cho '" + searchQuery + "' trong collection " + collectionName);
                } else {
                    liveData.setValue(new ArrayList<>());
                }
            } else {
                Log.e(TAG, "Lỗi khi tìm kiếm trong " + collectionName, task.getException());
                liveData.setValue(null); // Trả về null để báo hiệu có lỗi xảy ra
            }
        });

        return liveData;
    }
}