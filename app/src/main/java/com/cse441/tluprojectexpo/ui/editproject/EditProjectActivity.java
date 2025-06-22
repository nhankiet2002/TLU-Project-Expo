//package com.cse441.tluprojectexpo.Project;
//
//
//import android.app.Activity;
//import android.content.DialogInterface;
//import android.content.Intent;
//import android.net.Uri;
//import android.os.Bundle;
//import android.provider.MediaStore;
//import android.text.TextUtils;
//import android.util.Log;
//import android.view.MenuItem;
//import android.view.View;
//import android.widget.ArrayAdapter;
//import android.widget.Button;
//import android.widget.ImageView;
//import android.widget.Spinner;
//import android.widget.Toast;
//
//import androidx.activity.result.ActivityResultLauncher;
//import androidx.activity.result.contract.ActivityResultContracts;
//import androidx.annotation.NonNull;
//import androidx.appcompat.app.AlertDialog;
//import androidx.appcompat.app.AppCompatActivity;
//import androidx.appcompat.widget.Toolbar;
//import androidx.recyclerview.widget.LinearLayoutManager;
//import androidx.recyclerview.widget.RecyclerView;
//
//import com.bumptech.glide.Glide;
//import com.cse441.tluprojectexpo.R;
//import com.cse441.tluprojectexpo.model.Project;
//import com.cse441.tluprojectexpo.model.User; // Giả sử bạn có model User
//// Import các adapter nếu bạn tạo file riêng
//// import com.cse441.tluprojectexpo.adapter.EditableMemberListAdapter;
//// import com.cse441.tluprojectexpo.adapter.EditableMediaGalleryAdapter;
//
//import com.google.android.gms.tasks.Continuation;
//import com.google.android.gms.tasks.OnCompleteListener;
//import com.google.android.gms.tasks.Task;
//import com.google.android.gms.tasks.Tasks;
//import com.google.android.material.chip.Chip;
//import com.google.android.material.chip.ChipGroup;
//import com.google.android.material.progressindicator.LinearProgressIndicator; // Thêm nếu dùng
//import com.google.android.material.textfield.TextInputEditText;
//import com.google.firebase.Timestamp;
//import com.google.firebase.auth.FirebaseAuth;
//import com.google.firebase.auth.FirebaseUser;
//import com.google.firebase.firestore.CollectionReference;
//import com.google.firebase.firestore.DocumentReference;
//import com.google.firebase.firestore.DocumentSnapshot;
//import com.google.firebase.firestore.FieldValue;
//import com.google.firebase.firestore.FirebaseFirestore;
//import com.google.firebase.firestore.QueryDocumentSnapshot;
//import com.google.firebase.firestore.WriteBatch;
//
//import java.util.ArrayList;
//import java.util.Arrays;
//import java.util.HashMap;
//import java.util.List;
//import java.util.Map;
//import java.util.UUID;
//import java.util.stream.Collectors;
//
//public class EditProjectActivity extends AppCompatActivity {
//
//    private static final String TAG = "EditProjectActivity";
//    public static final String EXTRA_EDIT_PROJECT_ID = "EXTRA_EDIT_PROJECT_ID";
//
//    // UI Elements
//    private Toolbar toolbar;
//    private TextInputEditText editTextProjectTitleEdit, editTextProjectDescriptionEdit,
//            editTextProjectSourceUrlEdit, editTextProjectDemoUrlEdit;
//    private ImageView imageViewProjectThumbnailEdit;
//    private Button buttonChangeThumbnail, buttonSelectCategoriesEdit, buttonSelectTechnologiesEdit,
//            buttonAddMemberEdit, buttonAddMediaEdit, buttonSaveChanges;
//    private Spinner spinnerProjectStatusEdit, spinnerProjectCourseEdit;
//    private ChipGroup chipGroupCategoriesEdit, chipGroupTechnologiesEdit;
//    private RecyclerView recyclerViewMembersEdit, recyclerViewMediaGalleryEdit;
//    // private LinearProgressIndicator progressIndicator; // Optional
//
//    // Firebase
//    private FirebaseFirestore db;
//    private FirebaseAuth mAuth;
//    private FirebaseUser currentUser;
//
//
//    // Data
//    private String projectIdToEdit;
//    private Project currentEditingProject;
//    private Uri newThumbnailUri = null; // Uri của ảnh thumbnail mới được chọn
//    private List<Project.MediaItem> currentMediaItems = new ArrayList<>();
//    private List<Uri> newMediaUris = new ArrayList<>(); // Uri của media mới được chọn
//    private List<String> removedMediaUrls = new ArrayList<>(); // URL của media bị xóa
//    private List<Project.UserShortInfo> currentMembers = new ArrayList<>();
//    // TODO: Adapter cho members và media (cần có chức năng xóa)
//    // private EditableMemberListAdapter memberAdapterEdit;
//    // private EditableMediaGalleryAdapter mediaAdapterEdit;
//
//    // TODO: Lists for selected categories/technologies
//    private List<String> selectedCategoryIds = new ArrayList<>();
//    private List<String> selectedTechnologyIds = new ArrayList<>();
//    // TODO: Data for spinners
//    private List<String> statusOptions = Arrays.asList("Đang phát triển", "Hoàn thành", "Tạm dừng", "Đã hủy"); // Ví dụ
//    private Map<String, String> courseMap = new HashMap<>(); // <CourseName, CourseId>
//
//
//    // ActivityResultLaunchers
//    private ActivityResultLauncher<Intent> pickThumbnailLauncher;
//    private ActivityResultLauncher<Intent> pickMediaLauncher;
//
//
//    @Override
//    protected void onCreate(Bundle savedInstanceState) {
//        super.onCreate(savedInstanceState);
//        setContentView(R.layout.activity_edit_project);
//
//        db = FirebaseFirestore.getInstance();
//        mAuth = FirebaseAuth.getInstance();
//        currentUser = mAuth.getCurrentUser();
//        storage = FirebaseStorage.getInstance();
//
//        projectIdToEdit = getIntent().getStringExtra(EXTRA_EDIT_PROJECT_ID);
//
//        if (currentUser == null) {
//            Toast.makeText(this, "Bạn cần đăng nhập để sửa dự án.", Toast.LENGTH_LONG).show();
//            finish();
//            return;
//        }
//        if (projectIdToEdit == null || projectIdToEdit.isEmpty()) {
//            Toast.makeText(this, "Không có ID dự án để sửa.", Toast.LENGTH_LONG).show();
//            finish();
//            return;
//        }
//
//        initViews();
//        setupToolbar();
//        setupSpinners(); // Load data cho spinner trước
//        initResultLaunchers();
//        setupListeners();
//
//        loadProjectDataForEditing();
//    }
//
//    private void initViews() {
//        toolbar = findViewById(R.id.toolbarEditProject);
//        editTextProjectTitleEdit = findViewById(R.id.editTextProjectTitleEdit);
//        editTextProjectDescriptionEdit = findViewById(R.id.editTextProjectDescriptionEdit);
//        imageViewProjectThumbnailEdit = findViewById(R.id.imageViewProjectThumbnailEdit);
//        buttonChangeThumbnail = findViewById(R.id.buttonChangeThumbnail);
//        editTextProjectSourceUrlEdit = findViewById(R.id.editTextProjectSourceUrlEdit);
//        editTextProjectDemoUrlEdit = findViewById(R.id.editTextProjectDemoUrlEdit);
//        spinnerProjectStatusEdit = findViewById(R.id.spinnerProjectStatusEdit);
//        spinnerProjectCourseEdit = findViewById(R.id.spinnerProjectCourseEdit);
//        chipGroupCategoriesEdit = findViewById(R.id.chipGroupCategoriesEdit);
//        buttonSelectCategoriesEdit = findViewById(R.id.buttonSelectCategoriesEdit);
//        chipGroupTechnologiesEdit = findViewById(R.id.chipGroupTechnologiesEdit);
//        buttonSelectTechnologiesEdit = findViewById(R.id.buttonSelectTechnologiesEdit);
//        recyclerViewMembersEdit = findViewById(R.id.recyclerViewMembersEdit);
//        buttonAddMemberEdit = findViewById(R.id.buttonAddMemberEdit);
//        recyclerViewMediaGalleryEdit = findViewById(R.id.recyclerViewMediaGalleryEdit);
//        buttonAddMediaEdit = findViewById(R.id.buttonAddMediaEdit);
//        buttonSaveChanges = findViewById(R.id.buttonSaveChanges);
//        // progressIndicator = findViewById(R.id.progressIndicatorEdit); // Nếu bạn thêm
//
//        // TODO: Setup RecyclerViews với Editable Adapters
//        // recyclerViewMembersEdit.setLayoutManager(new LinearLayoutManager(this));
//        // memberAdapterEdit = new EditableMemberListAdapter(this, currentMembers, memberId -> removeMember(memberId));
//        // recyclerViewMembersEdit.setAdapter(memberAdapterEdit);
//
//        // recyclerViewMediaGalleryEdit.setLayoutManager(new LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false));
//        // mediaAdapterEdit = new EditableMediaGalleryAdapter(this, currentMediaItems, mediaItem -> removeMediaItem(mediaItem));
//        // recyclerViewMediaGalleryEdit.setAdapter(mediaAdapterEdit);
//    }
//
//    private void setupToolbar() {
//        setSupportActionBar(toolbar);
//        if (getSupportActionBar() != null) {
//            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
//            getSupportActionBar().setDisplayShowHomeEnabled(true);
//        }
//    }
//
//    @Override
//    public boolean onOptionsItemSelected(MenuItem item) {
//        if (item.getItemId() == android.R.id.home) {
//            finish(); // Hoặc hiển thị dialog xác nhận nếu có thay đổi chưa lưu
//            return true;
//        }
//        return super.onOptionsItemSelected(item);
//    }
//
//    private void initResultLaunchers() {
//        pickThumbnailLauncher = registerForActivityResult(
//                new ActivityResultContracts.StartActivityForResult(),
//                result -> {
//                    if (result.getResultCode() == Activity.RESULT_OK && result.getData() != null && result.getData().getData() != null) {
//                        newThumbnailUri = result.getData().getData();
//                        Glide.with(this).load(newThumbnailUri).centerCrop().into(imageViewProjectThumbnailEdit);
//                    }
//                });
//
//        pickMediaLauncher = registerForActivityResult(
//                new ActivityResultContracts.StartActivityForResult(),
//                result -> {
//                    if (result.getResultCode() == Activity.RESULT_OK && result.getData() != null) {
//                        if (result.getData().getClipData() != null) { // Chọn nhiều ảnh/video
//                            int count = result.getData().getClipData().getItemCount();
//                            for (int i = 0; i < count; i++) {
//                                Uri mediaUri = result.getData().getClipData().getItemAt(i).getUri();
//                                newMediaUris.add(mediaUri);
//                                // TODO: Hiển thị preview các media mới chọn hoặc cập nhật adapter ngay
//                            }
//                        } else if (result.getData().getData() != null) { // Chọn một ảnh/video
//                            Uri mediaUri = result.getData().getData();
//                            newMediaUris.add(mediaUri);
//                            // TODO: Hiển thị preview
//                        }
//                        // TODO: Cập nhật UI cho media gallery
//                        // mediaAdapterEdit.notifyDataSetChanged(); // Cần cập nhật list cho adapter trước
//                        Toast.makeText(this, "Đã thêm " + newMediaUris.size() + " media mới.", Toast.LENGTH_SHORT).show();
//                    }
//                });
//    }
//
//
//    private void setupSpinners() {
//        // Status Spinner
//        ArrayAdapter<String> statusAdapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, statusOptions);
//        statusAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
//        spinnerProjectStatusEdit.setAdapter(statusAdapter);
//
//        // Course Spinner (Load từ Firestore)
//        List<String> courseNames = new ArrayList<>();
//        courseNames.add("Không chọn môn học"); // Option mặc định
//        ArrayAdapter<String> courseAdapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, courseNames);
//        courseAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
//        spinnerProjectCourseEdit.setAdapter(courseAdapter);
//
//        db.collection("Courses").get().addOnSuccessListener(queryDocumentSnapshots -> {
//            courseMap.clear();
//            for (QueryDocumentSnapshot doc : queryDocumentSnapshots) {
//                String courseId = doc.getId();
//                String courseName = doc.getString("CourseName"); // Giả sử trường tên là "CourseName"
//                if (courseName != null) {
//                    courseMap.put(courseName, courseId);
//                    courseNames.add(courseName);
//                }
//            }
//            courseAdapter.notifyDataSetChanged();
//            // Sau khi load xong course, nếu currentEditingProject đã có, thì chọn course đó
//            if (currentEditingProject != null && currentEditingProject.getCourseId() != null) {
//                selectSpinnerItemByCourseId(currentEditingProject.getCourseId());
//            }
//        }).addOnFailureListener(e -> Log.e(TAG, "Error loading courses for spinner", e));
//    }
//
//
//    private void setupListeners() {
//        buttonChangeThumbnail.setOnClickListener(v -> openImagePickerForThumbnail());
//        buttonAddMediaEdit.setOnClickListener(v -> openMediaPicker());
//        buttonSaveChanges.setOnClickListener(v -> confirmSaveChanges());
//
//        // TODO: Listeners for selecting categories, technologies, adding members
//        // buttonSelectCategoriesEdit.setOnClickListener(v -> showCategorySelectionDialog());
//        // buttonSelectTechnologiesEdit.setOnClickListener(v -> showTechnologySelectionDialog());
//        // buttonAddMemberEdit.setOnClickListener(v -> showMemberSearchDialog());
//    }
//
//    private void openImagePickerForThumbnail() {
//        Intent intent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
//        pickThumbnailLauncher.launch(intent);
//    }
//    private void openMediaPicker() {
//        Intent intent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
//        intent.putExtra(Intent.EXTRA_ALLOW_MULTIPLE, true);
//        intent.setType("image/* video/*"); // Cho phép chọn cả ảnh và video
//        // Hoặc chỉ ảnh: intent.setType("image/*");
//        pickMediaLauncher.launch(intent);
//    }
//
//
//    private void loadProjectDataForEditing() {
//        // showLoading(true); // Nếu có progress indicator
//        DocumentReference projectRef = db.collection("Projects").document(projectIdToEdit);
//        projectRef.get().addOnSuccessListener(documentSnapshot -> {
//            // showLoading(false);
//            if (documentSnapshot.exists()) {
//                currentEditingProject = documentSnapshot.toObject(Project.class);
//                if (currentEditingProject != null) {
//                    // Kiểm tra quyền sở hữu (creatorId phải là user hiện tại)
//                    if (!currentUser.getUid().equals(currentEditingProject.getCreatorUserId())) {
//                        Toast.makeText(this, "Bạn không có quyền sửa dự án này.", Toast.LENGTH_LONG).show();
//                        finish();
//                        return;
//                    }
//                    currentEditingProject.setProjectId(documentSnapshot.getId());
//                    populateFieldsWithProjectData();
//                    // Load thêm thông tin liên quan (categories, technologies, members)
//                    loadRelatedDataForEditing();
//                } else {
//                    Toast.makeText(this, "Lỗi khi đọc dữ liệu dự án.", Toast.LENGTH_SHORT).show();
//                    finish();
//                }
//            } else {
//                Toast.makeText(this, "Dự án không tồn tại.", Toast.LENGTH_SHORT).show();
//                finish();
//            }
//        }).addOnFailureListener(e -> {
//            // showLoading(false);
//            Log.e(TAG, "Error loading project for editing", e);
//            Toast.makeText(this, "Lỗi tải dự án: " + e.getMessage(), Toast.LENGTH_SHORT).show();
//            finish();
//        });
//    }
//
//    private void populateFieldsWithProjectData() {
//        if (currentEditingProject == null) return;
//
//        editTextProjectTitleEdit.setText(currentEditingProject.getTitle());
//        editTextProjectDescriptionEdit.setText(currentEditingProject.getDescription());
//        editTextProjectSourceUrlEdit.setText(currentEditingProject.getProjectUrl());
//        editTextProjectDemoUrlEdit.setText(currentEditingProject.getDemoUrl());
//
//        if (currentEditingProject.getThumbnailUrl() != null && !currentEditingProject.getThumbnailUrl().isEmpty()) {
//            Glide.with(this)
//                    .load(currentEditingProject.getThumbnailUrl())
//                    .placeholder(R.drawable.ic_placeholder_image)
//                    .error(R.drawable.ic_image_error)
//                    .centerCrop()
//                    .into(imageViewProjectThumbnailEdit);
//        }
//
//        // Set status spinner
//        if (currentEditingProject.getStatus() != null) {
//            int statusPosition = ((ArrayAdapter<String>) spinnerProjectStatusEdit.getAdapter()).getPosition(currentEditingProject.getStatus());
//            if (statusPosition >= 0) {
//                spinnerProjectStatusEdit.setSelection(statusPosition);
//            }
//        }
//
//        // Set course spinner (sẽ được chọn lại sau khi courseMap load xong nếu cần)
//        if (currentEditingProject.getCourseId() != null && !courseMap.isEmpty()) {
//            selectSpinnerItemByCourseId(currentEditingProject.getCourseId());
//        }
//
//        // Populate media gallery
//        if(currentEditingProject.getMediaGalleryUrls() != null){
//            currentMediaItems.clear();
//            currentMediaItems.addAll(currentEditingProject.getMediaGalleryUrls());
//            // TODO: mediaAdapterEdit.notifyDataSetChanged();
//        }
//    }
//
//    private void selectSpinnerItemByCourseId(String courseIdToSelect) {
//        if (courseIdToSelect == null || courseMap.isEmpty()) return;
//        for (Map.Entry<String, String> entry : courseMap.entrySet()) {
//            if (courseIdToSelect.equals(entry.getValue())) {
//                String courseNameToSelect = entry.getKey();
//                int coursePosition = ((ArrayAdapter<String>) spinnerProjectCourseEdit.getAdapter()).getPosition(courseNameToSelect);
//                if (coursePosition >= 0) {
//                    spinnerProjectCourseEdit.setSelection(coursePosition);
//                }
//                break;
//            }
//        }
//    }
//
//
//    private void loadRelatedDataForEditing() {
//        // TODO: Load Categories, Technologies, Members và điền vào ChipGroups/RecyclerViews
//        // Ví dụ load Categories:
//        // db.collection("ProjectCategories").whereEqualTo("projectId", projectIdToEdit).get()...
//        // rồi lấy categoryId, query "Categories" để lấy tên, add vào selectedCategoryIds và updateChipGroupCategoriesEdit()
//
//        // Ví dụ load Members:
//        // db.collection("ProjectMembers").whereEqualTo("projectId", projectIdToEdit).get()...
//        // rồi lấy userId, role, query "Users" để lấy thông tin, add vào currentMembers và memberAdapterEdit.notifyDataSetChanged()
//    }
//
//
//    private void confirmSaveChanges() {
//        new AlertDialog.Builder(this)
//                .setTitle("Xác nhận Lưu")
//                .setMessage("Bạn có chắc chắn muốn lưu các thay đổi này không?")
//                .setPositiveButton("Lưu", (dialog, which) -> saveProjectChanges())
//                .setNegativeButton("Hủy", null)
//                .show();
//    }
//
//    private void saveProjectChanges() {
//        String title = editTextProjectTitleEdit.getText().toString().trim();
//        String description = editTextProjectDescriptionEdit.getText().toString().trim();
//        // ... lấy các giá trị khác
//
//        if (TextUtils.isEmpty(title) || TextUtils.isEmpty(description)) {
//            Toast.makeText(this, "Tên dự án và mô tả không được để trống.", Toast.LENGTH_SHORT).show();
//            return;
//        }
//
//        // showLoading(true);
//        buttonSaveChanges.setEnabled(false);
//
//        DocumentReference projectRef = db.collection("Projects").document(projectIdToEdit);
//        Map<String, Object> projectUpdates = new HashMap<>();
//        projectUpdates.put("Title", title);
//        projectUpdates.put("Description", description);
//        projectUpdates.put("ProjectUrl", editTextProjectSourceUrlEdit.getText().toString().trim());
//        projectUpdates.put("DemoUrl", editTextProjectDemoUrlEdit.getText().toString().trim());
//        projectUpdates.put("Status", spinnerProjectStatusEdit.getSelectedItem().toString());
//        projectUpdates.put("UpdatedAt", FieldValue.serverTimestamp());
//
//        String selectedCourseName = spinnerProjectCourseEdit.getSelectedItem().toString();
//        if (!"Không chọn môn học".equals(selectedCourseName) && courseMap.containsKey(selectedCourseName)) {
//            projectUpdates.put("CourseId", courseMap.get(selectedCourseName));
//        } else {
//            projectUpdates.put("CourseId", null); // Hoặc FieldValue.delete() nếu muốn xóa trường
//        }
//
//
//        // Xử lý upload ảnh (thumbnail, media gallery) và cập nhật URL
//        // Đây là phần phức tạp, cần xử lý tuần tự hoặc song song có kiểm soát
//
//        Task<Void> mainUpdateTask = Task.forResult(null); // Task khởi đầu
//
//        // 1. Upload thumbnail mới nếu có
//        if (newThumbnailUri != null) {
//            mainUpdateTask = mainUpdateTask.continueWithTask(task ->
//                    uploadImageToStorage(newThumbnailUri, "project_thumbnails/" + projectIdToEdit + "_thumbnail_" + System.currentTimeMillis())
//                            .onSuccessTask(downloadUrl -> {
//                                projectUpdates.put("ThumbnailUrl", downloadUrl.toString());
//                                return Tasks.forResult(null);
//                            })
//            );
//        }
//
//        // 2. Xử lý Media Gallery: Xóa cũ, upload mới
//        mainUpdateTask = mainUpdateTask.continueWithTask(task -> {
//            // Lấy danh sách URL media hiện tại (không bao gồm những cái đã bị removedMediaUrls)
//            List<Project.MediaItem> finalMediaItems = new ArrayList<>(currentEditingProject.getMediaGalleryUrls()); // Bắt đầu với list gốc
//            finalMediaItems.removeIf(item -> removedMediaUrls.contains(item.getUrl())); // Xóa những cái đã đánh dấu xóa
//
//
//            // Upload media mới
//            List<Task<Project.MediaItem>> uploadMediaTasks = new ArrayList<>();
//            for (Uri mediaUri : newMediaUris) {
//                // TODO: Xác định type (image/video) từ Uri nếu cần
//                String mediaType = "image"; // Hoặc "video"
//                String fileName = "project_media/" + projectIdToEdit + "/" + UUID.randomUUID().toString();
//                uploadMediaTasks.add(
//                        uploadImageToStorage(mediaUri, fileName)
//                                .onSuccessTask(downloadUrl -> Tasks.forResult(new Project.MediaItem(downloadUrl.toString(), mediaType)))
//                );
//            }
//
//            return Tasks.whenAllSuccess(uploadMediaTasks).onSuccessTask(newUploadedItems -> {
//                finalMediaItems.addAll((List<Project.MediaItem>)newUploadedItems); // Ép kiểu cẩn thận
//                projectUpdates.put("MediaGalleryUrls", finalMediaItems.stream()
//                        .map(item -> { // Chuyển thành Map để Firestore lưu đúng
//                            Map<String, Object> map = new HashMap<>();
//                            map.put("url", item.getUrl());
//                            map.put("type", item.getType());
//                            return map;
//                        }).collect(Collectors.toList()));
//                // TODO: Xóa các file media cũ trên Storage (removedMediaUrls)
//                deleteOldMediaFromStorage(removedMediaUrls);
//                return Tasks.forResult(null);
//            });
//        });
//
//
//        // 3. Thực hiện cập nhật document Project
//        mainUpdateTask = mainUpdateTask.continueWithTask(task -> projectRef.update(projectUpdates));
//
//        // 4. Cập nhật các collection liên quan (ProjectCategories, ProjectTechnologies, ProjectMembers)
//        // Dùng WriteBatch cho hiệu quả
//        mainUpdateTask = mainUpdateTask.continueWithTask(task -> {
//            WriteBatch batch = db.batch();
//            // TODO: Xóa các liên kết cũ và thêm các liên kết mới cho Categories, Technologies, Members
//            // Ví dụ cho Categories:
//            // - Query ProjectCategories theo projectIdToEdit để xóa
//            // - Thêm document mới vào ProjectCategories cho mỗi selectedCategoryId
//            return batch.commit();
//        });
//
//
//        mainUpdateTask.addOnSuccessListener(aVoid -> {
//            // showLoading(false);
//            buttonSaveChanges.setEnabled(true);
//            Toast.makeText(EditProjectActivity.this, "Dự án đã được cập nhật!", Toast.LENGTH_SHORT).show();
//            setResult(Activity.RESULT_OK); // Để activity trước có thể refresh nếu cần
//            finish();
//        }).addOnFailureListener(e -> {
//            // showLoading(false);
//            buttonSaveChanges.setEnabled(true);
//            Log.e(TAG, "Error updating project", e);
//            Toast.makeText(EditProjectActivity.this, "Lỗi cập nhật dự án: " + e.getMessage(), Toast.LENGTH_LONG).show();
//        });
//    }
//
//
//    private Task<Uri> uploadImageToStorage(Uri imageUri, String storagePath) {
//        StorageReference fileRef = storage.getReference().child(storagePath);
//        UploadTask uploadTask = fileRef.putFile(imageUri);
//
//        return uploadTask.continueWithTask((Task<UploadTask.TaskSnapshot> task) -> {
//            if (!task.isSuccessful()) {
//                throw task.getException();
//            }
//            return fileRef.getDownloadUrl();
//        });
//    }
//
//    private void deleteOldMediaFromStorage(List<String> urlsToDelete) {
//        for (String url : urlsToDelete) {
//            if (url == null || url.isEmpty()) continue;
//            try {
//                StorageReference photoRef = storage.getReferenceFromUrl(url);
//                photoRef.delete().addOnSuccessListener(aVoid ->
//                        Log.d(TAG, "Successfully deleted old media: " + url)
//                ).addOnFailureListener(exception ->
//                        Log.e(TAG, "Failed to delete old media: " + url, exception)
//                );
//            } catch (IllegalArgumentException e) {
//                Log.e(TAG, "Invalid URL for deletion: " + url, e);
//            }
//        }
//    }
//
//
//    // TODO: Các hàm để hiển thị dialog chọn Categories/Technologies/Members
//    // Ví dụ: private void showCategorySelectionDialog() { ... }
//
//    // TODO: Các hàm để xử lý việc xóa item khỏi RecyclerViews và cập nhật các list tương ứng
//    // private void removeMember(String memberIdToRemove) { ... }
//    // private void removeMediaItem(Project.MediaItem mediaItemToRemove) { ... currentMediaItems.remove(...); removedMediaUrls.add(...); }
//
//
//    // private void showLoading(boolean isLoading) {
//    //     if (progressIndicator != null) {
//    //         progressIndicator.setVisibility(isLoading ? View.VISIBLE : View.GONE);
//    //     }
//    //     buttonSaveChanges.setEnabled(!isLoading);
//    //     // Vô hiệu hóa các trường khác nếu cần
//    // }
//}
