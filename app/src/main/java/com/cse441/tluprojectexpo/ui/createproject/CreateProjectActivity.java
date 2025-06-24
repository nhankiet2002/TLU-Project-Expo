// CreateProjectActivity.java
package com.cse441.tluprojectexpo.ui.createproject;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.OnBackPressedCallback;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import com.bumptech.glide.Glide;
import com.cse441.tluprojectexpo.R;
import com.cse441.tluprojectexpo.model.LinkItem;
import com.cse441.tluprojectexpo.model.User;
import com.cse441.tluprojectexpo.ui.common.uimanager.AddedLinksUiManager;
import com.cse441.tluprojectexpo.ui.common.uimanager.MediaGalleryUiManager;
import com.cse441.tluprojectexpo.ui.common.uimanager.SelectedMembersUiManager;
import com.cse441.tluprojectexpo.service.CloudinaryUploadService;
import com.cse441.tluprojectexpo.service.FirestoreService;
import com.cse441.tluprojectexpo.repository.ProjectCreationService;
import com.cse441.tluprojectexpo.utils.ImagePickerDelegate;
import com.cse441.tluprojectexpo.utils.PermissionManager;
import com.cse441.tluprojectexpo.utils.UiHelper;
import com.cse441.tluprojectexpo.utils.Constants;

import com.google.android.flexbox.FlexboxLayout;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.chip.Chip;
import com.google.android.material.chip.ChipGroup;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;
import com.google.firebase.Timestamp;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;


