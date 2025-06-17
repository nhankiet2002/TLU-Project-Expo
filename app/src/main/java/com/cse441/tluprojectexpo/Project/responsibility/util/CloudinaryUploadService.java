package com.cse441.tluprojectexpo.Project.responsibility.util;

import android.net.Uri;
import android.util.Log;
import com.cloudinary.android.MediaManager;
import com.cloudinary.android.callback.ErrorInfo;
import com.cloudinary.android.callback.UploadCallback;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Dịch vụ xử lý việc tải file lên Cloudinary.
 */
public class CloudinaryUploadService {

    private static final String TAG = "CloudinaryUploadSvc"; // Thay đổi TAG để phân biệt

    /**
     * Listener cho kết quả upload ảnh bìa.
     */
    public interface ThumbnailUploadListener {
        void onThumbnailUploadSuccess(String thumbnailUrl);
        void onThumbnailUploadError(String errorMessage);
    }

    /**
     * Listener cho kết quả upload nhiều file media.
     */
    public interface MediaUploadListener {
        void onAllMediaUploaded(List<String> uploadedUrls); // Tất cả đã xử lý (kể cả lỗi)
        void onMediaUploadItemSuccess(String url, int currentIndex, int totalCount); // Một item thành công
        void onMediaUploadItemError(String errorMessage, Uri erroredUri, int currentIndex, int totalCount); // Một item lỗi
        void onMediaUploadProgress(int processedCount, int totalCount); // Cập nhật tiến trình chung
    }

    /**
     * Tải ảnh bìa lên Cloudinary.
     * @param imageUri URI của ảnh cần tải.
     * @param uploadPreset Tên của unsigned upload preset trên Cloudinary.
     * @param folder Tên thư mục trên Cloudinary (tùy chọn).
     * @param listener Callback để nhận kết quả.
     */
    public void uploadThumbnail(Uri imageUri, String uploadPreset, String folder, ThumbnailUploadListener listener) {
        if (listener == null) {
            Log.e(TAG, "ThumbnailUploadListener cannot be null.");
            return;
        }
        if (imageUri == null) {
            listener.onThumbnailUploadSuccess(null); // Không có ảnh, trả về null
            return;
        }

        // Log upload preset đang sử dụng
        Log.d(TAG, "Uploading thumbnail with preset: " + uploadPreset + " to folder: " + folder);

        MediaManager.get().upload(imageUri)
                .unsigned(uploadPreset)
                .option("folder", folder)
                .callback(new UploadCallback() {
                    @Override public void onStart(String requestId) { Log.d(TAG, "Thumbnail Upload Started: " + requestId); }
                    @Override public void onProgress(String rId, long b, long tb) { /* Có thể thêm log tiến trình */ }
                    @Override public void onSuccess(String requestId, Map resultData) {
                        String url = (String) resultData.get("secure_url");
                        if (url == null) url = (String) resultData.get("url");
                        Log.d(TAG, "Thumbnail Upload Success: " + url);
                        listener.onThumbnailUploadSuccess(url);
                    }
                    @Override public void onError(String requestId, ErrorInfo error) {
                        Log.e(TAG, "Thumbnail Upload Error: " + error.getDescription());
                        listener.onThumbnailUploadError("Lỗi tải ảnh bìa: " + error.getDescription());
                    }
                    @Override public void onReschedule(String r, ErrorInfo e) {
                        Log.w(TAG, "Thumbnail Upload Rescheduled: " + e.getDescription());
                        listener.onThumbnailUploadError("Tải ảnh bìa bị hoãn: " + e.getDescription());
                    }
                }).dispatch();
    }

