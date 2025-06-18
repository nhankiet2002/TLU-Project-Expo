package com.cse441.tluprojectexpo.Project.adapter;

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
import java.util.ArrayList;
import java.util.List;
import com.cse441.tluprojectexpo.R;
import com.cse441.tluprojectexpo.model.Project;

public class ProjectAdapter extends RecyclerView.Adapter<ProjectAdapter.ProjectViewHolder> {

    private List<Project> projectList;
    private Context context;
    private OnProjectClickListener listener;

    public interface OnProjectClickListener {
        void onProjectClick(Project project);
    }

    public ProjectAdapter(Context context, List<Project> projectList, OnProjectClickListener listener) {
        this.context = context;
        // Đảm bảo projectList không bao giờ null, giúp tránh NullPointerException ở những nơi khác
        this.projectList = projectList != null ? projectList : new ArrayList<>();
        this.listener = listener;
    }

    @NonNull
    @Override
    public ProjectViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        // Sử dụng context của parent để inflate layout, đây là cách làm tốt hơn
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.list_item, parent, false);
        return new ProjectViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ProjectViewHolder holder, int position) {
        Project project = projectList.get(position);
        // Kiểm tra project không null trước khi bind, mặc dù hiếm khi xảy ra nếu constructor và updateProjects được quản lý tốt
        if (project != null) {
            holder.bind(project, listener);
        }
    }

    @Override
    public int getItemCount() {
        return projectList.size();
    }

    /**
     * Cập nhật toàn bộ danh sách dự án.
     * Xóa danh sách cũ và thêm tất cả các dự án mới.
     * @param newProjects Danh sách dự án mới.
     */
    public void updateProjects(List<Project> newProjects) {
        this.projectList.clear();
        if (newProjects != null) { // Kiểm tra null cho newProjects
            this.projectList.addAll(newProjects);
        }
        notifyDataSetChanged(); // Thông báo cho RecyclerView cập nhật toàn bộ
    }

    /**
     * Thêm một danh sách các dự án vào cuối danh sách hiện tại.
     * @param moreProjects Danh sách các dự án cần thêm.
     */
    public void addProjects(List<Project> moreProjects) {
        if (moreProjects != null && !moreProjects.isEmpty()) { // Kiểm tra null và rỗng
            int startPosition = this.projectList.size();
            this.projectList.addAll(moreProjects);
            // Thông báo cho RecyclerView về các item mới được chèn, hiệu quả hơn notifyDataSetChanged()
            notifyItemRangeInserted(startPosition, moreProjects.size());
        }
    }

    /**
     * Xóa tất cả các dự án khỏi danh sách.
     */
    public void clearProjects() {
        this.projectList.clear();
        notifyDataSetChanged();
    }


    static class ProjectViewHolder extends RecyclerView.ViewHolder {
        ImageView imageViewProject;
        TextView textViewProjectName;
        TextView textViewDescription;
        TextView textViewAuthor;
        TextView textViewTechnology;
        // Thêm các View khác nếu có trong list_item.xml

        public ProjectViewHolder(@NonNull View itemView) {
            super(itemView);
            imageViewProject = itemView.findViewById(R.id.imageViewProject);
            textViewProjectName = itemView.findViewById(R.id.textViewProjectName);
            textViewDescription = itemView.findViewById(R.id.textViewDescription); // Giả sử bạn có TextView này
            textViewAuthor = itemView.findViewById(R.id.textViewAuthor);
            textViewTechnology = itemView.findViewById(R.id.textViewTechnology);
        }

        public void bind(final Project project, final OnProjectClickListener listener) {
            // Hiển thị tiêu đề dự án
            textViewProjectName.setText(project.getTitle());

            // Hiển thị mô tả dự án (nếu có TextView cho nó)
            // Bạn cần đảm bảo R.id.textViewDescription tồn tại trong layout list_item.xml
            if (textViewDescription != null) {
                // Giới hạn độ dài mô tả nếu cần để item không quá dài
                String description = project.getDescription();
                if (description != null && description.length() > 100) { // Ví dụ giới hạn 100 ký tự
                    description = description.substring(0, 100) + "...";
                }
                textViewDescription.setText(description);
            }


            // Hiển thị tên tác giả (CreatorFullName)
            // Trường này được @Exclude trong model Project, nghĩa là bạn phải tự load và gán giá trị cho nó
            // trước khi truyền Project object vào Adapter.
            if (project.getCreatorFullName() != null && !project.getCreatorFullName().isEmpty()) {
                textViewAuthor.setText("Tác giả: " + project.getCreatorFullName());
                textViewAuthor.setVisibility(View.VISIBLE);
            } else {
                // Nếu không có thông tin tác giả (chưa được load hoặc không có), có thể ẩn đi
                textViewAuthor.setVisibility(View.GONE);
                // Hoặc hiển thị một giá trị mặc định nếu muốn
                // textViewAuthor.setText("Tác giả: N/A");
            }

            // Hiển thị công nghệ (TechnologyNames)
            // Tương tự CreatorFullName, trường này cũng @Exclude và cần được load riêng.
            if (project.getTechnologyNames() != null && !project.getTechnologyNames().isEmpty()) {
                textViewTechnology.setText("Công nghệ: " + TextUtils.join(", ", project.getTechnologyNames()));
                textViewTechnology.setVisibility(View.VISIBLE);
            } else {
                // Nếu không có thông tin công nghệ, ẩn đi
                textViewTechnology.setVisibility(View.GONE);
                // Hoặc hiển thị giá trị mặc định
                // textViewTechnology.setText("Công nghệ: N/A");
            }

            // Tải ảnh thumbnail chính của dự án bằng Glide
            if (project.getThumbnailUrl() != null && !project.getThumbnailUrl().isEmpty()) {
                Glide.with(itemView.getContext()) // Sử dụng itemView.getContext() là an toàn nhất
                        .load(project.getThumbnailUrl())
                        .placeholder(R.drawable.ic_placeholder_image) // Ảnh hiển thị trong khi tải
                        .error(R.drawable.ic_image_error)       // Ảnh hiển thị nếu có lỗi tải
                        .centerCrop() // Hoặc fitCenter() tùy theo thiết kế
                        .into(imageViewProject);
            } else {
                // Nếu không có ThumbnailUrl, hiển thị ảnh placeholder mặc định
                imageViewProject.setImageResource(R.drawable.ic_placeholder_image);
            }

            // Xử lý sự kiện click vào một item dự án
            itemView.setOnClickListener(v -> {
                if (listener != null) {
                    listener.onProjectClick(project);
                }
            });
        }
    }
}