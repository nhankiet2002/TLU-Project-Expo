package com.cse441.tluprojectexpo.utils;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.storage.FirebaseStorage;

public class FirebaseUtils {

    // Các instance tĩnh của Firebase services
    private static FirebaseAuth mAuth;
    private static FirebaseFirestore db;
    private static FirebaseStorage storage;

    // Phương thức để lấy FirebaseAuth instance
    // Đảm bảo chỉ có một instance được tạo
    public static FirebaseAuth getFirebaseAuth() {
        if (mAuth == null) {
            mAuth = FirebaseAuth.getInstance();
        }
        return mAuth;
    }

    // Phương thức để lấy FirebaseFirestore instance
    public static FirebaseFirestore getFirestoreInstance() {
        if (db == null) {
            db = FirebaseFirestore.getInstance();
        }
        return db;
    }

    // Phương thức để lấy FirebaseStorage instance
    public static FirebaseStorage getFirebaseStorage() {
        if (storage == null) {
            storage = FirebaseStorage.getInstance();
        }
        return storage;
    }

    // Kiểm tra xem người dùng hiện tại đã đăng nhập chưa
    public static boolean isLoggedIn() {
        return getFirebaseAuth().getCurrentUser() != null;
    }

    // Lấy thông tin người dùng Firebase hiện tại
    public static FirebaseUser getCurrentUser() {
        return getFirebaseAuth().getCurrentUser();
    }

    // Lấy UID (User ID) của người dùng hiện tại
    public static String getCurrentUserId() {
        FirebaseUser user = getCurrentUser();
        return (user != null) ? user.getUid() : null;
    }

    // Đăng xuất người dùng hiện tại
    public static void signOut() {
        getFirebaseAuth().signOut();
    }
}