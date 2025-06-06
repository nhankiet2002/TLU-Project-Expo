package com.cse441.tluprojectexpo.fragment;

import android.os.Bundle;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.Toast;

import com.cse441.tluprojectexpo.R;
import com.cse441.tluprojectexpo.adapter.ProjectAdapter;
import com.cse441.tluprojectexpo.model.Project;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.firestore.DocumentSnapshot; // Quan trọng cho startAfter
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.QuerySnapshot;

import java.util.ArrayList;
import java.util.List;

public class HomeFragment extends Fragment {

    private static final String TAG = "HomeFragment";
    private static final int ITEMS_PER_PAGE = 10; // Số lượng mục tải mỗi lần

    private RecyclerView recyclerViewProjects;
    private ProjectAdapter projectAdapter;
    private List<Project> projectDataList;
    private FirebaseFirestore db;
    private ProgressBar progressBarMain; // ProgressBar chính khi tải lần đầu
    private ProgressBar progressBarLoadMore; // ProgressBar ở cuối danh sách khi tải thêm
    private EditText searchEditText;

    private boolean isLoading = false; // Cờ báo đang tải dữ liệu
    private boolean isLastPage = false; // Cờ báo đã tải hết dữ liệu
    private DocumentSnapshot lastVisibleDocument = null; // Document cuối cùng đã thấy
    private LinearLayoutManager linearLayoutManager;


    public HomeFragment() {
        // Required empty public constructor
    }

    public static HomeFragment newInstance() {
        return new HomeFragment();
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        projectDataList = new ArrayList<>();
        db = FirebaseFirestore.getInstance();
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_home, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        recyclerViewProjects = view.findViewById(R.id.recyclerViewProjects);
        progressBarMain = view.findViewById(R.id.progressBar); // ID của ProgressBar chính
        searchEditText = view.findViewById(R.id.searchEditText);
        // Giả sử bạn có một ProgressBar khác ở cuối layout của Fragment hoặc trong item đặc biệt của RecyclerView
        // Nếu không, bạn có thể quản lý việc hiển thị/ẩn progressBarMain cho cả hai trường hợp
        // progressBarLoadMore = view.findViewById(R.id.progressBarLoadMore); // ID của ProgressBar load more

        linearLayoutManager = new LinearLayoutManager(getContext());
        recyclerViewProjects.setLayoutManager(linearLayoutManager);
        projectAdapter = new ProjectAdapter(getContext(), projectDataList); // Truyền context nếu Adapter cần
        recyclerViewProjects.setAdapter(projectAdapter);

        // Thêm Listener để phát hiện cuộn
        setupScrollListener();

        // Thiết lập EditText tìm kiếm
        setupSearchEditText();

        // Tải dữ liệu ban đầu
        loadInitialProjects();
    }

    private void setupScrollListener() {
        recyclerViewProjects.addOnScrollListener(new RecyclerView.OnScrollListener() {
            @Override
            public void onScrolled(@NonNull RecyclerView recyclerView, int dx, int dy) {
                super.onScrolled(recyclerView, dx, dy);

                int visibleItemCount = linearLayoutManager.getChildCount();
                int totalItemCount = linearLayoutManager.getItemCount();
                int firstVisibleItemPosition = linearLayoutManager.findFirstVisibleItemPosition();

                // Kiểm tra nếu không đang tải, chưa phải trang cuối, và đã cuộn gần đến cuối
                if (!isLoading && !isLastPage) {
                    if ((visibleItemCount + firstVisibleItemPosition) >= totalItemCount
                            && firstVisibleItemPosition >= 0
                            && totalItemCount >= ITEMS_PER_PAGE) { // Đảm bảo có ít nhất 1 trang đã tải
                        Log.d(TAG, "Reached end of list, loading more projects...");
                        loadMoreProjects();
                    }
                }
            }
        });
    }

    private void loadInitialProjects() {
        isLoading = true;
        isLastPage = false;
        lastVisibleDocument = null; // Reset cho lần tải đầu
        projectDataList.clear();    // Xóa dữ liệu cũ nếu có
        projectAdapter.notifyDataSetChanged(); // Cập nhật UI ngay

        if (progressBarMain != null) progressBarMain.setVisibility(View.VISIBLE);
        // if (progressBarLoadMore != null) progressBarLoadMore.setVisibility(View.GONE);

        Query firstQuery = db.collection("projects")
                .orderBy("name", Query.Direction.ASCENDING) // SẮP XẾP THEO MỘT TRƯỜNG CỤ THỂ
                .limit(ITEMS_PER_PAGE);

        fetchProjects(firstQuery, true);
    }

