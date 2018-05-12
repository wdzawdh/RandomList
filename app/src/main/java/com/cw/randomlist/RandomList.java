package com.cw.randomlist;

import android.content.Context;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.util.AttributeSet;
import android.widget.FrameLayout;
import android.widget.HorizontalScrollView;
import android.widget.ScrollView;

import static com.cw.randomlist.RandomLayout.HORIZONTAL;


/**
 * @author cw
 * @date 2018/5/12
 */
public class RandomList extends FrameLayout {

    private FrameLayout mScrollView;
    private RandomLayout mRandomLayout;
    private int mTopPadding;
    private int mLeftPadding;
    private int mRightPadding;
    private int mBottomPadding;

    public RandomList(@NonNull Context context) {
        this(context, null);
    }

    public RandomList(@NonNull Context context, @Nullable AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public RandomList(@NonNull Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init(HORIZONTAL);
    }

    @Override
    public void setPadding(int left, int top, int right, int bottom) {
        mLeftPadding = left;
        mTopPadding = top;
        mRightPadding = right;
        mBottomPadding = bottom;
    }

    public void setOrientation(@RandomLayout.OrientationMode int orientation) {
        init(orientation);
    }

    public void setAdapter(RandomLayout.Adapter adapter) {
        mRandomLayout.setAdapter(adapter);
        mRandomLayout.refresh();
    }

    private void init(int orientation) {
        if (orientation == HORIZONTAL) {
            mScrollView = new RandomHorizontalScrollView(getContext());
        } else {
            mScrollView = new RandomScrollView(getContext());
        }
        mRandomLayout = new RandomLayout(getContext());
        LayoutParams randomLayoutParams = new LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.MATCH_PARENT);
        randomLayoutParams.leftMargin = mLeftPadding;
        randomLayoutParams.topMargin = mTopPadding;
        randomLayoutParams.rightMargin = mRightPadding;
        randomLayoutParams.bottomMargin = mBottomPadding;
        mRandomLayout.setLayoutParams(randomLayoutParams);
        mRandomLayout.setOrientation(orientation);
        mScrollView.addView(mRandomLayout);

        removeAllViews();
        LayoutParams layoutParams = new LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT);
        mScrollView.setLayoutParams(layoutParams);
        mScrollView.setClipChildren(false);
        mScrollView.setHorizontalScrollBarEnabled(false);
        mScrollView.setVerticalScrollBarEnabled(false);
        addView(mScrollView);
    }

    public void setRandomScrollListener(final RandomScrollListener listener) {
        if (mScrollView instanceof RandomScrollView) {
            RandomScrollView randomScrollView = (RandomScrollView) mScrollView;
            randomScrollView.setScrollViewListener(new ScrollViewListener() {
                @Override
                public void onScrollChanged(RandomList randomList, int x, int y, int oldx, int oldy) {
                    listener.onScrollChanged(randomList, x, y, oldx, oldy);
                    mRandomLayout.onScroll();
                }
            });
        }
        if (mScrollView instanceof RandomHorizontalScrollView) {
            RandomHorizontalScrollView randomScrollView = (RandomHorizontalScrollView) mScrollView;
            randomScrollView.setScrollViewListener(new ScrollViewListener() {
                @Override
                public void onScrollChanged(RandomList randomList, int x, int y, int oldx, int oldy) {
                    listener.onScrollChanged(randomList, x, y, oldx, oldy);
                    mRandomLayout.onScroll();
                }
            });
        }
    }

    public interface RandomScrollListener {
        void onScrollChanged(RandomList randomList, int x, int y, int oldx, int oldy);
    }


    //----------------------------------RandomScrollView--------------------------------------------

    private class RandomScrollView extends ScrollView {

        private ScrollViewListener scrollViewListener = null;

        public RandomScrollView(Context context) {
            super(context);
        }

        public void setScrollViewListener(ScrollViewListener scrollViewListener) {
            this.scrollViewListener = scrollViewListener;
        }

        @Override
        protected void onScrollChanged(int x, int y, int oldx, int oldy) {
            super.onScrollChanged(x, y, oldx, oldy);
            if (scrollViewListener != null) {
                scrollViewListener.onScrollChanged(RandomList.this, x, y, oldx, oldy);
            }
        }
    }

    private class RandomHorizontalScrollView extends HorizontalScrollView {

        private ScrollViewListener scrollViewListener = null;

        public RandomHorizontalScrollView(Context context) {
            super(context);
        }

        public void setScrollViewListener(ScrollViewListener scrollViewListener) {
            this.scrollViewListener = scrollViewListener;
        }

        @Override
        protected void onScrollChanged(int x, int y, int oldx, int oldy) {
            super.onScrollChanged(x, y, oldx, oldy);
            if (scrollViewListener != null) {
                scrollViewListener.onScrollChanged(RandomList.this, x, y, oldx, oldy);
            }
        }
    }

    private interface ScrollViewListener {
        void onScrollChanged(RandomList randomList, int x, int y, int oldx, int oldy);
    }
}
