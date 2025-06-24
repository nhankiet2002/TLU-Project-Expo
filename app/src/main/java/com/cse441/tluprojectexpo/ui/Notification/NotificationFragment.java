package com.cse441.tluprojectexpo.ui.Notification;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import com.cse441.tluprojectexpo.R;
import com.cse441.tluprojectexpo.admin.utils.AppToast;
import com.cse441.tluprojectexpo.model.Notification;
import com.cse441.tluprojectexpo.service.FirestoreService;
import com.cse441.tluprojectexpo.ui.Notification.adapter.NotificationAdapter;
import com.cse441.tluprojectexpo.ui.detailproject.ProjectDetailActivity; // Đảm bảo đường dẫn này đúng
import com.cse441.tluprojectexpo.utils.Constants;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.ArrayList;
import java.util.List;

public class NotificationFragment extends Fragment implements NotificationAdapter.NotificationActionListener {

    private static final String TAG = "NotificationFragment";

    private RecyclerView recyclerViewNotifications;
    private NotificationAdapter notificationAdapter;
    private List<Notification> notificationList;
    private SwipeRefreshLayout swipeRefreshLayout;
    private ProgressBar progressBarInitialLoad;
    private LinearLayout emptyView;

    private FirestoreService firestoreService;
    private String currentUserId;
    private FirebaseFirestore db;


    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_notification, container, false);

        // Khởi tạo Views
        recyclerViewNotifications = view.findViewById(R.id.recyclerViewNotifications);
        swipeRefreshLayout = view.findViewById(R.id.swipeRefreshLayout);
        progressBarInitialLoad = view.findViewById(R.id.progressBarInitialLoad);
        emptyView = view.findViewById(R.id.emptyView);

        // Khởi tạo Services và Firebase
        firestoreService = new FirestoreService();
        db = FirebaseFirestore.getInstance();
        FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
        if (currentUser != null) {
            currentUserId = currentUser.getUid();
        }

        // Khởi tạo RecyclerView
        notificationList = new ArrayList<>();
        // Truyền context (getContext()) vào adapter, getContext() an toàn ở đây
        if (getContext() != null) {
            notificationAdapter = new NotificationAdapter(getContext(), notificationList, this);
            recyclerViewNotifications.setLayoutManager(new LinearLayoutManager(getContext()));
            recyclerViewNotifications.setAdapter(notificationAdapter);
        } else {
            Log.e(TAG, "Context is null during adapter initialization in onCreateView.");
            // Có thể hiển thị thông báo lỗi hoặc không làm gì cả, tùy thuộc vào luồng ứng dụng
        }


        // Thiết lập SwipeRefreshLayout
        swipeRefreshLayout.setOnRefreshListener(this::loadNotifications);

        // Tải dữ liệu ban đầu
        if (currentUserId != null) {
            // Hiển thị ProgressBar cho lần tải đầu tiên nếu danh sách rỗng
            if (notificationList.isEmpty()) {
                progressBarInitialLoad.setVisibility(View.VISIBLE);
                recyclerViewNotifications.setVisibility(View.GONE);
                emptyView.setVisibility(View.GONE);
            }
            loadNotifications();
        } else {
            // Xử lý trường hợp người dùng chưa đăng nhập
            progressBarInitialLoad.setVisibility(View.GONE);
            swipeRefreshLayout.setEnabled(false); // Vô hiệu hóa refresh nếu chưa login
            emptyView.setVisibility(View.VISIBLE);
            recyclerViewNotifications.setVisibility(View.GONE);
            if (getContext() != null) {
                AppToast.show(getContext(), "Vui lòng đăng nhập để xem thông báo.", Toast.LENGTH_LONG);
            }
        }

        return view;
    }

    private void loadNotifications() {
        if (currentUserId == null) {
            swipeRefreshLayout.setRefreshing(false);
            progressBarInitialLoad.setVisibility(View.GONE);
            updateUIBasedOnData(); // Đảm bảo empty view hiển thị nếu cần
            return;
        }

        // Chỉ hiển thị progressBarInitialLoad nếu không phải là refresh từ swipe và list rỗng
        if (!swipeRefreshLayout.isRefreshing() && notificationList.isEmpty()) {
            progressBarInitialLoad.setVisibility(View.VISIBLE);
            recyclerViewNotifications.setVisibility(View.GONE);
            emptyView.setVisibility(View.GONE);
        }

        db.collection(Constants.COLLECTION_NOTIFICATIONS)
                .whereEqualTo("recipientUserId", currentUserId)
                .orderBy("createdAt", Query.Direction.DESCENDING)
                .get()
                .addOnCompleteListener(task -> {
                    progressBarInitialLoad.setVisibility(View.GONE);
                    swipeRefreshLayout.setRefreshing(false);

                    if (task.isSuccessful() && task.getResult() != null) {
                        notificationList.clear();
                        for (QueryDocumentSnapshot document : task.getResult()) {
                            Notification notification = document.toObject(Notification.class);
                            notificationList.add(notification);
                        }
                        if (notificationAdapter != null) {
                            notificationAdapter.updateNotifications(new ArrayList<>(notificationList)); // Tạo copy để tránh lỗi ConcurrentModification
                        }
                        updateUIBasedOnData();
                    } else {
                        Log.e(TAG, "Lỗi tải thông báo: ", task.getException());
                        if (getContext() != null) {
                            AppToast.show(getContext(), getString(R.string.error_loading_notifications) + (task.getException() != null ? ": " + task.getException().getMessage() : ""), Toast.LENGTH_SHORT);
                        }
                        updateUIBasedOnData(); // Vẫn cập nhật UI để hiển thị empty view nếu cần
                    }
                });
    }

    private void updateUIBasedOnData() {
        if (notificationList.isEmpty()) {
            emptyView.setVisibility(View.VISIBLE);
            recyclerViewNotifications.setVisibility(View.GONE);
        } else {
            emptyView.setVisibility(View.GONE);
            recyclerViewNotifications.setVisibility(View.VISIBLE);
        }
    }


    @Override
    public void onNotificationClicked(Notification notification) {
        Log.d(TAG, "Notification clicked: " + notification.getMessage() + " | Type: " + notification.getType() + " | ProjectId: " + notification.getTargetProjectId());

        // Đánh dấu là đã đọc nếu chưa đọc (chỉ khi người dùng click trực tiếp vào item)
        if (!notification.isRead()) {
            // Không gọi onToggleReadStatus ở đây để tránh Toast "Đã đánh dấu đã đọc"
            // khi người dùng chỉ muốn mở item.
            // onToggleReadStatus nên dành cho action từ menu.
            // Trực tiếp gọi service để cập nhật trạng thái đọc.
            if (notification.getNotificationId() != null && !notification.getNotificationId().isEmpty()) {
                firestoreService.updateNotificationReadStatus(notification.getNotificationId(), true, new FirestoreService.NotificationUpdateListener() {
                    @Override
                    public void onSuccess() {
                        int position = -1;
                        for (int i = 0; i < notificationList.size(); i++) {
                            if (notificationList.get(i).getNotificationId().equals(notification.getNotificationId())) {
                                position = i;
                                break;
                            }
                        }
                        if (position != -1) {
                            notificationList.get(position).setRead(true);
                            if(notificationAdapter != null) {
                                notificationAdapter.updateNotificationItem(position, notificationList.get(position));
                            }
                        }
                        Log.d(TAG, "Notification marked as read upon click.");
                    }
                    @Override
                    public void onError(String errorMessage) {
                        Log.e(TAG, "Failed to mark notification as read upon click: " + errorMessage);
                    }
                });
            }
        }

        // Kiểm tra xem thông báo có targetProjectId không để điều hướng đến chi tiết dự án
        String targetProjectId = notification.getTargetProjectId();
        if (targetProjectId != null && !targetProjectId.isEmpty() && getContext() != null) {
            switch (notification.getType()) {
                case "PROJECT_INVITATION":
                case "NEW_COMMENT":
                case "NEW_REPLY":
                case "PROJECT_VOTE":
                case "INVITATION_ACCEPTED":
                case "INVITATION_DECLINED":
                    // Và các type khác mà bạn muốn điều hướng đến chi tiết dự án
                    Intent intent = new Intent(getContext(), ProjectDetailActivity.class);
                    intent.putExtra(ProjectDetailActivity.EXTRA_PROJECT_ID, targetProjectId);
                    startActivity(intent);
                    break;
                case "COMMENT_VOTE":
                    Intent commentIntent = new Intent(getContext(), ProjectDetailActivity.class);
                    commentIntent.putExtra(ProjectDetailActivity.EXTRA_PROJECT_ID, targetProjectId);
                    // if (notification.getTargetCommentId() != null) {
                    //     commentIntent.putExtra("COMMENT_ID_KEY", notification.getTargetCommentId());
                    // }
                    startActivity(commentIntent);
                    break;
                default:
                    AppToast.show(getContext(), "Hành động cho thông báo này chưa được xác định.", Toast.LENGTH_SHORT);
                    break;
            }
        } else if (notification.getActionUrl() != null && !notification.getActionUrl().isEmpty() && getContext() != null) {
            // Intent browserIntent = new Intent(Intent.ACTION_VIEW, Uri.parse(notification.getActionUrl()));
            // startActivity(browserIntent);
            AppToast.show(getContext(), "Action URL: " + notification.getActionUrl(), Toast.LENGTH_SHORT);
        } else if (getContext() != null) {
            AppToast.show(getContext(), "Thông báo này không có hành động cụ thể.", Toast.LENGTH_SHORT);
        }
    }


    @Override
    public void onAcceptInvite(Notification notification) {
        Log.d(TAG, "Accept invite for project: " + notification.getTargetProjectId());
        if (getContext() != null) {
            AppToast.show(getContext(), "Đã chấp nhận lời mời (chức năng đang phát triển)", Toast.LENGTH_SHORT);
        }
        // TODO: Implement accept invitation logic
    }

    @Override
    public void onDeclineInvite(Notification notification) {
        Log.d(TAG, "Decline invite for project: " + notification.getTargetProjectId());
        if (getContext() != null) {
            AppToast.show(getContext(), "Đã từ chối lời mời (chức năng đang phát triển)", Toast.LENGTH_SHORT);
        }
        // TODO: Implement decline invitation logic
    }

    @Override
    public void onToggleReadStatus(Notification notification) {
        if (notification.getNotificationId() == null || notification.getNotificationId().isEmpty()) {
            if (getContext() != null) AppToast.show(getContext(), String.valueOf(R.string.error_updating_notification), Toast.LENGTH_SHORT);
            Log.e(TAG, "Notification ID is null or empty for toggle read status.");
            return;
        }

        boolean newReadStatus = !notification.isRead();

        firestoreService.updateNotificationReadStatus(notification.getNotificationId(), newReadStatus, new FirestoreService.NotificationUpdateListener() {
            @Override
            public void onSuccess() {
                int position = -1;
                for (int i = 0; i < notificationList.size(); i++) {
                    if (notificationList.get(i).getNotificationId().equals(notification.getNotificationId())) {
                        position = i;
                        break;
                    }
                }

                if (position != -1) {
                    notificationList.get(position).setRead(newReadStatus);
                    if (notificationAdapter != null) {
                        notificationAdapter.updateNotificationItem(position, notificationList.get(position));
                    }
                } else {
                    Log.w(TAG, "Notification to toggle read status not found in current list: " + notification.getNotificationId());
                }
                if (getContext() != null) {
                    AppToast.show(getContext(),
                            newReadStatus ? String.valueOf(R.string.notification_marked_as_read) : String.valueOf(R.string.notification_marked_as_unread),
                            Toast.LENGTH_SHORT
                    );
                }
            }

            @Override
            public void onError(String errorMessage) {
                Log.e(TAG, "Error toggling read status: " + errorMessage);
                if (getContext() != null) AppToast.show(getContext(), String.valueOf(R.string.error_updating_notification), Toast.LENGTH_SHORT);
            }
        });
    }

    @Override
    public void onDeleteNotification(Notification notification) {
        if (notification.getNotificationId() == null || notification.getNotificationId().isEmpty()) {
            if (getContext() != null) AppToast.show(getContext(), String.valueOf(R.string.error_deleting_notification), Toast.LENGTH_SHORT);
            Log.e(TAG, "Notification ID is null or empty for delete.");
            return;
        }

        if (getContext() == null) {
            Log.e(TAG, "Context is null, cannot show AlertDialog for delete.");
            return;
        }

        new AlertDialog.Builder(requireContext()) // requireContext() is safer here
                .setTitle(R.string.delete_notification)
                .setMessage(R.string.confirm_delete_notification)
                .setPositiveButton(R.string.delete, (dialog, which) -> {
                    firestoreService.deleteNotification(notification.getNotificationId(), new FirestoreService.NotificationUpdateListener() {
                        @Override
                        public void onSuccess() {
                            int position = -1;
                            // Phải tìm lại vị trí vì list có thể đã thay đổi
                            for (int i = 0; i < notificationList.size(); i++) {
                                if (notificationList.get(i).getNotificationId().equals(notification.getNotificationId())) {
                                    position = i;
                                    break;
                                }
                            }
                            if (position != -1) {
                                if (notificationAdapter != null) {
                                    // Xóa khỏi list nguồn TRƯỚC khi gọi removeNotificationItem của adapter
                                    // nếu removeNotificationItem của adapter không tự xóa khỏi list nguồn.
                                    // Trong trường hợp này, removeNotificationItem trong adapter đã làm điều đó.
                                    notificationAdapter.removeNotificationItem(position);
                                }
                            } else {
                                Log.w(TAG, "Notification to delete not found in current list: " + notification.getNotificationId());
                                // Nếu không tìm thấy, có thể tải lại toàn bộ list để đồng bộ
                                // loadNotifications();
                            }
                            updateUIBasedOnData();
                            if (getContext() != null) AppToast.show(getContext(), String.valueOf(R.string.notification_deleted), Toast.LENGTH_SHORT);
                        }

                        @Override
                        public void onError(String errorMessage) {
                            Log.e(TAG, "Error deleting notification: " + errorMessage);
                            if (getContext() != null) AppToast.show(getContext(), String.valueOf(R.string.error_deleting_notification), Toast.LENGTH_SHORT);
                        }
                    });
                })
                .setNegativeButton(R.string.cancel, null)
                .show();
    }

    private void sendPushNotification(String recipientUserId, String title, String message, String type, String projectId, String commentId) {
        // TODO: Implement gửi push notification qua Firebase Cloud Functions
        // Hiện tại chỉ log để debug
        Log.d(TAG, "Should send push notification to user: " + recipientUserId);
        Log.d(TAG, "Title: " + title + ", Message: " + message);
        Log.d(TAG, "Type: " + type + ", ProjectId: " + projectId + ", CommentId: " + commentId);
    }
}