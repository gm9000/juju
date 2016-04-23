package com.juju.app.activity.party;

import android.os.Bundle;
import android.view.View;
import android.view.WindowManager;
import android.widget.AdapterView;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.RelativeLayout;
import android.widget.TextView;

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
import com.lidroid.xutils.db.sqlite.Selector;
import com.lidroid.xutils.db.sqlite.WhereBuilder;
import com.lidroid.xutils.exception.DbException;
import com.lidroid.xutils.view.annotation.ContentView;
import com.lidroid.xutils.view.annotation.ViewInject;
import com.lidroid.xutils.view.annotation.event.OnClick;

import org.apache.http.message.BasicNameValuePair;

import java.util.List;

@ContentView(R.layout.layout_my_party_list)
public class MyPartyListlActivity extends BaseActivity implements AdapterView.OnItemClickListener, MyPartyListAdapter.Callback {

    private static final String TAG = "MyPartyListlActivity";

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
    private ListView listPartyView;

    private MyPartyListAdapter partyListAdapter;

    private List<Party> partyList;

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
            initData();
        }
    }

    private void initListeners() {
        listPartyView.setOnItemClickListener(this);
    }

    private void initData() {

        UserInfoBean userInfoBean = BaseApplication.getInstance().getUserInfoBean();
        try {
            partyList = JujuDbUtils.getInstance(getContext()).findAll(Selector.from(Party.class).where("createUserNo", "=", userInfoBean.getJujuNo()).orderBy("local_id", true));
        } catch (DbException e) {
            e.printStackTrace();
        }

        wrapPartyList(partyList);
    }

    private void wrapPartyList(List<Party> planList) {
        partyListAdapter = new MyPartyListAdapter(this,partyList,this);
        listPartyView.setAdapter(partyListAdapter);
        listPartyView.setCacheColorHint(0);
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

    }


    public void initParam() {
    }

    @OnClick(R.id.txt_left)
    private void cancelOperation(View view){
        ActivityUtil.finish(this);
    }



    @Override
    public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
        Party curParty = (Party)listPartyView.getItemAtPosition(position);
        switch(curParty.getStatus()){
            case -1: // 草稿箱
                ActivityUtil.startActivity(this, PartyCreateActivity.class,new BasicNameValuePair(Constants.PARTY_ID,curParty.getId()));
                break;
            case 0: // 召集中
                BasicNameValuePair param = new BasicNameValuePair(Constants.PARTY_ID,curParty.getId());
                ActivityUtil.startActivity(this, PartyDetailActivity.class,param);
                break;
            case 1: //  进行中
                ActivityUtil.startActivity(this, PartyActivity.class);
                break;
            case 2: //  已结束
                ActivityUtil.startActivity(this, PartyActivity.class);
                break;
        }
    }

    @Override
    public void deleteParty(int position) {

        try {
            Party party = partyList.get(position);
            JujuDbUtils.getInstance(this).delete(Plan.class, WhereBuilder.b("partyId","=",party.getId()));
            JujuDbUtils.getInstance(this).delete(party);
        } catch (DbException e) {
            e.printStackTrace();
        }
        partyList.remove(position);
        partyListAdapter.setPartyList(partyList);
        partyListAdapter.notifyDataSetChanged();
    }
}
