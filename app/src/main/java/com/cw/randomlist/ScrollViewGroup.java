package com.cw.randomlist;

import android.content.Context;
import android.support.annotation.IntDef;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.VelocityTracker;
import android.view.View;
import android.view.ViewConfiguration;
import android.view.ViewGroup;
import android.widget.Scroller;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

/**
 * @author cw
 * @date 2018/5/21
 */
public abstract class ScrollViewGroup extends ViewGroup {

    private VelocityTracker mVelocityTracker;
    private Scroller mScroller;

    private int mOrientation = HORIZONTAL;
    private int mTouchSlop;
    private boolean sameDirection;//是否同向(嵌套时)
    private boolean isFirst;
    private int mPointerId;
    private int mMaxVelocity;//最大速度
    private float downX, downY, lastX, lastY;

    public ScrollViewGroup(Context context, AttributeSet attrs) {
        super(context, attrs);
        mMaxVelocity = ViewConfiguration.get(context).getScaledMaximumFlingVelocity();
        mScroller = new Scroller(context);
        mTouchSlop = ViewConfiguration.get(getContext()).getScaledTouchSlop();
    }

    /**
     * 定位到子view
     *
     * @param position position
     */
    public void setCurrentItem(int position, boolean smooth) {
        if (getChildCount() > position) {
            View child = getChildAt(position);
            final int[] location = new int[2];
            child.getLocationOnScreen(location);
            if (mOrientation == HORIZONTAL) {
                int localX = location[0];
                if (smooth) {
                    mScroller.startScroll(getScrollX(), 0, localX, 0, 300);
                    postInvalidate();
                } else {
                    scrollTo(getScrollX() + localX, 0);
                }
            } else {
                int localY = location[1];
                if (smooth) {
                    mScroller.startScroll(0, getScrollY(), 0, localY, 300);
                    postInvalidate();
                } else {
                    scrollTo(0, getScrollY() + localY);
                }
            }
        }
    }

    @Override
    public boolean onInterceptTouchEvent(MotionEvent ev) {
        int action = ev.getAction();
        if (action == MotionEvent.ACTION_DOWN) {
            downX = lastX = ev.getX();
            downY = lastY = ev.getY();
        }
        return action == MotionEvent.ACTION_MOVE;
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        obtainVelocityTracker(event);
        int action = event.getAction();
        float x = event.getX();
        float y = event.getY();
        if (action == MotionEvent.ACTION_DOWN) {
            if (!mScroller.isFinished()) {
                mScroller.abortAnimation();
            }
            mPointerId = event.getPointerId(0);
            lastX = x;
            lastY = y;
            getParent().requestDisallowInterceptTouchEvent(true);
        } else if (action == MotionEvent.ACTION_MOVE) {
            if (isFirst) {
                lastX = x;
                lastY = y;
                isFirst = false;
            }
            if (mOrientation == HORIZONTAL) {
                touchMoveHorizontal(event);
            } else {
                touchMoveVertical(event);
            }
            //滚动距离的回调
            //scrollChangeListener.onScrollChange(getScrollX(), getScrollY());
            mScrollState = SCROLLING;
        } else if (action == MotionEvent.ACTION_UP || action == MotionEvent.ACTION_CANCEL) {
            //计算1000ms的速度
            mVelocityTracker.computeCurrentVelocity(1000, mMaxVelocity);
            //获取x，y在mPointerId上的的速度
            final float velocityX = mVelocityTracker.getXVelocity(mPointerId);
            final float velocityY = mVelocityTracker.getYVelocity(mPointerId);
            if (mOrientation == HORIZONTAL) {
                if (getScrollX() < 0) {
                    //超出起始边界，弹回起始位置
                    mScroller.startScroll(getScrollX(), 0, -getScrollX(), 0, 300);
                    onStartPoint();
                } else if (getScrollX() + getParentWidth() > getWidth()) {
                    //超过结尾边界同理
                    mScroller.startScroll(getScrollX(), 0, -(getScrollX() + getParentWidth() - getWidth()), 0, 300);
                    onEndPoint();
                } else {
                    //中间时候，按最后的瞬时速度抛出，不超过剩下的距离
                    mScroller.fling(getScrollX(), 0, (int) -velocityX, 0, 0, getWidth() - getParentWidth() + 100, 0, 0);
                }
            } else {
                if (getScrollY() < 0) {
                    mScroller.startScroll(0, getScrollY(), 0, -getScrollY(), 300);
                } else if (getScrollY() + getParentHeight() > getHeight()) {
                    mScroller.startScroll(0, getScrollY(), 0, -(getScrollY() + getParentHeight() - getHeight()), 300);
                } else {
                    mScroller.fling(0, getScrollY(), 0, (int) -velocityY, 0, 0, 0, getHeight() - getParentHeight() + 100);
                }
            }
            isFirst = true;
            recycleVelocityTracker();
            //调用重绘才会调用computeScroll方法，形成动画
            postInvalidate();
        }
        return true;
    }

