package com.cse441.tluprojectexpo;

import android.content.DialogInterface;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.cse441.tluprojectexpo.adapter.CategoryAdapter;
import com.cse441.tluprojectexpo.model.Category;
import com.cse441.tluprojectexpo.model.FirestoreUtils;
import com.google.firebase.firestore.FirebaseFirestore;

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
        fieldsRecyclerView = findViewById(R.id.recycler_view_field);
        technologiesRecyclerView = findViewById(R.id.recycler_view_technology);

        // Khởi tạo các danh sách
        fieldsList = new ArrayList<>();
        technologiesList = new ArrayList<>();

        // Thiết lập RecyclerViews
        setupRecyclerViews();

        // Tải dữ liệu từ Firestore
        loadFields();
        loadTechnologies();

        //Xóa các fields trong collection
        //db = FirebaseFirestore.getInstance();

        // Giả sử bạn có một nút bấm để thực hiện việc này
        Button btnDeleteField = findViewById(R.id.btn_delete_fields);
        // Cú pháp cũ, hoạt động trên cả Java 7
        // Giả sử Button này tồn tại trong file layout của bạn

        if(btnDeleteField !=null)

        {
            // Sử dụng cú pháp cũ cho setOnClickListener
            btnDeleteField.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    // Toàn bộ code trong AlertDialog
                    new AlertDialog.Builder(CatalogManagementPage.this)
                            .setTitle("Xác nhận Xóa")
                            .setMessage("Bạn có chắc chắn muốn xóa trường 'technologyId' khỏi TẤT CẢ các công nghệ không? Hành động này không thể hoàn tác.")

                            // SỬA Ở ĐÂY: Dùng cú pháp cũ cho setPositiveButton
                            .setPositiveButton("Xóa", new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialog, int which) {
                                    String collection = "technologies";
                                    String field = "technologyId";
                                    // Gọi hàm tiện ích
                                    FirestoreUtils.deleteFieldFromAllDocuments(db, collection, field);
                                    Toast.makeText(CatalogManagementPage.this, "Đang xử lý...", Toast.LENGTH_SHORT).show();
                                }
                            })

                            // SỬA Ở ĐÂY: Dùng cú pháp cũ cho setNegativeButton (dù chỉ là null)
                            .setNegativeButton("Hủy", null) // Có thể để null hoặc new OnClickListener rỗng

                            .show();
                }
            });
        } else

        {
            Log.e("CatalogManagementPage", "Không tìm thấy Button với ID: btn_delete_fields");
        }
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
        db.collection("categories") // Tên collection cho "Lĩnh vực"
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