package com.juju.app.fragment;

import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.Rect;
import android.graphics.Typeface;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.RadioButton;
import android.widget.RadioGroup;

import com.baidu.location.BDLocation;
import com.baidu.location.BDLocationListener;
import com.baidu.location.LocationClient;
import com.baidu.location.LocationClientOption;
import com.handmark.pulltorefresh.library.PullToRefreshBase;
import com.handmark.pulltorefresh.library.PullToRefreshListView;
import com.juju.app.R;
import com.juju.app.activity.party.PartyActivity;
import com.juju.app.activity.party.PartyDetailActivity;
import com.juju.app.adapters.PartyListAdapter;
import com.juju.app.annotation.CreateFragmentUI;
import com.juju.app.entity.Party;
import com.juju.app.entity.User;
import com.juju.app.entity.chat.GroupEntity;
import com.juju.app.entity.http.GetPartysRes;
import com.juju.app.golobal.Constants;
import com.juju.app.golobal.JujuDbUtils;
import com.juju.app.https.HttpCallBack;
import com.juju.app.https.HttpCallBack4OK;
import com.juju.app.ui.base.BaseFragment;
import com.juju.app.ui.base.CreateUIHelper;
import com.juju.app.utils.ActivityUtil;
import com.juju.app.utils.SpfUtil;
import com.juju.app.utils.ToastUtil;

import org.apache.http.message.BasicNameValuePair;
import org.xutils.common.Callback;
import org.xutils.db.Selector;
import org.xutils.ex.DbException;
import org.xutils.view.annotation.ContentView;

import java.util.ArrayList;
import java.util.List;

/**
 * 项目名称：juju
 * 类描述：聚会—Fragment
 * 创建人：gm
 * 日期：2016/2/18 15:10
 * 版本：V1.0.0
 */
@ContentView(R.layout.layout_group_list)
@CreateFragmentUI(viewId = R.layout.layout_group_list)
public class GroupPartyFragment extends BaseFragment implements CreateUIHelper, RadioGroup.OnCheckedChangeListener,TextWatcher,ListView.OnItemClickListener,PartyListAdapter.Callback, HttpCallBack,PullToRefreshBase.OnRefreshListener {

    private static final String TAG = "GroupPartyFragment";
    private PullToRefreshListView listView;

//    private BitmapUtils bitmapUtils;
//    private BitmapDisplayConfig bdConfig;
    private PartyListAdapter partyListAdapter;
    private LayoutInflater inflater;
    private EditText searchInput;

    private RadioGroup partyTypeGroup;
    private RadioButton allBtn;
    private RadioButton attendBtn;
    private RadioButton followBtn;
    List<Party> partyList;
    private int filterType = 0;
    private int pageIndex = 0;
    private int pageSize = 15;
    private long totalSize = 0;

    LocationClient mLocClient;
    public MyLocationListenner myListener = new MyLocationListenner();


    @Override
    public void onStop() {
        super.onStop();
        System.out.println("GroupPartyFragment onStop:" + inflater.toString());
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater,
                             ViewGroup container, Bundle savedInstanceState) {

        this.inflater = inflater;
        return super.onCreateView(inflater, container, savedInstanceState);
    }

    @Override
    public void onResume() {
        Log.d(TAG, "onResume method");
        super.onResume();
        if(JujuDbUtils.needRefresh(Party.class)) {
            try {
                pageIndex = 0;
                Selector selector = JujuDbUtils.getInstance(getContext()).selector(Party.class).where("status", ">", -1);
                switch (filterType){
                    case 0:
                        break;
                    case 1:
                        selector.where("attendFlag","=",1);
                        break;
                    case 2:
                        selector.where("followFlag","=",1);
                        break;
                }
                totalSize = selector.count();
                selector.orderBy("local_id", true).offset(pageIndex*pageSize).limit(pageSize);
                partyList = selector.findAll();
            } catch (DbException e) {
                e.printStackTrace();
            }

            if(partyList.size()>=totalSize){
                listView.getLoadingLayoutProxy().setReleaseLabel(getResources().getString(R.string.pull_up_no_data_label));
            }
            if(partyList != null) {
                try {
                    for (Party part : partyList) {
                        User dbUser = JujuDbUtils.getInstance(getContext())
                                .selector(User.class).where("user_no", "=", part.getUserNo()).findFirst();
                        part.setCreator(dbUser);
                    }
                } catch (DbException e) {
                    e.printStackTrace();
                }
            }

            partyListAdapter.setPartyList(partyList);
            partyListAdapter.notifyDataSetChanged();
        }
    }

