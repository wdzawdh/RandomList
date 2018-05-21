package com.cw.randomlist;

import android.content.Context;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.TextView;

import java.util.Random;

/**
 * @author cw
 * @date 2018/5/2
 */
public class MainActivity extends AppCompatActivity {

    private Random mRdm = new Random();
    private RandomLayout mRandomList;
    private int count = 100;
    private RandomAdapter mAdapter;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.layout_main);

        mRandomList = findViewById(R.id.randomList);
        mAdapter = new RandomAdapter() {
            @Override
            public int getCount() {
                return count;
            }

            @Override
            public View getView(Context context, int position, View convertView) {
                View view = View.inflate(getApplicationContext(), R.layout.layout_item, null);
                boolean b = mRdm.nextInt(100) % 3 > 0;
                int i = b ? 200 : 300;
                RandomLayout.LayoutParams layoutParams = new RandomLayout.LayoutParams(i, i);
                view.setLayoutParams(layoutParams);

                TextView tvNum = view.findViewById(R.id.tvNum);
                tvNum.setText(position + "");
                return view;
            }
        };
        mRandomList.setAdapter(mAdapter);
        mAdapter.notifyDataSetChanged();
        mRandomList.post(new Runnable() {
            @Override
            public void run() {
                mRandomList.setCurrentItem(50, false);
            }
        });

    }

    public void onGo(View view) {
        mRandomList.setCurrentItem(10, false);
    }

    public void add(View view) {
        count += 10;
        mAdapter.notifyDataSetChanged();
    }

    public void addEnd(View view) {
        if (mRandomList.isLayoutOnEnd()) {
            mRandomList.setLayoutOnEnd(false);
        } else {
            mRandomList.setLayoutOnEnd(true);
        }
    }
}
