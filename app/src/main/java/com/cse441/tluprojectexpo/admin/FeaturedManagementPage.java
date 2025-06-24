package com.cse441.tluprojectexpo.admin;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.cse441.tluprojectexpo.R;
import com.cse441.tluprojectexpo.admin.adapter.FeaturedProjectAdapter;
import com.cse441.tluprojectexpo.admin.utils.AppToast;
import com.cse441.tluprojectexpo.admin.utils.NavigationUtil;
import com.cse441.tluprojectexpo.model.FeaturedProjectUIModel;
import com.cse441.tluprojectexpo.admin.repository.ProjectRepository;

import java.util.ArrayList;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;

public class FeaturedManagementPage extends AppCompatActivity implements FeaturedProjectAdapter.OnProjectInteractionListener {

    private static final long SEARCH_DELAY = 500;
    private static final int PROJECT_DETAIL_REQUEST_CODE = 2002;

    private RecyclerView recyclerView;
    private ProgressBar progressBar;
    private FeaturedProjectAdapter adapter;
    private ProjectRepository projectRepository;
    private List<FeaturedProjectUIModel> originalUiModelList = new ArrayList<>();
    private List<FeaturedProjectUIModel> displayedUiModelList = new ArrayList<>();

    private Timer searchTimer = new Timer();
    private ImageButton btnBackToDashboard;
    private EditText searchFeatured;
    private TextView totalFeaturedCount;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // Đảm bảo tên file layout của bạn là "activity_featured_management_page"
        setContentView(R.layout.activity_featured_management_page);

        recyclerView = findViewById(R.id.hold_featured_projects);
        progressBar = findViewById(R.id.progress_bar_loading);
        btnBackToDashboard = findViewById(R.id.back_to_dashboard);
        searchFeatured = findViewById(R.id.search_featured);
        totalFeaturedCount = findViewById(R.id.total_featured);

        projectRepository = new ProjectRepository();
        adapter = new FeaturedProjectAdapter(this, displayedUiModelList, this);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        recyclerView.setAdapter(adapter);

        loadFeaturedProjects();
        setupSearchListener();

        btnBackToDashboard.setOnClickListener(v -> finish());
    }

    // THÊM HÀM NÀY
    private void setupSearchListener() {
        searchFeatured.addTextChangedListener(new TextWatcher() {
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

    private void loadFeaturedProjects() {
        progressBar.setVisibility(View.VISIBLE);
        recyclerView.setVisibility(View.GONE);

        projectRepository.getFeaturedProjectsForManagement().observe(this, featuredProjects -> {
            progressBar.setVisibility(View.GONE);
            recyclerView.setVisibility(View.VISIBLE);

            if (featuredProjects != null) {
                Log.d("FeaturedPage", "Số dự án nổi bật lấy về: " + featuredProjects.size());

                // Cập nhật cả 2 danh sách
                originalUiModelList.clear();
                originalUiModelList.addAll(featuredProjects);

                updateDisplayedList(originalUiModelList);

                // Thông báo cho adapter rằng dữ liệu đã thay đổi và cần vẽ lại UI.
                adapter.notifyDataSetChanged();

            } else {
                totalFeaturedCount.setText("0 dự án");
                AppToast.show(this, "Lỗi khi tải dự án nổi bật.", Toast.LENGTH_SHORT);
                Log.e("FeaturedPage", "Danh sách dự án nổi bật trả về là null.");
            }
        });
    }

    // THÊM HÀM NÀY
    private void performSearch(String query) {
        if (query.isEmpty()) {
            // Nếu không tìm kiếm, hiển thị lại danh sách gốc
            updateDisplayedList(originalUiModelList);
            return;
        }

        // Thực hiện tìm kiếm trên danh sách gốc
        List<FeaturedProjectUIModel> searchResults = new ArrayList<>();
        String lowerCaseQuery = query.toLowerCase();

        for (FeaturedProjectUIModel project : originalUiModelList) {
            // Tìm kiếm không phân biệt hoa thường trên ProjectTitle
            if (project.getProjectTitle().toLowerCase().contains(lowerCaseQuery)) {
                searchResults.add(project);
            }
        }

        updateDisplayedList(searchResults);
    }

    // THÊM HÀM NÀY
    private void updateDisplayedList(List<FeaturedProjectUIModel> newList) {
        displayedUiModelList.clear();
        displayedUiModelList.addAll(newList);
        adapter.notifyDataSetChanged();

        // CẬP NHẬT SỐ LƯỢNG MỖI KHI DANH SÁCH THAY ĐỔI
        if (totalFeaturedCount != null) {
            String countText = newList.size() + " dự án";
            totalFeaturedCount.setText(countText);
        }
    }


    @Override
    public void onSwitchChanged(FeaturedProjectUIModel item, boolean isChecked) {
        // Logic này chỉ chạy khi người dùng gạt TẮT switch (isChecked == false)
        if (!isChecked) {
            AppToast.show(this, "Đang bỏ nổi bật...", Toast.LENGTH_SHORT);

            projectRepository.setProjectFeaturedStatus(item.getProjectId(), false, new ProjectRepository.OnTaskCompleteListener() {
                @Override
                public void onSuccess() {
                    AppToast.show(FeaturedManagementPage.this, "Đã bỏ nổi bật: " + item.getProjectTitle(), Toast.LENGTH_SHORT);

                    originalUiModelList.remove(item);
                    updateDisplayedList(new ArrayList<>(originalUiModelList));
                }

                @Override
                public void onFailure(Exception e) {
                    AppToast.show(FeaturedManagementPage.this, "Lỗi khi bỏ nổi bật: " + e.getMessage(), Toast.LENGTH_SHORT);
                    // Nếu lỗi, tải lại toàn bộ danh sách để đảm bảo dữ liệu đồng bộ
                    loadFeaturedProjects();
                }
            });
        }
    }

    @Override
    public void onViewMoreClicked(FeaturedProjectUIModel item) {
        NavigationUtil.navigateWithObjectForResult(this, ProjectDetailViewAdmin.class, "PROJECT_ID", item.getProjectId(), PROJECT_DETAIL_REQUEST_CODE);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        // 1. Kiểm tra xem đây có phải kết quả từ trang chi tiết không
        if (requestCode == PROJECT_DETAIL_REQUEST_CODE) {
            // 2. Kiểm tra xem hành động ở trang chi tiết có thành công không
            if (resultCode == Activity.RESULT_OK) {
                // 3. Nếu thành công, cách đơn giản và an toàn nhất là tải lại toàn bộ danh sách
                //    Điều này xử lý được cả trường hợp XÓA và BỎ NỔI BẬT từ trang chi tiết.
                AppToast.show(this, "Đang cập nhật danh sách nổi bật...", Toast.LENGTH_SHORT);
                loadFeaturedProjects();
            }
        }
    }
}