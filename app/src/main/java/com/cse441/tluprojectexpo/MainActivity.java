package com.cse441.tluprojectexpo;

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
// import androidx.coordinatorlayout.widget.CoordinatorLayout; // Không cần nếu root là RelativeLayout

import com.cse441.tluprojectexpo.fragment.HomeFragment;
import com.cse441.tluprojectexpo.adapter.ViewPagerAdapter;
import com.google.android.material.bottomnavigation.BottomNavigationView;

public class MainActivity extends AppCompatActivity implements HomeFragment.OnScrollInteractionListener {

    private ViewPager viewPager;
    private BottomNavigationView bottomNavigationView;
    private boolean isBottomNavVisible = true;
    private int bottomNavHeight = 0;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_main);

        viewPager = findViewById(R.id.view_pager);
        bottomNavigationView = findViewById(R.id.bottom_navigation);

        bottomNavigationView.post(() -> {
            bottomNavHeight = bottomNavigationView.getHeight();
            // Thiết lập margin ban đầu cho ViewPager dựa trên trạng thái của BottomNav
            if (bottomNavigationView.getVisibility() == View.VISIBLE) {
                adjustViewPagerMargin(false); // isBottomNavHidden = false
            } else {
                adjustViewPagerMargin(true);  // isBottomNavHidden = true
            }
        });

        ViewPagerAdapter adapter = new ViewPagerAdapter(getSupportFragmentManager(), FragmentStatePagerAdapter.BEHAVIOR_RESUME_ONLY_CURRENT_FRAGMENT);
        viewPager.setAdapter(adapter);

        viewPager.addOnPageChangeListener(new ViewPager.OnPageChangeListener() {
            @Override
            public void onPageScrolled(int position, float positionOffset, int positionOffsetPixels) {}

            @Override
            public void onPageSelected(int position){
                if (position == 1) { // Trang Create ẩn BottomNav
                    hideBottomNavForPageChange();
                } else { // Các trang khác hiện BottomNav
                    showBottomNavForPageChange();
                }

                switch (position){
                    case 0:
                        bottomNavigationView.getMenu().findItem(R.id.menu_home).setChecked(true);
                        break;
                    case 1:
                        MenuItem createItem = bottomNavigationView.getMenu().findItem(R.id.menu_create);
                        if (createItem != null) {
                            createItem.setChecked(true);
                        }
                        break;
                    case 2:
                        bottomNavigationView.getMenu().findItem(R.id.menu_profile).setChecked(true);
                        break;
                }
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
                viewPager.setCurrentItem(1);
                return true;
            } else if (itemId == R.id.menu_profile) {
                viewPager.setCurrentItem(2);
                return true;
            }
            return false;
        });

        // Xử lý trạng thái hiển thị ban đầu trong bottomNavigationView.post() ở trên

        ViewCompat.setOnApplyWindowInsetsListener(bottomNavigationView, (v, windowInsets) -> {
            Insets insets = windowInsets.getInsets(WindowInsetsCompat.Type.systemBars() | WindowInsetsCompat.Type.ime());
            v.setPadding(v.getPaddingLeft(), v.getPaddingTop(), v.getPaddingRight(), insets.bottom);
            return WindowInsetsCompat.CONSUMED;
        });
    }

    private void adjustViewPagerMargin(boolean isBottomNavEffectivelyHidden) {
        if (viewPager == null || (bottomNavHeight == 0 && !isBottomNavEffectivelyHidden) ) {
            // Nếu bottomNavHeight chưa được tính và chúng ta cần nó (khi bottomNav không ẩn), thì đợi
            // Tuy nhiên, nếu bottomNav ẩn, chúng ta có thể đặt margin là 0 ngay cả khi height chưa có.
            if (!isBottomNavEffectivelyHidden) return;
        }

        ViewGroup.MarginLayoutParams params = (ViewGroup.MarginLayoutParams) viewPager.getLayoutParams();
        if (isBottomNavEffectivelyHidden) {
            params.bottomMargin = 0;
        } else {
            params.bottomMargin = bottomNavHeight; // Sử dụng chiều cao đã lưu
        }
        viewPager.setLayoutParams(params);
        viewPager.requestLayout(); // Yêu cầu tính toán lại layout
    }

    private void hideBottomNavForPageChange() {
        if (isBottomNavVisible) { // Chỉ thực hiện nếu nó đang hiện
            bottomNavigationView.setVisibility(View.GONE);
            isBottomNavVisible = false;
            adjustViewPagerMargin(true);
        }
    }

    private void showBottomNavForPageChange() {
        if (!isBottomNavVisible) { // Chỉ thực hiện nếu nó đang ẩn
            bottomNavigationView.setVisibility(View.VISIBLE);
            bottomNavigationView.setTranslationY(0); // Đảm bảo nó ở vị trí đúng
            isBottomNavVisible = true;
            adjustViewPagerMargin(false);
        }
    }

    @Override
    public void onScrollUp() { // Người dùng cuộn nội dung lên, BottomNav ẩn
        if (viewPager.getCurrentItem() == 0 && isBottomNavVisible) {
            isBottomNavVisible = false; // Đặt cờ ngay để tránh gọi lại animation
            bottomNavigationView.animate()
                    .translationY(bottomNavHeight) // Dùng chiều cao đã lưu
                    .setInterpolator(new AccelerateInterpolator(2))
                    .setDuration(200)
                    .withEndAction(() -> {
                        // Không cần setVisibility(View.GONE) nếu bạn muốn nó chỉ "trượt" đi
                        // Nhưng nếu muốn ViewPager chiếm không gian đó, cần adjust margin
                        adjustViewPagerMargin(true);
                    })
                    .start();
        }
    }

    @Override
    public void onScrollDown() { // Người dùng cuộn nội dung xuống, BottomNav hiện
        if (viewPager.getCurrentItem() == 0 && !isBottomNavVisible) {
            isBottomNavVisible = true; // Đặt cờ ngay
            // Điều chỉnh margin TRƯỚC khi animation để ViewPager co lại, tạo không gian
            adjustViewPagerMargin(false);
            // Nếu bạn đã dùng setVisibility(View.GONE) ở đâu đó, hãy đặt VISIBLE ở đây
            // bottomNavigationView.setVisibility(View.VISIBLE);
            bottomNavigationView.animate()
                    .translationY(0)
                    .setInterpolator(new DecelerateInterpolator(2))
                    .setDuration(200)
                    .start();
        }
    }
}