// MainActivity.java
package com.cse441.tluprojectexpo;

import android.content.Intent; // THÊM
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

import com.cse441.tluprojectexpo.ui.createproject.CreateProjectActivity; // THÊM: Import Activity mới
import com.cse441.tluprojectexpo.ui.Home.HomeFragment;
import com.google.android.material.bottomnavigation.BottomNavigationView;

public class MainActivity extends AppCompatActivity implements HomeFragment.OnScrollInteractionListener {

    private ViewPager viewPager;
    private BottomNavigationView bottomNavigationView;
    private boolean isBottomNavVisible = true;
    private int bottomNavHeight = 0;
    private int previouslySelectedViewPagerItem = 0; // Để lưu lại tab trước khi chuyển sang CreateProjectActivity

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

        ViewPagerAdapter adapter = new ViewPagerAdapter(getSupportFragmentManager(), FragmentStatePagerAdapter.BEHAVIOR_RESUME_ONLY_CURRENT_FRAGMENT);
        viewPager.setAdapter(adapter);

        viewPager.addOnPageChangeListener(new ViewPager.OnPageChangeListener() {
            @Override
            public void onPageScrolled(int position, float positionOffset, int positionOffsetPixels) {}

            @Override
            public void onPageSelected(int position){
                // THAY ĐỔI: Logic ẩn/hiện BottomNav giờ không cần cho CreateFragment nữa
                // vì nó là Activity riêng.
                // Nếu bạn vẫn muốn ẩn BottomNav cho các Fragment khác, giữ lại logic đó.
                // Ví dụ, nếu HomeFragment (position 0) có thể ẩn/hiện BottomNav khi cuộn.
                if (position == 0) { // Chỉ áp dụng cho HomeFragment nếu cần
                    // showBottomNavForPageChange(); // Hoặc logic ẩn/hiện dựa trên trạng thái cuộn
                } else {
                    // showBottomNavForPageChange(); // Hiện cho các trang khác
                }

                // Cập nhật item được chọn trên BottomNavigationView
                // Chú ý: Index của ViewPager và thứ tự MenuItem có thể khác nhau sau khi bỏ CreateFragment
                switch (position){
                    case 0: // HomeFragment
                        bottomNavigationView.getMenu().findItem(R.id.menu_home).setChecked(true);
                        break;
                    // menu_create sẽ được xử lý riêng
                    case 1: // NotificationFragment (trước là vị trí 2)
                        bottomNavigationView.getMenu().findItem(R.id.menu_notification).setChecked(true);
                        break;
                    case 2: // ProfileFragment (trước là vị trí 3)
                        bottomNavigationView.getMenu().findItem(R.id.menu_profile).setChecked(true);
                        break;
                }
                previouslySelectedViewPagerItem = position; // Lưu lại tab hiện tại của ViewPager
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
                // THAY ĐỔI: Khởi động CreateProjectActivity
                Intent intent = new Intent(MainActivity.this, CreateProjectActivity.class);
                startActivity(intent);
                // Không return true ngay lập tức nếu bạn muốn giữ item "Create" không được chọn
                // hoặc bạn có thể chọn lại item trước đó sau khi CreateProjectActivity đóng.
                // Tạm thời, không làm gì với ViewPager ở đây.
                return false; // Return false để item không được đánh dấu là selected cố định
                // Vì chúng ta đang chuyển Activity
            } else if (itemId == R.id.menu_notification) {
                viewPager.setCurrentItem(1); // Index mới cho Notification
                return true;
            } else if (itemId == R.id.menu_profile) {
                viewPager.setCurrentItem(2); // Index mới cho Profile
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
        // Khi quay lại MainActivity từ CreateProjectActivity,
        // đặt lại item được chọn trên BottomNavigationView về tab trước đó của ViewPager
        // hoặc tab Home nếu không có thông tin.
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
                // Nếu có lỗi, mặc định chọn Home
                bottomNavigationView.getMenu().findItem(R.id.menu_home).setChecked(true);
            }
        }
        // Đảm bảo BottomNav hiển thị khi quay lại từ CreateProjectActivity
        showBottomNavForPageChange();
    }


    private void adjustViewPagerMargin(boolean isBottomNavEffectivelyHidden) {
        // ... (Giữ nguyên logic)
        if (viewPager == null || (bottomNavHeight == 0 && !isBottomNavEffectivelyHidden) ) {
            if (!isBottomNavEffectivelyHidden) return;
        }
        ViewGroup.MarginLayoutParams params = (ViewGroup.MarginLayoutParams) viewPager.getLayoutParams();
        if (isBottomNavEffectivelyHidden) {
            params.bottomMargin = 0;
        } else {
            params.bottomMargin = bottomNavHeight;
        }
        viewPager.setLayoutParams(params);
        viewPager.requestLayout();
    }

    private void hideBottomNavForPageChange() {
        // ... (Giữ nguyên logic)
        if (isBottomNavVisible) {
            bottomNavigationView.setVisibility(View.GONE);
            isBottomNavVisible = false;
            adjustViewPagerMargin(true);
        }
    }

    private void showBottomNavForPageChange() {
        // ... (Giữ nguyên logic)
        if (!isBottomNavVisible) {
            bottomNavigationView.setVisibility(View.VISIBLE);
            bottomNavigationView.setTranslationY(0);
            isBottomNavVisible = true;
            adjustViewPagerMargin(false);
        }
    }

    @Override
    public void onScrollUp() {
        // ... (Giữ nguyên logic, nhưng đảm bảo getCurrentItem() không bị lỗi index)
        if (viewPager.getAdapter() != null && viewPager.getCurrentItem() < viewPager.getAdapter().getCount() &&
                viewPager.getCurrentItem() == 0 && isBottomNavVisible) { // Chỉ áp dụng cho Home
            isBottomNavVisible = false;
            bottomNavigationView.animate()
                    .translationY(bottomNavHeight)
                    .setInterpolator(new AccelerateInterpolator(2))
                    .setDuration(200)
                    .withEndAction(() -> {
                        adjustViewPagerMargin(true);
                    })
                    .start();
        }
    }

    @Override
    public void onScrollDown() {
        // ... (Giữ nguyên logic)
        if (viewPager.getAdapter() != null && viewPager.getCurrentItem() < viewPager.getAdapter().getCount() &&
                viewPager.getCurrentItem() == 0 && !isBottomNavVisible) { // Chỉ áp dụng cho Home
            isBottomNavVisible = true;
            adjustViewPagerMargin(false);
            bottomNavigationView.animate()
                    .translationY(0)
                    .setInterpolator(new DecelerateInterpolator(2))
                    .setDuration(200)
                    .start();
        }
    }
}