package com.cse441.tluprojectexpo.admin;

import android.content.Context;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.ContextThemeWrapper;
import android.view.View;
import android.widget.EditText;
import android.widget.PopupMenu;
import android.widget.ProgressBar;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.cse441.tluprojectexpo.R;
import com.cse441.tluprojectexpo.admin.adapter.AdminProjectAdapter;
import com.cse441.tluprojectexpo.admin.repository.CatalogRepository;
import com.cse441.tluprojectexpo.admin.repository.ProjectRepository;
import com.cse441.tluprojectexpo.model.Category;
import com.cse441.tluprojectexpo.model.Project;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.chip.Chip;

import java.util.ArrayList;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;

public class AdminHomePage extends AppCompatActivity implements AdminProjectAdapter.OnProjectAdminInteraction {

    private static final String TAG = "AdminHomePage";
    private static final long SEARCH_DELAY = 500; // 500ms delay

    // Views
    private EditText etSearch;
    private Chip chipCategory, chipTechnology, chipStatus;
    private RecyclerView recyclerView;
    private ProgressBar progressBar;
    private BottomNavigationView bottomNavigationView;

    // Data & Repositories
    private AdminProjectAdapter adapter;
    private ProjectRepository projectRepository;
    private CatalogRepository catalogRepository;
    private List<Project> projectList = new ArrayList<>();
    private List<Category> allCategories = new ArrayList<>();
    private List<Category> allTechnologies = new ArrayList<>();

