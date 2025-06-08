package com.cse441.tluprojectexpo.model;

import com.google.firebase.firestore.DocumentId;

public class Category {
    @DocumentId
    private String id;
    private String name;

    // Firestore yêu cầu một constructor không tham số
    public Category() {}

    public Category(String name) {
        this.name = name;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }
}
