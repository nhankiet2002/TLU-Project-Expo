package com.cse441.tluprojectexpo.model;

import com.google.firebase.Timestamp;          // Dùng để làm việc với kiểu dữ liệu Timestamp của Firestore
import com.google.firebase.firestore.PropertyName; // Dùng để ánh xạ tên trường trong Java với tên trường trong Firestore nếu chúng khác nhau
import com.google.firebase.firestore.Exclude;      // Dùng để loại trừ một trường không được mapping với Firestore

import java.util.List;
import java.util.ArrayList;

public class Project {

    // --- CÁC TRƯỜNG LƯU TRỮ TRỰC TIẾP TRONG DOCUMENT 'Projects' TRÊN FIRESTORE ---
    // Các trường này sẽ được Firestore tự động đọc/ghi khi bạn thao tác với đối tượng Project.
    @Exclude // Trường này không được đọc/ghi trực tiếp từ/vào Firestore như một field của document.
    // Nó sẽ được gán giá trị ID của document sau khi đọc dữ liệu.
    private String projectId; // ID của document Project trong Firestore.

    @PropertyName("Title") // Ánh xạ với trường "Title" trong Firestore.
    // Dùng khi tên biến Java (Title) có thể gây nhầm lẫn hoặc bạn muốn chắc chắn.
    private String Title; // Tiêu đề của dự án.

    @PropertyName("Description")
    private String Description; // Mô tả chi tiết về dự án.

    @PropertyName("ThumbnailUrl")
    private String ThumbnailUrl; // URL của ảnh thumbnail chính, hiển thị đại diện cho dự án.
    // Đây là ảnh bìa chính.

    // Danh sách các media (ảnh, video) khác liên quan đến dự án.
    // Firestore sẽ lưu trữ đây là một mảng các đối tượng (map).
    @PropertyName("MediaGalleryUrls")
    private List<MediaItem> MediaGalleryUrls; // Mỗi MediaItem sẽ có 'url' và 'type'.

    @PropertyName("ProjectUrl")
    private String ProjectUrl; // URL tới mã nguồn của dự án (ví dụ: link GitHub, GitLab).
    // Có thể là null nếu không có.

    @PropertyName("DemoUrl")
    private String DemoUrl; // URL tới bản demo trực tuyến của dự án (nếu có).
    // Có thể là null nếu không có.

    @PropertyName("Status")
    private String Status; // Trạng thái hiện tại của dự án (ví dụ: "Hoàn thành", "Đang thực hiện", "Tạm dừng").

    @PropertyName("CourseId")
    private String CourseId; // ID của khóa học hoặc môn học mà dự án này thuộc về (nếu có).
    // Có thể là null.

    @PropertyName("CreatorUserId")
    private String CreatorUserId; // ID của người dùng (User) đã tạo ra dự án này.
    // Dùng để liên kết với collection 'Users'.

    @PropertyName("CreatedAt")
    private Timestamp CreatedAt; // Thời điểm dự án được tạo (lưu dưới dạng Timestamp của Firestore).

    @PropertyName("UpdatedAt")
    private Timestamp UpdatedAt; // Thời điểm thông tin dự án được cập nhật lần cuối (Timestamp).

    @PropertyName("IsApproved")
    private boolean IsApproved; // Trạng thái phê duyệt của dự án (true nếu đã được duyệt, false nếu chưa).

    @PropertyName("VoteCount")
    private int VoteCount; // Tổng số lượt bình chọn (upvote) mà dự án nhận được.


    // --- CÁC TRƯỜNG THÔNG TIN BỔ SUNG (KHÔNG LƯU TRỰC TIẾP TRONG DOCUMENT 'Projects') ---
    // Các trường này KHÔNG được Firestore tự động map khi bạn đọc một document từ collection 'Projects'.
    // Thay vào đó, bạn sẽ cần viết logic riêng để truy vấn các collection khác (Users, Technologies, Categories, ProjectMembers)
    // và gán giá trị cho các trường này sau khi đã có đối tượng Project chính.
    // Chúng được đánh dấu @Exclude để Firestore bỏ qua khi ghi/đọc.

    @Exclude
    private String creatorFullName; // Họ tên đầy đủ của người tạo dự án.
    // Sẽ được lấy từ collection 'Users' dựa trên 'CreatorUserId'.

    @Exclude
    private List<String> technologyNames; // Danh sách tên các công nghệ được sử dụng trong dự án.
    // Sẽ được tổng hợp từ 'ProjectTechnologies' và 'Technologies'.
    @Exclude
    private List<String> categoryNames;   // Danh sách tên các danh mục mà dự án thuộc về.
    // Sẽ được tổng hợp từ 'ProjectCategories' và 'Categories'.
    @Exclude
    private List<UserShortInfo> projectMembersInfo; // Danh sách thông tin rút gọn của các thành viên tham gia dự án.
    // Sẽ được tổng hợp từ 'ProjectMembers' và 'Users'.


