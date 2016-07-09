package com.juju.app.activity.party;

import android.content.DialogInterface;
import android.graphics.Color;
import android.graphics.Rect;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.util.TypedValue;
import android.view.View;
import android.view.WindowManager;
import android.widget.AdapterView;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.handmark.pulltorefresh.library.PullToRefreshBase;
import com.handmark.pulltorefresh.library.PullToRefreshListView;
import com.juju.app.R;
import com.juju.app.adapters.MyPartyListAdapter;
import com.juju.app.bean.UserInfoBean;
import com.juju.app.entity.Party;
import com.juju.app.entity.Plan;
import com.juju.app.golobal.Constants;
import com.juju.app.golobal.JujuDbUtils;
import com.juju.app.ui.base.BaseActivity;
import com.juju.app.ui.base.BaseApplication;
import com.juju.app.utils.ActivityUtil;
import com.juju.app.view.dialog.WarnTipDialog;

import org.apache.http.message.BasicNameValuePair;
import org.xutils.db.Selector;
import org.xutils.db.sqlite.WhereBuilder;
import org.xutils.ex.DbException;
import org.xutils.view.annotation.ContentView;
import org.xutils.view.annotation.Event;
import org.xutils.view.annotation.ViewInject;

import java.util.ArrayList;
import java.util.List;

@ContentView(R.layout.layout_my_party_list)
public class DraftPartyListActivity extends BaseActivity implements AdapterView.OnItemClickListener, MyPartyListAdapter.Callback,PullToRefreshBase.OnRefreshListener {

    private static final String TAG = "DraftPartyListActivity";

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



    @ViewInject(R.id.listPartyView)
    private PullToRefreshListView listPartyView;

    private MyPartyListAdapter partyListAdapter;

    private List<Party> partyList;

