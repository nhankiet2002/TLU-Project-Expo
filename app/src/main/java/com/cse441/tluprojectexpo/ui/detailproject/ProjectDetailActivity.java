package com.cse441.tluprojectexpo.ui.detailproject;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
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
// Models
import com.cse441.tluprojectexpo.model.Comment;
import com.cse441.tluprojectexpo.model.Project;
import com.cse441.tluprojectexpo.model.User;
// Repositories
import com.cse441.tluprojectexpo.repository.CommentRepository;
import com.cse441.tluprojectexpo.repository.CourseRepository;
import com.cse441.tluprojectexpo.repository.ProjectRepository;
import com.cse441.tluprojectexpo.repository.UserRepository;
import com.cse441.tluprojectexpo.repository.VoteRepository;
// Utils
import com.cse441.tluprojectexpo.utils.UiHelper;
// Adapters
import com.cse441.tluprojectexpo.ui.detailproject.adapter.CommentAdapter;
import com.cse441.tluprojectexpo.ui.detailproject.adapter.MediaGalleryAdapter;
import com.cse441.tluprojectexpo.ui.detailproject.adapter.ProjectMemberAdapter;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.chip.Chip;
import com.google.android.material.chip.ChipGroup;
import com.google.firebase.Timestamp;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class ProjectDetailActivity extends AppCompatActivity implements CommentAdapter.OnCommentInteractionListener {

    public static final String EXTRA_PROJECT_ID = "EXTRA_PROJECT_ID";
    private static final String TAG = "ProjectDetailActivity";

    // UI Elements (giữ nguyên)
    private ImageView ivBackArrow;
    private TextView tvToolbarTitle; /* ... và các UI elements khác ... */
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
    private TextView tvReplyingTo;


    // Repositories
    private ProjectRepository projectRepository;
    private UserRepository userRepository;
    private CourseRepository courseRepository;
    private CommentRepository commentRepository;
    private VoteRepository voteRepository;

    // Firebase Auth
    private FirebaseAuth mAuth;
    private FirebaseUser currentUser;

    // Data (giữ nguyên)
    private String projectId;
    private Project currentProject;
    private ProjectMemberAdapter memberAdapter;
    private MediaGalleryAdapter mediaGalleryAdapter;
    private CommentAdapter commentAdapter;
    private List<Project.UserShortInfo> memberList = new ArrayList<>();
    private List<Project.MediaItem> mediaList = new ArrayList<>();
    private List<Comment> rootCommentList = new ArrayList<>();
    private String replyingToCommentId = null;
    private String commentIdToScroll = null;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_project_detail);

        mAuth = FirebaseAuth.getInstance();
        currentUser = mAuth.getCurrentUser();

        // Khởi tạo Repositories
        projectRepository = new ProjectRepository();
        userRepository = new UserRepository();
        courseRepository = new CourseRepository();
        commentRepository = new CommentRepository();
        voteRepository = new VoteRepository();

        initViews();
        setupRecyclerViews();
        setupListeners();

        projectId = getIntent().getStringExtra(EXTRA_PROJECT_ID);
        commentIdToScroll = getIntent().getStringExtra("commentId");
        if (projectId == null || projectId.isEmpty()) {
            UiHelper.showToast(this, "Không có ID dự án.", Toast.LENGTH_LONG);
            finish();
            return;
        }
        loadAllProjectData();
    }

    private void initViews() { /* ... (giữ nguyên) ... */
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
        tvReplyingTo = findViewById(R.id.tvReplyingTo);
    }
    private void setupRecyclerViews() { /* ... (giữ nguyên) ... */
        recyclerViewMembers.setLayoutManager(new LinearLayoutManager(this));
        memberAdapter = new ProjectMemberAdapter(this, memberList);
        recyclerViewMembers.setAdapter(memberAdapter);
        recyclerViewMembers.setNestedScrollingEnabled(false);

        recyclerViewMediaGallery.setLayoutManager(new LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false));
        mediaGalleryAdapter = new MediaGalleryAdapter(this, mediaList);
        recyclerViewMediaGallery.setAdapter(mediaGalleryAdapter);

        recyclerViewComments.setLayoutManager(new LinearLayoutManager(this));
        commentAdapter = new CommentAdapter(this, new ArrayList<>(), this);
        recyclerViewComments.setAdapter(commentAdapter);
        recyclerViewComments.setNestedScrollingEnabled(false);
    }
    private void setupListeners() { /* ... (giữ nguyên) ... */
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
    private void openUrl(String url) { /* ... (giữ nguyên) ... */
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
        projectRepository.fetchProjectDetails(projectId, new ProjectRepository.ProjectDetailsListener() {
            @Override
            public void onProjectFetched(Project project) {
                currentProject = project;
                populateBaseUI();
                loadAdditionalProjectData();
                checkInitialUpvoteStatus();
            }
            @Override public void onProjectNotFound() { showErrorAndFinish("Dự án không tồn tại."); }
            @Override public void onError(String errorMessage) { showErrorAndFinish("Lỗi tải dự án: " + errorMessage); }
        });
    }

    private void populateBaseUI() { /* ... (giữ nguyên) ... */
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
            if (mediaGalleryAdapter != null) mediaGalleryAdapter.notifyDataSetChanged();
        } else {
            textViewMediaGalleryTitle.setVisibility(View.GONE);
            recyclerViewMediaGallery.setVisibility(View.GONE);
        }
    }
    private void updateLinkUI(TextView textView, String prefix, String url) { /* ... (giữ nguyên) ... */
        if (url != null && !url.isEmpty()) {
            textView.setText(prefix + url);
            textView.setVisibility(View.VISIBLE);
        } else {
            textView.setVisibility(View.GONE);
        }
    }
    private void setStatusUI(String status) { /* ... (giữ nguyên) ... */
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
            userRepository.fetchUserDetails(currentProject.getCreatorUserId(), new UserRepository.UserDetailsFetchListener() {
                @Override
                public void onUserDetailsFetched(User user) {
                    if (currentProject != null) currentProject.setCreatorFullName(user.getFullName());
                    updateCreatorInfoUI(user.getFullName());
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
            courseRepository.fetchCourseDetails(currentProject.getCourseId(), new CourseRepository.CourseDetailsListener() {
                @Override public void onCourseFetched(String courseName) {
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

        // Load Categories, Technologies, Members using ProjectRepository
        projectRepository.fetchCategoriesForProject(projectId, new ProjectRepository.ProjectRelatedListListener<String>() {
            @Override public void onListFetched(List<String> items) {
                if (currentProject != null) currentProject.setCategoryNames(items);
                updateChipGroupUI(chipGroupCategories, items);
            }
            @Override public void onListEmpty() { updateChipGroupUI(chipGroupCategories, new ArrayList<>()); }
            @Override public void onError(String e) { updateChipGroupUI(chipGroupCategories, new ArrayList<>()); Log.e(TAG, "Lỗi tải categories: " + e);}
        });

        projectRepository.fetchTechnologiesForProject(projectId, new ProjectRepository.ProjectRelatedListListener<String>() {
            @Override public void onListFetched(List<String> items) {
                if (currentProject != null) currentProject.setTechnologyNames(items);
                updateChipGroupUI(chipGroupTechnologies, items);
            }
            @Override public void onListEmpty() { updateChipGroupUI(chipGroupTechnologies, new ArrayList<>()); }
            @Override public void onError(String e) { updateChipGroupUI(chipGroupTechnologies, new ArrayList<>()); Log.e(TAG, "Lỗi tải technologies: " + e);}
        });

        projectRepository.fetchProjectMembers(projectId, new ProjectRepository.ProjectRelatedListListener<Project.UserShortInfo>() {
            @Override public void onListFetched(List<Project.UserShortInfo> items) {
                memberList.clear(); memberList.addAll(items);
                if (currentProject != null) currentProject.setProjectMembersInfo(items);
                if (memberAdapter != null) memberAdapter.notifyDataSetChanged();
            }
            @Override public void onListEmpty() {
                memberList.clear(); if (currentProject != null) currentProject.setProjectMembersInfo(new ArrayList<>());
                if (memberAdapter != null) memberAdapter.notifyDataSetChanged();
            }
            @Override public void onError(String e) {
                memberList.clear(); if (currentProject != null) currentProject.setProjectMembersInfo(new ArrayList<>());
                if (memberAdapter != null) memberAdapter.notifyDataSetChanged();
                Log.e(TAG, "Lỗi tải members: " + e);
            }
        });

        // Load Comments using CommentRepository
        loadCommentsWithReplies();
    }

    private void updateCreatorInfoUI(String creatorName) { /* ... (giữ nguyên) ... */
        String createdDateStr = "N/A";
        if (currentProject != null && currentProject.getCreatedAt() != null) {
            createdDateStr = formatDate(currentProject.getCreatedAt());
        }
        textViewProjectCreator.setText(String.format("Tạo bởi: %s - Ngày: %s", creatorName, createdDateStr));
    }
    private void updateChipGroupUI(ChipGroup chipGroup, List<String> items) { /* ... (giữ nguyên) ... */
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
            voteRepository.checkIfUserUpvoted(projectId, currentUser.getUid(), this::updateUpvoteButtonUI);
        } else {
            updateUpvoteButtonUI(false);
        }
    }
    private void updateUpvoteButtonUI(boolean hasVoted) { /* ... (giữ nguyên) ... */
        if (buttonUpvote == null) return;
        if (hasVoted) {
            buttonUpvote.setText("Đã bình chọn");
            buttonUpvote.setIconResource(R.drawable.ic_thumb_up_filled);
        } else {
            buttonUpvote.setText("Bình chọn");
            buttonUpvote.setIconResource(R.drawable.ic_thumb_up);
        }
    }

    private void triggerUpvoteAction() {
        if (currentUser == null) { UiHelper.showToast(this, "Bạn cần đăng nhập.", Toast.LENGTH_SHORT); return; }
        if (projectId == null) { UiHelper.showToast(this, "Lỗi dự án.", Toast.LENGTH_SHORT); return; }
        buttonUpvote.setEnabled(false);
        voteRepository.handleUpvote(currentUser, projectId, new VoteRepository.VoteInteractionListener<VoteRepository.UpvoteResult>() {
            @Override
            public void onSuccess(VoteRepository.UpvoteResult result) {
                if (currentProject != null) currentProject.setVoteCount((int) result.getNewVoteCount());
                textViewVoteCount.setText(String.format(Locale.getDefault(), "%d lượt bình chọn", result.getNewVoteCount()));
                updateUpvoteButtonUI(result.didUserUpvoteNow());
                UiHelper.showToast(ProjectDetailActivity.this, result.didUserUpvoteNow() ? "Đã bình chọn!" : "Đã hủy bình chọn.", Toast.LENGTH_SHORT);
                buttonUpvote.setEnabled(true);
            }
            @Override
            public void onFailure(String errorMessage) {
                UiHelper.showToast(ProjectDetailActivity.this, "Lỗi: " + errorMessage, Toast.LENGTH_SHORT);
                buttonUpvote.setEnabled(true);
                checkInitialUpvoteStatus();
            }
        });
    }

    private void triggerPostCommentAction() {
        if (currentUser == null) { UiHelper.showToast(this, "Bạn cần đăng nhập.", Toast.LENGTH_SHORT); return; }
        String commentText = editTextNewComment.getText().toString().trim();
        if (commentText.isEmpty()) { UiHelper.showToast(this, "Nhập bình luận.", Toast.LENGTH_SHORT); return; }
        if (projectId == null) { UiHelper.showToast(this, "Lỗi dự án.", Toast.LENGTH_SHORT); return; }

        buttonPostComment.setEnabled(false);
        String parentIdForThisPost = replyingToCommentId;

        commentRepository.postComment(currentUser, projectId, commentText, parentIdForThisPost,
                new CommentRepository.CommentPostListener() {
                    @Override
                    public void onCommentPosted(Comment newPostedComment) {
                        UiHelper.showToast(ProjectDetailActivity.this, "Đã gửi bình luận.", Toast.LENGTH_SHORT);
                        editTextNewComment.setText("");
                        hideKeyboard();
                        loadCommentsWithReplies(); // Tải lại comments
                        if (tvReplyingTo != null) tvReplyingTo.setVisibility(View.GONE);
                        replyingToCommentId = null;
                        buttonPostComment.setEnabled(true);
                    }
                    @Override
                    public void onPostFailed(String errorMessage) {
                        UiHelper.showToast(ProjectDetailActivity.this, "Lỗi: " + errorMessage, Toast.LENGTH_SHORT);
                        buttonPostComment.setEnabled(true);
                    }
                });
    }

    private void loadCommentsWithReplies() {
        if (projectId == null || commentRepository == null) return;
        commentRepository.fetchProjectCommentsWithReplies(projectId, new CommentRepository.CommentsLoadListener() {
            @Override
            public void onCommentsLoaded(List<Comment> rootComments) {
                rootCommentList.clear(); rootCommentList.addAll(rootComments);
                if (commentAdapter != null) commentAdapter.updateComments(rootCommentList);
                if (commentIdToScroll != null) {
                    int pos = findCommentPositionById(commentIdToScroll);
                    if (pos >= 0) {
                        recyclerViewComments.scrollToPosition(pos);
                    }
                    commentIdToScroll = null;
                }
            }
            @Override public void onCommentsEmpty() {
                rootCommentList.clear();
                if (commentAdapter != null) commentAdapter.updateComments(new ArrayList<>());
            }
            @Override public void onError(String errorMessage) {
                Log.e(TAG, "Lỗi tải lại comments: " + errorMessage);
                rootCommentList.clear();
                if (commentAdapter != null) commentAdapter.updateComments(new ArrayList<>());
            }
        });
    }

    private int findCommentPositionById(String commentId) {
        if (commentAdapter == null || commentId == null) return -1;
        for (int i = 0; i < commentAdapter.getItemCount(); i++) {
            com.cse441.tluprojectexpo.ui.detailproject.adapter.DisplayableCommentItem item = commentAdapter.getDisplayableItemAt(i);
            if (item.comment != null && commentId.equals(item.comment.getCommentId())) {
                return i;
            }
        }
        return -1;
    }

    @Override
    public void onReplyClicked(Comment parentComment) {
        // ... (giữ nguyên như phiên bản trước)
        if (parentComment == null || parentComment.getCommentId() == null) return;
        replyingToCommentId = parentComment.getCommentId();

        if (tvReplyingTo != null && parentComment.getUserName() != null) {
            tvReplyingTo.setText("Đang trả lời " + parentComment.getUserName() + "...");
            tvReplyingTo.setVisibility(View.VISIBLE);
        } else if (tvReplyingTo != null) {
            tvReplyingTo.setText("Đang trả lời bình luận...");
            tvReplyingTo.setVisibility(View.VISIBLE);
        }

        if (editTextNewComment != null) {
            editTextNewComment.requestFocus();
            InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
            if (imm != null) {
                imm.showSoftInput(editTextNewComment, InputMethodManager.SHOW_IMPLICIT);
            }
        }
    }
    private void hideKeyboard() { /* ... (giữ nguyên) ... */
        View view = this.getCurrentFocus();
        if (view != null) {
            InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
            if (imm != null) {
                imm.hideSoftInputFromWindow(view.getWindowToken(), 0);
            }
        }
    }
    private String formatDate(Timestamp timestamp) { /* ... (giữ nguyên) ... */
        if (timestamp == null) return "N/A";
        Date date = timestamp.toDate();
        SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault());
        return sdf.format(date);
    }
    private void showErrorAndFinish(String message) { /* ... (giữ nguyên) ... */
        UiHelper.showToast(this, message, Toast.LENGTH_LONG);
        finish();
    }
}