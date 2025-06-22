package com.cse441.tluprojectexpo.model;

import com.google.firebase.Timestamp;
import com.google.firebase.firestore.DocumentId;
import com.google.firebase.firestore.Exclude;
import com.google.firebase.firestore.PropertyName;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

public class Project implements Serializable {
    @DocumentId
    private String projectId;

    @PropertyName("Title")
    private String title;

    @PropertyName("CreatorUserId")
    private String creatorUserId;

    @PropertyName("IsApproved")
    private boolean isApproved;

    @PropertyName("IsFeatured")
    private boolean isFeatured;

    @PropertyName("CreatedAt")
    private Timestamp createdAt;

    @PropertyName("Description")
    private String description;

    @PropertyName("ThumbnailUrl")
    private String thumbnailUrl;

    @PropertyName("Status")
    private String status;

    @Exclude
    private String creatorFullName;
    @Exclude
    private List<String> technologyNames = new ArrayList<>();
    @Exclude
    private List<String> categoryNames = new ArrayList<>();

    public Project() {}

    // Getters and Setters chuẩn hóa
    public String getProjectId() { return projectId; }
    public void setProjectId(String projectId) { this.projectId = projectId; }

    @PropertyName("Title")
    public String getTitle() { return title; }
    @PropertyName("Title")
    public void setTitle(String title) { this.title = title; }

    @PropertyName("CreatorUserId")
    public String getCreatorUserId() { return creatorUserId; }
    @PropertyName("CreatorUserId")
    public void setCreatorUserId(String creatorUserId) { this.creatorUserId = creatorUserId; }

    @PropertyName("IsApproved")
    public boolean isApproved() { return isApproved; }
    @PropertyName("IsApproved")
    public void setApproved(boolean approved) { isApproved = approved; }

    @PropertyName("IsFeatured")
    public boolean isFeatured() { return isFeatured; }
    @PropertyName("IsFeatured")
    public void setFeatured(boolean featured) { this.isFeatured = featured; }

    @PropertyName("CreatedAt")
    public Timestamp getCreatedAt() { return createdAt; }
    @PropertyName("CreatedAt")
    public void setCreatedAt(Timestamp createdAt) { this.createdAt = createdAt; }

    @PropertyName("Description")
    public String getDescription() { return description; }
    @PropertyName("Description")
    public void setDescription(String description) { this.description = description; }

    @PropertyName("ThumbnailUrl")
    public String getThumbnailUrl() { return thumbnailUrl; }
    @PropertyName("ThumbnailUrl")
    public void setThumbnailUrl(String thumbnailUrl) { this.thumbnailUrl = thumbnailUrl; }

    @PropertyName("Status")
    public String getStatus() { return status; }
    @PropertyName("Status")
    public void setStatus(String status) { this.status = status; }

    @Exclude
    public String getCreatorFullName() { return creatorFullName; }
    @Exclude
    public void setCreatorFullName(String creatorFullName) { this.creatorFullName = creatorFullName; }

    @Exclude
    public List<String> getTechnologyNames() { return technologyNames; }
    @Exclude
    public void setTechnologyNames(List<String> technologyNames) { this.technologyNames = technologyNames; }

    @Exclude
    public List<String> getCategoryNames() { return categoryNames; }
    @Exclude
    public void setCategoryNames(List<String> categoryNames) { this.categoryNames = categoryNames; }
}