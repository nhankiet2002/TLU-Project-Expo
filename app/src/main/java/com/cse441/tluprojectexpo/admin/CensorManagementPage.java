package com.cse441.tluprojectexpo.admin;

import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.cse441.tluprojectexpo.R;
import com.cse441.tluprojectexpo.admin.adapter.CensorAdapter;
import com.cse441.tluprojectexpo.admin.repository.ProjectRepository;
import com.cse441.tluprojectexpo.admin.utils.AppToast;
import com.cse441.tluprojectexpo.model.Project;

import java.util.ArrayList;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;

public class CensorManagementPage extends AppCompatActivity implements CensorAdapter.OnCensorInteractionListener {

    private static final long SEARCH_DELAY = 500;
    private EditText searchCensor;
    private RecyclerView recyclerView;
    private TextView totalCensor;
    private ImageButton btnBack;
    private ProgressBar progressBar;

    private CensorAdapter adapter;
    private ProjectRepository projectRepository;
    private List<Project> originalProjectList = new ArrayList<>();
    private List<Project> displayedProjectList = new ArrayList<>();

    private Timer searchTimer = new Timer();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_censor_management_page);

        projectRepository = new ProjectRepository();

        initViews();
        setupRecyclerView();
        setupListeners();

        loadUnapprovedProjects();
    }

    private void initViews() {
        recyclerView = findViewById(R.id.unapproved_projects_list);
        totalCensor = findViewById(R.id.total_censor);
        btnBack = findViewById(R.id.back_from_censor);
        progressBar = findViewById(R.id.progress_bar);
        searchCensor = findViewById(R.id.search_censor);
    }

    private void setupRecyclerView() {
        adapter = new CensorAdapter(this, displayedProjectList, this);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        recyclerView.setAdapter(adapter);
    }

    private void setupListeners() {

        btnBack.setOnClickListener(v -> finish());
        searchCensor.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                if (searchTimer != null) searchTimer.cancel();
            }
            @Override
            public void afterTextChanged(Editable s) {
                String query = s.toString().trim();
                searchTimer = new Timer();
                searchTimer.schedule(new TimerTask() {
                    @Override
                    public void run() {
                        runOnUiThread(() -> performSearch(query));
                    }
                }, SEARCH_DELAY);
            }
        });
    }

    private void loadUnapprovedProjects() {
        if(progressBar != null) progressBar.setVisibility(View.VISIBLE);

        projectRepository.getUnapprovedProjects().observe(this, projects -> {
            if(progressBar != null) progressBar.setVisibility(View.GONE);
            if (projects != null) {
                // Cập nhật cả 2 danh sách
                originalProjectList.clear();
                originalProjectList.addAll(projects);
                updateDisplayedList(originalProjectList);
                adapter.notifyDataSetChanged();
                updateTotalCount();
            } else {
                AppToast.show(this, "Lỗi khi tải danh sách chờ duyệt.", Toast.LENGTH_SHORT);
            }
        });
    }

    // THÊM HÀM NÀY
    private void performSearch(String query) {
        if (query.isEmpty()) {
            updateDisplayedList(originalProjectList);
            return;
        }

        List<Project> searchResults = new ArrayList<>();
        String lowerCaseQuery = query.toLowerCase();

        for (Project project : originalProjectList) {
            if (project.getTitle().toLowerCase().contains(lowerCaseQuery)) {
                searchResults.add(project);
            }
        }
        updateDisplayedList(searchResults);
    }

    // THÊM HÀM NÀY
    private void updateDisplayedList(List<Project> newList) {
        displayedProjectList.clear();
        displayedProjectList.addAll(newList);
        adapter.notifyDataSetChanged();
        updateTotalCount();
    }

    private void updateTotalCount() {
        totalCensor.setText(displayedProjectList.size() + " mục");
    }

    // --- Xử lý sự kiện từ Adapter ---

    @Override
    public void onAcceptClick(Project project, int position) {
        if(progressBar != null) progressBar.setVisibility(View.VISIBLE);

        projectRepository.setProjectApprovalStatus(project.getProjectId(), true, new ProjectRepository.OnTaskCompleteListener() {
            @Override
            public void onSuccess() {
                if(progressBar != null) progressBar.setVisibility(View.GONE);
                AppToast.show(CensorManagementPage.this, "Đã duyệt dự án: " + project.getTitle(), Toast.LENGTH_SHORT);

                // Xóa item khỏi danh sách và cập nhật UI
                originalProjectList.remove(project);
                // Cập nhật lại danh sách hiển thị
                updateDisplayedList(new ArrayList<>(originalProjectList));
                adapter.notifyItemRemoved(position);
                updateTotalCount();
            }

            @Override
            public void onFailure(Exception e) {
                if(progressBar != null) progressBar.setVisibility(View.GONE);
                AppToast.show(CensorManagementPage.this, "Duyệt thất bại: " + e.getMessage(), Toast.LENGTH_SHORT);
            }
        });
    }

    @Override
    public void onRejectClick(Project project, int position) {
        // Hiển thị dialog xác nhận trước khi xóa
        new AlertDialog.Builder(this)
                .setTitle("Xác nhận từ chối")
                .setMessage("Bạn có chắc chắn muốn từ chối và xóa vĩnh viễn dự án '" + project.getTitle() + "' không?")
                .setPositiveButton("Xóa", (dialog, which) -> {
                    // Người dùng đã xác nhận, tiến hành xóa
                    if(progressBar != null) progressBar.setVisibility(View.VISIBLE);

                    projectRepository.deleteProject(project.getProjectId(), new ProjectRepository.OnTaskCompleteListener() {
                        @Override
                        public void onSuccess() {
                            if(progressBar != null) progressBar.setVisibility(View.GONE);
                            AppToast.show(CensorManagementPage.this, "Đã xóa dự án: " + project.getTitle(), Toast.LENGTH_SHORT);

                            // Xóa khỏi danh sách gốc
                            originalProjectList.remove(project);
                            // Cập nhật lại danh sách hiển thị
                            updateDisplayedList(new ArrayList<>(originalProjectList));
                            adapter.notifyItemRemoved(position);
                            updateTotalCount();
                        }

                        @Override
                        public void onFailure(Exception e) {
                            if(progressBar != null) progressBar.setVisibility(View.GONE);
                            AppToast.show(CensorManagementPage.this, "Xóa thất bại: " + e.getMessage(), Toast.LENGTH_SHORT);
                        }
                    });
                })
                .setNegativeButton("Hủy", null)
                .show();
    }

    @Override
    public void onItemClick(Project project) {
        AppToast.show(this, "Xem chi tiết: " + project.getTitle(), Toast.LENGTH_SHORT);
        // Intent intent = new Intent(this, ProjectDetailViewAdmin.class);
        // intent.putExtra("PROJECT_ID", project.getProjectId());
        // startActivity(intent);
    }
}