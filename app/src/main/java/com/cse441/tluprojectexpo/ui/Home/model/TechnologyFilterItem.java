package com.cse441.tluprojectexpo.ui.Home.model;

import androidx.annotation.NonNull;

public class TechnologyFilterItem implements FilterableItem {
    private String id;
    private String name;

    public TechnologyFilterItem(String id, String name) {
        this.id = id;
        this.name = name;
    }

    @Override
    public String getId() {
        return id;
    }

    @Override
    public String getName() {
        return name;
    }

    @NonNull
    @Override
    public String toString() {
        return name; // Quan trọng cho ArrayAdapter trong AlertDialog
    }
}