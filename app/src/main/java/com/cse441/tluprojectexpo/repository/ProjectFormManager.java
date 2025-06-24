package com.cse441.tluprojectexpo.repository;

import android.content.Context;
import android.net.Uri;
import android.text.TextUtils;
import android.util.Log;

import com.cse441.tluprojectexpo.model.LinkItem;
import com.cse441.tluprojectexpo.model.Project;
import com.cse441.tluprojectexpo.model.User;
import com.cse441.tluprojectexpo.service.FirestoreService;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FieldValue;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Quản lý form dự án chung cho cả Create và Edit Project
 */
public class ProjectFormManager {
    private static final String TAG = "ProjectFormManager";

    private Uri projectImageUri;

    // Data containers
    private List<String> selectedCategoryNames = new ArrayList<>();
    private List<String> selectedTechnologyNames = new ArrayList<>();
    private List<User> selectedProjectUsers = new ArrayList<>();
    private Map<String, String> userRolesInProject = new HashMap<>();
    private List<LinkItem> projectLinks = new ArrayList<>();
    private List<Uri> selectedMediaUris = new ArrayList<>();

    // Dropdown data
    private List<String> categoryNameListForDropdown = new ArrayList<>();
    private Map<String, String> categoryNameToIdMap = new HashMap<>();
    private List<String> allAvailableTechnologyNames = new ArrayList<>();
    private Map<String, String> technologyNameToIdMap = new HashMap<>();
    private Map<String, String> technologyIdToNameMap = new HashMap<>();
    private List<String> statusNameListForDropdown = Arrays.asList("Đang thực hiện", "Hoàn thành", "Tạm dừng");

    // Services
    private FirestoreService firestoreService;

    public ProjectFormManager(Context context) {
        this.firestoreService = new FirestoreService();
    }

    // === CATEGORIES MANAGEMENT ===
    public void fetchCategories(FirestoreService.CategoriesFetchListener listener) {
        firestoreService.fetchCategories(listener);
    }
    
    public void updateCategoryData(List<String> fetchedCategoryNames, Map<String, String> fetchedNameToIdMap) {
        categoryNameListForDropdown.clear();
        categoryNameToIdMap.clear();
        categoryNameListForDropdown.addAll(fetchedCategoryNames);
        categoryNameToIdMap.putAll(fetchedNameToIdMap);
        Log.d(TAG, "Categories fetched: " + fetchedCategoryNames.size());
    }

    public List<String> getCategoryNameListForDropdown() {
        return categoryNameListForDropdown;
    }

    public Map<String, String> getCategoryNameToIdMap() {
        return categoryNameToIdMap;
    }

    public List<String> getSelectedCategoryNames() {
        return selectedCategoryNames;
    }
    
    public boolean addCategory(String categoryName) {
        if (categoryName != null && !categoryName.trim().isEmpty() &&
                !selectedCategoryNames.contains(categoryName) &&
                selectedCategoryNames.size() < 3) {
            selectedCategoryNames.add(categoryName);
            return true;
        }
        return false;
    }

    public boolean removeCategory(String categoryName) {
        return selectedCategoryNames.remove(categoryName);
    }

    public List<String> getSelectedCategoryIds() {
        return selectedCategoryNames.stream()
                .map(name -> categoryNameToIdMap.get(name))
                .filter(id -> id != null && !id.isEmpty())
                .collect(Collectors.toList());
    }

    // === TECHNOLOGIES MANAGEMENT ===
    public void fetchTechnologies(FirestoreService.TechnologyFetchListener listener) {
        firestoreService.fetchTechnologies(listener);
    }
    
    public void updateTechnologyData(List<String> fetchedTechnologyNames, Map<String, String> fetchedTechNameToIdMap) {
        allAvailableTechnologyNames.clear();
        technologyNameToIdMap.clear();
        technologyIdToNameMap.clear();
        allAvailableTechnologyNames.addAll(fetchedTechnologyNames);
        technologyNameToIdMap.putAll(fetchedTechNameToIdMap);

        for (Map.Entry<String, String> entry : fetchedTechNameToIdMap.entrySet()) {
            technologyIdToNameMap.put(entry.getValue(), entry.getKey());
        }
        Log.d(TAG, "Technologies fetched: " + fetchedTechnologyNames.size());
    }

    public List<String> getAllAvailableTechnologyNames() {
        return allAvailableTechnologyNames;
    }
    