    @Override
    public void loadData() {


        // 开启定位图层
        mLocClient = new LocationClient(getContext());
        mLocClient.registerLocationListener(myListener);
        LocationClientOption option = new LocationClientOption();
        option.setOpenGps(true); // 打开gps
        option.setCoorType("bd09ll"); // 设置坐标类型
        option.setScanSpan(1000);
        mLocClient.setLocOption(option);
        mLocClient.start();


        partyTypeGroup = (RadioGroup) findViewById(R.id.party_type);
        allBtn = (RadioButton) findViewById(R.id.all_party);
        attendBtn = (RadioButton) findViewById(R.id.attend_party);
        followBtn = (RadioButton) findViewById(R.id.follow_party);
        searchInput = (EditText) findViewById(R.id.txt_search);


        partyTypeGroup.setOnCheckedChangeListener(this);
        searchInput.addTextChangedListener(this);

        listView = (PullToRefreshListView) findViewById(R.id.listPartyView);

        Drawable loadingDrawable = getResources().getDrawable(R.drawable.pull_to_refresh_indicator);
        final int indicatorWidth = (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 29,
                getResources().getDisplayMetrics());
        loadingDrawable.setBounds(new Rect(0, indicatorWidth, 0, indicatorWidth));
//        listView.getLoadingLayoutProxy().setRefreshingVisible(false);
        listView.getLoadingLayoutProxy().setLoadingDrawable(loadingDrawable);
        listView.getLoadingLayoutProxy().setPullLabel(getResources().getString(R.string.pull_up_refresh_pull_label));
        listView.getRefreshableView().setCacheColorHint(Color.WHITE);
        listView.getRefreshableView().setSelector(new ColorDrawable(Color.WHITE));
        listView.getRefreshableView().setOnItemClickListener(this);
        listView.setOnRefreshListener(this);


//        bitmapUtils = new BitmapUtils(getContext());
//        //  配置缓存大小100K
//        bitmapUtils.configDefaultCacheExpiry(128 * 1024);
//        bitmapUtils.configDiskCacheEnabled(true);
//        bitmapUtils.configDefaultCacheExpiry(2048 * 1024);
//
//        bdConfig = new BitmapDisplayConfig();
//
//        //设置显示图片特性
//        bdConfig.setBitmapConfig(Bitmap.Config.ARGB_4444);
//        bdConfig.setBitmapMaxSize(BitmapCommonUtils.getScreenSize(getActivity())); //图片的最大尺寸
////        bdConfig.setLoadingDrawable(GroupActivity.this.getResources().getDrawable(R.mipmap.ic_launcher)); //加载时显示的图片
////        bdConfig.setLoadFailedDrawable(GroupActivity.this.getResources().getDrawable(R.mipmap.ic_launcher)); //加载失败时显示的图片
//        bdConfig.setShowOriginal(false); //不显示源图片
////        bdConfig.setAnimation(AnimationUtils.loadAnimation(context, R.anim.slide_in_from_top));
//        bitmapUtils.configDefaultDisplayConfig(bdConfig);

        loadPartyData();
    }

