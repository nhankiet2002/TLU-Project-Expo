package com.cse441.tluprojectexpo.model;

public class FeaturedProjectUIModel {

    private String projectId;
    private String projectTitle;
    private String creatorName;
    private String categoryName;

    public FeaturedProjectUIModel() {}

    public FeaturedProjectUIModel(String projectId, String title, String authorName, String categoryName, String thumbnailUrl, int voteCount) {
        this.projectId = projectId;
        this.projectTitle = title;
        this.creatorName = authorName;
        this.categoryName = categoryName;
    }

    // Getters
    public String getProjectId() { return projectId; }
    public String getProjectTitle() { return projectTitle; }
    public String getCreatorName() { return creatorName; }
    public String getCategoryName() { return categoryName; }

    // Setters
    public void setProjectId(String projectId) { this.projectId = projectId; }
    public void setProjectTitle(String projectTitle) { this.projectTitle = projectTitle; }
    public void setCreatorName(String creatorName) { this.creatorName = creatorName; }
    public void setCategoryName(String categoryName) { this.categoryName = categoryName; }

}
