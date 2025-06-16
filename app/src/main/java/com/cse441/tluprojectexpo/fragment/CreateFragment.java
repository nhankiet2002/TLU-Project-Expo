package com.cse441.tluprojectexpo.fragment;

import android.Manifest;
import android.app.Activity;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.MediaStore;
import android.text.Editable;
import android.text.TextWatcher;
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
import androidx.viewpager.widget.ViewPager;

import com.bumptech.glide.Glide;
import com.cse441.tluprojectexpo.R;
import com.cse441.tluprojectexpo.model.LinkItem;
import com.google.android.flexbox.FlexboxLayout;
import com.google.android.material.button.MaterialButton; // Used for remove link button
import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.QuerySnapshot;
import com.google.android.material.textfield.TextInputLayout;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;


import com.cse441.tluprojectexpo.model.User; // Sử dụng model User
// import com.cse441.tluprojectexpo.dialog.SuccessNotificationDialogFragment; // Bỏ comment nếu bạn có Dialog này

import com.google.firebase.Timestamp; // Đổi sang com.google.firebase.Timestamp


import java.util.Arrays; // Thêm import này

import java.util.HashSet; // Thêm import này


public class CreateFragment extends Fragment implements AddMemberDialogFragment.AddUserDialogListener {

    private static final String TAG = "CreateFragment";

    // --- ActivityResultLaunchers ---
    private ActivityResultLauncher<Intent> projectImagePickerLauncher;
    private ActivityResultLauncher<Intent> mediaPickerLauncher;
    private ActivityResultLauncher<String[]> requestPermissionLauncher;

    // --- Views ---
    private TextInputEditText etProjectName, etProjectDescription, etTechnology;
    private AutoCompleteTextView actvCategory, actvStatus;
    private TextInputLayout tilCategory, tilStatus, tilTechnology, tilProjectName, tilProjectDescription; // Thêm TextInputLayouts
    private ImageView ivBackArrow;
    private FrameLayout flProjectImageContainer;
    private ImageView ivProjectImagePreview, ivProjectImagePlaceholderIcon;
    private MaterialButton btnAddMedia, btnAddMember, btnAddLink, btnCreateProject;
    private FlexboxLayout flexboxMediaPreviewContainer;
    private TextView tvMediaGalleryLabel;
    private LinearLayout llSelectedMembersContainer, llAddedLinksContainer;
    private LinearLayout llMemberSectionRoot, llLinkSectionRoot, llMediaSectionRoot;

    // --- Data ---
    private Uri projectImageUri = null;
    private List<Uri> selectedMediaUris = new ArrayList<>();
    private List<User> selectedProjectUsers = new ArrayList<>();
    private Map<String, String> userRolesInProject = new HashMap<>(); // UserId -> Role
    private List<LinkItem> projectLinks = new ArrayList<>();
    private List<String> categoryList = new ArrayList<>();
    private List<String> statusList = new ArrayList<>();

    // --- Adapters ---
    private ArrayAdapter<String> categoryAdapter;
    private ArrayAdapter<String> statusAdapter;

    // --- Firebase ---
    private FirebaseFirestore db;
    private FirebaseAuth mAuth;

    // --- Constants for Permissions ---
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
        mAuth = FirebaseAuth.getInstance();

        // --- Initialize ActivityResultLaunchers ---
        requestPermissionLauncher = registerForActivityResult(new ActivityResultContracts.RequestMultiplePermissions(), permissions -> {
            boolean allGranted = true;
            for (Boolean granted : permissions.values()) {
                if (!granted) {
                    allGranted = false;
                    break;
                }
            }
            if (allGranted) {
                if (currentPickerAction == ACTION_PICK_PROJECT_IMAGE) launchProjectImagePicker();
                else if (currentPickerAction == ACTION_PICK_MEDIA) launchMediaPicker();
            } else {
                if (getContext() != null) Toast.makeText(getContext(), "Quyền truy cập bộ nhớ bị từ chối.", Toast.LENGTH_SHORT).show();
            }
        });

