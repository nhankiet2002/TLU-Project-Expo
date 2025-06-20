package com.cse441.tluprojectexpo.util;

import android.Manifest;
import android.content.pm.PackageManager;
import android.os.Build;
import androidx.activity.result.ActivityResultLauncher;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import java.util.ArrayList;
import java.util.List;

/**
 * Lớp trợ giúp quản lý việc kiểm tra và yêu cầu quyền truy cập.
 */
public class PermissionManager {

    private Fragment fragment; // Fragment đang yêu cầu quyền
    private ActivityResultLauncher<String[]> permissionLauncher; // Launcher để thực hiện yêu cầu quyền

    /**
     * Constructor cho PermissionManager.
     * @param fragment Fragment hiện tại.
     * @param permissionLauncher ActivityResultLauncher đã được đăng ký để xử lý kết quả yêu cầu quyền.
     */
    public PermissionManager(Fragment fragment, ActivityResultLauncher<String[]> permissionLauncher) {
        this.fragment = fragment;
        this.permissionLauncher = permissionLauncher;
    }

    /**
     * Kiểm tra các quyền cần thiết cho việc đọc media. Nếu chưa được cấp, sẽ yêu cầu quyền.
     * @return true nếu tất cả quyền đã được cấp, false nếu đang yêu cầu hoặc bị từ chối.
     */
    public boolean checkAndRequestStoragePermissions() {
        // Kiểm tra context của fragment có tồn tại không
        if (fragment.getContext() == null) {
            return false;
        }

        // Xác định các quyền cần thiết dựa trên phiên bản Android
        String[] permissionsToRequest;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) { // Android 13 trở lên
            permissionsToRequest = new String[]{
                    Manifest.permission.READ_MEDIA_IMAGES, // Quyền đọc ảnh
                    Manifest.permission.READ_MEDIA_VIDEO   // Quyền đọc video
            };
        } else { // Android cũ hơn
            permissionsToRequest = new String[]{Manifest.permission.READ_EXTERNAL_STORAGE}; // Quyền đọc bộ nhớ ngoài
        }

        // Tạo danh sách các quyền chưa được cấp
        List<String> permissionsNeeded = new ArrayList<>();
        for (String permission : permissionsToRequest) {
            if (ContextCompat.checkSelfPermission(fragment.requireContext(), permission) != PackageManager.PERMISSION_GRANTED) {
                permissionsNeeded.add(permission);
            }
        }

        // Nếu có quyền chưa được cấp, yêu cầu chúng
        if (!permissionsNeeded.isEmpty()) {
            permissionLauncher.launch(permissionsNeeded.toArray(new String[0]));
            return false; // Đang trong quá trình yêu cầu, chưa có quyền ngay
        }
        return true; // Tất cả quyền đã được cấp
    }
}
