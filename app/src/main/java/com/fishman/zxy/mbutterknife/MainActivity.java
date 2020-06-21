package com.fishman.zxy.mbutterknife;


import android.os.Bundle;

import com.fishman.zxy.annotion_lib.BindString;
import com.fishman.zxy.annotion_lib.BindView;
import com.fishman.zxy.annotion_lib.OnClick;
import com.fishman.zxy.butterknife.ButterKnife;

import androidx.appcompat.app.AppCompatActivity;

import android.view.View;
import android.widget.TextView;

public class MainActivity extends AppCompatActivity {

    @BindView(R.id.tv_one)
    TextView textView;

    @BindString(R.string.change)
    String  str;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        ButterKnife.bind(this);

//        textView=findViewById(R.id.tv_one);
//          str=getResources().getString(R.string.change);
    }

    @OnClick({R.id.tv_one})
    public void Change(View view) {
        textView.setText(str);
    }
}