    private void loadMoreProjects() {
        if (isLoading || isLastPage || lastVisibleDocument == null) {
            return; // Không tải nếu đang tải, đã hết trang, hoặc chưa có document cuối
        }
        isLoading = true;
        // Hiển thị ProgressBar ở cuối danh sách (nếu có)
        // if (progressBarLoadMore != null) progressBarLoadMore.setVisibility(View.VISIBLE);
        // Hoặc dùng lại progressBarMain và đặt vị trí phù hợp, hoặc thêm item loading vào adapter

        Log.d(TAG, "Loading more after document: " + lastVisibleDocument.getId());

        Query nextQuery = db.collection("projects")
                .orderBy("name", Query.Direction.ASCENDING) // PHẢI GIỐNG orderBy của query đầu
                .startAfter(lastVisibleDocument) // Bắt đầu từ sau document cuối cùng
                .limit(ITEMS_PER_PAGE);

        fetchProjects(nextQuery, false);
    }

    private void fetchProjects(Query query, final boolean isInitialLoad) {
        query.get().addOnCompleteListener(new OnCompleteListener<QuerySnapshot>() {
            @Override
            public void onComplete(@NonNull Task<QuerySnapshot> task) {
                isLoading = false;
                if (isInitialLoad && progressBarMain != null) {
                    progressBarMain.setVisibility(View.GONE);
                }
                // if (!isInitialLoad && progressBarLoadMore != null) {
                //     progressBarLoadMore.setVisibility(View.GONE);
                // }

                if (task.isSuccessful()) {
                    QuerySnapshot querySnapshot = task.getResult();
                    if (querySnapshot != null && !querySnapshot.isEmpty()) {
                        List<Project> newProjects = new ArrayList<>();
                        for (QueryDocumentSnapshot document : querySnapshot) {
                            Project project = document.toObject(Project.class);
                            project.setId(document.getId());
                            newProjects.add(project);
                        }

                        projectDataList.addAll(newProjects);
                        projectAdapter.notifyItemRangeInserted(projectDataList.size() - newProjects.size(), newProjects.size());

                        // Lưu lại document cuối cùng của trang hiện tại
                        if (querySnapshot.getDocuments().size() > 0) {
                            lastVisibleDocument = querySnapshot.getDocuments().get(querySnapshot.size() - 1);
                        }

                        // Kiểm tra xem có phải trang cuối không
                        if (newProjects.size() < ITEMS_PER_PAGE) {
                            isLastPage = true;
                            Log.d(TAG, "Reached the last page of projects.");
                        }
                    } else {
                        // Không có document mới nào được trả về -> đã hết dữ liệu
                        isLastPage = true;
                        Log.d(TAG, "No more projects to load.");
                        if (isInitialLoad) { // Nếu là lần tải đầu mà không có gì
                            // Toast.makeText(getContext(), "Không có dự án nào.", Toast.LENGTH_SHORT).show();
                        }
                    }
                } else {
                    Log.w(TAG, "Error getting documents: ", task.getException());
                    if (getContext() != null && task.getException() != null) {
                        Toast.makeText(getContext(), "Lỗi tải dữ liệu: " + task.getException().getMessage(), Toast.LENGTH_LONG).show();
                    }
                }
            }
        });
    }

    private void setupSearchEditText() {
        // Ngăn EditText tự động nhận focus khi fragment được tạo
        searchEditText.clearFocus();
        
        // Thêm sự kiện click để hiện bàn phím khi người dùng chạm vào
        searchEditText.setOnClickListener(v -> {
            searchEditText.setFocusable(true);
            searchEditText.setFocusableInTouchMode(true);
            searchEditText.requestFocus();
        });

        // Thêm sự kiện khi người dùng nhập text
        searchEditText.setOnEditorActionListener((v, actionId, event) -> {
            // Xử lý tìm kiếm ở đây
            String searchText = searchEditText.getText().toString();
            if (!searchText.isEmpty()) {
                // Thực hiện tìm kiếm
                searchProjects(searchText);
            }
            return true;
        });
    }

    private void searchProjects(String searchText) {
        // Xóa dữ liệu cũ
        projectDataList.clear();
        projectAdapter.notifyDataSetChanged();
        
        // Tạo query tìm kiếm
        Query searchQuery = db.collection("projects")
                .whereGreaterThanOrEqualTo("name", searchText)
                .whereLessThanOrEqualTo("name", searchText + '\uf8ff')
                .limit(ITEMS_PER_PAGE);

        // Thực hiện tìm kiếm
        searchQuery.get().addOnCompleteListener(task -> {
            if (task.isSuccessful()) {
                QuerySnapshot querySnapshot = task.getResult();
                if (querySnapshot != null && !querySnapshot.isEmpty()) {
                    for (QueryDocumentSnapshot document : querySnapshot) {
                        Project project = document.toObject(Project.class);
                        project.setId(document.getId());
                        projectDataList.add(project);
                    }
                    projectAdapter.notifyDataSetChanged();
                } else {
                    Toast.makeText(getContext(), "Không tìm thấy kết quả", Toast.LENGTH_SHORT).show();
                }
            } else {
                Toast.makeText(getContext(), "Lỗi tìm kiếm: " + task.getException().getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    @Override
    public void onDestroyView() {
        // if (projectAdapter != null) {
        //     projectAdapter.shutdownExecutor(); // Nếu adapter có executor cần shutdown
        // }
        super.onDestroyView();
    }
}