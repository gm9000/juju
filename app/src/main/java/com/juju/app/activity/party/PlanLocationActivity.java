package com.juju.app.activity.party;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.juju.app.R;
import com.juju.app.golobal.Constants;
import com.juju.app.ui.base.BaseActivity;
import com.juju.app.utils.ActivityUtil;
import com.lidroid.xutils.view.annotation.ContentView;
import com.lidroid.xutils.view.annotation.ViewInject;
import com.lidroid.xutils.view.annotation.event.OnClick;

@ContentView(R.layout.layout_plan_location)
public class PlanLocationActivity extends BaseActivity {

    private static final String TAG = "MyPartyListlActivity";

    @ViewInject(R.id.txt_title)
    private TextView txt_title;
    @ViewInject(R.id.img_back)
    private ImageView img_back;
    @ViewInject(R.id.txt_left)
    private TextView txt_left;
    @ViewInject(R.id.txt_right)
    private TextView txt_right;
    @ViewInject(R.id.img_right)
    private ImageView img_right;

    private double latitude;
    private double longitude;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        initParam();
        initView();

    }


    private void initView() {

        txt_left.setVisibility(View.GONE);

        RelativeLayout.LayoutParams layoutParams = (RelativeLayout.LayoutParams)txt_left.getLayoutParams();
        layoutParams.leftMargin = 15;
        txt_title.setText(R.string.location_select);
        txt_left.setLayoutParams(layoutParams);
        txt_right.setVisibility(View.GONE);
        img_right.setVisibility(View.GONE);

    }


    public void initParam() {
    }

    @OnClick(R.id.btn_confirm)
    private void confirmLocation(View view){
        latitude = 2323.233f;
        longitude = 1111.1111f;
        String address = "hellowllsdfl";

        Intent intent = getIntent();
        intent.putExtra(Constants.ADDRESS,address);
        intent.putExtra(Constants.LONGITUDE,longitude);
        intent.putExtra(Constants.LATITUDE, latitude);
        setResult(RESULT_OK,intent);
        ActivityUtil.finish(this);
    }

    @OnClick(R.id.btn_cancel)
    private void cancel(View view){
        setResult(RESULT_CANCELED);
        ActivityUtil.finish(this);
    }

}
