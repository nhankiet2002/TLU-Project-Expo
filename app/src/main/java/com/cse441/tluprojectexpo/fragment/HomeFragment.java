// HomeFragment.java
package com.cse441.tluprojectexpo.fragment; // THAY ĐỔI CHO ĐÚNG PACKAGE CỦA BẠN

import android.content.Context;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ProgressBar;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import com.cse441.tluprojectexpo.R; // THAY ĐỔI CHO ĐÚNG PACKAGE CỦA BẠN
import com.cse441.tluprojectexpo.adapter.ProjectAdapter; // THAY ĐỔI CHO ĐÚNG PACKAGE CỦA BẠN
import com.cse441.tluprojectexpo.model.Project;    // THAY ĐỔI CHO ĐÚNG PACKAGE CỦA BẠN
import com.cse441.tluprojectexpo.model.User;       // THAY ĐỔI CHO ĐÚNG PACKAGE CỦA BẠN

import com.google.android.gms.tasks.Task;
import com.google.android.gms.tasks.Tasks;
import com.google.android.material.chip.Chip;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.QuerySnapshot;

import java.util.ArrayList;
import java.util.List;

public class HomeFragment extends Fragment implements ProjectAdapter.OnProjectClickListener {

    private static final String TAG = "HomeFragment";
    private static final int PROJECTS_PER_PAGE = 10;

    public interface OnScrollInteractionListener {
        void onScrollUp();
        void onScrollDown();
    }
    private OnScrollInteractionListener scrollListener;

    private RecyclerView recyclerViewProjects;
    private ProjectAdapter projectAdapter;
    private List<Project> projectList;
    private FirebaseFirestore db;
    private CollectionReference projectsRef;
    private CollectionReference usersRef;
    private CollectionReference techRef;
    private CollectionReference projectTechRef;

    private SwipeRefreshLayout swipeRefreshLayout;
    private ProgressBar progressBarMain;
    private ProgressBar progressBarLoadMore;
    private EditText searchEditText;
    private ImageButton buttonSort;
    private Chip chipLinhVuc, chipCongNghe, chipTrangThai;

    private DocumentSnapshot lastVisibleDocument = null;
    private boolean isLoading = false;
    private boolean isLastPage = false;
    private String currentSearchQuery = "";

    public HomeFragment() {
        // Required empty public constructor
    }

    @Override
    public void onAttach(@NonNull Context context) {
        super.onAttach(context);
        if (context instanceof OnScrollInteractionListener) {
            scrollListener = (OnScrollInteractionListener) context;
        } else {
            Log.w(TAG, context.toString() + " must implement OnScrollInteractionListener");
        }
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_home, container, false);

        db = FirebaseFirestore.getInstance();
        projectsRef = db.collection("Projects");
        usersRef = db.collection("Users");
        techRef = db.collection("Technologies");
        projectTechRef = db.collection("ProjectTechnologies");

        swipeRefreshLayout = view.findViewById(R.id.swipeRefreshLayout);
        recyclerViewProjects = view.findViewById(R.id.recyclerViewProjects);
        progressBarMain = view.findViewById(R.id.progressBarMain);
        progressBarLoadMore = view.findViewById(R.id.progressBarLoadMore);
        searchEditText = view.findViewById(R.id.searchEditText);
        buttonSort = view.findViewById(R.id.button_sort);
        chipLinhVuc = view.findViewById(R.id.chip_linh_vuc);
        chipCongNghe = view.findViewById(R.id.chip_cong_nghe);
        chipTrangThai = view.findViewById(R.id.chip_trang_thai);

        projectList = new ArrayList<>();
        if (getContext() != null) { // Thêm kiểm tra getContext() trước khi sử dụng
            projectAdapter = new ProjectAdapter(getContext(), projectList, this);
            recyclerViewProjects.setLayoutManager(new LinearLayoutManager(getContext()));
            recyclerViewProjects.setAdapter(projectAdapter);
        } else {
            Log.e(TAG, "Context is null in onCreateView, cannot initialize adapter.");
        }


        setupListeners();
        loadInitialProjects();

