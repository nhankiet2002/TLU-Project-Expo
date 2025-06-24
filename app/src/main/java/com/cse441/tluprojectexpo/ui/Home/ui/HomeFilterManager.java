package com.cse441.tluprojectexpo.ui.Home.ui;

import android.content.Context;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.core.content.ContextCompat;

import com.cse441.tluprojectexpo.R;
import com.cse441.tluprojectexpo.admin.utils.AppToast;
import com.cse441.tluprojectexpo.ui.Home.model.FilterableItem;
import com.google.android.material.chip.Chip;

import java.util.List;

public class HomeFilterManager {
    private Context context;
    private Chip chipCategory, chipTechnology, chipStatus;

    private String selectedCategoryId = null;
    private String selectedCategoryName = null;
    private String selectedTechnologyId = null;
    private String selectedTechnologyName = null;
    private String selectedStatus = null;

    private List<? extends FilterableItem> categoryDataSource;
    private List<? extends FilterableItem> technologyDataSource;
    private String[] statusDataSource;

    public interface FilterChangeListener {
        void onFilterChanged();
    }
    private FilterChangeListener filterChangeListener;

    public HomeFilterManager(Context context, Chip chipCategory, Chip chipTechnology, Chip chipStatus,
                             FilterChangeListener listener) {
        this.context = context;
        this.chipCategory = chipCategory;
        this.chipTechnology = chipTechnology;
        this.chipStatus = chipStatus;
        this.filterChangeListener = listener;
        setupChipClickListeners();
    }

    public void setCategoryDataSource(List<? extends FilterableItem> categories) {
        this.categoryDataSource = categories;
    }

    public void setTechnologyDataSource(List<? extends FilterableItem> technologies) {
        this.technologyDataSource = technologies;
    }

    public void setStatusDataSource(String[] statuses) {
        this.statusDataSource = statuses;
    }

    public String getSelectedCategoryId() { return selectedCategoryId; }
    public String getSelectedTechnologyId() { return selectedTechnologyId; }
    public String getSelectedStatus() { return selectedStatus; }

    private void setupChipClickListeners() {
        chipCategory.setOnClickListener(v -> showCategoryDialog());
        chipCategory.setOnCloseIconClickListener(v -> clearCategoryFilter());

        chipTechnology.setOnClickListener(v -> showTechnologyDialog());
        chipTechnology.setOnCloseIconClickListener(v -> clearTechnologyFilter());

        chipStatus.setOnClickListener(v -> showStatusDialog());
        chipStatus.setOnCloseIconClickListener(v -> clearStatusFilter());
    }

    private void showCategoryDialog() {
        if (categoryDataSource == null || categoryDataSource.isEmpty()) {
            AppToast.show(context, "Dữ liệu lĩnh vực chưa sẵn sàng.", Toast.LENGTH_SHORT);
            return;
        }
        showGenericFilterDialog("Chọn Lĩnh vực/Chủ đề", categoryDataSource, selectedCategoryId,
                (id, name) -> {
                    selectedCategoryId = id; selectedCategoryName = name;
                    updateChipUI(chipCategory, "Lĩnh vực/ Chủ đề", true, selectedCategoryName);
                    if (filterChangeListener != null) filterChangeListener.onFilterChanged();
                }, this::clearCategoryFilter);
    }

    private void showTechnologyDialog() {
        if (technologyDataSource == null || technologyDataSource.isEmpty()) {
            AppToast.show(context, "Dữ liệu công nghệ chưa sẵn sàng.", Toast.LENGTH_SHORT);
            return;
        }
        showGenericFilterDialog("Chọn Công nghệ", technologyDataSource, selectedTechnologyId,
                (id, name) -> {
                    selectedTechnologyId = id; selectedTechnologyName = name;
                    updateChipUI(chipTechnology, "Công nghệ", true, selectedTechnologyName);
                    if (filterChangeListener != null) filterChangeListener.onFilterChanged();
                }, this::clearTechnologyFilter);
    }

