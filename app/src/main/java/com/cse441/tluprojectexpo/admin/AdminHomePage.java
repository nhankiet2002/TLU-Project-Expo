package com.cse441.tluprojectexpo.admin;

import android.os.Bundle;
import android.view.MenuItem;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.cse441.tluprojectexpo.R;
import com.cse441.tluprojectexpo.admin.adapter.AdminProjectAdapter;
import com.cse441.tluprojectexpo.admin.repository.ProjectRepository;
import com.cse441.tluprojectexpo.admin.utils.NavigationUtil;
import com.cse441.tluprojectexpo.model.Project;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.navigation.NavigationBarView;

import java.util.ArrayList;
import java.util.List;

public class AdminHomePage extends AppCompatActivity implements AdminProjectAdapter.OnProjectAdminInteraction {

    private RecyclerView recyclerView;
    private AdminProjectAdapter adapter;
    private ProjectRepository repository;
    private List<Project> projectList = new ArrayList<>();
    private ProgressBar progressBar;
    private BottomNavigationView bottomNavigationView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_admin_home_page);

        // Ánh xạ View
        recyclerView = findViewById(R.id.recycler_view_project_list);
        // Ánh xạ ProgressBar từ layout của bạn
        progressBar = findViewById(R.id.progress_bar_loading);
        bottomNavigationView = findViewById(R.id.bottom_navigation);
        // Khởi tạo và thiết lập
        repository = new ProjectRepository();
        adapter = new AdminProjectAdapter(this, projectList, this);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        recyclerView.setAdapter(adapter);

        loadAllProjects();

        setupBottomNavigation();
    }

    private void loadAllProjects() {
        // Hiển thị ProgressBar trước khi gọi API
        progressBar.setVisibility(View.VISIBLE);
        recyclerView.setVisibility(View.GONE); // Ẩn danh sách đi

        repository.getAllProjectsWithDetails().observe(this, projects -> {
            // Ẩn ProgressBar khi có kết quả
            progressBar.setVisibility(View.GONE);
            recyclerView.setVisibility(View.VISIBLE); // Hiển thị lại danh sách

            if (projects != null) {
                adapter.updateData(projects);
            } else {
                Toast.makeText(this, "Lỗi khi tải danh sách dự án.", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void setupBottomNavigation() {

        bottomNavigationView.setOnItemSelectedListener(new NavigationBarView.OnItemSelectedListener() {
            @Override
            public boolean onNavigationItemSelected(@NonNull MenuItem item) {
                // Lấy ID của item được nhấn
                int itemId = item.getItemId();

                // Sử dụng if-else if để xử lý
                if (itemId == R.id.nav_manage) {
                    NavigationUtil.navigateTo(AdminHomePage.this, Dashboard.class);
                    return true;
                }
                else if (itemId == R.id.nav_profile) {
                    Toast.makeText(AdminHomePage.this, "Bạn đã nhấn vào Quản lý tài khoản", Toast.LENGTH_SHORT).show();
                    return true;
                }
                return false;
            }
        });
    }



    @Override
    public void onSetFeaturedClick(Project project, int position) {
        Toast.makeText(this, "Đang thêm " + project.getTitle() + "...", Toast.LENGTH_SHORT).show();
        repository.addFeaturedProject(project.getProjectId(), new ProjectRepository.OnTaskCompleteListener() {
            @Override
            public void onSuccess() {
                Toast.makeText(AdminHomePage.this, "Thêm thành công!", Toast.LENGTH_SHORT).show();
                // Cập nhật lại trạng thái của project trong list và thông báo cho adapter
                project.setFeatured(true);
                adapter.notifyItemChanged(position);
            }

            @Override
            public void onFailure(Exception e) {
                Toast.makeText(AdminHomePage.this, "Thêm thất bại: " + e.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    @Override
    public void onProjectClick(Project project) {
        Toast.makeText(this, "Đã nhấn vào dự án: " + project.getTitle(), Toast.LENGTH_SHORT).show();
        // Logic chuyển sang trang chi tiết dự án tại đây
    }
}