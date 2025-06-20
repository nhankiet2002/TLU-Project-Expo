package com.cse441.tluprojectexpo.ui.createproject.adapter;

// UserProjectsAdapter.java

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.bumptech.glide.Glide;
import java.util.ArrayList;
import java.util.List;
import com.cse441.tluprojectexpo.R; // Thay đổi package
import com.cse441.tluprojectexpo.model.Project; // Thay đổi package

public class UserProjectsAdapter extends RecyclerView.Adapter<UserProjectsAdapter.ProjectViewHolder> {

    private List<Project> projectList;
    private Context context;
    private OnProjectActionListener listener;

    public interface OnProjectActionListener {
        void onEditClick(Project project);
        void onDeleteClick(Project project);
        void onItemClick(Project project); // Cho phép click vào toàn bộ item nếu cần
    }

    public UserProjectsAdapter(Context context, List<Project> projectList, OnProjectActionListener listener) {
        this.context = context;
        this.projectList = (projectList != null) ? projectList : new ArrayList<>();
        this.listener = listener;
    }

    @NonNull
    @Override
    public ProjectViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.list_item_project_actions, parent, false);
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

    public void removeProject(Project project) {
        int position = projectList.indexOf(project);
        if (position > -1) {
            projectList.remove(position);
            notifyItemRemoved(position);
            // Optional: notifyItemRangeChanged(position, projectList.size());
        }
    }

    public void clearProjects() {
        this.projectList.clear();
        notifyDataSetChanged();
    }

    static class ProjectViewHolder extends RecyclerView.ViewHolder {
        ImageView imageViewProject;
        TextView textViewProjectTitle, textViewProjectDescription;
        Button buttonEdit, buttonDelete;

        public ProjectViewHolder(@NonNull View itemView) {
            super(itemView);
            imageViewProject = itemView.findViewById(R.id.imageViewProject);
            textViewProjectTitle = itemView.findViewById(R.id.textViewProjectTitle);
            textViewProjectDescription = itemView.findViewById(R.id.textViewProjectDescription);
            buttonEdit = itemView.findViewById(R.id.buttonEdit);
            buttonDelete = itemView.findViewById(R.id.buttonDelete);
        }

        public void bind(final Project project, final OnProjectActionListener listener) {
            textViewProjectTitle.setText(project.getTitle());
            textViewProjectDescription.setText(project.getDescription());

            if (project.getThumbnailUrl() != null && !project.getThumbnailUrl().isEmpty()) {
                Glide.with(itemView.getContext())
                        .load(project.getThumbnailUrl())
                        .placeholder(R.drawable.ic_placeholder_image) // Tạo drawable này
                        .error(R.drawable.ic_image_error) // Tạo drawable này
                        .into(imageViewProject);
            } else {
                imageViewProject.setImageResource(R.drawable.ic_placeholder_image);
            }

            buttonEdit.setOnClickListener(v -> {
                if (listener != null) listener.onEditClick(project);
            });

            buttonDelete.setOnClickListener(v -> {
                if (listener != null) listener.onDeleteClick(project);
            });

            itemView.setOnClickListener(v -> {
                if (listener != null) listener.onItemClick(project);
            });
        }
    }
}
