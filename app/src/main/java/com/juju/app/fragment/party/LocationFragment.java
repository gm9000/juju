package com.juju.app.fragment.party;

import android.annotation.SuppressLint;
import android.graphics.drawable.Drawable;
import android.util.Log;
import android.view.View;
import android.widget.Button;

import com.baidu.location.BDLocation;
import com.baidu.location.BDLocationListener;
import com.baidu.location.LocationClient;
import com.baidu.location.LocationClientOption;
import com.baidu.mapapi.map.BaiduMap;
import com.baidu.mapapi.map.BitmapDescriptor;
import com.baidu.mapapi.map.BitmapDescriptorFactory;
import com.baidu.mapapi.map.LogoPosition;
import com.baidu.mapapi.map.MapStatusUpdate;
import com.baidu.mapapi.map.MapStatusUpdateFactory;
import com.baidu.mapapi.map.MapView;
import com.baidu.mapapi.map.Marker;
import com.baidu.mapapi.map.MarkerOptions;
import com.baidu.mapapi.map.MyLocationConfiguration;
import com.baidu.mapapi.map.OverlayOptions;
import com.baidu.mapapi.model.LatLng;
import com.baidu.mapapi.model.LatLngBounds;
import com.juju.app.R;
import com.juju.app.activity.party.PartyActivity;
import com.juju.app.annotation.CreateFragmentUI;
import com.juju.app.config.HttpConstants;
import com.juju.app.entity.Plan;
import com.juju.app.entity.User;
import com.juju.app.golobal.BitmapUtilFactory;
import com.juju.app.golobal.JujuDbUtils;
import com.juju.app.ui.base.BaseApplication;
import com.juju.app.ui.base.BaseFragment;
import com.juju.app.ui.base.CreateUIHelper;
import com.juju.app.utils.ToastUtil;
import com.juju.app.view.LocationImageView;

import org.xutils.common.Callback;
import org.xutils.ex.DbException;
import org.xutils.image.ImageOptions;
import org.xutils.view.annotation.ContentView;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Random;

@SuppressLint("ValidFragment")
@ContentView(R.layout.fragment_location)
@CreateFragmentUI(viewId = R.layout.fragment_location)
public class LocationFragment extends BaseFragment implements CreateUIHelper, BaiduMap.OnMarkerClickListener {

    private static final String TAG = "LocationFragment";

    private PartyActivity parentActivity;

    private Plan plan;

    private MapView mapView;
    private Button btnUpdateLocation;

    private BaiduMap mBaiduMap;

    private double latitude;
    private double longitude;
    private String userNo;
    private float zoom = 18f;

    private HashMap<String,Marker> userMarkerMap;
    private List<UserLocationInfo> userLocationInfoList;

    private List<LatLng> locationList;

    private LatLngBounds.Builder boundsBuilder;


    // 定位相关
    LocationClient mLocClient;
    public MyLocationListenner myListener = new MyLocationListenner();
    private boolean isFirstLoc = true; // 是否首次定位

    private MyLocationConfiguration.LocationMode mCurrentMode = MyLocationConfiguration.LocationMode.FOLLOWING;
    private static final int accuracyCircleFillColor = 0xAAFFFF88;
    private static final int accuracyCircleStrokeColor = 0xAA00FF00;

    public LocationFragment(Plan plan){
        super();
        this.plan = plan;
    }


    @Override
    protected void findViews() {

        mapView = (MapView)findViewById(R.id.mapView);
        mBaiduMap = mapView.getMap();
        mBaiduMap.getUiSettings().setOverlookingGesturesEnabled(false);
        mBaiduMap.getUiSettings().setRotateGesturesEnabled(false);
        mBaiduMap.setMapType(BaiduMap.MAP_TYPE_NORMAL);

        btnUpdateLocation = (Button)findViewById(R.id.btn_update_location);

        parentActivity = (PartyActivity) getActivity();

    }

    @Override
    public void loadData() {

        locationList = new ArrayList<LatLng>();
        locationList.add(new LatLng(39.9128964508378,116.317821504652));
        locationList.add(new LatLng(39.9594476748648,116.310460947917));
        locationList.add(new LatLng(39.9727856437081,116.328102464588));
        locationList.add(new LatLng(39.9489004384569,116.331535766545));
        locationList.add(new LatLng(39.974325406894,116.386965867815));
        locationList.add(new LatLng(40.0245711174139,116.396177490971));
        locationList.add(new LatLng(39.9682743181947,116.316048927732));
        locationList.add(new LatLng(39.9222810304695,116.401137771222));
        locationList.add(new LatLng(39.8872623839031,116.38875275265));
        locationList.add(new LatLng(40.0368943327654,116.391552769546));

        latitude = plan.getLatitude();
        longitude = plan.getLongitude();

        userNo = BaseApplication.getInstance().getUserInfoBean().getJujuNo();

        userMarkerMap = new HashMap<String,Marker>();
        userLocationInfoList = new ArrayList<UserLocationInfo>();
        boundsBuilder = new LatLngBounds.Builder();

        String userNo = "100000002";
        UserLocationInfo userLocationInfo = new UserLocationInfo(userNo,39.9727856437081,116.328102464588);
        userLocationInfoList.add(userLocationInfo);

        String userNo1 = "100000003";
        UserLocationInfo userLocationInfo1 = new UserLocationInfo(userNo1,39.9128964508378,116.317821504652);
        userLocationInfoList.add(userLocationInfo1);

        String userNo2 = "100000005";
        UserLocationInfo userLocationInfo2 = new UserLocationInfo(userNo2,39.974325406894,116.386965867815);
        userLocationInfoList.add(userLocationInfo2);

        String userNo3 = "100000001";
        UserLocationInfo userLocationInfo3 = new UserLocationInfo(userNo3,39.9222810304695,116.401137771222);
        userLocationInfoList.add(userLocationInfo3);

        String userNo4 = "100000004";
        UserLocationInfo userLocationInfo4 = new UserLocationInfo(userNo4,40.0368943327654,116.391552769546);
        userLocationInfoList.add(userLocationInfo4);

    }