    /**
     * Tải nhiều file media (ảnh/video) lên Cloudinary.
     * @param mediaUris Danh sách URI của các file media.
     * @param uploadPreset Tên của unsigned upload preset.
     * @param folder Tên thư mục trên Cloudinary.
     * @param listener Callback để nhận kết quả.
     */
    public void uploadMultipleMedia(List<Uri> mediaUris, String uploadPreset, String folder, MediaUploadListener listener) {
        if (listener == null) {
            Log.e(TAG, "MediaUploadListener cannot be null.");
            return;
        }
        if (mediaUris == null || mediaUris.isEmpty()) {
            listener.onAllMediaUploaded(new ArrayList<>()); // Không có media, trả về list rỗng
            return;
        }

        final List<String> uploadedUrls = new ArrayList<>();
        final AtomicInteger processedCounter = new AtomicInteger(0); // Đếm số file đã được xử lý (cả thành công và lỗi)
        final int totalFiles = mediaUris.size();

        // Thông báo tiến trình ban đầu
        listener.onMediaUploadProgress(0, totalFiles);

        // Log upload preset đang sử dụng
        Log.d(TAG, "Uploading " + totalFiles + " media files with preset: " + uploadPreset + " to folder: " + folder);


        for (int i = 0; i < totalFiles; i++) {
            final Uri uri = mediaUris.get(i);
            final int currentIndex = i; // Index của file hiện tại (0-based)

            MediaManager.get().upload(uri)
                    .unsigned(uploadPreset)
                    .option("folder", folder)
                    .option("resource_type", "auto") // Cloudinary tự phát hiện type (image/video)
                    .callback(new UploadCallback() {
                        @Override public void onStart(String rId) { Log.d(TAG, "Media Upload Start (" + (currentIndex + 1) + "/" + totalFiles + "): " + uri.getLastPathSegment());}
                        @Override public void onProgress(String r, long b, long tB) {}
                        @Override public void onSuccess(String rId, Map resultData) {
                            String url = (String) resultData.get("secure_url");
                            if (url == null) url = (String) resultData.get("url");
                            if (url != null) {
                                uploadedUrls.add(url);
                                Log.d(TAG, "Media Upload Success (" + (currentIndex + 1) + "/" + totalFiles + "): " + url);
                                listener.onMediaUploadItemSuccess(url, currentIndex, totalFiles);
                            } else {
                                Log.w(TAG, "Media Upload Success but URL is null (" + (currentIndex + 1) + "/" + totalFiles + "): " + uri.getLastPathSegment());
                                // Vẫn coi như xử lý xong nhưng không có URL
                                listener.onMediaUploadItemError("URL trả về null", uri, currentIndex, totalFiles);
                            }
                            checkIfAllMediaProcessed(processedCounter, totalFiles, uploadedUrls, listener);
                        }
                        @Override public void onError(String rId, ErrorInfo error) {
                            Log.e(TAG, "Media Upload Error (" + (currentIndex + 1) + "/" + totalFiles + "): " + uri.getLastPathSegment() + " - " + error.getDescription());
                            listener.onMediaUploadItemError(error.getDescription(), uri, currentIndex, totalFiles);
                            checkIfAllMediaProcessed(processedCounter, totalFiles, uploadedUrls, listener);
                        }
                        @Override public void onReschedule(String r, ErrorInfo e) {
                            Log.w(TAG, "Media Upload Rescheduled (" + (currentIndex + 1) + "/" + totalFiles + "): " + uri.getLastPathSegment() + " - " + e.getDescription());
                            listener.onMediaUploadItemError("Tải media bị hoãn: " + e.getDescription(), uri, currentIndex, totalFiles);
                            checkIfAllMediaProcessed(processedCounter, totalFiles, uploadedUrls, listener);
                        }
                    }).dispatch();
        }
    }

    private void checkIfAllMediaProcessed(AtomicInteger processedCounter, int totalFiles, List<String> uploadedUrls, MediaUploadListener listener) {
        int processed = processedCounter.incrementAndGet();
        listener.onMediaUploadProgress(processed, totalFiles); // Cập nhật tiến trình tổng thể
        if (processed == totalFiles) {
            Log.d(TAG, "All media files processed. Total successful uploads: " + uploadedUrls.size() + "/" + totalFiles);
            listener.onAllMediaUploaded(uploadedUrls);
        }
    }
}
