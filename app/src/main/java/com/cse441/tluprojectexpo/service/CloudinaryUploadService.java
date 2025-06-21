package com.cse441.tluprojectexpo.service;

import android.net.Uri;
import android.util.Log;
import com.cloudinary.android.MediaManager;
import com.cloudinary.android.callback.ErrorInfo;
import com.cloudinary.android.callback.UploadCallback;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Dịch vụ xử lý việc tải file lên Cloudinary.
 * Dịch vụ này cung cấp phương thức để tải ảnh bìa (thumbnail) và nhiều file media (ảnh/video).
 * Nó sử dụng Cloudinary Android SDK và các callback để xử lý kết quả bất đồng bộ.
 */
public class CloudinaryUploadService {

    private static final String TAG = "CloudinaryUploadSvc";

    public interface ThumbnailUploadListener {
        void onThumbnailUploadSuccess(String thumbnailUrl);
        void onThumbnailUploadError(String errorMessage);
    }

    public interface MediaUploadListener {
        void onAllMediaUploaded(List<Map<String, String>> uploadedMediaDetails);
        void onMediaUploadItemSuccess(String url, String resourceType, int currentIndex, int totalCount);
        void onMediaUploadItemError(String errorMessage, Uri erroredUri, int currentIndex, int totalCount);
        void onMediaUploadProgress(int processedCount, int totalCount);
    }

    public void uploadThumbnail(Uri imageUri, String uploadPreset, String folder, ThumbnailUploadListener listener) {
        if (listener == null) {
            Log.e(TAG, "ThumbnailUploadListener không được null. Không thể tiếp tục upload thumbnail.");
            return;
        }
        if (imageUri == null) {
            Log.d(TAG, "imageUri cho thumbnail là null. Trả về thành công với URL null.");
            listener.onThumbnailUploadSuccess(null);
            return;
        }

        Log.d(TAG, "Bắt đầu tải thumbnail. Preset: " + uploadPreset + ", Folder: " + folder + ", URI: " + imageUri.toString());

        MediaManager.get().upload(imageUri)
                .unsigned(uploadPreset)
                .option("folder", folder)
                .callback(new UploadCallback() {
                    @Override
                    public void onStart(String requestId) {
                        Log.d(TAG, "Upload Thumbnail Bắt đầu. Request ID: " + requestId);
                    }

                    @Override
                    public void onProgress(String requestId, long bytes, long totalBytes) {
                        // double progress = (double) bytes / totalBytes;
                        // Log.d(TAG, "Tiến trình Upload Thumbnail: " + String.format("%.2f", progress * 100) + "%");
                    }

                    @Override
                    public void onSuccess(String requestId, Map resultData) {
                        String url = (String) resultData.get("secure_url");
                        if (url == null) {
                            url = (String) resultData.get("url");
                        }
                        Log.i(TAG, "Upload Thumbnail Thành công. Request ID: " + requestId + ". URL: " + url);
                        listener.onThumbnailUploadSuccess(url);
                    }

                    @Override
                    public void onError(String requestId, ErrorInfo error) {
                        // SỬA Ở ĐÂY: Xóa .getException()
                        Log.e(TAG, "Lỗi Upload Thumbnail. Request ID: " + requestId + ". Code: " + error.getCode() + ", Lỗi: " + error.getDescription());
                        listener.onThumbnailUploadError("Lỗi tải ảnh bìa: " + error.getDescription() + " (Code: " + error.getCode() + ")");
                    }

                    @Override
                    public void onReschedule(String requestId, ErrorInfo error) {
                        Log.w(TAG, "Upload Thumbnail Được Lên Lịch Lại. Request ID: " + requestId + ". Lý do: " + error.getDescription());
                        listener.onThumbnailUploadError("Tải ảnh bìa bị hoãn: " + error.getDescription());
                    }
                }).dispatch();
    }

