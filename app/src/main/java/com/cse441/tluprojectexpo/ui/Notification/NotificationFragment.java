package com.cse441.tluprojectexpo.ui.Notification;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import com.cse441.tluprojectexpo.R;
import com.cse441.tluprojectexpo.model.Notification;
import com.cse441.tluprojectexpo.repository.NotificationRepository;
import com.cse441.tluprojectexpo.ui.Notification.adapter.NotificationAdapter;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;
import com.cse441.tluprojectexpo.utils.Constants;
import android.app.AlertDialog;
import android.util.Log;

import java.util.ArrayList;
import java.util.List;

/**
 * A simple {@link Fragment} subclass.
 * Use the {@link NotificationFragment#newInstance} factory method to
 * create an instance of this fragment.
 */
public class NotificationFragment extends Fragment implements NotificationAdapter.NotificationActionListener {

    // TODO: Rename parameter arguments, choose names that match
    // the fragment initialization parameters, e.g. ARG_ITEM_NUMBER
    private static final String ARG_PARAM1 = "param1";
    private static final String ARG_PARAM2 = "param2";

    // TODO: Rename and change types of parameters
    private String mParam1;
    private String mParam2;

    private RecyclerView recyclerView;
    private NotificationAdapter adapter;
    private NotificationRepository notificationRepository;
    private SwipeRefreshLayout swipeRefreshLayout;
    private View emptyView;
    private FirebaseUser currentUser;

    public NotificationFragment() {
        // Required empty public constructor
    }

    /**
     * Use this factory method to create a new instance of
     * this fragment using the provided parameters.
     *
     * @param param1 Parameter 1.
     * @param param2 Parameter 2.
     * @return A new instance of fragment HomeFragment.
     */
    // TODO: Rename and change types and number of parameters
    public static NotificationFragment newInstance(String param1, String param2) {
        NotificationFragment fragment = new NotificationFragment();
        Bundle args = new Bundle();
        args.putString(ARG_PARAM1, param1);
        args.putString(ARG_PARAM2, param2);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            mParam1 = getArguments().getString(ARG_PARAM1);
            mParam2 = getArguments().getString(ARG_PARAM2);
        }
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View root = inflater.inflate(R.layout.fragment_notification, container, false);

        // Khởi tạo các view
        recyclerView = root.findViewById(R.id.recyclerViewNotifications);
        swipeRefreshLayout = root.findViewById(R.id.swipeRefreshLayout);
        emptyView = root.findViewById(R.id.emptyView);

        // Khởi tạo RecyclerView
        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        adapter = new NotificationAdapter(new ArrayList<>(), this);
        recyclerView.setAdapter(adapter);

        // Khởi tạo repository và lấy user hiện tại
        notificationRepository = new NotificationRepository();
        currentUser = FirebaseAuth.getInstance().getCurrentUser();

        // Setup swipe refresh
        swipeRefreshLayout.setOnRefreshListener(this::loadNotifications);

        // Load thông báo lần đầu
        loadNotifications();

