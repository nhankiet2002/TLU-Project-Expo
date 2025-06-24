package com.cse441.tluprojectexpo.model;

import com.google.firebase.Timestamp;
import com.google.firebase.firestore.DocumentId;
import com.google.firebase.firestore.Exclude;
import com.google.firebase.firestore.PropertyName;

import java.io.Serializable;
import java.util.List;
import java.util.ArrayList;

public class Project implements Serializable {

    @DocumentId
    private String projectId;


    @PropertyName("Title")
    private String title;

    @PropertyName("Description")
    private String description;

    @PropertyName("ThumbnailUrl")
    private String thumbnailUrl;

    @PropertyName("MediaGalleryUrls")
    private List<MediaItem> mediaGalleryUrls;

    @PropertyName("ProjectUrl")
    private String projectUrl;

    @PropertyName("DemoUrl")
    private String demoUrl;

    @PropertyName("Status")
    private String status;

    @PropertyName("CourseId") // Có thể là null
    private String courseId;

    @PropertyName("CreatorUserId")
    private String creatorUserId;

    @PropertyName("CreatedAt")
    private Timestamp createdAt;

    // Trường này có thể null nếu không tồn tại trong document
    @PropertyName("UpdatedAt")
    private Timestamp updatedAt;


    private boolean isApproved;


    private boolean isFeatured = false;

    @PropertyName("VoteCount")
    private int voteCount;

    // --- CÁC TRƯỜNG BỔ SUNG, KHÔNG LƯU TRÊN FIRESTORE ---
    @Exclude
    private String creatorFullName;
    @Exclude
    private List<String> technologyNames;
    @Exclude
    private List<String> categoryNames;
    @Exclude
    private List<UserShortInfo> projectMembersInfo;

    public Project() {
        this.mediaGalleryUrls = new ArrayList<>();
        this.technologyNames = new ArrayList<>();
        this.categoryNames = new ArrayList<>();
        this.projectMembersInfo = new ArrayList<>();
    }

    // --- GETTERS AND SETTERS ---
    // (Giữ nguyên các getters và setters bạn đã có)
    public String getProjectId() { return projectId; }
    public void setProjectId(String projectId) { this.projectId = projectId; }

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public String getThumbnailUrl() { return thumbnailUrl; }
    public void setThumbnailUrl(String thumbnailUrl) { this.thumbnailUrl = thumbnailUrl; }

    public List<MediaItem> getMediaGalleryUrls() { return mediaGalleryUrls; }
    public void setMediaGalleryUrls(List<MediaItem> mediaGalleryUrls) { this.mediaGalleryUrls = mediaGalleryUrls; }

    public String getProjectUrl() { return projectUrl; }
    public void setProjectUrl(String projectUrl) { this.projectUrl = projectUrl; }

    public String getDemoUrl() { return demoUrl; }
    public void setDemoUrl(String demoUrl) { this.demoUrl = demoUrl; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public String getCourseId() { return courseId; }
    public void setCourseId(String courseId) { this.courseId = courseId; }

    public String getCreatorUserId() { return creatorUserId; }
    public void setCreatorUserId(String creatorUserId) { this.creatorUserId = creatorUserId; }

    public Timestamp getCreatedAt() { return createdAt; }
    public void setCreatedAt(Timestamp createdAt) { this.createdAt = createdAt; }

    public Timestamp getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(Timestamp updatedAt) { this.updatedAt = updatedAt; }

    // Đối với kiểu boolean, getter chuẩn là "isSomething()" và setter là "setSomething()"
    @PropertyName("IsApproved")
    public boolean getIsApproved() { return isApproved; }
    @PropertyName("IsApproved")
    public void setApproved(boolean approved) { isApproved = approved; }

    @PropertyName("IsFeatured")
    public boolean isFeatured() { return isFeatured; }

    @PropertyName("IsFeatured")
    public void setFeatured(boolean featured) { isFeatured = featured; }

    public int getVoteCount() { return voteCount; }
    public void setVoteCount(int voteCount) { this.voteCount = voteCount; }

    @Exclude
    public String getCreatorFullName() { return creatorFullName; }
    public void setCreatorFullName(String creatorFullName) { this.creatorFullName = creatorFullName; }

    @Exclude
    public List<String> getTechnologyNames() { return technologyNames; }
    public void setTechnologyNames(List<String> technologyNames) { this.technologyNames = technologyNames; }

    @Exclude
    public List<String> getCategoryNames() { return categoryNames; }
    public void setCategoryNames(List<String> categoryNames) { this.categoryNames = categoryNames; }

    @Exclude
    public List<UserShortInfo> getProjectMembersInfo() { return projectMembersInfo; }
    public void setProjectMembersInfo(List<UserShortInfo> projectMembersInfo) { this.projectMembersInfo = projectMembersInfo; }


    // --- INNER STATIC CLASS CHO MediaItem ---
    public static class MediaItem implements Serializable { // Thêm Serializable nếu cần
        @PropertyName("url") // Khớp với JSON
        private String url;
        @PropertyName("type") // Khớp với JSON
        private String type;

        public MediaItem() {}
        public String getUrl() { return url; }
        public void setUrl(String url) { this.url = url; }
        public String getType() { return type; }
        public void setType(String type) { this.type = type; }
    }

    public static class UserShortInfo implements Serializable {
        private String userId;
        private String fullName;
        private String avatarUrl;
        private String roleInProject;
        private String className;

        public UserShortInfo() {}

        public UserShortInfo(String id, String fullName, String avatarUrl, String roleInProject) {
            this.userId = id;
            this.fullName = fullName;
            this.avatarUrl = avatarUrl;
            this.roleInProject = roleInProject;
        }
        public UserShortInfo(String userId, String fullName, String avatarUrl, String roleInProject, String className) {
            this.userId = userId;
            this.fullName = fullName;
            this.avatarUrl = avatarUrl;
            this.roleInProject = roleInProject;
            this.className = className;
        }
        public String getUserId() { return userId; }
        public void setUserId(String userId) { this.userId = userId; }
        public String getFullName() { return fullName; }
        public void setFullName(String fullName) { this.fullName = fullName; }
        public String getAvatarUrl() { return avatarUrl; }
        public void setAvatarUrl(String avatarUrl) { this.avatarUrl = avatarUrl; }
        public String getRoleInProject() { return roleInProject; }
        public void setRoleInProject(String roleInProject) { this.roleInProject = roleInProject; }
        public String getClassName() { return className; }
        public void setClassName(String className) { this.className = className; }
    }

    @Exclude
    private List<Comment> comments;
    @Exclude
    public List<Comment> getComments() {
        return comments;
    }
    @Exclude
    public void setComments(List<Comment> comments) {
        this.comments = comments;
    }
}