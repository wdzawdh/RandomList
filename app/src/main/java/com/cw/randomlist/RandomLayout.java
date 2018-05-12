package com.cw.randomlist;

import android.content.Context;
import android.graphics.Rect;
import android.support.annotation.IntDef;
import android.util.AttributeSet;
import android.util.DisplayMetrics;
import android.view.View;
import android.view.ViewGroup;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Random;
import java.util.Set;

public class RandomLayout extends ViewGroup {

    private Random mRdm;
    /**
     * 行数
     */
    private int mXRegularity;
    /**
     * 列数
     */
    private int mYRegularity;
    /**
     * 区域的二维数组
     */
    private int[][] mAreaDensity;
    /**
     * 存放已经确定位置的View
     */
    private Set<View> mFixedViews;
    /**
     * 提供子View的adapter
     */
    private Adapter mAdapter;
    /**
     * 记录被回收的View，以便重复利用
     */
    private List<View> mRecycledViews;
    /**
     * 是否已经layout
     */
    private boolean mHasLayout;
    /**
     * 排列方向
     */
    private int mOrientation = HORIZONTAL;
    /**
     * 屏幕宽高
     */
    private int mScreenWidth, mScreenHeigth;

    /**
     * 构造方法
     */
    public RandomLayout(Context context) {
        this(context, null);
    }

    public RandomLayout(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public RandomLayout(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init();
    }

    /**
     * 初始化方法
     */
    private void init() {
        mHasLayout = false;
        DisplayMetrics dm = getResources().getDisplayMetrics();
        mScreenWidth = dm.widthPixels;
        mScreenHeigth = dm.heightPixels;
        mRdm = new Random();
        mFixedViews = new HashSet<>();
        mRecycledViews = new LinkedList<>();
    }

    public boolean hasLayouted() {
        return mHasLayout;
    }

    /**
     * 设置mXRegularity和mXRegularity，确定区域的个数
     */
    public void setRegularity(int xRegularity) {
        if (xRegularity < 1) return;
        this.mXRegularity = xRegularity;
        this.mYRegularity = (int) Math.ceil((float) mAdapter.getCount() / mXRegularity);
        this.mAreaDensity = new int[mYRegularity][mXRegularity];// 存放区域的二维数组
    }

    /**
     * 设置数据源
     */
    public void setAdapter(Adapter adapter) {
        this.mAdapter = adapter;
        setRegularity(4);
    }

    /**
     * 重新设置区域，把所有的区域记录都归0
     */
    private void resetAllAreas() {
        mFixedViews.clear();
        for (int i = 0; i < mYRegularity; i++) {
            for (int j = 0; j < mXRegularity; j++) {
                mAreaDensity[i][j] = 0;
            }
        }
    }

    /**
     * 把复用的View加入集合，新加入的放入集合第一个。
     */
    private void pushRecycler(View scrapView) {
        if (null != scrapView) {
            mRecycledViews.add(0, scrapView);
        }
    }

    /**
     * 取出复用的View，从集合的第一个位置取出
     */
    private View popRecycler() {
        final int size = mRecycledViews.size();
        if (size > 0) {
            return mRecycledViews.remove(0);
        } else {
            return null;
        }
    }

    /**
     * 产生子View，这个就是listView复用的简化版，但是原理一样
     */
    private void generateChildren() {
        if (null == mAdapter) {
            return;
        }
        // 先把子View全部存入集合
        final int childCount = super.getChildCount();
        for (int i = childCount - 1; i >= 0; i--) {
            pushRecycler(super.getChildAt(i));
        }
        // 删除所有子View
        super.removeAllViewsInLayout();
        // 得到Adapter中的数据量
        final int count = mAdapter.getCount();
        for (int i = 0; i < count; i++) {
            // 从集合中取出之前存入的子View
            View convertView = popRecycler();
            // 把该子View作为adapter的getView的历史View传入，得到返回的View
            View newChild = mAdapter.getView(i, convertView);
            if (newChild != convertView) {// 如果发生了复用，那么newChild应该等于convertView
                // 这说明没发生复用，所以重新把这个没用到的子View存入集合中
                pushRecycler(convertView);
            }
            // 调用父类的方法把子View添加进来
            ViewGroup.LayoutParams layoutParams = newChild.getLayoutParams();
            if (layoutParams == null) {
                layoutParams = new LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT);
            }
            super.addView(newChild, layoutParams);
        }
    }

