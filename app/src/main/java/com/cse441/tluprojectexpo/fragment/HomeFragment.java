package com.cse441.tluprojectexpo.fragment;

import android.content.Context;
import android.os.Bundle;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout; // Import SwipeRefreshLayout

import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
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
    private static final int SCROLL_THRESHOLD = 15; // Ngưỡng pixel để phát hiện cuộn (điều chỉnh nếu cần)

    private RecyclerView recyclerViewProjects;
    private ProjectAdapter projectAdapter;
    private List<Project> projectDataList;
    private FirebaseFirestore db;
    private ProgressBar progressBarMain; // ProgressBar chính khi tải lần đầu
    private ProgressBar progressBarLoadMore; // ProgressBar ở cuối danh sách khi tải thêm
    private EditText searchEditText;
    private SwipeRefreshLayout swipeRefreshLayout; // Thêm biến cho SwipeRefreshLayout

    private boolean isLoading = false; // Cờ báo đang tải dữ liệu
    private boolean isLastPage = false; // Cờ báo đã tải hết dữ liệu
    private DocumentSnapshot lastVisibleDocument = null; // Document cuối cùng đã thấy
    private LinearLayoutManager linearLayoutManager;

    // Interface for communication with Activity
    public interface OnScrollInteractionListener {
        void onScrollUp();
        void onScrollDown();
    }
    private OnScrollInteractionListener mListener;


    public HomeFragment() {
        // Required empty public constructor
    }

    public static HomeFragment newInstance() {
        return new HomeFragment();
    }

    @Override
    public void onAttach(@NonNull Context context) {
        super.onAttach(context);
        if (context instanceof OnScrollInteractionListener) {
            mListener = (OnScrollInteractionListener) context;
        } else {
            throw new RuntimeException(context.toString()
                    + " must implement OnScrollInteractionListener");
        }
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
        progressBarMain = view.findViewById(R.id.progressBarMain); // ID của ProgressBar chính
        progressBarLoadMore = view.findViewById(R.id.progressBarLoadMore); // ID của ProgressBar load more
        searchEditText = view.findViewById(R.id.searchEditText);
        swipeRefreshLayout = view.findViewById(R.id.swipeRefreshLayout); // Khởi tạo SwipeRefreshLayout

        linearLayoutManager = new LinearLayoutManager(getContext());
        recyclerViewProjects.setLayoutManager(linearLayoutManager);
        projectAdapter = new ProjectAdapter(getContext(), projectDataList); // Truyền context nếu Adapter cần
        recyclerViewProjects.setAdapter(projectAdapter);

        // Thêm Listener để phát hiện cuộn
        setupScrollListener();

        // Thiết lập EditText tìm kiếm
        setupSearchEditText();

        // Thiết lập SwipeRefreshLayout
        setupSwipeRefreshLayout(); // Gọi phương thức mới

        // Tải dữ liệu ban đầu
        // Không gọi loadInitialProjects() ngay nếu muốn SwipeRefreshLayout tự kích hoạt lần đầu (tùy chọn)
        // Hoặc có thể gọi để tải dữ liệu khi fragment được tạo
        loadInitialProjects();
    }

    private void setupScrollListener() {
        recyclerViewProjects.addOnScrollListener(new RecyclerView.OnScrollListener() {
            @Override
            public void onScrolled(@NonNull RecyclerView recyclerView, int dx, int dy) {
                super.onScrolled(recyclerView, dx, dy);

                // For BottomNavigationView
                if (mListener != null) {
                    if (dy > SCROLL_THRESHOLD) { // Scrolling up
                        mListener.onScrollUp();
                    } else if (dy < -SCROLL_THRESHOLD) { // Scrolling down
                        mListener.onScrollDown();
                    }
                }


                // For pagination
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

    private void setupSwipeRefreshLayout() {
        if (swipeRefreshLayout == null) {
            return;
        }
        // swipeRefreshLayout.setColorSchemeResources(android.R.color.holo_blue_bright,
        //         android.R.color.holo_green_light,
        //         android.R.color.holo_orange_light,
        //         android.R.color.holo_red_light);

        swipeRefreshLayout.setOnRefreshListener(new SwipeRefreshLayout.OnRefreshListener() {
            @Override
            public void onRefresh() {
                // Hành động khi người dùng kéo để làm mới
                Log.d(TAG, "Pull to refresh triggered.");
                // Tùy chọn: Xóa ô tìm kiếm và ẩn bàn phím nếu đang tìm kiếm
                // searchEditText.setText("");
                // searchEditText.clearFocus();
                // InputMethodManager imm = (InputMethodManager) getActivity().getSystemService(Context.INPUT_METHOD_SERVICE);
                // if (imm != null && getActivity().getCurrentFocus() != null) {
                //    imm.hideSoftInputFromWindow(getActivity().getCurrentFocus().getWindowToken(), 0);
                // }
                loadInitialProjects(); // Tải lại dữ liệu từ đầu
            }
        });
    }


    private void loadInitialProjects() {
        // isLoading = true; // Sẽ được đặt trong fetchProjects
        isLastPage = false;
        lastVisibleDocument = null; // Reset cho lần tải đầu
        // projectDataList.clear();    // Sẽ được clear trong fetchProjects nếu là isInitialLoad
        // projectAdapter.notifyDataSetChanged(); // Sẽ được xử lý trong fetchProjects

        // Chỉ hiển thị progressBarMain nếu SwipeRefreshLayout không đang làm mới
        // và đây không phải là một tìm kiếm đang diễn ra (vì tìm kiếm cũng dùng progressBarMain)
        if (swipeRefreshLayout != null && !swipeRefreshLayout.isRefreshing() && progressBarMain != null) {
            // Kiểm tra thêm để đảm bảo không phải đang trong quá trình tìm kiếm đã hiển thị progressBarMain
            // Điều này hơi phức tạp, cách đơn giản là để fetchProjects quản lý progressBarMain
            // progressBarMain.setVisibility(View.VISIBLE);
        }
        if (progressBarLoadMore != null) progressBarLoadMore.setVisibility(View.GONE);


        Query firstQuery = db.collection("projects")
                .orderBy("name", Query.Direction.ASCENDING) // SẮP XẾP THEO MỘT TRƯỜNG CỤ THỂ
                .limit(ITEMS_PER_PAGE);

        // Hiển thị ProgressBar chính nếu không phải là hành động làm mới từ SwipeRefreshLayout
        // (vì SwipeRefreshLayout đã có animation riêng)
        // Tuy nhiên, việc này được xử lý tốt hơn trong fetchProjects
        fetchProjects(firstQuery, true, false); // isInitialLoad = true, isSearchExecution = false
    }

    private void loadMoreProjects() {
        if (isLoading || isLastPage || lastVisibleDocument == null) {
            return; // Không tải nếu đang tải, đã hết trang, hoặc chưa có document cuối
        }
        // isLoading = true; // Sẽ được đặt trong fetchProjects
        // Hiển thị ProgressBar ở cuối danh sách (nếu có)
        if (progressBarLoadMore != null) progressBarLoadMore.setVisibility(View.VISIBLE);
        // Hoặc dùng lại progressBarMain và đặt vị trí phù hợp, hoặc thêm item loading vào adapter

        Log.d(TAG, "Loading more after document: " + lastVisibleDocument.getId());

        Query nextQuery = db.collection("projects")
                .orderBy("name", Query.Direction.ASCENDING) // PHẢI GIỐNG orderBy của query đầu
                .startAfter(lastVisibleDocument) // Bắt đầu từ sau document cuối cùng
                .limit(ITEMS_PER_PAGE);

        fetchProjects(nextQuery, false, false); // isInitialLoad = false, isSearchExecution = false
    }

    private void fetchProjects(Query query, final boolean isInitialLoad, final boolean isSearchExecution) {
        isLoading = true; // Đặt isLoading = true ở đầu mỗi lần fetch

        // Hiển thị progressBarMain nếu là tải lần đầu (cho tìm kiếm hoặc tải bình thường)
        // và SwipeRefreshLayout không đang hoạt động (để tránh 2 loading indicator)
        if (isInitialLoad && progressBarMain != null) {
            if (swipeRefreshLayout != null && !swipeRefreshLayout.isRefreshing()) {
                progressBarMain.setVisibility(View.VISIBLE);
            } else if (swipeRefreshLayout == null) { // Trường hợp không có SwipeRefreshLayout
                progressBarMain.setVisibility(View.VISIBLE);
            }
        }


        // Nếu là tải lần đầu (cho dù là tìm kiếm hay tải bình thường), xóa dữ liệu cũ.
        if (isInitialLoad) {
            projectDataList.clear();
            projectAdapter.notifyDataSetChanged();
            lastVisibleDocument = null; // Reset lại document cuối cho một lượt tải/tìm kiếm mới
            isLastPage = false; // Reset cờ trang cuối
        }

        query.get().addOnCompleteListener(new OnCompleteListener<QuerySnapshot>() {
            @Override
            public void onComplete(@NonNull Task<QuerySnapshot> task) {
                isLoading = false; // Đặt lại cờ isLoading

                // Ẩn progressBarMain nếu nó đang hiển thị
                if (progressBarMain != null) {
                    progressBarMain.setVisibility(View.GONE);
                }
                // Ẩn progressBarLoadMore nếu nó đang hiển thị
                if (progressBarLoadMore != null) {
                    progressBarLoadMore.setVisibility(View.GONE);
                }
                // Dừng animation của SwipeRefreshLayout nếu nó đang chạy
                if (swipeRefreshLayout != null) {
                    swipeRefreshLayout.setRefreshing(false);
                }


                if (task.isSuccessful()) {
                    QuerySnapshot querySnapshot = task.getResult();
                    if (querySnapshot != null && !querySnapshot.isEmpty()) {
                        List<Project> newProjects = new ArrayList<>();
                        for (QueryDocumentSnapshot document : querySnapshot) {
                            Project project = document.toObject(Project.class);
                            project.setId(document.getId());
                            newProjects.add(project);
                        }

                        int currentSize = projectDataList.size(); // Kích thước trước khi thêm
                        projectDataList.addAll(newProjects);
                        projectAdapter.notifyItemRangeInserted(currentSize, newProjects.size());

                        // Lưu lại document cuối cùng của trang hiện tại
                        if (querySnapshot.getDocuments().size() > 0) {
                            lastVisibleDocument = querySnapshot.getDocuments().get(querySnapshot.size() - 1);
                        }

                        // Kiểm tra xem có phải trang cuối không
                        if (newProjects.size() < ITEMS_PER_PAGE) {
                            isLastPage = true;
                            // Log.d(TAG, "Reached the last page of projects.");
                        }
                    } else {
                        // Không có document mới nào được trả về -> đã hết dữ liệu
                        isLastPage = true;
                        // Log.d(TAG, "No more projects to load.");
                        if (isInitialLoad) { // Nếu là lần tải đầu mà không có gì
                            if (isSearchExecution) {
                                Toast.makeText(getContext(), "Không tìm thấy kết quả nào.", Toast.LENGTH_SHORT).show();
                            } else {
                                // Toast.makeText(getContext(), "Không có dự án nào.", Toast.LENGTH_SHORT).show();
                            }
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
        // searchEditText.clearFocus(); // Không cần thiết nếu XML đã đúng

        // Thêm sự kiện click để hiện bàn phím khi người dùng chạm vào
        searchEditText.setOnClickListener(v -> {
            // searchEditText.setFocusable(true); // Đã được đặt trong XML
            // searchEditText.setFocusableInTouchMode(true); // Đã được đặt trong XML
            searchEditText.requestFocus();
            InputMethodManager imm = (InputMethodManager) getActivity().getSystemService(Context.INPUT_METHOD_SERVICE);
            if (imm != null) {
                imm.showSoftInput(searchEditText, InputMethodManager.SHOW_IMPLICIT);
            }
        });

        // Thêm sự kiện khi người dùng nhập text và nhấn nút search trên bàn phím
        searchEditText.setOnEditorActionListener((v, actionId, event) -> {
            if (actionId == EditorInfo.IME_ACTION_SEARCH) {
                String searchText = searchEditText.getText().toString().trim();
                // Xử lý tìm kiếm ở đây

                // Ẩn bàn phím trước
                InputMethodManager imm = (InputMethodManager) getActivity().getSystemService(Context.INPUT_METHOD_SERVICE);
                if (imm != null && getActivity().getCurrentFocus() != null) {
                    imm.hideSoftInputFromWindow(getActivity().getCurrentFocus().getWindowToken(), 0);
                }
                searchEditText.clearFocus(); // Bỏ focus để AppBar có thể thu gọn

                if (!searchText.isEmpty()) {
                    // Thực hiện tìm kiếm
                    performSearch(searchText); // Gọi hàm tìm kiếm mới
                } else {
                    // Nếu người dùng xóa hết và nhấn search, có thể muốn tải lại danh sách gốc
                    loadInitialProjects();
                    Toast.makeText(getContext(), "Hiển thị tất cả dự án.", Toast.LENGTH_SHORT).show();
                }
                return true; // Đã xử lý sự kiện
            }
            return false; // Chưa xử lý
        });
    }

    // Đổi tên hàm searchProjects thành performSearch để tránh nhầm lẫn với hàm cũ
    private void performSearch(String searchText) {
        // isLoading = true; // Sẽ được đặt trong fetchProjects
        isLastPage = false; // Reset cờ trang cuối cho tìm kiếm mới
        // lastVisibleDocument = null; // Sẽ được reset trong fetchProjects
        // projectDataList.clear(); // Sẽ được clear trong fetchProjects
        // projectAdapter.notifyDataSetChanged(); // Sẽ được xử lý trong fetchProjects

        // Hiển thị progressBarMain được xử lý trong fetchProjects
        // if (progressBarMain != null) progressBarMain.setVisibility(View.VISIBLE);

        if (progressBarLoadMore != null) progressBarLoadMore.setVisibility(View.GONE); // Luôn ẩn khi bắt đầu tìm kiếm mới

        Log.d(TAG, "Thực hiện tìm kiếm với: " + searchText);
        // Tạo query tìm kiếm
        Query searchQuery = db.collection("projects")
                .orderBy("name") // Cần orderBy cho các truy vấn phạm vi
                .whereGreaterThanOrEqualTo("name", searchText)
                .whereLessThanOrEqualTo("name", searchText + '\uf8ff')
                .limit(ITEMS_PER_PAGE);

        // Thực hiện tìm kiếm bằng cách gọi fetchProjects
        fetchProjects(searchQuery, true, true); // isInitialLoad = true, isSearchExecution = true
    }


    @Override
    public void onDetach() {
        super.onDetach();
        mListener = null;
    }

    @Override
    public void onDestroyView() {
        // if (projectAdapter != null) {
        //     projectAdapter.shutdownExecutor(); // Nếu adapter có executor cần shutdown
        // }
        super.onDestroyView();
    }
}