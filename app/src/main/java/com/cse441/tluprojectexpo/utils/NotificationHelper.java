package com.cse441.tluprojectexpo.utils;

import android.Manifest;
import android.app.Activity;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.provider.Settings;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AlertDialog;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.cse441.tluprojectexpo.R;

public class NotificationHelper {
    private static final String CHANNEL_ID = "TLUProjectExpo_Channel";
    private static final String CHANNEL_NAME = "TLU Project Expo Notifications";
    private static final String CHANNEL_DESCRIPTION = "Notifications for TLU Project Expo app";

    /**
     * Kiểm tra và yêu cầu quyền notification
     */
    public static void checkAndRequestNotificationPermission(Activity activity, ActivityResultLauncher<String> permissionLauncher) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(activity, Manifest.permission.POST_NOTIFICATIONS) 
                    != PackageManager.PERMISSION_GRANTED) {
                
                // Kiểm tra xem có nên hiển thị explanation không
                if (ActivityCompat.shouldShowRequestPermissionRationale(activity, Manifest.permission.POST_NOTIFICATIONS)) {
                    showNotificationPermissionDialog(activity, permissionLauncher);
                } else {
                    // Yêu cầu quyền trực tiếp
                    permissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS);
                }
            }
        }
    }

    /**
     * Hiển thị dialog giải thích về quyền notification
     */
    private static void showNotificationPermissionDialog(Activity activity, ActivityResultLauncher<String> permissionLauncher) {
        new AlertDialog.Builder(activity)
                .setTitle("Quyền thông báo")
                .setMessage("Ứng dụng cần quyền thông báo để gửi cho bạn những thông tin quan trọng về dự án, bình luận và lời mời tham gia.")
                .setPositiveButton("Cấp quyền", (dialog, which) -> {
                    permissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS);
                })
                .setNegativeButton("Hủy", (dialog, which) -> {
                    dialog.dismiss();
                })
                .setNeutralButton("Cài đặt", (dialog, which) -> {
                    openAppSettings(activity);
                })
                .show();
    }

    /**
     * Mở cài đặt ứng dụng
     */
    private static void openAppSettings(Context context) {
        Intent intent = new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
        Uri uri = Uri.fromParts("package", context.getPackageName(), null);
        intent.setData(uri);
        context.startActivity(intent);
    }

    /**
     * Tạo notification channel
     */
    public static void createNotificationChannel(Context context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                    CHANNEL_ID,
                    CHANNEL_NAME,
                    NotificationManager.IMPORTANCE_HIGH // HIGH để hiển thị heads up
            );
            channel.setDescription(CHANNEL_DESCRIPTION);
            channel.enableLights(true);
            channel.enableVibration(true);
            channel.setShowBadge(true);

            NotificationManager notificationManager = context.getSystemService(NotificationManager.class);
            if (notificationManager != null) {
                notificationManager.createNotificationChannel(channel);
            }
        }
    }

    /**
     * Kiểm tra xem notification channel đã được tạo chưa
     */
    public static boolean isNotificationChannelCreated(Context context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationManager notificationManager = context.getSystemService(NotificationManager.class);
            if (notificationManager != null) {
                return notificationManager.getNotificationChannel(CHANNEL_ID) != null;
            }
        }
        return true; // Với Android < 8.0, luôn return true
    }

    /**
     * Lấy CHANNEL_ID để sử dụng trong NotificationCompat.Builder
     */
    public static String getChannelId() {
        return CHANNEL_ID;
    }
} 