// PermissionManager.java
package com.cse441.tluprojectexpo.utils; // Hoặc package đúng của bạn

import android.Manifest;
import android.app.Activity; // THÊM
import android.content.Context; // THÊM
import android.content.pm.PackageManager;
import android.os.Build;
import android.util.Log;

import androidx.activity.result.ActivityResultLauncher;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import java.util.ArrayList;
import java.util.List;

public class PermissionManager {

    private Fragment fragment; // Có thể là null nếu dùng với Activity
    private Activity activity; // Có thể là null nếu dùng với Fragment
    private ActivityResultLauncher<String[]> permissionLauncher;

    /**
     * Constructor cho PermissionManager khi sử dụng với Fragment.
     * @param fragment Fragment hiện tại.
     * @param permissionLauncher ActivityResultLauncher.
     */
    public PermissionManager(Fragment fragment, ActivityResultLauncher<String[]> permissionLauncher) {
        this.fragment = fragment;
        this.activity = null; // Đảm bảo activity là null
        this.permissionLauncher = permissionLauncher;
    }

    /**
     * Constructor cho PermissionManager khi sử dụng với Activity.
     * @param activity Activity hiện tại.
     * @param permissionLauncher ActivityResultLauncher.
     */
    public PermissionManager(Activity activity, ActivityResultLauncher<String[]> permissionLauncher) {
        this.activity = activity;
        this.fragment = null; // Đảm bảo fragment là null
        this.permissionLauncher = permissionLauncher;
    }

    private Context getContext() {
        if (fragment != null) {
            return fragment.requireContext();
        } else if (activity != null) {
            return activity;
        }
        return null; // Trường hợp không nên xảy ra
    }


    public boolean checkAndRequestStoragePermissions() {
        Context context = getContext();
        if (context == null) {
            Log.e("PermissionManager", "Context is null in checkAndRequestStoragePermissions");
            return false;
        }

        String[] permissionsToRequest;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            permissionsToRequest = new String[]{
                    Manifest.permission.READ_MEDIA_IMAGES,
                    Manifest.permission.READ_MEDIA_VIDEO
            };
        } else {
            permissionsToRequest = new String[]{Manifest.permission.READ_EXTERNAL_STORAGE};
        }

        List<String> permissionsNeeded = new ArrayList<>();
        for (String permission : permissionsToRequest) {
            if (ContextCompat.checkSelfPermission(context, permission) != PackageManager.PERMISSION_GRANTED) {
                permissionsNeeded.add(permission);
            }
        }

        if (!permissionsNeeded.isEmpty()) {
            // Không cần gọi shouldShowRequestPermissionRationale ở đây vì ActivityResultLauncher sẽ xử lý
            // việc hiển thị dialog yêu cầu quyền chuẩn.
            // Việc hiển thị giải thích tùy chỉnh nên được thực hiện trước khi gọi hàm này nếu cần.
            permissionLauncher.launch(permissionsNeeded.toArray(new String[0]));
            return false;
        }
        return true;
    }
}