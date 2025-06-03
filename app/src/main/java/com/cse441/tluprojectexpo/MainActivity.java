package com.cse441.tluprojectexpo;

import android.os.Bundle;
import android.view.MenuItem;
import android.view.View; // Thêm import này
// import android.widget.TableLayout; // Import này không được sử dụng

import androidx.activity.EdgeToEdge;
import androidx.annotation.NonNull;
// import androidx.annotation.Nullable; // Import này không được sử dụng
import androidx.appcompat.app.AppCompatActivity;
// import androidx.cardview.widget.CardView; // Import này không được sử dụng
// import androidx.core.graphics.Insets; // Import này không được sử dụng
// import androidx.core.view.ViewCompat; // Import này không được sử dụng
// import androidx.core.view.WindowInsetsCompat; // Import này không được sử dụng
import androidx.fragment.app.FragmentStatePagerAdapter;
// import androidx.viewpager.widget.PagerAdapter; // Import này không được sử dụng
import androidx.viewpager.widget.ViewPager;

import com.cse441.tluprojectexpo.fragment.ViewPagerAdapter;
import com.google.android.material.bottomnavigation.BottomNavigationView;

public class MainActivity extends AppCompatActivity {

    private ViewPager viewPager;
    private BottomNavigationView bottomNavigationView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_main);

        viewPager = findViewById(R.id.view_pager);
        bottomNavigationView = findViewById(R.id.bottom_navigation);

        ViewPagerAdapter adapter = new ViewPagerAdapter(getSupportFragmentManager(), FragmentStatePagerAdapter.BEHAVIOR_RESUME_ONLY_CURRENT_FRAGMENT);
        viewPager.setAdapter(adapter);

        viewPager.addOnPageChangeListener(new ViewPager.OnPageChangeListener() {

            @Override
            public void onPageScrolled(int position,
                                       float positionOffset,
                                       int positionOffsetPixels)
            {}

            @Override
            public void onPageSelected(int position){
                // Kiểm tra position để ẩn/hiện BottomNavigationView
                if (position == 1) { // Position 1 là fragment_create
                    bottomNavigationView.setVisibility(View.GONE);
                } else {
                    bottomNavigationView.setVisibility(View.VISIBLE);
                }

                // Cập nhật trạng thái item được chọn trong BottomNavigationView
                // Ngay cả khi BottomNavigationView bị ẩn, việc cập nhật trạng thái này
                // đảm bảo khi nó hiển thị lại, item đúng sẽ được chọn.
                switch (position){
                    case 0:
                        bottomNavigationView.getMenu().findItem(R.id.menu_home).setChecked(true);
                        break;
                    case 1:
                        bottomNavigationView.getMenu().findItem(R.id.menu_create).setChecked(true);
                        break;
                    case 2:
                        bottomNavigationView.getMenu().findItem(R.id.menu_profile).setChecked(true);
                        break;
                }
            }

            @Override
            public void onPageScrollStateChanged(int state) {

            }

        });

        bottomNavigationView.setOnNavigationItemSelectedListener(new BottomNavigationView.OnNavigationItemSelectedListener() {

            @Override
            public boolean onNavigationItemSelected(@NonNull MenuItem item) {
                int itemId = item.getItemId();

                if (itemId == R.id.menu_home) {
                    viewPager.setCurrentItem(0);
                    // onPageSelected sẽ xử lý việc hiển thị BottomNavigationView và setChecked
                    return true;
                } else if (itemId == R.id.menu_create) {
                    viewPager.setCurrentItem(1);
                    // onPageSelected sẽ xử lý việc ẩn BottomNavigationView và setChecked
                    return true;
                }else if (itemId == R.id.menu_profile) {
                    viewPager.setCurrentItem(2);
                    // onPageSelected sẽ xử lý việc hiển thị BottomNavigationView và setChecked
                    return true;
                }
                return false;
            }
        });

        // Không cần gọi setVisibility ban đầu ở đây vì onPageSelected sẽ được gọi
        // khi ViewPager được thiết lập adapter và trang đầu tiên được chọn.
    }
}