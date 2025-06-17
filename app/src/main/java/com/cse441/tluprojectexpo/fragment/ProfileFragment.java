package com.cse441.tluprojectexpo.fragment;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button; // <-- Thêm import này
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull; // Thêm import này
import androidx.annotation.Nullable; // Thêm import này
import androidx.fragment.app.Fragment;

import com.bumptech.glide.Glide;
import com.cse441.tluprojectexpo.LoginActivity; // <-- Import LoginActivity
import com.cse441.tluprojectexpo.R;
import com.cse441.tluprojectexpo.model.User;
import com.google.firebase.auth.FirebaseAuth; // <-- Import FirebaseAuth
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

import de.hdodenhof.circleimageview.CircleImageView;

public class ProfileFragment extends Fragment {

    // Khai báo biến cho nút Đăng xuất
    private Button btnLogout; // <-- Khai báo

    // Khai báo Firebase Authentication instance
    private FirebaseAuth mAuth; // <-- Khai báo

    private CircleImageView ivUserAvatar;
    private TextView tvUserName;
    private TextView tvUserEmail;

    private FirebaseFirestore db;
    // TODO: Rename parameter arguments, choose names that match
    // the fragment initialization parameters, e.g. ARG_ITEM_NUMBER
    private static final String ARG_PARAM1 = "param1";
    private static final String ARG_PARAM2 = "param2";
    private static final String TAG = "ProfileFragment";

    // TODO: Rename and change types of parameters
    private String mParam1;
    private String mParam2;

    public ProfileFragment() {
        // Required empty public constructor
    }

    /**
     * Use this factory method to create a new instance of
     * this fragment using the provided parameters.
     *
     * @param param1 Parameter 1.
     * @param param2 Parameter 2.
     * @return A new instance of fragment HomeFragment.
     */
    // TODO: Rename and change types and number of parameters
    public static ProfileFragment newInstance(String param1, String param2) {
        ProfileFragment fragment = new ProfileFragment();
        Bundle args = new Bundle();
        args.putString(ARG_PARAM1, param1);
        args.putString(ARG_PARAM2, param2);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) { // Thêm @Nullable
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            mParam1 = getArguments().getString(ARG_PARAM1);
            mParam2 = getArguments().getString(ARG_PARAM2);
        }
        // Khởi tạo Firebase Auth instance
        mAuth = FirebaseAuth.getInstance(); // <-- Khởi tạo
        db = FirebaseFirestore.getInstance();

    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, // Thêm @NonNull, @Nullable
                             @Nullable Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View view = inflater.inflate(R.layout.fragment_profile, container, false);

        // Ánh xạ nút Đăng xuất
        btnLogout = view.findViewById(R.id.btnLogout); // <-- Ánh xạ
        ivUserAvatar = view.findViewById(R.id.iv_profile_image);
        tvUserName = view.findViewById(R.id.tv_user_name);
        tvUserEmail = view.findViewById(R.id.tv_user_email);

        loadUserProfile();
        // Thiết lập OnClickListener cho nút Đăng xuất
        if (btnLogout != null) { // Đảm bảo nút không null trước khi thiết lập listener
            btnLogout.setOnClickListener(v -> {
                logoutUser(); // Gọi hàm đăng xuất khi nút được nhấn
            });
        }

