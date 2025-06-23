package com.cse441.tluprojectexpo.ui.detailproject.adapter;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.bumptech.glide.Glide;
import com.cse441.tluprojectexpo.R;
import com.cse441.tluprojectexpo.model.Project; // Model Project chứa MediaItem
import com.cse441.tluprojectexpo.utils.UiHelper;   // Helper của bạn (nếu có)
import java.util.List;

public class MediaGalleryAdapter extends RecyclerView.Adapter<MediaGalleryAdapter.ViewHolder> {
    private static final String TAG = "MediaGalleryAdapter"; // Thêm TAG để debug
    private Context context;
    private List<Project.MediaItem> mediaItems; // Sử dụng MediaItem từ model Project của bạn

    public MediaGalleryAdapter(Context context, List<Project.MediaItem> mediaItems) {
        this.context = context;
        this.mediaItems = mediaItems;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_media_gallery, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        Project.MediaItem mediaItem = mediaItems.get(position);
        // Đảm bảo MediaItem của bạn có getUrl() và getType()
        Glide.with(context)
                .load(mediaItem.getUrl())
                .placeholder(R.drawable.ic_placeholder_image) // Cần drawable này
                .error(R.drawable.ic_image_error)           // Cần drawable này
                .centerCrop()
                .into(holder.imageViewMediaItem);

        if ("video".equalsIgnoreCase(mediaItem.getType())) {
            holder.imageViewPlayIcon.setVisibility(View.VISIBLE);
        } else {
            holder.imageViewPlayIcon.setVisibility(View.GONE);
        }

        holder.itemView.setOnClickListener(v -> {
            String url = mediaItem.getUrl();
            if (url == null || url.isEmpty()) {
                UiHelper.showToast(context, "Liên kết media không hợp lệ", Toast.LENGTH_SHORT);
                return;
            }
            Intent intent = new Intent(Intent.ACTION_VIEW);
            String type = "video".equalsIgnoreCase(mediaItem.getType()) ? "video/*" : "image/*";
            intent.setDataAndType(Uri.parse(url), type);
            try {
                if (intent.resolveActivity(context.getPackageManager()) != null) {
                    context.startActivity(intent);
                } else {
                    UiHelper.showToast(context, "Không tìm thấy ứng dụng để mở media này.", Toast.LENGTH_SHORT);
                }
            } catch (Exception e) {
                UiHelper.showToast(context, "Không thể mở media. Vui lòng thử lại.", Toast.LENGTH_SHORT);
                Log.e(TAG, "Error opening media: " + url, e);
            }
        });
    }

    @Override
    public int getItemCount() {
        return mediaItems.size();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        ImageView imageViewMediaItem;
        ImageView imageViewPlayIcon; // Đảm bảo ID này có trong item_media_gallery.xml

        ViewHolder(View itemView) {
            super(itemView);
            // Đảm bảo các ID này khớp với file item_media_gallery.xml
            imageViewMediaItem = itemView.findViewById(R.id.imageViewMediaItem);
            imageViewPlayIcon = itemView.findViewById(R.id.imageViewPlayIcon);
        }
    }
}