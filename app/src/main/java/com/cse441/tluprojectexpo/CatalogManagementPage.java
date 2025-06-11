package com.cse441.tluprojectexpo;

import android.content.DialogInterface;
import android.os.Bundle;
import android.text.InputType;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.cse441.tluprojectexpo.adapter.CategoryAdapter;
import com.cse441.tluprojectexpo.model.Category;
import com.cse441.tluprojectexpo.model.FirestoreUtils;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import com.google.android.gms.tasks.OnFailureListener;
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
        setContentView(R.layout.activity_catalog_management_page);
        Log.d(TAG, "--- onCreate ĐÃ ĐƯỢC GỌI! MÀN HÌNH ĐÃ HIỂN THỊ. ---");

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

        // 1. Ánh xạ và thiết lập sự kiện cho nút "Thêm mới"
        Button btnAddNewCatalog = findViewById(R.id.add_new_catalog);
        btnAddNewCatalog.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showAddOptionsDialog();
            }
        });

//        //Xóa các fields trong collection
//        //db = FirebaseFirestore.getInstance();
//
//        // Giả sử bạn có một nút bấm để thực hiện việc này
//        Button btnDeleteField = findViewById(R.id.btn_delete_fields);
//        // Cú pháp cũ, hoạt động trên cả Java 7
//        // Giả sử Button này tồn tại trong file layout của bạn
//
//        if(btnDeleteField !=null)
//
//        {
//            // Sử dụng cú pháp cũ cho setOnClickListener
//            btnDeleteField.setOnClickListener(new View.OnClickListener() {
//                @Override
//                public void onClick(View v) {
//                    // Toàn bộ code trong AlertDialog
//                    new AlertDialog.Builder(CatalogManagementPage.this)
//                            .setTitle("Xác nhận Xóa")
//                            .setMessage("Bạn có chắc chắn muốn xóa trường 'technologyId' khỏi TẤT CẢ các công nghệ không? Hành động này không thể hoàn tác.")
//
//                            // SỬA Ở ĐÂY: Dùng cú pháp cũ cho setPositiveButton
//                            .setPositiveButton("Xóa", new DialogInterface.OnClickListener() {
//                                @Override
//                                public void onClick(DialogInterface dialog, int which) {
//                                    String collection = "technologies";
//                                    String field = "technologyId";
//                                    // Gọi hàm tiện ích
//                                    FirestoreUtils.deleteFieldFromAllDocuments(db, collection, field);
//                                    Toast.makeText(CatalogManagementPage.this, "Đang xử lý...", Toast.LENGTH_SHORT).show();
//                                }
//                            })
//
//                            // SỬA Ở ĐÂY: Dùng cú pháp cũ cho setNegativeButton (dù chỉ là null)
//                            .setNegativeButton("Hủy", null) // Có thể để null hoặc new OnClickListener rỗng
//
//                            .show();
//                }
//            });
//        } else
//
//        {
//            Log.e("CatalogManagementPage", "Không tìm thấy Button với ID: btn_delete_fields");
//        }
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
        db.collection("categories")
                .get()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        // Cách đơn giản để chuyển đổi toàn bộ kết quả thành danh sách các đối tượng
                        List<Category> fetchedList = task.getResult().toObjects(Category.class);
                        fieldsAdapter.updateData(fetchedList);
                        Log.d(TAG, "[FIELDS] Yêu cầu thành công. Tìm thấy ");
                    } else {
                        Log.w(TAG, "Error getting documents.", task.getException());
                        Toast.makeText(this, "Lỗi khi tải Lĩnh vực.", Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private void loadTechnologies() {
        db.collection("technologies")
                .get()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        List<Category> fetchedList = task.getResult().toObjects(Category.class);
                        technologiesAdapter.updateData(fetchedList);
                        Log.d(TAG, "[TECHNOLOGIES] Yêu cầu thành công. Tìm thấy ");
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

    private void showAddOptionsDialog() {
        // Mảng chứa các lựa chọn sẽ hiển thị
        final CharSequence[] options = {"Thêm Lĩnh vực mới", "Thêm Công nghệ mới", "Quay lại"};

        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Thêm danh mục mới");
        builder.setItems(options, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int item) {
                if (options[item].equals("Thêm Lĩnh vực mới")) {
                    showAddInputDialog("field", "categories");
                } else if (options[item].equals("Thêm Công nghệ mới")) {
                    showAddInputDialog("technology", "technologies");
                } else if (options[item].equals("Hủy")) {
                    dialog.dismiss();
                }
            }
        });
        builder.show();
    }

    private void showAddInputDialog(final String type, final String collectionName) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);

        // Thiết lập tiêu đề cho dialog
        if (type.equals("field")) {
            builder.setTitle("Thêm Lĩnh vực mới");
        } else {
            builder.setTitle("Thêm Công nghệ mới");
        }

        // Tạo một EditText để người dùng nhập liệu
        final EditText input = new EditText(this);
        input.setInputType(InputType.TYPE_CLASS_TEXT);
        input.setHint("Nhập tên tại đây...");

        // Thêm một layout để có padding cho EditText
        LinearLayout container = new LinearLayout(this);
        container.setOrientation(LinearLayout.VERTICAL);
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT);
        lp.setMargins(40, 0, 40, 0);
        input.setLayoutParams(lp);
        container.addView(input);
        builder.setView(container);

        // Thiết lập nút "Thêm"
        builder.setPositiveButton("Thêm", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                String name = input.getText().toString().trim();
                if (name.isEmpty()) {
                    Toast.makeText(CatalogManagementPage.this, "Tên không được để trống!", Toast.LENGTH_SHORT).show();
                } else {
                    addNewCategoryToFirestore(name, collectionName, type);
                }
            }
        });

        // Thiết lập nút "Hủy"
        builder.setNegativeButton("Hủy", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                dialog.cancel();
            }
        });

        builder.show();
    }

    /**
     * Phương thức cuối cùng, thực hiện việc thêm document mới vào Firestore.
     * @param name Tên của danh mục mới.
     * @param collectionName Tên collection trên Firestore.
     * @param type Loại danh mục để biết cần reload RecyclerView nào.
     */
    private void addNewCategoryToFirestore(final String name, final String collectionName, final String type) {
        // Tạo một đối tượng mới
        Category newCategory = new Category(name);
        // Để trống ID, Firestore sẽ tự sinh
        db.collection(collectionName)
                .add(newCategory) // Sử dụng add() để Firestore tự tạo ID
                .addOnSuccessListener(new OnSuccessListener<DocumentReference>() {
                    @Override
                    public void onSuccess(DocumentReference documentReference) {
                        Toast.makeText(CatalogManagementPage.this, "Thêm thành công!", Toast.LENGTH_SHORT).show();
                        // Tải lại danh sách tương ứng để cập nhật giao diện
                        if (type.equals("field")) {
                            loadFields();
                        } else {
                            loadTechnologies();
                        }
                    }
                })
                .addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        Log.w(TAG, "Lỗi khi thêm document", e);
                        Toast.makeText(CatalogManagementPage.this, "Thêm thất bại!", Toast.LENGTH_SHORT).show();
                    }
                });
    }
}