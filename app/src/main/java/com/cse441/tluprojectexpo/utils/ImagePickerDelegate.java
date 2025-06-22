// Xử lý việc mở trình chọn ảnh/media.
package com.cse441.tluprojectexpo.utils;


import android.content.Intent;
import android.provider.MediaStore;
import androidx.activity.result.ActivityResultLauncher;

/**
 * Lớp ủy quyền xử lý việc mở trình chọn ảnh/media từ thiết bị.
 */
public class ImagePickerDelegate {

    private ActivityResultLauncher<Intent> projectImagePickerLauncher; // Launcher cho ảnh bìa dự án
    private ActivityResultLauncher<Intent> mediaPickerLauncher;        // Launcher cho các media khác (ảnh/video)

    /**
     * Constructor cho ImagePickerDelegate.
     * @param projectImagePickerLauncher Launcher đã đăng ký để chọn ảnh bìa.
     * @param mediaPickerLauncher Launcher đã đăng ký để chọn các media khác.
     */
    public ImagePickerDelegate(ActivityResultLauncher<Intent> projectImagePickerLauncher,
                               ActivityResultLauncher<Intent> mediaPickerLauncher) {
        this.projectImagePickerLauncher = projectImagePickerLauncher;
        this.mediaPickerLauncher = mediaPickerLauncher;
    }

    /**
     * Mở trình chọn ảnh để chọn ảnh bìa cho dự án.
     * Chỉ cho phép chọn ảnh.
     */
    public void launchProjectImagePicker() {
        Intent intent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
        intent.setType("image/*"); // Chỉ cho phép chọn file ảnh
        projectImagePickerLauncher.launch(intent);
    }

    /**
     * Mở trình chọn file để chọn các media (ảnh hoặc video).
     */
    public void launchMediaPicker() {
        Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
        intent.setType("*/*"); // Cho phép chọn bất kỳ loại file nào ban đầu
        // Chỉ định cụ thể các loại MIME được phép (ảnh và video)
        intent.putExtra(Intent.EXTRA_MIME_TYPES, new String[]{"image/*", "video/*"});
        // intent.putExtra(Intent.EXTRA_ALLOW_MULTIPLE, true); // Bỏ comment nếu muốn cho phép chọn nhiều file cùng lúc
        mediaPickerLauncher.launch(intent);
    }
}
