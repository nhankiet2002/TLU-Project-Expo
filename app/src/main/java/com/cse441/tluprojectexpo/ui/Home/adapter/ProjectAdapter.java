package com.cse441.tluprojectexpo.ui.Home.adapter; // Hoặc package đúng của bạn: com.cse441.tluprojectexpo.ui.createproject.adapter

import android.content.Context;
import android.text.TextUtils;
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

import java.util.ArrayList;
import java.util.List;

public class ProjectAdapter extends RecyclerView.Adapter<ProjectAdapter.ProjectViewHolder> {

    private List<Project> projectList;
    private Context context; // Context này có thể không cần thiết nếu bạn luôn dùng itemView.getContext()
    private OnProjectClickListener listener;

    public interface OnProjectClickListener {
        void onProjectClick(Project project);
    }

    public ProjectAdapter(Context context, List<Project> projectList, OnProjectClickListener listener) {
        this.context = context;
        this.projectList = projectList != null ? projectList : new ArrayList<>();
        this.listener = listener;
    }

    @NonNull
    @Override
    public ProjectViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.list_item_project, parent, false);
        return new ProjectViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ProjectViewHolder holder, int position) {
        Project project = projectList.get(position);
        if (project != null) {
            holder.bind(project, listener);
        }
    }

    @Override
    public int getItemCount() {
        return projectList.size();
    }

    public void updateProjects(List<Project> newProjects) {
        this.projectList.clear();
        if (newProjects != null) {
            this.projectList.addAll(newProjects);
        }
        notifyDataSetChanged();
    }

    public void addProjects(List<Project> moreProjects) {
        if (moreProjects != null && !moreProjects.isEmpty()) {
            int startPosition = this.projectList.size();
            this.projectList.addAll(moreProjects);
            notifyItemRangeInserted(startPosition, moreProjects.size());
        }
    }

    public void clearProjects() {
        this.projectList.clear();
        notifyDataSetChanged();
    }

    static class ProjectViewHolder extends RecyclerView.ViewHolder {
        ImageView imageViewProject;
        TextView textViewProjectName;
        TextView textViewCategory; // THAY ĐỔI: Thêm Category
        TextView textViewAuthor;
        // KHÔNG CÒN: textViewDescription, textViewTechnology
        ImageView imageViewFeaturedStar;

        public ProjectViewHolder(@NonNull View itemView) {
            super(itemView);
            imageViewProject = itemView.findViewById(R.id.imageViewProject);
            textViewProjectName = itemView.findViewById(R.id.textViewProjectName);
            textViewCategory = itemView.findViewById(R.id.textViewCategory); // THAY ĐỔI: Gán ID
            textViewAuthor = itemView.findViewById(R.id.textViewAuthor);
            imageViewFeaturedStar = itemView.findViewById(R.id.iv_featured_star);
        }

        public void bind(final Project project, final OnProjectClickListener listener) {
            // Hiển thị tiêu đề dự án
            textViewProjectName.setText(project.getTitle());

            // THAY ĐỔI: Hiển thị tên lĩnh vực/chủ đề (CategoryNames)
            // Giống như CreatorFullName và TechnologyNames trước đây,
            // categoryNames cũng là @Exclude và cần được load và set trong HomeFragment.
            if (project.getCategoryNames() != null && !project.getCategoryNames().isEmpty()) {
                // Nếu chỉ muốn hiển thị 1 category đầu tiên (ví dụ):
                // textViewCategory.setText("Lĩnh vực: " + project.getCategoryNames().get(0));
                // Hoặc hiển thị tất cả, nối bằng dấu phẩy:
                textViewCategory.setText("Lĩnh vực: " + TextUtils.join(", ", project.getCategoryNames()));
                textViewCategory.setVisibility(View.VISIBLE);
            } else {
                // Nếu không có thông tin lĩnh vực, ẩn đi
                textViewCategory.setVisibility(View.GONE);
                // Hoặc hiển thị giá trị mặc định
                // textViewCategory.setText("Lĩnh vực: Chưa xác định");
            }

            // Hiển thị tên tác giả (CreatorFullName)
            if (project.getCreatorFullName() != null && !project.getCreatorFullName().isEmpty()) {
                textViewAuthor.setText("Bởi: "+ project.getCreatorFullName());
                textViewAuthor.setVisibility(View.VISIBLE);
            } else {
                textViewAuthor.setVisibility(View.GONE);
            }
            if (project.isFeatured()) {
                imageViewFeaturedStar.setVisibility(View.VISIBLE);
            } else {
                imageViewFeaturedStar.setVisibility(View.GONE);
            }

            // KHÔNG CÒN LOGIC CHO textViewDescription và textViewTechnology

            // Tải ảnh thumbnail chính của dự án bằng Glide
            if (project.getThumbnailUrl() != null && !project.getThumbnailUrl().isEmpty()) {
                Glide.with(itemView.getContext())
                        .load(project.getThumbnailUrl())
                        .placeholder(R.drawable.ic_placeholder_image) // Đảm bảo bạn có drawable này
                        .error(R.drawable.ic_image_error)       // Đảm bảo bạn có drawable này
                        .centerCrop()
                        .into(imageViewProject);
            } else {
                imageViewProject.setImageResource(R.drawable.ic_placeholder_image); // Hoặc một placeholder khác
            }

            itemView.setOnClickListener(v -> {
                if (listener != null) {
                    listener.onProjectClick(project);
                }
            });
        }
    }
}