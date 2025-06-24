package com.cse441.tluprojectexpo.service;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Build;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.core.app.NotificationCompat;

import com.cse441.tluprojectexpo.MainActivity;
import com.cse441.tluprojectexpo.R;
import com.cse441.tluprojectexpo.ui.detailproject.ProjectDetailActivity;
import com.google.firebase.messaging.FirebaseMessagingService;
import com.google.firebase.messaging.RemoteMessage;

import java.util.Map;

public class NotificationService extends FirebaseMessagingService {
    private static final String TAG = "NotificationService";
    private static final String CHANNEL_ID = "TLUProjectExpo_Channel";
    private static final String CHANNEL_NAME = "TLU Project Expo Notifications";
    private static final String CHANNEL_DESCRIPTION = "Notifications for TLU Project Expo app";

    @Override
    public void onNewToken(@NonNull String token) {
        super.onNewToken(token);
        Log.d(TAG, "New FCM token: " + token);
        // TODO: Gửi token lên server để lưu trữ
        sendTokenToServer(token);
    }

    @Override
    public void onMessageReceived(@NonNull RemoteMessage remoteMessage) {
        super.onMessageReceived(remoteMessage);
        Log.d(TAG, "From: " + remoteMessage.getFrom());

        // Kiểm tra xem message có data không
        if (remoteMessage.getData().size() > 0) {
            Log.d(TAG, "Message data payload: " + remoteMessage.getData());
            handleDataMessage(remoteMessage.getData());
        }

        // Kiểm tra xem message có notification payload không
        if (remoteMessage.getNotification() != null) {
            Log.d(TAG, "Message Notification Body: " + remoteMessage.getNotification().getBody());
            handleNotificationMessage(remoteMessage.getNotification(), remoteMessage.getData());
        }
    }

    private void handleDataMessage(Map<String, String> data) {
        String title = data.get("title");
        String message = data.get("message");
        String type = data.get("type");
        String projectId = data.get("projectId");
        String commentId = data.get("commentId");

        // Tạo notification với heads up
        createHeadsUpNotification(title, message, type, projectId, commentId);
    }

    private void handleNotificationMessage(RemoteMessage.Notification notification, Map<String, String> data) {
        String title = notification.getTitle();
        String message = notification.getBody();
        String type = data.get("type");
        String projectId = data.get("projectId");
        String commentId = data.get("commentId");

        // Tạo notification với heads up
        createHeadsUpNotification(title, message, type, projectId, commentId);
    }

    private void createHeadsUpNotification(String title, String message, String type, String projectId, String commentId) {
        // Tạo notification channel cho Android 8.0+
        createNotificationChannel();

        // Tạo intent để mở activity khi click vào notification
        Intent intent;
        if (projectId != null && !projectId.isEmpty()) {
            // Mở ProjectDetailActivity nếu có projectId
            intent = new Intent(this, ProjectDetailActivity.class);
            intent.putExtra("projectId", projectId);
            if (commentId != null && !commentId.isEmpty()) {
                intent.putExtra("commentId", commentId);
            }
        } else {
            // Mở MainActivity nếu không có projectId
            intent = new Intent(this, MainActivity.class);
            intent.putExtra("openNotifications", true);
        }

        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
        PendingIntent pendingIntent = PendingIntent.getActivity(this, 0, intent,
                PendingIntent.FLAG_ONE_SHOT | PendingIntent.FLAG_IMMUTABLE);

        // Tạo notification sound
        Uri defaultSoundUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION);

        // Tạo notification với heads up
        NotificationCompat.Builder notificationBuilder = new NotificationCompat.Builder(this, CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_bell)
                .setContentTitle(title != null ? title : "TLU Project Expo")
                .setContentText(message)
                .setAutoCancel(true)
                .setSound(defaultSoundUri)
                .setContentIntent(pendingIntent)
                .setPriority(NotificationCompat.PRIORITY_HIGH) // Để hiển thị heads up
                .setCategory(NotificationCompat.CATEGORY_MESSAGE)
                .setVisibility(NotificationCompat.VISIBILITY_PUBLIC);

        // Thêm style cho notification dài
        if (message != null && message.length() > 50) {
            notificationBuilder.setStyle(new NotificationCompat.BigTextStyle().bigText(message));
        }

        // Hiển thị notification
        NotificationManager notificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        if (notificationManager != null) {
            notificationManager.notify((int) System.currentTimeMillis(), notificationBuilder.build());
        }
    }

    private void createNotificationChannel() {
        // Chỉ tạo channel cho Android 8.0+
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

            NotificationManager notificationManager = getSystemService(NotificationManager.class);
            if (notificationManager != null) {
                notificationManager.createNotificationChannel(channel);
            }
        }
    }

    private void sendTokenToServer(String token) {
        // TODO: Implement gửi token lên server
        Log.d(TAG, "Token should be sent to server: " + token);
    }
} 