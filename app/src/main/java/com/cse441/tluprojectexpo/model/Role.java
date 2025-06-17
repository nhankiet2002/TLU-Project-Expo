package com.cse441.tluprojectexpo.model;

import com.google.firebase.firestore.DocumentId;

public class Role {
    @DocumentId
    private String roleId;
    private String roleName;

    public Role(){}

    public String getRoleId() {
        return roleId;
    }

    public void setRoleId(String roleId) {
        this.roleId = roleId;
    }

    public String getRoleName() {
        return roleName;
    }

    public void setRoleName(String roleName) {
        this.roleName = roleName;
    }
}