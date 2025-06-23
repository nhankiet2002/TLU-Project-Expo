package com.cse441.tluprojectexpo.admin;

import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
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
import com.cse441.tluprojectexpo.model.Project;
import java.util.ArrayList;
import java.util.List;

public class CensorManagementPage extends AppCompatActivity implements CensorAdapter.OnCensorInteractionListener {

    private RecyclerView recyclerView;
    private TextView totalCensor;
    private ImageView btnBack;
    private ProgressBar progressBar;

    private CensorAdapter adapter;
    private ProjectRepository projectRepository;
    private List<Project> unapprovedProjectList = new ArrayList<>();

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
    }

    private void setupRecyclerView() {
        adapter = new CensorAdapter(this, unapprovedProjectList, this);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        recyclerView.setAdapter(adapter);
    }

    private void setupListeners() {
        btnBack.setOnClickListener(v -> finish());
    }

    private void loadUnapprovedProjects() {
        if(progressBar != null) progressBar.setVisibility(View.VISIBLE);

        projectRepository.getUnapprovedProjects().observe(this, projects -> {
            if(progressBar != null) progressBar.setVisibility(View.GONE);
            if (projects != null) {
                unapprovedProjectList.clear();
                unapprovedProjectList.addAll(projects);
                adapter.notifyDataSetChanged();
                updateTotalCount();
            } else {
                Toast.makeText(this, "Lỗi khi tải danh sách chờ duyệt.", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void updateTotalCount() {
        totalCensor.setText(unapprovedProjectList.size() + " mục");
    }

    // --- Xử lý sự kiện từ Adapter ---

    @Override
    public void onAcceptClick(Project project, int position) {
        if(progressBar != null) progressBar.setVisibility(View.VISIBLE);

        projectRepository.setProjectApprovalStatus(project.getProjectId(), true, new ProjectRepository.OnTaskCompleteListener() {
            @Override
            public void onSuccess() {
                if(progressBar != null) progressBar.setVisibility(View.GONE);
                Toast.makeText(CensorManagementPage.this, "Đã duyệt dự án: " + project.getTitle(), Toast.LENGTH_SHORT).show();

                // Xóa item khỏi danh sách và cập nhật UI
                unapprovedProjectList.remove(position);
                adapter.notifyItemRemoved(position);
                updateTotalCount();
            }

            @Override
            public void onFailure(Exception e) {
                if(progressBar != null) progressBar.setVisibility(View.GONE);
                Toast.makeText(CensorManagementPage.this, "Duyệt thất bại: " + e.getMessage(), Toast.LENGTH_SHORT).show();
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
                            Toast.makeText(CensorManagementPage.this, "Đã xóa dự án: " + project.getTitle(), Toast.LENGTH_SHORT).show();

                            unapprovedProjectList.remove(position);
                            adapter.notifyItemRemoved(position);
                            updateTotalCount();
                        }

                        @Override
                        public void onFailure(Exception e) {
                            if(progressBar != null) progressBar.setVisibility(View.GONE);
                            Toast.makeText(CensorManagementPage.this, "Xóa thất bại: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                        }
                    });
                })
                .setNegativeButton("Hủy", null)
                .show();
    }

    @Override
    public void onItemClick(Project project) {
        Toast.makeText(this, "Xem chi tiết: " + project.getTitle(), Toast.LENGTH_SHORT).show();
        // Intent intent = new Intent(this, ProjectDetailViewAdmin.class);
        // intent.putExtra("PROJECT_ID", project.getProjectId());
        // startActivity(intent);
    }
}