    public List<String> getSelectedTechnologyNames() {
        return selectedTechnologyNames;
    }

    public boolean addTechnology(String techName) {
        if (techName != null && !techName.trim().isEmpty() &&
                !selectedTechnologyNames.contains(techName) &&
                selectedTechnologyNames.size() < 10) {
            selectedTechnologyNames.add(techName);
            return true;
        }
        return false;
    }

    public boolean removeTechnology(String techName) {
        return selectedTechnologyNames.remove(techName);
    }

    public List<String> getSelectedTechnologyIds() {
        return selectedTechnologyNames.stream()
                .map(name -> technologyNameToIdMap.get(name))
                .filter(id -> id != null && !id.isEmpty())
                .collect(Collectors.toList());
    }

    // === STATUS MANAGEMENT ===
    public List<String> getStatusNameListForDropdown() {
        return statusNameListForDropdown;
    }

    // === MEMBERS MANAGEMENT ===
    public List<User> getSelectedProjectUsers() {
        return selectedProjectUsers;
    }

    public Map<String, String> getUserRolesInProject() {
        return userRolesInProject;
    }

    public boolean addMember(User user) {
        if (user != null && !isUserAlreadyAdded(user.getUserId())) {
            selectedProjectUsers.add(user);
            userRolesInProject.put(user.getUserId(), "Thành viên");
            return true;
        }
        return false;
    }

    public boolean removeMember(String userId) {
        User userToRemove = null;
        for (User u : selectedProjectUsers) {
            if (u.getUserId().equals(userId)) {
                userToRemove = u;
                break;
            }
        }
        if (userToRemove != null) {
            selectedProjectUsers.remove(userToRemove);
            userRolesInProject.remove(userId);
            return true;
        }
        return false;
    }

    public boolean updateMemberRole(String userId, String newRole) {
        if (userRolesInProject.containsKey(userId)) {
            userRolesInProject.put(userId, newRole);
            return true;
        }
        return false;
    }

    private boolean isUserAlreadyAdded(String userId) {
        if (userId == null) return false;
        for (User u : selectedProjectUsers) {
            if (u.getUserId() != null && u.getUserId().equals(userId)) return true;
        }
        return false;
    }

    // === LINKS MANAGEMENT ===
    public List<LinkItem> getProjectLinks() {
        return projectLinks;
    }

    public boolean addLink(String url, String platform) {
        // Prevent adding more than one of each type
        boolean platformExists = projectLinks.stream().anyMatch(item -> platform.equalsIgnoreCase(item.getPlatform()));
        if (!platformExists) {
            projectLinks.add(new LinkItem(url, platform));
            return true;
        }
        return false;
    }

    public boolean removeLink(int index) {
        if (index >= 0 && index < projectLinks.size()) {
            projectLinks.remove(index);
            return true;
        }
        return false;
    }

    public boolean updateLink(int index, String newUrl, String newPlatform) {
        if (index >= 0 && index < projectLinks.size()) {
            LinkItem item = projectLinks.get(index);
            item.setUrl(newUrl);
            item.setPlatform(newPlatform);
            return true;
        }
        return false;
    }

    public String getProjectUrlFromLinks() {
        for (LinkItem item : projectLinks) {
            if ("GitHub".equalsIgnoreCase(item.getPlatform())) {
                return item.getUrl();
            }
        }
        return null;
    }

    public String getDemoUrlFromLinks() {
        for (LinkItem item : projectLinks) {
            if ("Demo".equalsIgnoreCase(item.getPlatform())) {
                return item.getUrl();
            }
        }
        return null;
    }

    // === MEDIA MANAGEMENT ===
    public List<Uri> getSelectedMediaUris() {
        return selectedMediaUris;
    }
    
    public Uri getProjectImageUri() {
        return projectImageUri;
    }

    public void setProjectImageUri(Uri projectImageUri) {
        this.projectImageUri = projectImageUri;
    }

    public boolean addMedia(Uri uri) {
        if (uri != null && !selectedMediaUris.contains(uri) && selectedMediaUris.size() < 10) {
            selectedMediaUris.add(uri);
            return true;
        }
        return false;
    }

    public boolean removeMedia(int index) {
        if (index >= 0 && index < selectedMediaUris.size()) {
            selectedMediaUris.remove(index);
            return true;
        }
        return false;
    }

