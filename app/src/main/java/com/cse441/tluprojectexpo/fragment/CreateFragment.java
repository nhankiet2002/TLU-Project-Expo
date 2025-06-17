// CreateFragment.java
package com.cse441.tluprojectexpo.fragment; // THAY ĐỔI PACKAGE CHO ĐÚNG


import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar; // Import ProgressBar
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.viewpager.widget.ViewPager;

import com.bumptech.glide.Glide;
import com.cse441.tluprojectexpo.Project.fragment.AddMemberDialogFragment;
import com.cse441.tluprojectexpo.Project.responsibility.ui.AddedLinksUiManager;
import com.cse441.tluprojectexpo.Project.responsibility.ui.MediaGalleryUiManager;
import com.cse441.tluprojectexpo.Project.responsibility.ui.SelectedMembersUiManager;
import com.cse441.tluprojectexpo.Project.responsibility.util.UiHelper;
import com.cse441.tluprojectexpo.Project.responsibility.util.CloudinaryUploadService;
import com.cse441.tluprojectexpo.Project.responsibility.util.FirestoreFetchService;
import com.cse441.tluprojectexpo.Project.responsibility.util.ImagePickerDelegate;
import com.cse441.tluprojectexpo.Project.responsibility.util.PermissionManager;
import com.cse441.tluprojectexpo.Project.responsibility.util.ProjectCreationService;
import com.cse441.tluprojectexpo.Project.responsibility.form.ProjectFormUiManager;
import com.cse441.tluprojectexpo.Project.responsibility.form.ProjectFormDelegate;
import com.cse441.tluprojectexpo.R;
import com.cse441.tluprojectexpo.model.LinkItem;
import com.cse441.tluprojectexpo.model.User;



import com.google.android.flexbox.FlexboxLayout;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;
import com.google.firebase.Timestamp;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
// FirebaseFirestore không cần import trực tiếp ở đây nếu dùng FirestoreHelper

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.HashSet;

