package com.cse441.tluprojectexpo.admin;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.PopupMenu;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import com.bumptech.glide.Glide;
import com.cse441.tluprojectexpo.R;
import com.cse441.tluprojectexpo.admin.repository.ProjectRepository;
import com.cse441.tluprojectexpo.model.Project;
import java.text.SimpleDateFormat;
import java.util.Locale;

public class ProjectDetailViewAdmin extends AppCompatActivity {

    // Khai báo các View từ layout
    private ImageView imgThumb, backToHome;
    private TextView projectName, projectStatus, createdAt, updatedAt, likeCount, desc;
    private Button btnDeleteProject, btnMember, btnComment;
    private ImageButton optionProjectDetail;

    // Repository và project hiện tại
    private ProjectRepository projectRepository;
    private Project currentProject;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_admin_project_detail);

        projectRepository = new ProjectRepository();
        bindViews();
        setupInitialListeners();

        // Lấy projectId từ Intent
        String projectId = getIntent().getStringExtra("PROJECT_ID");

        if (projectId != null && !projectId.isEmpty()) {
            // Dùng ID để tải dữ liệu chi tiết
            fetchAndDisplayProjectDetails(projectId);
        } else {
            Toast.makeText(this, "Không tìm thấy thông tin dự án.", Toast.LENGTH_SHORT).show();
            finish();
        }
    }

    private void fetchAndDisplayProjectDetails(String projectId) {
        projectRepository.getProjectDetailsById(projectId).observe(this, project -> {
            if (project != null) {
                // 1. Dữ liệu đã về, gán cho biến toàn cục
                this.currentProject = project;
                // 2. Đổ dữ liệu lên UI
                populateUI(project);
                // 3. SAU KHI CÓ DỮ LIỆU, MỚI GÁN CÁC LISTENER CÒN LẠI
                setupDataDependentListeners();
            } else {
                Toast.makeText(this, "Tải thông tin dự án thất bại.", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void bindViews() {
        imgThumb = findViewById(R.id.img_thumb);
        backToHome = findViewById(R.id.back_to_home);
        optionProjectDetail = findViewById(R.id.option_project_detail);
        projectName = findViewById(R.id.project_name);
        projectStatus = findViewById(R.id.project_status);
        createdAt = findViewById(R.id.created_at);
        updatedAt = findViewById(R.id.updated_at);
        likeCount = findViewById(R.id.like_count);
        desc = findViewById(R.id.desc);
        btnMember = findViewById(R.id.btn_member);
        btnComment = findViewById(R.id.btn_comment);
        btnDeleteProject = findViewById(R.id.btn_delete_project);
    }

    private void setupInitialListeners() {
        backToHome.setOnClickListener(v -> finish());
    }

    private void setupDataDependentListeners() {
        optionProjectDetail.setOnClickListener(this::showOptionsMenu);
        btnDeleteProject.setOnClickListener(v -> showDeleteConfirmationDialog());
    }

    // Hàm chính để đổ dữ liệu lên UI
    private void populateUI(@NonNull Project project) {
        // Tải ảnh thumbnail bằng thư viện Glide
        Glide.with(this)
                .load(project.getThumbnailUrl())
                .placeholder(R.drawable.project_thumbnail) // Ảnh hiển thị trong lúc tải
                .error(R.drawable.project_thumbnail) // Ảnh hiển thị khi lỗi
                .into(imgThumb);

        // Fill các thông tin text
        projectName.setText(project.getTitle());
        projectStatus.setText(project.getStatus());
        desc.setText(project.getDescription());
        likeCount.setText(String.valueOf(project.getVoteCount()));

        // Format ngày tháng cho dễ đọc
        SimpleDateFormat dateFormat = new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault());
        if (project.getCreatedAt() != null) {
            createdAt.setText("Tạo: " + dateFormat.format(project.getCreatedAt().toDate()));
        } else {
            createdAt.setText("Tạo: N/A");
        }
        if (project.getUpdatedAt() != null) {
            updatedAt.setText("Cập nhật: " + dateFormat.format(project.getUpdatedAt().toDate()));
        } else {
            updatedAt.setText("Cập nhật: N/A");
        }
    }

    // Hàm hiển thị menu tùy chọn khi nhấn nút 3 chấm
    // Hàm hiển thị menu tùy chọn khi nhấn nút 3 chấm
    private void showOptionsMenu(View view) {
        // Hàm này giờ sẽ an toàn vì nó chỉ được gọi khi currentProject không còn null
        PopupMenu popupMenu = new PopupMenu(this, view);
        if (currentProject.isFeatured()) {
            popupMenu.getMenu().add("Bỏ nổi bật");
        } else {
            popupMenu.getMenu().add("Thêm vào dự án nổi bật");
        }
        popupMenu.getMenu().add("Xóa dự án");

        popupMenu.setOnMenuItemClickListener(item -> {
            String title = item.getTitle().toString();
            if (title.contains("nổi bật")) {
                toggleFeaturedStatus();
            } else if (title.equals("Xóa dự án")) {
                showDeleteConfirmationDialog();
            }
            return true;
        });
        popupMenu.show();
    }


    // Hàm xử lý logic khi nhấn nút "Thêm/Bỏ nổi bật"
    private void toggleFeaturedStatus() {
        boolean newStatus = !currentProject.isFeatured();
        projectRepository.setProjectFeaturedStatus(currentProject.getProjectId(), newStatus, new ProjectRepository.OnTaskCompleteListener() {
            @Override
            public void onSuccess() {
                String message = newStatus ? "Đã thêm vào dự án nổi bật" : "Đã bỏ nổi bật dự án";
                Toast.makeText(ProjectDetailViewAdmin.this, message, Toast.LENGTH_SHORT).show();

                // Cập nhật trạng thái và trả về kết quả
                currentProject.setFeatured(newStatus);
                Intent resultIntent = new Intent();
                resultIntent.putExtra("ACTION", "STATUS_CHANGED"); // Một action mới
                setResult(Activity.RESULT_OK, resultIntent);

                // Không cần đóng màn hình, chỉ cần cập nhật lại menu (nếu cần)
            }

            @Override
            public void onFailure(Exception e) {
                Toast.makeText(ProjectDetailViewAdmin.this, "Cập nhật thất bại", Toast.LENGTH_SHORT).show();
            }
        });
    }

    // Hàm hiển thị dialog xác nhận xóa
    private void showDeleteConfirmationDialog() {
        new AlertDialog.Builder(this)
                .setTitle("Xác nhận xóa")
                .setMessage("Bạn có chắc chắn muốn xóa dự án '" + currentProject.getTitle() + "' không? Hành động này không thể hoàn tác.")
                .setPositiveButton("Xóa", (dialog, which) -> deleteCurrentProject())
                .setNegativeButton("Hủy", null)
                .setIcon(android.R.drawable.ic_dialog_alert)
                .show();
    }

    // Hàm xử lý logic xóa và trả kết quả
    private void deleteCurrentProject() {
        projectRepository.deleteProject(currentProject.getProjectId(), new ProjectRepository.OnTaskCompleteListener() {
            @Override
            public void onSuccess() {
                Toast.makeText(ProjectDetailViewAdmin.this, "Đã xóa dự án thành công!", Toast.LENGTH_LONG).show();
                Intent resultIntent = new Intent();
                resultIntent.putExtra("ACTION", "DELETED");
                resultIntent.putExtra("PROJECT_ID", currentProject.getProjectId());
                setResult(Activity.RESULT_OK, resultIntent);
                finish(); // Đóng trang sau khi xóa
            }

            @Override
            public void onFailure(Exception e) {
                Toast.makeText(ProjectDetailViewAdmin.this, "Xóa thất bại: " + e.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }
}