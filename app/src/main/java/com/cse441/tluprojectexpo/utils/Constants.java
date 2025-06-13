// app/src/main/java/com/cse441.tluprojectexpo/utils/Constants.java
package com.cse441.tluprojectexpo.utils;

public class Constants {

    // Firestore Collections
    public static final String COLLECTION_USERS = "users";
    public static final String COLLECTION_PROJECTS = "projects";
    public static final String COLLECTION_NOTIFICATIONS = "notifications";
    public static final String COLLECTION_COMMENTS = "comments";

    // User Roles
    public static final String ROLE_ADMIN = "admin";
    public static final String ROLE_STUDENT = "student";
    public static final String ROLE_FACULTY = "faculty"; // Giảng viên/Cố vấn

    // Permissions (Ví dụ các quyền)
    public static final String PERMISSION_CREATE_PROJECT = "create_project";
    public static final String PERMISSION_EDIT_OWN_PROJECT = "edit_own_project";
    public static final String PERMISSION_DELETE_OWN_PROJECT = "delete_own_project";
    public static final String PERMISSION_VIEW_OWN_PROJECTS = "view_own_projects";
    public static final String PERMISSION_VIEW_ALL_PROJECTS = "view_all_projects";
    public static final String PERMISSION_COMMENT = "comment";
    public static final String PERMISSION_VOTE = "vote";
    public static final String PERMISSION_APPROVE_CONTENT = "approve_content"; // Ví dụ cho giảng viên
    public static final String PERMISSION_MANAGE_USERS = "manage_users"; // Ví dụ cho admin

    // Storage Paths
    public static final String STORAGE_PROFILE_IMAGES = "profile_images/";
    public static final String STORAGE_PROJECT_IMAGES = "project_images/";

    // Other Constants
    public static final int PICK_IMAGE_REQUEST = 1; // Request code cho việc chọn ảnh
    public static final String EXTRA_PROJECT_ID = "projectId"; // Key cho Intent extra

    // Thêm các hằng số khác nếu cần...
}