    private void loadPartyData() {
        try {
            Selector selector = JujuDbUtils.getInstance(getContext()).selector(Party.class).where("status", ">", -1);
            totalSize = selector.count();
            selector.orderBy("local_id", true).offset(pageIndex*pageSize).limit(pageSize);
            partyList = selector.findAll();
            if(partyList == null) {
                partyList = new ArrayList<Party>();
            }

            if(partyList.size()>=totalSize){
                listView.getLoadingLayoutProxy().setReleaseLabel(getResources().getString(R.string.pull_up_no_data_label));
            }
            wrapPartyList(partyList);

        } catch (DbException e) {
            e.printStackTrace();
        }

//            UserInfoBean userInfoBean = BaseApplication.getInstance().getUserInfoBean();
//            Map<String, Object> valueMap = new HashMap<String, Object>();
//            valueMap.put("userNo", userInfoBean.getJujuNo());
//            valueMap.put("token", userInfoBean.getToken());
//            valueMap.put("queryType",2);
//            valueMap.put("queryId",userInfoBean.getJujuNo());
//            valueMap.put("index",0);
//            valueMap.put("size",10);
//
//            JlmHttpClient<Map<String, Object>> client = new JlmHttpClient<Map<String, Object>>(
//                    R.id.txt_party, HttpConstants.getUserUrl() + "/getPartys", this, valueMap,
//                    GetPartysRes.class);
//            try {
//                client.sendGet();
//            } catch (UnsupportedEncodingException e) {
//                e.printStackTrace();
//            } catch (JSONException e) {
//                e.printStackTrace();
//            }
    }

    @Override
    public void initView() {

    }

    private void wrapPartyList(List<Party> partyList) {
        if(partyList != null) {
            for(Party part : partyList) {
                try {
                    User dbUser = JujuDbUtils.getInstance(getContext())
                            .selector(User.class).where("user_no", "=", part.getUserNo()).findFirst();
                    part.setCreator(dbUser);
                } catch (DbException e) {
                    e.printStackTrace();
                }
            }
        }
        partyListAdapter = new PartyListAdapter(inflater, partyList,this);
        listView.getRefreshableView().setAdapter(partyListAdapter);
    }


    @Override
    public void onCheckedChanged(RadioGroup group, int checkedId) {
        if (partyTypeGroup.getId() == group.getId()) {
            try {
                pageIndex = 0;
                Selector selector = JujuDbUtils.getInstance(getContext()).selector(Party.class).where("status", ">", -1);

                //  处理渲染用户相关的所有聚会
                if (checkedId == allBtn.getId()) {
                    filterType = 0;
                }
                //  处理渲染用户参加的所有聚会
                if (checkedId == attendBtn.getId()) {
                    filterType = 1;
                    selector.where("attendFlag", "=", 1);
                }
                //  处理渲染用户关注的所有聚会
                if (checkedId == followBtn.getId()) {
                    filterType = 2;
                    selector.where("followFlag","=",1);
                }
                totalSize = selector.count();
                selector.orderBy("local_id", true).offset(pageIndex*pageSize).limit(pageSize);
                partyList = selector.findAll();
            } catch (DbException e) {
                e.printStackTrace();
            }
            if(partyList == null) {
                partyList = new ArrayList<Party>();
            }
            if(partyList.size()>= totalSize){
                listView.getLoadingLayoutProxy().setReleaseLabel(getResources().getString(R.string.pull_up_no_data_label));
            }else{
                listView.getLoadingLayoutProxy().setReleaseLabel(getResources().getString(R.string.pull_to_refresh_from_bottom_release_label));
            }
            partyListAdapter.setPartyList(partyList);
            partyListAdapter.setFilterType(filterType);
            partyListAdapter.notifyDataSetChanged();
        }

    }

    @Override
    public void beforeTextChanged(CharSequence s, int start, int count, int after) {

    }
    //  搜索框搜索处理
    @Override
    public void onTextChanged(CharSequence s, int start, int before, int count) {
        partyListAdapter.getFilter().filter(s);
    }

    @Override
    public void afterTextChanged(Editable s) {

    }

