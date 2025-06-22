// ViewPagerAdapter.java
package com.cse441.tluprojectexpo;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentStatePagerAdapter;

// Bỏ import CreateFragment
// import com.cse441.tluprojectexpo.ui.createproject.CreateFragment;
import com.cse441.tluprojectexpo.ui.Home.HomeFragment;
import com.cse441.tluprojectexpo.ui.Notification.NotificationFragment;
import com.cse441.tluprojectexpo.ui.Profile.ProfileFragment;

public class ViewPagerAdapter extends FragmentStatePagerAdapter {
    public ViewPagerAdapter(@NonNull FragmentManager fm, int behavior) {
        super(fm, behavior);
    }

    @NonNull
    @Override
    public Fragment getItem(int position) {
        switch (position) {
            case 0:
                return new HomeFragment();
            // Bỏ case 1 (CreateFragment)
            // case 1:
            //     return new CreateFragment();
            case 1: // Trước là case 2, giờ là NotificationFragment
                return new NotificationFragment();
            case 2: // Trước là case 3, giờ là ProfileFragment
                return new ProfileFragment();
            default:
                return new HomeFragment(); // Mặc định vẫn là Home
        }
    }

    @Override
    public int getCount() {
        return 3; // THAY ĐỔI: Giảm số lượng tab xuống còn 3
    }
}