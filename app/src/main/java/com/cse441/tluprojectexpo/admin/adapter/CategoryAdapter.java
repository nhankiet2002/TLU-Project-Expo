// PATH: com/cse441/tluprojectexpo/admin/adapter/CategoryAdapter.java

package com.cse441.tluprojectexpo.admin.adapter;

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

    public interface OnCategoryClickListener {
        void onEditClick(Category category);
        void onDeleteClick(Category category);
    }

    private final List<Category> categoryList;
    private final OnCategoryClickListener listener;

    public CategoryAdapter(List<Category> categoryList, OnCategoryClickListener listener) {
        this.categoryList = categoryList;
        this.listener = listener;
    }

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

        public void bind(final Category category, final OnCategoryClickListener listener) {
            tvItemText.setText(category.getName());
            editButton.setOnClickListener(v -> listener.onEditClick(category));
            deleteButton.setOnClickListener(v -> listener.onDeleteClick(category));
        }
    }

    @NonNull
    @Override
    public CategoryViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_catalog, parent, false);
        return new CategoryViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull CategoryViewHolder holder, int position) {
        Category currentCategory = categoryList.get(position);
        holder.bind(currentCategory, listener);
    }

    @Override
    public int getItemCount() {
        return categoryList != null ? categoryList.size() : 0;
    }

    // ĐÃ XÓA HÀM updateData() KHÔNG CẦN THIẾT
}