package com.cse441.tluprojectexpo.ui.Home;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ProgressBar;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import com.cse441.tluprojectexpo.R;
import com.cse441.tluprojectexpo.ui.Home.adapter.ProjectAdapter;
// Import Repositories
import com.cse441.tluprojectexpo.repository.CategoryRepository;
import com.cse441.tluprojectexpo.repository.ProjectRepository;
import com.cse441.tluprojectexpo.repository.TechnologyRepository;
// Import Models và UI Managers/Listeners đã có
import com.cse441.tluprojectexpo.ui.Home.model.CategoryFilterItem;
import com.cse441.tluprojectexpo.ui.Home.model.TechnologyFilterItem;
import com.cse441.tluprojectexpo.ui.Home.listener.OnScrollInteractionListener;
import com.cse441.tluprojectexpo.ui.Home.ui.HomeFilterManager;
import com.cse441.tluprojectexpo.ui.Home.ui.HomeSortManager;
import com.cse441.tluprojectexpo.ui.detailproject.ProjectDetailActivity;
import com.cse441.tluprojectexpo.model.Project;
import com.cse441.tluprojectexpo.utils.Constants;
import com.cse441.tluprojectexpo.utils.UiHelper;

import com.google.android.material.chip.Chip;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.Query;

import java.util.ArrayList;
import java.util.List;

