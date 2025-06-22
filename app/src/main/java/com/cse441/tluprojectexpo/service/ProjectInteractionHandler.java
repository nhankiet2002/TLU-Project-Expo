package com.cse441.tluprojectexpo.service;


import android.util.Log;

import com.cse441.tluprojectexpo.model.Comment;
import com.cse441.tluprojectexpo.model.User;
import com.cse441.tluprojectexpo.util.Constants;
import com.google.firebase.Timestamp;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.FirebaseFirestoreException;


import java.util.HashMap;
import java.util.Map;

public class ProjectInteractionHandler {
    private FirebaseFirestore db;
    private static final String TAG = "ProjectInteraction";

    public ProjectInteractionHandler(FirebaseFirestore dbInstance) {
        this.db = dbInstance;
    }

    public interface InteractionListener<T> {
        void onSuccess(T result);
        void onFailure(String errorMessage);
    }
    public interface UpvoteResult {
        long getNewVoteCount();
        boolean didUserUpvoteNow(); // true nếu user vừa upvote, false nếu vừa hủy upvote
    }

    private static class UpvoteResultImpl implements UpvoteResult {
        private final long newVoteCount;
        private final boolean didUserUpvoteNow;

        public UpvoteResultImpl(long newVoteCount, boolean didUserUpvoteNow) {
            this.newVoteCount = newVoteCount;
            this.didUserUpvoteNow = didUserUpvoteNow;
        }
        @Override
        public long getNewVoteCount() { return newVoteCount; }
        @Override
        public boolean didUserUpvoteNow() { return didUserUpvoteNow; }
    }


    public void handleUpvote(FirebaseUser currentUser, String projectId, InteractionListener<UpvoteResult> listener) {
        if (currentUser == null || projectId == null || projectId.isEmpty()) {
            if (listener != null) listener.onFailure("Thông tin không hợp lệ để bình chọn.");
            return;
        }

        DocumentReference projectRef = db.collection(Constants.COLLECTION_PROJECTS).document(projectId);
        DocumentReference voteRef = db.collection(Constants.COLLECTION_PROJECT_VOTES).document(projectId)
                .collection(Constants.SUB_COLLECTION_VOTERS).document(currentUser.getUid());

        db.runTransaction(transaction -> {
            DocumentSnapshot voteSnapshot = transaction.get(voteRef);
            DocumentSnapshot projectSnapshot = transaction.get(projectRef);

            if (!projectSnapshot.exists()) {
                throw new FirebaseFirestoreException("Dự án không tồn tại.", FirebaseFirestoreException.Code.NOT_FOUND);
            }
            long currentDBVoteCount = projectSnapshot.getLong(Constants.FIELD_VOTE_COUNT) != null ? projectSnapshot.getLong(Constants.FIELD_VOTE_COUNT) : 0;
            boolean newActionIsUpvote;

            if (voteSnapshot.exists()) { // User already voted, so unvote
                transaction.delete(voteRef);
                transaction.update(projectRef, Constants.FIELD_VOTE_COUNT, FieldValue.increment(-1));
                newActionIsUpvote = false;
                return new UpvoteResultImpl(currentDBVoteCount - 1, newActionIsUpvote);
            } else { // User has not voted, so upvote
                Map<String, Object> voterData = new HashMap<>();
                voterData.put(Constants.FIELD_VOTED_AT, FieldValue.serverTimestamp());
                transaction.set(voteRef, voterData);
                transaction.update(projectRef, Constants.FIELD_VOTE_COUNT, FieldValue.increment(1));
                newActionIsUpvote = true;
                return new UpvoteResultImpl(currentDBVoteCount + 1, newActionIsUpvote);
            }
        }).addOnSuccessListener(result -> {
            if (listener != null && result instanceof UpvoteResult) {
                listener.onSuccess((UpvoteResult) result);
            }
        }).addOnFailureListener(e -> {
            Log.e(TAG, "Lỗi giao dịch bình chọn: ", e);
            if (listener != null) listener.onFailure("Lỗi khi bình chọn: " + e.getMessage());
        });
    }

    public void postComment(FirebaseUser currentUser, String projectId, String commentText, InteractionListener<Comment> listener) {
        if (currentUser == null || projectId == null || projectId.isEmpty() || commentText == null || commentText.trim().isEmpty()) {
            if (listener != null) listener.onFailure("Thông tin không hợp lệ để bình luận.");
            return;
        }

        // Lấy thông tin người dùng hiện tại (tên, avatar) để hiển thị ngay
        db.collection(Constants.COLLECTION_USERS).document(currentUser.getUid()).get()
                .addOnSuccessListener(userSnapshot -> {
                    if (userSnapshot.exists()) {
                        User user = userSnapshot.toObject(User.class);
                        if (user != null) {
                            String userName = user.getFullName() != null ? user.getFullName() : "Người dùng ẩn danh";
                            String userAvatarUrl = user.getAvatarUrl();

                            Comment newCommentObject = new Comment(
                                    projectId,
                                    currentUser.getUid(), // AuthorUserId
                                    userName,             // Dùng để hiển thị ngay
                                    userAvatarUrl,        // Dùng để hiển thị ngay
                                    commentText.trim(),
                                    Timestamp.now()
                            );

                            db.collection(Constants.COLLECTION_COMMENTS).add(newCommentObject) // newCommentObject chứa các PropertyName đúng
                                    .addOnSuccessListener(documentReference -> {
                                        newCommentObject.setCommentId(documentReference.getId());
                                        if (listener != null) listener.onSuccess(newCommentObject);
                                    })
                                    .addOnFailureListener(e -> {
                                        Log.e(TAG, "Lỗi khi đăng bình luận", e);
                                        if (listener != null) listener.onFailure("Lỗi khi gửi bình luận: " + e.getMessage());
                                    });
                        } else {
                            if (listener != null) listener.onFailure("Không thể lấy thông tin người dùng.");
                        }
                    } else {
                        if (listener != null) listener.onFailure("Không tìm thấy người dùng.");
                    }
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Lỗi tải thông tin user để bình luận", e);
                    if (listener != null) listener.onFailure("Lỗi tải thông tin người dùng: " + e.getMessage());
                });
    }
}
