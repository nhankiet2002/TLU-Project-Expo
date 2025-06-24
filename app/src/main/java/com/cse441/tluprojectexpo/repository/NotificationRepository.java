package com.cse441.tluprojectexpo.repository;

import android.util.Log;

import com.cse441.tluprojectexpo.model.Notification;
import com.cse441.tluprojectexpo.model.Project;
import com.cse441.tluprojectexpo.model.User;
import com.cse441.tluprojectexpo.utils.Constants;
import com.google.android.gms.tasks.Task;
import com.google.android.gms.tasks.Tasks;
import com.google.firebase.Timestamp;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.ArrayList;
import java.util.List;

public class NotificationRepository {
    private static final String TAG = "NotificationRepository";
    private FirebaseFirestore db;

    public interface NotificationsLoadListener {
        void onNotificationsLoaded(List<Notification> notifications);
        void onNotificationsEmpty();
        void onError(String errorMessage);
    }

    public interface NotificationActionListener {
        void onSuccess();
        void onError(String errorMessage);
    }

    public NotificationRepository() {
        this.db = FirebaseFirestore.getInstance();
    }

    /**
     * Lưu FCM token của user
     */
    public void saveFCMToken(String userId, String fcmToken, NotificationActionListener listener) {
        if (userId == null || userId.isEmpty() || fcmToken == null || fcmToken.isEmpty()) {
            if (listener != null) listener.onError("Thông tin không hợp lệ");
            return;
        }

        db.collection("UserTokens")
            .document(userId)
            .set(new java.util.HashMap<String, Object>() {{
                put("UserId", userId);
                put("fcmToken", fcmToken);
                put("lastUpdated", Timestamp.now());
            }})
            .addOnSuccessListener(aVoid -> {
                if (listener != null) listener.onSuccess();
            })
            .addOnFailureListener(e -> {
                if (listener != null) listener.onError("Lỗi khi lưu token: " + e.getMessage());
            });
    }

    /**
     * Lấy FCM token của user
     */
    public void getFCMToken(String userId, NotificationActionListener listener) {
        if (userId == null || userId.isEmpty()) {
            if (listener != null) listener.onError("User ID không hợp lệ");
            return;
        }

        db.collection("UserTokens")
            .document(userId)
            .get()
            .addOnSuccessListener(documentSnapshot -> {
                if (documentSnapshot.exists()) {
                    String fcmToken = documentSnapshot.getString("fcmToken");
                    if (listener != null) listener.onSuccess();
                } else {
                    if (listener != null) listener.onError("Không tìm thấy token");
                }
            })
            .addOnFailureListener(e -> {
                if (listener != null) listener.onError("Lỗi khi lấy token: " + e.getMessage());
            });
    }

    /**
     * Tạo thông báo mời tham gia dự án
     */
    public void createProjectInvitation(
        String recipientUserId,
        String actorUserId,
        String actorFullName,
        String actorAvatarUrl,
        String targetProjectId,
        String targetProjectTitle,
        String invitationRole,
        NotificationActionListener listener
    ) {
        Notification notification = new Notification();
        notification.setRecipientUserId(recipientUserId);
        notification.setActorUserId(actorUserId);
        notification.setActorFullName(actorFullName);
        notification.setActorAvatarUrl(actorAvatarUrl);
        notification.setType("PROJECT_INVITATION");
        notification.setMessage(actorFullName + " đã mời bạn tham gia dự án " + targetProjectTitle + " với vai trò " + invitationRole);
        notification.setTargetProjectId(targetProjectId);
        notification.setTargetProjectTitle(targetProjectTitle);
        notification.setInvitationRole(invitationRole);
        notification.setInvitationStatus("pending");
        notification.setCreatedAt(com.google.firebase.Timestamp.now());
        notification.setRead(false);
        notification.setActionUrl("/project/" + targetProjectId);

        db.collection("Notifications")
          .add(notification)
          .addOnSuccessListener(docRef -> { 
              if (listener != null) listener.onSuccess(); 
              // TODO: Gửi push notification qua FCM
              sendPushNotification(recipientUserId, "Lời mời tham gia dự án", notification.getMessage(), "PROJECT_INVITATION", targetProjectId, null);
          })
          .addOnFailureListener(e -> { if (listener != null) listener.onError(e.getMessage()); });
    }

