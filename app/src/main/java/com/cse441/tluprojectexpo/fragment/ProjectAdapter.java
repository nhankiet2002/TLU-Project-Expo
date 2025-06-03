package com.cse441.tluprojectexpo.fragment;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.cse441.tluprojectexpo.R;
import com.cse441.tluprojectexpo.model.Project; // Đảm bảo đúng đường dẫn
import java.util.List;

public class ProjectAdapter extends RecyclerView.Adapter<ProjectAdapter.ProjectViewHolder> {

    private List<Project> projectList;
    private Context context; // Thêm context nếu cần cho Glide hoặc các thao tác khác

    public ProjectAdapter(Context context, List<Project> projectList) {
        this.context = context;
        this.projectList = projectList;
    }

    @NonNull
    @Override
    public ProjectViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.list_item, parent, false);
        return new ProjectViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ProjectViewHolder holder, int position) {
        Project project = projectList.get(position);

        holder.textViewProjectName.setText(project.getName());
        holder.textViewDescription.setText(project.getDescription());
        holder.textViewAuthor.setText(project.getAuthor());
        holder.textViewTechnology.setText(project.getTechnology());

    }

    @Override
    public int getItemCount() {
        return projectList == null ? 0 : projectList.size();
    }

    // Hàm để cập nhật dữ liệu
    public void setData(List<Project> newList) {
        this.projectList.clear();
        if (newList != null) {
            this.projectList.addAll(newList);
        }
        notifyDataSetChanged(); // Hoặc sử dụng DiffUtil để hiệu quả hơn
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
    }
}