    @Override
    public void initView() {

        mapView.showZoomControls(false);
        mapView.setLogoPosition(LogoPosition.logoPostionCenterBottom);

        initOverlay();

        // 开启定位图层
        mLocClient = new LocationClient(getContext());
        mLocClient.registerLocationListener(myListener);
        LocationClientOption option = new LocationClientOption();
        option.setOpenGps(true); // 打开gps
        option.setCoorType("bd09ll"); // 设置坐标类型
        option.setScanSpan(1000);
        mLocClient.setLocOption(option);
        mLocClient.start();

    }

    @Override
    public void setOnListener() {

        btnUpdateLocation.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                updateLocation();
            }
        });

        mBaiduMap.setOnMarkerClickListener(this);
    }

    private void updateLocation() {
        for(String targetNo:userMarkerMap.keySet()){
            if(targetNo.equals(userNo)){
                continue;
            }
            userMarkerMap.get(targetNo).setPosition(locationList.get(new Random().nextInt(10)));
            mBaiduMap.hideInfoWindow();
        }
    }


    public void initOverlay() {

        for(UserLocationInfo locationInfo:userLocationInfoList){
            locationInfo.showMapIcon();
            boundsBuilder.include(locationInfo.getLocation());
        }

        if (latitude != 0) {
            LatLng center = new LatLng(latitude, longitude);

            BitmapDescriptor bd = BitmapDescriptorFactory.fromResource(R.mipmap.party_location);
            OverlayOptions oo = new MarkerOptions().icon(bd).position(center).zIndex(10);
            mBaiduMap.addOverlay(oo);

            boundsBuilder.include(center);
        }

        LatLngBounds bounds = boundsBuilder.build();
        MapStatusUpdate u = MapStatusUpdateFactory.newLatLngBounds(bounds);
        mBaiduMap.animateMapStatus(u);
    }


    @Override
    public void onDestroy()
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
    public void onResume()
    {
        super.onResume();
        // 在activity执行onResume时执行mMapView. onResume ()，实现地图生命周期管理
        mapView.onResume();
    }

    @Override
    public void onPause()
    {
        super.onPause();
        // 在activity执行onPause时执行mMapView. onPause ()，实现地图生命周期管理
        mapView.onPause();

    }

    /**
     * 刷新页面
     */
    public void refresh() {

    }

    @Override
    public boolean onMarkerClick(Marker marker) {
        String clickUserNo = null;
        for(String targetNo:userMarkerMap.keySet()){
            if(marker == userMarkerMap.get(targetNo)){
                clickUserNo = targetNo;
                break;
            }
        }
        if(clickUserNo != null){
            User clickUser = null;
            try {
                clickUser = JujuDbUtils.getInstance()
                        .selector(User.class).where("user_no", "=", clickUserNo).findFirst();
            } catch (DbException e) {
                e.printStackTrace();
            }
            ToastUtil.showShortToast(getContext(),clickUser.getNickName(),1);
        }
        return true;
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

            LatLng ll = new LatLng(location.getLatitude(), location.getLongitude());
            if (isFirstLoc) {
                isFirstLoc = false;
                UserLocationInfo myLocationInfo = new UserLocationInfo(userNo,location.getLatitude(),location.getLongitude());
                userLocationInfoList.add(myLocationInfo);
                myLocationInfo.showMapIcon();
                boundsBuilder.include(ll);
                mBaiduMap.animateMapStatus(MapStatusUpdateFactory.newLatLngBounds(boundsBuilder.build()));
            }else{
                userMarkerMap.get(userNo).setPosition(ll);
                mBaiduMap.hideInfoWindow();
            }
        }
    }

    public class UserLocationInfo{
        private double latitude;
        private double longitude;
        private LocationImageView headImg;
        private String userNo;

        public UserLocationInfo(String userNo,double latitude,double longitude){
            this.userNo = userNo;
            this.latitude = latitude;
            this.longitude = longitude;
            headImg = new LocationImageView(getContext());
        }

        public LatLng getLocation() {
            return new LatLng(latitude,longitude);
        }

        public String getUserNo(){
            return this.userNo;
        }

        public void showMapIcon() {
            BitmapUtilFactory.getInstance(getContext()).loadDrawable(HttpConstants.getUserUrl() + "/getPortraitSmall?targetNo=" + userNo, ImageOptions.DEFAULT, new Callback.CommonCallback<Drawable>() {
                @Override
                public void onSuccess(Drawable result) {
                    headImg.setImageDrawable(result);
                    MarkerOptions mo = new MarkerOptions().position(getLocation()).icon(BitmapDescriptorFactory.fromView(headImg));
                    userMarkerMap.put(userNo, (Marker) mBaiduMap.addOverlay(mo));
                }

                @Override
                public void onError(Throwable ex, boolean isOnCallback) {

                }

                @Override
                public void onCancelled(CancelledException cex) {

                }

                @Override
                public void onFinished() {

                }
            });

        }
    }


}
