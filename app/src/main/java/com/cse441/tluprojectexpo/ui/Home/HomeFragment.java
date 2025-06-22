// HomeFragment.java
package com.cse441.tluprojectexpo.ui.Home; // Hoặc package đúng của bạn

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.PopupMenu;
import android.widget.ProgressBar;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import com.cse441.tluprojectexpo.R;
import com.cse441.tluprojectexpo.ui.Home.adapter.ProjectAdapter;
import com.cse441.tluprojectexpo.ui.detailproject.ProjectDetailActivity; // Đảm bảo tên package/class này đúng
import com.cse441.tluprojectexpo.model.Project;
import com.cse441.tluprojectexpo.model.User;

import com.google.android.gms.tasks.Task;
import com.google.android.gms.tasks.Tasks;
import com.google.android.material.chip.Chip;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FieldPath;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.QuerySnapshot;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

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
    private CollectionReference categoriesRef;
    private CollectionReference projectCategoriesRef;

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
    private String currentSortField = "CreatedAt"; // Mặc định vẫn là CreatedAt
    private Query.Direction currentSortDirection = Query.Direction.DESCENDING;

    private String selectedCategoryId = null;
    private String selectedCategoryName = null;
    private String selectedTechnologyId = null;
    private String selectedTechnologyName = null;
    private String selectedStatus = null;

    private List<CategoryItem> categoryListForDialog = new ArrayList<>();
    private List<TechnologyItem> technologyListForDialog = new ArrayList<>();
    private String[] statusArrayForDialog;

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
        categoriesRef = db.collection("Categories");
        projectCategoriesRef = db.collection("ProjectCategories");

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
        if (getContext() != null) {
            projectAdapter = new ProjectAdapter(getContext(), projectList, this);
            recyclerViewProjects.setLayoutManager(new LinearLayoutManager(getContext()));
            recyclerViewProjects.setAdapter(projectAdapter);
            statusArrayForDialog = getResources().getStringArray(R.array.project_statuses);
        } else {
            Log.e(TAG, "Context is null in onCreateView, cannot initialize adapter or resources.");
        }

        setupListeners();
        preloadFilterData();
        reloadData();

        return view;
    }

    private void preloadFilterData() {
        if (categoriesRef != null) {
            categoriesRef.orderBy("Name").get().addOnSuccessListener(queryDocumentSnapshots -> {
                categoryListForDialog.clear();
                for (QueryDocumentSnapshot doc : queryDocumentSnapshots) {
                    categoryListForDialog.add(new CategoryItem(doc.getId(), doc.getString("Name")));
                }
            }).addOnFailureListener(e -> Log.e(TAG, "Error loading categories", e));
        }

        if (techRef != null) {
            techRef.orderBy("Name").get().addOnSuccessListener(queryDocumentSnapshots -> {
                technologyListForDialog.clear();
                for (QueryDocumentSnapshot doc : queryDocumentSnapshots) {
                    technologyListForDialog.add(new TechnologyItem(doc.getId(), doc.getString("Name")));
                }
            }).addOnFailureListener(e -> Log.e(TAG, "Error loading technologies", e));
        }
    }

    private void setupListeners() {
        if (swipeRefreshLayout != null) {
            swipeRefreshLayout.setOnRefreshListener(() -> {
                reloadData();
                if (scrollListener != null) scrollListener.onScrollDown();
            });
        }

        if (recyclerViewProjects != null) {
            recyclerViewProjects.addOnScrollListener(new RecyclerView.OnScrollListener() {
                @Override
                public void onScrolled(@NonNull RecyclerView recyclerView, int dx, int dy) {
                    super.onScrolled(recyclerView, dx, dy);
                    LinearLayoutManager layoutManager = (LinearLayoutManager) recyclerView.getLayoutManager();
                    if (layoutManager != null && projectAdapter != null && projectAdapter.getItemCount() > 0 &&
                            layoutManager.findLastCompletelyVisibleItemPosition() == projectAdapter.getItemCount() - 1) {
                        if (!isLoading && !isLastPage) {
                            loadMoreProjects();
                        }
                    }
                    if (scrollListener != null) {
                        if (dy > 5) scrollListener.onScrollUp();
                        else if (dy < -5) scrollListener.onScrollDown();
                    }
                }
            });
        }

        if (searchEditText != null) {
            searchEditText.addTextChangedListener(new TextWatcher() {
                @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
                @Override public void onTextChanged(CharSequence s, int start, int before, int count) {}
                @Override public void afterTextChanged(Editable s) {
                    currentSearchQuery = s.toString().trim();
                    reloadData();
                }
            });
        }

        if (buttonSort != null) buttonSort.setOnClickListener(this::showSortMenu);

        if (chipLinhVuc != null) {
            chipLinhVuc.setOnClickListener(v -> showCategoryFilterDialog());
            chipLinhVuc.setOnCloseIconClickListener(v -> {
                selectedCategoryId = null; selectedCategoryName = null;
                updateChipUI(chipLinhVuc, "Lĩnh vực/ Chủ đề", false);
                reloadData();
            });
        }
        if (chipCongNghe != null) {
            chipCongNghe.setOnClickListener(v -> showTechnologyFilterDialog());
            chipCongNghe.setOnCloseIconClickListener(v -> {
                selectedTechnologyId = null; selectedTechnologyName = null;
                updateChipUI(chipCongNghe, "Công nghệ", false);
                reloadData();
            });
        }
        if (chipTrangThai != null) {
            chipTrangThai.setOnClickListener(v -> showStatusFilterDialog());
            chipTrangThai.setOnCloseIconClickListener(v -> {
                selectedStatus = null;
                updateChipUI(chipTrangThai, "Trạng thái", false);
                reloadData();
            });
        }
    }

    private void updateChipUI(Chip chip, String baseText, boolean active) {
        if (chip == null || getContext() == null) return;
        String displayText = baseText;
        if (active) {
            String selectedText = "";
            if (chip.getId() == R.id.chip_linh_vuc && selectedCategoryName != null) {
                selectedText = ": " + selectedCategoryName;
            } else if (chip.getId() == R.id.chip_cong_nghe && selectedTechnologyName != null) {
                selectedText = ": " + selectedTechnologyName;
            } else if (chip.getId() == R.id.chip_trang_thai && selectedStatus != null) {
                selectedText = ": " + selectedStatus;
            }
            displayText += selectedText;
            chip.setChipBackgroundColorResource(R.color.colorPrimaryLight);
            chip.setCloseIconVisible(true);
            chip.setTextColor(ContextCompat.getColor(getContext(), android.R.color.white));
            chip.setChipStrokeColorResource(R.color.colorPrimary);
        } else {
            chip.setChipBackgroundColorResource(android.R.color.white);
            chip.setCloseIconVisible(false);
            chip.setTextColor(ContextCompat.getColor(getContext(), android.R.color.black));
            chip.setChipStrokeColorResource(R.color.black);
        }
        chip.setText(displayText);
    }

    private void showSortMenu(View v) {
        if (getContext() == null) return;
        PopupMenu popup = new PopupMenu(getContext(), v);
        popup.getMenuInflater().inflate(R.menu.sort_options_menu, popup.getMenu());
        popup.setOnMenuItemClickListener(item -> {
            int itemId = item.getItemId();
            if (itemId == R.id.sort_by_name_asc) {
                currentSortField = "Title"; currentSortDirection = Query.Direction.ASCENDING;
            } else if (itemId == R.id.sort_by_name_desc) {
                currentSortField = "Title"; currentSortDirection = Query.Direction.DESCENDING;
            } else if (itemId == R.id.sort_by_date_desc) {
                currentSortField = "CreatedAt"; currentSortDirection = Query.Direction.DESCENDING;
            } else if (itemId == R.id.sort_by_date_asc) {
                currentSortField = "CreatedAt"; currentSortDirection = Query.Direction.ASCENDING;
            } else if (itemId == R.id.sort_by_votes_desc) {
                currentSortField = "VoteCount"; currentSortDirection = Query.Direction.DESCENDING;
            }
            reloadData();
            return true;
        });
        popup.show();
    }

    private void showFilterDialog(String title, List<? extends FilterableItem> items, String selectedId, OnFilterSelectedListener listener, OnFilterClearedListener clearListener) {
        if (getContext() == null || items.isEmpty()) {
            Toast.makeText(getContext(), title + " không có dữ liệu.", Toast.LENGTH_SHORT).show();
            return;
        }
        String[] itemNames = items.stream().map(FilterableItem::getName).toArray(String[]::new);
        int currentSelection = -1;
        if (selectedId != null) {
            for (int i = 0; i < items.size(); i++) {
                if (items.get(i).getId().equals(selectedId)) {
                    currentSelection = i;
                    break;
                }
            }
        }
        new AlertDialog.Builder(getContext())
                .setTitle(title)
                .setSingleChoiceItems(itemNames, currentSelection, (dialog, which) -> {
                    listener.onSelected(items.get(which).getId(), items.get(which).getName());
                    dialog.dismiss();
                })
                .setNegativeButton("Bỏ qua", (dialog, which) -> dialog.dismiss())
                .setNeutralButton("Xóa lọc", (dialog, which) -> {
                    clearListener.onCleared();
                    dialog.dismiss();
                })
                .show();
    }

    private void showCategoryFilterDialog() {
        showFilterDialog("Chọn Lĩnh vực/Chủ đề", categoryListForDialog, selectedCategoryId,
                (id, name) -> {
                    selectedCategoryId = id; selectedCategoryName = name;
                    updateChipUI(chipLinhVuc, "Lĩnh vực/ Chủ đề", true);
                    reloadData();
                },
                () -> {
                    selectedCategoryId = null; selectedCategoryName = null;
                    updateChipUI(chipLinhVuc, "Lĩnh vực/ Chủ đề", false);
                    reloadData();
                }
        );
    }

    private void showTechnologyFilterDialog() {
        showFilterDialog("Chọn Công nghệ", technologyListForDialog, selectedTechnologyId,
                (id, name) -> {
                    selectedTechnologyId = id; selectedTechnologyName = name;
                    updateChipUI(chipCongNghe, "Công nghệ", true);
                    reloadData();
                },
                () -> {
                    selectedTechnologyId = null; selectedTechnologyName = null;
                    updateChipUI(chipCongNghe, "Công nghệ", false);
                    reloadData();
                }
        );
    }

    private void showStatusFilterDialog() {
        if (getContext() == null || statusArrayForDialog == null || statusArrayForDialog.length == 0) return;
        int currentSelection = -1;
        if (selectedStatus != null) {
            for (int i = 0; i < statusArrayForDialog.length; i++) {
                if (statusArrayForDialog[i].equals(selectedStatus)) {
                    currentSelection = i;
                    break;
                }
            }
        }
        new AlertDialog.Builder(getContext())
                .setTitle("Chọn Trạng thái")
                .setSingleChoiceItems(statusArrayForDialog, currentSelection, (dialog, which) -> {
                    selectedStatus = statusArrayForDialog[which];
                    updateChipUI(chipTrangThai, "Trạng thái", true);
                    reloadData();
                    dialog.dismiss();
                })
                .setNegativeButton("Bỏ qua", (dialog, which) -> dialog.dismiss())
                .setNeutralButton("Xóa lọc", (dialog, which) -> {
                    selectedStatus = null;
                    updateChipUI(chipTrangThai, "Trạng thái", false);
                    reloadData();
                    dialog.dismiss();
                })
                .show();
    }

    private void reloadData() {
        isLastPage = false;
        lastVisibleDocument = null;
        if (projectAdapter != null) projectAdapter.clearProjects();
        updateChipUI(chipLinhVuc, "Lĩnh vực/ Chủ đề", selectedCategoryId != null);
        updateChipUI(chipCongNghe, "Công nghệ", selectedTechnologyId != null);
        updateChipUI(chipTrangThai, "Trạng thái", selectedStatus != null);
        loadInitialProjects();
    }

    private void loadInitialProjects() {
        if (isLoading) return;
        isLoading = true;
        if (progressBarMain != null) progressBarMain.setVisibility(View.VISIBLE);
        isLastPage = false;
        lastVisibleDocument = null;
        fetchProjects(true);
    }

    private void loadMoreProjects() {
        if (isLoading || isLastPage || lastVisibleDocument == null) return;
        isLoading = true;
        if (progressBarLoadMore != null) progressBarLoadMore.setVisibility(View.VISIBLE);
        fetchProjects(false);
    }

    private Task<List<String>> getProjectIdsByJoinTable(String filterId, CollectionReference joinTableRef, String idFieldInJoinTable) {
        if (filterId == null || joinTableRef == null) {
            return Tasks.forResult(null);
        }
        return joinTableRef.whereEqualTo(idFieldInJoinTable, filterId)
                .get()
                .continueWith(task -> {
                    List<String> projectIds = new ArrayList<>();
                    if (task.isSuccessful() && task.getResult() != null) {
                        for (QueryDocumentSnapshot doc : task.getResult()) {
                            projectIds.add(doc.getString("ProjectId"));
                        }
                    } else {
                        Log.w(TAG, "Error fetching project IDs from join table for " + idFieldInJoinTable, task.getException());
                    }
                    if (projectIds.isEmpty()){
                        projectIds.add("NO_PROJECTS_MATCH_THIS_FILTER");
                    }
                    return projectIds;
                });
    }

    private void fetchProjects(boolean isInitialLoad) {
        Task<List<String>> categoryProjectIdsTask = getProjectIdsByJoinTable(selectedCategoryId, projectCategoriesRef, "CategoryId");
        Task<List<String>> technologyProjectIdsTask = getProjectIdsByJoinTable(selectedTechnologyId, projectTechRef, "TechnologyId");

        Tasks.whenAllSuccess(categoryProjectIdsTask, technologyProjectIdsTask).onSuccessTask(results -> {
            List<String> idsFromCategory = (List<String>) results.get(0);
            List<String> idsFromTechnology = (List<String>) results.get(1);
            List<String> finalFilteredProjectIds = null;

            if (idsFromCategory != null && idsFromTechnology != null) {
                if (idsFromCategory.contains("NO_PROJECTS_MATCH_THIS_FILTER") || idsFromTechnology.contains("NO_PROJECTS_MATCH_THIS_FILTER")){
                    finalFilteredProjectIds = Collections.singletonList("NO_PROJECTS_MATCH_THIS_FILTER");
                } else {
                    finalFilteredProjectIds = new ArrayList<>(idsFromCategory);
                    finalFilteredProjectIds.retainAll(idsFromTechnology);
                    if (finalFilteredProjectIds.isEmpty()) {
                        finalFilteredProjectIds.add("NO_PROJECTS_MATCH_THIS_FILTER");
                    }
                }
            } else if (idsFromCategory != null) {
                finalFilteredProjectIds = idsFromCategory;
            } else if (idsFromTechnology != null) {
                finalFilteredProjectIds = idsFromTechnology;
            }

            Query query = projectsRef.whereEqualTo("IsApproved", true);

            // Thêm sắp xếp theo IsFeatured LÀM ƯU TIÊN HÀNG ĐẦU
            // Sau đó mới đến các sắp xếp khác
            if (!currentSearchQuery.isEmpty()) {
                // Khi tìm kiếm, IsFeatured vẫn là ưu tiên, sau đó đến Title
                query = query.orderBy("IsFeatured", Query.Direction.DESCENDING)
                        .orderBy("Title") // Sắp xếp theo Title để whereGreaterThanOrEqualTo hoạt động
                        .whereGreaterThanOrEqualTo("Title", currentSearchQuery)
                        .whereLessThanOrEqualTo("Title", currentSearchQuery + "\uf8ff");
            } else {
                // Khi không tìm kiếm, IsFeatured là ưu tiên, sau đó đến lựa chọn sắp xếp của người dùng
                query = query.orderBy("IsFeatured", Query.Direction.DESCENDING)
                        .orderBy(currentSortField, currentSortDirection);
            }

            if (selectedStatus != null && !selectedStatus.isEmpty()) {
                query = query.whereEqualTo("Status", selectedStatus);
            }

            if (finalFilteredProjectIds != null && !finalFilteredProjectIds.isEmpty()) {
                if (finalFilteredProjectIds.contains("NO_PROJECTS_MATCH_THIS_FILTER")) {
                    if (isInitialLoad && projectAdapter != null) projectAdapter.clearProjects();
                    isLastPage = true;
                    hideProgress(isInitialLoad);
                    isLoading = false;
                    if(getContext() != null && isInitialLoad) Toast.makeText(getContext(), "Không có dự án nào khớp với bộ lọc.", Toast.LENGTH_SHORT).show();
                    return Tasks.forResult(null);
                }
                if (finalFilteredProjectIds.size() > 30) { // Firestore `whereIn` limit
                    Log.w(TAG, "Too many project IDs for whereIn clause: " + finalFilteredProjectIds.size() + ". Truncating to 30.");
                    query = query.whereIn(FieldPath.documentId(), finalFilteredProjectIds.subList(0, 30));
                } else {
                    query = query.whereIn(FieldPath.documentId(), finalFilteredProjectIds);
                }
            }

            if (!isInitialLoad && lastVisibleDocument != null) {
                query = query.startAfter(lastVisibleDocument);
            }
            query = query.limit(PROJECTS_PER_PAGE);
            return query.get();

        }).addOnCompleteListener(task -> {
            if (!isAdded() || getContext() == null) {
                Log.w(TAG, "Fragment not attached or context is null in fetchProjects final callback.");
                hideProgress(isInitialLoad); isLoading = false; return;
            }
            hideProgress(isInitialLoad); isLoading = false;

            if (task.isSuccessful() && task.getResult() != null) {
                QuerySnapshot querySnapshot = task.getResult();
                if (!querySnapshot.isEmpty()) {
                    List<Project> fetchedProjects = new ArrayList<>();
                    List<Task<Void>> tasksToCompleteDetails = new ArrayList<>();

                    for (QueryDocumentSnapshot document : querySnapshot) {
                        Project project = document.toObject(Project.class);
                        project.setProjectId(document.getId());
                        fetchedProjects.add(project);

                        if (usersRef != null && project.getCreatorUserId() != null && !project.getCreatorUserId().isEmpty()) {
                            Task<Void> userTask = usersRef.document(project.getCreatorUserId()).get()
                                    .continueWith(userDocTask -> {
                                        if (userDocTask.isSuccessful() && userDocTask.getResult() != null && userDocTask.getResult().exists()) {
                                            User user = userDocTask.getResult().toObject(User.class);
                                            if (user != null) project.setCreatorFullName(user.getFullName());
                                        } else project.setCreatorFullName(null);
                                        return null;
                                    });
                            tasksToCompleteDetails.add(userTask);
                        } else project.setCreatorFullName(null);

                        if (projectTechRef != null && techRef != null) {
                            Task<Void> techTask = projectTechRef.whereEqualTo("ProjectId", project.getProjectId()).get()
                                    .continueWithTask(ptQueryTask -> {
                                        if (ptQueryTask.isSuccessful() && ptQueryTask.getResult() != null) {
                                            List<Task<DocumentSnapshot>> techNameTasks = new ArrayList<>();
                                            for (QueryDocumentSnapshot ptDoc : ptQueryTask.getResult()) {
                                                String techId = ptDoc.getString("TechnologyId");
                                                if (techId != null) techNameTasks.add(techRef.document(techId).get());
                                            }
                                            return Tasks.whenAllSuccess(techNameTasks);
                                        } return Tasks.forResult(new ArrayList<>());
                                    }).continueWith(techNameResultsTask -> {
                                        if (techNameResultsTask.isSuccessful() && techNameResultsTask.getResult() != null) {
                                            List<String> techNames = ((List<?>) techNameResultsTask.getResult()).stream()
                                                    .filter(DocumentSnapshot.class::isInstance)
                                                    .map(obj -> (DocumentSnapshot) obj)
                                                    .filter(DocumentSnapshot::exists)
                                                    .map(techDoc -> techDoc.getString("Name"))
                                                    .filter(name -> name != null && !name.isEmpty())
                                                    .collect(Collectors.toList());
                                            project.setTechnologyNames(techNames);
                                        } else project.setTechnologyNames(new ArrayList<>());
                                        return null;
                                    });
                            tasksToCompleteDetails.add(techTask);
                        } else project.setTechnologyNames(new ArrayList<>());

                        if (projectCategoriesRef != null && categoriesRef != null) {
                            Task<Void> categoryTask = projectCategoriesRef.whereEqualTo("ProjectId", project.getProjectId()).get()
                                    .continueWithTask(pcQueryTask -> {
                                        if (pcQueryTask.isSuccessful() && pcQueryTask.getResult() != null) {
                                            List<Task<DocumentSnapshot>> catNameTasks = new ArrayList<>();
                                            for (QueryDocumentSnapshot pcDoc : pcQueryTask.getResult()) {
                                                String catId = pcDoc.getString("CategoryId");
                                                if (catId != null) catNameTasks.add(categoriesRef.document(catId).get());
                                            }
                                            return Tasks.whenAllSuccess(catNameTasks);
                                        } return Tasks.forResult(new ArrayList<>());
                                    }).continueWith(catNameResultsTask -> {
                                        if (catNameResultsTask.isSuccessful() && catNameResultsTask.getResult() != null) {
                                            List<String> catNames = ((List<?>) catNameResultsTask.getResult()).stream()
                                                    .filter(DocumentSnapshot.class::isInstance)
                                                    .map(obj -> (DocumentSnapshot) obj)
                                                    .filter(DocumentSnapshot::exists)
                                                    .map(catDoc -> catDoc.getString("Name"))
                                                    .filter(name -> name != null && !name.isEmpty())
                                                    .collect(Collectors.toList());
                                            project.setCategoryNames(catNames);
                                        } else project.setCategoryNames(new ArrayList<>());
                                        return null;
                                    });
                            tasksToCompleteDetails.add(categoryTask);
                        } else project.setCategoryNames(new ArrayList<>());
                    }

                    Tasks.whenAll(tasksToCompleteDetails).addOnCompleteListener(allExtraTasks -> {
                        if (!isAdded() || projectAdapter == null) return;
                        if (isInitialLoad) projectAdapter.updateProjects(fetchedProjects);
                        else projectAdapter.addProjects(fetchedProjects);
                        if (querySnapshot.size() < PROJECTS_PER_PAGE) isLastPage = true;
                        if (!querySnapshot.isEmpty()) lastVisibleDocument = querySnapshot.getDocuments().get(querySnapshot.size() - 1);
                    });

                } else {
                    if (isInitialLoad && projectAdapter != null) {
                        projectAdapter.clearProjects();
                        if (getContext() != null) Toast.makeText(getContext(), "Không có dự án nào.", Toast.LENGTH_SHORT).show();
                    }
                    isLastPage = true;
                }
            } else {
                Log.e(TAG, "Error getting projects: ", task.getException());
                if (getContext() != null) Toast.makeText(getContext(), "Lỗi tải dự án: " + (task.getException() != null ? task.getException().getMessage() : "Unknown error"), Toast.LENGTH_LONG).show();
            }
        }).addOnFailureListener(e -> {
            Log.e(TAG, "Error in pre-fetching IDs for filters: ", e);
            hideProgress(isInitialLoad); isLoading = false;
            if (getContext() != null) Toast.makeText(getContext(), "Lỗi khi áp dụng bộ lọc.", Toast.LENGTH_LONG).show();
        });
    }

    private void hideProgress(boolean isInitialLoad) {
        if (isInitialLoad) {
            if (progressBarMain != null) progressBarMain.setVisibility(View.GONE);
            if (swipeRefreshLayout != null && swipeRefreshLayout.isRefreshing()) swipeRefreshLayout.setRefreshing(false);
        } else {
            if (progressBarLoadMore != null) progressBarLoadMore.setVisibility(View.GONE);
        }
    }

    @Override
    public void onProjectClick(Project project) {
        if (getActivity() != null && project != null && project.getProjectId() != null) {
            Intent intent = new Intent(getActivity(), ProjectDetailActivity.class);
            intent.putExtra(ProjectDetailActivity.EXTRA_PROJECT_ID, project.getProjectId());
            startActivity(intent);
        } else {
            Log.e(TAG, "Cannot start ProjectDetailActivity. Activity, project, or project ID is null.");
            if (getContext() != null) Toast.makeText(getContext(), "Không thể mở chi tiết dự án.", Toast.LENGTH_SHORT).show();
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

    private interface FilterableItem {
        String getId();
        String getName();
    }
    private interface OnFilterSelectedListener {
        void onSelected(String id, String name);
    }
    private interface OnFilterClearedListener {
        void onCleared();
    }

    private static class CategoryItem implements FilterableItem {
        String id; String name;
        public CategoryItem(String id, String name) { this.id = id; this.name = name; }
        @Override public String getId() { return id; }
        @Override public String getName() { return name; }
        @NonNull @Override public String toString() { return name; }
    }

    private static class TechnologyItem implements FilterableItem {
        String id; String name;
        public TechnologyItem(String id, String name) { this.id = id; this.name = name; }
        @Override public String getId() { return id; }
        @Override public String getName() { return name; }
        @NonNull @Override public String toString() { return name; }
    }
}