    public void uploadMultipleMedia(List<Uri> mediaUris, String uploadPreset, String folder, MediaUploadListener listener) {
        if (listener == null) {
            Log.e(TAG, "MediaUploadListener không được null. Không thể tiếp tục upload media.");
            return;
        }
        if (mediaUris == null || mediaUris.isEmpty()) {
            Log.d(TAG, "Danh sách mediaUris rỗng. Trả về onAllMediaUploaded với danh sách rỗng.");
            listener.onAllMediaUploaded(new ArrayList<>());
            return;
        }

        final List<Map<String, String>> uploadedMediaDetailsList = new ArrayList<>();
        final AtomicInteger processedCounter = new AtomicInteger(0);
        final int totalFiles = mediaUris.size();

        listener.onMediaUploadProgress(0, totalFiles);
        Log.d(TAG, "Bắt đầu tải " + totalFiles + " file media. Preset: " + uploadPreset + ", Folder: " + folder);

        for (int i = 0; i < totalFiles; i++) {
            final Uri uri = mediaUris.get(i);
            final int currentIndex = i;

            if (uri == null) {
                Log.w(TAG, "Media URI tại index " + currentIndex + " là null. Bỏ qua item này.");
                listener.onMediaUploadItemError("URI của media là null", uri, currentIndex, totalFiles); // uri ở đây sẽ là null
                checkIfAllMediaProcessed(processedCounter, totalFiles, uploadedMediaDetailsList, listener);
                continue;
            }

            Log.d(TAG, "Chuẩn bị upload media item: " + (currentIndex + 1) + "/" + totalFiles + ", URI: " + uri.toString());

            MediaManager.get().upload(uri)
                    .unsigned(uploadPreset)
                    .option("folder", folder)
                    .option("resource_type", "auto")
                    .callback(new UploadCallback() {
                        @Override
                        public void onStart(String requestId) {
                            Log.d(TAG, "Upload Media Bắt đầu (" + (currentIndex + 1) + "/" + totalFiles + "). File: " + uri.getLastPathSegment() + ", Request ID: " + requestId);
                        }

                        @Override
                        public void onProgress(String requestId, long bytes, long totalBytes) {
                        }

                        @Override
                        public void onSuccess(String requestId, Map resultData) {
                            String url = (String) resultData.get("secure_url");
                            if (url == null) {
                                url = (String) resultData.get("url");
                            }
                            String resourceType = (String) resultData.get("resource_type");

                            if (url != null && resourceType != null) {
                                Map<String, String> mediaDetail = new HashMap<>();
                                mediaDetail.put("url", url);
                                if ("video".equals(resourceType)) {
                                    mediaDetail.put("type", "video");
                                } else if ("image".equals(resourceType)) {
                                    mediaDetail.put("type", "image");
                                } else {
                                    mediaDetail.put("type", "other");
                                    Log.w(TAG, "Media item có resource_type không xác định là image/video: " + resourceType + ". URL: " + url);
                                }
                                uploadedMediaDetailsList.add(mediaDetail);
                                Log.i(TAG, "Upload Media Thành công (" + (currentIndex + 1) + "/" + totalFiles + "). Request ID: " + requestId + ". URL: " + url + ", Type: " + resourceType);
                                listener.onMediaUploadItemSuccess(url, resourceType, currentIndex, totalFiles);
                            } else {
                                String errorMsg = "URL" + (url == null ? " " : " (có giá trị) ") + "hoặc resourceType" + (resourceType == null ? " " : " (có giá trị "+resourceType+") ") + "trả về null từ Cloudinary.";
                                Log.w(TAG, "Upload Media Thành công nhưng " + errorMsg + " (" + (currentIndex + 1) + "/" + totalFiles + "). File: " + uri.getLastPathSegment());
                                listener.onMediaUploadItemError(errorMsg, uri, currentIndex, totalFiles);
                            }
                            checkIfAllMediaProcessed(processedCounter, totalFiles, uploadedMediaDetailsList, listener);
                        }

                        @Override
                        public void onError(String requestId, ErrorInfo error) {
                            // SỬA Ở ĐÂY: Xóa .getException()
                            Log.e(TAG, "Lỗi Upload Media (" + (currentIndex + 1) + "/" + totalFiles + "). File: " + uri.getLastPathSegment() + ", Request ID: " + requestId + ". Code: " + error.getCode() + ", Lỗi: " + error.getDescription());
                            listener.onMediaUploadItemError(error.getDescription() + " (Code: " + error.getCode() + ")", uri, currentIndex, totalFiles);
                            checkIfAllMediaProcessed(processedCounter, totalFiles, uploadedMediaDetailsList, listener);
                        }

                        @Override
                        public void onReschedule(String requestId, ErrorInfo error) {
                            Log.w(TAG, "Upload Media Được Lên Lịch Lại (" + (currentIndex + 1) + "/" + totalFiles + "). File: " + uri.getLastPathSegment() + ", Request ID: " + requestId + ". Lý do: " + error.getDescription());
                            listener.onMediaUploadItemError("Tải media bị hoãn: " + error.getDescription(), uri, currentIndex, totalFiles);
                            checkIfAllMediaProcessed(processedCounter, totalFiles, uploadedMediaDetailsList, listener);
                        }
                    }).dispatch();
        }
    }

    private void checkIfAllMediaProcessed(AtomicInteger processedCounter, int totalFiles, List<Map<String, String>> uploadedDetails, MediaUploadListener listener) {
        int processed = processedCounter.incrementAndGet();
        listener.onMediaUploadProgress(processed, totalFiles);
        if (processed == totalFiles) {
            Log.i(TAG, "Tất cả " + totalFiles + " file media đã được xử lý. Số lượng upload thành công: " + uploadedDetails.size());
            listener.onAllMediaUploaded(uploadedDetails);
        }
    }
}