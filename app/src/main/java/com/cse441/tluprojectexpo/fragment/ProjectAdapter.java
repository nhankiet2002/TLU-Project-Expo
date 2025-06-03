package com.cse441.tluprojectexpo.fragment;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.cse441.tluprojectexpo.R;
import com.cse441.tluprojectexpo.model.Project;

import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class ProjectAdapter extends RecyclerView.Adapter<ProjectAdapter.ProjectViewHolder> {

    private List<Project> projectList;
    // ExecutorService để chạy tác vụ tải ảnh trên luồng nền
    private final ExecutorService executorService = Executors.newFixedThreadPool(4); // Số luồng có thể điều chỉnh
    // Handler để cập nhật UI từ luồng nền
    private final Handler mainThreadHandler = new Handler(Looper.getMainLooper());

    public ProjectAdapter(Context context, List<Project> projectList) {
        // Context có thể không cần thiết nữa nếu không dùng Glide
        this.projectList = projectList;
    }

    @NonNull
    @Override
    public ProjectViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.list_item, parent, false);
        return new ProjectViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ProjectViewHolder holder, int position) {
        Project project = projectList.get(position);

        holder.textViewProjectName.setText(project.getName());
        holder.textViewDescription.setText(project.getDescription());
        holder.textViewAuthor.setText(project.getAuthor());
        holder.textViewTechnology.setText(project.getTechnology());

        String imageUrl = project.getImageUrl();
        Log.d("ProjectAdapter", "Attempting to load image from URL: " + imageUrl + " for project: " + project.getName());

        // Đặt placeholder trước khi tải
        holder.imageViewProject.setImageResource(R.drawable.ic_image_placeholder);

        if (imageUrl != null && !imageUrl.isEmpty()) {
            // Sử dụng ExecutorService để tải ảnh trên luồng nền
            executorService.execute(() -> {
                Bitmap bitmap = null;
                try {
                    URL url = new URL(imageUrl);
                    HttpURLConnection connection = (HttpURLConnection) url.openConnection();
                    connection.setDoInput(true);
                    connection.connect();
                    InputStream input = connection.getInputStream();
                    // Cân nhắc giải mã với BitmapFactory.Options để tránh OutOfMemoryError cho ảnh lớn
                    // Ví dụ: tính toán inSampleSize
                    bitmap = BitmapFactory.decodeStream(input);
                    input.close();
                    connection.disconnect();
                } catch (Exception e) {
                    Log.e("ProjectAdapter", "Error downloading image: " + imageUrl, e);
                }

                // Sau khi tải xong (hoặc lỗi), cập nhật ImageView trên luồng chính
                final Bitmap finalBitmap = bitmap;
                mainThreadHandler.post(() -> {
                    if (finalBitmap != null) {
                        holder.imageViewProject.setImageBitmap(finalBitmap);
                    } else {
                        // Nếu lỗi, hiển thị ảnh lỗi
                        holder.imageViewProject.setImageResource(R.drawable.error);
                    }
                });
            });
        } else {
            Log.d("ProjectAdapter", "Image URL is empty or null for project: " + project.getName());
            holder.imageViewProject.setImageResource(R.drawable.error); // Hoặc placeholder
        }
    }

    @Override
    public int getItemCount() {
        return projectList == null ? 0 : projectList.size();
    }

    public void setData(List<Project> newList) {
        this.projectList.clear();
        if (newList != null) {
            this.projectList.addAll(newList);
        }
        notifyDataSetChanged();
    }

    // Quan trọng: Dọn dẹp ExecutorService khi Adapter không còn được sử dụng
    // (ví dụ, trong Fragment/Activity's onDestroy hoặc khi RecyclerView bị detach)
    // Tuy nhiên, việc này hơi phức tạp vì Adapter không có lifecycle trực tiếp.
    // Một cách tốt hơn là truyền ExecutorService từ bên ngoài vào.
    // Hoặc, nếu ExecutorService chỉ dùng cho Adapter này, bạn có thể tạo một phương thức
    // để shutdown nó, và gọi từ Fragment/Activity khi thích hợp.
    public void shutdownExecutor() {
        if (executorService != null && !executorService.isShutdown()) {
            executorService.shutdown();
        }
    }


    static class ProjectViewHolder extends RecyclerView.ViewHolder {
        ImageView imageViewProject;
        TextView textViewProjectName;
        TextView textViewDescription;
        TextView textViewAuthor;
        TextView textViewTechnology;

        public ProjectViewHolder(@NonNull View itemView) {
            super(itemView);
            imageViewProject = itemView.findViewById(R.id.imageViewProject);
            textViewProjectName = itemView.findViewById(R.id.textViewProjectName);
            textViewDescription = itemView.findViewById(R.id.textViewDescription);
            textViewAuthor = itemView.findViewById(R.id.textViewAuthor);
            textViewTechnology = itemView.findViewById(R.id.textViewTechnology);
        }
    }
}