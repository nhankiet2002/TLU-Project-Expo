package com.cse441.tluprojectexpo.admin;

import android.os.Bundle;
import android.text.InputType;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.cse441.tluprojectexpo.R;
import com.cse441.tluprojectexpo.admin.adapter.CategoryAdapter;
import com.cse441.tluprojectexpo.model.Category;
import com.cse441.tluprojectexpo.admin.repository.CatalogRepository;

import java.util.ArrayList;
import java.util.List;

public class CatalogManagementPage extends AppCompatActivity {

    private static final String TAG = "CatalogManagementPage";

    // THAY ĐỔI: Bỏ FirebaseFirestore db, thay bằng CatalogRepository
    private CatalogRepository catalogRepository;

    private RecyclerView fieldsRecyclerView;
    private RecyclerView technologiesRecyclerView;

    private CategoryAdapter fieldsAdapter;
    private CategoryAdapter technologiesAdapter;
    private ProgressBar progressBar;

    // Các list này vẫn được giữ nguyên để cung cấp dữ liệu cho Adapter
    private List<Category> fieldsList = new ArrayList<>();
    private List<Category> technologiesList = new ArrayList<>();

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_catalog_management_page);
        progressBar = findViewById(R.id.progress_bar_loading);
        Log.d(TAG, "--- onCreate ĐÃ ĐƯỢC GỌI! ---");

        // THAY ĐỔI: Khởi tạo Repository thay vì Firestore
        catalogRepository = new CatalogRepository();

        // Ánh xạ Views từ XML (giữ nguyên)
        fieldsRecyclerView = findViewById(R.id.recycler_view_field);
        technologiesRecyclerView = findViewById(R.id.recycler_view_technology);

        // Thiết lập RecyclerViews
        setupRecyclerViews();

        // THAY ĐỔI: Tải dữ liệu thông qua Repository
        loadData();

        // Nút "Thêm mới" (giữ nguyên, chỉ thay đổi nội dung hàm nó gọi)
        Button btnAddNewCatalog = findViewById(R.id.add_new_catalog);
        btnAddNewCatalog.setOnClickListener(v -> showAddOptionsDialog());
    }

    private void setupRecyclerViews() {
        // THAY ĐỔI: Tạo listener riêng cho "Lĩnh vực"
        CategoryAdapter.OnCategoryClickListener fieldClickListener = new CategoryAdapter.OnCategoryClickListener() {
            @Override
            public void onEditClick(Category category) {
                // Gọi hàm sửa với đúng loại là FIELD
                showEditDialog(category, CatalogRepository.CatalogType.FIELD);
            }

            @Override
            public void onDeleteClick(Category category) {
                // Gọi hàm xóa với đúng loại là FIELD
                showDeleteConfirmationDialog(category, CatalogRepository.CatalogType.FIELD);
            }
        };

        // THAY ĐỔI: Tạo listener riêng cho "Công nghệ"
        CategoryAdapter.OnCategoryClickListener techClickListener = new CategoryAdapter.OnCategoryClickListener() {
            @Override
            public void onEditClick(Category category) {
                showEditDialog(category, CatalogRepository.CatalogType.TECHNOLOGY);
            }

            @Override
            public void onDeleteClick(Category category) {
                showDeleteConfirmationDialog(category, CatalogRepository.CatalogType.TECHNOLOGY);
            }
        };

        // Setup cho RecyclerView "Lĩnh vực" (Fields)
        fieldsRecyclerView.setLayoutManager(new LinearLayoutManager(this));
        // THAY ĐỔI: Truyền listener vừa tạo thay vì "this"
        fieldsAdapter = new CategoryAdapter(fieldsList, fieldClickListener);
        fieldsRecyclerView.setAdapter(fieldsAdapter);

        // Setup cho RecyclerView "Công nghệ" (Technologies)
        technologiesRecyclerView.setLayoutManager(new LinearLayoutManager(this));
        // THAY ĐỔI: Truyền listener vừa tạo thay vì "this"
        technologiesAdapter = new CategoryAdapter(technologiesList, techClickListener);
        technologiesRecyclerView.setAdapter(technologiesAdapter);
    }

    // THAY ĐỔI: Gộp loadFields() và loadTechnologies() thành một hàm
    private void loadData() {
        if (progressBar != null) {
            progressBar.setVisibility(View.VISIBLE);
        }

        // Tạo một biến đếm để theo dõi các tác vụ đã hoàn thành
        final int[] tasksCompleted = {0};
        final int totalTasks = 2; // Chúng ta có 2 tác vụ tải dữ liệu

        // Hàm helper để ẩn ProgressBar khi tất cả các tác vụ đã xong
        Runnable onTaskFinished = () -> {
            tasksCompleted[0]++;
            if (tasksCompleted[0] >= totalTasks) {
                if (progressBar != null) {
                    progressBar.setVisibility(View.GONE);
                }
            }
        };

        // Tải Lĩnh vực
        catalogRepository.getAllItems(CatalogRepository.CatalogType.FIELD, new CatalogRepository.CatalogDataListener() {
            @Override
            public void onDataLoaded(List<Category> items) {
                fieldsAdapter.updateData(items);
                Log.d(TAG, "[FIELDS] Tải dữ liệu thành công qua Repository.");
                // Gọi hàm helper để cập nhật số lượng tác vụ đã hoàn thành
                onTaskFinished.run();
            }
            @Override
            public void onError(Exception e) {
                Log.w(TAG, "[FIELDS] Lỗi tải dữ liệu.", e);
                Toast.makeText(CatalogManagementPage.this, "Lỗi tải Lĩnh vực.", Toast.LENGTH_SHORT).show();
                // Báo cáo tác vụ đã hoàn thành (dù là lỗi)
                onTaskFinished.run();
            }
        });

        // Tải Công nghệ
        catalogRepository.getAllItems(CatalogRepository.CatalogType.TECHNOLOGY, new CatalogRepository.CatalogDataListener() {
            @Override
            public void onDataLoaded(List<Category> items) {
                technologiesAdapter.updateData(items);
                Log.d(TAG, "[TECHNOLOGIES] Tải dữ liệu thành công qua Repository.");
                // Báo cáo tác vụ đã hoàn thành
                onTaskFinished.run();
            }
            @Override
            public void onError(Exception e) {
                Log.w(TAG, "[TECHNOLOGIES] Lỗi tải dữ liệu.", e);
                Toast.makeText(CatalogManagementPage.this, "Lỗi tải Công nghệ.", Toast.LENGTH_SHORT).show();
                // Báo cáo tác vụ đã hoàn thành
                onTaskFinished.run();
            }
        });
    }

    // THAY ĐỔI: Các phương thức onEditClick và onDeleteClick cũ đã bị xóa vì không còn implement interface

    // THAY ĐỔI: Dialog thêm mới giờ sẽ gọi hàm với Enum thay vì chuỗi
    private void showAddOptionsDialog() {
        final CharSequence[] options = {"Thêm Lĩnh vực mới", "Thêm Công nghệ mới", "Hủy"};
        new AlertDialog.Builder(this)
                .setTitle("Thêm danh mục mới")
                .setItems(options, (dialog, item) -> {
                    if (options[item].equals("Thêm Lĩnh vực mới")) {
                        showAddInputDialog(CatalogRepository.CatalogType.FIELD);
                    } else if (options[item].equals("Thêm Công nghệ mới")) {
                        showAddInputDialog(CatalogRepository.CatalogType.TECHNOLOGY);
                    } else {
                        dialog.dismiss();
                    }
                })
                .show();
    }

    // THAY ĐỔI: Dialog thêm giờ nhận CatalogType, không còn cần collectionName
    private void showAddInputDialog(final CatalogRepository.CatalogType type) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle(type == CatalogRepository.CatalogType.FIELD ? "Thêm Lĩnh vực mới" : "Thêm Công nghệ mới");

        final EditText input = createInputDialogEditText();
        builder.setView(createContainerForEditText(input));

        builder.setPositiveButton("Thêm", (dialog, which) -> {
            String name = input.getText().toString().trim();
            if (!name.isEmpty()) {
                // Gọi hàm xử lý logic mới
                addNewItem(new Category(name), type);
            } else {
                Toast.makeText(this, "Tên không được để trống!", Toast.LENGTH_SHORT).show();
            }
        });
        builder.setNegativeButton("Hủy", (dialog, which) -> dialog.cancel());
        builder.show();
    }

    // MỚI: Thêm hàm hiển thị dialog SỬA
    private void showEditDialog(final Category categoryToEdit, final CatalogRepository.CatalogType type) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle(type == CatalogRepository.CatalogType.FIELD ? "Sửa Lĩnh vực" : "Sửa Công nghệ");

        final EditText input = createInputDialogEditText();
        input.setText(categoryToEdit.getName()); // Hiển thị tên cũ
        builder.setView(createContainerForEditText(input));

        builder.setPositiveButton("Lưu", (dialog, which) -> {
            String newName = input.getText().toString().trim();
            if (!newName.isEmpty() && !newName.equals(categoryToEdit.getName())) {
                categoryToEdit.setName(newName);
                updateItem(categoryToEdit, type); // Gọi hàm cập nhật
            }
        });
        builder.setNegativeButton("Hủy", (dialog, which) -> dialog.cancel());
        builder.show();
    }

    // MỚI: Thêm hàm hiển thị dialog xác nhận XÓA
    private void showDeleteConfirmationDialog(final Category categoryToDelete, final CatalogRepository.CatalogType type) {
        new AlertDialog.Builder(this)
                .setTitle("Xác nhận Xóa")
                .setMessage("Bạn có chắc chắn muốn xóa '" + categoryToDelete.getName() + "' không?")
                .setPositiveButton("Xóa", (dialog, which) -> deleteItem(categoryToDelete.getId(), type))
                .setNegativeButton("Hủy", null)
                .setIcon(android.R.drawable.ic_dialog_alert)
                .show();
    }

    // THAY ĐỔI: Xóa hàm `addNewCategoryToFirestore`, thay bằng 3 hàm logic gọi Repository

    // MỚI: Hàm logic để THÊM item, gọi đến Repository
    private void addNewItem(Category newItem, CatalogRepository.CatalogType type) {
        catalogRepository.addItem(type, newItem, task -> {
            if (task.isSuccessful()) {
                Toast.makeText(this, "Thêm thành công!", Toast.LENGTH_SHORT).show();
                loadData(); // Tải lại dữ liệu sau khi thêm
            } else {
                Toast.makeText(this, "Thêm thất bại!", Toast.LENGTH_SHORT).show();
                Log.w(TAG, "Lỗi khi thêm item qua Repository", task.getException());
            }
        });
    }

    // MỚI: Hàm logic để CẬP NHẬT item, gọi đến Repository
    private void updateItem(Category itemToUpdate, CatalogRepository.CatalogType type) {
        catalogRepository.updateItem(type, itemToUpdate, task -> {
            if (task.isSuccessful()) {
                Toast.makeText(this, "Cập nhật thành công!", Toast.LENGTH_SHORT).show();
                loadData();
            } else {
                Toast.makeText(this, "Cập nhật thất bại!", Toast.LENGTH_SHORT).show();
                Log.w(TAG, "Lỗi khi cập nhật item qua Repository", task.getException());
            }
        });
    }

    // MỚI: Hàm logic để XÓA item, gọi đến Repository
    private void deleteItem(String itemId, CatalogRepository.CatalogType type) {
        catalogRepository.deleteItem(type, itemId, task -> {
            if (task.isSuccessful()) {
                Toast.makeText(this, "Xóa thành công!", Toast.LENGTH_SHORT).show();
                loadData();
            } else {
                Toast.makeText(this, "Xóa thất bại!", Toast.LENGTH_SHORT).show();
                Log.w(TAG, "Lỗi khi xóa item qua Repository", task.getException());
            }
        });
    }

    // Các hàm tiện ích cho UI để tránh lặp code (thêm vào cuối file)
    private EditText createInputDialogEditText() {
        final EditText input = new EditText(this);
        input.setInputType(InputType.TYPE_CLASS_TEXT);
        input.setHint("Nhập tên tại đây...");
        return input;
    }

    private LinearLayout createContainerForEditText(EditText editText) {
        LinearLayout container = new LinearLayout(this);
        container.setOrientation(LinearLayout.VERTICAL);
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT);
        lp.setMargins(40, 20, 40, 0);
        editText.setLayoutParams(lp);
        container.addView(editText);
        return container;
    }
}