package com.cse441.tluprojectexpo.repository;


import android.util.Log;
import androidx.annotation.NonNull;

import com.cse441.tluprojectexpo.utils.Constants;
import com.google.firebase.firestore.FirebaseFirestore;

public class CourseRepository {
    private static final String TAG = "CourseRepository";
    private FirebaseFirestore db;

    public interface CourseDetailsListener {
        void onCourseFetched(String courseName);
        void onCourseNotFound();
        void onError(String errorMessage);
    }

    public CourseRepository() {
        this.db = FirebaseFirestore.getInstance();
    }

    public void fetchCourseDetails(String courseId, @NonNull CourseDetailsListener listener) {
        if (courseId == null || courseId.isEmpty()) {
            listener.onCourseNotFound(); // Hoặc onError
            return;
        }
        db.collection(Constants.COLLECTION_COURSES).document(courseId).get()
                .addOnSuccessListener(courseDoc -> {
                    if (courseDoc.exists() && courseDoc.getString(Constants.FIELD_COURSE_NAME) != null) {
                        listener.onCourseFetched(courseDoc.getString(Constants.FIELD_COURSE_NAME));
                    } else {
                        listener.onCourseNotFound();
                    }
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Lỗi tải môn học: " + courseId, e);
                    listener.onError("Lỗi tải môn học: " + e.getMessage());
                });
    }
}
