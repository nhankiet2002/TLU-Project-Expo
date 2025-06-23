package com.cse441.tluprojectexpo.ui.detailproject.adapter;

import android.content.Context;
import android.text.format.DateUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout; // Để set padding
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.recyclerview.widget.RecyclerView;
import com.bumptech.glide.Glide;
import com.cse441.tluprojectexpo.R;
import com.cse441.tluprojectexpo.model.Comment;
// Import DisplayableCommentItem
import com.cse441.tluprojectexpo.ui.detailproject.adapter.DisplayableCommentItem; // Đường dẫn tới DisplayableCommentItem

import java.util.ArrayList;
import java.util.List;

public class CommentAdapter extends RecyclerView.Adapter<CommentAdapter.ViewHolder> {
    private Context context;
    private List<DisplayableCommentItem> displayableItems; // Sử dụng danh sách mới

    // Interface để xử lý click vào nút trả lời
    public interface OnCommentInteractionListener {
        void onReplyClicked(Comment parentComment);
    }
    private OnCommentInteractionListener interactionListener;


    public CommentAdapter(Context context, List<Comment> rootComments, OnCommentInteractionListener listener) {
        this.context = context;
        this.interactionListener = listener;
        this.displayableItems = new ArrayList<>();
        buildDisplayableList(rootComments);
    }

    // Hàm xây dựng danh sách phẳng từ cây bình luận
    private void buildDisplayableList(List<Comment> rootComments) {
        this.displayableItems.clear();
        for (Comment rootComment : rootComments) {
            addCommentAndRepliesToDisplayList(rootComment, 0);
        }
    }

    private void addCommentAndRepliesToDisplayList(Comment comment, int depth) {
        if (depth == 0) {
            displayableItems.add(new DisplayableCommentItem(comment, DisplayableCommentItem.TYPE_ROOT_COMMENT, depth));
        } else {
            displayableItems.add(new DisplayableCommentItem(comment, DisplayableCommentItem.TYPE_REPLY_COMMENT, depth));
        }
        if (comment.getReplies() != null) {
            for (Comment reply : comment.getReplies()) {
                addCommentAndRepliesToDisplayList(reply, depth + 1);
            }
        }
    }

    public void updateComments(List<Comment> newRootComments) {
        buildDisplayableList(newRootComments);
        notifyDataSetChanged();
    }

    // Phương thức thêm một comment mới vào đầu (có thể là gốc hoặc reply)
    public void addCommentToDisplayList(Comment newComment, @Nullable String parentIdToReply) {
        // TODO: Cần logic phức tạp hơn để chèn reply vào đúng vị trí trong displayableItems
        // Hiện tại, đơn giản là thêm vào đầu nếu là comment gốc, hoặc rebuild nếu là reply
        if (parentIdToReply == null) {
            displayableItems.add(0, new DisplayableCommentItem(newComment, DisplayableCommentItem.TYPE_ROOT_COMMENT, 0));
            notifyItemInserted(0);
        } else {
            // Cần tìm parent comment trong cấu trúc dữ liệu gốc (không phải displayableItems)
            // và sau đó rebuild lại displayableItems. Đây là phần phức tạp.
            // Tạm thời, để đơn giản, bạn có thể gọi lại fetchProjectCommentsWithReplies.
            // Hoặc, bạn cần một cách hiệu quả hơn để cập nhật cây và danh sách hiển thị.
            // Ví dụ đơn giản (không hiệu quả lắm cho nhiều reply):
            // 1. Tìm parent trong cấu trúc dữ liệu gốc.
            // 2. Add reply vào parent đó.
            // 3. Gọi lại buildDisplayableList với rootComments đã được cập nhật.
            // 4. Gọi notifyDataSetChanged().
            // Đây là một giới hạn của việc flatten list, việc cập nhật động có thể khó.
            // Chúng ta sẽ xử lý việc post reply đơn giản trước.
        }
    }

    public DisplayableCommentItem getDisplayableItemAt(int position) {
        return displayableItems.get(position);
    }

    @Override
    public int getItemViewType(int position) {
        return displayableItems.get(position).type;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        // Bạn có thể dùng layout khác nhau cho root và reply nếu muốn
        // Hiện tại dùng chung một layout item_comment.xml
        View view = LayoutInflater.from(context).inflate(R.layout.item_comment, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        DisplayableCommentItem displayableItem = displayableItems.get(position);
        Comment comment = displayableItem.comment;

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
                .placeholder(R.drawable.ic_default_avatar)
                .error(R.drawable.ic_default_avatar)
                .circleCrop()
                .into(holder.imageViewCommenterAvatar);

        // Thụt lề cho các replies
        int paddingStart = (int) (displayableItem.depth * context.getResources().getDimension(R.dimen.comment_reply_indent)); // Tạo dimens.xml: <dimen name="comment_reply_indent">16dp</dimen>
        holder.itemView.setPadding(paddingStart, holder.itemView.getPaddingTop(), holder.itemView.getPaddingRight(), holder.itemView.getPaddingBottom());

        // Xử lý click nút "Trả lời"
        holder.buttonReply.setOnClickListener(v -> {
            if (interactionListener != null) {
                interactionListener.onReplyClicked(comment);
            }
        });
    }

    @Override
    public int getItemCount() {
        return displayableItems.size();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        ImageView imageViewCommenterAvatar;
        TextView textViewCommenterName;
        TextView textViewCommentContent;
        TextView textViewCommentDate;
        TextView buttonReply; // Thêm nút trả lời vào item_comment.xml

        ViewHolder(View itemView) {
            super(itemView);
            imageViewCommenterAvatar = itemView.findViewById(R.id.imageViewCommenterAvatar);
            textViewCommenterName = itemView.findViewById(R.id.textViewCommenterName);
            textViewCommentContent = itemView.findViewById(R.id.textViewCommentContent);
            textViewCommentDate = itemView.findViewById(R.id.textViewCommentDate);
            buttonReply = itemView.findViewById(R.id.buttonReply); // ID của nút trả lời
        }
    }
}