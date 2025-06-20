package com.cse441.tluprojectexpo.util;

public class Constants {

    // Firestore Collection Names
    public static final String COLLECTION_PROJECTS = "Projects";
    public static final String COLLECTION_USERS = "Users";
    public static final String COLLECTION_CATEGORIES = "Categories";
    public static final String COLLECTION_TECHNOLOGIES = "Technologies";
    public static final String COLLECTION_PROJECT_MEMBERS = "ProjectMembers";
    public static final String COLLECTION_PROJECT_CATEGORIES = "ProjectCategories";
    public static final String COLLECTION_PROJECT_TECHNOLOGIES = "ProjectTechnologies";
    public static final String COLLECTION_USER_ROLES = "UserRoles";

    // Firestore Field Names (ví dụ, nếu cần tham chiếu ở nhiều nơi)
    public static final String FIELD_NAME = "Name";
    public static final String FIELD_USER_ID = "UserId";
    public static final String FIELD_PROJECT_ID = "ProjectId";
    public static final String FIELD_CATEGORY_ID = "CategoryId";
    public static final String FIELD_TECHNOLOGY_ID = "TechnologyId";
    public static final String FIELD_ROLE_IN_PROJECT = "RoleInProject";
    public static final String FIELD_FULL_NAME = "FullName";
    public static final String FIELD_ROLE_ID = "RoleId"; // Trong UserRoles và có thể cả Roles collection THÊM MỚI

    // Firestore Role ID Values
    public static final String ROLE_ID_ADMIN = "role_admin"; // THÊM MỚI - ID của vai trò Admin

    // Cloudinary Upload Presets
    public static final String CLOUDINARY_UPLOAD_PRESET_THUMBNAIL = "TLUProjectExpo"; // Thay bằng preset thực tế của bạn
    public static final String CLOUDINARY_UPLOAD_PRESET_MEDIA = "TLUProjectExpo";     // Thay bằng preset thực tế của bạn

    // Cloudinary Folders
    public static final String CLOUDINARY_FOLDER_PROJECT_THUMBNAILS = "project_thumbnails_expo";
    public static final String CLOUDINARY_FOLDER_PROJECT_MEDIA = "project_media_expo";


    // Other constants
    public static final String DEFAULT_MEMBER_ROLE = "thành viên";
    public static final String DEFAULT_LEADER_ROLE = "nhóm trưởng";
}
