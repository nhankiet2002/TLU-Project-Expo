# Hướng dẫn Setup Firebase Console cho Heads Up Notification

## Bước 1: Tạo Firebase Project

### 1.1 Truy cập Firebase Console
1. Vào [Firebase Console](https://console.firebase.google.com/)
2. Click **"Create a project"** hoặc **"Add project"**

### 1.2 Thiết lập Project
1. **Nhập tên project**: `TLUProjectExpo` (hoặc tên bạn muốn)
2. **Google Analytics**: Bật (khuyến nghị)
3. **Analytics account**: Chọn account hiện có hoặc tạo mới
4. Click **"Create project"**

### 1.3 Cấu hình Project
1. Chọn **"Continue"** để bỏ qua Google Analytics setup
2. Click **"Continue to console"**

## Bước 2: Thêm Android App

### 2.1 Thêm Android App
1. Trong Firebase Console, click **"Add app"** (biểu tượng Android)
2. Chọn **Android** platform

### 2.2 Cấu hình App
1. **Android package name**: `com.cse441.tluprojectexpo`
2. **App nickname**: `TLU Project Expo` (tùy chọn)
3. **Debug signing certificate SHA-1**: (để test, có thể bỏ qua)
4. Click **"Register app"**

### 2.3 Download google-services.json
1. Download file `google-services.json`
2. Đặt file vào thư mục `app/` của project Android
3. Click **"Next"**

### 2.4 Verify Installation
1. Click **"Continue to console"**
2. Kiểm tra app đã xuất hiện trong danh sách

## Bước 3: Cấu hình Cloud Messaging

### 3.1 Vào Cloud Messaging
1. Trong sidebar, click **"Cloud Messaging"**
2. Click **"Get started"**

### 3.2 Cấu hình Android
1. **Default notification channel**: `TLUProjectExpo_Channel`
2. **Default notification icon**: Chọn icon (có thể dùng default)
3. **Default color**: Chọn màu chủ đạo của app
4. Click **"Save"**

### 3.3 Tạo Server Key (nếu cần)
1. Vào **Project Settings** (biểu tượng bánh răng)
2. Tab **"Cloud Messaging"**
3. Trong **"Project credentials"**, click **"Generate new private key"**
4. Download file JSON (giữ bí mật)

## Bước 4: Test FCM

### 4.1 Gửi Test Message
1. Trong **Cloud Messaging**, click **"Send your first message"**
2. **Notification title**: `Test Notification`
3. **Notification text**: `Đây là test notification từ Firebase`
4. **Target**: Chọn **"Single device"**
5. **FCM registration token**: (sẽ có sau khi chạy app)
6. Click **"Send"**

### 4.2 Lấy FCM Token từ App
```java
// Thêm code này vào MainActivity để lấy token
FirebaseMessaging.getInstance().getToken()
    .addOnCompleteListener(task -> {
        if (!task.isSuccessful()) {
            Log.w("FCM", "Fetching FCM registration token failed", task.getException());
            return;
        }

        // Lấy token thành công
        String token = task.getResult();
        Log.d("FCM", "Token: " + token);
        
        // Lưu token vào database
        NotificationRepository notificationRepository = new NotificationRepository();
        FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
        if (currentUser != null) {
            notificationRepository.saveFCMToken(currentUser.getUid(), token, 
                new NotificationRepository.NotificationActionListener() {
                    @Override
                    public void onSuccess() {
                        Log.d("FCM", "Token saved successfully");
                    }
                    @Override
                    public void onError(String errorMessage) {
                        Log.e("FCM", "Error saving token: " + errorMessage);
                    }
                });
        }
    });
```

## Bước 5: Deploy Cloud Functions (Tùy chọn)

### 5.1 Cài đặt Firebase CLI
```bash
npm install -g firebase-tools
```

### 5.2 Login Firebase
```bash
firebase login
```

### 5.3 Khởi tạo Functions
```bash
# Trong thư mục gốc của project
firebase init functions

# Chọn:
# - Use an existing project
# - Chọn project vừa tạo
# - JavaScript
# - ESLint: Yes
# - Install dependencies: Yes
```

### 5.4 Deploy Functions
```bash
firebase deploy --only functions
```

## Bước 6: Test Heads Up Notification

### 6.1 Test Local Notification
Thêm code này vào MainActivity để test:

```java
private void testLocalNotification() {
    NotificationHelper.createNotificationChannel(this);
    
    NotificationCompat.Builder builder = new NotificationCompat.Builder(this, NotificationHelper.getChannelId())
        .setSmallIcon(R.drawable.ic_bell)
        .setContentTitle("Test Heads Up")
        .setContentText("Đây là test heads up notification")
        .setPriority(NotificationCompat.PRIORITY_HIGH)
        .setAutoCancel(true)
        .setCategory(NotificationCompat.CATEGORY_MESSAGE)
        .setVisibility(NotificationCompat.VISIBILITY_PUBLIC);

    NotificationManager notificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
    notificationManager.notify(999, builder.build());
}

// Gọi trong onCreate hoặc button click
// testLocalNotification();
```

### 6.2 Test FCM Notification
1. Chạy app và lấy FCM token từ log
2. Vào Firebase Console > Cloud Messaging
3. Gửi test message với token vừa lấy
4. Kiểm tra notification hiển thị

## Troubleshooting

### 1. App không nhận được notification
- Kiểm tra `google-services.json` đã đúng vị trí
- Kiểm tra package name khớp với Firebase
- Kiểm tra internet connection
- Kiểm tra quyền notification đã được cấp

### 2. Heads up không hiển thị
- Kiểm tra Android version (cần 5.0+)
- Kiểm tra priority là HIGH
- Kiểm tra notification channel importance là HIGH
- Kiểm tra app không bị battery optimization

### 3. FCM token không lưu được
- Kiểm tra Firestore rules cho phép write
- Kiểm tra internet connection
- Kiểm tra user đã đăng nhập

## Lưu ý quan trọng

1. **google-services.json**: Không commit file này lên git (thêm vào .gitignore)
2. **Server Key**: Giữ bí mật, không chia sẻ
3. **FCM Token**: Thay đổi khi app được reinstall
4. **Testing**: Test trên device thật, không phải emulator 