        projectImagePickerLauncher = registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), result -> {
            if (result.getResultCode() == Activity.RESULT_OK && result.getData() != null) {
                projectImageUri = result.getData().getData();
                if (projectImageUri != null && getContext() != null && ivProjectImagePreview != null && ivProjectImagePlaceholderIcon != null) {
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
                    if (getContext() != null) Toast.makeText(getContext(), "Đã thêm media.", Toast.LENGTH_SHORT).show();
                }
            }
        });
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_create, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        initializeViews(view);
        setupAdaptersAndData();
        setupListeners();
        addCurrentUserAsMember();
        synchronizeButtonWidthsAfterLayout(view);
        updateAllUIs(); // Cập nhật UI ban đầu
    }

    private void initializeViews(@NonNull View view) {
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
    }

    private void setupAdaptersAndData() {
        if (getContext() == null) return;

        categoryAdapter = new ArrayAdapter<>(requireContext(), android.R.layout.simple_dropdown_item_1line, categoryList);
        if (actvCategory != null) actvCategory.setAdapter(categoryAdapter);

        statusAdapter = new ArrayAdapter<>(requireContext(), android.R.layout.simple_dropdown_item_1line, statusList);
        if (actvStatus != null) actvStatus.setAdapter(statusAdapter);

        fetchDataFromFirestore("Categories", "Name", categoryList, categoryAdapter, "Lĩnh vực");
        // Giả sử collection cho status là "ProjectStatuses" và trường tên là "StatusName"
        // Nếu không có, bạn có thể hardcode list status:
        // statusList.addAll(Arrays.asList("Đang thực hiện", "Hoàn thành", "Tạm dừng"));
        // if (statusAdapter != null) statusAdapter.notifyDataSetChanged();
        fetchDataFromFirestore("ProjectStatuses", "StatusName", statusList, statusAdapter, "Trạng thái");

    }

    private void setupListeners() {
        if (ivBackArrow != null) {
            ivBackArrow.setOnClickListener(v -> navigateToHome());
        }
        if (flProjectImageContainer != null) {
            flProjectImageContainer.setOnClickListener(v -> {
                currentPickerAction = ACTION_PICK_PROJECT_IMAGE;
                if (checkAndRequestPermissions()) launchProjectImagePicker();
            });
        }
        if (btnAddMember != null) {
            btnAddMember.setOnClickListener(v -> {
                AddMemberDialogFragment addMemberDialog = AddMemberDialogFragment.newInstance();
                addMemberDialog.setDialogListener(this);
                // Sử dụng getChildFragmentManager() cho DialogFragment bên trong Fragment
                addMemberDialog.show(getChildFragmentManager(), "AddMemberDialog");
            });
        }
        if (btnAddLink != null) {
            btnAddLink.setOnClickListener(v -> {
                if (getContext() == null) return;
                String[] platforms = getResources().getStringArray(R.array.link_platforms);
                String defaultPlatform = platforms.length > 0 ? platforms[0] : "Khác";
                projectLinks.add(new LinkItem("", defaultPlatform));
                updateAddedLinksUI();
                // Focus vào URL của link mới thêm
                if (llAddedLinksContainer != null && llAddedLinksContainer.getChildCount() > 0) {
                    View lastLinkView = llAddedLinksContainer.getChildAt(llAddedLinksContainer.getChildCount() - 1);
                    TextInputEditText etNewUrl = lastLinkView.findViewById(R.id.et_added_link_url);
                    if (etNewUrl != null) etNewUrl.requestFocus();
                }
            });
        }
        if (btnAddMedia != null) {
            btnAddMedia.setOnClickListener(v -> {
                currentPickerAction = ACTION_PICK_MEDIA;
                if (checkAndRequestPermissions()) launchMediaPicker();
            });
        }
        if (btnCreateProject != null) {
            btnCreateProject.setOnClickListener(v -> createProject());
        }

        // Setup Dropdown Toggle Listeners
        setupDropdownToggle(tilCategory, actvCategory);
        setupDropdownToggle(tilStatus, actvStatus);
    }

    private void synchronizeButtonWidthsAfterLayout(View rootView) {
        if (rootView == null) return;
        rootView.getViewTreeObserver().addOnGlobalLayoutListener(new ViewTreeObserver.OnGlobalLayoutListener() {
            @Override
            public void onGlobalLayout() {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN) {
                    rootView.getViewTreeObserver().removeOnGlobalLayoutListener(this);
                } else {
                    //noinspection deprecation
                    rootView.getViewTreeObserver().removeGlobalOnLayoutListener(this);
                }
                synchronizeButtonWidths();
            }
        });
    }

    private void updateAllUIs() {
        updateSelectedMembersUI();
        updateMediaPreviewGallery();
        updateAddedLinksUI();
    }


    private void addCurrentUserAsMember() {
        FirebaseUser fbCurrentUser = mAuth.getCurrentUser();
        if (fbCurrentUser == null) {
            Log.w(TAG, "addCurrentUserAsMember: No Firebase user. Cannot add to project.");
            if (getContext() != null) Toast.makeText(getContext(), "Vui lòng đăng nhập để tạo dự án.", Toast.LENGTH_LONG).show();
            // Disable create button or navigate to login
            if (btnCreateProject != null) btnCreateProject.setEnabled(false);
            return;
        }
        if (btnCreateProject != null) btnCreateProject.setEnabled(true); // Enable if user is logged in

        String currentUserId = fbCurrentUser.getUid();
        String defaultRole = "Trưởng nhóm"; // Vai trò mặc định cho người tạo

        if (!isUserAlreadyAdded(currentUserId)) {
            db.collection("Users").document(currentUserId).get()
                    .addOnSuccessListener(documentSnapshot -> {
                        if (!isAdded() || getContext() == null) return; // Check fragment state
                        User currentUserApp = null;
                        if (documentSnapshot.exists()) {
                            currentUserApp = documentSnapshot.toObject(User.class);
                            if (currentUserApp != null) {
                                currentUserApp.setUserId(currentUserId); // Quan trọng: Gán ID từ document
                            }
                        }

                        if (currentUserApp == null) { // Fallback nếu không fetch được từ Firestore
                            Log.w(TAG, "Current user data not found in Firestore or failed to parse. Using basic FirebaseUser info.");
                            String userName = fbCurrentUser.getDisplayName();
                            if (userName == null || userName.isEmpty()) {
                                userName = fbCurrentUser.getEmail() != null ? fbCurrentUser.getEmail().split("@")[0] : "User";
                            }
                            String avatarUrl = fbCurrentUser.getPhotoUrl() != null ? fbCurrentUser.getPhotoUrl().toString() : null;
                            currentUserApp = new User();
                            currentUserApp.setUserId(currentUserId);
                            currentUserApp.setFullName(userName);
                            currentUserApp.setAvatarUrl(avatarUrl);
                            currentUserApp.setUserClass("N/A"); // Hoặc một giá trị mặc định
                        }

                        userRolesInProject.put(currentUserId, defaultRole);
                        selectedProjectUsers.add(0, currentUserApp);
                        updateSelectedMembersUI();
                    })
                    .addOnFailureListener(e -> {
                        Log.e(TAG, "Error fetching current user data from Firestore", e);
                        // Fallback nếu lỗi
                        if (!isAdded() || getContext() == null) return;
                        String userName = fbCurrentUser.getDisplayName();
                        if (userName == null || userName.isEmpty()) {
                            userName = fbCurrentUser.getEmail() != null ? fbCurrentUser.getEmail().split("@")[0] : "User";
                        }
                        String avatarUrl = fbCurrentUser.getPhotoUrl() != null ? fbCurrentUser.getPhotoUrl().toString() : null;
                        User fallbackUser = new User();
                        fallbackUser.setUserId(currentUserId);
                        fallbackUser.setFullName(userName);
                        fallbackUser.setAvatarUrl(avatarUrl);
                        fallbackUser.setUserClass("N/A");

                        userRolesInProject.put(currentUserId, defaultRole);
                        selectedProjectUsers.add(0, fallbackUser);
                        updateSelectedMembersUI();
                        Toast.makeText(getContext(), "Lỗi tải thông tin người dùng, sử dụng thông tin cơ bản.", Toast.LENGTH_SHORT).show();
                    });
        } else {
            // If current user is already added, ensure their role is "Trưởng nhóm" if not set or different
            // This logic might be more complex if users can be added then roles changed before creation
            userRolesInProject.put(currentUserId, defaultRole); // Ensure leader role
            updateSelectedMembersUI(); // Refresh UI in case role changed
        }
    }


    private void createProject() {
        FirebaseUser currentUser = mAuth.getCurrentUser();
        if (currentUser == null) {
            showNotification("Vui lòng đăng nhập để tạo dự án.");
            return;
        }

        String projectName = (etProjectName != null && etProjectName.getText() != null) ? etProjectName.getText().toString().trim() : "";
        String projectDescription = (etProjectDescription != null && etProjectDescription.getText() != null) ? etProjectDescription.getText().toString().trim() : "";
        String categoryName = (actvCategory != null && actvCategory.getText() != null) ? actvCategory.getText().toString().trim() : "";
        String technologyInput = (etTechnology != null && etTechnology.getText() != null) ? etTechnology.getText().toString().trim() : "";
        String statusName = (actvStatus != null && actvStatus.getText() != null) ? actvStatus.getText().toString().trim() : "";

        if (projectName.isEmpty()) { showNotification("Bạn chưa điền tên dự án."); return; }
        if (categoryName.isEmpty()) { showNotification("Bạn chưa chọn lĩnh vực."); return; }
        if (selectedProjectUsers.isEmpty()) { showNotification("Dự án cần có ít nhất một thành viên (bạn)."); return; }

        // Đảm bảo người tạo dự án là trưởng nhóm
        boolean creatorIsLeader = false;
        for(User u : selectedProjectUsers){
            if(u.getUserId() != null && u.getUserId().equals(currentUser.getUid()) &&
                    "Trưởng nhóm".equals(userRolesInProject.get(u.getUserId()))){
                creatorIsLeader = true;
                break;
            }
        }
        if(!creatorIsLeader){
            showNotification("Bạn (người tạo) phải là Trưởng nhóm. Vui lòng kiểm tra lại vai trò.");
            return;
        }


        // --- Dữ liệu sẽ lưu vào Firestore ---
        Map<String, Object> projectData = new HashMap<>();
        projectData.put("Title", projectName);
        projectData.put("Description", projectDescription);
        projectData.put("Status", statusName.isEmpty() ? "Đang thực hiện" : statusName); // Tên trạng thái
        projectData.put("ThumbnailUrl", projectImageUri != null ? projectImageUri.toString() : null);
        // ImageUrl, ProjectUrl, DemoUrl, VideoUrl sẽ được cập nhật sau nếu cần, hoặc từ links/media
        projectData.put("CreatorUserId", currentUser.getUid());
        projectData.put("CreatedAt", Timestamp.now()); // Sử dụng com.google.firebase.Timestamp
        projectData.put("UpdatedAt", null);
        projectData.put("IsApproved", false); // Mặc định chưa duyệt
        projectData.put("VoteCount", 0);
        // projectData.put("CourseId", null); // Nếu có

        // Tạo keywords cho tìm kiếm (đơn giản)
        List<String> keywords = new ArrayList<>();
        if (!projectName.isEmpty()) keywords.addAll(generateSubstrings(projectName.toLowerCase()));
        if (!categoryName.isEmpty()) keywords.add(categoryName.toLowerCase());
        if (!technologyInput.isEmpty()) {
            keywords.addAll(Arrays.asList(technologyInput.toLowerCase().split("\\s*,\\s*")));
        }
        projectData.put("SearchKeywords", new ArrayList<>(new HashSet<>(keywords))); // Loại bỏ trùng lặp


        db.collection("Projects").add(projectData)
                .addOnSuccessListener(documentReference -> {
                    String newProjectId = documentReference.getId();
                    Log.d(TAG, "Project document created with ID: " + newProjectId);

                    // 1. Lưu ProjectMembers
                    for (User user : selectedProjectUsers) {
                        if (user.getUserId() != null) {
                            Map<String, Object> memberData = new HashMap<>();
                            memberData.put("ProjectId", newProjectId);
                            memberData.put("UserId", user.getUserId());
                            memberData.put("RoleInProject", userRolesInProject.get(user.getUserId()));
                            // Tạo ID document cho ProjectMembers, ví dụ: ProjectID_UserID
                            db.collection("ProjectMembers").document(newProjectId + "_" + user.getUserId())
                                    .set(memberData)
                                    .addOnSuccessListener(aVoid -> Log.d(TAG, "ProjectMember link added for user: " + user.getUserId()))
                                    .addOnFailureListener(e -> Log.e(TAG, "Error adding ProjectMember for user: " + user.getUserId(), e));
                        }
                    }

                    // 2. Lưu ProjectCategories (Lấy CategoryId từ CategoryName)
                    if (!categoryName.isEmpty()) {
                        db.collection("Categories").whereEqualTo("Name", categoryName).limit(1).get()
                                .addOnSuccessListener(categorySnapshots -> {
                                    if (!categorySnapshots.isEmpty()) {
                                        String categoryId = categorySnapshots.getDocuments().get(0).getId();
                                        Map<String, Object> projectCategoryData = new HashMap<>();
                                        projectCategoryData.put("ProjectId", newProjectId);
                                        projectCategoryData.put("CategoryId", categoryId);
                                        db.collection("ProjectCategories").document(newProjectId + "_" + categoryId)
                                                .set(projectCategoryData)
                                                .addOnSuccessListener(aVoid1 -> Log.d(TAG, "ProjectCategory link added for category: " + categoryName))
                                                .addOnFailureListener(e -> Log.e(TAG, "Error adding ProjectCategory link", e));
                                    } else {
                                        Log.w(TAG, "Category not found: " + categoryName + ". Cannot link project to category.");
                                    }
                                }).addOnFailureListener(e -> Log.e(TAG, "Error fetching category ID for: " + categoryName, e));
                    }

                    // 3. Lưu ProjectTechnologies (Tạo mới nếu chưa có)
                    if (!technologyInput.isEmpty()) {
                        String[] techNames = technologyInput.split("\\s*,\\s*");
                        for (String techName : techNames) {
                            if (techName.trim().isEmpty()) continue;
                            final String currentTechName = techName.trim();
                            db.collection("Technologies").whereEqualTo("Name", currentTechName).limit(1).get()
                                    .addOnCompleteListener(techTask -> {
                                        if (techTask.isSuccessful() && techTask.getResult() != null) {
                                            if (!techTask.getResult().isEmpty()) { // Công nghệ đã tồn tại
                                                String techId = techTask.getResult().getDocuments().get(0).getId();
                                                addProjectTechnologyLink(newProjectId, techId, currentTechName);
                                            } else { // Tạo công nghệ mới
                                                Map<String, Object> newTechData = new HashMap<>();
                                                newTechData.put("Name", currentTechName);
                                                db.collection("Technologies").add(newTechData)
                                                        .addOnSuccessListener(techDocRef -> addProjectTechnologyLink(newProjectId, techDocRef.getId(), currentTechName))
                                                        .addOnFailureListener(e -> Log.e(TAG, "Error creating new technology: " + currentTechName, e));
                                            }
                                        } else {
                                            Log.e(TAG, "Error finding/creating technology: " + currentTechName, techTask.getException());
                                        }
                                    });
                        }
                    }

// 4. Cập nhật ProjectUrl, DemoUrl, VideoUrl vào Project document
                    Map<String, Object> projectLinkUpdates = new HashMap<>();
// SỬA Ở ĐÂY:
                    for (Map<String, String> linkMap : getValidProjectLinks()) { // Kiểu dữ liệu của linkMap là Map<String, String>
                        String platform = linkMap.get("platform"); // Lấy từ Map
                        String url = linkMap.get("url");          // Lấy từ Map

                        // Kiểm tra null cho platform trước khi toLowerCase
                        if (platform != null && url != null) { // Thêm kiểm tra null cho url nữa cho chắc
                            platform = platform.toLowerCase(); // Chuyển platform về chữ thường
                            switch (platform) {
                                case "github": case "gitlab": case "bitbucket":
                                    if (projectLinkUpdates.get("ProjectUrl") == null) projectLinkUpdates.put("ProjectUrl", url);
                                    break;
                                case "demo": case "website":
                                    if (projectLinkUpdates.get("DemoUrl") == null) projectLinkUpdates.put("DemoUrl", url);
                                    break;
                                case "youtube": case "vimeo":
                                    if (projectLinkUpdates.get("VideoUrl") == null) projectLinkUpdates.put("VideoUrl", url);
                                    break;
                            }
                        }
                    }
                    if (!projectLinkUpdates.isEmpty()) {
                        db.collection("Projects").document(newProjectId).update(projectLinkUpdates)
                                .addOnSuccessListener(aVoid -> Log.d(TAG, "Project main links updated successfully."))
                                .addOnFailureListener(e -> Log.e(TAG, "Error updating project main links.", e));
                    }


                    // TODO: 5. Xử lý upload media (selectedMediaUris) lên Firebase Storage
                    // và lưu URLs vào Project document (ví dụ: một array ImageUrls hoặc VideoUrls)
                    // hoặc collection ProjectMedia riêng.
                    // Sau khi upload, bạn sẽ có một List<String> chứa các download URLs.
                    // Ví dụ: projectData.put("MediaUrls", downloadUrlsList); (cần update document)

                    showSuccessAndClearForm();

                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Error creating project document", e);
                    showNotification("Tạo dự án thất bại. Lỗi: " + e.getMessage());
                });
    }

    private void addProjectTechnologyLink(String projectId, String technologyId, String techName) {
        Map<String, Object> projectTechData = new HashMap<>();
        projectTechData.put("ProjectId", projectId);
        projectTechData.put("TechnologyId", technologyId);
        // Tạo ID document cho ProjectTechnologies, ví dụ: ProjectID_TechnologyID
        db.collection("ProjectTechnologies").document(projectId + "_" + technologyId)
                .set(projectTechData)
                .addOnSuccessListener(aVoid -> Log.d(TAG, "ProjectTechnology link added for: " + techName))
                .addOnFailureListener(e -> Log.e(TAG, "Error adding ProjectTechnology link for: " + techName, e));
    }

    private void showSuccessAndClearForm() {
        if (!isAdded() || getContext() == null) return;
        // Bỏ comment dòng dưới nếu bạn đã có SuccessNotificationDialogFragment
        // SuccessNotificationDialogFragment dialog = SuccessNotificationDialogFragment.newInstance("Tạo dự án thành công!");
        // dialog.setOnDismissListener(this::clearFormAndNavigateHome);
        // dialog.show(getParentFragmentManager(), "success_dialog_create");
        Toast.makeText(getContext(), "Tạo dự án thành công!", Toast.LENGTH_LONG).show(); // Thay thế tạm thời
        clearFormAndNavigateHome();
    }

    private void clearFormAndNavigateHome() {
        clearForm();
        navigateToHome();
    }

    private void navigateToHome() {
        if (getActivity() != null) {
            ViewPager viewPager = getActivity().findViewById(R.id.view_pager);
            if (viewPager != null) {
                viewPager.setCurrentItem(0, true); // Chuyển về tab Home (index 0)
            } else {
                // Fallback nếu không có ViewPager, ví dụ pop back stack
                if (getParentFragmentManager().getBackStackEntryCount() > 0) {
                    getParentFragmentManager().popBackStack();
                }
            }
        }
    }

    private void clearForm() {
        if(etProjectName != null) etProjectName.setText("");
        if(etProjectDescription != null) etProjectDescription.setText("");
        if(actvCategory != null) actvCategory.setText("", false);
        if(etTechnology != null) etTechnology.setText("");
        if(actvStatus != null) actvStatus.setText("", false);

        projectImageUri = null;
        if(ivProjectImagePreview != null) ivProjectImagePreview.setVisibility(View.GONE);
        if(ivProjectImagePlaceholderIcon != null) ivProjectImagePlaceholderIcon.setVisibility(View.VISIBLE);
        if (flProjectImageContainer != null && getContext() != null) {
            try { // Thêm try-catch nếu getDrawable có thể null
                flProjectImageContainer.setBackground(ContextCompat.getDrawable(requireContext(), R.drawable.image_placeholder_square));
            } catch (Exception e) {
                Log.e(TAG, "Error setting placeholder background", e);
            }
        }

        selectedProjectUsers.clear();
        userRolesInProject.clear();
        addCurrentUserAsMember(); // Add lại người dùng hiện tại
        // updateSelectedMembersUI(); // addCurrentUserAsMember sẽ gọi

        projectLinks.clear();
        updateAddedLinksUI();

        selectedMediaUris.clear();
        updateMediaPreviewGallery();

        if(etProjectName != null) etProjectName.requestFocus(); // Focus lại trường tên
    }


    // --- Các hàm UI và Helper giữ nguyên hoặc đã được tích hợp ---
    @Override
    public void onUserSelected(User user) { // Callback từ AddMemberDialogFragment
        if (user == null || user.getUserId() == null) {
            if (getContext()!= null) Toast.makeText(getContext(), "Không thể thêm thành viên không hợp lệ.", Toast.LENGTH_SHORT).show();
            return;
        }
        if (!isUserAlreadyAdded(user.getUserId())) {
            userRolesInProject.put(user.getUserId(), "Thành viên"); // Mặc định vai trò
            selectedProjectUsers.add(user);
            updateSelectedMembersUI();
            if (getContext()!= null) Toast.makeText(getContext(), user.getFullName() + " đã được thêm.", Toast.LENGTH_SHORT).show();
        } else {
            if (getContext()!= null) Toast.makeText(getContext(), user.getFullName() + " đã có trong danh sách.", Toast.LENGTH_SHORT).show();
        }
    }

    private boolean isUserAlreadyAdded(String userId) {
        if (userId == null) return false;
        for (User u : selectedProjectUsers) {
            if (u.getUserId() != null && u.getUserId().equals(userId)) return true;
        }
        return false;
    }

    private void updateSelectedMembersUI() {
        if (getContext() == null || llSelectedMembersContainer == null) return;
        llSelectedMembersContainer.removeAllViews();
        if (selectedProjectUsers.isEmpty()) {
            llSelectedMembersContainer.setVisibility(View.GONE);
            return;
        }
        llSelectedMembersContainer.setVisibility(View.VISIBLE);
        LayoutInflater inflater = LayoutInflater.from(getContext());
        String[] roleItems = getResources().getStringArray(R.array.member_roles);

        for (int i = 0; i < selectedProjectUsers.size(); i++) {
            User user = selectedProjectUsers.get(i);
            if (user == null || user.getUserId() == null) continue;

            View memberView = inflater.inflate(R.layout.item_selected_member, llSelectedMembersContainer, false);
            ImageView ivAvatar = memberView.findViewById(R.id.iv_selected_member_avatar);
            TextView tvName = memberView.findViewById(R.id.tv_selected_member_name);
            TextView tvClass = memberView.findViewById(R.id.tv_selected_member_class);
            AutoCompleteTextView actvRole = memberView.findViewById(R.id.actv_member_role);
            ImageView ivRemove = memberView.findViewById(R.id.iv_remove_member);
            TextInputLayout tilRole = memberView.findViewById(R.id.til_member_role);


            tvName.setText(user.getFullName() != null ? user.getFullName() : "N/A");
            tvClass.setText(user.getUserClass() != null ? user.getUserClass() : "N/A");
            Glide.with(this).load(user.getAvatarUrl())
                    .placeholder(R.drawable.ic_default_avatar)
                    .error(R.drawable.ic_default_avatar)
                    .circleCrop().into(ivAvatar);

            ArrayAdapter<String> roleAdapter = new ArrayAdapter<>(getContext(), android.R.layout.simple_dropdown_item_1line, roleItems);
            actvRole.setAdapter(roleAdapter);

            String currentRole = userRolesInProject.get(user.getUserId());
            if (currentRole != null && !currentRole.isEmpty()) {
                actvRole.setText(currentRole, false);
            } else {
                // Logic đặt vai trò mặc định khi thêm, hoặc user hiện tại là trưởng nhóm
                String defaultRole = "Thành viên";
                FirebaseUser fbUser = mAuth.getCurrentUser();
                if (fbUser != null && user.getUserId().equals(fbUser.getUid()) && i == 0) { // Người đầu tiên được thêm (current user)
                    defaultRole = "Trưởng nhóm";
                }
                actvRole.setText(defaultRole, false);
                userRolesInProject.put(user.getUserId(), defaultRole);
            }

            final int userIndex = i;
            actvRole.setOnItemClickListener((parent, MView, position, id) -> {
                if (userIndex < selectedProjectUsers.size()) {
                    userRolesInProject.put(selectedProjectUsers.get(userIndex).getUserId(), parent.getItemAtPosition(position).toString());
                }
            });
            if (tilRole != null) { // Thêm toggle cho dropdown role
                tilRole.setEndIconOnClickListener(v_icon -> {
                    if (!actvRole.isPopupShowing()) actvRole.showDropDown();
                    else actvRole.dismissDropDown();
                });
            }


            FirebaseUser fbCurrentUser = mAuth.getCurrentUser();
            boolean isCurrentUserAndOnlyLeader = selectedProjectUsers.size() == 1 &&
                    fbCurrentUser != null &&
                    user.getUserId().equals(fbCurrentUser.getUid()) &&
                    "Trưởng nhóm".equals(userRolesInProject.get(user.getUserId()));

            if (isCurrentUserAndOnlyLeader) {
                ivRemove.setVisibility(View.GONE);
            } else {
                ivRemove.setVisibility(View.VISIBLE);
                ivRemove.setOnClickListener(v_remove -> {
                    if (userIndex < selectedProjectUsers.size()) {
                        User removedUser = selectedProjectUsers.remove(userIndex);
                        userRolesInProject.remove(removedUser.getUserId());
                        updateSelectedMembersUI();
                        if (getContext() != null) Toast.makeText(getContext(), "Đã xóa " + removedUser.getFullName(), Toast.LENGTH_SHORT).show();
                    }
                });
            }
            llSelectedMembersContainer.addView(memberView);
        }
    }

    private void updateAddedLinksUI() {
        if (getContext() == null || llAddedLinksContainer == null) return;
        llAddedLinksContainer.removeAllViews();
        if (projectLinks.isEmpty()) {
            llAddedLinksContainer.setVisibility(View.GONE);
            return;
        }
        llAddedLinksContainer.setVisibility(View.VISIBLE);
        LayoutInflater inflater = LayoutInflater.from(getContext());
        String[] platformItems = getResources().getStringArray(R.array.link_platforms);

        for (int i = 0; i < projectLinks.size(); i++) {
            final int linkIndex = i;
            LinkItem currentLinkItem = projectLinks.get(linkIndex);
            View linkView = inflater.inflate(R.layout.item_added_link, llAddedLinksContainer, false);
            TextInputEditText etLinkUrl = linkView.findViewById(R.id.et_added_link_url);
            AutoCompleteTextView actvPlatform = linkView.findViewById(R.id.actv_link_platform);
            ImageView ivRemoveLink = linkView.findViewById(R.id.iv_remove_link);
            TextInputLayout tilPlatform = linkView.findViewById(R.id.til_link_platform);


            etLinkUrl.setText(currentLinkItem.getUrl());
            ArrayAdapter<String> platformAdapter = new ArrayAdapter<>(requireContext(), android.R.layout.simple_dropdown_item_1line, platformItems);
            actvPlatform.setAdapter(platformAdapter);
            if (currentLinkItem.getPlatform() != null && !currentLinkItem.getPlatform().isEmpty()) {
                actvPlatform.setText(currentLinkItem.getPlatform(), false);
            } else if (platformItems.length > 0) {
                actvPlatform.setText(platformItems[0], false);
                if(linkIndex < projectLinks.size()) projectLinks.get(linkIndex).setPlatform(platformItems[0]);
            }

            actvPlatform.setOnItemClickListener((parent, MView, position, id) -> {
                if (linkIndex < projectLinks.size()) {
                    projectLinks.get(linkIndex).setPlatform(parent.getItemAtPosition(position).toString());
                }
            });
            if (tilPlatform != null) { // Thêm toggle cho dropdown platform
                tilPlatform.setEndIconOnClickListener(v_icon -> {
                    if(!actvPlatform.isPopupShowing()) actvPlatform.showDropDown();
                    else actvPlatform.dismissDropDown();
                });
            }


            etLinkUrl.addTextChangedListener(new TextWatcher() {
                @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
                @Override public void onTextChanged(CharSequence s, int start, int before, int count) {
                    if (linkIndex < projectLinks.size()) projectLinks.get(linkIndex).setUrl(s.toString());
                }
                @Override public void afterTextChanged(Editable s) {}
            });

            ivRemoveLink.setOnClickListener(v_remove -> {
                if (linkIndex < projectLinks.size()) {
                    projectLinks.remove(linkIndex);
                    updateAddedLinksUI();
                    if(getContext() != null) Toast.makeText(getContext(), "Đã xóa liên kết", Toast.LENGTH_SHORT).show();
                }
            });
            llAddedLinksContainer.addView(linkView);
        }
    }

    public List<Map<String, String>> getValidProjectLinks() {
        List<Map<String, String>> validLinksData = new ArrayList<>();
        for (LinkItem linkItem : projectLinks) {
            String url = linkItem.getUrl() != null ? linkItem.getUrl().trim() : "";
            String platform = linkItem.getPlatform() != null ? linkItem.getPlatform() : "Khác";
            if (!url.isEmpty() && android.util.Patterns.WEB_URL.matcher(url).matches()) {
                Map<String, String> linkMap = new HashMap<>();
                linkMap.put("url", url);
                linkMap.put("platform", platform); // Firestore key nên nhất quán
                validLinksData.add(linkMap);
            } else if (!url.isEmpty()){
                Log.w(TAG, "URL không hợp lệ đã được bỏ qua: " + url);
            }
        }
        return validLinksData;
    }


    private void fetchDataFromFirestore(String collectionName, String fieldName, List<String> dataList, ArrayAdapter<String> adapter, String logTagPrefix) {
        if (db == null || dataList == null || adapter == null) return; // Kiểm tra null
        dataList.clear();
        db.collection(collectionName).orderBy(fieldName).get().addOnCompleteListener(task -> {
            if (!isAdded() || getContext() == null) return; // Kiểm tra fragment state
            if (task.isSuccessful()) {
                QuerySnapshot querySnapshot = task.getResult();
                if (querySnapshot != null && !querySnapshot.isEmpty()) {
                    for (QueryDocumentSnapshot document : querySnapshot) {
                        String value = document.getString(fieldName);
                        if (value != null && !value.isEmpty()) dataList.add(value);
                    }
                    adapter.notifyDataSetChanged();
                } else {
                    Toast.makeText(getContext(), "Không tìm thấy " + logTagPrefix.toLowerCase() + ".", Toast.LENGTH_SHORT).show();
                }
            } else {
                Toast.makeText(getContext(), "Lỗi khi tải " + logTagPrefix.toLowerCase() + ".", Toast.LENGTH_SHORT).show();
                Log.w(TAG, "Lỗi khi lấy dữ liệu " + logTagPrefix + ": ", task.getException());
            }
        });
    }

    private void setupItemClickListener(AutoCompleteTextView autoCompleteTextView, String fieldName) {
        if (autoCompleteTextView == null) return;
        autoCompleteTextView.setOnItemClickListener((parent, MView, position, id) -> {
            // String selectedItem = (String) parent.getItemAtPosition(position);
            // Hiện tại không cần làm gì thêm ở đây, giá trị đã được set vào AutoCompleteTextView
        });
    }

    private void setupDropdownToggle(TextInputLayout textInputLayout, AutoCompleteTextView autoCompleteTextView) {
        if (textInputLayout == null || autoCompleteTextView == null) return;
        // Đảm bảo TextInputLayout có endIcon là dropdown (thường được set trong XML)
        // Nếu không, có thể set icon ở đây: textInputLayout.setEndIconDrawable(R.drawable.ic_arrow_drop_down);
        textInputLayout.setEndIconOnClickListener(v -> {
            if (!autoCompleteTextView.isPopupShowing()) {
                autoCompleteTextView.showDropDown();
            } else {
                autoCompleteTextView.dismissDropDown();
            }
        });
        // Hoặc cho phép click vào toàn bộ AutoCompleteTextView để mở dropdown
        autoCompleteTextView.setOnClickListener(v -> {
            if (!autoCompleteTextView.isPopupShowing()) {
                autoCompleteTextView.showDropDown();
            }
        });
    }


    private boolean checkAndRequestPermissions() {
        if (getContext() == null) return false;
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
        intent.setType("image/*"); // Chỉ chọn ảnh
        projectImagePickerLauncher.launch(intent);
    }

    private void launchMediaPicker() {
        Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
        intent.setType("*/*"); // Cho phép chọn nhiều loại file
        intent.putExtra(Intent.EXTRA_MIME_TYPES, new String[]{"image/*", "video/*"}); // Ưu tiên ảnh và video
        // intent.putExtra(Intent.EXTRA_ALLOW_MULTIPLE, true); // Nếu muốn cho phép chọn nhiều file cùng lúc
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
        int imageSizeInDp = 80; // Giảm kích thước để vừa nhiều hơn
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
            // Sử dụng drawable placeholder và error từ resource của bạn
            Glide.with(this).load(uri)
                    .placeholder(R.drawable.image_placeholder_square) // Đảm bảo bạn có drawable này
                    .error(R.drawable.error)           // Đảm bảo bạn có drawable này
                    .centerCrop()
                    .into(imageView);

            final int indexToRemove = i; // Cần final để dùng trong lambda
            imageView.setOnLongClickListener(v -> {
                if (indexToRemove < selectedMediaUris.size()) {
                    selectedMediaUris.remove(indexToRemove);
                    updateMediaPreviewGallery();
                    if(getContext() != null) Toast.makeText(getContext(), "Đã xóa media.", Toast.LENGTH_SHORT).show();
                }
                return true;
            });
            flexboxMediaPreviewContainer.addView(imageView);
        }
    }

    private List<String> generateSubstrings(String input) {
        List<String> substrings = new ArrayList<>();
        if (input == null || input.trim().isEmpty()) return substrings;
        String[] words = input.split("\\s+");
        for (String word : words) {
            if (word.length() < 2) {
                if(!word.isEmpty()) substrings.add(word); // Thêm từ đơn nếu nó không rỗng
                continue;
            }
            for (int i = 0; i < word.length(); i++) {
                for (int j = i + 1; j <= word.length(); j++) {
                    if (word.substring(i, j).length() >= 1) { // Cho phép substring độ dài 1
                        substrings.add(word.substring(i, j));
                    }
                }
            }
        }
        if (!input.trim().isEmpty() && !substrings.contains(input.trim())) {
            substrings.add(input.trim()); // Thêm cả cụm từ gốc
        }
        return substrings;
    }


    private void showNotification(String message) {
        if (getContext() != null && isAdded()) {
            // Tạm thời dùng Toast, bạn có thể thay thế bằng SuccessNotificationDialogFragment
            Toast.makeText(getContext(), message, Toast.LENGTH_LONG).show();
            // SuccessNotificationDialogFragment.newInstance(message).show(getParentFragmentManager(), "notification_dialog_create");
        } else {
            Log.w(TAG, "Cannot show notification, context or fragment not available: " + message);
        }
    }

    private void synchronizeButtonWidths() {
        if (llMemberSectionRoot == null || llLinkSectionRoot == null || llMediaSectionRoot == null) return;
        // Đảm bảo các view đã được đo lường
        if (llMemberSectionRoot.getWidth() == 0 && llMemberSectionRoot.getVisibility() == View.VISIBLE) {
            // Nếu view chưa được đo, post runnable để chạy sau khi layout pass hoàn tất
            llMemberSectionRoot.post(this::performWidthSync);
        } else {
            performWidthSync();
        }
    }

    private void performWidthSync() {
        if (llMemberSectionRoot == null || llLinkSectionRoot == null || llMediaSectionRoot == null) return;
        int memberSectionWidth = llMemberSectionRoot.getWidth();
        int linkSectionWidth = llLinkSectionRoot.getWidth();
        int mediaSectionWidth = llMediaSectionRoot.getWidth();

        int maxWidth = 0;
        if (llMemberSectionRoot.getVisibility() == View.VISIBLE && memberSectionWidth > maxWidth) maxWidth = memberSectionWidth;
        if (llLinkSectionRoot.getVisibility() == View.VISIBLE && linkSectionWidth > maxWidth) maxWidth = linkSectionWidth;
        if (llMediaSectionRoot.getVisibility() == View.VISIBLE && mediaSectionWidth > maxWidth) maxWidth = mediaSectionWidth;

        if (maxWidth > 0) {
            if (llMemberSectionRoot.getVisibility() == View.VISIBLE) setButtonWidth(llMemberSectionRoot, maxWidth);
            if (llLinkSectionRoot.getVisibility() == View.VISIBLE) setButtonWidth(llLinkSectionRoot, maxWidth);
            if (llMediaSectionRoot.getVisibility() == View.VISIBLE) setButtonWidth(llMediaSectionRoot, maxWidth);
        }
    }


    private void setButtonWidth(View view, int width) {
        if (view == null) return;
        ViewGroup.LayoutParams params = view.getLayoutParams();
        params.width = width;
        view.setLayoutParams(params);
    }
}