# Heads Up Notification Setup

## Tổng quan
Ứng dụng TLU Project Expo đã được tích hợp Heads up notification sử dụng Firebase Cloud Messaging (FCM). Notification sẽ hiển thị dạng popup khi có thông báo mới.

## Các tính năng đã implement

### 1. Firebase Cloud Messaging
- ✅ NotificationService để xử lý push notification
- ✅ Heads up notification với priority HIGH
- ✅ Notification channel với sound và vibration
- ✅ Deep linking khi click vào notification

### 2. Permission Management
- ✅ Tự động yêu cầu quyền notification (Android 13+)
- ✅ Dialog giải thích về quyền notification
- ✅ Link đến cài đặt ứng dụng nếu cần

### 3. Notification Types
- ✅ PROJECT_INVITATION: Lời mời tham gia dự án
- ✅ NEW_COMMENT: Bình luận mới
- ✅ NEW_REPLY: Trả lời bình luận
- ✅ PROJECT_VOTE: Vote dự án
- ✅ COMMENT_VOTE: Vote bình luận

## Cách setup

### 1. Firebase Console Setup
1. Vào [Firebase Console](https://console.firebase.google.com/)
2. Chọn project của bạn
3. Vào **Project Settings** > **Cloud Messaging**
4. Tạo **Server key** nếu chưa có
5. Download `google-services.json` và đặt vào `app/`

### 2. Firebase Cloud Functions (Tùy chọn)
Nếu muốn gửi push notification tự động:

```bash
# Cài đặt Firebase CLI
npm install -g firebase-tools

# Login vào Firebase
firebase login

# Khởi tạo Functions trong project
firebase init functions

# Deploy functions
firebase deploy --only functions
```

## Cấu trúc code

### Files chính:
- `NotificationService.java`: Xử lý FCM messages
- `NotificationHelper.java`: Quản lý permission và channel
- `NotificationRepository.java`: Tích hợp với FCM
- `MainActivity.java`: Khởi tạo notification system

### Database Collections:
- `Notifications`: Lưu thông báo in-app
- `UserTokens`: Lưu FCM token của users

## Lưu ý quan trọng

1. **Android 13+**: Cần quyền `POST_NOTIFICATIONS`
2. **Android 8.0+**: Cần tạo notification channel
3. **FCM Token**: Cần lưu và gửi lên server để nhận push notification
4. **Background**: App phải được whitelist để nhận notification khi background

## Next Steps

1. Implement server-side logic để gửi FCM
2. Thêm notification settings cho user
3. Implement notification history
4. Thêm rich notification với image
5. Implement notification grouping 