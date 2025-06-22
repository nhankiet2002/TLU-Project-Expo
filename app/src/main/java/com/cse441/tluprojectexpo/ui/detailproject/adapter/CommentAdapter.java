package com.cse441.tluprojectexpo.ui.detailproject.adapter;

import android.content.Context;
import android.text.format.DateUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.bumptech.glide.Glide;
import com.cse441.tluprojectexpo.R;
import com.cse441.tluprojectexpo.model.Comment; // Sử dụng model Comment của bạn
import java.util.List;

public class CommentAdapter extends RecyclerView.Adapter<CommentAdapter.ViewHolder> {
    private Context context;
    private List<Comment> comments; // Sử dụng model Comment của bạn

    public CommentAdapter(Context context, List<Comment> comments) {
        this.context = context;
        this.comments = comments;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_comment, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        Comment comment = comments.get(position);
        // Đảm bảo model Comment của bạn có các getter này
        holder.textViewCommenterName.setText(comment.getUserName());
        holder.textViewCommentContent.setText(comment.getText());

        if (comment.getTimestamp() != null) {
            holder.textViewCommentDate.setText(
                    DateUtils.getRelativeTimeSpanString(
                            comment.getTimestamp().toDate().getTime(),
                            System.currentTimeMillis(),
                            DateUtils.MINUTE_IN_MILLIS));
        } else {
            holder.textViewCommentDate.setText("");
        }

        Glide.with(context)
                .load(comment.getUserAvatarUrl())
                .placeholder(R.drawable.ic_default_avatar) // Cần drawable này
                .error(R.drawable.ic_default_avatar)       // Cần drawable này
                .circleCrop() // Làm avatar tròn
                .into(holder.imageViewCommenterAvatar);
    }

    @Override
    public int getItemCount() {
        return comments.size();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        ImageView imageViewCommenterAvatar;
        TextView textViewCommenterName;
        TextView textViewCommentContent;
        TextView textViewCommentDate;

        ViewHolder(View itemView) {
            super(itemView);
            // Đảm bảo các ID này khớp với file item_comment.xml
            imageViewCommenterAvatar = itemView.findViewById(R.id.imageViewCommenterAvatar);
            textViewCommenterName = itemView.findViewById(R.id.textViewCommenterName);
            textViewCommentContent = itemView.findViewById(R.id.textViewCommentContent);
            textViewCommentDate = itemView.findViewById(R.id.textViewCommentDate);
        }
    }
}