public class HomeFragment extends Fragment implements ProjectAdapter.OnProjectClickListener,
        HomeFilterManager.FilterChangeListener, HomeSortManager.SortChangeListener {

    private static final String TAG = "HomeFragment";

    private OnScrollInteractionListener scrollListener;

    private RecyclerView recyclerViewProjects;
    private ProjectAdapter projectAdapter;
    private List<Project> projectList = new ArrayList<>();

    private SwipeRefreshLayout swipeRefreshLayout;
    private ProgressBar progressBarMain;
    private ProgressBar progressBarLoadMore;
    private EditText searchEditText;
    private ImageButton buttonSort;
    private Chip chipLinhVuc, chipCongNghe, chipTrangThai;

    // Repositories
    private ProjectRepository projectRepository;
    private CategoryRepository categoryRepository;
    private TechnologyRepository technologyRepository;

    // UI Managers
    private HomeFilterManager homeFilterManager;
    private HomeSortManager homeSortManager;

    private boolean isLoading = false;
    private String currentSearchQuery = "";
    private DocumentSnapshot lastVisibleDocumentForPagination = null;
    private boolean isLastPageLoaded = false;


    public HomeFragment() { /* Required */ }

    @Override
    public void onAttach(@NonNull Context context) {
        super.onAttach(context);
        if (context instanceof OnScrollInteractionListener) {
            scrollListener = (OnScrollInteractionListener) context;
        }
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_home, container, false);

        // Khởi tạo Repositories
        projectRepository = new ProjectRepository();
        categoryRepository = new CategoryRepository();
        technologyRepository = new TechnologyRepository();

        // Khởi tạo UI Managers
        homeSortManager = new HomeSortManager(requireContext(), this);

        initViews(view); // Khởi tạo views trước khi dùng trong HomeFilterManager
        homeFilterManager = new HomeFilterManager(requireContext(), chipLinhVuc, chipCongNghe, chipTrangThai, this);

        setupRecyclerView();
        setupEventListeners();
        preloadFilterDataForDialogs();
        reloadData(); // Tải dữ liệu lần đầu

        return view;
    }

    private void initViews(View view) {
        swipeRefreshLayout = view.findViewById(R.id.swipeRefreshLayout);
        recyclerViewProjects = view.findViewById(R.id.recyclerViewProjects);

        progressBarLoadMore = view.findViewById(R.id.progressBarLoadMore);
        searchEditText = view.findViewById(R.id.searchEditText);
        buttonSort = view.findViewById(R.id.button_sort);
        chipLinhVuc = view.findViewById(R.id.chip_linh_vuc);
        chipCongNghe = view.findViewById(R.id.chip_cong_nghe);
        chipTrangThai = view.findViewById(R.id.chip_trang_thai);
    }

    private void setupRecyclerView() {
        if (getContext() != null) {
            projectAdapter = new ProjectAdapter(getContext(), projectList, this);
            recyclerViewProjects.setLayoutManager(new LinearLayoutManager(getContext()));
            recyclerViewProjects.setAdapter(projectAdapter);
        }
    }

    private void preloadFilterDataForDialogs() {
        categoryRepository.fetchAllCategories(new CategoryRepository.CategoriesLoadListener() {
            @Override
            public void onCategoriesLoaded(List<CategoryFilterItem> categories) {
                if (homeFilterManager != null) homeFilterManager.setCategoryDataSource(categories);
            }
            @Override
            public void onError(String message) {
                if (getContext()!= null) UiHelper.showToast(getContext(), message, Toast.LENGTH_SHORT);
            }
        });

        technologyRepository.fetchAllTechnologies(new TechnologyRepository.TechnologiesLoadListener() {
            @Override
            public void onTechnologiesLoaded(List<TechnologyFilterItem> technologies) {
                if (homeFilterManager != null) homeFilterManager.setTechnologyDataSource(technologies);
            }
            @Override
            public void onError(String message) {
                if (getContext()!= null) UiHelper.showToast(getContext(), message, Toast.LENGTH_SHORT);
            }
        });

        if (homeFilterManager != null && getContext() != null) {
            homeFilterManager.setStatusDataSource(getResources().getStringArray(R.array.project_statuses));
        }
    }

    private void setupEventListeners() {
        swipeRefreshLayout.setOnRefreshListener(() -> {
            reloadData();
            if (scrollListener != null) scrollListener.onScrollDown(); // Hoặc logic bạn muốn khi refresh
        });

        recyclerViewProjects.addOnScrollListener(new RecyclerView.OnScrollListener() {
            @Override
            public void onScrolled(@NonNull RecyclerView recyclerView, int dx, int dy) {
                super.onScrolled(recyclerView, dx, dy);
                LinearLayoutManager layoutManager = (LinearLayoutManager) recyclerView.getLayoutManager();
                if (layoutManager != null && projectAdapter != null && projectAdapter.getItemCount() > 0 &&
                        layoutManager.findLastCompletelyVisibleItemPosition() == projectAdapter.getItemCount() - 1) {
                    if (!isLoading && !isLastPageLoaded) {
                        loadMoreProjects();
                    }
                }
                if (scrollListener != null) {
                    if (dy > 5) scrollListener.onScrollUp();
                    else if (dy < -5) scrollListener.onScrollDown();
                }
            }
        });

        searchEditText.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) {}
            @Override public void afterTextChanged(Editable s) {
                currentSearchQuery = s.toString().trim();
                reloadData();
            }
        });

        buttonSort.setOnClickListener(v -> {
            if (homeSortManager != null) homeSortManager.showSortMenu(v);
        });
        // Chip click listeners đã được HomeFilterManager xử lý
    }

    private void reloadData() {
        isLoading = false;
        isLastPageLoaded = false;
        lastVisibleDocumentForPagination = null;
        if (projectAdapter != null) projectAdapter.clearProjects();
        if (homeFilterManager != null) homeFilterManager.updateAllChipUI();
        loadProjects(true);
    }

    private void loadProjects(boolean isInitialLoad) {
        if (isLoading || projectRepository == null) return;
        isLoading = true;
        showProgress(isInitialLoad, true);

        String categoryId = homeFilterManager != null ? homeFilterManager.getSelectedCategoryId() : null;
        String technologyId = homeFilterManager != null ? homeFilterManager.getSelectedTechnologyId() : null;
        String status = homeFilterManager != null ? homeFilterManager.getSelectedStatus() : null;
        String sortField = homeSortManager != null ? homeSortManager.getCurrentSortField() : Constants.FIELD_CREATED_AT;
        Query.Direction sortDirection = homeSortManager != null ? homeSortManager.getCurrentSortDirection() : Query.Direction.DESCENDING;
        DocumentSnapshot lastVisibleForQuery = isInitialLoad ? null : lastVisibleDocumentForPagination;


        projectRepository.fetchProjectsList(currentSearchQuery, sortField, sortDirection,
                categoryId, technologyId, status, lastVisibleForQuery,
                new ProjectRepository.ProjectsListFetchListener() {
                    @Override
                    public void onProjectsFetched(ProjectRepository.ProjectListResult result) {
                        if (!isAdded() || projectAdapter == null) return;
                        isLoading = false;
                        showProgress(isInitialLoad, false);

                        if (isInitialLoad) {
                            projectList.clear();
                        }
                        projectList.addAll(result.projects);
                        projectAdapter.notifyDataSetChanged(); // Hoặc dùng các notify cụ thể hơn

                        lastVisibleDocumentForPagination = result.newLastVisible;
                        isLastPageLoaded = result.isLastPage;

                        if (isInitialLoad && projectList.isEmpty()) {
                            // onNoProjectsFound(); // Sẽ được gọi từ repository nếu cần
                        }
                    }

                    @Override
                    public void onFetchFailed(String errorMessage) {
                        if (!isAdded()) return;
                        isLoading = false;
                        showProgress(isInitialLoad, false);
                        if (getContext() != null) UiHelper.showToast(getContext(), errorMessage, Toast.LENGTH_LONG);
                    }

                    @Override
                    public void onNoProjectsFound() {
                        if (!isAdded() || projectAdapter == null) return;
                        isLoading = false;
                        showProgress(isInitialLoad, false);
                        if (isInitialLoad) { // Chỉ clear và hiển thị toast nếu là lần tải đầu
                            projectAdapter.clearProjects();
                            if (getContext() != null) UiHelper.showToast(getContext(), "Không có dự án nào khớp.", Toast.LENGTH_SHORT);
                        }
                        isLastPageLoaded = true; // Đánh dấu là trang cuối
                    }
                });
    }

    private void loadMoreProjects() {
        loadProjects(false);
    }

    private void showProgress(boolean isInitial, boolean show) {
        if (isInitial) {
            if (!show && swipeRefreshLayout != null && swipeRefreshLayout.isRefreshing()) {
                swipeRefreshLayout.setRefreshing(false);
            }
        } else {
            if (progressBarLoadMore != null) progressBarLoadMore.setVisibility(show ? View.VISIBLE : View.GONE);
        }
    }

    @Override
    public void onProjectClick(Project project) {
        if (getActivity() != null && project != null && project.getProjectId() != null) {
            Intent intent = new Intent(getActivity(), ProjectDetailActivity.class);
            intent.putExtra(ProjectDetailActivity.EXTRA_PROJECT_ID, project.getProjectId());
            startActivity(intent);
        } else {
            if (getContext() != null) UiHelper.showToast(getContext(), "Không thể mở chi tiết dự án.", Toast.LENGTH_SHORT);
        }
    }

    @Override
    public void onFilterChanged() {
        reloadData();
    }

    @Override
    public void onSortChanged(String newSortField, Query.Direction newSortDirection) {
        reloadData();
    }

    @Override
    public void onDetach() {
        super.onDetach();
        scrollListener = null;
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        recyclerViewProjects = null; projectAdapter = null; swipeRefreshLayout = null;
        progressBarLoadMore = null; searchEditText = null;
        buttonSort = null; chipLinhVuc = null; chipCongNghe = null; chipTrangThai = null;
        projectRepository = null; categoryRepository = null; technologyRepository = null;
        homeFilterManager = null; homeSortManager = null;
        if(projectList != null) projectList.clear();
    }
}