public class CreateProjectActivity extends AppCompatActivity implements
        AddMemberDialogFragment.AddUserDialogListener,
        SelectedMembersUiManager.OnMemberInteractionListener,
        AddedLinksUiManager.OnLinkInteractionListener,
        MediaGalleryUiManager.OnMediaInteractionListener,
        ProjectCreationService.ProjectCreationListener {

    private static final String TAG = "CreateProjectActivity";

    // --- CỜ ĐỂ BẬT/TẮT CHẾ ĐỘ TEST VỚI USER GIẢ LẬP ---
    // private static final boolean IS_TESTING_WITH_MOCK_USER = true;
    private static final boolean IS_TESTING_WITH_MOCK_USER = false; // Chạy bình thường
    private static final String MOCK_USER_ID = "user_004";
    private static final String MOCK_USER_DISPLAY_NAME = "Kiet Debug User";
    private static final String MOCK_USER_EMAIL = "kiet.debug@example.com";
    // ----------------------------------------------------

    private TextInputEditText etProjectName, etProjectDescription;
    private AutoCompleteTextView actvCategoryInput, actvStatus;
    private TextInputLayout tilProjectName, tilProjectDescription, tilCategory, tilStatus;
    private ChipGroup chipGroupCategories;
    private ChipGroup chipGroupTechnologies;
    private AutoCompleteTextView actvTechnologyInput;
    private TextInputLayout tilTechnologyInput;
    private FrameLayout flProjectImageContainer;
    private ImageView ivProjectImagePreview, ivProjectImagePlaceholderIcon;
    private ImageView ivBackArrow;
    private MaterialButton btnAddMedia, btnAddMember, btnAddLink, btnCreateProject;
    private FlexboxLayout flexboxMediaPreviewContainer;
    private TextView tvMediaGalleryLabel;
    private LinearLayout llSelectedMembersContainer, llAddedLinksContainer;
    private LinearLayout llMemberSectionRoot, llLinkSectionRoot, llMediaSectionRoot;
    private ProgressBar pbCreatingProject;

    private Uri projectImageUri = null;
    private List<Uri> selectedMediaUris = new ArrayList<>();
    private List<User> selectedProjectUsers = new ArrayList<>();
    private Map<String, String> userRolesInProject = new HashMap<>();
    private List<LinkItem> projectLinks = new ArrayList<>();
    private List<String> selectedTechnologyNames = new ArrayList<>();
    private List<String> selectedCategoryNames = new ArrayList<>();

    private List<String> categoryNameListForDropdown = new ArrayList<>();
    private Map<String, String> categoryNameToIdMap = new HashMap<>();
    private List<String> statusNameListForDropdown = new ArrayList<>();
    private List<String> allAvailableTechnologyNames = new ArrayList<>();
    private Map<String, String> technologyNameToIdMap = new HashMap<>();

    private ArrayAdapter<String> categoryAdapter;
    private ArrayAdapter<String> statusAdapter;
    private ArrayAdapter<String> technologyAdapter;

    private FirebaseAuth mAuth;
    private PermissionManager permissionManager;
    private ActivityResultLauncher<String[]> permissionLauncher;
    private ImagePickerDelegate imagePickerDelegate;
    private CloudinaryUploadService cloudinaryUploadService;
    private FirestoreService firestoreService;
    private ProjectCreationService projectCreationService;

    private SelectedMembersUiManager selectedMembersUiManager;
    private AddedLinksUiManager addedLinksUiManager;
    private MediaGalleryUiManager mediaGalleryUiManager;

    private static final int ACTION_PICK_PROJECT_IMAGE = 1;
    private static final int ACTION_PICK_MEDIA = 2;
    private int currentPickerAction;
    private boolean hasUserMadeChanges = false;

    // Hàm kiểm tra đăng nhập và hiển thị thông báo nếu cần
    // TẠM THỜI LUÔN TRẢ VỀ TRUE ĐỂ DEBUG VẤN ĐỀ KHÁC
    private boolean checkLoginAndNotify(String actionMessage) {
        if (mAuth.getCurrentUser() == null && !IS_TESTING_WITH_MOCK_USER) {
            if (!isFinishing() && !isDestroyed()) {
                UiHelper.showInfoDialog(this, "Yêu cầu đăng nhập", "Vui lòng đăng nhập để " + actionMessage + ".");
            }
            return false; // Vẫn trả về false để logic gọi nó biết là chưa đăng nhập
        }
        return true; // Đã đăng nhập hoặc đang test
    }
    // PHIÊN BẢN TẠM THỜI ĐỂ DEBUG VĂNG APP (LUÔN CHO PHÉP HÀNH ĐỘNG)
    // private boolean checkLoginAndNotify(String actionMessage) {
    //     Log.d(TAG, "checkLoginAndNotify called for: " + actionMessage + ". Current user: " + (mAuth.getCurrentUser() == null ? "null" : mAuth.getCurrentUser().getUid()));
    //     // Tạm thời luôn trả về true để không chặn hành động, giúp debug lỗi văng app khác nếu có
    //     return true;
    // }


    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_create_project);

        mAuth = FirebaseAuth.getInstance();
        firestoreService = new FirestoreService();
        projectCreationService = new ProjectCreationService();
        cloudinaryUploadService = new CloudinaryUploadService();

        initializeLaunchersAndHelpers();

        View rootView = findViewById(android.R.id.content);
        initializeViews(rootView);
        initializeUiManagers();
        setupAdaptersAndData();
        addCurrentUserAsMember();
        setupEventListeners();
        updateAllUIs();
        setupInputValidationListeners();
        UiHelper.synchronizeButtonWidthsAfterLayout(rootView, this::synchronizeButtonWidths);

        getOnBackPressedDispatcher().addCallback(this, new OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
                handleCustomBackPressed();
            }
        });
    }


    private void initializeLaunchersAndHelpers() {
        permissionLauncher = registerForActivityResult(
                new ActivityResultContracts.RequestMultiplePermissions(), this::handlePermissionResult);
        ActivityResultLauncher<Intent> projectImageLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(), this::handleProjectImageResult);
        ActivityResultLauncher<Intent> mediaLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(), this::handleMediaResult);
        permissionManager = new PermissionManager(this, permissionLauncher);
        imagePickerDelegate = new ImagePickerDelegate(projectImageLauncher, mediaLauncher);
    }

    private void initializeViews(@NonNull View view) {
        etProjectName = findViewById(R.id.et_project_name);
        etProjectDescription = findViewById(R.id.et_project_description);
        tilProjectName = findViewById(R.id.til_project_name);
        tilProjectDescription = findViewById(R.id.til_project_description);

        chipGroupCategories = findViewById(R.id.chip_group_categories);
        actvCategoryInput = findViewById(R.id.actv_category_input);
        tilCategory = findViewById(R.id.til_category);

        actvStatus = findViewById(R.id.actv_status);
        tilStatus = findViewById(R.id.til_status);
        chipGroupTechnologies = findViewById(R.id.chip_group_technologies);
        actvTechnologyInput = findViewById(R.id.actv_technology_input);
        tilTechnologyInput = findViewById(R.id.til_technology_input);
        ivBackArrow = findViewById(R.id.iv_back_arrow);
        flProjectImageContainer = findViewById(R.id.fl_project_image_container);
        ivProjectImagePreview = findViewById(R.id.iv_project_image_preview);
        ivProjectImagePlaceholderIcon = findViewById(R.id.iv_project_image_placeholder_icon);
        llMemberSectionRoot = findViewById(R.id.ll_member_section_root);
        btnAddMember = findViewById(R.id.btn_add_member);
        llSelectedMembersContainer = findViewById(R.id.ll_selected_members_container);
        llLinkSectionRoot = findViewById(R.id.ll_link_section_root);
        btnAddLink = findViewById(R.id.btn_add_link);
        llAddedLinksContainer = findViewById(R.id.ll_added_links_container);
        llMediaSectionRoot = findViewById(R.id.ll_media_section_root);
        btnAddMedia = findViewById(R.id.btn_add_media);
        flexboxMediaPreviewContainer = findViewById(R.id.flexbox_media_preview_container);
        tvMediaGalleryLabel = findViewById(R.id.tv_media_gallery_label);
        btnCreateProject = findViewById(R.id.btn_create_project);

        pbCreatingProject = findViewById(R.id.pb_creating_project_activity);
        if (pbCreatingProject == null) {
            pbCreatingProject = new ProgressBar(this, null, android.R.attr.progressBarStyleLarge);
            ViewGroup rootView = (ViewGroup) findViewById(android.R.id.content).getRootView();
            if (rootView instanceof RelativeLayout || rootView instanceof FrameLayout) {
                RelativeLayout.LayoutParams params = new RelativeLayout.LayoutParams(
                        ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
                params.addRule(RelativeLayout.CENTER_IN_PARENT);
                pbCreatingProject.setLayoutParams(params);
                try { rootView.addView(pbCreatingProject); }
                catch (IllegalStateException e) { Log.w(TAG, "ProgressBar already added to root.", e); }
            } else {
                LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
                params.gravity = android.view.Gravity.CENTER;
                pbCreatingProject.setLayoutParams(params);
                if (rootView != null) {
                    try { rootView.addView(pbCreatingProject); }
                    catch (IllegalStateException e) { Log.w(TAG, "ProgressBar already added to root (LinearLayout fallback).", e); }
                }
            }
            pbCreatingProject.setZ(10f);
            pbCreatingProject.setVisibility(View.GONE);
        } else {
            pbCreatingProject.setVisibility(View.GONE);
        }
    }

    private void initializeUiManagers() {
        if (llSelectedMembersContainer != null) {
            selectedMembersUiManager = new SelectedMembersUiManager(this, llSelectedMembersContainer,
                    selectedProjectUsers, userRolesInProject, this);
        }
        if (llAddedLinksContainer != null) {
            addedLinksUiManager = new AddedLinksUiManager(this, llAddedLinksContainer,
                    projectLinks, this);
        }
        if (flexboxMediaPreviewContainer != null && tvMediaGalleryLabel != null) {
            mediaGalleryUiManager = new MediaGalleryUiManager(this, flexboxMediaPreviewContainer,
                    tvMediaGalleryLabel, selectedMediaUris, this);
        }
    }

    private void setupAdaptersAndData() {
        if (firestoreService == null) return;
        categoryAdapter = new ArrayAdapter<>(this, android.R.layout.simple_dropdown_item_1line, categoryNameListForDropdown);
        if (actvCategoryInput != null) actvCategoryInput.setAdapter(categoryAdapter);
        firestoreService.fetchCategories(new FirestoreService.CategoriesFetchListener() {
            @Override
            public void onCategoriesFetched(List<String> fetchedCategoryNames, Map<String, String> fetchedNameToIdMap) {
                categoryNameListForDropdown.clear(); categoryNameListForDropdown.addAll(fetchedCategoryNames);
                categoryNameToIdMap.clear(); categoryNameToIdMap.putAll(fetchedNameToIdMap);
                if (categoryAdapter != null) {
                    categoryAdapter.getFilter().filter(null);
                    categoryAdapter.notifyDataSetChanged();
                }
            }
            @Override
            public void onError(String errorMessage) {
                UiHelper.showToast(CreateProjectActivity.this, "Lỗi tải danh mục: " + errorMessage, Toast.LENGTH_SHORT);
            }
        });
        statusNameListForDropdown.clear();
        statusNameListForDropdown.addAll(Arrays.asList(getResources().getStringArray(R.array.project_statuses)));
        statusAdapter = new ArrayAdapter<>(this, android.R.layout.simple_dropdown_item_1line, statusNameListForDropdown);
        if (actvStatus != null) {
            actvStatus.setAdapter(statusAdapter);
            if (!statusNameListForDropdown.isEmpty()) actvStatus.setText(statusNameListForDropdown.get(0), false);
        }
        technologyAdapter = new ArrayAdapter<>(this, android.R.layout.simple_dropdown_item_1line, allAvailableTechnologyNames);
        if (actvTechnologyInput != null) actvTechnologyInput.setAdapter(technologyAdapter);
        firestoreService.fetchTechnologies(new FirestoreService.TechnologyFetchListener() {
            @Override
            public void onTechnologiesFetched(List<String> fetchedTechnologyNames, Map<String, String> fetchedTechNameToIdMap) {
                allAvailableTechnologyNames.clear(); allAvailableTechnologyNames.addAll(fetchedTechnologyNames);
                technologyNameToIdMap.clear(); technologyNameToIdMap.putAll(fetchedTechNameToIdMap);
                if (technologyAdapter != null) {
                    technologyAdapter.getFilter().filter(null);
                    technologyAdapter.notifyDataSetChanged();
                }
            }
            @Override
            public void onError(String errorMessage) {
                UiHelper.showToast(CreateProjectActivity.this, "Lỗi tải công nghệ: " + errorMessage, Toast.LENGTH_LONG);
            }
        });
    }

    private void setupEventListeners() {
        if (ivBackArrow != null) ivBackArrow.setOnClickListener(v -> handleCustomBackPressed());
        if (flProjectImageContainer != null) flProjectImageContainer.setOnClickListener(v -> {
            if (!checkLoginAndNotify("chọn ảnh bìa")) return;
            currentPickerAction = ACTION_PICK_PROJECT_IMAGE;
            if (permissionManager != null && permissionManager.checkAndRequestStoragePermissions()) {
                if (imagePickerDelegate != null) imagePickerDelegate.launchProjectImagePicker();
            }
        });
        if (btnAddMember != null) btnAddMember.setOnClickListener(v -> {
            if (!checkLoginAndNotify("thêm thành viên")) return;
            AddMemberDialogFragment dialog = AddMemberDialogFragment.newInstance();
            dialog.setDialogListener(this);
            dialog.show(getSupportFragmentManager(), "AddMemberDialog");
        });
        if (btnAddLink != null) btnAddLink.setOnClickListener(v -> {
            if (!checkLoginAndNotify("thêm liên kết")) return;
            boolean githubLinkExists = projectLinks.stream().anyMatch(item -> Constants.PLATFORM_GITHUB.equalsIgnoreCase(item.getPlatform()));
            boolean demoLinkExists = projectLinks.stream().anyMatch(item -> Constants.PLATFORM_DEMO.equalsIgnoreCase(item.getPlatform()));
            if (githubLinkExists && demoLinkExists) {
                UiHelper.showToast(this, "Bạn đã thêm đủ liên kết GitHub và Demo.", Toast.LENGTH_SHORT);
                return;
            }
            String platformToAdd = "";
            String[] availablePlatforms = getResources().getStringArray(R.array.link_platforms);
            if (!githubLinkExists && arrayContains(availablePlatforms, Constants.PLATFORM_GITHUB)) {
                platformToAdd = Constants.PLATFORM_GITHUB;
            } else if (!demoLinkExists && arrayContains(availablePlatforms, Constants.PLATFORM_DEMO)) {
                platformToAdd = Constants.PLATFORM_DEMO;
            }
            if (!platformToAdd.isEmpty()) {
                projectLinks.add(new LinkItem("", platformToAdd));
                if (addedLinksUiManager != null) addedLinksUiManager.updateUI();
                if (llAddedLinksContainer != null && llAddedLinksContainer.getChildCount() > 0) {
                    View lastLinkView = llAddedLinksContainer.getChildAt(llAddedLinksContainer.getChildCount() - 1);
                    TextInputEditText etUrl = lastLinkView.findViewById(R.id.et_added_link_url);
                    if (etUrl != null) etUrl.requestFocus();
                }
            } else {
                UiHelper.showToast(this, "Không thể thêm liên kết mới hoặc các platform đã được chọn.", Toast.LENGTH_SHORT);
            }
        });
        if (btnAddMedia != null) btnAddMedia.setOnClickListener(v -> {
            if (!checkLoginAndNotify("thêm media")) return;
            if (selectedMediaUris.size() >= 10) {
                UiHelper.showToast(this, "Bạn chỉ có thể thêm tối đa 10 media.", Toast.LENGTH_SHORT);
                return;
            }
            currentPickerAction = ACTION_PICK_MEDIA;
            if (permissionManager != null && permissionManager.checkAndRequestStoragePermissions()) {
                if (imagePickerDelegate != null) imagePickerDelegate.launchMediaPicker();
            }
        });
        if (btnCreateProject != null) {
            btnCreateProject.setOnClickListener(v -> {
                Log.d(TAG, "Nút Tạo dự án được nhấn.");
                if (!checkLoginAndNotify("tạo dự án")) {
                    return;
                }
                if (validateForm()) {
                    startProjectCreationProcess();
                } else {
                    UiHelper.showToast(this, "Vui lòng kiểm tra lại các thông tin đã nhập.", Toast.LENGTH_LONG);
                }
            });
        }

        // Listener cho Category Input - TẠM THỜI BỎ CHECK LOGIN ĐỂ DEBUG
        if (actvCategoryInput != null) {
            actvCategoryInput.setOnClickListener(v -> {
                // if (!checkLoginAndNotify("chọn lĩnh vực")) { hideKeyboard(); return; }
                if(!actvCategoryInput.isPopupShowing()) {
                    actvCategoryInput.showDropDown();
                }
            });
            actvCategoryInput.setOnFocusChangeListener((v, hasFocus) -> {
                if (hasFocus) {
                    // if (!checkLoginAndNotify("chọn lĩnh vực")) { hideKeyboard(); findViewById(android.R.id.content).getRootView().requestFocus(); return; }
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
                // if (!checkLoginAndNotify("thêm lĩnh vực")) { hideKeyboard(); return true; }
                if (actionId == EditorInfo.IME_ACTION_DONE || actionId == EditorInfo.IME_ACTION_NEXT) {
                    String currentText = actvCategoryInput.getText().toString().trim();
                    if (!currentText.isEmpty()) {
                        if (categoryNameListForDropdown.contains(currentText)) {
                            addCategoryChip(currentText);
                            actvCategoryInput.setText("");
                        } else {
                            UiHelper.showToast(this, "Lĩnh vực '" + currentText + "' không hợp lệ.", Toast.LENGTH_SHORT);
                        }
                        hideKeyboard();
                        return true;
                    }
                }
                return false;
            });
        }


        // Listener cho Technology Input - TẠM THỜI BỎ CHECK LOGIN ĐỂ DEBUG
        if (actvTechnologyInput != null) {
            actvTechnologyInput.setOnClickListener(v -> {
                // if (!checkLoginAndNotify("chọn công nghệ")) { hideKeyboard(); return; }
                if(!actvTechnologyInput.isPopupShowing()) {
                    actvTechnologyInput.showDropDown();
                }
            });
            actvTechnologyInput.setOnFocusChangeListener((v, hasFocus) -> {
                if (hasFocus) {
                    // if (!checkLoginAndNotify("chọn công nghệ")) { hideKeyboard(); findViewById(android.R.id.content).getRootView().requestFocus(); return; }
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
                // if (!checkLoginAndNotify("thêm công nghệ")) { hideKeyboard(); return true; }
                if (actionId == EditorInfo.IME_ACTION_DONE || actionId == EditorInfo.IME_ACTION_NEXT) {
                    String currentText = actvTechnologyInput.getText().toString().trim();
                    if (!currentText.isEmpty()) {
                        if (allAvailableTechnologyNames.contains(currentText)) {
                            addTechnologyChip(currentText);
                            actvTechnologyInput.setText("");
                        } else {
                            UiHelper.showToast(this, "Công nghệ '" + currentText + "' không hợp lệ.", Toast.LENGTH_SHORT);
                        }
                        hideKeyboard();
                        return true;
                    }
                }
                return false;
            });
        }

        UiHelper.setupDropdownToggle(tilCategory, actvCategoryInput);
        UiHelper.setupDropdownToggle(tilStatus, actvStatus);
        if (tilTechnologyInput != null && actvTechnologyInput != null) {
            UiHelper.setupDropdownToggle(tilTechnologyInput, actvTechnologyInput);
        }
    }

    private boolean arrayContains(String[] array, String value) { return Arrays.stream(array).anyMatch(s -> s.equalsIgnoreCase(value));}

    private void setupInputValidationListeners() {
        addTextWatcherToClearError(etProjectName, tilProjectName);
        addTextWatcherToClearError(etProjectDescription, tilProjectDescription);
        addTextWatcherToClearError(actvCategoryInput, tilCategory);
        addTextWatcherToClearError(actvTechnologyInput, tilTechnologyInput);
        TextWatcher changesWatcher = new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) { hasUserMadeChanges = true; }
            @Override public void afterTextChanged(Editable s) {}
        };
        etProjectName.addTextChangedListener(changesWatcher);
        etProjectDescription.addTextChangedListener(changesWatcher);
        actvCategoryInput.addTextChangedListener(changesWatcher);
        actvStatus.addTextChangedListener(changesWatcher);
        actvTechnologyInput.addTextChangedListener(changesWatcher);
    }
    private void addTextWatcherToClearError(TextInputEditText editText, TextInputLayout textInputLayout) {
        if (editText != null && textInputLayout != null) {
            editText.addTextChangedListener(new TextWatcher() {
                @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
                @Override public void onTextChanged(CharSequence s, int start, int before, int count) {
                    if (textInputLayout.isErrorEnabled()) {
                        textInputLayout.setError(null);
                        textInputLayout.setErrorEnabled(false);
                    }
                }
                @Override public void afterTextChanged(Editable s) {}
            });
        }
    }
    private void addTextWatcherToClearError(AutoCompleteTextView autoCompleteTextView, TextInputLayout textInputLayout) {
        if (autoCompleteTextView != null && textInputLayout != null) {
            autoCompleteTextView.addTextChangedListener(new TextWatcher() {
                @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
                @Override public void onTextChanged(CharSequence s, int start, int before, int count) {
                    if (textInputLayout.isErrorEnabled()) {
                        textInputLayout.setError(null);
                        textInputLayout.setErrorEnabled(false);
                    }
                }
                @Override public void afterTextChanged(Editable s) {}
            });
        }
    }
    private void hideKeyboard() {
        View view = getCurrentFocus();
        if (view != null ) {
            InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
            if (imm != null) imm.hideSoftInputFromWindow(view.getWindowToken(), 0);
        }
        View rootView = findViewById(android.R.id.content).getRootView();
        if(rootView != null) rootView.clearFocus();
    }
    private void handlePermissionResult(Map<String, Boolean> permissionsResult) {
        boolean allGranted = permissionsResult.values().stream().allMatch(b -> b);
        if (allGranted) {
            if (currentPickerAction == ACTION_PICK_PROJECT_IMAGE && imagePickerDelegate != null) imagePickerDelegate.launchProjectImagePicker();
            else if (currentPickerAction == ACTION_PICK_MEDIA && imagePickerDelegate != null) imagePickerDelegate.launchMediaPicker();
        } else {
            UiHelper.showToast(this, "Quyền truy cập bộ nhớ bị từ chối.", Toast.LENGTH_SHORT);
        }
    }
    private void handleProjectImageResult(androidx.activity.result.ActivityResult result) {
        if (result.getResultCode() == Activity.RESULT_OK && result.getData() != null) {
            projectImageUri = result.getData().getData(); hasUserMadeChanges = true;
            if (projectImageUri != null && ivProjectImagePreview != null && ivProjectImagePlaceholderIcon != null) {
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
                boolean alreadyExists = selectedMediaUris.stream().anyMatch(uri -> uri.equals(selectedMediaUri));
                if (!alreadyExists) {
                    selectedMediaUris.add(selectedMediaUri); hasUserMadeChanges = true;
                    if (mediaGalleryUiManager != null) mediaGalleryUiManager.updateUI();
                    UiHelper.showToast(this, "Đã thêm media.", Toast.LENGTH_SHORT);
                } else {
                    UiHelper.showToast(this, "Media này đã được thêm.", Toast.LENGTH_SHORT);
                }
            }
        }
    }

    private void addCategoryChip(String categoryName) {
        if (categoryName == null || categoryName.trim().isEmpty() || selectedCategoryNames.contains(categoryName)) {
            if (selectedCategoryNames.contains(categoryName)) UiHelper.showToast(this, "Lĩnh vực '" + categoryName + "' đã được thêm.", Toast.LENGTH_SHORT);
            return;
        }
        if (chipGroupCategories == null) return;
        if (selectedCategoryNames.size() >= 3) {
            UiHelper.showToast(this, "Bạn chỉ có thể chọn tối đa 3 lĩnh vực.", Toast.LENGTH_SHORT);
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
                tilCategory.setError(null); tilCategory.setErrorEnabled(false);
            }
        });
        chipGroupCategories.addView(chip);
        selectedCategoryNames.add(categoryName);
        hasUserMadeChanges = true;
        if (tilCategory != null && tilCategory.isErrorEnabled()) {
            tilCategory.setError(null); tilCategory.setErrorEnabled(false);
        }
    }

    private void addTechnologyChip(String techName) {
        if (techName == null || techName.trim().isEmpty() || selectedTechnologyNames.contains(techName)) {
            if (selectedTechnologyNames.contains(techName)) UiHelper.showToast(this, techName + " đã được thêm.", Toast.LENGTH_SHORT);
            return;
        }
        if (chipGroupTechnologies == null) return;
        if (selectedTechnologyNames.size() >= 10) {
            UiHelper.showToast(this, "Bạn chỉ có thể chọn tối đa 10 công nghệ.", Toast.LENGTH_SHORT);
            return;
        }
        Chip chip = new Chip(this);
        chip.setText(techName);
        chip.setCloseIconVisible(true);
        chip.setCheckable(false);
        chip.setClickable(false);
        chip.setOnCloseIconClickListener(v -> {
            chipGroupTechnologies.removeView(chip);
            selectedTechnologyNames.remove(techName); hasUserMadeChanges = true;
            if (actvTechnologyInput != null && !actvTechnologyInput.isEnabled()) actvTechnologyInput.setEnabled(true);
            if (tilTechnologyInput != null && tilTechnologyInput.isErrorEnabled() && !selectedTechnologyNames.isEmpty()) {
                tilTechnologyInput.setError(null); tilTechnologyInput.setErrorEnabled(false);
            }
        });
        chipGroupTechnologies.addView(chip);
        selectedTechnologyNames.add(techName); hasUserMadeChanges = true;
        if (tilTechnologyInput != null && tilTechnologyInput.isErrorEnabled()) {
            tilTechnologyInput.setError(null); tilTechnologyInput.setErrorEnabled(false);
        }
    }
    private void updateAllUIs() {  if (selectedMembersUiManager != null) selectedMembersUiManager.updateUI(); if (mediaGalleryUiManager != null) mediaGalleryUiManager.updateUI(); if (addedLinksUiManager != null) addedLinksUiManager.updateUI(); }

    private void addCurrentUserAsMember() {
        String userIdToUse;
        String displayNameForFallback;
        String emailForFallback;
        Uri photoUrlForFallback;

        if (IS_TESTING_WITH_MOCK_USER) {
            userIdToUse = MOCK_USER_ID;
            displayNameForFallback = MOCK_USER_DISPLAY_NAME;
            emailForFallback = MOCK_USER_EMAIL;
            photoUrlForFallback = null;
            Log.i(TAG, "--- CHẾ ĐỘ TEST: Giả lập người dùng '" + userIdToUse + "' ---");
        } else {
            FirebaseUser fbRealCurrentUser = mAuth.getCurrentUser();
            if (fbRealCurrentUser == null) {
                if (!selectedProjectUsers.isEmpty() || !userRolesInProject.isEmpty()) {
                    selectedProjectUsers.clear();
                    userRolesInProject.clear();
                    if (selectedMembersUiManager != null) selectedMembersUiManager.updateUI();
                }
                updateCreateButtonState();
                return;
            }
            userIdToUse = fbRealCurrentUser.getUid();
            displayNameForFallback = fbRealCurrentUser.getDisplayName();
            emailForFallback = fbRealCurrentUser.getEmail();
            photoUrlForFallback = fbRealCurrentUser.getPhotoUrl();
        }
        updateCreateButtonState();

        final String finalUserIdToUse = userIdToUse;
        final String finalDisplayNameForFallback = displayNameForFallback;
        final String finalEmailForFallback = emailForFallback;
        final Uri finalPhotoUrlForFallback = photoUrlForFallback;

        if (!isUserAlreadyAdded(finalUserIdToUse)) {
            if (firestoreService != null) {
                firestoreService.fetchUserDetails(finalUserIdToUse, new FirestoreService.UserDetailsFetchListener() {
                    @Override
                    public void onUserDetailsFetched(User userFromFirestore) {
                        if (isFinishing() || isDestroyed()) return;
                        userRolesInProject.put(userFromFirestore.getUserId(), Constants.DEFAULT_MEMBER_ROLE);
                        selectedProjectUsers.add(0, userFromFirestore); hasUserMadeChanges = true;
                        if (selectedMembersUiManager != null) selectedMembersUiManager.updateUI();
                    }
                    @Override
                    public void onUserNotFound() {
                        if (isFinishing() || isDestroyed()) return;
                        String nameToUse = (finalDisplayNameForFallback != null && !finalDisplayNameForFallback.isEmpty()) ? finalDisplayNameForFallback : ((finalEmailForFallback != null && finalEmailForFallback.contains("@")) ? finalEmailForFallback.split("@")[0] : "User");
                        User fallbackUser = new User(finalUserIdToUse, nameToUse, finalEmailForFallback, "N/A", finalPhotoUrlForFallback != null ? finalPhotoUrlForFallback.toString() : null);
                        userRolesInProject.put(finalUserIdToUse, Constants.DEFAULT_MEMBER_ROLE);
                        selectedProjectUsers.add(0, fallbackUser); hasUserMadeChanges = true;
                        if (selectedMembersUiManager != null) selectedMembersUiManager.updateUI();
                    }
                    @Override
                    public void onError(String errorMessage) {
                        if (isFinishing() || isDestroyed()) return;
                        String nameToUse = (finalDisplayNameForFallback != null && !finalDisplayNameForFallback.isEmpty()) ? finalDisplayNameForFallback : ((finalEmailForFallback != null && finalEmailForFallback.contains("@")) ? finalEmailForFallback.split("@")[0] : "User");
                        User fallbackUserOnError = new User(finalUserIdToUse, nameToUse, finalEmailForFallback, "N/A", finalPhotoUrlForFallback != null ? finalPhotoUrlForFallback.toString() : null);
                        userRolesInProject.put(finalUserIdToUse, Constants.DEFAULT_MEMBER_ROLE);
                        selectedProjectUsers.add(0, fallbackUserOnError); hasUserMadeChanges = true;
                        if (selectedMembersUiManager != null) selectedMembersUiManager.updateUI();
                        UiHelper.showToast(CreateProjectActivity.this, "Lỗi tải thông tin người dùng: " + errorMessage, Toast.LENGTH_SHORT);
                    }
                });
            }
        } else {
            if (userRolesInProject.get(finalUserIdToUse) == null) {
                userRolesInProject.put(finalUserIdToUse, Constants.DEFAULT_MEMBER_ROLE);
                hasUserMadeChanges = true;
            }
            User userInList = null; int userIndex = -1;
            for (int i = 0; i < selectedProjectUsers.size(); i++) {
                if (selectedProjectUsers.get(i).getUserId().equals(finalUserIdToUse)) {
                    userInList = selectedProjectUsers.get(i); userIndex = i; break;
                }
            }
            if (userInList != null && userIndex > 0) {
                selectedProjectUsers.remove(userIndex); selectedProjectUsers.add(0, userInList); hasUserMadeChanges = true;
            }
            if (selectedMembersUiManager != null) selectedMembersUiManager.updateUI();
        }
    }

    private boolean validateForm() {
        boolean isValid = true;
        String projectName = getTextFromInput(etProjectName);
        if (TextUtils.isEmpty(projectName)) {
            tilProjectName.setErrorEnabled(true); tilProjectName.setError("Tên dự án không được để trống."); isValid = false;
        } else if (projectName.length() < 5) {
            tilProjectName.setErrorEnabled(true); tilProjectName.setError("Tên dự án phải có ít nhất 5 ký tự."); isValid = false;
        } else {
            tilProjectName.setError(null); tilProjectName.setErrorEnabled(false);
        }
        String projectDescription = getTextFromInput(etProjectDescription);
        if (TextUtils.isEmpty(projectDescription)) {
            tilProjectDescription.setErrorEnabled(true); tilProjectDescription.setError("Mô tả dự án không được để trống."); isValid = false;
        } else if (projectDescription.length() < 20) {
            tilProjectDescription.setErrorEnabled(true); tilProjectDescription.setError("Mô tả dự án phải có ít nhất 20 ký tự."); isValid = false;
        } else {
            tilProjectDescription.setError(null); tilProjectDescription.setErrorEnabled(false);
        }

        if (selectedCategoryNames.isEmpty()) {
            tilCategory.setErrorEnabled(true);
            tilCategory.setError("Vui lòng chọn ít nhất một lĩnh vực.");
            if(actvCategoryInput != null) actvCategoryInput.requestFocus();
            isValid = false;
        } else {
            if (tilCategory != null) {
                tilCategory.setError(null);
                tilCategory.setErrorEnabled(false);
            }
        }

        if (selectedTechnologyNames.isEmpty()) {
            tilTechnologyInput.setErrorEnabled(true); tilTechnologyInput.setError("Vui lòng chọn ít nhất một công nghệ.");
            if(actvTechnologyInput != null) actvTechnologyInput.requestFocus();
            isValid = false;
        } else {
            tilTechnologyInput.setError(null); tilTechnologyInput.setErrorEnabled(false);
        }
        if (projectImageUri == null) {
            UiHelper.showToast(this, "Vui lòng chọn ảnh bìa cho dự án.", Toast.LENGTH_SHORT);
            if(flProjectImageContainer != null) flProjectImageContainer.requestFocus();
            isValid = false;
        }
        if (selectedProjectUsers.isEmpty()) {
            UiHelper.showToast(this, "Dự án phải có ít nhất một thành viên.", Toast.LENGTH_SHORT);
            isValid = false;
        }
        boolean hasLeader = userRolesInProject.values().stream().anyMatch(role -> Constants.DEFAULT_LEADER_ROLE.equalsIgnoreCase(role));
        if (!hasLeader && !selectedProjectUsers.isEmpty()) {
            UiHelper.showToast(this, "Vui lòng chọn một trưởng nhóm cho dự án.", Toast.LENGTH_LONG);
            isValid = false;
        }
        return isValid;
    }

    private void startProjectCreationProcess() {
        FirebaseUser currentUser = mAuth.getCurrentUser();
        String creatorUserIdToUse;
        if (IS_TESTING_WITH_MOCK_USER) {
            creatorUserIdToUse = MOCK_USER_ID;
        } else {
            if (currentUser == null) {
                Log.e(TAG, "startProjectCreationProcess: currentUser là null (không phải test).");
                UiHelper.showInfoDialog(this, "Lỗi", "Phiên đăng nhập không hợp lệ. Vui lòng đăng nhập lại.");
                showCreatingProgress(false, null);
                return;
            }
            creatorUserIdToUse = currentUser.getUid();
        }
        showCreatingProgress(true, "Đang xử lý dữ liệu...");
        cloudinaryUploadService.uploadThumbnail(projectImageUri,
                Constants.CLOUDINARY_UPLOAD_PRESET_THUMBNAIL,
                Constants.CLOUDINARY_FOLDER_PROJECT_THUMBNAILS,
                new CloudinaryUploadService.ThumbnailUploadListener() {
                    @Override
                    public void onThumbnailUploadSuccess(String thumbnailUrl) {
                        if (isFinishing() || isDestroyed()) return;
                        if (selectedMediaUris.isEmpty()) {
                            prepareAndExecuteProjectCreation(creatorUserIdToUse, thumbnailUrl, new ArrayList<>());
                            return;
                        }
                        cloudinaryUploadService.uploadMultipleMedia(selectedMediaUris,
                                Constants.CLOUDINARY_UPLOAD_PRESET_MEDIA,
                                Constants.CLOUDINARY_FOLDER_PROJECT_MEDIA,
                                new CloudinaryUploadService.MediaUploadListener() {
                                    @Override
                                    public void onAllMediaUploaded(List<Map<String, String>> uploadedMediaDetails) {
                                        if (isFinishing() || isDestroyed()) return;
                                        prepareAndExecuteProjectCreation(creatorUserIdToUse, thumbnailUrl, uploadedMediaDetails);
                                    }
                                    @Override public void onMediaUploadItemSuccess(String url, String resourceType, int currentIndex, int totalCount) {}
                                    @Override
                                    public void onMediaUploadItemError(String errorMessage, Uri erroredUri, int currentIndex, int totalCount) {
                                        if (isFinishing() || isDestroyed()) return;
                                        UiHelper.showToast(CreateProjectActivity.this, "Lỗi tải media: " + erroredUri.getLastPathSegment() + ". Item này sẽ bị bỏ qua.", Toast.LENGTH_LONG);
                                    }
                                    @Override
                                    public void onMediaUploadProgress(int processedCount, int totalCount) {
                                        if (isFinishing() || isDestroyed()) return;
                                        showCreatingProgress(true, "Đang tải media (" + processedCount + "/" + totalCount + ")");
                                    }
                                });
                    }
                    @Override
                    public void onThumbnailUploadError(String errorMessage) {
                        if (isFinishing() || isDestroyed()) return;
                        showCreatingProgress(false, null);
                        UiHelper.showInfoDialog(CreateProjectActivity.this, "Lỗi tải ảnh bìa", "Không thể tải ảnh bìa: " + errorMessage + ". Vui lòng thử lại.");
                    }
                });
    }

    private void prepareAndExecuteProjectCreation(String creatorUserId, String finalThumbnailUrl, List<Map<String, String>> uploadedMediaDetails) {
        if (isFinishing() || isDestroyed()) { showCreatingProgress(false, null); return; }
        showCreatingProgress(true, "Đang lưu dự án...");
        String projectName = getTextFromInput(etProjectName);
        String projectDescription = getTextFromInput(etProjectDescription);
        // String categoryName = getTextFromInput(actvCategoryInput); // Không dùng nữa
        String statusValue = getTextFromInput(actvStatus);
        Map<String, Object> projectData = new HashMap<>();
        projectData.put("Title", projectName);
        projectData.put("Description", projectDescription);
        projectData.put("Status", TextUtils.isEmpty(statusValue) ? (statusNameListForDropdown.isEmpty() ? "Đang thực hiện" : statusNameListForDropdown.get(0)) : statusValue);
        projectData.put("ThumbnailUrl", finalThumbnailUrl);
        projectData.put("CreatorUserId", creatorUserId);
        projectData.put("CreatedAt", new Timestamp(new Date()));
        projectData.put("UpdatedAt", new Timestamp(new Date()));
        projectData.put("IsApproved", false);
        projectData.put("IsFeatured", false);
        projectData.put("VoteCount", 0);

        List<Map<String, String>> mediaGalleryForFirestore = new ArrayList<>();
        if (uploadedMediaDetails != null && !uploadedMediaDetails.isEmpty()) {
            mediaGalleryForFirestore.addAll(uploadedMediaDetails);
        }
        projectData.put("MediaGalleryUrls", mediaGalleryForFirestore);
        String projectUrlFromUiLinks = null;
        String demoUrlFromUiLinks = null;
        for (LinkItem linkItem : projectLinks) {
            String url = linkItem.getUrl() != null ? linkItem.getUrl().trim() : "";
            String platform = linkItem.getPlatform() != null ? linkItem.getPlatform().trim() : "";
            if (!url.isEmpty() && android.util.Patterns.WEB_URL.matcher(url).matches()) {
                if (Constants.PLATFORM_GITHUB.equalsIgnoreCase(platform) && projectUrlFromUiLinks == null) {
                    projectUrlFromUiLinks = url;
                } else if (Constants.PLATFORM_DEMO.equalsIgnoreCase(platform) && demoUrlFromUiLinks == null) {
                    demoUrlFromUiLinks = url;
                }
            } else if (!url.isEmpty()) {
                Log.w(TAG, "URL không hợp lệ và sẽ được bỏ qua khi lưu: " + url + " cho platform " + platform);
            }
        }
        projectData.put("ProjectUrl", projectUrlFromUiLinks);
        projectData.put("DemoUrl", demoUrlFromUiLinks);

        List<String> selectedCategoryIds = selectedCategoryNames.stream()
                .map(name -> categoryNameToIdMap.get(name))
                .filter(id -> id != null && !id.isEmpty())
                .collect(Collectors.toList());

        List<Map<String, Object>> membersForFirestore = new ArrayList<>();
        for (User u : selectedProjectUsers) {
            if (u.getUserId() == null) continue;
            Map<String, Object> memberMap = new HashMap<>();
            memberMap.put(Constants.FIELD_USER_ID, u.getUserId());
            memberMap.put(Constants.FIELD_ROLE_IN_PROJECT, userRolesInProject.get(u.getUserId()));
            membersForFirestore.add(memberMap);
        }
        List<String> selectedTechnologyIds = selectedTechnologyNames.stream()
                .map(name -> technologyNameToIdMap.get(name))
                .filter(id -> id != null && !id.isEmpty())
                .collect(Collectors.toList());

        if (projectCreationService != null) {
            projectCreationService.createNewProject(projectData, membersForFirestore, selectedCategoryIds, selectedTechnologyIds, this);
        } else {
            Log.e(TAG, "projectCreationService is null! Cannot create project.");
            onProjectCreationFailed("Lỗi hệ thống khi tạo dự án.");
        }
    }

    @Override
    public void onProjectCreatedSuccessfully(String newProjectId) {
        if (isFinishing() || isDestroyed()) return;
        showCreatingProgress(false, null);
        new AlertDialog.Builder(this)
                .setTitle("Thành công")
                .setMessage("Dự án đã được tạo thành công! " +
                        "Vui lòng chờ hệ thống duyệt dự án nhé.")
                .setPositiveButton("Đóng", (dialog, which) -> {
                    clearForm();
                    updateCreateButtonState();
                })
                .setCancelable(false)
                .show();
    }
    @Override
    public void onProjectCreationFailed(String errorMessage) {
        if (isFinishing() || isDestroyed()) return;
        showCreatingProgress(false, null);
        UiHelper.showInfoDialog(this, "Lỗi", "Tạo dự án thất bại: " + errorMessage);
    }
    @Override
    public void onSubTaskError(String warningMessage) {
        if (isFinishing() || isDestroyed()) return;
        showCreatingProgress(false, null);
        new AlertDialog.Builder(this)
                .setTitle("Lưu ý")
                .setMessage("Dự án đã được tạo, tuy nhiên có một số thông tin phụ chưa được lưu đầy đủ: " + warningMessage + ". Bạn có thể chỉnh sửa dự án sau.")
                .setPositiveButton("Đóng", (dialog, which) -> {
                    clearForm();
                    updateCreateButtonState();
                })
                .setCancelable(false)
                .show();
    }

    private void updateCreateButtonState() {
        if (btnCreateProject != null) {
            btnCreateProject.setEnabled(true);
        }
    }

    private void showCreatingProgress(boolean show, @Nullable String message) {
        if (isFinishing() || isDestroyed()) return;
        if (pbCreatingProject != null) pbCreatingProject.setVisibility(show ? View.VISIBLE : View.GONE);
        if (show && message != null) {
            runOnUiThread(() -> UiHelper.showToast(this, message, Toast.LENGTH_SHORT));
        }
        if (btnCreateProject != null) {
            btnCreateProject.setEnabled(!show); // Chỉ disable khi đang show progress
            btnCreateProject.setText(show ? "Đang xử lý..." : "Tạo dự án");
        }
    }

    private void clearForm() {
        hasUserMadeChanges = false;
        if (etProjectName != null) etProjectName.setText("");
        if (tilProjectName != null) { tilProjectName.setError(null); tilProjectName.setErrorEnabled(false); }
        if (etProjectDescription != null) etProjectDescription.setText("");
        if (tilProjectDescription != null) { tilProjectDescription.setError(null); tilProjectDescription.setErrorEnabled(false); }

        if (chipGroupCategories != null) chipGroupCategories.removeAllViews();
        selectedCategoryNames.clear();
        if (actvCategoryInput != null) actvCategoryInput.setText("");
        if (tilCategory != null) { tilCategory.setError(null); tilCategory.setErrorEnabled(false); }


        if (actvStatus != null) {
            if (statusAdapter != null && !statusNameListForDropdown.isEmpty()) {
                actvStatus.setText(statusNameListForDropdown.get(0), false);
            } else {
                actvStatus.setText("", false);
            }
        }
        if (tilStatus != null) { tilStatus.setError(null); tilStatus.setErrorEnabled(false); }
        if (chipGroupTechnologies != null) chipGroupTechnologies.removeAllViews();
        selectedTechnologyNames.clear();
        if (actvTechnologyInput != null) actvTechnologyInput.setText("");
        if (tilTechnologyInput != null) { tilTechnologyInput.setError(null); tilTechnologyInput.setErrorEnabled(false); }
        projectImageUri = null;
        if (ivProjectImagePreview != null ) {
            Glide.with(this).clear(ivProjectImagePreview);
            ivProjectImagePreview.setImageDrawable(null);
            ivProjectImagePreview.setVisibility(View.GONE);
        }
        if (ivProjectImagePlaceholderIcon != null) ivProjectImagePlaceholderIcon.setVisibility(View.VISIBLE);
        if (flProjectImageContainer != null ) {
            try { flProjectImageContainer.setBackground(ContextCompat.getDrawable(this, R.drawable.image_placeholder_square));}
            catch (Exception e) { Log.e(TAG, "Error setting placeholder bg on clearForm", e); }
        }
        selectedMediaUris.clear();
        projectLinks.clear();
        selectedProjectUsers.clear();
        userRolesInProject.clear();
        addCurrentUserAsMember();
        updateAllUIs();
        updateCreateButtonState();
        if (etProjectName != null) etProjectName.requestFocus();
        hideKeyboard();
    }

    @Override public void onMemberRemoved(User user, int index) {
        if (user!= null && user.getUserId() != null && index >= 0 && index < selectedProjectUsers.size()) {
            hasUserMadeChanges = true;
            if (selectedProjectUsers.size() == 1) {
                UiHelper.showToast(this, "Dự án phải có ít nhất một thành viên.", Toast.LENGTH_SHORT);
                return;
            }
            User removedUser = selectedProjectUsers.remove(index);
            userRolesInProject.remove(removedUser.getUserId());
            if (selectedMembersUiManager != null) selectedMembersUiManager.updateUI();
            UiHelper.showToast(this, "Đã xóa " + removedUser.getFullName(), Toast.LENGTH_SHORT);
        }
    }
    @Override public void onMemberRoleChanged(User user, String newRole, int index) {
        if (user != null && user.getUserId() != null && newRole != null && index >= 0 && index < selectedProjectUsers.size()) {
            hasUserMadeChanges = true;
            if (Constants.DEFAULT_LEADER_ROLE.equals(newRole)) {
                for (Map.Entry<String, String> entry : userRolesInProject.entrySet()) {
                    if (Constants.DEFAULT_LEADER_ROLE.equals(entry.getValue()) && !entry.getKey().equals(user.getUserId())) {
                        userRolesInProject.put(entry.getKey(), Constants.DEFAULT_MEMBER_ROLE);
                    }
                }
            }
            userRolesInProject.put(user.getUserId(), newRole);
            if (selectedMembersUiManager != null) selectedMembersUiManager.updateUI();
        }
    }

    @Override
    public void onLinkRemoved(LinkItem linkItem, int index) {
        if (index >= 0 && index < projectLinks.size()) {
            hasUserMadeChanges = true;
            LinkItem removedLink = projectLinks.remove(index);
            if (addedLinksUiManager != null) {
                addedLinksUiManager.updateUI();
            }
            if (removedLink != null) {
                UiHelper.showToast(this, "Đã xóa liên kết: " + (TextUtils.isEmpty(removedLink.getUrl()) ? removedLink.getPlatform() : removedLink.getUrl()), Toast.LENGTH_SHORT);
            } else {
                UiHelper.showToast(this, "Đã xóa liên kết", Toast.LENGTH_SHORT);
            }
        }
    }

    @Override
    public void onLinkUrlChanged(LinkItem linkItem, String newUrl, int index) {
        if (linkItem != null && newUrl != null && index >= 0 && index < projectLinks.size()) {
            if(!linkItem.getUrl().equals(newUrl.trim())) hasUserMadeChanges = true;
            projectLinks.get(index).setUrl(newUrl.trim());
        }
    }

    @Override
    public void onLinkPlatformChanged(LinkItem linkItemBeingChanged, String newPlatform, int index) {
        if (linkItemBeingChanged == null || newPlatform == null || index < 0 || index >= projectLinks.size()) {
            return;
        }
        if(!linkItemBeingChanged.getPlatform().equalsIgnoreCase(newPlatform)) hasUserMadeChanges = true;
        for (int i = 0; i < projectLinks.size(); i++) {
            if (i == index) continue;
            LinkItem existingLink = projectLinks.get(i);
            if (newPlatform.equalsIgnoreCase(existingLink.getPlatform())) {
                UiHelper.showToast(this, "Platform '" + newPlatform + "' đã được sử dụng.", Toast.LENGTH_LONG);
                if (addedLinksUiManager != null) {
                    addedLinksUiManager.updateUI();
                }
                hasUserMadeChanges = false;
                return;
            }
        }
        projectLinks.get(index).setPlatform(newPlatform);
    }


    @Override public void onMediaRemoved(Uri uri, int index) {
        if (uri != null && index >= 0 && index < selectedMediaUris.size()) {
            selectedMediaUris.remove(index); hasUserMadeChanges = true;
            if (mediaGalleryUiManager != null) mediaGalleryUiManager.updateUI();
            UiHelper.showToast(this, "Đã xóa media.", Toast.LENGTH_SHORT);
        }
    }
    @Override public void onUserSelected(User user) {
        if (user == null || user.getUserId() == null) {
            UiHelper.showToast(this, "Lỗi chọn thành viên.", Toast.LENGTH_SHORT); return;
        }
        if (!isUserAlreadyAdded(user.getUserId())) {
            if (selectedProjectUsers.size() >= 7) {
                UiHelper.showToast(this, "Bạn chỉ có thể thêm tối đa 7 thành viên.", Toast.LENGTH_SHORT);
                return;
            }
            userRolesInProject.put(user.getUserId(), Constants.DEFAULT_MEMBER_ROLE);
            selectedProjectUsers.add(user); hasUserMadeChanges = true;
            if(selectedMembersUiManager != null) selectedMembersUiManager.updateUI();
            UiHelper.showToast(this, user.getFullName() + " đã được thêm.", Toast.LENGTH_SHORT);
        } else {
            UiHelper.showToast(this, user.getFullName() + " đã có trong danh sách.", Toast.LENGTH_SHORT);
        }
    }
    private boolean isUserAlreadyAdded(String userId) { if (userId == null) return false; for(User u : selectedProjectUsers) if(u.getUserId() != null && u.getUserId().equals(userId)) return true; return false; }
    private void synchronizeButtonWidths() { if (llMemberSectionRoot == null || llLinkSectionRoot == null || llMediaSectionRoot == null) return; int maxWidth = UiHelper.getMaxWidth(llMemberSectionRoot, llLinkSectionRoot, llMediaSectionRoot); if (maxWidth > 0) { if (llMemberSectionRoot.getVisibility() == View.VISIBLE) UiHelper.setViewWidth(llMemberSectionRoot, maxWidth); if (llLinkSectionRoot.getVisibility() == View.VISIBLE) UiHelper.setViewWidth(llLinkSectionRoot, maxWidth); if (llMediaSectionRoot.getVisibility() == View.VISIBLE) UiHelper.setViewWidth(llMediaSectionRoot, maxWidth); } }
    private String getTextFromInput(TextInputEditText editText) { if (editText != null && editText.getText() != null) return editText.getText().toString().trim(); return ""; }
    private String getTextFromInput(AutoCompleteTextView autoCompleteTextView) { if (autoCompleteTextView != null && autoCompleteTextView.getText() != null) return autoCompleteTextView.getText().toString().trim(); return ""; }

    private void handleCustomBackPressed() {
        if (hasUserMadeChanges || isAnyFieldNotEmpty()) {
            new AlertDialog.Builder(this)
                    .setTitle("Xác nhận thoát")
                    .setMessage("Bạn có thay đổi chưa lưu. Bạn có chắc chắn muốn thoát và hủy bỏ các thay đổi?")
                    .setPositiveButton("Thoát", (dialog, which) -> {
                        finish();
                    })
                    .setNegativeButton("Ở lại", null)
                    .show();
        } else {
            finish();
        }
    }

    private boolean isAnyFieldNotEmpty() {
        if (!TextUtils.isEmpty(getTextFromInput(etProjectName))) return true;
        if (!TextUtils.isEmpty(getTextFromInput(etProjectDescription))) return true;
        if (!selectedCategoryNames.isEmpty()) return true;
        if (projectImageUri != null) return true;
        if (!selectedMediaUris.isEmpty()) return true;
        if (projectLinks.stream().anyMatch(link -> !TextUtils.isEmpty(link.getUrl()))) return true;
        if (!selectedTechnologyNames.isEmpty()) return true;
        String currentAuthUserId = IS_TESTING_WITH_MOCK_USER ? MOCK_USER_ID : (mAuth.getCurrentUser() != null ? mAuth.getCurrentUser().getUid() : null);
        if (currentAuthUserId != null && selectedProjectUsers.size() > 1) return true;
        if (currentAuthUserId == null && !selectedProjectUsers.isEmpty()) return true;
        return false;
    }


    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (pbCreatingProject != null && pbCreatingProject.getParent() instanceof ViewGroup) {
            ((ViewGroup) pbCreatingProject.getParent()).removeView(pbCreatingProject);
        }
        pbCreatingProject = null;
        etProjectName=null; etProjectDescription=null;
        actvCategoryInput=null; actvStatus=null;
        tilCategory=null; tilStatus=null;
        tilProjectName=null; tilProjectDescription=null;
        chipGroupCategories=null; chipGroupTechnologies = null; actvTechnologyInput = null; tilTechnologyInput = null;
        ivBackArrow=null; flProjectImageContainer=null; ivProjectImagePreview=null; ivProjectImagePlaceholderIcon=null;
        btnAddMedia=null; btnAddMember=null; btnAddLink=null; btnCreateProject=null;
        flexboxMediaPreviewContainer=null; tvMediaGalleryLabel=null; llSelectedMembersContainer=null;
        llAddedLinksContainer=null; llMemberSectionRoot=null; llLinkSectionRoot=null; llMediaSectionRoot=null;
        selectedMembersUiManager = null;
        addedLinksUiManager = null;
        mediaGalleryUiManager = null;
    }
}