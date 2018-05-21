package com.cw.randomlist;

import android.content.Context;
import android.graphics.Rect;
import android.util.AttributeSet;
import android.view.View;
import android.view.ViewGroup;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Random;
import java.util.Set;

public class RandomLayout extends ScrollViewGroup {

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
     * 尾部空闲的个数
     */
    private int mEndFree;
    /**
     * 头部空闲的个数
     */
    private int mStartFree;
    /**
     * 区域的二维数组
     */
    private int[][] mAreaDensity = new int[4][0];
    /**
     * 存放已经确定位置的View
     */
    private Set<View> mFixedViews;
    /**
     * 提供子View的adapter
     */
    private RandomAdapter mAdapter;
    /**
     * 记录被回收的View，以便重复利用
     */
    private List<View> mRecycledViews;
    /**
     * 是否已经layout
     */
    private boolean mHasLayout;
    /**
     * 记录本次计算的自身宽高
     */
    private int mLastH;
    /**
     * 结尾布局
     */
    private boolean mLayoutOnEnd = true;

    /**
     * 构造方法
     */
    public RandomLayout(Context context) {
        this(context, null);
    }

    public RandomLayout(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    /**
     * 设置数据源
     */
    public void setAdapter(RandomAdapter adapter) {
        this.mAdapter = adapter;
        this.mAdapter.setRandomLayout(this);
    }

    /**
     * 设置布局方向
     *
     * @param layoutOnEnd layoutOnEnd
     */
    public void setLayoutOnEnd(boolean layoutOnEnd) {
        mLayoutOnEnd = layoutOnEnd;
    }

    /**
     * 布局方向是否是向后添加
     */
    public boolean isLayoutOnEnd() {
        return mLayoutOnEnd;
    }

    public boolean hasLayouted() {
        return mHasLayout;
    }

    /**
     * 初始化方法
     */
    private void init() {
        mHasLayout = false;
        mRdm = new Random();
        mFixedViews = new HashSet<>();
        mRecycledViews = new LinkedList<>();
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
     * 设置mXRegularity和mXRegularity，确定区域的个数
     */
    private void refreshRegularity() {
        this.mXRegularity = mAreaDensity.length;
        int adapterCount = mAdapter.getCount();
        if (mLayoutOnEnd) {
            this.mEndFree = (adapterCount + mStartFree) % mXRegularity;
            this.mYRegularity = (int) Math.ceil((float) (adapterCount + mStartFree) / mXRegularity);
        } else {
            this.mStartFree = (adapterCount + mEndFree) % mXRegularity;
            this.mYRegularity = (int) Math.ceil((float) (adapterCount + mEndFree) / mXRegularity);
        }
        // 存放区域的二维数组,区域不满时自动扩展
        this.mAreaDensity = extendsArray(mAreaDensity, mYRegularity - mAreaDensity[0].length, mLayoutOnEnd);
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
        int count = mAdapter.getCount();
        for (int i = mFixedViews.size(); i < count; i++) {
            // 从集合中取出之前存入的子View
            View convertView = popRecycler();
            View newChild = mAdapter.getView(getContext(), i, convertView);
            if (newChild != convertView) {
                // 这说明没发生复用，所以重新把这个没用到的子View存入集合中
                pushRecycler(convertView);
            }
            ViewGroup.LayoutParams layoutParams = newChild.getLayoutParams();
            if (layoutParams == null) {
                layoutParams = new LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT);
            }
            super.addView(newChild, layoutParams);
        }
    }

    /**
     * 重新更新子View
     */
    public void refresh() {
        //resetAllAreas();// 重新分配区域
        refreshRegularity();
        generateChildren();// 重新产生子View
        requestLayout();
    }

    @Override
    protected void onScrollChanged(int l, int t, int oldl, int oldt) {
        super.onScrollChanged(l, t, oldl, oldt);
        for (int i = 0; i < getChildCount(); i++) {
            final View view = getChildAt(i);
            final int[] location = new int[2];
            view.getLocationOnScreen(location);

            float scale;
            int out = 0;
            if (getOrientation() == HORIZONTAL) {
                if (location[0] < 0) {
                    out = Math.abs(location[0]);
                }
                if (location[0] + view.getWidth() > getParentWidth()) {
                    out = Math.abs(location[0] + view.getWidth() - getParentWidth());
                }
            } else {
                if (location[1] < 0) {
                    out = Math.abs(location[1]);
                }
                if (location[1] + view.getHeight() > getParentHeight()) {
                    out = Math.abs(location[1] + view.getHeight() - getParentHeight());
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
        super.removeAllViews();
        resetAllAreas();
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
        int width, height;
        if (getOrientation() == VERTICAL) {
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
        if (getOrientation() == VERTICAL) {
            thisW = r - l - this.getPaddingLeft() - this.getPaddingRight();
            thisH = b - t - this.getPaddingTop() - this.getPaddingBottom();
        } else {
            thisH = r - l - this.getPaddingLeft() - this.getPaddingRight();
            thisW = b - t - this.getPaddingTop() - this.getPaddingBottom();
        }

        //如果是向前添加布局，将已经布局的view整体向后偏移offset
        int offset = thisH - mLastH;
        if (!mLayoutOnEnd) {
            for (int i = 0; i < getChildCount(); i++) {
                View child = getChildAt(i);
                if (getOrientation() == VERTICAL) {
                    child.layout(child.getLeft(), child.getTop() + offset, child.getRight(), child.getBottom() + offset);
                } else {
                    child.layout(child.getLeft() + offset, child.getTop(), child.getRight() + offset, child.getBottom());
                }
            }
        }

        //记录本次计算的自身宽高，VERTICAL时代表实际布局的高，HORIZONTAL时代表实际布局的宽
        mLastH = thisH;

        //自身内容区域的右边和下边
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
                if (getOrientation() == VERTICAL) {
                    colW = thisW / (float) mXRegularity;
                    rowH = thisH / (float) mYRegularity;
                } else {
                    rowH = thisW / (float) mXRegularity;
                    colW = thisH / (float) mYRegularity;
                }

                int arrayIdx;
                while ((arrayIdx = getAreaIdx(areaCapacity)) >= 0) { // 如果使用区域大于0，就可以为子View尝试分配
                    int row = arrayIdx % mXRegularity;// 计算出在二维数组中的位置
                    int col = arrayIdx / mXRegularity;

                    //实际的排列方向
                    float rowAct, colAct;
                    if (getOrientation() == VERTICAL) {
                        rowAct = row;
                        colAct = col;
                    } else {
                        rowAct = col;
                        colAct = row;
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

        //如果是向前添加布局,将布局滚动回原来位置
        if (!mLayoutOnEnd) {
            if (getOrientation() == VERTICAL) {
                scrollBy(0, offset);
            } else {
                scrollBy(offset, 0);
            }
        }
    }

    /**
     * 获取密度还不满的区域
     *
     * @param areaCapacity 密度
     */
    private int getAreaIdx(int areaCapacity) {
        ArrayList<Integer> temp = new ArrayList<>();
        for (int y = 0; y < mYRegularity; y++) {
            for (int x = 0; x < mXRegularity; x++) {
                if (mAreaDensity[x][y] < areaCapacity) {
                    temp.add(y * mXRegularity + (x + 1)); //栗子：第13个 = 3 * 4 + (0 + 1)
                }
            }
        }
        int num;
        //如果是向前添加布局,从最后一个位置开始取
        if (mLayoutOnEnd) {
            //return temp.get(mRdm.nextInt(temp.size() - 1)) - 1; 是否有序
            num = mStartFree;
        } else {
            num = temp.size() - 1 - mEndFree;
        }
        if (temp.size() > num && num >= 0) {
            return temp.get(num) - 1;
        }
        return -1;
    }

    /**
     * 扩展二维数组（Y方向扩展）
     *
     * @param array  需要扩展的array
     * @param extend Y方向扩展的大小
     * @return 扩展后的二维数组
     */
    private int[][] extendsArray(int[][] array, int extend, boolean addEnd) {
        if (extend <= 0) {
            return array;
        }
        int[][] arr = new int[array.length][array[0].length + extend];
        for (int i = 0; i < array.length; i++) {
            if (addEnd) {
                System.arraycopy(array[i], 0, arr[i], 0, array[i].length);  //数组拷贝
            } else {
                System.arraycopy(array[i], 0, arr[i], extend, array[i].length);
            }
        }
        return arr;
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

    private int getParentWidth() {
        ViewGroup parent = (ViewGroup) getParent();
        return parent.getWidth();
    }

    private int getParentHeight() {
        ViewGroup parent = (ViewGroup) getParent();
        return parent.getHeight();
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

