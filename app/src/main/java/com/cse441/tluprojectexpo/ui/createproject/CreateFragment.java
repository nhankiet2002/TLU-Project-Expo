package com.cse441.tluprojectexpo.ui.createproject;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.util.Log;
import android.view.LayoutInflater;
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

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.viewpager.widget.ViewPager;

import com.bumptech.glide.Glide;
import com.cse441.tluprojectexpo.R;
import com.cse441.tluprojectexpo.model.LinkItem;
import com.cse441.tluprojectexpo.model.User;
// Import các UI Manager
// Import các Service và Util
import com.cse441.tluprojectexpo.service.CloudinaryUploadService;
import com.cse441.tluprojectexpo.service.FirestoreService;
import com.cse441.tluprojectexpo.repository.project.ProjectCreationService;
import com.cse441.tluprojectexpo.ui.createproject.uimanager.AddedLinksUiManager;
import com.cse441.tluprojectexpo.ui.createproject.uimanager.MediaGalleryUiManager;
import com.cse441.tluprojectexpo.ui.createproject.uimanager.SelectedMembersUiManager;
import com.cse441.tluprojectexpo.util.ImagePickerDelegate;
import com.cse441.tluprojectexpo.util.PermissionManager;
import com.cse441.tluprojectexpo.util.UiHelper;
import com.cse441.tluprojectexpo.util.Constants;

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

