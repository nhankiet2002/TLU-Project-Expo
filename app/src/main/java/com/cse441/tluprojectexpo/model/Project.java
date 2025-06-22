package com.cse441.tluprojectexpo.model;

import com.google.firebase.Timestamp;
import com.google.firebase.firestore.PropertyName;
import com.google.firebase.firestore.Exclude;

import java.io.Serializable;
import java.util.List;
import java.util.ArrayList;

public class Project implements Serializable {

    // --- CÁC TRƯỜNG LƯU TRỮ TRỰC TIẾP TRONG DOCUMENT 'Projects' TRÊN FIRESTORE ---
    @Exclude
    private String projectId;

    // Các PropertyName ở đây cần khớp với tên trường trong Firestore "Projects" collection
    @PropertyName("Title")
    private String Title;

    @PropertyName("Description")
    private String Description;

    @PropertyName("ThumbnailUrl")
    private String ThumbnailUrl;

    @PropertyName("MediaGalleryUrls")
    private List<MediaItem> MediaGalleryUrls;

    @PropertyName("ProjectUrl")
    private String ProjectUrl;

    @PropertyName("DemoUrl")
    private String DemoUrl;

    @PropertyName("Status")
    private String Status;

    @PropertyName("CourseId") // Có thể là null
    private String CourseId;

    @PropertyName("CreatorUserId")
    private String CreatorUserId;

    @PropertyName("CreatedAt") // Nên là Timestamp trong Firestore
    private Timestamp CreatedAt;

    @PropertyName("UpdatedAt") // Nên là Timestamp trong Firestore
    private Timestamp UpdatedAt;

    @PropertyName("IsApproved")
    private boolean IsApproved;
    @PropertyName("IsFeatured")
    private boolean IsFeatured =false;

    @PropertyName("VoteCount")
    private int VoteCount;

    // --- CÁC TRƯỜNG THÔNG TIN BỔ SUNG (KHÔNG LƯU TRỰC TIẾP) ---
    @Exclude
    private String creatorFullName;
    @Exclude
    private List<String> technologyNames;
    @Exclude
    private List<String> categoryNames;
    @Exclude
    private List<UserShortInfo> projectMembersInfo;

    public Project() {
        this.MediaGalleryUrls = new ArrayList<>();
        this.technologyNames = new ArrayList<>();
        this.categoryNames = new ArrayList<>();
        this.projectMembersInfo = new ArrayList<>();
    }

    // --- GETTERS AND SETTERS ---
    // (Giữ nguyên các getters và setters bạn đã có)
    public String getProjectId() { return projectId; }
    public void setProjectId(String projectId) { this.projectId = projectId; }

    public String getTitle() { return Title; }
    public void setTitle(String title) { this.Title = title; }

    public String getDescription() { return Description; }
    public void setDescription(String description) { this.Description = description; }

    public String getThumbnailUrl() { return ThumbnailUrl; }
    public void setThumbnailUrl(String thumbnailUrl) { this.ThumbnailUrl = thumbnailUrl; }

    public List<MediaItem> getMediaGalleryUrls() { return MediaGalleryUrls; }
    public void setMediaGalleryUrls(List<MediaItem> mediaGalleryUrls) { this.MediaGalleryUrls = mediaGalleryUrls; }

    public String getProjectUrl() { return ProjectUrl; }
    public void setProjectUrl(String projectUrl) { this.ProjectUrl = projectUrl; }

    public String getDemoUrl() { return DemoUrl; }
    public void setDemoUrl(String demoUrl) { this.DemoUrl = demoUrl; }

    public String getStatus() { return Status; }
    public void setStatus(String status) { this.Status = status; }

    public String getCourseId() { return CourseId; }
    public void setCourseId(String courseId) { this.CourseId = courseId; }

    public String getCreatorUserId() { return CreatorUserId; }
    public void setCreatorUserId(String creatorUserId) { this.CreatorUserId = creatorUserId; }

    public Timestamp getCreatedAt() { return CreatedAt; }
    public void setCreatedAt(Timestamp createdAt) { this.CreatedAt = createdAt; }

    public Timestamp getUpdatedAt() { return UpdatedAt; }
    public void setUpdatedAt(Timestamp updatedAt) { this.UpdatedAt = updatedAt; }

    public boolean isApproved() { return IsApproved; } // Hoặc getIsApproved()
    public void setApproved(boolean approved) { IsApproved = approved; } // Hoặc setIsApproved()
    @PropertyName("IsFeatured")
    public boolean isFeatured() { return IsFeatured; }
    @PropertyName("IsFeatured")
    public void setFeatured(boolean featured) { IsFeatured = featured; }
    public int getVoteCount() { return VoteCount; }
    public void setVoteCount(int voteCount) { this.VoteCount = voteCount; }

    public String getCreatorFullName() { return creatorFullName; }
    public void setCreatorFullName(String creatorFullName) { this.creatorFullName = creatorFullName; }

    public List<String> getTechnologyNames() { return technologyNames; }
    public void setTechnologyNames(List<String> technologyNames) { this.technologyNames = technologyNames; }

    public List<String> getCategoryNames() { return categoryNames; }
    public void setCategoryNames(List<String> categoryNames) { this.categoryNames = categoryNames; }

    public List<UserShortInfo> getProjectMembersInfo() { return projectMembersInfo; }
    public void setProjectMembersInfo(List<UserShortInfo> projectMembersInfo) { this.projectMembersInfo = projectMembersInfo; }


    // --- INNER STATIC CLASS CHO MediaItem ---
    public static class MediaItem implements Serializable { // Thêm Serializable nếu cần
        @PropertyName("url") // Khớp với JSON
        private String url;
        @PropertyName("type") // Khớp với JSON
        private String type;

        public MediaItem() {}
        public MediaItem(String url, String type) {
            this.url = url;
            this.type = type;
        }
        public String getUrl() { return url; }
        public void setUrl(String url) { this.url = url; }
        public String getType() { return type; }
        public void setType(String type) { this.type = type; }
    }

    // --- INNER STATIC CLASS CHO UserShortInfo ---
    public static class UserShortInfo implements Serializable { // Thêm Serializable nếu cần
        private String userId;
        private String fullName;
        private String avatarUrl;
        private String roleInProject;

        public UserShortInfo() {}
        public UserShortInfo(String userId, String fullName, String avatarUrl, String roleInProject) {
            this.userId = userId;
            this.fullName = fullName;
            this.avatarUrl = avatarUrl;
            this.roleInProject = roleInProject;
        }
        public String getUserId() { return userId; }
        public void setUserId(String userId) { this.userId = userId; }
        public String getFullName() { return fullName; }
        public void setFullName(String fullName) { this.fullName = fullName; }
        public String getAvatarUrl() { return avatarUrl; }
        public void setAvatarUrl(String avatarUrl) { this.avatarUrl = avatarUrl; }
        public String getRoleInProject() { return roleInProject; }
        public void setRoleInProject(String roleInProject) { this.roleInProject = roleInProject; }
    }

    // --- INNER STATIC CLASS CHO Comment ---
    // XÓA BỎ CLASS NÀY ĐI VÌ ĐÃ CÓ model.Comment.java
    /*
    public static class Comment {
        // ...
    }
    */
}