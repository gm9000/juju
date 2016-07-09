package com.juju.app.activity.party;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.baidu.location.BDLocation;
import com.baidu.location.BDLocationListener;
import com.baidu.location.LocationClient;
import com.baidu.location.LocationClientOption;
import com.baidu.mapapi.cloud.CloudListener;
import com.baidu.mapapi.cloud.CloudPoiInfo;
import com.baidu.mapapi.cloud.CloudSearchResult;
import com.baidu.mapapi.cloud.DetailSearchResult;
import com.baidu.mapapi.map.BaiduMap;
import com.baidu.mapapi.map.BitmapDescriptor;
import com.baidu.mapapi.map.BitmapDescriptorFactory;
import com.baidu.mapapi.map.LogoPosition;
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
import com.juju.app.utils.StringUtils;
import com.juju.app.utils.ToastUtil;

import org.xutils.view.annotation.ContentView;
import org.xutils.view.annotation.Event;
import org.xutils.view.annotation.ViewInject;


@ContentView(R.layout.layout_plan_location)
public class PlanLocationActivity extends BaseActivity implements OnGetGeoCoderResultListener,CloudListener {

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

    @ViewInject(R.id.img_address)
    private ImageView img_address;

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

    private boolean editMode = false;
    private double latitude;
    private double longitude;
    private String address;
    private float zoom = 18f;

    private Animation anim_zoom_out;

