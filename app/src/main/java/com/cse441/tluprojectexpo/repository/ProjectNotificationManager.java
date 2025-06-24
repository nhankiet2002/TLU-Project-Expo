package com.cse441.tluprojectexpo.repository;

import android.util.Log;

import com.cse441.tluprojectexpo.model.Notification;
import com.cse441.tluprojectexpo.model.User;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.List;
import java.util.Map;

/**
 * Quản lý thông báo cho dự án (thêm/xóa thành viên)
 */
public class ProjectNotificationManager {
    private static final String TAG = "ProjectNotificationManager";

    private FirebaseAuth mAuth;
    private FirebaseFirestore db;
    private NotificationRepository notificationRepository;

    public ProjectNotificationManager() {
        this.mAuth = FirebaseAuth.getInstance();
        this.db = FirebaseFirestore.getInstance();
        this.notificationRepository = new NotificationRepository();
    }

    /**
     * Gửi thông báo khi có thay đổi thành viên trong dự án
     */
    public void sendMemberChangeNotifications(List<User> originalMembers, Map<String, String> originalUserRoles,
                                            List<User> currentMembers, Map<String, String> currentUserRoles,
                                            String projectId, String projectTitle) {
        
        if (mAuth.getCurrentUser() == null) {
            Log.w(TAG, "Current user is null, cannot send notifications");
            return;
        }
        
        String currentUserId = mAuth.getCurrentUser().getUid();
        String currentUserName = mAuth.getCurrentUser().getDisplayName() != null ? 
            mAuth.getCurrentUser().getDisplayName() : "Người dùng";
        String currentUserAvatar = mAuth.getCurrentUser().getPhotoUrl() != null ? 
            mAuth.getCurrentUser().getPhotoUrl().toString() : "";
        
        // Find newly added members
        for (User newMember : currentMembers) {
            boolean isNewMember = true;
            for (User originalMember : originalMembers) {
                if (newMember.getUserId().equals(originalMember.getUserId())) {
                    isNewMember = false;
                    break;
                }
            }
            
            if (isNewMember && !newMember.getUserId().equals(currentUserId)) {
                sendInvitationNotification(newMember, currentUserId, currentUserName, currentUserAvatar, 
                                         projectId, projectTitle, currentUserRoles.get(newMember.getUserId()));
            }
        }
        
        // Find removed members
        for (User removedMember : originalMembers) {
            boolean isRemoved = true;
            for (User currentMember : currentMembers) {
                if (removedMember.getUserId().equals(currentMember.getUserId())) {
                    isRemoved = false;
                    break;
                }
            }
            
            if (isRemoved && !removedMember.getUserId().equals(currentUserId)) {
                sendRemovalNotification(removedMember, currentUserName, projectId, projectTitle);
            }
        }
    }

    /**
     * Gửi thông báo mời tham gia dự án
     */
    private void sendInvitationNotification(User newMember, String actorUserId, String actorName, 
                                          String actorAvatar, String projectId, String projectTitle, String role) {
        
        if (role == null) role = "Thành viên";
        
        notificationRepository.createProjectInvitation(
            newMember.getUserId(), // recipientUserId
            actorUserId, // actorUserId
            actorName, // actorFullName
            actorAvatar, // actorAvatarUrl
            projectId, // targetProjectId
            projectTitle, // targetProjectTitle
            role, // invitationRole
            new NotificationRepository.NotificationActionListener() {
                @Override
                public void onSuccess() {
                    Log.d(TAG, "Sent invitation notification to: " + newMember.getFullName());
                }
                @Override
                public void onError(String errorMessage) {
                    Log.e(TAG, "Error sending invitation notification: " + errorMessage);
                }
            }
        );
    }

    /**
     * Gửi thông báo xóa khỏi dự án
     */
    private void sendRemovalNotification(User removedMember, String actorName, String projectId, String projectTitle) {
        Notification notification = new Notification();
        notification.setRecipientUserId(removedMember.getUserId());
        notification.setActorUserId(mAuth.getCurrentUser().getUid());
        notification.setActorFullName(actorName);
        notification.setActorAvatarUrl(mAuth.getCurrentUser().getPhotoUrl() != null ? 
            mAuth.getCurrentUser().getPhotoUrl().toString() : "");
        notification.setType("MEMBER_REMOVED");
        notification.setMessage(actorName + " đã xóa bạn khỏi dự án " + projectTitle);
        notification.setTargetProjectId(projectId);
        notification.setTargetProjectTitle(projectTitle);
        notification.setCreatedAt(com.google.firebase.Timestamp.now());
        notification.setRead(false);
        notification.setActionUrl("/project/" + projectId);
        
        db.collection("Notifications")
            .add(notification)
            .addOnSuccessListener(docRef -> {
                Log.d(TAG, "Sent removal notification to: " + removedMember.getFullName());
                // TODO: Send push notification via FCM
            })
            .addOnFailureListener(e -> {
                Log.e(TAG, "Error sending removal notification: " + e.getMessage());
            });
    }

