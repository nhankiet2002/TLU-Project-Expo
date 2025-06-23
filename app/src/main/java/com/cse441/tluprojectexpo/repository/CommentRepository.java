package com.cse441.tluprojectexpo.repository; // Hoặc package con .comment

import android.util.Log;

import androidx.annotation.Nullable;

import com.cse441.tluprojectexpo.model.Comment; // Model Comment của bạn
import com.cse441.tluprojectexpo.model.User;    // Model User của bạn
import com.cse441.tluprojectexpo.utils.Constants;
import com.google.android.gms.tasks.Task;
import com.google.android.gms.tasks.Tasks;
import com.google.firebase.Timestamp;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class CommentRepository {
    private static final String TAG = "CommentRepository";
    private FirebaseFirestore db;

    public interface CommentsLoadListener {
        void onCommentsLoaded(List<Comment> rootComments); // Trả về danh sách comment gốc đã có replies
        void onCommentsEmpty();
        void onError(String errorMessage);
    }

    public interface CommentPostListener {
        void onCommentPosted(Comment newComment);
        void onPostFailed(String errorMessage);
    }

    public CommentRepository() {
        this.db = FirebaseFirestore.getInstance();
    }

    /**
     * Tải tất cả bình luận cho một dự án và xây dựng cấu trúc cha-con.
     */
    public void fetchProjectCommentsWithReplies(String projectId, CommentsLoadListener listener) {
        if (projectId == null || projectId.isEmpty()) {
            if (listener != null) listener.onError("Project ID không hợp lệ.");
            return;
        }
        if (listener == null) {
            Log.e(TAG, "CommentsLoadListener không được null.");
            return;
        }

        db.collection(Constants.COLLECTION_COMMENTS)
                .whereEqualTo(Constants.FIELD_PROJECT_ID, projectId)
                .orderBy(Constants.FIELD_CREATED_AT, Query.Direction.ASCENDING)
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    if (queryDocumentSnapshots.isEmpty()) {
                        listener.onCommentsEmpty();
                        return;
                    }

                    Map<String, Comment> commentsMap = new HashMap<>();
                    List<Comment> rootComments = new ArrayList<>();
                    List<Task<Void>> userLoadTasks = new ArrayList<>();

                    for (QueryDocumentSnapshot doc : queryDocumentSnapshots) {
                        Comment comment = doc.toObject(Comment.class);
                        if (comment == null) {
                            Log.w(TAG, "Comment document " + doc.getId() + " cannot be converted.");
                            continue;
                        }
                        comment.setCommentId(doc.getId());
                        commentsMap.put(doc.getId(), comment);

                        if (comment.getUserId() != null && !comment.getUserId().isEmpty()) {
                            Task<Void> userTask = db.collection(Constants.COLLECTION_USERS).document(comment.getUserId()).get()
                                    .continueWithTask(task -> {
                                        if (task.isSuccessful()) {
                                            DocumentSnapshot userDoc = task.getResult();
                                            if (userDoc != null && userDoc.exists()) {
                                                User user = userDoc.toObject(User.class);
                                                if (user != null) {
                                                    comment.setUserName(user.getFullName());
                                                    comment.setUserAvatarUrl(user.getAvatarUrl());
                                                }
                                            }
                                        }
                                        return Tasks.forResult(null);
                                    });
                            userLoadTasks.add(userTask);
                        }
                    }

                    Tasks.whenAllComplete(userLoadTasks).addOnCompleteListener(allUserTasks -> {
                        for (Comment comment : commentsMap.values()) {
                            if (comment.getParentCommentId() != null && commentsMap.containsKey(comment.getParentCommentId())) {
                                Comment parent = commentsMap.get(comment.getParentCommentId());
                                if (parent != null) parent.addReply(comment);
                            } else {
                                rootComments.add(comment);
                            }
                        }

                        Collections.sort(rootComments, (c1, c2) -> {
                            if (c1.getTimestamp() == null && c2.getTimestamp() == null) return 0;
                            if (c1.getTimestamp() == null) return 1;
                            if (c2.getTimestamp() == null) return -1;
                            return c2.getTimestamp().compareTo(c1.getTimestamp());
                        });

                        for(Comment rootComment : rootComments) {
                            if (rootComment.getReplies() != null && !rootComment.getReplies().isEmpty()) {
                                Collections.sort(rootComment.getReplies(), (r1, r2) -> {
                                    if (r1.getTimestamp() == null && r2.getTimestamp() == null) return 0;
                                    if (r1.getTimestamp() == null) return -1;
                                    if (r2.getTimestamp() == null) return 1;
                                    return r1.getTimestamp().compareTo(r2.getTimestamp());
                                });
                            }
                        }
                        listener.onCommentsLoaded(rootComments);
                    });

                }).addOnFailureListener(e -> {
                    Log.e(TAG, "Lỗi tải bình luận cho dự án: " + projectId, e);
                    listener.onError("Lỗi tải bình luận: " + e.getMessage());
                });
    }

    /**
     * Đăng một bình luận mới.
     */
    public void postComment(FirebaseUser currentUser, String projectId, String commentText,
                            @Nullable String parentCommentId, CommentPostListener listener) {
        if (currentUser == null || projectId == null || projectId.isEmpty() || commentText == null || commentText.trim().isEmpty()) {
            if (listener != null) listener.onPostFailed("Thông tin không hợp lệ để bình luận.");
            return;
        }
        if (listener == null) {
            Log.e(TAG, "CommentPostListener không được null.");
            return;
        }

        db.collection(Constants.COLLECTION_USERS).document(currentUser.getUid()).get()
                .addOnSuccessListener(userSnapshot -> {
                    if (userSnapshot.exists()) {
                        User user = userSnapshot.toObject(User.class);
                        if (user != null) {
                            String userName = user.getFullName() != null ? user.getFullName() : "Người dùng ẩn danh";
                            String userAvatarUrl = user.getAvatarUrl();

                            Comment newCommentObject = new Comment();
                            newCommentObject.setProjectId(projectId);
                            newCommentObject.setUserId(currentUser.getUid()); // Sẽ map tới AuthorUserId
                            newCommentObject.setText(commentText.trim());
                            newCommentObject.setTimestamp(Timestamp.now());
                            if (parentCommentId != null && !parentCommentId.isEmpty()) {
                                newCommentObject.setParentCommentId(parentCommentId);
                            }
                            // Set các trường Exclude để trả về cho UI
                            newCommentObject.setUserName(userName);
                            newCommentObject.setUserAvatarUrl(userAvatarUrl);

                            db.collection(Constants.COLLECTION_COMMENTS).add(newCommentObject)
                                    .addOnSuccessListener(documentReference -> {
                                        newCommentObject.setCommentId(documentReference.getId());
                                        listener.onCommentPosted(newCommentObject);

                                        // Tích hợp notification cho chủ dự án
                                        FirebaseFirestore db2 = FirebaseFirestore.getInstance();
                                        db2.collection(Constants.COLLECTION_PROJECTS).document(projectId).get()
                                            .addOnSuccessListener(projectDoc -> {
                                                if (projectDoc.exists()) {
                                                    com.cse441.tluprojectexpo.model.Project project = projectDoc.toObject(com.cse441.tluprojectexpo.model.Project.class);
                                                    if (project != null && project.getCreatorUserId() != null) {
                                                        String projectOwnerId = project.getCreatorUserId();
                                                        if (!currentUser.getUid().equals(projectOwnerId)) {
                                                            NotificationRepository notificationRepository = new NotificationRepository();
                                                            notificationRepository.createCommentNotification(
                                                                currentUser,
                                                                projectId,
                                                                documentReference.getId(),
                                                                commentText,
                                                                projectOwnerId,
                                                                new NotificationRepository.NotificationActionListener() {
                                                                    @Override
                                                                    public void onSuccess() { }
                                                                    @Override
                                                                    public void onError(String errorMessage) { }
                                                                }
                                                            );
                                                        }
                                                    }
                                                }
                                            });
                                    })
                                    .addOnFailureListener(e -> {
                                        Log.e(TAG, "Lỗi khi đăng bình luận vào Firestore", e);
                                        listener.onPostFailed("Lỗi khi gửi bình luận: " + e.getMessage());
                                    });
                        } else {
                            listener.onPostFailed("Không thể phân tích dữ liệu người dùng.");
                        }
                    } else {
                        listener.onPostFailed("Không tìm thấy thông tin người dùng.");
                    }
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Lỗi tải thông tin người dùng để bình luận", e);
                    listener.onPostFailed("Lỗi tải thông tin người dùng: " + e.getMessage());
                });
    }
}