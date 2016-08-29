package com.juju.app.activity.party;

import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.View;
import android.view.WindowManager;
import android.widget.AdapterView;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.baidu.location.BDLocation;
import com.baidu.location.BDLocationListener;
import com.baidu.location.LocationClient;
import com.baidu.location.LocationClientOption;
import com.baidu.mapapi.map.BaiduMap;
import com.baidu.mapapi.map.BitmapDescriptor;
import com.baidu.mapapi.map.BitmapDescriptorFactory;
import com.baidu.mapapi.map.InfoWindow;
import com.baidu.mapapi.map.LogoPosition;
import com.baidu.mapapi.map.MapStatus;
import com.baidu.mapapi.map.MapStatusUpdate;
import com.baidu.mapapi.map.MapStatusUpdateFactory;
import com.baidu.mapapi.map.MapView;
import com.baidu.mapapi.map.Marker;
import com.baidu.mapapi.map.MarkerOptions;
import com.baidu.mapapi.map.OverlayOptions;
import com.baidu.mapapi.model.LatLng;
import com.baidu.mapapi.search.core.PoiInfo;
import com.baidu.mapapi.search.core.SearchResult;
import com.baidu.mapapi.search.geocode.GeoCodeResult;
import com.baidu.mapapi.search.geocode.GeoCoder;
import com.baidu.mapapi.search.geocode.OnGetGeoCoderResultListener;
import com.baidu.mapapi.search.geocode.ReverseGeoCodeOption;
import com.baidu.mapapi.search.geocode.ReverseGeoCodeResult;
import com.baidu.mapapi.search.poi.OnGetPoiSearchResultListener;
import com.baidu.mapapi.search.poi.PoiCitySearchOption;
import com.baidu.mapapi.search.poi.PoiDetailResult;
import com.baidu.mapapi.search.poi.PoiDetailSearchOption;
import com.baidu.mapapi.search.poi.PoiResult;
import com.baidu.mapapi.search.poi.PoiSearch;
import com.baidu.mapapi.search.sug.OnGetSuggestionResultListener;
import com.baidu.mapapi.search.sug.SuggestionResult;
import com.baidu.mapapi.search.sug.SuggestionSearch;
import com.baidu.mapapi.search.sug.SuggestionSearchOption;
import com.juju.app.R;
import com.juju.app.adapters.SuggestionListAdapter;
import com.juju.app.bean.SuggestionBean;
import com.juju.app.golobal.Constants;
import com.juju.app.golobal.GlobalVariable;
import com.juju.app.ui.base.BaseActivity;
import com.juju.app.utils.ActivityUtil;
import com.juju.app.utils.ScreenUtil;
import com.juju.app.utils.StringUtils;
import com.juju.app.utils.ToastUtil;
import com.juju.app.view.map.PoiOverlay;

import org.xutils.view.annotation.ContentView;
import org.xutils.view.annotation.Event;
import org.xutils.view.annotation.ViewInject;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;


@ContentView(R.layout.layout_plan_location)
public class PlanLocationActivity extends BaseActivity implements OnGetPoiSearchResultListener, OnGetSuggestionResultListener, AdapterView.OnItemClickListener {

    private static final String TAG = "PlanLocationActivity";

    private static final int SELECT_CITY = 0x01;

    @ViewInject(R.id.txt_address)
    private TextView txt_address;

    @ViewInject(R.id.txt_search)
    private EditText txt_search;
    @ViewInject(R.id.txt_city)
    private TextView txtCity;
    @ViewInject(R.id.txt_reset)
    private TextView txtReset;
    @ViewInject(R.id.listview_suggestion)
    private ListView listViewSuggestion;

    private SuggestionListAdapter suggestionListAdapter;
    private List<SuggestionBean> suggestionBeanList;

    @ViewInject(R.id.layout_search)
    private RelativeLayout layout_search;
    @ViewInject(R.id.layout_button)
    private LinearLayout layout_button;


    @ViewInject(R.id.mapView)
    private MapView mapView;
    private BaiduMap mBaiduMap;

    private boolean editMode = false;
    private double latitude;
    private double longitude;
    private String address;
    private float zoom = 18f;

    // 定位相关
    LocationClient mLocClient;
    public MyLocationListenner myListener = new MyLocationListenner();
    private boolean isFirstLoc = true; // 是否首次定位

    private String targetCity = null;
    private String locateCity = null;
    private PoiSearch mPoiSearch = null;
    private SuggestionSearch mSuggestionSearch = null;
    private int loadIndex = 0;  //  搜索结果分页索引
    private int suggestionLength = 0;
    private boolean suggestionFinish = false;
    private int geoDecodeCount = 0;

