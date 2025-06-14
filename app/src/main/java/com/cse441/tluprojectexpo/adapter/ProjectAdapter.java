package com.cse441.tluprojectexpo.adapter;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.Filter;
import android.widget.Filterable;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.cse441.tluprojectexpo.R;
import com.cse441.tluprojectexpo.model.Project;

import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class ProjectAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> implements Filterable {

    private List<Project> projectList;
    private List<Project> projectListFull; // For filtering
    private Context context;
    private OnProjectActionListener actionListener;

    private final ExecutorService executorService = Executors.newFixedThreadPool(Runtime.getRuntime().availableProcessors());
    private final Handler mainThreadHandler = new Handler(Looper.getMainLooper());

    private static final int VIEW_TYPE_ITEM_DEFAULT = 0; // For HomeFragment
    private static final int VIEW_TYPE_ITEM_WITH_ACTIONS = 1; // For ProfileFragment

    public interface OnProjectActionListener {
        void onEditProject(Project project);
        void onDeleteProject(Project project, int position);
    }

    // Constructor for HomeFragment (no actions)
    public ProjectAdapter(Context context, List<Project> projectList) {
        this.context = context;
        this.projectList = projectList;
        this.projectListFull = new ArrayList<>(projectList);
        this.actionListener = null;
    }

    // Constructor for ProfileFragment (with actions)
    public ProjectAdapter(Context context, List<Project> projectList, OnProjectActionListener listener) {
        this.context = context;
        this.projectList = projectList;
        this.projectListFull = new ArrayList<>(projectList);
        this.actionListener = listener;
    }

    @Override
    public int getItemViewType(int position) {
        return actionListener == null ? VIEW_TYPE_ITEM_DEFAULT : VIEW_TYPE_ITEM_WITH_ACTIONS;
    }

    @NonNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        LayoutInflater inflater = LayoutInflater.from(parent.getContext());
        if (viewType == VIEW_TYPE_ITEM_WITH_ACTIONS) {
            View view = inflater.inflate(R.layout.list_item_project_actions, parent, false);
            return new ProjectActionViewHolder(view);
        } else {
            // Assuming list_item.xml is for HomeFragment and might have author/technology
            View view = inflater.inflate(R.layout.list_item, parent, false);
            return new ProjectDefaultViewHolder(view);
        }
    }

    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
        Project project = projectList.get(position);
        if (project == null) return;

        if (holder.getItemViewType() == VIEW_TYPE_ITEM_WITH_ACTIONS) {
            ProjectActionViewHolder actionViewHolder = (ProjectActionViewHolder) holder;
            actionViewHolder.textViewProjectName.setText(project.getName());
            actionViewHolder.textViewDescription.setText(project.getDescription());
            loadImage(project.getImageUrl(), actionViewHolder.imageViewProject, project.getName());

            actionViewHolder.buttonEdit.setOnClickListener(v -> {
                if (actionListener != null) {
                    actionListener.onEditProject(project);
                }
            });

            actionViewHolder.buttonDelete.setOnClickListener(v -> {
                if (actionListener != null) {
                    actionListener.onDeleteProject(project, holder.getAdapterPosition());
                }
            });

        } else { // VIEW_TYPE_ITEM_DEFAULT
            ProjectDefaultViewHolder defaultViewHolder = (ProjectDefaultViewHolder) holder;
            defaultViewHolder.textViewProjectName.setText(project.getName());
            defaultViewHolder.textViewDescription.setText(project.getDescription());
            // Assuming list_item.xml has these. If not, add null checks or remove.
            if (defaultViewHolder.textViewAuthor != null) defaultViewHolder.textViewAuthor.setText(project.getAuthor());
            if (defaultViewHolder.textViewTechnology != null) defaultViewHolder.textViewTechnology.setText(project.getTechnology());
            loadImage(project.getImageUrl(), defaultViewHolder.imageViewProject, project.getName());
        }
    }

    private void loadImage(String imageUrl, ImageView imageView, String projectName) {
        imageView.setImageResource(R.drawable.ic_image_placeholder); // Default placeholder

        if (imageUrl != null && !imageUrl.isEmpty()) {
            executorService.execute(() -> {
                Bitmap bitmap = null;
                HttpURLConnection connection = null;
                InputStream input = null;
                try {
                    URL url = new URL(imageUrl);
                    connection = (HttpURLConnection) url.openConnection();
                    connection.setConnectTimeout(10000); // 10 seconds
                    connection.setReadTimeout(15000); // 15 seconds
                    connection.setDoInput(true);
                    connection.connect();
                    input = connection.getInputStream();
                    bitmap = BitmapFactory.decodeStream(input);
                } catch (OutOfMemoryError oom) {
                    Log.e("ProjectAdapter", "OutOfMemoryError for image: " + imageUrl, oom);
                    // Consider downsampling or other memory optimization techniques
                } catch (Exception e) {
                    Log.e("ProjectAdapter", "Error downloading image: " + imageUrl, e);
                } finally {
                    if (input != null) try { input.close(); } catch (Exception e) { /* ignore */ }
                    if (connection != null) connection.disconnect();
                }

                final Bitmap finalBitmap = bitmap;
                mainThreadHandler.post(() -> {
                    // Check if the imageView instance is still valid (ViewHolder might have been recycled)
                    // This check is often implicitly handled by RecyclerView's recycling if an image loading library is used.
                    // For manual loading, it's good practice.
                    if (imageView.getTag() == null || imageView.getTag().equals(imageUrl)) { // Basic tag check
                        if (finalBitmap != null) {
                            imageView.setImageBitmap(finalBitmap);
                        } else {
                            imageView.setImageResource(R.drawable.error); // Error placeholder
                        }
                    }
                });
            });
            imageView.setTag(imageUrl); // Set tag to help with recycled views
        } else {
            Log.d("ProjectAdapter", "Image URL is empty or null for project: " + projectName);
            imageView.setImageResource(R.drawable.error); // Error placeholder if URL is invalid
        }
    }


    @Override
    public int getItemCount() {
        return projectList == null ? 0 : projectList.size();
    }

    public void updateProjects(List<Project> newProjects) {
        this.projectList.clear();
        this.projectList.addAll(newProjects);
        this.projectListFull = new ArrayList<>(newProjects);
        notifyDataSetChanged();
    }

    public void removeProject(int position) {
        if (position >= 0 && position < projectList.size()) {
            Project removed = projectList.remove(position);
            projectListFull.remove(removed); // Ensure consistency with the full list
            notifyItemRemoved(position);
            notifyItemRangeChanged(position, projectList.size()); // To update subsequent item positions
        }
    }

    public void shutdownExecutor() {
        if (executorService != null && !executorService.isShutdown()) {
            executorService.shutdown();
        }
    }

    // ViewHolder for list_item.xml (HomeFragment)
    static class ProjectDefaultViewHolder extends RecyclerView.ViewHolder {
        ImageView imageViewProject;
        TextView textViewProjectName;
        TextView textViewDescription;
        TextView textViewAuthor;
        TextView textViewTechnology;

        public ProjectDefaultViewHolder(@NonNull View itemView) {
            super(itemView);
            imageViewProject = itemView.findViewById(R.id.imageViewProject);
            textViewProjectName = itemView.findViewById(R.id.textViewProjectName);
            textViewDescription = itemView.findViewById(R.id.textViewDescription);
            textViewAuthor = itemView.findViewById(R.id.textViewAuthor); // Ensure this ID exists in list_item.xml
            textViewTechnology = itemView.findViewById(R.id.textViewTechnology); // Ensure this ID exists in list_item.xml
        }
    }

    // ViewHolder for list_item_project_actions.xml (ProfileFragment)
    static class ProjectActionViewHolder extends RecyclerView.ViewHolder {
        ImageView imageViewProject;
        TextView textViewProjectName;
        TextView textViewDescription;
        Button buttonEdit;
        Button buttonDelete;

        public ProjectActionViewHolder(@NonNull View itemView) {
            super(itemView);
            imageViewProject = itemView.findViewById(R.id.imageViewProject);
            textViewProjectName = itemView.findViewById(R.id.textViewProjectName);
            textViewDescription = itemView.findViewById(R.id.textViewDescription);
            buttonEdit = itemView.findViewById(R.id.buttonEdit);
            buttonDelete = itemView.findViewById(R.id.buttonDelete);
        }
    }

    @Override
    public Filter getFilter() {
        return projectFilter;
    }

    private final Filter projectFilter = new Filter() {
        @Override
        protected FilterResults performFiltering(CharSequence constraint) {
            List<Project> filteredList = new ArrayList<>();
            if (constraint == null || constraint.length() == 0) {
                filteredList.addAll(projectListFull);
            } else {
                String filterPattern = constraint.toString().toLowerCase().trim();
                for (Project item : projectListFull) {
                    if (item.getName() != null && item.getName().toLowerCase().contains(filterPattern)) {
                        filteredList.add(item);
                    } else if (item.getDescription() != null && item.getDescription().toLowerCase().contains(filterPattern)) {
                        filteredList.add(item);
                    }
                    // Add more fields to search if needed
                }
            }
            FilterResults results = new FilterResults();
            results.values = filteredList;
            return results;
        }

        @Override
        protected void publishResults(CharSequence constraint, FilterResults results) {
            projectList.clear();
            if (results.values != null) {
                projectList.addAll((List<Project>) results.values);
            }
            notifyDataSetChanged();
        }
    };

    // Helper method to trigger filtering
    public void filter(String query) {
        getFilter().filter(query);
    }
}