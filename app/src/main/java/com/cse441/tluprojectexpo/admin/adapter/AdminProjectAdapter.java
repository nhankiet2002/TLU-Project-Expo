package com.cse441.tluprojectexpo.admin.adapter;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.bumptech.glide.Glide;
import com.cse441.tluprojectexpo.R;
import com.cse441.tluprojectexpo.model.Project;
import com.google.android.material.button.MaterialButton;

import java.util.List;
import java.util.Locale;

public class AdminProjectAdapter extends RecyclerView.Adapter<AdminProjectAdapter.ProjectViewHolder> {

    private final Context context;
    private final List<Project> projectList;
    private final OnProjectAdminInteraction listener;

    public interface OnProjectAdminInteraction {
        void onSetFeaturedClick(Project project, int position);
        void onProjectClick(Project project);
    }

    public AdminProjectAdapter(Context context, List<Project> projectList, OnProjectAdminInteraction listener) {
        this.context = context;
        this.projectList = projectList;
        this.listener = listener;
    }

    @NonNull
    @Override
    public ProjectViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        // Lưu ý: Đảm bảo tên file layout của bạn là "item_admin_project.xml"
        View view = LayoutInflater.from(context).inflate(R.layout.item_project_admin, parent, false);
        return new ProjectViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ProjectViewHolder holder, int position) {
        Project project = projectList.get(position);
        holder.bind(project, listener, context);
    }

    @Override
    public int getItemCount() {
        return projectList == null ? 0 : projectList.size();
    }

    public static class ProjectViewHolder extends RecyclerView.ViewHolder {
        // Sử dụng chính xác tên biến camelCase bạn đã chọn
        ImageView thumbnail, iconStar;
        TextView projectName, field, technology, creator, projectStatus;
        MaterialButton btnMakeFeatured;

        public ProjectViewHolder(@NonNull View itemView) {
            super(itemView);
            // Ánh xạ View với đúng ID và tên biến của bạn
            thumbnail = itemView.findViewById(R.id.thumbnail);
            iconStar = itemView.findViewById(R.id.iv_featured_star);
            projectName = itemView.findViewById(R.id.project_name);
            field = itemView.findViewById(R.id.field);
            technology = itemView.findViewById(R.id.technology);
            creator = itemView.findViewById(R.id.creator);
            projectStatus = itemView.findViewById(R.id.project_status);
            btnMakeFeatured = itemView.findViewById(R.id.btn_set_featured);
        }

        public void bind(final Project project, final OnProjectAdminInteraction listener, Context context) {
            // 1. Tải ảnh
            Glide.with(context)
                    .load(project.getThumbnailUrl())
                    .placeholder(R.drawable.image7) // Đảm bảo bạn có drawable tên "image7"
                    .error(R.color.button_gray_action_background_color) // Đảm bảo bạn có color này
                    .into(thumbnail);

            // 2. Đổ dữ liệu text - Sửa lại các getter cho đúng với model Project.java
            projectName.setText(project.getTitle());

            // Sử dụng getCategoryNames() cho trường "field"
            if (project.getCategoryNames() != null && !project.getCategoryNames().isEmpty()) {
                field.setText("Lĩnh vực: " + String.join(", ", project.getCategoryNames()));
            } else {
                field.setText("Lĩnh vực: Chưa xác định");
            }

            // Sử dụng getTechnologyNames() cho trường "technology"
            if (project.getTechnologyNames() != null && !project.getTechnologyNames().isEmpty()) {
                technology.setText("Công nghệ: " + String.join(", ", project.getTechnologyNames()));
            } else {
                technology.setText("Công nghệ: Chưa xác định");
            }

            // Sử dụng getCreatorFullName() cho trường "creator"
            creator.setText("Tác giả: " + (project.getCreatorFullName() != null ? project.getCreatorFullName() : "Chưa có"));

            // 3. Xử lý logic ẩn/hiện cho Ngôi sao và nút
            if (project.isFeatured()) {
                iconStar.setVisibility(View.VISIBLE);
                btnMakeFeatured.setVisibility(View.GONE);
            } else {
                iconStar.setVisibility(View.GONE);
                btnMakeFeatured.setVisibility(View.VISIBLE);
            }

            // 4. Đặt text và background cho trạng thái
            setProjectStatus(projectStatus, project.getStatus());

            // 5. Gán sự kiện click
            btnMakeFeatured.setOnClickListener(v -> {
                if (listener != null) {
                    listener.onSetFeaturedClick(project, getAdapterPosition());
                }
            });

            itemView.setOnClickListener(v -> {
                if (listener != null) {
                    listener.onProjectClick(project);
                }
            });
        }

        // Sửa lại hoàn chỉnh hàm này
        private void setProjectStatus(TextView textView, String status) {
            if (status == null || status.trim().isEmpty()) {
                textView.setVisibility(View.GONE);
                return;
            }

            textView.setVisibility(View.VISIBLE);
            textView.setText(status);

            // So sánh không phân biệt hoa thường
            switch (status.toLowerCase(Locale.getDefault())) {
                case "đang thực hiện":
                    textView.setText("Đang thực hiện");
                    textView.setBackgroundResource(R.drawable.progress_background);
                    break;
                case "hoàn thành":
                    textView.setText("Đã hoàn thành");
                    textView.setBackgroundResource(R.drawable.finish);
                    break;
                case "tạm dừng":
                    textView.setText("Tạm dừng");
                    textView.setBackgroundResource(R.drawable.stopped);
                    break;
                default:
                    break;
            }
        }
    }
}