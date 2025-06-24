package com.cse441.tluprojectexpo.repository;

import android.content.Context;
import android.net.Uri;
import android.util.Log;

import com.cse441.tluprojectexpo.model.Project;
import com.cse441.tluprojectexpo.model.User;
import com.cse441.tluprojectexpo.service.CloudinaryUploadService;
import com.cse441.tluprojectexpo.service.FirestoreService;
import com.cse441.tluprojectexpo.utils.Constants;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Quản lý việc lưu dự án chung cho cả Create và Edit Project
 */
public class ProjectSaveManager {
    private static final String TAG = "ProjectSaveManager";

    private final Context context;
    private final FirestoreService firestoreService;
    private final CloudinaryUploadService cloudinaryUploadService;
    private final ProjectFormManager formManager;

    public ProjectSaveManager(Context context, ProjectFormManager formManager) {
        this.context = context;
        this.formManager = formManager;
        this.firestoreService = new FirestoreService();
        this.cloudinaryUploadService = new CloudinaryUploadService();
    }

    public interface ProjectSaveListener {
        void onSuccess(String projectId, boolean wasPreviouslyApproved);
        void onError(String errorMessage);
        void onProgress(String message);
    }

    /**
     * Lưu dự án mới
     */
    public void saveNewProject(String projectName, String projectDescription, String status, ProjectSaveListener listener) {
        Log.d(TAG, "Starting to save new project: " + projectName);
        listener.onProgress("Đang chuẩn bị để lưu...");

        Uri newThumbnailUri = formManager.getProjectImageUri();
        List<Uri> newMediaUris = formManager.getSelectedMediaUris();

        uploadAllMedia(newThumbnailUri, newMediaUris, listener, (thumbnailUrl, mediaUrls) -> {
            saveProjectData(projectName, projectDescription, status, thumbnailUrl, mediaUrls, listener);
        });
    }

    /**
     * Cập nhật dự án hiện có
     */
    public void updateProject(String projectId, String projectName, String projectDescription, String status,
                              Project currentProject, List<Map<String, String>> existingMediaUrls,
                              List<Uri> newMediaUris, ProjectSaveListener listener) {

        Log.d(TAG, "Starting to update project: " + projectId);
        listener.onProgress("Đang chuẩn bị để cập nhật...");

        Uri newThumbnailUri = formManager.getProjectImageUri();

        uploadAllMedia(newThumbnailUri, newMediaUris, listener, (thumbnailUrl, newUploadedUrls) -> {
            updateProjectData(projectId, projectName, projectDescription, status, currentProject, existingMediaUrls, newUploadedUrls, thumbnailUrl, listener);
        });
    }

    private void uploadAllMedia(Uri newThumbnailUri, List<Uri> newMediaUris, ProjectSaveListener listener, MediaUploadCallback onComplete) {
        AtomicReference<String> uploadedThumbnailUrl = new AtomicReference<>(null);
        List<Map<String, String>> uploadedMediaUrls = new ArrayList<>();

        boolean shouldUploadThumbnail = newThumbnailUri != null;
        boolean shouldUploadMedia = newMediaUris != null && !newMediaUris.isEmpty();

        if (!shouldUploadThumbnail && !shouldUploadMedia) {
            onComplete.onUploadsFinished(null, new ArrayList<>());
            return;
        }

        AtomicInteger uploadsToFinish = new AtomicInteger((shouldUploadThumbnail ? 1 : 0) + (shouldUploadMedia ? 1 : 0));

        if (shouldUploadThumbnail) {
            listener.onProgress("Đang tải ảnh bìa...");
            cloudinaryUploadService.uploadThumbnail(newThumbnailUri, Constants.CLOUDINARY_UPLOAD_PRESET_THUMBNAIL, Constants.CLOUDINARY_FOLDER_PROJECT_THUMBNAILS, new CloudinaryUploadService.ThumbnailUploadListener() {
                @Override
                public void onThumbnailUploadSuccess(String thumbnailUrl) {
                    uploadedThumbnailUrl.set(thumbnailUrl);
                    if (uploadsToFinish.decrementAndGet() == 0) {
                        onComplete.onUploadsFinished(uploadedThumbnailUrl.get(), uploadedMediaUrls);
                    }
                }

                @Override
                public void onThumbnailUploadError(String errorMessage) {
                    listener.onError("Lỗi tải ảnh bìa: " + errorMessage);
                }
            });
        }

        if (shouldUploadMedia) {
            listener.onProgress("Đang tải media (0/" + newMediaUris.size() + ")");
            cloudinaryUploadService.uploadMultipleMedia(newMediaUris, Constants.CLOUDINARY_UPLOAD_PRESET_MEDIA, Constants.CLOUDINARY_FOLDER_PROJECT_MEDIA, new CloudinaryUploadService.MediaUploadListener() {
                @Override
                public void onAllMediaUploaded(List<Map<String, String>> uploadedMediaDetails) {
                    uploadedMediaUrls.addAll(uploadedMediaDetails);
                    if (uploadsToFinish.decrementAndGet() == 0) {
                        onComplete.onUploadsFinished(uploadedThumbnailUrl.get(), uploadedMediaUrls);
                    }
                }

                @Override
                public void onMediaUploadItemSuccess(String url, String resourceType, int currentIndex, int totalCount) {}

                @Override
                public void onMediaUploadItemError(String errorMessage, Uri erroredUri, int currentIndex, int totalCount) {
                     Log.e(TAG, "Media upload error for " + erroredUri.toString() + ": " + errorMessage);
                }

                @Override
                public void onMediaUploadProgress(int processedCount, int totalCount) {
                    listener.onProgress("Đang tải media (" + processedCount + "/" + totalCount + ")");
                }
            });
        }
    }

