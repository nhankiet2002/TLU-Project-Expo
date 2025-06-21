package com.cse441.tluprojectexpo;

import android.content.Context;
import android.util.AttributeSet;
import androidx.viewpager.widget.ViewPager;
import android.view.MotionEvent;

public class NonSwipeableViewPager extends ViewPager {
    public NonSwipeableViewPager(Context context) {
        super(context);
    }

    public NonSwipeableViewPager(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    @Override
    public boolean onInterceptTouchEvent(MotionEvent event) {
        // Không cho phép ViewPager nhận sự kiện vuốt
        return false;
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        // Không xử lý sự kiện vuốt
        return false;
    }
} 