    /**
     * 重新分配区域
     */
    public void redistribute() {
        resetAllAreas();// 重新设置区域
        requestLayout();
    }

    /**
     * 重新更新子View
     */
    public void refresh() {
        resetAllAreas();// 重新分配区域
        generateChildren();// 重新产生子View
        requestLayout();
    }

    public void onScroll() {
        for (int i = 0; i < getChildCount(); i++) {
            final View view = getChildAt(i);
            final int[] location = new int[2];
            view.getLocationOnScreen(location);

            float scale;
            int out = 0;
            if (mOrientation == HORIZONTAL) {
                if (location[0] < 0) {
                    out = Math.abs(location[0]);
                }
                if (location[0] + view.getWidth() > mScreenWidth) {
                    out = Math.abs(location[0] + view.getWidth() - mScreenWidth);
                }
            } else {
                if (location[1] < 0) {
                    out = Math.abs(location[1]);
                }
                if (location[1] + view.getHeight() > mScreenHeigth) {
                    out = Math.abs(location[1] + view.getHeight() - mScreenHeigth);
                }
            }
            scale = 1 - (float) out / view.getWidth();
            if (scale < 0.5f) {
                scale = 0.5f;
            }
            view.setScaleX(scale);
            view.setScaleY(scale);
        }
    }

    @Override
    public void removeAllViews() {
        super.removeAllViews();// 先删除所有View
        resetAllAreas();// 重新设置所有区域
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        int maxChildWidth = 0;
        int maxChildHeight = 0;
        for (int i = 0; i < getChildCount(); i++) {
            View child = getChildAt(i);
            measureChildren(widthMeasureSpec, heightMeasureSpec);
            int measuredWidth = child.getMeasuredWidth();
            if (measuredWidth > maxChildWidth) {
                maxChildWidth = measuredWidth;
            }
            int measuredHeight = child.getMeasuredHeight();
            if (measuredHeight > maxChildHeight) {
                maxChildHeight = measuredHeight;
            }
        }
        int width;
        int height;
        if (mOrientation == VERTICAL) {
            width = MeasureSpec.getSize(widthMeasureSpec);
            height = (int) (mYRegularity * maxChildHeight * 1.2);
        } else {
            width = (int) (mYRegularity * maxChildWidth * 1.2);
            height = MeasureSpec.getSize(heightMeasureSpec);
        }
        setMeasuredDimension(width, height);
    }

    @Override
    public void onLayout(boolean changed, int l, int t, int r, int b) {
        // 确定自身的宽高
        int thisW, thisH;
        if (mOrientation == VERTICAL) {
            thisW = r - l - this.getPaddingLeft() - this.getPaddingRight();
            thisH = b - t - this.getPaddingTop() - this.getPaddingBottom();
        } else {
            thisH = r - l - this.getPaddingLeft() - this.getPaddingRight();
            thisW = b - t - this.getPaddingTop() - this.getPaddingBottom();
        }
        // 自身内容区域的右边和下边
        int contentRight = r - getPaddingRight();
        int contentBottom = b - getPaddingBottom();

        final int count = getChildCount();
        int areaCapacity = 1;// 区域密度，表示一个区域内可以放几个View

        for (int i = 0; i < count; i++) {
            final View child = getChildAt(i);
            if (child.getVisibility() == View.GONE) { // gone掉的view是不参与布局
                continue;
            }

            if (!mFixedViews.contains(child)) {// mFixedViews用于存放已经确定好位置的View，存到了就没必要再次存放
                LayoutParams params = (LayoutParams) child.getLayoutParams();
                int childW = child.getLayoutParams().width;
                int childH = child.getLayoutParams().height;
                // 用自身的高度去除以分配值，可以算出每一个区域的宽和高
                float rowH, colW;
                if (mOrientation == VERTICAL) {
                    colW = thisW / (float) mXRegularity;
                    rowH = thisH / (float) mYRegularity;
                } else {
                    rowH = thisW / (float) mXRegularity;
                    colW = thisH / (float) mYRegularity;
                }

                int arrayIdx;
                while ((arrayIdx = getAreaIdx(areaCapacity)) >= 0) { // 如果使用区域大于0，就可以为子View尝试分配
                    int row = arrayIdx / mXRegularity;
                    int col = arrayIdx % mXRegularity;// 计算出在二维数组中的位置

                    //实际的排列方向
                    float rowAct, colAct;
                    if (mOrientation == VERTICAL) {
                        rowAct = col;
                        colAct = row;
                    } else {
                        rowAct = row;
                        colAct = col;
                    }
                    if (mAreaDensity[row][col] < areaCapacity) {// 区域密度未超过限定，将view置入该区域
                        int xOffset = (int) colW - childW; // 区域宽度 和 子View的宽度差值，差值可以用来做区域内的位置随机
                        if (xOffset <= 0) {
                            xOffset = 1;
                        }
                        int yOffset = (int) rowH - childH;
                        if (yOffset <= 0) {
                            yOffset = 1;
                        }
                        // 确定左边，等于区域宽度*左边的区域
                        params.mLeft = getPaddingLeft() + (int) (colW * rowAct + mRdm.nextInt(xOffset));
                        int rightEdge = contentRight - childW;
                        if (params.mLeft > rightEdge) {// 加上子View的宽度后不能超出右边界
                            params.mLeft = rightEdge;
                        }
                        params.mRight = params.mLeft + childW;

                        params.mTop = getPaddingTop() + (int) (rowH * colAct + mRdm.nextInt(yOffset));
                        int bottomEdge = contentBottom - childH;
                        if (params.mTop > bottomEdge) {// 加上子View的宽度后不能超出右边界
                            params.mTop = bottomEdge;
                        }
                        params.mBottom = params.mTop + childH;

                        /*if (!isOverlap(params)) {
                            // 判断是否和别的View重叠了
                        }*/
                        mAreaDensity[row][col]++;// 没有重叠，把该区域的密度加1
                        child.layout(params.mLeft, params.mTop, params.mRight, params.mBottom);// 布局子View
                        mFixedViews.add(child);// 添加到已经布局的集合中
                        break;
                    }
                }
            }
        }
        mHasLayout = true;
    }

