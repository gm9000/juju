package com.juju.app.fragment;

import android.annotation.SuppressLint;
import android.graphics.Color;
import android.graphics.Rect;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.util.Log;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AbsListView;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;

import com.baidu.location.BDLocation;
import com.baidu.location.BDLocationListener;
import com.baidu.location.LocationClient;
import com.baidu.location.LocationClientOption;
import com.handmark.pulltorefresh.library.PullToRefreshBase;
import com.handmark.pulltorefresh.library.PullToRefreshListView;
import com.juju.app.R;
import com.juju.app.activity.MainActivity;
import com.juju.app.activity.party.PartyDetailActivity;
import com.juju.app.activity.party.PartyLiveActivity;
import com.juju.app.adapters.PartyListAdapter;
import com.juju.app.annotation.CreateFragmentUI;
import com.juju.app.entity.Party;
import com.juju.app.enums.DisplayAnimation;
import com.juju.app.event.notify.PartyNotifyEvent;
import com.juju.app.golobal.Constants;
import com.juju.app.golobal.JujuDbUtils;
import com.juju.app.ui.base.BaseFragment;
import com.juju.app.ui.base.CreateUIHelper;
import com.juju.app.utils.ActivityUtil;
import com.juju.app.utils.SpfUtil;
import com.juju.app.view.MenuDisplayProcess;
import com.juju.app.view.SearchEditText;
import com.rey.material.app.BottomSheetDialog;

import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;
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
public class GroupPartyFragment extends BaseFragment implements CreateUIHelper,PartyListAdapter.Callback, PullToRefreshBase.OnRefreshListener2 {

    private static final String TAG = "GroupPartyFragment";
    private PullToRefreshListView listView;

    private PartyListAdapter partyListAdapter;
    private LayoutInflater inflater;

    private SearchEditText searchEditText;


    List<Party> partyList;
    private int pageIndex = 0;
    private int pageSize = 15;
    private long totalSize = 0;

    private BottomSheetDialog msgDialog;
    private TextView txtTop;
    private TextView txtStock;
    private TextView txtCancel;

    private ImageView topLeftBtn;

    private int lastVisibleItemPosition = 0;

    LocationClient mLocClient;
    public MyLocationListenner myListener = new MyLocationListenner();

    private MenuDisplayProcess mdProdess;
    private boolean menuIsShow = true;

    @SuppressLint("ValidFragment")
    public GroupPartyFragment(MenuDisplayProcess mdProdess){
        super();
        this.mdProdess = mdProdess;
    }

    public GroupPartyFragment(){
        super();
    }

