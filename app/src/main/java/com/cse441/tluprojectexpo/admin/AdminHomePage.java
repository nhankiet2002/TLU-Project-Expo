package com.cse441.tluprojectexpo.admin;

import android.content.Context;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.util.SparseBooleanArray;
import android.view.ContextThemeWrapper;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.PopupMenu;
import android.widget.ProgressBar;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.cse441.tluprojectexpo.R;
import com.cse441.tluprojectexpo.admin.adapter.AdminProjectAdapter;
import com.cse441.tluprojectexpo.admin.repository.CatalogRepository;
import com.cse441.tluprojectexpo.admin.repository.ProjectRepository;
import com.cse441.tluprojectexpo.admin.utils.NavigationUtil;
import com.cse441.tluprojectexpo.auth.SettingProfileActivity;
import com.cse441.tluprojectexpo.model.Category;
import com.cse441.tluprojectexpo.model.Project;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.chip.Chip;

import java.util.ArrayList;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;
import java.util.stream.Collectors;

public class AdminHomePage extends AppCompatActivity implements AdminProjectAdapter.OnProjectAdminInteraction {

    private static final String TAG = "AdminHomePage";
    private static final long SEARCH_DELAY = 500;

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
    private List<String> selectedTechnologyIds = new ArrayList<>();
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

        setupBottomNavigation();
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
        chipTechnology.setOnClickListener(v -> showTechnologyMultiSelectDialog());
        chipStatus.setOnClickListener(this::showStatusMenu);
    }

    private void loadFilterDataAndProjects() {
        progressBar.setVisibility(View.VISIBLE);

        catalogRepository.getAllItems(CatalogRepository.CatalogType.FIELD, new CatalogRepository.CatalogDataListener() {
            @Override
            public void onDataLoaded(List<Category> items) { allCategories = items; }
            @Override
            public void onError(Exception e) { Toast.makeText(AdminHomePage.this, "Lỗi tải danh sách lĩnh vực", Toast.LENGTH_SHORT).show(); }
        });

        catalogRepository.getAllItems(CatalogRepository.CatalogType.TECHNOLOGY, new CatalogRepository.CatalogDataListener() {
            @Override
            public void onDataLoaded(List<Category> items) { allTechnologies = items; }
            @Override
            public void onError(Exception e) { Toast.makeText(AdminHomePage.this, "Lỗi tải danh sách công nghệ", Toast.LENGTH_SHORT).show(); }
        });

        loadFilteredProjects();
    }

    private void loadFilteredProjects() {
        progressBar.setVisibility(View.VISIBLE);
        Log.d(TAG, "Đang tải dự án với Query: '" + currentSearchQuery + "', CategoryID: " + selectedCategoryId + ", TechIDs: " + selectedTechnologyIds + ", Status: " + selectedStatus);

        projectRepository.getFilteredProjects(currentSearchQuery, selectedCategoryId, selectedTechnologyIds, selectedStatus)
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

    private void showCategoryMenu(View view) {
        if (allCategories.isEmpty()) { Toast.makeText(this, "Đang tải danh sách...", Toast.LENGTH_SHORT).show(); return; }

        Context wrapper = new ContextThemeWrapper(this, R.style.App_PopupMenu);
        PopupMenu popupMenu = new PopupMenu(wrapper, view);
        popupMenu.getMenu().add(0, -1, 0, "Tất cả lĩnh vực");

        for (Category category : allCategories) {
            popupMenu.getMenu().add(0, View.generateViewId(), 0, category.getName()).setIntent(new android.content.Intent(category.getId()));
        }

        popupMenu.setOnMenuItemClickListener(menuItem -> {
            if (menuItem.getItemId() == -1) {
                selectedCategoryId = null;
                chipCategory.setText(getString(R.string.field_subject));
            } else {
                selectedCategoryId = menuItem.getIntent().getAction();
                chipCategory.setText(menuItem.getTitle());
            }
            loadFilteredProjects();
            return true;
        });
        popupMenu.show();
    }

    private void showTechnologyMultiSelectDialog() {
        if (allTechnologies.isEmpty()) {
            Toast.makeText(this, "Đang tải danh sách công nghệ...", Toast.LENGTH_SHORT).show();
            return;
        }

        LayoutInflater inflater = this.getLayoutInflater();
        View dialogView = inflater.inflate(R.layout.dialog_multi_select_technology, null);

        ListView listView = dialogView.findViewById(R.id.technology_list_view);
        Button btnApply = dialogView.findViewById(R.id.btn_apply);
        Button btnCancel = dialogView.findViewById(R.id.btn_cancel);
        Button btnClearAll = dialogView.findViewById(R.id.btn_clear_all);

        String[] techNames = allTechnologies.stream().map(Category::getName).toArray(String[]::new);
        ArrayAdapter<String> arrayAdapter = new ArrayAdapter<>(this, android.R.layout.simple_list_item_multiple_choice, techNames);
        listView.setAdapter(arrayAdapter);

        for (int i = 0; i < allTechnologies.size(); i++) {
            if (selectedTechnologyIds.contains(allTechnologies.get(i).getId())) {
                listView.setItemChecked(i, true);
            }
        }

        AlertDialog.Builder builder = new AlertDialog.Builder(this, R.style.MyCustomDialogTheme);
        builder.setView(dialogView);
        AlertDialog dialog = builder.create();

        btnApply.setOnClickListener(v -> {
            selectedTechnologyIds.clear();
            SparseBooleanArray checkedPositions = listView.getCheckedItemPositions();
            for (int i = 0; i < allTechnologies.size(); i++) {
                if (checkedPositions.get(i)) {
                    selectedTechnologyIds.add(allTechnologies.get(i).getId());
                }
            }
            updateTechnologyChipText();
            loadFilteredProjects();
            dialog.dismiss();
        });

        btnCancel.setOnClickListener(v -> dialog.dismiss());

        btnClearAll.setOnClickListener(v -> {
            for (int i = 0; i < listView.getCount(); i++) {
                listView.setItemChecked(i, false);
            }
        });

        dialog.show();
    }

    private void updateTechnologyChipText() {
        if (selectedTechnologyIds.isEmpty()) {
            chipTechnology.setText(R.string.technology);
        } else if (selectedTechnologyIds.size() == 1) {
            String selectedName = allTechnologies.stream()
                    .filter(t -> t.getId().equals(selectedTechnologyIds.get(0)))
                    .map(Category::getName)
                    .findFirst()
                    .orElse("Công nghệ");
            chipTechnology.setText(selectedName);
        } else {
            chipTechnology.setText(selectedTechnologyIds.size() + " công nghệ");
        }
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
            } else {
                selectedStatus = item.getTitle().toString();
                chipStatus.setText(selectedStatus);
            }
            loadFilteredProjects();
            return true;
        });
        popupMenu.show();
    }

    private void setupBottomNavigation() {
        bottomNavigationView.setOnItemSelectedListener(item -> {
            int itemId = item.getItemId();

            if (itemId == R.id.nav_manage) {
                NavigationUtil.navigateTo(this, Dashboard.class);
                return true;
            } else if (itemId == R.id.nav_profile) {
                NavigationUtil.navigateTo(this, SettingProfileActivity.class);
                return true;
            }

            return false;
        });
    }

    @Override
    public void onSetFeaturedClick(Project project, int position) {
        // ...
    }

    @Override
    public void onProjectClick(Project project) {
        // ...
    }
}