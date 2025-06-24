package com.cse441.tluprojectexpo.admin;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.PopupMenu;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.cse441.tluprojectexpo.R;
import com.cse441.tluprojectexpo.admin.repository.ProjectRepository;
import com.cse441.tluprojectexpo.admin.utils.AppToast;
import com.cse441.tluprojectexpo.model.Comment;
import com.cse441.tluprojectexpo.model.Project;
import java.text.SimpleDateFormat;
import java.util.Locale;
import com.cse441.tluprojectexpo.ui.detailproject.adapter.ProjectMemberAdapter;
import com.cse441.tluprojectexpo.ui.detailproject.adapter.CommentAdapter;


public class ProjectDetailViewAdmin extends AppCompatActivity implements CommentAdapter.OnCommentInteractionListener {

    // Khai báo các View từ layout
    private ImageView imgThumb, isFeaturedIcon;
    private ImageButton backToHome;
    private TextView projectName, projectStatus, createdAt, updatedAt, likeCount, desc, authorName, categoryName, technologyNames, textViewMemberRole;
    private Button btnDeleteProject, btnMember, btnComment;
    private ImageButton optionProjectDetail;

    private LinearLayout btnGithub, btnVideoDemo;

    private ProjectRepository projectRepository;
    private ProjectMemberAdapter memberAdapter;
    private CommentAdapter commentAdapter;
    private Project currentProject;
    private RecyclerView recyclerViewMembers, recyclerViewComments;

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
            fetchAndDisplayProjectDetails(projectId);
        } else {
            AppToast.show(this, "Không tìm thấy thông tin dự án.", Toast.LENGTH_SHORT);
            finish();
        }
    }

    private void fetchAndDisplayProjectDetails(String projectId) {
        projectRepository.getProjectDetailsById(projectId).observe(this, project -> {
            if (project != null) {
                this.currentProject = project;
                populateUI(project);
                setupDataDependentListeners();
            } else {
                AppToast.show(this, "Tải thông tin dự án thất bại.", Toast.LENGTH_SHORT);
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
        btnGithub = findViewById(R.id.github_button);
        btnVideoDemo = findViewById(R.id.video_button);

        // Ánh xạ các View mới
        authorName = findViewById(R.id.author_name);
        categoryName = findViewById(R.id.category_name);
        technologyNames = findViewById(R.id.technology_names);
        isFeaturedIcon = findViewById(R.id.is_featured_icon);
        recyclerViewMembers = findViewById(R.id.recycler_view_members);
        recyclerViewComments = findViewById(R.id.recycler_view_comments);
        textViewMemberRole = findViewById(R.id.textViewMemberRole);
    }

    private void setupInitialListeners() {
        backToHome.setOnClickListener(v -> finish());
        btnGithub.setOnClickListener(v -> {
            if (currentProject != null && currentProject.getProjectUrl() != null) {
                Intent intent = new Intent(Intent.ACTION_VIEW, android.net.Uri.parse(currentProject.getProjectUrl()));
                startActivity(intent);
            } else {
                AppToast.show(this, "Không có liên kết GitHub cho dự án này.", Toast.LENGTH_SHORT);
            }
        });
        btnVideoDemo.setOnClickListener(v -> {
            if (currentProject != null && currentProject.getDemoUrl() != null) {
                Intent intent = new Intent(Intent.ACTION_VIEW, android.net.Uri.parse(currentProject.getDemoUrl()));
                startActivity(intent);
            } else {
                AppToast.show(this, "Không có liên kết video demo cho dự án này.", Toast.LENGTH_SHORT);
            }
        });
    }

    private void setupDataDependentListeners() {
        optionProjectDetail.setOnClickListener(this::showOptionsMenu);
        btnDeleteProject.setOnClickListener(v -> showDeleteConfirmationDialog());
        btnMember.setOnClickListener(v -> selectTab(v));
        btnComment.setOnClickListener(v -> selectTab(v));
    }

    // Hàm chính để đổ dữ liệu lên UI
    private void populateUI(@NonNull Project project) {
        Glide.with(this)
                .load(project.getThumbnailUrl())
                .placeholder(R.drawable.project_thumbnail)
                .error(R.drawable.project_thumbnail)
                .into(imgThumb);

        projectName.setText(project.getTitle());
        switch (project.getStatus().toLowerCase(Locale.getDefault())) {
            case "đang thực hiện":
                projectStatus.setText("Đang thực hiện");
                projectStatus.setBackgroundResource(R.drawable.progress_background);
                break;
            case "hoàn thành":
                projectStatus.setText("Đã hoàn thành");
                projectStatus.setBackgroundResource(R.drawable.finish);
                break;
            case "tạm dừng":
                projectStatus.setText("Tạm dừng");
                projectStatus.setBackgroundResource(R.drawable.stopped);
                break;
            default:
                break;
        }
        desc.setText(project.getDescription());
        likeCount.setText(String.valueOf(project.getVoteCount()));

        SimpleDateFormat dateFormat = new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault());
        if (project.getCreatedAt() != null) {
            createdAt.setText("Tạo: " + dateFormat.format(project.getCreatedAt().toDate()));
        }
        if (project.getUpdatedAt() != null) {
            updatedAt.setText("Cập nhật: " + dateFormat.format(project.getUpdatedAt().toDate()));
        }

        // Đổ dữ liệu cho các View mới
        authorName.setText("Tác giả: " + (project.getCreatorFullName() != null ? project.getCreatorFullName() : "N/A"));

        if (project.getCategoryNames() != null && !project.getCategoryNames().isEmpty()) {
            categoryName.setText("Lĩnh vực: " + String.join(", ", project.getCategoryNames()));
        } else {
            categoryName.setText("Lĩnh vực: Chưa xác định");
        }

        if (project.getTechnologyNames() != null && !project.getTechnologyNames().isEmpty()) {
            technologyNames.setText("Công nghệ: " + String.join(", ", project.getTechnologyNames()));
        } else {
            technologyNames.setText("Công nghệ: Chưa xác định");
        }

        isFeaturedIcon.setVisibility(project.isFeatured() ? View.VISIBLE : View.GONE);
        setupRecyclerViews(project);
        selectTab(btnMember); // Mặc định chọn tab Thành viên
    }

    private void setupRecyclerViews(Project project) {
        // Setup cho thành viên
        if (project.getProjectMembersInfo() != null) {
            memberAdapter = new ProjectMemberAdapter(this, project.getProjectMembersInfo()); // << DÙNG ProjectMemberAdapter
            recyclerViewMembers.setLayoutManager(new LinearLayoutManager(this));
            recyclerViewMembers.setAdapter(memberAdapter);
        }

        // Setup cho bình luận
        if (project.getComments() != null) {
            commentAdapter = new CommentAdapter(this, project.getComments(), this);
            recyclerViewComments.setLayoutManager(new LinearLayoutManager(this));
            recyclerViewComments.setAdapter(commentAdapter);
        }
    }

    private void selectTab(View selectedButton) {
        boolean isMemberSelected = selectedButton.getId() == R.id.btn_member;

        btnMember.setSelected(isMemberSelected);
        btnComment.setSelected(!isMemberSelected);

        if( isMemberSelected) {
            btnMember.setBackgroundResource(R.drawable.button_selected);
            btnMember.setTextColor(getResources().getColor(R.color.white));

            btnComment.setBackgroundResource(R.drawable.border_button_medium);
            btnComment.setTextColor(getResources().getColor(R.color.black));
        } else {
            btnMember.setBackgroundResource(R.drawable.border_button_medium);
            btnMember.setTextColor(getResources().getColor(R.color.black));

            btnComment.setBackgroundResource(R.drawable.button_selected);
            btnComment.setTextColor(getResources().getColor(R.color.white));
        }

        recyclerViewMembers.setVisibility(isMemberSelected ? View.VISIBLE : View.GONE);
        recyclerViewComments.setVisibility(!isMemberSelected ? View.VISIBLE : View.GONE);
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
                AppToast.show(ProjectDetailViewAdmin.this, message, Toast.LENGTH_SHORT);

                // Cập nhật trạng thái và trả về kết quả
                currentProject.setFeatured(newStatus);

                updateFeaturedIcon();

                Intent resultIntent = new Intent();
                resultIntent.putExtra("ACTION", "STATUS_CHANGED"); // Một action mới
                setResult(Activity.RESULT_OK, resultIntent);

                // Không cần đóng màn hình, chỉ cần cập nhật lại menu (nếu cần)
            }

            @Override
            public void onFailure(Exception e) {
                AppToast.show(ProjectDetailViewAdmin.this, "Cập nhật thất bại", Toast.LENGTH_SHORT);
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
                AppToast.show(ProjectDetailViewAdmin.this, "Đã xóa dự án thành công!", Toast.LENGTH_LONG);
                Intent resultIntent = new Intent();
                resultIntent.putExtra("ACTION", "DELETED");
                resultIntent.putExtra("PROJECT_ID", currentProject.getProjectId());
                setResult(Activity.RESULT_OK, resultIntent);
                finish(); // Đóng trang sau khi xóa
            }

            @Override
            public void onFailure(Exception e) {
                AppToast.show(ProjectDetailViewAdmin.this, "Xóa thất bại: " + e.getMessage(), Toast.LENGTH_SHORT);
            }
        });
    }

    @Override
    public void onReplyClicked(Comment parentComment) {
        AppToast.show(this, "Chức năng trả lời của Admin đang phát triển.", Toast.LENGTH_SHORT);
    }

    // THÊM HÀM PHỤ TRỢ MỚI NÀY VÀO
    private void updateFeaturedIcon() {
        if (currentProject != null && isFeaturedIcon != null) {
            isFeaturedIcon.setVisibility(currentProject.isFeatured() ? View.VISIBLE : View.GONE);
        }
    }
}