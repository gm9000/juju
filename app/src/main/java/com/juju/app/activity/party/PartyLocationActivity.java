package com.juju.app.activity.party;

import android.content.Intent;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

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
import com.baidu.mapapi.utils.DistanceUtil;
import com.juju.app.R;
import com.juju.app.config.HttpConstants;
import com.juju.app.entity.Plan;
import com.juju.app.entity.User;
import com.juju.app.event.notify.LocationReportEvent;
import com.juju.app.golobal.AppContext;
import com.juju.app.golobal.BitmapUtilFactory;
import com.juju.app.golobal.Constants;
import com.juju.app.golobal.JujuDbUtils;
import com.juju.app.service.notify.LocationReportNotify;
import com.juju.app.ui.base.BaseActivity;
import com.juju.app.utils.ActivityUtil;
import com.juju.app.utils.ImageLoaderUtil;
import com.juju.app.view.LocationImageView;
import com.rey.material.app.BottomSheetDialog;
import com.skyfishjy.library.RippleBackground;

import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;
import org.xutils.common.Callback;
import org.xutils.ex.DbException;
import org.xutils.image.ImageOptions;
import org.xutils.view.annotation.ContentView;
import org.xutils.view.annotation.ViewInject;

import java.util.HashMap;

import de.hdodenhof.circleimageview.CircleImageView;

@ContentView(R.layout.activity_party_location)
public class PartyLocationActivity extends BaseActivity implements View.OnClickListener, BaiduMap.OnMarkerClickListener, View.OnTouchListener{

    private static final String TAG = "PartyLocationActivity";

    @ViewInject(R.id.img_right)
    private ImageView img_right;
    @ViewInject(R.id.txt_title)
    private TextView txt_title;
    @ViewInject(R.id.img_back)
    private ImageView img_back;
    @ViewInject(R.id.txt_left)
    private TextView txt_left;


    @ViewInject(R.id.mapView)
    private MapView mapView;

    @ViewInject(R.id.mic_ripple)
    private RippleBackground micRipple;
    @ViewInject(R.id.img_mic)
    private ImageView imgMic;
    @ViewInject(R.id.img_locate)
    private ImageView imgLocate;

    private BaiduMap mBaiduMap;

    private double latitude;
    private double longitude;
    private String userNo;
    private float zoom = 18f;

    private HashMap<String,Marker> userMarkerMap;


    private LatLngBounds.Builder boundsBuilder;

    private  LatLng myLatLng;


    // 定位相关
    LocationClient mLocClient;
    public MyLocationListenner myListener = new MyLocationListenner();
    private boolean isFirstLoc = true; // 是否首次定位

    private MyLocationConfiguration.LocationMode mCurrentMode = MyLocationConfiguration.LocationMode.FOLLOWING;
    private static final int accuracyCircleFillColor = 0xAAFFFF88;
    private static final int accuracyCircleStrokeColor = 0xAA00FF00;


    private BottomSheetDialog msgDialog;
    private TextView txtCall;
    private TextView txtSms;
    private TextView txtCancel;
    private String targetPhone;

    private String groupId;
    private String partyId;
    private Plan plan;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EventBus.getDefault().register(this);
        initParam();
        initData();
        initView();
        addListener();

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

        if(EventBus.getDefault().isRegistered(this)){
            EventBus.getDefault().unregister(this);
        }

        //  报告退出位置分享
        LocationReportEvent.LocationReportBean quitBean = new LocationReportEvent.LocationReportBean();
        quitBean.setGroupId(groupId);
        quitBean.setPartyId(partyId);
        quitBean.setUserNo(AppContext.getUserInfoBean().getUserNo());
        quitBean.setNickName(AppContext.getUserInfoBean().getNickName());
        quitBean.setExit(true);
        LocationReportNotify.instance().executeCommand4Send(quitBean);
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

