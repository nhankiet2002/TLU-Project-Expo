package com.cse441.tluprojectexpo.repository;


import android.util.Log;
import androidx.annotation.NonNull;

import com.cse441.tluprojectexpo.utils.Constants;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.FirebaseFirestoreException;

import java.util.HashMap;
import java.util.Map;

public class VoteRepository {
    private static final String TAG = "VoteRepository";
    private FirebaseFirestore db;

    public interface VoteInteractionListener<T> {
        void onSuccess(T result);
        void onFailure(String errorMessage);
    }

    public interface UpvoteResult {
        long getNewVoteCount();
        boolean didUserUpvoteNow();
    }

    private static class UpvoteResultImpl implements UpvoteResult {
        private final long newVoteCount;
        private final boolean didUserUpvoteNow;
        public UpvoteResultImpl(long newVoteCount, boolean didUserUpvoteNow) {
            this.newVoteCount = newVoteCount; this.didUserUpvoteNow = didUserUpvoteNow;
        }
        @Override public long getNewVoteCount() { return newVoteCount; }
        @Override public boolean didUserUpvoteNow() { return didUserUpvoteNow; }
    }

    public interface UpvoteStatusListener {
        void onStatusChecked(boolean hasVoted);
    }

    public VoteRepository() {
        this.db = FirebaseFirestore.getInstance();
    }

    public void handleUpvote(FirebaseUser currentUser, String projectId, @NonNull VoteInteractionListener<UpvoteResult> listener) {
        if (currentUser == null || projectId == null || projectId.isEmpty()) {
            listener.onFailure("Thông tin không hợp lệ để bình chọn.");
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
            if (voteSnapshot.exists()) {
                transaction.delete(voteRef);
                transaction.update(projectRef, Constants.FIELD_VOTE_COUNT, FieldValue.increment(-1));
                newActionIsUpvote = false;
                return new UpvoteResultImpl(currentDBVoteCount - 1, newActionIsUpvote);
            } else {
                Map<String, Object> voterData = new HashMap<>();
                voterData.put(Constants.FIELD_VOTED_AT, FieldValue.serverTimestamp());
                transaction.set(voteRef, voterData);
                transaction.update(projectRef, Constants.FIELD_VOTE_COUNT, FieldValue.increment(1));
                newActionIsUpvote = true;
                return new UpvoteResultImpl(currentDBVoteCount + 1, newActionIsUpvote);
            }
        }).addOnSuccessListener(result -> {
            if (result instanceof UpvoteResult) {
                listener.onSuccess((UpvoteResult) result);
            } else {
                listener.onFailure("Kết quả giao dịch không hợp lệ.");
            }
        }).addOnFailureListener(e -> {
            Log.e(TAG, "Lỗi giao dịch bình chọn: ", e);
            listener.onFailure("Lỗi khi bình chọn: " + e.getMessage());
        });
    }

    public void checkIfUserUpvoted(String projectId, String userId, @NonNull UpvoteStatusListener listener) {
        if (projectId == null || userId == null) {
            listener.onStatusChecked(false);
            return;
        }
        db.collection(Constants.COLLECTION_PROJECT_VOTES).document(projectId)
                .collection(Constants.SUB_COLLECTION_VOTERS).document(userId)
                .get()
                .addOnSuccessListener(documentSnapshot -> listener.onStatusChecked(documentSnapshot.exists()))
                .addOnFailureListener(e -> {
                    Log.w(TAG, "Lỗi kiểm tra trạng thái upvote cho dự án: " + projectId, e);
                    listener.onStatusChecked(false);
                });
    }
}
