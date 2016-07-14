package com.juju.app.activity.party;

import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.WindowManager;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

import com.baidu.mapapi.model.LatLng;
import com.juju.app.R;
import com.juju.app.adapters.PlanInfoListAdapter;
import com.juju.app.bean.UserInfoBean;
import com.juju.app.config.HttpConstants;
import com.juju.app.entity.Party;
import com.juju.app.entity.Plan;
import com.juju.app.entity.User;
import com.juju.app.event.notify.PartyNotifyEvent;
import com.juju.app.event.notify.PlanVoteEvent;
import com.juju.app.golobal.AppContext;
import com.juju.app.golobal.Constants;
import com.juju.app.golobal.JujuDbUtils;
import com.juju.app.https.HttpCallBack;
import com.juju.app.https.JlmHttpClient;
import com.juju.app.service.notify.PartyCancelNotify;
import com.juju.app.service.notify.PartyConfirmNotify;
import com.juju.app.ui.base.BaseActivity;
import com.juju.app.utils.ActivityUtil;
import com.juju.app.utils.ImageLoaderUtil;
import com.juju.app.utils.SpfUtil;
import com.juju.app.utils.ToastUtil;
import com.juju.app.view.RoundImageView;
import com.juju.app.view.dialog.WarnTipDialog;
import com.juju.app.view.scroll.NoScrollListView;

import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;
import org.json.JSONException;
import org.json.JSONObject;
import org.xutils.common.Callback;
import org.xutils.ex.DbException;
import org.xutils.view.annotation.ContentView;
import org.xutils.view.annotation.Event;
import org.xutils.view.annotation.ViewInject;

import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@ContentView(R.layout.activity_party_detail)
public class PartyDetailActivity extends BaseActivity implements HttpCallBack, AdapterView.OnItemClickListener, AdapterView.OnItemLongClickListener {

    private static final String TAG = "PartyDetailActivity";

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


    @ViewInject(R.id.img_head)
    private RoundImageView img_head;
    @ViewInject(R.id.txt_nick_name)
    private TextView txt_nickName;
    @ViewInject(R.id.txt_party_title)
    private TextView txt_partyTitle;
    @ViewInject(R.id.txt_description)
    private TextView txt_description;


    @ViewInject(R.id.btn_start)
    private Button btn_start;
    @ViewInject(R.id.btn_cancel)
    private Button btn_cancel;

    @ViewInject(R.id.listview_plan)
    private NoScrollListView listview_plan;

    @ViewInject(R.id.txt_fullDesc)
    private TextView txt_fullDesc;

