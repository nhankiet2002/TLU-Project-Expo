package com.cse441.tluprojectexpo.fragment;

// ... (Imports như cũ) ...
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
import android.view.ViewTreeObserver;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
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
import com.cse441.tluprojectexpo.model.Member;
import com.google.android.flexbox.FlexboxLayout;
import com.google.android.material.button.MaterialButton;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.QuerySnapshot;
import com.google.android.material.textfield.TextInputLayout;

import java.util.ArrayList;
import java.util.List;


public class CreateFragment extends Fragment implements AddMemberDialogFragment.AddMemberDialogListener {

    private static final String TAG = "CreateFragment";

    private ActivityResultLauncher<Intent> projectImagePickerLauncher;
    private ActivityResultLauncher<Intent> mediaPickerLauncher;
    private ActivityResultLauncher<String[]> requestPermissionLauncher;

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

    private ImageView ivBackArrow;
    private FrameLayout flProjectImageContainer;
    private ImageView ivProjectImagePreview;
    private ImageView ivProjectImagePlaceholderIcon;
    private Uri projectImageUri = null;

    private MaterialButton btnAddMedia;
    private FlexboxLayout flexboxMediaPreviewContainer;
    private TextView tvMediaGalleryLabel;
    private List<Uri> selectedMediaUris = new ArrayList<>();

    private MaterialButton btnAddMember;
    private MaterialButton btnAddLink;
    private LinearLayout llSelectedMembersContainer;
    private List<Member> selectedProjectMembers = new ArrayList<>();

    private LinearLayout llMemberSectionRoot, llLinkSectionRoot, llMediaSectionRoot;