public class CreateFragment extends Fragment implements
        AddMemberDialogFragment.AddUserDialogListener,
        SelectedMembersUiManager.OnMemberInteractionListener,
        AddedLinksUiManager.OnLinkInteractionListener,
        MediaGalleryUiManager.OnMediaInteractionListener,
        ProjectCreationService.ProjectCreationListener {

    private static final String TAG = "CreateFragment";

    // Views cho Form chính
    private TextInputEditText etProjectName, etProjectDescription;
    private AutoCompleteTextView actvCategory, actvStatus;
    private TextInputLayout tilProjectName, tilProjectDescription, tilCategory, tilStatus;

    // Views cho Công nghệ
    private ChipGroup chipGroupTechnologies;
    private AutoCompleteTextView actvTechnologyInput;
    private TextInputLayout tilTechnologyInput;

    // Views cho Ảnh bìa
    private FrameLayout flProjectImageContainer;
    private ImageView ivProjectImagePreview, ivProjectImagePlaceholderIcon;

    // Views cho các nút hành động và section khác
    private ImageView ivBackArrow;
    private MaterialButton btnAddMedia, btnAddMember, btnAddLink, btnCreateProject;
    private FlexboxLayout flexboxMediaPreviewContainer;
    private TextView tvMediaGalleryLabel;
    private LinearLayout llSelectedMembersContainer, llAddedLinksContainer;
    private LinearLayout llMemberSectionRoot, llLinkSectionRoot, llMediaSectionRoot;
    private ProgressBar pbCreatingProject;

    // Dữ liệu Form
    private Uri projectImageUri = null;
    private List<Uri> selectedMediaUris = new ArrayList<>();
    private List<User> selectedProjectUsers = new ArrayList<>();
    private Map<String, String> userRolesInProject = new HashMap<>(); // UserId -> Role
    private List<LinkItem> projectLinks = new ArrayList<>();
    private List<String> selectedTechnologyNames = new ArrayList<>();

    // Dữ liệu cho Dropdowns
    private List<String> categoryNameListForDropdown = new ArrayList<>();
    private Map<String, String> categoryNameToIdMap = new HashMap<>(); // Name -> ID
    private List<String> statusNameListForDropdown = new ArrayList<>();
    private List<String> allAvailableTechnologyNames = new ArrayList<>();
    private Map<String, String> technologyNameToIdMap = new HashMap<>(); // Name -> ID

    // Adapters
    private ArrayAdapter<String> categoryAdapter;
    private ArrayAdapter<String> statusAdapter;
    private ArrayAdapter<String> technologyAdapter;

    // Services và Helpers
    private FirebaseAuth mAuth;
    private PermissionManager permissionManager;
    private ImagePickerDelegate imagePickerDelegate;
    private CloudinaryUploadService cloudinaryUploadService;
    private FirestoreService firestoreService;
    private ProjectCreationService projectCreationService;

    // UI Managers
    private SelectedMembersUiManager selectedMembersUiManager;
    private AddedLinksUiManager addedLinksUiManager;
    private MediaGalleryUiManager mediaGalleryUiManager;

    // Constants cho Image Picker
    private static final int ACTION_PICK_PROJECT_IMAGE = 1;
    private static final int ACTION_PICK_MEDIA = 2;
    private int currentPickerAction;

    public CreateFragment() {
        // Required empty public constructor
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // Khởi tạo services
        mAuth = FirebaseAuth.getInstance();
        firestoreService = new FirestoreService();
        projectCreationService = new ProjectCreationService();
        cloudinaryUploadService = new CloudinaryUploadService();

        initializeLaunchersAndHelpers();
    }

    private void initializeLaunchersAndHelpers() {
        ActivityResultLauncher<String[]> permissionLauncher = registerForActivityResult(
                new ActivityResultContracts.RequestMultiplePermissions(), this::handlePermissionResult);
        ActivityResultLauncher<Intent> projectImageLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(), this::handleProjectImageResult);
        ActivityResultLauncher<Intent> mediaLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(), this::handleMediaResult);

        permissionManager = new PermissionManager(this, permissionLauncher);
        imagePickerDelegate = new ImagePickerDelegate(projectImageLauncher, mediaLauncher);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_create, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        initializeViews(view);
        initializeUiManagers(); // Khởi tạo các UI manager sau khi views đã được ánh xạ
        setupAdaptersAndData(); // Tải dữ liệu và cài đặt adapters cho dropdowns
        setupEventListeners();  // Cài đặt listeners cho các tương tác người dùng
        addCurrentUserAsMember(); // Tự động thêm người dùng hiện tại làm trưởng nhóm
        UiHelper.synchronizeButtonWidthsAfterLayout(view, this::synchronizeButtonWidths); // Đồng bộ chiều rộng nút
        updateAllUIs(); // Cập nhật giao diện của các UI manager
        setupInputValidationListeners(); // Thêm listener để xóa lỗi khi người dùng nhập
    }

    private void initializeViews(@NonNull View view) {
        // Ánh xạ các views từ layout
        etProjectName = view.findViewById(R.id.et_project_name);
        etProjectDescription = view.findViewById(R.id.et_project_description);
        tilProjectName = view.findViewById(R.id.til_project_name);
        tilProjectDescription = view.findViewById(R.id.til_project_description);
        actvCategory = view.findViewById(R.id.actv_category);
        actvStatus = view.findViewById(R.id.actv_status);
        tilCategory = view.findViewById(R.id.til_category);
        tilStatus = view.findViewById(R.id.til_status);

        chipGroupTechnologies = view.findViewById(R.id.chip_group_technologies);
        actvTechnologyInput = view.findViewById(R.id.actv_technology_input);
        tilTechnologyInput = view.findViewById(R.id.til_technology_input);

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

        // Khởi tạo ProgressBar động nếu chưa có trong layout
        if (pbCreatingProject == null && getContext() != null) {
            pbCreatingProject = new ProgressBar(getContext(), null, android.R.attr.progressBarStyleLarge);
            // Thiết lập LayoutParams cho ProgressBar để nó nằm giữa màn hình
            if (view instanceof RelativeLayout) {
                RelativeLayout.LayoutParams rlParams = new RelativeLayout.LayoutParams(
                        ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
                rlParams.addRule(RelativeLayout.CENTER_IN_PARENT);
                pbCreatingProject.setLayoutParams(rlParams);
            } else if (view instanceof FrameLayout) {
                FrameLayout.LayoutParams flParams = new FrameLayout.LayoutParams(
                        ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
                flParams.gravity = android.view.Gravity.CENTER;
                pbCreatingProject.setLayoutParams(flParams);
            } else { // Fallback cho LinearLayout hoặc ConstraintLayout (cần điều chỉnh)
                LinearLayout.LayoutParams llParams = new LinearLayout.LayoutParams(
                        ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
                llParams.gravity = android.view.Gravity.CENTER;
                pbCreatingProject.setLayoutParams(llParams);
            }

            // Thêm ProgressBar vào view gốc
            if (view instanceof ViewGroup) {
                try {
                    ((ViewGroup) view).addView(pbCreatingProject);
                } catch (IllegalStateException e) {
                    // ProgressBar có thể đã được thêm bởi một lần onViewCreated trước đó (ví dụ khi xoay màn hình mà fragment không bị hủy hoàn toàn)
                    Log.w(TAG, "ProgressBar already added or view hierarchy issue.", e);
                }
            }
            pbCreatingProject.setVisibility(View.GONE); // Ẩn ban đầu
        }
    }

    private void initializeUiManagers() {
        if (getContext() == null) {
            Log.e(TAG, "Context is null, cannot initialize UI Managers.");
            return;
        }
        // Khởi tạo các manager cho các phần UI động
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
        if (getContext() == null || firestoreService == null) {
            Log.e(TAG, "setupAdaptersAndData: Context or FirestoreService is null.");
            return;
        }

        // Category Adapter và lấy dữ liệu
        categoryAdapter = new ArrayAdapter<>(requireContext(), android.R.layout.simple_dropdown_item_1line, categoryNameListForDropdown);
        if (actvCategory != null) actvCategory.setAdapter(categoryAdapter);
        firestoreService.fetchCategories(new FirestoreService.CategoriesFetchListener() {
            @Override
            public void onCategoriesFetched(List<String> fetchedCategoryNames, Map<String, String> fetchedNameToIdMap) {
                if (!isAdded() || getContext() == null) return;
                categoryNameListForDropdown.clear();
                categoryNameListForDropdown.addAll(fetchedCategoryNames);
                categoryNameToIdMap.clear();
                categoryNameToIdMap.putAll(fetchedNameToIdMap);
                if (categoryAdapter != null) categoryAdapter.notifyDataSetChanged();
            }
            @Override
            public void onError(String errorMessage) {
                if (!isAdded() || getContext() == null) return;
                UiHelper.showToast(getContext(), "Lỗi tải danh mục: " + errorMessage, Toast.LENGTH_SHORT);
            }
        });

        // Status Adapter (lấy từ R.array.project_statuses)
        statusNameListForDropdown.clear();
        statusNameListForDropdown.addAll(Arrays.asList(getResources().getStringArray(R.array.project_statuses)));
        statusAdapter = new ArrayAdapter<>(requireContext(), android.R.layout.simple_dropdown_item_1line, statusNameListForDropdown);
        if (actvStatus != null) {
            actvStatus.setAdapter(statusAdapter);
            // Đặt giá trị mặc định cho Status nếu có
            if (!statusNameListForDropdown.isEmpty()) {
                actvStatus.setText(statusNameListForDropdown.get(0), false);
            }
        }

        // Technology Adapter và lấy dữ liệu
        technologyAdapter = new ArrayAdapter<>(requireContext(), android.R.layout.simple_dropdown_item_1line, allAvailableTechnologyNames);
        if (actvTechnologyInput != null) actvTechnologyInput.setAdapter(technologyAdapter);
        firestoreService.fetchTechnologies(new FirestoreService.TechnologyFetchListener() {
            @Override
            public void onTechnologiesFetched(List<String> fetchedTechnologyNames, Map<String, String> fetchedTechNameToIdMap) {
                if (!isAdded() || getContext() == null) return;
                allAvailableTechnologyNames.clear();
                allAvailableTechnologyNames.addAll(fetchedTechnologyNames);
                technologyNameToIdMap.clear();
                technologyNameToIdMap.putAll(fetchedTechNameToIdMap);
                if (technologyAdapter != null) {
                    technologyAdapter.getFilter().filter(null); // Force refresh dropdown
                    technologyAdapter.notifyDataSetChanged();
                }
            }
            @Override
            public void onError(String errorMessage) {
                if (!isAdded() || getContext() == null) return;
                UiHelper.showToast(getContext(), "Lỗi tải công nghệ: " + errorMessage, Toast.LENGTH_LONG);
            }
        });
    }

    private void setupEventListeners() {
        if (ivBackArrow != null) ivBackArrow.setOnClickListener(v -> navigateToHome());

        // Chọn ảnh bìa
        if (flProjectImageContainer != null) flProjectImageContainer.setOnClickListener(v -> {
            currentPickerAction = ACTION_PICK_PROJECT_IMAGE;
            if (permissionManager != null && permissionManager.checkAndRequestStoragePermissions()) {
                if (imagePickerDelegate != null) imagePickerDelegate.launchProjectImagePicker();
            }
        });

        // Thêm thành viên
        if (btnAddMember != null) btnAddMember.setOnClickListener(v -> {
            if (isAdded() && getChildFragmentManager() != null) {
                AddMemberDialogFragment.newInstance().show(getChildFragmentManager(), "AddMemberDialog");
            }
        });

        // Thêm link
        if (btnAddLink != null) btnAddLink.setOnClickListener(v -> {
            if (getContext() == null) return;
            if (projectLinks.size() >= 5) { // Giới hạn số link
                UiHelper.showToast(getContext(), "Bạn chỉ có thể thêm tối đa 5 liên kết.", Toast.LENGTH_SHORT);
                return;
            }
            projectLinks.add(new LinkItem("", getResources().getStringArray(R.array.link_platforms)[0]));
            if (addedLinksUiManager != null) addedLinksUiManager.updateUI();
            // Focus vào trường URL của link vừa thêm
            if (llAddedLinksContainer != null && llAddedLinksContainer.getChildCount() > 0) {
                View lastLinkView = llAddedLinksContainer.getChildAt(llAddedLinksContainer.getChildCount() - 1);
                TextInputEditText etUrl = lastLinkView.findViewById(R.id.et_added_link_url);
                if (etUrl != null) etUrl.requestFocus();
            }
        });

        // Thêm media
        if (btnAddMedia != null) btnAddMedia.setOnClickListener(v -> {
            if (selectedMediaUris.size() >= 10) { // Giới hạn số media
                UiHelper.showToast(getContext(), "Bạn chỉ có thể thêm tối đa 10 media.", Toast.LENGTH_SHORT);
                return;
            }
            currentPickerAction = ACTION_PICK_MEDIA;
            if (permissionManager != null && permissionManager.checkAndRequestStoragePermissions()) {
                if (imagePickerDelegate != null) imagePickerDelegate.launchMediaPicker();
            }
        });

        // Tạo dự án
        if (btnCreateProject != null) {
            btnCreateProject.setOnClickListener(v -> {
                Log.d(TAG, "Nút Tạo dự án được nhấn.");
                if (validateForm()) { // Gọi hàm validate trước khi tạo
                    startProjectCreationProcess();
                } else {
                    UiHelper.showToast(getContext(), "Vui lòng kiểm tra lại các thông tin đã nhập.", Toast.LENGTH_LONG);
                }
            });
        }

        // Xử lý nhập liệu và chọn công nghệ
        if (actvTechnologyInput != null) {
            actvTechnologyInput.setOnItemClickListener((parent, view, position, id) -> {
                String selectedTech = (String) parent.getItemAtPosition(position);
                addTechnologyChip(selectedTech);
                actvTechnologyInput.setText(""); // Xóa text sau khi chọn
                actvTechnologyInput.dismissDropDown();
                hideKeyboard();
            });
            actvTechnologyInput.setOnEditorActionListener((v, actionId, event) -> {
                if (actionId == EditorInfo.IME_ACTION_DONE || actionId == EditorInfo.IME_ACTION_NEXT) {
                    String currentText = actvTechnologyInput.getText().toString().trim();
                    if (!currentText.isEmpty()) {
                        // Chỉ cho phép thêm nếu có trong danh sách gợi ý (ngăn người dùng nhập tự do công nghệ mới)
                        if (allAvailableTechnologyNames.contains(currentText)) {
                            addTechnologyChip(currentText);
                            actvTechnologyInput.setText("");
                        } else {
                            UiHelper.showToast(getContext(), "Công nghệ '" + currentText + "' không hợp lệ. Vui lòng chọn từ danh sách.", Toast.LENGTH_SHORT);
                        }
                        hideKeyboard();
                        return true;
                    }
                }
                return false;
            });
            actvTechnologyInput.setOnFocusChangeListener((v, hasFocus) -> {
                if (!hasFocus && actvTechnologyInput.isPopupShowing()) {
                    actvTechnologyInput.dismissDropDown();
                }
            });
        }

        // Setup toggle cho các dropdowns
        UiHelper.setupDropdownToggle(tilCategory, actvCategory);
        UiHelper.setupDropdownToggle(tilStatus, actvStatus);
        if (tilTechnologyInput != null && actvTechnologyInput != null) {
            UiHelper.setupDropdownToggle(tilTechnologyInput, actvTechnologyInput);
        }
    }

    /**
     * Thêm TextWatcher để tự động xóa lỗi khi người dùng bắt đầu nhập liệu.
     */
    private void setupInputValidationListeners() {
        addTextWatcherToClearError(etProjectName, tilProjectName);
        addTextWatcherToClearError(etProjectDescription, tilProjectDescription);
        // Với AutoCompleteTextView, lỗi thường được set khi validate,
        // và xóa khi người dùng chọn item hoặc khi form được validate lại.
        // Tuy nhiên, có thể thêm watcher nếu muốn xóa lỗi ngay khi text thay đổi.
        addTextWatcherToClearError(actvCategory, tilCategory);
        // actvTechnologyInput không trực tiếp hiển thị lỗi, tilTechnologyInput sẽ hiển thị
        addTextWatcherToClearError(actvTechnologyInput, tilTechnologyInput);

    }

    private void addTextWatcherToClearError(TextInputEditText editText, TextInputLayout textInputLayout) {
        if (editText != null && textInputLayout != null) {
            editText.addTextChangedListener(new TextWatcher() {
                @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
                @Override public void onTextChanged(CharSequence s, int start, int before, int count) {
                    if (textInputLayout.getError() != null) {
                        textInputLayout.setError(null); // Xóa lỗi
                        textInputLayout.setErrorEnabled(false); // Quan trọng: Tắt chế độ lỗi để không chiếm không gian
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
                    if (textInputLayout.getError() != null) {
                        textInputLayout.setError(null);
                        textInputLayout.setErrorEnabled(false);
                    }
                }
                @Override public void afterTextChanged(Editable s) {}
            });
        }
    }


    private void hideKeyboard() {
        View view = getActivity() != null ? getActivity().getCurrentFocus() : null;
        if (view != null && getContext() != null) {
            InputMethodManager imm = (InputMethodManager) getContext().getSystemService(Context.INPUT_METHOD_SERVICE);
            if (imm != null) {
                imm.hideSoftInputFromWindow(view.getWindowToken(), 0);
            }
        }
        if (etProjectName != null) etProjectName.clearFocus(); // Bỏ focus khỏi các trường để bàn phím chắc chắn ẩn
        if (etProjectDescription != null) etProjectDescription.clearFocus();
        if (actvTechnologyInput != null) actvTechnologyInput.clearFocus();

    }

    // --- Xử lý kết quả từ Image Pickers ---
    private void handlePermissionResult(Map<String, Boolean> permissionsResult) {
        // (Giữ nguyên logic)
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
        // (Giữ nguyên logic)
        if (result.getResultCode() == Activity.RESULT_OK && result.getData() != null) {
            projectImageUri = result.getData().getData();
            if (projectImageUri != null && getContext() != null && ivProjectImagePreview != null && ivProjectImagePlaceholderIcon != null) {
                Glide.with(this).load(projectImageUri).centerCrop().into(ivProjectImagePreview);
                ivProjectImagePreview.setVisibility(View.VISIBLE);
                ivProjectImagePlaceholderIcon.setVisibility(View.GONE);
                if (flProjectImageContainer != null) flProjectImageContainer.setBackground(null);
                // Xóa lỗi nếu có (liên quan đến ảnh bìa)
                if (flProjectImageContainer.getTag() instanceof TextInputLayout) {
                    ((TextInputLayout)flProjectImageContainer.getTag()).setError(null);
                    ((TextInputLayout)flProjectImageContainer.getTag()).setErrorEnabled(false);
                }
            }
        }
    }

    private void handleMediaResult(androidx.activity.result.ActivityResult result) {
        // (Giữ nguyên logic, có thể thêm kiểm tra trùng lặp URI)
        if (result.getResultCode() == Activity.RESULT_OK && result.getData() != null) {
            Uri selectedMediaUri = result.getData().getData();
            if (selectedMediaUri != null) {
                boolean alreadyExists = false;
                for (Uri existingUri : selectedMediaUris) {
                    if (existingUri.equals(selectedMediaUri)) {
                        alreadyExists = true;
                        break;
                    }
                }
                if (!alreadyExists) {
                    selectedMediaUris.add(selectedMediaUri);
                    if (mediaGalleryUiManager != null) mediaGalleryUiManager.updateUI();
                    UiHelper.showToast(getContext(), "Đã thêm media.", Toast.LENGTH_SHORT);
                } else {
                    UiHelper.showToast(getContext(), "Media này đã được thêm.", Toast.LENGTH_SHORT);
                }
            }
        }
    }

    // --- Logic thêm và quản lý Công nghệ (Chips) ---
    private void addTechnologyChip(String techName) {
        if (techName == null || techName.trim().isEmpty() || selectedTechnologyNames.contains(techName)) {
            if (selectedTechnologyNames.contains(techName)) {
                UiHelper.showToast(getContext(), techName + " đã được thêm.", Toast.LENGTH_SHORT);
            }
            return;
        }
        if (getContext() == null || chipGroupTechnologies == null) return;
        if (selectedTechnologyNames.size() >= 10) { // Giới hạn số công nghệ
            UiHelper.showToast(getContext(), "Bạn chỉ có thể chọn tối đa 10 công nghệ.", Toast.LENGTH_SHORT);
            return;
        }

        Chip chip = new Chip(getContext());
        chip.setText(techName);
        chip.setCloseIconVisible(true);
        chip.setCheckable(false);
        chip.setClickable(false);
        chip.setOnCloseIconClickListener(v -> {
            chipGroupTechnologies.removeView(chip);
            selectedTechnologyNames.remove(techName);
            // Kích hoạt lại AutoCompleteTextView nếu nó bị vô hiệu hóa
            if (actvTechnologyInput != null && !actvTechnologyInput.isEnabled()) {
                actvTechnologyInput.setEnabled(true);
            }
            // Xóa lỗi nếu có sau khi xóa chip (nếu lỗi liên quan đến số lượng)
            if (tilTechnologyInput != null && tilTechnologyInput.getError() != null && !selectedTechnologyNames.isEmpty()) {
                tilTechnologyInput.setError(null);
                tilTechnologyInput.setErrorEnabled(false);
            }
        });
        chipGroupTechnologies.addView(chip);
        selectedTechnologyNames.add(techName);
        // Xóa lỗi nếu có sau khi thêm chip thành công
        if (tilTechnologyInput != null && tilTechnologyInput.getError() != null) {
            tilTechnologyInput.setError(null);
            tilTechnologyInput.setErrorEnabled(false);
        }
    }

    // --- Cập nhật UI và Thêm người dùng hiện tại ---
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
        if (btnCreateProject != null) btnCreateProject.setEnabled(true); // Kích hoạt nút nếu đã đăng nhập

        String currentUserId = fbCurrentUser.getUid();
        if (!isUserAlreadyAdded(currentUserId)) {
            if (firestoreService != null) {
                firestoreService.fetchUserDetails(currentUserId, new FirestoreService.UserDetailsFetchListener() {
                    @Override
                    public void onUserDetailsFetched(User user) {
                        if (!isAdded() || getContext() == null) return;
                        userRolesInProject.put(currentUserId, Constants.DEFAULT_LEADER_ROLE);
                        selectedProjectUsers.add(0, user); // Thêm vào đầu danh sách
                        if (selectedMembersUiManager != null) selectedMembersUiManager.updateUI();
                    }
                    @Override
                    public void onUserNotFound() {
                        if (!isAdded() || getContext() == null) return;
                        User fallbackUser = createFallbackUser(fbCurrentUser);
                        userRolesInProject.put(currentUserId, Constants.DEFAULT_LEADER_ROLE);
                        selectedProjectUsers.add(0, fallbackUser);
                        if (selectedMembersUiManager != null) selectedMembersUiManager.updateUI();
                    }
                    @Override
                    public void onError(String errorMessage) {
                        if (!isAdded() || getContext() == null) return;
                        User fallbackUser = createFallbackUser(fbCurrentUser);
                        userRolesInProject.put(currentUserId, Constants.DEFAULT_LEADER_ROLE);
                        selectedProjectUsers.add(0, fallbackUser);
                        if (selectedMembersUiManager != null) selectedMembersUiManager.updateUI();
                        UiHelper.showToast(getContext(), "Lỗi tải thông tin người dùng: " + errorMessage, Toast.LENGTH_SHORT);
                    }
                });
            }
        } else { // Đảm bảo người dùng hiện tại luôn là trưởng nhóm và ở đầu
            User currentUserInList = null;
            int currentUserIndex = -1;
            for(int i=0; i < selectedProjectUsers.size(); i++) {
                if(selectedProjectUsers.get(i).getUserId().equals(currentUserId)) {
                    currentUserInList = selectedProjectUsers.get(i);
                    currentUserIndex = i;
                    break;
                }
            }
            if(currentUserInList != null) {
                userRolesInProject.put(currentUserId, Constants.DEFAULT_LEADER_ROLE);
                if(currentUserIndex > 0) { // Nếu không ở đầu, đưa lên đầu
                    selectedProjectUsers.remove(currentUserIndex);
                    selectedProjectUsers.add(0, currentUserInList);
                }
                if (selectedMembersUiManager != null) selectedMembersUiManager.updateUI();
            }
        }
    }

    private User createFallbackUser(FirebaseUser fbUser) {
        String displayName = fbUser.getDisplayName();
        String email = fbUser.getEmail();
        String nameFromEmail = (email != null && email.contains("@")) ? email.split("@")[0] : "User";
        String fallbackName = (displayName != null && !displayName.isEmpty()) ? displayName : nameFromEmail;
        return new User(fbUser.getUid(), fallbackName, email, "N/A",
                fbUser.getPhotoUrl() != null ? fbUser.getPhotoUrl().toString() : null);
    }

    // --- Validation Form ---
    private boolean validateForm() {
        boolean isValid = true;

        // Validate Tên dự án
        String projectName = getTextFromInput(etProjectName);
        if (TextUtils.isEmpty(projectName)) {
            tilProjectName.setError("Tên dự án không được để trống.");
            isValid = false;
        } else if (projectName.length() < 5) {
            tilProjectName.setError("Tên dự án phải có ít nhất 5 ký tự.");
            isValid = false;
        } else {
            tilProjectName.setError(null);
            tilProjectName.setErrorEnabled(false);
        }

        // Validate Mô tả dự án
        String projectDescription = getTextFromInput(etProjectDescription);
        if (TextUtils.isEmpty(projectDescription)) {
            tilProjectDescription.setError("Mô tả dự án không được để trống.");
            isValid = false;
        } else if (projectDescription.length() < 20) {
            tilProjectDescription.setError("Mô tả dự án phải có ít nhất 20 ký tự.");
            isValid = false;
        } else {
            tilProjectDescription.setError(null);
            tilProjectDescription.setErrorEnabled(false);
        }

        // Validate Lĩnh vực
        if (TextUtils.isEmpty(getTextFromInput(actvCategory))) {
            tilCategory.setError("Vui lòng chọn một lĩnh vực.");
            isValid = false;
        } else {
            tilCategory.setError(null);
            tilCategory.setErrorEnabled(false);
        }

        // Validate Công nghệ
        if (selectedTechnologyNames.isEmpty()) {
            tilTechnologyInput.setError("Vui lòng chọn ít nhất một công nghệ.");
            actvTechnologyInput.requestFocus(); // Focus để người dùng dễ chọn
            isValid = false;
        } else {
            tilTechnologyInput.setError(null);
            tilTechnologyInput.setErrorEnabled(false);
        }

        // Validate Ảnh bìa
        if (projectImageUri == null) {
            // Không có TextInputLayout chuẩn cho ảnh, có thể hiển thị Toast hoặc thay đổi màu viền
            UiHelper.showToast(getContext(), "Vui lòng chọn ảnh bìa cho dự án.", Toast.LENGTH_SHORT);
            // Để trực quan hơn, có thể thêm một tag TextInputLayout ảo vào flProjectImageContainer nếu muốn set error
            // Hoặc đơn giản là focus vào nó (nếu nó focusable)
            flProjectImageContainer.requestFocus(); // Cần set focusable="true" trong XML
            isValid = false;
        }
        // Validate số lượng thành viên (ít nhất là trưởng nhóm)
        if (selectedProjectUsers.isEmpty()) {
            UiHelper.showToast(getContext(), "Dự án phải có ít nhất một thành viên (trưởng nhóm).", Toast.LENGTH_SHORT);
            // Có thể làm nổi bật nút "Thêm thành viên"
            isValid = false;
        }


        return isValid;
    }

    // --- Quá trình tạo dự án ---
    private void startProjectCreationProcess() {
        FirebaseUser currentUser = mAuth.getCurrentUser();
        if (currentUser == null) { // Kiểm tra lại phòng trường hợp
            UiHelper.showInfoDialog(getContext(), "Lỗi", "Vui lòng đăng nhập để tạo dự án.");
            return;
        }

        showCreatingProgress(true, "Đang xử lý dữ liệu...");
        if (btnCreateProject != null) btnCreateProject.setEnabled(false);

        cloudinaryUploadService.uploadThumbnail(projectImageUri,
                Constants.CLOUDINARY_UPLOAD_PRESET_THUMBNAIL,
                Constants.CLOUDINARY_FOLDER_PROJECT_THUMBNAILS,
                new CloudinaryUploadService.ThumbnailUploadListener() {
                    @Override
                    public void onThumbnailUploadSuccess(String thumbnailUrl) {
                        if (!isAdded() || getContext() == null) return;
                        cloudinaryUploadService.uploadMultipleMedia(selectedMediaUris,
                                Constants.CLOUDINARY_UPLOAD_PRESET_MEDIA,
                                Constants.CLOUDINARY_FOLDER_PROJECT_MEDIA,
                                new CloudinaryUploadService.MediaUploadListener() {
                                    @Override
                                    public void onAllMediaUploaded(List<String> uploadedMediaUrls) {
                                        if (!isAdded() || getContext() == null) return;
                                        prepareAndExecuteProjectCreation(currentUser, thumbnailUrl, uploadedMediaUrls);
                                    }
                                    @Override public void onMediaUploadItemSuccess(String url, int currentIndex, int totalCount) {}
                                    @Override
                                    public void onMediaUploadItemError(String errorMessage, Uri erroredUri, int currentIndex, int totalCount) {
                                        if (!isAdded() || getContext() == null) return;
                                        UiHelper.showToast(getContext(), "Lỗi tải media: " + erroredUri.getLastPathSegment() + ". Dự án vẫn sẽ được tạo.", Toast.LENGTH_LONG);
                                    }
                                    @Override
                                    public void onMediaUploadProgress(int processedCount, int totalCount) {
                                        if (!isAdded() || getContext() == null) return;
                                        showCreatingProgress(true, "Đang tải media (" + processedCount + "/" + totalCount + ")");
                                    }
                                });
                    }
                    @Override
                    public void onThumbnailUploadError(String errorMessage) {
                        if (!isAdded() || getContext() == null) return;
                        showCreatingProgress(false, null);
                        if (btnCreateProject != null) btnCreateProject.setEnabled(true);
                        UiHelper.showInfoDialog(getContext(), "Lỗi tải ảnh bìa", "Không thể tải ảnh bìa: " + errorMessage + ". Vui lòng thử lại.");
                    }
                });
    }

    private void prepareAndExecuteProjectCreation(FirebaseUser currentUser, String finalThumbnailUrl, List<String> finalMediaUrls) {
        if (!isAdded() || getContext() == null) {
            if (btnCreateProject != null) btnCreateProject.setEnabled(true);
            showCreatingProgress(false, null);
            return;
        }
        showCreatingProgress(true, "Đang lưu dự án...");

        // Lấy giá trị từ form một lần nữa để đảm bảo là mới nhất (mặc dù đã validate)
        String projectName = getTextFromInput(etProjectName);
        String projectDescription = getTextFromInput(etProjectDescription);
        String categoryName = getTextFromInput(actvCategory);
        String statusValue = getTextFromInput(actvStatus);

        Map<String, Object> projectData = new HashMap<>();
        projectData.put("Title", projectName);
        projectData.put("Description", projectDescription);
        projectData.put("Status", TextUtils.isEmpty(statusValue) ? (statusNameListForDropdown.isEmpty() ? "Đang thực hiện" : statusNameListForDropdown.get(0)) : statusValue);
        projectData.put("ThumbnailUrl", finalThumbnailUrl);
        projectData.put("CreatorUserId", currentUser.getUid());
        projectData.put("CreatedAt", new Timestamp(new Date()));
        projectData.put("UpdatedAt", new Timestamp(new Date()));
        projectData.put("IsApproved", false);
        projectData.put("VoteCount", 0);
        projectData.put("CourseId", null); // Hoặc giá trị mặc định

        // Xử lý MediaGalleryUrls (với type)
        List<Map<String, String>> mediaGalleryForFirestore = new ArrayList<>();
        if (finalMediaUrls != null && !finalMediaUrls.isEmpty()) {
            for (int i=0; i < finalMediaUrls.size(); i++) {
                String url = finalMediaUrls.get(i);
                Uri originalUri = (i < selectedMediaUris.size()) ? selectedMediaUris.get(i) : null;
                Map<String, String> mediaItem = new HashMap<>();
                mediaItem.put("url", url);
                mediaItem.put("type", getMediaType(originalUri));
                mediaGalleryForFirestore.add(mediaItem);
            }
        }
        projectData.put("MediaGalleryUrls", mediaGalleryForFirestore);

        // Xử lý ProjectUrl, DemoUrl từ projectLinks
        for (Map<String, String> linkMap : getValidProjectLinks()) {
            String platform = linkMap.get("platform").toLowerCase();
            String url = linkMap.get("url");
            if ("github".equals(platform) || "gitlab".equals(platform) || "bitbucket".equals(platform)) {
                if (projectData.get("ProjectUrl") == null) projectData.put("ProjectUrl", url);
            } else if ("demo".equals(platform) || "website".equals(platform)) {
                if (projectData.get("DemoUrl") == null) projectData.put("DemoUrl", url);
            }
        }

        String categoryIdToSave = categoryNameToIdMap.get(categoryName);

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
            projectCreationService.createNewProject(projectData, membersForFirestore, categoryIdToSave, selectedTechnologyIds, this);
        } else {
            Log.e(TAG, "projectCreationService is null! Cannot create project.");
            onProjectCreationFailed("Lỗi hệ thống khi tạo dự án.");
        }
    }

    private String getMediaType(Uri uri) {
        if (uri == null || getContext() == null || getContext().getContentResolver() == null) {
            return "image"; // Default
        }
        String mimeType = getContext().getContentResolver().getType(uri);
        if (mimeType != null) {
            if (mimeType.startsWith("video/")) return "video";
            if (mimeType.startsWith("image/")) return "image";
            // Thêm các loại khác nếu cần
        }
        return "image"; // Default nếu không xác định được
    }

    // --- Callbacks từ ProjectCreationService ---
    @Override
    public void onProjectCreatedSuccessfully(String newProjectId) {
        if (!isAdded() || getContext() == null) return;
        showCreatingProgress(false, null);
        if (btnCreateProject != null) btnCreateProject.setEnabled(true);
        UiHelper.showInfoDialog(getContext(), "Thành công", "Tạo dự án thành công!");
        clearFormAndNavigateToProfile();
    }

    @Override
    public void onProjectCreationFailed(String errorMessage) {
        if (!isAdded() || getContext() == null) return;
        showCreatingProgress(false, null);
        if (btnCreateProject != null) btnCreateProject.setEnabled(true);
        UiHelper.showInfoDialog(getContext(), "Lỗi", "Tạo dự án thất bại: " + errorMessage);
    }

    @Override
    public void onSubTaskError(String warningMessage) {
        if (!isAdded() || getContext() == null) return;
        showCreatingProgress(false, null);
        if (btnCreateProject != null) btnCreateProject.setEnabled(true);
        UiHelper.showInfoDialog(getContext(),"Lưu ý", "Dự án đã được tạo, tuy nhiên có một số thông tin phụ chưa được lưu đầy đủ: " + warningMessage + ". Bạn có thể chỉnh sửa dự án sau.");
        clearFormAndNavigateToProfile();
    }

    // --- Helper UI methods (Progress, Dialog, Navigation, Clear) ---
    private void showCreatingProgress(boolean show, @Nullable String message) {
        if (!isAdded() || getContext() == null) return;
        if (pbCreatingProject != null) pbCreatingProject.setVisibility(show ? View.VISIBLE : View.GONE);
        if (show && message != null && getActivity() != null) {
            getActivity().runOnUiThread(() -> UiHelper.showToast(getContext(), message, Toast.LENGTH_SHORT));
        }
        if (btnCreateProject != null) {
            btnCreateProject.setEnabled(!show);
            btnCreateProject.setText(show ? "Đang xử lý..." : "Tạo dự án");
        }
    }

    private void clearFormAndNavigateToProfile() { clearForm(); navigateToProfile(); }

    private void navigateToProfile() {
        if (getActivity() != null && isAdded()) {
            ViewPager viewPager = getActivity().findViewById(R.id.view_pager);
            if (viewPager != null) {
                viewPager.setCurrentItem(3, true); // Index 3 là ProfileFragment
            } else {
                UiHelper.showToast(getContext(), "Chuyển qua tab Profile.", Toast.LENGTH_SHORT);
            }
        }
    }

    private void navigateToHome() {
        if (getActivity() != null && isAdded()) {
            ViewPager viewPager = getActivity().findViewById(R.id.view_pager);
            if (viewPager != null) viewPager.setCurrentItem(0, true); // Index 0 là HomeFragment
        }
    }

    private void clearForm() {
        // Clear TextInputs and errors
        etProjectName.setText(""); tilProjectName.setError(null); tilProjectName.setErrorEnabled(false);
        etProjectDescription.setText(""); tilProjectDescription.setError(null); tilProjectDescription.setErrorEnabled(false);
        actvCategory.setText("", false); tilCategory.setError(null); tilCategory.setErrorEnabled(false);
        if (!statusNameListForDropdown.isEmpty()) actvStatus.setText(statusNameListForDropdown.get(0), false);
        else actvStatus.setText("", false);
        tilStatus.setError(null); tilStatus.setErrorEnabled(false);

        // Clear Technologies
        chipGroupTechnologies.removeAllViews();
        selectedTechnologyNames.clear();
        actvTechnologyInput.setText("");
        actvTechnologyInput.setEnabled(true); // Ensure enabled
        tilTechnologyInput.setError(null); tilTechnologyInput.setErrorEnabled(false);


        // Clear Project Image
        projectImageUri = null;
        if (ivProjectImagePreview != null) {
            Glide.with(this).clear(ivProjectImagePreview);
            ivProjectImagePreview.setImageDrawable(null);
            ivProjectImagePreview.setVisibility(View.GONE);
        }
        if (ivProjectImagePlaceholderIcon != null) ivProjectImagePlaceholderIcon.setVisibility(View.VISIBLE);
        if (flProjectImageContainer != null && getContext() != null) {
            try {
                flProjectImageContainer.setBackground(ContextCompat.getDrawable(requireContext(), R.drawable.image_placeholder_square));
            } catch (Exception e) { Log.e(TAG, "Error setting placeholder bg", e); }
        }

        // Clear lists and maps
        selectedMediaUris.clear();
        projectLinks.clear();
        selectedProjectUsers.clear();
        userRolesInProject.clear();

        // Re-add current user and update UIs
        addCurrentUserAsMember();
        updateAllUIs();

        // Request focus on the first field
        if (etProjectName != null) etProjectName.requestFocus();
        hideKeyboard(); // Ẩn bàn phím sau khi clear
    }

    // --- Callbacks từ UI Managers và Dialogs ---
    @Override public void onMemberRemoved(User user, int index) { /* (Giữ nguyên logic) */
        if (user!= null && user.getUserId() != null && index >= 0 && index < selectedProjectUsers.size()) {
            User removedUser = selectedProjectUsers.remove(index);
            userRolesInProject.remove(removedUser.getUserId());
            if (selectedMembersUiManager != null) selectedMembersUiManager.updateUI();
            UiHelper.showToast(getContext(), "Đã xóa " + removedUser.getFullName(), Toast.LENGTH_SHORT);
        }
    }
    @Override public void onMemberRoleChanged(User user, String newRole, int index) { /* (Giữ nguyên logic) */
        if (user != null && user.getUserId() != null && newRole != null && index >= 0 && index < selectedProjectUsers.size()) {
            userRolesInProject.put(user.getUserId(), newRole);
        }
    }
    @Override public void onLinkRemoved(LinkItem linkItem, int index) { /* (Giữ nguyên logic) */
        if (index >= 0 && index < projectLinks.size()) {
            projectLinks.remove(index);
            if (addedLinksUiManager != null) addedLinksUiManager.updateUI();
            UiHelper.showToast(getContext(), "Đã xóa liên kết", Toast.LENGTH_SHORT);
        }
    }
    @Override public void onLinkUrlChanged(LinkItem linkItem, String newUrl, int index) { /* (Giữ nguyên logic) */
        if (linkItem != null && newUrl != null && index >= 0 && index < projectLinks.size()) {
            projectLinks.get(index).setUrl(newUrl.trim());
        }
    }
    @Override public void onLinkPlatformChanged(LinkItem linkItem, String newPlatform, int index) { /* (Giữ nguyên logic) */
        if (linkItem != null && newPlatform != null && index >= 0 && index < projectLinks.size()) {
            projectLinks.get(index).setPlatform(newPlatform);
        }
    }
    @Override public void onMediaRemoved(Uri uri, int index) { /* (Giữ nguyên logic) */
        if (uri != null && index >= 0 && index < selectedMediaUris.size()) {
            selectedMediaUris.remove(index);
            if (mediaGalleryUiManager != null) mediaGalleryUiManager.updateUI();
            UiHelper.showToast(getContext(), "Đã xóa media.", Toast.LENGTH_SHORT);
        }
    }
    @Override public void onUserSelected(User user) { /* (Giữ nguyên logic, có thể thêm giới hạn số lượng) */
        if (user == null || user.getUserId() == null) {
            UiHelper.showToast(getContext(), "Lỗi chọn thành viên.", Toast.LENGTH_SHORT); return;
        }
        if (!isUserAlreadyAdded(user.getUserId())) {
            if (selectedProjectUsers.size() >= 7) { // Ví dụ giới hạn 7 thành viên
                UiHelper.showToast(getContext(), "Bạn chỉ có thể thêm tối đa 7 thành viên.", Toast.LENGTH_SHORT);
                return;
            }
            userRolesInProject.put(user.getUserId(), Constants.DEFAULT_MEMBER_ROLE);
            selectedProjectUsers.add(user);
            if(selectedMembersUiManager != null) selectedMembersUiManager.updateUI();
            UiHelper.showToast(getContext(), user.getFullName() + " đã được thêm.", Toast.LENGTH_SHORT);
        } else {
            UiHelper.showToast(getContext(), user.getFullName() + " đã có trong danh sách.", Toast.LENGTH_SHORT);
        }
    }

    // --- Helper methods khác ---
    private boolean isUserAlreadyAdded(String userId) { /* (Giữ nguyên logic) */
        if (userId == null) return false;
        for(User u : selectedProjectUsers) {
            if(u.getUserId() != null && u.getUserId().equals(userId)) return true;
        }
        return false;
    }

    public List<Map<String, String>> getValidProjectLinks() { /* (Giữ nguyên logic) */
        List<Map<String, String>> validLinksData = new ArrayList<>();
        for (LinkItem linkItem : projectLinks) {
            String url = linkItem.getUrl() != null ? linkItem.getUrl().trim() : "";
            String platform = linkItem.getPlatform() != null ? linkItem.getPlatform().trim() : "Khác";
            if (!url.isEmpty() && android.util.Patterns.WEB_URL.matcher(url).matches()) {
                Map<String, String> linkMap = new HashMap<>();
                linkMap.put("url", url);
                linkMap.put("platform", platform);
                validLinksData.add(linkMap);
            } else if (!url.isEmpty()){
                Log.w(TAG, "URL không hợp lệ và sẽ được bỏ qua: " + url);
            }
        }
        return validLinksData;
    }

    private void synchronizeButtonWidths() { /* (Giữ nguyên logic) */
        if (llMemberSectionRoot == null || llLinkSectionRoot == null || llMediaSectionRoot == null) return;
        int maxWidth = UiHelper.getMaxWidth(llMemberSectionRoot, llLinkSectionRoot, llMediaSectionRoot);
        if (maxWidth > 0) {
            if (llMemberSectionRoot.getVisibility() == View.VISIBLE) UiHelper.setViewWidth(llMemberSectionRoot, maxWidth);
            if (llLinkSectionRoot.getVisibility() == View.VISIBLE) UiHelper.setViewWidth(llLinkSectionRoot, maxWidth);
            if (llMediaSectionRoot.getVisibility() == View.VISIBLE) UiHelper.setViewWidth(llMediaSectionRoot, maxWidth);
        }
    }

    private String getTextFromInput(TextInputEditText editText) { /* (Giữ nguyên logic) */
        if (editText != null && editText.getText() != null) return editText.getText().toString().trim();
        return "";
    }
    private String getTextFromInput(AutoCompleteTextView autoCompleteTextView) { /* (Giữ nguyên logic) */
        if (autoCompleteTextView != null && autoCompleteTextView.getText() != null) return autoCompleteTextView.getText().toString().trim();
        return "";
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        // Gỡ ProgressBar nếu nó được thêm vào view gốc
        if (pbCreatingProject != null && pbCreatingProject.getParent() instanceof ViewGroup) {
            ((ViewGroup) pbCreatingProject.getParent()).removeView(pbCreatingProject);
        }
        pbCreatingProject = null;

        // Gán null cho các view để tránh memory leaks
        etProjectName=null; etProjectDescription=null;
        actvCategory=null; actvStatus=null;
        tilCategory=null; tilStatus=null;
        tilProjectName=null; tilProjectDescription=null;
        chipGroupTechnologies = null; actvTechnologyInput = null; tilTechnologyInput = null;
        ivBackArrow=null; flProjectImageContainer=null; ivProjectImagePreview=null; ivProjectImagePlaceholderIcon=null;
        btnAddMedia=null; btnAddMember=null; btnAddLink=null; btnCreateProject=null;
        flexboxMediaPreviewContainer=null; tvMediaGalleryLabel=null; llSelectedMembersContainer=null;
        llAddedLinksContainer=null; llMemberSectionRoot=null; llLinkSectionRoot=null; llMediaSectionRoot=null;
    }
}