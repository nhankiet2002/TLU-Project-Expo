// MainActivity.java
package com.cse441.tluprojectexpo;

import android.content.Intent;
import android.os.Bundle;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.AccelerateInterpolator;
import android.view.animation.DecelerateInterpolator;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;

import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.fragment.app.FragmentStatePagerAdapter;
import androidx.viewpager.widget.ViewPager;

import com.cse441.tluprojectexpo.ui.createproject.CreateProjectActivity;
// SỬA: Import interface OnScrollInteractionListener từ package đúng của nó
import com.cse441.tluprojectexpo.ui.Home.listener.OnScrollInteractionListener;
// Bỏ import HomeFragment nếu không cần trực tiếp (chỉ cần interface)
// import com.cse441.tluprojectexpo.ui.Home.HomeFragment;
import com.google.android.material.bottomnavigation.BottomNavigationView;

// SỬA: Implement interface đã import
public class MainActivity extends AppCompatActivity implements OnScrollInteractionListener {

    private ViewPager viewPager;
    private BottomNavigationView bottomNavigationView;
    private boolean isBottomNavVisible = true;
    private int bottomNavHeight = 0;
    private int previouslySelectedViewPagerItem = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_main);

        viewPager = findViewById(R.id.view_pager);
        bottomNavigationView = findViewById(R.id.bottom_navigation);

        bottomNavigationView.post(() -> {
            bottomNavHeight = bottomNavigationView.getHeight();
            if (bottomNavigationView.getVisibility() == View.VISIBLE) {
                adjustViewPagerMargin(false);
            } else {
                adjustViewPagerMargin(true);
            }
        });

        // Đảm bảo ViewPagerAdapter được khai báo và sử dụng đúng cách
        // Nếu ViewPagerAdapter nằm trong package khác, cần import.
        // Giả sử ViewPagerAdapter nằm cùng package với MainActivity hoặc được import đúng
        ViewPagerAdapter adapter = new ViewPagerAdapter(getSupportFragmentManager(), FragmentStatePagerAdapter.BEHAVIOR_RESUME_ONLY_CURRENT_FRAGMENT);
        viewPager.setAdapter(adapter);

        viewPager.addOnPageChangeListener(new ViewPager.OnPageChangeListener() {
            @Override
            public void onPageScrolled(int position, float positionOffset, int positionOffsetPixels) {}

            @Override
            public void onPageSelected(int position){
                switch (position){
                    case 0:
                        bottomNavigationView.getMenu().findItem(R.id.menu_home).setChecked(true);
                        break;
                    case 1:
                        bottomNavigationView.getMenu().findItem(R.id.menu_notification).setChecked(true);
                        break;
                    case 2:
                        bottomNavigationView.getMenu().findItem(R.id.menu_profile).setChecked(true);
                        break;
                }
                previouslySelectedViewPagerItem = position;
            }

            @Override
            public void onPageScrollStateChanged(int state) {}
        });

        bottomNavigationView.setOnNavigationItemSelectedListener(item -> {
            int itemId = item.getItemId();
            if (itemId == R.id.menu_home) {
                viewPager.setCurrentItem(0);
                return true;
            } else if (itemId == R.id.menu_create) {
                Intent intent = new Intent(MainActivity.this, CreateProjectActivity.class);
                startActivity(intent);
                return false;
            } else if (itemId == R.id.menu_notification) {
                viewPager.setCurrentItem(1);
                return true;
            } else if (itemId == R.id.menu_profile) {
                viewPager.setCurrentItem(2);
                return true;
            }
            return false;
        });

        ViewCompat.setOnApplyWindowInsetsListener(bottomNavigationView, (v, windowInsets) -> {
            Insets insets = windowInsets.getInsets(WindowInsetsCompat.Type.systemBars() | WindowInsetsCompat.Type.ime());
            v.setPadding(v.getPaddingLeft(), v.getPaddingTop(), v.getPaddingRight(), insets.bottom);
            return WindowInsetsCompat.CONSUMED;
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (viewPager != null && bottomNavigationView != null) {
            int currentViewPagerItem = viewPager.getCurrentItem();
            MenuItem menuItemToSelect = null;
            switch (currentViewPagerItem) {
                case 0: menuItemToSelect = bottomNavigationView.getMenu().findItem(R.id.menu_home); break;
                case 1: menuItemToSelect = bottomNavigationView.getMenu().findItem(R.id.menu_notification); break;
                case 2: menuItemToSelect = bottomNavigationView.getMenu().findItem(R.id.menu_profile); break;
            }
            if (menuItemToSelect != null) {
                menuItemToSelect.setChecked(true);
            } else {
                bottomNavigationView.getMenu().findItem(R.id.menu_home).setChecked(true);
            }
        }
        showBottomNavForPageChange();
    }


    private void adjustViewPagerMargin(boolean isBottomNavEffectivelyHidden) {
        if (viewPager == null || (bottomNavHeight == 0 && !isBottomNavEffectivelyHidden) ) {
            if (!isBottomNavEffectivelyHidden) return; // Chỉ return nếu bottomNavHeight=0 VÀ isBottomNavEffectivelyHidden=false
        }
        ViewGroup.MarginLayoutParams params = (ViewGroup.MarginLayoutParams) viewPager.getLayoutParams();
        if (isBottomNavEffectivelyHidden) {
            params.bottomMargin = 0;
        } else {
            params.bottomMargin = bottomNavHeight;
        }
        viewPager.setLayoutParams(params);
        // viewPager.requestLayout(); // Có thể không cần thiết nếu setLayoutParams đã trigger layout pass
    }


    private void hideBottomNavForPageChange() {
        if (isBottomNavVisible) {
            bottomNavigationView.setVisibility(View.GONE); // Ẩn ngay lập tức
            isBottomNavVisible = false;
            adjustViewPagerMargin(true);
        }
    }

    private void showBottomNavForPageChange() {
        if (!isBottomNavVisible) {
            bottomNavigationView.setVisibility(View.VISIBLE); // Hiện ngay lập tức
            bottomNavigationView.setTranslationY(0); // Đảm bảo nó ở đúng vị trí
            isBottomNavVisible = true;
            adjustViewPagerMargin(false);
        }
    }

    // GHI ĐÈ onScrollUp TỪ INTERFACE
    @Override
    public void onScrollUp() {
        if (viewPager.getAdapter() != null && viewPager.getCurrentItem() < viewPager.getAdapter().getCount() &&
                viewPager.getCurrentItem() == 0 && isBottomNavVisible) { // Chỉ áp dụng cho HomeFragment (index 0)
            isBottomNavVisible = false;
            bottomNavigationView.animate()
                    .translationY(bottomNavHeight)
                    .setInterpolator(new AccelerateInterpolator(2))
                    .setDuration(200)
                    .withEndAction(() -> {
                        // Không cần setVisibility(View.GONE) nữa vì adjustViewPagerMargin
                        // sẽ xử lý không gian dựa trên bottomNavHeight
                        adjustViewPagerMargin(true);
                    })
                    .start();
        }
    }

    // GHI ĐÈ onScrollDown TỪ INTERFACE
    @Override
    public void onScrollDown() {
        if (viewPager.getAdapter() != null && viewPager.getCurrentItem() < viewPager.getAdapter().getCount() &&
                viewPager.getCurrentItem() == 0 && !isBottomNavVisible) { // Chỉ áp dụng cho HomeFragment (index 0)
            isBottomNavVisible = true;
            adjustViewPagerMargin(false); // Điều chỉnh margin trước khi animation
            bottomNavigationView.setVisibility(View.VISIBLE); // Đảm bảo nó hiện ra trước khi animate
            bottomNavigationView.animate()
                    .translationY(0)
                    .setInterpolator(new DecelerateInterpolator(2))
                    .setDuration(200)
                    .start();
        }
    }
}