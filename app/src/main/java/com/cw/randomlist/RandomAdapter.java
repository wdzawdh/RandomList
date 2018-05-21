package com.cw.randomlist;

import android.content.Context;
import android.view.View;

/**
 * @author cw
 * @date 2018/5/14
 */
public abstract class RandomAdapter {

    private RandomLayout mRandomLayout;

    public abstract int getCount();

    public abstract View getView(Context context, int position, View convertView);

    public void setRandomLayout(RandomLayout randomLayout) {
        mRandomLayout = randomLayout;
    }

    public void notifyDataSetChanged() {
        if (mRandomLayout != null) {
            mRandomLayout.refresh();
        }
    }
}