    @Override
    public void onStop() {
        super.onStop();
        Log.d(TAG,"onStop:" + inflater.toString());
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EventBus.getDefault().register(GroupPartyFragment.this);
    }

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
                Selector selector = JujuDbUtils.getInstance().selector(Party.class).where("status", ">", -1);
                selector.and("follow_flag",">",-1);
                totalSize = selector.count();
                selector.orderBy("follow_flag",true).orderBy("local_id", true).offset(pageIndex*pageSize).limit(pageSize);
                partyList = selector.findAll();
            } catch (DbException e) {
                e.printStackTrace();
            }

            if(partyList.size()>=totalSize){
                listView.getLoadingLayoutProxy().setReleaseLabel(getResources().getString(R.string.pull_up_no_data_label));
            }

            partyListAdapter.setPartyList(partyList);
            if(partyListAdapter.isSearchMode()) {
                partyListAdapter.onSearch(searchEditText.getText().toString());
            }
            partyListAdapter.notifyDataSetChanged();
            JujuDbUtils.closeRefresh(Party.class);
        }
    }

    @Override
    public void onDestroy() {
        if(EventBus.getDefault().isRegistered(GroupPartyFragment.this)){
            EventBus.getDefault().unregister(GroupPartyFragment.this);
        }
        super.onDestroy();

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


        searchEditText = (SearchEditText) findViewById(R.id.filter_edit);
        searchEditText.addTextChangedListener(new TextWatcher() {

            @Override
            public void onTextChanged(CharSequence s, int start, int before,
                                      int count) {

                String key = s.toString();
                if(TextUtils.isEmpty(key)){
                    partyListAdapter.recover();
                    searchEditText.startAnimation(DisplayAnimation.UP_HIDDEN);
                    searchEditText.setVisibility(View.GONE);
                }else{
                    partyListAdapter.onSearch(key);
                }
            }

            @Override
            public void beforeTextChanged(CharSequence s, int start, int count,
                                          int after) {
            }

            @Override
            public void afterTextChanged(Editable s) {
            }
        });

        listView = (PullToRefreshListView) findViewById(R.id.listPartyView);

        Drawable loadingDrawable = getResources().getDrawable(R.drawable.pull_to_refresh_indicator);
        final int indicatorWidth = (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 29,
                getResources().getDisplayMetrics());
        loadingDrawable.setBounds(new Rect(0, indicatorWidth, 0, indicatorWidth));
        listView.getLoadingLayoutProxy().setLoadingDrawable(loadingDrawable);
        listView.getLoadingLayoutProxy().setPullLabel(getResources().getString(R.string.pull_up_refresh_pull_label));
        listView.getRefreshableView().setCacheColorHint(Color.WHITE);
        listView.getRefreshableView().setSelector(new ColorDrawable(Color.WHITE));
        listView.setOnRefreshListener(this);


        if(mdProdess!=null) {
            listView.setOnScrollListener(new AbsListView.OnScrollListener() {

                @Override
                public void onScrollStateChanged(AbsListView view, int scrollState) {
                    // TODO Auto-generated method stub
                    switch (scrollState) {
                        // 当不滚动时
                        case AbsListView.OnScrollListener.SCROLL_STATE_IDLE:// 当屏幕停止滚动时
                            break;
                        case AbsListView.OnScrollListener.SCROLL_STATE_TOUCH_SCROLL:// 当屏幕滚动时
                            break;
                        case AbsListView.OnScrollListener.SCROLL_STATE_FLING:// 当用户由于之前划动屏幕并抬起手指，屏幕产生惯性滑动时
                            break;
                    }
                }

                /**
                 * firstVisibleItem：当前能看见的第一个列表项ID（从0开始）
                 * visibleItemCount：当前能看见的列表项个数（小半个也算） totalItemCount：列表项共数
                 */
                @Override
                public void onScroll(AbsListView view, int firstVisibleItem,
                                     int visibleItemCount, int totalItemCount) {
                    if (firstVisibleItem > lastVisibleItemPosition) {// 上滑
                        if (menuIsShow) {
                            mdProdess.hiddenMenu();
                            menuIsShow = false;
                        }
                    } else if (firstVisibleItem < lastVisibleItemPosition) {// 下滑
                        if (!menuIsShow) {
                            mdProdess.showMenu();
                            menuIsShow = true;
                        }
                    } else {
                        return;
                    }
                    lastVisibleItemPosition = firstVisibleItem;
                }
            });
        }

        loadPartyData();
    }


    private void loadPartyData() {
        try {
            Selector selector = JujuDbUtils.getInstance().selector(Party.class).where("status", ">", -1);
            selector.and("follow_flag",">",-1);
            totalSize = selector.count();
            selector.orderBy("follow_flag",true).orderBy("local_id", true).offset(pageIndex*pageSize).limit(pageSize);
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

    }


    @Override
    public void initView() {
        topLeftBtn = ((MainActivity)getActivity()).getTopLeftBtn();
        topLeftBtn.setOnClickListener(new View.OnClickListener(){
            @Override
            public void onClick(View v) {
                if((searchEditText.getVisibility()==View.VISIBLE && TextUtils.isEmpty(searchEditText.getText().toString()))) {
                    searchEditText.startAnimation(DisplayAnimation.UP_HIDDEN);
                    searchEditText.setVisibility(View.GONE);
                }else{
                    searchEditText.startAnimation(DisplayAnimation.DOWN_SHOW);
                    searchEditText.setVisibility(View.VISIBLE);
                }
            }
        });
    }

    private void wrapPartyList(List<Party> partyList) {
        partyListAdapter = new PartyListAdapter(inflater,this);
        partyListAdapter.setPartyList(partyList);
        listView.getRefreshableView().setAdapter(partyListAdapter);
    }


    @Override
    public void follow(final Party party) {

        msgDialog = new BottomSheetDialog(getActivity());
        msgDialog.setCanceledOnTouchOutside(false);
        msgDialog.contentView(R.layout.layout_bottom_menu)
                .inDuration(300);
        txtTop = (TextView)msgDialog.findViewById(R.id.btn_menu1);
        txtStock = (TextView)msgDialog.findViewById(R.id.btn_menu2);
        txtCancel = (TextView)msgDialog.findViewById(R.id.txt_cancel);
        txtCancel.setOnClickListener(new View.OnClickListener(){
            @Override
            public void onClick(View v) {
                msgDialog.dismiss();
            }
        });

        if(party.getFollowFlag()==0) {
            txtTop.setText(R.string.top_location);
            txtTop.setOnClickListener(null);
            txtTop.setOnClickListener(new View.OnClickListener(){

                @Override
                public void onClick(View v) {
                    follow(party,1);
                    msgDialog.dismiss();
                }
            });
        }else{
            txtTop.setText(R.string.cancel_top_message);
            txtTop.setOnClickListener(null);
            txtTop.setOnClickListener(new View.OnClickListener(){

                @Override
                public void onClick(View v) {
                    follow(party,0);
                    msgDialog.dismiss();
                }
            });
        }
        txtStock.setText(R.string.stock);
        txtStock.setOnClickListener(null);
        txtStock.setOnClickListener(new View.OnClickListener(){

            @Override
            public void onClick(View v) {
                follow(party,-1);
                msgDialog.dismiss();
            }
        });

        msgDialog.show();
    }

    private void follow(Party party,int follow){
        party.setFollowFlag(follow);
        JujuDbUtils.saveOrUpdate(party);
        // TODO 处理特别关注和归档的操作
        switch(follow){
            //  归档
            case -1:
                partyList.remove(party);
                partyListAdapter.getMatchPartyList().remove(party);
                break;
            //  取消置顶
            case 0:
                partyListAdapter.reOrderParty();
                break;
            //  置顶
            case 1:
                partyListAdapter.reOrderParty();
                break;
        }
        partyListAdapter.notifyDataSetChanged();
    }

    @Override
    public void showParty(Party party) {
        switch(party.getStatus()){
            case 0: // 召集中
                ActivityUtil.startActivityNew(getActivity(), PartyDetailActivity.class,Constants.PARTY_ID,party.getId());
                break;
            case 1: //  进行中
                ActivityUtil.startActivityNew(getActivity(), PartyLiveActivity.class,Constants.PARTY_ID,party.getId());
                break;
            case 2: //  已结束
                ActivityUtil.startActivityNew(getActivity(), PartyLiveActivity.class,Constants.PARTY_ID,party.getId());
                break;
        }
    }


    @Override
    public void onPullDownToRefresh(PullToRefreshBase refreshView) {

    }

    @Override
    public void onPullUpToRefresh(PullToRefreshBase refreshView) {

        refreshView.postDelayed(new Runnable() {
            @Override
            public void run() {
                if(partyList.size()>=totalSize){
                    listView.onRefreshComplete();
                    return;
                }
                // 获取下一页数据
                ListView partyListView = listView.getRefreshableView();
                int preSum = partyListAdapter.getCount();
                try {
                    Selector selector = JujuDbUtils.getInstance().selector(Party.class).where("status", ">", -1);
                    selector.and("follow_flag",">",-1);
                    totalSize = selector.count();
                    selector.orderBy("follow_flag",true).orderBy("local_id", true).offset(++pageIndex*pageSize).limit(pageSize);
                    List<Party> pagePartyList = selector.findAll();
                    partyList.addAll(pagePartyList);
                } catch (DbException e) {
                    e.printStackTrace();
                }

                if(partyList.size()>=totalSize){
                    listView.getLoadingLayoutProxy().setReleaseLabel(getResources().getString(R.string.pull_up_no_data_label));
                }
                partyListAdapter.setPartyList(partyList);
                if(partyListAdapter.isSearchMode()) {
                    partyListAdapter.onSearch(searchEditText.getText().toString());
                }
                int afterSum = partyListAdapter.getCount();
                partyListView.setSelection(afterSum-preSum);
                partyListAdapter.notifyDataSetChanged();
                listView.onRefreshComplete();
            }
        }, 200);
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
            mLocClient.unRegisterLocationListener(myListener);
            mLocClient.stop();
            mLocClient = null;
        }
    }


    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onEvent4PartyNotifyEvent(PartyNotifyEvent event){
        Log.d(TAG,"PartyNotifyEvent:"+event);
        switch (event.event){
            case PARTY_RECRUIT_OK:
                onResume();
                break;
            case PARTY_CANCEL_OK:
                onResume();
                break;
            case PARTY_CONFIRM_OK:
                onResume();
                break;
        }
    }

}
