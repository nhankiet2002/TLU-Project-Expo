package com.cse441.tluprojectexpo.fragment;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button; // <-- Thêm import này
import android.widget.Toast;

import androidx.annotation.NonNull; // Thêm import này
import androidx.annotation.Nullable; // Thêm import này
import androidx.fragment.app.Fragment;

import com.cse441.tluprojectexpo.LoginActivity; // <-- Import LoginActivity
import com.cse441.tluprojectexpo.R;
import com.google.firebase.auth.FirebaseAuth; // <-- Import FirebaseAuth

public class ProfileFragment extends Fragment {

    // Khai báo biến cho nút Đăng xuất
    private Button btnLogout; // <-- Khai báo

    // Khai báo Firebase Authentication instance
    private FirebaseAuth mAuth; // <-- Khai báo

    // TODO: Rename parameter arguments, choose names that match
    // the fragment initialization parameters, e.g. ARG_ITEM_NUMBER
    private static final String ARG_PARAM1 = "param1";
    private static final String ARG_PARAM2 = "param2";

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
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, // Thêm @NonNull, @Nullable
                             @Nullable Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View view = inflater.inflate(R.layout.fragment_profile, container, false);

        // Ánh xạ nút Đăng xuất
        btnLogout = view.findViewById(R.id.btnLogout); // <-- Ánh xạ

        // Thiết lập OnClickListener cho nút Đăng xuất
        if (btnLogout != null) { // Đảm bảo nút không null trước khi thiết lập listener
            btnLogout.setOnClickListener(v -> {
                logoutUser(); // Gọi hàm đăng xuất khi nút được nhấn
            });
        }

        return view;
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