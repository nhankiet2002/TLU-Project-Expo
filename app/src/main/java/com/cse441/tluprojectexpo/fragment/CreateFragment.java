package com.cse441.tluprojectexpo.fragment;

import android.Manifest;
import android.app.Activity;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.MediaStore;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.navigation.fragment.NavHostFragment; // Dùng cho hành động quay lại của Navigation Component

import com.bumptech.glide.Glide; // Thư viện được khuyến nghị để tải ảnh
import com.cse441.tluprojectexpo.R;
import com.google.android.material.button.MaterialButton;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.QuerySnapshot;
import com.google.android.material.textfield.TextInputLayout;

import java.util.ArrayList;
import java.util.List;
// import java.util.Map; // Không còn được sử dụng trong đoạn mã này

public class CreateFragment extends Fragment {

    private static final String TAG = "CreateFragment";

    private AutoCompleteTextView actvCategory;
    private AutoCompleteTextView actvTechnology;
    private AutoCompleteTextView actvStatus;

    private ArrayAdapter<String> categoryAdapter;
    private ArrayAdapter<String> technologyAdapter;
    private ArrayAdapter<String> statusAdapter;

    private List<String> categoryList = new ArrayList<>();
    private List<String> technologyList = new ArrayList<>();
    private List<String> statusList = new ArrayList<>();

    private FirebaseFirestore db;

    // Các View để xử lý ảnh/media
    private ImageView ivBackArrow;
    private FrameLayout flProjectImageContainer;
    private ImageView ivProjectImagePreview;
    private ImageView ivProjectImagePlaceholderIcon;
    private MaterialButton btnAddMedia;

    private Uri projectImageUri = null; // Để lưu trữ URI của ảnh dự án đã chọn

    // Các ActivityResultLauncher để chọn tệp và yêu cầu quyền
    private ActivityResultLauncher<Intent> projectImagePickerLauncher;
    private ActivityResultLauncher<Intent> mediaPickerLauncher;
    private ActivityResultLauncher<String[]> requestPermissionLauncher;

    private static final int PICK_PROJECT_IMAGE_ACTION = 1;
    private static final int PICK_MEDIA_ACTION = 2;
    private int currentPickerAction; // Để biết trình chọn nào sẽ khởi chạy sau khi cấp quyền


