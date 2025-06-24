const functions = require('firebase-functions');
const admin = require('firebase-admin');

admin.initializeApp();

/**
 * Cloud Function để gửi push notification khi có notification mới
 */
exports.sendPushNotification = functions.firestore
    .document('Notifications/{notificationId}')
    .onCreate(async (snap, context) => {
        const notification = snap.data();
        
        try {
            // Lấy FCM token của user nhận thông báo
            const userTokenDoc = await admin.firestore()
                .collection('UserTokens')
                .doc(notification.recipientUserId)
                .get();

            if (!userTokenDoc.exists) {
                console.log('User token not found for user:', notification.recipientUserId);
                return null;
            }

            const userToken = userTokenDoc.data().fcmToken;
            
            if (!userToken) {
                console.log('FCM token not found for user:', notification.recipientUserId);
                return null;
            }

            // Tạo message payload
            const message = {
                token: userToken,
                notification: {
                    title: getNotificationTitle(notification.type),
                    body: notification.message
                },
                data: {
                    type: notification.type,
                    projectId: notification.targetProjectId || '',
                    commentId: notification.targetCommentId || '',
                    notificationId: context.params.notificationId
                },
                android: {
                    priority: 'high',
                    notification: {
                        channelId: 'TLUProjectExpo_Channel',
                        priority: 'high',
                        defaultSound: true,
                        defaultVibrateTimings: true
                    }
                },
                apns: {
                    payload: {
                        aps: {
                            sound: 'default',
                            badge: 1
                        }
                    }
                }
            };

            // Gửi notification
            const response = await admin.messaging().send(message);
            console.log('Successfully sent message:', response);
            return response;

        } catch (error) {
            console.error('Error sending notification:', error);
            return null;
        }
    });

/**
 * Lấy title cho notification dựa trên type
 */
function getNotificationTitle(type) {
    switch (type) {
        case 'PROJECT_INVITATION':
            return 'Lời mời tham gia dự án';
        case 'NEW_COMMENT':
            return 'Bình luận mới';
        case 'NEW_REPLY':
            return 'Trả lời bình luận';
        case 'PROJECT_VOTE':
            return 'Vote dự án';
        case 'COMMENT_VOTE':
            return 'Vote bình luận';
        default:
            return 'Thông báo mới';
    }
}

/**
 * Cloud Function để gửi notification tùy chỉnh
 */
exports.sendCustomNotification = functions.https.onCall(async (data, context) => {
    // Kiểm tra authentication
    if (!context.auth) {
        throw new functions.https.HttpsError('unauthenticated', 'User must be authenticated');
    }

    const { recipientUserId, title, message, type, projectId, commentId } = data;

    if (!recipientUserId || !title || !message) {
        throw new functions.https.HttpsError('invalid-argument', 'Missing required parameters');
    }

    try {
        // Lấy FCM token
        const userTokenDoc = await admin.firestore()
            .collection('UserTokens')
            .doc(recipientUserId)
            .get();

        if (!userTokenDoc.exists) {
            throw new functions.https.HttpsError('not-found', 'User token not found');
        }

        const userToken = userTokenDoc.data().fcmToken;
        
        if (!userToken) {
            throw new functions.https.HttpsError('not-found', 'FCM token not found');
        }

        // Tạo message
        const messagePayload = {
            token: userToken,
            notification: {
                title: title,
                body: message
            },
            data: {
                type: type || 'CUSTOM',
                projectId: projectId || '',
                commentId: commentId || ''
            },
            android: {
                priority: 'high',
                notification: {
                    channelId: 'TLUProjectExpo_Channel',
                    priority: 'high',
                    defaultSound: true,
                    defaultVibrateTimings: true
                }
            }
        };

        // Gửi notification
        const response = await admin.messaging().send(messagePayload);
        console.log('Successfully sent custom notification:', response);
        
        return { success: true, messageId: response };

    } catch (error) {
        console.error('Error sending custom notification:', error);
        throw new functions.https.HttpsError('internal', 'Error sending notification');
    }
}); 