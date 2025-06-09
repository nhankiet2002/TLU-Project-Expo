package com.cse441.tluprojectexpo.adapter;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.cse441.tluprojectexpo.R;
import com.cse441.tluprojectexpo.model.Category;

import java.util.List;

public class CategoryAdapter extends RecyclerView.Adapter<CategoryAdapter.CategoryViewHolder> {

    // 1. Định nghĩa Interface để gửi sự kiện click về Activity/Fragment
    public interface OnCategoryClickListener {
        void onEditClick(Category category);
        void onDeleteClick(Category category);
    }

    private List<Category> categoryList;
    private OnCategoryClickListener listener;

    // 2. Constructor nhận dữ liệu và listener
    public CategoryAdapter(List<Category> categoryList, OnCategoryClickListener listener) {
        this.categoryList = categoryList;
        this.listener = listener;
    }

    // 3. ViewHolder để giữ các view của một item
    public static class CategoryViewHolder extends RecyclerView.ViewHolder {
        TextView tvItemText;
        ImageView editButton;
        ImageView deleteButton;

        public CategoryViewHolder(@NonNull View itemView) {
            super(itemView);
            tvItemText = itemView.findViewById(R.id.tvItemText);
            editButton = itemView.findViewById(R.id.edit_catalog);
            deleteButton = itemView.findViewById(R.id.delete_catalog);
        }

        // 4. Hàm bind để gán dữ liệu và listener cho các view
        public void bind(final Category category, final OnCategoryClickListener listener) {
            tvItemText.setText(category.getName());
            editButton.setOnClickListener(v -> listener.onEditClick(category));
            deleteButton.setOnClickListener(v -> listener.onDeleteClick(category));
        }
    }

    // 5. Phương thức để tạo ViewHolder mới(Custom layout được bày trí trong item_catalog.xml)
    @NonNull
    @Override
    public CategoryViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_catalog, parent, false); // Sử dụng item_layout.xml của bạn
        return new CategoryViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull CategoryViewHolder holder, int position) {
        Category currentCategory = categoryList.get(position);
        holder.bind(currentCategory, listener); // Gán dữ liệu và listener
    }

    @Override
    public int getItemCount() {
        return categoryList != null ? categoryList.size() : 0;
    }

    // Hàm helper để cập nhật dữ liệu mới cho adapter
    public void updateData(List<Category> newCategories) {
        this.categoryList.clear();
        this.categoryList.addAll(newCategories);
        notifyDataSetChanged(); // Báo cho RecyclerView biết dữ liệu đã thay đổi để vẽ lại
    }
}
