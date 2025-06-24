package com.cse441.tluprojectexpo.ui.common.uimanager;


import android.content.Context;
import android.net.Uri;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import com.bumptech.glide.Glide;
import com.cse441.tluprojectexpo.R;
import com.google.android.flexbox.FlexboxLayout; // Sử dụng FlexboxLayout
import java.util.List;

public class MediaGalleryUiManager {
    private static final String TAG = "MediaGalleryUiManager";
    private Context context;
    private FlexboxLayout flexboxContainer; // Sử dụng FlexboxLayout
    private TextView tvMediaLabel;
    private List<Uri> mediaUris;
    private OnMediaInteractionListener listener;

    public interface OnMediaInteractionListener {
        void onMediaRemoved(Uri uri, int index);
    }

    public MediaGalleryUiManager(Context context, FlexboxLayout flexboxContainer,
                                 TextView tvMediaLabel, List<Uri> mediaUris,
                                 OnMediaInteractionListener listener) {
        this.context = context;
        this.flexboxContainer = flexboxContainer;
        this.tvMediaLabel = tvMediaLabel;
        this.mediaUris = mediaUris;
        this.listener = listener;
    }

    public void updateUI() {
        if (flexboxContainer == null || tvMediaLabel == null) return;
        flexboxContainer.removeAllViews();
        if (mediaUris.isEmpty()) {
            flexboxContainer.setVisibility(View.GONE);
            tvMediaLabel.setVisibility(View.GONE);
            return;
        }
        flexboxContainer.setVisibility(View.VISIBLE);
        tvMediaLabel.setVisibility(View.VISIBLE);

        int imageSizeInDp = 80;
        int imageSizeInPx = (int) (imageSizeInDp * context.getResources().getDisplayMetrics().density);
        int marginInDp = 4;
        int marginInPx = (int) (marginInDp * context.getResources().getDisplayMetrics().density);

        for (int i = 0; i < mediaUris.size(); i++) {
            Uri uri = mediaUris.get(i);
            ImageView imageView = new ImageView(context);
            FlexboxLayout.LayoutParams layoutParams = new FlexboxLayout.LayoutParams(imageSizeInPx, imageSizeInPx);
            layoutParams.setMargins(marginInPx, marginInPx, marginInPx, marginInPx);
            imageView.setLayoutParams(layoutParams);
            imageView.setScaleType(ImageView.ScaleType.CENTER_CROP);
            
            // Load media content (both images and videos show thumbnail)
            Glide.with(context).load(uri)
                    .placeholder(R.drawable.image_placeholder_square)
                    .error(R.drawable.ic_image_error)
                    .centerCrop().into(imageView);
            
            Log.d(TAG, "Displaying media thumbnail for URI: " + uri);

            final int indexToRemove = i;
            imageView.setOnLongClickListener(v -> {
                if (indexToRemove < mediaUris.size() && listener != null) { // Kiểm tra index
                    listener.onMediaRemoved(mediaUris.get(indexToRemove), indexToRemove);
                }
                return true;
            });
            flexboxContainer.addView(imageView);
        }
    }
} 