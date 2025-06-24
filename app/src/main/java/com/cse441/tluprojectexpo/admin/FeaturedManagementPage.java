package com.cse441.tluprojectexpo.admin;

import android.os.Bundle;
import android.util.Log;
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
        // Đảm bảo tên file layout của bạn là "activity_featured_management_page"
        setContentView(R.layout.activity_featured_management_page);

        recyclerView = findViewById(R.id.hold_featured_projects);
        progressBar = findViewById(R.id.progress_bar_loading);
        btnBackToDashboard = findViewById(R.id.back_to_dashboard);

        projectRepository = new ProjectRepository();
        // Adapter được khởi tạo với uiModelList. Mọi thay đổi trên list này
        // sẽ được adapter nhận biết sau khi gọi notifyDataSetChanged().
        adapter = new FeaturedProjectAdapter(this, uiModelList, this);

        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        recyclerView.setAdapter(adapter);

        loadFeaturedProjects();

        btnBackToDashboard.setOnClickListener(v -> finish());
    }

    private void loadFeaturedProjects() {
        progressBar.setVisibility(View.VISIBLE);
        recyclerView.setVisibility(View.GONE);

        projectRepository.getFeaturedProjectsForManagement().observe(this, featuredProjects -> {
            progressBar.setVisibility(View.GONE);
            recyclerView.setVisibility(View.VISIBLE);

            if (featuredProjects != null) {
                Log.d("FeaturedPage", "Số dự án nổi bật lấy về: " + featuredProjects.size());

                // Cập nhật danh sách mà adapter đang giữ tham chiếu
                this.uiModelList.clear();
                this.uiModelList.addAll(featuredProjects);

                // Thông báo cho adapter rằng dữ liệu đã thay đổi và cần vẽ lại UI.
                adapter.notifyDataSetChanged();

            } else {
                Toast.makeText(this, "Lỗi khi tải dự án nổi bật.", Toast.LENGTH_SHORT).show();
                Log.e("FeaturedPage", "Danh sách dự án nổi bật trả về là null.");
            }
        });
    }

    @Override
    public void onSwitchChanged(FeaturedProjectUIModel item, boolean isChecked) {
        // Logic này chỉ chạy khi người dùng gạt TẮT switch (isChecked == false)
        if (!isChecked) {
            Toast.makeText(this, "Đang bỏ nổi bật...", Toast.LENGTH_SHORT).show();

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
                        // Gọi phương thức removeItem từ adapter
                        adapter.removeItem(positionToRemove);
                    }
                }

                @Override
                public void onFailure(Exception e) {
                    Toast.makeText(FeaturedManagementPage.this, "Lỗi khi bỏ nổi bật: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    // Nếu lỗi, tải lại toàn bộ danh sách để đảm bảo dữ liệu đồng bộ
                    loadFeaturedProjects();
                }
            });
        }
    }

    @Override
    public void onViewMoreClicked(FeaturedProjectUIModel item) {
        // Logic chuyển sang trang chi tiết của bạn ở đây
        Toast.makeText(this, "Xem chi tiết: " + item.getProjectTitle(), Toast.LENGTH_SHORT).show();
    }
}