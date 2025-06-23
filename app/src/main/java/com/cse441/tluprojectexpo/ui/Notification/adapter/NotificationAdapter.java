package com.cse441.tluprojectexpo.ui.Notification.adapter;

import android.graphics.Typeface;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.cse441.tluprojectexpo.R;
import com.cse441.tluprojectexpo.model.Notification;
import com.google.firebase.Timestamp;

import java.text.SimpleDateFormat;
import java.util.List;
import java.util.Locale;

public class NotificationAdapter extends RecyclerView.Adapter<NotificationAdapter.NotificationViewHolder> {
    private List<Notification> notifications;
    private NotificationActionListener listener;
    private SimpleDateFormat dateFormat;

    public interface NotificationActionListener {
        void onNotificationClicked(Notification notification);
        void onAcceptInvite(Notification notification);
        void onDeclineInvite(Notification notification);
    }

    public NotificationAdapter(List<Notification> notifications, NotificationActionListener listener) {
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

    class NotificationViewHolder extends RecyclerView.ViewHolder {
        private ImageView ivAvatarOrIcon;
        private TextView tvTitle;
        private TextView tvContent;
        private TextView tvTime;
        private LinearLayout layoutInviteActions;
        private Button btnAccept, btnDecline;
        private View separator;

        public NotificationViewHolder(@NonNull View itemView) {
            super(itemView);
            ivAvatarOrIcon = itemView.findViewById(R.id.ivAvatarOrIcon);
            tvTitle = itemView.findViewById(R.id.tvTitle);
            tvContent = itemView.findViewById(R.id.tvContent);
            tvTime = itemView.findViewById(R.id.tvTime);
            layoutInviteActions = itemView.findViewById(R.id.layoutInviteActions);
            btnAccept = itemView.findViewById(R.id.btnAccept);
            btnDecline = itemView.findViewById(R.id.btnDecline);
            separator = itemView.findViewById(R.id.separator);

            itemView.setOnClickListener(v -> {
                int position = getAdapterPosition();
                if (position != RecyclerView.NO_POSITION && listener != null) {
                    listener.onNotificationClicked(notifications.get(position));
                }
            });
        }

        public void bind(Notification notification) {
            // Reset visibility
            layoutInviteActions.setVisibility(View.GONE);
            btnAccept.setVisibility(View.GONE);
            btnDecline.setVisibility(View.GONE);
            tvTitle.setTypeface(null, Typeface.NORMAL);
            ivAvatarOrIcon.setImageResource(R.drawable.ic_default_avatar);

            // Thời gian
            if (notification.getCreatedAt() != null) {
                tvTime.setText(dateFormat.format(notification.getCreatedAt().toDate()));
            } else {
                tvTime.setText("");
            }

            switch (notification.getType()) {
                case "PROJECT_INVITATION":
                    // Avatar, tên người gửi, nội dung, 2 nút
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
                    tvTitle.setTypeface(null, Typeface.BOLD);
                    tvContent.setText(notification.getMessage());
                    if ("pending".equalsIgnoreCase(notification.getInvitationStatus())) {
                        layoutInviteActions.setVisibility(View.VISIBLE);
                        btnAccept.setVisibility(View.VISIBLE);
                        btnDecline.setVisibility(View.VISIBLE);
                    } else {
                        layoutInviteActions.setVisibility(View.GONE);
                        btnAccept.setVisibility(View.GONE);
                        btnDecline.setVisibility(View.GONE);
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
                    tvTitle.setTypeface(null, Typeface.BOLD);
                    tvContent.setText(notification.getMessage());
                    break;
                case "PROJECT_VOTE":
                case "COMMENT_VOTE":
                    // Có thể dùng icon vote hoặc avatar
                    ivAvatarOrIcon.setImageResource(R.drawable.ic_thumb_up);
                    tvTitle.setText(notification.getActorFullName() != null ? notification.getActorFullName() : "Người dùng");
                    tvTitle.setTypeface(null, Typeface.BOLD);
                    tvContent.setText(notification.getMessage());
                    break;
                case "INVITATION_ACCEPTED":
                case "INVITATION_DECLINED":
                    ivAvatarOrIcon.setImageResource(R.drawable.ic_bell);
                    tvTitle.setText(notification.getActorFullName() != null ? notification.getActorFullName() : "Người dùng");
                    tvTitle.setTypeface(null, Typeface.BOLD);
                    tvContent.setText(notification.getMessage());
                    break;
                default:
                    ivAvatarOrIcon.setImageResource(R.drawable.ic_default_avatar);
                    tvTitle.setText(notification.getActorFullName() != null ? notification.getActorFullName() : "Người dùng");
                    tvTitle.setTypeface(null, Typeface.BOLD);
                    tvContent.setText(notification.getMessage());
                    break;
            }
        }
    }
} 