public class CreateFragment extends Fragment implements
        AddMemberDialogFragment.AddUserDialogListener, // Listener từ dialog thêm thành viên
        SelectedMembersUiManager.OnMemberInteractionListener, // Listener từ UI quản lý thành viên
        AddedLinksUiManager.OnLinkInteractionListener,       // Listener từ UI quản lý link
        MediaGalleryUiManager.OnMediaInteractionListener,   // Listener từ UI quản lý media
        ProjectCreationService.ProjectCreationListener {   // Listener từ service tạo dự án

    private static final String TAG = "CreateFragment";
    // !!! THAY THẾ BẰNG UPLOAD PRESET THỰC TẾ CỦA BẠN !!!
    private static final String CLOUDINARY_UPLOAD_PRESET_THUMBNAIL = "TLUProjectExpo";
    private static final String CLOUDINARY_UPLOAD_PRESET_MEDIA = "TLUProjectExpo";


    // --- Views ---
    private TextInputEditText etProjectName, etProjectDescription, etTechnology;
    private AutoCompleteTextView actvCategory, actvStatus;
    private TextInputLayout tilProjectName, tilProjectDescription, tilCategory, tilTechnology, tilStatus;
    private ImageView ivBackArrow;
    private FrameLayout flProjectImageContainer;
    private ImageView ivProjectImagePreview, ivProjectImagePlaceholderIcon;
    private MaterialButton btnAddMedia, btnAddMember, btnAddLink, btnCreateProject;
    private FlexboxLayout flexboxMediaPreviewContainer;
    private TextView tvMediaGalleryLabel;
    private LinearLayout llSelectedMembersContainer, llAddedLinksContainer;
    private LinearLayout llMemberSectionRoot, llLinkSectionRoot, llMediaSectionRoot; // Để đồng bộ chiều rộng nút
    private ProgressBar pbCreatingProject; // ProgressBar cho quá trình tạo


    // --- Data Models ---
    private Uri projectImageUri = null;
    private List<Uri> selectedMediaUris = new ArrayList<>();
    private List<User> selectedProjectUsers = new ArrayList<>();
    private Map<String, String> userRolesInProject = new HashMap<>(); // UserId -> Role
    private List<LinkItem> projectLinks = new ArrayList<>();
    private List<String> categoryNameListForDropdown = new ArrayList<>(); // Chỉ tên cho dropdown
    private Map<String, String> categoryNameToIdMap = new HashMap<>(); // Map Name -> ID
    private List<String> statusNameListForDropdown = new ArrayList<>(); // Tên status cho dropdown

    // --- Adapters ---
    private ArrayAdapter<String> categoryAdapter;
    private ArrayAdapter<String> statusAdapter;

    // --- Firebase ---
    private FirebaseAuth mAuth;
    // Không cần FirebaseFirestore instance trực tiếp ở đây nữa, dùng qua Helper

    // --- Helpers and Managers ---
    private PermissionManager permissionManager;
    private ImagePickerDelegate imagePickerDelegate;
    private CloudinaryUploadService cloudinaryUploadService;
    private FirestoreFetchService firestoreFetchService;
    private ProjectCreationService projectCreationService;
    // Các UI Managers
    private SelectedMembersUiManager selectedMembersUiManager;
    private AddedLinksUiManager addedLinksUiManager;
    private MediaGalleryUiManager mediaGalleryUiManager;
    // (Tùy chọn) Trình quản lý và xác thực form tĩnh
    private ProjectFormUiManager projectFormUiManager;
    private ProjectFormDelegate projectFormDelegate;


    private static final int ACTION_PICK_PROJECT_IMAGE = 1;
    private static final int ACTION_PICK_MEDIA = 2;
    private int currentPickerAction;

    public CreateFragment() {}

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mAuth = FirebaseAuth.getInstance();
        initializeLaunchersAndHelpers();
    }

    private void initializeLaunchersAndHelpers() {
        // ActivityResultLaunchers
        ActivityResultLauncher<String[]> permissionLauncher = registerForActivityResult(
                new ActivityResultContracts.RequestMultiplePermissions(), this::handlePermissionResult);
        ActivityResultLauncher<Intent> projectImageLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(), this::handleProjectImageResult);
        ActivityResultLauncher<Intent> mediaLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(), this::handleMediaResult);

        // Helpers
        permissionManager = new PermissionManager(this, permissionLauncher);
        imagePickerDelegate = new ImagePickerDelegate(projectImageLauncher, mediaLauncher);
        cloudinaryUploadService = new CloudinaryUploadService();
        firestoreFetchService = new FirestoreFetchService();
        projectCreationService = new ProjectCreationService(); // FirestoreHelper được dùng bên trong nó
        projectFormDelegate = new ProjectFormDelegate(); // Nếu có
    }

    // --- Callbacks cho ActivityResultLaunchers ---
    private void handlePermissionResult(Map<String, Boolean> permissionsResult) {
        boolean allGranted = true;
        for(Boolean b : permissionsResult.values()) if(!b) allGranted = false;
        if (allGranted) {
            if (currentPickerAction == ACTION_PICK_PROJECT_IMAGE && imagePickerDelegate != null) imagePickerDelegate.launchProjectImagePicker();
            else if (currentPickerAction == ACTION_PICK_MEDIA && imagePickerDelegate != null) imagePickerDelegate.launchMediaPicker();
        } else {
            UiHelper.showToast(getContext(), "Quyền truy cập bộ nhớ bị từ chối.", Toast.LENGTH_SHORT);
        }
    }
    private void handleProjectImageResult(androidx.activity.result.ActivityResult result) {
        if (result.getResultCode() == Activity.RESULT_OK && result.getData() != null) {
            projectImageUri = result.getData().getData();
            if (projectImageUri != null && getContext() != null && ivProjectImagePreview != null && ivProjectImagePlaceholderIcon != null) {
                Glide.with(this).load(projectImageUri).centerCrop().into(ivProjectImagePreview);
                ivProjectImagePreview.setVisibility(View.VISIBLE);
                ivProjectImagePlaceholderIcon.setVisibility(View.GONE);
                if (flProjectImageContainer != null) flProjectImageContainer.setBackground(null);
            }
        }
    }
    private void handleMediaResult(androidx.activity.result.ActivityResult result) {
        if (result.getResultCode() == Activity.RESULT_OK && result.getData() != null) {
            Uri selectedMediaUri = result.getData().getData();
            if (selectedMediaUri != null) {
                selectedMediaUris.add(selectedMediaUri);
                if (mediaGalleryUiManager != null) mediaGalleryUiManager.updateUI();
                UiHelper.showToast(getContext(), "Đã thêm media.", Toast.LENGTH_SHORT);
            }
        }
    }
    // --- Kết thúc Callbacks ---


    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_create, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        initializeViews(view);      // Ánh xạ các views từ XML
        initializeUiManagers();     // Khởi tạo các UI Managers (cần views và context)
        setupAdaptersAndData();     // Setup ArrayAdapter cho dropdowns và fetch data ban đầu
        setupEventListeners();      // Gán listeners cho các nút
        addCurrentUserAsMember();   // Tự động thêm user hiện tại làm thành viên
        UiHelper.synchronizeButtonWidthsAfterLayout(view, this::synchronizeButtonWidths); // Đồng bộ chiều rộng nút
        updateAllUIs();             // Cập nhật giao diện ban đầu của các UI Managers
    }

    private void initializeViews(@NonNull View view) {
        // Ánh xạ tất cả các view cần thiết từ layout
        etProjectName = view.findViewById(R.id.et_project_name);
        etProjectDescription = view.findViewById(R.id.et_project_description);
        tilProjectName = view.findViewById(R.id.til_project_name);
        tilProjectDescription = view.findViewById(R.id.til_project_description);
        actvCategory = view.findViewById(R.id.actv_category);
        etTechnology = view.findViewById(R.id.et_technology);
        actvStatus = view.findViewById(R.id.actv_status);
        tilCategory = view.findViewById(R.id.til_category);
        tilTechnology = view.findViewById(R.id.til_technology);
        tilStatus = view.findViewById(R.id.til_status);
        ivBackArrow = view.findViewById(R.id.iv_back_arrow);
        flProjectImageContainer = view.findViewById(R.id.fl_project_image_container);
        ivProjectImagePreview = view.findViewById(R.id.iv_project_image_preview);
        ivProjectImagePlaceholderIcon = view.findViewById(R.id.iv_project_image_placeholder_icon);
        llMemberSectionRoot = view.findViewById(R.id.ll_member_section_root);
        btnAddMember = view.findViewById(R.id.btn_add_member);
        llSelectedMembersContainer = view.findViewById(R.id.ll_selected_members_container);
        llLinkSectionRoot = view.findViewById(R.id.ll_link_section_root);
        btnAddLink = view.findViewById(R.id.btn_add_link);
        llAddedLinksContainer = view.findViewById(R.id.ll_added_links_container);
        llMediaSectionRoot = view.findViewById(R.id.ll_media_section_root);
        btnAddMedia = view.findViewById(R.id.btn_add_media);
        flexboxMediaPreviewContainer = view.findViewById(R.id.flexbox_media_preview_container);
        tvMediaGalleryLabel = view.findViewById(R.id.tv_media_gallery_label);
        btnCreateProject = view.findViewById(R.id.btn_create_project);

        // Khởi tạo ProgressBar (có thể lấy từ XML hoặc tạo bằng code)
        // Nếu bạn có ProgressBar với ID trong XML:
        // pbCreatingProject = view.findViewById(R.id.your_progress_bar_id);
        // Nếu không, tạo bằng code và thêm vào root view:
        if (pbCreatingProject == null && getContext() != null) { // Chỉ tạo nếu chưa có
            pbCreatingProject = new ProgressBar(getContext(), null, android.R.attr.progressBarStyleLarge);
            ViewGroup.LayoutParams progressBarParams = new ViewGroup.LayoutParams(
                    ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
            // Để ProgressBar ở giữa, root view của fragment nên là RelativeLayout hoặc FrameLayout
            // Hoặc bạn có thể thêm nó vào một container cụ thể
            if (view instanceof ViewGroup) { // Đảm bảo view là một ViewGroup
                // Cần điều chỉnh LayoutParams cho phù hợp với root layout của fragment
                if (view instanceof RelativeLayout) {
                    RelativeLayout.LayoutParams rlParams = new RelativeLayout.LayoutParams(progressBarParams);
                    rlParams.addRule(RelativeLayout.CENTER_IN_PARENT);
                    pbCreatingProject.setLayoutParams(rlParams);
                } else if (view instanceof FrameLayout) {
                    FrameLayout.LayoutParams flParams = new FrameLayout.LayoutParams(progressBarParams);
                    flParams.gravity = android.view.Gravity.CENTER;
                    pbCreatingProject.setLayoutParams(flParams);
                } else {
                    // Nếu là LinearLayout, bạn cần thêm vào một vị trí cụ thể
                    // hoặc bọc Fragment layout trong FrameLayout
                }
                ((ViewGroup) view).addView(pbCreatingProject);
            }
            pbCreatingProject.setVisibility(View.GONE); // Ẩn ban đầu
        }
    }

    private void initializeUiManagers() {
        if (getContext() == null) {
            Log.e(TAG, "Context is null, cannot initialize UI Managers.");
            return;
        }
        // ProjectFormUiManager (quản lý các trường input tĩnh) - TÙY CHỌN
        // projectFormUiManager = new ProjectFormUiManager(getView(), requireContext(),
        //        etProjectName, etProjectDescription, actvCategory, etTechnology, actvStatus,
        //        tilProjectName, tilProjectDescription, tilCategory, tilTechnology, tilStatus);

        if (llSelectedMembersContainer != null) {
            selectedMembersUiManager = new SelectedMembersUiManager(requireContext(), llSelectedMembersContainer,
                    selectedProjectUsers, userRolesInProject, this);
        }
        if (llAddedLinksContainer != null) {
            addedLinksUiManager = new AddedLinksUiManager(requireContext(), llAddedLinksContainer,
                    projectLinks, this);
        }
        if (flexboxMediaPreviewContainer != null && tvMediaGalleryLabel != null) {
            mediaGalleryUiManager = new MediaGalleryUiManager(requireContext(), flexboxMediaPreviewContainer,
                    tvMediaGalleryLabel, selectedMediaUris, this);
        }
    }

    private void setupAdaptersAndData() {
        if (getContext() == null || firestoreFetchService == null) return;

        // Category Adapter
        categoryAdapter = new ArrayAdapter<>(requireContext(), android.R.layout.simple_dropdown_item_1line, categoryNameListForDropdown);
        if (actvCategory != null) actvCategory.setAdapter(categoryAdapter);
        firestoreFetchService.fetchCategories(new FirestoreFetchService.CategoriesFetchListener() {
            @Override
            public void onCategoriesFetched(List<String> fetchedCategoryNames, Map<String, String> fetchedNameToIdMap) {
                if (!isAdded() || getContext() == null) return;
                categoryNameListForDropdown.clear(); categoryNameListForDropdown.addAll(fetchedCategoryNames);
                categoryNameToIdMap.clear(); categoryNameToIdMap.putAll(fetchedNameToIdMap);
                if (categoryAdapter != null) categoryAdapter.notifyDataSetChanged();
            }
            @Override
            public void onError(String errorMessage) {
                if (!isAdded() || getContext() == null) return;
                UiHelper.showToast(getContext(), errorMessage, Toast.LENGTH_SHORT);
            }
        });

        // Status Adapter
        statusNameListForDropdown.clear();
        statusNameListForDropdown.addAll(Arrays.asList(getResources().getStringArray(R.array.project_statuses)));
        statusAdapter = new ArrayAdapter<>(requireContext(), android.R.layout.simple_dropdown_item_1line, statusNameListForDropdown);
        if (actvStatus != null) {
            actvStatus.setAdapter(statusAdapter);
            if (!statusNameListForDropdown.isEmpty()) actvStatus.setText(statusNameListForDropdown.get(0), false); // Set default
        }
    }

    private void setupEventListeners() {
        if (ivBackArrow != null) ivBackArrow.setOnClickListener(v -> navigateToHome());

        if (flProjectImageContainer != null) flProjectImageContainer.setOnClickListener(v -> {
            currentPickerAction = ACTION_PICK_PROJECT_IMAGE;
            if (permissionManager != null && permissionManager.checkAndRequestStoragePermissions()) { // Đổi tên hàm trong PermissionManager
                if (imagePickerDelegate != null) imagePickerDelegate.launchProjectImagePicker();
            }
        });

        if (btnAddMember != null) btnAddMember.setOnClickListener(v -> {
            if (isAdded() && getChildFragmentManager() != null) {
                AddMemberDialogFragment dialog = AddMemberDialogFragment.newInstance();
                dialog.setDialogListener(this); // `this` implement AddUserDialogListener
                dialog.show(getChildFragmentManager(), "AddMemberDialog");
            }
        });

        if (btnAddLink != null) btnAddLink.setOnClickListener(v -> {
            if (getContext() == null) return;
            projectLinks.add(new LinkItem("", getResources().getStringArray(R.array.link_platforms)[0])); // Default platform
            if (addedLinksUiManager != null) addedLinksUiManager.updateUI();
            // Focus logic (tùy chọn)
            if (llAddedLinksContainer != null && llAddedLinksContainer.getChildCount() > 0) {
                View lastLinkView = llAddedLinksContainer.getChildAt(llAddedLinksContainer.getChildCount() - 1);
                TextInputEditText etUrl = lastLinkView.findViewById(R.id.et_added_link_url);
                if (etUrl != null) etUrl.requestFocus();
            }
        });

        if (btnAddMedia != null) btnAddMedia.setOnClickListener(v -> {
            currentPickerAction = ACTION_PICK_MEDIA;
            if (permissionManager != null && permissionManager.checkAndRequestStoragePermissions()) { // Đổi tên hàm
                if (imagePickerDelegate != null) imagePickerDelegate.launchMediaPicker();
            }
        });

        if (btnCreateProject != null) {
            btnCreateProject.setOnClickListener(v -> {
                Log.d(TAG, "Nút Tạo dự án được nhấn.");
                startProjectCreationProcess();
            });
        } else {
            Log.e(TAG, "Lỗi: btnCreateProject là null trong setupEventListeners!");
        }

        UiHelper.setupDropdownToggle(tilCategory, actvCategory);
        UiHelper.setupDropdownToggle(tilStatus, actvStatus);
    }

    private void updateAllUIs() {
        if (selectedMembersUiManager != null) selectedMembersUiManager.updateUI();
        if (mediaGalleryUiManager != null) mediaGalleryUiManager.updateUI();
        if (addedLinksUiManager != null) addedLinksUiManager.updateUI();
    }

    private void addCurrentUserAsMember() {
        FirebaseUser fbCurrentUser = mAuth.getCurrentUser();
        if (fbCurrentUser == null) {
            UiHelper.showToast(getContext(), "Vui lòng đăng nhập để tạo dự án.", Toast.LENGTH_LONG);
            if (btnCreateProject != null) btnCreateProject.setEnabled(false);
            return;
        }
        if (btnCreateProject != null) btnCreateProject.setEnabled(true);
        String currentUserId = fbCurrentUser.getUid();

        if (!isUserAlreadyAdded(currentUserId)) {
            // Sử dụng FirestoreFetchService để lấy chi tiết user
            if (firestoreFetchService != null) {
                firestoreFetchService.fetchUserDetails(currentUserId, new FirestoreFetchService.UserDetailsFetchListener() {
                    @Override
                    public void onUserDetailsFetched(User user) {
                        if (!isAdded()) return;
                        userRolesInProject.put(currentUserId, "Trưởng nhóm");
                        selectedProjectUsers.add(0, user);
                        if (selectedMembersUiManager != null) selectedMembersUiManager.updateUI();
                    }
                    @Override
                    public void onUserNotFound() { // Fallback nếu user không có trong "Users" collection
                        if (!isAdded()) return;
                        Log.w(TAG, "Current user doc not found in Firestore. Using basic FirebaseUser info.");
                        User fallbackUser = new User(); fallbackUser.setUserId(currentUserId);
                        fallbackUser.setFullName(fbCurrentUser.getDisplayName() != null && !fbCurrentUser.getDisplayName().isEmpty() ? fbCurrentUser.getDisplayName() : (fbCurrentUser.getEmail() != null ? fbCurrentUser.getEmail().split("@")[0] : "User"));
                        fallbackUser.setAvatarUrl(fbCurrentUser.getPhotoUrl() != null ? fbCurrentUser.getPhotoUrl().toString() : null);
                        fallbackUser.setUserClass("N/A");
                        userRolesInProject.put(currentUserId, "Trưởng nhóm"); selectedProjectUsers.add(0, fallbackUser);
                        if (selectedMembersUiManager != null) selectedMembersUiManager.updateUI();
                    }
                    @Override
                    public void onError(String errorMessage) {
                        if (!isAdded()) return;
                        Log.e(TAG, "Error fetching current user details: " + errorMessage);
                        UiHelper.showToast(getContext(), "Lỗi tải thông tin người dùng.", Toast.LENGTH_SHORT);
                        // Vẫn thêm user với thông tin cơ bản
                        User fallbackUser = new User(); fallbackUser.setUserId(currentUserId);
                        fallbackUser.setFullName(fbCurrentUser.getDisplayName() != null && !fbCurrentUser.getDisplayName().isEmpty() ? fbCurrentUser.getDisplayName() : (fbCurrentUser.getEmail() != null ? fbCurrentUser.getEmail().split("@")[0] : "User"));
                        fallbackUser.setAvatarUrl(fbCurrentUser.getPhotoUrl() != null ? fbCurrentUser.getPhotoUrl().toString() : null);
                        fallbackUser.setUserClass("N/A");
                        userRolesInProject.put(currentUserId, "Trưởng nhóm"); selectedProjectUsers.add(0, fallbackUser);
                        if (selectedMembersUiManager != null) selectedMembersUiManager.updateUI();
                    }
                });
            }
        } else {
            // User đã có trong list, đảm bảo vai trò là Trưởng nhóm
            userRolesInProject.put(currentUserId, "Trưởng nhóm");
            if (selectedMembersUiManager != null) selectedMembersUiManager.updateUI();
        }
    }


    private void startProjectCreationProcess() {
        Log.d(TAG, "startProjectCreationProcess called.");
        FirebaseUser currentUser = mAuth.getCurrentUser();
        if (currentUser == null) {
            UiHelper.showInfoDialog(getContext(), "Lỗi", "Vui lòng đăng nhập để tạo dự án.");
            return;
        }

        // Lấy dữ liệu từ form (sử dụng projectFormUiManager nếu có, hoặc lấy trực tiếp)
        String projectName = getTextFromInput(etProjectName); // Sử dụng hàm tiện ích
        String projectDescription = getTextFromInput(etProjectDescription);
        String categoryName = getTextFromInput(actvCategory);
        String technologyInput = getTextFromInput(etTechnology);
        String statusValue = getTextFromInput(actvStatus);

        // Validate dữ liệu (sử dụng projectFormValidator nếu có)
        if (TextUtils.isEmpty(projectName)) {
            if (tilProjectName != null) tilProjectName.setError("Tên dự án không được để trống.");
            UiHelper.showInfoDialog(getContext(), "Thông tin chưa đầy đủ", "Bạn chưa điền tên dự án.");
            return;
        } else {
            if (tilProjectName != null) tilProjectName.setError(null);
        }


        showCreatingProgress(true, "Đang xử lý dữ liệu...");
        if (btnCreateProject != null) btnCreateProject.setEnabled(false);


        cloudinaryUploadService.uploadThumbnail(projectImageUri, CLOUDINARY_UPLOAD_PRESET_THUMBNAIL, "project_thumbnails_expo",
                new CloudinaryUploadService.ThumbnailUploadListener() {
                    @Override
                    public void onThumbnailUploadSuccess(String thumbnailUrl) {
                        if (!isAdded()) return;
                        cloudinaryUploadService.uploadMultipleMedia(selectedMediaUris, CLOUDINARY_UPLOAD_PRESET_MEDIA, "project_media_expo",
                                new CloudinaryUploadService.MediaUploadListener() {
                                    @Override
                                    public void onAllMediaUploaded(List<String> uploadedMediaUrls) {
                                        if (!isAdded()) return;
                                        prepareAndExecuteProjectCreation(currentUser, projectName, projectDescription, categoryName, technologyInput, statusValue, thumbnailUrl, uploadedMediaUrls);
                                    }
                                    @Override
                                    public void onMediaUploadItemSuccess(String url, int currentIndex, int totalCount) {
                                        // Có thể cập nhật UI tiến trình chi tiết ở đây nếu muốn
                                    }
                                    @Override
                                    public void onMediaUploadItemError(String errorMessage, Uri erroredUri, int currentIndex, int totalCount) {
                                        if (!isAdded()) return;
                                        UiHelper.showToast(getContext(), "Lỗi tải media: " + erroredUri.getLastPathSegment(), Toast.LENGTH_SHORT);
                                        // Dù có lỗi item, vẫn tiếp tục finalize khi tất cả đã được xử lý
                                    }
                                    @Override
                                    public void onMediaUploadProgress(int processedCount, int totalCount) {
                                        if (!isAdded()) return;
                                        showCreatingProgress(true, "Đang tải media (" + processedCount + "/" + totalCount + ")");
                                    }
                                });
                    }
                    @Override
                    public void onThumbnailUploadError(String errorMessage) {
                        if (!isAdded()) return;
                        showCreatingProgress(false, null);
                        if (btnCreateProject != null) btnCreateProject.setEnabled(true);
                        UiHelper.showInfoDialog(getContext(), "Lỗi tải ảnh bìa", "Không thể tải ảnh bìa: " + errorMessage);
                    }
                });
    }

    private void prepareAndExecuteProjectCreation(FirebaseUser currentUser, String projectName, String projectDescription,
                                                  String categoryName, String technologyInput, String statusValue,
                                                  String finalThumbnailUrl, List<String> finalMediaUrls) {
        if (!isAdded() || getContext() == null) {
            if (btnCreateProject != null) btnCreateProject.setEnabled(true);
            showCreatingProgress(false, null);
            return;
        }
        showCreatingProgress(true, "Đang lưu dự án...");

        Map<String, Object> projectData = new HashMap<>();
        projectData.put("Title", projectName);
        projectData.put("Description", projectDescription);
        projectData.put("Status", TextUtils.isEmpty(statusValue) ? (statusNameListForDropdown.isEmpty() ? "Đang thực hiện" : statusNameListForDropdown.get(0)) : statusValue);
        projectData.put("ThumbnailUrl", finalThumbnailUrl);
        projectData.put("CreatorUserId", currentUser.getUid());
        projectData.put("CreatedAt", Timestamp.now());
        projectData.put("IsApproved", false); // Mặc định là false
        projectData.put("VoteCount", 0);

        if (finalMediaUrls != null && !finalMediaUrls.isEmpty()) {
            projectData.put("ImageUrl", finalMediaUrls.get(0)); // Ảnh chi tiết chính
            // projectData.put("MediaGalleryUrls", finalMediaUrls); // Nếu muốn lưu tất cả
        } else if (finalThumbnailUrl != null) {
            projectData.put("ImageUrl", finalThumbnailUrl);
        }

        List<String> keywords = new ArrayList<>();
        if (!TextUtils.isEmpty(projectName)) keywords.addAll(generateSubstrings(projectName.toLowerCase()));
        if (!TextUtils.isEmpty(categoryName)) keywords.add(categoryName.toLowerCase());
        if (!TextUtils.isEmpty(technologyInput)) { String[] techs = technologyInput.toLowerCase().split("\\s*,\\s*"); for (String tech : techs) if (!tech.trim().isEmpty()) keywords.add(tech.trim()); }
        projectData.put("SearchKeywords", new ArrayList<>(new HashSet<>(keywords)));

        for (Map<String, String> linkMap : getValidProjectLinks()) {
            String platform = linkMap.get("platform"); String url = linkMap.get("url"); if (url == null || url.trim().isEmpty() || platform == null) continue; platform = platform.toLowerCase();
            switch (platform) {
                case "github": case "gitlab": case "bitbucket": if (projectData.get("ProjectUrl") == null) projectData.put("ProjectUrl", url); break;
                case "demo": case "website": if (projectData.get("DemoUrl") == null) projectData.put("DemoUrl", url); break;
                case "youtube": case "vimeo": if (projectData.get("VideoUrl") == null) projectData.put("VideoUrl", url); break;
            }
        }

        // Gọi ProjectCreationService để lưu tất cả lên Firestore
        String categoryIdToSave = categoryNameToIdMap.get(categoryName); // Lấy ID category

        List<Map<String, Object>> membersForFirestore = new ArrayList<>();
        for(User u : selectedProjectUsers) {
            if (u.getUserId() == null) continue;
            Map<String, Object> memberMap = new HashMap<>();
            memberMap.put("UserId", u.getUserId());
            memberMap.put("RoleInProject", userRolesInProject.get(u.getUserId()));
            membersForFirestore.add(memberMap);
        }
        List<String> techNamesToProcess = new ArrayList<>();
        if (!TextUtils.isEmpty(technologyInput)) {
            techNamesToProcess.addAll(Arrays.asList(technologyInput.split("\\s*,\\s*")));
        }


        projectCreationService.createNewProject(projectData, membersForFirestore, categoryIdToSave, techNamesToProcess, this);
    }


    // --- Implement ProjectCreationListener ---
    @Override
    public void onProjectCreatedSuccessfully(String newProjectId) {
        if (!isAdded()) return;
        Log.d(TAG, "Project and sub-data created successfully with ID: " + newProjectId);
        showCreatingProgress(false, null); // Ẩn progress
        if (btnCreateProject != null) btnCreateProject.setEnabled(true);
        showSuccessDialogAndNavigate();
    }

    @Override
    public void onProjectCreationFailed(String errorMessage) {
        if (!isAdded()) return;
        Log.e(TAG, "Project creation failed: " + errorMessage);
        showCreatingProgress(false, null); // Ẩn progress
        if (btnCreateProject != null) btnCreateProject.setEnabled(true);
        UiHelper.showInfoDialog(getContext(), "Lỗi", "Tạo dự án thất bại: " + errorMessage);
    }
    @Override
    public void onSubTaskError(String warningMessage) {
        if (!isAdded()) return;
        Log.w(TAG, "Project created but sub-task had an error: " + warningMessage);
        showCreatingProgress(false, null); // Ẩn progress
        if (btnCreateProject != null) btnCreateProject.setEnabled(true);
        // Vẫn hiển thị dialog thành công chính, nhưng có thể kèm cảnh báo này
        // Hoặc chỉ log và để người dùng tự kiểm tra/sửa sau
        UiHelper.showInfoDialog(getContext(),"Lưu ý", warningMessage);
        // Vẫn clear form và điều hướng vì project chính đã được tạo
        clearFormAndNavigateToProfile();
    }
    // --- Kết thúc ProjectCreationListener ---


    private void showCreatingProgress(boolean show, @Nullable String message) {
        if (!isAdded() || getContext() == null) return;
        if (pbCreatingProject != null) pbCreatingProject.setVisibility(show ? View.VISIBLE : View.GONE);
        if (show && message != null) UiHelper.showToast(getContext(), message, Toast.LENGTH_SHORT);
        if (btnCreateProject != null) {
            btnCreateProject.setEnabled(!show);
            btnCreateProject.setText(show ? "Đang xử lý..." : "Tạo dự án");
        }
    }
    private void showSuccessDialogAndNavigate() {
        if (!isAdded() || getContext() == null) return;
        UiHelper.showInfoDialog(getContext(), "Thành công", "Tạo dự án thành công!");
        clearFormAndNavigateToProfile();
    }
    private void clearFormAndNavigateToProfile() { clearForm(); navigateToProfile(); }
    private void navigateToProfile() { if (getActivity() != null) { ViewPager viewPager = getActivity().findViewById(R.id.view_pager); if (viewPager != null) { int profileFragmentIndex = 3; viewPager.setCurrentItem(profileFragmentIndex, true); } else if (isAdded() && getParentFragmentManager().getBackStackEntryCount() > 0) { UiHelper.showToast(getContext(), "Chuyển qua tab Profile.", Toast.LENGTH_SHORT); } } }
    private void navigateToHome() { if (getActivity() != null) { ViewPager viewPager = getActivity().findViewById(R.id.view_pager); if (viewPager != null) viewPager.setCurrentItem(0, true); } }
    private void clearForm() {
        if(etProjectName != null) etProjectName.setText(""); if(tilProjectName != null) tilProjectName.setError(null);
        if(etProjectDescription != null) etProjectDescription.setText(""); if(actvCategory != null) actvCategory.setText("", false);
        if(tilCategory != null) tilCategory.setError(null); if(etTechnology != null) etTechnology.setText("");
        if(actvStatus != null && statusAdapter != null && !statusNameListForDropdown.isEmpty()) actvStatus.setText(statusNameListForDropdown.get(0), false);
        else if (actvStatus != null) actvStatus.setText("", false);
        projectImageUri = null; if(ivProjectImagePreview != null) ivProjectImagePreview.setVisibility(View.GONE);
        if(ivProjectImagePlaceholderIcon != null) ivProjectImagePlaceholderIcon.setVisibility(View.VISIBLE);
        if (flProjectImageContainer != null && getContext() != null) try { flProjectImageContainer.setBackground(ContextCompat.getDrawable(requireContext(), R.drawable.image_placeholder_square));} catch (Exception e) {Log.e(TAG, "Error setting placeholder bg", e);}
        selectedProjectUsers.clear(); userRolesInProject.clear(); projectLinks.clear(); selectedMediaUris.clear();
        addCurrentUserAsMember(); // Gọi lại để reset và thêm user hiện tại
        updateAllUIs(); // Gọi để các UI manager làm mới giao diện của chúng
        if(etProjectName != null) etProjectName.requestFocus();
    }

    // --- Callbacks từ các UI Manager và Dialogs ---
    @Override public void onMemberRemoved(User user, int index) { if (index < selectedProjectUsers.size()) { User removedUser = selectedProjectUsers.remove(index); userRolesInProject.remove(removedUser.getUserId()); if (selectedMembersUiManager != null) selectedMembersUiManager.updateUI(); UiHelper.showToast(getContext(), "Đã xóa " + removedUser.getFullName(), Toast.LENGTH_SHORT); } }
    @Override public void onMemberRoleChanged(User user, String newRole, int index) { if (user != null && user.getUserId() != null && index < selectedProjectUsers.size()) userRolesInProject.put(user.getUserId(), newRole); }
    @Override public void onLinkRemoved(LinkItem linkItem, int index) { if (index < projectLinks.size()) { projectLinks.remove(index); if (addedLinksUiManager != null) addedLinksUiManager.updateUI(); UiHelper.showToast(getContext(), "Đã xóa liên kết", Toast.LENGTH_SHORT); } }
    @Override public void onLinkUrlChanged(LinkItem linkItem, String newUrl, int index) { if (linkItem != null && index < projectLinks.size()) projectLinks.get(index).setUrl(newUrl); }
    @Override public void onLinkPlatformChanged(LinkItem linkItem, String newPlatform, int index) { if (linkItem != null && index < projectLinks.size()) projectLinks.get(index).setPlatform(newPlatform); }
    @Override public void onMediaRemoved(Uri uri, int index) { if (index < selectedMediaUris.size()) { selectedMediaUris.remove(index); if (mediaGalleryUiManager != null) mediaGalleryUiManager.updateUI(); UiHelper.showToast(getContext(), "Đã xóa media.", Toast.LENGTH_SHORT); } }
    @Override public void onUserSelected(User user) { if (user == null || user.getUserId() == null) { UiHelper.showToast(getContext(), "Lỗi chọn thành viên.", Toast.LENGTH_SHORT); return; } if (!isUserAlreadyAdded(user.getUserId())) { userRolesInProject.put(user.getUserId(), "Thành viên"); selectedProjectUsers.add(user); if(selectedMembersUiManager != null) selectedMembersUiManager.updateUI(); UiHelper.showToast(getContext(), user.getFullName() + " đã thêm.", Toast.LENGTH_SHORT); } else { UiHelper.showToast(getContext(), user.getFullName() + " đã có.", Toast.LENGTH_SHORT); } }

    // --- Các hàm helper còn lại ---
    private boolean isUserAlreadyAdded(String userId) { if (userId == null) return false; for(User u : selectedProjectUsers) if(u.getUserId() != null && u.getUserId().equals(userId)) return true; return false; }
    private List<String> generateSubstrings(String input) { List<String> substrings = new ArrayList<>(); if (input == null || input.trim().isEmpty()) return substrings; String[] words = input.split("\\s+"); for (String word : words) { if (word.length() < 2) { if(!word.isEmpty()) substrings.add(word); continue; } for (int i = 0; i < word.length(); i++) for (int j = i + 1; j <= word.length(); j++) if (word.substring(i, j).length() >= 1) substrings.add(word.substring(i, j)); } if (!input.trim().isEmpty() && !substrings.contains(input.trim())) substrings.add(input.trim()); return substrings; }
    public List<Map<String, String>> getValidProjectLinks() { List<Map<String, String>> validLinksData = new ArrayList<>(); for (LinkItem linkItem : projectLinks) { String url = linkItem.getUrl() != null ? linkItem.getUrl().trim() : ""; String platform = linkItem.getPlatform() != null ? linkItem.getPlatform().trim() : "Khác"; if (!url.isEmpty() && android.util.Patterns.WEB_URL.matcher(url).matches()) { Map<String, String> linkMap = new HashMap<>(); linkMap.put("url", url); linkMap.put("platform", platform); validLinksData.add(linkMap); } else if (!url.isEmpty()){ Log.w(TAG, "URL không hợp lệ: " + url); } } return validLinksData; }

    private void synchronizeButtonWidths() { if (llMemberSectionRoot == null || llLinkSectionRoot == null || llMediaSectionRoot == null) return; int maxWidth = UiHelper.getMaxWidth(llMemberSectionRoot, llLinkSectionRoot, llMediaSectionRoot); if (maxWidth > 0) { if (llMemberSectionRoot.getVisibility() == View.VISIBLE) UiHelper.setViewWidth(llMemberSectionRoot, maxWidth); if (llLinkSectionRoot.getVisibility() == View.VISIBLE) UiHelper.setViewWidth(llLinkSectionRoot, maxWidth); if (llMediaSectionRoot.getVisibility() == View.VISIBLE) UiHelper.setViewWidth(llMediaSectionRoot, maxWidth); } }
    private String getTextFromInput(TextInputEditText editText) { if (editText != null && editText.getText() != null) return editText.getText().toString().trim(); return ""; }
    private String getTextFromInput(AutoCompleteTextView autoCompleteTextView) { if (autoCompleteTextView != null && autoCompleteTextView.getText() != null) return autoCompleteTextView.getText().toString().trim(); return ""; }


    @Override
    public void onDestroyView() {
        super.onDestroyView();
        if (pbCreatingProject != null && getView() instanceof ViewGroup) {
            ((ViewGroup) getView()).removeView(pbCreatingProject);
        }
        pbCreatingProject = null;
        etProjectName=null; etProjectDescription=null; etTechnology=null; actvCategory=null; actvStatus=null;
        tilCategory=null; tilStatus=null; tilTechnology=null; tilProjectName=null; tilProjectDescription=null;
        ivBackArrow=null; flProjectImageContainer=null; ivProjectImagePreview=null; ivProjectImagePlaceholderIcon=null;
        btnAddMedia=null; btnAddMember=null; btnAddLink=null; btnCreateProject=null;
        flexboxMediaPreviewContainer=null; tvMediaGalleryLabel=null; llSelectedMembersContainer=null;
        llAddedLinksContainer=null; llMemberSectionRoot=null; llLinkSectionRoot=null; llMediaSectionRoot=null;
        // Không cần gán null cho các helper vì chúng được khởi tạo trong onCreate
    }
}