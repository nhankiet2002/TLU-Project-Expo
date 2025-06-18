// PATH: com/cse441/tluprojectexpo/model/Category.java

package com.cse441.tluprojectexpo.model;

import com.google.firebase.firestore.DocumentId;
import com.google.firebase.firestore.PropertyName;

public class Category {
    @DocumentId
    private String id;

    // Annotation này báo cho Firestore biết: tìm trường "Name" (viết hoa)
    // trên CSDL và gán giá trị của nó vào biến "name" (viết thường) này.
    @PropertyName("Name")
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