    private Marker locationMarker;
    private Marker clickMarker;
    private  BitmapDescriptor originBitmapDescriptor;
    private PoiOverlay overlay;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        initParam();
        initView();
        initListener();
        getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_PAN);
    }

    public void initParam() {
        Intent intent = getIntent();
        editMode = intent.getBooleanExtra(Constants.EDIT_MODE,false);
        latitude = intent.getDoubleExtra(Constants.LATITUDE,0);
        longitude = intent.getDoubleExtra(Constants.LONGITUDE, 0);
        address = intent.getStringExtra(Constants.ADDRESS);

    }

    private void initView() {

        // 初始化搜索模块，注册事件监听

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
                final GeoCoder mSearch = GeoCoder.newInstance();
                mSearch.setOnGetGeoCodeResultListener(new OnGetGeoCoderResultListener() {
                    @Override
                    public void onGetGeoCodeResult(GeoCodeResult geoCodeResult) {
                    }
                    @Override
                    public void onGetReverseGeoCodeResult(ReverseGeoCodeResult reverseGeoCodeResult) {
                        targetCity = reverseGeoCodeResult.getAddressDetail().city;
                        txtCity.setText(targetCity);
                        mSearch.destroy();
                    }
                });
                mSearch.reverseGeoCode(new ReverseGeoCodeOption().location(p));
            }else{
                mMapStatus = new MapStatus.Builder().zoom(zoom).build();
            }

            if(!editMode){
                layout_search.setVisibility(View.GONE);
                layout_button.setVisibility(View.GONE);
                RelativeLayout.LayoutParams lp = (RelativeLayout.LayoutParams) txt_address.getLayoutParams();
                lp.bottomMargin = 0;
                txt_address.setLayoutParams(lp);
            }

            if(p!=null) {
                BitmapDescriptor bd = BitmapDescriptorFactory.fromResource(R.mipmap.party_location);
                OverlayOptions oo = new MarkerOptions().icon(bd).position(p);
                mBaiduMap.addOverlay(oo);

                TextView targetAddress = new TextView(context);
                targetAddress.setText(address);
                targetAddress.setBackgroundColor(Color.parseColor("#90FFFFFF"));
                targetAddress.setPadding(5,0,5,0);
                InfoWindow infoWindow = new InfoWindow(targetAddress, p, ScreenUtil.dip2px(this,20));
                mBaiduMap.showInfoWindow(infoWindow);
            }

        } else {
            mMapStatus = new MapStatus.Builder().zoom(zoom).build();
        }

        // 开启定位图层
        mLocClient = new LocationClient(this);
        mLocClient.registerLocationListener(myListener);
        LocationClientOption option = new LocationClientOption();
        option.setOpenGps(true); // 打开gps
        option.setCoorType("bd09ll"); // 设置坐标类型
        option.setScanSpan(1000);
        mLocClient.setLocOption(option);
        mLocClient.start();

        MapStatusUpdate mMapStatusUpdate = MapStatusUpdateFactory.newMapStatus(mMapStatus);
        mBaiduMap.setMapStatus(mMapStatusUpdate);

        suggestionBeanList = new ArrayList<SuggestionBean>();
        suggestionListAdapter = new SuggestionListAdapter(this,suggestionBeanList);
        listViewSuggestion.setAdapter(suggestionListAdapter);

        if(editMode){
            mPoiSearch = PoiSearch.newInstance();
            mSuggestionSearch = SuggestionSearch.newInstance();
        }

        txtCity.setText(targetCity==null? GlobalVariable.defaultCity:targetCity);

    }

    private void initListener() {

        if(editMode) {

            mPoiSearch.setOnGetPoiSearchResultListener(this);
            mSuggestionSearch.setOnGetSuggestionResultListener(this);

            /**
             * 当输入关键字变化时，动态更新建议列表
             */
            txt_search.addTextChangedListener(new TextWatcher() {

                @Override
                public void afterTextChanged(Editable arg0) {

                }

                @Override
                public void beforeTextChanged(CharSequence arg0, int arg1,
                                              int arg2, int arg3) {

                }

                @Override
                public void onTextChanged(CharSequence cs, int arg1, int arg2,
                                          int arg3) {
                    if (cs.length() <= 0) {
                        listViewSuggestion.setVisibility(View.GONE);
                        txtReset.setVisibility(View.GONE);
                        return;
                    }
                    txtReset.setVisibility(View.VISIBLE);
                    mSuggestionSearch
                            .requestSuggestion((new SuggestionSearchOption())
                                    .keyword(cs.toString()).city(targetCity==null? GlobalVariable.defaultCity:targetCity));
                }
            });
        }

        listViewSuggestion.setOnItemClickListener(this);

        txt_address.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(latitude>0) {
                    MapStatus mMapStatus = new MapStatus.Builder().target(new LatLng(latitude, longitude)).build();
                    MapStatusUpdate mMapStatusUpdate = MapStatusUpdateFactory.newMapStatus(mMapStatus);
                    mBaiduMap.setMapStatus(mMapStatusUpdate);
                }
            }
        });
    }

    private void searchAddress(String searchKey){
        mPoiSearch.searchInCity((new PoiCitySearchOption())
                .city(targetCity==null? GlobalVariable.defaultCity:targetCity).keyword(searchKey).pageNum(loadIndex));
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

    @Event(R.id.txt_reset)
    private void clearSearch(View view){
        txt_search.setText("");
        listViewSuggestion.setVisibility(View.GONE);
        txtReset.setVisibility(View.GONE);
    }

    @Event(R.id.txt_city)
    private void selectCity(View view){

        Map<String,String> paramMap = new HashMap<String,String>();
        paramMap.put(Constants.CITY,targetCity);
        paramMap.put(Constants.LOCATE_CITY,locateCity);
        startActivityForResultNew(PlanLocationActivity.this, CitySelectActivity.class,SELECT_CITY,paramMap);
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (resultCode == RESULT_OK) {
            switch (requestCode) {
                case SELECT_CITY:
                String city = data.getStringExtra(Constants.SELECTED_CITY);
                targetCity = city;
                txtCity.setText(city);
                break;
            }
        }
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
        if( mPoiSearch !=null) {
            mPoiSearch.destroy();
        }
        if(mSuggestionSearch != null) {
            mSuggestionSearch.destroy();
        }
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
    public void onGetPoiResult(PoiResult result) {
        if (result == null || result.error == SearchResult.ERRORNO.RESULT_NOT_FOUND || result.error == SearchResult.ERRORNO.AMBIGUOUS_KEYWORD) {
            String strInfo = "在 " + targetCity + "市 未找到结果";
            ToastUtil.showShortToast(PlanLocationActivity.this, strInfo, 1);
            return;
        }
        if (result.error == SearchResult.ERRORNO.NO_ERROR) {
            mBaiduMap.clear();
            overlay = new MyPoiOverlay(mBaiduMap);
            mBaiduMap.setOnMarkerClickListener(overlay);
            overlay.setData(result);
            overlay.addToMap();
            overlay.zoomToSpan();
            return;
        }
    }

    @Override
    public void onGetPoiDetailResult(PoiDetailResult result) {
        if (result.error != SearchResult.ERRORNO.NO_ERROR) {
            ToastUtil.showShortToast(PlanLocationActivity.this, "抱歉，未找到结果", 1);
        } else {
            LatLng target = result.getLocation();
            latitude = target.latitude;
            longitude = target.longitude;
            mBaiduMap.setMapStatus(MapStatusUpdateFactory.newLatLng(target));
            BitmapDescriptor bd = BitmapDescriptorFactory.fromResource(R.mipmap.party_location);
            OverlayOptions oo = new MarkerOptions().icon(bd).position(target);
            if(locationMarker != null){
                locationMarker.remove();
            }
            locationMarker = (Marker) mBaiduMap.addOverlay(oo);

            mBaiduMap.hideInfoWindow();
            TextView targetAddress = new TextView(context);
            targetAddress.setText(result.getName());
            targetAddress.setBackgroundColor(Color.parseColor("#90FFFFFF"));
            targetAddress.setPadding(5,0,5,0);
            InfoWindow infoWindow = new InfoWindow(targetAddress, target, ScreenUtil.dip2px(this,20));
            mBaiduMap.showInfoWindow(infoWindow);

            originBitmapDescriptor = clickMarker.getIcon();
            clickMarker.setIcon(bd);
            txt_address.setText(result.getName());
            address = result.getName();
        }
    }

    @Override
    public void onGetSuggestionResult(SuggestionResult res) {

        if (res == null || res.getAllSuggestions() == null) {
            return;
        }

        listViewSuggestion.setVisibility(View.VISIBLE);
        suggestionLength = 0;
        suggestionFinish = false;
        geoDecodeCount = 0;
        suggestionBeanList = new ArrayList<SuggestionBean>();
        suggestionListAdapter.setSuggestionList(suggestionBeanList);
        listViewSuggestion.setAdapter(suggestionListAdapter);
        for (SuggestionResult.SuggestionInfo info : res.getAllSuggestions()) {
            if (info.key != null) {
                final SuggestionBean bean = new SuggestionBean();
                bean.setKey(info.key);
                if(info.pt != null) {
                    bean.setLocation(info.pt);
                    final GeoCoder addressDeCoder = GeoCoder.newInstance();
                    addressDeCoder.setOnGetGeoCodeResultListener(new OnGetGeoCoderResultListener() {
                        @Override
                        public void onGetGeoCodeResult(GeoCodeResult geoCodeResult) {
                        }
                        @Override
                        public void onGetReverseGeoCodeResult(ReverseGeoCodeResult reverseGeoCodeResult) {
                                bean.setAddress(reverseGeoCodeResult.getAddress());
                                suggestionBeanList.add(bean);
                                geoDecodeCount++;
                                if(suggestionFinish && geoDecodeCount==suggestionLength) {
                                    suggestionListAdapter.notifyDataSetChanged();
                                }
                            addressDeCoder.destroy();
                        }
                    });
                    if(addressDeCoder.reverseGeoCode(new ReverseGeoCodeOption().location(info.pt))){
                        suggestionLength++;
                    }

                }else{
                    suggestionBeanList.add(bean);
                }
            }
        }
        if(suggestionLength == 0){
            suggestionListAdapter.notifyDataSetChanged();
        }
        suggestionFinish = true;

    }

    @Override
    public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
        SuggestionBean bean = suggestionBeanList.get(position);
        txt_search.setText("");
        listViewSuggestion.setVisibility(View.GONE);
        if(bean.getAddress() == null){
            searchAddress(bean.getKey());
        }else{

            LatLng target = bean.getLocation();
            latitude = target.latitude;
            longitude = target.longitude;
            mBaiduMap.setMapStatus(MapStatusUpdateFactory.newLatLng(target));
            BitmapDescriptor bd = BitmapDescriptorFactory.fromResource(R.mipmap.party_location);
            OverlayOptions oo = new MarkerOptions().icon(bd).position(target);
            if(locationMarker != null){
                locationMarker.remove();
            }
            locationMarker = (Marker) mBaiduMap.addOverlay(oo);
            if(overlay!=null) {
                overlay.removeFromMap();
            }
            mBaiduMap.hideInfoWindow();
            TextView targetAddress = new TextView(context);
            targetAddress.setText(bean.getKey());
            targetAddress.setBackgroundColor(Color.parseColor("#90FFFFFF"));
            targetAddress.setPadding(5,0,5,0);
            InfoWindow infoWindow = new InfoWindow(targetAddress, target, ScreenUtil.dip2px(this,20));
            mBaiduMap.showInfoWindow(infoWindow);

            txt_address.setText(bean.getKey());
            address = bean.getKey();
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
                LatLng ll = new LatLng(location.getLatitude(),location.getLongitude());
                if(latitude==0) {
                    MapStatus.Builder builder = new MapStatus.Builder();
                    builder.target(ll).zoom(18.0f);
                    mBaiduMap.animateMapStatus(MapStatusUpdateFactory.newMapStatus(builder.build()));
                }
                final GeoCoder mSearch = GeoCoder.newInstance();
                mSearch.setOnGetGeoCodeResultListener(new OnGetGeoCoderResultListener() {
                    @Override
                    public void onGetGeoCodeResult(GeoCodeResult geoCodeResult) {
                    }
                    @Override
                    public void onGetReverseGeoCodeResult(ReverseGeoCodeResult reverseGeoCodeResult) {
                        locateCity = reverseGeoCodeResult.getAddressDetail().city;
                        if(latitude==0){
                            targetCity = reverseGeoCodeResult.getAddressDetail().city;;
                        }
                        mSearch.destroy();
                    }
                });
                mSearch.reverseGeoCode(new ReverseGeoCodeOption().location(ll));
            }
        }
    }


    private class MyPoiOverlay extends PoiOverlay {

        public MyPoiOverlay(BaiduMap baiduMap) {
            super(baiduMap);
        }

        @Override
        public boolean onPoiClick(Marker marker) {
            super.onPoiClick(marker);
            int index = marker.getExtraInfo().getInt("index");
            PoiInfo poi = getPoiResult().getAllPoi().get(index);
            // if (poi.hasCaterDetails) {
            if(clickMarker != null){
                clickMarker.setIcon(originBitmapDescriptor);
            }
            clickMarker = marker;
            mPoiSearch.searchPoiDetail((new PoiDetailSearchOption())
                    .poiUid(poi.uid));
            // }
            return true;
        }
    }


}