    // Filter states
    private String currentSearchQuery = null;
    private String selectedCategoryId = null;
    private String selectedTechnologyId = null;
    private String selectedStatus = null;
    private Timer searchTimer = new Timer();


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_admin_home_page);

        initViews();
        initRepositories();
        setupRecyclerView();
        setupListeners();

        loadFilterDataAndProjects();
    }

    private void initViews() {
        etSearch = findViewById(R.id.et_search);
        chipCategory = findViewById(R.id.chip_category);
        chipTechnology = findViewById(R.id.chip_technology);
        chipStatus = findViewById(R.id.chip_status);
        recyclerView = findViewById(R.id.recycler_view_project_list);
        progressBar = findViewById(R.id.progress_bar_loading);
        bottomNavigationView = findViewById(R.id.bottom_navigation);
    }

    private void initRepositories() {
        projectRepository = new ProjectRepository();
        catalogRepository = new CatalogRepository();
    }

    private void setupRecyclerView() {
        adapter = new AdminProjectAdapter(this, projectList, this);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        recyclerView.setAdapter(adapter);
    }

    private void setupListeners() {
        etSearch.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                if (searchTimer != null) searchTimer.cancel();
            }
            @Override
            public void afterTextChanged(Editable s) {
                searchTimer = new Timer();
                searchTimer.schedule(new TimerTask() {
                    @Override
                    public void run() {
                        runOnUiThread(() -> {
                            currentSearchQuery = s.toString();
                            loadFilteredProjects();
                        });
                    }
                }, SEARCH_DELAY);
            }
        });

        chipCategory.setOnClickListener(this::showCategoryMenu);
        chipTechnology.setOnClickListener(this::showTechnologyMenu);
        chipStatus.setOnClickListener(this::showStatusMenu);
    }

    private void loadFilterDataAndProjects() {
        progressBar.setVisibility(View.VISIBLE);

        // Tải dữ liệu cho các bộ lọc từ CatalogRepository
        catalogRepository.getAllItems(CatalogRepository.CatalogType.FIELD, new CatalogRepository.CatalogDataListener() {
            @Override
            public void onDataLoaded(List<Category> items) {
                allCategories = items;
            }
            @Override
            public void onError(Exception e) {
                Toast.makeText(AdminHomePage.this, "Lỗi tải danh sách lĩnh vực", Toast.LENGTH_SHORT).show();
            }
        });

        catalogRepository.getAllItems(CatalogRepository.CatalogType.TECHNOLOGY, new CatalogRepository.CatalogDataListener() {
            @Override
            public void onDataLoaded(List<Category> items) {
                allTechnologies = items;
            }
            @Override
            public void onError(Exception e) {
                Toast.makeText(AdminHomePage.this, "Lỗi tải danh sách công nghệ", Toast.LENGTH_SHORT).show();
            }
        });

        // Tải danh sách dự án lần đầu tiên
        loadFilteredProjects();
    }

    private void loadFilteredProjects() {
        progressBar.setVisibility(View.VISIBLE);
        Log.d(TAG, "Đang tải dự án với Query: '" + currentSearchQuery + "', CategoryID: " + selectedCategoryId + ", TechID: " + selectedTechnologyId + ", Status: " + selectedStatus);

        projectRepository.getFilteredProjects(currentSearchQuery, selectedCategoryId, selectedTechnologyId, selectedStatus)
                .observe(this, projects -> {
                    progressBar.setVisibility(View.GONE);
                    if (projects != null) {
                        Log.d(TAG, "Tải thành công " + projects.size() + " dự án.");
                        projectList.clear();
                        projectList.addAll(projects);
                        adapter.notifyDataSetChanged();
                    } else {
                        Toast.makeText(this, "Lỗi khi tải danh sách dự án.", Toast.LENGTH_SHORT).show();
                    }
                });
    }

    // Sử dụng chung một hàm để hiển thị menu cho cả Lĩnh vực và Công nghệ
    private void showCatalogMenu(View anchorView, String title, List<Category> items, OnCatalogItemSelectedListener listener) {
        Context wrapper = new ContextThemeWrapper(this, R.style.App_PopupMenu);
        PopupMenu popupMenu = new PopupMenu(wrapper, anchorView);

        // Thêm mục "Tất cả"
        popupMenu.getMenu().add(0, -1, 0, title);

        // Thêm các mục từ danh sách đã tải
        for (Category item : items) {
            // Chuyển đổi ID từ String sang int để dùng làm ItemId
            try {
                // Chúng ta sẽ dùng ID của item để xác định nó được chọn
                popupMenu.getMenu().add(0, View.generateViewId(), 0, item.getName()).setIntent(new android.content.Intent(item.getId()));
            } catch (NumberFormatException e) {
                Log.e(TAG, "Invalid ID format for menu item: " + item.getId());
            }
        }

        popupMenu.setOnMenuItemClickListener(menuItem -> {
            if (menuItem.getItemId() == -1) { // Chọn "Tất cả"
                listener.onItemSelected(null, title); // null ID
            } else {
                // Lấy ID đã lưu trong Intent
                String itemId = menuItem.getIntent().getAction();
                listener.onItemSelected(itemId, menuItem.getTitle().toString());
            }
            return true;
        });
        popupMenu.show();
    }

    private void showCategoryMenu(View view) {
        showCatalogMenu(view, "Tất cả lĩnh vực", allCategories, (id, name) -> {
            selectedCategoryId = id;
            chipCategory.setText(id == null ? getString(R.string.field_subject) : name);
            loadFilteredProjects();
        });
    }

    private void showTechnologyMenu(View view) {
        showCatalogMenu(view, "Tất cả công nghệ", allTechnologies, (id, name) -> {
            selectedTechnologyId = id;
            chipTechnology.setText(id == null ? getString(R.string.technology) : name);
            loadFilteredProjects();
        });
    }

    // Interface nội bộ để xử lý callback từ showCatalogMenu
    private interface OnCatalogItemSelectedListener {
        void onItemSelected(String id, String name);
    }

    private void showStatusMenu(View view) {
        Context wrapper = new ContextThemeWrapper(this, R.style.App_PopupMenu);
        PopupMenu popupMenu = new PopupMenu(wrapper, view);
        popupMenu.getMenuInflater().inflate(R.menu.status_menu, popupMenu.getMenu());
        popupMenu.setOnMenuItemClickListener(item -> {
            int itemId = item.getItemId();
            if (itemId == R.id.status_all) {
                selectedStatus = null;
                chipStatus.setText(R.string.status_2);
            } else if (itemId == R.id.status_in_progress || itemId == R.id.status_completed || itemId == R.id.status_stopped) {
                selectedStatus = item.getTitle().toString();
                chipStatus.setText(selectedStatus);
            }
            loadFilteredProjects();
            return true;
        });
        popupMenu.show();
    }

    @Override
    public void onSetFeaturedClick(Project project, int position) {
        // ... code của bạn
    }

    @Override
    public void onProjectClick(Project project) {
        // ... code của bạn
    }
}