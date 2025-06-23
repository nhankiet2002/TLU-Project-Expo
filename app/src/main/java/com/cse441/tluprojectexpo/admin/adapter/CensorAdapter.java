package com.cse441.tluprojectexpo.admin.adapter;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.cse441.tluprojectexpo.R;
import com.cse441.tluprojectexpo.model.Project;
import java.text.SimpleDateFormat;
import java.util.List;
import java.util.Locale;

public class CensorAdapter extends RecyclerView.Adapter<CensorAdapter.CensorViewHolder> {

    private final Context context;
    private final List<Project> projectList;
    private final OnCensorInteractionListener listener;

    public interface OnCensorInteractionListener {
        void onAcceptClick(Project project, int position);
        void onRejectClick(Project project, int position);
        void onItemClick(Project project);
    }

    public CensorAdapter(Context context, List<Project> projectList, OnCensorInteractionListener listener) {
        this.context = context;
        this.projectList = projectList;
        this.listener = listener;
    }

    @NonNull
    @Override
    public CensorViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_censor, parent, false);
        return new CensorViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull CensorViewHolder holder, int position) {
        Project project = projectList.get(position);
        holder.bind(project, listener);
    }

    @Override
    public int getItemCount() {
        return projectList.size();
    }

    public static class CensorViewHolder extends RecyclerView.ViewHolder {
        TextView projectName, user, field, createdAt;
        ImageButton acceptButton, rejectButton;

        public CensorViewHolder(@NonNull View itemView) {
            super(itemView);
            projectName = itemView.findViewById(R.id.project_name);
            user = itemView.findViewById(R.id.user);
            field = itemView.findViewById(R.id.field);
            createdAt = itemView.findViewById(R.id.created_at);
            acceptButton = itemView.findViewById(R.id.accept);
            rejectButton = itemView.findViewById(R.id.reject);
        }

        public void bind(final Project project, final OnCensorInteractionListener listener) {
            projectName.setText(project.getTitle());
            user.setText("Bởi: " + (project.getCreatorFullName() != null ? project.getCreatorFullName() : "N/A"));

            if (project.getCategoryNames() != null && !project.getCategoryNames().isEmpty()) {
                field.setText(project.getCategoryNames().get(0));
            } else {
                field.setText("Chưa có");
            }

            SimpleDateFormat dateFormat = new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault());
            if (project.getCreatedAt() != null) {
                createdAt.setText(dateFormat.format(project.getCreatedAt().toDate()));
            }

            acceptButton.setOnClickListener(v -> listener.onAcceptClick(project, getAdapterPosition()));
            rejectButton.setOnClickListener(v -> listener.onRejectClick(project, getAdapterPosition()));
            itemView.setOnClickListener(v -> listener.onItemClick(project));
        }
    }
}