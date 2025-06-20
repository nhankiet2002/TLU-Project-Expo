package com.cse441.tluprojectexpo.model;

import com.google.firebase.Timestamp;          // Dùng để làm việc với kiểu dữ liệu Timestamp của Firestore
import com.google.firebase.firestore.PropertyName; // Dùng để ánh xạ tên trường trong Java với tên trường trong Firestore nếu chúng khác nhau
import com.google.firebase.firestore.Exclude;      // Dùng để loại trừ một trường không được mapping với Firestore

import java.io.Serializable;
import java.util.List;
import java.util.ArrayList;

public class Project implements Serializable {

    // --- CÁC TRƯỜNG LƯU TRỮ TRỰC TIẾP TRONG DOCUMENT 'Projects' TRÊN FIRESTORE ---
    @Exclude
    private String projectId;

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

    @PropertyName("CourseId")
    private String CourseId;

    @PropertyName("CreatorUserId")
    private String CreatorUserId;

    @PropertyName("CreatedAt")
    private Timestamp CreatedAt;

    @PropertyName("UpdatedAt")
    private Timestamp UpdatedAt;

    @PropertyName("IsApproved")
    private boolean IsApproved;

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

    // --- GETTERS AND SETTERS CHO PROJECT ---
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

    public boolean isApproved() { return IsApproved; }
    public void setApproved(boolean approved) { this.IsApproved = approved; }

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
    public static class MediaItem {
        @PropertyName("url")
        private String url;
        @PropertyName("type")
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
    public static class UserShortInfo {
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
    // Model này được thiết kế để phù hợp với cấu trúc dữ liệu comment trên Firestore
    // ví dụ: "comment_014": { "ProjectId": "project_001", "AuthorUserId": "user_002",
    //                        "Content": "...", "CreatedAt": "2023-08-04T15:00:00Z",
    //                        "ParentCommentId": "comment_013" }
    public static class Comment {
        @Exclude // ID của document comment, sẽ được gán sau khi đọc
        private String commentId;

        @PropertyName("ProjectId")
        private String projectId;

        @PropertyName("AuthorUserId") // Ánh xạ với AuthorUserId trong Firestore
        private String userId; // Tên biến trong Java là userId

        @PropertyName("Content") // Ánh xạ với Content trong Firestore
        private String text; // Tên biến trong Java là text

        @PropertyName("CreatedAt") // Ánh xạ với CreatedAt trong Firestore
        private Timestamp timestamp; // QUAN TRỌNG: Firestore nên lưu trường này dưới dạng Timestamp.
        // Nếu Firestore lưu là String (ví dụ: "2023-08-04T15:00:00Z"),
        // bạn cần đổi kiểu ở đây thành String và tự parse,
        // hoặc đảm bảo dữ liệu được lưu đúng kiểu Timestamp khi ghi vào Firestore.
        // Với ví dụ JSON bạn đưa, nó là String. Nếu dữ liệu thực sự là String,
        // bạn nên đổi `Timestamp timestamp;` thành `String createdAtString;`
        // và có thể thêm getter để parse nó thành Date/Timestamp nếu cần.
        // Tạm thời, tôi giả định bạn sẽ lưu nó dạng Timestamp trong Firestore.

        @PropertyName("ParentCommentId") // Ánh xạ với ParentCommentId trong Firestore
        private String parentCommentId;  // Có thể là null nếu không phải comment trả lời

        // Các trường này sẽ được load riêng sau khi có userId (AuthorUserId)
        @Exclude
        private String userName;
        @Exclude
        private String userAvatarUrl;

        // Firestore cần constructor rỗng
        public Comment() {}

        // Constructor để tạo comment mới (khi post)
        // Lưu ý: userName và userAvatarUrl không nhất thiết phải có trong constructor này
        // nếu chúng chỉ dùng để hiển thị và được lấy sau.
        // Tuy nhiên, nếu bạn muốn truyền chúng khi tạo comment mới (ví dụ, để hiển thị ngay lập tức
        // mà không cần truy vấn lại user), thì có thể giữ chúng.
        public Comment(String projectId, String userId, String text, Timestamp timestamp, String parentCommentId) {
            this.projectId = projectId;
            this.userId = userId;
            this.text = text;
            this.timestamp = timestamp;
            this.parentCommentId = parentCommentId;
            // userName và userAvatarUrl sẽ được set riêng
        }

        // Constructor đầy đủ hơn nếu bạn muốn truyền cả thông tin user khi tạo
        public Comment(String projectId, String userId, String userName, String userAvatarUrl, String text, Timestamp timestamp, String parentCommentId) {
            this.projectId = projectId;
            this.userId = userId;
            this.userName = userName;
            this.userAvatarUrl = userAvatarUrl;
            this.text = text;
            this.timestamp = timestamp;
            this.parentCommentId = parentCommentId;
        }


        // Getters and Setters
        @Exclude
        public String getCommentId() { return commentId; }
        @Exclude
        public void setCommentId(String commentId) { this.commentId = commentId; }

        @PropertyName("ProjectId")
        public String getProjectId() { return projectId; }
        @PropertyName("ProjectId")
        public void setProjectId(String projectId) { this.projectId = projectId; }

        @PropertyName("AuthorUserId")
        public String getUserId() { return userId; }
        @PropertyName("AuthorUserId")
        public void setUserId(String userId) { this.userId = userId; }

        @PropertyName("Content")
        public String getText() { return text; }
        @PropertyName("Content")
        public void setText(String text) { this.text = text; }

        @PropertyName("CreatedAt")
        public Timestamp getTimestamp() { return timestamp; }
        @PropertyName("CreatedAt")
        public void setTimestamp(Timestamp timestamp) { this.timestamp = timestamp; }

        @PropertyName("ParentCommentId")
        public String getParentCommentId() { return parentCommentId; }
        @PropertyName("ParentCommentId")
        public void setParentCommentId(String parentCommentId) { this.parentCommentId = parentCommentId; }

        @Exclude
        public String getUserName() { return userName; }
        @Exclude
        public void setUserName(String userName) { this.userName = userName; }

        @Exclude
        public String getUserAvatarUrl() { return userAvatarUrl; }
        @Exclude
        public void setUserAvatarUrl(String userAvatarUrl) { this.userAvatarUrl = userAvatarUrl; }
    }
}