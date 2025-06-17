package com.cse441.tluprojectexpo.model; // Thay package của bạn
// Project.java

import com.google.firebase.Timestamp;
import com.google.firebase.firestore.PropertyName;

import java.util.List;

public class Project {
    private String projectId; // Để lưu ID document
    private String Title;
    private String Description;
    private String ThumbnailUrl;
    private String ProjectUrl;
    private String DemoUrl;
    private String VideoUrl;
    private String Status;
    private String CourseId;
    private String CreatorUserId;
    private Timestamp CreatedAt;
    private Timestamp UpdatedAt;
    private boolean IsApproved;
    private int VoteCount;
    private String ImageUrl;

    // Thông tin bổ sung (lấy từ các collection khác)
    private String creatorFullName;
    private List<String> technologyNames; // Hoặc List<Technology> nếu bạn có model Technology

    // Constructor mặc định cần thiết cho Firestore
    public Project() {}

    // Getters and Setters (Quan trọng là tên getter/setter phải khớp với tên trường trong Firestore,
    // hoặc sử dụng @PropertyName nếu tên khác)

    public String getProjectId() { return projectId; }
    public void setProjectId(String projectId) { this.projectId = projectId; }

    @PropertyName("Title")
    public String getTitle() { return Title; }
    @PropertyName("Title")
    public void setTitle(String title) { Title = title; }

    @PropertyName("Description")
    public String getDescription() { return Description; }
    @PropertyName("Description")
    public void setDescription(String description) { Description = description; }

    @PropertyName("ThumbnailUrl")
    public String getThumbnailUrl() { return ThumbnailUrl; }
    @PropertyName("ThumbnailUrl")
    public void setThumbnailUrl(String thumbnailUrl) { ThumbnailUrl = thumbnailUrl; }

    @PropertyName("ProjectUrl")
    public String getProjectUrl() { return ProjectUrl; }
    @PropertyName("ProjectUrl")
    public void setProjectUrl(String projectUrl) { ProjectUrl = projectUrl; }

    @PropertyName("DemoUrl")
    public String getDemoUrl() { return DemoUrl; }
    @PropertyName("DemoUrl")
    public void setDemoUrl(String demoUrl) { DemoUrl = demoUrl; }

    @PropertyName("VideoUrl")
    public String getVideoUrl() { return VideoUrl; }
    @PropertyName("VideoUrl")
    public void setVideoUrl(String videoUrl) { VideoUrl = videoUrl; }

    @PropertyName("Status")
    public String getStatus() { return Status; }
    @PropertyName("Status")
    public void setStatus(String status) { Status = status; }

    @PropertyName("CourseId")
    public String getCourseId() { return CourseId; }
    @PropertyName("CourseId")
    public void setCourseId(String courseId) { CourseId = courseId; }

    @PropertyName("CreatorUserId")
    public String getCreatorUserId() { return CreatorUserId; }
    @PropertyName("CreatorUserId")
    public void setCreatorUserId(String creatorUserId) { CreatorUserId = creatorUserId; }

    @PropertyName("CreatedAt")
    public Timestamp getCreatedAt() { return CreatedAt; }
    @PropertyName("CreatedAt")
    public void setCreatedAt(Timestamp createdAt) { CreatedAt = createdAt; }

    @PropertyName("UpdatedAt")
    public Timestamp getUpdatedAt() { return UpdatedAt; }
    @PropertyName("UpdatedAt")
    public void setUpdatedAt(Timestamp updatedAt) { UpdatedAt = updatedAt; }

    @PropertyName("IsApproved")
    public boolean isApproved() { return IsApproved; }
    @PropertyName("IsApproved")
    public void setApproved(boolean approved) { IsApproved = approved; }

    @PropertyName("VoteCount")
    public int getVoteCount() { return VoteCount; }
    @PropertyName("VoteCount")
    public void setVoteCount(int voteCount) { VoteCount = voteCount; }

    @PropertyName("ImageUrl")
    public String getImageUrl() { return ImageUrl; }
    @PropertyName("ImageUrl")
    public void setImageUrl(String imageUrl) { ImageUrl = imageUrl; }


    // Getters and Setters cho thông tin bổ sung
    public String getCreatorFullName() { return creatorFullName; }
    public void setCreatorFullName(String creatorFullName) { this.creatorFullName = creatorFullName; }

    public List<String> getTechnologyNames() { return technologyNames; }
    public void setTechnologyNames(List<String> technologyNames) { this.technologyNames = technologyNames; }
}