package com.cse441.tluprojectexpo.fragment;

// Trong package fragment hoặc dialog của bạn
import android.app.Dialog;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.DialogFragment;
import androidx.recyclerview.widget.DividerItemDecoration;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.cse441.tluprojectexpo.R; // Thay thế
import com.cse441.tluprojectexpo.adapter.MemberSearchAdapter; // Thay thế
import com.cse441.tluprojectexpo.model.Member; // Thay thế
import java.util.ArrayList;
import java.util.List;

public class AddMemberDialogFragment extends DialogFragment implements MemberSearchAdapter.OnMemberClickListener {

    private static final String TAG = "AddMemberDialog";
    private TextInputEditText etSearchMember;
    private RecyclerView rvMembersList;
    private ImageView ivCloseDialog;
    private ProgressBar pbLoadingMembers;
    private MemberSearchAdapter adapter;
    private List<Member> allMembers = new ArrayList<>();
    private FirebaseFirestore db;

    // Interface để gửi thành viên đã chọn về Fragment cha
    public interface AddMemberDialogListener {
        void onMemberSelected(Member member);
    }
    private AddMemberDialogListener dialogListener;

    public static AddMemberDialogFragment newInstance() {
        return new AddMemberDialogFragment();
    }

    // Để Fragment cha có thể lắng nghe
    public void setDialogListener(AddMemberDialogListener listener) {
        this.dialogListener = listener;
    }


    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.dialog_add_member, container, false);

        db = FirebaseFirestore.getInstance();

        etSearchMember = view.findViewById(R.id.et_search_member_dialog);
        rvMembersList = view.findViewById(R.id.rv_members_list_dialog);
        ivCloseDialog = view.findViewById(R.id.iv_close_dialog_member);
        pbLoadingMembers = view.findViewById(R.id.pb_loading_members);

        setupRecyclerView();
        fetchMembersFromFirestore();

        ivCloseDialog.setOnClickListener(v -> dismiss());

        etSearchMember.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                if (adapter != null) {
                    adapter.filter(s.toString());
                }
            }
            @Override
            public void afterTextChanged(Editable s) {}
        });

        return view;
    }

    @Override
    public void onStart() {
        super.onStart();
        Dialog dialog = getDialog();
        if (dialog != null && dialog.getWindow() != null) {
            Window window = dialog.getWindow();
            window.setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT)); // Để bo góc hoạt động
            window.setGravity(Gravity.CENTER); // Đảm bảo dialog ở giữa

            // Thiết lập kích thước dialog
            DisplayMetrics displayMetrics = new DisplayMetrics();
            getActivity().getWindowManager().getDefaultDisplay().getMetrics(displayMetrics);
            int dialogWidth = (int)(displayMetrics.widthPixels * 0.90); // 90% chiều rộng màn hình
            window.setLayout(dialogWidth, ViewGroup.LayoutParams.WRAP_CONTENT);
        }
    }

    private void setupRecyclerView() {
        adapter = new MemberSearchAdapter(getContext(), new ArrayList<>(), this); // Truyền this làm listener
        rvMembersList.setLayoutManager(new LinearLayoutManager(getContext()));
        rvMembersList.setAdapter(adapter);
        // Thêm đường kẻ giữa các item (tùy chọn)
        rvMembersList.addItemDecoration(new DividerItemDecoration(getContext(), DividerItemDecoration.VERTICAL));
    }

    private void fetchMembersFromFirestore() {
        pbLoadingMembers.setVisibility(View.VISIBLE);
        rvMembersList.setVisibility(View.GONE);

        db.collection("users")
                .orderBy("fullName") // Vẫn sắp xếp theo tên
                .get()
                .addOnCompleteListener(task -> {
                    pbLoadingMembers.setVisibility(View.GONE);
                    rvMembersList.setVisibility(View.VISIBLE);
                    if (task.isSuccessful() && task.getResult() != null) {
                        allMembers.clear();
                        List<Member> tempMemberList = new ArrayList<>(); // Danh sách tạm thời để lọc

                        for (QueryDocumentSnapshot document : task.getResult()) {
                            try {
                                // Lấy danh sách roles từ document
                                List<String> roles = (List<String>) document.get("roles");

                                // Kiểm tra xem "Admin" có trong danh sách roles không
                                // Hoặc nếu roles là null/không tồn tại thì coi như không phải Admin (tùy logic của bạn)
                                boolean isAdmin = false;
                                if (roles != null) {
                                    for (String role : roles) {
                                        if ("Admin".equalsIgnoreCase(role)) {
                                            isAdmin = true;
                                            break;
                                        }
                                    }
                                }

                                // Chỉ thêm vào danh sách nếu không phải là Admin
                                if (!isAdmin) {
                                    Member member = document.toObject(Member.class);
                                    member.setUserId(document.getId());
                                    tempMemberList.add(member); // Thêm vào danh sách tạm thời
                                    Log.d(TAG, "Fetched non-admin member: " + member.toString());
                                } else {
                                    Log.d(TAG, "Skipped admin user: " + document.getString("fullName"));
                                }

                            } catch (Exception e) {
                                Log.e(TAG, "Error processing document: " + document.getId(), e);
                            }
                        }
                        allMembers.addAll(tempMemberList); // Cập nhật danh sách chính sau khi lọc

                        if (adapter != null) {
                            adapter.updateData(allMembers);
                            adapter.filter(etSearchMember.getText().toString());
                        } else {
                            Log.e(TAG, "Adapter is null after fetching members.");
                        }
                    } else {
                        Log.w(TAG, "Lỗi khi tải danh sách thành viên.", task.getException());
                        if(getContext() != null) {
                            Toast.makeText(getContext(), "Lỗi tải danh sách thành viên.", Toast.LENGTH_SHORT).show();
                        }
                    }
                });
    }

    // Xử lý khi một thành viên được chọn từ Adapter
    @Override
    public void onMemberClick(Member member) {
        if (dialogListener != null) {
            dialogListener.onMemberSelected(member);
        }
        // Có thể bạn muốn Toast hoặc làm gì đó ở đây trước khi đóng
        Toast.makeText(getContext(), "Đã chọn: " + member.getName(), Toast.LENGTH_SHORT).show();
        dismiss(); // Đóng dialog sau khi chọn
    }
}
