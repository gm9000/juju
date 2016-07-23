package com.juju.app.activity.party;

import android.graphics.Color;
import android.graphics.Rect;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.AdapterView;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.handmark.pulltorefresh.library.PullToRefreshBase;
import com.handmark.pulltorefresh.library.PullToRefreshListView;
import com.juju.app.R;
import com.juju.app.adapters.PartyListBackupAdapter;
import com.juju.app.entity.Party;
import com.juju.app.golobal.Constants;
import com.juju.app.golobal.JujuDbUtils;
import com.juju.app.ui.base.BaseActivity;
import com.juju.app.utils.ActivityUtil;
import com.juju.app.view.SearchEditText;

import org.xutils.db.Selector;
import org.xutils.ex.DbException;
import org.xutils.view.annotation.ContentView;
import org.xutils.view.annotation.Event;
import org.xutils.view.annotation.ViewInject;

import java.util.ArrayList;
import java.util.List;

@ContentView(R.layout.layout_stock_group_list)
public class StockPartyListActivity extends BaseActivity implements AdapterView.OnItemClickListener, PullToRefreshBase.OnRefreshListener2, PartyListBackupAdapter.Callback {

    private static final String TAG = "StockPartyListActivity";

    @ViewInject(R.id.listPartyView)
    private PullToRefreshListView listView;

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

    private PartyListBackupAdapter partyListAdapter;

    @ViewInject(R.id.filter_edit)
    private SearchEditText searchEditText;


    List<Party> partyList;
    private int pageIndex = 0;
    private int pageSize = 15;
    private long totalSize = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        initParam();
        initView();
        initListeners();
        initData();
        getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_PAN);

    }

    @Override
    protected void onResume(){
        super.onResume();
        if(JujuDbUtils.needRefresh(Party.class)) {
            try {
                pageIndex = 0;
                Selector selector = JujuDbUtils.getInstance().selector(Party.class).where("status", ">", -1);
                selector.and("follow_flag","=",-1);
                totalSize = selector.count();
                selector.orderBy("local_id", true).offset(pageIndex*pageSize).limit(pageSize);
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
        }
    }

    private void initListeners() {
        searchEditText.addTextChangedListener(new TextWatcher() {

            @Override
            public void onTextChanged(CharSequence s, int start, int before,
                                      int count) {

                String key = s.toString();
                if(TextUtils.isEmpty(key)){
                    partyListAdapter.recover();
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

        listView.getRefreshableView().setOnItemClickListener(this);
        listView.setOnRefreshListener(this);
    }

    private void initData() {

        try {
            Selector selector = JujuDbUtils.getInstance().selector(Party.class).where("status", ">", -1);
            selector.and("follow_flag","=",-1);
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

    }

    private void wrapPartyList(List<Party> planList) {
        partyListAdapter = new PartyListBackupAdapter(LayoutInflater.from(this),this);
        partyListAdapter.setStockMode(true);
        partyListAdapter.setPartyList(partyList);
        listView.getRefreshableView().setAdapter(partyListAdapter);
    }


    private void initView() {

        txt_left.setVisibility(View.VISIBLE);
        txt_left.setText(R.string.top_left_back);
        RelativeLayout.LayoutParams layoutParams = (RelativeLayout.LayoutParams)txt_left.getLayoutParams();
        layoutParams.leftMargin = 15;
        txt_title.setText(R.string.stock_party);
        txt_left.setLayoutParams(layoutParams);
        txt_right.setVisibility(View.GONE);
        img_right.setVisibility(View.GONE);

        Drawable loadingDrawable = getResources().getDrawable(R.drawable.pull_to_refresh_indicator);
        final int indicatorWidth = (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 29,
                getResources().getDisplayMetrics());
        loadingDrawable.setBounds(new Rect(0, indicatorWidth, 0, indicatorWidth));
        listView.getLoadingLayoutProxy().setLoadingDrawable(loadingDrawable);
        listView.getLoadingLayoutProxy().setPullLabel(getResources().getString(R.string.pull_up_refresh_pull_label));
        listView.getRefreshableView().setCacheColorHint(Color.WHITE);
        listView.getRefreshableView().setSelector(new ColorDrawable(Color.WHITE));

        View emptyView = getLayoutInflater().inflate(R.layout.layout_empty, null);
        ((ViewGroup)listView.getRefreshableView().getParent()).addView(emptyView,new ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
        listView.getRefreshableView().setEmptyView(emptyView);

    }


    public void initParam() {
    }

    @Event(R.id.txt_left)
    private void cancelOperation(View view){
        ActivityUtil.finish(this);
    }

    @Event(R.id.img_back)
    private void goBack(View view){
        ActivityUtil.finish(this);
    }



    @Override
    public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
        Party curParty = (Party)listView.getRefreshableView().getItemAtPosition(position);
        switch(curParty.getStatus()){
            case 0: // 召集中
//                BasicNameValuePair param = new BasicNameValuePair(Constants.PARTY_ID,curParty.getId());
                ActivityUtil.startActivityNew(this, PartyDetailActivity.class, Constants.PARTY_ID, curParty.getId());
                break;
            case 1: //  进行中
                ActivityUtil.startActivityNew(this, PartyLiveActivity.class, Constants.PARTY_ID, curParty.getId());
                break;
            case 2: //  已结束
                ActivityUtil.startActivityNew(this, PartyLiveActivity.class, Constants.PARTY_ID, curParty.getId());
                break;
        }
    }

    @Override
    public void follow(Party party, int follow) {
        party.setFollowFlag(follow);
        JujuDbUtils.saveOrUpdate(party);
        switch(follow){
            //  恢复归档
            case 0:
                partyList.remove(party);
                partyListAdapter.getMatchPartyList().remove(party);
                break;
        }
        partyListAdapter.notifyDataSetChanged();
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
                    selector.and("follow_flag","=",-1);
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
                if(partyListAdapter.isSearchMode()) {
                    partyListAdapter.onSearch(searchEditText.getText().toString());
                    listView.setShowViewWhileRefreshing(false);
                }
                int afterSum = partyListAdapter.getCount();
                partyListView.setSelection(afterSum-preSum);
                partyListAdapter.notifyDataSetChanged();
                listView.onRefreshComplete();
            }
        },200);
    }
}