    private void initParam() {
        groupId = getIntent().getStringExtra(Constants.GROUP_ID);
        partyId = getIntent().getStringExtra(Constants.PARTY_ID);
        try {
            plan = JujuDbUtils.getInstance().selector(Plan.class).where("status", "=", 1).and("party_id", "=", partyId).findFirst();
        } catch (DbException e) {
            e.printStackTrace();
        }
    }

    public void initData() {

        latitude = plan.getLatitude();
        longitude = plan.getLongitude();

        userNo = AppContext.getUserInfoBean().getUserNo();

        userMarkerMap = new HashMap<String,Marker>();
        boundsBuilder = new LatLngBounds.Builder();


    }

    private void addListener() {
        img_back.setOnClickListener(this);
        txt_left.setOnClickListener(this);

        mBaiduMap.setOnMarkerClickListener(this);
        imgMic.setOnTouchListener(this);
        imgLocate.setOnClickListener(this);
    }

    private void initView() {
        img_back.setVisibility(View.VISIBLE);
        txt_left.setVisibility(View.VISIBLE);
        txt_left.setText(R.string.top_left_back);
        txt_title.setText(R.string.location);
        img_right.setVisibility(View.GONE);

        mBaiduMap = mapView.getMap();
        mBaiduMap.getUiSettings().setOverlookingGesturesEnabled(false);
        mBaiduMap.getUiSettings().setRotateGesturesEnabled(false);
        mBaiduMap.setMapType(BaiduMap.MAP_TYPE_NORMAL);

        mapView.showZoomControls(false);
        mapView.setLogoPosition(LogoPosition.logoPostionCenterBottom);

        initOverlay();

        // 开启定位图层
        mLocClient = new LocationClient(getContext());
        mLocClient.registerLocationListener(myListener);
        LocationClientOption option = new LocationClientOption();
        option.setOpenGps(true); // 打开gps
        option.setCoorType("bd09ll"); // 设置坐标类型
        option.setScanSpan(5000);
        mLocClient.setLocOption(option);
        mLocClient.start();

    }