    private void saveProjectData(String projectName, String projectDescription, String status, String thumbnailUrl, List<Map<String, String>> mediaDetails, ProjectSaveListener listener) {
        listener.onProgress("Đang lưu dự án...");
        Map<String, Object> projectData = formManager.collectProjectDataForSave(projectName, projectDescription, status, thumbnailUrl, mediaDetails);

        firestoreService.createProject(projectData, new FirestoreService.ProjectCreationListener() {
            @Override
            public void onProjectCreated(String newProjectId) {
                Log.d(TAG, "Project created with ID: " + newProjectId);
                updateProjectRelations(newProjectId, () -> listener.onSuccess(newProjectId, false));
            }

            @Override
            public void onError(String errorMessage) {
                Log.e(TAG, "Project creation error: " + errorMessage);
                listener.onError("Lỗi tạo dự án: " + errorMessage);
            }
        });
    }

    private void updateProjectData(String projectId, String projectName, String projectDescription, String status, Project currentProject, List<Map<String, String>> existingMediaUrls, List<Map<String, String>> newMediaUrls, String newThumbnailUrl, ProjectSaveListener listener) {
        listener.onProgress("Đang cập nhật dự án...");
        final boolean wasApproved = currentProject.getIsApproved();
        Map<String, Object> projectUpdates = formManager.collectProjectDataForUpdate(projectName, projectDescription, status, currentProject, existingMediaUrls, newThumbnailUrl, newMediaUrls);

        firestoreService.updateProject(projectId, projectUpdates, new FirestoreService.ProjectUpdateListener() {
            @Override
            public void onProjectUpdated() {
                Log.d(TAG, "Project updated successfully: " + projectId);
                updateProjectRelations(projectId, () -> listener.onSuccess(projectId, wasApproved));
            }

            @Override
            public void onError(String errorMessage) {
                Log.e(TAG, "Project update error: " + errorMessage);
                listener.onError("Lỗi cập nhật dự án: " + errorMessage);
            }
        });
    }

    private void updateProjectRelations(String projectId, Runnable onComplete) {
        AtomicInteger relationsToUpdate = new AtomicInteger(3); // Categories, Technologies, Members

        Runnable decrementAndCheck = () -> {
            if (relationsToUpdate.decrementAndGet() == 0) {
                onComplete.run();
            }
        };

        firestoreService.updateProjectCategories(projectId, formManager.getSelectedCategoryIds(), new FirestoreService.UpdateCallback() {
            @Override public void onSuccess() { Log.d(TAG, "Categories updated for " + projectId); decrementAndCheck.run(); }
            @Override public void onError(String errorMessage) { Log.e(TAG, "Error updating categories: " + errorMessage); decrementAndCheck.run(); }
        });

        firestoreService.updateProjectTechnologies(projectId, formManager.getSelectedTechnologyIds(), new FirestoreService.UpdateCallback() {
            @Override public void onSuccess() { Log.d(TAG, "Technologies updated for " + projectId); decrementAndCheck.run(); }
            @Override public void onError(String errorMessage) { Log.e(TAG, "Error updating technologies: " + errorMessage); decrementAndCheck.run(); }
        });

        firestoreService.updateProjectMembers(projectId, formManager.getSelectedProjectUsers(), formManager.getUserRolesInProject(), new FirestoreService.UpdateCallback() {
            @Override public void onSuccess() { Log.d(TAG, "Members updated for " + projectId); decrementAndCheck.run(); }
            @Override public void onError(String errorMessage) { Log.e(TAG, "Error updating members: " + errorMessage); decrementAndCheck.run(); }
        });
    }

    private interface MediaUploadCallback {
        void onUploadsFinished(String thumbnailUrl, List<Map<String, String>> mediaUrls);
    }
} 