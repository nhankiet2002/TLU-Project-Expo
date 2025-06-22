package com.cse441.tluprojectexpo.ui.detailproject.adapter;

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
import com.cse441.tluprojectexpo.model.Project; // Model Project chứa UserShortInfo
import java.util.List;

public class ProjectMemberAdapter extends RecyclerView.Adapter<ProjectMemberAdapter.ViewHolder> {
    private Context context;
    private List<Project.UserShortInfo> members; // Sử dụng UserShortInfo từ model Project của bạn

    public ProjectMemberAdapter(Context context, List<Project.UserShortInfo> members) {
        this.context = context;
        this.members = members;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_member, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        Project.UserShortInfo member = members.get(position);
        // Đảm bảo UserShortInfo của bạn có các getter này
        holder.textViewMemberName.setText(member.getFullName());
        holder.textViewMemberRole.setText(member.getRoleInProject());

        Glide.with(context)
                .load(member.getAvatarUrl())
                .placeholder(R.drawable.ic_default_avatar) // Cần drawable này
                .error(R.drawable.ic_default_avatar)       // Cần drawable này
                .circleCrop() // Làm avatar tròn
                .into(holder.imageViewMemberAvatar);
    }

    @Override
    public int getItemCount() {
        return members.size();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        ImageView imageViewMemberAvatar;
        TextView textViewMemberName;
        TextView textViewMemberRole;

        ViewHolder(View itemView) {
            super(itemView);
            // Đảm bảo các ID này khớp với file item_member.xml
            imageViewMemberAvatar = itemView.findViewById(R.id.imageViewMemberAvatar);
            textViewMemberName = itemView.findViewById(R.id.textViewMemberName);
            textViewMemberRole = itemView.findViewById(R.id.textViewMemberRole);
        }
    }
}