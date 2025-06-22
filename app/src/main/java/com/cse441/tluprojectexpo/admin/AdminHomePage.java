package com.cse441.tluprojectexpo.admin;

import android.os.Bundle;
import android.util.Log;
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
        adapter = new AdminProjectAdapter(this, projectList, this);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        recyclerView.setAdapter(adapter);

        loadAllProjects();
        setupBottomNavigation();
    }

    private void loadAllProjects() {
        progressBar.setVisibility(View.VISIBLE);
        recyclerView.setVisibility(View.GONE);

        // Phương thức này trong repository đã được cập nhật để sắp xếp nổi bật lên đầu
        repository.getAllProjectsForAdmin().observe(this, projects -> {
            progressBar.setVisibility(View.GONE);
            recyclerView.setVisibility(View.VISIBLE);

            if (projects != null) {
                // projectList đã được gán địa chỉ mới, cần gán lại cho adapter
                Log.d("AdminHomePage", "Dữ liệu trả về. Số lượng dự án: " + projects.size());
                if (!projects.isEmpty()) {
                    Log.d("AdminHomePage", "Dự án đầu tiên: " + projects.get(0).getTitle() + " - isFeatured: " + projects.get(0).isFeatured());
                }
                this.projectList.clear();
                this.projectList.addAll(projects);
                adapter.updateData(this.projectList); // Cập nhật adapter với danh sách mới
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
                // Giả sử Dashboard là một Activity khác
                NavigationUtil.navigateTo(AdminHomePage.this, Dashboard.class);
                return true;
            } else if (itemId == R.id.nav_profile) {
                // Giả sử AdminProfile là một Activity khác
                // NavigationUtil.navigateTo(AdminHomePage.this, AdminProfileActivity.class);
                Toast.makeText(AdminHomePage.this, "Chuyển đến trang cá nhân Admin", Toast.LENGTH_SHORT).show();
                return true;
            }
            return false;
        });
    }

    // --- SỬA LẠI HOÀN TOÀN PHƯƠNG THỨC NÀY ---
    /**
     * Được gọi khi Admin nhấn nút "Làm nổi bật" trên một item.
     * @param project Đối tượng Project của item được nhấn.
     * @param position Vị trí của item trong RecyclerView.
     */
    @Override
    public void onSetFeaturedClick(Project project, int position) {
        Toast.makeText(this, "Đang làm nổi bật: " + project.getTitle(), Toast.LENGTH_SHORT).show();

        // Gọi phương thức mới để đặt IsFeatured = true
        repository.setProjectFeaturedStatus(project.getProjectId(), true, new ProjectRepository.OnTaskCompleteListener() {
            @Override
            public void onSuccess() {
                Toast.makeText(AdminHomePage.this, "Đã làm nổi bật thành công!", Toast.LENGTH_SHORT).show();

                // Cập nhật trạng thái của project trong danh sách local
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

                // Thông báo cho adapter rằng toàn bộ dữ liệu đã thay đổi (vì thứ tự đã thay đổi)
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