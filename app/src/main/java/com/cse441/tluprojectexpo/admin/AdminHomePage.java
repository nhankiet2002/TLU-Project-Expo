package com.cse441.tluprojectexpo.admin;

import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.cse441.tluprojectexpo.R;
import com.cse441.tluprojectexpo.admin.adapter.AdminProjectAdapter;
import com.cse441.tluprojectexpo.admin.repository.ProjectRepository;
import com.cse441.tluprojectexpo.admin.utils.NavigationUtil;
import com.cse441.tluprojectexpo.model.Project;
import com.google.android.material.bottomnavigation.BottomNavigationView;

import java.util.ArrayList;
import java.util.Collections;
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
        progressBar = findViewById(R.id.progress_bar_loading);
        bottomNavigationView = findViewById(R.id.bottom_navigation);

        // Khởi tạo và thiết lập
        repository = new ProjectRepository();
        // Quan trọng: Adapter được khởi tạo với projectList. Mọi thay đổi trên projectList
        // sẽ được adapter nhận biết sau khi gọi notifyDataSetChanged().
        adapter = new AdminProjectAdapter(this, projectList, this);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        recyclerView.setAdapter(adapter);

        loadAllProjects();
        setupBottomNavigation();
    }

    private void loadAllProjects() {
        progressBar.setVisibility(View.VISIBLE);
        recyclerView.setVisibility(View.GONE);

        repository.getAllProjectsForAdmin().observe(this, projects -> {
            progressBar.setVisibility(View.GONE);
            recyclerView.setVisibility(View.VISIBLE);

            if (projects != null) {
                Log.d("AdminHomePage", "Dữ liệu trả về. Số lượng dự án: " + projects.size());
                if (!projects.isEmpty()) {
                    Log.d("AdminHomePage", "Dự án đầu tiên: " + projects.get(0).getTitle() + " - isFeatured: " + projects.get(0).isFeatured());
                }

                // Cập nhật danh sách mà adapter đang giữ tham chiếu
                this.projectList.clear();
                this.projectList.addAll(projects);

                // THAY ĐỔI QUAN TRỌNG NHẤT Ở ĐÂY
                // Thông báo cho adapter rằng dữ liệu đã thay đổi và cần vẽ lại UI.
                adapter.notifyDataSetChanged();

            } else {
                Log.e("AdminHomePage", "Dữ liệu trả về là NULL. Có lỗi trong Repository.");
                Toast.makeText(this, "Lỗi khi tải danh sách dự án.", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void setupBottomNavigation() {
        bottomNavigationView.setOnItemSelectedListener(item -> {
            int itemId = item.getItemId();
            if (itemId == R.id.nav_manage) {
                NavigationUtil.navigateTo(AdminHomePage.this, Dashboard.class);
                return true;
            } else if (itemId == R.id.nav_profile) {
                Toast.makeText(AdminHomePage.this, "Chuyển đến trang cá nhân Admin", Toast.LENGTH_SHORT).show();
                return true;
            }
            return false;
        });
    }

    @Override
    public void onSetFeaturedClick(Project project, int position) {
        Toast.makeText(this, "Đang làm nổi bật: " + project.getTitle(), Toast.LENGTH_SHORT).show();

        repository.setProjectFeaturedStatus(project.getProjectId(), true, new ProjectRepository.OnTaskCompleteListener() {
            @Override
            public void onSuccess() {
                Toast.makeText(AdminHomePage.this, "Đã làm nổi bật thành công!", Toast.LENGTH_SHORT).show();

                project.setFeatured(true);

                // Sắp xếp lại danh sách để đưa item vừa được làm nổi bật lên đầu
                Collections.sort(projectList, (p1, p2) -> {
                    if (p1.isFeatured() && !p2.isFeatured()) return -1;
                    if (!p1.isFeatured() && p2.isFeatured()) return 1;
                    if (p1.getCreatedAt() != null && p2.getCreatedAt() != null) {
                        return p2.getCreatedAt().compareTo(p1.getCreatedAt());
                    }
                    return 0;
                });

                adapter.notifyDataSetChanged();
            }

            @Override
            public void onFailure(Exception e) {
                Toast.makeText(AdminHomePage.this, "Làm nổi bật thất bại: " + e.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    @Override
    public void onProjectClick(Project project) {
        Toast.makeText(this, "Đã nhấn vào dự án: " + project.getTitle(), Toast.LENGTH_SHORT).show();
        // Logic chuyển sang trang chi tiết dự án
    }
}