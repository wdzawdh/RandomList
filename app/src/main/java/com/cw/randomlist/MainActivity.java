package com.cw.randomlist;

import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.TextView;

import java.util.Random;

/**
 * @author cw
 * @date 2018/5/2
 */
public class MainActivity extends AppCompatActivity {

    private Random mRdm = new Random();

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.layout_main);

        RandomList randomList = findViewById(R.id.randomList);

        RandomLayout.Adapter adapter = new RandomLayout.Adapter() {
            @Override
            public int getCount() {
                return 50;
            }

            @Override
            public View getView(int position, View convertView) {

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
        randomList.setAdapter(adapter);

        randomList.setRandomScrollListener(new RandomList.RandomScrollListener() {
            @Override
            public void onScrollChanged(RandomList randomList, int x, int y, int oldx, int oldy) {
                Log.d("cw", "");
            }
        });
    }
}
