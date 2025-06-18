// PATH: com/cse441/tluprojectexpo/admin/CatalogManagementPage.java

package com.cse441.tluprojectexpo.admin;

import android.os.Bundle;
import android.text.InputType;
import android.util.Log;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
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

public class CatalogManagementPage extends AppCompatActivity {

    private static final String TAG = "CatalogManagementPage";

    private CatalogRepository catalogRepository;
    private RecyclerView fieldsRecyclerView;
    private RecyclerView technologiesRecyclerView;
    // Đã xóa các biến Adapter và List không cần thiết ở cấp lớp

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_catalog_management_page);
        Log.d(TAG, "--- onCreate ĐÃ ĐƯỢC GỌI! ---");

        // Ánh xạ Views
        fieldsRecyclerView = findViewById(R.id.recycler_view_field);
        technologiesRecyclerView = findViewById(R.id.recycler_view_technology);

        // Khởi tạo Repository
        catalogRepository = new CatalogRepository();

        // Thiết lập chỉ LayoutManager cho RecyclerViews
        setupRecyclerViews();

        // Tải dữ liệu (hàm này sẽ tự tạo và gán Adapter)
        loadData();

        // Nút "Thêm mới"
        Button btnAddNewCatalog = findViewById(R.id.add_new_catalog);
        btnAddNewCatalog.setOnClickListener(v -> showAddOptionsDialog());
    }

    private void setupRecyclerViews() {
        // Chỉ cần setup LayoutManager ở đây
        fieldsRecyclerView.setLayoutManager(new LinearLayoutManager(this));
        technologiesRecyclerView.setLayoutManager(new LinearLayoutManager(this));
    }

    private void loadData() {
        // Tải Lĩnh vực
        catalogRepository.getAllItems(CatalogRepository.CatalogType.FIELD, new CatalogRepository.CatalogDataListener() {
            @Override
            public void onDataLoaded(java.util.List<Category> items) {
                // Tạo listener cho các sự kiện click
                CategoryAdapter.OnCategoryClickListener fieldClickListener = new CategoryAdapter.OnCategoryClickListener() {
                    @Override public void onEditClick(Category category) { showEditDialog(category, CatalogRepository.CatalogType.FIELD); }
                    @Override public void onDeleteClick(Category category) { showDeleteConfirmationDialog(category, CatalogRepository.CatalogType.FIELD); }
                };

                // Tạo một Adapter hoàn toàn mới và gán nó cho RecyclerView
                CategoryAdapter newFieldsAdapter = new CategoryAdapter(items, fieldClickListener);
                fieldsRecyclerView.setAdapter(newFieldsAdapter);

                Log.d(TAG, "[FIELDS] ĐÃ GÁN ADAPTER MỚI. Số lượng: " + items.size());
            }

            @Override
            public void onError(Exception e) {
                Log.w(TAG, "[FIELDS] Lỗi tải dữ liệu.", e);
                Toast.makeText(CatalogManagementPage.this, "Lỗi tải Lĩnh vực.", Toast.LENGTH_SHORT).show();
            }
        });

        // Tải Công nghệ
        catalogRepository.getAllItems(CatalogRepository.CatalogType.TECHNOLOGY, new CatalogRepository.CatalogDataListener() {
            @Override
            public void onDataLoaded(java.util.List<Category> items) {
                // Tạo listener cho các sự kiện click
                CategoryAdapter.OnCategoryClickListener techClickListener = new CategoryAdapter.OnCategoryClickListener() {
                    @Override public void onEditClick(Category category) { showEditDialog(category, CatalogRepository.CatalogType.TECHNOLOGY); }
                    @Override public void onDeleteClick(Category category) { showDeleteConfirmationDialog(category, CatalogRepository.CatalogType.TECHNOLOGY); }
                };

                // Tạo một Adapter hoàn toàn mới và gán nó cho RecyclerView
                CategoryAdapter newTechAdapter = new CategoryAdapter(items, techClickListener);
                technologiesRecyclerView.setAdapter(newTechAdapter);

                Log.d(TAG, "[TECHNOLOGIES] ĐÃ GÁN ADAPTER MỚI. Số lượng: " + items.size());
            }

            @Override
            public void onError(Exception e) {
                Log.w(TAG, "[TECHNOLOGIES] Lỗi tải dữ liệu.", e);
                Toast.makeText(CatalogManagementPage.this, "Lỗi tải Công nghệ.", Toast.LENGTH_SHORT).show();
            }
        });
    }

    // Các hàm dialog và xử lý logic thêm/sửa/xóa giữ nguyên không thay đổi
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

    private void showAddInputDialog(final CatalogRepository.CatalogType type) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle(type == CatalogRepository.CatalogType.FIELD ? "Thêm Lĩnh vực mới" : "Thêm Công nghệ mới");

        final EditText input = createInputDialogEditText();
        builder.setView(createContainerForEditText(input));

        builder.setPositiveButton("Thêm", (dialog, which) -> {
            String name = input.getText().toString().trim();
            if (!name.isEmpty()) {
                addNewItem(new Category(name), type);
            } else {
                Toast.makeText(this, "Tên không được để trống!", Toast.LENGTH_SHORT).show();
            }
        });
        builder.setNegativeButton("Hủy", (dialog, which) -> dialog.cancel());
        builder.show();
    }

    private void showEditDialog(final Category categoryToEdit, final CatalogRepository.CatalogType type) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle(type == CatalogRepository.CatalogType.FIELD ? "Sửa Lĩnh vực" : "Sửa Công nghệ");

        final EditText input = createInputDialogEditText();
        input.setText(categoryToEdit.getName());
        builder.setView(createContainerForEditText(input));

        builder.setPositiveButton("Lưu", (dialog, which) -> {
            String newName = input.getText().toString().trim();
            if (!newName.isEmpty() && !newName.equals(categoryToEdit.getName())) {
                categoryToEdit.setName(newName);
                updateItem(categoryToEdit, type);
            }
        });
        builder.setNegativeButton("Hủy", (dialog, which) -> dialog.cancel());
        builder.show();
    }

    private void showDeleteConfirmationDialog(final Category categoryToDelete, final CatalogRepository.CatalogType type) {
        new AlertDialog.Builder(this)
                .setTitle("Xác nhận Xóa")
                .setMessage("Bạn có chắc chắn muốn xóa '" + categoryToDelete.getName() + "' không?")
                .setPositiveButton("Xóa", (dialog, which) -> deleteItem(categoryToDelete.getId(), type))
                .setNegativeButton("Hủy", null)
                .setIcon(android.R.drawable.ic_dialog_alert)
                .show();
    }

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