    @Override
    public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
        Party curParty = (Party)listView.getRefreshableView().getItemAtPosition(position);
        switch(curParty.getStatus()){
            case 0: // 召集中
                BasicNameValuePair param = new BasicNameValuePair(Constants.PARTY_ID,curParty.getId());
                ActivityUtil.startActivity(getActivity(), PartyDetailActivity.class,param);
                break;
            case 1: //  进行中
                ActivityUtil.startActivity(getActivity(), PartyActivity.class,new BasicNameValuePair(Constants.PARTY_ID,curParty.getId()));
                break;
            case 2: //  已结束
                ActivityUtil.startActivity(getActivity(), PartyActivity.class,new BasicNameValuePair(Constants.PARTY_ID,curParty.getId()));
                break;
        }
    }

    @Override
    public void click(View v) {
        ToastUtil.showShortToast(getActivity(), ((Integer) v.getTag(R.id.tag_index)).toString(), 1);
        //  TODO    增加是否关注的修改、保存。

    }

    @Override
    public void follow(Party party, int follow) {
        party.setFollowFlag(follow);
        JujuDbUtils.saveOrUpdate(party);
        if(filterType==2){
            partyList.remove(party);
        }
        partyListAdapter.notifyDataSetChanged();
    }

//    @Override
//    public void onSuccess(ResponseInfo<String> responseInfo, int accessId, Object... obj) {
//
//        switch (accessId) {
//            case R.id.txt_party:
//                if(obj != null && obj.length > 0) {
//                    GetPartysRes partysRes = (GetPartysRes)obj[0];
//                    int status = partysRes.getStatus();
//                    if(status == 0) {
//                        partyList = partysRes.getPartys();
//                        wrapPartyList(partyList);
//                    }else{
//
//                    }
//                }
//                break;
//        }
//    }
//
//    @Override
//    public void onFailure(HttpException error, String msg, int accessId) {
//        System.out.println("accessId:" + accessId + "\r\n msg:" + msg + "\r\n code:" +
//                error.getExceptionCode());
//    }

    @Override
    public void onSuccess(Object obj, int accessId) {
        switch (accessId) {
            case R.id.txt_party:
                if(obj != null) {
                    GetPartysRes partysRes = (GetPartysRes)obj;
                    int status = partysRes.getStatus();
                    if(status == 0) {
                        partyList = partysRes.getPartys();

                        wrapPartyList(partyList);
                    }else{

                    }
                }
                break;
        }
    }

    @Override
    public void onFailure(Throwable ex, boolean isOnCallback, int accessId) {
        System.out.println("accessId:" + accessId + "\r\n isOnCallback:" + isOnCallback );
        Log.e(TAG, "onFailure", ex);
    }

    @Override
    public void onCancelled(Callback.CancelledException cex) {

    }

    @Override
    public void onFinished() {

    }

    @Override
    public void onRefresh(PullToRefreshBase refreshView) {
        if(partyList.size()>=totalSize){
            listView.onRefreshComplete();
            return;
        }
        // 获取下一页数据
        ListView partyListView = listView.getRefreshableView();
        int preSum = partyList.size();
        try {
            Selector selector = JujuDbUtils.getInstance(getContext()).selector(Party.class).where("status", ">", -1);
            switch (filterType){
                case 0:
                    break;
                case 1:
                    selector.where("attendFlag", "=", 1);
                    break;
                case 2:
                    selector.where("followFlag","=",1);
                    break;
            }


            totalSize = selector.count();
            selector.orderBy("local_id", true).offset(++pageIndex*pageSize).limit(pageSize);
            List<Party> pagePartyList = selector.findAll();
            partyList.addAll(pagePartyList);
        } catch (DbException e) {
            e.printStackTrace();
        }

        if(partyList.size()>=totalSize){
            listView.getLoadingLayoutProxy().setReleaseLabel(getResources().getString(R.string.pull_up_no_data_label));
        }
        partyListAdapter.setPartyList(partyList);
        int afterSum = partyList.size();
        partyListView.setSelection(afterSum-preSum);
        partyListAdapter.notifyDataSetChanged();
        listView.onRefreshComplete();
    }




    /**
     * 定位SDK监听函数
     */
    private class MyLocationListenner implements BDLocationListener {

        @Override
        public void onReceiveLocation(BDLocation location) {
            if (location == null) {
                return;
            }
            SpfUtil.put(getContext().getApplicationContext(), Constants.LOCATION, location.getLatitude()+","+location.getLongitude());
            mLocClient.stop();
            mLocClient = null;
        }
    }
}
