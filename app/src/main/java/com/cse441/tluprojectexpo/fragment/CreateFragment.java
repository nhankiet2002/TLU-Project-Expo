package com.cse441.tluprojectexpo.fragment;

import android.os.Bundle;
import android.util.Log; // Thư viện để ghi log, giúp theo dõi và debug
import android.view.LayoutInflater; // Dùng để "thổi phồng" (inflate) file layout XML thành đối tượng View
import android.view.View; // Lớp cơ sở cho các thành phần giao diện người dùng
import android.view.ViewGroup; // Một View đặc biệt có thể chứa các View khác (con)
import android.widget.ArrayAdapter; // Adapter để kết nối dữ liệu (ví dụ: List) với các View hiển thị danh sách (như AutoCompleteTextView)
import android.widget.AutoCompleteTextView; // View cho phép người dùng nhập văn bản và hiển thị gợi ý dựa trên một danh sách
import android.widget.Toast; // Dùng để hiển thị một thông báo ngắn trên màn hình

import androidx.annotation.NonNull; // Annotation chỉ ra rằng một tham số, trường hoặc giá trị trả về không bao giờ là null
import androidx.annotation.Nullable; // Annotation chỉ ra rằng một tham số, trường hoặc giá trị trả về có thể là null
import androidx.fragment.app.Fragment; // Một phần giao diện người dùng có thể tái sử dụng, có vòng đời riêng

import com.cse441.tluprojectexpo.R;
import com.google.firebase.firestore.FirebaseFirestore; // Lớp chính để tương tác với Cloud Firestore
import com.google.firebase.firestore.QueryDocumentSnapshot; // Đại diện cho một document được trả về từ một query Firestore
import com.google.firebase.firestore.QuerySnapshot; // Đại diện cho kết quả của một query Firestore (có thể chứa nhiều document)
import com.google.android.material.textfield.TextInputLayout; // Layout bao bọc TextInputEditText hoặc AutoCompleteTextView, cung cấp các tính năng như hint nổi, bộ đếm ký tự, hiển thị lỗi

import java.util.ArrayList; // Lớp triển khai List có thể thay đổi kích thước
import java.util.List; // Interface đại diện cho một tập hợp các phần tử có thứ tự

public class CreateFragment extends Fragment {

    // TAG dùng để lọc log trong Logcat, giúp dễ dàng tìm kiếm log của Fragment này
    private static final String TAG = "CreateFragment";

    // Khai báo các View sẽ được sử dụng trong layout
    private AutoCompleteTextView actvCategory; // Dành cho Lĩnh vực/Chủ đề
    private AutoCompleteTextView actvTechnology; // Dành cho Công nghệ
    private AutoCompleteTextView actvStatus;     // Dành cho Trạng thái

    // Khai báo các ArrayAdapter tương ứng cho mỗi AutoCompleteTextView
    // Adapter chịu trách nhiệm cung cấp dữ liệu và cách hiển thị mỗi mục trong dropdown
    private ArrayAdapter<String> categoryAdapter;
    private ArrayAdapter<String> technologyAdapter;
    private ArrayAdapter<String> statusAdapter;

    // Khai báo các List để lưu trữ dữ liệu lấy từ Firestore
    private List<String> categoryList = new ArrayList<>();
    private List<String> technologyList = new ArrayList<>();
    private List<String> statusList = new ArrayList<>();

    // Khai báo đối tượng FirebaseFirestore để tương tác với cơ sở dữ liệu
    private FirebaseFirestore db;

    // Constructor mặc định, bắt buộc phải có cho Fragment
    public CreateFragment() {
        // Required empty public constructor
    }

