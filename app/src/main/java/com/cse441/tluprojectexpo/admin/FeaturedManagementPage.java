package com.cse441.tluprojectexpo.admin;

import android.os.Bundle;
import android.view.View;
import android.widget.ImageButton;
import android.widget.ProgressBar;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.cse441.tluprojectexpo.R;
import com.cse441.tluprojectexpo.admin.adapter.FeaturedProjectAdapter;
import com.cse441.tluprojectexpo.model.FeaturedProjectUIModel;
import com.cse441.tluprojectexpo.admin.repository.ProjectRepository;

import java.util.ArrayList;
import java.util.List;

public class FeaturedManagementPage extends AppCompatActivity implements FeaturedProjectAdapter.OnProjectInteractionListener {

    private RecyclerView recyclerView;
    private ProgressBar progressBar;
    private FeaturedProjectAdapter adapter;
    private ProjectRepository projectRepository;
    private List<FeaturedProjectUIModel> uiModelList = new ArrayList<>();
    private ImageButton btnBackToDashboard;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_featured_management_page);

        recyclerView = findViewById(R.id.hold_featured_projects);
        progressBar = findViewById(R.id.progress_bar_loading);
        btnBackToDashboard = findViewById(R.id.back_to_dashboard);

        projectRepository = new ProjectRepository();
        adapter = new FeaturedProjectAdapter(this, uiModelList, this);

        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        recyclerView.setAdapter(adapter);

        loadFeaturedProjects();

        btnBackToDashboard.setOnClickListener(v -> finish());
    }

    private void loadFeaturedProjects() {
        progressBar.setVisibility(View.VISIBLE);
        // Phương thức này trong repository đã được sửa để lấy các project có IsFeatured == true
        projectRepository.getFeaturedProjectsForManagement().observe(this, featuredProjects -> {
            progressBar.setVisibility(View.GONE);
            if (featuredProjects != null) {
                adapter.updateData(featuredProjects);
            } else {
                Toast.makeText(this, "Lỗi khi tải dự án nổi bật.", Toast.LENGTH_SHORT).show();
            }
        });
    }

    // --- SỬA LẠI HOÀN TOÀN PHƯƠNG THỨC NÀY ---
    /**
     * Được gọi khi người dùng gạt TẮT switch "Nổi bật".
     * @param item Dữ liệu của item được tương tác.
     * @param isChecked Trạng thái mới của switch (luôn là false trong trường hợp này).
     */
    @Override
    public void onSwitchChanged(FeaturedProjectUIModel item, boolean isChecked) {
        // Logic này chỉ chạy khi người dùng gạt TẮT switch (isChecked == false)
        if (!isChecked) {
            Toast.makeText(this, "Đang bỏ nổi bật...", Toast.LENGTH_SHORT).show();

            // Gọi phương thức mới trong repository để đặt IsFeatured = false
            projectRepository.setProjectFeaturedStatus(item.getProjectId(), false, new ProjectRepository.OnTaskCompleteListener() {
                @Override
                public void onSuccess() {
                    Toast.makeText(FeaturedManagementPage.this, "Đã bỏ nổi bật: " + item.getProjectTitle(), Toast.LENGTH_SHORT).show();

                    // Tìm và xóa item khỏi danh sách hiện tại để giao diện cập nhật ngay lập tức
                    int positionToRemove = -1;
                    for (int i = 0; i < uiModelList.size(); i++) {
                        if (uiModelList.get(i).getProjectId().equals(item.getProjectId())) {
                            positionToRemove = i;
                            break;
                        }
                    }
                    if (positionToRemove != -1) {
                        adapter.removeItem(positionToRemove);
                    }
                }

                @Override
                public void onFailure(Exception e) {
                    Toast.makeText(FeaturedManagementPage.this, "Lỗi khi bỏ nổi bật", Toast.LENGTH_SHORT).show();
                    // Nếu lỗi, tải lại toàn bộ danh sách để đảm bảo dữ liệu đồng bộ
                    loadFeaturedProjects();
                }
            });
        }
    }

    /**
     * Được gọi khi người dùng nhấn vào "Xem chi tiết".
     * @param item Dữ liệu của item được nhấn.
     */
    @Override
    public void onViewMoreClicked(FeaturedProjectUIModel item) {
        // Logic chuyển trang của bạn ở đây
        Toast.makeText(this, "Xem chi tiết: " + item.getProjectTitle(), Toast.LENGTH_SHORT).show();
    }
}