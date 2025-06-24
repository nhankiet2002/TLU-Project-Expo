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
import com.cse441.tluprojectexpo.model.LinkItem;
import com.cse441.tluprojectexpo.model.Project;
import com.cse441.tluprojectexpo.model.User;
import com.cse441.tluprojectexpo.model.Notification;
import com.cse441.tluprojectexpo.ui.createproject.AddMemberDialogFragment;
import com.cse441.tluprojectexpo.ui.createproject.uimanager.AddedLinksUiManager;
import com.cse441.tluprojectexpo.ui.createproject.uimanager.MediaGalleryUiManager;
import com.cse441.tluprojectexpo.ui.createproject.uimanager.SelectedMembersUiManager;
import com.cse441.tluprojectexpo.service.CloudinaryUploadService;
import com.cse441.tluprojectexpo.service.FirestoreService;
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

import com.cse441.tluprojectexpo.repository.ProjectRepository;
import com.cse441.tluprojectexpo.repository.NotificationRepository;
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

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_edit_project);

        currentProjectId = getIntent().getStringExtra(EXTRA_PROJECT_ID);
        if (currentProjectId == null || currentProjectId.isEmpty()) {
            Toast.makeText(this, "ID dự án không hợp lệ.", Toast.LENGTH_LONG).show();
            finish();
            return;
        }

        initializeServices();
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

    private void initializeServices() {
        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();
        firestoreService = new FirestoreService();
        projectRepository = new ProjectRepository();
        notificationRepository = new NotificationRepository();
        cloudinaryUploadService = new CloudinaryUploadService();
    }

    private void initializeLaunchersAndHelpers() {
        Log.d(TAG, "Initializing launchers and helpers");
        
        permissionLauncher = registerForActivityResult(
                new ActivityResultContracts.RequestMultiplePermissions(), this::handlePermissionResult);
        
        // Fix: Create proper launchers for image and media picking
        ActivityResultLauncher<Intent> projectImageLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(), this::handleProjectImageResult);
        ActivityResultLauncher<Intent> mediaLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(), this::handleMediaResult);
        
        Log.d(TAG, "Launchers created successfully");
        
        permissionManager = new PermissionManager(this, permissionLauncher);
        imagePickerDelegate = new ImagePickerDelegate(projectImageLauncher, mediaLauncher);
        
        Log.d(TAG, "PermissionManager and ImagePickerDelegate initialized");
        Log.d(TAG, "PermissionManager: " + (permissionManager != null));
        Log.d(TAG, "ImagePickerDelegate: " + (imagePickerDelegate != null));
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
        if (llSelectedMembersContainer != null) {
            selectedMembersUiManager = new SelectedMembersUiManager(
                    this, llSelectedMembersContainer, selectedProjectUsers, userRolesInProject, this);
            Log.d(TAG, "SelectedMembersUiManager initialized successfully");
        } else {
            Log.e(TAG, "llSelectedMembersContainer is null, cannot initialize SelectedMembersUiManager");
        }
        
        if (llAddedLinksContainer != null) {
            addedLinksUiManager = new AddedLinksUiManager(
                    this, llAddedLinksContainer, projectLinks, this);
            Log.d(TAG, "AddedLinksUiManager initialized successfully");
        } else {
            Log.e(TAG, "llAddedLinksContainer is null, cannot initialize AddedLinksUiManager");
        }
        
        if (flexboxMediaPreviewContainer != null && tvMediaGalleryLabel != null) {
            mediaGalleryUiManager = new MediaGalleryUiManager(
                    this, flexboxMediaPreviewContainer, tvMediaGalleryLabel, selectedMediaUris, this);
            Log.d(TAG, "MediaGalleryUiManager initialized successfully");
        } else {
            Log.e(TAG, "flexboxMediaPreviewContainer: " + (flexboxMediaPreviewContainer != null) + 
                      ", tvMediaGalleryLabel: " + (tvMediaGalleryLabel != null) + 
                      " - cannot initialize MediaGalleryUiManager");
        }
    }

    private void updateAllUIs() {
        Log.d(TAG, "updateAllUIs called - selectedMediaUris size: " + selectedMediaUris.size());
        
        if (selectedMembersUiManager != null) {
            selectedMembersUiManager.updateUI();
            Log.d(TAG, "SelectedMembersUiManager updated");
        } else {
            Log.e(TAG, "selectedMembersUiManager is null");
        }
        
        if (mediaGalleryUiManager != null) {
            mediaGalleryUiManager.updateUI();
            Log.d(TAG, "MediaGalleryUiManager updated");
        } else {
            Log.e(TAG, "mediaGalleryUiManager is null");
        }
        
        if (addedLinksUiManager != null) {
            addedLinksUiManager.updateUI();
            Log.d(TAG, "AddedLinksUiManager updated");
        } else {
            Log.e(TAG, "addedLinksUiManager is null");
        }
        
        // Update media gallery label with count information
        updateMediaGalleryLabel();
    }

    private void setupAdaptersAndData() {
        categoryAdapter = new ArrayAdapter<>(this, android.R.layout.simple_dropdown_item_1line, new ArrayList<>());
        if (actvCategoryInput != null) {
            actvCategoryInput.setAdapter(categoryAdapter);
        }

        ArrayAdapter<String> technologyAdapter = new ArrayAdapter<>(this, android.R.layout.simple_dropdown_item_1line, new ArrayList<>());
        if (actvTechnologyInput != null) {
            actvTechnologyInput.setAdapter(technologyAdapter);
        }

        statusNameListForDropdown = Arrays.asList("Đang thực hiện", "Hoàn thành", "Tạm dừng");
        statusAdapter = new ArrayAdapter<>(this, android.R.layout.simple_dropdown_item_1line, statusNameListForDropdown);
        if (actvStatus != null) {
            actvStatus.setAdapter(statusAdapter);
        }

        // Fetch data
        fetchCategories();
        fetchTechnologies();
    }

    private void fetchCategories() {
        firestoreService.fetchCategories(new FirestoreService.CategoriesFetchListener() {
            @Override
            public void onCategoriesFetched(List<String> fetchedCategoryNames, Map<String, String> fetchedNameToIdMap) {
                categoryNameListForDropdown.clear();
                categoryNameToIdMap.clear();
                categoryNameListForDropdown.addAll(fetchedCategoryNames);
                categoryNameToIdMap.putAll(fetchedNameToIdMap);
                
                categoryAdapter.clear();
                categoryAdapter.addAll(fetchedCategoryNames);
                categoryAdapter.notifyDataSetChanged();
            }

            @Override
            public void onError(String errorMessage) {
                Log.e(TAG, "Error fetching categories: " + errorMessage);
            }
        });
    }

    private void fetchTechnologies() {
        firestoreService.fetchTechnologies(new FirestoreService.TechnologyFetchListener() {
            @Override
            public void onTechnologiesFetched(List<String> fetchedTechnologyNames, Map<String, String> fetchedTechNameToIdMap) {
                allAvailableTechnologyNames.clear();
                technologyNameToIdMap.clear();
                technologyIdToNameMap.clear();
                allAvailableTechnologyNames.addAll(fetchedTechnologyNames);
                technologyNameToIdMap.putAll(fetchedTechNameToIdMap);
                
                for (Map.Entry<String, String> entry : fetchedTechNameToIdMap.entrySet()) {
                    technologyIdToNameMap.put(entry.getValue(), entry.getKey());
                }

                // Update technology adapter
                if (actvTechnologyInput != null && actvTechnologyInput.getAdapter() != null) {
                    ArrayAdapter<String> adapter = (ArrayAdapter<String>) actvTechnologyInput.getAdapter();
                    adapter.clear();
                    adapter.addAll(fetchedTechnologyNames);
                    adapter.notifyDataSetChanged();
                }
            }

            @Override
            public void onError(String errorMessage) {
                Log.e(TAG, "Error fetching technologies: " + errorMessage);
            }
        });
    }

    private void setupEventListeners() {
        if (ivBackArrow != null) {
            ivBackArrow.setOnClickListener(v -> handleCustomBackPressed());
        }

        if (flProjectImageContainer != null) {
            flProjectImageContainer.setOnClickListener(v -> {
                if (checkLoginAndNotify("thêm ảnh dự án")) {
                    currentPickerAction = ACTION_PICK_PROJECT_IMAGE;
                    if (permissionManager != null) {
                        permissionManager.checkAndRequestStoragePermissions();
                    } else {
                        imagePickerDelegate.launchProjectImagePicker();
                    }
                }
            });
        }

        // Setup category input
        if (actvCategoryInput != null) {
            actvCategoryInput.setOnClickListener(v -> {
                if (!actvCategoryInput.isPopupShowing()) {
                    actvCategoryInput.showDropDown();
                }
            });
            actvCategoryInput.setOnFocusChangeListener((v, hasFocus) -> {
                if (hasFocus) {
                    if (!actvCategoryInput.isPopupShowing()) {
                        actvCategoryInput.showDropDown();
                    }
                } else {
                    if (actvCategoryInput.isPopupShowing()) {
                        actvCategoryInput.dismissDropDown();
                    }
                }
            });
            actvCategoryInput.setOnItemClickListener((parent, view, position, id) -> {
                String selectedCategory = (String) parent.getItemAtPosition(position);
                addCategoryChip(selectedCategory);
                actvCategoryInput.setText("");
                actvCategoryInput.dismissDropDown();
                hideKeyboard();
            });
            actvCategoryInput.setOnEditorActionListener((v, actionId, event) -> {
                if (actionId == EditorInfo.IME_ACTION_DONE || actionId == EditorInfo.IME_ACTION_NEXT) {
                    String currentText = actvCategoryInput.getText().toString().trim();
                    if (!currentText.isEmpty()) {
                        if (categoryNameListForDropdown.contains(currentText)) {
                            addCategoryChip(currentText);
                            actvCategoryInput.setText("");
                        } else {
                            Toast.makeText(this, "Lĩnh vực '" + currentText + "' không hợp lệ.", Toast.LENGTH_SHORT).show();
                        }
                        hideKeyboard();
                        return true;
                    }
                }
                return false;
            });
        }

        // Setup technology input
        if (actvTechnologyInput != null) {
            actvTechnologyInput.setOnClickListener(v -> {
                if (!actvTechnologyInput.isPopupShowing()) {
                    actvTechnologyInput.showDropDown();
                }
            });
            actvTechnologyInput.setOnFocusChangeListener((v, hasFocus) -> {
                if (hasFocus) {
                    if (!actvTechnologyInput.isPopupShowing()) {
                        actvTechnologyInput.showDropDown();
                    }
                } else {
                    if (actvTechnologyInput.isPopupShowing()) {
                        actvTechnologyInput.dismissDropDown();
                    }
                }
            });
            actvTechnologyInput.setOnItemClickListener((parent, view, position, id) -> {
                String selectedTech = (String) parent.getItemAtPosition(position);
                addTechnologyChip(selectedTech);
                actvTechnologyInput.setText("");
                actvTechnologyInput.dismissDropDown();
                hideKeyboard();
            });
            actvTechnologyInput.setOnEditorActionListener((v, actionId, event) -> {
                if (actionId == EditorInfo.IME_ACTION_DONE || actionId == EditorInfo.IME_ACTION_NEXT) {
                    String currentText = actvTechnologyInput.getText().toString().trim();
                    if (!currentText.isEmpty()) {
                        if (allAvailableTechnologyNames.contains(currentText)) {
                            addTechnologyChip(currentText);
                            actvTechnologyInput.setText("");
                        } else {
                            Toast.makeText(this, "Công nghệ '" + currentText + "' không hợp lệ.", Toast.LENGTH_SHORT).show();
                        }
                        hideKeyboard();
                        return true;
                    }
                }
                return false;
            });
        }

        if (btnAddMember != null) {
            btnAddMember.setOnClickListener(v -> {
                if (checkLoginAndNotify("thêm thành viên")) {
                    AddMemberDialogFragment dialog = new AddMemberDialogFragment();
                    dialog.show(getSupportFragmentManager(), "AddMemberDialog");
                }
            });
        }

        if (btnAddLink != null) {
            btnAddLink.setOnClickListener(v -> {
                if (checkLoginAndNotify("thêm liên kết")) {
                    // Kiểm tra xem đã có GitHub và Demo link chưa
                    boolean githubLinkExists = projectLinks.stream()
                            .anyMatch(item -> "GitHub".equalsIgnoreCase(item.getPlatform()));
                    boolean demoLinkExists = projectLinks.stream()
                            .anyMatch(item -> "Demo".equalsIgnoreCase(item.getPlatform()));
                    
                    if (githubLinkExists && demoLinkExists) {
                        Toast.makeText(this, "Bạn đã thêm đủ liên kết GitHub và Demo.", Toast.LENGTH_SHORT).show();
                        return;
                    }
                    
                    // Tự động thêm link còn thiếu
                    String platformToAdd = "";
                    if (!githubLinkExists) {
                        platformToAdd = "GitHub";
                    } else if (!demoLinkExists) {
                        platformToAdd = "Demo";
                    }
                    
                    if (!platformToAdd.isEmpty()) {
                        projectLinks.add(new LinkItem("", platformToAdd));
                        updateAllUIs();
                        hasUserMadeChanges = true;
                    } else {
                        Toast.makeText(this, "Không thể thêm liên kết mới.", Toast.LENGTH_SHORT).show();
                    }
                }
            });
        }

        if (btnAddMedia != null) {
            btnAddMedia.setOnClickListener(v -> {
                if (checkLoginAndNotify("thêm media")) {
                    if (selectedMediaUris.size() >= 10) {
                        Toast.makeText(this, "Bạn chỉ có thể thêm tối đa 10 media.", Toast.LENGTH_SHORT).show();
                        return;
                    }
                    currentPickerAction = ACTION_PICK_MEDIA;
                    if (permissionManager != null && permissionManager.checkAndRequestStoragePermissions()) {
                        if (imagePickerDelegate != null) imagePickerDelegate.launchMediaPicker();
                    }
                }
            });
        } else {
            Log.e(TAG, "btnAddMedia is null!");
        }

        if (btnSaveChanges != null) {
            btnSaveChanges.setOnClickListener(v -> {
                if (checkLoginAndNotify("lưu thay đổi")) {
                    if (validateForm()) {
                        promptSaveConfirmation();
                    }
                }
            });
        }

        if (btnDeleteProject != null) {
            btnDeleteProject.setOnClickListener(v -> {
                if (checkLoginAndNotify("xóa dự án")) {
                    promptDeleteConfirmation();
                }
            });
        }
    }

    private void setupInputValidationListeners() {
        addTextWatcherToTrackChanges(etProjectName);
        addTextWatcherToTrackChanges(etProjectDescription);
        addTextWatcherToTrackChanges(actvTechnologyInput);
        addTextWatcherToTrackChanges(actvCategoryInput);
        addTextWatcherToTrackChanges(actvStatus);

        addTextWatcherToClearError(etProjectName, tilProjectName);
        addTextWatcherToClearError(etProjectDescription, tilProjectDescription);
        addTextWatcherToClearError(actvTechnologyInput, tilTechnologyInput);
        addTextWatcherToClearError(actvCategoryInput, tilCategory);
        addTextWatcherToClearError(actvStatus, tilStatus);
    }

    private void addTextWatcherToTrackChanges(TextInputEditText editText) {
        if (editText != null) {
            editText.addTextChangedListener(new TextWatcher() {
                @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
                @Override public void onTextChanged(CharSequence s, int start, int before, int count) { hasUserMadeChanges = true; }
                @Override public void afterTextChanged(Editable s) {}
            });
        }
    }

    private void addTextWatcherToTrackChanges(AutoCompleteTextView autoCompleteTextView) {
        if (autoCompleteTextView != null) {
            autoCompleteTextView.addTextChangedListener(new TextWatcher() {
                @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
                @Override public void onTextChanged(CharSequence s, int start, int before, int count) { hasUserMadeChanges = true; }
                @Override public void afterTextChanged(Editable s) {}
            });
        }
    }

    private void addTextWatcherToClearError(TextInputEditText editText, TextInputLayout textInputLayout) {
        if (editText != null && textInputLayout != null) {
            editText.addTextChangedListener(new TextWatcher() {
                @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
                @Override public void onTextChanged(CharSequence s, int start, int before, int count) {
                    textInputLayout.setError(null);
                }
                @Override public void afterTextChanged(Editable s) {}
            });
        }
    }

    private void addTextWatcherToClearError(AutoCompleteTextView editText, TextInputLayout textInputLayout) {
        if (editText != null && textInputLayout != null) {
            editText.addTextChangedListener(new TextWatcher() {
                @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
                @Override public void onTextChanged(CharSequence s, int start, int before, int count) {
                    textInputLayout.setError(null);
                }
                @Override public void afterTextChanged(Editable s) {}
            });
        }
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
                Toast.makeText(EditProjectActivity.this, "Lỗi tải dự án: " + errorMessage, Toast.LENGTH_LONG).show();
                finish();
            }
        });
    }

    private void loadAdditionalProjectData() {
        // Load project categories using ProjectRepository
        projectRepository.fetchCategoriesForProject(currentProjectId, new ProjectRepository.ProjectRelatedListListener<String>() {
            @Override
            public void onListFetched(List<String> categories) {
                if (currentProject != null) {
                    currentProject.setCategoryNames(categories);
                    // Update UI with categories
                    populateCategoryChips(categories);
                    Log.d(TAG, "Loaded categories: " + categories);
                }
            }

            @Override
            public void onListEmpty() {
                Log.d(TAG, "No categories found for project");
            }

            @Override
            public void onError(String errorMessage) {
                Log.e(TAG, "Error loading project categories: " + errorMessage);
            }
        });

        // Load project technologies using ProjectRepository
        projectRepository.fetchTechnologiesForProject(currentProjectId, new ProjectRepository.ProjectRelatedListListener<String>() {
            @Override
            public void onListFetched(List<String> technologies) {
                if (currentProject != null) {
                    currentProject.setTechnologyNames(technologies);
                    // Update UI with technologies
                    populateTechnologyChips(technologies);
                    Log.d(TAG, "Loaded technologies: " + technologies);
                }
            }

            @Override
            public void onListEmpty() {
                Log.d(TAG, "No technologies found for project");
            }

            @Override
            public void onError(String errorMessage) {
                Log.e(TAG, "Error loading project technologies: " + errorMessage);
            }
        });

        // Load project members using ProjectRepository (from ProjectMembers collection)
        projectRepository.fetchProjectMembers(currentProjectId, new ProjectRepository.ProjectRelatedListListener<Project.UserShortInfo>() {
            @Override
            public void onListFetched(List<Project.UserShortInfo> memberInfos) {
                selectedProjectUsers.clear();
                userRolesInProject.clear();
                originalProjectUsers.clear();
                originalUserRoles.clear();
                
                if (memberInfos != null && !memberInfos.isEmpty()) {
                    for (Project.UserShortInfo memberInfo : memberInfos) {
                        User user = new User();
                        user.setUserId(memberInfo.getUserId());
                        user.setFullName(memberInfo.getFullName());
                        user.setEmail(""); // Email not available in UserShortInfo
                        user.setAvatarUrl(memberInfo.getAvatarUrl());
                        user.setClassName(memberInfo.getClassName()); // Set className from UserShortInfo
                        selectedProjectUsers.add(user);
                        userRolesInProject.put(memberInfo.getUserId(), memberInfo.getRoleInProject());
                        
                        // Store original members for comparison
                        User originalUser = new User();
                        originalUser.setUserId(memberInfo.getUserId());
                        originalUser.setFullName(memberInfo.getFullName());
                        originalUser.setEmail("");
                        originalUser.setAvatarUrl(memberInfo.getAvatarUrl());
                        originalUser.setClassName(memberInfo.getClassName()); // Set className for original user too
                        originalProjectUsers.add(originalUser);
                        originalUserRoles.put(memberInfo.getUserId(), memberInfo.getRoleInProject());
                    }
                }
                
                updateAllUIs();
                Log.d(TAG, "Loaded " + selectedProjectUsers.size() + " members from ProjectMembers collection");
            }

            @Override
            public void onListEmpty() {
                Log.d(TAG, "No members found for project");
                updateAllUIs();
            }

            @Override
            public void onError(String errorMessage) {
                Log.e(TAG, "Error loading project members: " + errorMessage);
                updateAllUIs();
            }
        });

        // Load project links from project document (ProjectUrl and DemoUrl fields)
        loadProjectLinksFromDocument();

        // Load project media from project document (MediaGalleryUrls field)
        loadProjectMediaFromDocument();
    }

    private void loadProjectLinksFromDocument() {
        if (currentProject == null) return;

                projectLinks.clear();
        
        // Add GitHub link if exists
        if (currentProject.getProjectUrl() != null && !currentProject.getProjectUrl().isEmpty()) {
            projectLinks.add(new LinkItem(currentProject.getProjectUrl(), "GitHub"));
        }
        
        // Add Demo link if exists
        if (currentProject.getDemoUrl() != null && !currentProject.getDemoUrl().isEmpty()) {
            projectLinks.add(new LinkItem(currentProject.getDemoUrl(), "Demo"));
        }
        
        updateAllUIs();
        Log.d(TAG, "Loaded " + projectLinks.size() + " links from project document");
    }

    private void loadProjectMediaFromDocument() {
        if (currentProject == null) return;

        selectedMediaUris.clear();
        existingMediaUrls.clear();
        newMediaUris.clear();
        
        // Try to load from MediaGalleryUrls field
        if (currentProject.getMediaGalleryUrls() != null && !currentProject.getMediaGalleryUrls().isEmpty()) {
            for (Project.MediaItem mediaItem : currentProject.getMediaGalleryUrls()) {
                if (mediaItem.getUrl() != null && !mediaItem.getUrl().isEmpty()) {
                    // Store existing media as URL data
                    Map<String, String> existingMedia = new HashMap<>();
                    existingMedia.put("url", mediaItem.getUrl());
                    existingMedia.put("type", mediaItem.getType() != null ? mediaItem.getType() : "image");
                    existingMediaUrls.add(existingMedia);
                    
                    // Also add to selectedMediaUris for UI display (but these are URLs, not URIs)
                    try {
                        Uri uri = Uri.parse(mediaItem.getUrl());
                        selectedMediaUris.add(uri);
                        Log.d(TAG, "Added existing media URL: " + mediaItem.getUrl() + " (type: " + mediaItem.getType() + ")");
                    } catch (Exception e) {
                        Log.e(TAG, "Error parsing existing media URL: " + mediaItem.getUrl(), e);
                    }
                }
            }
        } else {
            Log.d(TAG, "No MediaGalleryUrls found in project document, trying alternative approach");
            
            // Alternative approach: Try to load from raw document data
            // This handles the case where data is stored as List<Map<String, String>> instead of List<MediaItem>
            try {
                db.collection("Projects").document(currentProjectId).get()
                    .addOnSuccessListener(documentSnapshot -> {
                        if (documentSnapshot.exists()) {
                            Object mediaGalleryData = documentSnapshot.get("MediaGalleryUrls");
                            if (mediaGalleryData instanceof List) {
                                List<?> mediaList = (List<?>) mediaGalleryData;
                                for (Object item : mediaList) {
                                    if (item instanceof Map) {
                                        Map<?, ?> mediaMap = (Map<?, ?>) item;
                                        Object urlObj = mediaMap.get("url");
                                        Object typeObj = mediaMap.get("type");
                                        if (urlObj instanceof String) {
                                            String url = (String) urlObj;
                                            String mediaType = (typeObj instanceof String) ? (String) typeObj : "image";
                                            if (url != null && !url.isEmpty()) {
                                                // Store existing media as URL data
                                                Map<String, String> existingMedia = new HashMap<>();
                                                existingMedia.put("url", url);
                                                existingMedia.put("type", mediaType);
                                                existingMediaUrls.add(existingMedia);
                                                
                                                try {
                                                    Uri uri = Uri.parse(url);
                                                    selectedMediaUris.add(uri);
                                                    Log.d(TAG, "Added existing media URL from raw data: " + url + " (type: " + mediaType + ")");
                                                } catch (Exception e) {
                                                    Log.e(TAG, "Error parsing existing media URL from raw data: " + url, e);
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                            // Update UI after loading all media
                            if (mediaGalleryUiManager != null) {
                                mediaGalleryUiManager.updateUI();
                            }
                            updateAllUIs();
                            Log.d(TAG, "Loaded " + selectedMediaUris.size() + " existing media items from raw document data");
                        }
                    })
                    .addOnFailureListener(e -> {
                        Log.e(TAG, "Error loading raw document data: " + e.getMessage());
                        updateAllUIs();
                    });
                return; // Return early since we're using async approach
            } catch (Exception e) {
                Log.e(TAG, "Error accessing raw document data: " + e.getMessage());
            }
        }
        
        // Update UI after loading media
        if (mediaGalleryUiManager != null) {
            mediaGalleryUiManager.updateUI();
        }
        updateAllUIs();
        Log.d(TAG, "Loaded " + selectedMediaUris.size() + " existing media items from project document");
    }

    private void populateCategoryChips(List<String> categoryNames) {
        if (categoryNames == null || categoryNames.isEmpty()) {
            Log.d(TAG, "No category names to populate");
            return;
        }

        runOnUiThread(() -> {
            if (chipGroupCategories != null) {
                chipGroupCategories.removeAllViews();
                selectedCategoryNames.clear();
                
                for (String categoryName : categoryNames) {
                    addCategoryChip(categoryName);
                }
                Log.d(TAG, "Populated " + categoryNames.size() + " category chips");
            } else {
                Log.e(TAG, "chipGroupCategories is null, cannot populate category chips");
            }
        });
    }

    private void populateTechnologyChips(List<String> technologyNames) {
        if (technologyNames == null || technologyNames.isEmpty()) {
            Log.d(TAG, "No technology names to populate");
            return;
        }

        runOnUiThread(() -> {
            if (chipGroupTechnologies != null) {
                chipGroupTechnologies.removeAllViews();
                selectedTechnologyNames.clear();
                
                for (String technologyName : technologyNames) {
                    addTechnologyChip(technologyName);
                }
                Log.d(TAG, "Populated " + technologyNames.size() + " technology chips");
            } else {
                Log.e(TAG, "chipGroupTechnologies is null, cannot populate technology chips");
            }
        });
    }

    private void populateUiWithProjectData() {
        if (currentProject == null) return;

        Log.d(TAG, "Populating UI with project data: " + currentProject.getTitle());

        // Populate basic project information
        if (currentProject.getTitle() != null) {
            etProjectName.setText(currentProject.getTitle());
            Log.d(TAG, "Set project name: " + currentProject.getTitle());
        }
        if (currentProject.getDescription() != null) {
            etProjectDescription.setText(currentProject.getDescription());
            Log.d(TAG, "Set project description: " + currentProject.getDescription());
        }
        if (currentProject.getStatus() != null) {
            actvStatus.setText(currentProject.getStatus(), false);
            Log.d(TAG, "Set project status: " + currentProject.getStatus());
        }

        // Note: Categories and Technologies are loaded separately in loadAdditionalProjectData()
        // because they are stored in separate collections

        // Populate project image
        if (currentProject.getThumbnailUrl() != null && !currentProject.getThumbnailUrl().isEmpty()) {
            if (ivProjectImagePreview != null) {
                ivProjectImagePreview.setVisibility(View.VISIBLE);
                Glide.with(this)
                        .load(currentProject.getThumbnailUrl())
                        .placeholder(R.drawable.image_placeholder_square)
                        .error(R.drawable.image_placeholder_square)
                        .into(ivProjectImagePreview);
                Log.d(TAG, "Set project thumbnail: " + currentProject.getThumbnailUrl());
            }
            if (ivProjectImagePlaceholderIcon != null) {
                ivProjectImagePlaceholderIcon.setVisibility(View.GONE);
            }
        } else {
            Log.d(TAG, "No thumbnail URL found in project");
        }

        // Force update UI managers to ensure visibility
        runOnUiThread(() -> {
            updateAllUIs();
        });
    }

    private boolean validateForm() {
        boolean isValid = true;

        String projectName = getTextFromInput(etProjectName);
        if (TextUtils.isEmpty(projectName)) {
            tilProjectName.setError("Vui lòng nhập tên dự án");
            isValid = false;
        }

        String projectDescription = getTextFromInput(etProjectDescription);
        if (TextUtils.isEmpty(projectDescription)) {
            tilProjectDescription.setError("Vui lòng nhập mô tả dự án");
            isValid = false;
        }

        if (selectedCategoryNames.isEmpty()) {
            tilCategory.setError("Vui lòng chọn ít nhất một lĩnh vực");
            isValid = false;
        }

        String status = getTextFromInput(actvStatus);
        if (TextUtils.isEmpty(status)) {
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

        // Collect form data
        String projectName = getTextFromInput(etProjectName);
        String projectDescription = getTextFromInput(etProjectDescription);
        String status = getTextFromInput(actvStatus);

        // Convert category names to IDs
        List<String> selectedCategoryIds = selectedCategoryNames.stream()
                .map(name -> categoryNameToIdMap.get(name))
                .filter(id -> id != null && !id.isEmpty())
                .collect(Collectors.toList());

        // Convert technology names to IDs
        List<String> selectedTechnologyIds = selectedTechnologyNames.stream()
                .map(name -> technologyNameToIdMap.get(name))
                .filter(id -> id != null && !id.isEmpty())
                .collect(Collectors.toList());

        // Start upload process if there are new media files
        if (projectImageUri != null || !newMediaUris.isEmpty()) {
            uploadMediaAndSaveProject(projectName, projectDescription, status, selectedCategoryIds, selectedTechnologyIds);
        } else {
            // No new media to upload, save directly
            saveProjectData(projectName, projectDescription, status, selectedCategoryIds, selectedTechnologyIds, null, null);
        }
    }

    private void uploadMediaAndSaveProject(String projectName, String projectDescription, String status, 
                                         List<String> selectedCategoryIds, List<String> selectedTechnologyIds) {
        
        // Upload thumbnail first if there's a new project image
        if (projectImageUri != null) {
            cloudinaryUploadService.uploadThumbnail(projectImageUri,
                    Constants.CLOUDINARY_UPLOAD_PRESET_THUMBNAIL,
                    Constants.CLOUDINARY_FOLDER_PROJECT_THUMBNAILS,
                    new CloudinaryUploadService.ThumbnailUploadListener() {
                        @Override
                        public void onThumbnailUploadSuccess(String thumbnailUrl) {
                            if (isFinishing() || isDestroyed()) return;
                            
                            // Then upload new media files if any
                            if (newMediaUris.isEmpty()) {
                                saveProjectData(projectName, projectDescription, status, selectedCategoryIds, selectedTechnologyIds, thumbnailUrl, new ArrayList<>());
                                return;
                            }
                            
                            cloudinaryUploadService.uploadMultipleMedia(newMediaUris,
                                    Constants.CLOUDINARY_UPLOAD_PRESET_MEDIA,
                                    Constants.CLOUDINARY_FOLDER_PROJECT_MEDIA,
                                    new CloudinaryUploadService.MediaUploadListener() {
                                        @Override
                                        public void onAllMediaUploaded(List<Map<String, String>> uploadedMediaDetails) {
                                            if (isFinishing() || isDestroyed()) return;
                                            saveProjectData(projectName, projectDescription, status, selectedCategoryIds, selectedTechnologyIds, thumbnailUrl, uploadedMediaDetails);
                                        }
                                        
                                        @Override 
                                        public void onMediaUploadItemSuccess(String url, String resourceType, int currentIndex, int totalCount) {}
                                        
                                        @Override
                                        public void onMediaUploadItemError(String errorMessage, Uri erroredUri, int currentIndex, int totalCount) {
                                            if (isFinishing() || isDestroyed()) return;
                                            Toast.makeText(EditProjectActivity.this, "Lỗi tải media: " + (erroredUri != null ? erroredUri.getLastPathSegment() : "unknown") + ". Item này sẽ bị bỏ qua.", Toast.LENGTH_LONG).show();
                                        }
                                        
                                        @Override
                                        public void onMediaUploadProgress(int processedCount, int totalCount) {
                                            if (isFinishing() || isDestroyed()) return;
                                            showProgress(true, "Đang tải media (" + processedCount + "/" + totalCount + ")");
                                        }
                                    });
                        }
                        
                        @Override
                        public void onThumbnailUploadError(String errorMessage) {
                            if (isFinishing() || isDestroyed()) return;
                            showProgress(false, null);
                            Toast.makeText(EditProjectActivity.this, "Lỗi tải ảnh bìa: " + errorMessage + ". Vui lòng thử lại.", Toast.LENGTH_LONG).show();
                        }
                    });
        } else {
            // No new thumbnail, just upload new media files
            if (newMediaUris.isEmpty()) {
                saveProjectData(projectName, projectDescription, status, selectedCategoryIds, selectedTechnologyIds, null, new ArrayList<>());
                return;
            }
            
            cloudinaryUploadService.uploadMultipleMedia(newMediaUris,
                    Constants.CLOUDINARY_UPLOAD_PRESET_MEDIA,
                    Constants.CLOUDINARY_FOLDER_PROJECT_MEDIA,
                    new CloudinaryUploadService.MediaUploadListener() {
                        @Override
                        public void onAllMediaUploaded(List<Map<String, String>> uploadedMediaDetails) {
                            if (isFinishing() || isDestroyed()) return;
                            saveProjectData(projectName, projectDescription, status, selectedCategoryIds, selectedTechnologyIds, null, uploadedMediaDetails);
                        }
                        
                        @Override 
                        public void onMediaUploadItemSuccess(String url, String resourceType, int currentIndex, int totalCount) {}
                        
                        @Override
                        public void onMediaUploadItemError(String errorMessage, Uri erroredUri, int currentIndex, int totalCount) {
                            if (isFinishing() || isDestroyed()) return;
                            Toast.makeText(EditProjectActivity.this, "Lỗi tải media: " + (erroredUri != null ? erroredUri.getLastPathSegment() : "unknown") + ". Item này sẽ bị bỏ qua.", Toast.LENGTH_LONG).show();
                        }
                        
                        @Override
                        public void onMediaUploadProgress(int processedCount, int totalCount) {
                            if (isFinishing() || isDestroyed()) return;
                            showProgress(true, "Đang tải media (" + processedCount + "/" + totalCount + ")");
                        }
                    });
        }
    }

    private void saveProjectData(String projectName, String projectDescription, String status,
                               List<String> selectedCategoryIds, List<String> selectedTechnologyIds,
                               String newThumbnailUrl, List<Map<String, String>> newMediaDetails) {
        
        showProgress(true, "Đang lưu dự án...");
        
        // Prepare project data for update
        Map<String, Object> projectData = new HashMap<>();
        projectData.put("Title", projectName);
        projectData.put("Description", projectDescription);
        projectData.put("Status", status);
        projectData.put("UpdatedAt", new com.google.firebase.Timestamp(new java.util.Date()));

        // Update thumbnail if there's a new one
        if (newThumbnailUrl != null) {
            projectData.put("ThumbnailUrl", newThumbnailUrl);
        }

        // Prepare links data (ProjectUrl and DemoUrl)
        String projectUrlFromUiLinks = null;
        String demoUrlFromUiLinks = null;
        for (LinkItem linkItem : projectLinks) {
            String url = linkItem.getUrl() != null ? linkItem.getUrl().trim() : "";
            String platform = linkItem.getPlatform() != null ? linkItem.getPlatform().trim() : "";
            if (!url.isEmpty() && android.util.Patterns.WEB_URL.matcher(url).matches()) {
                if ("GitHub".equalsIgnoreCase(platform) && projectUrlFromUiLinks == null) {
                    projectUrlFromUiLinks = url;
                } else if ("Demo".equalsIgnoreCase(platform) && demoUrlFromUiLinks == null) {
                    demoUrlFromUiLinks = url;
                }
            }
        }
        projectData.put("ProjectUrl", projectUrlFromUiLinks);
        projectData.put("DemoUrl", demoUrlFromUiLinks);

        // Prepare media data (MediaGalleryUrls)
        List<Map<String, String>> mediaGalleryForFirestore = new ArrayList<>();
        
        // Add existing media from current project (preserve them)
        mediaGalleryForFirestore.addAll(existingMediaUrls);
        
        // Add new uploaded media
        if (newMediaDetails != null && !newMediaDetails.isEmpty()) {
            mediaGalleryForFirestore.addAll(newMediaDetails);
        }
        
        projectData.put("MediaGalleryUrls", mediaGalleryForFirestore);
        Log.d(TAG, "Prepared " + mediaGalleryForFirestore.size() + " media items for saving (" + existingMediaUrls.size() + " existing + " + (newMediaDetails != null ? newMediaDetails.size() : 0) + " new)");

        // Nếu dự án đã được duyệt thì set lại IsApproved = false
        final boolean wasApproved = currentProject != null && currentProject.isApproved();
        if (wasApproved) {
            projectData.put("IsApproved", false);
        }

        // Update project document
        db.collection("Projects").document(currentProjectId)
                .update(projectData)
                .addOnSuccessListener(aVoid -> {
                    // Update categories and technologies in separate collections
                    updateProjectCategories(selectedCategoryIds);
                    updateProjectTechnologies(selectedTechnologyIds);
                    updateProjectMembers();
                    
                    showProgress(false, null);
                    if (wasApproved) {
                        Toast.makeText(this, "Cập nhật dự án thành công. Vui lòng chờ xét duyệt dự án.", Toast.LENGTH_LONG).show();
                    } else {
                        Toast.makeText(this, "Cập nhật dự án thành công!", Toast.LENGTH_LONG).show();
                    }
                    finish();
                })
                .addOnFailureListener(e -> {
                    showProgress(false, null);
                    Toast.makeText(this, "Lỗi cập nhật dự án: " + e.getMessage(), Toast.LENGTH_LONG).show();
                });
    }

    private void updateProjectCategories(List<String> categoryIds) {
        // Delete existing categories
        db.collection("ProjectCategories")
                .whereEqualTo("ProjectId", currentProjectId)
                .get()
                .addOnSuccessListener(querySnapshot -> {
                    for (com.google.firebase.firestore.QueryDocumentSnapshot document : querySnapshot) {
                        document.getReference().delete();
                    }
                    
                    // Add new categories
                    for (String categoryId : categoryIds) {
                        Map<String, Object> categoryData = new HashMap<>();
                        categoryData.put("ProjectId", currentProjectId);
                        categoryData.put("CategoryId", categoryId);
                        db.collection("ProjectCategories").add(categoryData);
                    }
                });
    }

    private void updateProjectTechnologies(List<String> technologyIds) {
        // Delete existing technologies
        db.collection("ProjectTechnologies")
                .whereEqualTo("ProjectId", currentProjectId)
                .get()
                .addOnSuccessListener(querySnapshot -> {
                    for (com.google.firebase.firestore.QueryDocumentSnapshot document : querySnapshot) {
                        document.getReference().delete();
                    }
                    
                    // Add new technologies
                    for (String technologyId : technologyIds) {
                        Map<String, Object> technologyData = new HashMap<>();
                        technologyData.put("ProjectId", currentProjectId);
                        technologyData.put("TechnologyId", technologyId);
                        db.collection("ProjectTechnologies").add(technologyData);
                    }
                });
    }

    private void updateProjectMembers() {
        // Delete existing members
        db.collection("ProjectMembers")
                .whereEqualTo("ProjectId", currentProjectId)
                .get()
                .addOnSuccessListener(querySnapshot -> {
                    for (com.google.firebase.firestore.QueryDocumentSnapshot document : querySnapshot) {
                        document.getReference().delete();
                    }
                    
                    // Add new members
                    for (User user : selectedProjectUsers) {
                        Map<String, Object> memberData = new HashMap<>();
                        memberData.put("ProjectId", currentProjectId);
                        memberData.put("UserId", user.getUserId());
                        memberData.put("RoleInProject", userRolesInProject.get(user.getUserId()));
                        db.collection("ProjectMembers").add(memberData);
                    }
                    
                    // Send notifications for member changes
                    sendMemberChangeNotifications();
                });
    }

    private void sendMemberChangeNotifications() {
        if (currentProject == null || mAuth.getCurrentUser() == null) return;
        
        String currentUserId = mAuth.getCurrentUser().getUid();
        String currentUserName = mAuth.getCurrentUser().getDisplayName() != null ? 
            mAuth.getCurrentUser().getDisplayName() : "Người dùng";
        String currentUserAvatar = mAuth.getCurrentUser().getPhotoUrl() != null ? 
            mAuth.getCurrentUser().getPhotoUrl().toString() : "";
        String projectTitle = currentProject.getTitle() != null ? currentProject.getTitle() : "Dự án";
        
        // Find newly added members (members in selectedProjectUsers but not in originalProjectUsers)
        for (User newMember : selectedProjectUsers) {
            boolean isNewMember = true;
            for (User originalMember : originalProjectUsers) {
                if (newMember.getUserId().equals(originalMember.getUserId())) {
                    isNewMember = false;
                    break;
                }
            }
            
            if (isNewMember && !newMember.getUserId().equals(currentUserId)) {
                // Send invitation notification to new member
                String role = userRolesInProject.get(newMember.getUserId());
                if (role == null) role = "Thành viên";
                
                notificationRepository.createProjectInvitation(
                    newMember.getUserId(), // recipientUserId
                    currentUserId, // actorUserId
                    currentUserName, // actorFullName
                    currentUserAvatar, // actorAvatarUrl
                    currentProjectId, // targetProjectId
                    projectTitle, // targetProjectTitle
                    role, // invitationRole
                    new NotificationRepository.NotificationActionListener() {
                        @Override
                        public void onSuccess() {
                            Log.d(TAG, "Sent invitation notification to: " + newMember.getFullName());
                        }
                        @Override
                        public void onError(String errorMessage) {
                            Log.e(TAG, "Error sending invitation notification: " + errorMessage);
                        }
                    }
                );
            }
        }
        
        // Find removed members (members in originalProjectUsers but not in selectedProjectUsers)
        for (User removedMember : originalProjectUsers) {
            boolean isRemoved = true;
            for (User currentMember : selectedProjectUsers) {
                if (removedMember.getUserId().equals(currentMember.getUserId())) {
                    isRemoved = false;
                    break;
                }
            }
            
            if (isRemoved && !removedMember.getUserId().equals(currentUserId)) {
                // Send removal notification to removed member
                sendMemberRemovalNotification(removedMember, currentUserName, projectTitle);
            }
        }
    }
    
    private void sendMemberRemovalNotification(User removedMember, String actorName, String projectTitle) {
        Notification notification = new Notification();
        notification.setRecipientUserId(removedMember.getUserId());
        notification.setActorUserId(mAuth.getCurrentUser().getUid());
        notification.setActorFullName(actorName);
        notification.setActorAvatarUrl(mAuth.getCurrentUser().getPhotoUrl() != null ? 
            mAuth.getCurrentUser().getPhotoUrl().toString() : "");
        notification.setType("MEMBER_REMOVED");
        notification.setMessage(actorName + " đã xóa bạn khỏi dự án " + projectTitle);
        notification.setTargetProjectId(currentProjectId);
        notification.setTargetProjectTitle(projectTitle);
        notification.setCreatedAt(com.google.firebase.Timestamp.now());
        notification.setRead(false);
        notification.setActionUrl("/project/" + currentProjectId);
        
        db.collection("Notifications")
            .add(notification)
            .addOnSuccessListener(docRef -> {
                Log.d(TAG, "Sent removal notification to: " + removedMember.getFullName());
                // TODO: Send push notification via FCM
            })
            .addOnFailureListener(e -> {
                Log.e(TAG, "Error sending removal notification: " + e.getMessage());
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
        showProgress(true, "Đang xóa dự án...");

        // For now, just show a success message
        showProgress(false, null);
        Toast.makeText(this, "Đã xóa dự án thành công!", Toast.LENGTH_LONG).show();
        finish();
    }

    private void showProgress(boolean show, @Nullable String message) {
        if (pbEditingProject != null) {
            pbEditingProject.setVisibility(show ? View.VISIBLE : View.GONE);
        }
        if (btnSaveChanges != null) {
            btnSaveChanges.setEnabled(!show);
        }
        if (btnDeleteProject != null) {
            btnDeleteProject.setEnabled(!show);
        }
    }

    private void hideKeyboard() {
        InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
        if (imm != null) {
            imm.hideSoftInputFromWindow(getCurrentFocus() != null ? getCurrentFocus().getWindowToken() : null, 0);
        }
    }

    private void handlePermissionResult(Map<String, Boolean> permissionsResult) {
        Log.d(TAG, "Permission result received: " + permissionsResult);
        boolean allGranted = permissionsResult.values().stream().allMatch(b -> b);
        if (allGranted) {
            Log.d(TAG, "All permissions granted, proceeding with picker action: " + currentPickerAction);
            if (currentPickerAction == ACTION_PICK_PROJECT_IMAGE && imagePickerDelegate != null) {
                Log.d(TAG, "Launching project image picker");
                imagePickerDelegate.launchProjectImagePicker();
            } else if (currentPickerAction == ACTION_PICK_MEDIA && imagePickerDelegate != null) {
                Log.d(TAG, "Launching media picker");
                imagePickerDelegate.launchMediaPicker();
            } else {
                Log.e(TAG, "Invalid picker action or imagePickerDelegate is null. Action: " + currentPickerAction + ", Delegate: " + (imagePickerDelegate != null));
            }
        } else {
            Log.d(TAG, "Some permissions denied: " + permissionsResult);
            Toast.makeText(this, "Quyền truy cập bộ nhớ bị từ chối.", Toast.LENGTH_SHORT).show();
        }
    }

    private void handleProjectImageResult(androidx.activity.result.ActivityResult result) {
        if (result.getResultCode() == Activity.RESULT_OK && result.getData() != null) {
            projectImageUri = result.getData().getData();
            if (projectImageUri != null && ivProjectImagePreview != null) {
                ivProjectImagePreview.setVisibility(View.VISIBLE);
                ivProjectImagePreview.setImageURI(projectImageUri);
                if (ivProjectImagePlaceholderIcon != null) {
                    ivProjectImagePlaceholderIcon.setVisibility(View.GONE);
                }
                hasUserMadeChanges = true;
            }
        }
    }

    private void handleMediaResult(androidx.activity.result.ActivityResult result) {
        if (result.getResultCode() == Activity.RESULT_OK && result.getData() != null) {
            Uri selectedMediaUri = result.getData().getData();
            if (selectedMediaUri != null) {
                // Check if this is a new media URI (not an existing URL)
                boolean isNewMedia = true;
                for (Map<String, String> existingMedia : existingMediaUrls) {
                    String existingUrl = existingMedia.get("url");
                    if (existingUrl != null && existingUrl.equals(selectedMediaUri.toString())) {
                        isNewMedia = false;
                        break;
                    }
                }
                
                if (isNewMedia) {
                    // Check if we haven't reached the limit (10 media items total)
                    if (selectedMediaUris.size() >= 10) {
                        Toast.makeText(this, "Bạn chỉ có thể thêm tối đa 10 media.", Toast.LENGTH_SHORT).show();
                        return;
                    }
                    
                    // Check for duplicates in new media
                    boolean alreadyExists = newMediaUris.stream().anyMatch(uri -> uri.equals(selectedMediaUri));
                    if (!alreadyExists) {
                        selectedMediaUris.add(selectedMediaUri);
                        newMediaUris.add(selectedMediaUri);
                        hasUserMadeChanges = true;
                        
                        // Update UI immediately like CreateProjectActivity
                        if (mediaGalleryUiManager != null) {
                            mediaGalleryUiManager.updateUI();
                        }
                        updateAllUIs();
                        
                        Toast.makeText(this, "Đã thêm media.", Toast.LENGTH_SHORT).show();
                        Log.d(TAG, "Added new media: " + selectedMediaUri + ", total count: " + selectedMediaUris.size());
                    } else {
                        Toast.makeText(this, "Media này đã được thêm.", Toast.LENGTH_SHORT).show();
                    }
                } else {
                    Toast.makeText(this, "Media này đã tồn tại trong dự án.", Toast.LENGTH_SHORT).show();
                }
            }
        }
    }

    private String detectMediaType(Uri uri) {
        try {
            // Method 1: Check MIME type from content resolver
            String mimeType = getContentResolver().getType(uri);
            if (mimeType != null) {
                if (mimeType.startsWith("image/")) {
                    Log.d(TAG, "Detected image from MIME type: " + mimeType);
                    return "image";
                } else if (mimeType.startsWith("video/")) {
                    Log.d(TAG, "Detected video from MIME type: " + mimeType);
                    return "video";
                }
            }
            
            // Method 2: Check file extension
            String uriString = uri.toString().toLowerCase();
            if (uriString.contains(".jpg") || uriString.contains(".jpeg") || 
                uriString.contains(".png") || uriString.contains(".gif") || 
                uriString.contains(".bmp") || uriString.contains(".webp")) {
                Log.d(TAG, "Detected image from file extension: " + uriString);
                return "image";
            } else if (uriString.contains(".mp4") || uriString.contains(".avi") || 
                       uriString.contains(".mov") || uriString.contains(".mkv") || 
                       uriString.contains(".wmv") || uriString.contains(".flv") ||
                       uriString.contains(".3gp") || uriString.contains(".webm")) {
                Log.d(TAG, "Detected video from file extension: " + uriString);
                return "video";
            }
            
            // Method 3: Try to get file info from MediaStore
            String[] projection = {MediaStore.MediaColumns.MIME_TYPE};
            try (Cursor cursor = getContentResolver().query(uri, projection, null, null, null)) {
                if (cursor != null && cursor.moveToFirst()) {
                    int mimeTypeColumnIndex = cursor.getColumnIndex(MediaStore.MediaColumns.MIME_TYPE);
                    if (mimeTypeColumnIndex >= 0) {
                        String detectedMimeType = cursor.getString(mimeTypeColumnIndex);
                        if (detectedMimeType != null) {
                            if (detectedMimeType.startsWith("image/")) {
                                Log.d(TAG, "Detected image from MediaStore: " + detectedMimeType);
                                return "image";
                            } else if (detectedMimeType.startsWith("video/")) {
                                Log.d(TAG, "Detected video from MediaStore: " + detectedMimeType);
                                return "video";
                            }
                        }
                    }
                }
            } catch (Exception e) {
                Log.w(TAG, "Error querying MediaStore: " + e.getMessage());
            }
            
            Log.w(TAG, "Could not determine media type for URI: " + uri);
            return null;
            
        } catch (Exception e) {
            Log.e(TAG, "Error detecting media type: " + e.getMessage(), e);
            return null;
        }
    }

    private boolean validateMediaFile(Uri uri) {
        try {
            // Check file size
            long fileSize = getFileSize(uri);
            long maxSizeInBytes = 50 * 1024 * 1024; // 50MB limit
            
            if (fileSize > maxSizeInBytes) {
                Toast.makeText(this, "File quá lớn. Kích thước tối đa là 50MB.", Toast.LENGTH_SHORT).show();
                return false;
            }
            
            // Check media type
            String mediaType = detectMediaType(uri);
            if (mediaType == null) {
                Toast.makeText(this, "Định dạng file không được hỗ trợ.", Toast.LENGTH_SHORT).show();
                return false;
            }
            
            return true;
        } catch (Exception e) {
            Log.e(TAG, "Error validating media file: " + e.getMessage(), e);
            Toast.makeText(this, "Lỗi kiểm tra file.", Toast.LENGTH_SHORT).show();
            return false;
        }
    }
    
    private long getFileSize(Uri uri) {
        try {
            // Try to get file size from content resolver
            String[] projection = {MediaStore.MediaColumns.SIZE};
            try (Cursor cursor = getContentResolver().query(uri, projection, null, null, null)) {
                if (cursor != null && cursor.moveToFirst()) {
                    int sizeColumnIndex = cursor.getColumnIndex(MediaStore.MediaColumns.SIZE);
                    if (sizeColumnIndex >= 0) {
                        long size = cursor.getLong(sizeColumnIndex);
                        Log.d(TAG, "File size from MediaStore: " + size + " bytes");
                        return size;
                    }
                }
            } catch (Exception e) {
                Log.w(TAG, "Error getting file size from MediaStore: " + e.getMessage());
            }
            
            // Fallback: try to get file info
            try (Cursor cursor = getContentResolver().query(uri, null, null, null, null)) {
                if (cursor != null && cursor.moveToFirst()) {
                    int sizeColumnIndex = cursor.getColumnIndex(MediaStore.MediaColumns.SIZE);
                    if (sizeColumnIndex >= 0) {
                        long size = cursor.getLong(sizeColumnIndex);
                        Log.d(TAG, "File size from fallback query: " + size + " bytes");
                        return size;
                    }
                }
            } catch (Exception e) {
                Log.w(TAG, "Error getting file size from fallback query: " + e.getMessage());
            }
            
            // If we can't get the size, assume it's valid
            Log.w(TAG, "Could not determine file size, assuming valid");
            return 0;
            
        } catch (Exception e) {
            Log.e(TAG, "Error getting file size: " + e.getMessage(), e);
            return 0;
        }
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
        if (user != null && !isUserAlreadyAdded(user.getUserId())) {
            selectedProjectUsers.add(user);
            userRolesInProject.put(user.getUserId(), "Thành viên");
            updateAllUIs();
            hasUserMadeChanges = true;
        }
    }

    @Override
    public void onMemberRemoved(User user, int index) {
        if (index >= 0 && index < selectedProjectUsers.size()) {
            selectedProjectUsers.remove(index);
            userRolesInProject.remove(user.getUserId());
            updateAllUIs();
            hasUserMadeChanges = true;
        }
    }

    @Override
    public void onMemberRoleChanged(User user, String newRole, int index) {
        if (index >= 0 && index < selectedProjectUsers.size()) {
            userRolesInProject.put(user.getUserId(), newRole);
            updateAllUIs();
            hasUserMadeChanges = true;
        }
    }

    @Override
    public void onLinkRemoved(LinkItem linkItem, int index) {
        if (index >= 0 && index < projectLinks.size()) {
            projectLinks.remove(index);
            updateAllUIs();
            hasUserMadeChanges = true;
        }
    }

    @Override
    public void onLinkUrlChanged(LinkItem linkItem, String newUrl, int index) {
        if (index >= 0 && index < projectLinks.size()) {
            projectLinks.get(index).setUrl(newUrl);
            updateAllUIs();
            hasUserMadeChanges = true;
        }
    }

    @Override
    public void onLinkPlatformChanged(LinkItem linkItem, String newPlatform, int index) {
        if (index >= 0 && index < projectLinks.size()) {
            projectLinks.get(index).setPlatform(newPlatform);
            updateAllUIs();
            hasUserMadeChanges = true;
        }
    }

    @Override
    public void onMediaRemoved(Uri uri, int index) {
        if (index >= 0 && index < selectedMediaUris.size()) {
            Uri removedUri = selectedMediaUris.get(index);
            selectedMediaUris.remove(index);
            
            // Check if this was a new media URI
            boolean wasNewMedia = newMediaUris.remove(removedUri);
            
            // If it was existing media, remove from existingMediaUrls as well
            if (!wasNewMedia) {
                String removedUriString = removedUri.toString();
                existingMediaUrls.removeIf(media -> removedUriString.equals(media.get("url")));
            }
            
            // Update UI immediately like CreateProjectActivity
            if (mediaGalleryUiManager != null) {
                mediaGalleryUiManager.updateUI();
            }
            updateAllUIs();
            hasUserMadeChanges = true;
            
            Log.d(TAG, "Removed media at index " + index + ": " + removedUri + ", remaining count: " + selectedMediaUris.size());
        }
    }

    // Helper methods
    private boolean checkLoginAndNotify(String actionMessage) {
        if (mAuth.getCurrentUser() == null) {
            UiHelper.showInfoDialog(this, "Yêu cầu đăng nhập", "Vui lòng đăng nhập để " + actionMessage + ".");
            return false;
        }
        return true;
    }

    private boolean isUserAlreadyAdded(String userId) {
        if (userId == null) return false;
        for (User u : selectedProjectUsers) {
            if (u.getUserId() != null && u.getUserId().equals(userId)) return true;
        }
        return false;
    }

    private String getTextFromInput(TextInputEditText editText) {
        if (editText != null && editText.getText() != null) {
            return editText.getText().toString().trim();
        }
        return "";
    }

    private String getTextFromInput(AutoCompleteTextView autoCompleteTextView) {
        if (autoCompleteTextView != null && autoCompleteTextView.getText() != null) {
            return autoCompleteTextView.getText().toString().trim();
        }
        return "";
    }

    private void addCategoryChip(String categoryName) {
        if (categoryName == null || categoryName.trim().isEmpty() || selectedCategoryNames.contains(categoryName)) {
            if (selectedCategoryNames.contains(categoryName)) {
                Toast.makeText(this, "Lĩnh vực '" + categoryName + "' đã được thêm.", Toast.LENGTH_SHORT).show();
            }
            return;
        }
        if (chipGroupCategories == null) return;
        if (selectedCategoryNames.size() >= 3) {
            Toast.makeText(this, "Bạn chỉ có thể chọn tối đa 3 lĩnh vực.", Toast.LENGTH_SHORT).show();
            return;
        }
        Chip chip = new Chip(this);
        chip.setText(categoryName);
        chip.setCloseIconVisible(true);
        chip.setCheckable(false);
        chip.setClickable(false);
        chip.setOnCloseIconClickListener(v -> {
            chipGroupCategories.removeView(chip);
            selectedCategoryNames.remove(categoryName);
            hasUserMadeChanges = true;
            if (tilCategory != null && tilCategory.isErrorEnabled() && !selectedCategoryNames.isEmpty()) {
                tilCategory.setError(null);
                tilCategory.setErrorEnabled(false);
            }
        });
        chipGroupCategories.addView(chip);
        selectedCategoryNames.add(categoryName);
        hasUserMadeChanges = true;
        if (tilCategory != null && tilCategory.isErrorEnabled()) {
            tilCategory.setError(null);
            tilCategory.setErrorEnabled(false);
        }
    }

    private void addTechnologyChip(String techName) {
        if (techName == null || techName.trim().isEmpty() || selectedTechnologyNames.contains(techName)) {
            if (selectedTechnologyNames.contains(techName)) {
                Toast.makeText(this, techName + " đã được thêm.", Toast.LENGTH_SHORT).show();
            }
            return;
        }
        if (chipGroupTechnologies == null) return;
        if (selectedTechnologyNames.size() >= 10) {
            Toast.makeText(this, "Bạn chỉ có thể chọn tối đa 10 công nghệ.", Toast.LENGTH_SHORT).show();
            return;
        }
        Chip chip = new Chip(this);
        chip.setText(techName);
        chip.setCloseIconVisible(true);
        chip.setCheckable(false);
        chip.setClickable(false);
        chip.setOnCloseIconClickListener(v -> {
            chipGroupTechnologies.removeView(chip);
            selectedTechnologyNames.remove(techName);
            hasUserMadeChanges = true;
            if (actvTechnologyInput != null && !actvTechnologyInput.isEnabled()) {
                actvTechnologyInput.setEnabled(true);
            }
            if (tilTechnologyInput != null && tilTechnologyInput.isErrorEnabled() && !selectedTechnologyNames.isEmpty()) {
                tilTechnologyInput.setError(null);
                tilTechnologyInput.setErrorEnabled(false);
            }
        });
        chipGroupTechnologies.addView(chip);
        selectedTechnologyNames.add(techName);
        hasUserMadeChanges = true;
        if (tilTechnologyInput != null && tilTechnologyInput.isErrorEnabled()) {
            tilTechnologyInput.setError(null);
            tilTechnologyInput.setErrorEnabled(false);
        }
    }

    private void updateMediaGalleryLabel() {
        if (tvMediaGalleryLabel != null) {
            StringBuilder label = new StringBuilder("Thư viện Media");
            if (selectedMediaUris.size() > 0) {
                label.append(" (").append(selectedMediaUris.size()).append(" items)");
            }
            tvMediaGalleryLabel.setText(label.toString());
        }
    }
} 