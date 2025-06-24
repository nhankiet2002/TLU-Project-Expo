package com.cse441.tluprojectexpo.ui.common.uimanager;

import android.content.Context;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.ImageView;
import android.widget.LinearLayout;

import com.cse441.tluprojectexpo.utils.UiHelper;
import com.cse441.tluprojectexpo.R;
import com.cse441.tluprojectexpo.model.LinkItem;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;
import java.util.List;

public class AddedLinksUiManager {
    private Context context;
    private LayoutInflater inflater;
    private LinearLayout container;
    private List<LinkItem> projectLinks;
    private String[] platformItems;
    private OnLinkInteractionListener listener;

    public interface OnLinkInteractionListener {
        void onLinkRemoved(LinkItem linkItem, int index);
        void onLinkUrlChanged(LinkItem linkItem, String newUrl, int index);
        void onLinkPlatformChanged(LinkItem linkItem, String newPlatform, int index);
    }

    public AddedLinksUiManager(Context context, LinearLayout container,
                               List<LinkItem> projectLinks, OnLinkInteractionListener listener) {
        this.context = context;
        this.inflater = LayoutInflater.from(context);
        this.container = container;
        this.projectLinks = projectLinks;
        this.platformItems = context.getResources().getStringArray(R.array.link_platforms);
        this.listener = listener;
    }

    public void updateUI() {
        if (container == null) return;
        container.removeAllViews();
        if (projectLinks.isEmpty()) {
            container.setVisibility(View.GONE);
            return;
        }
        container.setVisibility(View.VISIBLE);

        for (int i = 0; i < projectLinks.size(); i++) {
            final int linkIndex = i;
            LinkItem currentLinkItem = projectLinks.get(linkIndex);

            View linkView = inflater.inflate(R.layout.item_added_link, container, false);
            TextInputEditText etLinkUrl = linkView.findViewById(R.id.et_added_link_url);
            AutoCompleteTextView actvPlatform = linkView.findViewById(R.id.actv_link_platform);
            ImageView ivRemoveLink = linkView.findViewById(R.id.iv_remove_link);
            TextInputLayout tilPlatform = linkView.findViewById(R.id.til_link_platform);

            etLinkUrl.setText(currentLinkItem.getUrl());
            ArrayAdapter<String> platformAdapter = new ArrayAdapter<>(context,
                    android.R.layout.simple_dropdown_item_1line, platformItems);
            actvPlatform.setAdapter(platformAdapter);

            if (currentLinkItem.getPlatform() != null && !currentLinkItem.getPlatform().isEmpty()) {
                actvPlatform.setText(currentLinkItem.getPlatform(), false);
            } else if (platformItems.length > 0) {
                actvPlatform.setText(platformItems[0], false);
                if (listener != null) listener.onLinkPlatformChanged(currentLinkItem, platformItems[0], linkIndex);
            }

            actvPlatform.setOnItemClickListener((parent, MView, position, id) -> {
                if (linkIndex < projectLinks.size() && listener != null) {
                    listener.onLinkPlatformChanged(projectLinks.get(linkIndex), parent.getItemAtPosition(position).toString(), linkIndex);
                }
            });
            if (tilPlatform != null) {
                UiHelper.setupDropdownToggle(tilPlatform, actvPlatform);
            }

            etLinkUrl.addTextChangedListener(new TextWatcher() {
                @Override public void beforeTextChanged(CharSequence s, int st, int c, int a) {}
                @Override public void onTextChanged(CharSequence s, int st, int b, int c) {
                    if (linkIndex < projectLinks.size() && listener != null) {
                        listener.onLinkUrlChanged(projectLinks.get(linkIndex), s.toString(), linkIndex);
                    }
                }
                @Override public void afterTextChanged(Editable s) {}
            });

            ivRemoveLink.setOnClickListener(v_remove -> {
                if (linkIndex < projectLinks.size() && listener != null) {
                    listener.onLinkRemoved(projectLinks.get(linkIndex), linkIndex);
                }
            });
            container.addView(linkView);
        }
    }
} 