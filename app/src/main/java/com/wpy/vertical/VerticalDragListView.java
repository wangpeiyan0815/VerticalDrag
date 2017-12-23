package com.wpy.vertical;

import android.content.Context;
import android.support.v4.view.ViewCompat;
import android.support.v4.widget.ViewDragHelper;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;
import android.widget.AbsListView;
import android.widget.FrameLayout;

/**
 * Created by dell on 2017/12/23.
 * 问题：
 * 当处理listView的事件处理之后时会出现 下拉时菜单可以出现   listView 滚动出现问题
 */

public class VerticalDragListView extends FrameLayout {

    //用户拖动和复位的有用操作和状态跟踪
    private ViewDragHelper mViewDragHelper;

    private int mMenuHeight;

    private View mDragListView;

    //记录菜单是否打开关闭
    private boolean mMenuIsOpen = false;

    public VerticalDragListView(Context context) {
        this(context, null);
    }

    public VerticalDragListView(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public VerticalDragListView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        mViewDragHelper = ViewDragHelper.create(this, mDragHelperCallback);
    }

    // 现象就是ListView可以滑动，但是菜单滑动没有效果了
    private float mDownY;

    @Override
    public boolean onInterceptTouchEvent(MotionEvent ev) {
        // 菜单打开要拦截
        if (mMenuIsOpen) {
            return true;
        }
        // 向下滑动拦截，不要给ListView做处理
        switch (ev.getAction()) {
            case MotionEvent.ACTION_DOWN:
                mDownY = ev.getY();
                // 让 DragHelper 拿一个完整的事件
                mViewDragHelper.processTouchEvent(ev);
                break;
            case MotionEvent.ACTION_MOVE:
                float moveY = ev.getY();
                if ((moveY - mDownY) > 0 && !canChildScrollUp()) {
                    return true;
                }
                break;
        }

        return super.onInterceptTouchEvent(ev);
    }

    private ViewDragHelper.Callback mDragHelperCallback = new ViewDragHelper.Callback() {
        //是否允许拖拽   判断只需要最上面的view 需要拖动
        @Override
        public boolean tryCaptureView(View child, int pointerId) {
            //判断当前拖动的view 是否满足要求
            return mDragListView == child;
        }

        @Override
        public int clampViewPositionVertical(View child, int top, int dy) {
            // 垂直拖动移动的位置
            // 2.3 垂直拖动的范围只能是后面菜单 View 的高度
            if (top <= 0) {
                top = 0;
            }

            if (top >= mMenuHeight) {
                top = mMenuHeight;
            }
            return top;
        }

        @Override
        public void onViewReleased(View releasedChild, float xvel, float yvel) {
            super.onViewReleased(releasedChild, xvel, yvel);
            if (releasedChild == mDragListView) {
                if (mDragListView.getTop() > mMenuHeight / 2) {
                    //打开
                    mViewDragHelper.settleCapturedViewAt(0, mMenuHeight);
                    mMenuIsOpen = true;
                } else {
                    //关闭
                    mViewDragHelper.settleCapturedViewAt(0, 0);
                    mMenuIsOpen = false;
                }
                invalidate();
            }

        }

        //左右拖动   不需要左右拖动可以注释改行代码
//        @Override
//        public int clampViewPositionHorizontal(View child, int left, int dx) {
//            return left;
//        }
    };

    /**
     * 响应滚动
     */
    @Override
    public void computeScroll() {
        if (mViewDragHelper.continueSettling(true)) {
            invalidate();
        }
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        //注意这里接收的事件必须是一整件事件序列  down move up  为一整套
        mViewDragHelper.processTouchEvent(event);
        return true;
    }

    @Override
    protected void onFinishInflate() {
        super.onFinishInflate();
        int childCount = getChildCount();
        if (childCount != 2) {
            throw new RuntimeException("至少两个布局");
        }
        mDragListView = getChildAt(1);

    }

    @Override
    protected void onLayout(boolean changed, int left, int top, int right, int bottom) {
        super.onLayout(changed, left, top, right, bottom);
        //得到View 的高度  高度只有在测量之后才能获得
        if (changed) {
            View childAt = getChildAt(0);
            mMenuHeight = childAt.getMeasuredHeight();
        }

    }

    /**
     * @return Whether it is possible for the child view of this layout to
     * scroll up. Override this if the child view is a custom view.
     * 判断View是否滚动到了最顶部,还能不能向上滚
     */
    public boolean canChildScrollUp() {
        if (android.os.Build.VERSION.SDK_INT < 14) {
            if (mDragListView instanceof AbsListView) {
                final AbsListView absListView = (AbsListView) mDragListView;
                return absListView.getChildCount() > 0
                        && (absListView.getFirstVisiblePosition() > 0 || absListView.getChildAt(0)
                        .getTop() < absListView.getPaddingTop());
            } else {
                return ViewCompat.canScrollVertically(mDragListView, -1) || mDragListView.getScrollY() > 0;
            }
        } else {
            return ViewCompat.canScrollVertically(mDragListView, -1);
        }
    }
}