    private void showStatusDialog() {
        if (statusDataSource == null || statusDataSource.length == 0) {
            AppToast.show(context, "Dữ liệu trạng thái chưa sẵn sàng.", Toast.LENGTH_SHORT);
            return;
        }
        int currentSelection = -1;
        if (selectedStatus != null) {
            for (int i = 0; i < statusDataSource.length; i++) {
                if (statusDataSource[i].equals(selectedStatus)) {
                    currentSelection = i; break;
                }
            }
        }
        new AlertDialog.Builder(context)
                .setTitle("Chọn Trạng thái")
                .setSingleChoiceItems(statusDataSource, currentSelection, (dialog, which) -> {
                    selectedStatus = statusDataSource[which];
                    updateChipUI(chipStatus, "Trạng thái", true, selectedStatus);
                    if (filterChangeListener != null) filterChangeListener.onFilterChanged();
                    dialog.dismiss();
                })
                .setNegativeButton("Bỏ qua", (dialog, which) -> dialog.dismiss())
                .setNeutralButton("Xóa lọc", (dialog, which) -> {
                    clearStatusFilter();
                    dialog.dismiss();
                })
                .show();
    }

    private void showGenericFilterDialog(String title, List<? extends FilterableItem> items, String currentSelectedId,
                                         OnItemSelectedListener itemSelectedListener, Runnable clearListener) {
        String[] itemNames = items.stream().map(FilterableItem::getName).toArray(String[]::new);
        int currentSelectionIndex = -1;
        if (currentSelectedId != null) {
            for (int i = 0; i < items.size(); i++) {
                if (items.get(i).getId().equals(currentSelectedId)) {
                    currentSelectionIndex = i; break;
                }
            }
        }
        new AlertDialog.Builder(context)
                .setTitle(title)
                .setSingleChoiceItems(itemNames, currentSelectionIndex, (dialog, which) -> {
                    itemSelectedListener.onSelected(items.get(which).getId(), items.get(which).getName());
                    dialog.dismiss();
                })
                .setNegativeButton("Bỏ qua", (dialog, which) -> dialog.dismiss())
                .setNeutralButton("Xóa lọc", (dialog, which) -> {
                    if(clearListener != null) clearListener.run();
                    dialog.dismiss();
                })
                .show();
    }


    private void clearCategoryFilter() {
        selectedCategoryId = null; selectedCategoryName = null;
        updateChipUI(chipCategory, "Lĩnh vực/ Chủ đề", false, null);
        if (filterChangeListener != null) filterChangeListener.onFilterChanged();
    }

    private void clearTechnologyFilter() {
        selectedTechnologyId = null; selectedTechnologyName = null;
        updateChipUI(chipTechnology, "Công nghệ", false, null);
        if (filterChangeListener != null) filterChangeListener.onFilterChanged();
    }

    private void clearStatusFilter() {
        selectedStatus = null;
        updateChipUI(chipStatus, "Trạng thái", false, null);
        if (filterChangeListener != null) filterChangeListener.onFilterChanged();
    }

    public void updateAllChipUI() {
        updateChipUI(chipCategory, "Lĩnh vực/ Chủ đề", selectedCategoryId != null, selectedCategoryName);
        updateChipUI(chipTechnology, "Công nghệ", selectedTechnologyId != null, selectedTechnologyName);
        updateChipUI(chipStatus, "Trạng thái", selectedStatus != null, selectedStatus);
    }

    private void updateChipUI(Chip chip, String baseText, boolean active, @Nullable String selectedValueText) {
        if (chip == null) return;
        String displayText = baseText;
        if (active && selectedValueText != null && !selectedValueText.isEmpty()) {
            displayText += ": " + selectedValueText;
            chip.setChipBackgroundColorResource(R.color.colorPrimaryLight); // Cần màu này trong colors.xml
            chip.setTextColor(ContextCompat.getColor(context, android.R.color.white));
            chip.setChipStrokeColorResource(R.color.colorPrimary); // Cần màu này
        } else {
            chip.setChipBackgroundColorResource(android.R.color.white);
            chip.setTextColor(ContextCompat.getColor(context, android.R.color.black));
            chip.setChipStrokeColorResource(R.color.black);
        }
        chip.setText(displayText);
        chip.setCloseIconVisible(active);
    }

    private interface OnItemSelectedListener {
        void onSelected(String id, String name);
    }
}