        return view;
    }

    private void setupListeners() {
        if (swipeRefreshLayout != null) {
            swipeRefreshLayout.setOnRefreshListener(() -> {
                isLastPage = false;
                lastVisibleDocument = null;
                if (projectAdapter != null) projectAdapter.clearProjects();
                loadInitialProjects();
                if (scrollListener != null) {
                    scrollListener.onScrollDown();
                }
            });
        }

        if (recyclerViewProjects != null) {
            recyclerViewProjects.addOnScrollListener(new RecyclerView.OnScrollListener() {
                @Override
                public void onScrolled(@NonNull RecyclerView recyclerView, int dx, int dy) {
                    super.onScrolled(recyclerView, dx, dy);
                    LinearLayoutManager layoutManager = (LinearLayoutManager) recyclerView.getLayoutManager();
                    if (layoutManager != null && projectList != null && !projectList.isEmpty() &&
                            layoutManager.findLastCompletelyVisibleItemPosition() == projectList.size() - 1) {
                        if (!isLoading && !isLastPage) {
                            loadMoreProjects();
                        }
                    }

                    if (scrollListener != null) {
                        if (dy > 5) {
                            scrollListener.onScrollUp();
                        } else if (dy < -5) {
                            scrollListener.onScrollDown();
                        }
                    }
                }
            });
        }

        if (searchEditText != null) {
            searchEditText.addTextChangedListener(new TextWatcher() {
                @Override
                public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
                @Override
                public void onTextChanged(CharSequence s, int start, int before, int count) {}
                @Override
                public void afterTextChanged(Editable s) {
                    currentSearchQuery = s.toString().trim();
                    isLastPage = false;
                    lastVisibleDocument = null;
                    if (projectAdapter != null) projectAdapter.clearProjects();
                    loadInitialProjects();
                }
            });
        }

        if (buttonSort != null) {
            buttonSort.setOnClickListener(v -> {
                if (getContext() != null)
                    Toast.makeText(getContext(), "Chức năng Sắp xếp (chưa triển khai)", Toast.LENGTH_SHORT).show();
            });
        }
        if (chipLinhVuc != null) {
            chipLinhVuc.setOnClickListener(v -> {
                if (getContext() != null)
                    Toast.makeText(getContext(), "Lọc Lĩnh vực (chưa triển khai)", Toast.LENGTH_SHORT).show();
            });
        }
        if (chipCongNghe != null) {
            chipCongNghe.setOnClickListener(v -> {
                if (getContext() != null)
                    Toast.makeText(getContext(), "Lọc Công nghệ (chưa triển khai)", Toast.LENGTH_SHORT).show();
            });
        }
        if (chipTrangThai != null) {
            chipTrangThai.setOnClickListener(v -> {
                if (getContext() != null)
                    Toast.makeText(getContext(), "Lọc Trạng thái (chưa triển khai)", Toast.LENGTH_SHORT).show();
            });
        }
    }

    private void loadInitialProjects() {
        if (isLoading) return;
        isLoading = true;
        if(progressBarMain != null) progressBarMain.setVisibility(View.VISIBLE);
        isLastPage = false;
        lastVisibleDocument = null;

        Query query;
        if (!currentSearchQuery.isEmpty()) {
            query = projectsRef
                    .whereEqualTo("IsApproved", true)
                    .orderBy("Title") // Cần index nếu kết hợp với CreatedAt hoặc filter khác
                    .whereGreaterThanOrEqualTo("Title", currentSearchQuery)
                    .whereLessThanOrEqualTo("Title", currentSearchQuery + "\uf8ff")
                    .limit(PROJECTS_PER_PAGE);
        } else {
            query = projectsRef
                    .whereEqualTo("IsApproved", true)
                    .orderBy("CreatedAt", Query.Direction.DESCENDING) // Index này bạn đã tạo
                    .limit(PROJECTS_PER_PAGE);
        }
        fetchProjects(query, true);
    }

    private void loadMoreProjects() {
        if (isLoading || isLastPage || lastVisibleDocument == null) return;
        isLoading = true;
        if(progressBarLoadMore != null) progressBarLoadMore.setVisibility(View.VISIBLE);

        Query query;
        if (!currentSearchQuery.isEmpty()) {
            query = projectsRef
                    .whereEqualTo("IsApproved", true)
                    .orderBy("Title")
                    .whereGreaterThanOrEqualTo("Title", currentSearchQuery)
                    .whereLessThanOrEqualTo("Title", currentSearchQuery + "\uf8ff")
                    .startAfter(lastVisibleDocument)
                    .limit(PROJECTS_PER_PAGE);
        } else {
            query = projectsRef
                    .whereEqualTo("IsApproved", true)
                    .orderBy("CreatedAt", Query.Direction.DESCENDING)
                    .startAfter(lastVisibleDocument)
                    .limit(PROJECTS_PER_PAGE);
        }
        fetchProjects(query, false);
    }

    private void fetchProjects(Query query, boolean isInitialLoad) {
        query.get().addOnCompleteListener(task -> {
            // Kiểm tra fragment còn attached và context không null trước khi thao tác UI
            if (!isAdded() || getContext() == null) {
                Log.w(TAG, "Fragment not attached or context is null in fetchProjects callback.");
                // Dọn dẹp nếu cần, ví dụ:
                if (swipeRefreshLayout != null && swipeRefreshLayout.isRefreshing()) {
                    swipeRefreshLayout.setRefreshing(false);
                }
                if (progressBarMain != null) progressBarMain.setVisibility(View.GONE);
                if (progressBarLoadMore != null) progressBarLoadMore.setVisibility(View.GONE);
                isLoading = false;
                return;
            }

            if (swipeRefreshLayout != null && swipeRefreshLayout.isRefreshing()) {
                swipeRefreshLayout.setRefreshing(false);
            }
            if(progressBarMain != null) progressBarMain.setVisibility(View.GONE);
            if(progressBarLoadMore != null) progressBarLoadMore.setVisibility(View.GONE);
            isLoading = false;

            if (task.isSuccessful()) {
                QuerySnapshot querySnapshot = task.getResult();
                if (querySnapshot != null && !querySnapshot.isEmpty()) {
                    List<Project> fetchedProjects = new ArrayList<>();
                    List<Task<Void>> tasksToComplete = new ArrayList<>();

                    for (QueryDocumentSnapshot document : querySnapshot) {
                        Project project = document.toObject(Project.class);
                        project.setProjectId(document.getId());
                        fetchedProjects.add(project);

                        // Lấy tên người tạo
                        if (project.getCreatorUserId() != null && !project.getCreatorUserId().isEmpty()) {
                            Task<Void> userTask = usersRef.document(project.getCreatorUserId()).get()
                                    .continueWith(userDocumentTask -> {
                                        if (userDocumentTask.isSuccessful() && userDocumentTask.getResult() != null && userDocumentTask.getResult().exists()) {
                                            User user = userDocumentTask.getResult().toObject(User.class);
                                            if (user != null) {
                                                project.setCreatorFullName(user.getFullName());
                                            }
                                        } else {
                                            Log.w(TAG, "User doc not found or error: " + project.getCreatorUserId(), userDocumentTask.getException());
                                            project.setCreatorFullName(null);
                                        }
                                        return null;
                                    });
                            tasksToComplete.add(userTask);
                        } else {
                            project.setCreatorFullName(null);
                        }

                        // Lấy tên các công nghệ
                        Task<Void> techTask = projectTechRef.whereEqualTo("ProjectId", project.getProjectId()).get()
                                .continueWithTask(projectTechQueryTask -> {
                                    if (projectTechQueryTask.isSuccessful() && projectTechQueryTask.getResult() != null) {
                                        List<Task<DocumentSnapshot>> techNameTasks = new ArrayList<>();
                                        for (QueryDocumentSnapshot ptDoc : projectTechQueryTask.getResult()) {
                                            String techId = ptDoc.getString("TechnologyId");
                                            if (techId != null) {
                                                techNameTasks.add(techRef.document(techId).get());
                                            }
                                        }
                                        return Tasks.whenAllSuccess(techNameTasks); // Trả về List<Object>
                                    }
                                    // Quan trọng: Trả về một task đã hoàn thành với kết quả rỗng nếu có lỗi ở bước trước
                                    return Tasks.forResult(new ArrayList<>());
                                }).continueWith(techNameResultsTask -> {
                                    if (techNameResultsTask.isSuccessful() && techNameResultsTask.getResult() != null) {
                                        // Kết quả của whenAllSuccess là List<Object>, cần cast và kiểm tra
                                        List<?> results = (List<?>) techNameResultsTask.getResult();
                                        List<String> techNames = new ArrayList<>();
                                        for (Object resultItem : results) {
                                            if (resultItem instanceof DocumentSnapshot) {
                                                DocumentSnapshot techDoc = (DocumentSnapshot) resultItem;
                                                if (techDoc.exists()) {
                                                    techNames.add(techDoc.getString("Name"));
                                                }
                                            }
                                        }
                                        project.setTechnologyNames(techNames);
                                    } else {
                                        Log.w(TAG, "Error getting technology names for project " + project.getProjectId(), techNameResultsTask.getException());
                                        project.setTechnologyNames(new ArrayList<>()); // Set list rỗng nếu lỗi
                                    }
                                    return null;
                                });
                        tasksToComplete.add(techTask);
                    }

                    Tasks.whenAll(tasksToComplete).addOnCompleteListener(allExtraInfoTasks -> {
                        if (!isAdded() || projectAdapter == null) { // Thêm kiểm tra projectAdapter
                            Log.w(TAG, "Fragment not attached or adapter is null in whenAllComplete.");
                            return;
                        }

                        if (isInitialLoad) {
                            projectAdapter.updateProjects(fetchedProjects);
                        } else {
                            projectAdapter.addProjects(fetchedProjects);
                        }

                        if (querySnapshot.size() < PROJECTS_PER_PAGE) {
                            isLastPage = true;
                        }
                        // Cập nhật lastVisibleDocument chỉ nếu querySnapshot không rỗng
                        if (!querySnapshot.isEmpty()) {
                            lastVisibleDocument = querySnapshot.getDocuments().get(querySnapshot.size() - 1);
                        } else if (isInitialLoad){
                            // Nếu là lần load đầu và không có kết quả, có thể lastVisibleDocument vẫn là null
                            // không cần làm gì thêm ở đây
                        }

                    });

                } else { // querySnapshot rỗng
                    if (isInitialLoad && projectAdapter != null) { // Thêm kiểm tra projectAdapter
                        projectAdapter.clearProjects();
                        if (getContext() != null) { // Kiểm tra getContext()
                            Toast.makeText(getContext(), "Không có dự án nào.", Toast.LENGTH_SHORT).show();
                        }
                    }
                    isLastPage = true;
                }
            } else { // task không thành công
                Log.e(TAG, "Error getting projects: ", task.getException());
                if (getContext() != null) { // Kiểm tra getContext()
                    Toast.makeText(getContext(), "Lỗi tải dự án: " + task.getException().getMessage(), Toast.LENGTH_LONG).show();
                }
            }
        });
    }


    @Override
    public void onProjectClick(Project project) {
        if (getContext() != null) {
            Toast.makeText(getContext(), "Clicked: " + project.getTitle(), Toast.LENGTH_SHORT).show();
            // Ví dụ: Chuyển sang ProjectDetailActivity
            // Intent intent = new Intent(getActivity(), ProjectDetailActivity.class);
            // intent.putExtra("PROJECT_ID", project.getProjectId());
            // startActivity(intent);
        }
    }

    @Override
    public void onDetach() {
        super.onDetach();
        scrollListener = null;
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        // Giải phóng các tham chiếu đến view ở đây để tránh memory leak
        // Ví dụ: recyclerViewProjects.setAdapter(null);
        // Tuy nhiên, việc này thường không quá cần thiết nếu bạn không giữ tham chiếu mạnh ở đâu đó
        // và ViewPager quản lý vòng đời Fragment tốt.
        // Chỉ làm nếu bạn gặp vấn đề cụ thể về memory leak.
        recyclerViewProjects = null;
        projectAdapter = null;
        projectList = null;
        swipeRefreshLayout = null;
        progressBarMain = null;
        progressBarLoadMore = null;
        searchEditText = null;
        buttonSort = null;
        chipLinhVuc = null;
        chipCongNghe = null;
        chipTrangThai = null;
    }
}