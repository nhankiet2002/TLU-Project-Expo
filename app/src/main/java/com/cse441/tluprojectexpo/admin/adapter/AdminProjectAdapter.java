package com.cse441.tluprojectexpo.admin.adapter;

import android.content.Context;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.bumptech.glide.Glide;
import com.cse441.tluprojectexpo.R;
import com.cse441.tluprojectexpo.model.Project;

import java.util.List;

public class AdminProjectAdapter extends RecyclerView.Adapter<AdminProjectAdapter.ViewHolder> {

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
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_project_admin, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        Project project = projectList.get(position);
        holder.bind(project);
    }

    @Override
    public int getItemCount() {
        return projectList.size();
    }

    public void updateData(List<Project> newList) {
        projectList.clear();
        projectList.addAll(newList);
        notifyDataSetChanged();
    }

    class ViewHolder extends RecyclerView.ViewHolder {
        ImageView thumbnail, ivFeaturedStar;
        TextView projectName, field, technology, creator, projectStatus;
        Button btnSetFeatured;

        ViewHolder(View itemView) {
            super(itemView);
            thumbnail = itemView.findViewById(R.id.thumbnail);
            ivFeaturedStar = itemView.findViewById(R.id.iv_featured_star);
            projectName = itemView.findViewById(R.id.project_name);
            field = itemView.findViewById(R.id.field);
            technology = itemView.findViewById(R.id.technology);
            creator = itemView.findViewById(R.id.creator);
            btnSetFeatured = itemView.findViewById(R.id.btn_set_featured);
            projectStatus = (TextView) itemView.findViewById(R.id.project_status);
        }

        void bind(final Project project) {
            projectName.setText(project.getTitle());
            creator.setText("Tác giả: " + (project.getCreatorFullName() != null ? project.getCreatorFullName() : "N/A"));

            String status = project.getStatus();
            if (status != null && !status.isEmpty()) {
                projectStatus.setText(status);
                projectStatus.setVisibility(View.VISIBLE);

                // Kiểm tra giá trị của status và đặt background tương ứng
                if (status.equalsIgnoreCase("Hoàn thành")) {
                    projectStatus.setText("Đã hoàn thành");
                    projectStatus.setBackgroundResource(R.drawable.finish);
                } else if (status.equalsIgnoreCase("Đang thực hiện")) {
                    projectStatus.setText("Đang thực hiện");
                    projectStatus.setBackgroundResource(R.drawable.progress_background);
                }else{
                    projectStatus.setText("Tạm dừng");
                    projectStatus.setBackgroundResource(R.drawable.stopped);
                }
            }

            if (project.getCategoryNames() != null && !project.getCategoryNames().isEmpty()) {
                field.setText("Lĩnh vực: " + project.getCategoryNames().get(0));
            } else { field.setText("Lĩnh vực: N/A"); }

            if (project.getTechnologyNames() != null && !project.getTechnologyNames().isEmpty()) {
                technology.setText("Công nghệ: " + TextUtils.join(", ", project.getTechnologyNames()));
            } else { technology.setText("Công nghệ: N/A"); }

            Glide.with(context).load(project.getThumbnailUrl()).placeholder(R.drawable.image7).into(thumbnail);

            if (project.isFeatured()) {
                ivFeaturedStar.setVisibility(View.VISIBLE);
                btnSetFeatured.setVisibility(View.GONE);
            } else {
                ivFeaturedStar.setVisibility(View.GONE);
                btnSetFeatured.setVisibility(View.VISIBLE);
            }

            btnSetFeatured.setOnClickListener(v -> {
                if (listener != null) listener.onSetFeaturedClick(project, getAdapterPosition());
            });

            itemView.setOnClickListener(v -> {
                if(listener != null) listener.onProjectClick(project);
            });
        }
    }
}