    private static final int ACTION_PICK_PROJECT_IMAGE = 1;
    private static final int ACTION_PICK_MEDIA = 2;
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
                if (currentPickerAction == ACTION_PICK_PROJECT_IMAGE) {
                    launchProjectImagePicker();
                } else if (currentPickerAction == ACTION_PICK_MEDIA) {
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
                    Glide.with(this).load(projectImageUri).centerCrop().into(ivProjectImagePreview);
                    ivProjectImagePreview.setVisibility(View.VISIBLE);
                    ivProjectImagePlaceholderIcon.setVisibility(View.GONE);
                    if (flProjectImageContainer != null) flProjectImageContainer.setBackground(null);
                }
            }
        });

        mediaPickerLauncher = registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), result -> {
            if (result.getResultCode() == Activity.RESULT_OK && result.getData() != null) {
                Uri selectedMediaUri = result.getData().getData();
                if (selectedMediaUri != null) {
                    selectedMediaUris.add(selectedMediaUri);
                    updateMediaPreviewGallery();
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
        ivBackArrow = view.findViewById(R.id.iv_back_arrow);
        flProjectImageContainer = view.findViewById(R.id.fl_project_image_container);
        ivProjectImagePreview = view.findViewById(R.id.iv_project_image_preview);
        ivProjectImagePlaceholderIcon = view.findViewById(R.id.iv_project_image_placeholder_icon);

        llMemberSectionRoot = view.findViewById(R.id.ll_member_section_root);
        btnAddMember = view.findViewById(R.id.btn_add_member);
        llSelectedMembersContainer = view.findViewById(R.id.ll_selected_members_container);

        llLinkSectionRoot = view.findViewById(R.id.ll_link_section_root);
        btnAddLink = view.findViewById(R.id.btn_add_link);

        llMediaSectionRoot = view.findViewById(R.id.ll_media_section_root);
        btnAddMedia = view.findViewById(R.id.btn_add_media);
        flexboxMediaPreviewContainer = view.findViewById(R.id.flexbox_media_preview_container);
        tvMediaGalleryLabel = view.findViewById(R.id.tv_media_gallery_label);

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
            currentPickerAction = ACTION_PICK_PROJECT_IMAGE;
            if (checkAndRequestPermissions()) launchProjectImagePicker();
        });
        btnAddMember.setOnClickListener(v -> {
            AddMemberDialogFragment addMemberDialog = AddMemberDialogFragment.newInstance();
            addMemberDialog.setDialogListener(this);
            addMemberDialog.show(getChildFragmentManager(), "AddMemberDialog");
        });
        btnAddLink.setOnClickListener(v -> Toast.makeText(getContext(), "Chức năng Thêm liên kết sắp ra mắt!", Toast.LENGTH_SHORT).show());
        btnAddMedia.setOnClickListener(v -> {
            currentPickerAction = ACTION_PICK_MEDIA;
            if (checkAndRequestPermissions()) launchMediaPicker();
        });

        final View rootView = view;
        rootView.getViewTreeObserver().addOnGlobalLayoutListener(new ViewTreeObserver.OnGlobalLayoutListener() {
            @Override
            public void onGlobalLayout() {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN) {
                    rootView.getViewTreeObserver().removeOnGlobalLayoutListener(this);
                } else {
                    rootView.getViewTreeObserver().removeGlobalOnLayoutListener(this);
                }
                synchronizeButtonWidths();
            }
        });
        updateSelectedMembersUI();
        updateMediaPreviewGallery();
    }

    private void synchronizeButtonWidths() {
        if (llMemberSectionRoot == null || llLinkSectionRoot == null || llMediaSectionRoot == null) {
            return;
        }
        int memberSectionWidth = llMemberSectionRoot.getWidth();
        int linkSectionWidth = llLinkSectionRoot.getWidth();
        int mediaSectionWidth = llMediaSectionRoot.getWidth();
        int maxWidth = 0;
        maxWidth = Math.max(maxWidth, memberSectionWidth);
        maxWidth = Math.max(maxWidth, linkSectionWidth);
        maxWidth = Math.max(maxWidth, mediaSectionWidth);
        if (maxWidth > 0) {
            ViewGroup.LayoutParams memberParams = llMemberSectionRoot.getLayoutParams();
            memberParams.width = maxWidth;
            llMemberSectionRoot.setLayoutParams(memberParams);
            ViewGroup.LayoutParams linkParams = llLinkSectionRoot.getLayoutParams();
            linkParams.width = maxWidth;
            llLinkSectionRoot.setLayoutParams(linkParams);
            ViewGroup.LayoutParams mediaParams = llMediaSectionRoot.getLayoutParams();
            mediaParams.width = maxWidth;
            llMediaSectionRoot.setLayoutParams(mediaParams);
        }
    }

    private boolean checkAndRequestPermissions() {
        String[] permissionsToRequest;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            permissionsToRequest = new String[]{Manifest.permission.READ_MEDIA_IMAGES, Manifest.permission.READ_MEDIA_VIDEO};
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
        mediaPickerLauncher.launch(intent);
    }

    private void updateMediaPreviewGallery() {
        if (getContext() == null || flexboxMediaPreviewContainer == null || tvMediaGalleryLabel == null) return;
        flexboxMediaPreviewContainer.removeAllViews();
        if (selectedMediaUris.isEmpty()) {
            flexboxMediaPreviewContainer.setVisibility(View.GONE);
            tvMediaGalleryLabel.setVisibility(View.GONE);
            return;
        }
        flexboxMediaPreviewContainer.setVisibility(View.VISIBLE);
        tvMediaGalleryLabel.setVisibility(View.VISIBLE);
        int imageSizeInDp = 120;
        int imageSizeInPx = (int) (imageSizeInDp * getResources().getDisplayMetrics().density);
        int marginInDp = 4;
        int marginInPx = (int) (marginInDp * getResources().getDisplayMetrics().density);
        for (int i = 0; i < selectedMediaUris.size(); i++) {
            Uri uri = selectedMediaUris.get(i);
            ImageView imageView = new ImageView(getContext());
            FlexboxLayout.LayoutParams layoutParams = new FlexboxLayout.LayoutParams(imageSizeInPx, imageSizeInPx);
            layoutParams.setMargins(marginInPx, marginInPx, marginInPx, marginInPx);
            imageView.setLayoutParams(layoutParams);
            imageView.setScaleType(ImageView.ScaleType.CENTER_CROP);
            Glide.with(this).load(uri).placeholder(R.drawable.image_placeholder_square).error(R.drawable.error).centerCrop().into(imageView);
            final int indexToRemove = i;
            imageView.setOnLongClickListener(v -> {
                if (indexToRemove < selectedMediaUris.size()) {
                    selectedMediaUris.remove(indexToRemove);
                    updateMediaPreviewGallery();
                    Toast.makeText(getContext(), "Đã xóa ảnh/video.", Toast.LENGTH_SHORT).show();
                }
                return true;
            });
            flexboxMediaPreviewContainer.addView(imageView);
        }
    }

    private void fetchDataFromFirestore(String collectionName, String fieldName, List<String> dataList, ArrayAdapter<String> adapter, String logTagPrefix) {
        dataList.clear();
        db.collection(collectionName).orderBy(fieldName).get().addOnCompleteListener(task -> {
            if (task.isSuccessful()) {
                QuerySnapshot querySnapshot = task.getResult();
                if (querySnapshot != null && !querySnapshot.isEmpty()) {
                    for (QueryDocumentSnapshot document : querySnapshot) {
                        String value = document.getString(fieldName);
                        if (value != null && !value.isEmpty()) dataList.add(value);
                    }
                    if (adapter != null) adapter.notifyDataSetChanged();
                } else {
                    if (getContext() != null) Toast.makeText(getContext(), "Không tìm thấy " + logTagPrefix.toLowerCase() + ".", Toast.LENGTH_SHORT).show();
                }
            } else {
                if (getContext() != null) Toast.makeText(getContext(), "Lỗi khi tải " + logTagPrefix.toLowerCase() + ".", Toast.LENGTH_SHORT).show();
                Log.w(TAG, "Lỗi khi lấy dữ liệu " + logTagPrefix + ": ", task.getException());
            }
        });
    }

    private void setupItemClickListener(AutoCompleteTextView autoCompleteTextView, String fieldName) {
        autoCompleteTextView.setOnItemClickListener((parent, MView, position, id) -> {
            String selectedItem = (String) parent.getItemAtPosition(position);
            if (getContext() != null) Toast.makeText(requireContext(), "Đã chọn " + fieldName + ": " + selectedItem, Toast.LENGTH_SHORT).show();
        });
    }

    private void setupDropdownToggle(TextInputLayout textInputLayout, AutoCompleteTextView autoCompleteTextView) {
        if (textInputLayout != null && textInputLayout.getEndIconDrawable() != null) {
            textInputLayout.setEndIconOnClickListener(v -> {
                if (!autoCompleteTextView.isPopupShowing()) {
                    autoCompleteTextView.showDropDown();
                } else {
                    autoCompleteTextView.dismissDropDown();
                }
            });
        }
    }

    @Override
    public void onMemberSelected(Member member) {
        if (!isMemberAlreadyAdded(member.getUserId())) {
            selectedProjectMembers.add(member);
            updateSelectedMembersUI();
            Toast.makeText(getContext(), member.getName() + " đã được thêm vào dự án.", Toast.LENGTH_SHORT).show();
        } else {
            Toast.makeText(getContext(), member.getName() + " đã có trong danh sách.", Toast.LENGTH_SHORT).show();
        }
    }

    private boolean isMemberAlreadyAdded(String userId) {
        if (userId == null) return false;
        for (Member m : selectedProjectMembers) {
            if (m.getUserId() != null && m.getUserId().equals(userId)) return true;
        }
        return false;
    }

    private void updateSelectedMembersUI() {
        if (getContext() == null || llSelectedMembersContainer == null) return;
        llSelectedMembersContainer.removeAllViews();
        if (selectedProjectMembers.isEmpty()) {
            llSelectedMembersContainer.setVisibility(View.GONE);
            return;
        }
        llSelectedMembersContainer.setVisibility(View.VISIBLE);
        LayoutInflater inflater = LayoutInflater.from(getContext());
        for (int i = 0; i < selectedProjectMembers.size(); i++) {
            Member member = selectedProjectMembers.get(i);
            if (member == null) continue;
            View memberView = inflater.inflate(R.layout.item_selected_member, llSelectedMembersContainer, false);
            ImageView ivAvatar = memberView.findViewById(R.id.iv_selected_member_avatar);
            TextView tvName = memberView.findViewById(R.id.tv_selected_member_name);
            TextView tvClass = memberView.findViewById(R.id.tv_selected_member_class);
            ImageView ivRemove = memberView.findViewById(R.id.iv_remove_member);
            tvName.setText(member.getName() != null ? member.getName() : "N/A");
            tvClass.setText(member.getClassName() != null ? member.getClassName() : "N/A");
            Glide.with(this).load(member.getAvatarUrl()).placeholder(R.drawable.ic_default_avatar).error(R.drawable.ic_default_avatar).into(ivAvatar);
            final int memberIndexToRemove = i;
            ivRemove.setOnClickListener(v -> {
                if (memberIndexToRemove < selectedProjectMembers.size()) {
                    Member removedMember = selectedProjectMembers.remove(memberIndexToRemove);
                    updateSelectedMembersUI();
                    Toast.makeText(getContext(), "Đã xóa " + (removedMember.getName() != null ? removedMember.getName() : "thành viên"), Toast.LENGTH_SHORT).show();
                }
            });
            llSelectedMembersContainer.addView(memberView);
        }
    }
}