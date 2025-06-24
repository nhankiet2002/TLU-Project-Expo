package com.cse441.tluprojectexpo.ui.Notification.adapter;

import android.content.Context;
import android.graphics.Typeface;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.PopupMenu;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.bumptech.glide.Glide;
import com.cse441.tluprojectexpo.R;
import com.cse441.tluprojectexpo.model.Notification;
import java.text.SimpleDateFormat;
import java.util.List;
import java.util.Locale;

public class NotificationAdapter extends RecyclerView.Adapter<NotificationAdapter.NotificationViewHolder> {
    private List<Notification> notifications;
    private NotificationActionListener listener;
    private SimpleDateFormat dateFormat;
    private Context context; // Thêm context để sử dụng cho PopupMenu và string resources

    public interface NotificationActionListener {
        void onNotificationClicked(Notification notification);
        void onAcceptInvite(Notification notification);
        void onDeclineInvite(Notification notification);
        // Thêm các phương thức mới
        void onToggleReadStatus(Notification notification);
        void onDeleteNotification(Notification notification);
    }

    public NotificationAdapter(Context context, List<Notification> notifications, NotificationActionListener listener) {
        this.context = context; // Khởi tạo context
        this.notifications = notifications;
        this.listener = listener;
        this.dateFormat = new SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault());
    }

    @NonNull
    @Override
    public NotificationViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_notification, parent, false);
        return new NotificationViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull NotificationViewHolder holder, int position) {
        Notification notification = notifications.get(position);
        holder.bind(notification);
    }

    @Override
    public int getItemCount() {
        return notifications.size();
    }

    public void updateNotifications(List<Notification> newNotifications) {
        this.notifications = newNotifications;
        notifyDataSetChanged();
    }

    // Phương thức để cập nhật một item cụ thể (ví dụ sau khi thay đổi trạng thái read)
    public void updateNotificationItem(int position, Notification notification) {
        if (position >= 0 && position < notifications.size()) {
            notifications.set(position, notification);
            notifyItemChanged(position);
        }
    }

    // Phương thức để xóa một item cụ thể
    public void removeNotificationItem(int position) {
        if (position >= 0 && position < notifications.size()) {
            notifications.remove(position);
            notifyItemRemoved(position);
            notifyItemRangeChanged(position, notifications.size()); // Cập nhật vị trí các item còn lại
        }
    }


    class NotificationViewHolder extends RecyclerView.ViewHolder {
        private ImageView ivAvatarOrIcon;
        private TextView tvTitle;
        private TextView tvContent;
        private TextView tvTime;
        private LinearLayout layoutInviteActions;
        private Button btnAccept, btnDecline;
        private ImageView ivMoreOptions; // Thêm ImageView cho menu

        public NotificationViewHolder(@NonNull View itemView) {
            super(itemView);
            ivAvatarOrIcon = itemView.findViewById(R.id.ivAvatarOrIcon);
            tvTitle = itemView.findViewById(R.id.tvTitle);
            tvContent = itemView.findViewById(R.id.tvContent);
            tvTime = itemView.findViewById(R.id.tvTime);
            layoutInviteActions = itemView.findViewById(R.id.layoutInviteActions);
            btnAccept = itemView.findViewById(R.id.btnAccept);
            btnDecline = itemView.findViewById(R.id.btnDecline);
            ivMoreOptions = itemView.findViewById(R.id.ivMoreOptions); // Khởi tạo ivMoreOptions

            itemView.setOnClickListener(v -> {
                int position = getAdapterPosition();
                if (position != RecyclerView.NO_POSITION && listener != null) {
                    listener.onNotificationClicked(notifications.get(position));
                }
            });

            // Thiết lập listener cho ivMoreOptions
            ivMoreOptions.setOnClickListener(v -> {
                int position = getAdapterPosition();
                if (position != RecyclerView.NO_POSITION && listener != null) {
                    showPopupMenu(v, notifications.get(position));
                }
            });
        }

        private void showPopupMenu(View anchorView, Notification notification) {
            PopupMenu popup = new PopupMenu(context, anchorView);
            popup.getMenuInflater().inflate(R.menu.notification_options_menu, popup.getMenu());

            // Thay đổi tiêu đề của item "Đánh dấu đã đọc/chưa đọc" dựa trên trạng thái hiện tại
            android.view.MenuItem toggleReadItem = popup.getMenu().findItem(R.id.action_toggle_read);
            if (notification.isRead()) {
                toggleReadItem.setTitle(R.string.mark_as_unread);
            } else {
                toggleReadItem.setTitle(R.string.mark_as_read);
            }

            popup.setOnMenuItemClickListener(item -> {
                int itemId = item.getItemId();
                if (itemId == R.id.action_toggle_read) {
                    if (listener != null) {
                        listener.onToggleReadStatus(notification);
                    }
                    return true;
                } else if (itemId == R.id.action_delete) {
                    if (listener != null) {
                        listener.onDeleteNotification(notification);
                    }
                    return true;
                }
                return false;
            });
            popup.show();
        }


        public void bind(Notification notification) {
            // Reset visibility and style
            layoutInviteActions.setVisibility(View.GONE);
            btnAccept.setVisibility(View.GONE);
            btnDecline.setVisibility(View.GONE);
            ivAvatarOrIcon.setImageResource(R.drawable.ic_default_avatar); // Default icon

            // Phân biệt trực quan thông báo đã đọc và chưa đọc
            if (notification.isRead()) {
                itemView.setAlpha(0.7f); // Ví dụ: làm mờ item đã đọc
                tvTitle.setTypeface(null, Typeface.NORMAL);
                tvContent.setTypeface(null, Typeface.NORMAL);
            } else {
                itemView.setAlpha(1.0f);
                tvTitle.setTypeface(null, Typeface.BOLD); // Tiêu đề của thông báo chưa đọc sẽ in đậm
                tvContent.setTypeface(null, Typeface.NORMAL);
            }


            // Thời gian
            if (notification.getCreatedAt() != null) {
                tvTime.setText(dateFormat.format(notification.getCreatedAt().toDate()));
            } else {
                tvTime.setText("");
            }

            // Nội dung chính
            tvContent.setText(notification.getMessage());

            switch (notification.getType()) {
                case "PROJECT_INVITATION":
                    if (notification.getActorAvatarUrl() != null && !notification.getActorAvatarUrl().isEmpty()) {
                        Glide.with(ivAvatarOrIcon.getContext())
                                .load(notification.getActorAvatarUrl())
                                .placeholder(R.drawable.ic_default_avatar)
                                .error(R.drawable.ic_default_avatar)
                                .circleCrop()
                                .into(ivAvatarOrIcon);
                    } else {
                        ivAvatarOrIcon.setImageResource(R.drawable.ic_default_avatar);
                    }
                    tvTitle.setText(notification.getActorFullName() != null ? notification.getActorFullName() : "Người dùng");
                    // tvTitle style (bold/normal) is handled by isRead status check above

                    if ("pending".equalsIgnoreCase(notification.getInvitationStatus())) {
                        layoutInviteActions.setVisibility(View.VISIBLE);
                        btnAccept.setVisibility(View.VISIBLE);
                        btnDecline.setVisibility(View.VISIBLE);
                    } else {
                        layoutInviteActions.setVisibility(View.GONE);
                    }
                    btnAccept.setOnClickListener(v -> {
                        if (listener != null) listener.onAcceptInvite(notification);
                    });
                    btnDecline.setOnClickListener(v -> {
                        if (listener != null) listener.onDeclineInvite(notification);
                    });
                    break;
                case "NEW_COMMENT":
                case "NEW_REPLY":
                    if (notification.getActorAvatarUrl() != null && !notification.getActorAvatarUrl().isEmpty()) {
                        Glide.with(ivAvatarOrIcon.getContext())
                                .load(notification.getActorAvatarUrl())
                                .placeholder(R.drawable.ic_default_avatar)
                                .error(R.drawable.ic_default_avatar)
                                .circleCrop()
                                .into(ivAvatarOrIcon);
                    } else {
                        ivAvatarOrIcon.setImageResource(R.drawable.ic_default_avatar);
                    }
                    tvTitle.setText(notification.getActorFullName() != null ? notification.getActorFullName() : "Người dùng");
                    break;
                case "PROJECT_VOTE":
                case "COMMENT_VOTE":
                    ivAvatarOrIcon.setImageResource(R.drawable.ic_thumb_up); // Hoặc avatar người vote
                    tvTitle.setText(notification.getActorFullName() != null ? notification.getActorFullName() : "Người dùng");
                    break;
                case "INVITATION_ACCEPTED":
                case "INVITATION_DECLINED":
                    ivAvatarOrIcon.setImageResource(R.drawable.ic_bell); // Hoặc avatar người phản hồi
                    tvTitle.setText(notification.getActorFullName() != null ? notification.getActorFullName() : "Người dùng");
                    break;
                default:
                    // Mặc định, có thể dùng avatar nếu có hoặc icon chung
                    if (notification.getActorAvatarUrl() != null && !notification.getActorAvatarUrl().isEmpty()) {
                        Glide.with(ivAvatarOrIcon.getContext())
                                .load(notification.getActorAvatarUrl())
                                .placeholder(R.drawable.ic_default_avatar)
                                .error(R.drawable.ic_default_avatar)
                                .circleCrop()
                                .into(ivAvatarOrIcon);
                    } else {
                        ivAvatarOrIcon.setImageResource(R.drawable.ic_bell); // Icon thông báo chung
                    }
                    tvTitle.setText(notification.getActorFullName() != null ? notification.getActorFullName() : "Thông báo hệ thống");
                    break;
            }
            // Ensure title boldness is primarily driven by isRead, but allow overrides if necessary
            if (notification.isRead()) {
                tvTitle.setTypeface(null, Typeface.NORMAL);
            } else {
                // For unread, if type specific bolding is not set, set it bold.
                // If switch case already set to bold, this is redundant but harmless.
                if(tvTitle.getTypeface() == null || tvTitle.getTypeface().getStyle() != Typeface.BOLD){
                    tvTitle.setTypeface(null, Typeface.BOLD);
                }
            }
        }
    }
}