package com.cse441.tluprojectexpo.ui.editproject;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.util.Log;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.OnBackPressedCallback;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.bumptech.glide.Glide;
import com.cse441.tluprojectexpo.R;
import com.cse441.tluprojectexpo.admin.utils.AppToast;
import com.cse441.tluprojectexpo.model.LinkItem;
import com.cse441.tluprojectexpo.model.Project;
import com.cse441.tluprojectexpo.model.User;
import com.cse441.tluprojectexpo.model.Notification;
import com.cse441.tluprojectexpo.repository.NotificationRepository;
import com.cse441.tluprojectexpo.repository.ProjectDeletionManager;
import com.cse441.tluprojectexpo.repository.ProjectFormManager;
import com.cse441.tluprojectexpo.repository.ProjectNotificationManager;
import com.cse441.tluprojectexpo.repository.ProjectRepository;
import com.cse441.tluprojectexpo.repository.ProjectSaveManager;
import com.cse441.tluprojectexpo.service.CloudinaryUploadService;
import com.cse441.tluprojectexpo.service.FirestoreService;
import com.cse441.tluprojectexpo.ui.createproject.AddMemberDialogFragment;
import com.cse441.tluprojectexpo.ui.common.uimanager.AddedLinksUiManager;
import com.cse441.tluprojectexpo.ui.common.uimanager.MediaGalleryUiManager;
import com.cse441.tluprojectexpo.ui.common.uimanager.SelectedMembersUiManager;
import com.cse441.tluprojectexpo.utils.ImagePickerDelegate;
import com.cse441.tluprojectexpo.utils.PermissionManager;
import com.cse441.tluprojectexpo.utils.UiHelper;

import com.google.android.flexbox.FlexboxLayout;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.chip.Chip;
import com.google.android.material.chip.ChipGroup;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import com.cse441.tluprojectexpo.utils.Constants;

