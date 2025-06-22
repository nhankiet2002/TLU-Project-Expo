package com.cse441.tluprojectexpo.service; // Hoặc package bạn đã chọn

import android.util.Log;

import com.cse441.tluprojectexpo.model.Comment; // Sử dụng model Comment của bạn
import com.cse441.tluprojectexpo.model.User;    // Sử dụng model User của bạn
import com.cse441.tluprojectexpo.utils.Constants;
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

    // Interface chung cho kết quả tương tác
    public interface InteractionListener<T> {
        void onSuccess(T result);
        void onFailure(String errorMessage);
    }

    // Interface cụ thể cho kết quả upvote
    public interface UpvoteResult {
        long getNewVoteCount();
        boolean didUserUpvoteNow(); // true nếu user vừa upvote, false nếu vừa hủy upvote
    }

    // Lớp triển khai UpvoteResult
    private static class UpvoteResultImpl implements UpvoteResult {
        private final long newVoteCount;
        private final boolean didUserUpvoteNow;

        public UpvoteResultImpl(long newVoteCount, boolean didUserUpvoteNow) {
            this.newVoteCount = newVoteCount;
            this.didUserUpvoteNow = didUserUpvoteNow;
        }
        @Override public long getNewVoteCount() { return newVoteCount; }
        @Override public boolean didUserUpvoteNow() { return didUserUpvoteNow; }
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
            // Giả sử Project model có getVoteCount() hoặc VoteCount là public
            // Nếu không, bạn cần lấy trực tiếp từ projectSnapshot.getLong(Constants.FIELD_VOTE_COUNT)
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
            if (listener != null && result instanceof UpvoteResult) { // result ở đây đã là UpvoteResultImpl
                listener.onSuccess((UpvoteResult) result);
            } else if (listener != null) {
                listener.onFailure("Kết quả giao dịch không hợp lệ.");
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

        db.collection(Constants.COLLECTION_USERS).document(currentUser.getUid()).get()
                .addOnSuccessListener(userSnapshot -> {
                    if (userSnapshot.exists()) {
                        User user = userSnapshot.toObject(User.class); // Sử dụng User model của bạn
                        if (user != null) {
                            // Giả sử User model của bạn có getFullName() và getAvatarUrl()
                            String userName = user.getFullName() != null ? user.getFullName() : "Người dùng ẩn danh";
                            String userAvatarUrl = user.getAvatarUrl();

                            // Sử dụng constructor của Comment model của bạn
                            // Đảm bảo Comment model của bạn có constructor phù hợp và các setter nếu cần
                            Comment newCommentObject = new Comment(
                                    projectId,
                                    currentUser.getUid(), // AuthorUserId
                                    userName,             // Dùng để hiển thị ngay, không lưu vào DB (do @Exclude trong model)
                                    userAvatarUrl,        // Dùng để hiển thị ngay, không lưu vào DB (do @Exclude trong model)
                                    commentText.trim(),
                                    Timestamp.now()
                            );
                            // Nếu model Comment của bạn không có constructor như trên, bạn cần tạo đối tượng
                            // và set từng trường một:
                            // Comment newCommentObject = new Comment();
                            // newCommentObject.setProjectId(projectId);
                            // newCommentObject.setUserId(currentUser.getUid()); // Đảm bảo setter này map tới AuthorUserId
                            // newCommentObject.setUserName(userName); // Trường này có @Exclude
                            // newCommentObject.setUserAvatarUrl(userAvatarUrl); // Trường này có @Exclude
                            // newCommentObject.setText(commentText.trim()); // Đảm bảo setter này map tới Content
                            // newCommentObject.setTimestamp(Timestamp.now()); // Đảm bảo setter này map tới CreatedAt

                            db.collection(Constants.COLLECTION_COMMENTS).add(newCommentObject)
                                    .addOnSuccessListener(documentReference -> {
                                        // Giả sử Comment model có setCommentId()
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