        return root;
    }

    private void loadNotifications() {
        if (currentUser == null) return;

        swipeRefreshLayout.setRefreshing(true);
        notificationRepository.fetchUserNotifications(currentUser.getUid(), 
            new NotificationRepository.NotificationsLoadListener() {
                @Override
                public void onNotificationsLoaded(List<Notification> notifications) {
                    swipeRefreshLayout.setRefreshing(false);
                    adapter.updateNotifications(notifications);
                    updateEmptyView(notifications.isEmpty());
                }

                @Override
                public void onNotificationsEmpty() {
                    swipeRefreshLayout.setRefreshing(false);
                    adapter.updateNotifications(new ArrayList<>());
                    updateEmptyView(true);
                }

                @Override
                public void onError(String errorMessage) {
                    swipeRefreshLayout.setRefreshing(false);
                    Toast.makeText(getContext(), errorMessage, Toast.LENGTH_SHORT).show();
                }
            });
    }

    private void updateEmptyView(boolean isEmpty) {
        if (isEmpty) {
            recyclerView.setVisibility(View.GONE);
            emptyView.setVisibility(View.VISIBLE);
        } else {
            recyclerView.setVisibility(View.VISIBLE);
            emptyView.setVisibility(View.GONE);
        }
    }

    @Override
    public void onNotificationClicked(Notification notification) {
        notificationRepository.markNotificationAsRead(notification.getNotificationId(),
            new NotificationRepository.NotificationActionListener() {
                @Override
                public void onSuccess() {
                    notification.setRead(true); // cập nhật trạng thái local
                    adapter.notifyDataSetChanged(); // cập nhật lại UI
                    // Xử lý click theo loại thông báo
                    switch (notification.getType()) {
                        case "PROJECT_INVITATION":
                            navigateToProjectDetail(notification.getTargetProjectId(), null);
                            break;
                        case "NEW_COMMENT":
                        case "NEW_REPLY":
                            navigateToProjectDetail(notification.getTargetProjectId(), notification.getTargetCommentId());
                            break;
                        default:
                            // Có thể xử lý các loại khác nếu cần
                            break;
                    }
                }
                @Override
                public void onError(String errorMessage) {
                    Toast.makeText(getContext(), errorMessage, Toast.LENGTH_SHORT).show();
                }
            });
    }

    private void handleProjectInvite(Notification notification) {
        // TODO: Hiển thị dialog xác nhận tham gia dự án
        // Sau khi người dùng chọn, cập nhật trạng thái thông báo
        showProjectInviteDialog(notification);
    }

    private void showProjectInviteDialog(Notification notification) {
        new AlertDialog.Builder(getContext())
            .setTitle("Lời mời tham gia dự án")
            .setMessage(notification.getMessage())
            .setPositiveButton("Đồng ý", (dialog, which) -> {
                // Thêm user vào ProjectMembers
                FirebaseFirestore db = FirebaseFirestore.getInstance();
                String projectId = notification.getTargetProjectId();
                String userId = notification.getRecipientUserId();
                String role = Constants.DEFAULT_MEMBER_ROLE;
                db.collection(Constants.COLLECTION_PROJECT_MEMBERS)
                    .add(new java.util.HashMap<String, Object>() {{
                        put(Constants.FIELD_PROJECT_ID, projectId);
                        put(Constants.FIELD_USER_ID, userId);
                        put(Constants.FIELD_ROLE_IN_PROJECT, role);
                    }})
                    .addOnSuccessListener(docRef -> {
                        notificationRepository.updateProjectInvitationStatus(notification.getNotificationId(), "ACCEPTED",
                            new NotificationRepository.NotificationActionListener() {
                                @Override
                                public void onSuccess() {
                                    Toast.makeText(getContext(), "Đã chấp nhận lời mời", Toast.LENGTH_SHORT).show();
                                    loadNotifications();
                                }
                                @Override
                                public void onError(String errorMessage) {
                                    Toast.makeText(getContext(), errorMessage, Toast.LENGTH_SHORT).show();
                                }
                            });
                    })
                    .addOnFailureListener(e -> {
                        Toast.makeText(getContext(), "Lỗi khi thêm vào dự án: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    });
            })
            .setNegativeButton("Từ chối", (dialog, which) -> {
                notificationRepository.updateProjectInvitationStatus(notification.getNotificationId(), "DECLINED",
                    new NotificationRepository.NotificationActionListener() {
                        @Override
                        public void onSuccess() {
                            Toast.makeText(getContext(), "Đã từ chối lời mời", Toast.LENGTH_SHORT).show();
                            loadNotifications();
                        }
                        @Override
                        public void onError(String errorMessage) {
                            Toast.makeText(getContext(), errorMessage, Toast.LENGTH_SHORT).show();
                        }
                    });
            })
            .setCancelable(true)
            .show();
    }

    private void navigateToProjectDetail(String projectId, String commentId) {
        Intent intent = new Intent(getContext(), com.cse441.tluprojectexpo.ui.detailproject.ProjectDetailActivity.class);
        intent.putExtra(com.cse441.tluprojectexpo.ui.detailproject.ProjectDetailActivity.EXTRA_PROJECT_ID, projectId);
        if (commentId != null) {
            intent.putExtra("commentId", commentId);
        }
        startActivity(intent);
    }

    @Override
    public void onAcceptInvite(Notification notification) {
        // Thêm user vào ProjectMembers
        FirebaseFirestore db = FirebaseFirestore.getInstance();
        String projectId = notification.getTargetProjectId();
        String userId = notification.getRecipientUserId();
        String role = notification.getInvitationRole() != null ? notification.getInvitationRole() : "Thành viên";
        db.collection("ProjectMembers")
            .add(new java.util.HashMap<String, Object>() {{
                put("ProjectId", projectId);
                put("UserId", userId);
                put("RoleInProject", role);
            }})
            .addOnSuccessListener(docRef -> {
                // Cập nhật trạng thái và nội dung notification
                String newContent = "Bạn đã chấp nhận lời mời tham gia vào dự án với vai trò " + role;
                db.collection("Notifications")
                    .document(notification.getNotificationId())
                    .update("invitationStatus", "accepted", "message", newContent)
                    .addOnSuccessListener(aVoid -> {
                        Toast.makeText(getContext(), "Đã chấp nhận lời mời", Toast.LENGTH_SHORT).show();
                        loadNotifications();
                    })
                    .addOnFailureListener(e -> {
                        Toast.makeText(getContext(), "Lỗi khi cập nhật nội dung thông báo: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    });
            })
            .addOnFailureListener(e -> {
                Toast.makeText(getContext(), "Lỗi khi thêm vào dự án: " + e.getMessage(), Toast.LENGTH_SHORT).show();
            });
    }

    @Override
    public void onDeclineInvite(Notification notification) {
        FirebaseFirestore db = FirebaseFirestore.getInstance();
        String projectId = notification.getTargetProjectId();
        String userId = notification.getRecipientUserId();
        String role = notification.getInvitationRole() != null ? notification.getInvitationRole() : "Thành viên";
        // Cập nhật trạng thái và nội dung notification trước
        String newContent = "Bạn đã từ chối lời mời tham gia vào dự án với vai trò " + role;
        db.collection("Notifications")
            .document(notification.getNotificationId())
            .update("invitationStatus", "declined", "message", newContent)
            .addOnSuccessListener(aVoid -> {
                // Xóa user khỏi ProjectMembers nếu đã có
                db.collection("ProjectMembers")
                    .whereEqualTo("ProjectId", projectId)
                    .whereEqualTo("UserId", userId)
                    .get()
                    .addOnSuccessListener(querySnapshot -> {
                        for (com.google.firebase.firestore.DocumentSnapshot doc : querySnapshot.getDocuments()) {
                            doc.getReference().delete();
                        }
                        Toast.makeText(getContext(), "Đã từ chối lời mời", Toast.LENGTH_SHORT).show();
                        loadNotifications();
                    })
                    .addOnFailureListener(e -> {
                        Toast.makeText(getContext(), "Lỗi khi xóa thành viên khỏi dự án: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    });
            })
            .addOnFailureListener(e -> {
                Toast.makeText(getContext(), "Lỗi khi cập nhật nội dung thông báo: " + e.getMessage(), Toast.LENGTH_SHORT).show();
            });
    }
}