public class EditProjectActivity extends AppCompatActivity implements
        AddMemberDialogFragment.AddUserDialogListener,
        SelectedMembersUiManager.OnMemberInteractionListener,
        AddedLinksUiManager.OnLinkInteractionListener,
        MediaGalleryUiManager.OnMediaInteractionListener {

    private static final String TAG = "EditProjectActivity";
    public static final String EXTRA_PROJECT_ID = "PROJECT_ID";

    // UI Components
    private TextInputEditText etProjectName, etProjectDescription;
    private AutoCompleteTextView actvCategoryInput, actvStatus, actvTechnologyInput;
    private TextInputLayout tilProjectName, tilProjectDescription, tilCategory, tilTechnologyInput, tilStatus;
    private ChipGroup chipGroupCategories, chipGroupTechnologies;
    private FrameLayout flProjectImageContainer;
    private ImageView ivProjectImagePreview, ivProjectImagePlaceholderIcon;
    private ImageView ivBackArrow;
    private TextView tvTitle;
    private MaterialButton btnAddMedia, btnAddMember, btnAddLink, btnSaveChanges, btnDeleteProject;
    private FlexboxLayout flexboxMediaPreviewContainer;
    private TextView tvMediaGalleryLabel;
    private LinearLayout llSelectedMembersContainer, llAddedLinksContainer;
    private LinearLayout llMemberSectionRoot, llLinkSectionRoot, llMediaSectionRoot;
    private ProgressBar pbEditingProject;

    // Data
    private Uri projectImageUri = null;
    private List<Uri> selectedMediaUris = new ArrayList<>();
    private List<User> selectedProjectUsers = new ArrayList<>();
    private Map<String, String> userRolesInProject = new HashMap<>();
    private List<LinkItem> projectLinks = new ArrayList<>();
    private List<String> selectedCategoryNames = new ArrayList<>();
    private List<String> selectedTechnologyNames = new ArrayList<>();
    
    // Track original members for notification purposes
    private List<User> originalProjectUsers = new ArrayList<>();
    private Map<String, String> originalUserRoles = new HashMap<>();
    
    // Track existing media URLs (from current project) vs new media URIs (to be uploaded)
    private List<Map<String, String>> existingMediaUrls = new ArrayList<>();
    private List<Uri> newMediaUris = new ArrayList<>();

    // Categories and Technologies
    private List<String> categoryNameListForDropdown = new ArrayList<>();
    private Map<String, String> categoryNameToIdMap = new HashMap<>();
    private ArrayAdapter<String> categoryAdapter;
    private String selectedCategoryId = null;

    private List<String> allAvailableTechnologyNames = new ArrayList<>();
    private Map<String, String> technologyNameToIdMap = new HashMap<>();
    private Map<String, String> technologyIdToNameMap = new HashMap<>();

    private List<String> statusNameListForDropdown = new ArrayList<>();
    private ArrayAdapter<String> statusAdapter;

    // Services
    private FirebaseAuth mAuth;
    private FirebaseFirestore db;
    private FirestoreService firestoreService;
    private ProjectRepository projectRepository;
    private NotificationRepository notificationRepository;
    private PermissionManager permissionManager;
    private ActivityResultLauncher<String[]> permissionLauncher;
    private ImagePickerDelegate imagePickerDelegate;
    private CloudinaryUploadService cloudinaryUploadService;

    // UI Managers
    private SelectedMembersUiManager selectedMembersUiManager;
    private AddedLinksUiManager addedLinksUiManager;
    private MediaGalleryUiManager mediaGalleryUiManager;

    private static final int ACTION_PICK_PROJECT_IMAGE = 1;
    private static final int ACTION_PICK_MEDIA = 2;
    private int currentPickerAction;
    private boolean hasUserMadeChanges = false;

    private String currentProjectId;
    private Project currentProject;

    // Managers
    private ProjectFormManager formManager;
    private ProjectSaveManager saveManager;
    private ProjectNotificationManager notificationManager;
    private ProjectDeletionManager deletionManager;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_edit_project);

        currentProjectId = getIntent().getStringExtra(EXTRA_PROJECT_ID);
        if (currentProjectId == null || currentProjectId.isEmpty()) {
            AppToast.show(this, "ID dự án không hợp lệ.", Toast.LENGTH_LONG);
            finish();
            return;
        }

        initializeManagersAndServices();
        initializeLaunchersAndHelpers();
        initializeViews();
        initializeUiManagers();
        setupAdaptersAndData();
        setupEventListeners();
        setupInputValidationListeners();

        loadProjectDetails();

        getOnBackPressedDispatcher().addCallback(this, new OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
                handleCustomBackPressed();
            }
        });
    }

    private void initializeManagersAndServices() {
        formManager = new ProjectFormManager(this);
        saveManager = new ProjectSaveManager(this, formManager);
        notificationManager = new ProjectNotificationManager();
        deletionManager = new ProjectDeletionManager();
        firestoreService = new FirestoreService();
        projectRepository = new ProjectRepository();
    }

    private void initializeLaunchersAndHelpers() {
        permissionLauncher = registerForActivityResult(new ActivityResultContracts.RequestMultiplePermissions(), this::handlePermissionResult);
        
        ActivityResultLauncher<Intent> projectImageLauncher = registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), this::handleProjectImageResult);
        ActivityResultLauncher<Intent> mediaLauncher = registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), this::handleMediaResult);
        
        permissionManager = new PermissionManager(this, permissionLauncher);
        imagePickerDelegate = new ImagePickerDelegate(projectImageLauncher, mediaLauncher);
    }

    private void initializeViews() {
        tvTitle = findViewById(R.id.tv_title);
        if (tvTitle != null) tvTitle.setText("Chỉnh sửa Dự án");

        etProjectName = findViewById(R.id.et_project_name);
        etProjectDescription = findViewById(R.id.et_project_description);
        actvCategoryInput = findViewById(R.id.actv_category_input);
        tilCategory = findViewById(R.id.til_category);
        chipGroupCategories = findViewById(R.id.chip_group_categories);
        actvStatus = findViewById(R.id.actv_status);
        tilStatus = findViewById(R.id.til_status);
        chipGroupTechnologies = findViewById(R.id.chip_group_technologies);
        actvTechnologyInput = findViewById(R.id.actv_technology_input);
        tilTechnologyInput = findViewById(R.id.til_technology_input);

        flProjectImageContainer = findViewById(R.id.fl_project_image_container);
        ivProjectImagePreview = findViewById(R.id.iv_project_image_preview);
        ivProjectImagePlaceholderIcon = findViewById(R.id.iv_project_image_placeholder_icon);
        ivBackArrow = findViewById(R.id.iv_back_arrow);

        btnAddMedia = findViewById(R.id.btn_add_media);
        btnAddMember = findViewById(R.id.btn_add_member);
        btnAddLink = findViewById(R.id.btn_add_link);
        btnSaveChanges = findViewById(R.id.btn_save_changes);
        btnDeleteProject = findViewById(R.id.btn_delete_project);

        flexboxMediaPreviewContainer = findViewById(R.id.flexbox_media_preview_container);
        tvMediaGalleryLabel = findViewById(R.id.tv_media_gallery_label);
        llSelectedMembersContainer = findViewById(R.id.ll_selected_members_container);
        llAddedLinksContainer = findViewById(R.id.ll_added_links_container);
        llMemberSectionRoot = findViewById(R.id.ll_member_section_root);
        llLinkSectionRoot = findViewById(R.id.ll_link_section_root);
        llMediaSectionRoot = findViewById(R.id.ll_media_section_root);

        pbEditingProject = findViewById(R.id.pb_editing_project);
    }

    private void initializeUiManagers() {
        selectedMembersUiManager = new SelectedMembersUiManager(this, llSelectedMembersContainer, formManager.getSelectedProjectUsers(), formManager.getUserRolesInProject(), this);
        addedLinksUiManager = new AddedLinksUiManager(this, llAddedLinksContainer, formManager.getProjectLinks(), this);
        mediaGalleryUiManager = new MediaGalleryUiManager(this, flexboxMediaPreviewContainer, tvMediaGalleryLabel, formManager.getSelectedMediaUris(), this);
    }

    private void updateAllUIs() {
        if (selectedMembersUiManager != null) selectedMembersUiManager.updateUI();
        if (mediaGalleryUiManager != null) mediaGalleryUiManager.updateUI();
        if (addedLinksUiManager != null) addedLinksUiManager.updateUI();
        updateMediaGalleryLabel();
    }

    private void setupAdaptersAndData() {
        // Category Adapter
        ArrayAdapter<String> categoryAdapter = new ArrayAdapter<>(this, android.R.layout.simple_dropdown_item_1line, new ArrayList<>());
        actvCategoryInput.setAdapter(categoryAdapter);
        formManager.fetchCategories(new FirestoreService.CategoriesFetchListener() {
            @Override
            public void onCategoriesFetched(List<String> fetchedCategoryNames, Map<String, String> fetchedNameToIdMap) {
                formManager.updateCategoryData(fetchedCategoryNames, fetchedNameToIdMap);
                categoryAdapter.clear();
                categoryAdapter.addAll(fetchedCategoryNames);
                categoryAdapter.notifyDataSetChanged();
            }
            @Override
            public void onError(String errorMessage) {
                Log.e(TAG, "Error fetching categories: " + errorMessage);
            }
        });

        // Technology Adapter
        ArrayAdapter<String> technologyAdapter = new ArrayAdapter<>(this, android.R.layout.simple_dropdown_item_1line, new ArrayList<>());
        actvTechnologyInput.setAdapter(technologyAdapter);
        formManager.fetchTechnologies(new FirestoreService.TechnologyFetchListener() {
            @Override
            public void onTechnologiesFetched(List<String> fetchedTechnologyNames, Map<String, String> fetchedTechNameToIdMap) {
                formManager.updateTechnologyData(fetchedTechnologyNames, fetchedTechNameToIdMap);
                technologyAdapter.clear();
                technologyAdapter.addAll(fetchedTechnologyNames);
                technologyAdapter.notifyDataSetChanged();
            }
            @Override
            public void onError(String errorMessage) {
                Log.e(TAG, "Error fetching technologies: " + errorMessage);
            }
        });

        // Status Adapter
        ArrayAdapter<String> statusAdapter = new ArrayAdapter<>(this, android.R.layout.simple_dropdown_item_1line, formManager.getStatusNameListForDropdown());
        actvStatus.setAdapter(statusAdapter);
    }

    private void setupEventListeners() {
        ivBackArrow.setOnClickListener(v -> handleCustomBackPressed());
        flProjectImageContainer.setOnClickListener(v -> onPickProjectImage());
        ivProjectImagePreview.setOnClickListener(v -> onPickProjectImage());
        actvCategoryInput.setOnItemClickListener((parent, view, position, id) -> onCategorySelected(parent.getItemAtPosition(position).toString()));
        actvTechnologyInput.setOnItemClickListener((parent, view, position, id) -> onTechnologySelected(parent.getItemAtPosition(position).toString()));
        btnAddMember.setOnClickListener(v -> onAddMember());
        btnAddLink.setOnClickListener(v -> onAddLink());
        btnAddMedia.setOnClickListener(v -> onAddMedia());
        btnSaveChanges.setOnClickListener(v -> onSaveChanges());
        btnDeleteProject.setOnClickListener(v -> onDeleteProject());
    }
    
    // Event handler methods
    private void onPickProjectImage() {
        if (checkLoginAndNotify("thêm ảnh dự án")) {
            currentPickerAction = ACTION_PICK_PROJECT_IMAGE;
            if (permissionManager != null) {
                permissionManager.checkAndRequestStoragePermissions();
            } else {
                imagePickerDelegate.launchProjectImagePicker();
            }
        }
    }
    
    private void onCategorySelected(String categoryName) {
        if (formManager.addCategory(categoryName)) {
            addChipToGroup(chipGroupCategories, categoryName, () -> formManager.removeCategory(categoryName));
            hasUserMadeChanges = true;
        } else {
            AppToast.show(this, "Không thể thêm lĩnh vực. Đã đạt giới hạn hoặc đã tồn tại.", Toast.LENGTH_SHORT);
        }
        actvCategoryInput.setText("");
        hideKeyboard();
    }
    
    private void onTechnologySelected(String techName) {
        if (formManager.addTechnology(techName)) {
            addChipToGroup(chipGroupTechnologies, techName, () -> formManager.removeTechnology(techName));
            hasUserMadeChanges = true;
        } else {
            AppToast.show(this, "Không thể thêm công nghệ. Đã đạt giới hạn hoặc đã tồn tại.", Toast.LENGTH_SHORT);
        }
        actvTechnologyInput.setText("");
        hideKeyboard();
    }
    
    private void onAddMember() {
        if (checkLoginAndNotify("thêm thành viên")) {
            AddMemberDialogFragment.newInstance().show(getSupportFragmentManager(), "AddMemberDialog");
        }
    }

    private void onAddLink() {
        if (checkLoginAndNotify("thêm liên kết")) {
            boolean githubLinkExists = formManager.getProjectLinks().stream().anyMatch(item -> "GitHub".equalsIgnoreCase(item.getPlatform()));
            boolean demoLinkExists = formManager.getProjectLinks().stream().anyMatch(item -> "Demo".equalsIgnoreCase(item.getPlatform()));

            if (githubLinkExists && demoLinkExists) {
                AppToast.show(this, "Bạn đã thêm đủ liên kết GitHub và Demo.", Toast.LENGTH_SHORT);
                return;
            }

            String platformToAdd = !githubLinkExists ? "GitHub" : "Demo";
            formManager.addLink("", platformToAdd);
            updateAllUIs();
            hasUserMadeChanges = true;
        }
    }

    private void onAddMedia() {
        if (checkLoginAndNotify("thêm media")) {
            if (formManager.getSelectedMediaUris().size() >= 10) {
                AppToast.show(this, "Bạn chỉ có thể thêm tối đa 10 media.", Toast.LENGTH_SHORT);
                return;
            }
            currentPickerAction = ACTION_PICK_MEDIA;
            if (permissionManager != null && permissionManager.checkAndRequestStoragePermissions()) {
                if (imagePickerDelegate != null) imagePickerDelegate.launchMediaPicker();
            }
        }
    }

    private void onSaveChanges() {
        if (checkLoginAndNotify("lưu thay đổi") && validateForm()) {
            promptSaveConfirmation();
        }
    }

    private void onDeleteProject() {
        if (checkLoginAndNotify("xóa dự án")) {
            promptDeleteConfirmation();
        }
    }

    private void setupInputValidationListeners() {
        addTextWatcherToTrackChanges(etProjectName);
        addTextWatcherToTrackChanges(etProjectDescription);
        addTextWatcherToClearError(etProjectName, tilProjectName);
        addTextWatcherToClearError(etProjectDescription, tilProjectDescription);
    }

    private void loadProjectDetails() {
        firestoreService.getProjectById(currentProjectId, new FirestoreService.ProjectFetchListener() {
            @Override
            public void onProjectFetched(Project project) {
                currentProject = project;
                populateUiWithProjectData();
                loadAdditionalProjectData();
            }
            @Override
            public void onError(String errorMessage) {
                AppToast.show(EditProjectActivity.this, "Lỗi tải dự án: " + errorMessage, Toast.LENGTH_LONG);
                finish();
            }
        });
    }

    private void populateUiWithProjectData() {
        if (currentProject == null) return;
        etProjectName.setText(currentProject.getTitle());
        etProjectDescription.setText(currentProject.getDescription());
        actvStatus.setText(currentProject.getStatus(), false);

        if (currentProject.getThumbnailUrl() != null && !currentProject.getThumbnailUrl().isEmpty()) {
            ivProjectImagePreview.setVisibility(View.VISIBLE);
            Glide.with(this).load(currentProject.getThumbnailUrl()).into(ivProjectImagePreview);
            ivProjectImagePlaceholderIcon.setVisibility(View.GONE);
        }
    }

    private void loadAdditionalProjectData() {
        // Categories
        projectRepository.fetchCategoriesForProject(currentProjectId, new ProjectRepository.ProjectRelatedListListener<String>() {
            @Override
            public void onListFetched(List<String> categories) {
                categories.forEach(cat -> {
                    formManager.addCategory(cat);
                    addChipToGroup(chipGroupCategories, cat, () -> formManager.removeCategory(cat));
                });
            }
            @Override public void onListEmpty() {}
            @Override public void onError(String errorMessage) {}
        });

        // Technologies
        projectRepository.fetchTechnologiesForProject(currentProjectId, new ProjectRepository.ProjectRelatedListListener<String>() {
            @Override
            public void onListFetched(List<String> technologies) {
                technologies.forEach(tech -> {
                    formManager.addTechnology(tech);
                    addChipToGroup(chipGroupTechnologies, tech, () -> formManager.removeTechnology(tech));
                });
            }
            @Override public void onListEmpty() {}
            @Override public void onError(String errorMessage) {}
        });

        // Members
        projectRepository.fetchProjectMembers(currentProjectId, new ProjectRepository.ProjectRelatedListListener<Project.UserShortInfo>() {
            @Override
            public void onListFetched(List<Project.UserShortInfo> memberInfos) {
                if (memberInfos == null) return;

                formManager.getSelectedProjectUsers().clear();
                formManager.getUserRolesInProject().clear();
                originalProjectUsers.clear();
                originalUserRoles.clear();

                for (Project.UserShortInfo info : memberInfos) {
                    User user = new User();
                    user.setUserId(info.getUserId());
                    user.setFullName(info.getFullName());
                    user.setAvatarUrl(info.getAvatarUrl());
                    user.setClassName(info.getClassName()); 
                    
                    String role = info.getRoleInProject();
                    
                    if(formManager.addMember(user)){
                        formManager.updateMemberRole(user.getUserId(), role);
                    }

                    originalProjectUsers.add(user);
                    if (user.getUserId() != null && role != null) {
                        originalUserRoles.put(user.getUserId(), role);
                    }
                }
                updateAllUIs();
            }
            @Override public void onListEmpty() {}
            @Override public void onError(String errorMessage) {}
        });

        // Links
        if (currentProject.getProjectUrl() != null) formManager.addLink(currentProject.getProjectUrl(), "GitHub");
        if (currentProject.getDemoUrl() != null) formManager.addLink(currentProject.getDemoUrl(), "Demo");

        // Media
        if (currentProject.getMediaGalleryUrls() != null) {
            currentProject.getMediaGalleryUrls().forEach(mediaItem -> {
                Uri uri = Uri.parse(mediaItem.getUrl());
                formManager.addMedia(uri);
                existingMediaUrls.add(Map.of("url", mediaItem.getUrl(), "type", mediaItem.getType()));
            });
        }
        updateAllUIs();
    }

    private boolean validateForm() {
        boolean isValid = true;
        if (TextUtils.isEmpty(etProjectName.getText())) {
            tilProjectName.setError("Vui lòng nhập tên dự án");
            isValid = false;
        }
        if (TextUtils.isEmpty(etProjectDescription.getText())) {
            tilProjectDescription.setError("Vui lòng nhập mô tả dự án");
            isValid = false;
        }
        if (formManager.getSelectedCategoryNames().isEmpty()) {
            tilCategory.setError("Vui lòng chọn ít nhất một lĩnh vực");
            isValid = false;
        }
        if (TextUtils.isEmpty(actvStatus.getText())) {
            tilStatus.setError("Vui lòng chọn trạng thái");
            isValid = false;
        }
        return isValid;
    }

    private void promptSaveConfirmation() {
        new AlertDialog.Builder(this)
                .setTitle("Lưu thay đổi")
                .setMessage("Bạn có chắc chắn muốn lưu các thay đổi này?")
                .setPositiveButton("Lưu", (dialog, which) -> startSaveProcess())
                .setNegativeButton("Hủy", null)
                .show();
    }

    private void startSaveProcess() {
        showProgress(true, "Đang xử lý dữ liệu...");
        hideKeyboard();

        String projectName = etProjectName.getText().toString().trim();
        String projectDescription = etProjectDescription.getText().toString().trim();
        String status = actvStatus.getText().toString().trim();

        saveManager.updateProject(currentProjectId, projectName, projectDescription, status, currentProject, existingMediaUrls, newMediaUris, new ProjectSaveManager.ProjectSaveListener() {
            @Override
            public void onSuccess(String projectId, boolean wasPreviouslyApproved) {
                showProgress(false, null);
                notificationManager.sendMemberChangeNotifications(originalProjectUsers, originalUserRoles, formManager.getSelectedProjectUsers(), formManager.getUserRolesInProject(), projectId, projectName);
                
                if (wasPreviouslyApproved) {
                    AppToast.show(EditProjectActivity.this, "Cập nhật dự án thành công. Vui lòng chờ xét duyệt dự án.", Toast.LENGTH_LONG);
                } else {
                    AppToast.show(EditProjectActivity.this, "Cập nhật dự án thành công!", Toast.LENGTH_LONG);
                }
                finish();
            }

            @Override
            public void onError(String errorMessage) {
                showProgress(false, null);
                AppToast.show(EditProjectActivity.this, "Lỗi cập nhật: " + errorMessage, Toast.LENGTH_LONG);
            }

            @Override
            public void onProgress(String message) {
                showProgress(true, message);
            }
        });
    }

    private void promptDeleteConfirmation() {
        new AlertDialog.Builder(this)
                .setTitle("Xóa dự án")
                .setMessage("Bạn có chắc chắn muốn xóa dự án này? Hành động này không thể hoàn tác.")
                .setPositiveButton("Xóa", (dialog, which) -> deleteProject())
                .setNegativeButton("Hủy", null)
                .show();
    }

    private void deleteProject() {
        showProgress(true, "Đang xóa dự án và các dữ liệu liên quan...");
        deletionManager.deleteProject(currentProjectId, new ProjectDeletionManager.DeleteCallback() {
            @Override
            public void onSuccess() {
                showProgress(false, null);
                AppToast.show(EditProjectActivity.this, "Đã xóa dự án thành công!", Toast.LENGTH_LONG);
                
                // Trả về kết quả để trang danh sách có thể cập nhật
                Intent resultIntent = new Intent();
                resultIntent.putExtra("project_deleted", true);
                resultIntent.putExtra("deleted_project_id", currentProjectId);
                setResult(Activity.RESULT_OK, resultIntent);
                finish();
            }

            @Override
            public void onError(String errorMessage) {
                showProgress(false, null);
                AppToast.show(EditProjectActivity.this, "Lỗi khi xóa dự án: " + errorMessage, Toast.LENGTH_LONG);
            }
        });
    }

    private void handleCustomBackPressed() {
        if (hasUserMadeChanges) {
            new AlertDialog.Builder(this)
                    .setTitle("Thoát")
                    .setMessage("Bạn có thay đổi chưa lưu. Bạn có chắc chắn muốn thoát?")
                    .setPositiveButton("Thoát", (dialog, which) -> finish())
                    .setNegativeButton("Ở lại", null)
                    .show();
        } else {
            finish();
        }
    }
    
    // Interface implementations
    @Override
    public void onUserSelected(User user) {
        if (formManager.addMember(user)) {
            updateAllUIs();
            hasUserMadeChanges = true;
        }
    }

    @Override
    public void onMemberRemoved(User user, int index) {
        if (formManager.removeMember(user.getUserId())) {
            updateAllUIs();
            hasUserMadeChanges = true;
        }
    }

    @Override
    public void onMemberRoleChanged(User user, String newRole, int index) {
        if (formManager.updateMemberRole(user.getUserId(), newRole)) {
            hasUserMadeChanges = true;
        }
    }

    @Override
    public void onLinkRemoved(LinkItem linkItem, int index) {
        if (formManager.removeLink(index)) {
            updateAllUIs();
            hasUserMadeChanges = true;
        }
    }

    @Override
    public void onLinkUrlChanged(LinkItem linkItem, String newUrl, int index) {
        if(formManager.updateLink(index, newUrl, linkItem.getPlatform())) {
             hasUserMadeChanges = true;
        }
    }

    @Override
    public void onLinkPlatformChanged(LinkItem linkItem, String newPlatform, int index) {
        if(formManager.updateLink(index, linkItem.getUrl(), newPlatform)) {
             hasUserMadeChanges = true;
        }
    }

    @Override
    public void onMediaRemoved(Uri uri, int index) {
        if(formManager.removeMedia(index)) {
            // Also update local tracking lists
            newMediaUris.remove(uri);
            existingMediaUrls.removeIf(media -> uri.toString().equals(media.get("url")));
            
            updateAllUIs();
            hasUserMadeChanges = true;
        }
    }
    
    // Result Handlers
    private void handlePermissionResult(Map<String, Boolean> permissionsResult) {
        boolean allGranted = permissionsResult.values().stream().allMatch(b -> b);
        if (allGranted) {
            if (currentPickerAction == ACTION_PICK_PROJECT_IMAGE) imagePickerDelegate.launchProjectImagePicker();
            else if (currentPickerAction == ACTION_PICK_MEDIA) imagePickerDelegate.launchMediaPicker();
        } else {
            AppToast.show(this, "Quyền truy cập bộ nhớ bị từ chối.", Toast.LENGTH_SHORT);
        }
    }

    private void handleProjectImageResult(androidx.activity.result.ActivityResult result) {
        if (result.getResultCode() == Activity.RESULT_OK && result.getData() != null) {
            Uri uri = result.getData().getData();
            formManager.setProjectImageUri(uri);
            ivProjectImagePreview.setVisibility(View.VISIBLE);
            ivProjectImagePreview.setImageURI(uri);
            ivProjectImagePlaceholderIcon.setVisibility(View.GONE);
            hasUserMadeChanges = true;
        }
    }

    private void handleMediaResult(androidx.activity.result.ActivityResult result) {
        if (result.getResultCode() == Activity.RESULT_OK && result.getData() != null) {
            Uri uri = result.getData().getData();
            if (formManager.addMedia(uri)) {
                newMediaUris.add(uri); // Track new files for upload
                updateAllUIs();
                hasUserMadeChanges = true;
                AppToast.show(this, "Đã thêm media.", Toast.LENGTH_SHORT);
            } else {
                AppToast.show(this, "Media đã tồn tại hoặc đã đạt giới hạn.", Toast.LENGTH_SHORT);
            }
        }
    }
    
    // Helper Methods
    private boolean checkLoginAndNotify(String actionMessage) {
        if (FirebaseAuth.getInstance().getCurrentUser() == null) {
            UiHelper.showInfoDialog(this, "Yêu cầu đăng nhập", "Vui lòng đăng nhập để " + actionMessage + ".");
            return false;
        }
        return true;
    }

    private void addChipToGroup(ChipGroup chipGroup, String text, Runnable onRemove) {
        Chip chip = new Chip(this);
        chip.setText(text);
        chip.setCloseIconVisible(true);
        chip.setOnCloseIconClickListener(v -> {
            chipGroup.removeView(chip);
            onRemove.run();
            hasUserMadeChanges = true;
        });
        chipGroup.addView(chip);
    }
    
    private void updateMediaGalleryLabel() {
        if (tvMediaGalleryLabel != null) {
            int count = formManager.getSelectedMediaUris().size();
            tvMediaGalleryLabel.setText("Thư viện Media" + (count > 0 ? " (" + count + " items)" : ""));
        }
    }
    
    private void showProgress(boolean show, @Nullable String message) {
        pbEditingProject.setVisibility(show ? View.VISIBLE : View.GONE);
        btnSaveChanges.setEnabled(!show);
        btnDeleteProject.setEnabled(!show);
    }

    private void hideKeyboard() {
        InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
        if (imm != null) {
            imm.hideSoftInputFromWindow(getCurrentFocus() != null ? getCurrentFocus().getWindowToken() : null, 0);
        }
    }
    
    private void addTextWatcherToTrackChanges(TextView textView) {
        if (textView != null) {
            textView.addTextChangedListener(new TextWatcher() {
                @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
                @Override public void onTextChanged(CharSequence s, int start, int before, int count) { hasUserMadeChanges = true; }
                @Override public void afterTextChanged(Editable s) {}
            });
        }
    }
    
    private void addTextWatcherToClearError(TextView textView, TextInputLayout textInputLayout) {
        if (textView != null && textInputLayout != null) {
            textView.addTextChangedListener(new TextWatcher() {
                @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
                @Override public void onTextChanged(CharSequence s, int start, int before, int count) {
                    textInputLayout.setError(null);
                }
                @Override public void afterTextChanged(Editable s) {}
            });
        }
    }
} 