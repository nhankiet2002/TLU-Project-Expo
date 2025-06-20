package com.cse441.tluprojectexpo.admin;

import android.os.Bundle;
import android.view.View;
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

// ĐÂY LÀ KHAI BÁO LỚP CHÍNH, KHÔNG CÓ LỚP NÀO LỒNG BÊN TRONG
// Activity này sẽ implement interface từ Adapter để lắng nghe các sự kiện click
public class FeaturedManagementPage extends AppCompatActivity implements FeaturedProjectAdapter.OnProjectInteractionListener {

    // Khai báo các thành phần UI và logic
    private RecyclerView recyclerView;
    private ProgressBar progressBar;
    private FeaturedProjectAdapter adapter;
    private ProjectRepository projectRepository;
    private List<FeaturedProjectUIModel> uiModelList = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // Đảm bảo tên file layout của bạn là chính xác
        setContentView(R.layout.activity_featured_management_page);

        // 1. Ánh xạ các View từ layout
        // Hãy chắc chắn rằng bạn có RecyclerView và ProgressBar với các ID này trong file XML
        recyclerView = findViewById(R.id.hold_featured_projects);
        progressBar = findViewById(R.id.progress_bar_loading);

        // 2. Khởi tạo các đối tượng logic
        projectRepository = new ProjectRepository();

        // 3. Khởi tạo Adapter và truyền 'this' vì Activity này đã implement interface listener
        adapter = new FeaturedProjectAdapter(this, uiModelList, this);

        // 4. Thiết lập cho RecyclerView
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        recyclerView.setAdapter(adapter);

        // 5. Bắt đầu tải dữ liệu
        loadFeaturedProjects();
    }

    /**
     * Gọi Repository để lấy dữ liệu các dự án nổi bật đã được xử lý.
     * Sử dụng LiveData để lắng nghe kết quả một cách an toàn.
     */
    private void loadFeaturedProjects() {
        progressBar.setVisibility(View.VISIBLE);
        projectRepository.getFeaturedProjectsWithDetails().observe(this, featuredProjects -> {
            progressBar.setVisibility(View.GONE);
            if (featuredProjects != null) {
                // Cập nhật dữ liệu cho adapter khi có kết quả
                adapter.updateData(featuredProjects);
            } else {
                Toast.makeText(this, "Lỗi khi tải dự án nổi bật.", Toast.LENGTH_SHORT).show();
            }
        });
    }

    // --- Các phương thức được implement từ OnProjectInteractionListener của Adapter ---

    /**
     * Được gọi khi người dùng gạt switch trên một item.
     * @param item Dữ liệu của item được tương tác.
     * @param isChecked Trạng thái mới của switch.
     */
    @Override
    public void onSwitchChanged(FeaturedProjectUIModel item, boolean isChecked) {
        // Vì đây là màn hình "Nổi bật", nên switch luôn bật.
        // Logic này chỉ chạy khi người dùng gạt TẮT switch.
        if (!isChecked) {
            Toast.makeText(this, "Đang bỏ nổi bật...", Toast.LENGTH_SHORT).show();

            projectRepository.removeFeaturedProject(item.getProjectId(), new ProjectRepository.OnTaskCompleteListener() {
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
        // Viết logic để chuyển sang trang chi tiết dự án tại đây
        // Ví dụ:
        // Intent intent = new Intent(this, ProjectDetailActivity.class);
        // intent.putExtra("PROJECT_ID", item.getProjectId());
        // startActivity(intent);

        Toast.makeText(this, "Xem chi tiết: " + item.getProjectTitle(), Toast.LENGTH_SHORT).show();
    }
}