    public CreateFragment() {
        // Constructor công khai rỗng bắt buộc
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        db = FirebaseFirestore.getInstance();

        // Khởi tạo ActivityResultLaunchers
        // Dùng để yêu cầu quyền
        requestPermissionLauncher = registerForActivityResult(new ActivityResultContracts.RequestMultiplePermissions(), permissions -> {
            boolean allGranted = true;
            for (Boolean granted : permissions.values()) {
                if (!granted) {
                    allGranted = false;
                    break;
                }
            }
            if (allGranted) {
                if (currentPickerAction == PICK_PROJECT_IMAGE_ACTION) {
                    launchProjectImagePicker();
                } else if (currentPickerAction == PICK_MEDIA_ACTION) {
                    launchMediaPicker();
                }
            } else {
                Toast.makeText(getContext(), "Quyền truy cập bộ nhớ bị từ chối.", Toast.LENGTH_SHORT).show();
            }
        });

        // Dùng để chọn ảnh dự án
        projectImagePickerLauncher = registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), result -> {
            if (result.getResultCode() == Activity.RESULT_OK && result.getData() != null) {
                projectImageUri = result.getData().getData();
                if (projectImageUri != null && getContext() != null) {
                    Glide.with(this)
                            .load(projectImageUri)
                            .centerCrop() // Đảm bảo ảnh lấp đầy ImageView
                            .into(ivProjectImagePreview);
                    ivProjectImagePreview.setVisibility(View.VISIBLE);
                    ivProjectImagePlaceholderIcon.setVisibility(View.GONE);
                    flProjectImageContainer.setBackground(null); // Xóa nền của trình giữ chỗ
                    Log.d(TAG, "Đã chọn ảnh dự án: " + projectImageUri.toString());
                }
            }
        });

        // Dùng để chọn media chung (ảnh/video)
        mediaPickerLauncher = registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), result -> {
            if (result.getResultCode() == Activity.RESULT_OK && result.getData() != null) {
                Uri selectedMediaUri = result.getData().getData();
                if (selectedMediaUri != null) {
                    // Xử lý URI media đã chọn (ví dụ: hiển thị, thêm vào danh sách)
                    Toast.makeText(getContext(), "Đã chọn Media: " + selectedMediaUri.toString(), Toast.LENGTH_LONG).show();
                    Log.d(TAG, "Đã chọn media chung: " + selectedMediaUri.toString());
                    // TODO: Thêm logic để xử lý media đã chọn này (ví dụ: thêm vào danh sách media của dự án)
                }
            }
        });
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_create, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        actvCategory = view.findViewById(R.id.actv_category);
        actvTechnology = view.findViewById(R.id.actv_technology);
        actvStatus = view.findViewById(R.id.actv_status);

        TextInputLayout tilCategory = view.findViewById(R.id.til_category);
        TextInputLayout tilTechnology = view.findViewById(R.id.til_technology);
        TextInputLayout tilStatus = view.findViewById(R.id.til_status);

        // Các view ảnh/media
        ivBackArrow = view.findViewById(R.id.iv_back_arrow);
        flProjectImageContainer = view.findViewById(R.id.fl_project_image_container);
        ivProjectImagePreview = view.findViewById(R.id.iv_project_image_preview);
        ivProjectImagePlaceholderIcon = view.findViewById(R.id.iv_project_image_placeholder_icon);
        btnAddMedia = view.findViewById(R.id.btn_add_media);

        // --- Thiết lập Adapters ---
        categoryAdapter = new ArrayAdapter<>(requireContext(), android.R.layout.simple_dropdown_item_1line, categoryList);
        actvCategory.setAdapter(categoryAdapter);

        technologyAdapter = new ArrayAdapter<>(requireContext(), android.R.layout.simple_dropdown_item_1line, technologyList);
        actvTechnology.setAdapter(technologyAdapter);

        statusAdapter = new ArrayAdapter<>(requireContext(), android.R.layout.simple_dropdown_item_1line, statusList);
        actvStatus.setAdapter(statusAdapter);

        // --- Tải dữ liệu từ Firestore ---
        fetchDataFromFirestore("categories", "name", categoryList, categoryAdapter, "Lĩnh vực");
        fetchDataFromFirestore("technologies", "name", technologyList, technologyAdapter, "Công nghệ");
        fetchDataFromFirestore("projectStatuses", "name", statusList, statusAdapter, "Trạng thái");

        // --- Bộ lắng nghe sự kiện Click cho AutoCompleteTextViews ---
        setupItemClickListener(actvCategory, "Lĩnh vực");
        setupItemClickListener(actvTechnology, "Công nghệ");
        setupItemClickListener(actvStatus, "Trạng thái");

        // --- Bộ chuyển đổi Dropdown cho TextInputLayouts ---
        setupDropdownToggle(tilCategory, actvCategory);
        setupDropdownToggle(tilTechnology, actvTechnology);
        setupDropdownToggle(tilStatus, actvStatus);

        // --- Thiết lập Bộ lắng nghe sự kiện Click cho các chức năng mới ---
        ivBackArrow.setOnClickListener(v -> {
            // Lựa chọn 1: Nếu sử dụng Android Navigation Component
            NavHostFragment.findNavController(CreateFragment.this).navigateUp();

            // Lựa chọn 2: Nếu quản lý giao dịch fragment thủ công
            // if (getParentFragmentManager().getBackStackEntryCount() > 0) {
            //     getParentFragmentManager().popBackStack();
            // } else {
            //     requireActivity().finish(); // Hoặc hành động quay lại phù hợp khác
            // }
        });

        flProjectImageContainer.setOnClickListener(v -> {
            currentPickerAction = PICK_PROJECT_IMAGE_ACTION;
            if (checkAndRequestPermissions()) {
                launchProjectImagePicker();
            }
        });

        btnAddMedia.setOnClickListener(v -> {
            currentPickerAction = PICK_MEDIA_ACTION;
            if (checkAndRequestPermissions()) {
                launchMediaPicker();
            }
        });
    }

    private boolean checkAndRequestPermissions() {
        String[] permissionsToRequest;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) { // Android 13+
            permissionsToRequest = new String[]{
                    Manifest.permission.READ_MEDIA_IMAGES,
                    Manifest.permission.READ_MEDIA_VIDEO
            };
        } else {
            permissionsToRequest = new String[]{Manifest.permission.READ_EXTERNAL_STORAGE};
        }

        List<String> permissionsNeeded = new ArrayList<>();
        for (String permission : permissionsToRequest) {
            if (ContextCompat.checkSelfPermission(requireContext(), permission) != PackageManager.PERMISSION_GRANTED) {
                permissionsNeeded.add(permission);
            }
        }

        if (!permissionsNeeded.isEmpty()) {
            requestPermissionLauncher.launch(permissionsNeeded.toArray(new String[0]));
            return false;
        }
        return true;
    }

    private void launchProjectImagePicker() {
        Intent intent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
        // Để chọn ảnh chung, cũng có thể sử dụng:
        // Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
        // intent.setType("image/*");
        projectImagePickerLauncher.launch(intent);
    }

    private void launchMediaPicker() {
        Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
        intent.setType("*/*"); // Ban đầu cho phép mọi loại tệp
        intent.putExtra(Intent.EXTRA_MIME_TYPES, new String[]{"image/*", "video/*"}); // Chỉ định các loại tệp mong muốn
        // Đối với Android 10 (API 29) trở lên, Intent.ACTION_OPEN_DOCUMENT có thể được ưu tiên để người dùng kiểm soát tốt hơn
        // Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
        // intent.addCategory(Intent.CATEGORY_OPENABLE);
        // intent.setType("*/*");
        // intent.putExtra(Intent.EXTRA_MIME_TYPES, new String[]{"image/*", "video/*"});
        mediaPickerLauncher.launch(intent);
    }


    private void fetchDataFromFirestore(String collectionName, String fieldName, List<String> dataList, ArrayAdapter<String> adapter, String logTagPrefix) {
        Log.d(TAG, "Đang tải " + logTagPrefix + " từ Firestore (Collection: " + collectionName + ")");
        dataList.clear();

        db.collection(collectionName)
                .orderBy(fieldName)
                .get()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        QuerySnapshot querySnapshot = task.getResult();
                        if (querySnapshot != null && !querySnapshot.isEmpty()) {
                            for (QueryDocumentSnapshot document : querySnapshot) {
                                String value = document.getString(fieldName);
                                if (value != null && !value.isEmpty()) {
                                    dataList.add(value);
                                }
                            }
                            adapter.notifyDataSetChanged();
                            Log.d(TAG, "Đã tải xong " + logTagPrefix + ": " + dataList.size() + " mục.");
                        } else {
                            Log.d(TAG, "Không có " + logTagPrefix + " nào trong collection '" + collectionName + "'.");
                            if(getContext() != null) {
                                Toast.makeText(getContext(), "Không tìm thấy " + logTagPrefix.toLowerCase() + ".", Toast.LENGTH_SHORT).show();
                            }
                        }
                    } else {
                        Log.w(TAG, "Lỗi khi lấy dữ liệu " + logTagPrefix + ": ", task.getException());
                        if(getContext() != null) {
                            Toast.makeText(getContext(), "Lỗi khi tải " + logTagPrefix.toLowerCase() + ".", Toast.LENGTH_SHORT).show();
                        }
                    }
                });
    }

    private void setupItemClickListener(AutoCompleteTextView autoCompleteTextView, String fieldName) {
        autoCompleteTextView.setOnItemClickListener((parent, MView, position, id) -> {
            String selectedItem = (String) parent.getItemAtPosition(position);
            if(getContext() != null) {
                Toast.makeText(requireContext(), "Đã chọn " + fieldName + ": " + selectedItem, Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void setupDropdownToggle(TextInputLayout textInputLayout, AutoCompleteTextView autoCompleteTextView) {
        if (textInputLayout != null && textInputLayout.getEndIconDrawable() != null) {
            textInputLayout.setEndIconOnClickListener(v -> {
                if (!autoCompleteTextView.isPopupShowing()) {
                    autoCompleteTextView.showDropDown();
                }
            });
        }
    }
}