    // 定位相关
    LocationClient mLocClient;
    public MyLocationListenner myListener = new MyLocationListenner();
    private boolean isFirstLoc = true; // 是否首次定位

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        initParam();
        initView();
        initListener();
    }

    private void initListener() {

        if(editMode) {

            mBaiduMap.setOnMapStatusChangeListener(new BaiduMap.OnMapStatusChangeListener() {
                @Override
                public void onMapStatusChangeStart(MapStatus status) {
                    img_address.startAnimation(anim_zoom_out);
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
    }

    private void updateMapState(MapStatus status) {
        LatLng mCenterLatLng = status.target;
        /**获取经纬度*/
        latitude = mCenterLatLng.latitude;
        longitude = mCenterLatLng.longitude;

        LatLng ptCenter = new LatLng(latitude,longitude);
        mSearch.reverseGeoCode(new ReverseGeoCodeOption().location(ptCenter));

//        NearbySearchInfo info = new NearbySearchInfo();
//        info.ak = "CuKT3fnAh994GcU6eiFUE710cvqTe0Ag";
//        info.geoTableId = 8050884;
//        info.radius = 200;
//        info.location = latitude+","+longitude;
//        CloudManager.getInstance().nearbySearch(info);
    }


    private void initView() {

        anim_zoom_out = AnimationUtils.loadAnimation(this,R.anim.image_view_zoomout);

        txt_left.setVisibility(View.GONE);

        RelativeLayout.LayoutParams layoutParams = (RelativeLayout.LayoutParams)txt_left.getLayoutParams();
        layoutParams.leftMargin = 15;
        txt_title.setText(R.string.location_select);
        txt_left.setLayoutParams(layoutParams);
        txt_right.setVisibility(View.GONE);
        img_right.setVisibility(View.GONE);


        mapView.showZoomControls(false);

        mapView.setLogoPosition(LogoPosition.logoPostionCenterBottom);

        mBaiduMap = mapView.getMap();
        mBaiduMap.setMapType(BaiduMap.MAP_TYPE_NORMAL);

        MapStatus mMapStatus;
        if (!StringUtils.empty(address)) {
            LatLng p = null;
            txt_address.setText(address);
            if(latitude>0) {
                p = new LatLng(latitude, longitude);
                mMapStatus = new MapStatus.Builder().target(p).zoom(zoom).build();
            }else{
                mMapStatus = new MapStatus.Builder().zoom(zoom).build();
            }

            if(!editMode){
                txt_title.setText(R.string.location);
                layout_search.setVisibility(View.GONE);
                layout_button.setVisibility(View.GONE);
                RelativeLayout.LayoutParams lp = (RelativeLayout.LayoutParams) txt_address.getLayoutParams();
                lp.bottomMargin = 0;
                txt_address.setLayoutParams(lp);
                txt_address.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        MapStatus mMapStatus = new MapStatus.Builder().target(new LatLng(latitude,longitude)).build();
                        MapStatusUpdate mMapStatusUpdate = MapStatusUpdateFactory.newMapStatus(mMapStatus);
                        mBaiduMap.setMapStatus(mMapStatusUpdate);
                    }
                });
                if(p!=null) {
                    BitmapDescriptor bd = BitmapDescriptorFactory.fromResource(R.mipmap.party_location);
                    OverlayOptions oo = new MarkerOptions().icon(bd).position(p);
                    mBaiduMap.addOverlay(oo);
                }

                img_address.setVisibility(View.GONE);
            }

        } else {

            mMapStatus = new MapStatus.Builder().zoom(zoom).build();

            // 开启定位图层
            mLocClient = new LocationClient(this);
            mLocClient.registerLocationListener(myListener);
            LocationClientOption option = new LocationClientOption();
            option.setOpenGps(true); // 打开gps
            option.setCoorType("bd09ll"); // 设置坐标类型
            option.setScanSpan(1000);
            mLocClient.setLocOption(option);
            mLocClient.start();
        }
        MapStatusUpdate mMapStatusUpdate = MapStatusUpdateFactory.newMapStatus(mMapStatus);
        mBaiduMap.setMapStatus(mMapStatusUpdate);

        // 初始化搜索模块，注册事件监听
        mSearch = GeoCoder.newInstance();
    }


    public void initParam() {
        Intent intent = getIntent();
        editMode = intent.getBooleanExtra(Constants.EDIT_MODE,false);
        latitude = intent.getDoubleExtra(Constants.LATITUDE,0);
        longitude = intent.getDoubleExtra(Constants.LONGITUDE, 0);
        address = intent.getStringExtra(Constants.ADDRESS);

    }

    @Event(R.id.btn_search)
    private void searchAddress(View view){

        if(txt_search.getText().toString().equals("")){
            return;
        }

        String city = "北京";
        // Geo搜索
        mSearch.geocode(new GeoCodeOption().city(city).address(txt_search.getText().toString()));
    }

    @Event(R.id.btn_confirm)
    private void confirmLocation(View view){
        Intent intent = getIntent();
        intent.putExtra(Constants.ADDRESS,address);
        intent.putExtra(Constants.LONGITUDE, longitude);
        intent.putExtra(Constants.LATITUDE, latitude);
        setResult(RESULT_OK, intent);
        ActivityUtil.finish(this);
    }

    @Event(R.id.btn_cancel)
    private void cancel(View view){
        setResult(RESULT_CANCELED);
        ActivityUtil.finish(this);
    }


    @Override
    protected void onDestroy()
    {
        Log.d(TAG, "onDestroy");
        if(mLocClient!=null) {
            // 退出时销毁定位
            mLocClient.stop();
            // 关闭定位图层
            mBaiduMap.setMyLocationEnabled(false);
        }
        // 在activity执行onDestroy时执行mMapView.onDestroy()，实现地图生命周期管理
        mapView.onDestroy();
        mapView = null;
        super.onDestroy();
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


    @Override
    public void onGetDetailSearchResult(DetailSearchResult result, int error) {
        if (result != null) {
            if (result.poiInfo != null) {
                Toast.makeText(PlanLocationActivity.this, result.poiInfo.title,
                        Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(PlanLocationActivity.this,
                        "status:" + result.status, Toast.LENGTH_SHORT).show();
            }
        }
    }

    @Override
    public void onGetSearchResult(CloudSearchResult result, int error) {
        if (result != null && result.poiList != null
                && result.poiList.size() > 0) {
            Log.d(TAG, "onGetSearchResult, result length: " + result.poiList.size());
            mBaiduMap.clear();
            BitmapDescriptor bd = BitmapDescriptorFactory.fromResource(R.mipmap.location_blue);
            LatLng center = null;
            LatLng ll = null;
            for (CloudPoiInfo info : result.poiList) {
                if(center == null){
                    center = new LatLng(info.latitude, info.longitude);
                    latitude = info.latitude;
                    longitude = info.longitude;
                    txt_address.setText(info.address);
                }else {
                    ll = new LatLng(info.latitude, info.longitude);
                    OverlayOptions oo = new MarkerOptions().icon(bd).position(ll);
                    mBaiduMap.addOverlay(oo);
                }
            }

            MapStatus mMapStatus = new MapStatus.Builder().target(center).zoom(zoom).build();
            MapStatusUpdate u = MapStatusUpdateFactory.newMapStatus(mMapStatus);
            mBaiduMap.animateMapStatus(u);
        }
    }

    /**
     * 定位SDK监听函数
     */
    private class MyLocationListenner implements BDLocationListener {

        @Override
        public void onReceiveLocation(BDLocation location) {
            // map view 销毁后不在处理新接收的位置
            if (location == null || mapView == null) {
                return;
            }
            if (isFirstLoc) {
                isFirstLoc = false;
                LatLng ll = new LatLng(location.getLatitude(),
                        location.getLongitude());
                MapStatus.Builder builder = new MapStatus.Builder();
                builder.target(ll).zoom(18.0f);
                mBaiduMap.animateMapStatus(MapStatusUpdateFactory.newMapStatus(builder.build()));
            }
        }
    }


}
