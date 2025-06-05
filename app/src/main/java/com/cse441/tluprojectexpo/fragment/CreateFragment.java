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
import android.widget.TextView; // Added for the label
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.navigation.fragment.NavHostFragment;

import com.bumptech.glide.Glide;
import com.cse441.tluprojectexpo.R;
import com.google.android.flexbox.FlexboxLayout; // Added for FlexboxLayout
import com.google.android.material.button.MaterialButton;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.QuerySnapshot;
import com.google.android.material.textfield.TextInputLayout;

import java.util.ArrayList;
import java.util.List;

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

    // Views for single project cover image
    private ImageView ivBackArrow;
    private FrameLayout flProjectImageContainer;
    private ImageView ivProjectImagePreview;
    private ImageView ivProjectImagePlaceholderIcon;
    private Uri projectImageUri = null;

    // Views and data for multiple media (gallery)
    private MaterialButton btnAddMedia;
    private FlexboxLayout flexboxMediaPreviewContainer; // Added
    private TextView tvMediaGalleryLabel; // Added
    private List<Uri> selectedMediaUris = new ArrayList<>(); // Added to store multiple URIs

    private ActivityResultLauncher<Intent> projectImagePickerLauncher;
    private ActivityResultLauncher<Intent> mediaPickerLauncher;
    private ActivityResultLauncher<String[]> requestPermissionLauncher;

    private static final int PICK_PROJECT_IMAGE_ACTION = 1;
    private static final int PICK_MEDIA_ACTION = 2;
    private int currentPickerAction;


    public CreateFragment() {
        // Required empty public constructor
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        db = FirebaseFirestore.getInstance();

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

        projectImagePickerLauncher = registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), result -> {
            if (result.getResultCode() == Activity.RESULT_OK && result.getData() != null) {
                projectImageUri = result.getData().getData();
                if (projectImageUri != null && getContext() != null) {
                    Glide.with(this)
                            .load(projectImageUri)
                            .centerCrop()
                            .into(ivProjectImagePreview);
                    ivProjectImagePreview.setVisibility(View.VISIBLE);
                    ivProjectImagePlaceholderIcon.setVisibility(View.GONE);
                    flProjectImageContainer.setBackground(null);
                    Log.d(TAG, "Đã chọn ảnh dự án: " + projectImageUri.toString());
                }
            }
        });

        mediaPickerLauncher = registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), result -> {
            if (result.getResultCode() == Activity.RESULT_OK && result.getData() != null) {
                Uri selectedMediaUri = result.getData().getData();
                if (selectedMediaUri != null) {
                    selectedMediaUris.add(selectedMediaUri); // Add to the list
                    updateMediaPreviewGallery(); // Refresh the gallery display
                    Log.d(TAG, "Đã chọn media chung: " + selectedMediaUri.toString() + ". Tổng số: " + selectedMediaUris.size());
                    Toast.makeText(getContext(), "Đã thêm media.", Toast.LENGTH_SHORT).show();
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

        // Single project cover image views
        ivBackArrow = view.findViewById(R.id.iv_back_arrow);
        flProjectImageContainer = view.findViewById(R.id.fl_project_image_container);
        ivProjectImagePreview = view.findViewById(R.id.iv_project_image_preview);
        ivProjectImagePlaceholderIcon = view.findViewById(R.id.iv_project_image_placeholder_icon);

        // Multiple media gallery views
        btnAddMedia = view.findViewById(R.id.btn_add_media);
        flexboxMediaPreviewContainer = view.findViewById(R.id.flexbox_media_preview_container); // Initialize FlexboxLayout
        tvMediaGalleryLabel = view.findViewById(R.id.tv_media_gallery_label); // Initialize Label

        categoryAdapter = new ArrayAdapter<>(requireContext(), android.R.layout.simple_dropdown_item_1line, categoryList);
        actvCategory.setAdapter(categoryAdapter);

        technologyAdapter = new ArrayAdapter<>(requireContext(), android.R.layout.simple_dropdown_item_1line, technologyList);
        actvTechnology.setAdapter(technologyAdapter);

        statusAdapter = new ArrayAdapter<>(requireContext(), android.R.layout.simple_dropdown_item_1line, statusList);
        actvStatus.setAdapter(statusAdapter);

        fetchDataFromFirestore("categories", "name", categoryList, categoryAdapter, "Lĩnh vực");
        fetchDataFromFirestore("technologies", "name", technologyList, technologyAdapter, "Công nghệ");
        fetchDataFromFirestore("projectStatuses", "name", statusList, statusAdapter, "Trạng thái");

        setupItemClickListener(actvCategory, "Lĩnh vực");
        setupItemClickListener(actvTechnology, "Công nghệ");
        setupItemClickListener(actvStatus, "Trạng thái");

        setupDropdownToggle(tilCategory, actvCategory);
        setupDropdownToggle(tilTechnology, actvTechnology);
        setupDropdownToggle(tilStatus, actvStatus);

        ivBackArrow.setOnClickListener(v -> NavHostFragment.findNavController(CreateFragment.this).navigateUp());

        flProjectImageContainer.setOnClickListener(v -> {
            currentPickerAction = PICK_PROJECT_IMAGE_ACTION;
            if (checkAndRequestPermissions()) {
                launchProjectImagePicker();
            }
        });

        btnAddMedia.setOnClickListener(v -> { // This button now populates the gallery
            currentPickerAction = PICK_MEDIA_ACTION;
            if (checkAndRequestPermissions()) {
                launchMediaPicker();
            }
        });
        updateMediaPreviewGallery(); // Initial call in case there's saved state (though not implemented here)
    }

    private boolean checkAndRequestPermissions() {
        String[] permissionsToRequest;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
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
        projectImagePickerLauncher.launch(intent);
    }

    private void launchMediaPicker() {
        Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
        intent.setType("*/*");
        intent.putExtra(Intent.EXTRA_MIME_TYPES, new String[]{"image/*", "video/*"});
        intent.putExtra(Intent.EXTRA_ALLOW_MULTIPLE, true); // Allow multiple selection if desired
        mediaPickerLauncher.launch(intent);
    }

    private void updateMediaPreviewGallery() {
        if (getContext() == null) return;

        flexboxMediaPreviewContainer.removeAllViews(); // Clear existing previews

        if (selectedMediaUris.isEmpty()) {
            flexboxMediaPreviewContainer.setVisibility(View.GONE);
            tvMediaGalleryLabel.setVisibility(View.GONE);
            return;
        }

        flexboxMediaPreviewContainer.setVisibility(View.VISIBLE);
        tvMediaGalleryLabel.setVisibility(View.VISIBLE);

        // Convert 120dp to pixels
        int imageSizeInDp = 120;
        int imageSizeInPx = (int) (imageSizeInDp * getResources().getDisplayMetrics().density);
        int marginInDp = 4; // Margin around each image
        int marginInPx = (int) (marginInDp * getResources().getDisplayMetrics().density);

        for (int i = 0; i < selectedMediaUris.size(); i++) {
            Uri uri = selectedMediaUris.get(i);
            ImageView imageView = new ImageView(getContext());
            FlexboxLayout.LayoutParams layoutParams = new FlexboxLayout.LayoutParams(imageSizeInPx, imageSizeInPx);
            layoutParams.setMargins(marginInPx, marginInPx, marginInPx, marginInPx);
            imageView.setLayoutParams(layoutParams);
            imageView.setScaleType(ImageView.ScaleType.CENTER_CROP);
            // You might want a placeholder drawable
            // imageView.setBackgroundResource(R.drawable.image_placeholder_square);


            Glide.with(this)
                    .load(uri)
                    .placeholder(R.drawable.image_placeholder_square) // Make sure you have this drawable
                    .error(R.drawable.error) // Make sure you have this drawable
                    .centerCrop()
                    .into(imageView);

            // Add long click listener to remove the image
            final int indexToRemove = i;
            imageView.setOnLongClickListener(v -> {
                selectedMediaUris.remove(indexToRemove);
                updateMediaPreviewGallery(); // Refresh the gallery
                Toast.makeText(getContext(), "Đã xóa ảnh/video.", Toast.LENGTH_SHORT).show();
                return true; // Consume the long click
            });
            // Optional: click listener for other actions (e.g., view full screen)
            // imageView.setOnClickListener(v -> { /* ... */ });

            flexboxMediaPreviewContainer.addView(imageView);
        }
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

    // Make sure you have these drawables in your res/drawable folder:
    // 1. image_placeholder_square.xml (e.g., a light gray square)
    // <shape xmlns:android="http://schemas.android.com/apk/res/android" android:shape="rectangle">
    //     <solid android:color="#E0E0E0"/>
    //     <corners android:radius="4dp"/>
    // </shape>

    // 2. ic_broken_image.xml (a generic broken image icon)
    // <vector xmlns:android="http://schemas.android.com/apk/res/android"
    //     android:width="24dp"
    //     android:height="24dp"
    //     android:viewportWidth="24.0"
    //     android:viewportHeight="24.0"
    //     android:tint="?attr/colorControlNormal">
    //     <path
    //         android:fillColor="@android:color/darker_gray"
    //         android:pathData="M21,5L3,5v14h18L21,5zM5,17l3.5,-4.5 2.5,3.01L14.5,11l4.5,6L5,17z"/>
    // </vector>
}