    /**
     * 水平滚动
     */
    private void touchMoveHorizontal(MotionEvent event) {
        float x = event.getX();
        int offsetX;
        //超出界限时，增加阻力
        if (getScrollX() < 0 || getScrollX() + getParentWidth() > getWidth()) {
            offsetX = (int) ((x - lastX) / 2.5);
        } else {
            offsetX = (int) (x - lastX);
        }
        if (!sameDirection) {
            //不同向
            if (Math.abs(x - downX) + mTouchSlop > Math.abs(event.getY() - downY) && getWidth() > getParentWidth()) {
                getParent().requestDisallowInterceptTouchEvent(true);
            } else {
                getParent().requestDisallowInterceptTouchEvent(false);
            }
        } else {
            //同向
            if (lastX >= downX) {
                if (getScrollX() <= 0) {
                    getParent().requestDisallowInterceptTouchEvent(false);
                } else {
                    getParent().requestDisallowInterceptTouchEvent(true);
                }
            } else {
                if (getScrollX() >= getWidth() - getParentWidth()) {
                    getParent().requestDisallowInterceptTouchEvent(false);
                } else {
                    getParent().requestDisallowInterceptTouchEvent(true);
                }
            }
        }
        if (Math.abs(downX - x) > mTouchSlop) {
            scrollBy(-offsetX, 0);
        }
        lastX = x;
    }

    /**
     * 垂直滚动
     */
    private void touchMoveVertical(MotionEvent event) {
        float y = event.getY();
        int offsetY;
        //超出界限时，增加阻力
        if (getScrollY() < 0 || getScrollY() + getParentHeight() > getHeight()) {
            offsetY = (int) ((y - lastY) / 2.5);
        } else {
            offsetY = (int) (y - lastY);
        }
        if (!sameDirection) {
            //不同向
            if (Math.abs(event.getX() - downX) < Math.abs(y - downY) + mTouchSlop && getHeight() > getParentHeight()) {
                getParent().requestDisallowInterceptTouchEvent(true);
            } else {
                getParent().requestDisallowInterceptTouchEvent(false);
            }
        } else {
            //同向
            if (lastY >= downY) {
                if (getScrollY() <= 0) {
                    getParent().requestDisallowInterceptTouchEvent(false);
                } else {
                    getParent().requestDisallowInterceptTouchEvent(true);
                }
            } else {
                if (getScrollY() >= getHeight() - getParentHeight()) {
                    getParent().requestDisallowInterceptTouchEvent(false);
                } else {
                    getParent().requestDisallowInterceptTouchEvent(true);
                }
            }
        }
        //有效滑动，滚动-offsetY距离
        if (Math.abs(downY - y) > mTouchSlop) {
            scrollBy(0, -offsetY);
        }
        lastY = y;
    }

    /**
     * 创建新的速度监视对象
     *
     * @param event 滑动事件
     */
    private void obtainVelocityTracker(MotionEvent event) {
        if (null == mVelocityTracker) {
            mVelocityTracker = VelocityTracker.obtain();
        }
        mVelocityTracker.addMovement(event);
    }

    /**
     * 释放资源
     */
    private void recycleVelocityTracker() {
        if (null != mVelocityTracker) {
            mVelocityTracker.clear();
            mVelocityTracker.recycle();
            mVelocityTracker = null;
        }
    }

    private int getParentWidth() {
        ViewGroup parent = (ViewGroup) getParent();
        return parent.getWidth();
    }

    private int getParentHeight() {
        ViewGroup parent = (ViewGroup) getParent();
        return parent.getHeight();
    }

    @Override
    public void computeScroll() {
        super.computeScroll();
        if (mScroller.computeScrollOffset()) {
            scrollTo(mScroller.getCurrX(), mScroller.getCurrY());
            postInvalidate();//当没滚动到需要的位置时，不断的重绘，形成动画
            mScrollState = SCROLLING;
        } else {
            mScrollState = IDLE;
        }
    }


    //-----------------------------------排列方向相关-------------------------------------------------

    public static final int HORIZONTAL = 0;
    public static final int VERTICAL = 1;

    @IntDef({HORIZONTAL, VERTICAL})
    @Retention(RetentionPolicy.SOURCE)
    public @interface OrientationMode {
    }

    public int getOrientation() {
        return mOrientation;
    }

    public void setOrientation(@OrientationMode int orientation) {
        if (mOrientation != orientation) {
            if (getParent() instanceof ScrollViewGroup) {
                sameDirection = ((ScrollViewGroup) getParent()).getOrientation() == orientation;
            }
            mOrientation = orientation;
            requestLayout();
        }
    }

    //-----------------------------------滚动相关----------------------------------------------------

    private int mScrollState;
    public static final int IDLE = 0;//闲置状态
    public static final int SCROLLING = 1;//滚动状态

    /**
     * @return 当前滚动状态
     */
    public int getScrollState() {
        return mScrollState;
    }

    private ScrollChangeListener scrollChangeListener;

    /**
     * 设置滚动监听
     *
     * @param l 回调
     */
    public void setScrollChangeListener(ScrollChangeListener l) {
        this.scrollChangeListener = l;
    }

    private void onStartPoint() {
        if (scrollChangeListener != null) {
            scrollChangeListener.onStartPoint();
        }
    }

    private void onEndPoint() {
        if (scrollChangeListener != null) {
            scrollChangeListener.onEndPoint();
        }
    }

    /**
     * 滚动距离监听器
     */
    interface ScrollChangeListener {
        void onStartPoint();

        void onEndPoint();
    }

}
