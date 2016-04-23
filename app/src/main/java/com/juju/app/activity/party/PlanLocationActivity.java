package com.juju.app.activity.party;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.baidu.mapapi.map.BaiduMap;
import com.baidu.mapapi.map.MapStatus;
import com.baidu.mapapi.map.MapStatusUpdate;
import com.baidu.mapapi.map.MapStatusUpdateFactory;
import com.baidu.mapapi.map.MapView;
import com.baidu.mapapi.map.MyLocationConfiguration;
import com.juju.app.R;
import com.juju.app.golobal.Constants;
import com.juju.app.ui.base.BaseActivity;
import com.juju.app.utils.ActivityUtil;
import com.lidroid.xutils.view.annotation.ContentView;
import com.lidroid.xutils.view.annotation.ViewInject;
import com.lidroid.xutils.view.annotation.event.OnClick;

@ContentView(R.layout.layout_plan_location)
public class PlanLocationActivity extends BaseActivity {

    private static final String TAG = "PlanLocationActivity";

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


    @ViewInject(R.id.mapView)
    private MapView mapView;
    private BaiduMap mBaiduMap;

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

        mapView.showZoomControls(false);
        mBaiduMap = mapView.getMap();
        mBaiduMap.setMapType(BaiduMap.MAP_TYPE_NORMAL);
//        MapStatus mMapStatus;
//
//
//
//        if (x > 0 && y > 0) {
//            // 当用intent参数时，设置中心点为指定点
//            LatLng p = new LatLng(y, x);
//            mMapStatus = new MapStatus.Builder().target(p).zoom(zoom).build();
//        } else {
//            mMapStatus = new MapStatus.Builder().zoom(zoom).build();
//        }
//        MapStatusUpdate mMapStatusUpdate = MapStatusUpdateFactory.newMapStatus(mMapStatus);
//        mBaiduMap.setMapStatus(mMapStatusUpdate);

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
        intent.putExtra(Constants.LONGITUDE, longitude);
        intent.putExtra(Constants.LATITUDE, latitude);
        setResult(RESULT_OK, intent);
        ActivityUtil.finish(this);
    }

    @OnClick(R.id.btn_cancel)
    private void cancel(View view){
        setResult(RESULT_CANCELED);
        ActivityUtil.finish(this);
    }


    @Override
    protected void onDestroy()
    {
        Log.d(TAG, "onDestroy");
        super.onDestroy();
        // 在activity执行onDestroy时执行mMapView.onDestroy()，实现地图生命周期管理
        mapView.onDestroy();
        mapView = null;
    }

    @Override
    protected void onResume()
    {
        super.onResume();
        // 在activity执行onResume时执行mMapView. onResume ()，实现地图生命周期管理
        mapView.onResume();
    }

    @Override
    protected void onPause()
    {
        super.onPause();
        // 在activity执行onPause时执行mMapView. onPause ()，实现地图生命周期管理
        mapView.onPause();

    }

}
