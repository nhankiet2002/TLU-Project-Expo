package com.cse441.tluprojectexpo.utils;

public class Constants {

    // Firestore Collection Names
    public static final String COLLECTION_PROJECTS = "Projects";
    public static final String COLLECTION_USERS = "Users";
    public static final String COLLECTION_COURSES = "Courses"; // Thêm nếu chưa có
    public static final String COLLECTION_CATEGORIES = "Categories";
    public static final String COLLECTION_TECHNOLOGIES = "Technologies";
    public static final String COLLECTION_PROJECT_MEMBERS = "ProjectMembers";
    public static final String COLLECTION_PROJECT_CATEGORIES = "ProjectCategories";
    public static final String COLLECTION_PROJECT_TECHNOLOGIES = "ProjectTechnologies";
    public static final String COLLECTION_COMMENTS = "Comments"; // Thêm
    public static final String COLLECTION_PROJECT_VOTES = "ProjectVotes"; // Thêm
    public static final String SUB_COLLECTION_VOTERS = "Voters"; // Thêm
    public static final String COLLECTION_NOTIFICATIONS = "Notifications";

    // Firestore Field Names
    public static final String FIELD_NAME = "Name";
    public static final String FIELD_COURSE_NAME = "CourseName"; // Thêm
    public static final String FIELD_USER_ID = "UserId"; // Trong ProjectMembers, UserRoles
    public static final String FIELD_PROJECT_ID = "ProjectId";
    public static final String FIELD_CATEGORY_ID = "CategoryId";
    public static final String FIELD_TECHNOLOGY_ID = "TechnologyId";
    public static final String FIELD_ROLE_IN_PROJECT = "RoleInProject";
    public static final String FIELD_FULL_NAME = "FullName";
    public static final String FIELD_AVATAR_URL = "AvatarUrl"; // Thêm
    public static final String FIELD_CREATED_AT = "CreatedAt"; // Trong Comments, Projects
    public static final String FIELD_VOTE_COUNT = "VoteCount"; // Trong Projects
    public static final String FIELD_VOTED_AT = "votedAt"; // Trong ProjectVotes/Voters
    public static final String FIELD_AUTHOR_USER_ID = "AuthorUserId"; // Trong Comments
    public static final String FIELD_CONTENT = "Content"; // Trong Comments
    public static final String FIELD_CREATOR_USER_ID = "CreatorUserId"; // Trong Projects

//     User Roles related (Nếu bạn có collection Roles và UserRoles riêng)
     public static final String COLLECTION_USER_ROLES = "UserRoles";
     public static final String FIELD_ROLE_ID = "RoleId";
     public static final String ROLE_ID_USER = "role_user";
     public static final String ROLE_ID_ADMIN = "role_admin";
     public static final String FIELD_IS_LOCKED = "IsLocked";


    // Cloudinary (Giữ nguyên hoặc cập nhật)
    public static final String CLOUDINARY_UPLOAD_PRESET_THUMBNAIL = "TLUProjectExpo";
    public static final String CLOUDINARY_UPLOAD_PRESET_MEDIA = "TLUProjectExpo";
    public static final String CLOUDINARY_FOLDER_PROJECT_THUMBNAILS = "project_thumbnails_expo";
    public static final String CLOUDINARY_FOLDER_PROJECT_MEDIA = "project_media_expo";

    // Link Platforms (Giữ nguyên)
    public static final String PLATFORM_GITHUB = "GitHub";
    public static final String PLATFORM_DEMO = "Demo";

    // Other constants
    public static final String DEFAULT_MEMBER_ROLE = "Thành viên";
    public static final String DEFAULT_LEADER_ROLE = "Trưởng nhóm";
}