    /**
     * Gửi thông báo khi dự án được duyệt
     */
    public void sendProjectApprovalNotification(String projectId, String projectTitle, String creatorUserId) {
        if (mAuth.getCurrentUser() == null) return;
        
        Notification notification = new Notification();
        notification.setRecipientUserId(creatorUserId);
        notification.setActorUserId(mAuth.getCurrentUser().getUid());
        notification.setActorFullName(mAuth.getCurrentUser().getDisplayName() != null ? 
            mAuth.getCurrentUser().getDisplayName() : "Admin");
        notification.setActorAvatarUrl(mAuth.getCurrentUser().getPhotoUrl() != null ? 
            mAuth.getCurrentUser().getPhotoUrl().toString() : "");
        notification.setType("PROJECT_APPROVED");
        notification.setMessage("Dự án " + projectTitle + " đã được duyệt");
        notification.setTargetProjectId(projectId);
        notification.setTargetProjectTitle(projectTitle);
        notification.setCreatedAt(com.google.firebase.Timestamp.now());
        notification.setRead(false);
        notification.setActionUrl("/project/" + projectId);
        
        db.collection("Notifications")
            .add(notification)
            .addOnSuccessListener(docRef -> {
                Log.d(TAG, "Sent project approval notification to: " + creatorUserId);
            })
            .addOnFailureListener(e -> {
                Log.e(TAG, "Error sending project approval notification: " + e.getMessage());
            });
    }

    /**
     * Gửi thông báo khi dự án bị từ chối
     */
    public void sendProjectRejectionNotification(String projectId, String projectTitle, String creatorUserId, String reason) {
        if (mAuth.getCurrentUser() == null) return;
        
        Notification notification = new Notification();
        notification.setRecipientUserId(creatorUserId);
        notification.setActorUserId(mAuth.getCurrentUser().getUid());
        notification.setActorFullName(mAuth.getCurrentUser().getDisplayName() != null ? 
            mAuth.getCurrentUser().getDisplayName() : "Admin");
        notification.setActorAvatarUrl(mAuth.getCurrentUser().getPhotoUrl() != null ? 
            mAuth.getCurrentUser().getPhotoUrl().toString() : "");
        notification.setType("PROJECT_REJECTED");
        notification.setMessage("Dự án " + projectTitle + " đã bị từ chối" + (reason != null ? ": " + reason : ""));
        notification.setTargetProjectId(projectId);
        notification.setTargetProjectTitle(projectTitle);
        notification.setCreatedAt(com.google.firebase.Timestamp.now());
        notification.setRead(false);
        notification.setActionUrl("/project/" + projectId);
        
        db.collection("Notifications")
            .add(notification)
            .addOnSuccessListener(docRef -> {
                Log.d(TAG, "Sent project rejection notification to: " + creatorUserId);
            })
            .addOnFailureListener(e -> {
                Log.e(TAG, "Error sending project rejection notification: " + e.getMessage());
            });
    }

    /**
     * Gửi thông báo khi có comment mới
     */
    public void sendNewCommentNotification(String projectId, String projectTitle, String commenterUserId, 
                                         String commenterName, String commenterAvatar, String commentContent) {
        
        // Get project creator and members to notify them
        db.collection("Projects").document(projectId).get()
            .addOnSuccessListener(documentSnapshot -> {
                if (documentSnapshot.exists()) {
                    String creatorUserId = documentSnapshot.getString("CreatorUserId");
                    
                    // Notify project creator
                    if (creatorUserId != null && !creatorUserId.equals(commenterUserId)) {
                        sendCommentNotification(creatorUserId, commenterName, projectTitle, commentContent, projectId);
                    }
                    
                    // Notify project members
                    db.collection("ProjectMembers")
                        .whereEqualTo("ProjectId", projectId)
                        .get()
                        .addOnSuccessListener(membersSnapshot -> {
                            for (com.google.firebase.firestore.QueryDocumentSnapshot memberDoc : membersSnapshot) {
                                String memberUserId = memberDoc.getString("UserId");
                                if (memberUserId != null && !memberUserId.equals(commenterUserId) && 
                                    !memberUserId.equals(creatorUserId)) {
                                    sendCommentNotification(memberUserId, commenterName, projectTitle, commentContent, projectId);
                                }
                            }
                        });
                }
            });
    }

    private void sendCommentNotification(String recipientUserId, String commenterName, String projectTitle, 
                                       String commentContent, String projectId) {
        Notification notification = new Notification();
        notification.setRecipientUserId(recipientUserId);
        notification.setActorUserId(mAuth.getCurrentUser().getUid());
        notification.setActorFullName(commenterName);
        notification.setActorAvatarUrl(mAuth.getCurrentUser().getPhotoUrl() != null ? 
            mAuth.getCurrentUser().getPhotoUrl().toString() : "");
        notification.setType("NEW_COMMENT");
        notification.setMessage(commenterName + " đã bình luận trong dự án " + projectTitle);
        notification.setTargetProjectId(projectId);
        notification.setTargetProjectTitle(projectTitle);
        notification.setCreatedAt(com.google.firebase.Timestamp.now());
        notification.setRead(false);
        notification.setActionUrl("/project/" + projectId);
        
        db.collection("Notifications")
            .add(notification)
            .addOnSuccessListener(docRef -> {
                Log.d(TAG, "Sent comment notification to: " + recipientUserId);
            })
            .addOnFailureListener(e -> {
                Log.e(TAG, "Error sending comment notification: " + e.getMessage());
            });
    }
} 