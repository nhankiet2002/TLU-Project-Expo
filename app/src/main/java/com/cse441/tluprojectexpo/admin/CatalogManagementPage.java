// PATH: com/cse441/tluprojectexpo/admin/CatalogManagementPage.java

package com.cse441.tluprojectexpo.admin;

import android.os.Bundle;
import android.text.Editable;
import android.text.InputType;
import android.text.TextWatcher;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
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
import com.cse441.tluprojectexpo.admin.utils.AppToast;
import com.cse441.tluprojectexpo.admin.utils.NavigationUtil;
import com.cse441.tluprojectexpo.model.Category;
import com.cse441.tluprojectexpo.admin.repository.CatalogRepository;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class CatalogManagementPage extends AppCompatActivity {

    private static final String TAG = "CatalogManagementPage";

    private CatalogRepository catalogRepository;
    private RecyclerView fieldsRecyclerView;
    private RecyclerView technologiesRecyclerView;
    private ImageButton btnBackToDashboard;
    private EditText searchCatalog;
    private ProgressBar progressBar;
    private int loadingTasksCount = 0;

    private CategoryAdapter fieldsAdapter;
    private CategoryAdapter technologiesAdapter;

    private List<Category> originalFieldList = new ArrayList<>();
    private List<Category> originalTechnologyList = new ArrayList<>();

    private List<Category> displayedFieldList = new ArrayList<>();
    private List<Category> displayedTechnologyList = new ArrayList<>();

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_catalog_management_page);
        Log.d(TAG, "--- onCreate ĐÃ ĐƯỢC GỌI! ---");

        // Ánh xạ Views
        fieldsRecyclerView = findViewById(R.id.recycler_view_field);
        technologiesRecyclerView = findViewById(R.id.recycler_view_technology);
        searchCatalog = findViewById(R.id.search_catalog);
        progressBar = findViewById(R.id.progress_bar_loading);

        // Khởi tạo Repository
        catalogRepository = new CatalogRepository();

        // Thiết lập chỉ LayoutManager cho RecyclerViews
        setupRecyclerViews();

        setupSearchListener();

        // Tải dữ liệu (hàm này sẽ tự tạo và gán Adapter)
        loadData();

        // Nút "Thêm mới"
        Button btnAddNewCatalog = findViewById(R.id.add_new_catalog);
        btnAddNewCatalog.setOnClickListener(v -> showAddOptionsDialog());

        btnBackToDashboard = (ImageButton) findViewById(R.id.back_to_dashboard);
        btnBackToDashboard.setOnClickListener(v -> NavigationUtil.navigateToDashboard(this));
    }

    private void setupRecyclerViews() {
        // Chỉ cần setup LayoutManager ở đây
        fieldsRecyclerView.setLayoutManager(new LinearLayoutManager(this));
        technologiesRecyclerView.setLayoutManager(new LinearLayoutManager(this));

        // Tạo listener chung cho cả hai
        CategoryAdapter.OnCategoryClickListener clickListener = new CategoryAdapter.OnCategoryClickListener() {
            @Override
            public void onEditClick(Category category) {
                boolean isField = originalFieldList.stream().anyMatch(c -> c.getId().equals(category.getId()));
                showEditDialog(category, isField ? CatalogRepository.CatalogType.FIELD : CatalogRepository.CatalogType.TECHNOLOGY);
            }
            @Override
            public void onDeleteClick(Category category) {
                boolean isField = originalFieldList.stream().anyMatch(c -> c.getId().equals(category.getId()));
                showDeleteConfirmationDialog(category, isField ? CatalogRepository.CatalogType.FIELD : CatalogRepository.CatalogType.TECHNOLOGY);
            }
        };

        // Khởi tạo Adapter với danh sách hiển thị
        fieldsAdapter = new CategoryAdapter(displayedFieldList, clickListener);
        technologiesAdapter = new CategoryAdapter(displayedTechnologyList, clickListener);

        fieldsRecyclerView.setAdapter(fieldsAdapter);
        technologiesRecyclerView.setAdapter(technologiesAdapter);
    }

    // THÊM HÀM NÀY
    private void setupSearchListener() {
        searchCatalog.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) {}
            @Override
            public void afterTextChanged(Editable s) {
                // Lọc cả hai danh sách mỗi khi người dùng gõ
                filterLists(s.toString());
            }
        });
    }

    private void loadData() {
        showLoading();
        // Tải Lĩnh vực
        catalogRepository.getAllItems(CatalogRepository.CatalogType.FIELD, new CatalogRepository.CatalogDataListener() {
            @Override
            public void onDataLoaded(List<Category> items) {
                originalFieldList.clear();
                originalFieldList.addAll(items);
                // Lọc danh sách ngay sau khi tải, để áp dụng query tìm kiếm hiện tại (nếu có)
                filterLists(searchCatalog.getText().toString());
                hideLoading();
            }

            @Override
            public void onError(Exception e) {
                Log.w(TAG, "[FIELDS] Lỗi tải dữ liệu.", e);
                AppToast.show(CatalogManagementPage.this, "Lỗi tải Lĩnh vực.", Toast.LENGTH_SHORT);
                hideLoading();
            }
        });

        // Tải Công nghệ
        catalogRepository.getAllItems(CatalogRepository.CatalogType.TECHNOLOGY, new CatalogRepository.CatalogDataListener() {
            @Override
            public void onDataLoaded(List<Category> items) {
                originalTechnologyList.clear();
                originalTechnologyList.addAll(items);
                filterLists(searchCatalog.getText().toString());
                hideLoading();
            }
            @Override
            public void onError(Exception e) {
                AppToast.show(CatalogManagementPage.this, "Lỗi tải Công nghệ.", Toast.LENGTH_SHORT);
                hideLoading();
            }
        });
    }

    // THÊM 2 HÀM NÀY ĐỂ QUẢN LÝ ProgressBar
    private void showLoading() {
        loadingTasksCount = 2; // Chúng ta có 2 tác vụ tải dữ liệu
        if (progressBar != null) {
            progressBar.setVisibility(View.VISIBLE);
        }
    }

    private void hideLoading() {
        loadingTasksCount--; // Giảm biến đếm mỗi khi một tác vụ hoàn thành
        if (loadingTasksCount <= 0 && progressBar != null) {
            progressBar.setVisibility(View.GONE);
        }
    }


    private void filterLists(String query) {
        String lowerCaseQuery = query.toLowerCase().trim();

        // Lọc danh sách lĩnh vực
        List<Category> filteredFields = originalFieldList.stream()
                .filter(category -> category.getName().toLowerCase().contains(lowerCaseQuery))
                .collect(Collectors.toList());
        updateDisplayedList(displayedFieldList, filteredFields, fieldsAdapter);

        // Lọc danh sách công nghệ
        List<Category> filteredTechnologies = originalTechnologyList.stream()
                .filter(category -> category.getName().toLowerCase().contains(lowerCaseQuery))
                .collect(Collectors.toList());
        updateDisplayedList(displayedTechnologyList, filteredTechnologies, technologiesAdapter);
    }

    // Hàm phụ trợ chung để cập nhật UI
    private void updateDisplayedList(List<Category> displayedList, List<Category> newList, CategoryAdapter adapter) {
        displayedList.clear();
        displayedList.addAll(newList);
        if (adapter != null) {
            adapter.notifyDataSetChanged();
        }
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
                AppToast.show(this, "Tên không được để trống!", Toast.LENGTH_SHORT);
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
        showLoadingSimple();
        catalogRepository.addItem(type, newItem, task -> {
            hideLoadingSimple();

            if (task.isSuccessful()) {
                AppToast.show(this, "Thêm thành công!", Toast.LENGTH_SHORT);
                loadData(); // Tải lại dữ liệu sau khi thêm
            } else {
                AppToast.show(this, "Thêm thất bại!", Toast.LENGTH_SHORT);
                Log.w(TAG, "Lỗi khi thêm item qua Repository", task.getException());
            }
        });
    }

    private void updateItem(Category itemToUpdate, CatalogRepository.CatalogType type) {
        showLoadingSimple();
        catalogRepository.updateItem(type, itemToUpdate, task -> {
            hideLoadingSimple();
            if (task.isSuccessful()) {
                AppToast.show(this, "Cập nhật thành công!", Toast.LENGTH_SHORT);
                loadData();
            } else {
                AppToast.show(this, "Cập nhật thất bại!", Toast.LENGTH_SHORT);
                Log.w(TAG, "Lỗi khi cập nhật item qua Repository", task.getException());
            }
        });
    }

    private void deleteItem(String itemId, CatalogRepository.CatalogType type) {
        showLoadingSimple();
        catalogRepository.deleteItem(type, itemId, task -> {
            hideLoadingSimple();
            if (task.isSuccessful()) {
                AppToast.show(this, "Xóa thành công!", Toast.LENGTH_SHORT);
                loadData();
            } else {
                AppToast.show(this, "Xóa thất bại!", Toast.LENGTH_SHORT);
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

    private void showLoadingSimple() {
        if (progressBar != null) progressBar.setVisibility(View.VISIBLE);
    }

    private void hideLoadingSimple() {
        if (progressBar != null) progressBar.setVisibility(View.GONE);
    }
}