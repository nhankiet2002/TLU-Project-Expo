package com.cse441.tluprojectexpo.ui.detailproject;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.cse441.tluprojectexpo.R;
// Sử dụng model Comment, Project, User của bạn
import com.cse441.tluprojectexpo.model.Comment;
import com.cse441.tluprojectexpo.model.Project;
import com.cse441.tluprojectexpo.model.User;
// Các service và util đã tạo/chỉnh sửa
import com.cse441.tluprojectexpo.service.FirestoreService;
import com.cse441.tluprojectexpo.service.ProjectInteractionHandler;
import com.cse441.tluprojectexpo.util.Constants;
import com.cse441.tluprojectexpo.util.UiHelper;
// Import các Adapter đã tách file
import com.cse441.tluprojectexpo.ui.detailproject.adapter.CommentAdapter;
import com.cse441.tluprojectexpo.ui.detailproject.adapter.MediaGalleryAdapter;
import com.cse441.tluprojectexpo.ui.detailproject.adapter.ProjectMemberAdapter;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.chip.Chip;
import com.google.android.material.chip.ChipGroup;
import com.google.firebase.Timestamp;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore; // Chỉ cần cho khởi tạo dbInstance

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class ProjectDetailActivity extends AppCompatActivity {

    public static final String EXTRA_PROJECT_ID = "EXTRA_PROJECT_ID";
    private static final String TAG = "ProjectDetailActivity";

    // UI Elements
    private ImageView ivBackArrow;
    private TextView tvToolbarTitle;
    private ImageView imageViewProjectThumbnail;
    private TextView textViewProjectTitle;
    private TextView textViewProjectCreator;
    private TextView textViewProjectStatus;
    private TextView textViewProjectDescription;
    private TextView textViewProjectLinkSourceCode;
    private TextView textViewProjectLinkDemo;
    private LinearLayout layoutProjectCourse;
    private TextView textViewProjectCourse;
    private ChipGroup chipGroupCategories;
    private ChipGroup chipGroupTechnologies;
    private RecyclerView recyclerViewMembers;
    private TextView textViewMediaGalleryTitle;
    private RecyclerView recyclerViewMediaGallery;
    private MaterialButton buttonUpvote;
    private TextView textViewVoteCount;
    private EditText editTextNewComment;
    private MaterialButton buttonPostComment;
    private RecyclerView recyclerViewComments;

    // Services and Handlers
    private FirebaseFirestore dbInstance;
    private FirestoreService firestoreService;
    private ProjectInteractionHandler interactionHandler;

    // Firebase Auth
    private FirebaseAuth mAuth;
    private FirebaseUser currentUser;

    // Data
    private String projectId;
    private Project currentProject; // Sử dụng model Project của bạn
    private ProjectMemberAdapter memberAdapter;
    private MediaGalleryAdapter mediaGalleryAdapter;
    private CommentAdapter commentAdapter;
    private List<Project.UserShortInfo> memberList = new ArrayList<>(); // UserShortInfo là inner class của Project
    private List<Project.MediaItem> mediaList = new ArrayList<>();   // MediaItem là inner class của Project
    private List<Comment> commentList = new ArrayList<>();             // Sử dụng model Comment của bạn

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_project_detail);

        dbInstance = FirebaseFirestore.getInstance();
        mAuth = FirebaseAuth.getInstance();
        currentUser = mAuth.getCurrentUser();

        firestoreService = new FirestoreService(); // Khởi tạo service
        interactionHandler = new ProjectInteractionHandler(dbInstance); // Khởi tạo handler

        initViews();
        setupRecyclerViews(); // Tách riêng setup RecyclerViews
        setupListeners();

        projectId = getIntent().getStringExtra(EXTRA_PROJECT_ID);
        if (projectId == null || projectId.isEmpty()) {
            UiHelper.showToast(this, "Không có ID dự án.", Toast.LENGTH_LONG);
            finish();
            return;
        }
        loadAllProjectData();
    }

    private void initViews() {
        ivBackArrow = findViewById(R.id.iv_back_arrow);
        tvToolbarTitle = findViewById(R.id.tv_title);
        imageViewProjectThumbnail = findViewById(R.id.imageViewProjectThumbnail);
        textViewProjectTitle = findViewById(R.id.textViewProjectTitle);
        textViewProjectCreator = findViewById(R.id.textViewProjectCreator);
        textViewProjectStatus = findViewById(R.id.textViewProjectStatus);
        textViewProjectDescription = findViewById(R.id.textViewProjectDescription);
        textViewProjectLinkSourceCode = findViewById(R.id.textViewProjectLinkSourceCode);
        textViewProjectLinkDemo = findViewById(R.id.textViewProjectLinkDemo);
        layoutProjectCourse = findViewById(R.id.layoutProjectCourse);
        textViewProjectCourse = findViewById(R.id.textViewProjectCourse);
        chipGroupCategories = findViewById(R.id.chipGroupCategories);
        chipGroupTechnologies = findViewById(R.id.chipGroupTechnologies);
        recyclerViewMembers = findViewById(R.id.recyclerViewMembers);
        textViewMediaGalleryTitle = findViewById(R.id.textViewMediaGalleryTitle);
        recyclerViewMediaGallery = findViewById(R.id.recyclerViewMediaGallery);
        buttonUpvote = findViewById(R.id.buttonUpvote);
        textViewVoteCount = findViewById(R.id.textViewVoteCount);
        editTextNewComment = findViewById(R.id.editTextNewComment);
        buttonPostComment = findViewById(R.id.buttonPostComment);
        recyclerViewComments = findViewById(R.id.recyclerViewComments);
    }

    private void setupRecyclerViews() {
        // Member Adapter
        recyclerViewMembers.setLayoutManager(new LinearLayoutManager(this));
        memberAdapter = new ProjectMemberAdapter(this, memberList);
        recyclerViewMembers.setAdapter(memberAdapter);
        recyclerViewMembers.setNestedScrollingEnabled(false);

        // Media Gallery Adapter
        recyclerViewMediaGallery.setLayoutManager(new LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false));
        mediaGalleryAdapter = new MediaGalleryAdapter(this, mediaList);
        recyclerViewMediaGallery.setAdapter(mediaGalleryAdapter);

        // Comment Adapter
        recyclerViewComments.setLayoutManager(new LinearLayoutManager(this));
        commentAdapter = new CommentAdapter(this, commentList); // Sử dụng Comment model của bạn
        recyclerViewComments.setAdapter(commentAdapter);
        recyclerViewComments.setNestedScrollingEnabled(false);
    }


    private void setupListeners() {
        ivBackArrow.setOnClickListener(v -> finish());
        buttonUpvote.setOnClickListener(v -> triggerUpvoteAction());
        buttonPostComment.setOnClickListener(v -> triggerPostCommentAction());

        textViewProjectLinkSourceCode.setOnClickListener(v -> {
            if (currentProject != null && currentProject.getProjectUrl() != null && !currentProject.getProjectUrl().isEmpty()) {
                openUrl(currentProject.getProjectUrl());
            }
        });
        textViewProjectLinkDemo.setOnClickListener(v -> {
            if (currentProject != null && currentProject.getDemoUrl() != null && !currentProject.getDemoUrl().isEmpty()) {
                openUrl(currentProject.getDemoUrl());
            }
        });
    }

    private void openUrl(String url) {
        if (url == null || url.trim().isEmpty()) {
            UiHelper.showToast(this, "Liên kết không hợp lệ.", Toast.LENGTH_SHORT);
            return;
        }
        String properUrl = url.trim();
        if (!properUrl.startsWith("http://") && !properUrl.startsWith("https://")) {
            properUrl = "https://" + properUrl;
        }
        Intent browserIntent = new Intent(Intent.ACTION_VIEW, Uri.parse(properUrl));
        try {
            startActivity(browserIntent);
        } catch (Exception e) {
            UiHelper.showToast(this, "Không thể mở liên kết.", Toast.LENGTH_SHORT);
            Log.e(TAG, "Could not open URL: " + properUrl, e);
        }
    }

    private void loadAllProjectData() {
        firestoreService.fetchProjectDetails(projectId, new FirestoreService.ProjectDetailsFetchListener() {
            @Override
            public void onProjectFetched(Project project) {
                currentProject = project;
                populateBaseUI();
                loadAdditionalProjectData();
                checkInitialUpvoteStatus();
            }
            @Override
            public void onProjectNotFound() { showErrorAndFinish("Dự án không tồn tại."); }
            @Override
            public void onError(String errorMessage) { showErrorAndFinish("Lỗi tải dự án: " + errorMessage); }
        });
    }

    private void populateBaseUI() {
        if (currentProject == null) return;
        tvToolbarTitle.setText(currentProject.getTitle() != null ? currentProject.getTitle() : "Chi tiết dự án");
        textViewProjectTitle.setText(currentProject.getTitle());
        textViewProjectDescription.setText(currentProject.getDescription());

        if (currentProject.getThumbnailUrl() != null && !currentProject.getThumbnailUrl().isEmpty()) {
            Glide.with(this).load(currentProject.getThumbnailUrl())
                    .placeholder(R.drawable.ic_placeholder_image).error(R.drawable.ic_image_error)
                    .centerCrop().into(imageViewProjectThumbnail);
        } else {
            imageViewProjectThumbnail.setImageResource(R.drawable.ic_placeholder_image);
        }
        setStatusUI(currentProject.getStatus());

        updateLinkUI(textViewProjectLinkSourceCode, "Mã nguồn: ", currentProject.getProjectUrl());
        updateLinkUI(textViewProjectLinkDemo, "Demo: ", currentProject.getDemoUrl());

        textViewVoteCount.setText(String.format(Locale.getDefault(), "%d lượt bình chọn", currentProject.getVoteCount()));

        if (currentProject.getMediaGalleryUrls() != null && !currentProject.getMediaGalleryUrls().isEmpty()) {
            textViewMediaGalleryTitle.setVisibility(View.VISIBLE);
            recyclerViewMediaGallery.setVisibility(View.VISIBLE);
            mediaList.clear();
            mediaList.addAll(currentProject.getMediaGalleryUrls());
            mediaGalleryAdapter.notifyDataSetChanged();
        } else {
            textViewMediaGalleryTitle.setVisibility(View.GONE);
            recyclerViewMediaGallery.setVisibility(View.GONE);
        }
    }

    private void updateLinkUI(TextView textView, String prefix, String url) {
        if (url != null && !url.isEmpty()) {
            textView.setText(prefix + url);
            textView.setVisibility(View.VISIBLE);
        } else {
            textView.setVisibility(View.GONE);
        }
    }

    private void setStatusUI(String status) {
        if (status == null) status = "Không rõ";
        textViewProjectStatus.setText(status);
        switch (status.toLowerCase().trim()) {
            case "hoàn thành": case "completed":
                textViewProjectStatus.setBackgroundResource(R.drawable.status_background_completed);
                break;
            case "đang phát triển": case "in progress": case "đang thực hiện":
                textViewProjectStatus.setBackgroundResource(R.drawable.status_background_inprogress);
                break;
            case "tạm dừng": case "paused":
                textViewProjectStatus.setBackgroundResource(R.drawable.status_background_paused);
                break;
            default:
                textViewProjectStatus.setBackgroundResource(R.drawable.status_background_paused);
                break;
        }
    }

    private void loadAdditionalProjectData() {
        if (currentProject == null || projectId == null) return;

        // Load Creator Info
        if (currentProject.getCreatorUserId() != null && !currentProject.getCreatorUserId().isEmpty()) {
            // SỬA Ở ĐÂY: Đổi fetchCreatorDetails thành fetchUserDetails
            firestoreService.fetchUserDetails(currentProject.getCreatorUserId(), new FirestoreService.UserDetailsFetchListener() {
                @Override
                public void onUserDetailsFetched(User user) {
                    // Giả sử Project model có setCreatorFullName và User model có getFullName
                    if (currentProject != null && user != null) { // Thêm kiểm tra null
                        currentProject.setCreatorFullName(user.getFullName());
                        updateCreatorInfoUI(user.getFullName());
                    } else if (user == null) {
                        Log.e(TAG, "User object is null after fetching details for creator.");
                        updateCreatorInfoUI("Lỗi dữ liệu người tạo");
                    }
                }
                @Override public void onUserNotFound() { updateCreatorInfoUI("Người tạo không tồn tại"); }
                @Override public void onError(String errorMessage) {
                    Log.e(TAG, "Error loading creator: " + errorMessage);
                    updateCreatorInfoUI("Lỗi tải người tạo");
                }
            });
        } else {
            updateCreatorInfoUI("N/A");
        }

        // Load Course Info
        if (currentProject.getCourseId() != null && !currentProject.getCourseId().isEmpty()) {
            firestoreService.fetchCourseDetails(currentProject.getCourseId(), new FirestoreService.CourseDetailsListener() {
                @Override
                public void onCourseFetched(String courseName) {
                    textViewProjectCourse.setText(courseName);
                    layoutProjectCourse.setVisibility(View.VISIBLE);
                }
                @Override public void onCourseNotFound() { layoutProjectCourse.setVisibility(View.GONE); }
                @Override public void onError(String errorMessage) {
                    Log.e(TAG, "Error loading course: " + errorMessage);
                    layoutProjectCourse.setVisibility(View.GONE);
                }
            });
        } else {
            layoutProjectCourse.setVisibility(View.GONE);
        }

        // Load Categories
        firestoreService.fetchCategoriesForProject(projectId, new FirestoreService.ProjectRelatedListListener<String>() {
            @Override public void onListFetched(List<String> items) {
                currentProject.setCategoryNames(items); updateChipGroupUI(chipGroupCategories, items);
            }
            @Override public void onListEmpty() { updateChipGroupUI(chipGroupCategories, new ArrayList<>()); }
            @Override public void onError(String errorMessage) {
                Log.e(TAG, "Error loading categories: " + errorMessage);
                updateChipGroupUI(chipGroupCategories, new ArrayList<>());
            }
        });

        // Load Technologies
        firestoreService.fetchTechnologiesForProject(projectId, new FirestoreService.ProjectRelatedListListener<String>() {
            @Override public void onListFetched(List<String> items) {
                currentProject.setTechnologyNames(items); updateChipGroupUI(chipGroupTechnologies, items);
            }
            @Override public void onListEmpty() { updateChipGroupUI(chipGroupTechnologies, new ArrayList<>()); }
            @Override public void onError(String errorMessage) {
                Log.e(TAG, "Error loading technologies: " + errorMessage);
                updateChipGroupUI(chipGroupTechnologies, new ArrayList<>());
            }
        });

        // Load Members
        firestoreService.fetchProjectMembers(projectId, new FirestoreService.ProjectRelatedListListener<Project.UserShortInfo>() {
            @Override public void onListFetched(List<Project.UserShortInfo> items) {
                memberList.clear(); memberList.addAll(items);
                currentProject.setProjectMembersInfo(items); memberAdapter.notifyDataSetChanged();
            }
            @Override public void onListEmpty() {
                memberList.clear(); currentProject.setProjectMembersInfo(new ArrayList<>()); memberAdapter.notifyDataSetChanged();
            }
            @Override public void onError(String errorMessage) {
                Log.e(TAG, "Error loading members: " + errorMessage);
                memberList.clear(); currentProject.setProjectMembersInfo(new ArrayList<>()); memberAdapter.notifyDataSetChanged();
            }
        });

        // Load Comments
        firestoreService.fetchProjectComments(projectId, new FirestoreService.ProjectRelatedListListener<Comment>() {
            @Override public void onListFetched(List<Comment> items) {
                commentList.clear(); commentList.addAll(items); commentAdapter.notifyDataSetChanged();
            }
            @Override public void onListEmpty() { commentList.clear(); commentAdapter.notifyDataSetChanged(); }
            @Override public void onError(String errorMessage) {
                Log.e(TAG, "Error loading comments: " + errorMessage);
                commentList.clear(); commentAdapter.notifyDataSetChanged();
            }
        });
    }

    private void updateCreatorInfoUI(String creatorName) {
        String createdDateStr = "N/A";
        if (currentProject != null && currentProject.getCreatedAt() != null) {
            createdDateStr = formatDate(currentProject.getCreatedAt());
        }
        textViewProjectCreator.setText(String.format("Tạo bởi: %s - Ngày: %s", creatorName, createdDateStr));
    }

    private void updateChipGroupUI(ChipGroup chipGroup, List<String> items) {
        chipGroup.removeAllViews();
        if (items == null || items.isEmpty()) {
            Chip noDataChip = new Chip(this); noDataChip.setText("Chưa có");
            noDataChip.setEnabled(false); chipGroup.addView(noDataChip); return;
        }
        for (String item : items) {
            Chip chip = new Chip(this); chip.setText(item); chipGroup.addView(chip);
        }
    }

    private void checkInitialUpvoteStatus() {
        if (currentUser != null && projectId != null) {
            firestoreService.checkIfUserUpvoted(projectId, currentUser.getUid(), this::updateUpvoteButtonUI);
        } else {
            updateUpvoteButtonUI(false);
        }
    }

    private void updateUpvoteButtonUI(boolean hasVoted) {
        if (buttonUpvote == null) return;
        if (hasVoted) {
            buttonUpvote.setText("Đã bình chọn");
            buttonUpvote.setIconResource(R.drawable.ic_thumb_up_filled); // Nên có icon filled
        } else {
            buttonUpvote.setText("Bình chọn");
            buttonUpvote.setIconResource(R.drawable.ic_thumb_up);
        }
    }

    private void triggerUpvoteAction() {
        if (currentUser == null) {
            UiHelper.showToast(this, "Bạn cần đăng nhập để bình chọn.", Toast.LENGTH_SHORT); return;
        }
        if (projectId == null) {
            UiHelper.showToast(this, "Lỗi: Không tìm thấy dự án.", Toast.LENGTH_SHORT); return;
        }
        buttonUpvote.setEnabled(false);
        interactionHandler.handleUpvote(currentUser, projectId, new ProjectInteractionHandler.InteractionListener<ProjectInteractionHandler.UpvoteResult>() {
            @Override
            public void onSuccess(ProjectInteractionHandler.UpvoteResult result) {
                if (currentProject != null) currentProject.setVoteCount((int) result.getNewVoteCount());
                textViewVoteCount.setText(String.format(Locale.getDefault(), "%d lượt bình chọn", result.getNewVoteCount()));
                updateUpvoteButtonUI(result.didUserUpvoteNow());
                UiHelper.showToast(ProjectDetailActivity.this, result.didUserUpvoteNow() ? "Đã bình chọn!" : "Đã hủy bình chọn.", Toast.LENGTH_SHORT);
                buttonUpvote.setEnabled(true);
            }
            @Override
            public void onFailure(String errorMessage) {
                UiHelper.showToast(ProjectDetailActivity.this, "Lỗi bình chọn: " + errorMessage, Toast.LENGTH_SHORT);
                buttonUpvote.setEnabled(true);
                checkInitialUpvoteStatus(); // Kiểm tra lại trạng thái vote từ DB khi có lỗi
            }
        });
    }

    private void triggerPostCommentAction() {
        if (currentUser == null) {
            UiHelper.showToast(this, "Bạn cần đăng nhập để bình luận.", Toast.LENGTH_SHORT); return;
        }
        String commentText = editTextNewComment.getText().toString().trim();
        if (commentText.isEmpty()) {
            UiHelper.showToast(this, "Vui lòng nhập bình luận.", Toast.LENGTH_SHORT); return;
        }
        if (projectId == null) {
            UiHelper.showToast(this, "Lỗi: ID dự án không hợp lệ.", Toast.LENGTH_SHORT); return;
        }

        buttonPostComment.setEnabled(false);
        interactionHandler.postComment(currentUser, projectId, commentText, new ProjectInteractionHandler.InteractionListener<Comment>() {
            @Override
            public void onSuccess(Comment newPostedComment) { // newPostedComment là đối tượng từ model của bạn
                UiHelper.showToast(ProjectDetailActivity.this, "Đã gửi bình luận.", Toast.LENGTH_SHORT);
                editTextNewComment.setText("");
                commentList.add(0, newPostedComment);
                commentAdapter.notifyItemInserted(0);
                if(recyclerViewComments.getLayoutManager() != null) recyclerViewComments.scrollToPosition(0);
                buttonPostComment.setEnabled(true);
            }
            @Override
            public void onFailure(String errorMessage) {
                UiHelper.showToast(ProjectDetailActivity.this, "Lỗi gửi bình luận: " + errorMessage, Toast.LENGTH_SHORT);
                buttonPostComment.setEnabled(true);
            }
        });
    }

    private String formatDate(Timestamp timestamp) {
        if (timestamp == null) return "N/A";
        Date date = timestamp.toDate();
        // SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault()); // Kèm giờ phút
        SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault());
        return sdf.format(date);
    }

    private void showErrorAndFinish(String message) {
        UiHelper.showToast(this, message, Toast.LENGTH_LONG);
        finish();
    }
}