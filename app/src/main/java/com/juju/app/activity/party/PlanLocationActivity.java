package com.juju.app.activity.party;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.baidu.mapapi.map.BaiduMap;
import com.baidu.mapapi.map.BitmapDescriptor;
import com.baidu.mapapi.map.BitmapDescriptorFactory;
import com.baidu.mapapi.map.MapStatus;
import com.baidu.mapapi.map.MapStatusUpdate;
import com.baidu.mapapi.map.MapStatusUpdateFactory;
import com.baidu.mapapi.map.MapView;
import com.baidu.mapapi.map.MarkerOptions;
import com.baidu.mapapi.map.OverlayOptions;
import com.baidu.mapapi.model.LatLng;
import com.baidu.mapapi.search.core.SearchResult;
import com.baidu.mapapi.search.geocode.GeoCodeOption;
import com.baidu.mapapi.search.geocode.GeoCodeResult;
import com.baidu.mapapi.search.geocode.GeoCoder;
import com.baidu.mapapi.search.geocode.OnGetGeoCoderResultListener;
import com.baidu.mapapi.search.geocode.ReverseGeoCodeOption;
import com.baidu.mapapi.search.geocode.ReverseGeoCodeResult;
import com.juju.app.R;
import com.juju.app.golobal.Constants;
import com.juju.app.ui.base.BaseActivity;
import com.juju.app.utils.ActivityUtil;
import com.juju.app.utils.ToastUtil;
import com.lidroid.xutils.view.annotation.ContentView;
import com.lidroid.xutils.view.annotation.ViewInject;
import com.lidroid.xutils.view.annotation.event.OnClick;


@ContentView(R.layout.layout_plan_location)
public class PlanLocationActivity extends BaseActivity implements OnGetGeoCoderResultListener {

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

    @ViewInject(R.id.txt_address)
    private TextView txt_address;

    @ViewInject(R.id.txt_search)
    private EditText txt_search;
    @ViewInject(R.id.btn_search)
    private Button btn_search;

    @ViewInject(R.id.layout_search)
    private RelativeLayout layout_search;
    @ViewInject(R.id.layout_button)
    private LinearLayout layout_button;


    @ViewInject(R.id.mapView)
    private MapView mapView;
    private BaiduMap mBaiduMap;
    GeoCoder mSearch = null; // 搜索模块，也可去掉地图模块独立使用

    private double latitude;
    private double longitude;
    private String address;
    private float zoom = 16f;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        initParam();
        initView();
        initListener();
    }

    private void initListener() {

        mBaiduMap.setOnMapStatusChangeListener(new BaiduMap.OnMapStatusChangeListener() {
            @Override
            public void onMapStatusChangeStart(MapStatus status) {
            }

            @Override
            public void onMapStatusChangeFinish(MapStatus status) {
                updateMapState(status);
            }

            @Override
            public void onMapStatusChange(MapStatus status) {
            }
        });

        mSearch.setOnGetGeoCodeResultListener(this);
    }

    private void updateMapState(MapStatus status) {
        LatLng mCenterLatLng = status.target;
        /**获取经纬度*/
        latitude = mCenterLatLng.latitude;
        longitude = mCenterLatLng.longitude;

        LatLng ptCenter = new LatLng(latitude,longitude);
        // 反Geo搜索
        mSearch.reverseGeoCode(new ReverseGeoCodeOption().location(ptCenter));
    }


    private void initView() {

        txt_left.setVisibility(View.GONE);

        RelativeLayout.LayoutParams layoutParams = (RelativeLayout.LayoutParams)txt_left.getLayoutParams();
        layoutParams.leftMargin = 15;
        txt_title.setText(R.string.location_select);
        txt_left.setLayoutParams(layoutParams);
        txt_right.setVisibility(View.GONE);
        img_right.setVisibility(View.GONE);

        if(address != null){
            layout_search.setVisibility(View.GONE);
            layout_button.setVisibility(View.GONE);
        }

        mapView.showZoomControls(true);

        mBaiduMap = mapView.getMap();
        mBaiduMap.setMapType(BaiduMap.MAP_TYPE_NORMAL);

        MapStatus mMapStatus;
        if (latitude > 0 && longitude > 0) {
            LatLng p = new LatLng(latitude, longitude);
            mMapStatus = new MapStatus.Builder().target(p).zoom(zoom).build();
        } else {
            mMapStatus = new MapStatus.Builder().zoom(zoom).build();
        }
        MapStatusUpdate mMapStatusUpdate = MapStatusUpdateFactory.newMapStatus(mMapStatus);
        mBaiduMap.setMapStatus(mMapStatusUpdate);

        // 初始化搜索模块，注册事件监听
        mSearch = GeoCoder.newInstance();

    }


    public void initParam() {
        Intent intent = getIntent();
        latitude = intent.getDoubleExtra(Constants.LATITUDE,0);
        longitude = intent.getDoubleExtra(Constants.LONGITUDE,0);
        address = intent.getStringExtra(Constants.ADDRESS);
    }

    @OnClick(R.id.btn_search)
    private void searchAddress(View view){

        if(txt_search.getText().toString().equals("")){
            return;
        }

        String city = "北京";
        // Geo搜索
        mSearch.geocode(new GeoCodeOption().city(city).address(txt_search.getText().toString()));
    }

    @OnClick(R.id.btn_confirm)
    private void confirmLocation(View view){
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

    @Override
    public void onGetGeoCodeResult(GeoCodeResult result) {
        if (result == null || result.error != SearchResult.ERRORNO.NO_ERROR) {
            ToastUtil.showShortToast(this, "抱歉，未能找到结果", 1);
            return;
        }
        LatLng location = result.getLocation();
        latitude = location.latitude;
        longitude = location.longitude;
        mBaiduMap.setMapStatus(MapStatusUpdateFactory.newLatLng(location));
        txt_address.setText(txt_search.getText());
    }

    @Override
    public void onGetReverseGeoCodeResult(ReverseGeoCodeResult reverseGeoCodeResult) {
        address = reverseGeoCodeResult.getAddress();
        txt_address.setText(address);
    }
}