        return view;
    }

    private void loadUserProfile() {
        FirebaseUser currentUser = mAuth.getCurrentUser();
        if (currentUser != null) {
            String userId = currentUser.getUid();

            // Lấy thông tin từ Firestore
            db.collection("users").document(userId)
                    .get()
                    .addOnCompleteListener(task -> {
                        if (task.isSuccessful()) {
                            DocumentSnapshot document = task.getResult();
                            if (document.exists()) {
                                // Chuyển đổi DocumentSnapshot sang đối tượng User của bạn
                                User user = document.toObject(User.class);
                                if (user != null) {
                                    // Cập nhật UI với thông tin từ Firestore
                                    tvUserName.setText(user.getFullName());
                                    tvUserEmail.setText(user.getEmail());

                                    // Tải ảnh đại diện bằng Glide
                                    // Kiểm tra avatarUrl có tồn tại và không rỗng
                                    if (user.getAvatarUrl() != null && !user.getAvatarUrl().isEmpty()) {
                                        // Sử dụng URL từ Firestore
                                        Glide.with(this)
                                                .load(user.getAvatarUrl())
                                                .placeholder(R.drawable.default_avatar) // Ảnh placeholder khi đang tải
                                                .error(R.drawable.default_avatar)     // Ảnh hiển thị nếu lỗi
                                                .into(ivUserAvatar);
                                    } else {
                                        // Nếu avatarUrl trống hoặc null, sử dụng URL mặc định với userId
                                        String defaultAvatarUrl = "https://i.pravatar.cc/150?u=" + userId;
                                        Glide.with(this)
                                                .load(defaultAvatarUrl)
                                                .placeholder(R.drawable.default_avatar)
                                                .error(R.drawable.default_avatar)
                                                .into(ivUserAvatar);
                                    }
                                } else {
                                    Log.e(TAG, "Lỗi: Không thể chuyển đổi dữ liệu thành đối tượng User.");
                                    // Fallback: Nếu không load được User từ Firestore, hiển thị email từ Firebase Auth
                                    tvUserName.setText("Người dùng");
                                    tvUserEmail.setText(currentUser.getEmail());
                                    Glide.with(this).load(R.drawable.default_avatar).into(ivUserAvatar);
                                }
                            } else {
                                Log.d(TAG, "Không tìm thấy tài liệu người dùng trong Firestore.");
                                // Fallback: Nếu không tìm thấy trong Firestore, hiển thị email từ Firebase Auth
                                tvUserName.setText("Người dùng");
                                tvUserEmail.setText(currentUser.getEmail());
                                Glide.with(this).load(R.drawable.default_avatar).into(ivUserAvatar);
                            }
                        } else {
                            Log.e(TAG, "Lỗi khi lấy tài liệu người dùng: ", task.getException());
                            // Fallback: Nếu có lỗi khi truy vấn Firestore, hiển thị email từ Firebase Auth
                            tvUserName.setText("Người dùng");
                            tvUserEmail.setText(currentUser.getEmail());
                            Glide.with(this).load(R.drawable.default_avatar).into(ivUserAvatar);
                        }
                    });
        } else {
            // Người dùng chưa đăng nhập, xử lý tùy theo luồng ứng dụng của bạn
            tvUserName.setText("Khách");
            tvUserEmail.setText("guest@example.com");
            Glide.with(this).load(R.drawable.default_avatar).into(ivUserAvatar);
            Toast.makeText(getContext(), "Bạn chưa đăng nhập.", Toast.LENGTH_SHORT).show();
            // Có thể chuyển hướng về màn hình đăng nhập nếu người dùng không phải guest
            // if (getActivity() instanceof MainActivity) {
            //     ((MainActivity) getActivity()).setGuestMode(true); // Đặt lại chế độ khách nếu cần
            // }
        }
    }
    // Hàm thực hiện đăng xuất
    private void logoutUser() {
        mAuth.signOut(); // Đăng xuất khỏi Firebase Authentication

        // Hiển thị thông báo (tùy chọn)
        Toast.makeText(getContext(), "Đã đăng xuất thành công.", Toast.LENGTH_SHORT).show();

        // Chuyển hướng về màn hình đăng nhập (LoginActivity)
        // Cờ này sẽ xóa tất cả các Activity khỏi back stack và bắt đầu LoginActivity như một task mới
        Intent intent = new Intent(getActivity(), LoginActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);

        // Đóng MainActivity (Activity chứa Fragment này) để không thể quay lại bằng nút Back
        if (getActivity() != null) {
            getActivity().finish();
        }
    }
}