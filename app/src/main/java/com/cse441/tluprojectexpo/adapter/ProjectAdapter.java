package com.cse441.tluprojectexpo.adapter;

// ProjectAdapter.java

import android.content.Context;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.bumptech.glide.Glide; // Hoặc Picasso
import java.util.ArrayList;
import java.util.List;
import com.cse441.tluprojectexpo.R; // Thay đổi package
import com.cse441.tluprojectexpo.model.Project; // Thay đổi package

public class ProjectAdapter extends RecyclerView.Adapter<ProjectAdapter.ProjectViewHolder> {

    private List<Project> projectList;
    private Context context;
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
        View view = LayoutInflater.from(context).inflate(R.layout.list_item, parent, false);
        return new ProjectViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ProjectViewHolder holder, int position) {
        Project project = projectList.get(position);
        holder.bind(project, listener);
    }

    @Override
    public int getItemCount() {
        return projectList.size();
    }

    public void updateProjects(List<Project> newProjects) {
        this.projectList.clear();
        this.projectList.addAll(newProjects);
        notifyDataSetChanged();
    }

    public void addProjects(List<Project> moreProjects) {
        int startPosition = this.projectList.size();
        this.projectList.addAll(moreProjects);
        notifyItemRangeInserted(startPosition, moreProjects.size());
    }

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

        public ProjectViewHolder(@NonNull View itemView) {
            super(itemView);
            imageViewProject = itemView.findViewById(R.id.imageViewProject);
            textViewProjectName = itemView.findViewById(R.id.textViewProjectName);
            textViewDescription = itemView.findViewById(R.id.textViewDescription);
            textViewAuthor = itemView.findViewById(R.id.textViewAuthor);
            textViewTechnology = itemView.findViewById(R.id.textViewTechnology);
        }

        public void bind(final Project project, final OnProjectClickListener listener) {
            textViewProjectName.setText(project.getTitle());
            textViewDescription.setText(project.getDescription());

            // Hiển thị tên tác giả (nếu đã được load)
            if (project.getCreatorFullName() != null && !project.getCreatorFullName().isEmpty()) {
                textViewAuthor.setText("Tác giả: " + project.getCreatorFullName());
                textViewAuthor.setVisibility(View.VISIBLE);
            } else {
                textViewAuthor.setText("Tác giả: Đang tải..."); // Hoặc ẩn đi
                textViewAuthor.setVisibility(View.GONE);
            }

            // Hiển thị công nghệ (nếu đã được load)
            if (project.getTechnologyNames() != null && !project.getTechnologyNames().isEmpty()) {
                textViewTechnology.setText("Công nghệ: " + TextUtils.join(", ", project.getTechnologyNames()));
                textViewTechnology.setVisibility(View.VISIBLE);
            } else {
                textViewTechnology.setText("Công nghệ: Đang tải..."); // Hoặc ẩn đi
                textViewTechnology.setVisibility(View.GONE);
            }


            // Tải ảnh thumbnail bằng Glide (hoặc Picasso)
            if (project.getThumbnailUrl() != null && !project.getThumbnailUrl().isEmpty()) {
                Glide.with(itemView.getContext())
                        .load(project.getThumbnailUrl())
                        .placeholder(R.drawable.ic_placeholder_image) // Ảnh placeholder
                        .error(R.drawable.error) // Ảnh khi lỗi
                        .into(imageViewProject);
            } else {
                // Nếu không có ThumbnailUrl, hiển thị ảnh mặc định
                imageViewProject.setImageResource(R.drawable.ic_placeholder_image);
            }

            itemView.setOnClickListener(v -> {
                if (listener != null) {
                    listener.onProjectClick(project);
                }
            });
        }
    }
}