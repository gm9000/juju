package com.juju.app.activity.party;

import android.os.Bundle;
import android.view.View;
import android.view.WindowManager;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.juju.app.R;
import com.juju.app.adapters.MyInviteListAdapter;
import com.juju.app.bean.UserInfoBean;
import com.juju.app.entity.Invite;
import com.juju.app.entity.Party;
import com.juju.app.golobal.JujuDbUtils;
import com.juju.app.ui.base.BaseActivity;
import com.juju.app.ui.base.BaseApplication;
import com.juju.app.utils.ActivityUtil;


import org.xutils.ex.DbException;
import org.xutils.view.annotation.ContentView;
import org.xutils.view.annotation.Event;
import org.xutils.view.annotation.ViewInject;

import java.util.List;

@ContentView(R.layout.layout_my_invite_list)
public class MyInviteListActivity extends BaseActivity implements MyInviteListAdapter.Callback {

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



    @ViewInject(R.id.listview_invite)
    private ListView listViewInvite;

    private MyInviteListAdapter inviteListAdapter;

    private List<Invite> inviteList;

    private String userNo;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        initParam();
        initData();
        initView();
        initListeners();

    }

    @Override
    protected void onResume(){
        super.onResume();
        if(JujuDbUtils.needRefresh(Party.class)){
            initData();
        }
    }

    private void initListeners() {
    }

    private void initData() {

        UserInfoBean userInfoBean = BaseApplication.getInstance().getUserInfoBean();
        userNo = userInfoBean.getJujuNo();
        try {
            inviteList = JujuDbUtils.getInstance(getContext()).selector(Invite.class).orderBy("local_id", true).findAll();
        } catch (DbException e) {
            e.printStackTrace();
        }

        wrapInviteList(inviteList);
    }

    private void wrapInviteList(List<Invite> inviteList) {
        inviteListAdapter = new MyInviteListAdapter(this,inviteList,this);
        listViewInvite.setAdapter(inviteListAdapter);
        listViewInvite.setCacheColorHint(0);
    }


    private void initView() {

        txt_left.setVisibility(View.VISIBLE);
        txt_left.setText(R.string.top_left_back);

        RelativeLayout.LayoutParams layoutParams = (RelativeLayout.LayoutParams)txt_left.getLayoutParams();
        layoutParams.leftMargin = 15;
        txt_title.setText(R.string.group_invite);
        txt_left.setLayoutParams(layoutParams);
        txt_right.setVisibility(View.GONE);
        img_right.setVisibility(View.GONE);

    }


    public void initParam() {
    }

    @Event(R.id.txt_left)
    private void cancelOperation(View view){
        ActivityUtil.finish(this);
    }

    @Override
    public void checkFailInvite(int position) {
        Invite invite = inviteList.get(position);
        invite.setStatus(0);
        JujuDbUtils.saveOrUpdate(invite);
        //TODO 邀请忽略，将通知后台服务
        inviteListAdapter.setInviteList(inviteList);
        inviteListAdapter.notifyDataSetChanged();
    }

    @Override
    public void passOrDeleteInvite(int position) {
        Invite invite = inviteList.get(position);
        //  接收的邀请消息，接收的操作请求
        if(invite.getFlag()==1 && invite.getStatus()==-1){
            invite.setStatus(1);
            JujuDbUtils.saveOrUpdate(invite);
            //TODO 邀请接收，将通知后台服务
        //  发出的邀请信息，进行删除操作
        }else{
            JujuDbUtils.delete(invite);
            inviteList.remove(position);
        }

        inviteListAdapter.setInviteList(inviteList);
        inviteListAdapter.notifyDataSetChanged();
    }
}
