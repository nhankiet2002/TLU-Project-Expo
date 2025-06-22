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
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import com.cse441.tluprojectexpo.R;
import com.cse441.tluprojectexpo.ui.Home.adapter.ProjectAdapter;
import com.cse441.tluprojectexpo.ui.Home.data.FilterDataProvider;
import com.cse441.tluprojectexpo.ui.Home.data.ProjectListFetcher;
import com.cse441.tluprojectexpo.ui.Home.data.model.CategoryFilterItem;
import com.cse441.tluprojectexpo.ui.Home.data.model.TechnologyFilterItem;
import com.cse441.tluprojectexpo.ui.Home.listener.OnScrollInteractionListener;
import com.cse441.tluprojectexpo.ui.Home.ui.HomeFilterManager;
import com.cse441.tluprojectexpo.ui.Home.ui.HomeSortManager;
import com.cse441.tluprojectexpo.ui.detailproject.ProjectDetailActivity;
import com.cse441.tluprojectexpo.model.Project;
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

    private ProjectListFetcher projectListFetcher;
    private FilterDataProvider filterDataProvider;
    private HomeFilterManager homeFilterManager;
    private HomeSortManager homeSortManager;

    private boolean isLoading = false;
    private String currentSearchQuery = "";

    public HomeFragment() { /* Required empty public constructor */ }

    @Override
    public void onAttach(@NonNull Context context) {
        super.onAttach(context);
        if (context instanceof OnScrollInteractionListener) {
            scrollListener = (OnScrollInteractionListener) context;
        }
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_home, container, false);

        projectListFetcher = new ProjectListFetcher();
        filterDataProvider = new FilterDataProvider();
        homeSortManager = new HomeSortManager(requireContext(), this); // Pass 'this' as listener

        initViews(view);
        setupRecyclerView();
        homeFilterManager = new HomeFilterManager(requireContext(), chipLinhVuc, chipCongNghe, chipTrangThai, this);
        setupEventListeners();
        preloadFilterDataForDialogs();
        reloadData();

        return view;
    }

    private void initViews(View view) {
        swipeRefreshLayout = view.findViewById(R.id.swipeRefreshLayout);
        recyclerViewProjects = view.findViewById(R.id.recyclerViewProjects);
        progressBarMain = view.findViewById(R.id.progressBarMain);
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
        filterDataProvider.loadCategories(new FilterDataProvider.CategoriesLoadListener() {
            @Override
            public void onCategoriesLoaded(List<CategoryFilterItem> categories) {
                if (homeFilterManager != null) homeFilterManager.setCategoryDataSource(categories);
            }
            @Override
            public void onError(String message) {
                if (getContext()!= null) Toast.makeText(getContext(), message, Toast.LENGTH_SHORT).show();
            }
        });

        filterDataProvider.loadTechnologies(new FilterDataProvider.TechnologiesLoadListener() {
            @Override
            public void onTechnologiesLoaded(List<TechnologyFilterItem> technologies) {
                if (homeFilterManager != null) homeFilterManager.setTechnologyDataSource(technologies);
            }
            @Override
            public void onError(String message) {
                if (getContext()!= null) Toast.makeText(getContext(), message, Toast.LENGTH_SHORT).show();
            }
        });
        if (homeFilterManager != null && getContext() != null) {
            homeFilterManager.setStatusDataSource(getResources().getStringArray(R.array.project_statuses));
        }
    }

    private void setupEventListeners() {
        swipeRefreshLayout.setOnRefreshListener(() -> {
            reloadData();
            if (scrollListener != null) scrollListener.onScrollDown();
        });

        recyclerViewProjects.addOnScrollListener(new RecyclerView.OnScrollListener() {
            @Override
            public void onScrolled(@NonNull RecyclerView recyclerView, int dx, int dy) {
                super.onScrolled(recyclerView, dx, dy);
                LinearLayoutManager layoutManager = (LinearLayoutManager) recyclerView.getLayoutManager();
                if (layoutManager != null && projectAdapter != null && projectAdapter.getItemCount() > 0 &&
                        layoutManager.findLastCompletelyVisibleItemPosition() == projectAdapter.getItemCount() - 1) {
                    if (!isLoading && !projectListFetcher.isLastPage()) {
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
        // Chip click listeners are handled by HomeFilterManager
    }

    private void reloadData() {
        isLoading = false; // Reset loading state
        if (projectListFetcher != null) projectListFetcher.resetPagination();
        if (projectAdapter != null) projectAdapter.clearProjects();
        if (homeFilterManager != null) homeFilterManager.updateAllChipUI(); // Cập nhật UI chip
        loadInitialProjects();
    }

    private void loadInitialProjects() {
        if (isLoading || projectListFetcher == null) return;
        isLoading = true;
        if (progressBarMain != null) progressBarMain.setVisibility(View.VISIBLE);

        projectListFetcher.fetchProjects(
                currentSearchQuery,
                homeSortManager.getCurrentSortField(),
                homeSortManager.getCurrentSortDirection(),
                homeFilterManager.getSelectedCategoryId(),
                homeFilterManager.getSelectedTechnologyId(),
                homeFilterManager.getSelectedStatus(),
                true, // isInitialLoad
                projectsFetchListener
        );
    }

    private void loadMoreProjects() {
        if (isLoading || projectListFetcher == null || projectListFetcher.isLastPage()) return;
        isLoading = true;
        if (progressBarLoadMore != null) progressBarLoadMore.setVisibility(View.VISIBLE);

        projectListFetcher.fetchProjects(
                currentSearchQuery,
                homeSortManager.getCurrentSortField(),
                homeSortManager.getCurrentSortDirection(),
                homeFilterManager.getSelectedCategoryId(),
                homeFilterManager.getSelectedTechnologyId(),
                homeFilterManager.getSelectedStatus(),
                false, // not initialLoad
                projectsFetchListener
        );
    }

    private ProjectListFetcher.ProjectsFetchListener projectsFetchListener = new ProjectListFetcher.ProjectsFetchListener() {
        @Override
        public void onProjectsFetched(List<Project> projects, boolean isNowLastPage, @Nullable DocumentSnapshot newLastVisible) {
            if (!isAdded() || projectAdapter == null) return; // Fragment not attached or adapter null
            hideProgress(projectListFetcher.isLastPage() && projects.isEmpty()); // Hide progress based on combined state

            if (projectListFetcher.isLastPage() && projectList.isEmpty() && projects.isEmpty()){ // True if was initial load and no projects
                // Handled by onNoProjectsFound
            } else if (projects.isEmpty() && !projectListFetcher.isLastPage()){
                // This case should ideally not happen if fetchProjects logic is correct (empty list but not last page)
                // but good to handle. Could mean an issue or just end of current filtered set.
            } else {
                if (projectList.isEmpty() && projects.isEmpty() && isNowLastPage) { // First load, no results, is last page
                    // onNoProjectsFound() will be called by fetcher if it's an initial load and no projects.
                    // If it's a "load more" and no projects, then it's truly the end.
                } else {
                    projectAdapter.addProjects(projects);
                }
            }
            isLoading = false;
        }


        @Override
        public void onFetchFailed(String errorMessage) {
            if (!isAdded()) return;
            hideProgress(true); // Hide all progress on failure
            isLoading = false;
            if (getContext() != null) Toast.makeText(getContext(), errorMessage, Toast.LENGTH_LONG).show();
        }

        @Override
        public void onNoProjectsFound() {
            if (!isAdded() || projectAdapter == null) return;
            hideProgress(true); // Hide all progress
            isLoading = false;
            projectAdapter.clearProjects(); // Đảm bảo danh sách trống
            if (getContext() != null) Toast.makeText(getContext(), "Không có dự án nào khớp.", Toast.LENGTH_SHORT).show();
        }
    };


    private void hideProgress(boolean isInitialLoadOrNoMore) {
        if (progressBarMain != null) progressBarMain.setVisibility(View.GONE);
        if (progressBarLoadMore != null) progressBarLoadMore.setVisibility(View.GONE);
        if (swipeRefreshLayout != null && swipeRefreshLayout.isRefreshing()) {
            swipeRefreshLayout.setRefreshing(false);
        }
    }

    @Override
    public void onProjectClick(Project project) {
        if (getActivity() != null && project != null && project.getProjectId() != null) {
            Intent intent = new Intent(getActivity(), ProjectDetailActivity.class);
            intent.putExtra(ProjectDetailActivity.EXTRA_PROJECT_ID, project.getProjectId());
            startActivity(intent);
        } else {
            if (getContext() != null) Toast.makeText(getContext(), "Không thể mở chi tiết.", Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    public void onFilterChanged() {
        reloadData(); // Khi bộ lọc thay đổi, tải lại dữ liệu
    }

    @Override
    public void onSortChanged(String newSortField, Query.Direction newSortDirection) {
        reloadData(); // Khi sắp xếp thay đổi, tải lại dữ liệu
    }

    @Override
    public void onDetach() {
        super.onDetach();
        scrollListener = null;
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        // Nullify views to avoid memory leaks
        recyclerViewProjects = null; projectAdapter = null; swipeRefreshLayout = null;
        progressBarMain = null; progressBarLoadMore = null; searchEditText = null;
        buttonSort = null; chipLinhVuc = null; chipCongNghe = null; chipTrangThai = null;
        projectListFetcher = null; filterDataProvider = null; homeFilterManager = null; homeSortManager = null;
        projectList.clear();
    }
}