    private String groupId;
    private String partyId;
    private String selectedPlanId;
    private PlanInfoListAdapter planListAdapter;
    private List<Plan> planList;
    private boolean isOwner;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EventBus.getDefault().register(this);
        initParam();
        initData();
        initView();
        initListeners();
        getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_PAN);

    }
    @Override
    protected void onDestroy(){
        if(EventBus.getDefault().isRegistered(this)){
            EventBus.getDefault().unregister(this);
        }
        super.onDestroy();
    }

    private void initListeners() {
        listview_plan.setOnItemClickListener(this);
        listview_plan.setOnItemLongClickListener(this);
    }

    private void initData() {

        Party party = null;
        try {
            party = JujuDbUtils.getInstance().selector(Party.class).where("id", "=", partyId).findFirst();
        } catch (DbException e) {
            e.printStackTrace();
        }

        if(party.isNew()){
            party.setNew(false);
            JujuDbUtils.saveOrUpdate(party);
        }
        groupId = party.getGroupId();

        txt_partyTitle.setText(party.getName());
        txt_description.setText(party.getDesc());

        String userNo = party.getUserNo();
        User creator = party.getCreator();
        ImageLoaderUtil.getImageLoaderInstance().displayImage(HttpConstants.getUserUrl() + "/getPortraitSmall?targetNo=" + userNo, img_head, ImageLoaderUtil.DISPLAY_IMAGE_OPTIONS);
        txt_nickName.setText(creator.getNickName());

        isOwner = creator.getUserNo().equals(AppContext.getUserInfoBean().getUserNo());

        try {
            planList = JujuDbUtils.getInstance().selector(Plan.class).where("party_id", "=", partyId).findAll();
        } catch (DbException e) {
            e.printStackTrace();
        }
        if (planList == null) {
            planList = new ArrayList<Plan>();
        }
        wrapPlanList(planList);
    }

    private void wrapPlanList(List<Plan> planList) {
        String latLonStr = (String) SpfUtil.get(getApplicationContext(), Constants.LOCATION, null);
        LatLng location = null;
        if (latLonStr != null) {
            String[] latLonArray = latLonStr.split(",");
            location = new LatLng(Double.parseDouble(latLonArray[0]), Double.parseDouble(latLonArray[1]));
        }
        planListAdapter = new PlanInfoListAdapter(this, planList, isOwner, location);
        listview_plan.setAdapter(planListAdapter);
        listview_plan.setCacheColorHint(0);
    }


    private void initView() {

        if (isOwner) {
            btn_start.setVisibility(View.VISIBLE);
            btn_cancel.setVisibility(View.VISIBLE);
        } else {
            btn_start.setVisibility(View.GONE);
            btn_cancel.setVisibility(View.GONE);
        }


        img_back.setVisibility(View.VISIBLE);
        txt_left.setVisibility(View.VISIBLE);
        txt_left.setText(R.string.top_left_back);

        txt_title.setText(R.string.group_party);
        txt_right.setVisibility(View.GONE);
        img_right.setVisibility(View.GONE);

    }


    public void initParam() {
        partyId = getIntent().getStringExtra(Constants.PARTY_ID);
    }

    @Event(R.id.txt_left)
    private void cancelOperation(View view) {
        ActivityUtil.finish(this);
    }


    @Event(R.id.btn_start)
    private void startParty(View view) {
        String planId = null;
        for (Plan plan : planList) {
            if (plan.getStatus() == 1) {
                planId = plan.getId();
                break;
            }
        }

        selectedPlanId = planId;

        if (selectedPlanId == null) {
            ToastUtil.showShortToast(this, "请选择确定的方案", 1);
        } else {
            WarnTipDialog tipdialog = new WarnTipDialog(this, "正式启动聚会？");
            tipdialog.setBtnOkLinstener(new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    startPartyToServer(selectedPlanId);
                }
            });
            tipdialog.show();
        }
    }

    @Event(R.id.btn_cancel)
    private void cancelParty(View view) {
        WarnTipDialog tipdialog = new WarnTipDialog(this, "取消本次聚会？");
        tipdialog.setBtnOkLinstener(new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                deletePartyFromServer();
            }
        });
        tipdialog.show();
    }

    private void startPartyToServer(String planId) {

        //TODO 增加本地保存

        UserInfoBean userTokenInfoBean = AppContext.getUserInfoBean();
        Map<String, Object> reqBean = new HashMap<String, Object>();
        reqBean.put("userNo", userTokenInfoBean.getUserNo());
        reqBean.put("token", userTokenInfoBean.getToken());
        reqBean.put("partyId", partyId);
        reqBean.put("planId", planId);

        JlmHttpClient<Map<String, Object>> client = new JlmHttpClient<Map<String, Object>>(R.id.party_name, HttpConstants.getUserUrl() + "/confirmParty", this, reqBean, JSONObject.class);
        try {
            loading(true, R.string.starting);
            client.sendPost();
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    private void deletePartyFromServer() {

        UserInfoBean userTokenInfoBean = AppContext.getUserInfoBean();
        Map<String, Object> reqBean = new HashMap<String, Object>();
        reqBean.put("userNo", userTokenInfoBean.getUserNo());
        reqBean.put("token", userTokenInfoBean.getToken());
        reqBean.put("partyId", partyId);

        JlmHttpClient<Map<String, Object>> client = new JlmHttpClient<Map<String, Object>>(R.id.txt_cancel, HttpConstants.getUserUrl() + "/deleteParty", this, reqBean, JSONObject.class);
        try {
            loading(true, R.string.canceling);
            client.sendPost();
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        if (JujuDbUtils.needRefresh(Plan.class)) {
            try {
                planList = JujuDbUtils.getInstance().selector(Plan.class).where("party_id", "=", partyId).findAll();
            } catch (DbException e) {
                e.printStackTrace();
            }
            planListAdapter.setPlanList(planList);
            planListAdapter.notifyDataSetChanged();
            JujuDbUtils.closeRefresh(Plan.class);
        }

    }

    @Override
    public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
        Plan curPlan = (Plan) listview_plan.getItemAtPosition(position);

        Intent intent = new Intent();
        intent.setClass(this, PlanDetailActivity.class);
        intent.putExtra(Constants.GROUP_ID, groupId);
        intent.putExtra(Constants.PARTY_ID, curPlan.getPartyId());
        intent.putExtra(Constants.PLAN_ID, curPlan.getId());
        intent.putExtra(Constants.IS_OWNER, this.isOwner);

        this.startActivity(intent);
        this.overridePendingTransition(R.anim.push_left_in,
                R.anim.push_left_out);
    }

    public void operatePlan(int position) {
        Plan plan = planList.get(position);
        switch (plan.getStatus()) {
            case 0:
                for (Plan curPlan : planList) {
                    if (curPlan.getStatus() == 1) {
                        curPlan.setStatus(0);
                        JujuDbUtils.saveOrUpdate(curPlan);
                    }
                }
                plan.setStatus(1);
                break;
            case 1:
                plan.setStatus(0);
                break;
        }
        JujuDbUtils.saveOrUpdate(plan);
        planListAdapter.notifyDataSetChanged();
    }

    @Override
    public boolean onItemLongClick(AdapterView<?> parent, View view, int position, long id) {
        Plan curPlan = (Plan) listview_plan.getItemAtPosition(position);
        if (curPlan.getDesc() != null && !curPlan.getDesc().equals("")) {
            txt_fullDesc.setText("\t\t" + curPlan.getDesc());
        }
        txt_fullDesc.setVisibility(View.VISIBLE);
        return true;
    }

    @Event(R.id.txt_fullDesc)
    public void hiddenFullDesc(View v) {
        txt_fullDesc.setVisibility(View.GONE);
        txt_fullDesc.setText(R.string.nodescription);
    }

    @Override
    public void onSuccess(Object obj, int accessId, Object inputParameter) {
        switch (accessId) {
            case R.id.party_name:
                if (obj != null) {
                    JSONObject jsonRoot = (JSONObject) obj;
                    try {
                        int status = jsonRoot.getInt("status");
                        if (status == 0) {
                            completeLoading();
                            partyId = jsonRoot.getString("partyId");
                            Party party = JujuDbUtils.getInstance().selector(Party.class).where("id", "=", partyId).findFirst();
                            party.setStatus(1);
                            party.setAttendFlag(1);
                            Plan plan = JujuDbUtils.getInstance().selector(Plan.class).where("id", "=", selectedPlanId).findFirst();
                            party.setCoverUrl(plan.getCoverUrl());
                            party.setTime(plan.getStartTime());
                            JujuDbUtils.saveOrUpdate(party);
                            //  通知 Party已经启动
                            PartyNotifyEvent.PartyNotifyBean partyNotifyBean = PartyNotifyEvent
                                    .PartyNotifyBean.valueOf(party.getGroupId(), partyId, party.getName(),
                                            AppContext.getUserInfoBean().getUserNo()
                                            , AppContext.getUserInfoBean().getNickName());
                            partyNotifyBean.setPlanId(selectedPlanId);
                            PartyConfirmNotify.instance().executeCommand4Send(partyNotifyBean);
                            ActivityUtil.finish(this);
                        } else {
                            completeLoading();
                            Log.e(TAG, "return status code:" + status);
                        }
                    } catch (JSONException e) {
                        Log.e(TAG, "回调解析失败", e);
                        e.printStackTrace();
                    } catch (DbException e) {
                        e.printStackTrace();
                    }
                }
                break;
            case R.id.txt_cancel:
                if (obj != null) {
                    JSONObject jsonRoot = (JSONObject) obj;
                    try {
                        int status = jsonRoot.getInt("status");
                        if (status == 0) {
                            completeLoading();
                            Party party = JujuDbUtils.getInstance().selector(Party.class).where("id", "=", partyId).findFirst();
                            party.setStatus(-1);
                            party.setId(String.valueOf(party.getLocalId()));
                            party.setFollowFlag(0);
                            party.setAttendFlag(1);
                            JujuDbUtils.saveOrUpdate(party);
                            List<Plan> planList = JujuDbUtils.getInstance().selector(Plan.class).where("party_id", "=", partyId).findAll();
                            for (Plan plan : planList) {
                                plan.setPartyId(party.getId());
                                JujuDbUtils.saveOrUpdate(plan);
                            }
                            //  通知 Party已经取消,其他用户删除本地记录
                            PartyNotifyEvent.PartyNotifyBean partyNotifyBean = PartyNotifyEvent
                                    .PartyNotifyBean.valueOf(party.getGroupId(), partyId, party.getName(),
                                            AppContext.getUserInfoBean().getUserNo()
                                            , AppContext.getUserInfoBean().getNickName());
                            PartyCancelNotify.instance().executeCommand4Send(partyNotifyBean);
                            ActivityUtil.finish(this);
                        } else {
                            completeLoading();
                            String desc = jsonRoot.getString("desc");
                            showMsgDialog(getResValue(desc));
                            Log.e(TAG, "return status code:" + status);
                        }
                    } catch (JSONException e) {
                        Log.e(TAG, "回调解析失败", e);
                        e.printStackTrace();
                    } catch (DbException e) {
                        e.printStackTrace();
                    }
                }
                break;
        }
    }

    @Override
    public void onFailure(Throwable ex, boolean isOnCallback, int accessId, Object inputParameter) {
        completeLoading();
        System.out.println("accessId:" + accessId + "\r\n isOnCallback:" + isOnCallback);
        Log.e(TAG, "onFailure", ex);
    }

    @Override
    public void onCancelled(Callback.CancelledException cex) {

    }

    @Override
    public void onFinished() {

    }


    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onEvent4PlanVote(PlanVoteEvent event){
        Log.d(TAG,"PlanVoteEvent:"+event);
        switch (event.event){
            case PLAN_VOTE_OK:
                onResume();
                break;
        }
    }


}
