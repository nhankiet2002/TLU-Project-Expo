// Đảm bảo package của bạn là chính xác
package com.cse441.tluprojectexpo.admin.adapter;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.appcompat.widget.SwitchCompat;
import androidx.recyclerview.widget.RecyclerView;

import com.cse441.tluprojectexpo.R; // Đảm bảo bạn đã import R
import com.cse441.tluprojectexpo.model.FeaturedProjectUIModel; // Import model UI

import java.util.List;

public class FeaturedProjectAdapter extends RecyclerView.Adapter<FeaturedProjectAdapter.ProjectViewHolder> {

    private Context context;
    private List<FeaturedProjectUIModel> uiModelList;
    private OnProjectInteractionListener listener;

    /**
     * Interface để gửi các sự kiện tương tác từ Adapter về lại Activity/Fragment.
     */
    public interface OnProjectInteractionListener {
        void onSwitchChanged(FeaturedProjectUIModel item, boolean isChecked);
        void onViewMoreClicked(FeaturedProjectUIModel item);
    }

    /**
     * Đây là CONSTRUCTOR DUY NHẤT và ĐÚNG của Adapter.
     * @param context Context của Activity/Fragment gọi nó.
     * @param list Danh sách dữ liệu để hiển thị.
     * @param listener Một đối tượng (thường là Activity/Fragment) đã implement OnProjectInteractionListener.
     */
    public FeaturedProjectAdapter(Context context, List<FeaturedProjectUIModel> list, OnProjectInteractionListener listener) {
        this.context = context;
        this.uiModelList = list;
        this.listener = listener;
    }

    @NonNull
    @Override
    public ProjectViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        // Đảm bảo tên file layout item là chính xác
        View view = LayoutInflater.from(context).inflate(R.layout.item_featured, parent, false);
        return new ProjectViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ProjectViewHolder holder, int position) {
        FeaturedProjectUIModel uiModel = uiModelList.get(position);
        holder.bind(uiModel);
    }

    @Override
    public int getItemCount() {
        return uiModelList != null ? uiModelList.size() : 0;
    }

    /**
     * Cập nhật toàn bộ danh sách dữ liệu và làm mới RecyclerView.
     * @param newList Danh sách mới.
     */
    public void updateData(List<FeaturedProjectUIModel> newList) {
        this.uiModelList.clear();
        this.uiModelList.addAll(newList);
        notifyDataSetChanged();
    }

    /**
     * Xóa một item khỏi danh sách tại một vị trí cụ thể và thông báo cho Adapter.
     * @param position Vị trí của item cần xóa.
     */
    public void removeItem(int position) {
        if (position >= 0 && position < uiModelList.size()) {
            uiModelList.remove(position);
            notifyItemRemoved(position);
        }
    }

    /**
     * Lớp ViewHolder để giữ các tham chiếu đến View của mỗi item.
     */
    class ProjectViewHolder extends RecyclerView.ViewHolder {
        TextView projectName, personPosting, txtCategory, viewMore;
        SwitchCompat switchSetFeatured;

        public ProjectViewHolder(@NonNull View itemView) {
            super(itemView);
            // Ánh xạ các View từ file layout item_featured_project.xml
            projectName = itemView.findViewById(R.id.project_name);
            personPosting = itemView.findViewById(R.id.person_posting);
            txtCategory = itemView.findViewById(R.id.txt_category);
            viewMore = itemView.findViewById(R.id.view_more);
            switchSetFeatured = itemView.findViewById(R.id.switch_set_featured);
        }

        /**
         * Gán dữ liệu từ một đối tượng UIModel lên các View.
         * @param uiModel Đối tượng chứa dữ liệu.
         */
        void bind(final FeaturedProjectUIModel uiModel) {
            projectName.setText(uiModel.getProjectTitle());
            personPosting.setText(uiModel.getCreatorName());
            txtCategory.setText(uiModel.getCategoryName());

            // Xóa listener cũ trước khi đặt trạng thái mới để tránh kích hoạt sự kiện không mong muốn
            switchSetFeatured.setOnCheckedChangeListener(null);
            // Vì đây là màn hình các dự án nổi bật, switch luôn được bật
            switchSetFeatured.setChecked(true);

            // Gán lại listener mới
            switchSetFeatured.setOnCheckedChangeListener((buttonView, isChecked) -> {
                // Chỉ kích hoạt sự kiện nếu người dùng thực sự nhấn vào switch
                if (listener != null && buttonView.isPressed()) {
                    listener.onSwitchChanged(uiModel, isChecked);
                }
            });

            viewMore.setOnClickListener(v -> {
                if (listener != null) {
                    listener.onViewMoreClicked(uiModel);
                }
            });
        }
    }
}