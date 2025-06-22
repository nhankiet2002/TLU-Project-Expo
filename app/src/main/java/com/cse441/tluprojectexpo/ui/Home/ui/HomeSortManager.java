package com.cse441.tluprojectexpo.ui.Home.ui;

import android.content.Context;
import android.view.View;
import android.widget.PopupMenu;
import com.cse441.tluprojectexpo.R; // Đảm bảo R file được import đúng
import com.cse441.tluprojectexpo.utils.Constants;
import com.google.firebase.firestore.Query;

public class HomeSortManager {
    private Context context;
    private String currentSortField = Constants.FIELD_CREATED_AT; // Mặc định
    private Query.Direction currentSortDirection = Query.Direction.DESCENDING;

    public interface SortChangeListener {
        void onSortChanged(String newSortField, Query.Direction newSortDirection);
    }
    private SortChangeListener sortChangeListener;

    public HomeSortManager(Context context, SortChangeListener listener) {
        this.context = context;
        this.sortChangeListener = listener;
    }

    public String getCurrentSortField() { return currentSortField; }
    public Query.Direction getCurrentSortDirection() { return currentSortDirection; }

    public void showSortMenu(View anchorView) {
        PopupMenu popup = new PopupMenu(context, anchorView);
        popup.getMenuInflater().inflate(R.menu.sort_options_menu, popup.getMenu()); // Cần file menu này
        popup.setOnMenuItemClickListener(item -> {
            boolean changed = true;
            int itemId = item.getItemId();
            if (itemId == R.id.sort_by_name_asc) {
                currentSortField = "Title"; currentSortDirection = Query.Direction.ASCENDING;
            } else if (itemId == R.id.sort_by_name_desc) {
                currentSortField = "Title"; currentSortDirection = Query.Direction.DESCENDING;
            } else if (itemId == R.id.sort_by_date_desc) {
                currentSortField = Constants.FIELD_CREATED_AT; currentSortDirection = Query.Direction.DESCENDING;
            } else if (itemId == R.id.sort_by_date_asc) {
                currentSortField = Constants.FIELD_CREATED_AT; currentSortDirection = Query.Direction.ASCENDING;
            } else if (itemId == R.id.sort_by_votes_desc) {
                currentSortField = Constants.FIELD_VOTE_COUNT; currentSortDirection = Query.Direction.DESCENDING;
            } else {
                changed = false;
            }

            if (changed && sortChangeListener != null) {
                sortChangeListener.onSortChanged(currentSortField, currentSortDirection);
            }
            return true;
        });
        popup.show();
    }
}