package com.cse441.tluprojectexpo.model;

import com.google.firebase.firestore.DocumentId;
import com.google.firebase.firestore.PropertyName;

public class Role {
    @DocumentId
    private String roleId;

    @PropertyName("RoleName")
    private String roleName;

    public Role(){}

    public String getRoleId() {
        return roleId;
    }

    public void setRoleId(String roleId) {
        this.roleId = roleId;
    }

    @PropertyName("RoleName")
    public String getRoleName() {
        return roleName;
    }

    @PropertyName("RoleName")
    public void setRoleName(String roleName) {
        this.roleName = roleName;
    }
}