    private int pageIndex = 0;
    private int pageSize = 15;
    private long totalSize = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        initParam();
        initData();
        initView();
        initListeners();
        getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_PAN);

    }

    @Override
    protected void onResume(){
        super.onResume();
        if(JujuDbUtils.needRefresh(Party.class)){
            pageIndex = 0;
            initData();
        }
    }

    private void initListeners() {
        listPartyView.getRefreshableView().setOnItemClickListener(this);
    }

    private void initData() {

        UserInfoBean userInfoBean = BaseApplication.getInstance().getUserInfoBean();
        try {
            Selector selector = JujuDbUtils.getInstance().selector(Party.class).where("status", "=",-1);
            totalSize = selector.count();
            selector.orderBy("status").orderBy("local_id", true).offset(pageIndex*pageSize).limit(pageSize);;
            partyList = selector.findAll();
        } catch (DbException e) {
            e.printStackTrace();
        }

        if(partyList == null) {
            partyList = new ArrayList<Party>();
        }

        if(partyList.size() >= totalSize){
            listPartyView.getLoadingLayoutProxy().setReleaseLabel(getResources().getString(R.string.pull_up_no_data_label));
        }else{
            listPartyView.getLoadingLayoutProxy().setReleaseLabel(getResources().getString(R.string.pull_to_refresh_release_label));
        }

        wrapPartyList(partyList);
    }

    private void wrapPartyList(List<Party> planList) {
        partyListAdapter = new MyPartyListAdapter(this,partyList,this);
        listPartyView.getRefreshableView().setAdapter(partyListAdapter);
        listPartyView.getRefreshableView().setCacheColorHint(0);
    }


    private void initView() {

        txt_left.setVisibility(View.VISIBLE);
        txt_left.setText(R.string.top_left_back);

        RelativeLayout.LayoutParams layoutParams = (RelativeLayout.LayoutParams)txt_left.getLayoutParams();
        layoutParams.leftMargin = 15;
        txt_title.setText(R.string.my_party);
        txt_left.setLayoutParams(layoutParams);
        txt_right.setVisibility(View.GONE);
        img_right.setVisibility(View.GONE);

        Drawable loadingDrawable = getResources().getDrawable(R.drawable.pull_to_refresh_indicator);
        final int indicatorWidth = (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 29,
                getResources().getDisplayMetrics());
        loadingDrawable.setBounds(new Rect(0, indicatorWidth, 0, indicatorWidth));
//        listPartyView.getLoadingLayoutProxy().setRefreshingVisible(false);
        listPartyView.getLoadingLayoutProxy().setLoadingDrawable(loadingDrawable);
        listPartyView.getLoadingLayoutProxy().setPullLabel(getResources().getString(R.string.pull_up_refresh_pull_label));
        listPartyView.getRefreshableView().setCacheColorHint(Color.WHITE);
        listPartyView.getRefreshableView().setSelector(new ColorDrawable(Color.WHITE));
        listPartyView.setOnRefreshListener(this);

    }


    public void initParam() {
    }

    @Event(R.id.txt_left)
    private void cancelOperation(View view){
        ActivityUtil.finish(this);
    }



    @Override
    public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
        Party curParty = (Party)listPartyView.getRefreshableView().getItemAtPosition(position);
        switch(curParty.getStatus()){
            case -1: // 草稿箱
                ActivityUtil.startActivity(this, PartyCreateActivity.class,new BasicNameValuePair(Constants.PARTY_ID,curParty.getId()));
                break;
            case 0: // 召集中
                BasicNameValuePair param = new BasicNameValuePair(Constants.PARTY_ID,curParty.getId());
                ActivityUtil.startActivity(this, PartyDetailActivity.class,param);
                break;
            case 1: //  进行中
                ActivityUtil.startActivity(this, PartyActivity.class,new BasicNameValuePair(Constants.PARTY_ID,curParty.getId()));
                break;
            case 2: //  已结束
                ActivityUtil.startActivity(this, PartyActivity.class);
                break;
        }
    }

    @Override
    public void onRefresh(PullToRefreshBase refreshView) {
        if(partyList.size()>=totalSize){
            listPartyView.onRefreshComplete();
            return;
        }
        // 获取下一页数据
        ListView partyListView = listPartyView.getRefreshableView();
        int preSum = partyList.size();

        UserInfoBean userInfoBean = BaseApplication.getInstance().getUserInfoBean();
        try {
            Selector selector = JujuDbUtils.getInstance().selector(Party.class).where("user_no", "=", userInfoBean.getUserNo());
            totalSize = selector.count();
            selector.orderBy("local_id", true).offset(++pageIndex*pageSize).limit(pageSize);
            List<Party> pagePartyList = selector.findAll();
            partyList.addAll(pagePartyList);
        } catch (DbException e) {
            e.printStackTrace();
        }

        if(partyList.size()>=totalSize){
            listPartyView.getLoadingLayoutProxy().setReleaseLabel(getResources().getString(R.string.pull_up_no_data_label));
        }
        partyListAdapter.setPartyList(partyList);
        int afterSum = partyList.size();
        partyListView.setSelection(afterSum-preSum);
        partyListAdapter.notifyDataSetChanged();
        listPartyView.onRefreshComplete();
    }

    @Override
    public void deleteParty(final int position) {

        WarnTipDialog tipdialog = new WarnTipDialog(this,"确定要删除该聚会吗？");
        tipdialog.setBtnOkLinstener(new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                try {
                    Party party = partyList.get(position);
                    String delSql = "delete from plan_vote where plan_id in (select id from plan where party_id=\""+party.getId()+"\")";
                    JujuDbUtils.getInstance().execNonQuery(delSql);
                    JujuDbUtils.getInstance().delete(Plan.class, WhereBuilder.b("party_id", "=", party.getId()));
                    JujuDbUtils.delete(party);
                } catch (DbException e) {
                    e.printStackTrace();
                }
                partyList.remove(position);
                partyListAdapter.setPartyList(partyList);
                partyListAdapter.notifyDataSetChanged();
            }
        });
        tipdialog.show();

    }
}