    /**
     * Tạo thông báo khi có người bình luận vào dự án
     */
    public void createCommentNotification(FirebaseUser currentUser, String projectId, 
                                        String commentId, String commentText,
                                        String projectOwnerId, NotificationActionListener listener) {
        if (currentUser == null || projectId == null || commentId == null || projectOwnerId == null) {
            if (listener != null) listener.onError("Thông tin không hợp lệ");
            return;
        }

        // Chỉ tạo thông báo nếu người comment không phải là chủ dự án
        if (!currentUser.getUid().equals(projectOwnerId)) {
            db.collection(Constants.COLLECTION_USERS).document(currentUser.getUid()).get()
                .addOnSuccessListener(userDoc -> {
                    final String[] userName = {"Người dùng"};
                    if (userDoc.exists()) {
                        User user = userDoc.toObject(User.class);
                        if (user != null && user.getFullName() != null) {
                            userName[0] = user.getFullName();
                        }
                    }

                    // Tạo notification object
                    Notification notification = new Notification();
                    notification.setRecipientUserId(projectOwnerId);
                    notification.setActorUserId(currentUser.getUid());
                    notification.setActorFullName(userName[0]);
                    notification.setActorAvatarUrl(currentUser.getPhotoUrl() != null ? currentUser.getPhotoUrl().toString() : "");
                    notification.setType("NEW_COMMENT");
                    notification.setMessage(userName[0] + " đã bình luận vào dự án của bạn: " + commentText);
                    notification.setTargetProjectId(projectId);
                    notification.setTargetCommentId(commentId);
                    notification.setTargetCommentSnippet(commentText.length() > 100 ? commentText.substring(0, 100) + "..." : commentText);
                    notification.setCreatedAt(Timestamp.now());
                    notification.setRead(false);
                    notification.setActionUrl("/project/" + projectId + "/comment/" + commentId);

                    // Lưu vào Firestore
                    db.collection(Constants.COLLECTION_NOTIFICATIONS)
                        .add(notification)
                        .addOnSuccessListener(docRef -> {
                            if (listener != null) listener.onSuccess();
                            // TODO: Gửi push notification qua FCM
                            sendPushNotification(projectOwnerId, "Bình luận mới", notification.getMessage(), "NEW_COMMENT", projectId, commentId);
                        })
                        .addOnFailureListener(e -> {
                            if (listener != null) listener.onError("Lỗi khi tạo thông báo: " + e.getMessage());
                        });
                })
                .addOnFailureListener(e -> {
                    if (listener != null) listener.onError("Lỗi khi lấy thông tin user: " + e.getMessage());
                });
        } else {
            if (listener != null) listener.onSuccess(); // Không cần thông báo cho chính mình
        }
    }

    /**
     * Gửi push notification qua FCM (placeholder - cần implement với Cloud Functions)
     */
    private void sendPushNotification(String recipientUserId, String title, String message, String type, String projectId, String commentId) {
        // TODO: Implement gửi push notification qua Firebase Cloud Functions
        // Hiện tại chỉ log để debug
        Log.d(TAG, "Should send push notification to user: " + recipientUserId);
        Log.d(TAG, "Title: " + title + ", Message: " + message);
        Log.d(TAG, "Type: " + type + ", ProjectId: " + projectId + ", CommentId: " + commentId);
    }

    /**
                    db.collection(Constants.COLLECTION_PROJECTS).document(projectId).get()
                        .addOnSuccessListener(projectDoc -> {
                            if (projectDoc.exists()) {
                                Project project = projectDoc.toObject(Project.class);
                                if (project != null) {
                                    Notification notification = new Notification();
                                    notification.setType("COMMENT");
                                    notification.setActorUserId(currentUser.getUid());
                                    notification.setRecipientUserId(projectOwnerId);
                                    notification.setTargetProjectId(projectId);
                                    notification.setTargetCommentId(commentId);
                                    notification.setCreatedAt(Timestamp.now());
                                    notification.setMessage(userName[0] + " đã bình luận về dự án " + project.getTitle());

                                    db.collection(Constants.COLLECTION_NOTIFICATIONS)
                                        .add(notification)
                                        .addOnSuccessListener(docRef -> {
                                            if (listener != null) listener.onSuccess();
                                        })
                                        .addOnFailureListener(e -> {
                                            if (listener != null) listener.onError("Lỗi khi tạo thông báo: " + e.getMessage());
                                        });
                                }
                            }
                        });
                });
        }
    }

    /**
     * Tạo thông báo khi có người trả lời bình luận
     */
    public void createReplyCommentNotification(FirebaseUser currentUser, String projectId,
                                             String parentCommentId, String replyCommentId,
                                             String parentCommentUserId, NotificationActionListener listener) {
        if (currentUser == null || projectId == null || parentCommentId == null || parentCommentUserId == null) {
            if (listener != null) listener.onError("Thông tin không hợp lệ");
            return;
        }

        // Chỉ tạo thông báo nếu người trả lời không phải là người comment gốc
        if (!currentUser.getUid().equals(parentCommentUserId)) {
            db.collection(Constants.COLLECTION_PROJECTS).document(projectId).get()
                .addOnSuccessListener(projectDoc -> {
                    if (projectDoc.exists()) {
                        Project project = projectDoc.toObject(Project.class);
                        if (project != null) {
                            Notification notification = new Notification();
                            notification.setType("REPLY_COMMENT");
                            notification.setActorUserId(currentUser.getUid());
                            notification.setRecipientUserId(parentCommentUserId);
                            notification.setTargetProjectId(projectId);
                            notification.setTargetCommentId(replyCommentId);
                            notification.setCreatedAt(Timestamp.now());
                            notification.setMessage(currentUser.getDisplayName() + " đã trả lời bình luận của bạn trong dự án " + project.getTitle());

                            db.collection(Constants.COLLECTION_NOTIFICATIONS)
                                .add(notification)
                                .addOnSuccessListener(docRef -> {
                                    if (listener != null) listener.onSuccess();
                                })
                                .addOnFailureListener(e -> {
                                    if (listener != null) listener.onError("Lỗi khi tạo thông báo: " + e.getMessage());
                                });
                        }
                    }
                });
        }
    }

