package com.cse441.tluprojectexpo.admin.repository;

import android.util.Log;

import androidx.lifecycle.MutableLiveData;

import com.cse441.tluprojectexpo.model.Role;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.ArrayList;
import java.util.List;

public class RoleRepository {
    private final FirebaseFirestore db = FirebaseFirestore.getInstance();
    private final CollectionReference rolesRef = db.collection("Roles");

    public MutableLiveData<List<Role>> getAllRoles() {
        MutableLiveData<List<Role>> rolesLiveData = new MutableLiveData<>(); //MutableLiveData chỉ cập nhật UI khi thành phần đó ở trạng thái hoạt động(started, resumed)

        rolesRef.get().addOnCompleteListener(task -> {
            if (task.isSuccessful()) {
                List<Role> roleList = new ArrayList<>();
                for (QueryDocumentSnapshot document : task.getResult()) {
                    Role role = document.toObject(Role.class);
                    // Quan trọng: Gán ID của document vào đối tượng Role
                    role.setRoleId(document.getId());
                    roleList.add(role);
                }
                rolesLiveData.setValue(roleList);
            } else {
                Log.e("RoleRepository", "Error getting roles: ", task.getException());
                rolesLiveData.setValue(null); // Hoặc một danh sách rỗng
            }
        });
        return rolesLiveData;
    }
}