    // === FORM VALIDATION ===
    public boolean validateForm(String projectName, String projectDescription, String status) {
        if (TextUtils.isEmpty(projectName)) {
            return false;
        }
        if (TextUtils.isEmpty(projectDescription)) {
            return false;
        }
        if (selectedCategoryNames.isEmpty()) {
            return false;
        }
        if (TextUtils.isEmpty(status)) {
            return false;
        }
        return true;
    }

    public String getValidationError(String projectName, String projectDescription, String status) {
        if (TextUtils.isEmpty(projectName)) {
            return "Vui lòng nhập tên dự án";
        }
        if (TextUtils.isEmpty(projectDescription)) {
            return "Vui lòng nhập mô tả dự án";
        }
        if (selectedCategoryNames.isEmpty()) {
            return "Vui lòng chọn ít nhất một lĩnh vực";
        }
        if (TextUtils.isEmpty(status)) {
            return "Vui lòng chọn trạng thái";
        }
        return null;
    }

    // === UTILITY METHODS ===
    public boolean hasUserMadeChanges() {
        return !selectedCategoryNames.isEmpty() || 
               !selectedTechnologyNames.isEmpty() || 
               !selectedProjectUsers.isEmpty() || 
               !projectLinks.isEmpty() || 
               !selectedMediaUris.isEmpty();
    }

    public void clearForm() {
        selectedCategoryNames.clear();
        selectedTechnologyNames.clear();
        selectedProjectUsers.clear();
        userRolesInProject.clear();
        projectLinks.clear();
        selectedMediaUris.clear();
    }

    public Map<String, Object> collectProjectDataForSave(String projectName, String projectDescription, String status, String thumbnailUrl, List<Map<String, String>> mediaDetails) {
        Map<String, Object> projectData = new HashMap<>();
        projectData.put("Title", projectName);
        projectData.put("Description", projectDescription);
        projectData.put("Status", status);
        projectData.put("CreatedAt", FieldValue.serverTimestamp());
        projectData.put("UpdatedAt", FieldValue.serverTimestamp());
        projectData.put("IsApproved", false); // New projects always need approval
        projectData.put("CreatorUserId", FirebaseAuth.getInstance().getCurrentUser().getUid());
        projectData.put("IsFeatured", false);
        projectData.put("VoteCount", 0);

        if (thumbnailUrl != null) {
            projectData.put("ThumbnailUrl", thumbnailUrl);
        }
        projectData.put("ProjectUrl", getProjectUrlFromLinks());
        projectData.put("DemoUrl", getDemoUrlFromLinks());
        if (mediaDetails != null && !mediaDetails.isEmpty()) {
            projectData.put("MediaGalleryUrls", mediaDetails);
        }
        
        return projectData;
    }

    public Map<String, Object> collectProjectDataForUpdate(String projectName, String projectDescription, String status, Project currentProject, List<Map<String, String>> existingMediaUrls, String newThumbnailUrl, List<Map<String, String>> newMediaDetails) {
        Map<String, Object> updates = new HashMap<>();
        updates.put("Title", projectName);
        updates.put("Description", projectDescription);
        updates.put("Status", status);
        updates.put("UpdatedAt", FieldValue.serverTimestamp());

        // Always require re-approval on edit
        updates.put("IsApproved", false);

        // Handle thumbnail
        if (newThumbnailUrl != null) {
            updates.put("ThumbnailUrl", newThumbnailUrl);
        } else if (currentProject != null) {
            updates.put("ThumbnailUrl", currentProject.getThumbnailUrl());
        }

        // Handle links
        String projectUrl = getProjectUrlFromLinks();
        if (projectUrl != null) {
            updates.put("ProjectUrl", projectUrl);
        } else {
            updates.put("ProjectUrl", FieldValue.delete());
        }

        String demoUrl = getDemoUrlFromLinks();
        if (demoUrl != null) {
            updates.put("DemoUrl", demoUrl);
        } else {
            updates.put("DemoUrl", FieldValue.delete());
        }

        // Handle media gallery
        List<Map<String, String>> finalMediaGallery = new ArrayList<>(existingMediaUrls);
        if (newMediaDetails != null) {
            finalMediaGallery.addAll(newMediaDetails);
        }
        updates.put("MediaGalleryUrls", finalMediaGallery);
        
        return updates;
    }
} 