    /**
     * Lấy danh sách thông báo của người dùng hiện tại
     */
    public void fetchUserNotifications(String userId, NotificationsLoadListener listener) {
        if (userId == null || userId.isEmpty()) {
            if (listener != null) listener.onError("User ID không hợp lệ");
            return;
        }

        db.collection(Constants.COLLECTION_NOTIFICATIONS)
            .whereEqualTo("recipientUserId", userId)
            .orderBy("createdAt", Query.Direction.DESCENDING)
            .get()
            .addOnSuccessListener(queryDocumentSnapshots -> {
                if (queryDocumentSnapshots.isEmpty()) {
                    listener.onNotificationsEmpty();
                    return;
                }

                List<Notification> notifications = new ArrayList<>();
                List<Task<Void>> userLoadTasks = new ArrayList<>();

                for (QueryDocumentSnapshot doc : queryDocumentSnapshots) {
                    Notification notification = doc.toObject(Notification.class);
                    notifications.add(notification);

                    // Load thông tin người gửi nếu actorUserId không null
                    if (notification.getActorUserId() != null) {
                        Task<Void> userTask = db.collection(Constants.COLLECTION_USERS)
                            .document(notification.getActorUserId())
                            .get()
                            .continueWith(task -> {
                                if (task.isSuccessful() && task.getResult() != null) {
                                    User sender = task.getResult().toObject(User.class);
                                    if (sender != null) {
                                        notification.setActorFullName(sender.getFullName());
                                        notification.setActorAvatarUrl(sender.getAvatarUrl());
                                    }
                                }
                                return null;
                            });
                        userLoadTasks.add(userTask);
                    }

                    // Load thông tin dự án nếu targetProjectId không null
                    String projectId = notification.getTargetProjectId();
                    if (projectId != null && !projectId.isEmpty()) {
                        Task<Void> projectTask = db.collection(Constants.COLLECTION_PROJECTS)
                            .document(projectId)
                            .get()
                            .continueWith(task -> {
                                if (task.isSuccessful() && task.getResult() != null) {
                                    Project project = task.getResult().toObject(Project.class);
                                    if (project != null) {
                                        notification.setTargetProjectTitle(project.getTitle());
                                    }
                                }
                                return null;
                            });
                        userLoadTasks.add(projectTask);
                    }
                }

                // Đợi tất cả các task load thông tin hoàn thành
                Tasks.whenAllComplete(userLoadTasks)
                    .addOnCompleteListener(task -> {
                        listener.onNotificationsLoaded(notifications);
                    });
            })
            .addOnFailureListener(e -> {
                listener.onError("Lỗi khi tải thông báo: " + e.getMessage());
            });
    }

    /**
     * Cập nhật trạng thái đã đọc của thông báo
     */
    public void markNotificationAsRead(String notificationId, NotificationActionListener listener) {
        if (notificationId == null || notificationId.isEmpty()) {
            if (listener != null) listener.onError("Notification ID không hợp lệ");
            return;
        }

        db.collection(Constants.COLLECTION_NOTIFICATIONS)
            .document(notificationId)
            .update("IsRead", true)
            .addOnSuccessListener(aVoid -> {
                if (listener != null) listener.onSuccess();
            })
            .addOnFailureListener(e -> {
                if (listener != null) listener.onError("Lỗi khi cập nhật trạng thái: " + e.getMessage());
            });
    }

    /**
     * Cập nhật trạng thái của lời mời tham gia dự án
     */
    public void updateProjectInvitationStatus(String notificationId, String status, 
                                            NotificationActionListener listener) {
        if (notificationId == null || notificationId.isEmpty() || status == null) {
            if (listener != null) listener.onError("Thông tin không hợp lệ");
            return;
        }

        db.collection(Constants.COLLECTION_NOTIFICATIONS)
            .document(notificationId)
            .update("Status", status)
            .addOnSuccessListener(aVoid -> {
                if (listener != null) listener.onSuccess();
            })
            .addOnFailureListener(e -> {
                if (listener != null) listener.onError("Lỗi khi cập nhật trạng thái: " + e.getMessage());
            });
    }
} 