    // Phương thức này được gọi khi Fragment được tạo lần đầu tiên
    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // Khởi tạo đối tượng FirebaseFirestore
        // getInstance() trả về một instance của Firestore, sẵn sàng để sử dụng
        db = FirebaseFirestore.getInstance();
    }

    // Phương thức này được gọi để tạo và trả về View của Fragment
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // "Thổi phồng" file layout XML (ví dụ: R.layout.fragment_create) thành một đối tượng View
        // container là ViewGroup cha mà View này sẽ được gắn vào
        // false nghĩa là không tự động gắn View vào container ngay (Fragment sẽ tự làm điều đó)
        return inflater.inflate(R.layout.fragment_create, container, false);
    }

    // Phương thức này được gọi ngay sau khi onCreateView() hoàn thành
    // và View của Fragment đã được tạo. Đây là nơi thích hợp để thực hiện các thao tác với View.
    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        // Ánh xạ các biến View đã khai báo với các View tương ứng trong file layout XML bằng ID của chúng
        actvCategory = view.findViewById(R.id.actv_category);
        actvTechnology = view.findViewById(R.id.actv_technology);
        actvStatus = view.findViewById(R.id.actv_status);

        // Ánh xạ các TextInputLayout (nếu bạn muốn tương tác với chúng, ví dụ: để set sự kiện cho end icon)
        TextInputLayout tilCategory = view.findViewById(R.id.til_category);
        TextInputLayout tilTechnology = view.findViewById(R.id.til_technology);
        TextInputLayout tilStatus = view.findViewById(R.id.til_status);

        // --- Thiết lập Adapters ---
        // Khởi tạo ArrayAdapter cho từng AutoCompleteTextView
        // requireContext() lấy Context của Fragment (cần thiết cho Adapter)
        // android.R.layout.simple_dropdown_item_1line là một layout XML có sẵn của Android để hiển thị một dòng text đơn giản trong dropdown
        // categoryList (hoặc technologyList, statusList) là nguồn dữ liệu cho Adapter
        categoryAdapter = new ArrayAdapter<>(requireContext(), android.R.layout.simple_dropdown_item_1line, categoryList);
        actvCategory.setAdapter(categoryAdapter); // Gán Adapter cho AutoCompleteTextView tương ứng

        technologyAdapter = new ArrayAdapter<>(requireContext(), android.R.layout.simple_dropdown_item_1line, technologyList);
        actvTechnology.setAdapter(technologyAdapter);

        statusAdapter = new ArrayAdapter<>(requireContext(), android.R.layout.simple_dropdown_item_1line, statusList);
        actvStatus.setAdapter(statusAdapter);

        // --- Tải dữ liệu từ Firestore ---
        // Gọi phương thức chung để tải dữ liệu cho từng trường
        // "categories" là tên collection trong Firestore
        // "name" là tên field trong document chứa tên Lĩnh vực
        // categoryList là List sẽ lưu dữ liệu
        // categoryAdapter là Adapter sẽ được cập nhật
        // "Lĩnh vực" là tiền tố cho log
        fetchDataFromFirestore("categories", "name", categoryList, categoryAdapter, "Lĩnh vực");
        fetchDataFromFirestore("technologies", "name", technologyList, technologyAdapter, "Công nghệ");
        // Giả sử collection cho Trạng thái là "projectStatuses" và field chứa tên trạng thái là "status_name"
        fetchDataFromFirestore("projectStatuses", "name", statusList, statusAdapter, "Trạng thái");

        // --- (Tùy chọn) Thiết lập sự kiện khi người dùng chọn một mục trong dropdown ---
        setupItemClickListener(actvCategory, "Lĩnh vực");
        setupItemClickListener(actvTechnology, "Công nghệ");
        setupItemClickListener(actvStatus, "Trạng thái");

        // --- (Tùy chọn) Thiết lập sự kiện để mở dropdown khi nhấn vào icon cuối của TextInputLayout (nếu có) ---
        setupDropdownToggle(tilCategory, actvCategory);
        setupDropdownToggle(tilTechnology, actvTechnology);
        setupDropdownToggle(tilStatus, actvStatus);
    }

    /**
     * Phương thức chung để tải dữ liệu từ một collection Firestore và điền vào một AutoCompleteTextView.
     *
     * @param collectionName Tên của collection trong Firestore (ví dụ: "categories", "technologies").
     * @param fieldName      Tên của trường trong document chứa giá trị chuỗi cần hiển thị (ví dụ: "name", "status_name").
     * @param dataList       List<String> để lưu trữ dữ liệu được tải về.
     * @param adapter        ArrayAdapter<String> tương ứng với AutoCompleteTextView, sẽ được thông báo khi dữ liệu thay đổi.
     * @param logTagPrefix   Một tiền tố chuỗi dùng cho việc ghi log, giúp phân biệt log của từng loại dữ liệu (ví dụ: "Lĩnh vực", "Công nghệ").
     */
    private void fetchDataFromFirestore(String collectionName, String fieldName, List<String> dataList, ArrayAdapter<String> adapter, String logTagPrefix) {
        Log.d(TAG, "Đang tải " + logTagPrefix + " từ Firestore (Collection: " + collectionName + ")");
        dataList.clear(); // Xóa dữ liệu cũ trong List trước khi tải dữ liệu mới (tránh trùng lặp nếu hàm này được gọi lại)

        db.collection(collectionName) // Truy cập vào collection Firestore dựa trên tên được cung cấp
                .orderBy(fieldName)    // (Tùy chọn) Sắp xếp các document lấy về theo giá trị của fieldName. Nếu không cần, có thể bỏ dòng này.
                .get() // Thực hiện một lần lấy dữ liệu (one-time fetch)
                .addOnCompleteListener(task -> { // Thêm một listener để xử lý kết quả khi tác vụ lấy dữ liệu hoàn thành (thành công hoặc thất bại)
                    if (task.isSuccessful()) { // Kiểm tra xem tác vụ có thành công không
                        QuerySnapshot querySnapshot = task.getResult(); // Lấy kết quả của query, chứa các document
                        if (querySnapshot != null && !querySnapshot.isEmpty()) { // Kiểm tra xem có kết quả và kết quả không rỗng
                            for (QueryDocumentSnapshot document : querySnapshot) { // Lặp qua từng document trong kết quả
                                String value = document.getString(fieldName); // Lấy giá trị của trường fieldName từ document hiện tại
                                if (value != null && !value.isEmpty()) { // Kiểm tra giá trị không null và không rỗng
                                    dataList.add(value); // Thêm giá trị vào List dữ liệu
                                }
                            }
                            adapter.notifyDataSetChanged(); // QUAN TRỌNG: Thông báo cho Adapter biết dữ liệu nguồn đã thay đổi, để nó cập nhật lại giao diện
                            Log.d(TAG, "Đã tải xong " + logTagPrefix + ": " + dataList.size() + " mục.");
                        } else {
                            Log.d(TAG, "Không có " + logTagPrefix + " nào trong collection '" + collectionName + "'.");
                            // Hiển thị thông báo cho người dùng nếu không có dữ liệu
                            if(getContext() != null) { // Kiểm tra context trước khi tạo Toast
                                Toast.makeText(getContext(), "Không tìm thấy " + logTagPrefix.toLowerCase() + ".", Toast.LENGTH_SHORT).show();
                            }
                        }
                    } else {
                        Log.w(TAG, "Lỗi khi lấy dữ liệu " + logTagPrefix + ": ", task.getException());
                        // Hiển thị thông báo lỗi cho người dùng
                        if(getContext() != null) { // Kiểm tra context
                            Toast.makeText(getContext(), "Lỗi khi tải " + logTagPrefix.toLowerCase() + ".", Toast.LENGTH_SHORT).show();
                        }
                    }
                });
    }

    /**
     * Thiết lập sự kiện onItemClick cho một AutoCompleteTextView.
     * Sự kiện này được kích hoạt khi người dùng chọn một mục từ danh sách thả xuống.
     *
     * @param autoCompleteTextView AutoCompleteTextView cần thiết lập sự kiện.
     * @param fieldName            Tên của trường (dùng để hiển thị trong Toast, ví dụ: "Lĩnh vực").
     */
    private void setupItemClickListener(AutoCompleteTextView autoCompleteTextView, String fieldName) {
        autoCompleteTextView.setOnItemClickListener((parent, MView, position, id) -> {
            // parent là AdapterView (trong trường hợp này là AutoCompleteTextView)
            // MView là View của mục được chọn trong dropdown
            // position là vị trí của mục được chọn trong Adapter
            // id là row ID của mục được chọn (thường không dùng nhiều với ArrayAdapter<String>)
            String selectedItem = (String) parent.getItemAtPosition(position); // Lấy chuỗi của mục được chọn
            // Hoặc có thể dùng: String selectedItem = adapter.getItem(position); (nếu bạn truyền adapter vào đây)

            // Hiển thị một Toast thông báo mục đã được chọn
            if(getContext() != null) {
                Toast.makeText(requireContext(), "Đã chọn " + fieldName + ": " + selectedItem, Toast.LENGTH_SHORT).show();
            }
            // Bạn có thể thực hiện các hành động khác với selectedItem ở đây (ví dụ: lưu vào biến, gọi API khác, ...)
        });
    }

    /**
     * (Tùy chọn) Thiết lập sự kiện để mở danh sách thả xuống khi người dùng nhấn vào icon cuối của TextInputLayout (nếu có).
     *
     * @param textInputLayout      TextInputLayout bao bọc AutoCompleteTextView.
     * @param autoCompleteTextView AutoCompleteTextView tương ứng.
     */
    private void setupDropdownToggle(TextInputLayout textInputLayout, AutoCompleteTextView autoCompleteTextView) {
        // Kiểm tra xem TextInputLayout có tồn tại và có icon cuối (end icon) không
        if (textInputLayout != null && textInputLayout.getEndIconDrawable() != null) {
            textInputLayout.setEndIconOnClickListener(v -> { // Thiết lập sự kiện click cho icon cuối
                // Nếu danh sách thả xuống chưa hiển thị, thì hiển thị nó
                if (!autoCompleteTextView.isPopupShowing()) {
                    autoCompleteTextView.showDropDown();
                }
                // Nếu đã hiển thị, có thể bạn muốn ẩn nó đi (tùy theo ý muốn)
                // else {
                //    autoCompleteTextView.dismissDropDown();
                // }
            });
        }
        // Lưu ý: Với AutoCompleteTextView có android:inputType="none",
        // việc nhấn vào chính AutoCompleteTextView thường đã đủ để hiển thị dropdown.
        // Tuy nhiên, việc set sự kiện cho end icon có thể cải thiện trải nghiệm người dùng.
        // Nếu không có end icon, bạn có thể thêm OnClickListener trực tiếp cho AutoCompleteTextView:
        // autoCompleteTextView.setOnClickListener(v -> autoCompleteTextView.showDropDown());
    }
}