package com.cse441.tluprojectexpo.admin.adapter;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.appcompat.widget.SwitchCompat;
import androidx.recyclerview.widget.RecyclerView;
import com.cse441.tluprojectexpo.R;
import com.cse441.tluprojectexpo.model.FeaturedProjectUIModel; // Đảm bảo bạn có model này

import java.util.List;

public class FeaturedProjectAdapter extends RecyclerView.Adapter<FeaturedProjectAdapter.ViewHolder> {

    private final Context context;
    private final List<FeaturedProjectUIModel> uiModelList;
    private final OnProjectInteractionListener listener;

    public interface OnProjectInteractionListener {
        void onSwitchChanged(FeaturedProjectUIModel item, boolean isChecked);
        void onViewMoreClicked(FeaturedProjectUIModel item);
    }

    public FeaturedProjectAdapter(Context context, List<FeaturedProjectUIModel> uiModelList, OnProjectInteractionListener listener) {
        this.context = context;
        this.uiModelList = uiModelList;
        this.listener = listener;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        // Đảm bảo tên file layout của bạn là "item_featured_project.xml"
        View view = LayoutInflater.from(context).inflate(R.layout.item_featured, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        FeaturedProjectUIModel item = uiModelList.get(position);
        holder.bind(item, listener);
    }

    @Override
    public int getItemCount() {
        return uiModelList.size();
    }

    // Phương thức để xóa item khỏi danh sách (dùng khi bỏ nổi bật)
    public void removeItem(int position) {
        if (position >= 0 && position < uiModelList.size()) {
            uiModelList.remove(position);
            notifyItemRemoved(position);
        }
    }

    public class ViewHolder extends RecyclerView.ViewHolder {
        // Khai báo View với đúng ID từ file XML của bạn
        TextView projectName, personPosting, txtCategory, viewMore;
        SwitchCompat switchSetFeatured;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            // Ánh xạ View với đúng ID từ file XML của bạn
            projectName = itemView.findViewById(R.id.project_name);
            personPosting = itemView.findViewById(R.id.person_posting);
            txtCategory = itemView.findViewById(R.id.txt_category);
            viewMore = itemView.findViewById(R.id.view_more);
            switchSetFeatured = itemView.findViewById(R.id.switch_set_featured);
        }

        public void bind(final FeaturedProjectUIModel item, final OnProjectInteractionListener listener) {
            // Đổ dữ liệu từ model vào các View
            // Giả sử model `FeaturedProjectUIModel` có các getter tương ứng
            projectName.setText(item.getProjectTitle());
            personPosting.setText("Bởi: " + item.getCreatorName());
            txtCategory.setText(item.getCategoryName()); // Giả sử có getCategoryName()

            viewMore.setOnClickListener(v -> {
                if (listener != null) {
                    listener.onViewMoreClicked(item);
                }
            });

            // Xử lý sự kiện cho Switch "Nổi bật"
            // Vì đây là danh sách các dự án đã nổi bật, switch luôn ở trạng thái "on"
            // Ta cần set listener về null trước khi set checked để tránh trigger sự kiện không mong muốn
            switchSetFeatured.setOnCheckedChangeListener(null);
            switchSetFeatured.setChecked(true);
            switchSetFeatured.setOnCheckedChangeListener((buttonView, isChecked) -> {
                // Sự kiện này chỉ được gọi khi người dùng tương tác
                // Nếu người dùng gạt TẮT (isChecked = false), ta sẽ báo cho Activity
                if (listener != null) {
                    listener.onSwitchChanged(item, isChecked);
                }
            });

            // Xử lý sự kiện cho TextView "Xem chi tiết"
            viewMore.setOnClickListener(v -> {
                if (listener != null) {
                    listener.onViewMoreClicked(item);
                }
            });
        }
    }
}