    private void initOverlay() {

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
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.img_right:
                ActivityUtil.startActivityNew(this,PartyLiveActivity.class);
                break;
            case R.id.img_back:
                ActivityUtil.finish(this);
                break;
            case R.id.txt_left:
                ActivityUtil.finish(this);
                break;
            case R.id.txt_call:
                Uri telUri = Uri.parse("tel:" + targetPhone);
                Intent callIntent = new Intent(Intent. ACTION_DIAL , telUri);
                startActivity(callIntent);
                targetPhone = null;
                msgDialog.dismiss();
                break;
            case R.id.txt_sms:
                Uri uri = Uri.parse("smsto:" + targetPhone);
                Intent smsIntent = new Intent(Intent.ACTION_SENDTO, uri);
                startActivity(smsIntent);
                targetPhone = null;
                msgDialog.dismiss();
                break;
            case R.id.txt_cancel:
                targetPhone = null;
                msgDialog.dismiss();
                break;
            case R.id.img_locate:
                LatLng toLatLng = myLatLng;
                if(latitude!=0){
                    toLatLng = new LatLng(latitude,longitude);
                }
                MapStatusUpdate u = MapStatusUpdateFactory.newLatLng(toLatLng);
                mBaiduMap.animateMapStatus(u);
                break;
        }

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
        if(clickUserNo==null || clickUserNo.equals(userNo)){
            return true;
        }
        if(clickUserNo != null){
            User clickUser = null;
            try {
                clickUser = JujuDbUtils.getInstance()
                        .selector(User.class).where("user_no", "=", clickUserNo).findFirst();
            } catch (DbException e) {
                e.printStackTrace();
            }


            msgDialog = new BottomSheetDialog(this);
            msgDialog.contentView(R.layout.layout_friend_contact)
                    .inDuration(300);
            CircleImageView headImg = (CircleImageView)msgDialog.findViewById(R.id.img_head);
            txtCall = (TextView)msgDialog.findViewById(R.id.txt_call);
            txtSms = (TextView)msgDialog.findViewById(R.id.txt_sms);
            txtCancel = (TextView)msgDialog.findViewById(R.id.txt_cancel);
            ImageLoaderUtil.getImageLoaderInstance().displayImage(HttpConstants.getUserUrl() + "/getPortraitSmall?targetNo="
                    + clickUserNo, headImg, ImageLoaderUtil.DISPLAY_IMAGE_OPTIONS);
            txtCall.setOnClickListener(this);
            txtSms.setOnClickListener(this);
            txtCancel.setOnClickListener(this);

            targetPhone = clickUser.getUserPhone();
            msgDialog.show();
        }
        return true;
    }

    @Override
    public boolean onTouch(View v, MotionEvent event) {
        //按下操作
        if(event.getAction()==MotionEvent.ACTION_DOWN){
            micRipple.startRippleAnimation();
            return true;
        }
        //抬起操作
        if(event.getAction()==MotionEvent.ACTION_UP){
            micRipple.stopRippleAnimation();
            return true;
        }
        //移动操作
        if(event.getAction()==MotionEvent.ACTION_MOVE){

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

            myLatLng = new LatLng(location.getLatitude(), location.getLongitude());

            if (isFirstLoc) {
                isFirstLoc = false;
                UserLocationInfo myLocationInfo = new UserLocationInfo(userNo,location.getLatitude(),location.getLongitude(),true);
                myLocationInfo.showMapIcon();
                boundsBuilder.include(myLatLng);
                mBaiduMap.animateMapStatus(MapStatusUpdateFactory.newLatLngBounds(boundsBuilder.build()));
            }else{
                userMarkerMap.get(userNo).setPosition(myLatLng);
                mBaiduMap.hideInfoWindow();
            }

            double distance = 0;
            if(latitude != 0 ){
                distance = DistanceUtil.getDistance(new LatLng(latitude,longitude),myLatLng);
            }

            //  报告自己的位置
            LocationReportEvent.LocationReportBean locationReportBean = LocationReportEvent
                    .LocationReportBean.valueOf(groupId,partyId, myLatLng.latitude,myLatLng.longitude,distance,
                            AppContext.getUserInfoBean().getUserNo()
                            , AppContext.getUserInfoBean().getNickName());
            LocationReportNotify.instance().executeCommand4Send(locationReportBean);
        }
    }

    private class UserLocationInfo{
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
        public UserLocationInfo(String userNo,double latitude,double longitude,boolean self){
            this.userNo = userNo;
            this.latitude = latitude;
            this.longitude = longitude;
            headImg = new LocationImageView(getContext(),self);
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

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onEvent4LocationReport(LocationReportEvent event) {
        LocationReportEvent.LocationReportBean locationReportBean = event.bean;
        if(!locationReportBean.getPartyId().equals(partyId)){
            return;
        }

        if(locationReportBean.isExit()){
            if(userMarkerMap.containsKey(locationReportBean.getUserNo())) {
                userMarkerMap.get(locationReportBean.getUserNo()).remove();
                userMarkerMap.remove(locationReportBean.getUserNo());
            }
            return;
        }

        switch (event.event) {
            case LOCATION_REPORT_OK:
                if(!userMarkerMap.containsKey(locationReportBean.getUserNo())){
                    UserLocationInfo userLocationInfo = new UserLocationInfo(locationReportBean.getUserNo(),locationReportBean.getLatitude(),locationReportBean.getLongitude());
                    userLocationInfo.showMapIcon();
                    boundsBuilder.include(myLatLng);
                    mBaiduMap.animateMapStatus(MapStatusUpdateFactory.newLatLngBounds(boundsBuilder.build()));

                }else{
                    userMarkerMap.get(locationReportBean.getUserNo()).setPosition(new LatLng(locationReportBean.getLatitude(),locationReportBean.getLongitude()));
                    mBaiduMap.hideInfoWindow();
                }
                break;
        }
    }


}