    /**
     * 获取密度还不满的区域
     *
     * @param areaCapacity 密度
     */
    private int getAreaIdx(int areaCapacity) {
        ArrayList<Integer> temp = new ArrayList<>();
        for (int i = 0; i < mAreaDensity.length; i++) {
            for (int j = 0; j < mAreaDensity[i].length; j++) {
                if (mAreaDensity[i][j] < areaCapacity) {
                    //添加位置
                    temp.add(i * mAreaDensity[i].length + (j + 1));
                }
            }
        }
        if (temp.size() == 0) {
            return -1;
        } else {
            //return temp.get(mRdm.nextInt(temp.size() - 1)) - 1; 是否有序
            return temp.get(0) - 1;
        }
    }

    /**
     * 计算两个View是否重叠，如果重叠，那么他们之间一定有一个矩形区域是共有的
     */
    private boolean isOverlap(LayoutParams params) {
        int overlapAdd = 2;//计算重叠时候的间距
        int l = params.mLeft - overlapAdd;
        int t = params.mTop - overlapAdd;
        int r = params.mRight + overlapAdd;
        int b = params.mBottom + overlapAdd;

        Rect rect = new Rect();

        for (View v : mFixedViews) {
            int vl = v.getLeft() - overlapAdd;
            int vt = v.getTop() - overlapAdd;
            int vr = v.getRight() + overlapAdd;
            int vb = v.getBottom() + overlapAdd;
            rect.left = Math.max(l, vl);
            rect.top = Math.max(t, vt);
            rect.right = Math.min(r, vr);
            rect.bottom = Math.min(b, vb);
            if (rect.right >= rect.left && rect.bottom >= rect.top) {
                return true;
            }
        }
        return false;
    }

    /**
     * 排列方向相关
     */
    public static final int HORIZONTAL = 0;
    public static final int VERTICAL = 1;

    @IntDef({HORIZONTAL, VERTICAL})
    @Retention(RetentionPolicy.SOURCE)
    public @interface OrientationMode {
    }

    public void setOrientation(@OrientationMode int orientation) {
        if (mOrientation != orientation) {
            mOrientation = orientation;
            requestLayout();
        }
    }

    /**
     * 内部类、接口
     */
    public interface Adapter {

        int getCount();

        View getView(int position, View convertView);
    }

    public static class LayoutParams extends MarginLayoutParams {

        private int mLeft;
        private int mRight;
        private int mTop;
        private int mBottom;

        public LayoutParams(int w, int h) {
            super(w, h);
        }
    }
}