    // Constructor mặc định (không tham số) là BẮT BUỘC để Firestore có thể deserialize
    // (chuyển đổi từ dữ liệu Firestore sang đối tượng Java).
    public Project() {
        // Khởi tạo các List để tránh NullPointerException nếu Firestore trả về null cho các trường này
        // hoặc nếu chúng không được thiết lập sau khi đối tượng được tạo.
        this.MediaGalleryUrls = new ArrayList<>();
        this.technologyNames = new ArrayList<>();
        this.categoryNames = new ArrayList<>();
        this.projectMembersInfo = new ArrayList<>();
    }

    // --- GETTERS AND SETTERS ---
    // Firestore sử dụng các public getters và setters này để đọc và ghi dữ liệu vào các trường.
    // Tên của getter/setter phải tuân theo convention của Java (ví dụ: getTitle(), setTitle())
    // hoặc khớp với tên đã khai báo trong @PropertyName.

    // Getter và Setter cho projectId
    public String getProjectId() { return projectId; }
    public void setProjectId(String projectId) { this.projectId = projectId; }

    // Các getter/setter cho các trường được ánh xạ với Firestore
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

    // Đối với kiểu boolean, getter có thể là is<PropertyName> hoặc get<PropertyName>.
    // Firestore hỗ trợ cả hai. isApproved() là cách viết phổ biến.
    public boolean isApproved() { return IsApproved; }
    // Setter cho boolean.
    public void setApproved(boolean approved) { this.IsApproved = approved; }


    public int getVoteCount() { return VoteCount; }
    public void setVoteCount(int voteCount) { this.VoteCount = voteCount; }


    // Getters and Setters cho các trường thông tin bổ sung (không cần @PropertyName vì chúng @Exclude)
    public String getCreatorFullName() { return creatorFullName; }
    public void setCreatorFullName(String creatorFullName) { this.creatorFullName = creatorFullName; }

    public List<String> getTechnologyNames() { return technologyNames; }
    public void setTechnologyNames(List<String> technologyNames) { this.technologyNames = technologyNames; }

    public List<String> getCategoryNames() { return categoryNames; }
    public void setCategoryNames(List<String> categoryNames) { this.categoryNames = categoryNames; }

    public List<UserShortInfo> getProjectMembersInfo() { return projectMembersInfo; }
    public void setProjectMembersInfo(List<UserShortInfo> projectMembersInfo) { this.projectMembersInfo = projectMembersInfo; }


    // --- INNER CLASS CHO MediaItem ---
    // Class này đại diện cho một mục media (ảnh hoặc video) trong danh sách MediaGalleryUrls.
    // Nó phải là public static class nếu bạn muốn Firestore có thể deserialize nó khi nó là một phần của Project.
    // Hoặc bạn có thể tạo MediaItem.java riêng.
    public static class MediaItem {
        @PropertyName("url")
        private String url; // URL của file media (ảnh hoặc video).

        @PropertyName("type")
        private String type; // Loại media, ví dụ: "image", "video".

        // Constructor mặc định cần thiết cho Firestore.
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

    // --- INNER CLASS (VÍ DỤ) CHO UserShortInfo ---
    // Class này dùng để chứa thông tin rút gọn của thành viên dự án.
    // Bạn có thể tùy chỉnh các trường cần thiết.
    public static class UserShortInfo {
        private String userId;
        private String fullName;
        private String avatarUrl;
        private String roleInProject; // Vai trò trong dự án (ví dụ: "nhóm trưởng", "thành viên")

        public UserShortInfo() {}

        public UserShortInfo(String userId, String fullName, String avatarUrl, String roleInProject) {
            this.userId = userId;
            this.fullName = fullName;
            this.avatarUrl = avatarUrl;
            this.roleInProject = roleInProject;
        }

        // Getters and Setters
        public String getUserId() { return userId; }
        public void setUserId(String userId) { this.userId = userId; }

        public String getFullName() { return fullName; }
        public void setFullName(String fullName) { this.fullName = fullName; }

        public String getAvatarUrl() { return avatarUrl; }
        public void setAvatarUrl(String avatarUrl) { this.avatarUrl = avatarUrl; }

        public String getRoleInProject() { return roleInProject; }
        public void setRoleInProject(String roleInProject) { this.roleInProject = roleInProject; }
    }
}