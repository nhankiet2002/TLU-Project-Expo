package com.cse441.tluprojectexpo.model;

import com.google.firebase.Timestamp;
import com.google.firebase.firestore.DocumentId;
import com.google.firebase.firestore.Exclude;
import com.google.firebase.firestore.PropertyName;

public class Notification {
    @DocumentId
    private String notificationId;

    @PropertyName("recipientUserId")
    private String recipientUserId;
    @PropertyName("actorUserId")
    private String actorUserId;
    @PropertyName("actorFullName")
    private String actorFullName;
    @PropertyName("actorAvatarUrl")
    private String actorAvatarUrl;
    @PropertyName("type")
    private String type;
    @PropertyName("message")
    private String message;
    @PropertyName("targetProjectId")
    private String targetProjectId;
    @PropertyName("targetProjectTitle")
    private String targetProjectTitle;
    @PropertyName("targetCommentId")
    private String targetCommentId;
    @PropertyName("targetCommentSnippet")
    private String targetCommentSnippet;
    @PropertyName("invitationRole")
    private String invitationRole;
    @PropertyName("invitationStatus")
    private String invitationStatus;
    @PropertyName("createdAt")
    private Timestamp createdAt;
    @PropertyName("isRead")
    private boolean isRead;
    @PropertyName("actionUrl")
    private String actionUrl;

    // Các trường bổ sung để hiển thị UI (không lưu vào Firestore)
    @Exclude
    private String senderName;
    @Exclude
    private String senderAvatarUrl;
    @Exclude
    private String projectTitle;

    public Notification() {
        this.isRead = false;
        this.invitationStatus = "pending";
    }

    // Getters and Setters với PropertyName annotations
    public String getNotificationId() {
        return notificationId;
    }

    public void setNotificationId(String notificationId) {
        this.notificationId = notificationId;
    }

    @PropertyName("recipientUserId")
    public String getRecipientUserId() {
        return recipientUserId;
    }

    @PropertyName("recipientUserId")
    public void setRecipientUserId(String recipientUserId) {
        this.recipientUserId = recipientUserId;
    }

    @PropertyName("actorUserId")
    public String getActorUserId() {
        return actorUserId;
    }

    @PropertyName("actorUserId")
    public void setActorUserId(String actorUserId) {
        this.actorUserId = actorUserId;
    }

    @PropertyName("actorFullName")
    public String getActorFullName() {
        return actorFullName;
    }

    @PropertyName("actorFullName")
    public void setActorFullName(String actorFullName) {
        this.actorFullName = actorFullName;
    }

    @PropertyName("actorAvatarUrl")
    public String getActorAvatarUrl() {
        return actorAvatarUrl;
    }

    @PropertyName("actorAvatarUrl")
    public void setActorAvatarUrl(String actorAvatarUrl) {
        this.actorAvatarUrl = actorAvatarUrl;
    }

    @PropertyName("type")
    public String getType() {
        return type;
    }

    @PropertyName("type")
    public void setType(String type) {
        this.type = type;
    }

    @PropertyName("message")
    public String getMessage() {
        return message;
    }

    @PropertyName("message")
    public void setMessage(String message) {
        this.message = message;
    }

    @PropertyName("targetProjectId")
    public String getTargetProjectId() {
        return targetProjectId;
    }

    @PropertyName("targetProjectId")
    public void setTargetProjectId(String targetProjectId) {
        this.targetProjectId = targetProjectId;
    }

    @PropertyName("targetProjectTitle")
    public String getTargetProjectTitle() {
        return targetProjectTitle;
    }

    @PropertyName("targetProjectTitle")
    public void setTargetProjectTitle(String targetProjectTitle) {
        this.targetProjectTitle = targetProjectTitle;
    }

    @PropertyName("targetCommentId")
    public String getTargetCommentId() {
        return targetCommentId;
    }

    @PropertyName("targetCommentId")
    public void setTargetCommentId(String targetCommentId) {
        this.targetCommentId = targetCommentId;
    }

    @PropertyName("targetCommentSnippet")
    public String getTargetCommentSnippet() {
        return targetCommentSnippet;
    }

    @PropertyName("targetCommentSnippet")
    public void setTargetCommentSnippet(String targetCommentSnippet) {
        this.targetCommentSnippet = targetCommentSnippet;
    }

    @PropertyName("invitationRole")
    public String getInvitationRole() {
        return invitationRole;
    }

    @PropertyName("invitationRole")
    public void setInvitationRole(String invitationRole) {
        this.invitationRole = invitationRole;
    }

    @PropertyName("invitationStatus")
    public String getInvitationStatus() {
        return invitationStatus;
    }

    @PropertyName("invitationStatus")
    public void setInvitationStatus(String invitationStatus) {
        this.invitationStatus = invitationStatus;
    }

    @PropertyName("createdAt")
    public Timestamp getCreatedAt() {
        return createdAt;
    }

    @PropertyName("createdAt")
    public void setCreatedAt(Timestamp createdAt) {
        this.createdAt = createdAt;
    }

    @PropertyName("isRead")
    public boolean isRead() {
        return isRead;
    }

    @PropertyName("isRead")
    public void setRead(boolean read) {
        isRead = read;
    }

    @PropertyName("actionUrl")
    public String getActionUrl() {
        return actionUrl;
    }

    @PropertyName("actionUrl")
    public void setActionUrl(String actionUrl) {
        this.actionUrl = actionUrl;
    }

    @Exclude
    public String getSenderName() {
        return senderName;
    }

    @Exclude
    public void setSenderName(String senderName) {
        this.senderName = senderName;
    }

    @Exclude
    public String getSenderAvatarUrl() {
        return senderAvatarUrl;
    }

    @Exclude
    public void setSenderAvatarUrl(String senderAvatarUrl) {
        this.senderAvatarUrl = senderAvatarUrl;
    }

    @Exclude
    public String getProjectTitle() {
        return projectTitle;
    }

    @Exclude
    public void setProjectTitle(String projectTitle) {
        this.projectTitle = projectTitle;
    }
} 