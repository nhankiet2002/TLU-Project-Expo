package com.cse441.tluprojectexpo.model;

import com.google.firebase.firestore.DocumentId;
import com.google.firebase.firestore.PropertyName;

import java.io.Serializable;

public class Role implements Serializable {
    @DocumentId
    private String roleId;

    @PropertyName("RoleName")
    private String roleName;

    public Role(){}

    public Role(String roleName){
        this.roleName = roleName;
    }



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