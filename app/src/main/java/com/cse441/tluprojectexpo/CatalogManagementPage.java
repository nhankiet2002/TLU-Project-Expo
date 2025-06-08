package com.cse441.tluprojectexpo;

import android.os.Bundle;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import android.os.Bundle;
import android.util.Log;
import android.widget.Toast;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.cse441.tluprojectexpo.adapter.CategoryAdapter;
import com.cse441.tluprojectexpo.model.Category;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.ArrayList;
import java.util.List;

// 1. Implement interface của Adapter để lắng nghe sự kiện click
public class CatalogManagementPage extends AppCompatActivity implements CategoryAdapter.OnCategoryClickListener {

    private static final String TAG = "CatalogManagementPage";

    private FirebaseFirestore db;

    private RecyclerView fieldsRecyclerView;
    private RecyclerView technologiesRecyclerView;

    private CategoryAdapter fieldsAdapter;
    private CategoryAdapter technologiesAdapter;

    private List<Category> fieldsList;
    private List<Category> technologiesList;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // Thay R.layout.activity_catalog_management bằng tên file layout chính của bạn
        setContentView(R.layout.activity_catalog_management_page);

        // Khởi tạo Firestore
        db = FirebaseFirestore.getInstance();

        // Ánh xạ Views từ XML
        fieldsRecyclerView = findViewById(R.id.recycler_view_technology);
        technologiesRecyclerView = findViewById(R.id.recycler_view_catalog);

        // Khởi tạo các danh sách
        fieldsList = new ArrayList<>();
        technologiesList = new ArrayList<>();

        // Thiết lập RecyclerViews
        setupRecyclerViews();

        // Tải dữ liệu từ Firestore
        loadFields();
        loadTechnologies();
    }

    private void setupRecyclerViews() {
        // Setup cho RecyclerView "Lĩnh vực" (Fields)
        fieldsRecyclerView.setLayoutManager(new LinearLayoutManager(this));
        fieldsAdapter = new CategoryAdapter(fieldsList, this); // Truyền 'this' vì Activity này đã implement listener
        fieldsRecyclerView.setAdapter(fieldsAdapter);

        // Setup cho RecyclerView "Công nghệ" (Technologies)
        technologiesRecyclerView.setLayoutManager(new LinearLayoutManager(this));
        technologiesAdapter = new CategoryAdapter(technologiesList, this);
        technologiesRecyclerView.setAdapter(technologiesAdapter);
    }

    private void loadFields() {
        db.collection("fields") // Tên collection cho "Lĩnh vực"
                .get()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        // Cách đơn giản để chuyển đổi toàn bộ kết quả thành danh sách các đối tượng
                        List<Category> fetchedList = task.getResult().toObjects(Category.class);
                        fieldsAdapter.updateData(fetchedList);
                    } else {
                        Log.w(TAG, "Error getting documents.", task.getException());
                        Toast.makeText(this, "Lỗi khi tải Lĩnh vực.", Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private void loadTechnologies() {
        db.collection("technologies") // Tên collection cho "Công nghệ"
                .get()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        List<Category> fetchedList = task.getResult().toObjects(Category.class);
                        technologiesAdapter.updateData(fetchedList);
                    } else {
                        Log.w(TAG, "Error getting documents.", task.getException());
                        Toast.makeText(this, "Lỗi khi tải Công nghệ.", Toast.LENGTH_SHORT).show();
                    }
                });
    }


    // 2. Override các phương thức của Interface
    @Override
    public void onEditClick(Category category) {
        // Xử lý khi người dùng nhấn nút sửa
        Toast.makeText(this, "Sửa: " + category.getName() + " (ID: " + category.getId() + ")", Toast.LENGTH_SHORT).show();
        // Tại đây, bạn có thể mở một Dialog hoặc Activity mới để cho phép người dùng sửa tên
        // Sử dụng category.getId() để biết cần cập nhật document nào trên Firestore
    }

    @Override
    public void onDeleteClick(Category category) {
        // Xử lý khi người dùng nhấn nút xóa
        Toast.makeText(this, "Xóa: " + category.getName() + " (ID: " + category.getId() + ")", Toast.LENGTH_SHORT).show();
        // Tại đây, bạn nên hiển thị một dialog xác nhận trước khi xóa
        // Ví dụ code xóa:
        /*
        String collectionPath = ...; // "fields" hoặc "technologies"
        db.collection(collectionPath).document(category.getId()).delete()
            .addOnSuccessListener(aVoid -> {
                Toast.makeText(this, "Xóa thành công", Toast.LENGTH_SHORT).show();
                // Tải lại danh sách sau khi xóa
                if (collectionPath.equals("fields")) {
                    loadFields();
                } else {
                    loadTechnologies();
                }
            })
            .addOnFailureListener(e -> Toast.makeText(this, "Xóa thất bại", Toast.LENGTH_SHORT).show());
        */
    }
}